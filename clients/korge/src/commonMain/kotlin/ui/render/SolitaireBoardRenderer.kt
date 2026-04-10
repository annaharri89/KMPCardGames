package ui.render

import app.CardTheme
import app.SolitaireUiState
import app.cardThemeSpec
import domain.model.CardColor
import domain.model.Rank
import domain.model.Suit
import domain.readmodel.CardFace
import domain.readmodel.CardViewModel
import domain.readmodel.GameRenderModel
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
import ui.layout.BoardLayout
import kotlin.math.max

private fun Container.removeAllChildViews() {
    while (numChildren > 0) {
        removeChildAt(numChildren - 1)
    }
}

internal fun calculateTableauHitHeight(
    cardHeight: Double,
    tableauCardOffsetY: Double,
    cardCount: Int,
): Double {
    return cardHeight + (max(0, cardCount - 1) * tableauCardOffsetY)
}

internal fun isHiddenCard(cardViewModel: CardViewModel): Boolean =
    cardViewModel.face is CardFace.Down

data class SolitaireRenderedBoard(
    val pileTapTargets: Map<String, SolidRect>,
    val pileHitAreas: Map<String, PileHitArea>,
    val draggableCardTargets: List<DraggableCardTarget>,
    val stockButton: Text,
    val recycleButton: Text,
    val autoMoveButton: Text,
    val undoButton: Text,
    val redoButton: Text,
)

data class PileHitArea(
    val pileId: String,
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
)

data class DraggableCardTarget(
    val pileId: String,
    val card: CardViewModel,
    val cardCount: Int,
    /** Cards moved with this drag, top-most first (matches tableau order from grab point through pile bottom). */
    val stackCards: List<CardViewModel>,
    val cardView: SolidRect,
)

/** [x]/[y] are stage coordinates of the pointer; the drag ghost is drawn centered on the top card of [stackCards]. */
data class BoardDragPreview(
    val stackCards: List<CardViewModel>,
    val x: Double,
    val y: Double,
) {
    val card: CardViewModel get() = stackCards.first()
}

/**
 * Builds the solitaire board under [rootContainer] using a board layer, HUD overlay, and drag overlay.
 * [boardLayer] and card views are **persistent**; [render] updates pile slots and syncs card content when
 * models change so face-card puppet [addUpdater] animation survives moves. Callers still re-bind input
 * with each returned [SolitaireRenderedBoard].
 */
