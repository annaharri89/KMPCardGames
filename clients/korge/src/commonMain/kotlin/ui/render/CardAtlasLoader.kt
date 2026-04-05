package ui.render

import korlibs.image.bitmap.Bitmap
import korlibs.image.bitmap.sliceWithBounds
import korlibs.image.format.readBitmap
import korlibs.io.resources.Resources
import korlibs.math.geom.slice.RectSlice

object CardAtlasLoader {
    private const val ATLAS_LOG_TAG = "CardAtlasLoader"

    private val texturePackerFrameRegex = Regex(
        pattern = """"([^"]+\.png)"\s*:\s*\{.*?"frame"\s*:\s*\{\s*"x"\s*:\s*(\d+),\s*"y"\s*:\s*(\d+),\s*"w"\s*:\s*(\d+),\s*"h"\s*:\s*(\d+)""",
        option = RegexOption.DOT_MATCHES_ALL,
    )

    suspend fun loadSliceMapFromResources(resources: Resources): Map<String, RectSlice<Bitmap>>? {
        return try {
            val root = resources.root
            val atlasJson = root["atlas/renamed_full_atlas.json"].readString()
            val atlasBitmap = root["atlas/renamed_full_atlas.png"].readBitmap()
            val sliceByKey = mutableMapOf<String, RectSlice<Bitmap>>()
            texturePackerFrameRegex.findAll(atlasJson).forEach { match ->
                val (fileName, x, y, w, h) = match.destructured
                val slice = atlasBitmap.sliceWithBounds(
                    x.toInt(),
                    y.toInt(),
                    w.toInt(),
                    h.toInt(),
                    fileName,
                )
                val baseNameWithoutExtension = fileName.removeSuffix(".png")
                sliceByKey[baseNameWithoutExtension] = slice
                sliceByKey[fileName] = slice
            }
            println("[$ATLAS_LOG_TAG] loadedAtlasSlices=${sliceByKey.size / 2}")
            sliceByKey
        } catch (throwable: Throwable) {
            println("[$ATLAS_LOG_TAG] atlasLoadFailed message=${throwable.message} cause=${throwable::class.simpleName}")
            null
        }
    }
}
