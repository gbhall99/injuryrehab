package com.recoverwell.app.screens

import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.recoverwell.app.MainActivity
import com.recoverwell.app.notify.Reminders
import com.recoverwell.app.ui.Forms
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.logic.Fitness
import com.recoverwell.core.logic.PhaseEngine
import com.recoverwell.core.logic.ScheduleEngine
import com.recoverwell.core.model.EventStatus
import java.time.LocalDate

/**
 * "Stay fit": general conditioning to keep the rest of the body strong during
 * recovery, gated to what's safe now, with a simple weekly session goal. Scales
 * per injury - the activities are protocol data.
 */
object StayFitScreen {

    fun build(a: MainActivity): View {
        val today = LocalDate.now()
        val profile = a.store.profile()
        val phase = PhaseEngine.currentPhase(profile, today).number
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Stay fit") { a.popOverlay() })
        col.addView(Ui.caption(a, "Anything that doesn't load the foot counts - it protects your mood, heart " +
            "and the rest of you, and gives you a head start when the tendon's ready."))

        // ---- this week: progress, one button to log, the goal changed in place ----
        val goal = a.store.setting("fitness_goal", Fitness.DEFAULT_WEEKLY_GOAL.toString()).toIntOrNull()
            ?: Fitness.DEFAULT_WEEKLY_GOAL
        val done = Fitness.sessionsThisWeek(a.store.allEvents(), today)
        val card = Ui.card(a, Ui.HERO_BG)
        card.setPadding(Ui.dp(a, 20), Ui.dp(a, 16), Ui.dp(a, 20), Ui.dp(a, 16))
        val onHero = Ui.ON_HERO
        card.addView(Ui.text(a, "This week", 12.5f, com.recoverwell.draw.Palette.withAlpha(onHero, 0xCC), bold = true))
        card.addView(Ui.text(a, "$done of $goal conditioning sessions", 22f, onHero, bold = true))
        card.addView(Ui.spacer(a, 8))
        card.addView(Ui.setDots(a, goal.coerceIn(1, 10), done.coerceAtMost(goal)))
        col.addView(card)
        col.addView(Ui.buttonPair(a,
            Ui.button(a, "Log a session") {
                Reminders.recordEvent(a, ScheduleEngine.ItemKind.EXERCISE, Fitness.SESSION_REF, "session", EventStatus.DONE)
                Toast.makeText(a, "Conditioning session logged - nice work", Toast.LENGTH_SHORT).show()
                a.refresh()
            },
            Ui.textButton(a, "Goal: $goal a week") { changeGoal(a, goal) }.apply {
                contentDescription = "Weekly goal, $goal sessions. Change"
            }))

        // ---- what's good now (with how), and what opens later (just when) ----
        val activities = Fitness.all(profile)
        val (now, later) = activities.partition { phase >= it.minPhase }
        if (now.isNotEmpty()) {
            col.addView(Ui.section(a, "Good to do now"))
            val group = Ui.card(a)
            now.forEachIndexed { i, act ->
                if (i > 0) group.addView(Ui.divider(a))
                val row = Ui.row(a)
                row.gravity = Gravity.TOP
                row.addView(Ui.icon(a, "ic_exercises", 18, Ui.PRIMARY))
                val texts = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
                texts.setPadding(Ui.dp(a, 10), 0, 0, 0)
                texts.addView(Ui.text(a, act.name, 14.5f, Ui.TEXT, bold = true))
                texts.addView(Ui.spacer(a, 2))
                texts.addView(Ui.text(a, act.detail, 13.5f, Ui.TEXT))
                row.addView(Ui.weight(texts, 1f))
                group.addView(row)
            }
            col.addView(group)
        }
        if (later.isNotEmpty()) {
            col.addView(Ui.section(a, "Later"))
            val group = Ui.card(a)
            later.sortedBy { it.minPhase }.forEachIndexed { i, act ->
                if (i > 0) group.addView(Ui.spacer(a, 4))
                val row = Ui.row(a)
                row.addView(Ui.icon(a, "ic_shield", 16, Ui.TEXT_DIM))
                row.addView(Ui.weight(Ui.text(a, act.name, 14f, Ui.TEXT_DIM).apply {
                    setPadding(Ui.dp(a, 10), 0, Ui.dp(a, 8), 0) }, 1f))
                row.addView(Ui.text(a, "phase ${act.minPhase}", 12.5f, Ui.TEXT_DIM, bold = true))
                group.addView(row)
            }
            col.addView(group)
        }

        col.addView(Ui.spacer(a, 6))
        col.addView(Ui.pillBadge(a, "Keep load off the healing tendon - if a move tugs the heel cord, stop",
            Ui.WARN, Ui.WARN_BG))
        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    /** The weekly goal, stepped in a small dialog (was an always-open stepper). */
    private fun changeGoal(a: MainActivity, goal: Int) {
        var value = goal
        val pad = Ui.dp(a, 18)
        val holder = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, Ui.dp(a, 6), pad, 0)
            addView(Forms.stepper(a, "Sessions a week", value, 1, 10) { value = it })
        }
        android.app.AlertDialog.Builder(a)
            .setTitle("Weekly goal")
            .setView(holder)
            .setPositiveButton("Save") { _, _ -> a.store.saveSetting("fitness_goal", value.toString()); a.refresh() }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
