package com.recoverwell.core

import com.recoverwell.core.export.AppState
import com.recoverwell.core.export.BackupCodec
import com.recoverwell.core.logic.MilestoneTimeline
import com.recoverwell.core.logic.PhysioPrep
import com.recoverwell.core.logic.ScheduleEngine
import com.recoverwell.core.logic.WeeklyDigest
import com.recoverwell.core.logic.Wellbeing
import com.recoverwell.core.model.EventLog
import com.recoverwell.core.model.EventStatus
import com.recoverwell.core.model.EventType
import com.recoverwell.core.model.Medication
import com.recoverwell.core.protocol.AchillesConservative
import com.recoverwell.core.protocol.ProtocolRegistry
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Guards for the UX + clinical audit fixes: dose-aware sessions, honest
 * milestones, medication copy, and the specialist-physio content additions.
 */
class ClinicalAuditTest {

    private val injury = LocalDate.of(2026, 6, 2)
    private val achilles = AchillesConservative.protocol
    private fun profile(phase: Int) = Fixtures.profile().copy(injuryDate = injury, physioConfirmedPhase = phase)

    // ---- dosing ------------------------------------------------------------

    @Test
    fun impactWorkIsScheduledOnAlternateDaysOnly() {
        val p = profile(5)
        val day = injury.plusWeeks(30)
        val impact = setOf("p5_jog", "p5_hop", "p5_agility", "p5_padel")
        val ids = { d: LocalDate -> ScheduleEngine.sessionPlan(p, emptyMap(), d, 3).flatMap { it.second }.map { it.id }.toSet() }
        val today = ids(day)
        val tomorrow = ids(day.plusDays(1))
        // impact work sits on exactly one of any two consecutive days
        assertTrue(impact.all { (it in today) != (it in tomorrow) })
        // strength work is daily
        assertTrue("p5_single_raise" in today && "p5_single_raise" in tomorrow)
    }

    @Test
    fun onceADayWorkIsNeverRepeatedAcrossSessions() {
        val plan = ScheduleEngine.sessionPlan(profile(5), emptyMap(), injury.plusWeeks(30), 3)
        // every phase-5 exercise is once a day: one session only, however many the user picked
        assertEquals(1, plan.size)
        val p4 = ScheduleEngine.sessionPlan(profile(4), emptyMap(), injury.plusWeeks(14), 3)
        assertTrue(p4.drop(1).flatMap { it.second }.none { it.id == "p4_swim" || it.id == "p4_double_raise" })
    }

    @Test
    fun prescriptionsReadNaturallyForTimedWork() {
        val all = achilles.phases.flatMap { it.exercises }.associateBy { it.id }
        assertEquals("10 min", ScheduleEngine.exercisePrescription(all.getValue("p3_bike")))
        assertEquals("5 × 1 min", ScheduleEngine.exercisePrescription(all.getValue("p3_gait")))
        assertEquals("8 × 3 min · alternate days", ScheduleEngine.exercisePrescription(all.getValue("p5_jog")))
        assertEquals("3 × 30s hold", ScheduleEngine.exercisePrescription(all.getValue("p4_balance")))
        assertEquals("3 sets × 12 · hold 1s", ScheduleEngine.exercisePrescription(all.getValue("p3_seated_raise")))
        for (ex in all.values) assertFalse(ex.id, ScheduleEngine.exercisePrescription(ex).contains("hold 10 min"))
    }

    @Test
    fun adherenceRespectsTheUsersChosenSessionCount() {
        val p = profile(1)
        val today = injury.plusDays(10)
        // one session a day, fully done every day this week
        val events = ArrayList<EventLog>()
        for (i in 0..6) {
            val d = today.minusDays(i.toLong())
            for (ex in ScheduleEngine.sessionPlan(p, emptyMap(), d, 1).single().second)
                events.add(EventLog("$i${ex.id}", d, EventType.EXERCISE, ex.id, "session1", EventStatus.DONE, 600))
        }
        assertEquals(100, ScheduleEngine.exerciseAdherence(p, emptyMap(), events, today, sessionsPerDay = 1))
        assertEquals(7, ScheduleEngine.exerciseStreak(p, emptyMap(), events, today, sessionsPerDay = 1))
    }

    // ---- medication copy -----------------------------------------------------

    @Test
    fun clotCopyOnlyForBloodThinners() {
        val pain = Medication("m2", "Paracetamol", "1 g", listOf(LocalTime.of(9, 0)), "", true)
        val items = ScheduleEngine.dailyChecklist(profile(1), Fixtures.medications() + pain, emptyList(),
            emptyMap(), emptyList(), injury.plusDays(2))
        assertTrue(items.first { it.refId == "m2" }.subtitle.contains("clot").not())
        assertTrue(items.first { it.refId == "med_anticoagulant" }.subtitle.contains("clot"))
        assertTrue(Medication("x", "Apixaban", "2.5 mg", emptyList(), "", true).isClotPrevention())
        assertFalse(pain.isClotPrevention())
    }

