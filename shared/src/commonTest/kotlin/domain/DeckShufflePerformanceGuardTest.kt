package domain

import domain.setup.SeededDeckShuffleService
import domain.setup.StandardDeckFactory
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

class DeckShufflePerformanceGuardTest {
    private val seededDeckShuffleService = SeededDeckShuffleService()

    @Test
    fun shuffleBatch_staysUnderBudget() {
        val orderedDeck = StandardDeckFactory.createOrderedDeck()
        val startMark = TimeSource.Monotonic.markNow()
        repeat(2_000) { iteration ->
            seededDeckShuffleService.shuffle(
                cards = orderedDeck,
                seed = iteration.toLong(),
            )
        }
        val elapsedDuration = startMark.elapsedNow()
        assertTrue(
            actual = elapsedDuration < 1_500.milliseconds,
            message = "Expected shuffle batch under budget but was $elapsedDuration",
        )
    }
}
