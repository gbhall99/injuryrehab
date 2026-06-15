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
            a.store.exerciseOverrides(), a.store.eventsOn(today), today
        )
        val doneCount = items.count { it.isDone }
        val dayProgress = if (items.isEmpty()) 0f else doneCount.toFloat() / items.size

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
        val streak = ScheduleEngine.medicationStreak(a.store.medications(), a.store.allEvents(), today)
        if (streak >= 2) {
            heroTexts.addView(Ui.spacer(a, 6))
            val streakRow = Ui.row(a)
            streakRow.addView(Ui.icon(a, "ic_flag", 14, onHero))
            val st = Ui.text(a, "$streak-day medication streak", 12.5f, onHero, bold = true)
            st.setPadding(Ui.dp(a, 6), 0, 0, 0)
            streakRow.addView(st)
            streakRow.background = Ui.rounded(com.recoverwell.draw.Palette.withAlpha(onHero, 0x28), 14f)
            streakRow.setPadding(Ui.dp(a, 10), Ui.dp(a, 5), Ui.dp(a, 12), Ui.dp(a, 5))
            heroTexts.addView(streakRow, android.widget.LinearLayout.LayoutParams(
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
        val allEvents = a.store.allEvents()
        val gate = PhaseEngine.nextPhaseGate(profile, today)
        val warnings = Capability.warnings(profile, recentLogs, today)

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

        // ---- daily check-in (the same shared form Progress uses) ----
        val todayLog = a.store.dailyLog(today)
        if (todayLog.pain != null && !checkInExpanded) {
            col.addView(checkInSummary(a, todayLog))
        } else {
            col.addView(checkInCard(a, today, checkInExpanded) { checkInExpanded = false; a.refresh() })
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
                "Open journal", TONE_INFO) { a.pushOverlay("Recovery journal") { JournalScreen.build(a) } })
        }
        // Monday: offer the weekly AI recovery summary if not already generated
        if (AiScreen.enabled(a) && today.dayOfWeek == java.time.DayOfWeek.MONDAY &&
            a.store.journalEntries().isNotEmpty()) {
            val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
            if (a.store.cachedWeeklySummary(weekStart).isBlank()) {
                prompts.add(Prompt(19, "ic_progress", "Your weekly recovery summary",
                    "Recap last week's logs and check-ins in a few sentences.",
                    "Open journal", TONE_INFO) { a.pushOverlay("Recovery journal") { JournalScreen.build(a) } })
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
        // all safety prompts as full cards, then the single top non-safety as the focus
        for (p in sorted.filter { it.safety }) col.addView(focusCard(a, p))
        val rest = sorted.filterNot { it.safety }
        rest.firstOrNull()?.let { col.addView(focusCard(a, it)) }

        // ---- checklist (the daily task: kept directly under the hero/focus,
        // above the discretionary "More for you" list, so it's never buried) ----
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
        // repeated care-task times collapse to ONE row per task with a counter
        fun addCollapsedGroup(label: String, kind: ScheduleEngine.ItemKind) {
            val group = items.filter { it.kind == kind }
            if (group.isEmpty()) return
            col.addView(Ui.section(a, label))
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
                    onItemTapped(a, sess.firstOrNull { !it.isDone } ?: sess.last())
                })
            }
        }
        addGroup("Medication", setOf(ScheduleEngine.ItemKind.MEDICATION))
        addCollapsedGroup("Daily care", ScheduleEngine.ItemKind.TASK)
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

        // ---- your recovery (standing destinations, promoted from More) ---------
        // Usability testing found the three highest-value features buried in the
        // settings-shaped More tab: ask, the voice journal, and physio visit prep.
        // They now have a stable home one tap from Today.
        col.addView(Ui.section(a, "Your recovery"))
        col.addView(Ui.listRow(a, "ic_info", "Ask about your recovery",
            if (AiScreen.enabled(a)) "Can I drive yet? What's next? - answered by AI"
            else "Can I drive yet? What's next? - answered offline") {
            a.pushOverlay("Ask my recovery") { AskScreen.build(a) }
        })
        if (AiScreen.enabled(a)) {
            col.addView(Ui.listRow(a, "ic_edit", "Recovery journal",
                "Speak your day - AI reflects it back and spots patterns") {
                a.pushOverlay("Recovery journal") { JournalScreen.build(a) }
            })
        }
        col.addView(Ui.listRow(a, "ic_calendar", "Physio visits",
            "Appointment pack, sign-offs and visit notes") {
            a.pushOverlay("Physio visits") { PhysioScreen.build(a) }
        })

        // ---- more for you (discretionary, below the daily task) ----------------
        // contextual prompts and the timely recovery surfaces only; standing
        // destinations now live in the "Your recovery" section above.
        val moreRows = ArrayList<View>()
        for (p in rest.drop(1).take(2)) moreRows.add(promptRow(a, p))
        val rts = com.recoverwell.core.logic.ReturnToSport.progress(
            profile, a.store.selfTestResults(), a.store.rtsSignoffs(), today)
        if (rts.available) {
            moreRows.add(Ui.listRow(a, "ic_flag", rts.returnPhrase,
                (rts.currentRung?.let { "Stage: ${it.title}" } ?: "Keep building strength") +
                    " · ${rts.readinessPct}% ready") { a.pushOverlay(rts.returnPhrase) { ReturnToSportScreen.build(a) } })
        }
        com.recoverwell.core.logic.Wellbeing.expectationFor(profile, today)?.let { exp ->
            moreRows.add(Ui.listRow(a, "ic_info", "What to expect this week", exp.title) {
                a.pushOverlay("What to expect") { WhatToExpectScreen.build(a) }
            })
        }
        if (moreRows.isNotEmpty()) {
            col.addView(Ui.section(a, "More for you"))
            for (r in moreRows) col.addView(r)
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
            ScheduleEngine.ItemKind.EXERCISE -> {
                val spec = ProtocolRegistry.forProfile(a.store.profile())
                    .phases.flatMap { it.exercises }.find { it.id == item.refId }
                if (spec != null) {
                    a.pushOverlay(spec.name) { ExercisesScreen.exerciseDetail(a, spec, item.slotKey) }
                } else {
                    record(a, item, EventStatus.DONE)
                }
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

    /** Compact confirmation shown on Today once the day is logged. */
    private fun checkInSummary(a: MainActivity, log: com.recoverwell.core.model.DailyLog): View {
        val card = Ui.card(a)
        val row = Ui.row(a)
        row.gravity = Gravity.CENTER_VERTICAL
        row.addView(Ui.icon(a, "ic_check", 20, Ui.DONE))
        val t = Ui.text(a, "Checked in today · pain ${log.pain}/10" +
            (log.mood?.let { " · mood $it/5" } ?: ""), 14.5f, Ui.TEXT, bold = true)
        t.setPadding(Ui.dp(a, 10), 0, Ui.dp(a, 8), 0)
        row.addView(Ui.weight(t, 1f))
        row.addView(Ui.textButton(a, "Update") { checkInExpanded = true; a.refresh() })
        card.addView(row)
        return card
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
        card.addView(Ui.text(a, if (date == today) "How's it feeling today?" else "Log for $date",
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
                a.pushOverlay("Recovery journal") { JournalScreen.build(a) }
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

    private fun promptRow(a: MainActivity, p: Prompt): View =
        Ui.listRow(a, p.icon, p.title, p.body, iconTint = toneFg(p.tone), iconBg = toneBg(p.tone)) { p.onTap() }

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
