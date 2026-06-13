package com.recoverwell.app.screens

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.recoverwell.app.MainActivity
import com.recoverwell.app.ui.SceneView
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.logic.Capability
import com.recoverwell.core.model.Side
import com.recoverwell.core.protocol.ProtocolRegistry
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
        val protocol = ProtocolRegistry.forProfile(profile)
        val snap = Capability.snapshot(profile, today)
        val col = Ui.column(a)

        // ---- body model + capability side by side ----
        val heroCard = Ui.card(a)
        val heroRow = Ui.row(a)
        // visuals are registered per protocol; unknown ids simply show no scene
        val device = protocol.supportDevice
        val initial = device?.plan?.initialWedges ?: 1
        val heelFraction = if (initial > 0) snap.wedges.toFloat() / initial else 0f
        // an orange wedge stack only makes sense for counted (wedge) boots
        val wedgeStack = if (device != null && device.unitSymbol.isEmpty()) snap.wedges else 0
        val scene: ((com.recoverwell.draw.Sketch) -> Unit)? =
            when (protocol.bodySceneId) {
                "lower_leg" -> { s -> BodyScene.render(s, snap.phaseNumber, heelFraction, wedgeStack, profile.side == com.recoverwell.core.model.Side.RIGHT) }
                else -> null
            }
        if (scene != null) {
            val body = SceneView(a, scene)
            heroRow.addView(body, LinearLayout.LayoutParams(Ui.dp(a, 150), Ui.dp(a, 210)))
        }
        val facts = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        facts.setPadding(Ui.dp(a, 14), 0, 0, 0)
        facts.addView(Ui.pillBadge(a, "Week ${snap.weeksSinceInjury} · Phase ${snap.phaseNumber}",
            Ui.ON_PRIMARY_CONTAINER, Ui.PRIMARY_CONTAINER))
        facts.addView(Ui.spacer(a, 8))
        val sideLabel = if (protocol.sided)
            profile.side.name.lowercase().replaceFirstChar { it.uppercase() } + " · " else ""
        facts.addView(Ui.text(a, sideLabel + protocol.injuryName, 16f, Ui.TEXT, bold = true))
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
            if (i > 0) checksCard.addView(Ui.divider(a))
            val row = Ui.row(a)
            row.gravity = android.view.Gravity.TOP
            val badge = Ui.iconBadge(a, if (c.allowed) "ic_check" else "ic_close",
                if (c.allowed) Ui.DONE else Ui.DANGER,
                if (c.allowed) Ui.DONE_BG else Ui.DANGER_BG, boxDp = 34)
            row.addView(badge)
            val texts = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
            texts.setPadding(Ui.dp(a, 12), Ui.dp(a, 1), 0, 0)
            texts.addView(Ui.text(a, c.movement, 15f, Ui.TEXT, bold = true))
            texts.addView(Ui.spacer(a, 2))
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

        col.addView(Ui.fullWidth(Ui.dangerButton(a, protocol.redFlagButtonLabel) {
            a.pushOverlay { RedFlagsScreen.build(a) }
        }, a))

        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }
}
