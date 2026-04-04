package domain.session

import domain.action.GameAction
import domain.model.GameState
import domain.model.GameVariant
import domain.reducer.DeterministicStateReducer
import domain.result.ActionResult
import domain.rules.RuleSet
import domain.setup.GameStateFactory

class GameSessionManager(
    private val gameStateFactory: GameStateFactory = GameStateFactory(),
    ruleSets: Set<RuleSet>,
) {
    private val deterministicStateReducer = DeterministicStateReducer(ruleSets = ruleSets)

    private var activeState: GameState? = null
    private val undoHistoryStack = ArrayDeque<GameState>()
    private val redoHistoryStack = ArrayDeque<GameState>()

    fun startNewSession(
        variant: GameVariant,
        seed: Long,
    ): GameState {
        val initialState = gameStateFactory.createInitialState(variant = variant, seed = seed)
        activeState = initialState
        undoHistoryStack.clear()
        redoHistoryStack.clear()
        return initialState
    }

    fun currentState(): GameState = requireNotNull(activeState) {
        "Session has not started. Call startNewSession first."
    }

    fun dispatch(action: GameAction): ActionResult {
        val existingState = currentState()
        val actionResult = deterministicStateReducer.reduce(
            state = existingState,
            action = action,
        )
        if (actionResult.wasAccepted && actionResult.state != existingState) {
            undoHistoryStack.addLast(existingState)
            redoHistoryStack.clear()
            activeState = actionResult.state
        }
        return actionResult
    }

    fun canUndo(): Boolean = undoHistoryStack.isNotEmpty()

    fun undo(): GameState {
        check(canUndo()) { "No undo history available." }
        val previousState = undoHistoryStack.removeLast()
        redoHistoryStack.addLast(currentState())
        activeState = previousState
        return previousState
    }

    fun canRedo(): Boolean = redoHistoryStack.isNotEmpty()

    fun redo(): GameState {
        check(canRedo()) { "No redo history available." }
        val nextState = redoHistoryStack.removeLast()
        undoHistoryStack.addLast(currentState())
        activeState = nextState
        return nextState
    }
}
