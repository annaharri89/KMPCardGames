import app.SolitaireScene
import korlibs.korge.Korge
import korlibs.math.geom.Size

suspend fun main() = Korge(
    windowSize = Size(1280, 720),
    title = "KMP Playable V1 - Solitaire",
) {
    SolitaireScene().install(this)
}
