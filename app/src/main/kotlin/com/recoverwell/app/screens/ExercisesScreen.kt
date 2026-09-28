package com.recoverwell.app.screens

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.recoverwell.app.MainActivity
import com.recoverwell.app.notify.Reminders
import com.recoverwell.app.ui.Forms
import com.recoverwell.app.ui.OwnClips
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
        // the thing people come here to do: start today's next session in one tap
        col.addView(todaySessionCard(a, profile, today))
        col.addView(Ui.spacer(a, 6))
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
            col.addView(Ui.caption(a, "Each dose below is for one session. Open an exercise to see how many of " +
                "your daily sessions it's in."))
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

    /** Next incomplete session today with a one-tap start, or an all-done note. */
    private fun todaySessionCard(a: MainActivity, profile: com.recoverwell.core.model.Profile, today: LocalDate): View {
        val plan = ScheduleEngine.sessionPlan(profile, a.store.exerciseOverrides(), today, a.store.exerciseSessions())
        val events = a.store.eventsOn(today)
        val next = plan.map { (n, exs) ->
            val slot = ScheduleEngine.sessionSlot(n)
            Triple(n, slot, exs.filter { ex ->
                events.lastOrNull { it.refId == ex.id && it.slotKey == slot }?.status != EventStatus.DONE
            })
        }.firstOrNull { it.third.isNotEmpty() }
        val card = Ui.card(a, if (next == null) Ui.DONE_BG else Ui.PRIMARY_CONTAINER)
        if (plan.isEmpty()) {
            card.addView(Ui.text(a, "No exercises scheduled today", 15.5f, Ui.TEXT, bold = true))
            card.addView(Ui.caption(a, "Rest day for alternate-day work - keep up your daily care."))
        } else if (next == null) {
            card.addView(Ui.text(a, "Today's exercise sessions are done", 15.5f, Ui.DONE, bold = true))
            card.addView(Ui.caption(a, "${plan.size} of ${plan.size} complete - nicely done."))
        } else {
            val (n, slot, remaining) = next
            card.addView(Ui.text(a, "Today · session $n of ${plan.size}", 15.5f, Ui.ON_PRIMARY_CONTAINER, bold = true))
            card.addView(Ui.text(a, "${remaining.size} exercise${if (remaining.size == 1) "" else "s"} to go · " +
                "one tap per set", 13.5f, Ui.ON_PRIMARY_CONTAINER))
            card.addView(Ui.fullWidth(Ui.button(a, "Start session $n") {
                SessionPlayer.open(a, "Exercise session $n", slot, remaining.map { it.id })
            }, a))
        }
        return card
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

        val playInApp = a.store.setting("video_inapp", "true") != "false"

        // animated quick reference (offline); a play overlay opens a real video
        val demoCard = Ui.frame(a)
        demoCard.background = Ui.rounded(Ui.SURFACE_HIGH)
        demoCard.clipToOutline = true
        // the user's own offline clip, when they've added one, replaces the animation
        val ownClip = OwnClips.clipFor(a, spec) != null
        demoCard.addView(OwnClips.demoView(a, spec), ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(a, 230))
        // "Quick reference" tag, top-left
        val tag = Ui.text(a, if (ownClip) "Your clip" else "Quick reference", 11.5f, Ui.TEXT_DIM, bold = true)
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
        watchRow.setOnClickListener { VideoScreen.watch(a, spec) }
        watchRow.addView(Ui.icon(a, "ic_play", 20, com.recoverwell.draw.Palette.ON_PRIMARY))
        val wlabel = Ui.text(a, "Watch video demonstration", 15.5f, com.recoverwell.draw.Palette.ON_PRIMARY, bold = true)
        wlabel.setPadding(Ui.dp(a, 10), 0, 0, 0)
        watchRow.addView(Ui.weight(wlabel, 1f))
        watchRow.addView(Ui.text(a, if (playInApp) "In-app" else "YouTube",
            12f, com.recoverwell.draw.Palette.withAlpha(com.recoverwell.draw.Palette.ON_PRIMARY, 0xCC)))
        // no safe YouTube search exists for some exercises: say why instead of offering one
        if (VideoScreen.hasVideo(a, spec)) col.addView(Ui.fullWidth(watchRow, a, 10))
        else col.addView(VideoScreen.noVideoCard(a, spec))


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
        if (effective.isTimed) {
            // timed work reads as rounds × duration, never "1 set · 1 rep · 10m hold"
            if (effective.sets > 1) tile("${effective.sets}", "sets")
            tile("${effective.reps}", if (effective.reps == 1) "round" else "rounds")
            tile(ScheduleEngine.durationLabel(effective.holdSeconds).replace(" min", "m"), "each")
        } else {
            tile("${effective.sets}", if (effective.sets == 1) "set" else "sets")
            tile("${effective.reps}", "reps")
            if (effective.holdSeconds > 0) tile("${effective.holdSeconds}s", "hold")
        }
        // how often THIS exercise is done: its own dose, capped by the user's daily sessions
        val perDay = minOf(effective.sessionsPerDay.coerceAtLeast(1), a.store.exerciseSessions())
        tile(if (effective.intervalDays > 1) "Alt" else "${perDay}×",
            if (effective.intervalDays > 1) "days" else "per day")
        col.addView(stats)
        col.addView(Ui.fullWidth(Ui.textButton(a, "Adjust dose or video") {
            a.pushOverlay("Adjust ${spec.name}") { editOverride(a, spec) }
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

        // the pain-monitoring rule: how much discomfort is acceptable, and when to back off
        val painRule = ProtocolRegistry.forProfile(a.store.profile()).exercisePainRule
        if (painRule.isNotBlank()) {
            val ruleCard = Ui.card(a, Ui.INFO_BG)
            val rr = Ui.row(a)
            rr.gravity = android.view.Gravity.TOP
            rr.addView(Ui.icon(a, "ic_pulse", 18, Ui.ON_INFO_BG))
            val rt = Ui.text(a, painRule, 14f, Ui.ON_INFO_BG)
            rt.setPadding(Ui.dp(a, 10), 0, 0, 0)
            rr.addView(Ui.weight(rt, 1f))
            ruleCard.addView(rr)
            col.addView(ruleCard)
        }

        if (spec.phase == currentPhase) {
            col.addView(Ui.fullWidth(Ui.button(a, "Start guided session") {
                // log into the first of today's sessions containing this exercise that isn't done yet
                val events = a.store.eventsOn(today)
                val slots = ScheduleEngine.sessionPlan(a.store.profile(), overrides, today,
                    a.store.exerciseSessions()).filter { (_, exs) -> exs.any { it.id == spec.id } }
                    .map { ScheduleEngine.sessionSlot(it.first) }.ifEmpty { listOf(ScheduleEngine.sessionSlot(1)) }
                val slot = slots.firstOrNull { sl ->
                    events.lastOrNull { it.refId == spec.id && it.slotKey == sl }?.status != EventStatus.DONE
                } ?: slots.first()
                SessionPlayer.open(a, "Guided session", slot, listOf(spec.id))
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
                // only the sessions this exercise is actually in today (dose + alternate days)
                val sessionsToday = ScheduleEngine.sessionPlan(a.store.profile(), overrides, today,
                    a.store.exerciseSessions()).filter { (_, exs) -> exs.any { it.id == spec.id } }.map { it.first }
                if (sessionsToday.isEmpty()) {
                    col.addView(Ui.caption(a, if (effective.intervalDays > 1)
                        "Not scheduled today - this one is done on alternate days to give the tendon a recovery day."
                    else "Not in today's plan."))
                }
                for (session in sessionsToday) {
                    val slot = ScheduleEngine.sessionSlot(session)
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

    private fun editOverride(a: MainActivity, spec: ExerciseSpec): View {
        val existing = a.store.exerciseOverrides()[spec.id]
        val effective = ScheduleEngine.mergedExercises(listOf(spec), a.store.exerciseOverrides()).firstOrNull() ?: spec
        var sets = effective.sets
        var reps = effective.reps
        var hold = effective.holdSeconds
        var perDay = effective.sessionsPerDay.coerceIn(1, ScheduleEngine.MAX_EXERCISE_SESSIONS)
        var enabled = existing?.enabled ?: true

        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Adjust") { a.popOverlay() })
        col.addView(Ui.title(a, spec.name))
        col.addView(Ui.spacer(a, 4))
        col.addView(Ui.caption(
            a, "Protocol default: ${ScheduleEngine.exercisePrescription(spec)}, " +
                "${spec.sessionsPerDay.coerceAtMost(ScheduleEngine.MAX_EXERCISE_SESSIONS)}× a day. " +
                "Change only as your physio advises."))
        col.addView(Ui.spacer(a, 8))
        val card = Ui.card(a)
        card.addView(Forms.stepper(a, "Sets", sets, 1, 10) { sets = it })
        card.addView(Forms.stepper(a, "Reps", reps, 1, 50) { reps = it })
        val holdStep = if (spec.holdSeconds >= 120) 30 else if (spec.holdSeconds >= 30) 5 else 1
        card.addView(Forms.stepper(a, if (spec.isTimed) "Time each (seconds)" else "Hold (seconds)",
            hold, 0, 3600, step = holdStep) { hold = it })
        card.addView(Forms.stepper(a, "Times a day", perDay, 1, ScheduleEngine.MAX_EXERCISE_SESSIONS) { perDay = it })
        card.addView(Ui.caption(a, "Capped by your number of daily sessions " +
            "(${a.store.exerciseSessions()} - change under More › Reminders › Exercise reminders)."))
        col.addView(card)

        col.addView(Ui.section(a, "Include in daily plan"))
        col.addView(Forms.toggle(a, enabled, "Enabled", "Disabled") { enabled = it })

        // pinning a specific demonstration is a rare, set-once choice - it lives here,
        // not on the exercise screen people read every day
        col.addView(Ui.section(a, "Demonstration video"))
        val pinned = existing?.videoId
        val suggested = com.recoverwell.core.protocol.ExerciseVideo.suggested[spec.id].orEmpty().isNotEmpty()
        col.addView(Ui.caption(a, when {
            pinned != null -> "Your pinned video always plays for this exercise."
            suggested -> "\"Watch video\" plays a suggested demonstration - tap \"Use this video\" there to keep " +
                "it, or paste one your physio recommends. The animation always works offline."
            else -> "\"Watch video\" shows YouTube results for this exercise. Paste one your physio recommends " +
                "to always play it. The animation always works offline."
        }))
        if (pinned != null) {
            col.addView(Ui.buttonPair(a,
                Ui.tonalButton(a, "Change video") { pinVideoDialog(a, spec) },
                Ui.textButton(a, "Remove my video", Ui.TEXT_DIM) { setPinnedVideo(a, spec, null); a.refresh() }))
        } else {
            col.addView(Ui.fullWidth(Ui.tonalButton(a, "Use a specific video") { pinVideoDialog(a, spec) }, a))
        }
        // own clips: say exactly which file this exercise looks for
        if (OwnClips.enabled(a)) {
            col.addView(Ui.caption(a, if (OwnClips.clipFor(a, spec) != null) "Your own clip replaces the animation."
                else "Own clip: add ${spec.demoId}.mp4 (or ${spec.id}.mp4) to your clips folder."))
        }

        col.addView(Ui.spacer(a, 12))
        col.addView(Ui.fullWidth(Ui.button(a, "Save changes") {
            a.store.saveExerciseOverride(ExerciseOverride(spec.id, sets, reps, hold,
                perDay.takeIf { it != spec.sessionsPerDay }, enabled, existing?.videoId))
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
