package domain

import domain.model.GameVariant
import domain.setup.GameStateFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameStateFactoryTest {
    private val gameStateFactory = GameStateFactory()

    @Test
    fun solitaireInitialDeal_hasExpectedPileSizes() {
        val state = gameStateFactory.createInitialState(
            variant = GameVariant.SOLITAIRE,
            seed = 1L,
        )
        val tableauCardCount = state.tableauColumns.sumOf { it.hiddenCards.size + it.visibleCards.size }
        assertEquals(28, tableauCardCount)
        assertEquals(24, state.stockPile.size)
        assertTrue(state.wastePile.isEmpty())
    }

    @Test
    fun freeCellInitialDeal_hasExpectedColumnSizes() {
        val state = gameStateFactory.createInitialState(
            variant = GameVariant.FREECELL,
            seed = 2L,
        )
        val columnSizes = state.tableauColumns.map { it.visibleCards.size }
        assertEquals(listOf(7, 7, 7, 7, 6, 6, 6, 6), columnSizes)
        assertEquals(4, state.freeCells.size)
        assertTrue(state.stockPile.isEmpty())
    }
}
