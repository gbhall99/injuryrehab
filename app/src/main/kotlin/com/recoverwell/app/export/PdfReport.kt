package com.recoverwell.app.export

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.recoverwell.app.data.Store
import com.recoverwell.core.logic.MilestoneTimeline
import com.recoverwell.core.logic.PhaseEngine
import com.recoverwell.core.logic.TrendMath
import com.recoverwell.core.model.EventStatus
import com.recoverwell.core.model.EventType
import com.recoverwell.core.protocol.ProtocolContent
import java.io.ByteArrayOutputStream
import java.time.LocalDate

/**
 * Recovery summary as a PDF, built with the platform PdfDocument API
 * (no third-party libraries, fully offline). A4 portrait at 72dpi.
 */
object PdfReport {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 40f

    fun build(store: Store): ByteArray {
        val doc = PdfDocument()
        val profile = store.profile()
        val logs = store.allLogs()
        val events = store.allEvents()
        val today = LocalDate.now()

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 22f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = 0xFF1B5E20.toInt()
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f }
        val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; color = 0xFF555555.toInt() }

        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
        var canvas = page.canvas
        var y = MARGIN + 10f

        fun newPageIfNeeded(needed: Float) {
            if (y + needed > PAGE_H - MARGIN) {
                doc.finishPage(page)
                pageNum += 1
                page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
                canvas = page.canvas
                y = MARGIN
            }
        }

        fun line(text: String, paint: Paint, gap: Float = 6f) {
            newPageIfNeeded(paint.textSize + gap)
            canvas.drawText(text, MARGIN, y, paint)
            y += paint.textSize + gap
        }

        fun section(title: String) {
            y += 10f
            line(title, headPaint, 8f)
        }

        line("RecoverWell - Achilles Rehab Report", titlePaint, 10f)
        line("Generated $today · ${ProtocolContent.PROTOCOL_NAME}", dimPaint, 4f)
        line(ProtocolContent.DISCLAIMER, dimPaint, 4f)

        section("Profile")
        line("Injury: ${profile.injuryDescription}", bodyPaint)
        line("Injury date: ${profile.injuryDate} (${PhaseEngine.weeksSinceInjury(profile, today)} weeks ago) · Side: ${profile.side}", bodyPaint)
        line("Pathway: conservative / non-surgical · Goal: ${profile.goal}", bodyPaint)
        val phase = PhaseEngine.currentPhase(profile, today)
        line("Current phase: ${phase.number} - ${phase.title}", bodyPaint)
        line("Boot wedges: ${profile.currentWedges} (plan expects ${profile.wedgePlan.expectedWedges(profile.injuryDate, today)}) · ${profile.weightBearing.label}", bodyPaint)
        for (a in profile.appointments) {
            line("Appointment: ${a.date} ${a.label}${if (a.completed) " (completed)" else ""}", bodyPaint)
        }

        section("Medication adherence")
        val medEvents = events.filter { it.type == EventType.MEDICATION }
        val taken = medEvents.count { it.status == EventStatus.TAKEN }
        val missed = medEvents.count { it.status == EventStatus.MISSED }
        line("Doses recorded: ${medEvents.size} · taken: $taken · missed: $missed", bodyPaint)
        for (m in store.medications().filter { it.active }) {
            line("${m.name} ${m.dose} at ${m.times.joinToString(", ")}", bodyPaint)
        }

        section("Milestones (typical conservative protocol - physio-confirmable)")
        for (e in MilestoneTimeline.build(profile, today)) {
            val mark = when (e.status) {
                MilestoneTimeline.Status.REACHED -> "[x]"
                MilestoneTimeline.Status.DUE_NOW -> "[>]"
                MilestoneTimeline.Status.UPCOMING -> "[ ]"
            }
            line("$mark Week ${e.milestone.week} (${e.expectedDate}): ${e.milestone.title}", bodyPaint, 4f)
        }

        section("Trends")
        val pain = TrendMath.pain(logs)
        if (pain.points.isNotEmpty()) {
            val avg = pain.points.map { it.value }.average()
            line("Pain: ${pain.points.size} entries, latest ${pain.points.last().value.toInt()}/10, average %.1f".format(avg), bodyPaint)
        } else line("Pain: no entries yet", bodyPaint)
        val swelling = TrendMath.swelling(logs)
        if (swelling.points.isNotEmpty()) {
            line("Swelling: latest ${swelling.points.last().value.toInt()}/3 (0 none - 3 severe)", bodyPaint)
        }

        section("Daily log (most recent 21 days)")
        line("date · pain · swelling · boot · wedges · weight-bearing · mood · energy", dimPaint, 4f)
        for (l in logs.sortedByDescending { it.date }.take(21)) {
            line(
                "${l.date} · ${l.pain ?: "-"} · ${l.swelling?.name?.lowercase() ?: "-"} · " +
                    "${when (l.bootWornAsPlanned) { null -> "-"; true -> "worn"; false -> "NOT worn" }} · " +
                    "${l.wedges ?: "-"} · ${l.weightBearing?.name?.lowercase() ?: "-"} · ${l.mood ?: "-"} · ${l.energy ?: "-"}",
                bodyPaint, 4f
            )
            if (!l.notes.isNullOrBlank()) line("   note: ${l.notes!!.take(90)}", dimPaint, 3f)
        }

        doc.finishPage(page)
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }
}
