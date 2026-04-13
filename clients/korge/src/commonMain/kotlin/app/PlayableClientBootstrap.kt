package app

import korlibs.korge.Korge
import korlibs.korge.view.Stage
import korlibs.math.geom.Size

fun playableClientKorgeModule(
    windowSize: Size,
    title: String,
    foxPuppetPreviewFromEnvironment: String?,
    foxPuppetPreviewFromJvmOrBridge: String?,
): Korge =
    Korge(
        windowSize = windowSize,
        title = title,
        main = {
            installPlayableClient(
                foxPuppetPreviewFromEnvironment = foxPuppetPreviewFromEnvironment,
                foxPuppetPreviewFromJvmOrBridge = foxPuppetPreviewFromJvmOrBridge,
            )
        },
    )

suspend fun Stage.installPlayableClient(
    foxPuppetPreviewFromEnvironment: String?,
    foxPuppetPreviewFromJvmOrBridge: String?,
) {
    val foxPreviewEnv = foxPuppetPreviewFromEnvironment?.lowercase()?.trim()
    val foxPreviewJvmProp = foxPuppetPreviewFromJvmOrBridge?.lowercase()?.trim()
    when {
        foxPreviewEnv == "heart" ||
            foxPreviewEnv == "queen" ||
            foxPreviewJvmProp == "heart" ||
            foxPreviewJvmProp == "queen" ->
            FoxHeartPuppetSheetPreviewScene().install(this)
        foxPreviewJvmProp == "true" ||
            foxPreviewEnv == "1" ||
            foxPreviewEnv == "true" ||
            foxPreviewEnv == "spade" ->
            FoxPuppetSheetPreviewScene().install(this)
        else -> SolitaireScene().install(this)
    }
}
