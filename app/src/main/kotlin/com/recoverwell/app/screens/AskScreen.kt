package com.recoverwell.app.screens

import android.view.View
import com.recoverwell.app.MainActivity
import com.recoverwell.app.ai.AiContext
import com.recoverwell.app.ai.Groq
import com.recoverwell.app.ui.Forms
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.logic.Ask
import java.time.LocalDate

/**
 * "Ask my recovery" - a question box about the user's rehab.
 *
 * Offline by default: answers come deterministically from the active protocol's
 * own data (see [Ask]). When the user has opted into AI (see [AiScreen]), the
 * question is instead answered in natural language by Groq, grounded in their
 * recovery context - with the offline answer kept as a fallback if the call fails.
 */
object AskScreen {

    private var lastAnswer: Ask.Answer? = null
    private var lastQuestion: String = ""
    private var aiText: String? = null
    private var aiError: String? = null
    private var loading = false

    fun build(a: MainActivity): View {
        val today = LocalDate.now()
        val profile = a.store.profile()
        val ai = AiScreen.enabled(a)
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Ask my recovery") { a.popOverlay() })
        col.addView(Ui.caption(a, if (ai)
            "Answered by AI from your recovery context. General guidance, not a substitute for your physio."
        else
            "Answered from your protocol, fully offline. Not a substitute for your physio."))
        col.addView(Ui.spacer(a, 8))

        // free-text box
        val input = Forms.editText(a, lastQuestion, "Type a question, e.g. \"Can I drive yet?\"")
        col.addView(input)
        col.addView(Ui.fullWidth(Ui.button(a, if (loading) "Thinking…" else "Ask") {
            if (loading) return@button
            val q = input.text.toString().ifBlank { "what can I do right now" }
            ask(a, q, profile, today, ai)
        }, a))

        when {
            loading -> {
                col.addView(Ui.section(a, "Answer"))
                val card = Ui.card(a, Ui.INFO_BG)
                card.addView(Ui.text(a, "Thinking…", 15f, Ui.ON_INFO_BG, bold = true))
                card.addView(Ui.text(a, "Asking your AI assistant.", 14f, Ui.ON_INFO_BG))
                col.addView(card)
            }
            aiText != null -> {
                col.addView(Ui.section(a, "Answer"))
                val card = Ui.card(a, Ui.INFO_BG)
                card.addView(Ui.text(a, aiText!!, 15f, Ui.ON_INFO_BG))
                card.addView(Ui.spacer(a, 6))
                card.addView(Ui.text(a, "AI-generated · always check with your physio.", 12f, Ui.ON_INFO_BG))
                col.addView(card)
            }
            aiError != null -> {
                col.addView(Ui.section(a, "Answer"))
                val warn = Ui.card(a, Ui.WARN_BG)
                warn.addView(Ui.text(a, "Couldn't reach the AI assistant", 14.5f, Ui.WARN, bold = true))
                warn.addView(Ui.text(a, aiError!!, 13.5f, Ui.TEXT))
                col.addView(warn)
                // fall back to the offline answer so the question still gets a response
                lastAnswer?.let { col.addView(answerCard(a, it, profile, today)) }
            }
            lastAnswer != null -> col.addView(answerCard(a, lastAnswer!!, profile, today))
        }

        // suggested questions
        col.addView(Ui.section(a, if (lastAnswer == null && aiText == null) "Try" else "Ask something else"))
        for (q in Ask.suggestions(profile)) {
            col.addView(Ui.listRow(a, "ic_info", q, null, chevron = true) {
                ask(a, q, profile, today, ai)
            })
        }

        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    /** Run a question: AI path (background) when enabled, else the offline answer. */
    private fun ask(a: MainActivity, q: String, profile: com.recoverwell.core.model.Profile,
                    today: LocalDate, ai: Boolean) {
        lastQuestion = q
        aiText = null
        aiError = null
        // always compute the offline answer; it's instant and doubles as the AI fallback
        lastAnswer = Ask.answer(q, profile, today)
        if (!ai) {
            a.refresh()
            return
        }
        loading = true
        a.refresh()
        val system = AiContext.system(profile, a.store.allLogs(), today)
        val key = AiScreen.apiKey(a)
        Thread {
            var ok: String? = null
            var err: String? = null
            try {
                ok = Groq.chat(key, system, q)
            } catch (e: Exception) {
                err = e.message ?: "Something went wrong"
            }
            a.runOnUiThread {
                loading = false
                aiText = ok
                aiError = err
                a.refresh()
            }
        }.start()
    }

    private fun answerCard(a: MainActivity, ans: Ask.Answer,
                           profile: com.recoverwell.core.model.Profile, today: LocalDate): View {
        val card = Ui.card(a, Ui.INFO_BG)
        card.addView(Ui.text(a, ans.title, 16f, Ui.ON_INFO_BG, bold = true))
        card.addView(Ui.spacer(a, 4))
        card.addView(Ui.text(a, ans.body, 14.5f, Ui.ON_INFO_BG))
        when (ans.action) {
            Ask.Action.OPEN_RED_FLAGS -> card.addView(Ui.fullWidth(Ui.dangerButton(a, "Open red flags") {
                a.pushOverlay("Red flags") { RedFlagsScreen.build(a) }
            }, a))
            Ask.Action.OPEN_PHASE_GUIDE -> card.addView(Ui.fullWidth(Ui.tonalButton(a, "Open phase guide") {
                val n = com.recoverwell.core.logic.PhaseEngine.currentPhase(profile, today).number
                a.pushOverlay("Phase $n") { TodayScreen.phaseDetail(a, n) }
            }, a))
            Ask.Action.NONE -> {}
        }
        return card
    }
}
