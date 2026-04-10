package ui.render

import domain.model.Rank

/**
 * UI-only bucket for the **center** of a face-up card: pip layout ([NONE]) vs Jack/Queen/King art
 * ([JACK], [QUEEN], [KING]). [domain.model.Rank] already tells you the rank; this type exists so
 * [SolitaireBoardRenderer] can pick layout and idle bob in one place via [faceCardAnimalForRank]
 * instead of scattering `when (rank)` for J/Q/K everywhere.
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
