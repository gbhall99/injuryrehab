package com.recoverwell.app.screens

import android.app.AlertDialog
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.recoverwell.app.MainActivity
import com.recoverwell.app.notify.Reminders
import com.recoverwell.app.ui.Forms
import com.recoverwell.app.ui.SceneView
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.logic.Capability
import com.recoverwell.core.logic.PhaseEngine
import com.recoverwell.core.logic.ScheduleEngine
import com.recoverwell.core.model.EventStatus
import com.recoverwell.core.model.Swelling
import com.recoverwell.core.protocol.ProtocolRegistry
import com.recoverwell.draw.RingScene
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Unified daily view: hero progress card, risk warnings, gate prompt, checklist. */
object TodayScreen {

    fun build(a: MainActivity): View {
        val today = LocalDate.now()
        val profile = a.store.profile()
        val col = Ui.column(a)

        val phase = PhaseEngine.currentPhase(profile, today)
        val week = PhaseEngine.weeksSinceInjury(profile, today)
        val items = ScheduleEngine.dailyChecklist(
            profile, a.store.medications(), a.store.tasks(),
            a.store.exerciseOverrides(), a.store.eventsOn(today), today,
            a.store.exerciseSessions()
        )
        val doneCount = items.count { it.isDone }
        val dayProgress = if (items.isEmpty()) 0f else doneCount.toFloat() / items.size
        val allEvents = a.store.allEvents()
        val medStreak = ScheduleEngine.medicationStreak(
            a.store.medications(), allEvents, today, afterDate = profile.injuryDate)
        val exStreak = ScheduleEngine.exerciseStreak(
            profile, a.store.exerciseOverrides(), allEvents, today)

        // ---- hero card -------------------------------------------------
        val hero = Ui.card(a, Ui.HERO_BG)
        hero.setPadding(Ui.dp(a, 20), Ui.dp(a, 18), Ui.dp(a, 20), Ui.dp(a, 18))
        val heroRow = Ui.row(a)
        val heroTexts = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        val onHero = Ui.ON_HERO
        val onHeroDim = com.recoverwell.draw.Palette.withAlpha(onHero, 0xCC)
        val dateLine = today.format(DateTimeFormatter.ofPattern("EEEE d MMMM"))
        heroTexts.addView(Ui.text(a,
            if (profile.name.isBlank()) dateLine else "Hi ${profile.name} · $dateLine",
            13f, onHeroDim, bold = true))
        heroTexts.addView(Ui.text(a, "Week $week", 30f, onHero, bold = true))
        heroTexts.addView(Ui.text(a, "Phase ${phase.number} · ${phase.title}", 14f,
            com.recoverwell.draw.Palette.withAlpha(onHero, 0xE6)))
        heroTexts.addView(Ui.spacer(a, 6))
        heroTexts.addView(Ui.text(a, "$doneCount of ${items.size} done today", 13f, onHeroDim))
        // streak chips: medication and exercise, side by side (each shown at 2+ days)
        val chips = ArrayList<View>()
        if (medStreak >= 2) chips.add(streakChip(a, "ic_flag", "$medStreak-day meds"))
        if (exStreak >= 2) chips.add(streakChip(a, "ic_exercises", "$exStreak-day exercise"))
        if (chips.isNotEmpty()) {
            heroTexts.addView(Ui.spacer(a, 8))
            val wrap = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL }
            chips.forEachIndexed { i, c ->
                val lp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                if (i > 0) lp.leftMargin = Ui.dp(a, 6)
                wrap.addView(c, lp)
            }
            heroTexts.addView(wrap, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        heroRow.addView(Ui.weight(heroTexts, 1f))

        val ringBox = FrameLayout(a)
        var sweep = 0f
        val ring = SceneView(a) { s ->
            RingScene.render(s, sweep, Ui.dpF(a, 9f),
                trackColor = com.recoverwell.draw.Palette.withAlpha(onHero, 0x59), color = onHero)
        }
        android.animation.ValueAnimator.ofFloat(0f, dayProgress).apply {
            duration = 700
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener { sweep = it.animatedValue as Float; ring.invalidate() }
            start()
        }
        ringBox.addView(ring, FrameLayout.LayoutParams(Ui.dp(a, 92), Ui.dp(a, 92)))
        val pct = Ui.text(a, "${(dayProgress * 100).toInt()}%", 19f, onHero, bold = true)
        val pctLp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        pctLp.gravity = Gravity.CENTER
        ringBox.addView(pct, pctLp)
        heroRow.addView(ringBox)
        hero.addView(heroRow)
        hero.addView(Ui.spacer(a, 10))
        val phaseBtn = Ui.text(a, "Phase guide", 13.5f, onHero, bold = true).apply {
            background = Ui.ripple(a, Ui.rounded(com.recoverwell.draw.Palette.withAlpha(onHero, 0x28), 22f), 0x40FFFFFF)
            setPadding(Ui.dp(a, 16), Ui.dp(a, 9), Ui.dp(a, 16), Ui.dp(a, 9))
            contentDescription = "Phase guide"
            setOnClickListener { a.pushOverlay("Phase ${phase.number}") { phaseDetail(a, phase.number) } }
        }
        hero.addView(phaseBtn, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        col.addView(hero)

        // ---- prioritized attention surface ("calm Today") -------------------
        // Everything that wants attention is ranked. Safety items always show as
        // full cards; the single most important of the rest becomes the focus
        // card; everything else collapses into a tidy "More for you" list so the
        // daily checklist below is never buried.
        val recentLogs = a.store.allLogs().filter { !it.date.isBefore(today.minusDays(7)) }
        val gate = PhaseEngine.nextPhaseGate(profile, today)
        val warnings = Capability.warnings(profile, recentLogs, today)
        // "settled" once the user has a few days of check-ins; before that, Today
        // stays minimal (hero + safety + checklist + check-in) to teach the rhythm.
        val settled = a.store.allLogs().count { it.pain != null } >= SETTLED_AFTER_CHECKINS

        // urgent (red-flag) warnings are never collapsed - their own danger cards
        for (w in warnings.filter { it.severity == Capability.Severity.URGENT }) {
            val card = Ui.card(a, Ui.DANGER_BG)
            val headRow = Ui.row(a)
            headRow.addView(Ui.icon(a, "ic_alert", 20, Ui.DANGER))
            val ht = Ui.text(a, w.title, 15.5f, Ui.ON_DANGER_BG, bold = true)
            ht.setPadding(Ui.dp(a, 10), 0, 0, 0)
            headRow.addView(Ui.weight(ht, 1f))
            card.addView(headRow)
            card.addView(Ui.spacer(a, 4))
            card.addView(Ui.text(a, w.detail, 14f, Ui.ON_DANGER_BG))
            card.addView(Ui.fullWidth(Ui.dangerButton(a, "Open red flags") {
                a.pushOverlay("Red flags") { RedFlagsScreen.build(a) }
            }, a))
            col.addView(card)
        }

        val prompts = ArrayList<Prompt>()

        // safety-class prompts (anticoagulant schedule, off-plan health warnings)
        if (com.recoverwell.app.notify.ReminderHealth.deliveryBlocked(a)) {
            prompts.add(Prompt(2, "ic_alert", "Reminders may not reach you",
                "A phone setting is blocking notifications - for an anticoagulant schedule that matters.",
                "Check reminder settings", TONE_WARN, safety = true) { a.show(MainActivity.Tab.MORE) })
        }
        for (w in warnings.filter { it.severity == Capability.Severity.WARNING }) {
            prompts.add(Prompt(3, "ic_alert", w.title, w.detail, "Open red flags", TONE_WARN, safety = true) {
                a.pushOverlay("Red flags") { RedFlagsScreen.build(a) }
            })
        }
        // a time-limited medicine (e.g. VTE prophylaxis) reaching its review/end:
        // prompt a clinician decision rather than silently stopping or continuing
        for (med in a.store.medications().filter { it.active && it.reviewDate != null }) {
            val review = med.reviewDate!!
            val end = med.courseEndDate
            if (!today.isBefore(review) && (end == null || !today.isAfter(end))) {
                val endNote = end?.let { " Reminders are set to stop after $it." } ?: ""
                prompts.add(Prompt(4, "ic_pill", "Review your ${med.name.lowercase()} course",
                    "Your prescribed course is due for review.$endNote Confirm with your clinician whether to " +
                        "continue or stop - never stop a clot-prevention medicine early without advice.",
                    "Manage medication", TONE_WARN, safety = true) {
                    a.pushOverlay("Medications") { MoreScreen.medsEditor(a) }
                })
            }
        }

        // progression gate
        if (gate.nextPhase != null && gate.readyToConfirm) {
            prompts.add(Prompt(10, "ic_calendar", "Ready for phase ${gate.nextPhase!!.number}?",
                "The typical timeline reaches \"${gate.nextPhase!!.title}\" on ${gate.startDate}. " +
                    "Only your physio can confirm you're ready.", "My physio confirmed it", TONE_INFO) {
                confirmGate(a, gate.nextPhase!!.number, gate.nextPhase!!.title, today)
            })
        }
        // backup nudge
        if (a.store.setting("last_backup", "").isBlank() && a.store.allLogs().size >= 7) {
            prompts.add(Prompt(11, "ic_restore", "Protect your progress",
                "A week of recovery data and no backup yet. One tap saves it to a file you control.",
                "Back up now", TONE_INFO) { a.exportBackup() })
        }
        // physio appointment prep / capture / re-book
        run {
            val outlook = com.recoverwell.core.logic.Appointments.outlook(profile.appointments, today)
            val soon = outlook.next?.takeIf { it.date.isBefore(today.plusDays(8)) }
            val toCapture = outlook.overdue.maxByOrNull { it.date }
            if (soon != null) {
                val days = java.time.temporal.ChronoUnit.DAYS.between(today, soon.date)
                prompts.add(Prompt(12, "ic_calendar",
                    if (days == 0L) "${soon.label} today" else "${soon.label} in $days day${if (days == 1L) "" else "s"}",
                    "Prep your questions and current numbers to make the most of it.",
                    "Open appointment pack", TONE_INFO) { a.pushOverlay("Physio visits") { PhysioScreen.build(a) } })
            } else if (toCapture != null) {
                prompts.add(Prompt(13, "ic_edit", "How did your appointment go?",
                    "Capture what your physio said so your plan stays in sync.",
                    "Capture the visit", TONE_INFO) { a.pushOverlay("Physio visits") { PhysioScreen.build(a) } })
            } else if (outlook.needsRebooking) {
                prompts.add(Prompt(14, "ic_calendar", "Book your next physio visit",
                    "Your last appointment is done and nothing's scheduled - line up the next one to keep your plan moving.",
                    "Add appointment", TONE_INFO) { a.pushOverlay("Physio visits") { PhysioScreen.build(a) } })
            }
            Unit
        }
        // urgent: a recent voice check-in flagged a possible red-flag symptom
        a.store.redFlagAlert()?.let { (_, note) ->
            prompts.add(Prompt(1, "ic_alert", "Worth getting checked",
                note.ifBlank { "Something from a recent check-in may need medical attention." },
                "See red-flag guidance", TONE_WARN, safety = true) {
                a.store.clearRedFlagAlert()
                a.pushOverlay("Red flags") { RedFlagsScreen.build(a) }
            })
        }
        // daily recovery-journal nudge (only when AI is on and not yet logged today)
        if (AiScreen.enabled(a) && a.store.journalEntries().none { it.date == today }) {
            prompts.add(Prompt(17, "ic_edit", "Record today's check-in",
                "Speak freely about your day - AI reflects it back and spots patterns.",
                "Open journal", TONE_INFO) { a.openJournal() })
        }
        // Monday: offer the weekly AI recovery summary if not already generated
        if (AiScreen.enabled(a) && today.dayOfWeek == java.time.DayOfWeek.MONDAY &&
            a.store.journalEntries().isNotEmpty()) {
            val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
            if (a.store.cachedWeeklySummary(weekStart).isBlank()) {
                prompts.add(Prompt(19, "ic_progress", "Your weekly recovery summary",
                    "Recap last week's logs and check-ins in a few sentences.",
                    "Open journal", TONE_INFO) { a.openJournal() })
            }
        }
        // milestone celebration
        com.recoverwell.core.logic.Wellbeing.recentlyReachedMilestone(profile, today)?.let { m ->
            prompts.add(Prompt(15, "ic_flag", "Milestone reached: ${m.title}", m.detail,
                "How you're doing", TONE_DONE) { a.pushOverlay("How you're doing") { WellbeingScreen.build(a) } })
        }
        // a single caution insight (positive/neutral insights live on Progress)
        val insights = com.recoverwell.core.logic.Insights.generate(
            profile, a.store.allLogs(), allEvents, a.store.medications(), a.store.tasks(), today)
        insights.firstOrNull { it.tone == com.recoverwell.core.logic.Insights.Tone.CAUTION }?.let { ins ->
            prompts.add(Prompt(16, "ic_alert", ins.title, ins.detail, "See trends", TONE_WARN) {
                a.show(MainActivity.Tab.TRACKER)
            })
        }
        // weekly review on Mondays
        if (today.dayOfWeek == java.time.DayOfWeek.MONDAY && a.store.allLogs().size >= 5) {
            prompts.add(Prompt(18, "ic_progress", "Your week in review is ready",
                "Last week's adherence, pain trend and what to focus on next.",
                "Open this week", TONE_INFO) { a.show(MainActivity.Tab.TRACKER) })
        }
        // adaptive reminder suggestion
        com.recoverwell.core.logic.AdaptiveReminders
            .timeSuggestions(a.store.medications(), allEvents, today).firstOrNull()?.let { sug ->
            val to = com.recoverwell.core.logic.Insights.minuteLabel(sug.typicalMinute)
            prompts.add(Prompt(20, "ic_clock", "Smarter reminder time",
                "You usually take ${sug.medName} around $to, but its reminder is set for " +
                    "${com.recoverwell.core.logic.Insights.minuteLabel(sug.scheduledMinute)}.",
                "Move reminder to $to", TONE_INFO) {
                a.store.saveMedications(com.recoverwell.core.logic.AdaptiveReminders
                    .applySuggestion(a.store.medications(), sug))
                Reminders.reschedule(a)
                a.refresh()
            })
        }

        val sorted = prompts.sortedBy { it.priority }
        val rest = sorted.filterNot { it.safety }
        // always-on safety prompts as full cards, directly under the hero - never
        // gated or collapsed, so a clot/health warning is never buried
        for (p in sorted.filter { it.safety }) col.addView(focusCard(a, p))

        // ---- recovery snapshot: the key stats & timelines at a glance --------
        col.addView(recoverySnapshot(a, profile, today, phase, medStreak, exStreak))

        // ---- checklist (the core daily task: now directly under the hero + any
        // safety cards, so the actions are immediate and never buried) ----
        // medication stays one row per dose (each dose is logged individually)
        fun addGroup(label: String, kinds: Set<ScheduleEngine.ItemKind>) {
            val group = items.filter { it.kind in kinds }
            if (group.isEmpty()) return
            col.addView(Ui.section(a, label))
            for (item in group) {
                val statusLabel = when (item.status) {
                    EventStatus.MISSED -> "Marked missed"
                    EventStatus.SKIPPED -> "Skipped"
                    else -> null
                }
                val time = item.time?.let { "%02d:%02d".format(it.hour, it.minute) }
                val subtitle = if (item.kind == ScheduleEngine.ItemKind.WEDGE_CHANGE)
                    "Only with your clinic's agreement" else ""
                col.addView(Ui.checkRow(a, item.title, subtitle, time, item.isDone, statusLabel) {
                    onItemTapped(a, item)
                })
            }
        }
        // Daily care: repeated care-task times collapse to ONE row per task with
        // a counter; the daily check-in lives here too (tap it to open the form).
        fun addDailyCare() {
            col.addView(Ui.section(a, "Daily care"))
            val group = items.filter { it.kind == ScheduleEngine.ItemKind.TASK }
            val byRef = LinkedHashMap<String, MutableList<ScheduleEngine.ChecklistItem>>()
            for (it in group) byRef.getOrPut(it.refId) { ArrayList() }.add(it)
            for ((_, slots) in byRef) {
                val first = slots.first()
                val total = slots.size
                val done = slots.count { it.isDone }
                val tap = { onItemTapped(a, slots.firstOrNull { !it.isDone } ?: slots.last()) }
                if (total > 1) {
                    col.addView(Ui.progressRow(a, first.title, first.subtitle, done, total) { tap() })
                } else {
                    col.addView(Ui.checkRow(a, first.title, first.subtitle, null, done == total, null) { tap() })
                }
            }
            // the daily check-in as a care item - tapping opens the shared form
            val log = a.store.dailyLog(today)
            val checkedIn = log.pain != null
            col.addView(Ui.checkRow(a, "Daily check-in",
                if (checkedIn) "Pain ${log.pain}/10 logged · tap to update"
                else "Log how your pain feels today",
                null, checkedIn, null) {
                checkInExpanded = false
                a.pushOverlay("Daily check-in") { checkInOverlay(a, today) }
            })
        }
        // exercises are grouped into uniform daily SESSIONS - same routine each
        // session - so they're clear and consistent, not 2x here / 4x there
        fun addExerciseSessions() {
            val exItems = items.filter { it.kind == ScheduleEngine.ItemKind.EXERCISE }
            if (exItems.isEmpty()) return
            col.addView(Ui.section(a, "Exercise sessions · tap to do"))
            val bySession = LinkedHashMap<String, MutableList<ScheduleEngine.ChecklistItem>>()
            for (it in exItems) bySession.getOrPut(it.slotKey) { ArrayList() }.add(it)
            bySession.keys.sorted().forEachIndexed { idx, key ->
                val sess = bySession[key]!!
                val total = sess.size
                val done = sess.count { it.isDone }
                col.addView(Ui.progressRow(a, "Exercise session ${idx + 1}",
                    "$total exercise${if (total == 1) "" else "s"}", done, total) {
                    a.pushOverlay("Exercise session ${idx + 1}") {
                        exerciseSession(a, idx + 1, key, sess.map { it.refId })
                    }
                })
            }
        }
        // one-time legend: explains why medicines list each dose while other
        // tasks collapse to one row with a counter (logical but not obvious)
        if (items.isNotEmpty() && a.store.setting("checklist_legend_seen", "") != "1") {
            val legend = Ui.card(a, Ui.INFO_BG)
            legend.addView(Ui.text(a, "Medicines show each dose; other tasks show once with a counter.",
                13.5f, Ui.ON_INFO_BG))
            legend.addView(Ui.fullWidth(Ui.textButton(a, "Got it") {
                a.store.saveSetting("checklist_legend_seen", "1"); a.refresh()
            }, a, 2))
            col.addView(legend)
        }
        addGroup("Medication", setOf(ScheduleEngine.ItemKind.MEDICATION))
        addDailyCare()
        addExerciseSessions()
        // boot changes are scheduled (~weekly), not daily - their own section,
        // shown only on the day one is due
        addGroup("Scheduled today", setOf(ScheduleEngine.ItemKind.WEDGE_CHANGE))

        if (gate.nextPhase != null && !gate.dateEligible) {
            col.addView(Ui.spacer(a, 8))
            col.addView(Ui.caption(a, "Next: phase ${gate.nextPhase!!.number} - ${gate.nextPhase!!.title}, " +
                "typically from ${gate.startDate}. Your physio may adjust this.").apply {
                gravity = Gravity.CENTER
            })
        }

        // Once the user has a few check-ins logged, surface the single most
        // important non-safety prompt as the focus card (kept calm on first run).
        if (settled) rest.firstOrNull()?.let { col.addView(focusCard(a, it)) }

        // ---- "jump to" card grid: the hybrid home's always-visible navigation,
        // promoting the destinations otherwise buried under the More tab ----
        col.addView(jumpGrid(a, profile, today))

        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    /** A small pill chip used on the hero for medication / exercise streaks. */
    private fun streakChip(a: MainActivity, icon: String, label: String): View {
        val row = Ui.row(a)
        row.addView(Ui.icon(a, icon, 13, Ui.ON_HERO))
        val t = Ui.text(a, label, 12f, Ui.ON_HERO, bold = true)
        t.setPadding(Ui.dp(a, 6), 0, 0, 0)
        row.addView(t)
        row.background = Ui.rounded(com.recoverwell.draw.Palette.withAlpha(Ui.ON_HERO, 0x28), 14f)
        row.setPadding(Ui.dp(a, 10), Ui.dp(a, 5), Ui.dp(a, 12), Ui.dp(a, 5))
        return row
    }

    /**
     * Key stats & timelines at a glance: the overall recovery-days timeline
     * ("day X of N" toward the estimated return-to-sport date), the phase
     * timeline, and a 2x2 grid of headline numbers (today's tasks, sport
     * readiness, medication & exercise streaks).
     */
    private fun recoverySnapshot(
        a: MainActivity, profile: com.recoverwell.core.model.Profile, today: LocalDate,
        phase: com.recoverwell.core.model.PhaseSpec,
        medStreak: Int, exStreak: Int
    ): View {
        val card = Ui.card(a)
        val head = Ui.row(a)
        head.addView(Ui.icon(a, "ic_progress", 18, Ui.PRIMARY))
        val ht = Ui.text(a, "Your recovery", 16f, Ui.TEXT, bold = true)
        ht.setPadding(Ui.dp(a, 8), 0, 0, 0)
        head.addView(Ui.weight(ht, 1f))
        card.addView(head)
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

        // phase timeline
        val phaseCount = ProtocolRegistry.forProfile(profile).phases.size
        card.addView(Ui.spacer(a, 14))
        card.addView(Ui.text(a, "Phase ${phase.number} of $phaseCount · ${phase.title}",
            13f, Ui.TEXT_DIM, bold = true))
        card.addView(Ui.spacer(a, 8))
        card.addView(Ui.setDots(a, phaseCount, phase.number).apply { gravity = Gravity.START })

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
            "${exerciseAdherence(profile, overrides, events, today)}% done · 7d") {
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
        if (rts.available) tiles.add(metricTile(a, "${rts.readinessPct}%", "Sport-ready",
            rts.currentRung?.let { "Stage: ${it.title}" } ?: "Building strength") {
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

    /** % of expected exercise sessions completed over the last [days] days. */
    private fun exerciseAdherence(
        profile: com.recoverwell.core.model.Profile,
        overrides: Map<String, com.recoverwell.core.model.ExerciseOverride>,
        events: List<com.recoverwell.core.model.EventLog>, today: LocalDate, days: Int = 7
    ): Int {
        val doneByDay = events
            .filter { it.type == com.recoverwell.core.model.EventType.EXERCISE && it.status == EventStatus.DONE }
            .groupBy { it.date }
        var expected = 0
        var done = 0
        for (i in 0 until days) {
            val d = today.minusDays(i.toLong())
            val exs = ScheduleEngine.mergedExercises(PhaseEngine.currentPhase(profile, d).exercises, overrides)
            if (exs.isEmpty()) continue
            val sessions = ScheduleEngine.clampSessions(ScheduleEngine.EXERCISE_SESSIONS_PER_DAY)
            expected += sessions
            val dayEvents = doneByDay[d] ?: continue
            for (s in 1..sessions) {
                val slot = "session$s"
                if (exs.all { ex -> dayEvents.any { it.refId == ex.id && it.slotKey == slot } }) done++
            }
        }
        return if (expected == 0) 0 else done * 100 / expected
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

    /** One tappable destination tile in the "jump to" grid. */
    private fun gridCell(a: MainActivity, icon: String, title: String, sub: String, onTap: () -> Unit): View {
        val card = Ui.tapCard(a) { onTap() }
        card.contentDescription = title
        card.addView(Ui.iconBadge(a, icon, boxDp = 38))
        card.addView(Ui.spacer(a, 8))
        // wrap-content width so these content labels are never mistaken for the
        // full-width, centred bottom-nav tab labels (guarded by NavAlignmentTest)
        val wrap = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        card.addView(Ui.text(a, title, 14.5f, Ui.TEXT, bold = true), wrap)
        card.addView(Ui.text(a, sub, 12f, Ui.TEXT_DIM), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        return card
    }

    /**
     * The hybrid home's always-visible navigation: a 2-column grid of the main
     * destinations, several of them otherwise buried under the More tab.
     */
    private fun jumpGrid(a: MainActivity, profile: com.recoverwell.core.model.Profile, today: LocalDate): View {
        val colv = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        colv.addView(Ui.section(a, "Jump to"))
        val cells = ArrayList<View>()
        cells.add(gridCell(a, "ic_exercises", "Exercises", "Today's routine") {
            a.show(MainActivity.Tab.EXERCISES)
        })
        cells.add(gridCell(a, "ic_pill", "Medication", "Doses & reminders") {
            a.pushOverlay("Medications") { MoreScreen.medsEditor(a) }
        })
        cells.add(gridCell(a, "ic_ask", "Recovery coach",
            if (AiScreen.enabled(a)) "Ask anything · AI" else "Ask anything") { a.openAsk() })
        if (AiScreen.enabled(a)) cells.add(gridCell(a, "ic_edit", "Recovery journal", "Speak your day") {
            a.openJournal()
        })
        cells.add(gridCell(a, "ic_calendar", "Physio visits", "Appointments & notes") {
            a.pushOverlay("Physio visits") { PhysioScreen.build(a) }
        })
        cells.add(gridCell(a, "ic_info", "What to expect", "This stage") {
            a.pushOverlay("What to expect") { WhatToExpectScreen.build(a) }
        })
        cells.add(gridCell(a, "ic_heart", "How you're doing", "Reassurance & milestones") {
            a.pushOverlay("How you're doing") { WellbeingScreen.build(a) }
        })
        cells.add(gridCell(a, "ic_progress", "Stay fit", "Keep conditioning") {
            a.pushOverlay("Stay fit") { StayFitScreen.build(a) }
        })
        val rts = com.recoverwell.core.logic.ReturnToSport.progress(
            profile, a.store.selfTestResults(), a.store.rtsSignoffs(), today)
        if (rts.available) cells.add(gridCell(a, "ic_flag", rts.returnPhrase, "${rts.readinessPct}% ready") {
            a.pushOverlay(rts.returnPhrase) { ReturnToSportScreen.build(a) }
        })

        val gap = Ui.dp(a, 4)
        val v = Ui.dp(a, 5)
        var i = 0
        while (i < cells.size) {
            val rowv = Ui.row(a).apply { gravity = Gravity.TOP }
            cells[i].layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                .apply { setMargins(0, v, gap, v) }
            rowv.addView(cells[i])
            if (i + 1 < cells.size) {
                cells[i + 1].layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { setMargins(gap, v, 0, v) }
                rowv.addView(cells[i + 1])
            } else {
                rowv.addView(View(a), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { setMargins(gap, 0, 0, 0) })
            }
            colv.addView(rowv)
            i += 2
        }
        return colv
    }

    /**
     * The exercises that make up one Today session. Each session repeats the same
     * routine, so this lists every exercise individually - tap one to learn it and
     * log it for this session. Replaces the old behaviour where tapping a session
     * jumped straight into the first exercise, making every session look identical.
     */
    private fun exerciseSession(a: MainActivity, number: Int, slotKey: String, refIds: List<String>): View {
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Exercise session $number") { a.popOverlay() })
        col.addView(Ui.caption(a, "Your routine for this session - tap an exercise to see how " +
            "to do it and log it. Each daily session is the same set."))
        col.addView(Ui.spacer(a, 4))
        val allExercises = ProtocolRegistry.forProfile(a.store.profile()).phases.flatMap { it.exercises }
        val events = a.store.eventsOn(LocalDate.now())
        for (refId in refIds) {
            val spec = allExercises.find { it.id == refId } ?: continue
            val done = events.lastOrNull { it.refId == refId && it.slotKey == slotKey }?.status == EventStatus.DONE
            col.addView(Ui.checkRow(a, spec.name, ScheduleEngine.exercisePrescription(spec), null, done, null) {
                a.pushOverlay(spec.name) { ExercisesScreen.exerciseDetail(a, spec, slotKey) }
            })
        }
        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    private fun onItemTapped(a: MainActivity, item: ScheduleEngine.ChecklistItem) {
        when (item.kind) {
            ScheduleEngine.ItemKind.MEDICATION -> {
                AlertDialog.Builder(a)
                    .setTitle(item.title)
                    .setMessage("Record the ${item.slotKey} dose. If in doubt about a missed dose, ask your pharmacist or 111 - never double up.")
                    .setPositiveButton("Taken") { _, _ -> record(a, item, EventStatus.TAKEN) }
                    .setNegativeButton("Missed") { _, _ -> record(a, item, EventStatus.MISSED) }
                    .setNeutralButton("Cancel", null)
                    .show()
            }
            ScheduleEngine.ItemKind.WEDGE_CHANGE -> {
                if (item.isDone) {
                    Forms.confirm(a, "Undo", "Mark \"${item.title}\" as not done?") {
                        record(a, item, EventStatus.SKIPPED)
                    }
                } else {
                    val after = item.refId.removePrefix("wedge_").toIntOrNull()
                    val device = ProtocolRegistry.forProfile(a.store.profile()).supportDevice
                    val afterLabel = after?.let { device?.format(it) ?: it.toString() }
                    Forms.confirm(
                        a, "Boot change",
                        "Only change the boot if your clinic agreed this step. " +
                            "Mark it done" + (afterLabel?.let { " (now $it)" } ?: "") + "?"
                    ) {
                        if (after != null) {
                            a.store.saveProfile(a.store.profile().copy(currentWedges = after))
                        }
                        record(a, item, EventStatus.DONE)
                    }
                }
            }
            else -> {
                if (item.isDone) {
                    Forms.confirm(a, "Undo", "Mark \"${item.title}\" as not done?") {
                        record(a, item, EventStatus.SKIPPED)
                    }
                } else record(a, item, EventStatus.DONE)
            }
        }
    }

    /** A single insight rendered as a toned card with a leading icon. */
    fun insightCard(a: MainActivity, ins: com.recoverwell.core.logic.Insights.Insight): View {
        val (bg, fg, icon) = when (ins.tone) {
            com.recoverwell.core.logic.Insights.Tone.POSITIVE -> Triple(Ui.DONE_BG, Ui.DONE, "ic_check")
            com.recoverwell.core.logic.Insights.Tone.CAUTION -> Triple(Ui.WARN_BG, Ui.WARN, "ic_alert")
            com.recoverwell.core.logic.Insights.Tone.NEUTRAL -> Triple(Ui.INFO_BG, Ui.ON_INFO_BG, "ic_info")
        }
        val card = Ui.card(a, bg)
        val r = Ui.row(a)
        r.gravity = android.view.Gravity.TOP
        r.addView(Ui.icon(a, icon, 20, fg))
        val texts = android.widget.LinearLayout(a).apply { orientation = android.widget.LinearLayout.VERTICAL }
        texts.setPadding(Ui.dp(a, 10), 0, 0, 0)
        texts.addView(Ui.text(a, ins.title, 15f, fg, bold = true))
        texts.addView(Ui.spacer(a, 2))
        texts.addView(Ui.text(a, ins.detail, 13.5f, Ui.TEXT))
        r.addView(Ui.weight(texts, 1f))
        card.addView(r)
        return card
    }

    fun record(a: MainActivity, item: ScheduleEngine.ChecklistItem, status: EventStatus) {
        Reminders.recordEvent(a, item.kind, item.refId, item.slotKey, status)
        a.refresh()
    }

    /** Whether the Today check-in is showing its expanded fields in place. */
    private var checkInExpanded = false

    /** Check-ins logged before Today reveals the focus card + "more for you"; until
     *  then the home screen stays minimal so new users learn the daily rhythm. */
    private const val SETTLED_AFTER_CHECKINS = 3

    /** Overlay wrapping the shared check-in form, opened from the Daily care row. */
    private fun checkInOverlay(a: MainActivity, date: LocalDate): View {
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Daily check-in") { a.popOverlay() })
        col.addView(checkInCard(a, date, checkInExpanded) { a.popOverlay(); a.refresh() })
        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    /**
     * The single daily check-in form, shared by Today and Progress. Pain is all
     * that's needed; mood/swelling/energy/notes are optional behind a disclosure.
     * No boot/ROM here - boot is profile state, ROM is captured at physio visits.
     */
    fun checkInCard(a: MainActivity, date: LocalDate, expanded: Boolean, onSaved: () -> Unit): View {
        val today = LocalDate.now()
        val log = a.store.dailyLog(date)
        val card = Ui.card(a)
        card.addView(Ui.text(a, if (date == today) "How's your pain today?" else "Log for $date",
            16f, Ui.TEXT, bold = true))
        card.addView(Ui.caption(a, "Pain is all that's needed - add detail if you like."))
        card.addView(Ui.spacer(a, 4))
        // start at the day's saved value, else yesterday's, so most days are one tap
        var pain = log.pain ?: (a.store.dailyLog(date.minusDays(1)).pain ?: 0)
        card.addView(Forms.scaleSlider(a, 10, pain, "0 · None", "10 · Worst") { pain = it })
        var mood: Int? = log.mood
        card.addView(Forms.label(a, "Mood · optional"))
        card.addView(Forms.choiceRow(a, (1..5).toList(), { "$it" }, log.mood) { mood = it })

        var swelling: Swelling? = log.swelling
        var energy: Int? = log.energy
        var notesEdit: android.widget.EditText? = null
        if (expanded) {
            card.addView(Forms.label(a, "Swelling"))
            card.addView(Forms.choiceRow(a, Swelling.values().toList(), { it.label }, log.swelling) { swelling = it })
            card.addView(Forms.label(a, "Energy"))
            card.addView(Forms.choiceRow(a, (1..5).toList(), { "$it" }, log.energy) { energy = it })
            card.addView(Forms.label(a, "Notes"))
            notesEdit = Forms.editText(a, log.notes ?: "", "Anything worth remembering", multiline = true)
            card.addView(notesEdit)
        }

        card.addView(Ui.fullWidth(Ui.button(a, if (date == today) "Save check-in" else "Save log for $date") {
            a.store.saveDailyLog(log.copy(
                pain = pain, mood = mood, swelling = swelling, energy = energy,
                notes = notesEdit?.text?.toString()?.ifBlank { null } ?: log.notes))
            onSaved()
        }, a))
        if (!expanded) {
            card.addView(Ui.fullWidth(Ui.textButton(a, "Add more detail (swelling, energy, notes)") {
                checkInExpanded = true
                a.refresh()
            }, a, 2))
        }
        // voice front-end: speak your day and let AI fill the check-in for you
        if (date == today && AiScreen.enabled(a)) {
            card.addView(Ui.fullWidth(Ui.textButton(a, "🎙  Speak your check-in instead") {
                a.openJournal()
            }, a, 2))
        }
        return card
    }

    // ---- calm-Today prompt model -------------------------------------------

    private const val TONE_INFO = 0
    private const val TONE_WARN = 1
    private const val TONE_DONE = 2

    private class Prompt(
        val priority: Int, val icon: String, val title: String, val body: String,
        val action: String, val tone: Int, val safety: Boolean = false, val onTap: () -> Unit
    )

    private fun toneBg(tone: Int) = when (tone) { TONE_WARN -> Ui.WARN_BG; TONE_DONE -> Ui.DONE_BG; else -> Ui.INFO_BG }
    private fun toneFg(tone: Int) = when (tone) { TONE_WARN -> Ui.WARN; TONE_DONE -> Ui.DONE; else -> Ui.ON_INFO_BG }
    private fun toneBody(tone: Int) = if (tone == TONE_INFO) Ui.ON_INFO_BG else Ui.TEXT

    private fun focusCard(a: MainActivity, p: Prompt): View {
        val card = Ui.card(a, toneBg(p.tone))
        val r = Ui.row(a)
        r.gravity = Gravity.TOP
        r.addView(Ui.icon(a, p.icon, 20, toneFg(p.tone)))
        val t = Ui.text(a, p.title, 15.5f, toneFg(p.tone), bold = true)
        t.setPadding(Ui.dp(a, 10), 0, 0, 0)
        r.addView(Ui.weight(t, 1f))
        card.addView(r)
        card.addView(Ui.spacer(a, 3))
        card.addView(Ui.text(a, p.body, 14f, toneBody(p.tone)))
        card.addView(Ui.fullWidth(Ui.button(a, p.action) { p.onTap() }, a))
        return card
    }

    private fun confirmGate(a: MainActivity, number: Int, title: String, today: LocalDate) {
        Forms.confirm(a, "Confirm progression",
            "Has your physiotherapist explicitly confirmed phase $number ($title)?") {
            val pp = a.store.profile()
            a.store.saveProfile(pp.copy(
                physioConfirmedPhase = number,
                phaseConfirmedDates = pp.phaseConfirmedDates + (number to today)))
            Reminders.reschedule(a)
            a.refresh()
        }
    }

    fun phaseDetail(a: MainActivity, phaseNumber: Int): View {
        val phase = ProtocolRegistry.forProfile(a.store.profile()).phase(phaseNumber)
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Phase ${phase.number}") { a.popOverlay() })
        col.addView(Ui.headline(a, phase.title))
        col.addView(Ui.caption(a, phase.subtitle))
        col.addView(Ui.spacer(a, 4))
        col.addView(Ui.pillBadge(a, "Typical timing - confirm with your physio", Ui.WARN, Ui.WARN_BG))

        fun bullets(heading: String, lines: List<String>, color: Int = Ui.TEXT) {
            col.addView(Ui.section(a, heading))
            val card = Ui.card(a)
            lines.forEachIndexed { i, l ->
                if (i > 0) card.addView(Ui.spacer(a, 6))
                card.addView(Ui.text(a, l, 14.5f, color))
            }
            col.addView(card)
        }
        bullets("Entry criteria", phase.entryCriteria)
        bullets("Goals", phase.goals)
        bullets("Precautions", phase.precautions, Ui.WARN)
        bullets("OK in this phase", phase.allowed, Ui.DONE)
        bullets("Not yet", phase.notAllowed, Ui.DANGER)
        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }
}
