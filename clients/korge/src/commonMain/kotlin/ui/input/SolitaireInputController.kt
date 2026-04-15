package ui.input

import domain.readmodel.CardFace
import domain.readmodel.CardViewModel
import korlibs.korge.input.onClick
import korlibs.korge.input.onMouseDrag
import korlibs.korge.view.SolidRect
import korlibs.time.DateTime
import korlibs.time.TimeSpan
import korlibs.time.seconds
import presentation.solitaire.UiIntent
import presentation.solitaire.geometry.DraggableCardInteractionTarget
import presentation.solitaire.geometry.PileHitRegion
import ui.render.BoardDragPreview
import ui.render.SolitaireRenderedBoard
import kotlin.math.abs

internal data class TapSelectionResolution(
    val nextSelectedSourcePileId: String?,
    val moveIntent: UiIntent.DragMove?,
)

internal fun resolveTapSelection(
    currentSelectedSourcePileId: String?,
    tappedPileId: String,
): TapSelectionResolution {
    if (currentSelectedSourcePileId == null) {
        return TapSelectionResolution(
            nextSelectedSourcePileId = tappedPileId,
            moveIntent = null,
        )
    }
    if (currentSelectedSourcePileId == tappedPileId) {
        return TapSelectionResolution(
            nextSelectedSourcePileId = null,
            moveIntent = null,
        )
    }
    return TapSelectionResolution(
        nextSelectedSourcePileId = null,
        moveIntent = UiIntent.DragMove(
            sourcePileId = currentSelectedSourcePileId,
            destinationPileId = tappedPileId,
            cardCount = 1,
        ),
    )
}

internal fun resolvePileIdAtPoint(
    x: Double,
    y: Double,
    pileHitRegions: Collection<PileHitRegion>,
): String? =
    pileHitRegions.firstOrNull { it.bounds.containsPoint(x, y) }?.pileId

/**
 * Handles clicks, taps, and drags for a [ui.render.SolitaireRenderedBoard].
 *
 * Converts pointer input into [UiIntent] calls and keeps selection/drag preview state.
 * Call [bind] after each render so hit targets stay current.
 */
class SolitaireInputController {
    companion object {
        private const val INPUT_LOG_TAG = "SolitaireInput"
        private val foundationDoubleTapMaxGap: TimeSpan = 0.35.seconds
    }

    private var latestRenderedBoard: SolitaireRenderedBoard? = null
    private val pileRectsWithInput = mutableSetOf<SolidRect>()
    private val cardRectsWithDragInput = mutableSetOf<SolidRect>()

    private var selectedSourcePileId: String? = null
    private var selectedSourceCardCount: Int = 1
    private var controlsAreBound = false

    private var lastCardTapForFoundationAt: DateTime? = null
    private var lastCardTapForFoundationRect: SolidRect? = null

    data class DragDropAttempt(
        val sourcePileId: String,
        val destinationPileId: String?,
        val cardCount: Int,
        val card: CardViewModel,
        val dropX: Double,
        val dropY: Double,
    )

