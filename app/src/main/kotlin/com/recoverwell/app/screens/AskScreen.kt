package com.recoverwell.app.screens

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.recoverwell.app.MainActivity
import com.recoverwell.app.ui.Forms
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.logic.Ask
import java.time.LocalDate

/**
 * "Ask my recovery" - an offline question box answered deterministically from
 * the active protocol's own data. No cloud, no guessing; every answer is the
 * protocol's content.
 */
object AskScreen {

    private var lastAnswer: Ask.Answer? = null

    fun build(a: MainActivity): View {
        val today = LocalDate.now()
        val profile = a.store.profile()
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Ask my recovery") { a.popOverlay() })
        col.addView(Ui.caption(a, "Answered from your protocol, fully offline. " +
            "Not a substitute for your physio."))
        col.addView(Ui.spacer(a, 8))

        // free-text box
        val input = Forms.editText(a, "", "Type a question, e.g. \"Can I drive yet?\"")
        col.addView(input)
        col.addView(Ui.fullWidth(Ui.button(a, "Ask") {
            lastAnswer = Ask.answer(input.text.toString().ifBlank { "what can I do right now" }, profile, today)
            a.refresh()
        }, a))

        // answer sits directly under the box so it's visible the instant you ask
        // or tap a suggestion - no scrolling to find it
        lastAnswer?.let { ans ->
            col.addView(Ui.section(a, "Answer"))
            val card = Ui.card(a, Ui.INFO_BG)
            card.addView(Ui.text(a, ans.title, 16f, Ui.ON_INFO_BG, bold = true))
            card.addView(Ui.spacer(a, 4))
            card.addView(Ui.text(a, ans.body, 14.5f, Ui.ON_INFO_BG))
            when (ans.action) {
                Ask.Action.OPEN_RED_FLAGS -> card.addView(Ui.fullWidth(Ui.dangerButton(a, "Open red flags") {
                    a.pushOverlay { RedFlagsScreen.build(a) }
                }, a))
                Ask.Action.OPEN_PHASE_GUIDE -> card.addView(Ui.fullWidth(Ui.tonalButton(a, "Open phase guide") {
                    val n = com.recoverwell.core.logic.PhaseEngine.currentPhase(profile, today).number
                    a.pushOverlay { TodayScreen.phaseDetail(a, n) }
                }, a))
                Ask.Action.NONE -> {}
            }
            col.addView(card)
        }

        // suggested questions
        col.addView(Ui.section(a, if (lastAnswer == null) "Try" else "Ask something else"))
        for (q in Ask.suggestions(profile)) {
            col.addView(Ui.listRow(a, "ic_info", q, null, chevron = true) {
                lastAnswer = Ask.answer(q, profile, today)
                a.refresh()
            })
        }

        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }
}
