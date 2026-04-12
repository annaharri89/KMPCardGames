package ui.render

/** Card and center-motif sizes used by [SolitaireBoardRenderer] (large slot = atlas or fox puppet). */
internal object SolitaireBoardFaceCardMetrics {
    const val cardWidth = 96.0
    const val cardHeight = 132.0
    const val largeFaceMotifWidth = 94.0
    const val largeFaceMotifHeight = 126.0

    /**
     * Uniform scale for J/Q/K center art when [SolitaireBoardRenderer] draws a topmost exposed card
     * so the animal can extend past the card rectangle; stacked tableau cards use 1.0.
     */
    const val faceMotifBleedScale = 1.38
}
