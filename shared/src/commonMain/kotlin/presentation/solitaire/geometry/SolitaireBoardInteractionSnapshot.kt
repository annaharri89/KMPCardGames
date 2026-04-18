package presentation.solitaire.geometry

import domain.readmodel.CardViewModel

data class PileHitRegion(
    val pileId: String,
    val bounds: AxisAlignedRect,
)

data class DraggableCardInteractionTarget(
    /** Stable id for this top-card surface for the current render pass; client maps it to a view. */
    val handleId: Int,
    val pileId: String,
    val card: CardViewModel,
    val cardCount: Int,
    /** Cards moved with a drag, top-most first (matches tableau order from grab point through pile bottom). */
    val stackCards: List<CardViewModel>,
    val topCardBounds: AxisAlignedRect,
)

/**
 * Hit testing and drag metadata in engine-neutral form. KorGE (or any host) maps [DraggableCardInteractionTarget.handleId]
 * to concrete views once per [render][ui.render.SolitaireBoardRenderer.render] pass.
 */
data class SolitaireBoardInteractionSnapshot(
    val pileTapBoundsByPileId: Map<String, AxisAlignedRect>,
    val pileHitRegionsByPileId: Map<String, PileHitRegion>,
    val draggableTopCards: List<DraggableCardInteractionTarget>,
)
