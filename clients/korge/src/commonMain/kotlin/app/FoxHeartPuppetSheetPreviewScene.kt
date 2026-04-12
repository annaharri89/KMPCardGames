package app

import korlibs.image.color.Colors
import korlibs.korge.view.Stage
import korlibs.korge.view.text
import ui.render.FoxHeartPuppetSheet

/**
 * Debug scene: loads [debug/fox_heart_puppet_sheet.png], shows an animated puppet stack and
 * full-sheet thumbnail. Puppet uses neck-swallow-only loop so queen neck motion is always visible.
 */
class FoxHeartPuppetSheetPreviewScene {
    fun install(stage: Stage) {
        with(stage) {
            installFoxPuppetPreview(
                sheet = FoxHeartPuppetSheet,
                sheetPath = "debug/fox_heart_puppet_sheet.png",
                previewId = "fox_heart",
            ) { _, _ ->
                this.text(
                    text = "Fox heart puppet sheet — preview",
                    textSize = 15.0,
                    color = Colors["#222222"],
                ) {
                    x = 24.0
                    y = 14.0
                    mouseEnabled = false
                }
            }
        }
    }
}
