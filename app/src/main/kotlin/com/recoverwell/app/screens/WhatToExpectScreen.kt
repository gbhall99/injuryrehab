package com.recoverwell.app.screens

import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.recoverwell.app.MainActivity
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.logic.MilestoneTimeline
import com.recoverwell.core.logic.PhaseEngine
import com.recoverwell.core.logic.Wellbeing
import com.recoverwell.core.protocol.ProtocolRegistry
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * "What to expect" - proactively answers the anxious questions about this stage
 * of recovery (what's normal now, what's coming, what's reassuring) from the
 * protocol's own week-banded content. Calm, honest, never alarmist.
 */
object WhatToExpectScreen {

    fun build(a: MainActivity): View {
        val today = LocalDate.now()
        val profile = a.store.profile()
        val week = PhaseEngine.weeksSinceInjury(profile, today)
        val phase = PhaseEngine.currentPhase(profile, today)
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "What to expect") { a.popOverlay() })
        col.addView(Ui.caption(a, "Typical, plain-language guidance for where you are now. Everyone's recovery " +
            "differs - your physio's advice always comes first."))

        val exp = Wellbeing.expectationFor(profile, today)
        if (exp != null) {
            val card = Ui.card(a, Ui.HERO_BG)
            card.setPadding(Ui.dp(a, 20), Ui.dp(a, 16), Ui.dp(a, 20), Ui.dp(a, 16))
            val onHero = Ui.ON_HERO
            card.addView(Ui.text(a, "Week $week · Phase ${phase.number}", 12.5f,
                com.recoverwell.draw.Palette.withAlpha(onHero, 0xCC), bold = true))
            card.addView(Ui.text(a, exp.title, 19f, onHero, bold = true))
            card.addView(Ui.spacer(a, 4))
            card.addView(Ui.text(a, exp.summary, 14.5f, com.recoverwell.draw.Palette.withAlpha(onHero, 0xF2)))
            col.addView(card)

            col.addView(Ui.section(a, "Common around now"))
            val likely = Ui.card(a)
            exp.likely.forEachIndexed { i, l ->
                if (i > 0) likely.addView(Ui.spacer(a, 8))
                val r = Ui.row(a)
                r.gravity = Gravity.TOP
                r.addView(Ui.icon(a, "ic_info", 18, Ui.PRIMARY))
                val tv = Ui.text(a, l, 14.5f, Ui.TEXT)
                tv.setPadding(Ui.dp(a, 10), 0, 0, 0)
                r.addView(Ui.weight(tv, 1f))
                likely.addView(r)
            }
            col.addView(likely)

            val rea = Ui.card(a, Ui.DONE_BG)
            val rr = Ui.row(a)
            rr.gravity = Gravity.TOP
            rr.addView(Ui.icon(a, "ic_heart", 18, Ui.DONE))
            val rt = Ui.text(a, exp.reassure, 14.5f, Ui.TEXT)
            rt.setPadding(Ui.dp(a, 10), 0, 0, 0)
            rr.addView(Ui.weight(rt, 1f))
            rea.addView(rr)
            col.addView(rea)
        }

        // what's coming next (next milestone)
        val fmt = DateTimeFormatter.ofPattern("d MMM")
        val upcoming = MilestoneTimeline.build(profile, today)
            .firstOrNull { it.status != MilestoneTimeline.Status.REACHED }
        if (upcoming != null) {
            col.addView(Ui.section(a, "Coming up"))
            val card = Ui.card(a)
            card.addView(Ui.text(a, "Week ${upcoming.milestone.week} · ${upcoming.milestone.title}", 15f, Ui.TEXT, bold = true))
            card.addView(Ui.caption(a, "~${upcoming.expectedDate.format(fmt)} · ${upcoming.milestone.detail}"))
            col.addView(card)
        }

        // reassurance about the signature fear + red flags
        ProtocolRegistry.forProfile(profile).reassurance?.let { r ->
            col.addView(Ui.section(a, r.title))
            val card = Ui.card(a)
            card.addView(Ui.text(a, r.body, 14.5f, Ui.TEXT))
            card.addView(Ui.fullWidth(Ui.tonalButton(a, "Normal vs warning signs") {
                a.pushOverlay { WellbeingScreen.build(a) }
            }, a))
            card.addView(Ui.fullWidth(Ui.textButton(a, "Open the red-flag guide", Ui.DANGER) {
                a.pushOverlay { RedFlagsScreen.build(a) }
            }, a, 2))
            col.addView(card)
        }

        // read ahead: the whole arc
        col.addView(Ui.section(a, "The whole journey"))
        for (e in ProtocolRegistry.forProfile(profile).expectations) {
            val here = week >= e.weekFrom && week < e.weekTo
            val card = Ui.card(a, if (here) Ui.PRIMARY_CONTAINER else Ui.CARD)
            card.addView(Ui.text(a, e.title, 14.5f, if (here) Ui.ON_PRIMARY_CONTAINER else Ui.TEXT, bold = true))
            card.addView(Ui.text(a, e.summary, 13.5f, if (here) Ui.ON_PRIMARY_CONTAINER else Ui.TEXT_DIM))
            col.addView(card)
        }

        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }
}
