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
        heroTexts.addView(Ui.text(a, today.format(DateTimeFormatter.ofPattern("EEEE d MMMM")),
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
            setOnClickListener { a.pushOverlay { phaseDetail(a, phase.number) } }
        }
        hero.addView(phaseBtn, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        col.addView(hero)

        // ---- urgent / warning cards ------------------------------------
        val recentLogs = a.store.allLogs().filter { !it.date.isBefore(today.minusDays(7)) }
        for (w in Capability.warnings(profile, recentLogs, today)
            .filter { it.severity != Capability.Severity.INFO }) {
            val urgent = w.severity == Capability.Severity.URGENT
            val card = Ui.card(a, if (urgent) Ui.DANGER_BG else Ui.WARN_BG)
            val headRow = Ui.row(a)
            headRow.addView(Ui.icon(a, "ic_alert", 20, if (urgent) Ui.DANGER else Ui.WARN))
            val ht = Ui.text(a, w.title, 15.5f, if (urgent) Ui.ON_DANGER_BG else Ui.WARN, bold = true)
            ht.setPadding(Ui.dp(a, 10), 0, 0, 0)
            headRow.addView(Ui.weight(ht, 1f))
            card.addView(headRow)
            card.addView(Ui.spacer(a, 4))
            card.addView(Ui.text(a, w.detail, 14f, if (urgent) Ui.ON_DANGER_BG else Ui.TEXT))
            if (urgent) {
                card.addView(Ui.fullWidth(Ui.dangerButton(a, "Open red flag guidance") {
                    a.pushOverlay { RedFlagsScreen.build(a) }
                }, a))
            }
            col.addView(card)
        }

        // ---- backup nudge (review-mined: data loss is the #1 trust killer) ----
        if (a.store.setting("last_backup", "").isBlank() && a.store.allLogs().size >= 7) {
            val card = Ui.card(a, Ui.INFO_BG)
            card.addView(Ui.text(a, "Protect your progress", 15.5f, Ui.ON_INFO_BG, bold = true))
            card.addView(Ui.spacer(a, 2))
            card.addView(Ui.text(a, "You have a week of recovery data and no backup yet. " +
                "One tap saves everything to a file you control.", 14f, Ui.ON_INFO_BG))
            card.addView(Ui.fullWidth(Ui.tonalButton(a, "Back up now") { a.exportBackup() }, a))
            col.addView(card)
        }

        // ---- progression gate -------------------------------------------
        val gate = PhaseEngine.nextPhaseGate(profile, today)
        if (gate.nextPhase != null && gate.readyToConfirm) {
            val gateCard = Ui.card(a, Ui.INFO_BG)
            gateCard.addView(Ui.text(a, "Ready for phase ${gate.nextPhase!!.number}?", 16f, Ui.ON_INFO_BG, bold = true))
            gateCard.addView(Ui.spacer(a, 2))
            gateCard.addView(Ui.text(
                a, "The typical timeline reaches \"${gate.nextPhase!!.title}\" on ${gate.startDate}. " +
                    "Only your physio can confirm you are ready.", 14f, Ui.ON_INFO_BG))
            gateCard.addView(Ui.fullWidth(Ui.button(a, "My physio confirmed it") {
                Forms.confirm(
                    a, "Confirm progression",
                    "Has your physiotherapist explicitly confirmed phase " +
                        "${gate.nextPhase!!.number} (${gate.nextPhase!!.title})?"
                ) {
                    a.store.saveProfile(a.store.profile().copy(physioConfirmedPhase = gate.nextPhase!!.number))
                    Reminders.reschedule(a)
                    a.refresh()
                }
            }, a))
            col.addView(gateCard)
        }

        // ---- checklist ----------------------------------------------------
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
                val subtitle = when (item.kind) {
                    ScheduleEngine.ItemKind.EXERCISE -> item.subtitle
                    ScheduleEngine.ItemKind.WEDGE_CHANGE -> "Only with your clinic's agreement"
                    else -> ""
                }
                col.addView(Ui.checkRow(a, item.title, subtitle, time, item.isDone, statusLabel) {
                    onItemTapped(a, item)
                })
            }
        }
        addGroup("Medication", setOf(ScheduleEngine.ItemKind.MEDICATION))
        addGroup("Daily care", setOf(ScheduleEngine.ItemKind.TASK, ScheduleEngine.ItemKind.WEDGE_CHANGE))
        addGroup("Exercises · tap for demo", setOf(ScheduleEngine.ItemKind.EXERCISE))

        if (gate.nextPhase != null && !gate.dateEligible) {
            col.addView(Ui.spacer(a, 8))
            col.addView(Ui.caption(a, "Next: phase ${gate.nextPhase!!.number} - ${gate.nextPhase!!.title}, " +
                "typically from ${gate.startDate}. Your physio may adjust this.").apply {
                gravity = Gravity.CENTER
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
            ScheduleEngine.ItemKind.EXERCISE -> {
                val spec = ProtocolRegistry.forProfile(a.store.profile())
                    .phases.flatMap { it.exercises }.find { it.id == item.refId }
                if (spec != null) {
                    a.pushOverlay { ExercisesScreen.exerciseDetail(a, spec, item.slotKey) }
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
                    Forms.confirm(
                        a, "Wedge change",
                        "Only change wedges if your clinic agreed this step. " +
                            "Mark one wedge removed" +
                            (after?.let { " ($it left in the boot)" } ?: "") + "?"
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

    fun record(a: MainActivity, item: ScheduleEngine.ChecklistItem, status: EventStatus) {
        Reminders.recordEvent(a, item.kind, item.refId, item.slotKey, status)
        a.refresh()
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
