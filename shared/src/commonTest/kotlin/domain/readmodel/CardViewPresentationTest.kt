package domain.readmodel

import domain.model.CardColor
import domain.model.Rank
import domain.model.Suit
import kotlin.test.Test
import kotlin.test.assertEquals

class CardViewPresentationTest {
    @Test
    fun rankAbbrev_mapsRanksForFaceUp() {
        val card = CardViewModel(
            suit = Suit.DIAMONDS,
            face = CardFace.Up(rank = Rank.QUEEN),
            color = CardColor.RED,
        )
        assertEquals("Q", card.rankAbbrev())
    }
}
