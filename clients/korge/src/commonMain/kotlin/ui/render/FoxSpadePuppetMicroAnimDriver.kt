package ui.render

import kotlin.random.Random

/** Drives blink, ear, and neck micro-animations for one puppet. */
data class PuppetMicroAnimConfig(
    val blinkHeadFrameIndices: List<Int>,
    val blinkTransitionFps: Double,
    val blinkEyesClosedHoldSec: Double,
    val earPassPairIndices: List<Int>,
    val earFrameDurationSec: Double,
    val neckFrameDurationSec: Double,
    val neckAdvanceCountPerSwallow: Int,
    val idleGapMinSec: Double,
    val idleGapMaxSec: Double,
    val weightBlink: Int,
    val weightEar: Int,
    val weightNeck: Int,
)

fun foxPuppetMicroAnimConfig(sheet: FoxPuppetSheetFacade, neckFrameCount: Int): PuppetMicroAnimConfig =
    PuppetMicroAnimConfig(
        blinkHeadFrameIndices = sheet.blinkHeadFrameIndices,
        blinkTransitionFps = sheet.blinkTransitionFps,
        blinkEyesClosedHoldSec = sheet.blinkEyesClosedHoldSec,
        earPassPairIndices = sheet.earWagPingPongIndices,
        earFrameDurationSec = sheet.spec.earWagFrameDurationSec,
        neckFrameDurationSec = sheet.spec.microAnimNeckStepDurationSec,
        neckAdvanceCountPerSwallow = neckFrameCount,
        idleGapMinSec = sheet.spec.microAnimIdleGapMinSec,
        idleGapMaxSec = sheet.spec.microAnimIdleGapMaxSec,
        weightBlink = sheet.spec.microAnimWeightBlink,
        weightEar = sheet.spec.microAnimWeightEar,
        weightNeck = sheet.spec.microAnimWeightNeck,
    )

/** Returns one symmetric ear pass: 0..n-1 then n-2..0. */
fun earPassIndicesForEarPairCount(earPairCount: Int): List<Int> {
    if (earPairCount <= 1) return List(earPairCount.coerceAtLeast(1)) { 0 }
    return buildList {
        addAll(0 until earPairCount)
        addAll(earPairCount - 2 downTo 0)
    }
}

private enum class MicroAnimPick {
    Blink,
    Ear,
    Neck,
}

private sealed class ActiveMicroAnim {
    data class Blinking(var stepIndex: Int, var timeInStep: Double) : ActiveMicroAnim()
    data class EarPass(var stepIndex: Int, var timeInStep: Double) : ActiveMicroAnim()
    data class NeckSwallow(var frameIndex: Int, var timeInStep: Double, var advancesRemaining: Int) :
        ActiveMicroAnim()
}

private fun ActiveMicroAnim.toPick(): MicroAnimPick =
    when (this) {
        is ActiveMicroAnim.Blinking -> MicroAnimPick.Blink
        is ActiveMicroAnim.EarPass -> MicroAnimPick.Ear
        is ActiveMicroAnim.NeckSwallow -> MicroAnimPick.Neck
    }

