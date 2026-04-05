package ui.adapter

import domain.action.GameAction
import domain.action.PileRef
import domain.model.GameVariant
import domain.readmodel.GameRenderModel
import domain.readmodel.GameRenderModelProjector
import domain.result.DomainEvent
import domain.result.RejectionReason
import domain.session.GameSessionFactory
import domain.session.GameSessionManager

sealed interface UiIntent {
    data object Draw : UiIntent
    data object Recycle : UiIntent
    data object AutoMove : UiIntent
    data class DragMove(
        val sourcePileId: String,
        val destinationPileId: String,
        val cardCount: Int = 1,
    ) : UiIntent
}

data class UiDispatchResult(
    val renderModel: GameRenderModel,
    val wasAccepted: Boolean,
    val rejectionReason: RejectionReason?,
    val events: List<DomainEvent>,
)

class DomainUiMapper(
    private val gameSessionManager: GameSessionManager = GameSessionFactory.createDefaultSessionManager(),
) {
    fun startGameSession(
        variant: GameVariant,
        seed: Long,
    ): GameRenderModel {
        val initialState = gameSessionManager.startNewSession(
            variant = variant,
            seed = seed,
        )
        return GameRenderModelProjector.toRenderModel(initialState)
    }

    fun applyUiIntent(uiIntent: UiIntent): UiDispatchResult {
        val domainAction = uiIntent.toDomainAction()
        val actionResult = gameSessionManager.dispatch(domainAction)
        return UiDispatchResult(
            renderModel = GameRenderModelProjector.toRenderModel(actionResult.state),
            wasAccepted = actionResult.wasAccepted,
            rejectionReason = actionResult.rejectionReason,
            events = actionResult.events,
        )
    }

    fun undo(): GameRenderModel {
        val updatedState = gameSessionManager.undo()
        return GameRenderModelProjector.toRenderModel(updatedState)
    }

    fun redo(): GameRenderModel {
        val updatedState = gameSessionManager.redo()
        return GameRenderModelProjector.toRenderModel(updatedState)
    }

    private fun UiIntent.toDomainAction(): GameAction = when (this) {
        UiIntent.Draw -> GameAction.DrawFromStock
        UiIntent.Recycle -> GameAction.RecycleWasteToStock
        UiIntent.AutoMove -> GameAction.AutoMoveEligibleCardsToFoundation
        is UiIntent.DragMove -> GameAction.MoveCards(
            from = parsePileRef(sourcePileId),
            to = parsePileRef(destinationPileId),
            cardCount = cardCount,
        )
    }

    private fun parsePileRef(pileId: String): PileRef = when {
        pileId.startsWith("tableau-") -> PileRef.Tableau(pileId.removePrefix("tableau-").toInt())
        pileId.startsWith("foundation-") -> {
            val suitToken = pileId.removePrefix("foundation-")
            PileRef.Foundation(domain.model.Suit.valueOf(suitToken.uppercase()))
        }
        pileId.startsWith("freecell-") -> PileRef.FreeCell(pileId.removePrefix("freecell-").toInt())
        pileId == "stock" -> PileRef.Stock
        pileId == "waste" -> PileRef.Waste
        else -> error("Unknown pileId=$pileId")
    }
}
