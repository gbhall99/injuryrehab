package com.recoverwell.app.screens

import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.recoverwell.app.MainActivity
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.logic.Insights
import com.recoverwell.core.logic.MilestoneTimeline
import com.recoverwell.core.logic.PhaseEngine
import com.recoverwell.core.logic.Wellbeing
import com.recoverwell.core.protocol.ProtocolRegistry
import java.time.LocalDate

/**
 * The human side of recovery: what's normal to feel right now, reassurance about
 * the fear that defines this injury, a reflection on mood, and quiet celebration
 * of milestones reached. Supportive, never clinical.
 */
object WellbeingScreen {

    fun build(a: MainActivity): View {
        val today = LocalDate.now()
        val profile = a.store.profile()
        val protocol = ProtocolRegistry.forProfile(profile)
        val phase = PhaseEngine.currentPhase(profile, today)
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "How you're doing") { a.popOverlay() })
        col.addView(Ui.caption(a, "Recovery is physical and emotional. This part is just for the human doing the work."))

        // ---- what's normal to feel now -------------------------------------
        Wellbeing.currentMindset(profile, today)?.let { mind ->
            col.addView(Ui.section(a, "What's normal to feel now"))
            col.addView(Ui.caption(a, "Phase ${phase.number} · ${phase.title}"))
            col.addView(Ui.spacer(a, 6))
            val card = Ui.card(a)
            mind.normalToFeel.forEachIndexed { i, f ->
                if (i > 0) card.addView(Ui.spacer(a, 8))
                val row = Ui.row(a)
                row.gravity = Gravity.TOP
                row.addView(Ui.icon(a, "ic_heart", 18, Ui.PRIMARY))
                val tv = Ui.text(a, f, 14.5f, Ui.TEXT)
                tv.setPadding(Ui.dp(a, 10), 0, 0, 0)
                row.addView(Ui.weight(tv, 1f))
                card.addView(row)
            }
            col.addView(card)
            val enc = Ui.card(a, Ui.DONE_BG)
            enc.addView(Ui.text(a, mind.encouragement, 14.5f, Ui.TEXT))
            col.addView(enc)
        }

        // ---- mood reflection -----------------------------------------------
        val moodInsight = Insights.generate(profile, a.store.allLogs(), a.store.allEvents(),
            a.store.medications(), a.store.tasks(), today)
            .firstOrNull { it.title.contains("mood", ignoreCase = true) || it.title.contains("Brighter") }
        if (moodInsight != null) {
            col.addView(Ui.section(a, "Your mood"))
            col.addView(TodayScreen.insightCard(a, moodInsight))
        } else {
            col.addView(Ui.section(a, "Your mood"))
            col.addView(Ui.caption(a, "Log your mood on the Progress tab for a few days and a gentle reflection will appear here."))
        }

        // ---- reassurance (fear of re-rupture) ------------------------------
        protocol.reassurance?.let { r ->
            col.addView(Ui.section(a, r.title))
            val card = Ui.card(a)
            card.addView(Ui.text(a, r.body, 14.5f, Ui.TEXT))
            card.addView(Ui.spacer(a, 10))
            // two-column heads
            val heads = Ui.row(a)
            heads.addView(Ui.weight(Ui.text(a, "Usually normal", 12.5f, Ui.DONE, bold = true), 1f))
            heads.addView(Ui.weight(Ui.text(a, "Tell your clinic", 12.5f, Ui.DANGER, bold = true), 1f))
            card.addView(heads)
            for ((normal, flag) in r.normalVsFlag) {
                card.addView(Ui.divider(a))
                val row = Ui.row(a)
                row.gravity = Gravity.TOP
                val left = Ui.text(a, normal, 13.5f, Ui.TEXT)
                left.setPadding(0, 0, Ui.dp(a, 8), 0)
                row.addView(Ui.weight(left, 1f))
                row.addView(Ui.weight(Ui.text(a, flag, 13.5f, Ui.ON_DANGER_BG), 1f))
                card.addView(row)
            }
            col.addView(card)
            col.addView(Ui.fullWidth(Ui.tonalButton(a, "See the full red-flag guide") {
                a.pushOverlay { RedFlagsScreen.build(a) }
            }, a))
        }

        // ---- milestones reached (celebration) ------------------------------
        val reached = MilestoneTimeline.build(profile, today)
            .filter { it.status == MilestoneTimeline.Status.REACHED }
        if (reached.isNotEmpty()) {
            col.addView(Ui.section(a, "How far you've come"))
            val card = Ui.card(a, Ui.DONE_BG)
            card.addView(Ui.text(a, "${reached.size} milestone${if (reached.size == 1) "" else "s"} behind you",
                15.5f, Ui.DONE, bold = true))
            card.addView(Ui.spacer(a, 6))
            for (e in reached.takeLast(4)) {
                val row = Ui.row(a)
                row.gravity = Gravity.CENTER_VERTICAL
                row.addView(Ui.icon(a, "ic_check", 16, Ui.DONE))
                val tv = Ui.text(a, "Week ${e.milestone.week} · ${e.milestone.title}", 13.5f, Ui.TEXT)
                tv.setPadding(Ui.dp(a, 8), Ui.dp(a, 2), 0, Ui.dp(a, 2))
                row.addView(Ui.weight(tv, 1f))
                card.addView(row)
            }
            col.addView(card)
        }

        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }
}
