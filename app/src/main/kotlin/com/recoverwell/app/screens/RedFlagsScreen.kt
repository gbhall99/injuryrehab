package com.recoverwell.app.screens

import android.view.View
import com.recoverwell.app.MainActivity
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.protocol.ProtocolContent

/**
 * Red-flag guidance: DVT, PE, re-rupture, anticoagulant bleeding, boot/skin.
 * Reachable in one tap from the persistent header button on every screen.
 */
object RedFlagsScreen {

    fun build(a: MainActivity): View {
        val col = Ui.column(a)
        col.addView(Ui.secondaryButton(a, "← Back") { a.popOverlay() })
        col.addView(Ui.title(a, "Red flags - act, don't wait"))
        col.addView(Ui.text(
            a,
            "After an Achilles rupture you are at raised risk of a blood clot (DVT), " +
                "and the healing tendon can re-tear. You take an anticoagulant precisely " +
                "because of this - so take these signs seriously even if they seem mild.",
            15f
        ))

        for (rf in ProtocolContent.redFlags) {
            val urgent = rf.id == "pe"
            val card = Ui.card(a, if (urgent) Ui.DANGER_BG else Ui.CARD)
            card.addView(Ui.text(a, rf.title, 18f, Ui.DANGER, bold = true))
            card.addView(Ui.badge(a, rf.urgency, Ui.DANGER, 0xFFFFFFFF.toInt()))
            card.addView(Ui.spacer(a, 6))
            for (s in rf.symptoms) card.addView(Ui.text(a, "•  $s", 15f))
            card.addView(Ui.spacer(a, 6))
            card.addView(Ui.text(a, rf.action, 15f, Ui.TEXT, bold = true))
            col.addView(card)
        }

        col.addView(Ui.text(a, ProtocolContent.DISCLAIMER, 13f, Ui.TEXT_DIM))
        col.addView(Ui.spacer(a, 20))
        return Ui.scroll(a, col)
    }
}
