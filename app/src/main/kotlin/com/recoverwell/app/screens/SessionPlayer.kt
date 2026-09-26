package com.recoverwell.app.screens

import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.recoverwell.app.MainActivity
import com.recoverwell.app.notify.Reminders
import com.recoverwell.app.ui.OwnClips
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.logic.ScheduleEngine
import com.recoverwell.core.model.EventStatus
import com.recoverwell.core.model.ExerciseSpec
import com.recoverwell.core.protocol.ProtocolRegistry

/**
 * Plays a whole exercise session with as little phone-handling as possible:
 * one exercise after another, ONE tap per set (never per rep - nobody should
 * have to tap 36 times to do 3 × 12), a real countdown for holds and timed
 * rounds, "skip" if something hurts, and each exercise logged the moment its
 * last set is done. State lives here rather than in the view, so a rebuild
 * (returning to the app, a theme change) resumes where the user was.
 */
object SessionPlayer {

    private class State(val title: String, val slot: String, val ids: List<String>) {
        var index = 0
        var step = 0
        val done = ArrayList<String>()
        val skipped = ArrayList<String>()
    }

    private var state: State? = null
    private var timer: android.os.CountDownTimer? = null

    /** Drop any session in progress (called when the player is closed). */
    fun reset() {
        timer?.cancel()
        timer = null
        state = null
    }

    /** Starts - or resumes, if it's the same session - playing [exerciseIds], logging into [slot]. */
    fun open(a: MainActivity, title: String, slot: String, exerciseIds: List<String>) {
        val st = state
        if (st == null || st.slot != slot || st.ids != exerciseIds) {
            reset()
            state = State(title, slot, exerciseIds)
        }
        a.pushOverlay(title, onDispose = { reset() }) { build(a) }
    }

    /** Taps an exercise takes in the player: a round per timed interval, else a set. */
    fun stepsFor(ex: ExerciseSpec): Int =
        if (ex.isTimed) ex.sets.coerceAtLeast(1) * ex.reps.coerceAtLeast(1) else ex.sets.coerceAtLeast(1)

    /** A single timed hold per set (e.g. 3 × 30s balance): the set itself is timed. */
    private fun holdOnly(ex: ExerciseSpec) = !ex.isTimed && ex.reps == 1 && ex.holdSeconds > 0

    private fun exercises(a: MainActivity, ids: List<String>): List<ExerciseSpec> {
        val all = ScheduleEngine.mergedExercises(
            ProtocolRegistry.forProfile(a.store.profile()).phases.flatMap { it.exercises },
            a.store.exerciseOverrides()
        ).associateBy { it.id }
        return ids.mapNotNull { all[it] }
    }

