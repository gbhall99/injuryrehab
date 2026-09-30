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
 * The My leg tab: your leg now (body model, boot, weight-bearing), what to expect
 * and what's coming up, what you can do yet, and the whole plan phase by phase.
 * One home for "where am I and what's next" (it replaced What to expect, How
 * you're doing, the phase guide and the phase reference).
 */
object TwinScreen {

    /** The "Can I..." row showing its detail, if any. */
    private var openCheck: String? = null

    fun build(a: MainActivity): View {
        val today = LocalDate.now()
        val profile = a.store.profile()
        val protocol = ProtocolRegistry.forProfile(profile)
        val snap = Capability.snapshot(profile, today)
        val col = Ui.column(a)

        // ---- your leg now: which leg and where you are, then the body model beside the
        // boot and weight-bearing (the tendon's state lives with each phase in Your plan) ----
        val heroCard = Ui.card(a)
        val sideLabel = if (protocol.sided)
            profile.side.name.lowercase().replaceFirstChar { it.uppercase() } + " · " else ""
        heroCard.addView(Ui.text(a, sideLabel + protocol.injuryName, 16f, Ui.TEXT, bold = true))
        heroCard.addView(Ui.spacer(a, 8))
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
            heroRow.addView(body, LinearLayout.LayoutParams(Ui.dp(a, 96), Ui.dp(a, 134)))
        }
        val facts = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        facts.setPadding(Ui.dp(a, 14), 0, 0, 0)
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
        facts.gravity = android.view.Gravity.CENTER_VERTICAL
        heroRow.addView(Ui.weight(facts, 1f))
        heroCard.addView(heroRow)

        // ---- watch-outs (off-plan risk; always visible), inside the leg card they're about ----
        val recent = a.store.allLogs().filter { !it.date.isBefore(today.minusDays(7)) }
        for (w in Capability.warnings(profile, recent, today)) {
            val fg = when (w.severity) {
                Capability.Severity.URGENT -> Ui.DANGER
                Capability.Severity.WARNING -> Ui.WARN
                Capability.Severity.INFO -> Ui.ON_INFO_BG
            }
            heroCard.addView(Ui.divider(a))
            val r = Ui.row(a)
            r.gravity = android.view.Gravity.TOP
            r.addView(Ui.icon(a, "ic_alert", 16, fg))
            r.addView(Ui.weight(Ui.text(a, "${w.title}. ${w.detail}", 13.5f, fg).apply {
                setPadding(Ui.dp(a, 8), 0, 0, 0) }, 1f))
            heroCard.addView(r)
        }

        col.addView(heroCard)

        // ---- what to expect now, and what's next: one card (was What to expect,
        // How you're doing and a separate "Coming up" card) ----
        val exp = com.recoverwell.core.logic.Wellbeing.expectationFor(profile, today)
        val gate = com.recoverwell.core.logic.PhaseEngine.nextPhaseGate(profile, today)
        val nextBootChange = device?.takeIf { adjustableDevice(a) === it }?.let { dev ->
            profile.wedgePlan.removalSchedule(profile.injuryDate, profile.wedgeDateOverrides)
                .firstOrNull { !it.first.isBefore(today) }?.let { (d, after) ->
                    "${Forms.friendlyDate(d)} · ${dev.reductionVerb} to ${dev.format(after)}" }
        }
        if (exp != null || gate.nextPhase != null) {
            // the way deeper - the whole plan, phase by phase - sits on the heading line
            val head = Ui.row(a)
            head.addView(Ui.weight(Ui.section(a, "What to expect now"), 1f))
            head.addView(Ui.textButton(a, "Your plan ›") {
                a.pushOverlay("Your plan") { PlanGuideScreen.build(a) }
            }.apply { contentDescription = "Your plan, phase by phase: goals, what's OK and not yet, your boot, what's normal" })
            col.addView(head)
            val card = Ui.card(a)
            exp?.let {
                card.addView(Ui.text(a, it.title, 15.5f, Ui.TEXT, bold = true))
                card.addView(Ui.spacer(a, 2))
                card.addView(Ui.text(a, it.summary, 14f, Ui.TEXT))
                // what's normal to feel, as the reassurance (the phase-by-phase list is in Your plan)
                card.addView(Ui.spacer(a, 8))
                val r = Ui.row(a)
                r.gravity = android.view.Gravity.TOP
                r.addView(Ui.icon(a, "ic_heart", 16, Ui.DONE))
                r.addView(Ui.weight(Ui.text(a, it.reassure, 14f, Ui.DONE).apply { setPadding(Ui.dp(a, 10), 0, 0, 0) }, 1f))
                card.addView(r)
            }
            // what's next, in the same card
            val next = ArrayList<String>()
            gate.nextPhase?.let { ph ->
                next.add("Next: phase ${ph.number} · ${ph.title} - " +
                    (gate.startDate?.let { "typically from ${Forms.friendlyDate(it)}" } ?: "soon") + ", once your physio agrees")
            }
            nextBootChange?.let { next.add("Next boot change: $it") }
            if (next.isNotEmpty() && exp != null) card.addView(Ui.divider(a))
            next.forEachIndexed { i, n ->
                if (i > 0) card.addView(Ui.spacer(a, 4))
                card.addView(Ui.text(a, n, 14f, Ui.TEXT))
            }
            col.addView(card)
        }

        // ---- movement checks: the verdict at a glance; tap one for the why and when ----
        col.addView(Ui.section(a, "Can I..."))
        val checksCard = Ui.card(a)
        val protoChecks = protocol.movementChecks
        Capability.movementChecks(profile, today).forEachIndexed { i, c ->
            val isOpen = openCheck == c.movement
            val unlock = protoChecks.firstOrNull { it.movement == c.movement }?.unlockPhase
            val verdict = if (c.allowed) "Yes" else unlock?.let { "Phase $it" } ?: "Not yet"
            val row = Ui.row(a)
            row.minimumHeight = Ui.dp(a, Ui.MIN_TOUCH_DP)
            row.isClickable = true
            row.isFocusable = true
            row.background = Ui.ripple(a, Ui.rounded(0, 10f))
            row.contentDescription = "Can I ${c.movement.replaceFirstChar { it.lowercase() }}? " +
                (if (c.allowed) "Yes" else "Not yet") + if (isOpen) ". ${c.note}" else ", tap for why"
            row.setOnClickListener { openCheck = if (isOpen) null else c.movement; a.refresh() }
            val badge = Ui.iconBadge(a, if (c.allowed) "ic_check" else "ic_close",
                if (c.allowed) Ui.DONE else Ui.DANGER,
                if (c.allowed) Ui.DONE_BG else Ui.DANGER_BG, boxDp = 26)
            row.addView(badge)
            row.addView(Ui.weight(Ui.text(a, c.movement, 14.5f, Ui.TEXT, bold = true).apply {
                setPadding(Ui.dp(a, 12), 0, Ui.dp(a, 8), 0) }, 1f))
            // the verdict in words too, so it never relies on colour/icon alone
            row.addView(Ui.text(a, verdict, 13f, if (c.allowed) Ui.DONE else Ui.DANGER, bold = true))
            checksCard.addView(row)
            if (isOpen) checksCard.addView(Ui.caption(a, c.note).apply { setPadding(Ui.dp(a, 42), 0, 0, Ui.dp(a, 8)) })
        }
        col.addView(checksCard)

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
