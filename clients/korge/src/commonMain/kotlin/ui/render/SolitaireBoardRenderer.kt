package ui.render

import app.CardTheme
import app.SolitaireUiState
import app.cardThemeSpec
import domain.model.CardColor
import domain.model.Suit
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

internal fun calculateTableauHitHeight(
    cardHeight: Double,
    tableauCardOffsetY: Double,
    cardCount: Int,
): Double {
    return cardHeight + (max(0, cardCount - 1) * tableauCardOffsetY)
}

internal fun isHiddenCard(cardViewModel: CardViewModel): Boolean = cardViewModel.rankSymbol == "HIDDEN"

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
 * Each [render] replaces [boardLayer] entirely so views stay in sync with [SolitaireUiState]; callers
 * typically re-bind input with the returned [SolitaireRenderedBoard] (see [ui.input.SolitaireInputController.bind]).
 */
class SolitaireBoardRenderer(
    private val rootContainer: Container,
    private val viewportWidth: Double = 1280.0,
    private val viewportHeight: Double = 720.0,
) {
    companion object {
        private const val THEME_RENDERER_LOG_TAG = "KawaiiThemeRenderer"
    }

    private val cardWidth = 96.0
    private val cardHeight = 132.0
    private val tableauCardOffsetY = 24.0

    private var activeCardTheme: CardTheme = CardTheme.KAWAII_NATURE
    private var activeThemeSpec = cardThemeSpec(activeCardTheme)
    private var texturePackerSliceByBaseName: Map<String, RectSlice<Bitmap>>? = null
    private var suitSymbolPainter = SuitSymbolPainter(activeThemeSpec, texturePackerSliceByBaseName)
    private var faceCardAnimalPainter = FaceCardAnimalPainter(texturePackerSliceByBaseName)

    fun setTexturePackerSlices(sliceByBaseName: Map<String, RectSlice<Bitmap>>?) {
        texturePackerSliceByBaseName = sliceByBaseName
        suitSymbolPainter = SuitSymbolPainter(activeThemeSpec, sliceByBaseName)
        faceCardAnimalPainter = FaceCardAnimalPainter(sliceByBaseName)
    }

    private var boardLayer = Container().addTo(rootContainer)
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
        refreshTheme(uiState.cardTheme)
        val renderModel = requireNotNull(uiState.renderModel) {
            "Cannot render before game starts"
        }
        boardLayer.parent?.removeChild(boardLayer)
        boardLayer = Container().addTo(rootContainer)
        val boardLayout = BoardLayout.create(
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
        )

        val pileTapTargets = linkedMapOf<String, SolidRect>()
        val pileHitAreas = linkedMapOf<String, PileHitArea>()
        val draggableCardTargets = mutableListOf<DraggableCardTarget>()

        val stockSlot = drawPileSlot(
            pileId = "stock",
            x = boardLayout.stockPile.x,
            y = boardLayout.stockPile.y,
            highlightColor = Colors["#3d6f3d"],
            isSelected = selectedPileId == "stock",
        )
        pileTapTargets["stock"] = stockSlot.first
        pileHitAreas["stock"] = stockSlot.second

        val wasteSlot = drawPileSlot(
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
            val foundationSlot = drawPileSlot(
                pileId = pileId,
                x = pileLayout.x,
                y = pileLayout.y,
                highlightColor = Colors["#3d4f6f"],
                isSelected = selectedPileId == pileId,
            )
            suitSymbolPainter.draw(
                parentContainer = boardLayer,
                suit = suit,
                x = pileLayout.x + 31.0,
                y = pileLayout.y + 40.0,
                symbolWidth = 34.0,
                symbolHeight = 32.0,
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
            val tableauSlot = drawPileSlot(
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

        drawStockCount(renderModel)
        drawWasteCards(
            renderModel = renderModel,
            x = boardLayout.wastePile.x,
            y = boardLayout.wastePile.y,
            draggableCardTargets = draggableCardTargets,
        )
        drawFoundationCards(
            renderModel = renderModel,
            boardLayout = boardLayout,
            draggableCardTargets = draggableCardTargets,
        )
        drawTableauCards(
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

    private fun refreshTheme(cardTheme: CardTheme) {
        if (activeCardTheme == cardTheme) return
        activeCardTheme = cardTheme
        activeThemeSpec = cardThemeSpec(cardTheme)
        suitSymbolPainter = SuitSymbolPainter(activeThemeSpec, texturePackerSliceByBaseName)
        faceCardAnimalPainter = FaceCardAnimalPainter(texturePackerSliceByBaseName)
        println("[$THEME_RENDERER_LOG_TAG] switchedTheme=$cardTheme")
    }

    private fun drawPileSlot(
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
        val slot = boardLayer.solidRect(
            width = cardWidth,
            height = slotHeight,
            color = slotFillColor,
        ) {
            this.x = x
            this.y = y
            name = pileId
        }
        val hitArea = PileHitArea(
            pileId = pileId,
            x = x,
            y = y,
            width = cardWidth,
            height = slotHeight,
        )
        return slot to hitArea
    }

    private fun drawWasteCards(
        renderModel: GameRenderModel,
        x: Double,
        y: Double,
        draggableCardTargets: MutableList<DraggableCardTarget>,
    ) {
        val topWasteCard = renderModel.wastePile.cards.lastOrNull() ?: return
        val drawnCard = drawCardView(
            card = topWasteCard,
            x = x,
            y = y,
            isInteractive = true,
        )
        draggableCardTargets += DraggableCardTarget(
            pileId = "waste",
            card = topWasteCard,
            cardCount = 1,
            stackCards = listOf(topWasteCard),
            cardView = drawnCard,
        )
    }

    private fun drawFoundationCards(
        renderModel: GameRenderModel,
        boardLayout: ui.layout.SolitaireBoardLayout,
        draggableCardTargets: MutableList<DraggableCardTarget>,
    ) {
        renderModel.foundationPiles.forEachIndexed { index, pileViewModel ->
            val topCard = pileViewModel.cards.lastOrNull() ?: return@forEachIndexed
            val pileLayout = boardLayout.foundationPiles[index]
            val drawnCard = drawCardView(
                card = topCard,
                x = pileLayout.x,
                y = pileLayout.y,
                isInteractive = true,
            )
            draggableCardTargets += DraggableCardTarget(
                pileId = pileViewModel.pileId,
                card = topCard,
                cardCount = 1,
                stackCards = listOf(topCard),
                cardView = drawnCard,
            )
        }
    }

    private fun drawTableauCards(
        renderModel: GameRenderModel,
        boardLayout: ui.layout.SolitaireBoardLayout,
        draggableCardTargets: MutableList<DraggableCardTarget>,
    ) {
        renderModel.tableauPiles.forEachIndexed { index, pileViewModel ->
            val pileLayout = boardLayout.tableauPiles[index]
            pileViewModel.cards.forEachIndexed { cardIndex, card ->
                val isVisibleTableauCard = !isHiddenCard(card)
                val cardCountFromSelection = pileViewModel.cards.size - cardIndex
                val drawnCard = drawCardView(
                    card = card,
                    x = pileLayout.x,
                    y = pileLayout.y + (cardIndex * tableauCardOffsetY),
                    isInteractive = isVisibleTableauCard,
                )
                if (isVisibleTableauCard) {
                    val stackCards = pileViewModel.cards.subList(cardIndex, pileViewModel.cards.size)
                    draggableCardTargets += DraggableCardTarget(
                        pileId = pileViewModel.pileId,
                        card = card,
                        cardCount = cardCountFromSelection,
                        stackCards = stackCards,
                        cardView = drawnCard,
                    )
                }
            }
        }
    }

    private fun drawStockCount(renderModel: GameRenderModel) {
        stockCountText.text = "Stock: ${renderModel.stockPileCount}"
    }

    private fun drawCardView(
        card: CardViewModel,
        x: Double,
        y: Double,
        isInteractive: Boolean,
    ): SolidRect {
        val cardContainer = Container().addTo(boardLayer)
        cardContainer.x = x
        cardContainer.y = y
        val cardRect = drawCardContent(
            parentContainer = cardContainer,
            card = card,
            isInteractive = isInteractive,
            enableAnimatedFaceMotif = true,
        )
        return cardRect
    }

    private fun drawCardContent(
        parentContainer: Container,
        card: CardViewModel,
        isInteractive: Boolean,
        enableAnimatedFaceMotif: Boolean,
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
        val rankColor = if (card.color == CardColor.RED) activeThemeSpec.redSuitColor else activeThemeSpec.blackSuitColor
        parentContainer.text(
            text = rankShortLabel(card.rankSymbol),
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
        val faceCardAnimal = faceCardAnimalForRankSymbol(card.rankSymbol)
        val faceMotifWidth = if (texturePackerSliceByBaseName != null) 84.0 else 62.0
        val faceMotifHeight = if (texturePackerSliceByBaseName != null) 96.0 else 74.0
        val centerMotif: View? = if (faceCardAnimal == FaceCardAnimal.NONE) {
            drawNumberCardOrnament(
                parentContainer = parentContainer,
                suit = card.suit,
                suitColor = rankColor,
            )
            suitSymbolPainter.draw(
                parentContainer = parentContainer,
                suit = card.suit,
                x = (cardWidth - 42.0) / 2.0,
                y = (cardHeight - 38.0) / 2.0,
                symbolWidth = 42.0,
                symbolHeight = 38.0,
            )
            null
        } else {
            faceCardAnimalPainter.draw(
                parentContainer = parentContainer,
                rankSymbol = card.rankSymbol,
                suit = card.suit,
                x = (cardWidth - faceMotifWidth) / 2.0,
                y = (cardHeight - faceMotifHeight) / 2.0,
                width = faceMotifWidth,
                height = faceMotifHeight,
                suitAccentColor = rankColor,
                enableAnimatedFaceMotif = enableAnimatedFaceMotif,
            )
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

    private fun drawNumberCardOrnament(
        parentContainer: Container,
        suit: Suit,
        suitColor: RGBA,
    ) {
        val suitOrnamentVariant = suitOrnamentVariantFor(suit)
        parentContainer.solidRect(
            width = 52.0,
            height = 58.0,
            color = activeThemeSpec.ornamentPrimaryColor.withAd(0.16),
        ) {
            x = (cardWidth - 52.0) / 2.0
            y = (cardHeight - 58.0) / 2.0
            mouseEnabled = false
        }
        drawMicroPatternPanel(
            parentContainer = parentContainer,
            microPatternStyle = suitOrnamentVariant.primaryPatternStyle,
            x = (cardWidth - 52.0) / 2.0 + 2.0,
            y = (cardHeight - 58.0) / 2.0 + 2.0,
            width = 22.0,
            height = 54.0,
            primaryColor = suitColor,
            secondaryColor = activeThemeSpec.ornamentPrimaryColor,
            baseAlpha = 0.52,
        )
        drawMicroPatternPanel(
            parentContainer = parentContainer,
            microPatternStyle = suitOrnamentVariant.secondaryPatternStyle,
            x = (cardWidth - 52.0) / 2.0 + 28.0,
            y = (cardHeight - 58.0) / 2.0 + 2.0,
            width = 22.0,
            height = 54.0,
            primaryColor = suitColor,
            secondaryColor = activeThemeSpec.ornamentPrimaryColor,
            baseAlpha = 0.52,
        )
        parentContainer.solidRect(
            width = 52.0,
            height = 2.0,
            color = suitColor.withAd(0.45),
        ) {
            x = (cardWidth - 52.0) / 2.0
            y = (cardHeight - 58.0) / 2.0 + 12.0
            mouseEnabled = false
        }
        parentContainer.solidRect(
            width = 52.0,
            height = 2.0,
            color = suitColor.withAd(0.45),
        ) {
            x = (cardWidth - 52.0) / 2.0
            y = (cardHeight - 58.0) / 2.0 + 44.0
            mouseEnabled = false
        }
        suitSymbolPainter.draw(
            parentContainer = parentContainer,
            suit = suit,
            x = (cardWidth - 14.0) / 2.0,
            y = (cardHeight - 14.0) / 2.0,
            symbolWidth = 14.0,
            symbolHeight = 14.0,
        )
    }

    private fun rankShortLabel(rankSymbol: String): String {
        return when (rankSymbol) {
            "ACE" -> "A"
            "JACK" -> "J"
            "QUEEN" -> "Q"
            "KING" -> "K"
            "TEN" -> "10"
            "TWO" -> "2"
            "THREE" -> "3"
            "FOUR" -> "4"
            "FIVE" -> "5"
            "SIX" -> "6"
            "SEVEN" -> "7"
            "EIGHT" -> "8"
            "NINE" -> "9"
            else -> rankSymbol
        }
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
        cards.joinToString("|") { "${it.rankSymbol}_${it.suit}" }

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
                )
            }
        }
    }
}
