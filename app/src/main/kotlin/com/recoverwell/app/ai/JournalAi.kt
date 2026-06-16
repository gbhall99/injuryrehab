package com.recoverwell.app.ai

import com.recoverwell.core.json.Json
import com.recoverwell.core.json.JsonValue
import com.recoverwell.core.model.DailyLog
import com.recoverwell.core.model.JournalEntry
import com.recoverwell.core.model.JournalMood
import com.recoverwell.core.model.Swelling
import java.time.LocalDate

/**
 * Turns a spoken recovery check-in into structured feedback (reflection,
 * insights, tips, mood) and produces the weekly narrative summary. Prompt
 * building and response parsing are pure and unit-tested; the network call
 * itself goes through [Groq].
 */
object JournalAi {

    /**
     * Structured daily-log values the user mentioned out loud. Every field is
     * nullable: only set what was actually said, so we never invent a number.
     */
    data class Metrics(
        val pain: Int?,
        val swelling: Swelling?,
        val moodRating: Int?,
        val energy: Int?
    )

    data class Analysis(
        val reflection: String,
        val insights: List<String>,
        val tips: List<String>,
        val mood: JournalMood,
        val metrics: Metrics,
        /** True when the entry describes possible emergency/urgent-care symptoms. */
        val redFlag: Boolean,
        val redFlagNote: String
    )

    private const val ANALYZE_RULES = """
The user just spoke a daily recovery journal entry; their transcript is the user message.
Reply with ONLY a JSON object of this exact shape:
{"reflection":"1-2 warm sentences reflecting back what they shared",
 "insights":["short observations or patterns, max 3"],
 "tips":["1-3 short, practical suggestions suited to their current phase"],
 "mood":"one of: Low, Mixed, Steady, Positive, Great",
 "metrics":{"pain":0-10 integer or null,
            "swelling":"None|Mild|Moderate|Severe or null",
            "moodRating":1-5 integer or null,
            "energy":1-5 integer or null},
 "redFlag":true or false,
 "redFlagNote":"if redFlag, one short sentence on the concerning symptom; else empty"}
For metrics, ONLY fill a field if the user actually described it; otherwise use null - never
guess a value. Set redFlag to true ONLY for symptoms needing urgent medical attention: signs
of a blood clot (new calf pain/swelling/warmth/redness), chest pain, breathlessness, fever,
a wound that looks infected, or sudden severe new pain. Keep each array to at most 3 short
items. Do not give medical clearance; defer clinical decisions to their physio."""

    private const val SUMMARY_RULES = """
Write a short, encouraging weekly recovery summary (about 4-6 sentences) from the data
below: note progress, any patterns in pain/swelling/mood, and end with one gentle,
practical suggestion. Warm and plain-language. This is not medical advice; remind them to
raise anything concerning with their physio."""

    private const val PATTERNS_RULES = """
Look across the recovery data below (last few weeks of daily logs and journal check-ins) and
surface up to 3 genuine patterns or correlations worth noticing (e.g. pain rising after busier
days, mood tracking with sleep). Reply as up to 3 short bullet-style sentences, plain text, one
per line, no numbering. If there isn't enough data for a real pattern, say so in one line. This
is observational, not medical advice."""

    /** Analyze a transcript. [context] is the grounding system prompt from [AiContext]. */
    fun analyze(apiKey: String, context: String, transcript: String): Analysis =
        parseAnalysis(Groq.chat(apiKey, context + "\n" + ANALYZE_RULES, transcript, jsonMode = true))

    /** Generate the weekly narrative summary. */
    fun weeklySummary(apiKey: String, context: String, logs: List<DailyLog>,
                      entries: List<JournalEntry>, today: LocalDate): String =
        Groq.chat(apiKey, context + "\n" + SUMMARY_RULES, weeklyPrompt(logs, entries, today))

    /** Surface cross-entry patterns/correlations over recent data. */
    fun findPatterns(apiKey: String, context: String, logs: List<DailyLog>,
                     entries: List<JournalEntry>, today: LocalDate): String =
        Groq.chat(apiKey, context + "\n" + PATTERNS_RULES, patternsPrompt(logs, entries, today))

    /** Parse the model's JSON analysis, tolerating code fences / surrounding prose. */
    internal fun parseAnalysis(raw: String): Analysis {
        val json = Json.parse(extractJsonObject(raw))
        val m = json.opt("metrics")
        return Analysis(
            reflection = json.opt("reflection")?.asString()?.trim() ?: "",
            insights = strList(json, "insights"),
            tips = strList(json, "tips"),
            mood = JournalMood.from(json.opt("mood")?.asString()),
            metrics = Metrics(
                pain = intIn(m, "pain", 0, 10),
                swelling = swellingOf(str(m, "swelling")),
                moodRating = intIn(m, "moodRating", 1, 5),
                energy = intIn(m, "energy", 1, 5)
            ),
            redFlag = (json.opt("redFlag") as? JsonValue.Bool)?.value ?: false,
            redFlagNote = json.opt("redFlagNote")?.asString()?.trim() ?: ""
        )
    }

    /** Numeric field, only when present as a real number and within range. */
    private fun intIn(json: JsonValue?, key: String, min: Int, max: Int): Int? {
        val n = (json?.opt(key) as? JsonValue.Num)?.value?.toInt() ?: return null
        return if (n in min..max) n else null
    }

    private fun str(json: JsonValue?, key: String): String? =
        (json?.opt(key) as? JsonValue.Str)?.value?.trim()?.takeIf { it.isNotBlank() }

    private fun swellingOf(s: String?): Swelling? {
        if (s == null) return null
        return Swelling.values().firstOrNull { it.label.equals(s, true) || it.name.equals(s, true) }
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

    /** Wider (3-week) data block for cross-entry pattern mining. Pure. */
    internal fun patternsPrompt(logs: List<DailyLog>, entries: List<JournalEntry>, today: LocalDate): String {
        val since = today.minusDays(20)
        val recentLogs = logs.filter { !it.date.isBefore(since) }.sortedBy { it.date }
        val recentEntries = entries.filter { !it.date.isBefore(since) }.sortedBy { it.date }
        return buildString {
            appendLine("Recovery data for the last ~3 weeks (oldest first):")
            appendLine()
            appendLine("Daily logs:")
            if (recentLogs.isEmpty()) appendLine("- none recorded")
            for (l in recentLogs) {
                val pain = l.pain?.let { "pain $it/10" } ?: "pain n/a"
                val swell = l.swelling?.label ?: "swelling n/a"
                val energy = l.energy?.let { ", energy $it/5" } ?: ""
                appendLine("- ${l.date}: $pain, $swell$energy")
            }
            appendLine()
            appendLine("Journal check-ins:")
            if (recentEntries.isEmpty()) appendLine("- none recorded")
            for (e in recentEntries) appendLine("- ${e.date} (mood ${e.mood.label}): ${e.transcript.take(220)}")
        }
    }

    private fun strList(json: com.recoverwell.core.json.JsonValue, key: String): List<String> =
        (json.opt(key)?.asArr() ?: emptyList())
            .mapNotNull { (it as? JsonValue.Str)?.value?.trim() }.filter { it.isNotBlank() }.take(3)

    /** Models sometimes wrap JSON in prose or ```fences```; grab the outer object. */
    internal fun extractJsonObject(raw: String): String {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        return if (start in 0 until end) raw.substring(start, end + 1) else raw
    }
}
