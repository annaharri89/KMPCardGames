package app

import domain.model.GameVariant
import domain.readmodel.GameRenderModel
import domain.result.RejectionReason
import ui.adapter.DomainUiMapper
import ui.adapter.UiIntent

data class SolitaireUiState(
    val renderModel: GameRenderModel?,
    val statusMessage: String,
    val wasLastMoveAccepted: Boolean,
    val lastRejectionReason: RejectionReason?,
    val hasWon: Boolean,
    val selectedSourcePileId: String?,
    val cardTheme: CardTheme,
)

/**
 * Mutable, UI-oriented snapshot for the solitaire screen: what to draw, what to tell the player,
 * and light interaction state (selection, theme).
 *
 * All rule enforcement and undo stacks live in the `shared` module behind [ui.adapter.DomainUiMapper].
 * This store wires KorGE to that layer: [start] opens a new Klondike game with [initialSeed],
 * [dispatchIntent] forwards taps/drags as [UiIntent]s and copies back the latest [GameRenderModel],
 * and it updates [SolitaireUiState.statusMessage], [SolitaireUiState.wasLastMoveAccepted], and
 * [SolitaireUiState.hasWon] so the scene can show feedback without parsing domain errors.
 *
 * [undo] and [redo] call the mapper and, when there is nothing to undo/redo, keep the prior model
 * and set a short status message instead of propagating an exception to the UI. [snapshot] returns
 * the current [SolitaireUiState] for readers that should not trigger side effects.
 */
class SolitaireGameStore(
    private val domainUiMapper: DomainUiMapper = DomainUiMapper(),
    // TODO: Default to a random seed per new game (keep injectable seed for tests/replays).
    private val initialSeed: Long = 20260320L,
) {
    private var currentState: SolitaireUiState = SolitaireUiState(
        renderModel = null,
        statusMessage = "Ready",
        wasLastMoveAccepted = true,
        lastRejectionReason = null,
        hasWon = false,
        selectedSourcePileId = null,
        cardTheme = CardTheme.KAWAII_NATURE,
    )

    fun start(): SolitaireUiState {
        val renderModel = domainUiMapper.startGameSession(
            variant = GameVariant.SOLITAIRE,
            seed = initialSeed,
        )
        currentState = currentState.copy(
            renderModel = renderModel,
            statusMessage = "Game started",
            wasLastMoveAccepted = true,
            lastRejectionReason = null,
            hasWon = hasWon(renderModel),
        )
        return currentState
    }

    fun dispatchIntent(uiIntent: UiIntent): SolitaireUiState {
        val dispatchResult = domainUiMapper.applyUiIntent(uiIntent)
        val latestRenderModel = dispatchResult.renderModel
        val latestStatusMessage = if (dispatchResult.wasAccepted) {
            "Move accepted"
        } else {
            "Move rejected: ${dispatchResult.rejectionReason ?: RejectionReason.RULE_VIOLATION}"
        }
        currentState = currentState.copy(
            renderModel = latestRenderModel,
            statusMessage = latestStatusMessage,
            wasLastMoveAccepted = dispatchResult.wasAccepted,
            lastRejectionReason = dispatchResult.rejectionReason,
            hasWon = hasWon(latestRenderModel),
        )
        return currentState
    }

    fun undo(): SolitaireUiState {
        val currentRenderModel = currentState.renderModel ?: return currentState
        currentState = try {
            val renderModel = domainUiMapper.undo()
            currentState.copy(
                renderModel = renderModel,
                statusMessage = "Undo",
                wasLastMoveAccepted = true,
                lastRejectionReason = null,
                hasWon = hasWon(renderModel),
            )
        } catch (_: IllegalStateException) {
            currentState.copy(
                renderModel = currentRenderModel,
                statusMessage = "Nothing to undo",
                wasLastMoveAccepted = false,
                lastRejectionReason = RejectionReason.RULE_VIOLATION,
                hasWon = hasWon(currentRenderModel),
            )
        }
        return currentState
    }

    fun redo(): SolitaireUiState {
        val currentRenderModel = currentState.renderModel ?: return currentState
        currentState = try {
            val renderModel = domainUiMapper.redo()
            currentState.copy(
                renderModel = renderModel,
                statusMessage = "Redo",
                wasLastMoveAccepted = true,
                lastRejectionReason = null,
                hasWon = hasWon(renderModel),
            )
        } catch (_: IllegalStateException) {
            currentState.copy(
                renderModel = currentRenderModel,
                statusMessage = "Nothing to redo",
                wasLastMoveAccepted = false,
                lastRejectionReason = RejectionReason.RULE_VIOLATION,
                hasWon = hasWon(currentRenderModel),
            )
        }
        return currentState
    }

    fun snapshot(): SolitaireUiState = currentState

    private fun hasWon(renderModel: GameRenderModel): Boolean {
        return renderModel.foundationPiles.sumOf { it.cards.size } == 52
    }
}
