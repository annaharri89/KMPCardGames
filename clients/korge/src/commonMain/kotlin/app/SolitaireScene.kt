package app

import korlibs.io.async.launchImmediately
import korlibs.korge.view.Stage
import korlibs.korge.view.solidRect
import ui.adapter.UiIntent
import ui.input.SolitaireInputController
import ui.render.SolitaireBoardRenderer
import ui.render.expectedTopCardYForSolitairePile

/**
 * KorGE entry for the playable solitaire screen: background, [SolitaireGameStore], [SolitaireBoardRenderer],
 * and [SolitaireInputController]. Card faces use text and shape fallbacks (no card atlas); the board
 * re-renders and input re-binds after each state change. Drag-drop runs a short [SolitaireBoardRenderer.animateCardTravel]
 * tween before applying the final layout.
 */
class SolitaireScene {
    companion object {
        private const val SCENE_LOG_TAG = "KawaiiThemeScene"
        private const val MOVE_ANIMATION_LOG_TAG = "KawaiiThemeMoveAnimation"
    }

    /** Attaches the full solitaire UI tree and handlers to [rootContainer] (usually the KorGE stage). */
    fun install(rootContainer: Stage) {
        with(rootContainer) {
            val solitaireGameStore = SolitaireGameStore()
            var solitaireUiState = solitaireGameStore.start()
            val themeSpec = cardThemeSpec(solitaireUiState.cardTheme)
            println("[$SCENE_LOG_TAG] activeTheme=${solitaireUiState.cardTheme}")

            solidRect(
                width = views.virtualWidth.toDouble(),
                height = views.virtualHeight.toDouble(),
                color = themeSpec.boardBackgroundColor,
            )
            val invalidMoveOverlay = solidRect(
                width = views.virtualWidth.toDouble(),
                height = views.virtualHeight.toDouble(),
                color = themeSpec.invalidMoveOverlayColor,
            ) {
                alpha = 0.0
            }
            val solitaireBoardRenderer = SolitaireBoardRenderer(
                rootContainer = this,
                viewportWidth = views.virtualWidth.toDouble(),
                viewportHeight = views.virtualHeight.toDouble(),
            )
            val solitaireInputController = SolitaireInputController()
            lateinit var latestRenderedBoard: ui.render.SolitaireRenderedBoard
            var moveAnimationRunning = false

            fun renderAndBind(bindInput: Boolean = true) {
                latestRenderedBoard = solitaireBoardRenderer.render(
                    uiState = solitaireUiState,
                    selectedPileId = solitaireUiState.selectedSourcePileId,
                )
                solitaireBoardRenderer.renderDragPreview(null)
                if (!bindInput) return
                solitaireInputController.bind(
                    renderedBoard = latestRenderedBoard,
                    dispatchIntent = { uiIntent ->
                        if (moveAnimationRunning) return@bind
                        solitaireUiState = solitaireGameStore.dispatchIntent(uiIntent)
                        solitaireUiState = solitaireUiState.copy(selectedSourcePileId = null)
                        if (!solitaireUiState.wasLastMoveAccepted) {
                            invalidMoveOverlay.alpha = 0.22
                        } else {
                            invalidMoveOverlay.alpha = 0.0
                        }
                        renderAndBind()
                    },
                    onSelectionChanged = { selectedPileId ->
                        if (moveAnimationRunning) return@bind
                        solitaireUiState = solitaireUiState.copy(selectedSourcePileId = selectedPileId)
                        renderAndBind()
                    },
                    onDragPreviewChanged = { dragPreview ->
                        if (moveAnimationRunning) return@bind
                        solitaireBoardRenderer.renderDragPreview(dragPreview)
                    },
                    onDragDropAttempt = { dragDropAttempt ->
                        if (moveAnimationRunning) return@bind
                        solitaireBoardRenderer.renderDragPreview(null)
                        val stateAfterDrop = solitaireGameStore.dispatchIntent(
                            UiIntent.DragMove(
                                sourcePileId = dragDropAttempt.sourcePileId,
                                destinationPileId = dragDropAttempt.destinationPileId,
                                cardCount = dragDropAttempt.cardCount,
                            ),
                        )
                        val pileIdForAnimationTarget = if (stateAfterDrop.wasLastMoveAccepted) {
                            dragDropAttempt.destinationPileId
                        } else {
                            dragDropAttempt.sourcePileId
                        }
                        val animationTargetHitArea = latestRenderedBoard.pileHitAreas[pileIdForAnimationTarget]
                        if (animationTargetHitArea == null) {
                            solitaireUiState = stateAfterDrop.copy(selectedSourcePileId = null)
                            invalidMoveOverlay.alpha = if (stateAfterDrop.wasLastMoveAccepted) 0.0 else 0.22
                            renderAndBind()
                            return@bind
                        }
                        println(
                            "[$MOVE_ANIMATION_LOG_TAG] accepted=${stateAfterDrop.wasLastMoveAccepted} " +
                                "source=${dragDropAttempt.sourcePileId} target=$pileIdForAnimationTarget",
                        )
                        val renderModelAfter = stateAfterDrop.renderModel
                        val expectedTopY = renderModelAfter?.let { rm ->
                            expectedTopCardYForSolitairePile(
                                pileId = pileIdForAnimationTarget,
                                renderModel = rm,
                                viewportWidth = views.virtualWidth.toDouble(),
                                viewportHeight = views.virtualHeight.toDouble(),
                            )
                        }
                        val animationEndY = expectedTopY ?: animationTargetHitArea.y
                        moveAnimationRunning = true
                        launchImmediately(coroutineContext) {
                            solitaireBoardRenderer.animateCardTravel(
                                card = dragDropAttempt.card,
                                startX = dragDropAttempt.dropX,
                                startY = dragDropAttempt.dropY,
                                endX = animationTargetHitArea.x,
                                endY = animationEndY,
                                isMoveAccepted = stateAfterDrop.wasLastMoveAccepted,
                            )
                            moveAnimationRunning = false
                            solitaireUiState = stateAfterDrop.copy(selectedSourcePileId = null)
                            invalidMoveOverlay.alpha = if (stateAfterDrop.wasLastMoveAccepted) 0.0 else 0.22
                            renderAndBind()
                        }
                    },
                    onUndo = {
                        if (moveAnimationRunning) return@bind
                        solitaireUiState = solitaireGameStore.undo()
                        solitaireUiState = solitaireUiState.copy(selectedSourcePileId = null)
                        renderAndBind()
                    },
                    onRedo = {
                        if (moveAnimationRunning) return@bind
                        solitaireUiState = solitaireGameStore.redo()
                        solitaireUiState = solitaireUiState.copy(selectedSourcePileId = null)
                        renderAndBind()
                    },
                )
            }

            renderAndBind()
        }
    }
}
