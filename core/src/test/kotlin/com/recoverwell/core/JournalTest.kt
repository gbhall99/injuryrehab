package com.recoverwell.core

import com.recoverwell.core.export.AppState
import com.recoverwell.core.export.BackupCodec
import com.recoverwell.core.logic.JournalTrends
import com.recoverwell.core.model.JournalEntry
import com.recoverwell.core.model.JournalMood
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class JournalTest {

    private val today = LocalDate.of(2026, 6, 14)

    private fun entry(date: LocalDate, mood: JournalMood) =
        JournalEntry(date.toString(), date, "talked about my day", "reflection", listOf("i1"), listOf("t1"), mood)

    @Test
    fun emptyJournalHasNoTrend() {
        val t = JournalTrends.compute(emptyList(), today)
        assertEquals(0, t.total)
        assertEquals(0, t.streakDays)
        assertFalse(t.loggedToday)
        assertEquals(JournalTrends.Direction.UNKNOWN, t.direction)
    }

    @Test
    fun streakCountsConsecutiveDaysIncludingToday() {
        val entries = listOf(
            entry(today, JournalMood.STEADY),
            entry(today.minusDays(1), JournalMood.STEADY),
            entry(today.minusDays(2), JournalMood.STEADY),
            entry(today.minusDays(4), JournalMood.STEADY)  // gap breaks the streak
        )
        val t = JournalTrends.compute(entries, today)
        assertTrue(t.loggedToday)
        assertEquals(3, t.streakDays)
        assertEquals(4, t.total)
    }

    @Test
    fun streakSurvivesWhenTodayNotYetLogged() {
        // logged yesterday and the day before, but not today -> streak still counts back from yesterday
        val entries = listOf(entry(today.minusDays(1), JournalMood.STEADY), entry(today.minusDays(2), JournalMood.STEADY))
        val t = JournalTrends.compute(entries, today)
        assertFalse(t.loggedToday)
        assertEquals(2, t.streakDays)
    }

    @Test
    fun moodDirectionComparesRecentToPriorWeek() {
        val entries = ArrayList<JournalEntry>()
        // prior week: low moods
        for (d in 7..12) entries.add(entry(today.minusDays(d.toLong()), JournalMood.LOW))
        // recent week: great moods
        for (d in 0..5) entries.add(entry(today.minusDays(d.toLong()), JournalMood.GREAT))
        val t = JournalTrends.compute(entries, today)
        assertEquals(JournalTrends.Direction.UP, t.direction)
        assertNotNull(t.recentAvgMood)
        assertNotNull(t.priorAvgMood)
        assertTrue(t.recentAvgMood!! > t.priorAvgMood!!)
    }

    @Test
    fun journalSurvivesBackupRoundTrip() {
        val profile = Fixtures.profile()
        val state = AppState(
            profile = profile,
            medications = emptyList(),
            tasks = emptyList(),
            exerciseOverrides = emptyMap(),
            dailyLogs = emptyList(),
            events = emptyList(),
            journalEntries = listOf(entry(today, JournalMood.POSITIVE))
        )
        val decoded = BackupCodec.decode(BackupCodec.encode(state))
        val e = decoded.journalEntries.single()
        assertEquals(today, e.date)
        assertEquals(JournalMood.POSITIVE, e.mood)
        assertEquals(listOf("i1"), e.insights)
        assertEquals("reflection", e.reflection)
    }
}
