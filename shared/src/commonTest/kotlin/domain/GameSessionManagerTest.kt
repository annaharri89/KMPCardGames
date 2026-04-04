package domain

import domain.action.GameAction
import domain.model.GameVariant
import domain.rules.freecell.FreeCellRuleSet
import domain.rules.solitaire.SolitaireRuleSet
import domain.session.GameSessionManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameSessionManagerTest {
    private val gameSessionManager = GameSessionManager(
        ruleSets = setOf(SolitaireRuleSet(), FreeCellRuleSet()),
    )

    @Test
    fun sameSeed_producesSameInitialState() {
        val firstState = gameSessionManager.startNewSession(
            variant = GameVariant.SOLITAIRE,
            seed = 2026L,
        )
        val secondState = gameSessionManager.startNewSession(
            variant = GameVariant.SOLITAIRE,
            seed = 2026L,
        )
        assertEquals(firstState, secondState)
    }

    @Test
    fun undoRedo_roundTripRestoresState() {
        val startedState = gameSessionManager.startNewSession(
            variant = GameVariant.SOLITAIRE,
            seed = 90210L,
        )
        val dispatchResult = gameSessionManager.dispatch(GameAction.DrawFromStock)
        assertTrue(dispatchResult.wasAccepted)
        val stateAfterUndo = gameSessionManager.undo()
        assertEquals(startedState, stateAfterUndo)
        val stateAfterRedo = gameSessionManager.redo()
        assertEquals(dispatchResult.state, stateAfterRedo)
    }
}
