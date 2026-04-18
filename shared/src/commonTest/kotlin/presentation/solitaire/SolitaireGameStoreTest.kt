package presentation.solitaire

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SolitaireGameStoreTest {
    @Test
    fun start_initializesRenderState() {
        val solitaireGameStore = SolitaireGameStore(initialSeed = 555L)
        val startedState = solitaireGameStore.start()
        assertNotNull(startedState.renderModel)
        assertTrue(startedState.wasLastMoveAccepted)
        assertTrue(startedState.cardTheme == CardTheme.REGAL_ANIMALS)
    }

    @Test
    fun invalidIntent_updatesFeedbackState() {
        val solitaireGameStore = SolitaireGameStore(initialSeed = 777L)
        solitaireGameStore.start()
        val updatedState = solitaireGameStore.dispatchIntent(UiIntent.Recycle)
        assertFalse(updatedState.wasLastMoveAccepted)
        assertNotNull(updatedState.lastRejectionReason)
    }

    @Test
    fun undo_beforeStart_isNoOpBecauseNoSessionYet() {
        val solitaireGameStore = SolitaireGameStore(initialSeed = 1L)
        val afterUndo = solitaireGameStore.undo()
        assertNull(afterUndo.renderModel)
        assertEquals("Ready", afterUndo.statusMessage)
        assertTrue(afterUndo.wasLastMoveAccepted)
    }

    @Test
    fun snapshot_reflectsLatestDispatch() {
        val solitaireGameStore = SolitaireGameStore(initialSeed = 2L)
        solitaireGameStore.start()
        solitaireGameStore.dispatchIntent(UiIntent.Recycle)
        val snap = solitaireGameStore.snapshot()
        assertFalse(snap.wasLastMoveAccepted)
        assertNotNull(snap.lastRejectionReason)
    }
}
