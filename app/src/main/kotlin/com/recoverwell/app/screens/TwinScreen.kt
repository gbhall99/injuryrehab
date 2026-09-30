package com.recoverwell.app.screens

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.recoverwell.app.MainActivity
import com.recoverwell.app.ui.Forms
import com.recoverwell.app.ui.SceneView
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.logic.Capability
import com.recoverwell.core.model.Side
import com.recoverwell.core.protocol.ProtocolRegistry
import com.recoverwell.draw.BodyScene
import java.time.LocalDate

/**
 * The Guide tab: your leg now (body model, boot, weight-bearing), what to expect
 * and what's normal to feel, what you can do yet, what's coming up, and the two
 * ways deeper - physio visits and the whole plan phase by phase. One home for
 * "where am I and what's next" (it replaced What to expect, How you're doing,
 * the phase guide and the phase reference).
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
            body.contentDescription = "Model of your ${if (protocol.sided) profile.side.name.lowercase() + " " else ""}leg. " +
                "${snap.tendonState}. ${snap.bootStatus}."
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
        // the clinic changed the angle? adjust it right where it's shown
        val phaseNow = com.recoverwell.core.logic.PhaseEngine.currentPhase(profile, today)
        if (device != null && adjustableDevice(a) === device) {
            facts.addView(Ui.textButton(a, changeLabel(device)) { adjustDevice(a, device) }.apply {
                contentDescription = "${changeLabel(device)} (${device.name})"
            })
        }
        facts.addView(Ui.spacer(a, 4))
        facts.addView(Ui.text(a, snap.weightBearing, 13.5f, Ui.TEXT))
        heroRow.addView(Ui.weight(facts, 1f))
        heroCard.addView(heroRow)
        col.addView(heroCard)

        // ---- watch-outs (off-plan risk; always visible) ----
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

        // ---- what to expect now: this week's picture and what's normal to feel, in one card
        // (was two separate screens: What to expect and How you're doing) ----
        com.recoverwell.core.logic.Wellbeing.expectationFor(profile, today)?.let { exp ->
            col.addView(Ui.section(a, "What to expect now"))
            val card = Ui.card(a)
            card.addView(Ui.text(a, exp.title, 15.5f, Ui.TEXT, bold = true))
            card.addView(Ui.spacer(a, 2))
            card.addView(Ui.text(a, exp.summary, 14f, Ui.TEXT))
            val normal = exp.likely.take(2) + (com.recoverwell.core.logic.Wellbeing.currentMindset(profile, today)
                ?.normalToFeel?.take(1) ?: emptyList())
            for (l in normal) {
                card.addView(Ui.spacer(a, 6))
                val r = Ui.row(a)
                r.gravity = android.view.Gravity.TOP
                r.addView(Ui.icon(a, "ic_heart", 16, Ui.PRIMARY))
                r.addView(Ui.weight(Ui.text(a, l, 14f, Ui.TEXT).apply { setPadding(Ui.dp(a, 10), 0, 0, 0) }, 1f))
                card.addView(r)
            }
            card.addView(Ui.spacer(a, 8))
            card.addView(Ui.text(a, exp.reassure, 14f, Ui.DONE))
            col.addView(card)
        }

        // ---- movement checks: the live "what can I do right now" (always visible,
        // the highest-value, most-used part of this screen) ----
        col.addView(Ui.section(a, "Can I..."))
        val checksCard = Ui.card(a)
        Capability.movementChecks(profile, today).forEachIndexed { i, c ->
            if (i > 0) checksCard.addView(Ui.divider(a))
            val row = Ui.row(a)
            row.gravity = android.view.Gravity.TOP
            val badge = Ui.iconBadge(a, if (c.allowed) "ic_check" else "ic_close",
                if (c.allowed) Ui.DONE else Ui.DANGER,
                if (c.allowed) Ui.DONE_BG else Ui.DANGER_BG, boxDp = 34)
            badge.contentDescription = if (c.allowed) "Allowed" else "Not yet"
            row.addView(badge)
            val texts = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
            texts.setPadding(Ui.dp(a, 12), Ui.dp(a, 1), 0, 0)
            texts.addView(Ui.text(a, c.movement, 15f, Ui.TEXT, bold = true))
            texts.addView(Ui.spacer(a, 2))
            texts.addView(Ui.caption(a, c.note))
            row.addView(Ui.weight(texts, 1f))
            // spell the verdict out in words too, so it never relies on colour/icon alone
            row.addView(Ui.pillBadge(a, if (c.allowed) "Yes" else "Not yet",
                if (c.allowed) Ui.DONE else Ui.DANGER,
                if (c.allowed) Ui.DONE_BG else Ui.DANGER_BG))
            checksCard.addView(row)
        }
        col.addView(checksCard)

        // ---- coming up: the next phase and the next milestone, in one card ----
        val gate = com.recoverwell.core.logic.PhaseEngine.nextPhaseGate(profile, today)
        val nextMilestone = com.recoverwell.core.logic.MilestoneTimeline.build(profile, today)
            .firstOrNull { it.status != com.recoverwell.core.logic.MilestoneTimeline.Status.REACHED }
        if (gate.nextPhase != null || nextMilestone != null) {
            col.addView(Ui.section(a, "Coming up"))
            val card = Ui.card(a)
            gate.nextPhase?.let { next ->
                card.addView(Ui.text(a, "Phase ${next.number} · ${next.title}", 15f, Ui.TEXT, bold = true))
                card.addView(Ui.caption(a, (gate.startDate?.let { "Typically from ${Forms.friendlyDate(it)}" } ?: "Next") +
                    " - your physio confirms when you're ready."))
            }
            nextMilestone?.let { m ->
                if (gate.nextPhase != null) card.addView(Ui.spacer(a, 8))
                card.addView(Ui.text(a, "Week ${m.milestone.week} · ${m.milestone.title}", 14.5f, Ui.TEXT, bold = true))
                card.addView(Ui.caption(a, m.milestone.detail))
            }
            col.addView(card)
        }

        // ---- the two ways deeper: your physio, and the whole plan phase by phase ----
        val nextVisit = profile.appointments.filter { !it.completed && !it.date.isBefore(today) }.minByOrNull { it.date }
        col.addView(Ui.listRow(a, "ic_calendar", "Physio visits",
            nextVisit?.let { "Next: ${Forms.friendlyDate(it.date)} · questions, notes, sign-offs" }
                ?: "Add your next appointment · questions, notes") {
            a.pushOverlay("Physio visits") { PhysioScreen.build(a) }
        })
        col.addView(Ui.listRow(a, "ic_info", "Your plan, phase by phase",
            "Goals, what's OK and not yet, your boot, what's normal") {
            a.pushOverlay("Your plan") { PlanGuideScreen.build(a) }
        })

        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    /** The boot the user can re-set right now (in a boot phase, still wearing it, not a cast), or null. */
    fun adjustableDevice(a: MainActivity): com.recoverwell.core.protocol.SupportDevice? {
        val profile = a.store.profile()
        val today = java.time.LocalDate.now()
        val device = com.recoverwell.core.protocol.ProtocolRegistry.deviceFor(profile) ?: return null
        val phaseNow = com.recoverwell.core.logic.PhaseEngine.currentPhase(profile, today)
        return device.takeIf {
            it.kind != com.recoverwell.core.protocol.DeviceKind.CAST && phaseNow.deviceUsage != null &&
                profile.usesDeviceOn(today)
        }
    }

    /** Says what changes, in the words people use: "Change boot angle" / "Change boot wedges". */
    fun changeLabel(device: com.recoverwell.core.protocol.SupportDevice): String =
        if (device.unitSymbol == "°") "Change boot angle" else "Change boot ${device.unitNamePlural}"

    /** One small dialog: step the boot's current setting, save, and every screen follows. */
    fun adjustDevice(a: MainActivity, device: com.recoverwell.core.protocol.SupportDevice) {
        var value = a.store.profile().currentWedges
        val pad = Ui.dp(a, 18)
        val holder = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, Ui.dp(a, 6), pad, 0)
            addView(com.recoverwell.app.ui.Forms.stepper(a, "Setting now (${device.unitNamePlural})", value, 0,
                device.maxValue, step = device.plan.stepSize.coerceAtLeast(1)) { value = it })
            addView(Ui.caption(a, "Only change it when your clinic has agreed the step."))
        }
        android.app.AlertDialog.Builder(a)
            .setTitle(device.name)
            .setView(holder)
            .setPositiveButton("Save") { _, _ ->
                a.store.saveProfile(a.store.profile().copy(currentWedges = value))
                com.recoverwell.app.notify.Reminders.reschedule(a)
                a.refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