class FoxSpadePuppetMicroAnimDriver(
    private val rng: Random,
    phaseJitterSec: Double,
    private val config: PuppetMicroAnimConfig,
    private val showHeadFrame: (Int) -> Unit,
    private val applyEarPairIndex: (Int) -> Unit,
    private val applyNeckFrameIndex: (Int) -> Unit,
    /** Preview mode: skip idle/blink/ear and loop neck swallow continuously. */
    private val neckSwallowLoopOnly: Boolean = false,
    /** If true, only blink runs after idle gaps (ignored when [neckSwallowLoopOnly] is true). */
    private val blinkOnlyMicroAnims: Boolean = false,
) {
    private val blinkQuickHoldSec = 1.0 / config.blinkTransitionFps

    private var idleSecondsRemaining: Double =
        if (neckSwallowLoopOnly) {
            0.0
        } else {
            rng.nextDouble(config.idleGapMinSec, config.idleGapMaxSec) +
                phaseJitterSec.coerceAtLeast(0.0) * 0.4
        }

    private var active: ActiveMicroAnim? = null

    private var lastCompletedKind: MicroAnimPick? = null
    private var consecutiveSameKindCompleted: Int = 0

    init {
        applyFullRest()
    }

    fun tick(dtSec: Double) {
        val current = active
        if (current == null) {
            idleSecondsRemaining -= dtSec
            if (idleSecondsRemaining <= 0.0) {
                if (neckSwallowLoopOnly) {
                    startNeckSwallowFromRest()
                } else {
                    startRandomMicroAnim()
                }
            }
            return
        }

        when (current) {
            is ActiveMicroAnim.Blinking -> tickBlink(current, dtSec)
            is ActiveMicroAnim.EarPass -> tickEarPass(current, dtSec)
            is ActiveMicroAnim.NeckSwallow -> tickNeckSwallow(current, dtSec)
        }
    }

    private fun blinkHoldSecForStep(stepIndex: Int): Double {
        if (stepIndex < 0 || stepIndex >= config.blinkHeadFrameIndices.size) return blinkQuickHoldSec
        return if (config.blinkHeadFrameIndices[stepIndex] == 2) {
            config.blinkEyesClosedHoldSec
        } else {
            blinkQuickHoldSec
        }
    }

    private fun tickBlink(state: ActiveMicroAnim.Blinking, dtSec: Double) {
        state.timeInStep += dtSec
        if (state.timeInStep < blinkHoldSecForStep(state.stepIndex)) return
        state.timeInStep = 0.0
        state.stepIndex++
        if (state.stepIndex >= config.blinkHeadFrameIndices.size) {
            finishMicroAnim()
            return
        }
        showHeadFrame(config.blinkHeadFrameIndices[state.stepIndex])
    }

    private fun tickEarPass(state: ActiveMicroAnim.EarPass, dtSec: Double) {
        state.timeInStep += dtSec
        if (state.timeInStep < config.earFrameDurationSec) return
        state.timeInStep = 0.0
        state.stepIndex++
        if (state.stepIndex >= config.earPassPairIndices.size) {
            finishMicroAnim()
            return
        }
        applyEarPairIndex(config.earPassPairIndices[state.stepIndex])
    }

    private fun tickNeckSwallow(state: ActiveMicroAnim.NeckSwallow, dtSec: Double) {
        state.timeInStep += dtSec
        if (state.timeInStep < config.neckFrameDurationSec) return
        state.timeInStep = 0.0
        val n = config.neckAdvanceCountPerSwallow
        if (n <= 0) {
            finishMicroAnim()
            return
        }
        state.frameIndex = (state.frameIndex + 1) % n
        applyNeckFrameIndex(state.frameIndex)
        state.advancesRemaining--
        if (state.advancesRemaining <= 0) {
            finishMicroAnim()
        }
    }

    private fun pickWeightedKind(forbidden: MicroAnimPick?): MicroAnimPick {
        var b = config.weightBlink.coerceAtLeast(0)
        var e = config.weightEar.coerceAtLeast(0)
        var n = config.weightNeck.coerceAtLeast(0)
        when (forbidden) {
            MicroAnimPick.Blink -> b = 0
            MicroAnimPick.Ear -> e = 0
            MicroAnimPick.Neck -> n = 0
            null -> Unit
        }
        val total = b + e + n
        if (total > 0) {
            val r = rng.nextDouble() * total
            return when {
                r < b -> MicroAnimPick.Blink
                r < b + e -> MicroAnimPick.Ear
                else -> MicroAnimPick.Neck
            }
        }
        val allowed = MicroAnimPick.entries.filter { it != forbidden }
        if (allowed.isEmpty()) return MicroAnimPick.Blink
        return allowed[rng.nextInt(allowed.size)]
    }

    private fun startRandomMicroAnim() {
        if (blinkOnlyMicroAnims && !neckSwallowLoopOnly) {
            active = ActiveMicroAnim.Blinking(0, 0.0)
            showHeadFrame(config.blinkHeadFrameIndices[0])
            return
        }
        val forbidden =
            if (consecutiveSameKindCompleted >= 2) lastCompletedKind else null
        when (pickWeightedKind(forbidden)) {
            MicroAnimPick.Blink -> {
                active = ActiveMicroAnim.Blinking(0, 0.0)
                showHeadFrame(config.blinkHeadFrameIndices[0])
            }
            MicroAnimPick.Ear -> {
                active = ActiveMicroAnim.EarPass(0, 0.0)
                applyEarPairIndex(config.earPassPairIndices[0])
            }
            MicroAnimPick.Neck -> {
                startNeckSwallowFromRest()
            }
        }
    }

    private fun startNeckSwallowFromRest() {
        active = ActiveMicroAnim.NeckSwallow(0, 0.0, config.neckAdvanceCountPerSwallow)
        applyNeckFrameIndex(0)
    }

    private fun finishMicroAnim() {
        val completedKind = active?.toPick()
        if (!neckSwallowLoopOnly) {
            if (completedKind != null) {
                if (completedKind == lastCompletedKind) {
                    consecutiveSameKindCompleted++
                } else {
                    lastCompletedKind = completedKind
                    consecutiveSameKindCompleted = 1
                }
            }
        }
        active = null
        if (neckSwallowLoopOnly && completedKind == MicroAnimPick.Neck) {
            startNeckSwallowFromRest()
            return
        }
        idleSecondsRemaining = rng.nextDouble(config.idleGapMinSec, config.idleGapMaxSec)
        applyFullRest()
    }

    private fun applyFullRest() {
        showHeadFrame(0)
        applyEarPairIndex(0)
        applyNeckFrameIndex(0)
    }
}
