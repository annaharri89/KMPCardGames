package domain.rules

import domain.model.Card
import domain.model.Rank

object CardStackPolicies {
    fun canPlaceOnFoundation(
        movingCard: Card,
        foundationTopCard: Card?,
    ): Boolean {
        if (foundationTopCard == null) {
            return movingCard.rank == Rank.ACE
        }
        val hasSameSuit = movingCard.suit == foundationTopCard.suit
        val hasSequentialRank = movingCard.rank.pipValue == foundationTopCard.rank.pipValue + 1
        return hasSameSuit && hasSequentialRank
    }

    fun canPlaceOnAlternatingDescendingTableau(
        movingCard: Card,
        tableauTopCard: Card?,
    ): Boolean {
        if (tableauTopCard == null) {
            return movingCard.rank == Rank.KING
        }
        val hasAlternatingColor = movingCard.color != tableauTopCard.color
        val hasDescendingRank = movingCard.rank.pipValue == tableauTopCard.rank.pipValue - 1
        return hasAlternatingColor && hasDescendingRank
    }

    fun canPlaceOnDescendingTableau(
        movingCard: Card,
        tableauTopCard: Card?,
    ): Boolean {
        if (tableauTopCard == null) {
            return true
        }
        return movingCard.rank.pipValue == tableauTopCard.rank.pipValue - 1
    }
}
