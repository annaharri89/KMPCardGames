package ui.render

import app.CardThemeSpec
import domain.model.Rank
import domain.model.Suit
import korlibs.image.bitmap.Bitmap
import korlibs.image.color.RGBA
import korlibs.korge.view.BaseImage
import korlibs.korge.view.Container
import korlibs.korge.view.View
import korlibs.korge.view.addTo
import korlibs.korge.view.image
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import korlibs.math.geom.Anchor2D
import korlibs.math.geom.slice.RectSlice
import kotlin.math.min

private data class FoxMotifScaledInner(
    val inner: Container,
    val bounds: FoxPuppetSheetLayout.DesignBounds,
    val offsets: FoxPuppetSheetLayout.CompositeOffsets,
)

private fun createFoxQueenPuppetMotifScaledInner(
    motifContainer: Container,
    slices: FoxPuppetSheetLayout.PuppetSlices,
    width: Double,
    height: Double,
    compositeOffsets: FoxPuppetSheetLayout.CompositeOffsets,
): FoxMotifScaledInner {
    val bounds = FoxPuppetSheetLayout.designBounds(compositeOffsets, slices)
    val uniformScale = min(width / bounds.width, height / bounds.height)
    val inner = Container().addTo(motifContainer)
    inner.x = (width - bounds.width * uniformScale) / 2.0
    inner.y = (height - bounds.height * uniformScale) / 2.0
    inner.scaleX = uniformScale
    inner.scaleY = uniformScale
    return FoxMotifScaledInner(inner, bounds, compositeOffsets)
}

internal fun layoutFoxQueenPuppetBoardMotif(
    motifContainer: Container,
    slices: FoxPuppetSheetLayout.PuppetSlices,
    width: Double,
    height: Double,
    compositeOffsets: FoxPuppetSheetLayout.CompositeOffsets,
) {
    val kit = createFoxQueenPuppetMotifScaledInner(motifContainer, slices, width, height, compositeOffsets)
    val inner = kit.inner
    val bounds = kit.bounds
    val offsets = kit.offsets

    fun placeFoxPuppetLayer(bitmap: Bitmap, layerX: Double, layerY: Double, layerScale: Double = offsets.displayScale) {
        inner.image(bitmap, Anchor2D.TOP_LEFT) {
            x = layerX - bounds.left
            y = layerY - bounds.top
            scaleX = layerScale
            scaleY = layerScale
            mouseEnabled = false
        }
    }

    fun placeHead(headBitmap: Bitmap, layerX: Double, layerY: Double) {
        inner.image(headBitmap, Anchor2D.TOP_LEFT) {
            x = layerX - bounds.left
            y = layerY - bounds.top
            scaleX = offsets.displayScale
            scaleY = offsets.displayScale
            mouseEnabled = false
        }
    }

    placeFoxPuppetLayer(slices.tails.first(), offsets.tailX, offsets.tailY)
    val bodyLayerScale = offsets.displayScale * offsets.bodyScaleMultiplier
    placeFoxPuppetLayer(slices.body, offsets.bodyX, offsets.bodyY, layerScale = bodyLayerScale)
    placeFoxPuppetLayer(
        slices.necks.first(),
        FoxPuppetSheetLayout.neckLayerLeftXAlignedToFirstNeckFrame(offsets, slices, 0),
        offsets.neckY,
    )
    placeFoxPuppetLayer(slices.earPairs.first(), offsets.earX, offsets.earY)
    placeHead(
        slices.heads.first(),
        FoxPuppetSheetLayout.headLayerLeftX(offsets, 0),
        offsets.headY,
    )
}

/** Static Q♠ fox stack; [FoxPuppetSheetPreviewScene] and tests. */
internal fun layoutFoxQueenSpadeBoardMotif(
    motifContainer: Container,
    slices: FoxPuppetSheetLayout.PuppetSlices,
    width: Double,
    height: Double,
) {
    layoutFoxQueenPuppetBoardMotif(
        motifContainer,
        slices,
        width,
        height,
        FoxSpadePuppetSheet.compositeOffsets,
    )
}

