package presentation.solitaire.geometry

import domain.model.Suit

/**
 * Left-to-right foundation column order used by the Klondike board chrome (glyphs and empty pile slots).
 * Foundation pile ids still come from [domain.readmodel.GameRenderModelProjector] (`foundation-${suit.name.lowercase()}` for each [domain.model.Suit]);
 * this list is only the **visual** column index to id mapping used by the client layout.
 */
val foundationSuitsVisualLeftToRight: List<Suit> = listOf(
    Suit.HEARTS,
    Suit.DIAMONDS,
    Suit.SPADES,
    Suit.CLUBS,
)
