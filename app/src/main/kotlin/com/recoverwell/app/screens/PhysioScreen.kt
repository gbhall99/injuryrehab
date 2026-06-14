package com.recoverwell.app.screens

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.recoverwell.app.MainActivity
import com.recoverwell.app.notify.Reminders
import com.recoverwell.app.ui.Forms
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.logic.PhaseEngine
import com.recoverwell.core.logic.PhysioPrep
import com.recoverwell.core.logic.ReturnToSport
import com.recoverwell.core.model.Appointment
import com.recoverwell.core.model.PhysioNote
import com.recoverwell.core.protocol.ProtocolRegistry
import java.time.LocalDate
import java.util.UUID

/**
 * The physio loop: an auto-generated "bring this to your appointment" pack, plus
 * a post-visit capture that writes straight back into the plan (phase
 * confirmations, return-to-sport sign-offs, boot/date edits) and a durable note.
 */
object PhysioScreen {

    fun build(a: MainActivity): View {
        val today = LocalDate.now()
        val profile = a.store.profile()
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Physio visits") { a.popOverlay() })
        col.addView(Ui.caption(a, "Your bridge to your physio: walk in with the right questions, " +
            "walk out and capture what they said so your plan stays in sync."))

        // ---- appointments ---------------------------------------------------
        col.addView(Ui.section(a, "Appointments"))
        val appts = profile.appointments.sortedBy { it.date }
        val upcoming = appts.filter { !it.completed && !it.date.isBefore(today) }.minByOrNull { it.date }
        val overdue = appts.filter { !it.completed && it.date.isBefore(today) }
        val apptCard = Ui.card(a)
        if (upcoming != null) {
            apptCard.addView(Ui.text(a, "Next: ${upcoming.label}", 15.5f, Ui.TEXT, bold = true))
            apptCard.addView(Ui.caption(a, "${upcoming.date} · in ${java.time.temporal.ChronoUnit.DAYS.between(today, upcoming.date)} days"))
            apptCard.addView(Ui.fullWidth(Ui.tonalButton(a, "Mark done & capture") {
                completeAppointment(a, upcoming); a.pushOverlay("Visit note") { captureNote(a) }
            }, a))
        } else {
            apptCard.addView(Ui.text(a, "No upcoming appointment scheduled", 14.5f, Ui.TEXT))
        }
        for (o in overdue) {
            apptCard.addView(Ui.divider(a))
            apptCard.addView(Ui.text(a, "Was: ${o.label} · ${o.date}", 14f, Ui.WARN, bold = true))
            apptCard.addView(Ui.fullWidth(Ui.tonalButton(a, "How did it go? Capture it") {
                completeAppointment(a, o); a.pushOverlay("Visit note") { captureNote(a) }
            }, a))
        }
        // quick add
        var newDate = today.plusWeeks(2)
        apptCard.addView(Ui.divider(a))
        apptCard.addView(Forms.dateRow(a, "New appointment", newDate) { newDate = it })
        val newLabel = Forms.editText(a, "", "e.g. Physio review")
        apptCard.addView(newLabel)
        apptCard.addView(Ui.fullWidth(Ui.textButton(a, "Add appointment") {
            val label = newLabel.text.toString().ifBlank { "Physio review" }
            a.store.saveProfile(a.store.profile().copy(
                appointments = a.store.profile().appointments + Appointment(newDate, label, false)))
            a.refresh()
        }, a, 4))
        col.addView(apptCard)

        // ---- bring-to-appointment pack -------------------------------------
        val pack = PhysioPrep.build(
            profile, a.store.allLogs(), a.store.allEvents(), a.store.medications(), a.store.tasks(),
            a.store.selfTestResults(), a.store.rtsSignoffs(), today
        )
        col.addView(Ui.section(a, "Bring to your appointment"))
        val packCard = Ui.card(a)
        packCard.addView(Ui.text(a, "Worth raising", 13.5f, Ui.TEXT_DIM, bold = true))
        for (p in pack.discussionPoints) {
            packCard.addView(bullet(a, p))
        }
        // user's own questions
        val questions = a.store.physioQuestions()
        if (questions.isNotEmpty()) {
            packCard.addView(Ui.spacer(a, 6))
            packCard.addView(Ui.text(a, "Your questions", 13.5f, Ui.TEXT_DIM, bold = true))
            questions.forEachIndexed { i, q ->
                val row = Ui.row(a)
                row.gravity = Gravity.CENTER_VERTICAL
                row.addView(Ui.icon(a, "ic_pulse", 16, Ui.PRIMARY))
                val tv = Ui.text(a, q, 14f, Ui.TEXT)
                tv.setPadding(Ui.dp(a, 8), Ui.dp(a, 3), Ui.dp(a, 8), Ui.dp(a, 3))
                row.addView(Ui.weight(tv, 1f))
                row.addView(Ui.iconButton(a, "ic_close", Ui.TEXT_DIM, desc = "Remove question") {
                    a.store.savePhysioQuestions(a.store.physioQuestions().filterIndexed { j, _ -> j != i })
                    a.refresh()
                })
                packCard.addView(row)
            }
        }
        val addQ = Forms.editText(a, "", "Add your own question")
        packCard.addView(Ui.spacer(a, 6))
        packCard.addView(addQ)
        packCard.addView(Ui.fullWidth(Ui.textButton(a, "Add question") {
            val q = addQ.text.toString().trim()
            if (q.isNotBlank()) {
                a.store.savePhysioQuestions(a.store.physioQuestions() + q)
                a.refresh()
            }
        }, a, 4))
        col.addView(packCard)

        val row = Ui.row(a)
        row.addView(Ui.weight(Ui.tonalButton(a, "Copy pack") {
            copyToClipboard(a, packText(pack, questions))
            Toast.makeText(a, "Copied - paste into notes or a message", Toast.LENGTH_SHORT).show()
        }, 1f))
        val pdfBtn = Ui.tonalButton(a, "Export PDF") { a.exportPdf() }
        val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        lp.setMargins(Ui.dp(a, 8), Ui.dp(a, 10), 0, 0)
        pdfBtn.layoutParams = lp
        row.addView(pdfBtn)
        col.addView(row)

        // ---- current numbers ------------------------------------------------
        col.addView(Ui.section(a, "Your current numbers"))
        val numCard = Ui.card(a)
        pack.summaryLines.forEachIndexed { i, s ->
            if (i > 0) numCard.addView(Ui.spacer(a, 5))
            numCard.addView(Ui.text(a, s, 14f, Ui.TEXT))
        }
        col.addView(numCard)

        // ---- after-visit capture -------------------------------------------
        col.addView(Ui.section(a, "After your visit"))
        col.addView(Ui.caption(a, "Record what your physio decided - it updates your plan directly."))
        col.addView(Ui.spacer(a, 4))

        val gate = PhaseEngine.nextPhaseGate(profile, today)
        if (gate.nextPhase != null) {
            col.addView(Ui.listRow(a, "ic_calendar", "Confirm phase ${gate.nextPhase!!.number} progression",
                "${gate.nextPhase!!.title}") {
                Forms.confirm(a, "Confirm progression",
                    "Record that your physio confirmed phase ${gate.nextPhase!!.number} (${gate.nextPhase!!.title})?") {
                    val pp = a.store.profile()
                    a.store.saveProfile(pp.copy(
                        physioConfirmedPhase = gate.nextPhase!!.number,
                        phaseConfirmedDates = pp.phaseConfirmedDates + (gate.nextPhase!!.number to today)))
                    Reminders.reschedule(a)
                    a.refresh()
                }
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
        col.addView(Ui.listRow(a, "ic_calendar", "Adjust phase dates",
            "If your physio re-timed a phase") { a.pushOverlay("Phase dates") { MoreScreen.phaseDatesEditor(a) } })
        col.addView(Ui.listRow(a, "ic_boot", "Adjust boot / injury plan",
            "Boot angle, schedule, weight-bearing") { a.pushOverlay("Injury & goal") { MoreScreen.profileEditor(a) } })
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
                if (it.date == appt.date && it.label == appt.label) it.copy(completed = true) else it
            }))
    }

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
        col.addView(card)
        col.addView(Ui.fullWidth(Ui.button(a, "Save note") {
            val t = text.text.toString().trim()
            if (t.isBlank()) { Forms.info(a, "Empty note", "Type what your physio said first."); return@button }
            a.store.addPhysioNote(PhysioNote(UUID.randomUUID().toString(), date, t))
            Toast.makeText(a, "Visit note saved", Toast.LENGTH_SHORT).show()
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