internal fun mountFoxQueenPuppetAnimatedBoardMotif(
    hub: FaceCardPuppetAnimationHub,
    motifContainer: Container,
    slices: FoxPuppetSheetLayout.PuppetSlices,
    width: Double,
    height: Double,
    slotKey: String,
    runtime: FoxQueenPuppetBoardRuntime,
): FoxSpadePuppetCardAnimator {
    val kit = createFoxQueenPuppetMotifScaledInner(
        motifContainer,
        slices,
        width,
        height,
        runtime.compositeOffsets,
    )
    val inner = kit.inner
    val bounds = kit.bounds
    val offsets = kit.offsets

    fun placeFoxPuppetLayer(bitmap: Bitmap, layerX: Double, layerY: Double, layerScale: Double = offsets.displayScale): BaseImage =
        inner.image(bitmap, Anchor2D.TOP_LEFT) {
            x = layerX - bounds.left
            y = layerY - bounds.top
            scaleX = layerScale
            scaleY = layerScale
            mouseEnabled = false
        }

    fun placeHead(headBitmap: Bitmap, layerX: Double, layerY: Double, alpha: Double): BaseImage =
        inner.image(headBitmap, Anchor2D.TOP_LEFT) {
            x = layerX - bounds.left
            y = layerY - bounds.top
            scaleX = offsets.displayScale
            scaleY = offsets.displayScale
            mouseEnabled = false
            this.alpha = alpha
        }

    val tailLayer = placeFoxPuppetLayer(slices.tails.first(), offsets.tailX, offsets.tailY)
    val bodyLayerScale = offsets.displayScale * offsets.bodyScaleMultiplier
    placeFoxPuppetLayer(slices.body, offsets.bodyX, offsets.bodyY, layerScale = bodyLayerScale)
    val neckLayer = placeFoxPuppetLayer(
        slices.necks.first(),
        FoxPuppetSheetLayout.neckLayerLeftXAlignedToFirstNeckFrame(offsets, slices, 0),
        offsets.neckY,
    )
    val earLayer = placeFoxPuppetLayer(slices.earPairs.first(), offsets.earX, offsets.earY)
    val headLayers = slices.heads.mapIndexed { frameIndex, headBitmap ->
        placeHead(
            headBitmap,
            FoxPuppetSheetLayout.headLayerLeftX(offsets, frameIndex),
            offsets.headY,
            alpha = if (frameIndex == 0) 1.0 else 0.0,
        )
    }

    val seed = slotKey.hashCode()
    val jitter = (slotKey.hashCode() and 0xffff) / 1000.0 % 2.4
    return FoxSpadePuppetCardAnimator(
        hub = hub,
        slices = slices,
        runtime = runtime,
        tailLayer = tailLayer,
        earLayer = earLayer,
        neckLayer = neckLayer,
        headFrameLayers = headLayers,
        seed = seed,
        phaseJitterSec = jitter,
        motifBoundsLeft = bounds.left,
        motifBoundsTop = bounds.top,
    )
}

internal fun mountFoxQueenSpadeAnimatedBoardMotif(
    hub: FaceCardPuppetAnimationHub,
    motifContainer: Container,
    slices: FoxPuppetSheetLayout.PuppetSlices,
    width: Double,
    height: Double,
    slotKey: String,
): FoxSpadePuppetCardAnimator =
    mountFoxQueenPuppetAnimatedBoardMotif(
        hub = hub,
        motifContainer = motifContainer,
        slices = slices,
        width = width,
        height = height,
        slotKey = slotKey,
        runtime = foxQueenPuppetBoardRuntime(FoxSpadePuppetSheet, slices, "fox_spade_card_puppet_frame"),
    )

data class FaceCardPuppetDrawContext(
    val hub: FaceCardPuppetAnimationHub,
    val slotKey: String,
    val animateFoxQueenSpade: Boolean,
)

data class FaceCardAnimalPaintResult(
    val motifRoot: View,
    val foxAnimator: FoxSpadePuppetCardAnimator?,
)

