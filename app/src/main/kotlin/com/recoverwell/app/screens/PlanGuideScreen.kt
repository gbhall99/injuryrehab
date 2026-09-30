package com.recoverwell.app.screens

import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.recoverwell.app.MainActivity
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.logic.PhaseEngine
import com.recoverwell.core.protocol.ProtocolRegistry
import java.time.LocalDate

/**
 * The whole plan in one place, phase by phase: what each phase is for, what's OK
 * and what's not yet, what's typical to feel along the way, the boot, and the
 * "normal or worth a call?" guide. The current phase is open; tap another to
 * read ahead. It replaces four overlapping places (What to expect's whole
 * journey, How you're doing, the single-phase guide, My leg's phase reference).
 */
object PlanGuideScreen {

    /** The card that's open: a phase number, [BOOT], [NORMAL], [NONE]; null = the current phase. */
    private var open: Int? = null
    private const val NONE = -1
    private const val BOOT = -2
    private const val NORMAL = -3

    fun build(a: MainActivity, focusPhase: Int? = null): View {
        if (focusPhase != null) open = focusPhase
        val today = LocalDate.now()
        val profile = a.store.profile()
        val protocol = ProtocolRegistry.forProfile(profile)
        val current = PhaseEngine.currentPhase(profile, today).number
        val openPhase = open ?: current
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Your plan") { open = null; a.popOverlay() })
        col.addView(Ui.caption(a, "Typical timing - your physio confirms each step. Tap a phase to read it."))

        for (ph in protocol.phases) {
            val isOpen = ph.number == openPhase
            val here = ph.number == current
            val card = Ui.card(a, if (here) Ui.PRIMARY_CONTAINER else Ui.CARD)
            val fg = if (here) Ui.ON_PRIMARY_CONTAINER else Ui.TEXT
            val head = Ui.row(a).apply {
                isClickable = true
                isFocusable = true
                contentDescription = "Phase ${ph.number}, ${ph.title}" + (if (here) ", now" else "") +
                    if (isOpen) ", open" else ", tap to read"
                setOnClickListener { open = if (isOpen) NONE else ph.number; a.refresh() }
            }
            val titles = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
            titles.addView(Ui.text(a, "Phase ${ph.number} · ${ph.title}", 15.5f, fg, bold = true))
            val weeks = ph.endWeek?.let { "Weeks ${ph.startWeek}-$it" } ?: "From week ${ph.startWeek}"
            titles.addView(Ui.text(a, "$weeks · ${ph.subtitle}", 13f, fg))
            head.addView(Ui.weight(titles, 1f))
            if (here) head.addView(Ui.pillBadge(a, "Now", com.recoverwell.draw.Palette.ON_PRIMARY, Ui.PRIMARY))
            card.addView(head)
            if (isOpen) {
                fun bullets(heading: String, lines: List<String>, color: Int = fg) {
                    if (lines.isEmpty()) return
                    card.addView(Ui.spacer(a, 10))
                    card.addView(Ui.text(a, heading, 13f, fg, bold = true))
                    for (l in lines) {
                        val r = Ui.row(a)
                        r.gravity = Gravity.TOP
                        r.addView(Ui.text(a, "·", 14f, color, bold = true).apply { setPadding(Ui.dp(a, 2), 0, Ui.dp(a, 8), 0) })
                        r.addView(Ui.weight(Ui.text(a, l, 14f, fg), 1f))
                        card.addView(r)
                    }
                }
                if (ph.tissueState.isNotBlank()) {
                    card.addView(Ui.spacer(a, 8))
                    card.addView(Ui.text(a, "Your tendon: " + ph.tissueState, 14f, fg))
                }
                bullets("Goals", ph.goals)
                bullets("OK in this phase", ph.allowed, Ui.DONE)
                bullets("Not yet", ph.notAllowed, Ui.DANGER)
                bullets("Take care", ph.precautions, Ui.WARN)
                // what's typical, week by week, when reading ahead (for the phase you're in,
                // My leg's "What to expect now" already says it)
                val end = ph.endWeek ?: Int.MAX_VALUE
                if (ph.number != current) bullets("Along the way", protocol.expectations
                    .filter { it.weekFrom >= ph.startWeek && it.weekFrom < end }
                    .map { "Week ${it.weekFrom}+ · ${it.title}: ${it.summary}" })
                protocol.mindset.firstOrNull { it.phase == ph.number }?.let { bullets("Normal to feel", it.normalToFeel) }
                // how a phase starts matters when reading ahead, not once you're in it
                if (ph.number > current) bullets("Starts when", ph.entryCriteria)
            }
            col.addView(card)
        }

        // the boot and "normal or worth a call?" fold like the phases: read when wanted
        protocol.supportDevice?.let { device ->
            col.addView(fold(a, "Your ${device.name.lowercase()}", "How it's set up and worn", openPhase == BOOT,
                { open = if (openPhase == BOOT) NONE else BOOT; a.refresh() }) { card ->
                if (device.operation.isNotBlank()) card.addView(Ui.text(a, device.operation, 14f, Ui.TEXT))
                for (n in device.setupNotes) {
                    card.addView(Ui.spacer(a, 6))
                    val r = Ui.row(a)
                    r.gravity = Gravity.TOP
                    r.addView(Ui.icon(a, "ic_check", 16, Ui.PRIMARY))
                    r.addView(Ui.weight(Ui.text(a, n, 14f, Ui.TEXT).apply { setPadding(Ui.dp(a, 10), 0, 0, 0) }, 1f))
                    card.addView(r)
                }
            })
        }

        // the signature fear, answered: what's usually normal vs what to tell the clinic
        protocol.reassurance?.let { r ->
            col.addView(fold(a, r.title, "Usually normal, or tell your clinic?", openPhase == NORMAL,
                { open = if (openPhase == NORMAL) NONE else NORMAL; a.refresh() }) { card ->
                card.addView(Ui.text(a, r.body, 14f, Ui.TEXT))
                card.addView(Ui.spacer(a, 10))
                val heads = Ui.row(a)
                heads.addView(Ui.weight(Ui.text(a, "Usually normal", 12.5f, Ui.DONE, bold = true), 1f))
                heads.addView(Ui.weight(Ui.text(a, "Tell your clinic", 12.5f, Ui.DANGER, bold = true), 1f))
                card.addView(heads)
                for ((normal, flag) in r.normalVsFlag) {
                    card.addView(Ui.divider(a))
                    val row = Ui.row(a)
                    row.gravity = Gravity.TOP
                    row.addView(Ui.weight(Ui.text(a, normal, 13.5f, Ui.TEXT).apply { setPadding(0, 0, Ui.dp(a, 8), 0) }, 1f))
                    row.addView(Ui.weight(Ui.text(a, flag, 13.5f, Ui.ON_DANGER_BG), 1f))
                    card.addView(row)
                }
            })
        }

        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    /** A card with a tappable title line that opens to [body]. */
    private fun fold(a: MainActivity, title: String, subtitle: String, isOpen: Boolean,
                     toggle: () -> Unit, body: (LinearLayout) -> Unit): View {
        val card = Ui.card(a)
        val head = Ui.row(a).apply {
            isClickable = true
            isFocusable = true
            minimumHeight = Ui.dp(a, Ui.MIN_TOUCH_DP)
            contentDescription = "$title, " + if (isOpen) "open" else "tap to read"
            setOnClickListener { toggle() }
        }
        val titles = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(Ui.text(a, title, 15.5f, Ui.TEXT, bold = true))
        titles.addView(Ui.text(a, subtitle, 13f, Ui.TEXT))
        head.addView(Ui.weight(titles, 1f))
        head.addView(Ui.icon(a, "ic_chevron", 18, Ui.TEXT_DIM).apply { rotation = if (isOpen) 90f else 0f })
        card.addView(head)
        if (isOpen) {
            card.addView(Ui.spacer(a, 10))
            body(card)
        }
        return card
    }
}
