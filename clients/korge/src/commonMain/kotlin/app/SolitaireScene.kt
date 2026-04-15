package app

import korlibs.image.format.readBitmap
import korlibs.io.async.launchImmediately
import korlibs.korge.view.Stage
import korlibs.korge.view.solidRect
import presentation.solitaire.SolitaireGameStore
import presentation.solitaire.UiIntent
import ui.input.SolitaireInputController
import ui.render.FoxHeartPuppetSheet
import ui.render.FoxPuppetSheetLayout
import ui.render.BoardDragPreview
import presentation.solitaire.geometry.DraggableCardInteractionTarget
import ui.overlay.NewGameConfirmDialog
import ui.render.FoxSpadePuppetSheet
import ui.render.SolitaireBoardRenderer
import ui.render.SuitPipBitmapNormalize
import ui.render.expectedTopCardYForSolitairePile

/**
 * Playable solitaire scene for KorGE.
 *
 * Wires store, renderer, and input; then loads optional pip and fox-sheet assets from `debug/`.
 * Drag-drop and foundation double-tap use [SolitaireBoardRenderer.animateCardTravel] before applying final UI state.
 */
class SolitaireScene {
    companion object {
        private const val SCENE_LOG_TAG = "KawaiiThemeScene"
        private const val MOVE_ANIMATION_LOG_TAG = "KawaiiThemeMoveAnimation"
        private const val SIMPLE_SUIT_PIP_LOG_TAG = "SimpleSuitPip"
    }

    /** Builds the solitaire UI under [rootContainer] and binds input handlers. */
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
            var newGameConfirmDialogInstalled = false

