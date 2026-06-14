package com.recoverwell.core

import com.recoverwell.core.logic.ScheduleEngine
import com.recoverwell.core.model.EventLog
import com.recoverwell.core.model.EventStatus
import com.recoverwell.core.model.EventType
import com.recoverwell.core.protocol.Defaults
import com.recoverwell.core.protocol.ProtocolRegistry
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ScheduleEngineTest {

    private val injury = LocalDate.of(2026, 6, 2)
    private val profile = Fixtures.profile()
    private val meds = Fixtures.medications()
    private val tasks = Fixtures.tasks()

    @Test
    fun checklistHasTwoMedicationSlots() {
        val items = ScheduleEngine.dailyChecklist(profile, meds, tasks, emptyMap(), emptyList(), injury.plusDays(3))
        val medItems = items.filter { it.kind == ScheduleEngine.ItemKind.MEDICATION }
        assertEquals(2, medItems.size)
        assertEquals(listOf("08:00", "20:00"), medItems.map { it.slotKey }.sorted())
    }

    @Test
    fun checklistIncludesPhase1TasksAndExercises() {
        val date = injury.plusDays(3)
        val items = ScheduleEngine.dailyChecklist(profile, meds, tasks, emptyMap(), emptyList(), date)
        // 3 task defs with 3+1+2 = 6 time slots
        assertEquals(6, items.count { it.kind == ScheduleEngine.ItemKind.TASK })
        // phase 1: 5 exercises with 4+3+2+2+2 = 13 sessions
        assertEquals(13, items.count { it.kind == ScheduleEngine.ItemKind.EXERCISE })
    }

    @Test
    fun eventStatusReflectedInChecklist() {
        val date = injury.plusDays(3)
        val events = listOf(
            EventLog("e1", date, EventType.MEDICATION, "med_anticoagulant", "08:00", EventStatus.TAKEN, 8 * 60 + 5)
        )
        val items = ScheduleEngine.dailyChecklist(profile, meds, tasks, emptyMap(), events, date)
        val morning = items.first { it.kind == ScheduleEngine.ItemKind.MEDICATION && it.slotKey == "08:00" }
        val evening = items.first { it.kind == ScheduleEngine.ItemKind.MEDICATION && it.slotKey == "20:00" }
        assertTrue(morning.isDone)
        assertEquals(EventStatus.TAKEN, morning.status)
        assertNull(evening.status)
    }

    @Test
    fun wedgeChangeAppearsOnPlannedDates() {
        // Default plan: heel angle 30 degrees, lowered 5 every week from week 3.
        val firstChange = injury.plusDays(14)
        val items = ScheduleEngine.wedgeChangesOn(profile, firstChange)
        assertEquals(1, items.size)
        assertTrue(items[0].title, items[0].title.contains("25°"))
        assertTrue(ScheduleEngine.wedgeChangesOn(profile, firstChange.plusDays(1)).isEmpty())
        // 30 -> 0 in 5-degree steps takes 6 changes; the 6th leaves 0 degrees.
        val last = ScheduleEngine.wedgeChangesOn(profile, firstChange.plusWeeks(5))
        assertEquals(1, last.size)
        assertTrue(last[0].title, last[0].title.contains("0°"))
    }

    @Test
    fun exerciseOverridesApplied() {
        val overrides = mapOf(
            "p1_slr" to com.recoverwell.core.model.ExerciseOverride("p1_slr", sets = 5, reps = 8, holdSeconds = null, sessionsPerDay = 1, enabled = true),
            "p1_toe_scrunch" to com.recoverwell.core.model.ExerciseOverride("p1_toe_scrunch", null, null, null, null, enabled = false)
        )
        val merged = ScheduleEngine.mergedExercises(ProtocolRegistry.default.phase(1).exercises, overrides)
        assertNull(merged.find { it.id == "p1_toe_scrunch" })
        val slr = merged.first { it.id == "p1_slr" }
        assertEquals(5, slr.sets)
        assertEquals(8, slr.reps)
        assertEquals(2, slr.holdSeconds) // default kept where override is null
    }

    @Test
    fun upcomingRemindersSortedAndInFuture() {
        val now = LocalDateTime.of(injury.plusDays(3), LocalTime.of(9, 30))
        val reminders = ScheduleEngine.upcomingReminders(profile, meds, tasks, now)
        assertTrue(reminders.isNotEmpty())
        assertTrue(reminders.all { it.at.isAfter(now) })
        assertEquals(reminders.sortedBy { it.at }, reminders)
        // The next reminder after 09:30 should be the 10:00 elevation task.
        assertEquals("task_elevation", reminders.first().refId)
        // Today's remaining med dose at 20:00 is present.
        assertTrue(reminders.any { it.refId == "med_anticoagulant" && it.at.toLocalTime() == LocalTime.of(20, 0) })
    }

    @Test
    fun medicationStreakCountsFullDaysOnly() {
        val today = injury.plusDays(10)
        fun taken(d: LocalDate, slot: String) =
            EventLog("e$d$slot", d, EventType.MEDICATION, "med_anticoagulant", slot, EventStatus.TAKEN, 500)

        // three full days, then today only half-complete -> streak 3 (ends yesterday)
        val events = listOf(
            taken(today.minusDays(3), "08:00"), taken(today.minusDays(3), "20:00"),
            taken(today.minusDays(2), "08:00"), taken(today.minusDays(2), "20:00"),
            taken(today.minusDays(1), "08:00"), taken(today.minusDays(1), "20:00"),
            taken(today, "08:00")
        )
        assertEquals(3, ScheduleEngine.medicationStreak(meds, events, today))
        // completing today extends it to 4
        assertEquals(4, ScheduleEngine.medicationStreak(meds, events + taken(today, "20:00"), today))
        // a MISSED day in the middle breaks the chain
        val broken = events.filter { it.date != today.minusDays(2) }
        assertEquals(1, ScheduleEngine.medicationStreak(meds, broken, today))
        // no active meds -> no streak
        assertEquals(0, ScheduleEngine.medicationStreak(emptyList(), events, today))
    }

    @Test
    fun remindersIncludeDatedWedgeChanges() {
        val now = LocalDateTime.of(injury.plusDays(13), LocalTime.of(12, 0))
        val reminders = ScheduleEngine.upcomingReminders(profile, meds, tasks, now)
        assertTrue(reminders.any { it.kind == ScheduleEngine.ItemKind.WEDGE_CHANGE })
    }
}
