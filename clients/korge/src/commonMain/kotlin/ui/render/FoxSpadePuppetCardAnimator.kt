package ui.render

import korlibs.image.bitmap.Bitmap
import korlibs.image.bitmap.sliceWithBounds
import korlibs.korge.view.BaseImage
import korlibs.math.geom.slice.RectSlice
import kotlin.random.Random

/**
 * Micro-animations (blink, ear twitch, neck swallow) and tail wag for one queen fox puppet on a card.
 * Blink / ear / neck are mutually exclusive and paced by [FoxSpadePuppetMicroAnimDriver].
 * [runtime] supplies suit-specific offsets, tail cadence, and micro-anim config (spade vs heart sheet).
 */
class FoxSpadePuppetCardAnimator(
    private val hub: FaceCardPuppetAnimationHub,
    private val slices: FoxPuppetSheetLayout.PuppetSlices,
    private val runtime: FoxQueenPuppetBoardRuntime,
    private val tailLayer: BaseImage,
    private val earLayer: BaseImage,
    private val neckLayer: BaseImage,
    private val headFrameLayers: List<BaseImage>,
    seed: Int,
    phaseJitterSec: Double,
    private val motifBoundsLeft: Double,
    private val motifBoundsTop: Double,
) : FoxSpadePuppetTickable {

    private val puppetOffsets = runtime.compositeOffsets
    private val rng = Random(seed)
    private val tailSequence = runtime.tailWagPingPongIndices

    private var tailSequenceIndex = 0
    private var tailElapsed = -phaseJitterSec.coerceAtLeast(0.0) * 0.7
    private val tailFrameDuration = runtime.tailFrameDurationSec

    private var displayedHeadFrameIndex = -1

    private val microDriver = FoxSpadePuppetMicroAnimDriver(
        rng = rng,
        phaseJitterSec = phaseJitterSec,
        config = runtime.microAnimConfig,
        showHeadFrame = { frameIndex -> showHeadFrame(frameIndex) },
        applyEarPairIndex = { pairIndex -> applyEarPairIndex(pairIndex) },
        applyNeckFrameIndex = { neckIndex -> applyNeckFrameIndex(neckIndex) },
    )

    init {
        hub.register(this)
    }

    fun dispose() {
        hub.unregister(this)
    }

    private fun showHeadFrame(frameIndex: Int) {
        if (frameIndex == displayedHeadFrameIndex) return
        displayedHeadFrameIndex = frameIndex
        headFrameLayers.forEachIndexed { index, layer ->
            layer.alpha = if (index == frameIndex) 1.0 else 0.0
        }
    }

    private fun applySlice(target: BaseImage, bitmap: Bitmap) {
        val slice: RectSlice<Bitmap> = bitmap.sliceWithBounds(
            0,
            0,
            bitmap.width,
            bitmap.height,
            runtime.bitmapSliceLabel,
        )
        target.bitmap = slice
    }

    private fun applyEarPairIndex(pairIndex: Int) {
        val earPairIndex = pairIndex.coerceIn(slices.earPairs.indices)
        applySlice(earLayer, slices.earPairs[earPairIndex])
        earLayer.y =
            FoxPuppetSheetLayout.earLayerStageY(puppetOffsets, earPairIndex, runtime.earPairCount) -
                motifBoundsTop
    }

    private fun applyNeckFrameIndex(neckFrameIndex: Int) {
        val index = neckFrameIndex.coerceIn(slices.necks.indices)
        applySlice(neckLayer, slices.necks[index])
        neckLayer.x =
            FoxPuppetSheetLayout.neckLayerLeftXAlignedToFirstNeckFrame(
                puppetOffsets,
                slices,
                index,
            ) - motifBoundsLeft
    }

    override fun tick(dtSec: Double) {
        microDriver.tick(dtSec)

        tailElapsed += dtSec
        if (tailElapsed >= tailFrameDuration) {
            tailElapsed = 0.0
            tailSequenceIndex = (tailSequenceIndex + 1) % tailSequence.size
            applySlice(tailLayer, slices.tails[tailSequence[tailSequenceIndex]])
        }
    }
}
