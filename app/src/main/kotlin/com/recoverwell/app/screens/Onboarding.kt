package com.recoverwell.app.screens

import android.view.View
import com.recoverwell.app.MainActivity
import com.recoverwell.app.notify.Reminders
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.protocol.ProtocolRegistry
import com.recoverwell.core.protocol.RehabFramework

/**
 * First-run flow, pre-filled with the user's real data (injury 2 June 2026,
 * left side, conservative pathway, consultant seen 7 June 2026, anticoagulant
 * 2.5 mg twice daily, goal: return to padel). Every field is editable here
 * and later under Settings.
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
        val disc = Ui.card(a, Ui.WARN_BG)
        disc.addView(Ui.text(a, "Before you start", 16f, Ui.WARN, bold = true))
        disc.addView(Ui.spacer(a, 4))
        disc.addView(Ui.text(a, RehabFramework.DISCLAIMER, 14.5f))
        col.addView(disc)

        val safety = Ui.card(a, Ui.DANGER_BG)
        val sr = Ui.row(a)
        sr.addView(Ui.iconBadge(a, "ic_alert", Ui.DANGER, 0x14B3261E, boxDp = 36))
        val st = Ui.text(a, protocol.safetyTitle, 16f, Ui.ON_DANGER_BG, bold = true)
        st.setPadding(Ui.dp(a, 12), 0, 0, 0)
        sr.addView(Ui.weight(st, 1f))
        safety.addView(sr)
        safety.addView(Ui.spacer(a, 6))
        safety.addView(Ui.text(a, protocol.safetyBlurb, 14.5f, Ui.ON_DANGER_BG))
        safety.addView(Ui.fullWidth(Ui.dangerButton(a, "Read the red flags") {
            a.pushOverlay { RedFlagsScreen.build(a) }
        }, a))
        col.addView(safety)

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
            a, "Pre-filled from what you told us - injury 2 June 2026, left side, " +
                "conservative pathway, consultant seen 7 June. Fix anything that is wrong."))
        col.addView(banner)
        col.addView(MoreScreen.profileEditor(a) {
            a.popOverlay()
            a.pushOverlay { stepMeds(a) }
        })
        return col
    }

    private fun stepMeds(a: MainActivity): View {
        val col = Ui.column(a, 0)
        val banner = Ui.column(a)
        banner.addView(Ui.pillBadge(a, "Step 2 of 2", Ui.ON_PRIMARY_CONTAINER, Ui.PRIMARY_CONTAINER))
        banner.addView(Ui.spacer(a, 6))
        banner.addView(Ui.headline(a, "Medication reminders"))
        banner.addView(Ui.caption(
            a, "Anticoagulant 2.5 mg is pre-loaded with reminders at 08:00 and 20:00. " +
                "Adjust the times to fit your routine."))
        col.addView(banner)
        col.addView(MoreScreen.medsEditor(a) {
            a.store.saveProfile(a.store.profile().copy(onboardingComplete = true))
            Reminders.reschedule(a)
            a.show(MainActivity.Tab.TODAY)
        })
        return col
    }
}
