package domain.rules.freecell

import domain.action.GameAction
import domain.action.PileRef
import domain.model.Card
import domain.model.GameOutcome
import domain.model.GameState
import domain.model.GameVariant
import domain.model.Suit
import domain.result.ActionResult
import domain.result.DomainEvent
import domain.result.RejectionReason
import domain.rules.CardStackPolicies
import domain.rules.RuleSet
import domain.rules.ValidationResult

class FreeCellRuleSet : RuleSet {
    override val variant: GameVariant = GameVariant.FREECELL

    override fun validate(
        action: GameAction,
        state: GameState,
    ): ValidationResult = when (action) {
        GameAction.DrawFromStock,
        GameAction.RecycleWasteToStock -> ValidationResult.invalid(RejectionReason.RULE_VIOLATION)
        is GameAction.MoveCards -> validateMoveCards(action = action, state = state)
        GameAction.AutoMoveEligibleCardsToFoundation -> {
            if (findSingleAutoMoveToFoundation(state) == null) {
                ValidationResult.invalid(RejectionReason.RULE_VIOLATION)
            } else {
                ValidationResult.valid()
            }
        }
    }

    override fun apply(
        action: GameAction,
        state: GameState,
    ): ActionResult = when (action) {
        is GameAction.MoveCards -> applyMoveCards(action = action, state = state)
        GameAction.AutoMoveEligibleCardsToFoundation -> {
            val autoMoveAction = findSingleAutoMoveToFoundation(state)
                ?: return ActionResult(
                    state = state,
                    rejectionReason = RejectionReason.RULE_VIOLATION,
                )
            val moveResult = applyMoveCards(action = autoMoveAction, state = state)
            moveResult.copy(events = moveResult.events + DomainEvent.AutoMovedToFoundation)
        }
        GameAction.DrawFromStock,
        GameAction.RecycleWasteToStock -> ActionResult(
            state = state,
            rejectionReason = RejectionReason.RULE_VIOLATION,
        )
    }

    private fun validateMoveCards(
        action: GameAction.MoveCards,
        state: GameState,
    ): ValidationResult {
        if (action.cardCount <= 0) {
            return ValidationResult.invalid(RejectionReason.INVALID_CARD_COUNT)
        }
        val movingCards = getMovingCards(
            state = state,
            source = action.from,
            cardCount = action.cardCount,
        ) ?: return ValidationResult.invalid(RejectionReason.INVALID_SOURCE)

        if (action.from is PileRef.Tableau && !isAlternatingDescendingSequence(movingCards)) {
            return ValidationResult.invalid(RejectionReason.RULE_VIOLATION)
        }
        if (action.from is PileRef.Tableau && action.cardCount > maximumMovableSequenceSize(state)) {
            return ValidationResult.invalid(RejectionReason.RULE_VIOLATION)
        }

        val firstMovingCard = movingCards.first()
        return when (val destination = action.to) {
            is PileRef.Tableau -> {
                val destinationColumn = state.tableauColumns.getOrNull(destination.columnIndex)
                    ?: return ValidationResult.invalid(RejectionReason.INVALID_DESTINATION)
                val destinationTopCard = destinationColumn.visibleCards.lastOrNull()
                if (CardStackPolicies.canPlaceOnAlternatingDescendingTableau(firstMovingCard, destinationTopCard)) {
                    ValidationResult.valid()
                } else {
                    ValidationResult.invalid(RejectionReason.RULE_VIOLATION)
                }
            }
            is PileRef.Foundation -> {
                if (action.cardCount != 1) {
                    return ValidationResult.invalid(RejectionReason.INVALID_CARD_COUNT)
                }
                if (firstMovingCard.suit != destination.suit) {
                    return ValidationResult.invalid(RejectionReason.RULE_VIOLATION)
                }
                val foundationTopCard = state.foundationPiles[destination.suit]?.lastOrNull()
                if (CardStackPolicies.canPlaceOnFoundation(firstMovingCard, foundationTopCard)) {
                    ValidationResult.valid()
                } else {
                    ValidationResult.invalid(RejectionReason.RULE_VIOLATION)
                }
            }
            is PileRef.FreeCell -> {
                if (action.cardCount != 1) {
                    return ValidationResult.invalid(RejectionReason.INVALID_CARD_COUNT)
                }
                val freeCellValue = state.freeCells.getOrNull(destination.slotIndex)
                    ?: return ValidationResult.invalid(RejectionReason.INVALID_DESTINATION)
                if (freeCellValue == null) {
                    ValidationResult.valid()
                } else {
                    ValidationResult.invalid(RejectionReason.RULE_VIOLATION)
                }
            }
            else -> ValidationResult.invalid(RejectionReason.INVALID_DESTINATION)
        }
    }

