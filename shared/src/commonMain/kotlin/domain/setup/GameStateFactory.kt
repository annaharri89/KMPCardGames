package domain.setup

import domain.model.GameState
import domain.model.GameVariant
import domain.model.Suit
import domain.model.TableauColumn

class GameStateFactory(
    private val deckShuffleService: DeckShuffleService = SeededDeckShuffleService(),
) {
    fun createInitialState(
        variant: GameVariant,
        seed: Long,
    ): GameState = when (variant) {
        GameVariant.SOLITAIRE -> createSolitaireState(seed = seed)
        GameVariant.FREECELL -> createFreeCellState(seed = seed)
    }

    private fun createSolitaireState(seed: Long): GameState {
        val shuffledDeck = deckShuffleService.shuffle(StandardDeckFactory.createOrderedDeck(), seed)
        var runningDeckIndex = 0
        val tableauColumns = buildList {
            repeat(7) { columnIndex ->
                val hiddenCardCount = columnIndex
                val visibleCardCount = 1
                val hiddenCards = shuffledDeck.subList(
                    runningDeckIndex,
                    runningDeckIndex + hiddenCardCount,
                )
                runningDeckIndex += hiddenCardCount
                val visibleCards = shuffledDeck.subList(
                    runningDeckIndex,
                    runningDeckIndex + visibleCardCount,
                )
                runningDeckIndex += visibleCardCount
                add(
                    TableauColumn(
                        hiddenCards = hiddenCards,
                        visibleCards = visibleCards,
                    ),
                )
            }
        }

        val stockPile = shuffledDeck.drop(runningDeckIndex)
        return GameState(
            variant = GameVariant.SOLITAIRE,
            tableauColumns = tableauColumns,
            foundationPiles = Suit.entries.associateWith { emptyList() },
            stockPile = stockPile,
            wastePile = emptyList(),
            freeCells = emptyList(),
            initialSeed = seed,
        )
    }

    private fun createFreeCellState(seed: Long): GameState {
        val shuffledDeck = deckShuffleService.shuffle(StandardDeckFactory.createOrderedDeck(), seed)
        val tableauColumns = buildList {
            repeat(8) { columnIndex ->
                val cardsPerColumn = if (columnIndex < 4) 7 else 6
                val startIndex = if (columnIndex < 4) {
                    columnIndex * 7
                } else {
                    28 + ((columnIndex - 4) * 6)
                }
                val columnCards = shuffledDeck.subList(startIndex, startIndex + cardsPerColumn)
                add(TableauColumn(hiddenCards = emptyList(), visibleCards = columnCards))
            }
        }
        return GameState(
            variant = GameVariant.FREECELL,
            tableauColumns = tableauColumns,
            foundationPiles = Suit.entries.associateWith { emptyList() },
            freeCells = List(4) { null },
            stockPile = emptyList(),
            wastePile = emptyList(),
            initialSeed = seed,
        )
    }
}
