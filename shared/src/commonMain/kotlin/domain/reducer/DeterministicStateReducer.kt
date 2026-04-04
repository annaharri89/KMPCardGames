package domain.reducer

import domain.action.GameAction
import domain.model.GameOutcome
import domain.model.GameState
import domain.model.GameVariant
import domain.result.ActionResult
import domain.result.RejectionReason
import domain.rules.RuleSet

class DeterministicStateReducer(
    ruleSets: Set<RuleSet>,
) {
    private val ruleSetByVariant: Map<GameVariant, RuleSet> = ruleSets.associateBy { it.variant }

    fun reduce(
        state: GameState,
        action: GameAction,
    ): ActionResult {
        if (state.outcome != GameOutcome.IN_PROGRESS) {
            return ActionResult(
                state = state,
                rejectionReason = RejectionReason.GAME_ALREADY_FINISHED,
            )
        }

        val matchingRuleSet = ruleSetByVariant[state.variant]
            ?: error("Missing rule set for variant=${state.variant}")
        val validationResult = matchingRuleSet.validate(action, state)
        if (!validationResult.isValid) {
            return ActionResult(
                state = state,
                rejectionReason = validationResult.rejectionReason,
            )
        }
        return matchingRuleSet.apply(action, state)
    }
}
