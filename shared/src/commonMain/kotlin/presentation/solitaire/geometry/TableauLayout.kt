package presentation.solitaire.geometry

import domain.readmodel.CardFace
import domain.readmodel.CardViewModel
import kotlin.math.max

fun calculateTableauHitHeight(
    cardHeight: Double,
    tableauCardOffsetY: Double,
    cardCount: Int,
): Double = cardHeight + (max(0, cardCount - 1) * tableauCardOffsetY)

fun isHiddenCard(cardViewModel: CardViewModel): Boolean =
    cardViewModel.face is CardFace.Down
