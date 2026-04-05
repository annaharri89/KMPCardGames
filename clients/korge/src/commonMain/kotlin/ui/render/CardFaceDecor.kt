package ui.render

import domain.model.Rank
import domain.model.Suit
import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.solidRect

/**
 * UI-only bucket for the **center** of a face-up card: pip-style ornament ([NONE]) vs Jack/Queen/King art
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

enum class MicroPatternStyle {
    DIAGONAL_STRIPES,
    DOT_GRID,
}

data class SuitOrnamentVariant(
    val primaryPatternStyle: MicroPatternStyle,
    val secondaryPatternStyle: MicroPatternStyle,
)

internal fun faceCardAnimalForRank(rank: Rank): FaceCardAnimal = when (rank) {
    Rank.JACK -> FaceCardAnimal.JACK
    Rank.QUEEN -> FaceCardAnimal.QUEEN
    Rank.KING -> FaceCardAnimal.KING
    else -> FaceCardAnimal.NONE
}

internal fun suitOrnamentVariantFor(suit: Suit): SuitOrnamentVariant = when (suit) {
    Suit.HEARTS, Suit.DIAMONDS -> SuitOrnamentVariant(
        primaryPatternStyle = MicroPatternStyle.DIAGONAL_STRIPES,
        secondaryPatternStyle = MicroPatternStyle.DOT_GRID,
    )
    Suit.CLUBS, Suit.SPADES -> SuitOrnamentVariant(
        primaryPatternStyle = MicroPatternStyle.DOT_GRID,
        secondaryPatternStyle = MicroPatternStyle.DIAGONAL_STRIPES,
    )
}

internal fun drawMicroPatternPanel(
    parentContainer: Container,
    microPatternStyle: MicroPatternStyle,
    x: Double,
    y: Double,
    width: Double,
    height: Double,
    primaryColor: RGBA,
    secondaryColor: RGBA,
    baseAlpha: Double,
) {
    when (microPatternStyle) {
        MicroPatternStyle.DIAGONAL_STRIPES -> {
            val stripeCount = 8
            repeat(stripeCount) { stripeIndex ->
                parentContainer.solidRect(
                    width = 2.5,
                    height = height,
                    color = primaryColor.withAd(baseAlpha),
                ) {
                    this.x = x + stripeIndex * (width / stripeCount)
                    this.y = y
                    mouseEnabled = false
                }
            }
        }
        MicroPatternStyle.DOT_GRID -> {
            val cols = 4
            val rows = 6
            val cellW = width / cols
            val cellH = height / rows
            repeat(rows) { row ->
                repeat(cols) { col ->
                    parentContainer.solidRect(
                        width = 3.0,
                        height = 3.0,
                        color = if ((row + col) % 2 == 0) primaryColor.withAd(baseAlpha) else secondaryColor.withAd(baseAlpha * 0.85),
                    ) {
                        this.x = x + col * cellW + cellW * 0.35
                        this.y = y + row * cellH + cellH * 0.35
                        mouseEnabled = false
                    }
                }
            }
        }
    }
}
