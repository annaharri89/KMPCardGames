package ui.adapter

import domain.model.GameVariant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DomainUiMapperTest {
    @Test
    fun startGameSession_returnsSolitaireRenderModel() {
        val domainUiMapper = DomainUiMapper()
        val renderModel = domainUiMapper.startGameSession(
            variant = GameVariant.SOLITAIRE,
            seed = 99L,
        )
        assertEquals(GameVariant.SOLITAIRE, renderModel.variant)
        assertEquals(7, renderModel.tableauPiles.size)
    }

    @Test
    fun applyUiIntent_draw_isAcceptedAfterStart() {
        val domainUiMapper = DomainUiMapper()
        domainUiMapper.startGameSession(
            variant = GameVariant.SOLITAIRE,
            seed = 123L,
        )
        val dispatchResult = domainUiMapper.applyUiIntent(UiIntent.Draw)
        assertTrue(dispatchResult.wasAccepted)
        assertNotNull(dispatchResult.renderModel)
    }

    @Test
    fun applyUiIntent_recycleBeforeWaste_isRejected() {
        val domainUiMapper = DomainUiMapper()
        domainUiMapper.startGameSession(
            variant = GameVariant.SOLITAIRE,
            seed = 888L,
        )
        val dispatchResult = domainUiMapper.applyUiIntent(UiIntent.Recycle)
        assertFalse(dispatchResult.wasAccepted)
        assertNotNull(dispatchResult.rejectionReason)
    }
}
