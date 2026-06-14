package com.recoverwell.core

import com.recoverwell.core.logic.Insights
import com.recoverwell.core.logic.Wellbeing
import com.recoverwell.core.model.DailyLog
import com.recoverwell.core.protocol.Defaults
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class WellbeingTest {

    private val injury = LocalDate.of(2026, 1, 1)
    private val meds = Defaults.medications()
    private val tasks = Defaults.tasks()

    @Test
    fun currentPhaseHasMindsetContent() {
        val profile = Defaults.profile().copy(injuryDate = injury)
        val mind = Wellbeing.currentMindset(profile, injury.plusDays(3))
        assertNotNull(mind)
        assertTrue(mind!!.normalToFeel.isNotEmpty())
        assertTrue(mind.encouragement.isNotBlank())
    }

    @Test
    fun recentlyReachedMilestoneIsTheMostRecentWithinAWeek() {
        val profile = Defaults.profile().copy(injuryDate = injury)
        val m = Wellbeing.recentlyReachedMilestone(profile, injury.plusDays(15))
        assertNotNull(m)
        assertEquals(2, m!!.week) // "Settled in the boot" at week 2
    }

    @Test
    fun expectationBandMatchesCurrentWeekAndResolvesSport() {
        val profile = Defaults.profile().copy(injuryDate = injury) // padel by default
        val exp = Wellbeing.expectationFor(profile, injury.plusWeeks(14)) // strengthening band 12-24
        assertNotNull(exp)
        assertTrue(14 >= exp!!.weekFrom && 14 < exp.weekTo)
        assertTrue("sport token resolved to padel", exp.reassure.contains("padel"))

        val running = Wellbeing.expectationFor(profile.copy(sportId = "running"), injury.plusWeeks(30))
        assertNotNull(running)
        assertTrue(running!!.title.contains("running") || running.reassure.contains("running"))
        assertFalse(running.title.contains("padel"))
    }

    @Test
    fun moodDipSurfacesAsupportiveInsight() {
        val today = injury.plusDays(40)
        val logs = (0..13).map { i ->
            DailyLog.empty(today.minusDays(i.toLong())).copy(mood = if (i <= 6) 2 else 5)
        }
        val ins = Insights.generate(Defaults.profile(), logs, emptyList(), meds, tasks, today)
        assertTrue(ins.any { it.tone == Insights.Tone.CAUTION && it.title.contains("mood", ignoreCase = true) })
    }
}
