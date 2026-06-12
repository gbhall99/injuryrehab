package com.recoverwell.app.screens

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.recoverwell.app.MainActivity
import com.recoverwell.app.ui.SceneView
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.logic.Capability
import com.recoverwell.core.model.Side
import com.recoverwell.draw.BodyScene
import java.time.LocalDate

/**
 * Digital twin: visual body model, current capability panel, phase-based
 * do/don't lists and off-plan risk warnings.
 */
object TwinScreen {

    fun build(a: MainActivity): View {
        val today = LocalDate.now()
        val profile = a.store.profile()
        val snap = Capability.snapshot(profile, today)
        val col = Ui.column(a)

        // ---- body model + capability side by side ----
        val heroCard = Ui.card(a)
        val heroRow = Ui.row(a)
        val body = SceneView(a) { s ->
            BodyScene.render(s, snap.phaseNumber, snap.wedges, profile.side == Side.RIGHT)
        }
        heroRow.addView(body, LinearLayout.LayoutParams(Ui.dp(a, 150), Ui.dp(a, 210)))
        val facts = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        facts.setPadding(Ui.dp(a, 14), 0, 0, 0)
        facts.addView(Ui.pillBadge(a, "Week ${snap.weeksSinceInjury} · Phase ${snap.phaseNumber}",
            Ui.ON_PRIMARY_CONTAINER, Ui.PRIMARY_CONTAINER))
        facts.addView(Ui.spacer(a, 8))
        facts.addView(Ui.text(a, "${profile.side.name.lowercase().replaceFirstChar { it.uppercase() }} Achilles",
            16f, Ui.TEXT, bold = true))
        facts.addView(Ui.caption(a, snap.tendonState))
        facts.addView(Ui.spacer(a, 8))
        facts.addView(Ui.text(a, snap.bootStatus, 13.5f, Ui.TEXT))
        facts.addView(Ui.spacer(a, 4))
        facts.addView(Ui.text(a, snap.weightBearing, 13.5f, Ui.TEXT))
        heroRow.addView(Ui.weight(facts, 1f))
        heroCard.addView(heroRow)
        col.addView(heroCard)

        // ---- warnings ----
        val recent = a.store.allLogs().filter { !it.date.isBefore(today.minusDays(7)) }
        val warnings = Capability.warnings(profile, recent, today)
        if (warnings.isNotEmpty()) {
            col.addView(Ui.section(a, "Watch-outs"))
            for (w in warnings) {
                val (bg, fg) = when (w.severity) {
                    Capability.Severity.URGENT -> Ui.DANGER_BG to Ui.ON_DANGER_BG
                    Capability.Severity.WARNING -> Ui.WARN_BG to Ui.WARN
                    Capability.Severity.INFO -> Ui.INFO_BG to Ui.ON_INFO_BG
                }
                val card = Ui.card(a, bg)
                card.addView(Ui.text(a, w.title, 15f, fg, bold = true))
                card.addView(Ui.spacer(a, 2))
                card.addView(Ui.text(a, w.detail, 13.5f, fg))
                col.addView(card)
            }
        }

        // ---- movement checks ----
        col.addView(Ui.section(a, "Can I..."))
        val checksCard = Ui.card(a)
        Capability.movementChecks(profile, today).forEachIndexed { i, c ->
            if (i > 0) checksCard.addView(Ui.spacer(a, 10))
            val row = Ui.row(a)
            row.addView(Ui.icon(a, if (c.allowed) "ic_check" else "ic_close", 20,
                if (c.allowed) Ui.DONE else Ui.DANGER))
            val texts = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
            texts.setPadding(Ui.dp(a, 12), 0, 0, 0)
            texts.addView(Ui.text(a, c.movement, 15f, Ui.TEXT, bold = true))
            texts.addView(Ui.caption(a, c.note))
            row.addView(Ui.weight(texts, 1f))
            checksCard.addView(row)
        }
        col.addView(checksCard)

        // ---- do / don't ----
        col.addView(Ui.section(a, "OK in this phase"))
        val doCard = Ui.card(a)
        snap.allowed.forEachIndexed { i, s ->
            if (i > 0) doCard.addView(Ui.spacer(a, 6))
            val r = Ui.row(a)
            r.addView(Ui.icon(a, "ic_check", 17, Ui.DONE))
            val t = Ui.text(a, s, 14f)
            t.setPadding(Ui.dp(a, 10), 0, 0, 0)
            r.addView(Ui.weight(t, 1f))
            doCard.addView(r)
        }
        col.addView(doCard)

        col.addView(Ui.section(a, "Not yet"))
        val dontCard = Ui.card(a)
        snap.notAllowed.forEachIndexed { i, s ->
            if (i > 0) dontCard.addView(Ui.spacer(a, 6))
            val r = Ui.row(a)
            r.addView(Ui.icon(a, "ic_close", 17, Ui.DANGER))
            val t = Ui.text(a, s, 14f)
            t.setPadding(Ui.dp(a, 10), 0, 0, 0)
            r.addView(Ui.weight(t, 1f))
            dontCard.addView(r)
        }
        col.addView(dontCard)

        col.addView(Ui.fullWidth(Ui.dangerButton(a, "DVT & re-rupture red flags") {
            a.pushOverlay { RedFlagsScreen.build(a) }
        }, a))

        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }
}
