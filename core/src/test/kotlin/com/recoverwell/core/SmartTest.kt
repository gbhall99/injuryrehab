package com.recoverwell.core

import com.recoverwell.core.logic.*
import com.recoverwell.core.model.*
import com.recoverwell.core.protocol.Defaults
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/** Tests the on-device "smart" layer: insights, adaptive reminders, pace, ask. */
class SmartTest {

    private val injury = LocalDate.of(2026, 6, 2)
    private val today = injury.plusDays(40)
    private val profile = Defaults.profile()
    private val meds = Defaults.medications()
    private val tasks = Defaults.tasks()

    private fun log(d: LocalDate, pain: Int? = null, swelling: Swelling? = null) =
        DailyLog.empty(d).copy(pain = pain, swelling = swelling)

    @Test
    fun painDownTrendIsPositiveInsight() {
        val logs = (0..13).map { i ->
            val d = today.minusDays(i.toLong())
            // recent week ~3, prior week ~6
            log(d, pain = if (i <= 6) 3 else 6)
        }
        val ins = Insights.generate(profile, logs, emptyList(), meds, tasks, today)
        assertTrue(ins.any { it.tone == Insights.Tone.POSITIVE && it.title.contains("Pain is easing") })
    }

    @Test
    fun painUpTrendIsCaution() {
        val logs = (0..13).map { i ->
            val d = today.minusDays(i.toLong())
            log(d, pain = if (i <= 6) 6 else 3)
        }
        val ins = Insights.generate(profile, logs, emptyList(), meds, tasks, today)
        assertTrue(ins.any { it.tone == Insights.Tone.CAUTION && it.title.contains("creeping up") })
    }

    @Test
    fun elevationCorrelationSurfacesWhenSwellingLowerOnElevationDays() {
        val elevation = tasks.first { it.kind == TaskKind.ELEVATION }
        val logs = ArrayList<DailyLog>()
        val events = ArrayList<EventLog>()
        for (i in 0..19) {
            val d = today.minusDays(i.toLong())
            val elevDay = i % 2 == 0
            logs.add(log(d, swelling = if (elevDay) Swelling.MILD else Swelling.MODERATE))
            if (elevDay) events.add(EventLog("e$i", d, EventType.TASK, elevation.id, "10:00", EventStatus.DONE, 600))
        }
        val ins = Insights.generate(profile, logs, events, meds, tasks, today)
        assertTrue(ins.any { it.title.contains("Elevation seems to help") })
    }

    @Test
    fun adaptiveReminderSuggestsShiftWhenTakenLate() {
        // 20:00 dose actually taken ~21:30 for 6 days
        val events = (0..5).map { i ->
            EventLog("e$i", today.minusDays(i.toLong()), EventType.MEDICATION,
                "med_anticoagulant", "20:00", EventStatus.TAKEN, 21 * 60 + 30)
        }
        val sugg = AdaptiveReminders.timeSuggestions(meds, events, today)
        assertTrue(sugg.any { it.slotKey == "20:00" && it.typicalMinute == 21 * 60 + 30 })
        // applying it moves the time
        val updated = AdaptiveReminders.applySuggestion(meds, sugg.first { it.slotKey == "20:00" })
        assertTrue(updated.first().times.any { it.hour == 21 && it.minute == 30 })
    }

    @Test
    fun missPatternFlagsRoutinelyMissedDose() {
        // morning dose taken only twice in 14 days
        val events = listOf(
            EventLog("a", today, EventType.MEDICATION, "med_anticoagulant", "08:00", EventStatus.TAKEN, 500),
            EventLog("b", today.minusDays(3), EventType.MEDICATION, "med_anticoagulant", "08:00", EventStatus.TAKEN, 500)
        )
        val miss = AdaptiveReminders.missPatterns(meds, events, today)
        assertTrue(miss.any { it.slotKey == "08:00" && it.missedRate >= 0.4 })
    }

    @Test
    fun paceReadsAheadWhenPhaseConfirmedEarly() {
        // phase 2 baseline starts week 2 (injury+14); confirm it a week early (injury+7)
        val p = profile.copy(
            physioConfirmedPhase = 2,
            phaseConfirmedDates = mapOf(2 to injury.plusDays(7))
        )
        val proj = Pace.project(p, today)
        assertFalse(proj.earlyDays)
        assertTrue("ahead by ~1 week", proj.deltaWeeks >= 1)
        assertTrue(proj.projectedMilestones.isNotEmpty())
    }

    @Test
    fun paceEarlyDaysWhenNoConfirmations() {
        val proj = Pace.project(profile, today)
        assertTrue(proj.earlyDays)
    }

    @Test
    fun askAnswersMovementFromProtocol() {
        val drive = Ask.answer("can i drive yet?", profile, today)
        assertTrue(drive.title.lowercase().contains("drive"))
        val padel = Ask.answer("when can I play padel", profile, today)
        assertTrue(padel.body.isNotBlank())
        val flags = Ask.answer("what are the red flags?", profile, today)
        assertEquals(Ask.Action.OPEN_RED_FLAGS, flags.action)
        val next = Ask.answer("what's next?", profile, today)
        assertEquals(Ask.Action.OPEN_PHASE_GUIDE, next.action)
        val fallback = Ask.answer("xyzzy", profile, today)
        assertTrue(fallback.title.contains("help"))
    }

    @Test
    fun phaseConfirmedDatesSurviveBackupRoundTrip() {
        val p = profile.copy(phaseConfirmedDates = mapOf(2 to injury.plusDays(15)))
        val state = com.recoverwell.core.export.AppState(p, meds, tasks, emptyMap(), emptyList(), emptyList())
        val decoded = com.recoverwell.core.export.BackupCodec.decode(
            com.recoverwell.core.export.BackupCodec.encode(state))
        assertEquals(injury.plusDays(15), decoded.profile.phaseConfirmedDates[2])
    }
}
