package presentation.solitaire

import kotlin.random.Random
import domain.model.GameVariant
import domain.readmodel.GameRenderModel
import domain.result.RejectionReason

data class SolitaireUiState(
    val renderModel: GameRenderModel?,
    val statusMessage: String,
    val wasLastMoveAccepted: Boolean,
    val lastRejectionReason: RejectionReason?,
    val hasWon: Boolean,
    val selectedSourcePileId: String?,
    val cardTheme: CardTheme,
)

/** Remembers the latest board picture, short status lines, and win flag while the UI talks to [DomainUiMapper]. */
class SolitaireGameStore(
    private val domainUiMapper: DomainUiMapper = DomainUiMapper(),
    /** Non-null: same seed every [start] (tests/replays). Null: a new [Long] from [gameSeedRandom] each [start]. */
    private val initialSeed: Long? = null,
    private val gameSeedRandom: Random = Random.Default,
) {
    private var currentState: SolitaireUiState = SolitaireUiState(
        renderModel = null,
        statusMessage = "Ready",
        wasLastMoveAccepted = true,
        lastRejectionReason = null,
        hasWon = false,
        selectedSourcePileId = null,
        cardTheme = CardTheme.REGAL_ANIMALS,
    )

    fun start(): SolitaireUiState {
        val sessionSeed = initialSeed ?: gameSeedRandom.nextLong()
        val renderModel = domainUiMapper.startGameSession(
            variant = GameVariant.SOLITAIRE,
            seed = sessionSeed,
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
        } catch (_ignored: IllegalStateException) {
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
        } catch (_ignored: IllegalStateException) {
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
