package com.recoverwell.app.screens

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.recoverwell.app.MainActivity
import com.recoverwell.app.notify.Reminders
import com.recoverwell.app.ui.ExerciseDemoView
import com.recoverwell.app.ui.Forms
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.logic.PhaseEngine
import com.recoverwell.core.logic.ScheduleEngine
import com.recoverwell.core.model.EventStatus
import com.recoverwell.core.model.ExerciseOverride
import com.recoverwell.core.model.ExerciseSpec
import com.recoverwell.core.protocol.ProtocolContent
import java.time.LocalDate

/** Per-phase exercise library with offline animated demonstrations. */
object ExercisesScreen {

    private var viewedPhase: Int? = null

    fun build(a: MainActivity): View {
        val today = LocalDate.now()
        val profile = a.store.profile()
        val current = PhaseEngine.currentPhase(profile, today).number
        val shown = viewedPhase ?: current

        val col = Ui.column(a)
        col.addView(Ui.title(a, "Exercise library"))
        col.addView(Ui.text(
            a, "Phases unlock by date AND physio confirmation. You can read ahead, " +
                "but locked phases stay view-only.", 14f, Ui.TEXT_DIM
        ))

        col.addView(Ui.spacer(a, 8))
        col.addView(Forms.choiceRow(a, ProtocolContent.phases.map { it.number }, { n ->
            if (n <= current) "P$n" else "🔒$n"
        }, shown) { n ->
            viewedPhase = n
            a.refresh()
        })

        val phase = ProtocolContent.phase(shown)
        val locked = shown > current
        col.addView(Ui.spacer(a, 6))
        col.addView(Ui.text(a, "Phase ${phase.number}: ${phase.title}", 19f, Ui.TEXT, bold = true))
        col.addView(Ui.text(a, phase.subtitle, 14f, Ui.TEXT_DIM))
        if (locked) {
            val warn = Ui.card(a, Ui.WARN_BG)
            warn.addView(Ui.text(
                a, "Not unlocked yet - these exercises are for a later stage. " +
                    "Doing them early risks re-rupture. Your physio confirms each progression.",
                15f, Ui.WARN, bold = true
            ))
            col.addView(warn)
        }

        val overrides = a.store.exerciseOverrides()
        for (spec in phase.exercises) {
            val o = overrides[spec.id]
            val effective = ScheduleEngine.mergedExercises(listOf(spec), overrides).firstOrNull()
            val card = Ui.card(a)
            card.isClickable = true
            card.setOnClickListener { a.pushOverlay { exerciseDetail(a, spec, null) } }
            card.contentDescription = "Open ${spec.name} demonstration"
            val titleRow = Ui.row(a)
            titleRow.addView(Ui.weight(Ui.text(a, spec.name, 17f, Ui.TEXT, bold = true), 1f))
            if (o?.enabled == false) titleRow.addView(Ui.badge(a, "off", Ui.BORDER, Ui.TEXT_DIM))
            card.addView(titleRow)
            if (effective != null) {
                card.addView(Ui.text(
                    a,
                    ScheduleEngine.exercisePrescription(effective) +
                        " · ${effective.sessionsPerDay}×/day", 14f, Ui.TEXT_DIM
                ))
            }
            card.addView(Ui.text(a, "▶ demonstration & cues", 14f, Ui.PRIMARY, bold = true))
            col.addView(card)
        }

        col.addView(Ui.spacer(a, 20))
        return Ui.scroll(a, col)
    }

    /** Detail overlay with the animated demo. [sessionSlot] non-null when opened from Today. */
    fun exerciseDetail(a: MainActivity, spec: ExerciseSpec, sessionSlot: String?): View {
        val today = LocalDate.now()
        val overrides = a.store.exerciseOverrides()
        val effective = ScheduleEngine.mergedExercises(listOf(spec), overrides).firstOrNull() ?: spec
        val currentPhase = PhaseEngine.currentPhase(a.store.profile(), today).number

        val col = Ui.column(a)
        col.addView(Ui.secondaryButton(a, "← Back") { a.popOverlay() })
        col.addView(Ui.title(a, spec.name))
        col.addView(Ui.text(a, "Phase ${spec.phase} exercise", 14f, Ui.TEXT_DIM))

        val demo = ExerciseDemoView(a)
        demo.demoId = spec.demoId
        demo.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(a, 240)
        )
        demo.background = Ui.roundedBg(0xFFF0F4F0.toInt(), strokeColor = Ui.BORDER)
        col.addView(demo)
        col.addView(Ui.text(a, "Tap the animation to pause/play. Works fully offline.", 13f, Ui.TEXT_DIM))

