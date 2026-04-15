package ui.render

import domain.model.Rank

/**
 * UI choice for face-card center art.
 *
 * Keeps J/Q/K mapping in [faceCardAnimalForRank] so renderer logic stays in one place.
 */
enum class FaceCardAnimal {
    NONE,
    JACK,
    QUEEN,
    KING,
}

internal fun faceCardAnimalForRank(rank: Rank): FaceCardAnimal = when (rank) {
    Rank.JACK -> FaceCardAnimal.JACK
    Rank.QUEEN -> FaceCardAnimal.QUEEN
    Rank.KING -> FaceCardAnimal.KING
    else -> FaceCardAnimal.NONE
}
