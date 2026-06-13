package com.recoverwell.app.screens

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import com.recoverwell.app.MainActivity
import com.recoverwell.app.ui.Forms
import com.recoverwell.app.ui.SceneView
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.logic.MilestoneTimeline
import com.recoverwell.core.logic.TrendMath
import com.recoverwell.core.model.Swelling
import com.recoverwell.core.model.WeightBearing
import com.recoverwell.core.protocol.ProtocolRegistry
import com.recoverwell.draw.ChartScene
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Daily log entry, trend charts, milestone timeline and export. */
object TrackerScreen {

    private var chartMetric = "Pain"
    private var selectedDate: LocalDate? = null

    fun build(a: MainActivity): View {
        val today = LocalDate.now()
        val day = (selectedDate ?: today).coerceAtMost(today)
        val col = Ui.column(a)

        // ---- daily log: any day is editable (review-mined: backfill matters) ----
        col.addView(Ui.section(a, if (day == today) "Today's log" else "Log · earlier day"))
        val nav = Ui.row(a)
        nav.addView(Ui.iconButton(a, "ic_back", Ui.TEXT, Ui.SURFACE_HIGH, desc = "Previous day") {
            selectedDate = day.minusDays(1); a.refresh()
        })
        val dayLabel = Ui.tonalButton(a, if (day == today) "Today" else day.toString()) {
            android.app.DatePickerDialog(a, { _, y, m, d ->
                selectedDate = LocalDate.of(y, m + 1, d).coerceAtMost(today)
                a.refresh()
            }, day.year, day.monthValue - 1, day.dayOfMonth).apply {
                datePicker.maxDate = System.currentTimeMillis()
            }.show()
        }
        val lp = android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        lp.setMargins(Ui.dp(a, 8), 0, Ui.dp(a, 8), 0)
        dayLabel.layoutParams = lp
        nav.addView(dayLabel)
        val nextBtn = Ui.iconButton(a, "ic_chevron", if (day == today) Ui.OUTLINE else Ui.TEXT,
            Ui.SURFACE_HIGH, desc = "Next day") {
            if (day < today) { selectedDate = day.plusDays(1); a.refresh() }
        }
        nav.addView(nextBtn)
        col.addView(nav)
        col.addView(Ui.spacer(a, 6))
        var log = a.store.dailyLog(day)
        val form = Ui.card(a)

        form.addView(Forms.label(a, "Pain right now"))
        form.addView(Forms.scaleSlider(a, 10, log.pain, "0 · None", "10 · Worst") { log = log.copy(pain = it) })

        form.addView(Forms.label(a, "Swelling"))
        form.addView(Forms.choiceRow(a, Swelling.values().toList(), { it.label }, log.swelling) {
            log = log.copy(swelling = it)
        })

        form.addView(Forms.label(a, "Mood"))
        form.addView(Forms.choiceRow(a, (1..5).toList(), { "$it" }, log.mood) { log = log.copy(mood = it) })

        form.addView(Forms.label(a, "Energy"))
        form.addView(Forms.choiceRow(a, (1..5).toList(), { "$it" }, log.energy) { log = log.copy(energy = it) })

        val device = ProtocolRegistry.forProfile(a.store.profile()).supportDevice
        if (device != null) {
            form.addView(Forms.label(a, "${device.name} worn as planned?"))
            form.addView(Forms.choiceRow(a, listOf(true, false), { if (it) "Yes" else "No" }, log.bootWornAsPlanned) {
                log = log.copy(bootWornAsPlanned = it)
            })
            form.addView(Forms.stepper(a,
                "${device.unitNamePlural.replaceFirstChar { it.uppercase() }} in ${device.name.lowercase()}",
                log.wedges ?: a.store.profile().currentWedges, 0, 8) {
                log = log.copy(wedges = it)
            })
        }

        form.addView(Forms.label(a, "Weight-bearing"))
        form.addView(Forms.choiceRow(
            a, WeightBearing.values().toList(), { it.shortLabel }, log.weightBearing
        ) { log = log.copy(weightBearing = it) })

        form.addView(Forms.label(a, "Range of movement · only if your physio measured it"))
        val romEdit = Forms.editText(a, log.romNote ?: "", "e.g. plantarflexion 30°")
        form.addView(romEdit)

        form.addView(Forms.label(a, "Notes"))
        val notesEdit = Forms.editText(a, log.notes ?: "", "Anything worth remembering", multiline = true)
        form.addView(notesEdit)

        form.addView(Ui.fullWidth(Ui.button(a, if (day == today) "Save today's log" else "Save log for $day") {
            val wedges = log.wedges
            a.store.saveDailyLog(log.copy(
                romNote = romEdit.text.toString().ifBlank { null },
                notes = notesEdit.text.toString().ifBlank { null }
            ))
            // the boot state only follows logs about today
            if (day == today && wedges != null && wedges != a.store.profile().currentWedges) {
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
        col.addView(Ui.spacer(a, 8))
        val series = when (chartMetric) {
            "Swelling" -> TrendMath.swelling(logs)
            "Mood" -> TrendMath.mood(logs)
            "Energy" -> TrendMath.energy(logs)
            else -> TrendMath.pain(logs)
        }
        val fmt = DateTimeFormatter.ofPattern("d MMM")
        val first = series.points.firstOrNull()?.date
        val pts = series.points.map { (it.date.toEpochDay() - (first?.toEpochDay() ?: 0)).toFloat() to it.value.toFloat() }
        val avg = TrendMath.movingAverage(series.points, 7)
            .map { (it.date.toEpochDay() - (first?.toEpochDay() ?: 0)).toFloat() to it.value.toFloat() }
        val chartCard = Ui.frame(a)
        chartCard.background = Ui.rounded(Ui.CARD)
        Ui.elevate(chartCard, a)
        chartCard.clipToOutline = true
        val chart = SceneView(a) { s ->
            ChartScene.render(s, ChartScene.Data(
                pts, avg, series.min.toFloat(), series.max.toFloat(),
                first?.format(fmt) ?: "", series.points.lastOrNull()?.date?.format(fmt) ?: "",
                "No entries yet - save today's log above"
            ))
        }
        chartCard.addView(chart, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(a, 210))
        col.addView(chartCard, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        col.addView(Ui.spacer(a, 6))
        col.addView(Ui.caption(a, "Solid line: daily entries · dashed: 7-entry average"))

        // ---- milestone timeline ----
        col.addView(Ui.section(a, "Milestones"))
        col.addView(Ui.pillBadge(a, ProtocolRegistry.forProfile(a.store.profile()).placeholderNote, Ui.WARN, Ui.WARN_BG))
        col.addView(Ui.spacer(a, 8))
        val profile = a.store.profile()
        val timeline = Ui.card(a)
        val entries = MilestoneTimeline.build(profile, today)
        entries.forEachIndexed { i, e ->
            val row = Ui.row(a)
            row.gravity = android.view.Gravity.TOP
            // timeline gutter: dot + connector
            val gutter = LinearLayout(a).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(Ui.dp(a, 28), ViewGroup.LayoutParams.MATCH_PARENT)
            }
            val dot = View(a).apply {
                val (size, color) = when (e.status) {
                    MilestoneTimeline.Status.REACHED -> 12 to Ui.PRIMARY
                    MilestoneTimeline.Status.DUE_NOW -> 16 to Ui.WARN
                    MilestoneTimeline.Status.UPCOMING -> 12 to Ui.OUTLINE
                }
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(color)
                }
                layoutParams = LinearLayout.LayoutParams(Ui.dp(a, size), Ui.dp(a, size)).apply {
                    topMargin = Ui.dp(a, 4)
                }
            }
            gutter.addView(dot)
            if (i < entries.size - 1) {
                gutter.addView(View(a).apply {
                    setBackgroundColor(Ui.OUTLINE)
                    layoutParams = LinearLayout.LayoutParams(Ui.dp(a, 2), Ui.dp(a, 40))
                })
            }
            row.addView(gutter)
            val texts = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
            texts.setPadding(Ui.dp(a, 8), 0, 0, Ui.dp(a, 10))
            val titleColor = if (e.status == MilestoneTimeline.Status.UPCOMING) Ui.TEXT_DIM else Ui.TEXT
            texts.addView(Ui.text(a, "Week ${e.milestone.week} · ${e.milestone.title}", 15f, titleColor, bold = true))
            texts.addView(Ui.caption(a, "${e.expectedDate.format(fmt)} · ${e.milestone.detail}"))
            row.addView(Ui.weight(texts, 1f))
            timeline.addView(row)
        }
        col.addView(timeline)

        // ---- export ----
        col.addView(Ui.section(a, "Export & backup"))
        col.addView(Ui.caption(a, "Everything stays on this phone unless you export it."))
        col.addView(Ui.spacer(a, 6))
        col.addView(Ui.listRow(a, "ic_export", "PDF report", "Share progress with your physio") { a.exportPdf() })
        col.addView(Ui.listRow(a, "ic_export", "Daily logs · CSV", "Spreadsheet-friendly") { a.exportLogsCsv() })
        col.addView(Ui.listRow(a, "ic_export", "Medication & task log · CSV", "Adherence history") { a.exportEventsCsv() })
        col.addView(Ui.listRow(a, "ic_export", "Full backup · JSON", "Everything, restorable") { a.exportBackup() })
        col.addView(Ui.listRow(a, "ic_restore", "Restore from backup", "Replaces all current data") { a.importBackup() })

        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }
}
