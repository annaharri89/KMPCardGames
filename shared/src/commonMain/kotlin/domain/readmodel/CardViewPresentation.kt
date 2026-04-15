package domain.readmodel

import domain.model.CardColor
import domain.model.Rank

fun CardViewModel.rankAbbrev(): String = when (val f = face) {
    is CardFace.Up -> f.rank.toAbbrev()
    is CardFace.Down -> ""
}

private fun Rank.toAbbrev(): String = when (this) {
    Rank.ACE -> "A"
    Rank.TWO -> "2"
    Rank.THREE -> "3"
    Rank.FOUR -> "4"
    Rank.FIVE -> "5"
    Rank.SIX -> "6"
    Rank.SEVEN -> "7"
    Rank.EIGHT -> "8"
    Rank.NINE -> "9"
    Rank.TEN -> "10"
    Rank.JACK -> "J"
    Rank.QUEEN -> "Q"
    Rank.KING -> "K"
}

fun CardViewModel.usesRedSuitInk(): Boolean = color == CardColor.RED