class SuitSymbolPainter(
    private val themeSpec: CardThemeSpec,
    private val sliceByBaseName: Map<String, RectSlice<Bitmap>>?,
    private val heartSuitPipBitmap: Bitmap? = null,
    private val diamondSuitPipBitmap: Bitmap? = null,
    private val spadeSuitPipBitmap: Bitmap? = null,
    private val clubSuitPipBitmap: Bitmap? = null,
) {
    fun draw(
        parentContainer: Container,
        suit: Suit,
        x: Double,
        y: Double,
        symbolWidth: Double,
        symbolHeight: Double,
    ) {
        val customPip = when (suit) {
            Suit.HEARTS -> heartSuitPipBitmap
            Suit.DIAMONDS -> diamondSuitPipBitmap
            Suit.SPADES -> spadeSuitPipBitmap
            Suit.CLUBS -> clubSuitPipBitmap
        }
        if (customPip != null) {
            drawCustomSuitPipBitmap(parentContainer, customPip, x, y, symbolWidth, symbolHeight)
            return
        }
        val textureKey = suitTextureBaseName(suit)
        val slice = sliceByBaseName?.let { map ->
            map[textureKey] ?: map["$textureKey.png"]
        }
        if (slice != null) {
            val sliceWidth = slice.width.toDouble()
            val sliceHeight = slice.height.toDouble()
            parentContainer.image(slice, Anchor2D.TOP_LEFT) {
                this.x = x
                this.y = y
                scaleX = symbolWidth / sliceWidth
                scaleY = symbolHeight / sliceHeight
                mouseEnabled = false
            }
        } else {
            val glyph = SuitGlyphBitmaps.bitmap(suit, suitTextColor(suit))
            val gw = glyph.width.toDouble()
            val gh = glyph.height.toDouble()
            parentContainer.image(glyph, Anchor2D.TOP_LEFT) {
                this.x = x
                this.y = y
                scaleX = symbolWidth / gw
                scaleY = symbolHeight / gh
                mouseEnabled = false
            }
        }
    }

    private fun drawCustomSuitPipBitmap(
        parentContainer: Container,
        pip: Bitmap,
        x: Double,
        y: Double,
        symbolWidth: Double,
        symbolHeight: Double,
    ) {
        val w = pip.width.toDouble()
        val h = pip.height.toDouble()
        val uniformScale = min(symbolWidth / w, symbolHeight / h)
        val drawnW = w * uniformScale
        val drawnH = h * uniformScale
        parentContainer.image(pip, Anchor2D.TOP_LEFT) {
            this.x = x + (symbolWidth - drawnW) / 2.0
            this.y = y + (symbolHeight - drawnH) / 2.0
            scaleX = uniformScale
            scaleY = uniformScale
            mouseEnabled = false
        }
    }

    private fun suitTextureBaseName(suit: Suit): String = when (suit) {
        Suit.CLUBS -> "suit_club_medium_01"
        Suit.DIAMONDS -> "suit_diamond_medium_01"
        Suit.HEARTS -> "suit_heart_medium_01"
        Suit.SPADES -> "suit_spade_medium_01"
    }

    private fun suitTextColor(suit: Suit): RGBA = when (suit) {
        Suit.HEARTS, Suit.DIAMONDS -> themeSpec.redSuitColor
        Suit.CLUBS, Suit.SPADES -> themeSpec.blackSuitColor
    }
}

/**
 * Renders Jack/Queen/King center art from an optional TexturePacker map, or from loaded fox puppet slices
 * for Queen of Spades / Queen of Hearts when no packed face texture is present for that card.
 */
