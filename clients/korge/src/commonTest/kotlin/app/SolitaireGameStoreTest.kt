package app

import ui.adapter.UiIntent
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SolitaireGameStoreTest {
    @Test
    fun start_initializesRenderState() {
        val solitaireGameStore = SolitaireGameStore(initialSeed = 555L)
        val startedState = solitaireGameStore.start()
        assertNotNull(startedState.renderModel)
        assertTrue(startedState.wasLastMoveAccepted)
        assertTrue(startedState.cardTheme == CardTheme.KAWAII_NATURE)
    }

    @Test
    fun invalidIntent_updatesFeedbackState() {
        val solitaireGameStore = SolitaireGameStore(initialSeed = 777L)
        solitaireGameStore.start()
        val updatedState = solitaireGameStore.dispatchIntent(UiIntent.Recycle)
        assertFalse(updatedState.wasLastMoveAccepted)
        assertNotNull(updatedState.lastRejectionReason)
    }
}
