package com.recoverwell.core

import com.recoverwell.core.logic.PhaseEngine
import com.recoverwell.core.protocol.Defaults
import com.recoverwell.core.protocol.ProtocolRegistry
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class PhaseEngineTest {

    private val injury = LocalDate.of(2026, 6, 2)
    private val profile = Fixtures.profile()

    @Test
    fun phase1AtInjury() {
        assertEquals(1, PhaseEngine.currentPhase(profile, injury).number)
        assertEquals(1, PhaseEngine.currentPhase(profile, injury.plusDays(13)).number)
    }

    @Test
    fun dateEligibilityAloneDoesNotAdvance() {
        // Week 3: phase 2 eligible by date, but physio confirmation still at 1.
        val today = injury.plusWeeks(3)
        assertEquals(2, PhaseEngine.dateEligiblePhase(profile, today))
        assertEquals(1, PhaseEngine.currentPhase(profile, today).number)
        val gate = PhaseEngine.nextPhaseGate(profile, today)
        assertNotNull(gate.nextPhase)
        assertEquals(2, gate.nextPhase!!.number)
        assertTrue(gate.dateEligible)
        assertFalse(gate.physioConfirmed)
        assertTrue(gate.readyToConfirm)
    }

    @Test
    fun physioConfirmationAdvancesPhase() {
        val confirmed = profile.copy(physioConfirmedPhase = 2)
        val today = injury.plusWeeks(3)
        assertEquals(2, PhaseEngine.currentPhase(confirmed, today).number)
    }

    @Test
    fun physioConfirmationAloneDoesNotAdvanceBeforeDate() {
        // Confirmed up to phase 3, but at week 3 only phase 2 is date-eligible.
        val confirmed = profile.copy(physioConfirmedPhase = 3)
        val today = injury.plusWeeks(3)
        assertEquals(2, PhaseEngine.currentPhase(confirmed, today).number)
    }

    @Test
    fun startDateOverrideRespected() {
        val overridden = profile.copy(
            physioConfirmedPhase = 2,
            phaseStartOverrides = mapOf(2 to injury.plusWeeks(4))
        )
        assertEquals(1, PhaseEngine.currentPhase(overridden, injury.plusWeeks(3)).number)
        assertEquals(2, PhaseEngine.currentPhase(overridden, injury.plusWeeks(4)).number)
    }

    @Test
    fun gateBeforeEligibilityCountsDays() {
        val gate = PhaseEngine.nextPhaseGate(profile, injury.plusDays(7))
        assertEquals(2, gate.nextPhase!!.number)
        assertFalse(gate.dateEligible)
        assertEquals(7, gate.daysUntilEligible)
        assertFalse(gate.readyToConfirm)
    }

    @Test
    fun finalPhaseHasNoNext() {
        val late = profile.copy(physioConfirmedPhase = 5)
        val gate = PhaseEngine.nextPhaseGate(late, injury.plusWeeks(60))
        assertNull(gate.nextPhase)
    }

    @Test
    fun phaseWindowsAreContiguousFromZero() {
        val phases = ProtocolRegistry.default.phases.sortedBy { it.number }
        assertEquals(0, phases.first().startWeek)
        for (i in 0 until phases.size - 1) {
            assertEquals(
                "phase ${phases[i].number} end must equal phase ${phases[i + 1].number} start",
                phases[i].endWeek, phases[i + 1].startWeek
            )
        }
        assertNull(phases.last().endWeek)
    }
}
