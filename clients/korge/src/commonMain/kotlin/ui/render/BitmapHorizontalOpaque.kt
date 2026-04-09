package ui.render

import korlibs.image.bitmap.Bitmap
import kotlin.math.min

private const val OPAQUE_ALPHA_THRESHOLD = 28

/** Leftmost opaque x in [rowStartInclusive, rowEndExclusive); empty band → width * 0.5. */
internal fun Bitmap.horizontalOpaqueContentMinXForRows(
    rowStartInclusive: Int,
    rowEndExclusive: Int,
): Double {
    val bitmapWidth = width
    val bitmapHeight = height
    if (bitmapWidth <= 0 || bitmapHeight <= 0) {
        return 0.0
    }
    val y0 = rowStartInclusive.coerceIn(0, bitmapHeight)
    val y1 = rowEndExclusive.coerceIn(0, bitmapHeight)
    if (y0 >= y1) {
        return bitmapWidth * 0.5
    }
    var columnMin = bitmapWidth
    val rowBuffer = IntArray(bitmapWidth)
    for (y in y0 until y1) {
        readPixelsUnsafe(0, y, bitmapWidth, 1, rowBuffer, offset = 0)
        for (x in 0 until bitmapWidth) {
            val alpha = (rowBuffer[x] ushr 24) and 0xFF
            if (alpha > OPAQUE_ALPHA_THRESHOLD) {
                columnMin = min(columnMin, x)
            }
        }
    }
    if (columnMin >= bitmapWidth) {
        return bitmapWidth * 0.5
    }
    return columnMin.toDouble()
}