            fun renderAndBind(bindInput: Boolean = true) {
                if (!newGameConfirmDialogInstalled) {
                    newGameConfirmDialogInstalled = true
                    NewGameConfirmDialog.install(
                        modalLayer = solitaireBoardRenderer.modalOverlayRoot,
                        stageWidth = views.virtualWidth.toDouble(),
                        stageHeight = views.virtualHeight.toDouble(),
                        onUserConfirmedNewGame = {
                            solitaireBoardRenderer.setModalOverlayVisible(false)
                            solitaireUiState = solitaireGameStore.start().copy(selectedSourcePileId = null)
                            invalidMoveOverlay.alpha = 0.0
                            renderAndBind()
                        },
                        onUserCancelledNewGame = {
                            solitaireBoardRenderer.setModalOverlayVisible(false)
                        },
                    )
                }
                latestRenderedBoard = solitaireBoardRenderer.render(
                    uiState = solitaireUiState,
                    selectedPileId = solitaireUiState.selectedSourcePileId,
                )
                solitaireBoardRenderer.renderDragPreview(null)
                if (!bindInput) return
                solitaireInputController.bind(
                    renderedBoard = latestRenderedBoard,
                    dispatchIntent = { uiIntent ->
                        if (moveAnimationRunning || solitaireBoardRenderer.isModalOverlayVisible()) return@bind
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
                        if (moveAnimationRunning || solitaireBoardRenderer.isModalOverlayVisible()) return@bind
                        solitaireUiState = solitaireUiState.copy(selectedSourcePileId = selectedPileId)
                        renderAndBind()
                    },
                    onDragPreviewChanged = { dragPreview ->
                        if (moveAnimationRunning || solitaireBoardRenderer.isModalOverlayVisible()) return@bind
                        solitaireBoardRenderer.renderDragPreview(dragPreview)
                    },
                    onDragDropAttempt = { dragDropAttempt ->
                        if (moveAnimationRunning || solitaireBoardRenderer.isModalOverlayVisible()) return@bind
                        solitaireBoardRenderer.clearDragPreviewGhost(retainSourceHidden = true)
                        val hasDropTarget =
                            dragDropAttempt.destinationPileId != null &&
                                dragDropAttempt.destinationPileId != dragDropAttempt.sourcePileId
                        val stateAfterDrop = if (hasDropTarget) {
                            solitaireGameStore.dispatchIntent(
                                UiIntent.DragMove(
                                    sourcePileId = dragDropAttempt.sourcePileId,
                                    destinationPileId = requireNotNull(dragDropAttempt.destinationPileId),
                                    cardCount = dragDropAttempt.cardCount,
                                ),
                            )
                        } else {
                            solitaireUiState.copy(wasLastMoveAccepted = false)
                        }
                        val pileIdForAnimationTarget = if (stateAfterDrop.wasLastMoveAccepted) {
                            requireNotNull(dragDropAttempt.destinationPileId)
                        } else {
                            dragDropAttempt.sourcePileId
                        }
                        val animationTargetBounds =
                            latestRenderedBoard.interaction.pileHitRegionsByPileId[pileIdForAnimationTarget]?.bounds
                        if (animationTargetBounds == null) {
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
                        val animationEndY = expectedTopY ?: animationTargetBounds.y
                        moveAnimationRunning = true
                        launchImmediately(coroutineContext) {
                            solitaireBoardRenderer.animateCardTravel(
                                card = dragDropAttempt.card,
                                startX = dragDropAttempt.dropX,
                                startY = dragDropAttempt.dropY,
                                endX = animationTargetBounds.x,
                                endY = animationEndY,
                                isMoveAccepted = stateAfterDrop.wasLastMoveAccepted,
                            )
                            moveAnimationRunning = false
                            solitaireUiState = stateAfterDrop.copy(selectedSourcePileId = null)
                            invalidMoveOverlay.alpha = if (stateAfterDrop.wasLastMoveAccepted) 0.0 else 0.22
                            renderAndBind()
                        }
                    },
                    onFoundationDoubleTapAttempt = { tapStageX, tapStageY, live: DraggableCardInteractionTarget ->
                        if (moveAnimationRunning || solitaireBoardRenderer.isModalOverlayVisible()) return@bind
                        val destinationPileId = "foundation-${live.card.suit.name.lowercase()}"
                        solitaireBoardRenderer.renderDragPreview(
                            BoardDragPreview(
                                sourcePileId = live.pileId,
                                sourceCardCount = 1,
                                stackCards = listOf(live.card),
                                x = tapStageX,
                                y = tapStageY,
                            ),
                        )
                        solitaireBoardRenderer.clearDragPreviewGhost(retainSourceHidden = true)
                        val stateAfterDrop = solitaireGameStore.dispatchIntent(
                            UiIntent.DragMove(
                                sourcePileId = live.pileId,
                                destinationPileId = destinationPileId,
                                cardCount = 1,
                            ),
                        )
                        if (!stateAfterDrop.wasLastMoveAccepted) {
                            println(
                                "[$MOVE_ANIMATION_LOG_TAG] foundationDoubleTapRejected " +
                                    "source=${live.pileId} dest=$destinationPileId",
                            )
                            solitaireBoardRenderer.clearDragPreviewGhost(retainSourceHidden = false)
                            solitaireUiState = stateAfterDrop.copy(selectedSourcePileId = null)
                            invalidMoveOverlay.alpha = 0.22
                            renderAndBind()
                            return@bind
                        }
                        val pileIdForAnimationTarget = destinationPileId
                        val animationTargetBounds =
                            latestRenderedBoard.interaction.pileHitRegionsByPileId[pileIdForAnimationTarget]?.bounds
                        if (animationTargetBounds == null) {
                            solitaireBoardRenderer.clearDragPreviewGhost(retainSourceHidden = false)
                            solitaireUiState = stateAfterDrop.copy(selectedSourcePileId = null)
                            invalidMoveOverlay.alpha = 0.0
                            renderAndBind()
                            return@bind
                        }
                        println(
                            "[$MOVE_ANIMATION_LOG_TAG] foundationDoubleTapAccepted " +
                                "source=${live.pileId} target=$pileIdForAnimationTarget",
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
                        val animationEndY = expectedTopY ?: animationTargetBounds.y
                        moveAnimationRunning = true
                        launchImmediately(coroutineContext) {
                            solitaireBoardRenderer.animateCardTravel(
                                card = live.card,
                                startX = tapStageX,
                                startY = tapStageY,
                                endX = animationTargetBounds.x,
                                endY = animationEndY,
                                isMoveAccepted = true,
                            )
                            moveAnimationRunning = false
                            solitaireUiState = stateAfterDrop.copy(selectedSourcePileId = null)
                            invalidMoveOverlay.alpha = 0.0
                            renderAndBind()
                        }
                    },
                    onUndo = {
                        if (moveAnimationRunning || solitaireBoardRenderer.isModalOverlayVisible()) return@bind
                        solitaireUiState = solitaireGameStore.undo()
                        solitaireUiState = solitaireUiState.copy(selectedSourcePileId = null)
                        renderAndBind()
                    },
                    onRedo = {
                        if (moveAnimationRunning || solitaireBoardRenderer.isModalOverlayVisible()) return@bind
                        solitaireUiState = solitaireGameStore.redo()
                        solitaireUiState = solitaireUiState.copy(selectedSourcePileId = null)
                        renderAndBind()
                    },
                    onRequestNewGameConfirmation = lambda@{
                        if (moveAnimationRunning) return@lambda
                        solitaireBoardRenderer.setModalOverlayVisible(true)
                    },
                )
            }

            renderAndBind()

            launchImmediately(coroutineContext) {
                suspend fun loadPip(path: String) = try {
                    resources.root[path].readBitmap()
                } catch (throwable: Throwable) {
                    println(
                        "[$SIMPLE_SUIT_PIP_LOG_TAG] loadFailed path=$path " +
                            "message=${throwable.message} cause=${throwable::class.simpleName}",
                    )
                    null
                }

                suspend fun loadPipNormalized(path: String) =
                    loadPip(path)?.let(SuitPipBitmapNormalize::clampTextureLongestEdge)

                val heartPip = loadPipNormalized("debug/fox_queen_style_heart_suit_pip.png")
                val diamondPip = loadPipNormalized("debug/simple_diamond_suit_pip.png")
                val spadePip = loadPipNormalized("debug/spade_pip.png")
                val clubPip = loadPipNormalized("debug/simple_club_suit_pip.png")
                solitaireBoardRenderer.setSimpleSuitPipBitmaps(heartPip, diamondPip, spadePip, clubPip)
                renderAndBind(bindInput = true)
            }

            launchImmediately(coroutineContext) {
                val sheetPath = "debug/fox_spade_puppet_sheet.png"
                runCatching {
                    val sheetBitmap = resources.root[sheetPath].readBitmap()
                    if (sheetBitmap.width != FoxPuppetSheetLayout.DEFAULT_SHEET_WIDTH_PX ||
                        sheetBitmap.height != FoxPuppetSheetLayout.DEFAULT_SHEET_HEIGHT_PX
                    ) {
                        println(
                            "[${FoxSpadePuppetSheet.logTag}] sheetSizeExpected=" +
                                "${FoxPuppetSheetLayout.DEFAULT_SHEET_WIDTH_PX}x${FoxPuppetSheetLayout.DEFAULT_SHEET_HEIGHT_PX} " +
                                "actual=${sheetBitmap.width}x${sheetBitmap.height}",
                        )
                    }
                    val slices = FoxSpadePuppetSheet.buildSlices(sheetBitmap)
                    solitaireBoardRenderer.setFoxSpadePuppetSlices(slices)
                    renderAndBind(bindInput = true)
                }.onFailure { throwable ->
                    println(
                        "[${FoxSpadePuppetSheet.logTag}] sheetLoadFailed path=$sheetPath " +
                            "message=${throwable.message} cause=${throwable::class.simpleName}",
                    )
                }
            }

            launchImmediately(coroutineContext) {
                val heartPath = "debug/fox_heart_puppet_sheet.png"
                runCatching {
                    val sheetBitmap = resources.root[heartPath].readBitmap()
                    if (sheetBitmap.width != FoxPuppetSheetLayout.DEFAULT_SHEET_WIDTH_PX ||
                        sheetBitmap.height != FoxPuppetSheetLayout.DEFAULT_SHEET_HEIGHT_PX
                    ) {
                        println(
                            "[${FoxHeartPuppetSheet.logTag}] sheetSizeExpected=" +
                                "${FoxPuppetSheetLayout.DEFAULT_SHEET_WIDTH_PX}x${FoxPuppetSheetLayout.DEFAULT_SHEET_HEIGHT_PX} " +
                                "actual=${sheetBitmap.width}x${sheetBitmap.height}",
                        )
                    }
                    val slices = FoxHeartPuppetSheet.buildSlices(sheetBitmap)
                    solitaireBoardRenderer.setFoxHeartPuppetSlices(slices)
                    renderAndBind(bindInput = true)
                }.onFailure { throwable ->
                    println(
                        "[${FoxHeartPuppetSheet.logTag}] sheetLoadFailed path=$heartPath " +
                            "message=${throwable.message} cause=${throwable::class.simpleName}",
                    )
                }
            }
        }
    }
}
