package ui.render

import app.cardThemeSpec
import presentation.solitaire.CardTheme
import presentation.solitaire.SolitaireUiState
import domain.model.Rank
import domain.readmodel.CardFace
import domain.readmodel.CardViewModel
import domain.readmodel.GameRenderModel
import domain.readmodel.rankAbbrev
import domain.readmodel.usesRedSuitInk
import presentation.solitaire.geometry.AxisAlignedRect
import presentation.solitaire.geometry.BoardLayout
import presentation.solitaire.geometry.DraggableCardInteractionTarget
import presentation.solitaire.geometry.PileHitRegion
import presentation.solitaire.geometry.SolitaireBoardInteractionSnapshot
import presentation.solitaire.geometry.calculateTableauHitHeight
import presentation.solitaire.geometry.foundationSuitsVisualLeftToRight
import presentation.solitaire.geometry.isHiddenCard
import presentation.solitaire.geometry.SolitaireBoardLayout
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.korge.tween.get
import korlibs.korge.tween.tween
import korlibs.image.bitmap.Bitmap
import korlibs.korge.view.Container
import korlibs.korge.view.SolidRect
import korlibs.korge.view.Text
import korlibs.korge.view.View
import korlibs.math.geom.slice.RectSlice
import korlibs.korge.view.addTo
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import korlibs.time.seconds

private fun Container.removeAllChildViews() {
    while (numChildren > 0) {
        removeChildAt(numChildren - 1)
    }
}

/** Result of [SolitaireBoardRenderer.render]: hit-test metadata plus stable views for pointer binding. */
class SolitaireRenderedBoard(
    val interaction: SolitaireBoardInteractionSnapshot,
    val pileTapTargets: Map<String, SolidRect>,
    val draggableTopCardRects: List<SolidRect>,
    val stockButton: Text,
    val recycleButton: Text,
    val autoMoveButton: Text,
    val undoButton: Text,
    val redoButton: Text,
    val newGameButton: Text,
) {
    fun draggableForTopCardSolidRect(cardRect: SolidRect): DraggableCardInteractionTarget? {
        val index = draggableTopCardRects.indexOfFirst { it === cardRect }
        if (index < 0) return null
        return interaction.draggableTopCards.getOrNull(index)
    }
}

/** Drag preview in stage coordinates; ghost centers on the top card in [stackCards]. */
data class BoardDragPreview(
    val sourcePileId: String,
    val sourceCardCount: Int,
    val stackCards: List<CardViewModel>,
    val x: Double,
    val y: Double,
) {
    val card: CardViewModel get() = stackCards.first()
}

/**
 * Renders the solitaire board in persistent layers under [rootContainer].
 *
 * [render] updates existing slots/views so card animation state can survive board updates.
 */