    /** Attaches input handlers and wires callbacks for intents, selection, drag preview, undo, and redo. */
    fun bind(
        renderedBoard: SolitaireRenderedBoard,
        dispatchIntent: (UiIntent) -> Unit,
        onSelectionChanged: (String?) -> Unit,
        onDragPreviewChanged: (BoardDragPreview?) -> Unit,
        onDragDropAttempt: (DragDropAttempt) -> Unit,
        onFoundationDoubleTapAttempt: (tapStageX: Double, tapStageY: Double, live: DraggableCardInteractionTarget) -> Unit,
        onUndo: () -> Unit,
        onRedo: () -> Unit,
        onRequestNewGameConfirmation: () -> Unit,
    ) {
        latestRenderedBoard = renderedBoard

        if (!controlsAreBound) {
            renderedBoard.stockButton.onClick {
                dispatchIntent(UiIntent.Draw)
                selectedSourcePileId = null
                selectedSourceCardCount = 1
                onSelectionChanged(null)
            }
            renderedBoard.recycleButton.onClick {
                dispatchIntent(UiIntent.Recycle)
                selectedSourcePileId = null
                selectedSourceCardCount = 1
                onSelectionChanged(null)
            }
            renderedBoard.autoMoveButton.onClick {
                dispatchIntent(UiIntent.AutoMove)
                selectedSourcePileId = null
                selectedSourceCardCount = 1
                onSelectionChanged(null)
            }
            renderedBoard.undoButton.onClick {
                onUndo()
                selectedSourcePileId = null
                selectedSourceCardCount = 1
                onSelectionChanged(null)
            }
            renderedBoard.redoButton.onClick {
                onRedo()
                selectedSourcePileId = null
                selectedSourceCardCount = 1
                onSelectionChanged(null)
            }
            renderedBoard.newGameButton.onClick {
                onRequestNewGameConfirmation()
                selectedSourcePileId = null
                selectedSourceCardCount = 1
                onSelectionChanged(null)
            }
            controlsAreBound = true
        }

        val currentPileRects = renderedBoard.pileTapTargets.values.toSet()
        pileRectsWithInput.retainAll { it in currentPileRects }
        renderedBoard.pileTapTargets.forEach { (pileId, pileView) ->
            if (pileView in pileRectsWithInput) return@forEach
            pileRectsWithInput.add(pileView)
            pileView.onClick {
                handlePileTap(
                    tappedPileId = pileId,
                    tappedCardCount = 1,
                    dispatchIntent = dispatchIntent,
                    onSelectionChanged = onSelectionChanged,
                )
            }
        }

        val currentCardRects = renderedBoard.draggableTopCardRects.toSet()
        cardRectsWithDragInput.retainAll { it in currentCardRects }
        renderedBoard.draggableTopCardRects.forEach { cardRect ->
            if (cardRect in cardRectsWithDragInput) return@forEach
            cardRectsWithDragInput.add(cardRect)
            bindTopCardInteractionForStableCardRect(
                cardRect = cardRect,
                dispatchIntent = dispatchIntent,
                onSelectionChanged = onSelectionChanged,
                onDragPreviewChanged = onDragPreviewChanged,
                onDragDropAttempt = onDragDropAttempt,
                onFoundationDoubleTapAttempt = onFoundationDoubleTapAttempt,
            )
        }
    }

    private fun handlePileTap(
        tappedPileId: String,
        tappedCardCount: Int,
        dispatchIntent: (UiIntent) -> Unit,
        onSelectionChanged: (String?) -> Unit,
    ) {
        if (tappedPileId == "stock") {
            dispatchIntent(UiIntent.Draw)
            selectedSourcePileId = null
            selectedSourceCardCount = 1
            onSelectionChanged(null)
            return
        }
        val existingSourcePileId = selectedSourcePileId
        val tapSelectionResolution = resolveTapSelection(
            currentSelectedSourcePileId = existingSourcePileId,
            tappedPileId = tappedPileId,
        )
        val moveIntent = tapSelectionResolution.moveIntent
        if (moveIntent != null) {
            dispatchIntent(
                moveIntent.copy(cardCount = selectedSourceCardCount),
            )
            selectedSourcePileId = null
            selectedSourceCardCount = 1
            onSelectionChanged(null)
            return
        }
        selectedSourcePileId = tapSelectionResolution.nextSelectedSourcePileId
        selectedSourceCardCount = if (tapSelectionResolution.nextSelectedSourcePileId != null) {
            tappedCardCount
        } else {
            1
        }
        onSelectionChanged(tapSelectionResolution.nextSelectedSourcePileId)
    }

    private fun liveDraggableForCardRect(cardRect: SolidRect): DraggableCardInteractionTarget? =
        latestRenderedBoard?.draggableForTopCardSolidRect(cardRect)

