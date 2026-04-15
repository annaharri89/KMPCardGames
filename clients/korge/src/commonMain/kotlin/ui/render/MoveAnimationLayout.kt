package ui.render

import domain.model.Suit
import domain.readmodel.GameRenderModel
import ui.layout.BoardLayout

/**
 * Screen Y of the top-left of the top visible card on [pileId] after [renderModel] is applied.
 * Matches [SolitaireBoardRenderer] card placement for tableau (stacked offset) and single-card piles.
 */
internal fun expectedTopCardYForSolitairePile(
    pileId: String,
    renderModel: GameRenderModel,
    viewportWidth: Double,
    viewportHeight: Double,
): Double? {
    val foundationSuitSlotOrder = listOf(
        Suit.HEARTS,
        Suit.DIAMONDS,
        Suit.SPADES,
        Suit.CLUBS,
    )
    val tableauCardOffsetY =
        SolitaireBoardPlayfieldMetrics.forViewport(viewportWidth, viewportHeight).tableauCardOffsetY
    val layout = BoardLayout.create(
        viewportWidth = viewportWidth,
        viewportHeight = viewportHeight,
    )
    return when {
        pileId.startsWith("tableau-") -> {
            val index = pileId.removePrefix("tableau-").toIntOrNull() ?: return null
            val pile = renderModel.tableauPiles.getOrNull(index) ?: return null
            val n = pile.cards.size
            if (n <= 0) return null
            layout.tableauPiles[index].y + ((n - 1) * tableauCardOffsetY)
        }
        pileId.startsWith("foundation-") -> {
            val suitKey = pileId.removePrefix("foundation-")
            val suitIndex = foundationSuitSlotOrder.indexOfFirst { it.name.lowercase() == suitKey }
            if (suitIndex < 0) return null
            val pile = renderModel.foundationPiles.firstOrNull { it.pileId == pileId } ?: return null
            if (pile.cards.isEmpty()) return null
            layout.foundationPiles[suitIndex].y
        }
        pileId == "waste" -> {
            if (renderModel.wastePile.cards.isEmpty()) return null
            layout.wastePile.y
        }
        pileId == "stock" -> layout.stockPile.y
        else -> null
    }
}
