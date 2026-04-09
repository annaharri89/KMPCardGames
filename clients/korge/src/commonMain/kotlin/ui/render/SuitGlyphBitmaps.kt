package ui.render

import domain.model.Suit
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import kotlin.math.abs

/**
 * KorGE default text fonts often omit ♥♦♣♠, so Unicode suit [korlibs.korge.view.text] draws nothing.
 * These small bitmaps render the same shapes as the image-atlas path when no slice is loaded.
 */
internal object SuitGlyphBitmaps {

    private const val rasterSizePx = 64

    private val cache = HashMap<Pair<Suit, Int>, Bitmap32>()

    fun bitmap(suit: Suit, color: RGBA): Bitmap32 {
        val key = suit to color.value
        cache[key]?.let { return it }
        val bmp = Bitmap32(rasterSizePx, rasterSizePx, premultiplied = true)
        val w = rasterSizePx.toDouble()
        val h = rasterSizePx.toDouble()
        for (iy in 0 until rasterSizePx) {
            val py = iy + 0.5
            for (ix in 0 until rasterSizePx) {
                val px = ix + 0.5
                if (pixelInsideSuit(suit, px, py, w, h)) {
                    bmp[ix, iy] = color
                }
            }
        }
        cache[key] = bmp
        return bmp
    }

    private fun pixelInsideSuit(suit: Suit, px: Double, py: Double, w: Double, h: Double): Boolean = when (suit) {
        Suit.DIAMONDS -> insideDiamond(px, py, w, h)
        Suit.HEARTS -> insideHeart(px, py, w, h)
        Suit.CLUBS -> insideClub(px, py, w, h)
        Suit.SPADES -> insideSpade(px, py, w, h)
    }

    private fun insideDiamond(px: Double, py: Double, w: Double, h: Double): Boolean {
        val cx = w / 2.0
        val cy = h / 2.0
        val rx = w / 2.0
        val ry = h / 2.0
        return abs(px - cx) / rx + abs(py - cy) / ry <= 1.0
    }

    private fun insideHeart(px: Double, py: Double, w: Double, h: Double): Boolean {
        val r = w * 0.19
        val cy = h * 0.30
        if (insideEllipse(px, py, w * 0.30, cy, r, r * 1.08)) return true
        if (insideEllipse(px, py, w * 0.70, cy, r, r * 1.08)) return true
        return pointInTriangle(
            px, py,
            w / 2.0, h * 0.96,
            w * 0.14, h * 0.40,
            w * 0.86, h * 0.40,
        )
    }

    private fun insideClub(px: Double, py: Double, w: Double, h: Double): Boolean {
        val cx = w / 2.0
        val stem = px in (cx - w * 0.07)..(cx + w * 0.07) && py >= h * 0.48
        if (stem) return true
        val bumpR = w * 0.17
        if (insideCircle(px, py, w * 0.28, h * 0.30, bumpR)) return true
        if (insideCircle(px, py, w * 0.50, h * 0.22, bumpR)) return true
        if (insideCircle(px, py, w * 0.72, h * 0.30, bumpR)) return true
        return false
    }

    private fun insideSpade(px: Double, py: Double, w: Double, h: Double): Boolean {
        val cx = w / 2.0
        if (insideEllipse(px, py, cx, h * 0.26, w * 0.46, h * 0.22)) return true
        if (pointInTriangle(px, py, cx, h * 0.98, w * 0.18, h * 0.38, w * 0.82, h * 0.38)) return true
        return px in (cx - w * 0.07)..(cx + w * 0.07) && py >= h * 0.55
    }

    private fun insideEllipse(px: Double, py: Double, cx: Double, cy: Double, rx: Double, ry: Double): Boolean {
        if (rx <= 0.0 || ry <= 0.0) return false
        val dx = (px - cx) / rx
        val dy = (py - cy) / ry
        return dx * dx + dy * dy <= 1.0
    }

    private fun insideCircle(px: Double, py: Double, cx: Double, cy: Double, r: Double): Boolean {
        if (r <= 0.0) return false
        val dx = px - cx
        val dy = py - cy
        return dx * dx + dy * dy <= r * r
    }

    private fun pointInTriangle(
        px: Double,
        py: Double,
        x1: Double,
        y1: Double,
        x2: Double,
        y2: Double,
        x3: Double,
        y3: Double,
    ): Boolean {
        fun cross(px: Double, py: Double, x1: Double, y1: Double, x2: Double, y2: Double) =
            (px - x2) * (y1 - y2) - (x1 - x2) * (py - y2)
        val d1 = cross(px, py, x1, y1, x2, y2)
        val d2 = cross(px, py, x2, y2, x3, y3)
        val d3 = cross(px, py, x3, y3, x1, y1)
        val hasNeg = d1 < 0.0 || d2 < 0.0 || d3 < 0.0
        val hasPos = d1 > 0.0 || d2 > 0.0 || d3 > 0.0
        return !(hasNeg && hasPos)
    }
}
