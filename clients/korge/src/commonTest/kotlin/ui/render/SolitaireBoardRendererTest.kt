package ui.render

import domain.model.CardColor
import domain.model.Suit
import domain.readmodel.CardViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SolitaireBoardRendererTest {
    @Test
    fun calculateTableauHitHeight_matchesVisibleStackHeight() {
        assertEquals(
            132.0,
            calculateTableauHitHeight(
                cardHeight = 132.0,
                tableauCardOffsetY = 24.0,
                cardCount = 1,
            ),
        )
        assertEquals(
            132.0,
            calculateTableauHitHeight(
                cardHeight = 132.0,
                tableauCardOffsetY = 24.0,
                cardCount = 0,
            ),
        )
        assertEquals(
            204.0,
            calculateTableauHitHeight(
                cardHeight = 132.0,
                tableauCardOffsetY = 24.0,
                cardCount = 4,
            ),
        )
    }

    @Test
    fun isHiddenCard_matchesHiddenMarkerOnly() {
        val hidden = CardViewModel(
            suit = Suit.SPADES,
            rankSymbol = "HIDDEN",
            color = CardColor.BLACK,
        )
        val visible = CardViewModel(
            suit = Suit.HEARTS,
            rankSymbol = "ACE",
            color = CardColor.RED,
        )
        assertTrue(isHiddenCard(hidden))
        assertFalse(isHiddenCard(visible))
    }
}
