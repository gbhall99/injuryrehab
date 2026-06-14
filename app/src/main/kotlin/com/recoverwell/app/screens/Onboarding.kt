package com.recoverwell.app.screens

import android.view.View
import android.widget.LinearLayout
import com.recoverwell.app.MainActivity
import com.recoverwell.app.notify.Reminders
import com.recoverwell.app.ui.Forms
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.protocol.ProtocolRegistry
import com.recoverwell.core.protocol.RehabFramework

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

    private fun stepProfile(a: MainActivity): View {
        val col = Ui.column(a, 0)
        val banner = Ui.column(a)
        banner.addView(Ui.pillBadge(a, "Step 1 of 2", Ui.ON_PRIMARY_CONTAINER, Ui.PRIMARY_CONTAINER))
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
        banner.addView(Ui.pillBadge(a, "Step 2 of 2", Ui.ON_PRIMARY_CONTAINER, Ui.PRIMARY_CONTAINER))
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
                card.addView(Ui.text(a, "Some people on a conservative Achilles pathway are prescribed " +
                    "an anticoagulant. Add it if that's you, then adjust the dose and times to match " +
                    "your prescription.", 14f, Ui.ON_INFO_BG))
                card.addView(Ui.fullWidth(Ui.tonalButton(a, "Add an anticoagulant reminder") {
                    a.store.saveMedications(a.store.medications() + proto.prefillMedications)
                    Reminders.reschedule(a)
                    a.refresh()
                }, a))
                banner.addView(card)
            }
        }
        col.addView(banner)
        col.addView(MoreScreen.medsEditor(a) {
            a.store.saveProfile(a.store.profile().copy(onboardingComplete = true))
            Reminders.reschedule(a)
            a.show(MainActivity.Tab.TODAY)
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        return col
    }
}
