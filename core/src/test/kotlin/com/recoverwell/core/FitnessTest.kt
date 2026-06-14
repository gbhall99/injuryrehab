package com.recoverwell.core

import com.recoverwell.core.logic.Fitness
import com.recoverwell.core.logic.WeeklyDigest
import com.recoverwell.core.model.*
import com.recoverwell.core.protocol.Defaults
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

/** Stay-fit conditioning: phase gating, weekly count, and digest separation. */
class FitnessTest {

    private val injury = LocalDate.of(2026, 1, 1)
    private val meds = Defaults.medications()
    private val tasks = Defaults.tasks()

    private fun fitnessEvent(d: LocalDate) =
        EventLog(UUID.randomUUID().toString(), d, EventType.EXERCISE, Fitness.SESSION_REF, "session", EventStatus.DONE, 600)

    @Test
    fun earlyPhaseExcludesOutOfBootActivities() {
        val p = Defaults.profile().copy(injuryDate = injury, physioConfirmedPhase = 1)
        val avail = Fitness.available(p, injury.plusDays(3))
        assertTrue(avail.isNotEmpty())
        assertTrue("no phase-3+ activities in phase 1", avail.all { it.minPhase <= 1 })
        assertFalse(avail.any { it.id == "f_bike" })
    }

    @Test
    fun laterPhaseUnlocksMoreActivities() {
        val p = Defaults.profile().copy(injuryDate = injury, physioConfirmedPhase = 4)
        val avail = Fitness.available(p, injury.plusWeeks(30))
        assertTrue(avail.any { it.id == "f_bike" })
        assertTrue(avail.any { it.id == "f_gym" })
    }

    @Test
    fun weeklyCountCountsRecentSessionsOnly() {
        val today = injury.plusWeeks(20)
        val events = listOf(fitnessEvent(today), fitnessEvent(today.minusDays(2)), fitnessEvent(today.minusDays(10)))
        assertEquals(2, Fitness.sessionsThisWeek(events, today))
    }

    @Test
    fun conditioningSessionsDoNotInflateRehabExerciseCount() {
        val today = injury.plusWeeks(20)
        val profile = Defaults.profile().copy(injuryDate = injury)
        val events = listOf(
            EventLog("a", today, EventType.EXERCISE, "p4_real_exercise", "session1", EventStatus.DONE, 600),
            fitnessEvent(today),
            fitnessEvent(today.minusDays(1))
        )
        val digest = WeeklyDigest.generate(profile, emptyList(), events, meds, tasks, today)
        assertEquals(1, digest.exercisesDone) // the 2 fitness sessions are excluded
    }
}
