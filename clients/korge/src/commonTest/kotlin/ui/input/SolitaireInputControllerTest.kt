package ui.input

import ui.adapter.UiIntent
import ui.render.PileHitArea
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SolitaireInputControllerTest {
    @Test
    fun resolveTapSelection_followsSelectToggleAndMoveFlow() {
        val firstTap = resolveTapSelection(
            currentSelectedSourcePileId = null,
            tappedPileId = "tableau-2",
        )
        assertEquals("tableau-2", firstTap.nextSelectedSourcePileId)
        assertNull(firstTap.moveIntent)

        val secondTapSamePile = resolveTapSelection(
            currentSelectedSourcePileId = firstTap.nextSelectedSourcePileId,
            tappedPileId = "tableau-2",
        )
        assertNull(secondTapSamePile.nextSelectedSourcePileId)
        assertNull(secondTapSamePile.moveIntent)

        val secondTapDifferentPile = resolveTapSelection(
            currentSelectedSourcePileId = "waste",
            tappedPileId = "foundation-hearts",
        )
        assertNull(secondTapDifferentPile.nextSelectedSourcePileId)
        assertEquals(
            UiIntent.DragMove(
                sourcePileId = "waste",
                destinationPileId = "foundation-hearts",
                cardCount = 1,
            ),
            secondTapDifferentPile.moveIntent,
        )
    }

    @Test
    fun resolvePileIdAtPoint_usesProvidedHitAreas() {
        val pileHitAreas = listOf(
            PileHitArea(
                pileId = "tableau-0",
                x = 100.0,
                y = 200.0,
                width = 96.0,
                height = 228.0,
            ),
            PileHitArea(
                pileId = "foundation-clubs",
                x = 420.0,
                y = 60.0,
                width = 96.0,
                height = 132.0,
            ),
        )

        assertEquals(
            "tableau-0",
            resolvePileIdAtPoint(
                x = 160.0,
                y = 390.0,
                pileHitAreas = pileHitAreas,
            ),
        )
        assertEquals(
            "foundation-clubs",
            resolvePileIdAtPoint(
                x = 455.0,
                y = 120.0,
                pileHitAreas = pileHitAreas,
            ),
        )
        assertNull(
            resolvePileIdAtPoint(
                x = 40.0,
                y = 40.0,
                pileHitAreas = pileHitAreas,
            ),
        )
    }
}
