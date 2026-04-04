package domain

import domain.action.GameAction
import domain.action.PileRef
import domain.model.GameState
import domain.model.GameVariant
import domain.model.Rank
import domain.model.Suit
import domain.model.TableauColumn
import domain.rules.freecell.FreeCellRuleSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FreeCellRuleSetTest {
    private val freeCellRuleSet = FreeCellRuleSet()

    @Test
    fun moveCardToFreeCell_isAccepted() {
        val state = baseFreeCellState(
            tableauColumns = listOf(
                TableauColumn(hiddenCards = emptyList(), visibleCards = listOf(card(Suit.SPADES, Rank.KING))),
            ) + List(7) { TableauColumn(hiddenCards = emptyList(), visibleCards = emptyList()) },
        )
        val result = freeCellRuleSet.apply(
            action = GameAction.MoveCards(
                from = PileRef.Tableau(0),
                to = PileRef.FreeCell(0),
            ),
            state = state,
        )
        assertTrue(result.wasAccepted)
        assertEquals(Rank.KING, result.state.freeCells[0]?.rank)
    }

    @Test
    fun moveCardFromFreeCellToFoundation_isAccepted() {
        val state = baseFreeCellState(
            freeCells = listOf(card(Suit.HEARTS, Rank.ACE), null, null, null),
        )
        val result = freeCellRuleSet.apply(
            action = GameAction.MoveCards(
                from = PileRef.FreeCell(0),
                to = PileRef.Foundation(Suit.HEARTS),
            ),
            state = state,
        )
        assertTrue(result.wasAccepted)
        assertNull(result.state.freeCells[0])
        assertEquals(1, result.state.foundationPiles[Suit.HEARTS]?.size)
    }

    @Test
    fun invalidMoveToOccupiedFreeCell_isRejected() {
        val state = baseFreeCellState(
            freeCells = listOf(card(Suit.CLUBS, Rank.ACE), null, null, null),
            tableauColumns = listOf(
                TableauColumn(hiddenCards = emptyList(), visibleCards = listOf(card(Suit.SPADES, Rank.TWO))),
            ) + List(7) { TableauColumn(hiddenCards = emptyList(), visibleCards = emptyList()) },
        )
        val validation = freeCellRuleSet.validate(
            action = GameAction.MoveCards(
                from = PileRef.Tableau(0),
                to = PileRef.FreeCell(0),
            ),
            state = state,
        )
        assertFalse(validation.isValid)
    }

    @Test
    fun autoMove_movesAceToFoundation() {
        val state = baseFreeCellState(
            freeCells = listOf(card(Suit.DIAMONDS, Rank.ACE), null, null, null),
        )
        val result = freeCellRuleSet.apply(GameAction.AutoMoveEligibleCardsToFoundation, state)
        assertTrue(result.wasAccepted)
        assertEquals(1, result.state.foundationPiles[Suit.DIAMONDS]?.size)
    }

    private fun baseFreeCellState(
        tableauColumns: List<TableauColumn> = List(8) {
            TableauColumn(hiddenCards = emptyList(), visibleCards = emptyList())
        },
        freeCells: List<domain.model.Card?> = List(4) { null },
    ): GameState = GameState(
        variant = GameVariant.FREECELL,
        tableauColumns = tableauColumns,
        foundationPiles = Suit.entries.associateWith { emptyList() },
        freeCells = freeCells,
        initialSeed = 24L,
    )
}
