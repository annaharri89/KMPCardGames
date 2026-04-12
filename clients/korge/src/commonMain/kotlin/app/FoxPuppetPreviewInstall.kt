package app

import korlibs.image.bitmap.Bitmap
import korlibs.image.bitmap.sliceWithBounds
import korlibs.image.color.Colors
import korlibs.image.format.readBitmap
import korlibs.io.async.launchImmediately
import korlibs.korge.view.BaseImage
import korlibs.korge.view.Container
import korlibs.korge.view.Stage
import korlibs.korge.view.addTo
import korlibs.korge.view.addUpdater
import korlibs.korge.view.image
import korlibs.korge.view.solidRect
import korlibs.math.geom.Anchor2D
import korlibs.math.geom.slice.RectSlice
import ui.render.FoxPuppetSheetFacade
import ui.render.FoxPuppetSheetLayout
import ui.render.FoxSpadePuppetMicroAnimDriver
import ui.render.foxPuppetMicroAnimConfig
import kotlin.random.Random
import kotlin.time.Duration

/**
 * Shared debug-preview installer for fox puppet sheets. Loads [sheetPath], slices it with [sheet],
 * builds an animated puppet stack with neck-swallow-only micro-animations, and shows a full-sheet
 * thumbnail. [extraWidgets] runs after slicing and before the thumbnail, so callers can inject
 * scene-specific labels, card previews, etc. [previewId] is used to tag bitmap slices.
 */
