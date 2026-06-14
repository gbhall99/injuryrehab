package com.recoverwell.app.ai

import com.recoverwell.core.json.Json
import com.recoverwell.core.model.DailyLog
import com.recoverwell.core.model.JournalEntry
import com.recoverwell.core.model.JournalMood
import java.time.LocalDate

/**
 * Turns a spoken recovery check-in into structured feedback (reflection,
 * insights, tips, mood) and produces the weekly narrative summary. Prompt
 * building and response parsing are pure and unit-tested; the network call
 * itself goes through [Groq].
 */
object JournalAi {

    data class Analysis(
        val reflection: String,
        val insights: List<String>,
        val tips: List<String>,
        val mood: JournalMood
    )

    private const val ANALYZE_RULES = """
The user just spoke a daily recovery journal entry; their transcript is the user message.
Reply with ONLY a JSON object of this exact shape:
{"reflection":"1-2 warm sentences reflecting back what they shared",
 "insights":["short observations or patterns, max 3"],
 "tips":["1-3 short, practical suggestions suited to their current phase"],
 "mood":"one of: Low, Mixed, Steady, Positive, Great"}
Keep each array to at most 3 short items. Do not give medical clearance; defer clinical
decisions to their physio. If they describe red-flag symptoms, make the first insight an
urgent note to seek medical advice now."""

    private const val SUMMARY_RULES = """
Write a short, encouraging weekly recovery summary (about 4-6 sentences) from the data
below: note progress, any patterns in pain/swelling/mood, and end with one gentle,
practical suggestion. Warm and plain-language. This is not medical advice; remind them to
raise anything concerning with their physio."""

    /** Analyze a transcript. [context] is the grounding system prompt from [AiContext]. */
    fun analyze(apiKey: String, context: String, transcript: String): Analysis =
        parseAnalysis(Groq.chat(apiKey, context + "\n" + ANALYZE_RULES, transcript, jsonMode = true))

    /** Generate the weekly narrative summary. */
    fun weeklySummary(apiKey: String, context: String, logs: List<DailyLog>,
                      entries: List<JournalEntry>, today: LocalDate): String =
        Groq.chat(apiKey, context + "\n" + SUMMARY_RULES, weeklyPrompt(logs, entries, today))

    /** Parse the model's JSON analysis, tolerating code fences / surrounding prose. */
    internal fun parseAnalysis(raw: String): Analysis {
        val json = Json.parse(extractJsonObject(raw))
        return Analysis(
            reflection = json.opt("reflection")?.asString()?.trim() ?: "",
            insights = strList(json, "insights"),
            tips = strList(json, "tips"),
            mood = JournalMood.from(json.opt("mood")?.asString())
        )
    }

    /** Build the user-message data block for the weekly summary. Pure. */
    internal fun weeklyPrompt(logs: List<DailyLog>, entries: List<JournalEntry>, today: LocalDate): String {
        val since = today.minusDays(6)
        val weekLogs = logs.filter { !it.date.isBefore(since) }.sortedBy { it.date }
        val weekEntries = entries.filter { !it.date.isBefore(since) }.sortedBy { it.date }
        return buildString {
            appendLine("Data for the last 7 days (oldest first):")
            appendLine()
            appendLine("Daily logs:")
            if (weekLogs.isEmpty()) appendLine("- none recorded")
            for (l in weekLogs) {
                val pain = l.pain?.let { "pain $it/10" } ?: "pain n/a"
                val swell = l.swelling?.label ?: "swelling n/a"
                appendLine("- ${l.date}: $pain, $swell")
            }
            appendLine()
            appendLine("Journal check-ins:")
            if (weekEntries.isEmpty()) appendLine("- none recorded")
            for (e in weekEntries) {
                appendLine("- ${e.date} (mood ${e.mood.label}): ${e.transcript.take(280)}")
            }
        }
    }

    private fun strList(json: com.recoverwell.core.json.JsonValue, key: String): List<String> =
        (json.opt(key)?.asArr() ?: emptyList()).map { it.asString().trim() }.filter { it.isNotBlank() }.take(3)

    /** Models sometimes wrap JSON in prose or ```fences```; grab the outer object. */
    internal fun extractJsonObject(raw: String): String {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        return if (start in 0 until end) raw.substring(start, end + 1) else raw
    }
}
