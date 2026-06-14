package com.recoverwell.core

import com.recoverwell.core.logic.ReturnToSport
import com.recoverwell.core.model.SelfTestResult
import com.recoverwell.core.protocol.Defaults
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

/** The criteria-based return-to-sport ladder: phase gating, thresholds, sign-off. */
class RtsTest {

    private val injury = LocalDate.of(2026, 1, 1)
    private val today = injury.plusWeeks(30) // past phase-5 start week (24)

    private fun profileAtPhase(n: Int) = Fixtures.profile().copy(
        injuryDate = injury, physioConfirmedPhase = n
    )

    private fun result(testId: String, injured: Double, other: Double? = null, painFree: Boolean = true) =
        SelfTestResult(UUID.randomUUID().toString(), testId, today, injured, other, painFree, "")

    private val strengthPasses = listOf(
        result("heel_rise_sym", 18.0, 20.0),  // 90% symmetry, pain-free
        result("balance_eo", 35.0),           // >= 30s
        result("calf_girth_sym", 38.0, 40.0), // 95%
        result("walk_tol", 35.0)              // >= 30 min, pain-free
    )

    @Test
    fun programIsNotAvailableInEarlyPhases() {
        val prog = ReturnToSport.progress(profileAtPhase(1), emptyList(), emptySet(), today)
        assertFalse(prog.available)
        assertEquals(0, prog.readinessPct)
        assertNull(prog.currentRung)
        assertEquals(4, prog.startPhase)
    }

    @Test
    fun strengthStageIsCurrentInPhase4() {
        val prog = ReturnToSport.progress(profileAtPhase(4), emptyList(), emptySet(), today)
        assertTrue(prog.available)
        assertEquals("rts_strength", prog.currentRung?.id)
    }

    @Test
    fun passingEveryTestClearsTheStrengthStage() {
        val prog = ReturnToSport.progress(profileAtPhase(4), strengthPasses, emptySet(), today)
        val strength = prog.rungs.first { it.rung.id == "rts_strength" }
        assertEquals(ReturnToSport.RungState.CLEARED, strength.state)
        assertEquals(20, prog.readinessPct) // 1 of 5 stages
        // the jogging stage is still locked because it lives in phase 5
        val jog = prog.rungs.first { it.rung.id == "rts_jog" }
        assertEquals(ReturnToSport.RungState.LOCKED, jog.state)
        assertEquals("rts_jog", prog.nextLockedByPhase?.id)
    }

    @Test
    fun impactStageNeedsPhysioSignoffOnTopOfTests() {
        // phase 5, strength + jog criteria all pass via the shared tests
        val results = strengthPasses
        val noSign = ReturnToSport.progress(profileAtPhase(5), results, emptySet(), today)
        val jog = noSign.rungs.first { it.rung.id == "rts_jog" }
        assertEquals(ReturnToSport.RungState.CURRENT, jog.state)
        assertTrue(jog.testsMet)
        assertFalse(jog.cleared)

        val signed = ReturnToSport.progress(profileAtPhase(5), results, setOf("rts_jog"), today)
        assertEquals(ReturnToSport.RungState.CLEARED, signed.rungs.first { it.rung.id == "rts_jog" }.state)
    }

    @Test
    fun sportChoiceReshapesTheLadder() {
        // padel: full impact foundation + court tail
        val padel = ReturnToSport.progress(profileAtPhase(5).copy(sportId = "padel"), emptyList(), emptySet(), today)
        assertEquals("Return to padel", padel.returnPhrase)
        assertEquals(5, padel.rungs.size)
        assertTrue(padel.rungs.any { it.rung.id == "rts_padel" })

        // cycling: low impact - the jogging/hopping stages are dropped entirely
        val cycling = ReturnToSport.progress(profileAtPhase(5).copy(sportId = "cycling"), emptyList(), emptySet(), today)
        assertEquals("Return to cycling", cycling.returnPhrase)
        assertEquals(2, cycling.rungs.size)
        assertTrue(cycling.rungs.any { it.rung.id == "rts_cycle_return" })
        assertFalse(cycling.rungs.any { it.rung.id == "rts_jog" })

        // running: foundation + distance-building tail
        val running = ReturnToSport.progress(profileAtPhase(5).copy(sportId = "running"), emptyList(), emptySet(), today)
        assertEquals("Return to running", running.returnPhrase)
        assertTrue(running.rungs.any { it.rung.id == "rts_run_return" })

        // stages are renumbered sequentially regardless of sport
        assertEquals((1..running.rungs.size).toList(), running.rungs.map { it.rung.order })
    }

    @Test
    fun painDuringTestFailsItEvenWithGoodNumbers() {
        val withPain = listOf(result("heel_rise_sym", 20.0, 20.0, painFree = false)) // 100% but painful
        val prog = ReturnToSport.progress(profileAtPhase(4), withPain, emptySet(), today)
        val test = prog.rungs.first { it.rung.id == "rts_strength" }.tests.first { it.test.id == "heel_rise_sym" }
        assertEquals(ReturnToSport.TestState.FAIL, test.state)
    }

    @Test
    fun selfTestResultsSurviveBackupRoundTrip() {
        val state = com.recoverwell.core.export.AppState(
            Fixtures.profile(), Fixtures.medications(), Fixtures.tasks(), emptyMap(), emptyList(), emptyList(),
            selfTestResults = strengthPasses, rtsSignoffs = listOf("rts_jog")
        )
        val decoded = com.recoverwell.core.export.BackupCodec.decode(
            com.recoverwell.core.export.BackupCodec.encode(state))
        assertEquals(4, decoded.selfTestResults.size)
        assertEquals(listOf("rts_jog"), decoded.rtsSignoffs)
        assertEquals(90, decoded.selfTestResults.first { it.testId == "heel_rise_sym" }.symmetryPct)
    }
}
