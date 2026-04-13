package ui.render

import korlibs.image.bitmap.Bitmap
import korlibs.image.bitmap.resizedUpTo
import kotlin.math.max

/**
 * Full-size suit pip PNGs are authored large for the pipeline, but drawing them at ~10–34 logical px
 * minifies on the GPU every frame. [clampTextureLongestEdge] bakes a smaller texture once at load
 * (antialiased resize) so sampling is closer to 1:1, memory stays low next to fox puppet sheets, and
 * the face-card puppet hub tick path is unchanged.
 */
internal object SuitPipBitmapNormalize {

    internal const val maxTextureEdgePx = 128

    internal fun clampTextureLongestEdge(source: Bitmap): Bitmap {
        val longestEdge = max(source.width, source.height)
        if (longestEdge <= maxTextureEdgePx) return source
        return source.resizedUpTo(maxTextureEdgePx, maxTextureEdgePx, native = true)
    }
}
