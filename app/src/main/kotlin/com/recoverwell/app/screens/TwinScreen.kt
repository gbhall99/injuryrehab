package com.recoverwell.app.screens

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.recoverwell.app.MainActivity
import com.recoverwell.app.ui.BodyModelView
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.logic.Capability
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

        col.addView(Ui.title(a, "My leg right now"))

        val body = BodyModelView(a)
        body.snapshot = snap
        body.side = profile.side
        body.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(a, 280))
        body.background = Ui.roundedBg(Ui.CARD, strokeColor = Ui.BORDER)
        col.addView(body)

        // capability panel
        col.addView(Ui.section(a, "Current capability"))
        val cap = Ui.card(a)
        cap.addView(Ui.text(a, "Week ${snap.weeksSinceInjury} · Phase ${snap.phaseNumber}: ${snap.phaseTitle}", 17f, Ui.TEXT, bold = true))
        cap.addView(Ui.text(a, "🥾  ${snap.bootStatus}", 15f))
        cap.addView(Ui.text(a, "🦶  ${snap.weightBearing}", 15f))
        cap.addView(Ui.text(a, "🩹  ${snap.tendonState}", 15f))
        col.addView(cap)

        // warnings
        val recent = a.store.allLogs().filter { !it.date.isBefore(today.minusDays(7)) }
        val warnings = Capability.warnings(profile, recent, today)
        if (warnings.isNotEmpty()) {
            col.addView(Ui.section(a, "Watch-outs"))
            for (w in warnings) {
                val (bg, fg) = when (w.severity) {
                    Capability.Severity.URGENT -> Ui.DANGER_BG to Ui.DANGER
                    Capability.Severity.WARNING -> Ui.WARN_BG to Ui.WARN
                    Capability.Severity.INFO -> Ui.INFO_BG to Ui.TEXT
                }
                val card = Ui.card(a, bg)
                card.addView(Ui.text(a, w.title, 16f, fg, bold = true))
                card.addView(Ui.text(a, w.detail, 14f))
                col.addView(card)
            }
        }

        // movement checks
        col.addView(Ui.section(a, "Can I...?"))
        val checksCard = Ui.card(a)
        for (c in Capability.movementChecks(profile, today)) {
            val row = Ui.row(a)
            row.addView(Ui.text(a, if (c.allowed) "✓  " else "✗  ", 18f, if (c.allowed) Ui.DONE else Ui.DANGER, bold = true))
            val texts = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
            texts.addView(Ui.text(a, c.movement, 16f, Ui.TEXT, bold = true))
            texts.addView(Ui.text(a, c.note, 13f, Ui.TEXT_DIM))
            row.addView(Ui.weight(texts, 1f))
            row.setPadding(0, Ui.dp(a, 6), 0, Ui.dp(a, 6))
            checksCard.addView(row)
        }
        col.addView(checksCard)

        // do / don't
        col.addView(Ui.section(a, "OK in this phase"))
        val doCard = Ui.card(a, 0xFFE8F5E9.toInt())
        for (s in snap.allowed) doCard.addView(Ui.text(a, "✓  $s", 15f, Ui.DONE))
        col.addView(doCard)

        col.addView(Ui.section(a, "Not yet - off-plan risk"))
        val dontCard = Ui.card(a, Ui.DANGER_BG)
        for (s in snap.notAllowed) dontCard.addView(Ui.text(a, "✗  $s", 15f, Ui.DANGER))
        col.addView(dontCard)

        col.addView(Ui.fullWidth(Ui.dangerButton(a, "⚠ DVT & re-rupture red flags") {
            a.pushOverlay { RedFlagsScreen.build(a) }
        }, a))

        col.addView(Ui.spacer(a, 20))
        return Ui.scroll(a, col)
    }
}
