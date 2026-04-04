package domain.model

enum class GameOutcome {
    IN_PROGRESS,
    WON,
    LOST,
}

data class TableauColumn(
    val hiddenCards: List<Card>,
    val visibleCards: List<Card>,
)

data class GameState(
    val variant: GameVariant,
    val tableauColumns: List<TableauColumn>,
    val foundationPiles: Map<Suit, List<Card>>,
    val freeCells: List<Card?> = emptyList(),
    val stockPile: List<Card> = emptyList(),
    val wastePile: List<Card> = emptyList(),
    val outcome: GameOutcome = GameOutcome.IN_PROGRESS,
    val moveCounter: Int = 0,
    val initialSeed: Long,
)
