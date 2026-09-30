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
import com.recoverwell.core.logic.Insights
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
        // real actions, not rows: each exercise session and the check-in count once
        val checkedInToday = a.store.dailyLog(today).pain != null
        val (doneCount, totalCount) = ScheduleEngine.dayProgress(items, checkedInToday)
        val dayProgress = if (totalCount == 0) 0f else doneCount.toFloat() / totalCount
        val allEvents = a.store.allEvents()

        // ---- hero card -------------------------------------------------
        // compact on purpose: the morning's actions (doses, the pain check-in) must fit
        // on the first screen beneath it - see docs/CUSTOMER_THINKING_AUDIT.md
        val hero = Ui.card(a, Ui.HERO_BG)
        hero.setPadding(Ui.dp(a, 18), Ui.dp(a, 14), Ui.dp(a, 18), Ui.dp(a, 14))
        val heroRow = Ui.row(a)
        val heroTexts = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        val onHero = Ui.ON_HERO
        val onHeroDim = com.recoverwell.draw.Palette.withAlpha(onHero, 0xCC)
        // the next physio visit rides on the date line (a dated chip would wrap the chips)
        val nextVisit = profile.appointments.filter { !it.completed && !it.date.isBefore(today) }.minByOrNull { it.date }
        val dateLine = if (nextVisit == null) today.format(DateTimeFormatter.ofPattern("EEEE d MMMM"))
            else today.format(DateTimeFormatter.ofPattern("EEE d MMM")) + " · physio " +
                (if (nextVisit.date == today) "today" else nextVisit.date.format(DateTimeFormatter.ofPattern("EEE d MMM")))
        heroTexts.addView(Ui.text(a,
            if (profile.name.isBlank()) dateLine else "Hi ${profile.name} · $dateLine",
            13f, onHeroDim, bold = true).apply { maxLines = 1 })
        heroTexts.addView(Ui.text(a, "Week $week", 24f, onHero, bold = true))
        heroTexts.addView(Ui.text(a, "Phase ${phase.number} · ${phase.title}", 14f,
            com.recoverwell.draw.Palette.withAlpha(onHero, 0xE6)))
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
        ringBox.addView(ring, FrameLayout.LayoutParams(Ui.dp(a, 68), Ui.dp(a, 68)))
        // the ring's % says it at a glance; the count stays for TalkBack
        ringBox.contentDescription = "${(dayProgress * 100).toInt()}% of today's care done, $doneCount of $totalCount"
        val pct = Ui.text(a, "${(dayProgress * 100).toInt()}%", 16f, onHero, bold = true)
        val pctLp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        pctLp.gravity = Gravity.CENTER
        ringBox.addView(pct, pctLp)
        heroRow.addView(ringBox)
        hero.addView(heroRow)
        hero.addView(Ui.spacer(a, 8))
        // the three things people come looking for between daily tasks, each named in
        // their words and one tap from home (they used to sit under Settings or a
        // "Phase guide" chip that named none of them)
        fun heroChip(label: String, desc: String, onTap: () -> Unit) =
            Ui.text(a, label, 13f, onHero, bold = true).apply {
                background = Ui.ripple(a, Ui.rounded(com.recoverwell.draw.Palette.withAlpha(onHero, 0x28), 22f), 0x40FFFFFF)
                setPadding(Ui.dp(a, 12), Ui.dp(a, 8), Ui.dp(a, 12), Ui.dp(a, 8))
                maxLines = 1
                isClickable = true
                isFocusable = true
                contentDescription = desc
                setOnClickListener { onTap() }
            }
        val chips = Ui.FlowRow(a, Ui.dp(a, 6))
        chips.addView(heroChip("What to expect", "What to expect now, phase ${phase.number}") {
            a.show(MainActivity.Tab.TWIN)
        })
        TwinScreen.adjustableDevice(a)?.let { device ->
            chips.addView(heroChip("Boot ${device.format(profile.currentWedges)}",
                "${TwinScreen.changeLabel(device)}, now ${device.format(profile.currentWedges)}") {
                TwinScreen.adjustDevice(a, device)
            })
        }
        chips.addView(heroChip("Physio",
            if (nextVisit == null) "Physio visits - add your next appointment"
            else "Physio visits - next on ${Forms.friendlyDate(nextVisit.date)}") {
            a.pushOverlay("Physio visits") { PhysioScreen.build(a) }
        })
        hero.addView(chips)
        col.addView(hero)

        // ---- prioritized attention surface ("calm Today") -------------------
        // Everything that wants attention is ranked. Safety items always show as
        // full cards; the single most important of the rest becomes the focus
        // card; everything else waits behind one "N more suggestions" line so the
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
                val endNote = end?.let { " Reminders are set to stop after ${Forms.friendlyDate(it)}." } ?: ""
                prompts.add(Prompt(4, "ic_pill", "Review your ${med.name.lowercase()} course",
                    "Your prescribed course is due for review.$endNote Confirm with your clinician whether to " +
                        "continue or stop - never stop a clot-prevention medicine early without advice.",
                    "Manage medication", TONE_WARN, safety = true) {
                    a.pushOverlay("Medications") { MoreScreen.medsEditor(a) }
                })
            }
        }
        // a course with NO end or review date at all (added before course ends
        // existed, or via an old backup) reminds forever - once past the typical
        // course length, ask ONCE whether it should still be running. Opening the
        // editor marks it handled, so an intentionally ongoing medicine is only
        // ever asked about one time.
        for (med in a.store.medications().filter {
                it.active && it.courseEndDate == null && it.reviewDate == null }) {
            val promptedKey = "med_course_prompted_${med.id}"
            val reviewFrom = profile.injuryDate.plusWeeks(
                com.recoverwell.core.model.Medication.TYPICAL_REVIEW_WEEKS)
            if (!today.isBefore(reviewFrom) && a.store.setting(promptedKey, "") != "1") {
                prompts.add(Prompt(5, "ic_pill", "Still taking ${med.name.lowercase()}?",
                    "This medicine has no end date set, so its reminders continue indefinitely. " +
                        "Courses like clot prevention are usually time-limited - check with your " +
                        "clinician, then set an end date (or pause it if you've been told to stop). " +
                        "Never stop a clot-prevention medicine without medical advice.",
                    "Manage medication", TONE_WARN, safety = true) {
                    a.store.saveSetting(promptedKey, "1")
                    a.pushOverlay("Medications") { MoreScreen.medsEditor(a) }
                })
            }
        }

        // progression gate
        if (gate.nextPhase != null && gate.readyToConfirm) {
            prompts.add(Prompt(10, "ic_calendar", "Ready for phase ${gate.nextPhase!!.number}?",
                "The typical timeline reaches \"${gate.nextPhase!!.title}\" on " +
                    "${gate.startDate?.let { Forms.friendlyDate(it) }}. " +
                    "Only your physio can confirm you're ready - until then your plan stays on phase " +
                    "${phase.number}.", "My physio confirmed it", TONE_INFO, pinned = true) {
                confirmGate(a, gate.nextPhase!!.number, today)
            })
        }
        // coming out of the boot is a real, physio-agreed event: ask once the wean
        // phase starts, so boot checks and boot-change reminders don't outlive the boot
        val device = ProtocolRegistry.deviceFor(profile)
        if (device != null && phase.number == 3 && profile.bootWeanedDate == null &&
            a.store.setting(BOOT_PROMPT_SNOOZE, "").let { v ->
                val until = runCatching { LocalDate.parse(v) }.getOrNull()
                until == null || !today.isBefore(until)
            }) {
            prompts.add(Prompt(11, "ic_boot", "Out of the ${device.name.lowercase()} yet?",
                "When your physio says you can stop wearing it day and night, record it here - the boot " +
                    "check and any boot-change reminders stop, and My leg shows you out of it.",
                "I'm out of the boot", TONE_INFO, pinned = true,
                secondaryLabel = "Not yet", onSecondary = {
                    a.store.saveSetting(BOOT_PROMPT_SNOOZE, today.plusDays(7).toString()); a.refresh()
                }) {
                Forms.confirm(a, "Out of the ${device.name.lowercase()}?",
                    "Only if your physio has agreed you can stop using it. You can change this under Settings › Configure my plan › Your boot.") {
                    a.store.saveProfile(a.store.profile().copy(bootWeanedDate = today))
                    Reminders.reschedule(a)
                    a.refresh()
                }
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
                "See your progress", TONE_DONE) { a.show(MainActivity.Tab.TRACKER) })
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

        // ---- checklist (the core daily task: now directly under the hero + any
        // safety cards, so the actions are immediate and never buried) ----
        // medication stays one row per dose (each dose is logged individually)
        val todayEvents = a.store.eventsOn(today)
        // a finished group folds to one done line, so what's still left stands out;
        // tapping it opens the rows again (e.g. to undo) for the rest of the day
        if (unfoldedDay != today) { unfoldedDay = today; unfolded.clear(); showMoreSuggestions = false }
        fun folded(key: String, title: String): Boolean {
            if (key in unfolded) return false
            col.addView(Ui.checkRow(a, title, "Tap to see them", null, true, null) {
                unfolded.add(key); a.refresh()
            })
            return true
        }
        fun addGroup(label: String, kinds: Set<ScheduleEngine.ItemKind>) {
            val group = items.filter { it.kind in kinds }
            if (group.isEmpty()) return
            col.addView(Ui.section(a, label))
            if (kinds == setOf(ScheduleEngine.ItemKind.MEDICATION) && group.all { it.isDone } &&
                folded("meds", "All ${group.size} dose${if (group.size == 1) "" else "s"} taken today")) return
            for (item in group) {
                // an undo returns a row to "not done" - it never reads as "skipped"
                val statusLabel = if (item.status == EventStatus.MISSED) "Marked missed" else null
                val time = item.time?.let { "%02d:%02d".format(it.hour, it.minute) }
                val subtitle = when {
                    item.kind == ScheduleEngine.ItemKind.WEDGE_CHANGE -> "Only with your clinic's agreement"
                    // "did I take it?" answered at a glance
                    item.kind == ScheduleEngine.ItemKind.MEDICATION && item.status == EventStatus.TAKEN ->
                        todayEvents.lastOrNull { it.refId == item.refId && it.slotKey == item.slotKey }
                            ?.let { "Taken at " + Insights.minuteLabel(it.recordedAtMinuteOfDay) } ?: "Taken"
                    item.kind == ScheduleEngine.ItemKind.MEDICATION && item.status == null -> "Tap when taken"
                    else -> ""
                }
                col.addView(Ui.checkRow(a, item.title, subtitle, time, item.isDone, statusLabel) {
                    onItemTapped(a, item)
                })
            }
        }
        // Daily care: repeated care-task times collapse to ONE row per task with
        // a counter; the daily check-in lives here too (tap it to open the form).
        // exercises are grouped into uniform daily SESSIONS - same routine each session.
        // Daily care shows the one that's next (with how many there are); the Exercises
        // tab has them all - three near-identical rows here were three choices for one job
        fun addExerciseSessions() {
            val exItems = items.filter { it.kind == ScheduleEngine.ItemKind.EXERCISE }
            if (exItems.isEmpty()) return
            val bySession = LinkedHashMap<String, MutableList<ScheduleEngine.ChecklistItem>>()
            for (it in exItems) bySession.getOrPut(it.slotKey) { ArrayList() }.add(it)
            val keys = bySession.keys.sorted()
            if (exItems.all { it.isDone }) {
                col.addView(Ui.checkRow(a, "All ${keys.size} exercise session${if (keys.size == 1) "" else "s"} done",
                    "", null, true, null) { a.show(MainActivity.Tab.EXERCISES) })
                return
            }
            val nextIdx = keys.indexOfFirst { k -> bySession[k]!!.any { !it.isDone } }.let { if (it < 0) 0 else it }
            val key = keys[nextIdx]
            val sess = bySession[key]!!
            val sessionsDone = keys.count { k -> bySession[k]!!.all { it.isDone } }
            col.addView(Ui.progressRow(a, "Exercise session ${nextIdx + 1}",
                "${sess.size} exercise${if (sess.size == 1) "" else "s"} · $sessionsDone of ${keys.size} sessions done today",
                sess.count { it.isDone }, sess.size) {
                a.pushOverlay("Exercise session ${nextIdx + 1}") {
                    exerciseSession(a, nextIdx + 1, key, sess.map { it.refId })
                }
            })
        }
        fun addDailyCare() {
            col.addView(Ui.section(a, "Daily care"))
            val group = items.filter { it.kind == ScheduleEngine.ItemKind.TASK }
            val exercisesDone = items.filter { it.kind == ScheduleEngine.ItemKind.EXERCISE }.all { it.isDone }
            if (group.all { it.isDone } && exercisesDone && a.store.dailyLog(today).pain != null &&
                folded("care", "Daily care, exercises and check-in done")) return
            val byRef = LinkedHashMap<String, MutableList<ScheduleEngine.ChecklistItem>>()
            for (it in group) byRef.getOrPut(it.refId) { ArrayList() }.add(it)
            // the most frequent task first (elevation 3x a day before a once-a-day check):
            // what people look for most sits where the eye lands first
            for ((_, slots) in byRef.entries.sortedByDescending { it.value.size }) {
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
            addExerciseSessions()
            // once logged, the check-in is a done row here that opens the full form to
            // update or add detail (before that it has its own section, higher up)
            val log = a.store.dailyLog(today)
            if (log.pain != null) {
                col.addView(Ui.checkRow(a, "Daily check-in",
                    "Pain ${log.pain}/10 logged · tap to change or add mood, swelling, a note",
                    null, true, null) {
                    a.pushOverlay("Daily check-in") { checkInOverlay(a, today) }
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
        // a boot change is a key (~weekly) event - shown near the top on the day it's due
        addGroup("Boot change due today", setOf(ScheduleEngine.ItemKind.WEDGE_CHANGE))
        // the daily check-in: one tap on a number logs today's pain. It sits above
        // daily care until it's done, so the morning's one-off is on the first screen
        if (a.store.dailyLog(today).pain == null && items.isNotEmpty()) {
            // the question is the heading - no separate title line to read past
            col.addView(Ui.section(a, "How's your pain today?"))
            col.addView(quickPainCard(a))
        }
        addDailyCare()

        // ---- one nudge at a time: pinned prompts (the phase gate) always show as
        // full cards; once the user has a few check-ins, the single most important
        // other prompt becomes the focus card and the rest wait behind one line -
        // never dropped, never a list competing with today's tasks. (The next
        // phase, stats and "jump to" tiles moved to My leg and Progress.) ----
        val pinned = rest.filter { it.pinned }
        val unpinned = rest.filterNot { it.pinned }
        val cards = if (pinned.isNotEmpty()) pinned else if (settled) unpinned.take(1) else emptyList()
        for (p in cards) col.addView(focusCard(a, p))
        val waiting = if (settled) unpinned.filterNot { it in cards } else emptyList()
        if (waiting.isNotEmpty()) {
            if (showMoreSuggestions) {
                for (p in waiting) col.addView(Ui.listRow(a, p.icon, p.title, p.action) { p.onTap() })
            } else {
                col.addView(Ui.fullWidth(Ui.textButton(a,
                    "${waiting.size} more suggestion${if (waiting.size == 1) "" else "s"}") {
                    showMoreSuggestions = true; a.refresh()
                }, a, 4))
            }
        }

        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }


    /** One tappable destination tile in the "jump to" grid. */
    private fun gridCell(a: MainActivity, icon: String, title: String, sub: String, onTap: () -> Unit): View {
        val card = Ui.tapCard(a) { onTap() }
        card.contentDescription = "$title. $sub"
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

    private fun exerciseSession(a: MainActivity, number: Int, slotKey: String, refIds: List<String>): View {
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Exercise session $number") { a.popOverlay() })
        val allExercises = ScheduleEngine.mergedExercises(
            ProtocolRegistry.forProfile(a.store.profile()).phases.flatMap { it.exercises },
            a.store.exerciseOverrides())
        val events = a.store.eventsOn(LocalDate.now())
        val remaining = refIds.filter { id ->
            events.lastOrNull { it.refId == id && it.slotKey == slotKey }?.status != EventStatus.DONE
        }
        // the main path: play the whole session through, one tap per set
        if (remaining.isNotEmpty()) {
            col.addView(Ui.fullWidth(Ui.button(a,
                if (remaining.size == refIds.size) "Start guided session" else "Continue guided session") {
                SessionPlayer.open(a, "Exercise session $number", slotKey, remaining)
            }, a, 4))
            col.addView(Ui.caption(a, "Plays each exercise in turn - one tap per set, timers for holds."))
        }
        col.addView(Ui.section(a, "In this session"))
        var doneCount = 0
        for (refId in refIds) {
            val spec = allExercises.find { it.id == refId } ?: continue
            val done = events.lastOrNull { it.refId == refId && it.slotKey == slotKey }?.status == EventStatus.DONE
            if (done) doneCount++
            col.addView(Ui.checkRow(a, spec.name, ScheduleEngine.exercisePrescription(spec), null, done, null) {
                a.pushOverlay(spec.name) { ExercisesScreen.exerciseDetail(a, spec, slotKey) }
            })
        }
        // one-tap: log (or undo) the entire session at once
        col.addView(Ui.spacer(a, 8))
        if (refIds.isNotEmpty() && doneCount == refIds.size) {
            col.addView(Ui.fullWidth(Ui.tonalButton(a, "Mark session not done") {
                for (rid in refIds) Reminders.recordEvent(a, ScheduleEngine.ItemKind.EXERCISE, rid, slotKey, EventStatus.SKIPPED)
                a.refresh()
            }, a))
        } else {
            col.addView(Ui.fullWidth(Ui.button(a, "Mark whole session done") {
                for (rid in refIds) Reminders.recordEvent(a, ScheduleEngine.ItemKind.EXERCISE, rid, slotKey, EventStatus.DONE)
                a.refresh()
            }, a))
        }
        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    private fun onItemTapped(a: MainActivity, item: ScheduleEngine.ChecklistItem) {
        when (item.kind) {
            ScheduleEngine.ItemKind.MEDICATION -> {
                if (item.status == null || item.status == EventStatus.SKIPPED) {
                    // the common case is one tap: taking the dose
                    record(a, item, EventStatus.TAKEN)
                } else {
                    val taken = item.status == EventStatus.TAKEN
                    AlertDialog.Builder(a)
                        .setTitle(item.title)
                        .setMessage("The ${item.slotKey} dose is logged as ${if (taken) "taken" else "missed"}. " +
                            "If in doubt about a missed dose, check the leaflet or ask a pharmacist or 111 - never double up.")
                        .setPositiveButton(if (taken) "Mark missed" else "Mark taken") { _, _ ->
                            record(a, item, if (taken) EventStatus.MISSED else EventStatus.TAKEN)
                        }
                        .setNegativeButton("Undo") { _, _ -> record(a, item, EventStatus.SKIPPED) }
                        .setNeutralButton("Cancel", null)
                        .show()
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

    /**
     * The one-tap check-in: 0-10 as two rows of 48dp number chips. A tap logs
     * today's pain (carrying the boot setting and weight-bearing forward); mood,
     * swelling and notes stay optional in the full form.
     */
    private fun quickPainCard(a: MainActivity): View {
        val card = Ui.card(a)
        card.addView(Ui.caption(a, "One tap logs it · 0 none, 10 worst"))
        // the other thing people come to log sits by the question, not under the scale
        card.addView(Ui.textButton(a, "Log swelling, mood or a note") {
            a.pushOverlay("Daily check-in") { checkInOverlay(a, LocalDate.now()) }
        }.apply { gravity = Gravity.START or Gravity.CENTER_VERTICAL })
        for (values in listOf(0..5, 6..10)) {
            val r = Ui.row(a)
            for (n in values) {
                val chip = Ui.text(a, "$n", 16f, Ui.TEXT, bold = true).apply {
                    gravity = Gravity.CENTER
                    background = Ui.ripple(a, Ui.rounded(Ui.SURFACE_HIGH, 14f))
                    minHeight = Ui.dp(a, Ui.MIN_TOUCH_DP)
                    isClickable = true
                    isFocusable = true
                    contentDescription = "Pain $n out of 10"
                    setOnClickListener {
                        it.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                        Reminders.recordCheckIn(a, n)
                        a.refresh()
                    }
                }
                val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                lp.setMargins(Ui.dp(a, 3), Ui.dp(a, 3), Ui.dp(a, 3), Ui.dp(a, 3))
                r.addView(chip, lp)
            }
            // keep both rows' chips the same width
            if (values.count() < 6) r.addView(View(a), LinearLayout.LayoutParams(0, 1, 1f).apply {
                setMargins(Ui.dp(a, 3), 0, Ui.dp(a, 3), 0)
            })
            card.addView(r)
        }
        return card
    }

    /** Check-ins logged before Today reveals the focus card + "more for you"; until
     *  then the home screen stays minimal so new users learn the daily rhythm. */
    private const val SETTLED_AFTER_CHECKINS = 3

    /** Overlay wrapping the shared check-in form, opened from the Daily care row. */
    fun checkInOverlay(a: MainActivity, date: LocalDate): View {
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Daily check-in") { a.popOverlay() })
        var save: View? = null
        col.addView(checkInCard(a, date, pinSave = { save = it }) { a.popOverlay(); a.refresh() })
        col.addView(Ui.spacer(a, 24))
        return Ui.withActionBar(a, Ui.scroll(a, col), save!!)
    }

    /**
     * The single daily check-in form, shared by Today and Progress. Pain is all
     * that's needed; mood, energy and swelling are saved only if the user sets
     * them, so untouched sliders never become fake data in trends and insights.
     * No boot/ROM here - boot is profile state, ROM is captured at physio visits.
     */
    /** The check-in form. With [pinSave] the host pins the save button below its scroll area. */
    fun checkInCard(a: MainActivity, date: LocalDate, pinSave: ((View) -> Unit)? = null, onSaved: () -> Unit): View {
        val today = LocalDate.now()
        val log = a.store.dailyLog(date)
        val card = Ui.card(a)
        card.addView(Ui.text(a, if (date == today) "How are you today?" else "Log for ${Forms.friendlyDate(date)}",
            16f, Ui.TEXT, bold = true))
        card.addView(Ui.caption(a, "Pain is all that's needed. Mood, energy and swelling are optional - " +
            "only what you set is saved."))
        card.addView(Ui.spacer(a, 6))

        // start pain at the day's saved value, else yesterday's, so most days are one tap
        var pain = log.pain ?: (a.store.dailyLog(date.minusDays(1)).pain ?: 0)
        card.addView(Forms.label(a, "Pain · 0 none – 10 worst"))
        card.addView(Forms.scaleSlider(a, 10, pain, "0 None", "10 Worst") { pain = it })

        // optional metrics start "not logged" (null) and are saved only once set
        var mood: Int? = log.mood
        card.addView(Forms.label(a, "Mood · optional"))
        card.addView(Forms.scaleSlider(a, 5, mood, "1 Low", "5 Great", min = 1) { mood = it })

        var energy: Int? = log.energy
        card.addView(Forms.label(a, "Energy · optional"))
        card.addView(Forms.scaleSlider(a, 5, energy, "1 Drained", "5 Energised", min = 1) { energy = it })

        var swellingScore: Int? = log.swelling?.score
        card.addView(Forms.label(a, "Swelling · optional"))
        card.addView(Forms.scaleSlider(a, 3, swellingScore, "0 None", "3 Severe") { swellingScore = it })

        card.addView(Forms.label(a, "Notes · optional"))
        val notesEdit = Forms.editText(a, log.notes ?: "", "Anything worth remembering", multiline = true)
        card.addView(notesEdit)

        val save = Ui.fullWidth(Ui.button(a, if (date == today) "Save check-in" else "Save log for $date") {
            a.store.saveDailyLog(log.copy(
                pain = pain, mood = mood, energy = energy,
                swelling = swellingScore?.let { s -> Swelling.values().firstOrNull { it.score == s } },
                notes = notesEdit.text?.toString()?.ifBlank { null }))
            onSaved()
        }, a, if (pinSave != null) 8 else 10)
        if (pinSave != null) pinSave(save) else card.addView(save)
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
        val action: String, val tone: Int, val safety: Boolean = false,
        /** Always shown as a full card, even during the calm first-run days. */
        val pinned: Boolean = false,
        val secondaryLabel: String? = null, val onSecondary: (() -> Unit)? = null,
        val onTap: () -> Unit
    )

    private const val BOOT_PROMPT_SNOOZE = "boot_out_prompt_snooze_until"

    // the extra suggestions opened today (they tuck away again tomorrow)
    private var showMoreSuggestions = false

    // finished checklist groups the user re-opened today (they fold again tomorrow)
    private var unfoldedDay: LocalDate? = null
    private val unfolded = HashSet<String>()

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
        // safety and the phase gate get a full button; a suggestion is one tappable card
        // whose last line says where it goes
        if (p.safety || p.pinned || p.secondaryLabel != null) {
            card.addView(Ui.fullWidth(Ui.button(a, p.action) { p.onTap() }, a))
        } else {
            card.addView(Ui.spacer(a, 6))
            card.addView(Ui.text(a, p.action + "  ›", 14f, toneFg(p.tone), bold = true))
            card.background = Ui.ripple(a, Ui.rounded(toneBg(p.tone)))
            card.isClickable = true
            card.isFocusable = true
            card.contentDescription = "${p.title}. ${p.action}"
            card.setOnClickListener { p.onTap() }
        }
        if (p.secondaryLabel != null && p.onSecondary != null) {
            card.addView(Ui.fullWidth(Ui.textButton(a, p.secondaryLabel) { p.onSecondary.invoke() }, a, 2))
        }
        return card
    }

    /**
     * Records the physio's go-ahead for phase [number], then says plainly what
     * changed - the new focus, exercises and freedoms - so progressing isn't a
     * silent state flip.
     */
    fun confirmGate(a: MainActivity, number: Int, today: LocalDate) {
        val spec = ProtocolRegistry.forProfile(a.store.profile()).phase(number)
        Forms.confirm(a, "Confirm progression",
            "Has your physiotherapist explicitly confirmed phase $number (${spec.title})?") {
            val pp = a.store.profile()
            a.store.saveProfile(pp.copy(
                physioConfirmedPhase = number,
                phaseConfirmedDates = pp.phaseConfirmedDates + (number to today)))
            Reminders.reschedule(a)
            a.refresh()
            Forms.info(a, "Phase $number unlocked",
                "${spec.title}\n\nFocus now:\n• " + spec.goals.take(3).joinToString("\n• ") +
                    "\n\nNewly OK:\n• " + spec.allowed.take(3).joinToString("\n• ") +
                    "\n\nYour exercise sessions now follow phase $number (${spec.exercises.size} exercises). " +
                    "My leg › Your plan has the full do's and don'ts.")
        }
    }
}
