package app

import domain.model.Suit
import korlibs.image.color.Colors
import korlibs.korge.view.Container
import korlibs.korge.view.Stage
import korlibs.korge.view.addTo
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import ui.render.FoxSpadePuppetSheet
import ui.render.SolitaireBoardFaceCardMetrics
import ui.render.SuitSymbolPainter
import ui.render.layoutFoxQueenSpadeBoardMotif

/**
 * Debug-only scene: loads [debug/fox_spade_puppet_sheet.png], shows an animated stacked puppet,
 * a full-sheet thumbnail, and a static Q♠ card center ([layoutFoxQueenSpadeBoardMotif] at
 * [SolitaireBoardFaceCardMetrics] sizes). Puppet uses neck-swallow-only loop; in-game Q♠ uses
 * full random micro-animations.
 */
class FoxPuppetSheetPreviewScene {
    fun install(stage: Stage) {
        with(stage) {
            installFoxPuppetPreview(
                sheet = FoxSpadePuppetSheet,
                sheetPath = "debug/fox_spade_puppet_sheet.png",
                previewId = "fox_spade",
            ) { slices, _ ->
                val themeSpec = cardThemeSpec(CardTheme.KAWAII_NATURE)
                val cardM = SolitaireBoardFaceCardMetrics

                this.text(
                    text = "Solitaire board — Q♠ static center (matches game)",
                    textSize = 15.0,
                    color = Colors["#222222"],
                ) {
                    x = 752.0
                    y = 14.0
                    mouseEnabled = false
                }

                val boardCardPeek = Container().addTo(this).apply {
                    x = 752.0
                    y = 44.0
                    scaleX = 1.85
                    scaleY = 1.85
                }
                boardCardPeek.solidRect(
                    width = cardM.cardWidth,
                    height = cardM.cardHeight,
                    color = Colors.BLACK.withAd(themeSpec.shadowAlpha),
                ) {
                    x = themeSpec.shadowOffset
                    y = themeSpec.shadowOffset
                    mouseEnabled = false
                }
                boardCardPeek.solidRect(
                    width = cardM.cardWidth,
                    height = cardM.cardHeight,
                    color = themeSpec.cardFrontColor,
                ) {
                    mouseEnabled = false
                }
                boardCardPeek.solidRect(
                    width = cardM.cardWidth,
                    height = themeSpec.borderWidth,
                    color = themeSpec.cardBorderColor,
                ) { mouseEnabled = false }
                boardCardPeek.solidRect(
                    width = cardM.cardWidth,
                    height = themeSpec.borderWidth,
                    color = themeSpec.cardBorderColor,
                ) {
                    y = cardM.cardHeight - themeSpec.borderWidth
                    mouseEnabled = false
                }
                boardCardPeek.solidRect(
                    width = themeSpec.borderWidth,
                    height = cardM.cardHeight,
                    color = themeSpec.cardBorderColor,
                ) { mouseEnabled = false }
                boardCardPeek.solidRect(
                    width = themeSpec.borderWidth,
                    height = cardM.cardHeight,
                    color = themeSpec.cardBorderColor,
                ) {
                    x = cardM.cardWidth - themeSpec.borderWidth
                    mouseEnabled = false
                }
                boardCardPeek.text(
                    text = "Q",
                    textSize = themeSpec.rankTextSize,
                    color = themeSpec.blackSuitColor,
                ) {
                    x = 8.0
                    y = 6.0
                    mouseEnabled = false
                }
                val boardSuitPainter = SuitSymbolPainter(themeSpec, sliceByBaseName = null)
                boardSuitPainter.draw(
                    parentContainer = boardCardPeek,
                    suit = Suit.SPADES,
                    x = 9.0,
                    y = 30.0,
                    symbolWidth = 22.0,
                    symbolHeight = 18.0,
                )
                boardSuitPainter.draw(
                    parentContainer = boardCardPeek,
                    suit = Suit.SPADES,
                    x = cardM.cardWidth - 31.0,
                    y = cardM.cardHeight - 26.0,
                    symbolWidth = 22.0,
                    symbolHeight = 18.0,
                )
                val boardMotifSlot = Container().addTo(boardCardPeek)
                boardMotifSlot.x = (cardM.cardWidth - cardM.largeFaceMotifWidth) / 2.0
                boardMotifSlot.y = (cardM.cardHeight - cardM.largeFaceMotifHeight) / 2.0
                layoutFoxQueenSpadeBoardMotif(
                    motifContainer = boardMotifSlot,
                    slices = slices,
                    width = cardM.largeFaceMotifWidth,
                    height = cardM.largeFaceMotifHeight,
                )
            }
        }
    }
}
