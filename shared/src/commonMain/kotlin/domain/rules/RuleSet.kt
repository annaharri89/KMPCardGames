package domain.rules

import domain.action.GameAction
import domain.model.GameState
import domain.model.GameVariant
import domain.result.ActionResult
import domain.result.RejectionReason

data class ValidationResult(
    val rejectionReason: RejectionReason? = null,
) {
    val isValid: Boolean = rejectionReason == null

    companion object {
        fun valid(): ValidationResult = ValidationResult()
        fun invalid(rejectionReason: RejectionReason): ValidationResult = ValidationResult(rejectionReason)
    }
}

interface RuleSet {
    val variant: GameVariant

    fun validate(
        action: GameAction,
        state: GameState,
    ): ValidationResult

    fun apply(
        action: GameAction,
        state: GameState,
    ): ActionResult
}
