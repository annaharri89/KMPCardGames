import app.playableClientKorgeModule
import korlibs.math.geom.Size

suspend fun main() {
    playableClientKorgeModule(
        windowSize = Size(1280, 720),
        title = "KMP Playable V1 - Solitaire",
        foxPuppetPreviewFromEnvironment = null,
        foxPuppetPreviewFromJvmOrBridge = null,
    ).start()
}