    private fun bindTopCardInteractionForStableCardRect(
        cardRect: SolidRect,
        dispatchIntent: (UiIntent) -> Unit,
        onSelectionChanged: (String?) -> Unit,
        onDragPreviewChanged: (BoardDragPreview?) -> Unit,
        onDragDropAttempt: (DragDropAttempt) -> Unit,
        onFoundationDoubleTapAttempt: (Double, Double, DraggableCardInteractionTarget) -> Unit,
    ) {
        cardRect.onClick { mouse ->
            val live = liveDraggableForCardRect(cardRect) ?: return@onClick
            val now = DateTime.now()
            val isFoundationDoubleTap =
                lastCardTapForFoundationRect === cardRect &&
                    lastCardTapForFoundationAt != null &&
                    (now - lastCardTapForFoundationAt!!) <= foundationDoubleTapMaxGap
            if (isFoundationDoubleTap && isSurfaceEligibleForFoundationDoubleTap(live)) {
                lastCardTapForFoundationAt = null
                lastCardTapForFoundationRect = null
                println(
                    "[$INPUT_LOG_TAG] foundationDoubleTap pile=${live.pileId} suit=${live.card.suit}",
                )
                onFoundationDoubleTapAttempt(
                    mouse.currentPosStage.x,
                    mouse.currentPosStage.y,
                    live,
                )
                return@onClick
            }
            lastCardTapForFoundationAt = now
            lastCardTapForFoundationRect = cardRect
            handlePileTap(
                tappedPileId = live.pileId,
                tappedCardCount = live.cardCount,
                dispatchIntent = dispatchIntent,
                onSelectionChanged = onSelectionChanged,
            )
        }

        var didDragPastThreshold = false
        cardRect.onMouseDrag { dragInfo ->
            val board = latestRenderedBoard ?: return@onMouseDrag
            val live = liveDraggableForCardRect(cardRect) ?: return@onMouseDrag

            if (dragInfo.start) {
                didDragPastThreshold = false
            }
            if (!didDragPastThreshold && (abs(dragInfo.dx) > 8.0 || abs(dragInfo.dy) > 8.0)) {
                didDragPastThreshold = true
                selectedSourcePileId = live.pileId
                selectedSourceCardCount = live.cardCount
            }
            if (!didDragPastThreshold) {
                return@onMouseDrag
            }

            val destinationPileId = findPileIdAt(
                x = dragInfo.cx,
                y = dragInfo.cy,
                pileHitRegions = board.interaction.pileHitRegionsByPileId.values,
            )
            if (dragInfo.end) {
                onDragDropAttempt(
                    DragDropAttempt(
                        sourcePileId = live.pileId,
                        destinationPileId = destinationPileId,
                        cardCount = live.cardCount,
                        card = live.card,
                        dropX = dragInfo.cx,
                        dropY = dragInfo.cy,
                    ),
                )
                selectedSourcePileId = null
                selectedSourceCardCount = 1
                return@onMouseDrag
            }
            onDragPreviewChanged(
                BoardDragPreview(
                    sourcePileId = live.pileId,
                    sourceCardCount = live.cardCount,
                    stackCards = live.stackCards,
                    x = dragInfo.cx,
                    y = dragInfo.cy,
                ),
            )
        }
    }

    private fun findPileIdAt(
        x: Double,
        y: Double,
        pileHitRegions: Collection<PileHitRegion>,
    ): String? = resolvePileIdAtPoint(
        x = x,
        y = y,
        pileHitRegions = pileHitRegions,
    )

    private fun isSurfaceEligibleForFoundationDoubleTap(live: DraggableCardInteractionTarget): Boolean {
        if (live.pileId.startsWith("foundation-") || live.pileId == "stock") {
            return false
        }
        if (live.pileId != "waste" && !live.pileId.startsWith("tableau-")) {
            return false
        }
        if (live.card.face !is CardFace.Up) {
            return false
        }
        if (live.pileId.startsWith("tableau-") && live.cardCount != 1) {
            return false
        }
        return true
    }
}
