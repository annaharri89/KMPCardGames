package ui.render

import korlibs.image.bitmap.Bitmap

/**
 * Hand-maintained slice geometry for [debug/fox_spade_puppet_sheet.png] (reference size 1376×768).
 * Shared by [app.FoxPuppetSheetPreviewScene] and [FaceCardAnimalPainter] / [SolitaireBoardRenderer]
 * so sheet tweaks stay in one place. Rects and shared math live in [FoxPuppetSheetLayout.spadeSheetSpec].
 *
 * All layers are copied into [Bitmap32] instances: raw sheet slices can report zero width in KorGE 5, so
 * assigning those slices to [korlibs.korge.view.image] would draw nothing for body, tail, ears, and necks.
 *
 * Desktop preview: `FOX_PUPPET_PREVIEW=1 ./gradlew :clients:korge:desktopRun` or
 * `./gradlew :clients:korge:desktopRun -PfoxPuppetPreview=true`
 *
 * **Edit [FoxPuppetSheetLayout.spadeSheetSpec] for slice rects and default offsets** — [FoxHeartPuppetSheet] uses
 * [FoxPuppetSheetLayout.heartSheetSpec] when `FOX_PUPPET_PREVIEW=heart` (or `queen` alias).
 */
object FoxSpadePuppetSheet {
    const val LOG_TAG = "FoxSpadePuppet"

    const val SHEET_WIDTH_PX = FoxPuppetSheetLayout.DEFAULT_SHEET_WIDTH_PX
    const val SHEET_HEIGHT_PX = FoxPuppetSheetLayout.DEFAULT_SHEET_HEIGHT_PX

    private val spec = FoxPuppetSheetLayout.spadeSheetSpec

    val compositeOffsets = spec.defaultCompositeOffsets

    val blinkHeadFrameIndices = spec.blinkHeadFrameIndices

    val blinkTransitionFps = spec.blinkTransitionFps

    val blinkEyesClosedHoldSec = spec.blinkEyesClosedHoldSec

    val tailWagPingPongIndices = spec.tailWagPingPongIndices

    const val TAIL_WAG_FRAME_DURATION_SEC = 0.3

    val earWagPingPongIndices = listOf(0, 1, 0, 1, 0)

    const val EAR_FRAME_DURATION_SEC = 0.1

    const val MICRO_ANIM_IDLE_GAP_MIN_SEC = 3.0
    const val MICRO_ANIM_IDLE_GAP_MAX_SEC = 8.0

    const val MICRO_ANIM_NECK_STEP_DURATION_SEC = 0.1

    const val MICRO_ANIM_WEIGHT_BLINK = 3
    const val MICRO_ANIM_WEIGHT_EAR = 2
    const val MICRO_ANIM_WEIGHT_NECK = 2

    fun neckLayerLeftXAlignedToFirstNeckFrame(
        offsets: FoxPuppetSheetLayout.CompositeOffsets,
        slices: FoxPuppetSheetLayout.PuppetSlices,
        neckFrameIndex: Int,
    ): Double = FoxPuppetSheetLayout.neckLayerLeftXAlignedToFirstNeckFrame(offsets, slices, neckFrameIndex)

    fun earLayerStageY(offsets: FoxPuppetSheetLayout.CompositeOffsets, earPairIndex: Int): Double =
        FoxPuppetSheetLayout.earLayerStageY(offsets, earPairIndex, spec.earPairRects.size)

    fun headLayerLeftX(offsets: FoxPuppetSheetLayout.CompositeOffsets, headFrameIndex: Int): Double =
        FoxPuppetSheetLayout.headLayerLeftX(offsets, headFrameIndex)

    fun buildSlices(sheetBitmap: Bitmap): FoxPuppetSheetLayout.PuppetSlices =
        FoxPuppetSheetLayout.buildSlices(sheetBitmap, spec)

    fun designBounds(
        offsets: FoxPuppetSheetLayout.CompositeOffsets,
        slices: FoxPuppetSheetLayout.PuppetSlices,
    ): FoxPuppetSheetLayout.DesignBounds = FoxPuppetSheetLayout.designBounds(offsets, slices)
}
