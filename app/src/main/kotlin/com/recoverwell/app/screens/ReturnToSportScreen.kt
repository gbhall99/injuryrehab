package com.recoverwell.app.screens

import android.app.AlertDialog
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import com.recoverwell.app.MainActivity
import com.recoverwell.app.ui.Forms
import com.recoverwell.app.ui.SceneView
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.logic.ReturnToSport
import com.recoverwell.core.model.SelfTestResult
import com.recoverwell.core.protocol.InjuryProtocol
import com.recoverwell.core.protocol.ProtocolRegistry
import com.recoverwell.core.protocol.SelfTest
import com.recoverwell.core.protocol.SportRegistry
import com.recoverwell.draw.RingScene
import java.time.LocalDate
import java.util.UUID

/**
 * The criteria-based return-to-sport program: objective self-tests, a ladder of
 * stages gated by hitting thresholds AND physio sign-off, and an overall
 * readiness toward the goal sport. Advisory - it never "clears" anyone.
 */
object ReturnToSportScreen {

    fun build(a: MainActivity): View {
        val today = LocalDate.now()
        val profile = a.store.profile()
        val protocol = ProtocolRegistry.forProfile(profile)
        val results = a.store.selfTestResults()
        val signoffs = a.store.rtsSignoffs()
        val prog = ReturnToSport.progress(profile, results, signoffs, today, protocol)
        val total = prog.rungs.size
        val clearedCount = prog.rungs.count { it.cleared }

        val col = Ui.column(a)
        col.addView(Ui.backRow(a, prog.returnPhrase) { a.popOverlay() })

        // ---- target sport selector (scales the whole ladder) ----------------
        if (protocol.supportedSportIds.size > 1) {
            col.addView(Ui.listRow(a, "ic_flag", "Training for",
                (prog.sport?.name ?: "Sport") + " · tap to change") {
                pickSport(a, protocol, prog.sport?.id ?: "")
            })
        }
        prog.sport?.demands?.let { col.addView(Ui.caption(a, it)) }

        // ---- readiness hero -------------------------------------------------
        val hero = Ui.card(a, Ui.HERO_BG)
        hero.setPadding(Ui.dp(a, 20), Ui.dp(a, 18), Ui.dp(a, 20), Ui.dp(a, 18))
        val onHero = Ui.ON_HERO
        val onHeroDim = com.recoverwell.draw.Palette.withAlpha(onHero, 0xCC)
        val heroRow = Ui.row(a)
        val texts = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        texts.addView(Ui.text(a, if (prog.available) "Earning your way back" else "On the horizon", 13f, onHeroDim, bold = true))
        texts.addView(Ui.text(a, "${prog.readinessPct}% ready", 30f, onHero, bold = true))
        texts.addView(Ui.text(a, "$clearedCount of $total stages cleared", 13.5f, com.recoverwell.draw.Palette.withAlpha(onHero, 0xE6)))
        prog.currentRung?.let {
            texts.addView(Ui.spacer(a, 4))
            texts.addView(Ui.text(a, "Now: ${it.title}", 13.5f, onHero, bold = true))
        }
        heroRow.addView(Ui.weight(texts, 1f))
        val ringBox = FrameLayout(a)
        var sweep = 0f
        val ring = SceneView(a) { s ->
            RingScene.render(s, sweep, Ui.dpF(a, 9f),
                trackColor = com.recoverwell.draw.Palette.withAlpha(onHero, 0x59), color = onHero)
        }
        android.animation.ValueAnimator.ofFloat(0f, prog.readinessPct / 100f).apply {
            duration = 700
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener { sweep = it.animatedValue as Float; ring.invalidate() }
            start()
        }
        ringBox.addView(ring, FrameLayout.LayoutParams(Ui.dp(a, 84), Ui.dp(a, 84)))
        val pct = Ui.text(a, "${prog.readinessPct}%", 17f, onHero, bold = true)
        val pctLp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        pctLp.gravity = Gravity.CENTER
        ringBox.addView(pct, pctLp)
        heroRow.addView(ringBox)
        hero.addView(heroRow)
        col.addView(hero)

        // ---- availability / safety framing ---------------------------------
        if (!prog.available) {
            val info = Ui.card(a, Ui.INFO_BG)
            info.addView(Ui.text(a, "Unlocks in the strengthening phase", 15.5f, Ui.ON_INFO_BG, bold = true))
            info.addView(Ui.spacer(a, 2))
            info.addView(Ui.text(a, "These stages open around phase ${prog.startPhase}, once you are out of the " +
                "boot and rebuilding strength. You can read ahead now to see exactly what you'll be working toward.",
                14f, Ui.ON_INFO_BG))
            col.addView(info)
        }
        col.addView(Ui.spacer(a, 4))
        col.addView(Ui.pillBadge(a, "A guide, not a clearance - your physio signs off each step", Ui.WARN, Ui.WARN_BG))

        // ---- the ladder -----------------------------------------------------
        for (status in prog.rungs.sortedBy { it.rung.order }) {
            col.addView(rungCard(a, status))
        }

        // ---- red flags reminder --------------------------------------------
        col.addView(Ui.spacer(a, 6))
        col.addView(Ui.listRow(a, "ic_alert", "Red flags",
            "New snap, calf pain, swelling - check before pushing on",
            iconTint = Ui.DANGER, iconBg = Ui.DANGER_BG) { a.pushOverlay { RedFlagsScreen.build(a) } })

        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    private fun pickSport(a: MainActivity, protocol: InjuryProtocol, currentId: String) {
        val sports = protocol.supportedSportIds.mapNotNull { SportRegistry.byId(it) }
        if (sports.isEmpty()) return
        val names = sports.map { it.name }.toTypedArray()
        val current = sports.indexOfFirst { it.id == currentId }
        AlertDialog.Builder(a)
            .setTitle("What are you working back to?")
            .setSingleChoiceItems(names, current) { d, which ->
                a.store.saveProfile(a.store.profile().copy(sportId = sports[which].id))
                d.dismiss()
                a.refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ------------------------------------------------------------------

    private fun rungCard(a: MainActivity, status: ReturnToSport.RungStatus): View {
        val rung = status.rung
        val (badgeIcon, badgeTint, badgeBg, chip, chipColor) = when (status.state) {
            ReturnToSport.RungState.CLEARED -> Quint("ic_check", Ui.DONE, Ui.DONE_BG, "Cleared", Ui.DONE)
            ReturnToSport.RungState.CURRENT -> Quint("ic_play", Ui.ON_PRIMARY_CONTAINER, Ui.PRIMARY_CONTAINER, "In progress", Ui.PRIMARY)
            ReturnToSport.RungState.LOCKED -> Quint("ic_shield", Ui.TEXT_DIM, Ui.SURFACE_HIGH, "Locked", Ui.TEXT_DIM)
        }
        val card = Ui.card(a)
        val head = Ui.row(a)
        head.gravity = Gravity.CENTER_VERTICAL
        head.addView(Ui.iconBadge(a, badgeIcon, badgeTint, badgeBg, boxDp = 38))
        val titles = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        titles.setPadding(Ui.dp(a, 12), 0, Ui.dp(a, 8), 0)
        val dim = status.state == ReturnToSport.RungState.LOCKED
        titles.addView(Ui.text(a, "Stage ${rung.order} · ${rung.title}", 15.5f,
            if (dim) Ui.TEXT_DIM else Ui.TEXT, bold = true))
        titles.addView(Ui.text(a, chip, 12.5f, chipColor, bold = true))
        head.addView(Ui.weight(titles, 1f))
        card.addView(head)
        card.addView(Ui.spacer(a, 6))
        card.addView(Ui.text(a, rung.summary, 14f, if (dim) Ui.TEXT_DIM else Ui.TEXT))

        when (status.state) {
            ReturnToSport.RungState.LOCKED -> {
                card.addView(Ui.spacer(a, 6))
                val reason = "Opens once you clear the stage above" +
                    (if (rung.phase > 1) " and reach phase ${rung.phase}" else "") + "."
                card.addView(Ui.caption(a, reason))
                // show the targets so the user can see what's coming
                card.addView(Ui.spacer(a, 6))
                for (t in status.tests) card.addView(targetLine(a, t, faded = true))
            }
            ReturnToSport.RungState.CURRENT -> {
                if (rung.guidance.isNotEmpty()) {
                    card.addView(Ui.spacer(a, 6))
                    for (g in rung.guidance) {
                        card.addView(Ui.text(a, "• $g", 13.5f, Ui.TEXT_DIM))
                    }
                }
                card.addView(Ui.spacer(a, 8))
                card.addView(Ui.text(a, "Tests to pass", 13f, Ui.TEXT_DIM, bold = true))
                for (t in status.tests) card.addView(testRow(a, t))
                // physio sign-off, once every test is green
                if (rung.requiresPhysioSignoff) {
                    card.addView(Ui.spacer(a, 8))
                    if (status.testsMet) {
                        val sign = Ui.card(a, Ui.INFO_BG)
                        sign.addView(Ui.text(a, "Every test passed", 14.5f, Ui.ON_INFO_BG, bold = true))
                        sign.addView(Ui.spacer(a, 2))
                        sign.addView(Ui.text(a, "This stage involves more load or impact. Record it only once your " +
                            "physio has explicitly cleared you for it.", 13.5f, Ui.ON_INFO_BG))
                        sign.addView(Ui.fullWidth(Ui.button(a, "My physio cleared this stage") {
                            Forms.confirm(a, "Confirm physio clearance",
                                "Has your physiotherapist explicitly cleared you for \"${rung.title}\"?") {
                                a.store.setRtsSignoff(rung.id, true)
                                a.refresh()
                            }
                        }, a))
                        card.addView(sign)
                    } else {
                        card.addView(Ui.caption(a, "When every test is green, you'll record your physio's sign-off here."))
                    }
                }
            }
            ReturnToSport.RungState.CLEARED -> {
                card.addView(Ui.spacer(a, 6))
                for (t in status.tests) card.addView(targetLine(a, t, faded = false))
                if (status.physioSignedOff) {
                    card.addView(Ui.spacer(a, 4))
                    val r = Ui.row(a)
                    r.addView(Ui.icon(a, "ic_check", 16, Ui.DONE))
                    val tv = Ui.text(a, "Physio cleared", 12.5f, Ui.DONE, bold = true)
                    tv.setPadding(Ui.dp(a, 6), 0, Ui.dp(a, 10), 0)
                    r.addView(tv)
                    r.addView(Ui.textButton(a, "Undo", Ui.TEXT_DIM) {
                        Forms.confirm(a, "Undo physio sign-off?",
                            "This stage will reopen until you record clearance again.") {
                            a.store.setRtsSignoff(rung.id, false)
                            a.refresh()
                        }
                    })
                    card.addView(r)
                }
            }
        }
        return card
    }

    /** A passable test in the current stage: value/target, state chip, log + how-to. */
    private fun testRow(a: MainActivity, t: ReturnToSport.TestStatus): View {
        val wrap = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        wrap.setPadding(0, Ui.dp(a, 8), 0, 0)
        val row = Ui.row(a)
        row.gravity = Gravity.CENTER_VERTICAL
        val (chipText, chipColor) = when (t.state) {
            ReturnToSport.TestState.PASS -> "Pass" to Ui.DONE
            ReturnToSport.TestState.FAIL -> "Keep going" to Ui.WARN
            ReturnToSport.TestState.UNTESTED -> "Not tested" to Ui.TEXT_DIM
        }
        row.addView(Ui.icon(a, if (t.state == ReturnToSport.TestState.PASS) "ic_check" else "ic_pulse", 18,
            if (t.state == ReturnToSport.TestState.PASS) Ui.DONE else Ui.TEXT_DIM))
        val texts = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        texts.setPadding(Ui.dp(a, 10), 0, Ui.dp(a, 8), 0)
        texts.addView(Ui.text(a, t.test.name, 14.5f, Ui.TEXT, bold = true))
        val sub = buildString {
            append("Target ${t.test.targetLabel()}")
            t.latest?.let { append("  ·  you: ${t.test.valueLabel(it)}") }
            if (t.test.requirePainFree) append("  ·  pain-free")
        }
        texts.addView(Ui.text(a, sub, 12.5f, Ui.TEXT_DIM))
        row.addView(Ui.weight(texts, 1f))
        row.addView(Ui.text(a, chipText, 12.5f, chipColor, bold = true))
        wrap.addView(row)
        val actions = Ui.row(a)
        actions.addView(Ui.weight(Ui.textButton(a, "How to test") {
            Forms.info(a, t.test.name,
                t.test.howTo.joinToString("\n\n") { "• $it" } + "\n\n" + t.test.precaution)
        }, 1f))
        actions.addView(Ui.weight(Ui.textButton(a, "Log result") {
            a.pushOverlay { logTest(a, t.test) }
        }, 1f))
        wrap.addView(actions)
        return wrap
    }

    /** Compact target/result line for locked (faded) and cleared stages. */
    private fun targetLine(a: MainActivity, t: ReturnToSport.TestStatus, faded: Boolean): View {
        val row = Ui.row(a)
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(0, Ui.dp(a, 3), 0, Ui.dp(a, 3))
        val pass = t.state == ReturnToSport.TestState.PASS
        row.addView(Ui.icon(a, if (pass) "ic_check" else "ic_pulse", 15,
            if (faded) Ui.TEXT_DIM else if (pass) Ui.DONE else Ui.TEXT_DIM))
        val label = Ui.text(a, "${t.test.name} · ${t.test.targetLabel()}", 13f,
            if (faded) Ui.TEXT_DIM else Ui.TEXT)
        label.setPadding(Ui.dp(a, 8), 0, 0, 0)
        row.addView(Ui.weight(label, 1f))
        return row
    }

    // ------------------------------------------------------------------

    private fun numField(a: MainActivity, hint: String): EditText =
        Forms.editText(a, "", hint).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

    private fun logTest(a: MainActivity, test: SelfTest): View {
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Log: ${test.name}") { a.popOverlay() })
        col.addView(Ui.caption(a, test.precaution))
        col.addView(Ui.spacer(a, 4))

        val howCard = Ui.card(a)
        howCard.addView(Ui.text(a, "How to test", 14f, Ui.TEXT, bold = true))
        howCard.addView(Ui.spacer(a, 4))
        test.howTo.forEachIndexed { i, h ->
            if (i > 0) howCard.addView(Ui.spacer(a, 4))
            howCard.addView(Ui.text(a, "${i + 1}. $h", 13.5f, Ui.TEXT))
        }
        col.addView(howCard)

        val form = Ui.card(a)
        form.addView(Forms.label(a, if (test.symmetry) "Injured side (${test.unit})" else "Result (${test.unit})"))
        val injured = numField(a, "e.g. 18")
        form.addView(injured)
        var other: EditText? = null
        if (test.symmetry) {
            form.addView(Forms.label(a, "Other side (${test.unit})"))
            other = numField(a, "e.g. 22")
            form.addView(other)
        }
        var painFree = true
        form.addView(Forms.label(a, "Pain-free?"))
        form.addView(Forms.choiceRow(a, listOf(true, false), { if (it) "Yes" else "No" }, painFree) { painFree = it })
        form.addView(Forms.label(a, "Note · optional"))
        val note = Forms.editText(a, "", "How it felt, conditions, support used", multiline = true)
        form.addView(note)
        col.addView(form)

        col.addView(Ui.fullWidth(Ui.button(a, "Save result") {
            val inj = injured.text.toString().trim().toDoubleOrNull()
            if (inj == null) {
                Forms.info(a, "Enter a number", "Please enter your ${test.unit} result for the injured side.")
                return@button
            }
            val oth = other?.text?.toString()?.trim()?.toDoubleOrNull()
            if (test.symmetry && (oth == null || oth <= 0.0)) {
                Forms.info(a, "Both sides needed", "Symmetry needs the other side's ${test.unit} too.")
                return@button
            }
            a.store.saveSelfTestResult(SelfTestResult(
                id = UUID.randomUUID().toString(),
                testId = test.id,
                date = LocalDate.now(),
                injuredValue = inj,
                otherValue = if (test.symmetry) oth else null,
                painFree = painFree,
                note = note.text.toString().trim()
            ))
            Toast.makeText(a, "Result saved", Toast.LENGTH_SHORT).show()
            a.popOverlay()
        }, a))

        // recent history for this test
        val history = a.store.selfTestResults().filter { it.testId == test.id }.sortedByDescending { it.date }.take(6)
        if (history.isNotEmpty()) {
            col.addView(Ui.section(a, "Recent results"))
            val hist = Ui.card(a)
            history.forEachIndexed { i, r ->
                if (i > 0) hist.addView(Ui.divider(a))
                val row = Ui.row(a)
                row.gravity = Gravity.CENTER_VERTICAL
                val passed = ReturnToSport.passes(test, r)
                row.addView(Ui.icon(a, if (passed) "ic_check" else "ic_pulse", 16, if (passed) Ui.DONE else Ui.TEXT_DIM))
                val tv = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
                tv.setPadding(Ui.dp(a, 8), 0, Ui.dp(a, 8), 0)
                tv.addView(Ui.text(a, test.valueLabel(r) + (if (r.painFree) "" else " · with pain"), 14f, Ui.TEXT, bold = true))
                tv.addView(Ui.caption(a, r.date.toString() + if (r.note.isNotBlank()) " · ${r.note}" else ""))
                row.addView(Ui.weight(tv, 1f))
                row.addView(Ui.iconButton(a, "ic_close", Ui.TEXT_DIM, desc = "Delete result") {
                    Forms.confirm(a, "Delete this result?", "It will be removed from your history.") {
                        a.store.deleteSelfTestResult(r.id)
                        a.refresh()
                    }
                })
                hist.addView(row)
            }
            col.addView(hist)
        }

        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    /** Tiny 5-tuple holder so rungCard can destructure its style. */
    private data class Quint(val a: String, val b: Int, val c: Int, val d: String, val e: Int)
}