    private fun build(a: MainActivity): View {
        timer?.cancel()
        timer = null
        val st = state ?: return Ui.column(a)
        val list = exercises(a, st.ids)
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, st.title) { a.popOverlay() })
        if (st.index >= list.size) {
            addComplete(a, col, st, list)
            return Ui.scroll(a, col)
        }
        val ex = list[st.index]
        val steps = stepsFor(ex)

        // where you are in the session
        if (list.size > 1) {
            col.addView(Ui.caption(a, "Exercise ${st.index + 1} of ${list.size}"))
            col.addView(Ui.setDots(a, list.size, st.index).apply { gravity = Gravity.START })
            col.addView(Ui.spacer(a, 8))
        }
        val demoCard = Ui.frame(a)
        demoCard.background = Ui.rounded(Ui.SURFACE_HIGH)
        demoCard.clipToOutline = true
        demoCard.addView(OwnClips.demoView(a, ex), ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(a, 170))
        col.addView(demoCard, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        // a real demonstration is one tap away mid-session (the animation is the quick reference)
        col.addView(Ui.textButton(a, "Watch video demonstration") {
            // a running hold would otherwise finish behind the video and rebuild it
            timer?.cancel()
            timer = null
            if (a.store.setting("video_inapp", "true") != "false") VideoScreen.open(a, ex)
            else a.openUrl(VideoScreen.externalUrl(a, ex))
        }.apply { contentDescription = "Watch a video demonstration of ${ex.name}" })
        col.addView(Ui.headline(a, ex.name))
        col.addView(Ui.text(a, ScheduleEngine.exercisePrescription(ex), 14.5f, Ui.PRIMARY, bold = true))
        ex.cues.forEachIndexed { i, c -> col.addView(Ui.text(a, "${i + 1}. $c", 14f, Ui.TEXT)) }

        // the one control that matters right now
        val stage = Ui.card(a)
        stage.gravity = Gravity.CENTER_HORIZONTAL
        val unit = if (ex.isTimed) "Round" else "Set"
        val detail = when {
            ex.isTimed -> ScheduleEngine.durationLabel(ex.holdSeconds)
            holdOnly(ex) -> "hold ${ex.holdSeconds}s"
            else -> "${ex.reps} reps" + if (ex.holdSeconds > 0) " · hold ${ex.holdSeconds}s each" else ""
        }
        stage.addView(Ui.pillBadge(a, "$unit ${st.step + 1} of $steps · $detail",
            com.recoverwell.draw.Palette.ON_PRIMARY_CONTAINER, Ui.PRIMARY_CONTAINER))
        stage.addView(Ui.spacer(a, 8))
        if (ex.isTimed || holdOnly(ex)) {
            val secs = ex.holdSeconds
            val btn = Ui.button(a, if (ex.isTimed) "Start ${ScheduleEngine.durationLabel(secs)} timer"
                else "Start ${secs}s hold") {}
            btn.setOnClickListener {
                if (timer != null) return@setOnClickListener
                it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                timer = object : android.os.CountDownTimer(secs * 1000L, 500L) {
                    override fun onTick(ms: Long) {
                        val t = ((ms + 999) / 1000).toInt()
                        btn.text = "%d:%02d left".format(t / 60, t % 60)
                    }
                    override fun onFinish() {
                        timer = null
                        stepDone(a, ex, steps)
                    }
                }.start()
            }
            stage.addView(Ui.fullWidth(btn, a))
            stage.addView(Ui.fullWidth(Ui.textButton(a, "Done - next") { stepDone(a, ex, steps) }, a, 2))
        } else {
            stage.addView(Ui.fullWidth(Ui.button(a, "$unit ${st.step + 1} done") { stepDone(a, ex, steps) }, a))
        }
        if (st.step > 0) stage.addView(Ui.caption(a, "Rest briefly, then go again."))
        col.addView(stage)
        col.addView(Ui.fullWidth(Ui.textButton(a, "Skip this exercise", Ui.TEXT_DIM) {
            st.skipped.add(ex.id)
            st.index++
            st.step = 0
            a.refresh()
        }, a, 2))
        col.addView(Ui.caption(a, ex.precaution))
        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    private fun stepDone(a: MainActivity, ex: ExerciseSpec, steps: Int) {
        val st = state ?: return
        a.window.decorView.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        st.step++
        if (st.step >= steps) {
            // logged as soon as it's finished, so leaving mid-session keeps what was done
            Reminders.recordEvent(a, ScheduleEngine.ItemKind.EXERCISE, ex.id, st.slot, EventStatus.DONE)
            st.done.add(ex.id)
            st.index++
            st.step = 0
        }
        a.refresh()
    }

    private fun addComplete(a: MainActivity, col: LinearLayout, st: State, list: List<ExerciseSpec>) {
        val card = Ui.card(a, Ui.DONE_BG)
        card.addView(Ui.text(a, if (st.done.isEmpty()) "Nothing logged" else "Session complete",
            20f, Ui.DONE, bold = true))
        card.addView(Ui.spacer(a, 4))
        card.addView(Ui.text(a, "${st.done.size} of ${list.size} exercise${if (list.size == 1) "" else "s"} " +
            "logged." + if (st.skipped.isEmpty()) "" else " Skipped: " +
            list.filter { it.id in st.skipped }.joinToString(", ") { it.name } + ".", 14.5f, Ui.TEXT))
        if (st.skipped.isNotEmpty()) {
            card.addView(Ui.spacer(a, 4))
            card.addView(Ui.caption(a, "If you skipped something because it hurt, mention it to your physio."))
        }
        col.addView(card)
        // back to wherever the session was started (Today or Exercises)
        col.addView(Ui.fullWidth(Ui.button(a, "Done") { a.show(a.currentTab) }, a))
    }
}
