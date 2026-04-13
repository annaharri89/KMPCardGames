import app.playableClientKorgeModule
import korlibs.math.geom.Size
import platform.Foundation.NSProcessInfo

suspend fun main() {
    playableClientKorgeModule(
        windowSize = Size(1280, 720),
        title = "KMP Playable V1 - Solitaire",
        foxPuppetPreviewFromEnvironment = iosFoxPuppetPreviewFromProcessEnvironment(),
        foxPuppetPreviewFromJvmOrBridge = iosFoxPuppetPreviewFromProcessArguments(),
    ).start()
}

private fun iosFoxPuppetPreviewFromProcessEnvironment(): String? {
    val env = NSProcessInfo.processInfo().environment
    return listOf("FOX_PUPPET_PREVIEW", "foxPuppetPreview")
        .firstNotNullOfOrNull { key -> env[key] as? String }
}

private fun iosFoxPuppetPreviewFromProcessArguments(): String? {
    val args = NSProcessInfo.processInfo().arguments
    var index = 0
    while (index < args.size) {
        val arg = args[index] as? String
        if (arg == null) {
            index++
            continue
        }
        if (arg == "--foxPuppetPreview" && index + 1 < args.size) {
            return args[index + 1] as? String
        }
        index++
    }
    return null
}