class SolitaireBoardRenderer(
    private val rootContainer: Container,
    private val viewportWidth: Double = 1280.0,
    private val viewportHeight: Double = 720.0,
) {
    companion object {
        private const val THEME_RENDERER_LOG_TAG = "KawaiiThemeRenderer"
    }

    private val cardWidth = SolitaireBoardFaceCardMetrics.cardWidth
    private val cardHeight = SolitaireBoardFaceCardMetrics.cardHeight
    private val tableauCardOffsetY = 24.0

    private var activeCardTheme: CardTheme = CardTheme.KAWAII_NATURE
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

    /**
     * Optional TexturePacker atlas for suit symbols and face-card bitmaps. When a slice exists it wins over
     * hand-authored assets (e.g. [setFoxSpadePuppetSlices] / [setFoxHeartPuppetSlices] for queen fox puppets).
     */
    fun setTexturePackerSlices(sliceByBaseName: Map<String, RectSlice<Bitmap>>?) {
        texturePackerSliceByBaseName = sliceByBaseName
        suitSymbolPainter = newSuitSymbolPainter()
        faceCardAnimalPainter = FaceCardAnimalPainter(sliceByBaseName, foxSpadePuppetSlices, foxHeartPuppetSlices)
        contentEpoch++
    }

    /**
     * Flat debug pips (621×586); uniform scale + center in [SuitSymbolPainter]. Null keeps atlas / glyph fallback for that suit.
     */
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

    /** Slices from [debug/fox_spade_puppet_sheet.png]; used for Queen of Spades when no packed face texture is present. */
    fun setFoxSpadePuppetSlices(slices: FoxPuppetSheetLayout.PuppetSlices?) {
        foxSpadePuppetSlices = slices
        faceCardAnimalPainter = FaceCardAnimalPainter(texturePackerSliceByBaseName, slices, foxHeartPuppetSlices)
        contentEpoch++
    }

    /** Slices from [debug/fox_heart_puppet_sheet.png]; used for Queen of Hearts when no packed face texture is present. */
    fun setFoxHeartPuppetSlices(slices: FoxPuppetSheetLayout.PuppetSlices?) {
        foxHeartPuppetSlices = slices
        faceCardAnimalPainter = FaceCardAnimalPainter(texturePackerSliceByBaseName, foxSpadePuppetSlices, slices)
        contentEpoch++
    }

    private val overlayLayer = Container().addTo(rootContainer)
    private val dragLayer = Container().addTo(rootContainer).also { layer ->
        layer.mouseEnabled = false
    }
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
    private var dragGhostView: Container? = null
    private var dragGhostContentSignature: String? = null

    /** Lays out piles and cards from [uiState], updates HUD text, and returns targets for [ui.input.SolitaireInputController]. */
    fun render(
        uiState: SolitaireUiState,
        selectedPileId: String? = null,
    ): SolitaireRenderedBoard {
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
            resetBoardChromeForViewportChange()
            lastBoardLayoutViewportW = viewportWidth
            lastBoardLayoutViewportH = viewportHeight
        }

        val pileTapTargets = linkedMapOf<String, SolidRect>()
        val pileHitAreas = linkedMapOf<String, PileHitArea>()
        val draggableCardTargets = mutableListOf<DraggableCardTarget>()

        val stockSlot = syncPileSlot(
            pileId = "stock",
            x = boardLayout.stockPile.x,
            y = boardLayout.stockPile.y,
            highlightColor = Colors["#3d6f3d"],
            isSelected = selectedPileId == "stock",
        )
        pileTapTargets["stock"] = stockSlot.first
        pileHitAreas["stock"] = stockSlot.second

        val wasteSlot = syncPileSlot(
            pileId = "waste",
            x = boardLayout.wastePile.x,
            y = boardLayout.wastePile.y,
            highlightColor = Colors["#3d6f3d"],
            isSelected = selectedPileId == "waste",
        )
        pileTapTargets["waste"] = wasteSlot.first
        pileHitAreas["waste"] = wasteSlot.second

        boardLayout.foundationPiles.forEachIndexed { index, pileLayout ->
            val suit = Suit.entries[index]
            val pileId = "foundation-${suit.name.lowercase()}"
            val foundationSlot = syncPileSlot(
                pileId = pileId,
                x = pileLayout.x,
                y = pileLayout.y,
                highlightColor = Colors["#3d4f6f"],
                isSelected = selectedPileId == pileId,
            )
            pileTapTargets[pileId] = foundationSlot.first
            pileHitAreas[pileId] = foundationSlot.second
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
            pileHitAreas[pileId] = tableauSlot.second
        }

        syncFoundationSuitGlyphs(boardLayout)

        drawStockCount(renderModel)
        syncWasteCard(
            renderModel = renderModel,
            x = boardLayout.wastePile.x,
            y = boardLayout.wastePile.y,
            draggableCardTargets = draggableCardTargets,
        )
        syncFoundationCards(
            renderModel = renderModel,
            boardLayout = boardLayout,
            draggableCardTargets = draggableCardTargets,
        )
        syncTableauCards(
            renderModel = renderModel,
            boardLayout = boardLayout,
            draggableCardTargets = draggableCardTargets,
        )
        updateHud(uiState, renderModel)

        restoreRootLayerOrderAfterBoardRebuild()

        return SolitaireRenderedBoard(
            pileTapTargets = pileTapTargets,
            pileHitAreas = pileHitAreas,
            draggableCardTargets = draggableCardTargets,
            stockButton = stockButton,
            recycleButton = recycleButton,
            autoMoveButton = autoMoveButton,
            undoButton = undoButton,
            redoButton = redoButton,
        )
    }

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
    ): Pair<SolidRect, PileHitArea> {
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
        val hitArea = PileHitArea(
            pileId = pileId,
            x = x,
            y = y,
            width = cardWidth,
            height = slotHeight,
        )
        return slot to hitArea
    }

    private fun syncFoundationSuitGlyphs(boardLayout: ui.layout.SolitaireBoardLayout) {
        while (foundationSuitContainer.numChildren < 4) {
            Container().addTo(foundationSuitContainer)
        }
        Suit.entries.forEachIndexed { index, suit ->
            val pileLayout = boardLayout.foundationPiles[index]
            val holder = foundationSuitContainer.getChildAt(index) as Container
            holder.removeAllChildViews()
            suitSymbolPainter.draw(
                parentContainer = holder,
                suit = suit,
                x = pileLayout.x + 31.0,
                y = pileLayout.y + 40.0,
                symbolWidth = 34.0,
                symbolHeight = 32.0,
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
        draggableCardTargets: MutableList<DraggableCardTarget>,
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
        draggableCardTargets += DraggableCardTarget(
            pileId = "waste",
            card = topWasteCard,
            cardCount = 1,
            stackCards = listOf(topWasteCard),
            cardView = binding.cardRect,
        )
    }

    private fun syncFoundationCards(
        renderModel: GameRenderModel,
        boardLayout: ui.layout.SolitaireBoardLayout,
        draggableCardTargets: MutableList<DraggableCardTarget>,
    ) {
        renderModel.foundationPiles.forEachIndexed { index, pileViewModel ->
            val topCard = pileViewModel.cards.lastOrNull()
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
            draggableCardTargets += DraggableCardTarget(
                pileId = pileViewModel.pileId,
                card = topCard,
                cardCount = 1,
                stackCards = listOf(topCard),
                cardView = binding.cardRect,
            )
        }
    }

    private fun syncTableauCards(
        renderModel: GameRenderModel,
        boardLayout: ui.layout.SolitaireBoardLayout,
        draggableCardTargets: MutableList<DraggableCardTarget>,
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
                    draggableCardTargets += DraggableCardTarget(
                        pileId = pileViewModel.pileId,
                        card = card,
                        cardCount = cardCountFromSelection,
                        stackCards = stackCards,
                        cardView = binding.cardRect,
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
        parentContainer.solidRect(
            width = cardWidth,
            height = cardHeight,
            color = Colors.BLACK.withAd(activeThemeSpec.shadowAlpha),
        ) {
            this.x = activeThemeSpec.shadowOffset
            this.y = activeThemeSpec.shadowOffset
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
        drawCardBorder(parentContainer)
        if (isCardHidden) {
            drawHiddenCardPattern(parentContainer)
            return cardRect
        }
        val visibleRank = (card.face as CardFace.Up).rank
        val rankColor = if (card.color == CardColor.RED) activeThemeSpec.redSuitColor else activeThemeSpec.blackSuitColor
        parentContainer.text(
            text = rankShortLabel(visibleRank),
            textSize = activeThemeSpec.rankTextSize,
            color = rankColor,
        ) {
            x = 8.0
            y = 6.0
            mouseEnabled = false
        }
        suitSymbolPainter.draw(
            parentContainer = parentContainer,
            suit = card.suit,
            x = 9.0,
            y = 30.0,
            symbolWidth = 22.0,
            symbolHeight = 18.0,
        )
        suitSymbolPainter.draw(
            parentContainer = parentContainer,
            suit = card.suit,
            x = cardWidth - 31.0,
            y = cardHeight - 26.0,
            symbolWidth = 22.0,
            symbolHeight = 18.0,
        )
        val faceCardAnimal = faceCardAnimalForRank(visibleRank)
        val useLargeFaceMotifSlot =
            texturePackerSliceByBaseName != null ||
                foxSpadePuppetSlices != null ||
                foxHeartPuppetSlices != null
        val baseFaceMotifWidth = if (useLargeFaceMotifSlot) {
            SolitaireBoardFaceCardMetrics.largeFaceMotifWidth
        } else {
            62.0
        }
        val baseFaceMotifHeight = if (useLargeFaceMotifSlot) {
            SolitaireBoardFaceCardMetrics.largeFaceMotifHeight
        } else {
            74.0
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
            centerMotif.y -= activeThemeSpec.faceCardIdleBobDistance
        }
        return cardRect
    }

    private fun drawCardBorder(parentContainer: Container) {
        parentContainer.solidRect(
            width = cardWidth,
            height = activeThemeSpec.borderWidth,
            color = activeThemeSpec.cardBorderColor,
        ) { mouseEnabled = false }
        parentContainer.solidRect(
            width = cardWidth,
            height = activeThemeSpec.borderWidth,
            color = activeThemeSpec.cardBorderColor,
        ) {
            y = cardHeight - activeThemeSpec.borderWidth
            mouseEnabled = false
        }
        parentContainer.solidRect(
            width = activeThemeSpec.borderWidth,
            height = cardHeight,
            color = activeThemeSpec.cardBorderColor,
        ) { mouseEnabled = false }
        parentContainer.solidRect(
            width = activeThemeSpec.borderWidth,
            height = cardHeight,
            color = activeThemeSpec.cardBorderColor,
        ) {
            x = cardWidth - activeThemeSpec.borderWidth
            mouseEnabled = false
        }
    }

    private fun drawHiddenCardPattern(parentContainer: Container) {
        val stripeWidth = 12.0
        repeat(8) { stripeIndex ->
            parentContainer.solidRect(
                width = stripeWidth,
                height = 8.0,
                color = activeThemeSpec.hiddenCardAccentColor.withAd(0.65),
            ) {
                x = 6.0 + (stripeIndex * 11.0)
                y = 16.0 + (stripeIndex % 2) * 16.0
                mouseEnabled = false
            }
        }
    }

    private fun rankShortLabel(rank: Rank): String = when (rank) {
        Rank.ACE -> "A"
        Rank.TWO -> "2"
        Rank.THREE -> "3"
        Rank.FOUR -> "4"
        Rank.FIVE -> "5"
        Rank.SIX -> "6"
        Rank.SEVEN -> "7"
        Rank.EIGHT -> "8"
        Rank.NINE -> "9"
        Rank.TEN -> "10"
        Rank.JACK -> "J"
        Rank.QUEEN -> "Q"
        Rank.KING -> "K"
    }

    private fun restoreRootLayerOrderAfterBoardRebuild() {
        overlayLayer.parent?.removeChild(overlayLayer)
        overlayLayer.addTo(rootContainer)
        dragLayer.parent?.removeChild(dragLayer)
        dragLayer.addTo(rootContainer)
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
        if (preview == null) {
            val existingGhost = dragGhostView
            if (existingGhost != null) {
                existingGhost.parent?.removeChild(existingGhost)
            }
            dragGhostView = null
            dragGhostContentSignature = null
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
