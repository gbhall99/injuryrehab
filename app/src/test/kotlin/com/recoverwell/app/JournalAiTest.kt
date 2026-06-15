package com.recoverwell.app

import com.recoverwell.app.ai.Groq
import com.recoverwell.app.ai.JournalAi
import com.recoverwell.core.model.DailyLog
import com.recoverwell.core.model.JournalEntry
import com.recoverwell.core.model.JournalMood
import com.recoverwell.core.model.Swelling
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/** Pure prompt/parse plumbing for the AI journal - no network. */
class JournalAiTest {

    private val today = LocalDate.of(2026, 6, 14)

    @Test
    fun parsesWellFormedAnalysis() {
        val raw = """{"reflection":"Sounds like a solid day.",
            "insights":["Pain is easing","Sleeping better"],
            "tips":["Keep elevating in the evening"],
            "mood":"Positive"}"""
        val a = JournalAi.parseAnalysis(raw)
        assertEquals("Sounds like a solid day.", a.reflection)
        assertEquals(2, a.insights.size)
        assertEquals(listOf("Keep elevating in the evening"), a.tips)
        assertEquals(JournalMood.POSITIVE, a.mood)
    }

    @Test
    fun extractsDailyMetricsWhenMentioned() {
        val raw = """{"reflection":"x","insights":[],"tips":[],"mood":"Mixed",
            "metrics":{"pain":4,"swelling":"Mild","moodRating":3,"energy":2}}"""
        val m = JournalAi.parseAnalysis(raw).metrics
        assertEquals(4, m.pain)
        assertEquals(Swelling.MILD, m.swelling)
        assertEquals(3, m.moodRating)
        assertEquals(2, m.energy)
    }

    @Test
    fun nullAndOutOfRangeMetricsAreIgnored() {
        val raw = """{"reflection":"x","insights":[],"tips":[],"mood":"Mixed",
            "metrics":{"pain":99,"swelling":null,"moodRating":null,"energy":7}}"""
        val m = JournalAi.parseAnalysis(raw).metrics
        assertNull("pain out of 0-10 range dropped", m.pain)
        assertNull(m.swelling)
        assertNull(m.moodRating)
        assertNull("energy out of 1-5 range dropped", m.energy)
    }

    @Test
    fun missingMetricsBlockYieldsAllNulls() {
        val m = JournalAi.parseAnalysis("""{"reflection":"x","insights":[],"tips":[],"mood":"Steady"}""").metrics
        assertNull(m.pain); assertNull(m.swelling); assertNull(m.moodRating); assertNull(m.energy)
    }

    @Test
    fun toleratesCodeFencesAndProse() {
        val raw = "Sure! Here you go:\n```json\n{\"reflection\":\"ok\",\"insights\":[],\"tips\":[],\"mood\":\"Steady\"}\n```"
        val a = JournalAi.parseAnalysis(raw)
        assertEquals("ok", a.reflection)
        assertEquals(JournalMood.STEADY, a.mood)
    }

    @Test
    fun unknownMoodFallsBackToMixedAndArraysCapAtThree() {
        val raw = """{"reflection":"x","insights":["a","b","c","d"],"tips":[],"mood":"ecstatic"}"""
        val a = JournalAi.parseAnalysis(raw)
        assertEquals(JournalMood.MIXED, a.mood)          // unknown label -> MIXED
        assertEquals(3, a.insights.size)                 // capped at 3
        assertEquals(listOf("a", "b", "c"), a.insights)
    }

    @Test
    fun analysisRequestUsesJsonMode() {
        // json mode must be requested so the model returns parseable output
        val body = Groq.requestBody("sys", "user", Groq.CHAT_MODEL, jsonMode = true)
        assertTrue(body.contains("response_format"))
        assertTrue(body.contains("json_object"))
        // and omitted by default for normal chat
        assertFalse(Groq.requestBody("sys", "user").contains("response_format"))
    }

    @Test
    fun transcriptParsedFromWhisperResponse() {
        assertEquals("the leg felt good today", Groq.parseTranscript("""{"text":" the leg felt good today "}"""))
    }

    @Test
    fun weeklyPromptIncludesLast7DaysOfLogsAndEntries() {
        val logs = listOf(
            DailyLog.empty(today).copy(pain = 3, swelling = Swelling.MILD),
            DailyLog.empty(today.minusDays(10)).copy(pain = 8)  // outside the window
        )
        val entries = listOf(
            JournalEntry("1", today.minusDays(1), "felt strong on the walk", "", emptyList(), emptyList(), JournalMood.POSITIVE),
            JournalEntry("2", today.minusDays(20), "old entry", "", emptyList(), emptyList(), JournalMood.LOW)
        )
        val prompt = JournalAi.weeklyPrompt(logs, entries, today)
        assertTrue(prompt.contains("pain 3/10"))
        assertTrue(prompt.contains("felt strong on the walk"))
        assertFalse("logs older than 7 days excluded", prompt.contains("pain 8/10"))
        assertFalse("entries older than 7 days excluded", prompt.contains("old entry"))
    }
}
