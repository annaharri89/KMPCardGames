package ui.render

import domain.model.Rank
import domain.model.Suit
import korlibs.korge.view.Container
import kotlin.math.max

/**
 * Classic-style center pips for ranks A–10: one suit symbol per [Rank.pipValue], arranged like a standard deck.
 */
internal object NumberCardPipLayout {

    private const val pipAreaLeftInset = 12.0
    private const val pipAreaTopInset = 42.0
    private const val pipAreaRightInset = 20.0
    private const val pipAreaBottomInset = 38.0

    private data class PipArea(
        val left: Double,
        val top: Double,
        val width: Double,
        val height: Double,
    )

    private fun pipArea(cardWidth: Double, cardHeight: Double): PipArea {
        val left = pipAreaLeftInset
        val top = pipAreaTopInset
        val width = max(8.0, cardWidth - pipAreaLeftInset - pipAreaRightInset)
        val height = max(8.0, cardHeight - pipAreaTopInset - pipAreaBottomInset)
        return PipArea(left, top, width, height)
    }

    fun draw(
        parentContainer: Container,
        cardWidth: Double,
        cardHeight: Double,
        rank: Rank,
        suit: Suit,
        suitSymbolPainter: SuitSymbolPainter,
    ) {
        val pipCount = rank.pipValue
        require(pipCount in 1..10) { "pip layout only for ranks A–10" }
        val area = pipArea(cardWidth, cardHeight)
        val pipWidth: Double
        val pipHeight: Double
        if (pipCount == 1) {
            pipWidth = 24.0
            pipHeight = 20.0
        } else {
            pipWidth = 12.0
            pipHeight = 10.0
        }
        slotsForPipCount(pipCount).forEach { (nx, ny) ->
            val centerX = area.left + nx * area.width
            val centerY = area.top + ny * area.height
            suitSymbolPainter.draw(
                parentContainer = parentContainer,
                suit = suit,
                x = centerX - pipWidth / 2.0,
                y = centerY - pipHeight / 2.0,
                symbolWidth = pipWidth,
                symbolHeight = pipHeight,
            )
        }
    }

    private fun slotsForPipCount(pipCount: Int): List<Pair<Double, Double>> = when (pipCount) {
        1 -> listOf(0.5 to 0.5)
        2 -> listOf(0.5 to 0.22, 0.5 to 0.78)
        3 -> listOf(0.5 to 0.15, 0.5 to 0.5, 0.5 to 0.85)
        4 -> listOf(
            0.24 to 0.22,
            0.76 to 0.22,
            0.24 to 0.78,
            0.76 to 0.78,
        )
        5 -> listOf(
            0.24 to 0.22,
            0.76 to 0.22,
            0.24 to 0.78,
            0.76 to 0.78,
            0.5 to 0.5,
        )
        6 -> {
            val columnXs = listOf(0.28, 0.72)
            val rowYs = listOf(0.2, 0.5, 0.8)
            columnXs.flatMap { nx -> rowYs.map { ny -> nx to ny } }
        }
        7 -> {
            val top = listOf(0.5 to 0.12)
            val columnXs = listOf(0.28, 0.72)
            val rowYs = listOf(0.36, 0.54, 0.72)
            val body = columnXs.flatMap { nx -> rowYs.map { ny -> nx to ny } }
            top + body
        }
        8 -> {
            val columnXs = listOf(0.28, 0.72)
            val rowYs = listOf(0.14, 0.36, 0.64, 0.86)
            columnXs.flatMap { nx -> rowYs.map { ny -> nx to ny } }
        }
        9 -> {
            val xs = listOf(0.2, 0.5, 0.8)
            val ys = listOf(0.18, 0.5, 0.82)
            ys.flatMap { ny -> xs.map { nx -> nx to ny } }
        }
        10 -> {
            val columnXs = listOf(0.28, 0.72)
            val rowYs = listOf(0.1, 0.28, 0.46, 0.64, 0.82)
            columnXs.flatMap { nx -> rowYs.map { ny -> nx to ny } }
        }
        else -> error("unexpected pipCount=$pipCount")
    }
}
