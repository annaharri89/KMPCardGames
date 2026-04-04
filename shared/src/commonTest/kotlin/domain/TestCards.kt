package domain

import domain.model.Card
import domain.model.Rank
import domain.model.Suit

fun card(
    suit: Suit,
    rank: Rank,
): Card = Card(suit = suit, rank = rank)
