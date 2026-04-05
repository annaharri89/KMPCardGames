package ui.input

import domain.readmodel.CardViewModel
import korlibs.korge.input.onClick
import korlibs.korge.input.onMouseDrag
import ui.render.BoardDragPreview
import ui.render.DraggableCardTarget
import ui.adapter.UiIntent
import ui.render.PileHitArea
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
    pileHitAreas: Collection<PileHitArea>,
): String? {
    return pileHitAreas.firstOrNull { pileHitArea ->
        x >= pileHitArea.x &&
            x <= pileHitArea.x + pileHitArea.width &&
            y >= pileHitArea.y &&
            y <= pileHitArea.y + pileHitArea.height
    }?.pileId
}

/**
 * Wires KorGE pointer input on an already-built [ui.render.SolitaireRenderedBoard] to game actions.
 *
 * It does **not** own game state; it turns clicks and drags into [UiIntent]s (via the `dispatchIntent`
 * callback) and tells the scene when selection or drag preview should update. Typical flow:
 *
 * - **Toolbar buttons** (stock draw, recycle, auto-move, undo/redo) dispatch the matching intent and
 *   clear any pile selection.
 * - **Tap on piles / cards** uses a two-step “pick source, then destination” model; tapping **stock**
 *   always draws immediately.
 * - **Drag from a visible stack** (tableau / free-cell style targets) shows a [ui.render.BoardDragPreview]
 * while moving; on release, `onDragDropAttempt` runs so the scene can validate the drop and dispatch
 * a multi-card [UiIntent.DragMove] if appropriate.
 *
 * Call [bind] after the renderer built views and hit areas so handlers attach to the right nodes.
 * Toolbar buttons are wired only on the first [bind]; pile and draggable targets are updated on
 * every call so a rebuilt board picks up new views.
 */
class SolitaireInputController {
    private var selectedSourcePileId: String? = null
    private var selectedSourceCardCount: Int = 1
    private var controlsAreBound = false

    data class DragDropAttempt(
        val sourcePileId: String,
        val destinationPileId: String,
        val cardCount: Int,
        val card: CardViewModel,
        val dropX: Double,
        val dropY: Double,
    )

    /** Attaches click and drag handlers; callbacks forward intents, selection, preview, and undo/redo. */
    fun bind(
        renderedBoard: SolitaireRenderedBoard,
        dispatchIntent: (UiIntent) -> Unit,
        onSelectionChanged: (String?) -> Unit,
        onDragPreviewChanged: (BoardDragPreview?) -> Unit,
        onDragDropAttempt: (DragDropAttempt) -> Unit,
        onUndo: () -> Unit,
        onRedo: () -> Unit,
    ) {
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
            controlsAreBound = true
        }
        renderedBoard.pileTapTargets.forEach { (pileId, pileView) ->
            pileView.onClick {
                handlePileTap(
                    tappedPileId = pileId,
                    tappedCardCount = 1,
                    dispatchIntent = dispatchIntent,
                    onSelectionChanged = onSelectionChanged,
                )
            }
        }
        renderedBoard.draggableCardTargets.forEach { draggableCardTarget ->
            bindTopCardInteraction(
                draggableCardTarget = draggableCardTarget,
                renderedBoard = renderedBoard,
                dispatchIntent = dispatchIntent,
                onSelectionChanged = onSelectionChanged,
                onDragPreviewChanged = onDragPreviewChanged,
                onDragDropAttempt = onDragDropAttempt,
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

    private fun bindTopCardInteraction(
        draggableCardTarget: DraggableCardTarget,
        renderedBoard: SolitaireRenderedBoard,
        dispatchIntent: (UiIntent) -> Unit,
        onSelectionChanged: (String?) -> Unit,
        onDragPreviewChanged: (BoardDragPreview?) -> Unit,
        onDragDropAttempt: (DragDropAttempt) -> Unit,
    ) {
        draggableCardTarget.cardView.onClick {
            handlePileTap(
                tappedPileId = draggableCardTarget.pileId,
                tappedCardCount = draggableCardTarget.cardCount,
                dispatchIntent = dispatchIntent,
                onSelectionChanged = onSelectionChanged,
            )
        }

        var didDragPastThreshold = false
        draggableCardTarget.cardView.onMouseDrag { dragInfo ->
            if (dragInfo.start) {
                didDragPastThreshold = false
            }
            if (!didDragPastThreshold && (abs(dragInfo.dx) > 8.0 || abs(dragInfo.dy) > 8.0)) {
                didDragPastThreshold = true
                selectedSourcePileId = draggableCardTarget.pileId
                selectedSourceCardCount = draggableCardTarget.cardCount
            }
            if (!didDragPastThreshold) {
                return@onMouseDrag
            }

            val destinationPileId = findPileIdAt(
                x = dragInfo.cx,
                y = dragInfo.cy,
                pileHitAreas = renderedBoard.pileHitAreas.values,
            )
            if (dragInfo.end) {
                onDragPreviewChanged(null)
                if (destinationPileId != null && destinationPileId != draggableCardTarget.pileId) {
                    onDragDropAttempt(
                        DragDropAttempt(
                            sourcePileId = draggableCardTarget.pileId,
                            destinationPileId = destinationPileId,
                            cardCount = draggableCardTarget.cardCount,
                            card = draggableCardTarget.card,
                            dropX = dragInfo.cx,
                            dropY = dragInfo.cy,
                        ),
                    )
                }
                selectedSourcePileId = null
                selectedSourceCardCount = 1
                return@onMouseDrag
            }
            onDragPreviewChanged(
                BoardDragPreview(
                    stackCards = draggableCardTarget.stackCards,
                    x = dragInfo.cx,
                    y = dragInfo.cy,
                ),
            )
        }
    }

    private fun findPileIdAt(
        x: Double,
        y: Double,
        pileHitAreas: Collection<PileHitArea>,
    ): String? {
        return resolvePileIdAtPoint(
            x = x,
            y = y,
            pileHitAreas = pileHitAreas,
        )
    }
}
