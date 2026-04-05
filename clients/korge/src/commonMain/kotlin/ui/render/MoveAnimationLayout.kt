package ui.render

import domain.model.Suit
import domain.readmodel.GameRenderModel
import ui.layout.BoardLayout

internal const val TABLEAU_CARD_OFFSET_Y_FOR_ANIMATION = 24.0

/**
 * Screen Y of the top-left of the top visible card on [pileId] after [renderModel] is applied.
 * Matches [SolitaireBoardRenderer] card placement for tableau (stacked offset) and single-card piles.
 */
internal fun expectedTopCardYForSolitairePile(
    pileId: String,
    renderModel: GameRenderModel,
    viewportWidth: Double,
    viewportHeight: Double,
    tableauCardOffsetY: Double = TABLEAU_CARD_OFFSET_Y_FOR_ANIMATION,
): Double? {
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
            val suitIndex = Suit.entries.indexOfFirst { it.name.lowercase() == suitKey }
            if (suitIndex < 0) return null
            val pile = renderModel.foundationPiles.getOrNull(suitIndex) ?: return null
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
