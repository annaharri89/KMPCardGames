package ui.layout

/** Top-left anchor for one pile, in the same coordinate space as the KorGE scene. */
data class PileLayout(
    val x: Double,
    val y: Double,
)

/** Screen positions for stock, waste, four foundations, and seven tableau columns. */
data class SolitaireBoardLayout(
    val stockPile: PileLayout,
    val wastePile: PileLayout,
    val foundationPiles: List<PileLayout>,
    val tableauPiles: List<PileLayout>,
)

/** Computes default Klondike pile anchors from viewport size using fixed width/height ratios. */
object BoardLayout {
    fun create(
        viewportWidth: Double,
        viewportHeight: Double,
    ): SolitaireBoardLayout {
        val cardSpacingX = viewportWidth * 0.09
        val topRowY = viewportHeight * 0.08
        val tableauRowY = viewportHeight * 0.28
        val leftMargin = viewportWidth * 0.05

        return SolitaireBoardLayout(
            stockPile = PileLayout(x = leftMargin, y = topRowY),
            wastePile = PileLayout(x = leftMargin + cardSpacingX, y = topRowY),
            foundationPiles = List(4) { index ->
                PileLayout(
                    x = leftMargin + (3 + index) * cardSpacingX,
                    y = topRowY,
                )
            },
            tableauPiles = List(7) { index ->
                PileLayout(
                    x = leftMargin + index * cardSpacingX,
                    y = tableauRowY,
                )
            },
        )
    }
}
