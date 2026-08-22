package com.recoverwell.core

import com.recoverwell.core.logic.Capability
import com.recoverwell.core.logic.MilestoneTimeline
import com.recoverwell.core.model.DailyLog
import com.recoverwell.core.model.Swelling
import com.recoverwell.core.protocol.Defaults
import com.recoverwell.core.protocol.ProtocolRegistry
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class CapabilityAndMilestoneTest {

    private val injury = LocalDate.of(2026, 6, 2)
    private val profile = Fixtures.profile()

    @Test
    fun snapshotReflectsPhase1() {
        val snap = Capability.snapshot(profile, injury.plusDays(5))
        assertEquals(1, snap.phaseNumber)
        assertEquals(30, snap.wedges)
        assertTrue(snap.bootStatus.contains("Boot on at all times"))
        assertTrue(snap.notAllowed.any { it.contains("without the boot") })
    }

    @Test
    fun severeSwellingRaisesUrgentWarning() {
        val log = DailyLog.empty(injury.plusDays(5)).copy(swelling = Swelling.SEVERE)
        val warnings = Capability.warnings(profile, listOf(log), injury.plusDays(5))
        assertTrue(warnings.any { it.severity == Capability.Severity.URGENT && it.title.contains("swelling") })
    }

    @Test
    fun wedgesAheadOfPlanWarns() {
        // Day 20: plan expects 4 wedges (first removal day 14); user says boot has 2.
        val rushed = profile.copy(currentWedges = 2)
        val warnings = Capability.warnings(rushed, emptyList(), injury.plusDays(20))
        assertTrue(warnings.any { it.severity == Capability.Severity.WARNING && it.title.contains("ahead of plan") })
    }

    @Test
    fun devicePlanWarningsStopOncePhaseDropsTheBoot() {
        // Phase 4 no longer uses the boot: a stale device value (never stepped
        // down to 0 in the app) must not keep warning "behind plan" forever.
        val out = profile.copy(physioConfirmedPhase = 4, currentWedges = 15)
        val warnings = Capability.warnings(out, emptyList(), injury.plusWeeks(13))
        assertFalse(warnings.any { it.title.contains("ahead of plan") || it.title.contains("behind plan") })
    }

    @Test
    fun bootNotWornWarnsInProtectionPhase() {
        val log = DailyLog.empty(injury.plusDays(5)).copy(bootWornAsPlanned = false)
        val warnings = Capability.warnings(profile, listOf(log), injury.plusDays(5))
        assertTrue(warnings.any { it.title.contains("not worn as planned") })
    }

    @Test
    fun bootWeanedFlipsTwinStatusAndSilencesBootWarnings() {
        val weaned = profile.copy(physioConfirmedPhase = 3, bootWeanedDate = injury.plusWeeks(10))
        val today = injury.plusWeeks(11)
        assertTrue(Capability.snapshot(weaned, today).bootStatus.contains("Out of the"))
        // a stale dial value and a "boot not worn" log must not warn once weaned
        val log = DailyLog.empty(today).copy(bootWornAsPlanned = false)
        val warnings = Capability.warnings(weaned, listOf(log), today)
        assertFalse(warnings.any {
            it.title.contains("not worn") || it.title.contains("ahead of plan") || it.title.contains("behind plan")
        })
    }

    @Test
    fun pendingPhaseConfirmationSurfaces() {
        val warnings = Capability.warnings(profile, emptyList(), injury.plusWeeks(3))
        assertTrue(warnings.any { it.title.contains("physio confirmation") })
    }

    @Test
    fun movementChecksGateByPhase() {
        val checks = Capability.movementChecks(profile, injury.plusDays(5))
        assertFalse(checks.first { it.movement.contains("padel") }.allowed)
        assertFalse(checks.first { it.movement.contains("Walk without the boot") }.allowed)

        val phase5 = profile.copy(physioConfirmedPhase = 5)
        val late = Capability.movementChecks(phase5, injury.plusWeeks(30))
        assertTrue(late.first { it.movement.contains("padel") }.allowed)
    }

    @Test
    fun milestoneTimelineAnchorsToInjuryDate() {
        val entries = MilestoneTimeline.build(profile, injury.plusWeeks(3))
        assertEquals(ProtocolRegistry.default.milestones.size, entries.size)
        val bootNeutral = entries.first { it.milestone.week == 8 }
        assertEquals(injury.plusWeeks(8), bootNeutral.expectedDate)
        assertEquals(MilestoneTimeline.Status.UPCOMING, bootNeutral.status)
        val first = entries.first { it.milestone.week == 0 }
        assertEquals(MilestoneTimeline.Status.REACHED, first.status)
        val current = entries.first { it.milestone.week == 3 }
        assertEquals(MilestoneTimeline.Status.DUE_NOW, current.status)
    }
}
