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
import com.recoverwell.core.protocol.ProtocolRegistry
import com.recoverwell.core.protocol.RehabFramework
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.Locale

/**
 * Recovery summary as a PDF, built with the platform PdfDocument API
 * (no third-party libraries, fully offline). Content assembly is separated
 * from native rendering so report content is unit-testable on the JVM.
 */
object PdfReport {

    enum class Style(val size: Float, val bold: Boolean, val color: Int) {
        TITLE(22f, true, 0xFF000000.toInt()),
        HEAD(14f, true, 0xFF1B5E20.toInt()),
        BODY(11f, false, 0xFF000000.toInt()),
        DIM(10f, false, 0xFF555555.toInt())
    }

    /** The whole report as styled lines; pure data, fully testable. */
    fun composeLines(store: Store): List<Pair<Style, String>> {
        val profile = store.profile()
        val logs = store.allLogs()
        val events = store.allEvents()
        val today = LocalDate.now()
        val out = ArrayList<Pair<Style, String>>()
        fun line(style: Style, text: String) = out.add(style to text)

        val protocol = ProtocolRegistry.forProfile(profile)
        line(Style.TITLE, "RecoverWell - Recovery Report")
        line(Style.DIM, "Generated $today · ${protocol.injuryName} · ${protocol.variantName}")
        line(Style.DIM, "Protocol basis: ${protocol.protocolName}")
        line(Style.DIM, RehabFramework.DISCLAIMER)

        line(Style.HEAD, "Profile")
        line(Style.BODY, "Injury: ${profile.injuryDescription}")
        line(Style.BODY, "Injury date: ${profile.injuryDate} (${PhaseEngine.weeksSinceInjury(profile, today)} weeks ago) · Side: ${profile.side}")
        line(Style.BODY, "Pathway: ${protocol.variantName} · Goal: ${profile.goal}")
        val phase = PhaseEngine.currentPhase(profile, today)
        line(Style.BODY, "Current phase: ${phase.number} - ${phase.title}")
        line(Style.BODY, "Boot wedges: ${profile.currentWedges} (plan expects ${profile.wedgePlan.expectedWedges(profile.injuryDate, today)}) · ${profile.weightBearing.label}")
        for (a in profile.appointments) {
            line(Style.BODY, "Appointment: ${a.date} ${a.label}${if (a.completed) " (completed)" else ""}")
        }

        line(Style.HEAD, "Medication adherence")
        val medEvents = events.filter { it.type == EventType.MEDICATION }
        val taken = medEvents.count { it.status == EventStatus.TAKEN }
        val missed = medEvents.count { it.status == EventStatus.MISSED }
        line(Style.BODY, "Doses recorded: ${medEvents.size} · taken: $taken · missed: $missed")
        for (m in store.medications().filter { it.active }) {
            line(Style.BODY, "${m.name} ${m.dose} at ${m.times.joinToString(", ")}")
        }

        line(Style.HEAD, "Milestones (typical conservative protocol - physio-confirmable)")
        for (e in MilestoneTimeline.build(profile, today)) {
            val mark = when (e.status) {
                MilestoneTimeline.Status.REACHED -> "[x]"
                MilestoneTimeline.Status.DUE_NOW -> "[>]"
                MilestoneTimeline.Status.UPCOMING -> "[ ]"
            }
            line(Style.BODY, "$mark Week ${e.milestone.week} (${e.expectedDate}): ${e.milestone.title}")
        }

        line(Style.HEAD, "Trends")
        val pain = TrendMath.pain(logs)
        if (pain.points.isNotEmpty()) {
            val avg = pain.points.map { it.value }.average()
            line(Style.BODY, "Pain: ${pain.points.size} entries, latest ${pain.points.last().value.toInt()}/10, average " +
                String.format(Locale.ROOT, "%.1f", avg))
        } else line(Style.BODY, "Pain: no entries yet")
        val swelling = TrendMath.swelling(logs)
        if (swelling.points.isNotEmpty()) {
            line(Style.BODY, "Swelling: latest ${swelling.points.last().value.toInt()}/3 (0 none - 3 severe)")
        }

        line(Style.HEAD, "Daily log (most recent 21 days)")
        line(Style.DIM, "date · pain · swelling · boot · wedges · weight-bearing · mood · energy")
        for (l in logs.sortedByDescending { it.date }.take(21)) {
            line(
                Style.BODY,
                "${l.date} · ${l.pain ?: "-"} · ${l.swelling?.name?.lowercase() ?: "-"} · " +
                    "${when (l.bootWornAsPlanned) { null -> "-"; true -> "worn"; false -> "NOT worn" }} · " +
                    "${l.wedges ?: "-"} · ${l.weightBearing?.name?.lowercase() ?: "-"} · ${l.mood ?: "-"} · ${l.energy ?: "-"}"
            )
            if (!l.notes.isNullOrBlank()) line(Style.DIM, "   note: ${l.notes!!.take(90)}")
        }
        return out
    }

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 40f

    /** Thin native-rendering layer over [composeLines]; exercised on-device. */
    fun build(store: Store): ByteArray {
        val doc = PdfDocument()
        val paints = Style.values().associateWith { s ->
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = s.size
                color = s.color
                if (s.bold) typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
        }
        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
        var y = MARGIN + 10f
        for ((style, text) in composeLines(store)) {
            val paint = paints.getValue(style)
            val gap = if (style == Style.HEAD) 8f else 5f
            if (style == Style.HEAD) y += 10f
            if (y + paint.textSize + gap > PAGE_H - MARGIN) {
                doc.finishPage(page)
                pageNum += 1
                page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
                y = MARGIN
            }
            page.canvas.drawText(text, MARGIN, y, paint)
            y += paint.textSize + gap
        }
        doc.finishPage(page)
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }
}
