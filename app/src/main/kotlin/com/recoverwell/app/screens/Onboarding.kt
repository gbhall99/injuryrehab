package com.recoverwell.app.screens

import android.view.View
import android.widget.LinearLayout
import com.recoverwell.app.MainActivity
import com.recoverwell.app.notify.Reminders
import com.recoverwell.app.ui.Forms
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.logic.ScheduleEngine
import com.recoverwell.core.protocol.ProtocolRegistry
import com.recoverwell.core.protocol.RehabFramework
import java.time.LocalTime

/**
 * First-run flow. Captures the user's own details (injury date, side, goal,
 * device) rather than assuming them; medications are added by explicit opt-in.
 * Defaults are neutral (today's date, blank goal) so the flow works for any
 * user. Every field is also editable later under More.
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
                "side and injury date - everything else can be adjusted later in More."))
        col.addView(banner)
        // the editor is a ScrollView: give it the remaining height (weight) so it
        // scrolls within itself, instead of overflowing and overlapping the banner
        col.addView(MoreScreen.profileEditor(a) {
            a.popOverlay()
            a.pushOverlay { stepMeds(a) }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        return col
    }

    private fun stepMeds(a: MainActivity): View {
        val col = Ui.column(a, 0)
        val banner = Ui.column(a)
        banner.addView(backLink(a) { a.popOverlay(); a.pushOverlay { stepProfile(a) } })
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
                    val seeded = proto.prefillMedications.map {
                        it.copy(
                            courseEndDate = injuryDate.plusWeeks(10),
                            reviewDate = injuryDate.plusWeeks(9)
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
                "in More › Reminders."))
            for (task in careTasks) {
                careCard.addView(Forms.label(a, task.title))
                careCard.addView(Forms.toggle(a, task.active) { on ->
                    a.store.saveTasks(a.store.tasks().map { if (it.id == task.id) it.copy(active = on) else it })
                    Reminders.reschedule(a)
                })
            }
            banner.addView(careCard)
        }
        col.addView(banner)
        col.addView(MoreScreen.medsEditor(a) {
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
     * this single moment - and everything here is editable later under More.
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
                "all of this later in More."))

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

        editor.addView(Ui.fullWidth(Ui.button(a, "Finish setup") {
            a.store.saveSetting("checkin_reminder",
                if (checkInOn) "%02d:%02d".format(checkInTime.hour, checkInTime.minute) else "off")
            a.store.saveSetting("exercise_reminder",
                if (exerciseOn) "%02d:%02d".format(exerciseTime.hour, exerciseTime.minute) else "off")
            a.store.saveExerciseSessions(sessions)
            a.store.saveProfile(a.store.profile().copy(onboardingComplete = true))
            Reminders.reschedule(a)
            a.show(MainActivity.Tab.TODAY)
        }, a))
        editor.addView(Ui.spacer(a, 24))

        col.addView(banner)
        col.addView(Ui.scroll(a, editor),
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        return col
    }
}
