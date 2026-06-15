package com.recoverwell.app.screens

import android.view.Gravity
import android.view.View
import android.widget.EditText
import com.recoverwell.app.MainActivity
import com.recoverwell.app.ai.AiContext
import com.recoverwell.app.ai.Groq
import com.recoverwell.app.ai.JournalAi
import com.recoverwell.app.ai.VoiceRecorder
import com.recoverwell.app.ui.Forms
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.logic.JournalTrends
import com.recoverwell.core.model.JournalEntry
import com.recoverwell.core.model.Swelling
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * The "Rosebud-style" recovery journal, and the voice front-end to the daily
 * check-in: record a spoken update, which Groq transcribes (Whisper) and turns
 * into a reflection, insights, tips, a mood AND any daily metrics the user
 * mentioned (pain/swelling/mood/energy). Those are shown pre-filled and editable
 * so one spoken check-in fills today's log and creates a journal entry together.
 * A weekly AI summary and mood trend tie entries together. Requires AI opt-in.
 */
object JournalScreen {

    private var recorder: VoiceRecorder? = null
    private var recording = false
    private var processing = false
    private var error: String? = null

    /** A finished analysis awaiting the user's confirmation before it's saved. */
    private class Pending(val transcript: String, val analysis: JournalAi.Analysis)
    private var pending: Pending? = null

    private var summary: String? = null
    private var summaryLoading = false
    private var summaryError: String? = null

    private var patterns: String? = null
    private var patternsLoading = false
    private var patternsError: String? = null

    private var editingId: String? = null

    private val dateFmt = DateTimeFormatter.ofPattern("EEE d MMM")

