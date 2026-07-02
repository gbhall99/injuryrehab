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
import com.recoverwell.core.protocol.ProtocolRegistry
import java.time.LocalDate

/** Per-phase exercise library with offline animated demonstrations. */
object ExercisesScreen {

    private var viewedPhase: Int? = null

    /** Icon per exercise family, keyed on the demo id. */
    fun iconFor(demoId: String): String = when (demoId) {
        "toe_scrunch", "towel_scrunch", "ankle_pump", "ankle_inv_ev" -> "ic_leg"
        "boot_walk", "gait_walk", "jog" -> "ic_progress"
        "bike" -> "ic_clock"
        "padel_drill", "agility", "hop" -> "ic_flag"
        "seated_core", "band_pf" -> "ic_pulse"
        else -> "ic_exercises"
    }

    fun build(a: MainActivity): View {
        val today = LocalDate.now()
        val profile = a.store.profile()
        val current = PhaseEngine.currentPhase(profile, today).number
        val shown = viewedPhase ?: current

        val col = Ui.column(a)
        col.addView(Ui.caption(a, "Phases unlock by date and physio confirmation. " +
            "Locked phases are view-only."))
        col.addView(Ui.spacer(a, 10))
        val protocol = ProtocolRegistry.forProfile(profile)
        col.addView(Forms.choiceRow(a, protocol.phases.map { it.number },
            { n -> if (n == current) "P$n · Now" else "P$n" }, shown) { n ->
            viewedPhase = n
            a.refresh()
        })

        val phase = protocol.phase(shown)
        val locked = shown > current
        col.addView(Ui.spacer(a, 12))
        col.addView(Ui.headline(a, phase.title))
        col.addView(Ui.caption(a, phase.subtitle))
        if (locked) {
            col.addView(Ui.spacer(a, 6))
            val warn = Ui.card(a, Ui.WARN_BG)
            val r = Ui.row(a)
            r.addView(Ui.icon(a, "ic_alert", 20, Ui.WARN))
            val t = Ui.text(a, "Not unlocked yet - starting these early risks re-rupture. " +
                "Your physio confirms each step.", 14f, Ui.WARN, bold = true)
            t.setPadding(Ui.dp(a, 10), 0, 0, 0)
            r.addView(Ui.weight(t, 1f))
            warn.addView(r)
            col.addView(warn)
        }
        col.addView(Ui.spacer(a, 8))
        if (phase.exercises.isNotEmpty()) {
            col.addView(Ui.caption(a, "Each set × reps below is one session - you do a few sessions a day."))
            col.addView(Ui.spacer(a, 4))
        }

        val overrides = a.store.exerciseOverrides()
        for (spec in phase.exercises) {
            val o = overrides[spec.id]
            val effective = ScheduleEngine.mergedExercises(listOf(spec), overrides).firstOrNull()
            val meta = if (effective != null)
                ScheduleEngine.exercisePrescription(effective)
            else "off"
            val row = Ui.listRow(
                a, iconFor(spec.demoId), spec.name,
                meta + if (o?.enabled == false) " · disabled" else "",
                iconTint = if (locked) Ui.TEXT_DIM else Ui.PRIMARY,
                iconBg = if (locked) Ui.SURFACE_HIGH else Ui.PRIMARY_CONTAINER
            ) { a.pushOverlay(spec.name) { exerciseDetail(a, spec) } }
            col.addView(row)
        }

        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    /** Detail overlay with the animated demo. [sessionSlot] is set when opened
     *  from a specific Today exercise session, so logging targets that session. */
    fun exerciseDetail(a: MainActivity, spec: ExerciseSpec, sessionSlot: String? = null): View {
        val today = LocalDate.now()
        val overrides = a.store.exerciseOverrides()
        val effective = ScheduleEngine.mergedExercises(listOf(spec), overrides).firstOrNull() ?: spec
        val currentPhase = PhaseEngine.currentPhase(a.store.profile(), today).number

        val col = Ui.column(a)
        col.addView(Ui.backRow(a, spec.name) { a.popOverlay() })

        val videoContext = ProtocolRegistry.forProfile(a.store.profile()).videoContext
        val videoUrl = com.recoverwell.core.protocol.ExerciseVideo.youtubeSearchUrl(spec, videoContext)
        val playInApp = a.store.setting("video_inapp", "true") != "false"
        val pinnedId = a.store.exerciseOverrides()[spec.id]?.videoId
        val resolvedId = com.recoverwell.core.protocol.ExerciseVideo.resolveVideoId(spec.id, pinnedId)

        // animated quick reference (offline); a play overlay opens a real video
        val demoCard = Ui.frame(a)
        demoCard.background = Ui.rounded(Ui.SURFACE_HIGH)
        demoCard.clipToOutline = true
        val demo = ExerciseDemoView(a)
        demo.demoId = spec.demoId
        demoCard.addView(demo, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(a, 230))
        // "Quick reference" tag, top-left
        val tag = Ui.text(a, "Quick reference", 11.5f, Ui.TEXT_DIM, bold = true)
        tag.background = Ui.rounded(com.recoverwell.draw.Palette.withAlpha(Ui.CARD, 0xE6), 10f)
        tag.setPadding(Ui.dp(a, 8), Ui.dp(a, 3), Ui.dp(a, 8), Ui.dp(a, 3))
        val tagLp = android.widget.FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        tagLp.setMargins(Ui.dp(a, 10), Ui.dp(a, 10), 0, 0)
        demoCard.addView(tag, tagLp)
        col.addView(demoCard, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // primary CTA: watch a real demonstration on YouTube
        val watchRow = Ui.row(a)
        watchRow.background = Ui.ripple(a, Ui.rounded(Ui.PRIMARY, 25f), 0x33FFFFFF)
        watchRow.minimumHeight = Ui.dp(a, 50)
        watchRow.setPadding(Ui.dp(a, 18), Ui.dp(a, 8), Ui.dp(a, 18), Ui.dp(a, 8))
        watchRow.isClickable = true
        watchRow.contentDescription = "Watch a video demonstration"
        watchRow.setOnClickListener {
            if (playInApp) VideoScreen.open(a, spec.name, resolvedId, videoUrl)
            else a.openUrl(if (resolvedId != null) "https://www.youtube.com/watch?v=$resolvedId" else videoUrl)
        }
        watchRow.addView(Ui.icon(a, "ic_play", 20, com.recoverwell.draw.Palette.ON_PRIMARY))
        val wlabel = Ui.text(a, "Watch video demonstration", 15.5f, com.recoverwell.draw.Palette.ON_PRIMARY, bold = true)
        wlabel.setPadding(Ui.dp(a, 10), 0, 0, 0)
        watchRow.addView(Ui.weight(wlabel, 1f))
        watchRow.addView(Ui.text(a, if (playInApp) "In-app" else "YouTube",
            12f, com.recoverwell.draw.Palette.withAlpha(com.recoverwell.draw.Palette.ON_PRIMARY, 0xCC)))
        col.addView(Ui.fullWidth(watchRow, a, 10))

        // pinned vs auto, with one-tap control so any exercise can be made to "always work"
        val sourceRow = Ui.row(a)
        sourceRow.gravity = android.view.Gravity.CENTER_VERTICAL
        sourceRow.addView(Ui.weight(Ui.caption(a, if (pinnedId != null) "Your pinned video · always plays"
            else "Best YouTube match · the offline animation above always works"), 1f))
        sourceRow.addView(Ui.textButton(a, if (pinnedId != null) "Change" else "Pin a video") {
            pinVideoDialog(a, spec)
        })
        if (pinnedId != null) {
            sourceRow.addView(Ui.textButton(a, "Reset", Ui.TEXT_DIM) {
                setPinnedVideo(a, spec, null); a.refresh()
            })
        }
        col.addView(sourceRow)

        // prescription as stat tiles
        col.addView(Ui.section(a, "Prescription"))
        val stats = Ui.row(a)
        fun tile(v: String, l: String) {
            val t = Ui.statTile(a, v, l)
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            lp.setMargins(Ui.dp(a, 2), 0, Ui.dp(a, 2), 0)
            t.layoutParams = lp
            stats.addView(t)
        }
        tile("${effective.sets}", if (effective.sets == 1) "set" else "sets")
        tile("${effective.reps}", "reps")
        if (effective.holdSeconds > 0) {
            tile(if (effective.holdSeconds >= 60) "${effective.holdSeconds / 60}m" else "${effective.holdSeconds}s", "hold")
        }
        tile("${a.store.exerciseSessions()}×", "per day")
        col.addView(stats)
        col.addView(Ui.fullWidth(Ui.textButton(a, "Change sets & reps") {
            a.pushOverlay { editOverride(a, spec) }
        }, a, 4))

        col.addView(Ui.section(a, "How to do it"))
        val cueCard = Ui.card(a)
        spec.cues.forEachIndexed { i, cue ->
            if (i > 0) cueCard.addView(Ui.spacer(a, 8))
            val r = Ui.row(a)
            val n = Ui.text(a, "${i + 1}", 13f, com.recoverwell.draw.Palette.ON_PRIMARY_CONTAINER, bold = true)
            n.background = Ui.rounded(Ui.PRIMARY_CONTAINER, 13f)
            n.setPadding(Ui.dp(a, 9), Ui.dp(a, 2), Ui.dp(a, 9), Ui.dp(a, 2))
            r.addView(n)
            val c = Ui.text(a, cue, 14.5f)
            c.setPadding(Ui.dp(a, 10), 0, 0, 0)
            r.addView(Ui.weight(c, 1f))
            cueCard.addView(r)
        }
        col.addView(cueCard)

        col.addView(Ui.section(a, "Why this matters"))
        val whyCard = Ui.card(a)
        whyCard.addView(Ui.text(a, spec.whyItMatters, 14.5f))
        col.addView(whyCard)

        val precCard = Ui.card(a, Ui.WARN_BG)
        val pr = Ui.row(a)
        pr.addView(Ui.icon(a, "ic_alert", 18, Ui.WARN))
        val pt = Ui.text(a, spec.precaution, 14f, Ui.WARN, bold = true)
        pt.setPadding(Ui.dp(a, 10), 0, 0, 0)
        pr.addView(Ui.weight(pt, 1f))
        precCard.addView(pr)
        col.addView(precCard)

        if (spec.phase == currentPhase) {
            col.addView(Ui.fullWidth(Ui.button(a, "Start guided session") {
                a.pushOverlay { guidedSession(a, spec) }
            }, a))
            val events = a.store.eventsOn(today)
            if (sessionSlot != null) {
                // opened from a specific Today session: one contextual control that
                // logs this exercise for that session, then returns to the session list
                val num = sessionSlot.removePrefix("session").toIntOrNull()
                val label = num?.let { "session $it" } ?: "this session"
                val done = events.lastOrNull {
                    it.refId == spec.id && it.slotKey == sessionSlot
                }?.status == EventStatus.DONE
                col.addView(Ui.fullWidth(
                    if (done) Ui.tonalButton(a, "Done for $label · undo") {
                        Forms.confirm(a, "Undo", "Mark this exercise as not done for $label?") {
                            Reminders.recordEvent(a, ScheduleEngine.ItemKind.EXERCISE, spec.id, sessionSlot, EventStatus.SKIPPED)
                            a.popOverlay()
                        }
                    } else Ui.button(a, "Mark done for $label") {
                        Reminders.recordEvent(a, ScheduleEngine.ItemKind.EXERCISE, spec.id, sessionSlot, EventStatus.DONE)
                        a.popOverlay()
                    }, a
                ))
            } else {
                col.addView(Ui.section(a, "Today's sessions"))
                for (session in 1..a.store.exerciseSessions()) {
                    val slot = "session$session"
                    val done = events.lastOrNull {
                        it.refId == spec.id && it.slotKey == slot
                    }?.status == EventStatus.DONE
                    col.addView(Ui.fullWidth(
                        if (done) Ui.tonalButton(a, "Session $session done · undo") {
                            Forms.confirm(a, "Undo", "Mark session $session as not done?") {
                                Reminders.recordEvent(a, ScheduleEngine.ItemKind.EXERCISE, spec.id, slot, EventStatus.SKIPPED)
                                a.refresh()
                            }
                        } else Ui.button(a, "Mark session $session done") {
                            Reminders.recordEvent(a, ScheduleEngine.ItemKind.EXERCISE, spec.id, slot, EventStatus.DONE)
                            a.popOverlay()
                        }, a
                    ))
                }
            }
        } else if (spec.phase > currentPhase) {
            val warn = Ui.card(a, Ui.WARN_BG)
            warn.addView(Ui.text(
                a, "Phase ${spec.phase} exercise - not unlocked yet. Doing it early risks re-rupture.",
                14f, Ui.WARN, bold = true
            ))
            col.addView(warn)
        }

        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    /** Pin (or clear) the user's chosen demonstration video, keeping prescription edits. */
    private fun setPinnedVideo(a: MainActivity, spec: ExerciseSpec, videoId: String?) {
        val existing = a.store.exerciseOverrides()[spec.id]
        val base = existing ?: ExerciseOverride(spec.id, null, null, null, null, true)
        a.store.saveExerciseOverride(base.copy(videoId = videoId))
    }

    private fun pinVideoDialog(a: MainActivity, spec: ExerciseSpec) {
        val input = Forms.editText(a, "", "Paste a YouTube link or video id")
        val pad = Ui.dp(a, 18)
        val holder = LinearLayout(a).apply { setPadding(pad, Ui.dp(a, 6), pad, 0); addView(input) }
        android.app.AlertDialog.Builder(a)
            .setTitle("Pin a video for \"${spec.name}\"")
            .setMessage("Paste any YouTube link (or 11-character id). It will always play for this " +
                "exercise and is saved in your backup. Find one you trust, then paste it here.")
            .setView(holder)
            .setPositiveButton("Pin") { _, _ ->
                val id = com.recoverwell.core.protocol.ExerciseVideo.parseVideoId(input.text.toString())
                if (id == null) {
                    Forms.info(a, "Couldn't read that link",
                        "Paste a normal YouTube link such as https://youtu.be/XXXXXXXXXXX or the 11-character id.")
                } else {
                    setPinnedVideo(a, spec, id)
                    a.refresh()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Guided session: counts reps set by set, runs hold countdowns, and logs
     * the session as done at the end. One giant tap target throughout.
     */
    private fun guidedSession(a: MainActivity, spec: ExerciseSpec): View {
        val effective = ScheduleEngine.mergedExercises(listOf(spec), a.store.exerciseOverrides())
            .firstOrNull() ?: spec
        val root = Ui.column(a)
        root.addView(Ui.backRow(a, spec.name) { a.popOverlay() })

        val demo = ExerciseDemoView(a)
        demo.demoId = spec.demoId
        val demoCard = Ui.frame(a)
        demoCard.background = Ui.rounded(Ui.SURFACE_HIGH)
        demoCard.clipToOutline = true
        demoCard.addView(demo, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(a, 170))
        root.addView(demoCard, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val stage = Ui.card(a)
        stage.gravity = android.view.Gravity.CENTER_HORIZONTAL
        root.addView(stage)
        val cue = Ui.caption(a, spec.cues.first())
        cue.gravity = android.view.Gravity.CENTER
        root.addView(Ui.fullWidth(cue, a, 4))

        var set = 1
        var rep = 0
        var timer: android.os.CountDownTimer? = null

        fun finish() {
            // log into the first session slot not yet done today
            val events = a.store.eventsOn(LocalDate.now())
            val slot = (1..a.store.exerciseSessions()).map { "session$it" }.firstOrNull { sl ->
                events.lastOrNull { it.refId == spec.id && it.slotKey == sl }?.status != EventStatus.DONE
            } ?: "session1"
            Reminders.recordEvent(a, ScheduleEngine.ItemKind.EXERCISE, spec.id, slot, EventStatus.DONE)
            a.popOverlay()
            a.refresh()
        }

        lateinit var render: () -> Unit
        fun repDone() {
            rep += 1
            if (rep >= effective.reps) {
                rep = 0
                set += 1
            }
            render()
        }

        render = {
            timer?.cancel()
            stage.removeAllViews()
            if (set > effective.sets) {
                stage.addView(Ui.headline(a, "Session complete"))
                stage.addView(Ui.spacer(a, 4))
                stage.addView(Ui.caption(a, "${effective.sets} sets of ${effective.reps} - nicely done"))
                stage.addView(Ui.fullWidth(Ui.button(a, "Finish & log session") { finish() }, a))
            } else {
                if (effective.sets > 1) {
                    stage.addView(Ui.setDots(a, effective.sets, set - 1))
                    stage.addView(Ui.spacer(a, 8))
                }
                stage.addView(Ui.pillBadge(a, "Set $set of ${effective.sets}",
                    com.recoverwell.draw.Palette.ON_PRIMARY_CONTAINER, Ui.PRIMARY_CONTAINER))
                stage.addView(Ui.spacer(a, 8))
                val big = Ui.text(a, "${rep + 1}", 56f, Ui.PRIMARY, bold = true)
                big.gravity = android.view.Gravity.CENTER
                stage.addView(big)
                stage.addView(Ui.caption(a, "of ${effective.reps} reps"))
                stage.addView(Ui.spacer(a, 10))
                if (effective.holdSeconds in 1..299) {
                    var holding = false
                    val btn = Ui.button(a, "Start ${effective.holdSeconds}s hold") {}
                    btn.setOnClickListener {
                        if (holding) return@setOnClickListener
                        holding = true
                        it.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                        timer = object : android.os.CountDownTimer(effective.holdSeconds * 1000L, 250L) {
                            override fun onTick(ms: Long) {
                                btn.text = "Hold... ${(ms / 1000) + 1}"
                            }
                            override fun onFinish() {
                                btn.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                                repDone()
                            }
                        }.start()
                    }
                    stage.addView(Ui.fullWidth(btn, a))
                } else {
                    stage.addView(Ui.fullWidth(Ui.button(a, "Rep done") {
                        stage.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                        repDone()
                    }, a))
                }
                if (rep == 0 && set > 1) {
                    stage.addView(Ui.spacer(a, 4))
                    stage.addView(Ui.caption(a, "Take a short rest before this set"))
                }
                stage.addView(Ui.fullWidth(Ui.textButton(a, "End early & log anyway") { finish() }, a, 2))
            }
        }
        render()
        return Ui.scroll(a, root)
    }

    private fun editOverride(a: MainActivity, spec: ExerciseSpec): View {
        val existing = a.store.exerciseOverrides()[spec.id]
        val effective = ScheduleEngine.mergedExercises(listOf(spec), a.store.exerciseOverrides()).firstOrNull() ?: spec
        var sets = effective.sets
        var reps = effective.reps
        var hold = effective.holdSeconds
        var enabled = existing?.enabled ?: true

        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Adjust") { a.popOverlay() })
        col.addView(Ui.title(a, spec.name))
        col.addView(Ui.spacer(a, 4))
        col.addView(Ui.caption(
            a, "Protocol default: ${ScheduleEngine.exercisePrescription(spec)} · done in each of " +
                "${a.store.exerciseSessions()} daily sessions. Change only as your physio advises."))
        col.addView(Ui.spacer(a, 8))
        val card = Ui.card(a)
        card.addView(Forms.stepper(a, "Sets", sets, 1, 10) { sets = it })
        card.addView(Forms.stepper(a, "Reps", reps, 1, 50) { reps = it })
        val holdStep = if (spec.holdSeconds >= 120) 30 else if (spec.holdSeconds >= 30) 5 else 1
        card.addView(Forms.stepper(a, "Hold (seconds)", hold, 0, 1800, step = holdStep) { hold = it })
        col.addView(card)

        col.addView(Ui.section(a, "Include in daily plan"))
        col.addView(Forms.toggle(a, enabled, "Enabled", "Disabled") { enabled = it })

        col.addView(Ui.spacer(a, 12))
        col.addView(Ui.fullWidth(Ui.button(a, "Save changes") {
            a.store.saveExerciseOverride(ExerciseOverride(spec.id, sets, reps, hold, null, enabled, existing?.videoId))
            a.popOverlay()
        }, a))
        col.addView(Ui.fullWidth(Ui.textButton(a, "Reset to protocol default") {
            // keep any pinned demonstration video; only the prescription resets
            a.store.saveExerciseOverride(ExerciseOverride(spec.id, null, null, null, null, true, existing?.videoId))
            a.popOverlay()
        }, a, 4))
        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }
}
