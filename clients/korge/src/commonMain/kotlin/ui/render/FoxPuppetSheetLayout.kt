package ui.render

import korlibs.image.bitmap.Bitmap
import korlibs.image.bitmap.Bitmap32

/**
 * Shared puppet-sheet geometry for spade and heart fox assets (1376×768 reference).
 * Per-variant rects and defaults live in [spadeSheetSpec] / [heartSheetSpec]; [FoxSpadePuppetSheet] and
 * [FoxHeartPuppetSheet] forward to this module so slicing and stage math stay in one place.
 */
data class FoxPuppetSheetRect(val x: Int, val y: Int, val w: Int, val h: Int)

object FoxPuppetSheetLayout {
    const val DEFAULT_SHEET_WIDTH_PX = 1376
    const val DEFAULT_SHEET_HEIGHT_PX = 768

    data class CompositeOffsets(
        /** Tuned for 1376×768 puppet slices (larger art than legacy 1024×571). */
        val displayScale: Double = 0.782,
        /** Body layer scale = [displayScale] * this; other layers use [displayScale] only. */
        val bodyScaleMultiplier: Double = 1.38,
        val tailX: Double = 763.0,
        val tailY: Double = 288.0,
        val bodyX: Double = 465.0,
        val bodyY: Double = 112.0,
        val neckX: Double = 506.0,
        val neckY: Double = 92.0,
        val earX: Double = 569.0,
        val earY: Double = -60.0,
        val headX: Double = 539.0,
        val headY: Double = -35.0,
        val referenceSheetX: Double = 24.0,
        val referenceSheetY: Double = 400.0,
        val referenceSheetScale: Double = 0.148,
        /**
         * Per ear-pair index: extra downward shift in **sheet pixels** (× [displayScale]) added to [earY].
         * Use when frames sit on different sheet rows but should align on-card; empty list or zeros = shared [earY].
         */
        val earPairExtraSheetYPx: List<Double> = listOf(0.0, 35.0),
    )

    data class PuppetSlices(
        val heads: List<Bitmap>,
        val earPairs: List<Bitmap>,
        val body: Bitmap,
        val tails: List<Bitmap>,
        val necks: List<Bitmap>,
        val neckJunctionLeftX: List<Double>,
    )

    data class DesignBounds(
        val left: Double,
        val top: Double,
        val right: Double,
        val bottom: Double,
    ) {
        val width: Double get() = right - left
        val height: Double get() = bottom - top
    }

    data class Spec(
        val headRects: List<FoxPuppetSheetRect>,
        val earPairRects: List<FoxPuppetSheetRect>,
        val bodyRect: FoxPuppetSheetRect,
        val tailRects: List<FoxPuppetSheetRect>,
        val neckRects: List<FoxPuppetSheetRect>,
        val neckJunctionBandRowCount: Int = 16,
        val defaultCompositeOffsets: CompositeOffsets,
        val blinkHeadFrameIndices: List<Int>,
        val blinkTransitionFps: Double,
        val blinkEyesClosedHoldSec: Double,
        val tailWagPingPongIndices: List<Int>,
    )

    /** Spade fox: equal-size head grid. Regenerate helpers: `clients/korge/scripts/regenerate_fox_head_slices.py`. */
    val spadeSheetSpec = Spec(
        headRects = listOf(
            FoxPuppetSheetRect(40, 12, 262, 275),
            FoxPuppetSheetRect(384, 12, 262, 275),
            FoxPuppetSheetRect(734, 12, 262, 275),
            FoxPuppetSheetRect(1072, 12, 262, 275),
        ),
        earPairRects = listOf(
            FoxPuppetSheetRect(300, 262, 195, 151),
            FoxPuppetSheetRect(300, 440, 195, 113),
        ),
        bodyRect = FoxPuppetSheetRect(527, 258, 337, 325),
        tailRects = listOf(
            FoxPuppetSheetRect(830, 258, 105, 320),
            FoxPuppetSheetRect(941, 258, 105, 320),
            FoxPuppetSheetRect(1051, 258, 105, 320),
            FoxPuppetSheetRect(1161, 258, 105, 320),
            FoxPuppetSheetRect(1271, 258, 97, 320),
        ),
        neckRects = listOf(
            FoxPuppetSheetRect(16, 589, 266, 159),
            FoxPuppetSheetRect(289, 589, 266, 159),
            FoxPuppetSheetRect(562, 589, 266, 159),
            FoxPuppetSheetRect(834, 589, 266, 159),
            FoxPuppetSheetRect(1107, 589, 253, 159),
        ),
        defaultCompositeOffsets = CompositeOffsets(
            bodyScaleMultiplier = 1.38,
            bodyX = 465.0,
            bodyY = 112.0,
            neckX = 506.0,
            neckY = 92.0,
            earX = 569.0,
            earY = -60.0,
            headX = 539.0,
            headY = -35.0,
            earPairExtraSheetYPx = listOf(0.0, 35.0),
        ),
        blinkHeadFrameIndices = listOf(0, 2, 0),
        blinkTransitionFps = 12.0,
        blinkEyesClosedHoldSec = 0.4,
        tailWagPingPongIndices = listOf(0, 1, 2, 3, 4, 3, 2, 1),
    )