class FaceCardAnimalPainter(
    private val sliceByBaseName: Map<String, RectSlice<Bitmap>>?,
    private val foxSpadePuppetSlices: FoxPuppetSheetLayout.PuppetSlices? = null,
    private val foxHeartPuppetSlices: FoxPuppetSheetLayout.PuppetSlices? = null,
) {
    fun draw(
        parentContainer: Container,
        rank: Rank,
        suit: Suit,
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        suitAccentColor: RGBA,
        puppetDrawContext: FaceCardPuppetDrawContext? = null,
    ): FaceCardAnimalPaintResult {
        val motifContainer = Container().addTo(parentContainer)
        motifContainer.x = x
        motifContainer.y = y
        val textureKey = faceTextureBaseName(rank, suit)
        val slice = textureKey?.let { key ->
            sliceByBaseName?.get(key) ?: sliceByBaseName?.get("$key.png")
        }
        if (slice != null) {
            val sliceWidth = slice.width.toDouble()
            val sliceHeight = slice.height.toDouble()
            motifContainer.image(slice, Anchor2D.TOP_LEFT) {
                this.x = 0.0
                this.y = 0.0
                scaleX = width / sliceWidth
                scaleY = height / sliceHeight
                mouseEnabled = false
            }
            return FaceCardAnimalPaintResult(motifRoot = motifContainer, foxAnimator = null)
        }
        if (rank == Rank.QUEEN && suit == Suit.SPADES && foxSpadePuppetSlices != null) {
            val ctx = puppetDrawContext
            val animator = if (ctx != null && ctx.animateFoxQueenSpade) {
                mountFoxQueenSpadeAnimatedBoardMotif(
                    hub = ctx.hub,
                    motifContainer = motifContainer,
                    slices = foxSpadePuppetSlices,
                    width = width,
                    height = height,
                    slotKey = ctx.slotKey,
                )
            } else {
                layoutFoxQueenSpadeBoardMotif(
                    motifContainer = motifContainer,
                    slices = foxSpadePuppetSlices,
                    width = width,
                    height = height,
                )
                null
            }
            return FaceCardAnimalPaintResult(motifRoot = motifContainer, foxAnimator = animator)
        }
        if (rank == Rank.QUEEN && suit == Suit.HEARTS && foxHeartPuppetSlices != null) {
            val ctx = puppetDrawContext
            val heartRuntime = foxQueenPuppetBoardRuntime(FoxHeartPuppetSheet, foxHeartPuppetSlices, "fox_heart_card_puppet_frame")
            val animator = if (ctx != null && ctx.animateFoxQueenSpade) {
                mountFoxQueenPuppetAnimatedBoardMotif(
                    hub = ctx.hub,
                    motifContainer = motifContainer,
                    slices = foxHeartPuppetSlices,
                    width = width,
                    height = height,
                    slotKey = ctx.slotKey,
                    runtime = heartRuntime,
                )
            } else {
                layoutFoxQueenPuppetBoardMotif(
                    motifContainer = motifContainer,
                    slices = foxHeartPuppetSlices,
                    width = width,
                    height = height,
                    compositeOffsets = heartRuntime.compositeOffsets,
                )
                null
            }
            return FaceCardAnimalPaintResult(motifRoot = motifContainer, foxAnimator = animator)
        }
        motifContainer.solidRect(
            width = width * 0.85,
            height = height * 0.55,
            color = suitAccentColor.withAd(0.22),
        ) {
            this.x = width * 0.075
            this.y = height * 0.22
            mouseEnabled = false
        }
        motifContainer.text(
            text = faceRankLetter(rank),
            textSize = 24.0,
            color = suitAccentColor,
        ) {
            this.x = width * 0.42
            this.y = height * 0.38
            mouseEnabled = false
        }
        return FaceCardAnimalPaintResult(motifRoot = motifContainer, foxAnimator = null)
    }

    private fun faceTextureBaseName(rank: Rank, suit: Suit): String? = when (rank) {
        Rank.QUEEN -> when (suit) {
            Suit.HEARTS -> "rank_q_heart_card"
            Suit.DIAMONDS -> "rank_q_diamond_card"
            Suit.SPADES -> "rank_q_spade_card"
            Suit.CLUBS -> "queen_head_front_01"
        }
        Rank.JACK -> "decor_moon_emblem_01"
        Rank.KING -> "queen_crown_large_01"
        else -> null
    }

    private fun faceRankLetter(rank: Rank): String = when (rank) {
        Rank.JACK -> "J"
        Rank.QUEEN -> "Q"
        Rank.KING -> "K"
        else -> "?"
    }
}
