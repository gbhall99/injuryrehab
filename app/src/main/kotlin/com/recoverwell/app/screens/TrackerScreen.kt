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

/** Progress is review-only: trends, pace, insights, milestones (+ backfill). */
object TrackerScreen {

    private var chartMetric = "Pain"
    private var showAllMilestones = false

    fun build(a: MainActivity): View {
        val today = LocalDate.now()
        val col = Ui.column(a)

        // Progress reviews the trend first; the option to add/edit a past day sits
        // at the bottom (logging today happens on Today) so a review-only screen
        // leads with the weekly digest rather than an editing control. If today
        // isn't logged yet, one row offers it - trends are only as good as the logs.
        // "send my progress to my physio" starts where the progress is
        col.addView(Ui.listRow(a, "ic_export", "Share with your physio", "A PDF of your progress, logs and plan") {
            a.exportPdf()
        })
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

    /**
     * "Your pace", in one card: the recovery-days timeline toward the estimated
     * return date, ahead / on track / behind, and the headline numbers (streaks,
     * sport readiness), each tile opening its history. Merges what were three
     * places: Today's stats card, Progress's pace card and its return-to-sport row.
     */
    private fun paceCard(a: MainActivity, profile: com.recoverwell.core.model.Profile, today: LocalDate): View {
        val events0 = a.store.allEvents()
        val medStreak = ScheduleEngine.medicationStreak(a.store.medications(), events0, today, afterDate = profile.injuryDate)
        val exStreak = ScheduleEngine.exerciseStreak(profile, a.store.exerciseOverrides(), events0, today,
            a.store.exerciseSessions())
        val card = Ui.card(a)
        val pace = com.recoverwell.core.logic.Pace.project(profile, today)
        card.addView(Ui.text(a, if (pace.earlyDays) "Tracking your progress"
            else if (pace.deltaWeeks >= 1) "~${pace.deltaWeeks} week${if (pace.deltaWeeks == 1) "" else "s"} ahead"
            else if (pace.deltaWeeks <= -1) "~${-pace.deltaWeeks} week${if (pace.deltaWeeks == -1) "" else "s"} behind"
            else "On track", 16f, Ui.TEXT, bold = true))
        card.addView(Ui.spacer(a, 2))
        card.addView(Ui.text(a, pace.summary, 14f, Ui.TEXT))
        card.addView(Ui.spacer(a, 12))

        // recovery-days timeline toward the estimated return date
        val cu = java.time.temporal.ChronoUnit.DAYS
        val target = profile.effectiveReturnDate()
        val totalDays = cu.between(profile.injuryDate, target).coerceAtLeast(1)
        val dayN = cu.between(profile.injuryDate, today).coerceIn(0, totalDays)
        val pct = ((dayN.toDouble() / totalDays) * 100).toInt()
        val dayRow = Ui.row(a)
        dayRow.addView(Ui.text(a, "Day $dayN", 24f, Ui.TEXT, bold = true))
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

        // headline numbers: each tile pairs a streak with a percentage and is
        // tappable through to the editable history for that metric. The
        // medication tile only appears when meds are actually being tracked
        // (otherwise the streak would read a meaningless "0").
        val rts = com.recoverwell.core.logic.ReturnToSport.progress(
            profile, a.store.selfTestResults(), a.store.rtsSignoffs(), today)
        val logs = a.store.allLogs()
        val events = a.store.allEvents()
        val overrides = a.store.exerciseOverrides()
        val meds = a.store.medications()
        val hasMeds = meds.any { it.active }
        val ciStreak = checkInStreak(logs, today, profile.injuryDate)
        val pain7 = recentPainAvg(logs, today)
        val weeksIn = PhaseEngine.weeksSinceInjury(profile, today)

        val tiles = ArrayList<View>()
        tiles.add(metricTile(a, "$exStreak-day", "Exercise streak",
            "${ScheduleEngine.exerciseAdherence(profile, overrides, events, today,
                a.store.exerciseSessions())}% done · 7d") {
            a.pushOverlay("Exercise history") { HistoryScreen.exercises(a) }
        })
        if (hasMeds) tiles.add(metricTile(a, "$medStreak-day", "Med streak",
            "${medAdherence(meds, events, today)}% taken · 7d") {
            a.pushOverlay("Medication history") { HistoryScreen.medication(a) }
        })
        tiles.add(metricTile(a, "$ciStreak-day", "Check-in streak",
            if (pain7 != null) "Pain $pain7/10 avg · 7d" else "Start logging your pain") {
            a.pushOverlay("Check-in history") { HistoryScreen.checkins(a) }
        })
        // return to sport is always a tile: readiness once it's open, when it opens before that
        tiles.add(metricTile(a, if (rts.available) "${rts.readinessPct}%" else "Phase ${rts.startPhase}",
            if (rts.available) "Sport-ready" else rts.returnPhrase,
            if (rts.available) rts.currentRung?.let { "Stage: ${it.title}" } ?: "Building strength"
            else "Self-tests open then") {
            a.pushOverlay(rts.returnPhrase) { ReturnToSportScreen.build(a) }
        })
        // recovery progress (and the editable injury / target dates behind it)
        if (tiles.size < 4) tiles.add(metricTile(a, "$pct%", "Recovery", "Week $weeksIn") {
            a.pushOverlay("Injury & goal") { MoreScreen.profileEditor(a) }
        })
        card.addView(Ui.spacer(a, 14))
        card.addView(statGrid(a, tiles.take(4)))
        return card
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

    /** Lay a list of equal-width tiles out two per row. */
    private fun statGrid(a: MainActivity, tiles: List<View>): View {
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

    /** Overlay: the shared check-in for a chosen past day. */
    private fun pastDayOverlay(a: MainActivity, date: LocalDate): View {
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Log for $date") { a.popOverlay() })
        var save: View? = null
        col.addView(TodayScreen.checkInCard(a, date, pinSave = { save = it }) {
            Toast.makeText(a, "Log saved", Toast.LENGTH_SHORT).show()
            a.popOverlay()
        })
        col.addView(Ui.spacer(a, 24))
        return Ui.withActionBar(a, Ui.scroll(a, col), save!!)
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
            digest.adherencePct?.let { line("ic_pill", "Medication: $it% of doses taken") }
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
                "No entries yet - log a check-in on Today"
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
        col.addView(Ui.section(a, "Your pace"))
        col.addView(paceCard(a, a.store.profile(), today))

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
        val all = MilestoneTimeline.build(profile, today)
        // what's behind you is one line; what's next is the detail - full list on request
        val reached = all.count { it.status == MilestoneTimeline.Status.REACHED }
        val entries = if (showAllMilestones) all
            else all.filter { it.status != MilestoneTimeline.Status.REACHED }.take(3)
        if (!showAllMilestones && reached > 0) {
            val r = Ui.row(a)
            r.addView(Ui.icon(a, "ic_check", 18, Ui.DONE))
            val t = Ui.text(a, "$reached milestone${if (reached == 1) "" else "s"} reached", 15f, Ui.DONE, bold = true)
            t.setPadding(Ui.dp(a, 10), 0, 0, Ui.dp(a, 8))
            r.addView(t)
            timeline.addView(r)
        }
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
        col.addView(timeline)
        if (all.size > entries.size || showAllMilestones) {
            col.addView(Ui.fullWidth(Ui.textButton(a,
                if (showAllMilestones) "Show fewer" else "Show all ${all.size} milestones") {
                showAllMilestones = !showAllMilestones
                a.refresh()
            }, a, 2))
        }
    }
}