    private fun applyMoveCards(
        action: GameAction.MoveCards,
        state: GameState,
    ): ActionResult {
        val movingCards = getMovingCards(
            state = state,
            source = action.from,
            cardCount = action.cardCount,
        ) ?: return ActionResult(state = state, rejectionReason = RejectionReason.INVALID_SOURCE)

        var updatedState = removeMovingCards(
            state = state,
            source = action.from,
            cardCount = action.cardCount,
        )
        updatedState = addMovingCards(
            state = updatedState,
            destination = action.to,
            cards = movingCards,
        )
        val withIncrementedMoveCounter = updatedState.copy(moveCounter = updatedState.moveCounter + 1)
        return ActionResult(
            state = withUpdatedOutcome(withIncrementedMoveCounter),
            events = listOf(DomainEvent.CardsMoved(cardCount = action.cardCount)),
        )
    }

    private fun getMovingCards(
        state: GameState,
        source: PileRef,
        cardCount: Int,
    ): List<Card>? = when (source) {
        is PileRef.Tableau -> {
            val sourceColumn = state.tableauColumns.getOrNull(source.columnIndex) ?: return null
            if (sourceColumn.visibleCards.size < cardCount) {
                null
            } else {
                sourceColumn.visibleCards.takeLast(cardCount)
            }
        }
        is PileRef.FreeCell -> {
            if (cardCount != 1) {
                null
            } else {
                state.freeCells.getOrNull(source.slotIndex)?.let { cellCard ->
                    if (cellCard == null) {
                        null
                    } else {
                        listOf(cellCard)
                    }
                }
            }
        }
        else -> null
    }

    private fun removeMovingCards(
        state: GameState,
        source: PileRef,
        cardCount: Int,
    ): GameState = when (source) {
        is PileRef.Tableau -> {
            val sourceColumn = state.tableauColumns[source.columnIndex]
            val updatedColumns = state.tableauColumns.toMutableList().also {
                it[source.columnIndex] = sourceColumn.copy(
                    visibleCards = sourceColumn.visibleCards.dropLast(cardCount),
                )
            }
            state.copy(tableauColumns = updatedColumns)
        }
        is PileRef.FreeCell -> {
            val updatedFreeCells = state.freeCells.toMutableList().also {
                it[source.slotIndex] = null
            }
            state.copy(freeCells = updatedFreeCells)
        }
        else -> state
    }

    private fun addMovingCards(
        state: GameState,
        destination: PileRef,
        cards: List<Card>,
    ): GameState = when (destination) {
        is PileRef.Tableau -> {
            val destinationColumn = state.tableauColumns[destination.columnIndex]
            val updatedColumns = state.tableauColumns.toMutableList().also {
                it[destination.columnIndex] = destinationColumn.copy(
                    visibleCards = destinationColumn.visibleCards + cards,
                )
            }
            state.copy(tableauColumns = updatedColumns)
        }
        is PileRef.Foundation -> {
            val existingFoundationCards = state.foundationPiles[destination.suit].orEmpty()
            state.copy(
                foundationPiles = state.foundationPiles + (destination.suit to (existingFoundationCards + cards)),
            )
        }
        is PileRef.FreeCell -> {
            val updatedFreeCells = state.freeCells.toMutableList().also {
                it[destination.slotIndex] = cards.single()
            }
            state.copy(freeCells = updatedFreeCells)
        }
        else -> state
    }

    private fun maximumMovableSequenceSize(state: GameState): Int {
        val emptyFreeCellCount = state.freeCells.count { it == null }
        return emptyFreeCellCount + 1
    }

    private fun isAlternatingDescendingSequence(cards: List<Card>): Boolean {
        if (cards.size <= 1) {
            return true
        }
        return cards.zipWithNext().all { (topCard, bottomCard) ->
            topCard.color != bottomCard.color &&
                topCard.rank.pipValue == bottomCard.rank.pipValue + 1
        }
    }

    private fun findSingleAutoMoveToFoundation(state: GameState): GameAction.MoveCards? {
        state.freeCells.forEachIndexed { index, freeCellCard ->
            if (freeCellCard != null && canMoveToFoundation(state, freeCellCard)) {
                return GameAction.MoveCards(
                    from = PileRef.FreeCell(index),
                    to = PileRef.Foundation(freeCellCard.suit),
                    cardCount = 1,
                )
            }
        }

        state.tableauColumns.forEachIndexed { columnIndex, tableauColumn ->
            val tableauTopCard = tableauColumn.visibleCards.lastOrNull() ?: return@forEachIndexed
            if (canMoveToFoundation(state, tableauTopCard)) {
                return GameAction.MoveCards(
                    from = PileRef.Tableau(columnIndex),
                    to = PileRef.Foundation(tableauTopCard.suit),
                    cardCount = 1,
                )
            }
        }
        return null
    }

    private fun canMoveToFoundation(
        state: GameState,
        card: Card,
    ): Boolean {
        val foundationTopCard = state.foundationPiles[card.suit]?.lastOrNull()
        return CardStackPolicies.canPlaceOnFoundation(card, foundationTopCard)
    }

    private fun withUpdatedOutcome(state: GameState): GameState {
        val hasWonGame = Suit.entries.all { suit ->
            state.foundationPiles[suit].orEmpty().size == 13
        }
        return if (hasWonGame) {
            state.copy(outcome = GameOutcome.WON)
        } else {
            state
        }
    }
}
