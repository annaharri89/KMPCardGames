package ui.render

import app.CardThemeSpec
import domain.model.Rank
import domain.model.Suit
import korlibs.image.bitmap.Bitmap
import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.View
import korlibs.korge.view.addTo
import korlibs.korge.view.image
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import korlibs.math.geom.Anchor2D
import korlibs.math.geom.slice.RectSlice

class SuitSymbolPainter(
    private val themeSpec: CardThemeSpec,
    private val sliceByBaseName: Map<String, RectSlice<Bitmap>>?,
) {
    fun draw(
        parentContainer: Container,
        suit: Suit,
        x: Double,
        y: Double,
        symbolWidth: Double,
        symbolHeight: Double,
    ) {
        val textureKey = suitTextureBaseName(suit)
        val slice = sliceByBaseName?.let { map ->
            map[textureKey] ?: map["$textureKey.png"]
        }
        if (slice != null) {
            val sliceWidth = slice.width.toDouble()
            val sliceHeight = slice.height.toDouble()
            parentContainer.image(slice, Anchor2D.TOP_LEFT) {
                this.x = x
                this.y = y
                scaleX = symbolWidth / sliceWidth
                scaleY = symbolHeight / sliceHeight
                mouseEnabled = false
            }
        } else {
            parentContainer.text(
                text = suitSymbolCharacter(suit),
                textSize = symbolHeight * 0.82,
                color = suitTextColor(suit),
            ) {
                this.x = x
                this.y = y
                mouseEnabled = false
            }
        }
    }

    private fun suitTextureBaseName(suit: Suit): String = when (suit) {
        Suit.CLUBS -> "suit_club_medium_01"
        Suit.DIAMONDS -> "suit_diamond_medium_01"
        Suit.HEARTS -> "suit_heart_medium_01"
        Suit.SPADES -> "suit_spade_medium_01"
    }

    private fun suitSymbolCharacter(suit: Suit): String = when (suit) {
        Suit.HEARTS -> "♥"
        Suit.DIAMONDS -> "♦"
        Suit.CLUBS -> "♣"
        Suit.SPADES -> "♠"
    }

    private fun suitTextColor(suit: Suit): RGBA = when (suit) {
        Suit.HEARTS, Suit.DIAMONDS -> themeSpec.redSuitColor
        Suit.CLUBS, Suit.SPADES -> themeSpec.blackSuitColor
    }
}

class FaceCardAnimalPainter(
    private val sliceByBaseName: Map<String, RectSlice<Bitmap>>?,
) {
    fun draw(
        parentContainer: Container,
        rank: Rank,
        suit: Suit,
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        suitAccentColor: RGBA,
    ): View? {
        val textureKey = faceTextureBaseName(rank, suit)
        val slice = textureKey?.let { key ->
            sliceByBaseName?.get(key) ?: sliceByBaseName?.get("$key.png")
        }
        val motifContainer = Container().addTo(parentContainer)
        motifContainer.x = x
        motifContainer.y = y
        if (slice != null) {
            val sliceWidth = slice.width.toDouble()
            val sliceHeight = slice.height.toDouble()
            motifContainer.image(slice, Anchor2D.TOP_LEFT) {
                this.x = 0.0
                this.y = 0.0
                scaleX = width / sliceWidth
                scaleY = height / sliceHeight
                mouseEnabled = false
            }
            return motifContainer
        }
        motifContainer.solidRect(
            width = width * 0.85,
            height = height * 0.55,
            color = suitAccentColor.withAd(0.22),
        ) {
            this.x = width * 0.075
            this.y = height * 0.22
            mouseEnabled = false
        }
        motifContainer.text(
            text = faceRankLetter(rank),
            textSize = 24.0,
            color = suitAccentColor,
        ) {
            this.x = width * 0.42
            this.y = height * 0.38
            mouseEnabled = false
        }
        return motifContainer
    }

    private fun faceTextureBaseName(rank: Rank, suit: Suit): String? = when (rank) {
        Rank.QUEEN -> when (suit) {
            Suit.HEARTS -> "rank_q_heart_card"
            Suit.DIAMONDS -> "rank_q_diamond_card"
            Suit.SPADES -> "rank_q_spade_card"
            Suit.CLUBS -> "queen_head_front_01"
        }
        Rank.JACK -> "decor_moon_emblem_01"
        Rank.KING -> "queen_crown_large_01"
        else -> null
    }

    private fun faceRankLetter(rank: Rank): String = when (rank) {
        Rank.JACK -> "J"
        Rank.QUEEN -> "Q"
        Rank.KING -> "K"
        else -> "?"
    }
}
