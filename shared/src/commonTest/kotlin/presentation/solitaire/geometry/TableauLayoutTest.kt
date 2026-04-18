package presentation.solitaire.geometry

import domain.model.CardColor
import domain.model.Rank
import domain.model.Suit
import domain.readmodel.CardFace
import domain.readmodel.CardViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TableauLayoutTest {
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
    fun isHiddenCard_trueOnlyForCardFaceDown() {
        val hidden = CardViewModel(
            suit = Suit.SPADES,
            face = CardFace.Down,
            color = CardColor.BLACK,
        )
        val visible = CardViewModel(
            suit = Suit.HEARTS,
            face = CardFace.Up(rank = Rank.ACE),
            color = CardColor.RED,
        )
        assertTrue(isHiddenCard(hidden))
        assertFalse(isHiddenCard(visible))
    }
}
