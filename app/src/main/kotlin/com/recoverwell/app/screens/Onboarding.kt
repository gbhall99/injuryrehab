package com.recoverwell.app.screens

import android.view.View
import android.widget.LinearLayout
import com.recoverwell.app.MainActivity
import com.recoverwell.app.notify.Reminders
import com.recoverwell.app.ui.Forms
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.logic.PhaseEngine
import com.recoverwell.core.logic.ScheduleEngine
import com.recoverwell.core.protocol.ProtocolRegistry
import com.recoverwell.core.protocol.RehabFramework
import java.time.LocalDate
import java.time.LocalTime

/**
 * First-run flow. Captures the user's own details (injury date, side, goal,
 * device) rather than assuming them; medications are added by explicit opt-in.
 * Defaults are neutral (today's date, blank goal) so the flow works for any
 * user. Every field is also editable later under Settings.
 */
object Onboarding {

    fun build(a: MainActivity): View {
        val col = Ui.column(a)
        col.addView(Ui.spacer(a, 24))
        col.addView(Ui.heroBadge(a, "ic_leg", boxDp = 72))
        col.addView(Ui.spacer(a, 14))
        col.addView(Ui.text(a, "Welcome to", 16f, Ui.TEXT_DIM))
        col.addView(Ui.display(a, "RecoverWell"))
        col.addView(Ui.spacer(a, 4))
        val protocol = ProtocolRegistry.forProfile(a.store.profile())
        col.addView(Ui.body(a, protocol.welcomeBlurb))

        col.addView(Ui.spacer(a, 12))
        // one-line acknowledgement up front; the full disclaimer is one tap away
        // (and stays permanently in the footer strip and About) so the first
        // screen is a light read, not a wall of cards
        col.addView(Ui.text(a, "RecoverWell supports - never replaces - your physio and consultant.",
            14.5f, Ui.TEXT_DIM))

        col.addView(Ui.spacer(a, 12))
        val safety = Ui.card(a, Ui.DANGER_BG)
        val sr = Ui.row(a)
        sr.addView(Ui.iconBadge(a, "ic_alert", Ui.DANGER, 0x14B3261E, boxDp = 36))
        val st = Ui.text(a, protocol.safetyTitle, 16f, Ui.ON_DANGER_BG, bold = true)
        st.setPadding(Ui.dp(a, 12), 0, 0, 0)
        sr.addView(Ui.weight(st, 1f))
        safety.addView(sr)
        safety.addView(Ui.spacer(a, 6))
        safety.addView(Ui.text(a, protocol.safetyBlurb, 14.5f, Ui.ON_DANGER_BG))
        safety.addView(Ui.fullWidth(Ui.dangerButton(a, "Open red flags") {
            a.pushOverlay("Red flags") { RedFlagsScreen.build(a) }
        }, a))
        col.addView(safety)

        col.addView(Ui.fullWidth(Ui.textButton(a, "Read the full disclaimer") {
            Forms.info(a, "Medical disclaimer", RehabFramework.DISCLAIMER)
        }, a, 4))

        col.addView(Ui.spacer(a, 12))
        col.addView(Ui.fullWidth(Ui.button(a, "I understand - continue") {
            a.store.saveProfile(a.store.profile().copy(disclaimerAcknowledged = true))
            a.popOverlay()
            a.pushOverlay { stepProfile(a) }
        }, a))
        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    /** A compact left-aligned "‹ Back" link for stepping back through onboarding. */
    private fun backLink(a: MainActivity, onBack: () -> Unit): View =
        Ui.text(a, "‹ Back", 14f, Ui.PRIMARY, bold = true).apply {
            setPadding(Ui.dp(a, 2), Ui.dp(a, 4), Ui.dp(a, 12), Ui.dp(a, 6))
            isClickable = true
            isFocusable = true
            contentDescription = "Back to the previous step"
            background = Ui.ripple(a, Ui.rounded(0x00000000, 20f))
            setOnClickListener { onBack() }
        }

    private fun stepProfile(a: MainActivity): View {
        val col = Ui.column(a, 0)
        val banner = Ui.column(a)
        banner.addView(Ui.pillBadge(a, "Step 1 of 3", Ui.ON_PRIMARY_CONTAINER, Ui.PRIMARY_CONTAINER))
        banner.addView(Ui.spacer(a, 6))
        banner.addView(Ui.headline(a, "Check your details"))
        banner.addView(Ui.caption(
            a, "Tell us about your injury and what you're working back to. Pick your " +
                "side and injury date - everything else can be adjusted later in Settings."))
        col.addView(banner)
        // the editor is a ScrollView: give it the remaining height (weight) so it
        // scrolls within itself, instead of overflowing and overlapping the banner
        col.addView(MoreScreen.profileEditor(a) {
            a.popOverlay()
            // someone joining mid-recovery says where their physio has them, so
            // the plan doesn't restart them on week-one boot exercises
            a.pushOverlay { if (joiningMidRecovery(a)) stepWhereNow(a) else stepMeds(a) }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        return col
    }

    /** True when the injury date is far enough back that the typical timeline is past phase 1. */
    private fun joiningMidRecovery(a: MainActivity): Boolean =
        PhaseEngine.dateEligiblePhase(a.store.profile(), LocalDate.now()) > 1

    /**
     * For people who start using the app part-way through: pick the stage the
     * physio has actually put them in (defaulting to the typical one for their
     * injury date) and whether they're still in the boot. Confirmation dates are
     * left unknown so "your pace" isn't skewed by the install date.
     */
    private fun stepWhereNow(a: MainActivity): View {
        val today = LocalDate.now()
        val profile = a.store.profile()
        val protocol = ProtocolRegistry.forProfile(profile)
        val typical = PhaseEngine.dateEligiblePhase(profile, today)
        val weeks = PhaseEngine.weeksSinceInjury(profile, today)
        val device = ProtocolRegistry.deviceFor(profile)
        var chosen = profile.physioConfirmedPhase.takeIf { it in 2..typical } ?: typical
        var inDevice = profile.bootWeanedDate == null && chosen <= 3
        var setting = if (profile.physioConfirmedPhase > 1) profile.currentWedges
            else profile.wedgePlan.expectedWedges(profile.injuryDate, today, profile.wedgeDateOverrides)
        var weanedOn = profile.bootWeanedDate ?: minOf(today, profile.injuryDate.plusWeeks(10))

        val col = Ui.column(a, 0)
        val banner = Ui.column(a)
        banner.addView(backLink(a) { a.popOverlay(); a.pushOverlay { stepProfile(a) } })
        banner.addView(Ui.pillBadge(a, "Step 1 of 3", Ui.ON_PRIMARY_CONTAINER, Ui.PRIMARY_CONTAINER))
        banner.addView(Ui.spacer(a, 6))
        banner.addView(Ui.headline(a, "Where are you now?"))
        banner.addView(Ui.caption(a, "You're about $weeks weeks in. Pick the stage your physio or clinic has " +
            "actually moved you to - your exercises, checks and reminders start from there."))
        col.addView(banner)

        val editor = Ui.column(a)
        val phases = Ui.card(a)
        val deviceCard = Ui.card(a)
        lateinit var rebuild: () -> Unit
        rebuild = {
            phases.removeAllViews()
            for (ph in protocol.phases.filter { it.number <= typical }) {
                val selected = ph.number == chosen
                val row = Ui.row(a)
                row.setPadding(0, Ui.dp(a, 8), 0, Ui.dp(a, 8))
                row.isClickable = true
                row.isFocusable = true
                row.minimumHeight = Ui.dp(a, Ui.MIN_TOUCH_DP)
                row.contentDescription = "Phase ${ph.number}, ${ph.title}" + if (selected) ", selected" else ""
                row.background = Ui.ripple(a, Ui.rounded(0x00000000, 12f))
                row.addView(Ui.iconBadge(a, if (selected) "ic_check" else "ic_flag",
                    if (selected) Ui.ON_PRIMARY_CONTAINER else Ui.TEXT_DIM,
                    if (selected) Ui.PRIMARY_CONTAINER else Ui.SURFACE_HIGH, boxDp = 34))
                val texts = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
                texts.setPadding(Ui.dp(a, 12), 0, 0, 0)
                texts.addView(Ui.text(a, "Phase ${ph.number} · ${ph.title}", 15f, Ui.TEXT, bold = selected))
                texts.addView(Ui.caption(a, ph.subtitle + if (ph.number == typical) " · typical for your date" else ""))
                row.addView(Ui.weight(texts, 1f))
                row.setOnClickListener {
                    chosen = ph.number
                    inDevice = when {
                        chosen <= 2 -> true
                        chosen >= 4 -> false
                        else -> inDevice
                    }
                    rebuild()
                }
                phases.addView(row)
            }
            deviceCard.removeAllViews()
            if (device != null) {
                val name = device.name.lowercase()
                deviceCard.addView(Forms.label(a, "Still wearing the $name?"))
                deviceCard.addView(Forms.toggle(a, inDevice) { on -> inDevice = on; rebuild() })
                if (inDevice && device.kind != com.recoverwell.core.protocol.DeviceKind.CAST) {
                    deviceCard.addView(Forms.stepper(a, "Setting now (${device.unitNamePlural})",
                        setting, 0, device.maxValue, step = device.plan.stepSize.coerceAtLeast(1)) { setting = it })
                    deviceCard.addView(Ui.caption(a, "Your plan would expect about " +
                        device.format(profile.wedgePlan.expectedWedges(profile.injuryDate, today,
                            profile.wedgeDateOverrides)) + " around now - enter what yours is actually set to."))
                } else if (!inDevice) {
                    deviceCard.addView(Forms.dateRow(a, "Out of it since", weanedOn) { weanedOn = it.coerceAtMost(today) })
                    deviceCard.addView(Ui.caption(a, "Boot checks and boot-change reminders won't be scheduled."))
                }
            }
        }
        rebuild()
        editor.addView(phases)
        if (device != null) editor.addView(deviceCard)
        val confirm = Ui.fullWidth(Ui.button(a, "Confirm & continue") {
            val p = a.store.profile()
            a.store.saveProfile(p.copy(
                physioConfirmedPhase = chosen,
                // when each phase was confirmed is unknown - leave it out rather than
                // stamping today, which would make "your pace" read weeks behind
                phaseConfirmedDates = emptyMap(),
                currentWedges = if (inDevice) setting else p.currentWedges,
                bootWeanedDate = if (inDevice) null else weanedOn
            ))
            Reminders.reschedule(a)
            a.popOverlay()
            a.pushOverlay { stepMeds(a) }
        }, a, 8)
        editor.addView(Ui.spacer(a, 24))
        // every setup step keeps its button in view, however long the step
        col.addView(Ui.withActionBar(a, Ui.scroll(a, editor), confirm),
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        return col
    }

    private fun stepMeds(a: MainActivity): View {
        val col = Ui.column(a, 0)
        val banner = Ui.column(a)
        banner.addView(backLink(a) {
            a.popOverlay(); a.pushOverlay { if (joiningMidRecovery(a)) stepWhereNow(a) else stepProfile(a) }
        })
        banner.addView(Ui.pillBadge(a, "Step 2 of 3", Ui.ON_PRIMARY_CONTAINER, Ui.PRIMARY_CONTAINER))
        banner.addView(Ui.spacer(a, 6))
        banner.addView(Ui.headline(a, "Medication reminders"))
        banner.addView(Ui.caption(
            a, "Add any medication you take and it gets a reminder with one-tap taken/missed " +
                "logging. You can skip this and add medications later."))
        // explicit opt-in for the protocol's typical medication, instead of
        // pre-loading a prescription nobody confirmed
        if (a.store.medications().isEmpty()) {
            val proto = com.recoverwell.core.protocol.ProtocolRegistry.default
            if (proto.prefillMedications.isNotEmpty()) {
                val card = Ui.card(a, Ui.INFO_BG)
                card.addView(Ui.text(a, "On a blood thinner?", 15.5f, Ui.ON_INFO_BG, bold = true))
                card.addView(Ui.spacer(a, 2))
                card.addView(Ui.text(a, "Some people recovering from an Achilles rupture are prescribed a " +
                    "blood-thinner to prevent clots. Add it if that's you, then adjust the dose and times " +
                    "to match your prescription.", 14f, Ui.ON_INFO_BG))
                card.addView(Ui.fullWidth(Ui.tonalButton(a, "Add a blood-thinner reminder") {
                    // Seed an editable, clinician-confirmable course end tied to the
                    // typical boot period rather than letting reminders run forever
                    // (VTE prophylaxis is time-limited - NICE NG89).
                    val injuryDate = a.store.profile().injuryDate
                    val today = LocalDate.now()
                    val typicalEnd = injuryDate.plusWeeks(
                        com.recoverwell.core.model.Medication.TYPICAL_COURSE_WEEKS)
                    val seeded = proto.prefillMedications.map {
                        if (!typicalEnd.isAfter(today)) {
                            // joining after the typical course would have ended: someone adding it
                            // is still taking it, so never seed an end date in the past (reminders
                            // would silently never fire) - ask them to confirm the end in a week
                            it.copy(courseEndDate = null, reviewDate = today.plusDays(7))
                        } else it.copy(
                            courseEndDate = typicalEnd,
                            reviewDate = injuryDate.plusWeeks(
                                com.recoverwell.core.model.Medication.TYPICAL_REVIEW_WEEKS)
                        )
                    }
                    a.store.saveMedications(a.store.medications() + seeded)
                    Reminders.reschedule(a)
                    a.refresh()
                }, a))
                banner.addView(card)
            }
        }
        // pick-at-setup: let the user choose which daily-care reminders apply,
        // so the daily list is theirs rather than a wall of defaults
        val careTasks = a.store.tasks()
        if (careTasks.isNotEmpty()) {
            val careCard = Ui.card(a)
            careCard.addView(Ui.text(a, "Daily care reminders", 15.5f, Ui.TEXT, bold = true))
            careCard.addView(Ui.caption(a, "Turn off any that don't apply - you can change these any time " +
                "in Settings › Reminders."))
            for (task in careTasks) {
                careCard.addView(Forms.label(a, task.title))
                careCard.addView(Forms.toggle(a, task.active) { on ->
                    a.store.saveTasks(a.store.tasks().map { if (it.id == task.id) it.copy(active = on) else it })
                    Reminders.reschedule(a)
                })
            }
            banner.addView(careCard)
        }
        // the intro scrolls with the list, so "Confirm & continue" is never pushed off-screen
        col.addView(MoreScreen.medsEditor(a, header = banner) {
            a.popOverlay()
            a.pushOverlay { stepRoutine(a) }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        return col
    }

    /**
     * Final setup step: pick the daily rhythm. The once-a-day check-in is the
     * habit anchor of recovery, so it is offered on by default; the exercise
     * nudge and the number of daily sessions (1-3) are set here too. Medication
     * reminders stay on their own clinically-timed schedule - never folded into
     * this single moment - and everything here is editable later under Settings.
     */
    private fun stepRoutine(a: MainActivity): View {
        val col = Ui.column(a, 0)
        val banner = Ui.column(a)
        banner.addView(backLink(a) { a.popOverlay(); a.pushOverlay { stepMeds(a) } })
        banner.addView(Ui.pillBadge(a, "Step 3 of 3", Ui.ON_PRIMARY_CONTAINER, Ui.PRIMARY_CONTAINER))
        banner.addView(Ui.spacer(a, 6))
        banner.addView(Ui.headline(a, "Set your daily rhythm"))
        banner.addView(Ui.caption(
            a, "One daily check-in anchors your recovery - it keeps your trends accurate and " +
                "is the home base for the day. Pick times that fit your life; you can change " +
                "all of this later in Settings."))

        val editor = Ui.column(a)

        // --- daily check-in (the anchor - on by default) ---
        var checkInOn = true
        var checkInTime = LocalTime.of(20, 0)
        val ciCard = Ui.card(a)
        ciCard.addView(Ui.text(a, "Daily check-in", 15.5f, Ui.TEXT, bold = true))
        ciCard.addView(Ui.caption(a, "A 10-second \"how does it feel?\" - log your pain in one tap, " +
            "straight from the notification."))
        val ciTimeCard = Ui.card(a)
        fun rebuildCi() {
            ciTimeCard.removeAllViews()
            if (checkInOn) ciTimeCard.addView(Forms.timeButton(a, checkInTime) { checkInTime = it })
            else ciTimeCard.addView(Ui.caption(a, "Off - turn it on to pick a time."))
        }
        ciCard.addView(Forms.choiceRow(a, listOf(true, false), { if (it) "On" else "Off" }, checkInOn) {
            checkInOn = it; rebuildCi()
        })
        rebuildCi()
        ciCard.addView(ciTimeCard)
        editor.addView(ciCard)

        // --- exercise reminder (on by default) ---
        var exerciseOn = true
        var exerciseTime = LocalTime.of(10, 0)
        val exCard = Ui.card(a)
        exCard.addView(Ui.text(a, "Exercise reminder", 15.5f, Ui.TEXT, bold = true))
        exCard.addView(Ui.caption(a, "A gentle nudge to run through your rehab exercises - only on days " +
            "your phase has them."))
        val exTimeCard = Ui.card(a)
        fun rebuildEx() {
            exTimeCard.removeAllViews()
            if (exerciseOn) exTimeCard.addView(Forms.timeButton(a, exerciseTime) { exerciseTime = it })
            else exTimeCard.addView(Ui.caption(a, "Off - turn it on to pick a time."))
        }
        exCard.addView(Forms.choiceRow(a, listOf(true, false), { if (it) "On" else "Off" }, exerciseOn) {
            exerciseOn = it; rebuildEx()
        })
        rebuildEx()
        exCard.addView(exTimeCard)
        editor.addView(exCard)

        // --- exercise sessions per day (1-3) ---
        var sessions = ScheduleEngine.EXERCISE_SESSIONS_PER_DAY
        val sessCard = Ui.card(a)
        sessCard.addView(Ui.text(a, "Exercise sessions per day", 15.5f, Ui.TEXT, bold = true))
        sessCard.addView(Ui.caption(a, "How many times a day you'll run the full routine. Little and often " +
            "rebuilds the tendon - three is typical; start lower if that fits better."))
        sessCard.addView(Forms.choiceRow(a,
            (ScheduleEngine.MIN_EXERCISE_SESSIONS..ScheduleEngine.MAX_EXERCISE_SESSIONS).toList(),
            { "$it" }, sessions) { sessions = it })
        editor.addView(sessCard)

        // --- meds stay separate (reassurance, matches the hybrid model) ---
        val note = Ui.card(a, Ui.INFO_BG)
        note.addView(Ui.text(a, "Medication reminders stay separate", 14.5f, Ui.ON_INFO_BG, bold = true))
        note.addView(Ui.spacer(a, 2))
        note.addView(Ui.text(a, "Any medication you added keeps its own on-time reminders - they're never " +
            "folded into the daily check-in, so a dose is never missed.", 13.5f, Ui.ON_INFO_BG))
        editor.addView(note)

        val finish = Ui.fullWidth(Ui.button(a, "Finish setup") {
            a.store.saveSetting("checkin_reminder",
                if (checkInOn) "%02d:%02d".format(checkInTime.hour, checkInTime.minute) else "off")
            a.store.saveSetting("exercise_reminder",
                if (exerciseOn) "%02d:%02d".format(exerciseTime.hour, exerciseTime.minute) else "off")
            a.store.saveExerciseSessions(sessions)
            a.store.saveProfile(a.store.profile().copy(onboardingComplete = true))
            Reminders.reschedule(a)
            a.show(MainActivity.Tab.TODAY)
        }, a, 8)
        editor.addView(Ui.spacer(a, 24))

        col.addView(banner)
        col.addView(Ui.withActionBar(a, Ui.scroll(a, editor), finish),
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        return col
    }
}