    val heartSheetSpec = Spec(
        headRects = listOf(
            FoxPuppetSheetRect(10, 12, 232, 250),
            FoxPuppetSheetRect(384, 12, 262, 250),
            FoxPuppetSheetRect(834, 12, 302, 250),
            FoxPuppetSheetRect(1072, 12, 262, 250),
        ),
        earPairRects = listOf(
            FoxPuppetSheetRect(20, 302, 195, 191),
            FoxPuppetSheetRect(222, 262, 195, 151),
            FoxPuppetSheetRect(20, 420, 195, 151),
            FoxPuppetSheetRect(222, 420, 195, 151),
        ),
        bodyRect = FoxPuppetSheetRect(427, 158, 387, 425),
        tailRects = spadeSheetSpec.tailRects,
        neckRects = listOf(
            FoxPuppetSheetRect(199, 589, 206, 159),//2nd
            FoxPuppetSheetRect(16, 589, 206, 159),//1st
            //FoxPuppetSheetRect(407, 589, 206, 159),//3rd
            //FoxPuppetSheetRect(582, 589, 206, 159),//4th
            //FoxPuppetSheetRect(804, 589, 206, 159),//5th

        ),
        defaultCompositeOffsets = CompositeOffsets(
            bodyScaleMultiplier = 1.18,
            bodyX = 378.0,
            bodyY = 88.0,
            neckX = 540.0,
            neckY = 178.0,
            earX = 540.0,
            earY = -48.0,
            headX = 529.0,
            headY = -36.0,
            earPairExtraSheetYPx = emptyList(),
        ),
        blinkHeadFrameIndices = listOf(0, 2, 0),
        blinkTransitionFps = 12.0,
        blinkEyesClosedHoldSec = .4,
        tailWagPingPongIndices = listOf(0, 1, 2, 3, 4, 3, 2, 1),
    )

    fun buildSlices(sheetBitmap: Bitmap, spec: Spec): PuppetSlices {
        fun extractRegionBitmap(r: FoxPuppetSheetRect): Bitmap {
            val out = Bitmap32(r.w, r.h, premultiplied = true)
            val buffer = IntArray(r.w * r.h)
            sheetBitmap.readPixelsUnsafe(r.x, r.y, r.w, r.h, buffer, offset = 0)
            out.writePixelsUnsafe(0, 0, r.w, r.h, buffer, offset = 0)
            return out
        }

        val neckBitmaps = spec.neckRects.map { rectangle -> extractRegionBitmap(rectangle) }
        val neckJunctionLeftX = neckBitmaps.map { neckBitmap ->
            val h = neckBitmap.height
            val y0 = (h - spec.neckJunctionBandRowCount).coerceAtLeast(0)
            neckBitmap.horizontalOpaqueContentMinXForRows(y0, h)
        }
        return PuppetSlices(
            heads = spec.headRects.map { rectangle -> extractRegionBitmap(rectangle) },
            earPairs = spec.earPairRects.map { rectangle -> extractRegionBitmap(rectangle) },
            body = extractRegionBitmap(spec.bodyRect),
            tails = spec.tailRects.map { rectangle -> extractRegionBitmap(rectangle) },
            necks = neckBitmaps,
            neckJunctionLeftX = neckJunctionLeftX,
        )
    }

    fun neckLayerLeftXAlignedToFirstNeckFrame(
        offsets: CompositeOffsets,
        slices: PuppetSlices,
        neckFrameIndex: Int,
    ): Double {
        val referenceJunctionLeftX = slices.neckJunctionLeftX[0]
        val frameJunctionLeftX = slices.neckJunctionLeftX[neckFrameIndex]
        return offsets.neckX + (referenceJunctionLeftX - frameJunctionLeftX) * offsets.displayScale
    }

