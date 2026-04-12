import app.FoxPuppetSheetPreviewScene
import app.FoxHeartPuppetSheetPreviewScene
import app.SolitaireScene
import korlibs.korge.Korge
import korlibs.math.geom.Size

suspend fun main() = Korge(
    windowSize = Size(1280, 720),
    title = "KMP Playable V1 - Solitaire",
) {
    val foxPreviewEnv = System.getenv("FOX_PUPPET_PREVIEW")?.lowercase()?.trim()
    val foxPreviewJvmProp = System.getProperty("foxPuppetPreview")?.lowercase()?.trim()
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
