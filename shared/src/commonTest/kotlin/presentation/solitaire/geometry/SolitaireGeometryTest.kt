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

class SolitaireGeometryTest {
    @Test
    fun axisAlignedRect_containsPoint_respectsInclusiveBounds() {
        val rect = AxisAlignedRect(x = 10.0, y = 20.0, width = 30.0, height = 40.0)
        assertTrue(rect.containsPoint(10.0, 20.0))
        assertTrue(rect.containsPoint(40.0, 60.0))
        assertFalse(rect.containsPoint(9.9, 20.0))
        assertFalse(rect.containsPoint(10.0, 60.1))
    }

    @Test
    fun boardLayout_create_exposesFourFoundationsAndSevenTableauAnchors() {
        val layout = BoardLayout.create(viewportWidth = 800.0, viewportHeight = 600.0)
        assertEquals(4, layout.foundationPiles.size)
        assertEquals(7, layout.tableauPiles.size)
        assertTrue(layout.stockPile.x >= 0.0)
        assertTrue(layout.wastePile.x > layout.stockPile.x)
    }

    @Test
    fun calculateTableauHitHeight_singleCardIsJustCardHeight() {
        assertEquals(120.0, calculateTableauHitHeight(cardHeight = 120.0, tableauCardOffsetY = 18.0, cardCount = 1))
    }

    @Test
    fun calculateTableauHitHeight_stacksFaceDownOffsets() {
        assertEquals(
            120.0 + 5 * 18.0,
            calculateTableauHitHeight(cardHeight = 120.0, tableauCardOffsetY = 18.0, cardCount = 6),
        )
    }

    @Test
    fun isHiddenCard_matchesFaceDown() {
        val faceUp = CardViewModel(Suit.HEARTS, CardFace.Up(Rank.ACE), CardColor.RED)
        val faceDown = CardViewModel(Suit.SPADES, CardFace.Down, CardColor.BLACK)
        assertFalse(isHiddenCard(faceUp))
        assertTrue(isHiddenCard(faceDown))
    }

    @Test
    fun foundationSuitsVisualLeftToRight_matchesKlondikeChromeOrder() {
        assertEquals(
            listOf(Suit.HEARTS, Suit.DIAMONDS, Suit.SPADES, Suit.CLUBS),
            foundationSuitsVisualLeftToRight,
        )
    }
}