    // ---- honest milestones ----------------------------------------------------

    @Test
    fun milestonesNeedTheRealPhaseNotJustTheCalendar() {
        val day = injury.plusWeeks(14)
        val stillInPhase3 = MilestoneTimeline.build(profile(3), day).first { it.milestone.title == "Out of the boot" }
        assertEquals(MilestoneTimeline.Status.DUE_NOW, stillInPhase3.status)
        val inPhase4 = MilestoneTimeline.build(profile(4), day).first { it.milestone.title == "Out of the boot" }
        assertEquals(MilestoneTimeline.Status.REACHED, inPhase4.status)
        // no "Milestone reached: Out of the boot" celebration while the physio keeps you in it
        assertNotEquals("Out of the boot", Wellbeing.recentlyReachedMilestone(profile(3), injury.plusWeeks(12))?.title)
        assertEquals("Out of the boot", Wellbeing.recentlyReachedMilestone(profile(4), injury.plusWeeks(12))?.title)
    }

    // ---- digest / pack / backup ---------------------------------------------

    @Test
    fun noMedicationMeansNoAdherenceFigure() {
        val d = WeeklyDigest.generate(profile(1), emptyList(), emptyList(), emptyList(), emptyList(), injury.plusDays(9))
        assertNull(d.adherencePct)
        val pack = PhysioPrep.build(profile(1), emptyList(), emptyList(), emptyList(), emptyList(),
            emptyList(), emptySet(), injury.plusDays(9))
        assertTrue(pack.summaryLines.none { it.contains("adherence") })
    }

    @Test
    fun physioPackCarriesStageQuestionsAndBootState() {
        val pack = PhysioPrep.build(profile(1), emptyList(), emptyList(), emptyList(), emptyList(),
            emptyList(), emptySet(), injury.plusDays(9))
        assertTrue(pack.stageQuestions.isNotEmpty())
        assertTrue(pack.summaryLines.any { it.contains("VACOped") })
        assertTrue(pack.summaryLines.any { it.startsWith("Weight-bearing") })
    }

    @Test
    fun clinicPhoneSurvivesBackup() {
        val state = AppState(Fixtures.profile().copy(clinicPhone = "01234 567890"), Fixtures.medications(),
            Fixtures.tasks(), emptyMap(), emptyList(), emptyList())
        assertEquals("01234 567890", BackupCodec.decode(BackupCodec.encode(state)).profile.clinicPhone)
    }

    // ---- specialist content guards --------------------------------------------

    @Test
    fun phase4BuildsSingleLegStrengthBeforePhase5AsksForIt() {
        val p4 = achilles.phase(4).exercises
        assertTrue(p4.any { e -> e.cues.any { it.contains("single-leg", ignoreCase = true) } })
        assertTrue(p4.any { it.id == "p4_soleus_raise" })
        assertTrue(achilles.phase(5).entryCriteria.any { it.contains("single-leg heel raises") })
    }

    @Test
    fun safetyAdviceASpecialistExpects() {
        val bleeding = achilles.redFlags.first { it.id == "bleeding" }
        assertTrue(bleeding.symptoms.any { it.contains("head", ignoreCase = true) })
        val rerupture = achilles.redFlags.first { it.id == "rerupture" }
        assertFalse("re-rupture advice must be device-neutral", rerupture.action.contains("wedge"))
        assertTrue(rerupture.action.contains("A&E"))
        val painkillers = achilles.faqs.first { it.id == "painkillers" }.answerFor(1).lowercase()
        assertTrue(painkillers.contains("ibuprofen") && painkillers.contains("paracetamol"))
        assertTrue(achilles.phase(1).allowed.any { it.contains("Stairs") })
        assertTrue(achilles.exercisePainRule.contains("next morning"))
        assertTrue(achilles.selfTests.any { it.id == "heel_rise_height" })
    }

    @Test
    fun conservativeLanguageOnly() {
        val text = (achilles.phases.flatMap { it.allowed + it.notAllowed + it.precautions + it.entryCriteria +
            it.goals + it.physioQuestions + it.exercises.flatMap { e -> e.cues + e.precaution + e.whyItMatters } } +
            achilles.faqs.flatMap { it.answers.values } + achilles.fitness.map { it.detail } +
            achilles.redFlags.map { it.action } +
            com.recoverwell.core.protocol.SportRegistry.all.flatMap { s -> s.tailRungs.flatMap { it.guidance } })
            .joinToString("\n").lowercase()
        for (term in listOf("repaired tendon", "wound", "operation", "surgeon")) {
            assertFalse("non-conservative term: $term", text.contains(term))
        }
    }

    @Test
    fun everyPhaseHasQuestionsForThePhysio() {
        for (p in ProtocolRegistry.all.flatMap { it.phases }) assertTrue(p.physioQuestions.size >= 3)
    }
}
