package ui.render

/**
 * Stage layout and animation tuning for a queen fox puppet on a card (spade or heart sheet).
 * Built from loaded [FoxPuppetSheetLayout.PuppetSlices] plus suit-specific defaults.
 */
data class FoxQueenPuppetBoardRuntime(
    val compositeOffsets: FoxPuppetSheetLayout.CompositeOffsets,
    val tailWagPingPongIndices: List<Int>,
    val tailFrameDurationSec: Double,
    val microAnimConfig: PuppetMicroAnimConfig,
    val earPairCount: Int,
    val bitmapSliceLabel: String,
)

fun foxQueenPuppetBoardRuntimeSpade(slices: FoxPuppetSheetLayout.PuppetSlices): FoxQueenPuppetBoardRuntime =
    FoxQueenPuppetBoardRuntime(
        compositeOffsets = FoxSpadePuppetSheet.compositeOffsets,
        tailWagPingPongIndices = FoxSpadePuppetSheet.tailWagPingPongIndices,
        tailFrameDurationSec = FoxSpadePuppetSheet.TAIL_WAG_FRAME_DURATION_SEC,
        microAnimConfig = foxSpadePuppetMicroAnimConfig(slices.necks.size),
        earPairCount = slices.earPairs.size,
        bitmapSliceLabel = "fox_spade_card_puppet_frame",
    )

fun foxQueenPuppetBoardRuntimeHeart(slices: FoxPuppetSheetLayout.PuppetSlices): FoxQueenPuppetBoardRuntime =
    FoxQueenPuppetBoardRuntime(
        compositeOffsets = FoxHeartPuppetSheet.compositeOffsets,
        tailWagPingPongIndices = FoxHeartPuppetSheet.tailWagPingPongIndices,
        tailFrameDurationSec = FoxHeartPuppetSheet.TAIL_WAG_FRAME_DURATION_SEC,
        microAnimConfig = foxHeartPuppetMicroAnimConfig(slices.earPairs.size, slices.necks.size),
        earPairCount = slices.earPairs.size,
        bitmapSliceLabel = "fox_heart_card_puppet_frame",
    )
