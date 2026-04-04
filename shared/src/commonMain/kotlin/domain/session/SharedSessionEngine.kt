package domain.session

import domain.rules.RuleSet

class SharedSessionEngine(ruleSets: Set<RuleSet>) {
    val sessionManager: GameSessionManager = GameSessionManager(ruleSets = ruleSets)
}
