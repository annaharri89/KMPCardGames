package ui.render

/** Debug flags for Queen of Hearts fox card motion. */
object FoxHeartQueenAnimationDebugFlags {
    /** If true, keep the card fully static (no blink). */
    const val suppressAllCardAnimations: Boolean = false

    /** If true, hide the neck on-card. */
    const val hideNeckOnCard: Boolean = true

    /**
     * If true (and not fully static), run blink only.
     * Ear, neck, and tail motion stay off.
     */
    const val suppressNonBlinkCardAnimations: Boolean = true
}
