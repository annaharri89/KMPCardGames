package domain.setup

import domain.model.Card
import domain.model.Rank
import domain.model.Suit
import kotlin.random.Random

fun interface DeckShuffleService {
    fun shuffle(
        cards: List<Card>,
        seed: Long,
    ): List<Card>
}

object StandardDeckFactory {
    fun createOrderedDeck(): List<Card> = buildList {
        Suit.entries.forEach { suit ->
            Rank.entries.forEach { rank ->
                add(Card(suit = suit, rank = rank))
            }
        }
    }
}

class SeededDeckShuffleService : DeckShuffleService {
    override fun shuffle(
        cards: List<Card>,
        seed: Long,
    ): List<Card> {
        val mutableCards = cards.toMutableList()
        val seededRandom = Random(seed)
        for (index in mutableCards.lastIndex downTo 1) {
            val swapIndex = seededRandom.nextInt(index + 1)
            val temporaryCard = mutableCards[index]
            mutableCards[index] = mutableCards[swapIndex]
            mutableCards[swapIndex] = temporaryCard
        }
        return mutableCards
    }
}
