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

fun foxQueenPuppetBoardRuntime(
    sheet: FoxPuppetSheetFacade,
    slices: FoxPuppetSheetLayout.PuppetSlices,
    bitmapSliceLabel: String,
): FoxQueenPuppetBoardRuntime = FoxQueenPuppetBoardRuntime(
    compositeOffsets = sheet.compositeOffsets,
    tailWagPingPongIndices = sheet.tailWagPingPongIndices,
    tailFrameDurationSec = sheet.spec.tailWagFrameDurationSec,
    microAnimConfig = foxPuppetMicroAnimConfig(sheet, slices.necks.size),
    earPairCount = slices.earPairs.size,
    bitmapSliceLabel = bitmapSliceLabel,
)
