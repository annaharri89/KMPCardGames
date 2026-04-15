package ui.render

/**
 * Tuning flags for the Queen of Hearts fox puppet on playable cards.
 */
object FoxHeartQueenAnimationDebugFlags {
    /** When true, Queen of Hearts is a static stack (no blink). Prefer false + [suppressNonBlinkCardAnimations] for blink-only. */
    const val suppressAllCardAnimations: Boolean = false

    /** Omits the neck slice in static layout, or hides the neck layer when the board animates fox queens. */
    const val hideNeckOnCard: Boolean = true

    /**
     * When [suppressAllCardAnimations] is false and fox queen animation is on: if true, only blink runs;
     * ear twitch, neck swallow, and tail wag stay off.
     */
    const val suppressNonBlinkCardAnimations: Boolean = true
}