        col.addView(Ui.section(a, "Prescription"))
        val presCard = Ui.card(a)
        presCard.addView(Ui.text(
            a, ScheduleEngine.exercisePrescription(effective) + " · ${effective.sessionsPerDay} session(s) per day",
            17f, Ui.TEXT, bold = true
        ))
        presCard.addView(Ui.fullWidth(Ui.secondaryButton(a, "Edit sets / reps / hold / frequency") {
            a.pushOverlay { editOverride(a, spec) }
        }, a))
        col.addView(presCard)

        col.addView(Ui.section(a, "How to do it"))
        val cueCard = Ui.card(a)
        spec.cues.forEachIndexed { i, cue -> cueCard.addView(Ui.text(a, "${i + 1}.  $cue", 16f)) }
        col.addView(cueCard)

        col.addView(Ui.section(a, "Why this matters"))
        val whyCard = Ui.card(a)
        whyCard.addView(Ui.text(a, spec.whyItMatters, 15f))
        col.addView(whyCard)

        val precCard = Ui.card(a, Ui.WARN_BG)
        precCard.addView(Ui.text(a, "⚠ ${spec.precaution}", 15f, Ui.WARN, bold = true))
        col.addView(precCard)

        if (spec.phase == currentPhase) {
            col.addView(Ui.section(a, "Log today's sessions"))
            val events = a.store.eventsOn(today)
            for (session in 1..effective.sessionsPerDay) {
                val slot = "session$session"
                val done = events.lastOrNull {
                    it.refId == spec.id && it.slotKey == slot
                }?.status == EventStatus.DONE
                val label = if (done) "✓ Session $session done (tap to undo)" else "Mark session $session done"
                col.addView(Ui.fullWidth(
                    if (done) Ui.secondaryButton(a, label) {
                        Forms.confirm(a, "Undo", "Mark session $session as not done?") {
                            Reminders.recordEvent(a, ScheduleEngine.ItemKind.EXERCISE, spec.id, slot, EventStatus.SKIPPED)
                            a.refresh()
                        }
                    } else Ui.button(a, label) {
                        Reminders.recordEvent(a, ScheduleEngine.ItemKind.EXERCISE, spec.id, slot, EventStatus.DONE)
                        a.popOverlay()
                    }, a
                ))
            }
        } else if (spec.phase > currentPhase) {
            val warn = Ui.card(a, Ui.WARN_BG)
            warn.addView(Ui.text(
                a, "This is a phase ${spec.phase} exercise - not unlocked yet. " +
                    "Doing it early risks re-rupture.", 15f, Ui.WARN, bold = true
            ))
            col.addView(warn)
        }

        col.addView(Ui.spacer(a, 20))
        return Ui.scroll(a, col)
    }

    private fun editOverride(a: MainActivity, spec: ExerciseSpec): View {
        val existing = a.store.exerciseOverrides()[spec.id]
        val effective = ScheduleEngine.mergedExercises(listOf(spec), a.store.exerciseOverrides()).firstOrNull() ?: spec
        var sets = effective.sets
        var reps = effective.reps
        var hold = effective.holdSeconds
        var perDay = effective.sessionsPerDay
        var enabled = existing?.enabled ?: true

        val col = Ui.column(a)
        col.addView(Ui.secondaryButton(a, "← Back") { a.popOverlay() })
        col.addView(Ui.title(a, "Edit: ${spec.name}"))
        col.addView(Ui.text(
            a, "Protocol default: ${ScheduleEngine.exercisePrescription(spec)} · " +
                "${spec.sessionsPerDay}×/day. Change only as your physio advises.",
            14f, Ui.WARN
        ))
        val card = Ui.card(a)
        card.addView(Forms.stepper(a, "Sets", sets, 1, 10) { sets = it })
        card.addView(Forms.stepper(a, "Reps", reps, 1, 50) { reps = it })
        val holdStep = if (spec.holdSeconds >= 120) 30 else if (spec.holdSeconds >= 30) 5 else 1
        card.addView(Forms.stepper(a, "Hold (seconds)", hold, 0, 1800, step = holdStep) { hold = it })
        card.addView(Forms.stepper(a, "Sessions per day", perDay, 1, 8) { perDay = it })
        col.addView(card)

        val enableRow = Forms.choiceRow(a, listOf(true, false), { if (it) "Enabled" else "Disabled" }, enabled) {
            enabled = it
        }
        col.addView(Ui.section(a, "Include in daily plan"))
        col.addView(enableRow)

        col.addView(Ui.spacer(a, 12))
        col.addView(Ui.fullWidth(Ui.button(a, "Save changes") {
            a.store.saveExerciseOverride(ExerciseOverride(spec.id, sets, reps, hold, perDay, enabled))
            a.popOverlay()
        }, a))
        col.addView(Ui.fullWidth(Ui.secondaryButton(a, "Reset to protocol default") {
            a.store.saveExerciseOverride(ExerciseOverride(spec.id, null, null, null, null, true))
            a.popOverlay()
        }, a))
        col.addView(Ui.spacer(a, 20))
        return Ui.scroll(a, col)
    }
}
