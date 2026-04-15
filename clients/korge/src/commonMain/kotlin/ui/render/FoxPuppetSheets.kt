package ui.render

import korlibs.image.bitmap.Bitmap

/**
 * Small suit-specific wrapper around [FoxPuppetSheetLayout].
 *
 * Keeps the log tag with the layout [spec] and forwards geometry/slicing helpers.
 * Prefer [FoxSpadePuppetSheet] or [FoxHeartPuppetSheet].
 */
class FoxPuppetSheetFacade(val logTag: String, val spec: FoxPuppetSheetLayout.Spec) {
    val compositeOffsets get() = spec.defaultCompositeOffsets
    val blinkHeadFrameIndices get() = spec.blinkHeadFrameIndices
    val blinkTransitionFps get() = spec.blinkTransitionFps
    val blinkEyesClosedHoldSec get() = spec.blinkEyesClosedHoldSec
    val tailWagPingPongIndices get() = spec.tailWagPingPongIndices
    val earWagPingPongIndices get() = spec.earWagPingPongIndices

    fun buildSlices(sheetBitmap: Bitmap): FoxPuppetSheetLayout.PuppetSlices =
        FoxPuppetSheetLayout.buildSlices(sheetBitmap, spec)

    fun neckLayerLeftXAlignedToFirstNeckFrame(
        offsets: FoxPuppetSheetLayout.CompositeOffsets,
        slices: FoxPuppetSheetLayout.PuppetSlices,
        neckFrameIndex: Int,
    ): Double = FoxPuppetSheetLayout.neckLayerLeftXAlignedToFirstNeckFrame(offsets, slices, neckFrameIndex)

    fun earLayerStageY(offsets: FoxPuppetSheetLayout.CompositeOffsets, earPairIndex: Int): Double =
        FoxPuppetSheetLayout.earLayerStageY(offsets, earPairIndex, spec.earPairRects.size)

    fun headLayerLeftX(offsets: FoxPuppetSheetLayout.CompositeOffsets, headFrameIndex: Int): Double =
        FoxPuppetSheetLayout.headLayerLeftX(offsets, headFrameIndex)

    fun designBounds(
        offsets: FoxPuppetSheetLayout.CompositeOffsets,
        slices: FoxPuppetSheetLayout.PuppetSlices,
        includeTailInDesignBounds: Boolean = true,
        includeNeckInDesignBounds: Boolean = true,
    ): FoxPuppetSheetLayout.DesignBounds =
        FoxPuppetSheetLayout.designBounds(offsets, slices, includeTailInDesignBounds, includeNeckInDesignBounds)
}

val FoxSpadePuppetSheet = FoxPuppetSheetFacade("FoxSpadePuppet", FoxPuppetSheetLayout.spadeSheetSpec)
val FoxHeartPuppetSheet = FoxPuppetSheetFacade("FoxHeartPuppet", FoxPuppetSheetLayout.heartSheetSpec)
