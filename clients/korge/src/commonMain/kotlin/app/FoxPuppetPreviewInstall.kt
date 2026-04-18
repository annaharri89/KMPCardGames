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
import ui.render.FoxHeartPuppetSheet
import ui.render.FoxHeartQueenAnimationDebugFlags
import ui.render.FoxPuppetSheetFacade
import ui.render.FoxPuppetSheetLayout
import ui.render.FoxQueenPuppetBoardTailVisibility
import ui.render.FoxSpadePuppetMicroAnimDriver
import ui.render.foxPuppetMicroAnimConfig
import kotlin.random.Random
import kotlin.time.Duration

/**
 * Installs the shared fox-sheet debug preview scene.
 *
 * Loads [sheetPath], builds slices with [sheet], runs [extraWidgets], then draws the sheet thumbnail and puppet stack.
 * [previewId] is used to name generated bitmap slices.
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

        val stepTailWagWithDtSec: ((Double) -> Unit)? =
            if (FoxQueenPuppetBoardTailVisibility.showTailOnBoardMotif) {
                val tailLayer = puppetRoot.image(slices.tails[0], Anchor2D.TOP_LEFT) {
                    x = offsets.tailX
                    y = offsets.tailY
                    applyPuppetScale()
                }
                val tailSequence = sheet.tailWagPingPongIndices
                var tailSequenceIndex = 0
                var tailElapsed = 0.0
                val tailFrameDurationSec = sheet.spec.tailWagFrameDurationSec
                { dtSec ->
                    tailElapsed += dtSec
                    if (tailElapsed >= tailFrameDurationSec) {
                        tailElapsed = 0.0
                        tailSequenceIndex = (tailSequenceIndex + 1) % tailSequence.size
                        applySlice(tailLayer, slices.tails[tailSequence[tailSequenceIndex]])
                    }
                }
            } else {
                null
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
        if (sheet === FoxHeartPuppetSheet && FoxHeartQueenAnimationDebugFlags.hideNeckOnCard) {
            neckLayer.visible = false
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

        val heartSheet = sheet === FoxHeartPuppetSheet
        val heartPreviewStatic =
            heartSheet && FoxHeartQueenAnimationDebugFlags.suppressAllCardAnimations
        val heartBlinkOnlyPreview =
            heartSheet && FoxHeartQueenAnimationDebugFlags.suppressNonBlinkCardAnimations
        if (!heartPreviewStatic) {
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
                neckSwallowLoopOnly = !heartBlinkOnlyPreview,
                blinkOnlyMicroAnims = heartBlinkOnlyPreview,
            )

            puppetRoot.addUpdater { dt: Duration ->
                val dtSec = dt.inWholeNanoseconds.toDouble() / 1_000_000_000.0
                microDriver.tick(dtSec)
                if (!heartBlinkOnlyPreview) {
                    stepTailWagWithDtSec?.invoke(dtSec)
                }
            }
        }

        println(
            "[${sheet.logTag}] previewReady heads=${slices.heads.size} ears=${slices.earPairs.size} " +
                "tails=${slices.tails.size} necks=${slices.necks.size}",
        )
    }
}
