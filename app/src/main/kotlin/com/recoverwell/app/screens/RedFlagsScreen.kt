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
        col.addView(Ui.body(a, protocol.redFlagIntro))
        col.addView(Ui.spacer(a, 8))

        for (rf in protocol.redFlags) {
            val urgent = rf.id == "pe"
            val card = Ui.card(a, if (urgent) Ui.DANGER_BG else Ui.CARD)
            val head = Ui.row(a)
            head.addView(Ui.iconBadge(a, if (rf.id == "rerupture" || rf.id == "boot") "ic_shield" else "ic_alert",
                Ui.DANGER, if (urgent) 0x14B3261E else Ui.DANGER_BG, boxDp = 36))
            val ht = Ui.text(a, rf.title, 16f, if (urgent) Ui.ON_DANGER_BG else Ui.TEXT, bold = true)
            ht.setPadding(Ui.dp(a, 12), 0, 0, 0)
            head.addView(Ui.weight(ht, 1f))
            card.addView(head)
            card.addView(Ui.spacer(a, 8))
            card.addView(Ui.pillBadge(a, rf.urgency, Ui.ON_DANGER, Ui.DANGER))
            card.addView(Ui.spacer(a, 8))
            for (s in rf.symptoms) {
                val r = Ui.row(a)
                val bullet = Ui.text(a, "·", 18f, if (urgent) Ui.ON_DANGER_BG else Ui.TEXT_DIM, bold = true)
                bullet.setPadding(Ui.dp(a, 4), 0, Ui.dp(a, 8), 0)
                r.addView(bullet)
                r.addView(Ui.weight(Ui.text(a, s, 14.5f, if (urgent) Ui.ON_DANGER_BG else Ui.TEXT), 1f))
                card.addView(r)
            }
            card.addView(Ui.spacer(a, 8))
            card.addView(Ui.text(a, rf.action, 14f, if (urgent) Ui.ON_DANGER_BG else Ui.TEXT, bold = true))
            col.addView(card)
        }

        col.addView(Ui.spacer(a, 8))
        col.addView(Ui.caption(a, RehabFramework.DISCLAIMER))
        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }
}
