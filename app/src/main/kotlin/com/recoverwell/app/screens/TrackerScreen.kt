package com.recoverwell.app.screens

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import com.recoverwell.app.MainActivity
import com.recoverwell.app.ui.ChartView
import com.recoverwell.app.ui.Forms
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.logic.MilestoneTimeline
import com.recoverwell.core.logic.TrendMath
import com.recoverwell.core.model.Swelling
import com.recoverwell.core.model.WeightBearing
import com.recoverwell.core.protocol.ProtocolContent
import java.time.LocalDate

/** Daily log entry, trend charts, milestone timeline and export. */
object TrackerScreen {

    private var chartMetric = "Pain"

    fun build(a: MainActivity): View {
        val today = LocalDate.now()
        val col = Ui.column(a)
        col.addView(Ui.title(a, "Recovery tracker"))

        // ---- today's log form ----
        col.addView(Ui.section(a, "Today's log · $today"))
        var log = a.store.dailyLog(today)
        val form = Ui.card(a)

        form.addView(Forms.label(a, "Pain right now (0 none - 10 worst)"))
        form.addView(Forms.scaleSlider(a, 10, log.pain) { log = log.copy(pain = it) })

        form.addView(Forms.label(a, "Swelling"))
        form.addView(Forms.choiceRow(a, Swelling.values().toList(), { it.label }, log.swelling) {
            log = log.copy(swelling = it)
        })

        form.addView(Forms.label(a, "Mood (1-5)"))
        form.addView(Forms.choiceRow(a, (1..5).toList(), { "$it" }, log.mood) { log = log.copy(mood = it) })

        form.addView(Forms.label(a, "Energy (1-5)"))
        form.addView(Forms.choiceRow(a, (1..5).toList(), { "$it" }, log.energy) { log = log.copy(energy = it) })

        form.addView(Forms.label(a, "Boot worn as planned today?"))
        form.addView(Forms.choiceRow(a, listOf(true, false), { if (it) "Yes" else "No" }, log.bootWornAsPlanned) {
            log = log.copy(bootWornAsPlanned = it)
        })

        form.addView(Forms.stepper(a, "Wedges in boot", log.wedges ?: a.store.profile().currentWedges, 0, 8) {
            log = log.copy(wedges = it)
        })

        form.addView(Forms.label(a, "Weight-bearing status"))
        form.addView(Forms.choiceRow(
            a, WeightBearing.values().toList(),
            { it.label.replace("weight-bearing", "WB").replace("Weight-bearing", "WB") },
            log.weightBearing
        ) { log = log.copy(weightBearing = it) })

        form.addView(Forms.label(a, "Range of movement (only if your physio measured it)"))
        val romEdit = Forms.editText(a, log.romNote ?: "", "e.g. plantarflexion 30°, dorsiflexion -10°")
        form.addView(romEdit)

        form.addView(Forms.label(a, "Notes"))
        val notesEdit = Forms.editText(a, log.notes ?: "", "Anything worth remembering about today", multiline = true)
        form.addView(notesEdit)

        form.addView(Ui.fullWidth(Ui.button(a, "Save today's log") {
            val wedges = log.wedges
            a.store.saveDailyLog(log.copy(
                romNote = romEdit.text.toString().ifBlank { null },
                notes = notesEdit.text.toString().ifBlank { null }
            ))
            // keep the profile wedge count in sync when logged
            if (wedges != null && wedges != a.store.profile().currentWedges) {
                a.store.saveProfile(a.store.profile().copy(currentWedges = wedges))
            }
            Toast.makeText(a, "Log saved", Toast.LENGTH_SHORT).show()
            a.refresh()
        }, a))
        col.addView(form)

        // ---- trends ----
        col.addView(Ui.section(a, "Trends"))
        val logs = a.store.allLogs()
        col.addView(Forms.choiceRow(a, listOf("Pain", "Swelling", "Mood", "Energy"), { it }, chartMetric) {
            chartMetric = it
            a.refresh()
        })
        val chart = ChartView(a)
        chart.series = when (chartMetric) {
            "Swelling" -> TrendMath.swelling(logs)
            "Mood" -> TrendMath.mood(logs)
            "Energy" -> TrendMath.energy(logs)
            else -> TrendMath.pain(logs)
        }
        chart.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(a, 220))
        chart.background = Ui.roundedBg(Ui.CARD, strokeColor = Ui.BORDER)
        col.addView(chart)
        col.addView(Ui.text(a, "Solid line: daily entries · dashed: 7-entry average", 13f, Ui.TEXT_DIM))

        // ---- milestone timeline ----
        col.addView(Ui.section(a, "Milestones vs typical conservative protocol"))
        col.addView(Ui.text(a, ProtocolContent.PLACEHOLDER_NOTE, 13f, Ui.WARN))
        val profile = a.store.profile()
        for (e in MilestoneTimeline.build(profile, today)) {
            val (mark, color) = when (e.status) {
                MilestoneTimeline.Status.REACHED -> "✓" to Ui.DONE
                MilestoneTimeline.Status.DUE_NOW -> "➤" to Ui.WARN
                MilestoneTimeline.Status.UPCOMING -> "○" to Ui.TEXT_DIM
            }
            val card = Ui.card(a, if (e.status == MilestoneTimeline.Status.DUE_NOW) Ui.INFO_BG else Ui.CARD)
            val row = Ui.row(a)
            row.addView(Ui.text(a, "$mark  ", 20f, color, bold = true))
            val texts = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
            texts.addView(Ui.text(a, "Week ${e.milestone.week}: ${e.milestone.title}", 16f, Ui.TEXT, bold = true))
            texts.addView(Ui.text(a, "${e.expectedDate} · ${e.milestone.detail}", 14f, Ui.TEXT_DIM))
            row.addView(Ui.weight(texts, 1f))
            card.addView(row)
            col.addView(card)
        }

        // ---- export ----
        col.addView(Ui.section(a, "Export & backup"))
        val exportCard = Ui.card(a)
        exportCard.addView(Ui.text(
            a, "Everything stays on this phone unless you export it. Files are " +
                "saved wherever you choose - share them with your physio if useful.",
            14f, Ui.TEXT_DIM
        ))
        exportCard.addView(Ui.fullWidth(Ui.button(a, "Export PDF report") { a.exportPdf() }, a))
        exportCard.addView(Ui.fullWidth(Ui.secondaryButton(a, "Export daily logs (CSV)") { a.exportLogsCsv() }, a))
        exportCard.addView(Ui.fullWidth(Ui.secondaryButton(a, "Export medication & task log (CSV)") { a.exportEventsCsv() }, a))
        exportCard.addView(Ui.fullWidth(Ui.secondaryButton(a, "Full backup (JSON)") { a.exportBackup() }, a))
        exportCard.addView(Ui.fullWidth(Ui.secondaryButton(a, "Restore from backup") { a.importBackup() }, a))
        col.addView(exportCard)

        col.addView(Ui.spacer(a, 20))
        return Ui.scroll(a, col)
    }
}
