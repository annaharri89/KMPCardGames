package domain.result

import domain.model.GameState

enum class RejectionReason {
    INVALID_SOURCE,
    INVALID_DESTINATION,
    INVALID_CARD_COUNT,
    RULE_VIOLATION,
    GAME_ALREADY_FINISHED,
    STOCK_EMPTY,
}

sealed interface DomainEvent {
    data class CardsDrawn(val cardCount: Int) : DomainEvent
    data class CardsMoved(val cardCount: Int) : DomainEvent
    data object StockRecycled : DomainEvent
    data object AutoMovedToFoundation : DomainEvent
    data object GameWon : DomainEvent
}

data class ActionResult(
    val state: GameState,
    val events: List<DomainEvent> = emptyList(),
    val rejectionReason: RejectionReason? = null,
) {
    val wasAccepted: Boolean = rejectionReason == null
}