    fun earLayerStageY(
        offsets: CompositeOffsets,
        earPairIndex: Int,
        earPairCount: Int,
    ): Double {
        if (earPairCount <= 0) return offsets.earY
        val safeIndex = earPairIndex.coerceIn(0 until earPairCount)
        val extraSheetY = offsets.earPairExtraSheetYPx.getOrElse(safeIndex) { 0.0 }
        return offsets.earY + extraSheetY * offsets.displayScale
    }

    @Suppress("UNUSED_PARAMETER")
    fun headLayerLeftX(offsets: CompositeOffsets, headFrameIndex: Int): Double = offsets.headX

    fun designBounds(offsets: CompositeOffsets, slices: PuppetSlices): DesignBounds {
        val s = offsets.displayScale
        val bodyS = s * offsets.bodyScaleMultiplier
        fun rect(left: Double, top: Double, pixelWidth: Int, pixelHeight: Int): DesignBounds =
            DesignBounds(left, top, left + pixelWidth * s, top + pixelHeight * s)

        val headW = slices.heads.first().width
        val headH = slices.heads.first().height
        var headMinLeft = Double.POSITIVE_INFINITY
        var headMaxRight = Double.NEGATIVE_INFINITY
        for (headIndex in slices.heads.indices) {
            val left = headLayerLeftX(offsets, headIndex)
            val right = left + headW * s
            headMinLeft = kotlin.math.min(headMinLeft, left)
            headMaxRight = kotlin.math.max(headMaxRight, right)
        }
        val headBounds = DesignBounds(
            headMinLeft,
            offsets.headY,
            headMaxRight,
            offsets.headY + headH * s,
        )
        val bodyBounds = DesignBounds(
            offsets.bodyX,
            offsets.bodyY,
            offsets.bodyX + slices.body.width * bodyS,
            offsets.bodyY + slices.body.height * bodyS,
        )
        val earPairCount = slices.earPairs.size
        val neckBounds = neckScreenBounds(offsets, slices)
        val earBounds = earScreenBounds(offsets, slices, earPairCount)
        val layers = listOf(
            rect(offsets.tailX, offsets.tailY, slices.tails.first().width, slices.tails.first().height),
            bodyBounds,
            neckBounds,
            earBounds,
            headBounds,
        )
        return layers.reduce { acc, r ->
            DesignBounds(
                kotlin.math.min(acc.left, r.left),
                kotlin.math.min(acc.top, r.top),
                kotlin.math.max(acc.right, r.right),
                kotlin.math.max(acc.bottom, r.bottom),
            )
        }
    }

    private fun neckScreenBounds(offsets: CompositeOffsets, slices: PuppetSlices): DesignBounds {
        val scale = offsets.displayScale
        val referenceJunctionLeftX = slices.neckJunctionLeftX[0]
        var minLeft = Double.POSITIVE_INFINITY
        var maxRight = Double.NEGATIVE_INFINITY
        for (index in slices.necks.indices) {
            val left =
                offsets.neckX + (referenceJunctionLeftX - slices.neckJunctionLeftX[index]) * scale
            val right = left + slices.necks[index].width * scale
            minLeft = kotlin.math.min(minLeft, left)
            maxRight = kotlin.math.max(maxRight, right)
        }
        val top = offsets.neckY
        val bottom = top + slices.necks.first().height * scale
        return DesignBounds(minLeft, top, maxRight, bottom)
    }

    private fun earScreenBounds(
        offsets: CompositeOffsets,
        slices: PuppetSlices,
        earPairCount: Int,
    ): DesignBounds {
        val scale = offsets.displayScale
        val left = offsets.earX
        var minTop = Double.POSITIVE_INFINITY
        var maxRight = Double.NEGATIVE_INFINITY
        var maxBottom = Double.NEGATIVE_INFINITY
        for (index in slices.earPairs.indices) {
            val top = earLayerStageY(offsets, index, earPairCount)
            val bitmap = slices.earPairs[index]
            minTop = kotlin.math.min(minTop, top)
            maxRight = kotlin.math.max(maxRight, left + bitmap.width * scale)
            maxBottom = kotlin.math.max(maxBottom, top + bitmap.height * scale)
        }
        return DesignBounds(left, minTop, maxRight, maxBottom)
    }
}
