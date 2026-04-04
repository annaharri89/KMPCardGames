package domain

import domain.action.GameAction
import domain.action.PileRef
import domain.model.GameState
import domain.model.GameVariant
import domain.model.Rank
import domain.model.Suit
import domain.model.TableauColumn
import domain.rules.solitaire.SolitaireRuleSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SolitaireRuleSetTest {
    private val solitaireRuleSet = SolitaireRuleSet()

    @Test
    fun drawFromStock_movesCardToWaste() {
        val state = baseSolitaireState(
            stockCards = listOf(card(Suit.CLUBS, Rank.KING)),
        )
        val result = solitaireRuleSet.apply(GameAction.DrawFromStock, state)
        assertTrue(result.wasAccepted)
        assertTrue(result.state.stockPile.isEmpty())
        assertEquals(card(Suit.CLUBS, Rank.KING), result.state.wastePile.last())
    }

    @Test
    fun moveWasteAceToFoundation_isAccepted() {
        val state = baseSolitaireState(
            wasteCards = listOf(card(Suit.HEARTS, Rank.ACE)),
        )
        val result = solitaireRuleSet.apply(
            action = GameAction.MoveCards(
                from = PileRef.Waste,
                to = PileRef.Foundation(Suit.HEARTS),
                cardCount = 1,
            ),
            state = state,
        )
        assertTrue(result.wasAccepted)
        assertTrue(result.state.wastePile.isEmpty())
        assertEquals(Rank.ACE, result.state.foundationPiles[Suit.HEARTS]?.single()?.rank)
    }

    @Test
    fun invalidMoveFromWasteToFoundationWithoutAce_isRejected() {
        val state = baseSolitaireState(
            wasteCards = listOf(card(Suit.HEARTS, Rank.TWO)),
        )
        val validation = solitaireRuleSet.validate(
            action = GameAction.MoveCards(
                from = PileRef.Waste,
                to = PileRef.Foundation(Suit.HEARTS),
            ),
            state = state,
        )
        assertFalse(validation.isValid)
    }

    @Test
    fun moveSequenceFromTableauToTableau_promotesHiddenCard() {
        val sourceColumn = TableauColumn(
            hiddenCards = listOf(card(Suit.CLUBS, Rank.ACE)),
            visibleCards = listOf(card(Suit.HEARTS, Rank.SIX)),
        )
        val destinationColumn = TableauColumn(
            hiddenCards = emptyList(),
            visibleCards = listOf(card(Suit.SPADES, Rank.SEVEN)),
        )
        val state = baseSolitaireState(
            tableauColumns = listOf(sourceColumn, destinationColumn) + List(5) {
                TableauColumn(hiddenCards = emptyList(), visibleCards = emptyList())
            },
        )
        val result = solitaireRuleSet.apply(
            action = GameAction.MoveCards(
                from = PileRef.Tableau(0),
                to = PileRef.Tableau(1),
            ),
            state = state,
        )
        assertTrue(result.wasAccepted)
        assertEquals(Rank.ACE, result.state.tableauColumns[0].visibleCards.single().rank)
        assertNull(result.state.tableauColumns[0].hiddenCards.lastOrNull())
    }

    @Test
    fun autoMove_movesEligibleCardToFoundation() {
        val state = baseSolitaireState(
            wasteCards = listOf(card(Suit.DIAMONDS, Rank.ACE)),
        )
        val result = solitaireRuleSet.apply(GameAction.AutoMoveEligibleCardsToFoundation, state)
        assertTrue(result.wasAccepted)
        assertEquals(1, result.state.foundationPiles[Suit.DIAMONDS]?.size)
    }

    private fun baseSolitaireState(
        tableauColumns: List<TableauColumn> = List(7) {
            TableauColumn(hiddenCards = emptyList(), visibleCards = emptyList())
        },
        stockCards: List<domain.model.Card> = emptyList(),
        wasteCards: List<domain.model.Card> = emptyList(),
    ): GameState = GameState(
        variant = GameVariant.SOLITAIRE,
        tableauColumns = tableauColumns,
        foundationPiles = Suit.entries.associateWith { emptyList() },
        stockPile = stockCards,
        wastePile = wasteCards,
        initialSeed = 42L,
    )
}
