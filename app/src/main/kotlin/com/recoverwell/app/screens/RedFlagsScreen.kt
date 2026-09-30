package com.recoverwell.app.screens

import android.view.View
import com.recoverwell.app.MainActivity
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.protocol.ProtocolRegistry
import com.recoverwell.core.protocol.RehabFramework

/**
 * Red-flag guidance: DVT, PE, re-rupture, anticoagulant bleeding, boot/skin.
 * Reachable in one tap from the persistent header button on every screen.
 */
object RedFlagsScreen {

    fun build(a: MainActivity): View {
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Red flags") { a.popOverlay() })
        val protocol = ProtocolRegistry.forProfile(a.store.profile())

        // one card per warning, most urgent first, in the NHS pattern: what to do (and the
        // right call, one tap) first, then the signs it's for, then why. Safety content is
        // never folded away - every sign stays in view (it used to be listed twice: a quick
        // "which one is it?" list, then all the signs again).
        for (rf in protocol.redFlags) {
            val urgent = rf.id == "pe"
            val fg = if (urgent) Ui.ON_DANGER_BG else Ui.TEXT
            val card = Ui.card(a, if (urgent) Ui.DANGER_BG else Ui.CARD)
            val head = Ui.row(a)
            head.addView(Ui.iconBadge(a, if (rf.id == "rerupture" || rf.id == "boot") "ic_shield" else "ic_alert",
                Ui.DANGER, if (urgent) 0x14B3261E else Ui.DANGER_BG, boxDp = 32))
            val ht = Ui.text(a, rf.title, 15.5f, fg, bold = true)
            ht.setPadding(Ui.dp(a, 10), 0, 0, 0)
            head.addView(Ui.weight(ht, 1f))
            card.addView(head)
            card.addView(Ui.spacer(a, 6))
            card.addView(Ui.text(a, rf.urgency, 14f, if (urgent) Ui.ON_DANGER_BG else Ui.DANGER, bold = true))
            addCallButtons(a, card, rf.urgency)
            card.addView(Ui.spacer(a, 8))
            card.addView(Ui.text(a, "If you have:", 13f, if (urgent) Ui.ON_DANGER_BG else Ui.TEXT_DIM, bold = true))
            for (s in rf.symptoms) {
                val r = Ui.row(a)
                r.gravity = android.view.Gravity.TOP
                val bullet = Ui.text(a, "·", 14f, if (urgent) Ui.ON_DANGER_BG else Ui.TEXT_DIM, bold = true)
                bullet.setPadding(Ui.dp(a, 4), 0, Ui.dp(a, 8), 0)
                r.addView(bullet)
                r.addView(Ui.weight(Ui.text(a, s, 14f, fg), 1f))
                card.addView(r)
            }
            card.addView(Ui.spacer(a, 6))
            card.addView(Ui.text(a, rf.action, 13.5f, if (urgent) Ui.ON_DANGER_BG else Ui.TEXT_DIM))
            col.addView(card)
        }

        // why these matter closes the page: someone who is worried lands straight on the
        // warnings and their call buttons (the calf/DVT call is on the first screen)
        col.addView(Ui.spacer(a, 8))
        col.addView(Ui.body(a, protocol.redFlagIntro))
        col.addView(Ui.spacer(a, 8))
        col.addView(Ui.caption(a, RehabFramework.DISCLAIMER))
        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    /**
     * Call buttons matching a section's urgency: 999 for emergencies, 111 for
     * same-day advice, and the user's own clinic (or a prompt to save its
     * number) where the advice is to contact the clinic.
     */
    fun addCallButtons(a: MainActivity, card: android.widget.LinearLayout, urgency: String) {
        val u = urgency.lowercase()
        if (u.contains("999")) card.addView(Ui.fullWidth(Ui.dangerButton(a, "Call 999") { dial(a, "999") }, a))
        if (u.contains("111")) card.addView(Ui.fullWidth(Ui.tonalButton(a, "Call 111") { dial(a, "111") }, a))
        if (u.contains("clinic")) addClinicButton(a, card)
    }

    /** "Call my clinic" when a number is saved; otherwise a route to save one. */
    fun addClinicButton(a: MainActivity, card: android.widget.LinearLayout) {
        val phone = a.store.profile().clinicPhone
        if (phone.isNotBlank()) {
            card.addView(Ui.fullWidth(Ui.tonalButton(a, "Call my clinic") { dial(a, phone) }, a))
        } else {
            card.addView(Ui.fullWidth(Ui.textButton(a, "Save your clinic's number for one-tap calls") {
                a.pushOverlay("Injury & goal") { MoreScreen.profileEditor(a) }
            }, a, 4))
        }
    }

    /** Opens the phone's dialler with [number] filled in - no call permission needed. */
    fun dial(a: MainActivity, number: String) {
        try {
            a.startActivity(android.content.Intent(android.content.Intent.ACTION_DIAL,
                android.net.Uri.parse("tel:" + number.filter { it.isDigit() || it == '+' })))
        } catch (e: Exception) {
            android.widget.Toast.makeText(a, "Couldn't open the dialler - call $number", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
