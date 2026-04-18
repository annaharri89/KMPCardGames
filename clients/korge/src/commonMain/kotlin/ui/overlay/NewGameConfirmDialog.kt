package ui.overlay

import korlibs.image.color.Colors
import korlibs.korge.input.onClick
import korlibs.korge.view.Container
import korlibs.korge.view.addTo
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import kotlin.math.min

/**
 * One-shot modal content for the solitaire HUD: dimmer, copy, Cancel / confirm actions.
 * Install into [SolitaireBoardRenderer.modalOverlayRoot] exactly once.
 */
object NewGameConfirmDialog {
    private const val DIALOG_LOG_TAG = "NewGameConfirmDialog"

    fun install(
        modalLayer: Container,
        stageWidth: Double,
        stageHeight: Double,
        onUserConfirmedNewGame: () -> Unit,
        onUserCancelledNewGame: () -> Unit,
    ) {
        require(modalLayer.numChildren == 0) {
            "[$DIALOG_LOG_TAG] modal layer must be empty before install (numChildren=${modalLayer.numChildren})"
        }
        modalLayer.solidRect(stageWidth, stageHeight, Colors["#050810"]) {
            alpha = 0.72
            onClick { }
        }
        val panelWidth = min(500.0, stageWidth * 0.88)
        val panelHeight = 176.0
        val panelLeft = (stageWidth - panelWidth) / 2.0
        val panelTop = (stageHeight - panelHeight) / 2.0
        val panel = Container().addTo(modalLayer).apply {
            x = panelLeft
            y = panelTop
        }
        panel.solidRect(panelWidth, panelHeight, Colors["#252a3d"]) {
            onClick { }
        }
        panel.text(
            text = "Start a new game?",
            textSize = 22.0,
            color = Colors.WHITE,
        ) {
            x = 22.0
            y = 20.0
        }
        panel.text(
            text = "Your current deal and undo history will be discarded.",
            textSize = 14.0,
            color = Colors["#c5cee0"],
        ) {
            x = 22.0
            y = 52.0
        }
        panel.text("Cancel", textSize = 19.0, color = Colors["#aebfe6"]) {
            x = 22.0
            y = panelHeight - 44.0
            onClick { onUserCancelledNewGame() }
        }
        panel.text("Start new game", textSize = 19.0, color = Colors["#8ae8ff"]) {
            x = panelWidth - 178.0
            y = panelHeight - 44.0
            onClick { onUserConfirmedNewGame() }
        }
    }
}
