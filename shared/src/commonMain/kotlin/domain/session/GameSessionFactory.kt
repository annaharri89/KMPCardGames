package domain.session

import domain.rules.freecell.FreeCellRuleSet
import domain.rules.solitaire.SolitaireRuleSet

object GameSessionFactory {
    fun createDefaultSessionManager(): GameSessionManager = GameSessionManager(
        ruleSets = setOf(
            SolitaireRuleSet(),
            FreeCellRuleSet(),
        ),
    )
}
