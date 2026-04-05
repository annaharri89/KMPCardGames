package domain.readmodel

import domain.model.Card
import domain.model.CardColor
import domain.model.GameState
import domain.model.GameVariant
import domain.model.Rank
import domain.model.Suit
import domain.model.TableauColumn

/** How this card appears in the UI: face-up with a [Rank], or face-down in the tableau. */
sealed class CardFace {
    data class Up(val rank: Rank) : CardFace()

    data object Down : CardFace()
}

data class CardViewModel(
    val suit: Suit,
    val face: CardFace,
    val color: CardColor,
)

data class PileViewModel(
    val pileId: String,
    val cards: List<CardViewModel>,
)

data class GameRenderModel(
    val variant: GameVariant,
    val tableauPiles: List<PileViewModel>,
    val foundationPiles: List<PileViewModel>,
    val freeCellPiles: List<PileViewModel>,
    val stockPileCount: Int,
    val wastePile: PileViewModel,
    val moveCounter: Int,
)

object GameRenderModelProjector {
    fun toRenderModel(gameState: GameState): GameRenderModel {
        return GameRenderModel(
            variant = gameState.variant,
            tableauPiles = gameState.tableauColumns.mapIndexed { index, column ->
                PileViewModel(
                    pileId = "tableau-$index",
                    cards = buildTableauCards(column),
                )
            },
            foundationPiles = Suit.entries.map { suit ->
                PileViewModel(
                    pileId = "foundation-${suit.name.lowercase()}",
                    cards = gameState.foundationPiles[suit].orEmpty().map { it.toCardViewModel() },
                )
            },
            freeCellPiles = gameState.freeCells.mapIndexed { index, freeCellCard ->
                PileViewModel(
                    pileId = "freecell-$index",
                    cards = listOfNotNull(freeCellCard?.toCardViewModel()),
                )
            },
            stockPileCount = gameState.stockPile.size,
            wastePile = PileViewModel(
                pileId = "waste",
                cards = gameState.wastePile.map { it.toCardViewModel() },
            ),
            moveCounter = gameState.moveCounter,
        )
    }

    private fun buildTableauCards(column: TableauColumn): List<CardViewModel> {
        val hiddenCardMarkers = List(column.hiddenCards.size) {
            CardViewModel(
                suit = Suit.SPADES,
                face = CardFace.Down,
                color = CardColor.BLACK,
            )
        }
        return hiddenCardMarkers + column.visibleCards.map { it.toCardViewModel() }
    }

    private fun Card.toCardViewModel(): CardViewModel = CardViewModel(
        suit = suit,
        face = CardFace.Up(rank = rank),
        color = color,
    )
}
