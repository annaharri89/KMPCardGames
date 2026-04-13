import app.playableClientKorgeModule
import korlibs.math.geom.Size

suspend fun main() {
    playableClientKorgeModule(
        windowSize = Size(1280, 720),
        title = "KMP Playable V1 - Solitaire",
        foxPuppetPreviewFromEnvironment = System.getenv("FOX_PUPPET_PREVIEW")?.lowercase()?.trim(),
        foxPuppetPreviewFromJvmOrBridge = System.getProperty("foxPuppetPreview")?.lowercase()?.trim(),
    ).start()
}
