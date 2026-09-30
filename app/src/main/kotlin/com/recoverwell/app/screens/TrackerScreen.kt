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
import com.recoverwell.core.logic.PhaseEngine
import com.recoverwell.core.logic.ScheduleEngine
import com.recoverwell.core.model.EventStatus

/** Progress is review-only, in three parts: this week, trends, and your road back. */
object TrackerScreen {

    private var chartMetric = "Pain"
    private var showAllMilestones = false

    fun build(a: MainActivity): View {
        val today = LocalDate.now()
        val col = Ui.column(a)
        // "send my progress to my physio" starts where the progress is
        col.addView(Ui.listRow(a, "ic_export", "Share with your physio", "A PDF of your progress, logs and plan") {
            a.exportPdf()
        })
        // review only: logging happens on Today, and a missed day is added from the
        // check-ins tile's history (it was also a row here)
        buildReview(a, today, col)
        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    /**
     * "Your road back", in one card: ahead / on track / behind, the recovery-days
     * timeline toward the estimated return date, [middle] (the milestones), and
     * where return to sport stands.
     */
    private fun paceCard(a: MainActivity, profile: com.recoverwell.core.model.Profile, today: LocalDate,
                         middle: (LinearLayout) -> Unit): View {
        val card = Ui.card(a)
        val pace = com.recoverwell.core.logic.Pace.project(profile, today)
        card.addView(Ui.text(a, if (pace.earlyDays) "Tracking your progress"
            else if (pace.deltaWeeks >= 1) "~${pace.deltaWeeks} week${if (pace.deltaWeeks == 1) "" else "s"} ahead"
            else if (pace.deltaWeeks <= -1) "~${-pace.deltaWeeks} week${if (pace.deltaWeeks == -1) "" else "s"} behind"
            else "On track", 16f, Ui.TEXT, bold = true))
        card.addView(Ui.spacer(a, 2))
        card.addView(Ui.text(a, pace.summary, 14f, Ui.TEXT))
        card.addView(Ui.spacer(a, 6))

        // recovery-days timeline toward the estimated return date
        val cu = java.time.temporal.ChronoUnit.DAYS
        val target = profile.effectiveReturnDate()
        val totalDays = cu.between(profile.injuryDate, target).coerceAtLeast(1)
        val dayN = cu.between(profile.injuryDate, today).coerceIn(0, totalDays)
        val pct = ((dayN.toDouble() / totalDays) * 100).toInt()
        val dayRow = Ui.row(a)
        dayRow.addView(Ui.text(a, "Day $dayN", 20f, Ui.TEXT, bold = true))
        dayRow.addView(Ui.text(a, "  of $totalDays", 15f, Ui.TEXT_DIM))
        dayRow.addView(Ui.weight(View(a), 1f))
        dayRow.addView(Ui.pillBadge(a, "$pct%", Ui.ON_PRIMARY_CONTAINER, Ui.PRIMARY_CONTAINER))
        card.addView(dayRow)
        card.addView(Ui.spacer(a, 8))
        card.addView(progressBar(a, dayN.toFloat() / totalDays))
        card.addView(Ui.spacer(a, 6))
        val sportName = com.recoverwell.core.logic.ReturnToSport
            .resolveSport(profile, ProtocolRegistry.forProfile(profile))?.name ?: "sport"
        val targetLabel = target.format(DateTimeFormatter.ofPattern("MMM yyyy"))
        val daysLeft = totalDays - dayN
        card.addView(Ui.caption(a, if (daysLeft <= 0)
            "Past your estimated return date - your physio guides the real timeline."
        else {
            val weeksLeft = (daysLeft + 6) / 7
            "~$weeksLeft week${if (weeksLeft == 1L) "" else "s"} to your estimated return to " +
                "$sportName · around $targetLabel"
        }))

        card.addView(Ui.divider(a))
        middle(card)
        // return to sport closes the card - where the road ends: readiness once it's
        // open, when it opens before that
        card.addView(Ui.divider(a))
        val rts = com.recoverwell.core.logic.ReturnToSport.progress(
            profile, a.store.selfTestResults(), a.store.rtsSignoffs(), today)
        card.addView(Ui.listRow(a, "ic_flag", rts.returnPhrase,
            if (rts.available) "${rts.readinessPct}% ready" + (rts.currentRung?.let { " · now: ${it.title}" } ?: "")
            else "Self-tests open in phase ${rts.startPhase} · see what's ahead") {
            a.pushOverlay(rts.returnPhrase) { ReturnToSportScreen.build(a) }
        }.apply { background = null; setPadding(0, paddingTop, 0, paddingBottom) })
        return card
    }

    /** This week's headline numbers, each tile opening its (editable) history. */
    private fun weekTiles(a: MainActivity, profile: com.recoverwell.core.model.Profile, today: LocalDate): List<View> {
        val events = a.store.allEvents()
        val logs = a.store.allLogs()
        val meds = a.store.medications()
        val overrides = a.store.exerciseOverrides()
        val exStreak = ScheduleEngine.exerciseStreak(profile, overrides, events, today, a.store.exerciseSessions())
        val medStreak = ScheduleEngine.medicationStreak(meds, events, today, afterDate = profile.injuryDate)
        val ciStreak = checkInStreak(logs, today, profile.injuryDate)
        val pain7 = recentPainAvg(logs, today)
        val tiles = ArrayList<View>()
        tiles.add(metricTile(a, "${ScheduleEngine.exerciseAdherence(profile, overrides, events, today,
                a.store.exerciseSessions())}%", "Exercises done", "$exStreak-day streak") {
            a.pushOverlay("Exercise history") { HistoryScreen.exercises(a) }
        })
        // the medication tile only when meds are tracked (a streak of "0" would mean nothing)
        if (meds.any { it.active }) tiles.add(metricTile(a, "${medAdherence(meds, events, today)}%", "Doses taken",
            "$medStreak-day streak") {
            a.pushOverlay("Medication history") { HistoryScreen.medication(a) }
        })
        tiles.add(metricTile(a, pain7?.let { "$it/10" } ?: "-", "Pain, average",
            if (ciStreak > 0) "$ciStreak-day check-in streak" else "Log it on Today") {
            a.pushOverlay("Check-in history") { HistoryScreen.checkins(a) }
        })
        return tiles
    }

    /** A tappable stat tile: a headline value, a label, and a secondary line
     *  (typically a streak paired with a percentage), routing to its history. */
    private fun metricTile(a: MainActivity, value: String, label: String, sub: String, onTap: () -> Unit): View {
        val tile = Ui.column(a, 0).apply {
            background = Ui.ripple(a, Ui.rounded(Ui.SURFACE_HIGH, Ui.RADIUS_SMALL))
            setPadding(Ui.dp(a, 12), Ui.dp(a, 10), Ui.dp(a, 12), Ui.dp(a, 10))
            isClickable = true
            isFocusable = true
            contentDescription = "$label: $value, $sub"
            setOnClickListener { onTap() }
        }
        tile.addView(Ui.text(a, value, 18f, Ui.TEXT, bold = true))
        tile.addView(Ui.caption(a, label))
        tile.addView(Ui.text(a, sub, 11.5f, Ui.PRIMARY, bold = true).apply { maxLines = 1 })
        return tile
    }

    /** % of scheduled medication doses logged as taken over the last [days] days. */
    private fun medAdherence(
        meds: List<com.recoverwell.core.model.Medication>,
        events: List<com.recoverwell.core.model.EventLog>, today: LocalDate, days: Int = 7
    ): Int {
        val taken = events.filter {
            it.type == com.recoverwell.core.model.EventType.MEDICATION && it.status == EventStatus.TAKEN
        }
        var expected = 0
        var got = 0
        for (i in 0 until days) {
            val d = today.minusDays(i.toLong())
            for (m in meds.filter { it.activeOn(d) }) for (t in m.times) {
                expected++
                val slot = ScheduleEngine.slotKey(t)
                if (taken.any { it.date == d && it.refId == m.id && it.slotKey == slot }) got++
            }
        }
        return if (expected == 0) 0 else got * 100 / expected
    }

    /** Consecutive days with a logged check-in (pain recorded), ending today/
     *  yesterday and only counting days strictly after [afterDate] (the injury). */
    private fun checkInStreak(
        logs: List<com.recoverwell.core.model.DailyLog>, today: LocalDate, afterDate: LocalDate
    ): Int {
        val logged = logs.filter { it.pain != null }.map { it.date }.toHashSet()
        var day = if (today in logged) today else today.minusDays(1)
        var n = 0
        while (day in logged && day.isAfter(afterDate)) { n++; day = day.minusDays(1) }
        return n
    }

    /** Mean pain over the last 7 days of check-ins, rounded; null if none logged. */
    private fun recentPainAvg(logs: List<com.recoverwell.core.model.DailyLog>, today: LocalDate): Int? {
        val recent = logs.filter {
            it.pain != null && !it.date.isBefore(today.minusDays(6)) && !it.date.isAfter(today)
        }.mapNotNull { it.pain }
        if (recent.isEmpty()) return null
        return Math.round(recent.average()).toInt()
    }

    /** Thin rounded progress bar (fraction of [frac] filled with the primary tint). */
    private fun progressBar(a: MainActivity, frac: Float): View {
        val f = frac.coerceIn(0f, 1f)
        val track = LinearLayout(a).apply {
            orientation = LinearLayout.HORIZONTAL
            background = Ui.rounded(Ui.SURFACE_HIGH, Ui.RADIUS_SMALL)
            clipToOutline = true
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(a, 10))
        }
        if (f > 0f) track.addView(View(a).apply { setBackgroundColor(Ui.PRIMARY) },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, f))
        if (f < 1f) track.addView(View(a),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f - f))
        return track
    }

    /** Lay a list of equal-width tiles out in one row (three or fewer) or two per row. */
    private fun statGrid(a: MainActivity, tiles: List<View>): View {
        if (tiles.size <= 3) {
            val rowv = Ui.row(a)
            tiles.forEachIndexed { i, t ->
                t.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { setMargins(if (i == 0) 0 else Ui.dp(a, 4), 0, if (i == tiles.size - 1) 0 else Ui.dp(a, 4), 0) }
                rowv.addView(t)
            }
            return rowv
        }
        val colv = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        val gap = Ui.dp(a, 5)
        val v = Ui.dp(a, 5)
        var i = 0
        while (i < tiles.size) {
            val rowv = Ui.row(a)
            tiles[i].layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                .apply { setMargins(0, v, gap, v) }
            rowv.addView(tiles[i])
            if (i + 1 < tiles.size) {
                tiles[i + 1].layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { setMargins(gap, v, 0, v) }
                rowv.addView(tiles[i + 1])
            } else {
                rowv.addView(View(a), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { setMargins(gap, 0, 0, 0) })
            }
            colv.addView(rowv)
            i += 2
        }
        return colv
    }

    /** The review surfaces: trends, pace, return-to-sport, insights, milestones. */
    private fun buildReview(a: MainActivity, today: LocalDate, col: LinearLayout) {
        // ---- this week: the numbers, then what they mean (insights folded in - they
        // were a section of their own) ----
        val logs = a.store.allLogs()
        run {
            val profile = a.store.profile()
            val digest = com.recoverwell.core.logic.WeeklyDigest.generate(
                profile, logs, a.store.allEvents(), a.store.medications(), a.store.tasks(), today)
            col.addView(Ui.section(a, "This week"))
            val card = Ui.card(a)
            card.addView(statGrid(a, weekTiles(a, profile, today)))
            card.addView(Ui.spacer(a, 6))
            fun line(icon: String, text: String, tint: Int = Ui.PRIMARY, bold: Boolean = false) {
                val r = Ui.row(a)
                r.gravity = android.view.Gravity.TOP
                r.addView(Ui.icon(a, icon, 18, tint))
                val t = Ui.text(a, text, 14f, Ui.TEXT, bold = bold)
                t.setPadding(Ui.dp(a, 10), 0, 0, 0)
                r.addView(Ui.weight(t, 1f))
                card.addView(r)
                card.addView(Ui.spacer(a, 6))
            }
            val painTint = when (digest.painTrend) {
                com.recoverwell.core.logic.WeeklyDigest.Trend.DOWN -> Ui.DONE
                com.recoverwell.core.logic.WeeklyDigest.Trend.UP -> Ui.WARN
                else -> Ui.PRIMARY
            }
            line("ic_pulse", digest.painDetail, painTint)
            if (digest.milestonesThisWeek.isNotEmpty()) {
                line("ic_flag", "Reached: ${digest.milestonesThisWeek.joinToString(", ")}", Ui.DONE)
            }
            val insights = com.recoverwell.core.logic.Insights.generate(
                profile, logs, a.store.allEvents(), a.store.medications(), a.store.tasks(), today)
            for (ins in insights.take(2)) {
                val (icon, tint) = when (ins.tone) {
                    com.recoverwell.core.logic.Insights.Tone.POSITIVE -> "ic_check" to Ui.DONE
                    com.recoverwell.core.logic.Insights.Tone.CAUTION -> "ic_alert" to Ui.WARN
                    com.recoverwell.core.logic.Insights.Tone.NEUTRAL -> "ic_info" to Ui.PRIMARY
                }
                line(icon, "${ins.title}. ${ins.detail}", tint)
            }
            line("ic_info", "Focus this week: ${digest.focus}", Ui.PRIMARY, bold = false)
            col.addView(card)
        }

        // ---- trends ----
        // what the chart shows is one control beside the heading (was a row of four chips)
        val metrics = if (AiScreen.enabled(a) && a.store.journalEntries().isNotEmpty())
            listOf("Pain", "Swelling", "Mood", "Energy", "Journal")
        else listOf("Pain", "Swelling", "Mood", "Energy")
        val trendHead = Ui.row(a)
        trendHead.addView(Ui.weight(Ui.section(a, "Trends"), 1f))
        trendHead.addView(Ui.textButton(a, "$chartMetric ▾") {
            android.app.AlertDialog.Builder(a)
                .setTitle("Show on the chart")
                .setSingleChoiceItems(metrics.toTypedArray(), metrics.indexOf(chartMetric)) { d, which ->
                    chartMetric = metrics[which]; d.dismiss(); a.refresh()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }.apply { contentDescription = "Chart shows $chartMetric. Change" })
        col.addView(trendHead)
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
                "No entries yet - log a check-in on Today"
            ))
        }
        chart.contentDescription = run {
            val last = series.points.lastOrNull()
            "$chartMetric trend chart. " + if (last == null) "No entries yet."
            else "${series.points.size} entries, latest ${last.value}."
        }
        chartCard.addView(chart, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(a, 160))
        col.addView(chartCard, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        col.addView(Ui.spacer(a, 6))
        col.addView(Ui.caption(a, "Line: each entry · dashed: 7-entry average"))

        // ---- your road back: pace, the timeline, milestones and return to sport in one
        // card (were "Your pace" and "Milestones", each with its own card) ----
        col.addView(Ui.section(a, "Your road back"))
        col.addView(paceCard(a, a.store.profile(), today) { card -> addMilestones(a, card, today) })
    }

    /** Milestones inside the road-back card: what's behind you is one line, the next one is the detail. */
    private fun addMilestones(a: MainActivity, timeline: LinearLayout, today: LocalDate) {
        val profile = a.store.profile()
        val fmt = DateTimeFormatter.ofPattern("d MMM")
        val all = MilestoneTimeline.build(profile, today)
        // what's behind you is one line; what's next is the detail - full list on request
        val reached = all.count { it.status == MilestoneTimeline.Status.REACHED }
        val entries = if (showAllMilestones) all
            else all.filter { it.status != MilestoneTimeline.Status.REACHED }.take(1)
        // "N reached · all 12" is one line and one control (the full list opens in place)
        val r = Ui.row(a)
        r.addView(Ui.icon(a, "ic_check", 18, Ui.DONE))
        val t = Ui.text(a, if (showAllMilestones) "All ${all.size} milestones"
            else "$reached milestone${if (reached == 1) "" else "s"} reached", 15f, Ui.DONE, bold = true)
        t.setPadding(Ui.dp(a, 10), 0, 0, 0)
        r.addView(Ui.weight(t, 1f))
        r.addView(Ui.textButton(a, if (showAllMilestones) "Show fewer" else "See all ${all.size}") {
            showAllMilestones = !showAllMilestones
            a.refresh()
        })
        timeline.addView(r)
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
            // a passed date without the phase is "typical", not an achievement
            val awaiting = e.status == MilestoneTimeline.Status.DUE_NOW && e.expectedDate.isBefore(today.minusDays(6))
            texts.addView(Ui.caption(a, (if (awaiting) "Typically ~${e.expectedDate.format(fmt)} · waiting on your " +
                "physio's go-ahead · " else "${e.expectedDate.format(fmt)} · ") + e.milestone.detail))
            row.addView(Ui.weight(texts, 1f))
            timeline.addView(row)
        }
    }
}
