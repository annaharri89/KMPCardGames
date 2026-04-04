package domain.model

enum class Suit {
    CLUBS,
    DIAMONDS,
    HEARTS,
    SPADES,
}

enum class CardColor {
    BLACK,
    RED,
}

enum class Rank(val pipValue: Int) {
    ACE(1),
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5),
    SIX(6),
    SEVEN(7),
    EIGHT(8),
    NINE(9),
    TEN(10),
    JACK(11),
    QUEEN(12),
    KING(13),
}

data class Card(
    val suit: Suit,
    val rank: Rank,
) {
    val color: CardColor = when (suit) {
        Suit.CLUBS, Suit.SPADES -> CardColor.BLACK
        Suit.DIAMONDS, Suit.HEARTS -> CardColor.RED
    }
}
