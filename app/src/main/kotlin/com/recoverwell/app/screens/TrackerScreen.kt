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

/** Progress is review-only: trends, pace, insights, milestones (+ backfill). */
object TrackerScreen {

    private var chartMetric = "Pain"

    fun build(a: MainActivity): View {
        val today = LocalDate.now()
        val col = Ui.column(a)

        // Progress reviews the trend first; the option to add/edit a past day sits
        // at the bottom (logging today happens on Today) so a review-only screen
        // leads with the weekly digest rather than an editing control.
        buildReview(a, today, col)

        col.addView(Ui.listRow(a, "ic_edit", "Add a check-in for a day you missed",
            "Or edit an earlier day") {
            android.app.DatePickerDialog(a, { _, y, m, d ->
                val date = LocalDate.of(y, m + 1, d).coerceAtMost(today)
                a.pushOverlay("Log for $date") { pastDayOverlay(a, date) }
            }, today.year, today.monthValue - 1, today.dayOfMonth).apply {
                datePicker.maxDate = System.currentTimeMillis()
            }.show()
        })

        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    /** Overlay: the shared check-in for a chosen past day. */
    private fun pastDayOverlay(a: MainActivity, date: LocalDate): View {
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Log for $date") { a.popOverlay() })
        col.addView(TodayScreen.checkInCard(a, date, expanded = true) {
            Toast.makeText(a, "Log saved", Toast.LENGTH_SHORT).show()
            a.popOverlay()
        })
        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    /** The review surfaces: trends, pace, return-to-sport, insights, milestones. */
    private fun buildReview(a: MainActivity, today: LocalDate, col: LinearLayout) {
        // ---- this week (weekly digest) ----
        val logs = a.store.allLogs()
        run {
            val digest = com.recoverwell.core.logic.WeeklyDigest.generate(
                a.store.profile(), logs, a.store.allEvents(),
                a.store.medications(), a.store.tasks(), today
            )
            col.addView(Ui.section(a, "This week"))
            val card = Ui.card(a)
            fun line(icon: String, text: String, tint: Int = Ui.PRIMARY) {
                val r = Ui.row(a)
                r.gravity = android.view.Gravity.TOP
                r.addView(Ui.icon(a, icon, 18, tint))
                val t = Ui.text(a, text, 14.5f, Ui.TEXT)
                t.setPadding(Ui.dp(a, 10), 0, 0, 0)
                r.addView(Ui.weight(t, 1f))
                card.addView(r)
                card.addView(Ui.spacer(a, 6))
            }
            line("ic_pill", "Medication: ${digest.adherencePct}% of doses taken")
            val painTint = when (digest.painTrend) {
                com.recoverwell.core.logic.WeeklyDigest.Trend.DOWN -> Ui.DONE
                com.recoverwell.core.logic.WeeklyDigest.Trend.UP -> Ui.WARN
                else -> Ui.PRIMARY
            }
            line("ic_pulse", digest.painDetail, painTint)
            line("ic_exercises", "${digest.exercisesDone} exercise session" +
                "${if (digest.exercisesDone == 1) "" else "s"} completed")
            if (digest.milestonesThisWeek.isNotEmpty()) {
                line("ic_flag", "Reached: ${digest.milestonesThisWeek.joinToString(", ")}", Ui.DONE)
            }
            val focusCard = Ui.card(a, Ui.INFO_BG)
            val fr = Ui.row(a)
            fr.gravity = android.view.Gravity.TOP
            fr.addView(Ui.icon(a, "ic_info", 18, Ui.ON_INFO_BG))
            val ft = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
            ft.setPadding(Ui.dp(a, 10), 0, 0, 0)
            ft.addView(Ui.text(a, "Focus for the week ahead", 13f, Ui.ON_INFO_BG, bold = true))
            ft.addView(Ui.text(a, digest.focus, 14.5f, Ui.ON_INFO_BG))
            fr.addView(Ui.weight(ft, 1f))
            focusCard.addView(fr)
            card.addView(focusCard)
            col.addView(card)
        }

        // ---- trends ----
        col.addView(Ui.section(a, "Trends"))
        val metrics = if (AiScreen.enabled(a) && a.store.journalEntries().isNotEmpty())
            listOf("Pain", "Swelling", "Mood", "Energy", "Journal")
        else listOf("Pain", "Swelling", "Mood", "Energy")
        col.addView(Forms.choiceRow(a, metrics, { it }, chartMetric) {
            chartMetric = it
            a.refresh()
        })
        col.addView(Ui.spacer(a, 8))
        val series = when (chartMetric) {
            "Swelling" -> TrendMath.swelling(logs)
            "Mood" -> TrendMath.mood(logs)
            "Energy" -> TrendMath.energy(logs)
            "Journal" -> TrendMath.journalMood(a.store.journalEntries())
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
        chart.contentDescription = run {
            val last = series.points.lastOrNull()
            "$chartMetric trend chart. " + if (last == null) "No entries yet."
            else "${series.points.size} entries, latest ${last.value}."
        }
        chartCard.addView(chart, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(a, 210))
        col.addView(chartCard, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        col.addView(Ui.spacer(a, 6))
        col.addView(Ui.caption(a, "Solid line: daily entries · dashed: 7-entry average"))

        // ---- your pace (personalised vs the typical timeline) ----
        val pace = com.recoverwell.core.logic.Pace.project(a.store.profile(), today)
        col.addView(Ui.section(a, "Your pace"))
        val paceCard = Ui.card(a)
        val paceHead = if (pace.earlyDays) "Tracking your progress"
            else if (pace.deltaWeeks >= 1) "~${pace.deltaWeeks} week${if (pace.deltaWeeks == 1) "" else "s"} ahead"
            else if (pace.deltaWeeks <= -1) "~${-pace.deltaWeeks} week${if (pace.deltaWeeks == -1) "" else "s"} behind"
            else "On track"
        paceCard.addView(Ui.text(a, paceHead, 16f, Ui.TEXT, bold = true))
        paceCard.addView(Ui.spacer(a, 2))
        paceCard.addView(Ui.text(a, pace.summary, 14f, Ui.TEXT))
        if (pace.projectedMilestones.isNotEmpty()) {
            paceCard.addView(Ui.spacer(a, 6))
            for ((m, date) in pace.projectedMilestones) {
                paceCard.addView(Ui.caption(a, "~${date.format(fmt)} · ${m.title}"))
            }
        }
        col.addView(paceCard)

        // ---- return to padel (criteria-based program) ----
        run {
            val rts = com.recoverwell.core.logic.ReturnToSport.progress(
                a.store.profile(), a.store.selfTestResults(), a.store.rtsSignoffs(), today)
            val cleared = rts.rungs.count { it.cleared }
            col.addView(Ui.section(a, "Return to sport"))
            col.addView(Ui.listRow(a, "ic_flag", rts.returnPhrase,
                if (rts.available) "$cleared of ${rts.rungs.size} stages cleared · ${rts.readinessPct}% ready"
                else "Objective self-tests unlock around phase ${rts.startPhase}") {
                a.pushOverlay(rts.returnPhrase) { ReturnToSportScreen.build(a) }
            })
        }

        // ---- insights ----
        val insights = com.recoverwell.core.logic.Insights.generate(
            a.store.profile(), logs, a.store.allEvents(), a.store.medications(), a.store.tasks(), today)
        if (insights.isNotEmpty()) {
            col.addView(Ui.section(a, "Insights"))
            for (ins in insights) col.addView(TodayScreen.insightCard(a, ins))
        }

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
        col.addView(Ui.spacer(a, 6))
        col.addView(Ui.caption(a, "Export a PDF, CSV or full backup any time from More › Data."))
    }
}