fun Stage.installFoxPuppetPreview(
    sheet: FoxPuppetSheetFacade,
    sheetPath: String,
    previewId: String,
    puppetStageOffsetY: Double = 100.0,
    extraWidgets: Stage.(
        slices: FoxPuppetSheetLayout.PuppetSlices,
        offsets: FoxPuppetSheetLayout.CompositeOffsets,
    ) -> Unit = { _, _ -> },
) {
    solidRect(
        width = views.virtualWidth.toDouble(),
        height = views.virtualHeight.toDouble(),
        color = Colors["#d4d4d4"],
    )

    launchImmediately {
        val sheetBitmap = try {
            resources.root[sheetPath].readBitmap()
        } catch (throwable: Throwable) {
            println(
                "[${sheet.logTag}] sheetLoadFailed path=$sheetPath " +
                    "message=${throwable.message} cause=${throwable::class.simpleName}",
            )
            return@launchImmediately
        }

        if (sheetBitmap.width != FoxPuppetSheetLayout.DEFAULT_SHEET_WIDTH_PX ||
            sheetBitmap.height != FoxPuppetSheetLayout.DEFAULT_SHEET_HEIGHT_PX
        ) {
            println(
                "[${sheet.logTag}] sheetSizeExpected=" +
                    "${FoxPuppetSheetLayout.DEFAULT_SHEET_WIDTH_PX}x${FoxPuppetSheetLayout.DEFAULT_SHEET_HEIGHT_PX} " +
                    "actual=${sheetBitmap.width}x${sheetBitmap.height} — update spec rects",
            )
        }

        val slices = sheet.buildSlices(sheetBitmap)
        val offsets = sheet.compositeOffsets

        extraWidgets(slices, offsets)

        image(
            sheetBitmap.sliceWithBounds(0, 0, sheetBitmap.width, sheetBitmap.height, "${previewId}_full_sheet"),
            Anchor2D.TOP_LEFT,
        ) {
            x = offsets.referenceSheetX
            y = offsets.referenceSheetY
            scaleX = offsets.referenceSheetScale
            scaleY = offsets.referenceSheetScale
            mouseEnabled = false
        }

        val puppetRoot = Container().addTo(this).apply {
            y = puppetStageOffsetY
        }

        fun BaseImage.applyPuppetScale() {
            scaleX = offsets.displayScale
            scaleY = offsets.displayScale
            mouseEnabled = false
        }

        fun applySlice(target: BaseImage, bitmap: Bitmap) {
            val slice: RectSlice<Bitmap> = bitmap.sliceWithBounds(
                0, 0, bitmap.width, bitmap.height, "${previewId}_puppet_frame",
            )
            target.bitmap = slice
        }

        val tailLayer = puppetRoot.image(slices.tails[0], Anchor2D.TOP_LEFT) {
            x = offsets.tailX
            y = offsets.tailY
            applyPuppetScale()
        }
        @Suppress("UNUSED_VARIABLE")
        val bodyLayer = puppetRoot.image(slices.body, Anchor2D.TOP_LEFT) {
            x = offsets.bodyX
            y = offsets.bodyY
            scaleX = offsets.displayScale * offsets.bodyScaleMultiplier
            scaleY = offsets.displayScale * offsets.bodyScaleMultiplier
            mouseEnabled = false
        }
        val neckLayer = puppetRoot.image(slices.necks[0], Anchor2D.TOP_LEFT) {
            x = sheet.neckLayerLeftXAlignedToFirstNeckFrame(offsets, slices, 0)
            y = offsets.neckY
            applyPuppetScale()
        }
        val earLayer = puppetRoot.image(slices.earPairs[0], Anchor2D.TOP_LEFT) {
            x = offsets.earX
            y = sheet.earLayerStageY(offsets, 0)
            applyPuppetScale()
        }

        val headFrameLayers = slices.heads.mapIndexed { frameIndex, headBitmap ->
            puppetRoot.image(headBitmap, Anchor2D.TOP_LEFT) {
                x = sheet.headLayerLeftX(offsets, frameIndex)
                y = offsets.headY
                applyPuppetScale()
                alpha = if (frameIndex == 0) 1.0 else 0.0
            }
        }

        var displayedHeadFrameIndex = -1
        fun showHeadFrame(frameIndex: Int) {
            if (frameIndex == displayedHeadFrameIndex) return
            displayedHeadFrameIndex = frameIndex
            headFrameLayers.forEachIndexed { index, layer ->
                layer.alpha = if (index == frameIndex) 1.0 else 0.0
            }
        }

        val tailSequence = sheet.tailWagPingPongIndices
        var tailSequenceIndex = 0
        var tailElapsed = 0.0
        val tailFrameDurationSec = sheet.spec.tailWagFrameDurationSec

        val microDriver = FoxSpadePuppetMicroAnimDriver(
            rng = Random.Default,
            phaseJitterSec = 0.0,
            config = foxPuppetMicroAnimConfig(sheet, slices.necks.size),
            showHeadFrame = { frameIndex -> showHeadFrame(frameIndex) },
            applyEarPairIndex = { pairIndex ->
                val i = pairIndex.coerceIn(slices.earPairs.indices)
                applySlice(earLayer, slices.earPairs[i])
                earLayer.y = sheet.earLayerStageY(offsets, i)
            },
            applyNeckFrameIndex = { neckIndex ->
                val i = neckIndex.coerceIn(slices.necks.indices)
                applySlice(neckLayer, slices.necks[i])
                neckLayer.x = sheet.neckLayerLeftXAlignedToFirstNeckFrame(offsets, slices, i)
            },
            neckSwallowLoopOnly = true,
        )

        puppetRoot.addUpdater { dt: Duration ->
            val dtSec = dt.inWholeNanoseconds.toDouble() / 1_000_000_000.0
            microDriver.tick(dtSec)
            tailElapsed += dtSec
            if (tailElapsed >= tailFrameDurationSec) {
                tailElapsed = 0.0
                tailSequenceIndex = (tailSequenceIndex + 1) % tailSequence.size
                applySlice(tailLayer, slices.tails[tailSequence[tailSequenceIndex]])
            }
        }

        println(
            "[${sheet.logTag}] previewReady heads=${slices.heads.size} ears=${slices.earPairs.size} " +
                "tails=${slices.tails.size} necks=${slices.necks.size}",
        )
    }
}
