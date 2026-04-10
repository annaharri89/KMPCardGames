package ui.render

import korlibs.korge.view.Container
import korlibs.korge.view.addUpdater
import kotlin.time.Duration

/**
 * Single [addUpdater] drives every registered face-card puppet. Register on mount, unregister on dispose
 * so hidden or rebuilt cards stop ticking.
 */
class FaceCardPuppetAnimationHub {
    private val tickables = ArrayList<FoxSpadePuppetTickable>()
    private val pendingUnregister = ArrayList<FoxSpadePuppetTickable>()
    private var updaterAttached = false

    fun register(tickable: FoxSpadePuppetTickable) {
        if (!tickables.contains(tickable)) {
            tickables.add(tickable)
        }
    }

    fun unregister(tickable: FoxSpadePuppetTickable) {
        pendingUnregister.add(tickable)
    }

    fun attachToBoard(boardLayer: Container) {
        if (updaterAttached) return
        updaterAttached = true
        boardLayer.addUpdater { dt: Duration ->
            flushPendingUnregisters()
            val dtSec = dt.inWholeMilliseconds / 1000.0
            var i = 0
            val n = tickables.size
            while (i < n) {
                tickables[i].tick(dtSec)
                i++
            }
        }
    }

    private fun flushPendingUnregisters() {
        if (pendingUnregister.isEmpty()) return
        for (t in pendingUnregister) {
            tickables.remove(t)
        }
        pendingUnregister.clear()
    }
}

interface FoxSpadePuppetTickable {
    fun tick(dtSec: Double)
}
