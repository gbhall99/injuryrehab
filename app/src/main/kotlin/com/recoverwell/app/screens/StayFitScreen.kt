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
        col.addView(Ui.caption(a, "Keeping generally fit while you recover protects your mood, heart and the rest " +
            "of your body - and gives you a head start when the tendon's ready. Anything that doesn't load the foot counts."))

        // ---- weekly goal ----
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

        col.addView(Ui.fullWidth(Ui.button(a, "Log a conditioning session") {
            Reminders.recordEvent(a, ScheduleEngine.ItemKind.EXERCISE, Fitness.SESSION_REF, "session", EventStatus.DONE)
            Toast.makeText(a, "Conditioning session logged - nice work", Toast.LENGTH_SHORT).show()
            a.refresh()
        }, a))
        val goalRow = Ui.card(a)
        goalRow.addView(Forms.stepper(a, "Weekly goal", goal, 1, 10) {
            a.store.saveSetting("fitness_goal", it.toString())
        })
        col.addView(goalRow)

        // ---- activities, grouped by category (locked ones shown greyed) ----
        val activities = Fitness.all(profile)
        for (category in activities.map { it.category }.distinct()) {
            col.addView(Ui.section(a, category))
            val group = Ui.card(a)
            activities.filter { it.category == category }.forEachIndexed { i, act ->
                if (i > 0) group.addView(Ui.divider(a))
                val unlocked = phase >= act.minPhase
                val row = Ui.row(a)
                row.gravity = Gravity.TOP
                row.addView(Ui.icon(a, if (unlocked) "ic_exercises" else "ic_shield", 18,
                    if (unlocked) Ui.PRIMARY else Ui.TEXT_DIM))
                val texts = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
                texts.setPadding(Ui.dp(a, 10), 0, 0, 0)
                texts.addView(Ui.text(a, act.name + if (!unlocked) " · from phase ${act.minPhase}" else "",
                    14.5f, if (unlocked) Ui.TEXT else Ui.TEXT_DIM, bold = true))
                texts.addView(Ui.spacer(a, 2))
                texts.addView(Ui.text(a, act.detail, 13.5f, if (unlocked) Ui.TEXT else Ui.TEXT_DIM))
                row.addView(Ui.weight(texts, 1f))
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
}
