package com.recoverwell.app.screens

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.Toast
import com.recoverwell.app.MainActivity
import com.recoverwell.app.notify.Reminders
import com.recoverwell.app.ui.Forms
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.logic.Appointments
import com.recoverwell.core.logic.PhaseEngine
import com.recoverwell.core.logic.PhysioPrep
import com.recoverwell.core.logic.ReturnToSport
import com.recoverwell.core.model.Appointment
import com.recoverwell.core.model.PhysioNote
import com.recoverwell.core.protocol.ProtocolRegistry
import java.time.LocalDate
import java.util.UUID

/**
 * The physio loop in three steps: the next visit (add or change it in a short
 * form), what to raise and ask (one list, one pack to share), and after the
 * visit - confirm a progression, update the plan (one door), keep a note.
 */
object PhysioScreen {

    fun build(a: MainActivity): View {
        val today = LocalDate.now()
        val profile = a.store.profile()
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Physio visits") { a.popOverlay() })

        // ---- next visit: what's booked, and one way to add one ----------------
        col.addView(Ui.section(a, "Next visit"))
        val outlook = Appointments.outlook(profile.appointments, today)
        val apptCard = Ui.card(a)
        val upcoming = profile.appointments.filter { !it.completed && !it.date.isBefore(today) }.sortedBy { it.date }
        if (upcoming.isEmpty() && outlook.overdue.isEmpty()) {
            apptCard.addView(Ui.text(a, if (outlook.needsRebooking) "Time to book your next visit"
                else "No visit booked yet", 15.5f, Ui.TEXT, bold = true))
            apptCard.addView(Ui.caption(a, "Add it and the app reminds you the day before, with your questions ready."))
        }
        upcoming.firstOrNull()?.let { appt ->
            val days = java.time.temporal.ChronoUnit.DAYS.between(today, appt.date)
            apptCard.addView(Ui.text(a, appt.label, 15.5f, Ui.TEXT, bold = true))
            apptCard.addView(Ui.caption(a, "${Forms.friendlyDate(appt.date)} · " +
                (if (days == 0L) "today" else "in $days day${if (days == 1L) "" else "s"}") +
                (if (appt.withWhom.isNotBlank()) " · with ${appt.withWhom}" else "")))
            apptCard.addView(Ui.buttonPair(a,
                Ui.tonalButton(a, "Mark done & capture") {
                    completeAppointment(a, appt); a.pushOverlay("Visit note") { captureNote(a) }
                },
                Ui.textButton(a, "Edit") { a.pushOverlay("Edit appointment") { appointmentEditor(a, appt) } }))
        }
        // later visits: one line each, tap to change
        for (appt in upcoming.drop(1)) {
            apptCard.addView(Ui.divider(a))
            apptCard.addView(Ui.text(a, "Then: ${appt.label} · ${Forms.friendlyDate(appt.date)}", 14f, Ui.TEXT).apply {
                background = Ui.ripple(a, Ui.rounded(0, 10f))
                isClickable = true
                isFocusable = true
                contentDescription = "Edit ${appt.label} on ${Forms.friendlyDate(appt.date)}"
                setOnClickListener { a.pushOverlay("Edit appointment") { appointmentEditor(a, appt) } }
            })
        }
        for (o in outlook.overdue) {
            if (apptCard.childCount > 0) apptCard.addView(Ui.divider(a))
            apptCard.addView(Ui.text(a, "Was: ${o.label} · ${Forms.friendlyDate(o.date)}" +
                (if (o.withWhom.isNotBlank()) " · with ${o.withWhom}" else ""), 14f, Ui.WARN, bold = true))
            apptCard.addView(Ui.buttonPair(a,
                Ui.tonalButton(a, "How did it go?") {
                    completeAppointment(a, o); a.pushOverlay("Visit note") { captureNote(a) }
                },
                Ui.textButton(a, "Edit") { a.pushOverlay("Edit appointment") { appointmentEditor(a, o) } }))
        }
        // adding is a short form of its own (was four always-open fields here)
        val add = { a.pushOverlay("New appointment") { appointmentEditor(a, null) } }
        apptCard.addView(Ui.fullWidth(
            if (upcoming.isEmpty()) Ui.button(a, "Add appointment") { add() }
            else Ui.textButton(a, "Add appointment") { add() }, a, 6))
        col.addView(apptCard)

        // ---- for your visit: what to raise, what to ask, one pack to share ----
        val pack = PhysioPrep.build(
            profile, a.store.allLogs(), a.store.allEvents(), a.store.medications(), a.store.tasks(),
            a.store.selfTestResults(), a.store.rtsSignoffs(), today
        )
        col.addView(Ui.section(a, "For your visit"))
        val packCard = Ui.card(a)
        if (pack.discussionPoints.isNotEmpty()) {
            packCard.addView(Ui.text(a, "Worth raising", 13.5f, Ui.TEXT_DIM, bold = true))
            for (p in pack.discussionPoints) packCard.addView(bullet(a, p))
        }
        // the stage's evergreen questions and the user's own, as one list (the pack carries both)
        val questions = a.store.physioQuestions()
        if (pack.stageQuestions.isNotEmpty() || questions.isNotEmpty()) {
            if (packCard.childCount > 0) packCard.addView(Ui.spacer(a, 6))
            packCard.addView(Ui.text(a, "Questions to ask", 13.5f, Ui.TEXT_DIM, bold = true))
            for (q in pack.stageQuestions.filterNot { it in questions }) packCard.addView(bullet(a, q))
            questions.forEachIndexed { i, q ->
                val row = Ui.row(a)
                row.gravity = Gravity.CENTER_VERTICAL
                row.addView(Ui.weight(bullet(a, q), 1f))
                row.addView(Ui.iconButton(a, "ic_close", Ui.TEXT_DIM, desc = "Remove question: $q") {
                    a.store.savePhysioQuestions(a.store.physioQuestions().filterIndexed { j, _ -> j != i })
                    a.refresh()
                })
                packCard.addView(row)
            }
        }
        packCard.addView(Ui.fullWidth(Ui.textButton(a, "Add a question") { addQuestion(a) }, a, 2))
        col.addView(packCard)
        val copyBtn = Ui.tonalButton(a, "Copy pack") {
            copyToClipboard(a, packText(pack, questions))
            Toast.makeText(a, "Copied - paste into notes or a message", Toast.LENGTH_SHORT).show()
        }
        val pdfBtn = Ui.tonalButton(a, "Share PDF") { a.exportPdf() }
        col.addView(Ui.buttonPair(a, copyBtn, pdfBtn))

        // ---- after your visit: what the physio decided goes straight into the plan ----
        col.addView(Ui.section(a, "After your visit"))
        val gate = PhaseEngine.nextPhaseGate(profile, today)
        if (gate.nextPhase != null) {
            col.addView(Ui.listRow(a, "ic_flag", "Confirm phase ${gate.nextPhase!!.number} progression",
                "${gate.nextPhase!!.title}") {
                TodayScreen.confirmGate(a, gate.nextPhase!!.number, today)
            })
        }
        // return-to-sport sign-offs the physio may have granted
        val rts = ReturnToSport.progress(profile, a.store.selfTestResults(), a.store.rtsSignoffs(), today)
        val signable = rts.rungs.filter {
            it.rung.requiresPhysioSignoff && !it.physioSignedOff &&
                (it.state == ReturnToSport.RungState.CURRENT || it.testsMet)
        }
        for (s in signable) {
            col.addView(Ui.listRow(a, "ic_flag", "Record clearance: ${s.rung.title}",
                "Physio sign-off for this return-to-sport stage") {
                Forms.confirm(a, "Confirm physio clearance",
                    "Record that your physio cleared you for \"${s.rung.title}\"?") {
                    a.store.setRtsSignoff(s.rung.id, true); a.refresh()
                }
            })
        }
        // one door for every plan change (boot angle and dates, phases, exercises, weight-bearing)
        col.addView(Ui.listRow(a, "ic_calendar", "Update my plan",
            "Boot, dates, phases or exercises your physio changed") {
            a.pushOverlay("Configure my plan") { MoreScreen.planEditor(a) }
        })
        col.addView(Ui.listRow(a, "ic_edit", "Add a visit note",
            "What your physio said - kept in your backup") { a.pushOverlay("Visit note") { captureNote(a) } })

        // ---- visit notes history -------------------------------------------
        val notes = a.store.physioNotes().sortedByDescending { it.date }
        if (notes.isNotEmpty()) {
            col.addView(Ui.section(a, "Visit notes"))
            for (n in notes) {
                val card = Ui.card(a)
                val head = Ui.row(a)
                head.addView(Ui.weight(Ui.text(a, n.date.toString(), 13f, Ui.TEXT_DIM, bold = true), 1f))
                head.addView(Ui.iconButton(a, "ic_close", Ui.TEXT_DIM, desc = "Delete note") {
                    Forms.confirm(a, "Delete note?", "This visit note will be removed.") {
                        a.store.deletePhysioNote(n.id); a.refresh()
                    }
                })
                card.addView(head)
                card.addView(Ui.text(a, n.text, 14.5f, Ui.TEXT))
                col.addView(card)
            }
        }

        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    /** One small dialog to add a question for the physio. */
    private fun addQuestion(a: MainActivity) {
        val input = Forms.editText(a, "", "e.g. When can I stop sleeping in the boot?")
        val pad = Ui.dp(a, 18)
        val holder = android.widget.FrameLayout(a).apply { setPadding(pad, Ui.dp(a, 6), pad, 0); addView(input) }
        android.app.AlertDialog.Builder(a)
            .setTitle("Add a question")
            .setView(holder)
            .setPositiveButton("Add") { _, _ ->
                val q = input.text.toString().trim()
                if (q.isNotBlank()) {
                    a.store.savePhysioQuestions(a.store.physioQuestions() + q)
                    a.refresh()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun bullet(a: MainActivity, text: String): View {
        val row = Ui.row(a)
        row.gravity = Gravity.TOP
        row.setPadding(0, Ui.dp(a, 3), 0, Ui.dp(a, 3))
        val dot = Ui.text(a, "•", 15f, Ui.PRIMARY, bold = true)
        dot.setPadding(0, 0, Ui.dp(a, 8), 0)
        row.addView(dot)
        row.addView(Ui.weight(Ui.text(a, text, 14f, Ui.TEXT), 1f))
        return row
    }

    private fun completeAppointment(a: MainActivity, appt: Appointment) {
        a.store.saveProfile(a.store.profile().copy(
            appointments = a.store.profile().appointments.map {
                if (it.matches(appt)) it.copy(completed = true) else it
            }))
        a.refresh()
    }

    private fun deleteAppointment(a: MainActivity, appt: Appointment) {
        a.store.saveProfile(a.store.profile().copy(
            appointments = a.store.profile().appointments.filterNot { it.matches(appt) }))
    }

    private fun updateAppointment(a: MainActivity, original: Appointment, updated: Appointment) {
        a.store.saveProfile(a.store.profile().copy(
            appointments = a.store.profile().appointments.map { if (it.matches(original)) updated else it }))
    }

    /** One form for a new appointment ([appt] null) or changing one, with Remove for existing ones. */
    private fun appointmentEditor(a: MainActivity, appt: Appointment?): View {
        val title = if (appt == null) "New appointment" else "Edit appointment"
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, title) { a.popOverlay() })
        val card = Ui.card(a)
        var date = appt?.date ?: LocalDate.now().plusWeeks(2)
        card.addView(Forms.dateRow(a, "Date", date) { date = it })
        card.addView(Forms.label(a, "What it's for"))
        val labelEdit = Forms.editText(a, appt?.label ?: "", "e.g. Physio review")
        card.addView(labelEdit)
        card.addView(Forms.label(a, "Who it's with · optional"))
        val withEdit = Forms.editText(a, appt?.withWhom ?: "", "e.g. Mr Patel (consultant)")
        card.addView(withEdit)
        col.addView(card)
        col.addView(Ui.fullWidth(Ui.button(a, if (appt == null) "Save appointment" else "Save changes") {
            val label = labelEdit.text.toString().trim()
            val who = withEdit.text.toString().trim()
            if (appt == null) {
                a.store.saveProfile(a.store.profile().copy(appointments = a.store.profile().appointments +
                    Appointment(date, label.ifBlank { "Physio review" }, false, UUID.randomUUID().toString(), who)))
            } else {
                updateAppointment(a, appt, appt.copy(date = date, label = label.ifBlank { appt.label }, withWhom = who))
            }
            a.popOverlay()
        }, a))
        if (appt != null) {
            col.addView(Ui.fullWidth(Ui.textButton(a, "Remove appointment", Ui.WARN) {
                Forms.confirm(a, "Remove appointment?", "\"${appt.label}\" on ${Forms.friendlyDate(appt.date)} will be deleted.") {
                    deleteAppointment(a, appt); a.popOverlay()
                }
            }, a, 4))
        }
        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    /** Prefer the stable id; fall back to date+label for legacy entries without one. */
    private fun Appointment.matches(other: Appointment): Boolean =
        if (id.isNotBlank() || other.id.isNotBlank()) id == other.id
        else date == other.date && label == other.label

    private fun captureNote(a: MainActivity): View {
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Visit note") { a.popOverlay() })
        col.addView(Ui.caption(a, "Jot down what your physio said - progressions, cautions, next steps. " +
            "Saved into your backup so it's never lost."))
        col.addView(Ui.spacer(a, 4))
        val card = Ui.card(a)
        var date = LocalDate.now()
        card.addView(Forms.dateRow(a, "Date", date) { date = it })
        card.addView(Forms.label(a, "Note"))
        val text = Forms.editText(a, "", "e.g. Cleared to start jogging; recheck calf strength in 3 weeks", multiline = true)
        card.addView(text)
        // ROM is measured at the visit, not daily - capture it here against this date
        card.addView(Forms.label(a, "Range of movement measured · optional"))
        val romEdit = Forms.editText(a, a.store.dailyLog(date).romNote ?: "", "e.g. plantarflexion 30°")
        card.addView(romEdit)
        col.addView(card)
        col.addView(Ui.fullWidth(Ui.button(a, "Save note") {
            val t = text.text.toString().trim()
            val rom = romEdit.text.toString().trim()
            if (t.isBlank() && rom.isBlank()) {
                Forms.info(a, "Nothing to save", "Add a note or a range-of-movement measurement first.")
                return@button
            }
            if (t.isNotBlank()) a.store.addPhysioNote(PhysioNote(UUID.randomUUID().toString(), date, t))
            if (rom.isNotBlank()) {
                a.store.saveDailyLog(a.store.dailyLog(date).copy(romNote = rom))
            }
            Toast.makeText(a, "Visit saved", Toast.LENGTH_SHORT).show()
            a.popOverlay()
        }, a))
        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    private fun packText(pack: PhysioPrep.Pack, questions: List<String>): String = buildString {
        appendLine("RecoverWell - for my physio appointment")
        appendLine()
        appendLine("Worth raising:")
        pack.discussionPoints.forEach { appendLine("- $it") }
        if (pack.stageQuestions.isNotEmpty()) {
            appendLine()
            appendLine("Worth asking at this stage:")
            pack.stageQuestions.forEach { appendLine("- $it") }
        }
        if (questions.isNotEmpty()) {
            appendLine()
            appendLine("My questions:")
            questions.forEach { appendLine("- $it") }
        }
        appendLine()
        appendLine("Current numbers:")
        pack.summaryLines.forEach { appendLine("- $it") }
    }

    private fun copyToClipboard(ctx: Context, text: String) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("RecoverWell physio pack", text))
    }
}