    fun build(a: MainActivity): View {
        val today = LocalDate.now()
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Recovery journal") { a.popOverlay() })

        if (!AiScreen.enabled(a)) {
            col.addView(Ui.caption(a, "The recovery journal uses AI to turn a spoken check-in into " +
                "your daily log plus insights and tips. Turn on AI features to use it."))
            col.addView(Ui.fullWidth(Ui.button(a, "Set up AI features") {
                a.pushOverlay("AI features") { AiScreen.settings(a) }
            }, a))
            col.addView(Ui.spacer(a, 24))
            return Ui.scroll(a, col)
        }

        // confirmation step takes over the screen until saved or discarded
        pending?.let {
            col.addView(confirmCard(a, today, it))
            col.addView(Ui.spacer(a, 24))
            return Ui.scroll(a, col)
        }

        col.addView(Ui.caption(a, "Speak freely about your day - how the leg felt, pain, swelling, wins, " +
            "worries. We turn it into your daily check-in plus a reflection. Audio is deleted after; only text is kept."))
        col.addView(Ui.spacer(a, 8))

        val entries = a.store.journalEntries().sortedByDescending { it.date }

        // ---- trends header --------------------------------------------------
        val trends = JournalTrends.compute(entries, today)
        if (trends.total > 0) {
            val head = Ui.card(a, Ui.INFO_BG)
            val streakStr = when {
                trends.streakDays >= 2 -> "${trends.streakDays}-day streak"
                trends.streakDays == 1 -> "Checked in"
                else -> "No active streak"
            }
            val moodStr = when (trends.direction) {
                JournalTrends.Direction.UP -> "mood trending up"
                JournalTrends.Direction.DOWN -> "mood dipping lately"
                JournalTrends.Direction.FLAT -> "mood holding steady"
                JournalTrends.Direction.UNKNOWN -> "${trends.total} entr${if (trends.total == 1) "y" else "ies"} so far"
            }
            head.addView(Ui.text(a, "$streakStr · $moodStr", 15f, Ui.ON_INFO_BG, bold = true))
            col.addView(head)
        }

        // ---- record control -------------------------------------------------
        col.addView(Ui.section(a, "Today's check-in"))
        val card = Ui.card(a)
        when {
            processing -> {
                card.addView(Ui.text(a, "Transcribing and analyzing…", 15f, Ui.TEXT, bold = true))
                card.addView(Ui.caption(a, "This takes a few seconds."))
            }
            recording -> {
                card.addView(Ui.text(a, "Recording…", 15.5f, Ui.WARN, bold = true))
                card.addView(Ui.caption(a, "Mention pain, swelling, mood or energy if you can - then stop."))
                card.addView(Ui.fullWidth(Ui.button(a, "Stop & analyze") { stopAndAnalyze(a, today) }, a))
                card.addView(Ui.fullWidth(Ui.textButton(a, "Cancel") {
                    recorder?.cancel(); recorder = null; recording = false; error = null; a.refresh()
                }, a, 4))
            }
            else -> {
                if (trends.loggedToday) {
                    card.addView(Ui.text(a, "You've checked in today", 14.5f, Ui.TEXT))
                    card.addView(Ui.caption(a, "Record again to update today's entry."))
                } else {
                    card.addView(Ui.text(a, "How has today been?", 14.5f, Ui.TEXT))
                }
                card.addView(Ui.fullWidth(Ui.button(a, "Record check-in") { startRecording(a) }, a))
            }
        }
        error?.let {
            card.addView(Ui.spacer(a, 6))
            card.addView(Ui.text(a, it, 13.5f, Ui.WARN))
        }
        col.addView(card)

        // ---- weekly AI summary (Slice 3) ------------------------------------
        if (entries.isNotEmpty()) {
            // surface this week's cached summary (generated here or via the Monday nudge)
            val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
            if (summary == null && !summaryLoading && summaryError == null) {
                a.store.cachedWeeklySummary(weekStart).takeIf { it.isNotBlank() }?.let { summary = it }
            }
            col.addView(Ui.section(a, "Weekly summary"))
            val sumCard = Ui.card(a)
            when {
                summaryLoading -> sumCard.addView(Ui.text(a, "Writing your weekly summary…", 14.5f, Ui.TEXT))
                summary != null -> {
                    sumCard.addView(Ui.text(a, summary!!, 14.5f, Ui.TEXT))
                    sumCard.addView(Ui.spacer(a, 6))
                    sumCard.addView(Ui.text(a, "AI-generated · not medical advice.", 12f, Ui.TEXT_DIM))
                    sumCard.addView(Ui.fullWidth(Ui.textButton(a, "Regenerate") { genSummary(a, today) }, a, 4))
                }
                else -> {
                    summaryError?.let { sumCard.addView(Ui.text(a, it, 13.5f, Ui.WARN)) }
                    sumCard.addView(Ui.text(a, "Pull your last 7 days of logs and check-ins into a short recap.", 14f, Ui.TEXT))
                    sumCard.addView(Ui.fullWidth(Ui.tonalButton(a, "Generate weekly summary") { genSummary(a, today) }, a))
                }
            }
            col.addView(sumCard)
        }

        // ---- cross-entry pattern mining -------------------------------------
        if (entries.size >= 3) {
            col.addView(Ui.section(a, "Patterns"))
            val patCard = Ui.card(a)
            when {
                patternsLoading -> patCard.addView(Ui.text(a, "Looking for patterns…", 14.5f, Ui.TEXT))
                patterns != null -> {
                    for (line in patterns!!.lines().map { it.trim().removePrefix("- ").trim() }.filter { it.isNotBlank() })
                        patCard.addView(bullet(a, line))
                    patCard.addView(Ui.fullWidth(Ui.textButton(a, "Refresh") { findPatterns(a, today) }, a, 4))
                }
                else -> {
                    patternsError?.let { patCard.addView(Ui.text(a, it, 13.5f, Ui.WARN)) }
                    patCard.addView(Ui.text(a, "Spot correlations across the last few weeks of logs and check-ins.", 14f, Ui.TEXT))
                    patCard.addView(Ui.fullWidth(Ui.tonalButton(a, "Find patterns") { findPatterns(a, today) }, a))
                }
            }
            col.addView(patCard)
        }

        // ---- history --------------------------------------------------------
        if (entries.isNotEmpty()) {
            col.addView(Ui.section(a, "Past check-ins"))
            for (e in entries) col.addView(entryCard(a, e))
        }

        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    /** Editable, pre-filled daily check-in + reflection, shown after analysis. */
    private fun confirmCard(a: MainActivity, today: LocalDate, p: Pending): View {
        val log = a.store.dailyLog(today)
        val m = p.analysis.metrics
        val wrap = Ui.column(a, 0)

        if (p.analysis.redFlag) {
            val flag = Ui.card(a, Ui.WARN_BG)
            flag.addView(Ui.text(a, "Worth getting checked", 15f, Ui.WARN, bold = true))
            flag.addView(Ui.text(a, p.analysis.redFlagNote.ifBlank {
                "Something you mentioned could need medical attention." }, 14f, Ui.TEXT))
            flag.addView(Ui.fullWidth(Ui.dangerButton(a, "See red-flag guidance") {
                a.pushOverlay("Red flags") { RedFlagsScreen.build(a) }
            }, a))
            wrap.addView(flag)
        }

        val card = Ui.card(a)
        card.addView(Ui.text(a, "Here's what I heard", 16f, Ui.TEXT, bold = true))
        if (p.analysis.reflection.isNotBlank()) {
            card.addView(Ui.spacer(a, 2))
            card.addView(Ui.text(a, p.analysis.reflection, 14.5f, Ui.TEXT))
        }
        card.addView(Ui.spacer(a, 4))
        card.addView(Ui.caption(a, "Check your daily numbers below - I filled in what you mentioned. Edit anything, then save."))

        // pain (always present; prefer what was heard, else today's, else yesterday's)
        card.addView(Forms.label(a, "Pain"))
        var pain = m.pain ?: log.pain ?: (a.store.dailyLog(today.minusDays(1)).pain ?: 0)
        card.addView(Forms.scaleSlider(a, 10, pain, "0 · None", "10 · Worst") { pain = it })

        var mood: Int? = m.moodRating ?: log.mood
        card.addView(Forms.label(a, "Mood · optional"))
        card.addView(Forms.choiceRow(a, (1..5).toList(), { "$it" }, mood) { mood = it })

        var swelling: Swelling? = m.swelling ?: log.swelling
        card.addView(Forms.label(a, "Swelling · optional"))
        card.addView(Forms.choiceRow(a, Swelling.values().toList(), { it.label }, swelling) { swelling = it })

        var energy: Int? = m.energy ?: log.energy
        card.addView(Forms.label(a, "Energy · optional"))
        card.addView(Forms.choiceRow(a, (1..5).toList(), { "$it" }, energy) { energy = it })

        card.addView(Forms.label(a, "What you said"))
        val notesEdit: EditText = Forms.editText(a, p.transcript, "Your words", multiline = true)
        card.addView(notesEdit)

        if (p.analysis.insights.isNotEmpty()) {
            card.addView(Ui.spacer(a, 6))
            card.addView(Ui.text(a, "Noticed", 12.5f, Ui.TEXT_DIM, bold = true))
            for (i in p.analysis.insights) card.addView(bullet(a, i))
        }
        if (p.analysis.tips.isNotEmpty()) {
            card.addView(Ui.spacer(a, 6))
            card.addView(Ui.text(a, "Try", 12.5f, Ui.TEXT_DIM, bold = true))
            for (t in p.analysis.tips) card.addView(bullet(a, t))
        }

        card.addView(Ui.fullWidth(Ui.button(a, "Save check-in") {
            val transcript = notesEdit.text.toString().trim().ifBlank { p.transcript }
            // one save updates today's daily log...
            a.store.saveDailyLog(log.copy(pain = pain, mood = mood, swelling = swelling, energy = energy))
            // ...and records today's journal entry (one per day - replaces any earlier one)
            a.store.upsertJournalEntry(
                JournalEntry(UUID.randomUUID().toString(), today, transcript,
                    p.analysis.reflection, p.analysis.insights, p.analysis.tips, p.analysis.mood)
            )
            // surface an urgent prompt on Today if the entry flagged a concerning symptom
            if (p.analysis.redFlag) a.store.setRedFlagAlert(today, p.analysis.redFlagNote)
            pending = null
            android.widget.Toast.makeText(a, "Check-in saved", android.widget.Toast.LENGTH_SHORT).show()
            a.refresh()
        }, a))
        card.addView(Ui.fullWidth(Ui.textButton(a, "Discard") { pending = null; a.refresh() }, a, 4))
        wrap.addView(card)
        return wrap
    }

    private fun startRecording(a: MainActivity) {
        a.requestMic { granted ->
            if (!granted) {
                error = "Microphone permission is needed to record a check-in."
                a.refresh()
                return@requestMic
            }
            val rec = VoiceRecorder(a)
            if (rec.start()) {
                recorder = rec; recording = true; error = null
            } else {
                error = "Couldn't start recording. Make sure nothing else is using the microphone."
            }
            a.refresh()
        }
    }

    private fun stopAndAnalyze(a: MainActivity, today: LocalDate) {
        val file = recorder?.stop()
        recording = false
        if (file == null) {
            recorder = null
            error = "Recording failed - please try again."
            a.refresh()
            return
        }
        processing = true
        error = null
        a.refresh()
        val key = AiScreen.apiKey(a)
        val context = AiContext.system(a.store.profile(), a.store.allLogs(), today)
        Thread {
            var result: JournalAi.Analysis? = null
            var transcript = ""
            var err: String? = null
            try {
                transcript = Groq.transcribe(key, file)
                if (transcript.isBlank()) throw Groq.GroqException("Didn't catch any speech - try again.")
                result = JournalAi.analyze(key, context, transcript)
            } catch (e: Exception) {
                err = e.message ?: "Something went wrong analyzing your check-in."
            } finally {
                file.delete()
            }
            val analysis = result
            a.runOnUiThread {
                processing = false
                recorder = null
                if (analysis != null) pending = Pending(transcript, analysis)
                error = err
                a.refresh()
            }
        }.start()
    }

    private fun genSummary(a: MainActivity, today: LocalDate) {
        summaryLoading = true
        summaryError = null
        summary = null
        a.refresh()
        val key = AiScreen.apiKey(a)
        val context = AiContext.system(a.store.profile(), a.store.allLogs(), today)
        val logs = a.store.allLogs()
        val entries = a.store.journalEntries()
        val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
        Thread {
            var text: String? = null
            var err: String? = null
            try {
                text = JournalAi.weeklySummary(key, context, logs, entries, today)
            } catch (e: Exception) {
                err = e.message ?: "Couldn't generate the summary."
            }
            a.runOnUiThread {
                summaryLoading = false
                summary = text
                summaryError = err
                if (text != null) a.store.saveWeeklySummary(weekStart, text)
                a.refresh()
            }
        }.start()
    }

    private fun findPatterns(a: MainActivity, today: LocalDate) {
        patternsLoading = true
        patternsError = null
        patterns = null
        a.refresh()
        val key = AiScreen.apiKey(a)
        val context = AiContext.system(a.store.profile(), a.store.allLogs(), today)
        val logs = a.store.allLogs()
        val entries = a.store.journalEntries()
        Thread {
            var text: String? = null
            var err: String? = null
            try {
                text = JournalAi.findPatterns(key, context, logs, entries, today)
            } catch (e: Exception) {
                err = e.message ?: "Couldn't find patterns right now."
            }
            a.runOnUiThread {
                patternsLoading = false
                patterns = text
                patternsError = err
                a.refresh()
            }
        }.start()
    }

    private fun entryCard(a: MainActivity, e: JournalEntry): View {
        val card = Ui.card(a)
        val head = Ui.row(a)
        head.gravity = Gravity.CENTER_VERTICAL
        head.addView(Ui.weight(Ui.text(a, e.date.format(dateFmt), 13f, Ui.TEXT_DIM, bold = true), 1f))
        head.addView(Ui.text(a, e.mood.label, 12.5f, Ui.PRIMARY, bold = true))
        head.addView(Ui.iconButton(a, "ic_edit", Ui.TEXT_DIM, desc = "Edit check-in") {
            editingId = e.id; a.refresh()
        })
        head.addView(Ui.iconButton(a, "ic_close", Ui.TEXT_DIM, desc = "Delete check-in") {
            Forms.confirm(a, "Delete check-in?", "This journal entry will be removed.") {
                a.store.deleteJournalEntry(e.id); a.refresh()
            }
        })
        card.addView(head)

        if (editingId == e.id) {
            card.addView(Forms.label(a, "What you said"))
            val tEdit = Forms.editText(a, e.transcript, "Your words", multiline = true)
            card.addView(tEdit)
            card.addView(Forms.label(a, "Reflection"))
            val rEdit = Forms.editText(a, e.reflection, "Reflection", multiline = true)
            card.addView(rEdit)
            card.addView(Ui.fullWidth(Ui.button(a, "Save") {
                a.store.updateJournalEntry(e.copy(
                    transcript = tEdit.text.toString().trim(),
                    reflection = rEdit.text.toString().trim()))
                editingId = null; a.refresh()
            }, a))
            card.addView(Ui.fullWidth(Ui.textButton(a, "Cancel") { editingId = null; a.refresh() }, a, 4))
            return card
        }

        if (e.reflection.isNotBlank()) {
            card.addView(Ui.spacer(a, 2))
            card.addView(Ui.text(a, e.reflection, 14.5f, Ui.TEXT))
        }
        if (e.insights.isNotEmpty()) {
            card.addView(Ui.spacer(a, 6))
            card.addView(Ui.text(a, "Noticed", 12.5f, Ui.TEXT_DIM, bold = true))
            for (i in e.insights) card.addView(bullet(a, i))
        }
        if (e.tips.isNotEmpty()) {
            card.addView(Ui.spacer(a, 6))
            card.addView(Ui.text(a, "Try", 12.5f, Ui.TEXT_DIM, bold = true))
            for (t in e.tips) card.addView(bullet(a, t))
        }
        return card
    }

    private fun bullet(a: MainActivity, text: String): View {
        val row = Ui.row(a)
        row.gravity = Gravity.TOP
        row.setPadding(0, Ui.dp(a, 2), 0, Ui.dp(a, 2))
        val dot = Ui.text(a, "•", 14f, Ui.PRIMARY, bold = true)
        dot.setPadding(0, 0, Ui.dp(a, 8), 0)
        row.addView(dot)
        row.addView(Ui.weight(Ui.text(a, text, 14f, Ui.TEXT), 1f))
        return row
    }
}
