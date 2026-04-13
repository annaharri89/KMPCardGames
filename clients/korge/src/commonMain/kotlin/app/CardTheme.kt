package app

import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import presentation.solitaire.CardTheme

data class CardThemeSpec(
    val boardBackgroundColor: RGBA,
    val invalidMoveOverlayColor: RGBA,
    val shadowAlpha: Double,
    val shadowOffset: Double,
    val hiddenCardFillColor: RGBA,
    val cardFrontColor: RGBA,
    val redSuitColor: RGBA,
    val blackSuitColor: RGBA,
    val rankTextSize: Double,
    val borderWidth: Double,
    val cardBorderColor: RGBA,
    val hiddenCardAccentColor: RGBA,
    val ornamentPrimaryColor: RGBA,
    val faceCardIdleBobDistance: Double,
)

fun cardThemeSpec(theme: CardTheme): CardThemeSpec = when (theme) {
    CardTheme.REGAL_ANIMALS -> CardThemeSpec(
        boardBackgroundColor = Colors["#2d4a3e"],
        invalidMoveOverlayColor = Colors["#ff6b6b"],
        shadowAlpha = 0.35,
        shadowOffset = 2.0,
        hiddenCardFillColor = Colors["#1e3d52"],
        cardFrontColor = Colors["#fff8f0"],
        redSuitColor = Colors["#d94a6a"],
        blackSuitColor = Colors["#2a2a3a"],
        rankTextSize = 20.0,
        borderWidth = 2.0,
        cardBorderColor = Colors["#e8d8c8"],
        hiddenCardAccentColor = Colors["#7ec8d4"],
        ornamentPrimaryColor = Colors["#c8e8d8"],
        faceCardIdleBobDistance = 3.0,
    )
}
