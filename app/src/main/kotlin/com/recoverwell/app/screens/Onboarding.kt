package com.recoverwell.app.screens

import android.view.View
import com.recoverwell.app.MainActivity
import com.recoverwell.app.notify.Reminders
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.protocol.ProtocolContent

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
        col.addView(Ui.title(a, "Welcome to RecoverWell"))
        col.addView(Ui.text(
            a,
            "Your daily coach for conservative (non-surgical) Achilles rupture " +
                "rehab - exercises, medication reminders, progress tracking and " +
                "safety guidance, all the way back to the padel court.",
            16f
        ))

        col.addView(Ui.spacer(a, 12))
        val disc = Ui.card(a, Ui.WARN_BG)
        disc.addView(Ui.text(a, "Before you start", 18f, Ui.WARN, bold = true))
        disc.addView(Ui.text(a, ProtocolContent.DISCLAIMER, 15f))
        disc.addView(Ui.text(
            a,
            "\nIn particular: phase timings in this app are typical-protocol " +
                "placeholders. Your physiotherapist's instructions always win.",
            15f
        ))
        col.addView(disc)

        val safety = Ui.card(a, Ui.DANGER_BG)
        safety.addView(Ui.text(a, "Safety first", 18f, Ui.DANGER, bold = true))
        safety.addView(Ui.text(
            a,
            "Achilles rupture carries a real risk of blood clots (DVT) - that is why " +
                "you take an anticoagulant. The Red Flags button stays at the top of " +
                "every screen. Read it once now so you know what to watch for.",
            15f
        ))
        safety.addView(Ui.fullWidth(Ui.dangerButton(a, "Read the red flags now") {
            a.pushOverlay { RedFlagsScreen.build(a) }
        }, a))
        col.addView(safety)

        col.addView(Ui.spacer(a, 12))
        col.addView(Ui.fullWidth(Ui.button(a, "I understand - it supports, never replaces, my clinicians") {
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
        banner.addView(Ui.text(a, "Step 1 of 2 · Check your details", 15f, Ui.PRIMARY, bold = true))
        banner.addView(Ui.text(
            a, "We have pre-filled what you told us - injury on 2 June 2026, left side, " +
                "conservative pathway, consultant seen 7 June 2026. Fix anything that is wrong.",
            14f, Ui.TEXT_DIM
        ))
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
        banner.addView(Ui.text(a, "Step 2 of 2 · Medication reminders", 15f, Ui.PRIMARY, bold = true))
        banner.addView(Ui.text(
            a, "Anticoagulant 2.5 mg is pre-loaded with reminders at 08:00 and 20:00. " +
                "Adjust the times to fit your routine.",
            14f, Ui.TEXT_DIM
        ))
        col.addView(banner)
        col.addView(MoreScreen.medsEditor(a) {
            a.store.saveProfile(a.store.profile().copy(onboardingComplete = true))
            Reminders.reschedule(a)
            a.show(MainActivity.Tab.TODAY)
        })
        return col
    }
}
