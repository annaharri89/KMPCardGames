package ui.render

import kotlin.math.max

/**
 * Scales card geometry and tableau vertical step so tall tableau columns and HUD fit inside
 * [viewportWidth]×[viewportHeight], matching [ui.layout.BoardLayout] row fractions.
 */
internal data class SolitaireBoardPlayfieldMetrics(
    val uniformScale: Double,
) {
    val cardWidth: Double = SolitaireBoardFaceCardMetrics.cardWidth * uniformScale
    val cardHeight: Double = SolitaireBoardFaceCardMetrics.cardHeight * uniformScale
    val tableauCardOffsetY: Double = BASE_TABLEAU_OFFSET_Y * uniformScale
    val hudPrimaryTextSize: Double = max(13.0, 24.0 * uniformScale)
    val hudSecondaryTextSize: Double = max(11.0, 18.0 * uniformScale)

    companion object {
        private const val BASE_TABLEAU_OFFSET_Y = 24.0
        private const val TABLEAU_START_HEIGHT_FRACTION = 0.28
        private const val BOTTOM_RESERVE_FRACTION = 0.09
        private const val COLUMN_PITCH_FRACTION = 0.09
        private const val COLUMN_PITCH_CARD_WIDTH_FACTOR = 0.92
        private const val PLANNING_MAX_TABLEAU_CARDS = 14

        fun forViewport(viewportWidth: Double, viewportHeight: Double): SolitaireBoardPlayfieldMetrics {
            val tableauStartY = viewportHeight * TABLEAU_START_HEIGHT_FRACTION
            val availableForStack =
                viewportHeight - tableauStartY - viewportHeight * BOTTOM_RESERVE_FRACTION
            val stackNeed =
                SolitaireBoardFaceCardMetrics.cardHeight +
                    (PLANNING_MAX_TABLEAU_CARDS - 1) * BASE_TABLEAU_OFFSET_Y
            val heightLimitedScale = availableForStack / stackNeed

            val maxCardWidthFromColumnPitch =
                viewportWidth * COLUMN_PITCH_FRACTION * COLUMN_PITCH_CARD_WIDTH_FACTOR
            val widthLimitedScale = maxCardWidthFromColumnPitch / SolitaireBoardFaceCardMetrics.cardWidth

            val uniformScale = minOf(1.0, heightLimitedScale, widthLimitedScale)
            return SolitaireBoardPlayfieldMetrics(uniformScale = uniformScale)
        }
    }
}