class SolitaireBoardRenderer(
    private val rootContainer: Container,
    private val viewportWidth: Double = 1280.0,
    private val viewportHeight: Double = 720.0,
) {
    companion object {
        private const val THEME_RENDERER_LOG_TAG = "KawaiiThemeRenderer"
    }

    private var playfieldMetrics = SolitaireBoardPlayfieldMetrics.forViewport(viewportWidth, viewportHeight)

    private val cardWidth get() = playfieldMetrics.cardWidth
    private val cardHeight get() = playfieldMetrics.cardHeight
    private val tableauCardOffsetY get() = playfieldMetrics.tableauCardOffsetY

    private var activeCardTheme: CardTheme = CardTheme.REGAL_ANIMALS
    private var activeThemeSpec = cardThemeSpec(activeCardTheme)
    private var texturePackerSliceByBaseName: Map<String, RectSlice<Bitmap>>? = null
    private var foxSpadePuppetSlices: FoxPuppetSheetLayout.PuppetSlices? = null
    private var foxHeartPuppetSlices: FoxPuppetSheetLayout.PuppetSlices? = null
    private var heartSuitPipBitmap: Bitmap? = null
    private var diamondSuitPipBitmap: Bitmap? = null
    private var spadeSuitPipBitmap: Bitmap? = null
    private var clubSuitPipBitmap: Bitmap? = null
    private var suitSymbolPainter = newSuitSymbolPainter()
    private var faceCardAnimalPainter = FaceCardAnimalPainter(
        texturePackerSliceByBaseName,
        foxSpadePuppetSlices,
        foxHeartPuppetSlices,
    )

    private var contentEpoch: Int = 0

    private val boardLayer = Container().addTo(rootContainer)
    private val pileSlotsContainer = Container().addTo(boardLayer)
    private val foundationSuitContainer = Container().addTo(boardLayer)
    private val wasteCardsLayer = Container().addTo(boardLayer)
    private val foundationCardsLayer = Container().addTo(boardLayer)
    private val tableauCardsLayer = Container().addTo(boardLayer)

    private val puppetHub = FaceCardPuppetAnimationHub()
    private var puppetHubAttached = false

    private val pileSlotById = linkedMapOf<String, SolidRect>()
    private var lastBoardLayoutViewportW: Double = 0.0
    private var lastBoardLayoutViewportH: Double = 0.0

    private var wasteBinding: CardSlotBinding? = null
    private val foundationBindings = arrayOfNulls<CardSlotBinding>(4)
    private val tableauBindings = Array(7) { mutableListOf<CardSlotBinding>() }

    private class CardSlotBinding {
        val root = Container()
        lateinit var cardRect: SolidRect
        var model: CardViewModel? = null
        var contentEpochSnapshot: Int = -1
        var lastAnimateFoxQueenSpade: Boolean = false
        var foxAnimator: FoxSpadePuppetCardAnimator? = null

        fun disposeFoxAnimator() {
            foxAnimator?.dispose()
            foxAnimator = null
        }

        fun clearVisuals() {
            disposeFoxAnimator()
            root.removeAllChildViews()
            model = null
            contentEpochSnapshot = -1
            lastAnimateFoxQueenSpade = false
        }
    }

    /** Optional TexturePacker atlas. Atlas slices take precedence over fallback suit/face assets. */
    fun setTexturePackerSlices(sliceByBaseName: Map<String, RectSlice<Bitmap>>?) {
        texturePackerSliceByBaseName = sliceByBaseName
        suitSymbolPainter = newSuitSymbolPainter()
        faceCardAnimalPainter = FaceCardAnimalPainter(sliceByBaseName, foxSpadePuppetSlices, foxHeartPuppetSlices)
        contentEpoch++
    }

    /** Sets optional debug suit pips; null keeps atlas/glyph fallback for that suit. */
    fun setSimpleSuitPipBitmaps(
        hearts: Bitmap?,
        diamonds: Bitmap?,
        spades: Bitmap?,
        clubs: Bitmap?,
    ) {
        heartSuitPipBitmap = hearts
        diamondSuitPipBitmap = diamonds
        spadeSuitPipBitmap = spades
        clubSuitPipBitmap = clubs
        suitSymbolPainter = newSuitSymbolPainter()
        contentEpoch++
    }

    /** Sets Q-spade fox slices used when packed face art is unavailable. */
    fun setFoxSpadePuppetSlices(slices: FoxPuppetSheetLayout.PuppetSlices?) {
        foxSpadePuppetSlices = slices
        faceCardAnimalPainter = FaceCardAnimalPainter(texturePackerSliceByBaseName, slices, foxHeartPuppetSlices)
        contentEpoch++
    }

    /** Sets Q-heart fox slices used when packed face art is unavailable. */
    fun setFoxHeartPuppetSlices(slices: FoxPuppetSheetLayout.PuppetSlices?) {
        foxHeartPuppetSlices = slices
        faceCardAnimalPainter = FaceCardAnimalPainter(texturePackerSliceByBaseName, foxSpadePuppetSlices, slices)
        contentEpoch++
    }

    private val overlayLayer = Container().addTo(rootContainer)
    private val dragLayer = Container().addTo(rootContainer).also { layer ->
        layer.mouseEnabled = false
    }
    /** Full-viewport modal layer kept above [dragLayer]. */
    private val modalOverlayLayer = Container().addTo(rootContainer).also { layer ->
        layer.visible = false
        layer.mouseEnabled = false
    }
    val modalOverlayRoot: Container get() = modalOverlayLayer
    private val statusText = overlayLayer.text(
        text = "",
        textSize = 24.0,
        color = Colors.WHITE,
    ) {
        x = 32.0
        y = 676.0
    }
    private val movesText = overlayLayer.text(
        text = "",
        textSize = 24.0,
        color = Colors.WHITE,
    ) {
        x = 940.0
        y = 676.0
    }
    private val stockCountText = overlayLayer.text(
        text = "",
        textSize = 18.0,
        color = Colors.WHITE,
    ) {
        x = 36.0
        y = 176.0
    }

    private val stockButton = overlayLayer.text("Draw", textSize = 24.0, color = Colors.WHITE) {
        x = 32.0
        y = 24.0
    }
    private val recycleButton = overlayLayer.text("Recycle", textSize = 24.0, color = Colors.WHITE) {
        x = 130.0
        y = 24.0
    }
    private val autoMoveButton = overlayLayer.text("Auto", textSize = 24.0, color = Colors.WHITE) {
        x = 260.0
        y = 24.0
    }
    private val undoButton = overlayLayer.text("Undo", textSize = 24.0, color = Colors.WHITE) {
        x = 350.0
        y = 24.0
    }
    private val redoButton = overlayLayer.text("Redo", textSize = 24.0, color = Colors.WHITE) {
        x = 450.0
        y = 24.0
    }
    private val newGameButton = overlayLayer.text("New game", textSize = 24.0, color = Colors.WHITE) {
        x = 550.0
        y = 24.0
    }

    init {
        positionHudOverlay()
    }

    private fun positionHudOverlay() {
        val w = viewportWidth
        val h = viewportHeight
        val primary = playfieldMetrics.hudPrimaryTextSize
        val secondary = playfieldMetrics.hudSecondaryTextSize
        val topBarY = h * (24.0 / 720.0)
        statusText.textSize = primary
        statusText.x = w * (32.0 / 1280.0)
        statusText.y = h * (676.0 / 720.0)
        movesText.textSize = primary
        movesText.x = w * (940.0 / 1280.0)
        movesText.y = h * (676.0 / 720.0)
        stockCountText.textSize = secondary
        stockCountText.x = w * (36.0 / 1280.0)
        stockCountText.y = h * (176.0 / 720.0)
        stockButton.textSize = primary
        stockButton.x = w * (32.0 / 1280.0)
        stockButton.y = topBarY
        recycleButton.textSize = primary
        recycleButton.x = w * (130.0 / 1280.0)
        recycleButton.y = topBarY
        autoMoveButton.textSize = primary
        autoMoveButton.x = w * (260.0 / 1280.0)
        autoMoveButton.y = topBarY
        undoButton.textSize = primary
        undoButton.x = w * (350.0 / 1280.0)
        undoButton.y = topBarY
        redoButton.textSize = primary
        redoButton.x = w * (450.0 / 1280.0)
        redoButton.y = topBarY
        newGameButton.textSize = primary
        newGameButton.x = w * (550.0 / 1280.0)
        newGameButton.y = topBarY
    }

    private var dragGhostView: Container? = null
    private var dragGhostContentSignature: String? = null
    private var hiddenDragSourcePileId: String? = null
    private var hiddenDragSourceCardCount: Int = 0

    /** Lays out piles/cards for [uiState], updates HUD text, and returns interaction targets. */
    fun render(
        uiState: SolitaireUiState,
        selectedPileId: String? = null,
    ): SolitaireRenderedBoard {
        clearDragPreviewGhost(retainSourceHidden = true)
        ensurePuppetHub()
        refreshTheme(uiState.cardTheme)
        val renderModel = requireNotNull(uiState.renderModel) {
            "Cannot render before game starts"
        }
        val boardLayout = BoardLayout.create(
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
        )
        if (viewportWidth != lastBoardLayoutViewportW || viewportHeight != lastBoardLayoutViewportH) {
            playfieldMetrics = SolitaireBoardPlayfieldMetrics.forViewport(viewportWidth, viewportHeight)
            positionHudOverlay()
            resetBoardChromeForViewportChange()
            lastBoardLayoutViewportW = viewportWidth
            lastBoardLayoutViewportH = viewportHeight
        }

        val pileTapTargets = linkedMapOf<String, SolidRect>()
        val pileHitRegions = linkedMapOf<String, PileHitRegion>()
        val draggableNeutrals = mutableListOf<DraggableCardInteractionTarget>()
        val draggableRects = mutableListOf<SolidRect>()
        var nextDragHandleId = 0

        val stockSlot = syncPileSlot(
            pileId = "stock",
            x = boardLayout.stockPile.x,
            y = boardLayout.stockPile.y,
            highlightColor = Colors["#3d6f3d"],
            isSelected = selectedPileId == "stock",
        )
        pileTapTargets["stock"] = stockSlot.first
        pileHitRegions["stock"] = stockSlot.second

        val wasteSlot = syncPileSlot(
            pileId = "waste",
            x = boardLayout.wastePile.x,
            y = boardLayout.wastePile.y,
            highlightColor = Colors["#3d6f3d"],
            isSelected = selectedPileId == "waste",
        )
        pileTapTargets["waste"] = wasteSlot.first
        pileHitRegions["waste"] = wasteSlot.second

        boardLayout.foundationPiles.forEachIndexed { index, pileLayout ->
            val suit = foundationSuitsVisualLeftToRight[index]
            val pileId = "foundation-${suit.name.lowercase()}"
            val foundationSlot = syncPileSlot(
                pileId = pileId,
                x = pileLayout.x,
                y = pileLayout.y,
                highlightColor = Colors["#3d4f6f"],
                isSelected = selectedPileId == pileId,
            )
            pileTapTargets[pileId] = foundationSlot.first
            pileHitRegions[pileId] = foundationSlot.second
        }

        boardLayout.tableauPiles.forEachIndexed { index, pileLayout ->
            val pileId = "tableau-$index"
            val tableauCardCount = renderModel.tableauPiles[index].cards.size
            val tableauHitHeight = calculateTableauHitHeight(
                cardHeight = cardHeight,
                tableauCardOffsetY = tableauCardOffsetY,
                cardCount = tableauCardCount,
            )
            val tableauSlot = syncPileSlot(
                pileId = pileId,
                x = pileLayout.x,
                y = pileLayout.y,
                highlightColor = Colors["#6f4a2a"],
                slotHeight = tableauHitHeight,
                isSelected = selectedPileId == pileId,
            )
            pileTapTargets[pileId] = tableauSlot.first
            pileHitRegions[pileId] = tableauSlot.second
        }

        syncFoundationSuitGlyphs(boardLayout)

        drawStockCount(renderModel)
        syncWasteCard(
            renderModel = renderModel,
            x = boardLayout.wastePile.x,
            y = boardLayout.wastePile.y,
            draggableNeutrals = draggableNeutrals,
            draggableRects = draggableRects,
            nextDragHandleId = { nextDragHandleId++ },
        )
        syncFoundationCards(
            renderModel = renderModel,
            boardLayout = boardLayout,
            draggableNeutrals = draggableNeutrals,
            draggableRects = draggableRects,
            nextDragHandleId = { nextDragHandleId++ },
        )
        syncTableauCards(
            renderModel = renderModel,
            boardLayout = boardLayout,
            draggableNeutrals = draggableNeutrals,
            draggableRects = draggableRects,
            nextDragHandleId = { nextDragHandleId++ },
        )
        clearDragPreviewGhost(retainSourceHidden = false)
        updateHud(uiState, renderModel)

        restoreRootLayerOrderAfterBoardRebuild()

        val pileTapBoundsByPileId = pileHitRegions.mapValues { (_, region) -> region.bounds }
        val interaction = SolitaireBoardInteractionSnapshot(
            pileTapBoundsByPileId = pileTapBoundsByPileId,
            pileHitRegionsByPileId = pileHitRegions,
            draggableTopCards = draggableNeutrals,
        )
        return SolitaireRenderedBoard(
            interaction = interaction,
            pileTapTargets = pileTapTargets,
            draggableTopCardRects = draggableRects,
            stockButton = stockButton,
            recycleButton = recycleButton,
            autoMoveButton = autoMoveButton,
            undoButton = undoButton,
            redoButton = redoButton,
            newGameButton = newGameButton,
        )
    }

    fun setModalOverlayVisible(visible: Boolean) {
        modalOverlayLayer.visible = visible
        modalOverlayLayer.mouseEnabled = visible
    }

    fun isModalOverlayVisible(): Boolean = modalOverlayLayer.visible

    private fun resetBoardChromeForViewportChange() {
        pileSlotById.clear()
        pileSlotsContainer.removeAllChildViews()
        foundationSuitContainer.removeAllChildViews()
        releaseWasteCard()
        foundationBindings.indices.forEach { index ->
            foundationBindings[index]?.let { b ->
                b.clearVisuals()
                foundationCardsLayer.removeChild(b.root)
                foundationBindings[index] = null
            }
        }
        tableauBindings.forEachIndexed { _, column ->
            while (column.isNotEmpty()) {
                val removed = column.removeAt(column.lastIndex)
                removed.clearVisuals()
                tableauCardsLayer.removeChild(removed.root)
            }
        }
        contentEpoch++
    }

    private fun refreshTheme(cardTheme: CardTheme) {
        if (activeCardTheme == cardTheme) return
        activeCardTheme = cardTheme
        activeThemeSpec = cardThemeSpec(cardTheme)
        suitSymbolPainter = newSuitSymbolPainter()
        faceCardAnimalPainter = FaceCardAnimalPainter(texturePackerSliceByBaseName, foxSpadePuppetSlices, foxHeartPuppetSlices)
        contentEpoch++
        println("[$THEME_RENDERER_LOG_TAG] switchedTheme=$cardTheme")
    }

    private fun newSuitSymbolPainter() = SuitSymbolPainter(
        activeThemeSpec,
        texturePackerSliceByBaseName,
        heartSuitPipBitmap,
        diamondSuitPipBitmap,
        spadeSuitPipBitmap,
        clubSuitPipBitmap,
    )

    private fun ensurePuppetHub() {
        if (puppetHubAttached) return
        puppetHub.attachToBoard(boardLayer)
        puppetHubAttached = true
    }

    private fun syncPileSlot(
        pileId: String,
        x: Double,
        y: Double,
        highlightColor: RGBA,
        slotHeight: Double = cardHeight,
        isSelected: Boolean = false,
    ): Pair<SolidRect, PileHitRegion> {
        val slotFillColor = if (isSelected) {
            highlightColor.withAd(0.72)
        } else {
            highlightColor.withAd(0.35)
        }
        val slot = pileSlotById.getOrPut(pileId) {
            pileSlotsContainer.solidRect(
                width = cardWidth,
                height = slotHeight,
                color = slotFillColor,
            ) {
                name = pileId
            }
        }
        slot.x = x
        slot.y = y
        slot.width = cardWidth
        slot.height = slotHeight
        slot.color = slotFillColor
        val hitRegion = PileHitRegion(
            pileId = pileId,
            bounds = AxisAlignedRect(
                x = x,
                y = y,
                width = cardWidth,
                height = slotHeight,
            ),
        )
        return slot to hitRegion
    }

    private fun syncFoundationSuitGlyphs(boardLayout: SolitaireBoardLayout) {
        while (foundationSuitContainer.numChildren < 4) {
            Container().addTo(foundationSuitContainer)
        }
        foundationSuitsVisualLeftToRight.forEachIndexed { index, suit ->
            val pileLayout = boardLayout.foundationPiles[index]
            val holder = foundationSuitContainer.getChildAt(index) as Container
            holder.removeAllChildViews()
            val foundationGlyphScale = playfieldMetrics.uniformScale
            suitSymbolPainter.draw(
                parentContainer = holder,
                suit = suit,
                x = pileLayout.x + 30.0 * foundationGlyphScale,
                y = pileLayout.y + 39.0 * foundationGlyphScale,
                symbolWidth = 36.0 * foundationGlyphScale,
                symbolHeight = 34.0 * foundationGlyphScale,
            )
        }
    }

    private fun releaseWasteCard() {
        val b = wasteBinding ?: return
        b.clearVisuals()
        wasteCardsLayer.removeChild(b.root)
        wasteBinding = null
    }

    private fun syncWasteCard(
        renderModel: GameRenderModel,
        x: Double,
        y: Double,
        draggableNeutrals: MutableList<DraggableCardInteractionTarget>,
        draggableRects: MutableList<SolidRect>,
        nextDragHandleId: () -> Int,
    ) {
        val topWasteCard = renderModel.wastePile.cards.lastOrNull()
        if (topWasteCard == null) {
            releaseWasteCard()
            return
        }
        val binding = wasteBinding ?: CardSlotBinding().also {
            wasteBinding = it
            it.root.addTo(wasteCardsLayer)
        }
        syncCardSlot(
            binding = binding,
            card = topWasteCard,
            x = x,
            y = y,
            isInteractive = true,
            slotKey = "waste",
            animateFoxQueenSpade = true,
        )
        val cardRect = binding.cardRect
        draggableRects += cardRect
        draggableNeutrals += DraggableCardInteractionTarget(
            handleId = nextDragHandleId(),
            pileId = "waste",
            card = topWasteCard,
            cardCount = 1,
            stackCards = listOf(topWasteCard),
            topCardBounds = AxisAlignedRect(cardRect.x, cardRect.y, cardRect.width, cardRect.height),
        )
    }

    private fun syncFoundationCards(
        renderModel: GameRenderModel,
        boardLayout: SolitaireBoardLayout,
        draggableNeutrals: MutableList<DraggableCardInteractionTarget>,
        draggableRects: MutableList<SolidRect>,
        nextDragHandleId: () -> Int,
    ) {
        val foundationPileById = renderModel.foundationPiles.associateBy { it.pileId }
        foundationSuitsVisualLeftToRight.forEachIndexed { index, suit ->
            val pileId = "foundation-${suit.name.lowercase()}"
            val pileViewModel = foundationPileById[pileId]
            val topCard = pileViewModel?.cards?.lastOrNull()
            val pileLayout = boardLayout.foundationPiles[index]
            if (topCard == null) {
                foundationBindings[index]?.let { b ->
                    b.clearVisuals()
                    foundationCardsLayer.removeChild(b.root)
                    foundationBindings[index] = null
                }
                return@forEachIndexed
            }
            val binding = foundationBindings[index] ?: CardSlotBinding().also {
                foundationBindings[index] = it
                it.root.addTo(foundationCardsLayer)
            }
            syncCardSlot(
                binding = binding,
                card = topCard,
                x = pileLayout.x,
                y = pileLayout.y,
                isInteractive = true,
                slotKey = "foundation-$index",
                animateFoxQueenSpade = true,
            )
            val topRect = binding.cardRect
            draggableRects += topRect
            draggableNeutrals += DraggableCardInteractionTarget(
                handleId = nextDragHandleId(),
                pileId = pileId,
                card = topCard,
                cardCount = 1,
                stackCards = listOf(topCard),
                topCardBounds = AxisAlignedRect(topRect.x, topRect.y, topRect.width, topRect.height),
            )
        }
    }

    private fun syncTableauCards(
        renderModel: GameRenderModel,
        boardLayout: SolitaireBoardLayout,
        draggableNeutrals: MutableList<DraggableCardInteractionTarget>,
        draggableRects: MutableList<SolidRect>,
        nextDragHandleId: () -> Int,
    ) {
        renderModel.tableauPiles.forEachIndexed { columnIndex, pileViewModel ->
            val pileLayout = boardLayout.tableauPiles[columnIndex]
            val bindings = tableauBindings[columnIndex]
            val cards = pileViewModel.cards
            while (bindings.size > cards.size) {
                val removed = bindings.removeAt(bindings.lastIndex)
                removed.clearVisuals()
                tableauCardsLayer.removeChild(removed.root)
            }
            while (bindings.size < cards.size) {
                val nb = CardSlotBinding()
                nb.root.addTo(tableauCardsLayer)
                bindings.add(nb)
            }
            cards.forEachIndexed { cardIndex, card ->
                val binding = bindings[cardIndex]
                val isVisibleTableauCard = !isHiddenCard(card)
                val isTopOfColumn = cardIndex == cards.lastIndex
                syncCardSlot(
                    binding = binding,
                    card = card,
                    x = pileLayout.x,
                    y = pileLayout.y + (cardIndex * tableauCardOffsetY),
                    isInteractive = isVisibleTableauCard,
                    slotKey = "tableau-$columnIndex-$cardIndex",
                    animateFoxQueenSpade = isTopOfColumn,
                )
                if (isVisibleTableauCard) {
                    val cardCountFromSelection = cards.size - cardIndex
                    val stackCards = cards.subList(cardIndex, cards.size)
                    val topRect = binding.cardRect
                    draggableRects += topRect
                    draggableNeutrals += DraggableCardInteractionTarget(
                        handleId = nextDragHandleId(),
                        pileId = pileViewModel.pileId,
                        card = card,
                        cardCount = cardCountFromSelection,
                        stackCards = stackCards,
                        topCardBounds = AxisAlignedRect(topRect.x, topRect.y, topRect.width, topRect.height),
                    )
                }
            }
        }
    }

    private fun syncCardSlot(
        binding: CardSlotBinding,
        card: CardViewModel,
        x: Double,
        y: Double,
        isInteractive: Boolean,
        slotKey: String,
        animateFoxQueenSpade: Boolean,
    ) {
        binding.root.x = x
        binding.root.y = y
        if (binding.model == card &&
            binding.contentEpochSnapshot == contentEpoch &&
            binding.lastAnimateFoxQueenSpade == animateFoxQueenSpade
        ) {
            binding.cardRect.mouseEnabled = isInteractive
            return
        }
        binding.clearVisuals()
        binding.contentEpochSnapshot = contentEpoch
        binding.model = card
        binding.lastAnimateFoxQueenSpade = animateFoxQueenSpade
        binding.cardRect = drawCardContent(
            parentContainer = binding.root,
            card = card,
            isInteractive = isInteractive,
            enableAnimatedFaceMotif = true,
            allowFaceMotifBleed = animateFoxQueenSpade,
            puppetDrawContext = FaceCardPuppetDrawContext(
                hub = puppetHub,
                slotKey = slotKey,
                animateFoxQueenSpade = animateFoxQueenSpade,
            ),
            binding = binding,
        )
    }

    private fun drawStockCount(renderModel: GameRenderModel) {
        stockCountText.text = "Stock: ${renderModel.stockPileCount}"
    }

    private fun drawCardContent(
        parentContainer: Container,
        card: CardViewModel,
        isInteractive: Boolean,
        enableAnimatedFaceMotif: Boolean,
        allowFaceMotifBleed: Boolean,
        puppetDrawContext: FaceCardPuppetDrawContext?,
        binding: CardSlotBinding?,
    ): SolidRect {
        val cornerScale = playfieldMetrics.uniformScale
        parentContainer.solidRect(
            width = cardWidth,
            height = cardHeight,
            color = Colors.BLACK.withAd(activeThemeSpec.shadowAlpha),
        ) {
            this.x = activeThemeSpec.shadowOffset * cornerScale
            this.y = activeThemeSpec.shadowOffset * cornerScale
            mouseEnabled = false
        }
        val isCardHidden = isHiddenCard(card)
        val cardRect = parentContainer.solidRect(
            width = cardWidth,
            height = cardHeight,
            color = if (isCardHidden) activeThemeSpec.hiddenCardFillColor else activeThemeSpec.cardFrontColor,
        ) {
            mouseEnabled = isInteractive
        }
        drawCardBorder(parentContainer, cornerScale)
        if (isCardHidden) {
            drawHiddenCardPattern(parentContainer)
            return cardRect
        }
        val visibleRank = (card.face as CardFace.Up).rank
        val rankColor = if (card.usesRedSuitInk()) activeThemeSpec.redSuitColor else activeThemeSpec.blackSuitColor
        parentContainer.text(
            text = card.rankAbbrev(),
            textSize = activeThemeSpec.rankTextSize * cornerScale,
            color = rankColor,
        ) {
            x = 8.0 * cornerScale
            y = 6.0 * cornerScale
            mouseEnabled = false
        }
        suitSymbolPainter.draw(
            parentContainer = parentContainer,
            suit = card.suit,
            x = 8.0 * cornerScale,
            y = 29.0 * cornerScale,
            symbolWidth = 26.0 * cornerScale,
            symbolHeight = 22.0 * cornerScale,
        )
        suitSymbolPainter.draw(
            parentContainer = parentContainer,
            suit = card.suit,
            x = cardWidth - 34.0 * cornerScale,
            y = cardHeight - 27.0 * cornerScale,
            symbolWidth = 26.0 * cornerScale,
            symbolHeight = 22.0 * cornerScale,
        )
        val faceCardAnimal = faceCardAnimalForRank(visibleRank)
        val useLargeFaceMotifSlot =
            texturePackerSliceByBaseName != null ||
                foxSpadePuppetSlices != null ||
                foxHeartPuppetSlices != null
        val baseFaceMotifWidth = if (useLargeFaceMotifSlot) {
            SolitaireBoardFaceCardMetrics.largeFaceMotifWidth * cornerScale
        } else {
            62.0 * cornerScale
        }
        val baseFaceMotifHeight = if (useLargeFaceMotifSlot) {
            SolitaireBoardFaceCardMetrics.largeFaceMotifHeight * cornerScale
        } else {
            74.0 * cornerScale
        }
        val bleedScale =
            if (allowFaceMotifBleed && faceCardAnimal != FaceCardAnimal.NONE) {
                SolitaireBoardFaceCardMetrics.faceMotifBleedScale
            } else {
                1.0
            }
        val faceMotifWidth = baseFaceMotifWidth * bleedScale
        val faceMotifHeight = baseFaceMotifHeight * bleedScale
        val centerMotif: View? = if (faceCardAnimal == FaceCardAnimal.NONE) {
            NumberCardPipLayout.draw(
                parentContainer = parentContainer,
                cardWidth = cardWidth,
                cardHeight = cardHeight,
                rank = visibleRank,
                suit = card.suit,
                suitSymbolPainter = suitSymbolPainter,
            )
            null
        } else {
            val paintResult = faceCardAnimalPainter.draw(
                parentContainer = parentContainer,
                rank = visibleRank,
                suit = card.suit,
                x = (cardWidth - faceMotifWidth) / 2.0,
                y = (cardHeight - faceMotifHeight) / 2.0,
                width = faceMotifWidth,
                height = faceMotifHeight,
                suitAccentColor = rankColor,
                puppetDrawContext = puppetDrawContext,
            )
            binding?.foxAnimator = paintResult.foxAnimator
            paintResult.motifRoot
        }
        if (enableAnimatedFaceMotif && centerMotif != null && faceCardAnimal != FaceCardAnimal.NONE) {
            centerMotif.y -= activeThemeSpec.faceCardIdleBobDistance * cornerScale
        }
        return cardRect
    }

    private fun drawCardBorder(parentContainer: Container, cornerScale: Double) {
        val borderWidth = activeThemeSpec.borderWidth * cornerScale
        parentContainer.solidRect(
            width = cardWidth,
            height = borderWidth,
            color = activeThemeSpec.cardBorderColor,
        ) { mouseEnabled = false }
        parentContainer.solidRect(
            width = cardWidth,
            height = borderWidth,
            color = activeThemeSpec.cardBorderColor,
        ) {
            y = cardHeight - borderWidth
            mouseEnabled = false
        }
        parentContainer.solidRect(
            width = borderWidth,
            height = cardHeight,
            color = activeThemeSpec.cardBorderColor,
        ) { mouseEnabled = false }
        parentContainer.solidRect(
            width = borderWidth,
            height = cardHeight,
            color = activeThemeSpec.cardBorderColor,
        ) {
            x = cardWidth - borderWidth
            mouseEnabled = false
        }
    }

    private fun drawHiddenCardPattern(parentContainer: Container) {
        val patternScale = playfieldMetrics.uniformScale
        val stripeWidth = 12.0 * patternScale
        repeat(8) { stripeIndex ->
            parentContainer.solidRect(
                width = stripeWidth,
                height = 8.0 * patternScale,
                color = activeThemeSpec.hiddenCardAccentColor.withAd(0.65),
            ) {
                x = 6.0 * patternScale + (stripeIndex * 11.0 * patternScale)
                y = 16.0 * patternScale + (stripeIndex % 2) * 16.0 * patternScale
                mouseEnabled = false
            }
        }
    }

    private fun restoreRootLayerOrderAfterBoardRebuild() {
        overlayLayer.parent?.removeChild(overlayLayer)
        overlayLayer.addTo(rootContainer)
        dragLayer.parent?.removeChild(dragLayer)
        dragLayer.addTo(rootContainer)
        modalOverlayLayer.parent?.removeChild(modalOverlayLayer)
        modalOverlayLayer.addTo(rootContainer)
    }

    private fun updateHud(
        uiState: SolitaireUiState,
        renderModel: GameRenderModel,
    ) {
        val baseStatus = if (uiState.hasWon) {
            "You won"
        } else {
            uiState.statusMessage
        }
        statusText.text = "[PlayableV1Hud] $baseStatus"
        statusText.color = if (uiState.wasLastMoveAccepted) Colors.WHITE else Colors["#ff9d9d"]
        movesText.text = "Moves: ${renderModel.moveCounter}"
    }

    fun renderDragPreview(preview: BoardDragPreview?) {
        if (
            preview?.sourcePileId != hiddenDragSourcePileId ||
            preview?.sourceCardCount != hiddenDragSourceCardCount
        ) {
            setDragSourceHidden(
                pileId = hiddenDragSourcePileId,
                cardCount = hiddenDragSourceCardCount,
                hidden = false,
            )
            if (preview != null) {
                setDragSourceHidden(
                    pileId = preview.sourcePileId,
                    cardCount = preview.sourceCardCount,
                    hidden = true,
                )
                hiddenDragSourcePileId = preview.sourcePileId
                hiddenDragSourceCardCount = preview.sourceCardCount
            } else {
                hiddenDragSourcePileId = null
                hiddenDragSourceCardCount = 0
            }
        }
        if (preview == null) {
            clearDragPreviewGhost(retainSourceHidden = false)
            return
        }
        val nextSignature = stackSignature(preview.stackCards)
        if (dragGhostView == null || dragGhostContentSignature != nextSignature) {
            dragGhostView?.parent?.removeChild(dragGhostView!!)
            dragGhostView = createDragGhostStack(preview.stackCards)
            dragGhostContentSignature = nextSignature
        }
        val ghost = dragGhostView!!
        ghost.x = preview.x - cardWidth / 2.0
        ghost.y = preview.y - cardHeight / 2.0
    }

    fun clearDragPreviewGhost(retainSourceHidden: Boolean) {
        val existingGhost = dragGhostView
        if (existingGhost != null) {
            existingGhost.parent?.removeChild(existingGhost)
        }
        dragGhostView = null
        dragGhostContentSignature = null
        if (retainSourceHidden) return
        setDragSourceHidden(
            pileId = hiddenDragSourcePileId,
            cardCount = hiddenDragSourceCardCount,
            hidden = false,
        )
        hiddenDragSourcePileId = null
        hiddenDragSourceCardCount = 0
    }

    private fun setDragSourceHidden(
        pileId: String?,
        cardCount: Int,
        hidden: Boolean,
    ) {
        if (pileId == null || cardCount <= 0) return
        when {
            pileId == "waste" -> {
                wasteBinding?.root?.visible = !hidden
            }
            pileId.startsWith("foundation-") -> {
                val suitToken = pileId.removePrefix("foundation-")
                val suitIndex = foundationSuitsVisualLeftToRight.indexOfFirst { it.name.lowercase() == suitToken }
                if (suitIndex >= 0) {
                    foundationBindings[suitIndex]?.root?.visible = !hidden
                }
            }
            pileId.startsWith("tableau-") -> {
                val columnIndex = pileId.removePrefix("tableau-").toIntOrNull() ?: return
                val columnBindings = tableauBindings.getOrNull(columnIndex) ?: return
                val startIndex = (columnBindings.size - cardCount).coerceAtLeast(0)
                for (bindingIndex in startIndex until columnBindings.size) {
                    columnBindings[bindingIndex].root.visible = !hidden
                }
            }
        }
    }

    suspend fun animateCardTravel(
        card: CardViewModel,
        startX: Double,
        startY: Double,
        endX: Double,
        endY: Double,
        isMoveAccepted: Boolean,
    ) {
        val animatedGhost = createDragGhostStack(listOf(card))
        animatedGhost.x = startX - cardWidth / 2.0
        animatedGhost.y = startY - cardHeight / 2.0
        val moveAnimationDuration = if (isMoveAccepted) 0.14.seconds else 0.11.seconds
        animatedGhost.tween(
            animatedGhost::x[endX],
            animatedGhost::y[endY],
            time = moveAnimationDuration,
        )
        animatedGhost.parent?.removeChild(animatedGhost)
    }

    private fun stackSignature(cards: List<CardViewModel>): String =
        cards.joinToString("|") { card ->
            val rankToken = when (val f = card.face) {
                is CardFace.Up -> f.rank.name
                CardFace.Down -> "DOWN"
            }
            "${rankToken}_${card.suit}"
        }

    private fun createDragGhostStack(cards: List<CardViewModel>): Container {
        return Container().addTo(dragLayer).also { ghostContainer ->
            ghostContainer.mouseEnabled = false
            ghostContainer.alpha = 0.86
            cards.forEachIndexed { stackIndex, card ->
                val cardLayer = Container().addTo(ghostContainer).also { layer ->
                    layer.y = stackIndex * tableauCardOffsetY
                }
                drawCardContent(
                    parentContainer = cardLayer,
                    card = card,
                    isInteractive = false,
                    enableAnimatedFaceMotif = false,
                    allowFaceMotifBleed = stackIndex == 0,
                    puppetDrawContext = null,
                    binding = null,
                )
            }
        }
    }
}
