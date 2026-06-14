package com.recoverwell.app.screens

import android.view.Gravity
import android.view.View
import com.recoverwell.app.MainActivity
import com.recoverwell.app.ai.AiContext
import com.recoverwell.app.ai.Groq
import com.recoverwell.app.ai.JournalAi
import com.recoverwell.app.ai.VoiceRecorder
import com.recoverwell.app.ui.Forms
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.logic.JournalTrends
import com.recoverwell.core.model.JournalEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * The "Rosebud-style" recovery journal: record a short spoken check-in, which is
 * transcribed (Whisper) and analyzed (chat) by Groq into a reflection, insights,
 * tips and a mood. Entries persist (and back up); a weekly AI summary and simple
 * mood trend tie them together. Requires the user to have opted into AI features.
 */
object JournalScreen {

    private var recorder: VoiceRecorder? = null
    private var recording = false
    private var processing = false
    private var error: String? = null

    private var summary: String? = null
    private var summaryLoading = false
    private var summaryError: String? = null

    private val dateFmt = DateTimeFormatter.ofPattern("EEE d MMM")

    fun build(a: MainActivity): View {
        val today = LocalDate.now()
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Recovery journal") { a.popOverlay() })

        if (!AiScreen.enabled(a)) {
            col.addView(Ui.caption(a, "The recovery journal uses AI to turn a spoken check-in into " +
                "insights and tips. Turn on AI features to use it."))
            col.addView(Ui.fullWidth(Ui.button(a, "Set up AI features") {
                a.pushOverlay("AI features") { AiScreen.settings(a) }
            }, a))
            col.addView(Ui.spacer(a, 24))
            return Ui.scroll(a, col)
        }

        col.addView(Ui.caption(a, "Speak freely about your day - how the leg felt, wins, worries. " +
            "We transcribe it, reflect it back, and spot patterns. Audio is deleted after; only text is kept."))
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
                card.addView(Ui.caption(a, "Speak freely, then stop when you're done."))
                card.addView(Ui.fullWidth(Ui.button(a, "Stop & analyze") { stopAndAnalyze(a, today) }, a))
                card.addView(Ui.fullWidth(Ui.textButton(a, "Cancel") {
                    recorder?.cancel(); recorder = null; recording = false; error = null; a.refresh()
                }, a, 4))
            }
            else -> {
                if (trends.loggedToday) {
                    card.addView(Ui.text(a, "You've checked in today", 14.5f, Ui.TEXT))
                    card.addView(Ui.caption(a, "Record again any time to add another entry."))
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

        // ---- history --------------------------------------------------------
        if (entries.isNotEmpty()) {
            col.addView(Ui.section(a, "Past check-ins"))
            for (e in entries) col.addView(entryCard(a, e))
        }

        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
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
            var err: String? = null
            try {
                val transcript = Groq.transcribe(key, file)
                if (transcript.isBlank()) throw Groq.GroqException("Didn't catch any speech - try again.")
                val analysis = JournalAi.analyze(key, context, transcript)
                a.store.addJournalEntry(
                    JournalEntry(UUID.randomUUID().toString(), today, transcript,
                        analysis.reflection, analysis.insights, analysis.tips, analysis.mood)
                )
            } catch (e: Exception) {
                err = e.message ?: "Something went wrong analyzing your check-in."
            } finally {
                file.delete()
            }
            a.runOnUiThread {
                processing = false
                recorder = null
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
        head.addView(Ui.iconButton(a, "ic_close", Ui.TEXT_DIM, desc = "Delete check-in") {
            Forms.confirm(a, "Delete check-in?", "This journal entry will be removed.") {
                a.store.deleteJournalEntry(e.id); a.refresh()
            }
        })
        card.addView(head)
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
