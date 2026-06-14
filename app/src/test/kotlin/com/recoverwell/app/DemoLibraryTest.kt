package com.recoverwell.app

import com.recoverwell.draw.DemoLibrary
import com.recoverwell.core.protocol.ProtocolRegistry
import org.junit.Assert.*
import org.junit.Test

/**
 * Every exercise in the protocol must have a real animated demonstration -
 * no silent fallbacks. Plain JUnit: DemoLibrary is data, no Android runtime
 * needed (the framework classes on the classpath satisfy linkage).
 */
class DemoLibraryTest {

    @Test
    fun everyExerciseHasItsOwnDemo() {
        val demoIds = DemoLibrary.demos.keys
        for (protocol in ProtocolRegistry.all) for (phase in protocol.phases) {
            for (ex in phase.exercises) {
                assertTrue(
                    "Exercise ${ex.id} references missing demo '${ex.demoId}'",
                    ex.demoId in demoIds
                )
            }
        }
    }

    @Test
    fun demosAreWellFormed() {
        for ((id, demo) in DemoLibrary.demos) {
            assertTrue("$id needs at least 2 keyframes", demo.frames.size >= 2)
            assertTrue("$id frames need positive durations", demo.frames.all { it.second > 0 })
            assertTrue("$id needs a caption", demo.caption.isNotBlank())
        }
    }

    @Test
    fun earlyPhaseAnkleDemosNeverShowDorsiflexionPastNeutral() {
        // Clinical guard: the phase 3 ankle pump demo must not animate the
        // ankle above neutral (negative = dorsiflexion in the rig).
        val pump = DemoLibrary.demos.getValue("ankle_pump")
        assertTrue(pump.frames.all { it.first.ankleA >= 0f })
    }
}
