package domain.action

sealed interface PileRef {
    data class Tableau(val columnIndex: Int) : PileRef
    data class Foundation(val suit: domain.model.Suit) : PileRef
    data object Stock : PileRef
    data object Waste : PileRef
    data class FreeCell(val slotIndex: Int) : PileRef
}

sealed interface GameAction {
    data object DrawFromStock : GameAction
    data object RecycleWasteToStock : GameAction
    data class MoveCards(
        val from: PileRef,
        val to: PileRef,
        val cardCount: Int = 1,
    ) : GameAction
    data object AutoMoveEligibleCardsToFoundation : GameAction
}
