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
 * "Ask my recovery".
 *
 * Offline by default: deterministic answers from the active protocol's data
 * (see [Ask]). When AI is enabled (see [AiScreen]), it becomes a grounded,
 * multi-turn chat via Groq - follow-up questions keep their context - while the
 * deterministic answer remains the fallback if a call fails.
 */
object AskScreen {

    // offline mode state
    private var lastAnswer: Ask.Answer? = null

    // AI conversation state
    private val turns = ArrayList<Groq.Message>()
    private var loading = false
    private var error: String? = null
    private var lastQuestion: String = ""

    /** Clear conversation/offline state when leaving, so reopening starts fresh. */
    fun reset() {
        turns.clear(); loading = false; error = null
        lastAnswer = null; lastQuestion = ""
    }

    fun build(a: MainActivity): View {
        val today = LocalDate.now()
        val profile = a.store.profile()
        return if (AiScreen.enabled(a)) buildAi(a, profile, today) else buildOffline(a, profile, today)
    }

    // ---- AI conversational mode --------------------------------------------

    private fun buildAi(a: MainActivity, profile: com.recoverwell.core.model.Profile, today: LocalDate): View {
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Ask my recovery") { a.popOverlay() })
        val head = Ui.row(a)
        head.addView(Ui.weight(Ui.caption(a, "A grounded chat about your recovery. General guidance, " +
            "not a substitute for your physio."), 1f))
        if (turns.isNotEmpty()) head.addView(Ui.textButton(a, "Clear") {
            turns.clear(); error = null; a.refresh()
        })
        col.addView(head)
        col.addView(Ui.spacer(a, 6))

        for (t in turns) {
            if (t.role == "user") {
                val c = Ui.card(a)
                c.addView(Ui.text(a, "You", 12f, Ui.TEXT_DIM, bold = true))
                c.addView(Ui.text(a, t.content, 14.5f, Ui.TEXT))
                col.addView(c)
            } else {
                val c = Ui.card(a, Ui.INFO_BG)
                c.addView(Ui.text(a, t.content, 14.5f, Ui.ON_INFO_BG))
                col.addView(c)
            }
        }
        if (loading) {
            val c = Ui.card(a, Ui.INFO_BG)
            c.addView(Ui.text(a, "Thinking…", 14.5f, Ui.ON_INFO_BG, bold = true))
            col.addView(c)
        }
        error?.let {
            val w = Ui.card(a, Ui.WARN_BG)
            w.addView(Ui.text(a, it, 13.5f, Ui.TEXT))
            col.addView(w)
        }

        val input = Forms.editText(a, "", "Ask a question…")
        col.addView(input)
        col.addView(Ui.fullWidth(Ui.button(a, if (loading) "…" else "Send") {
            if (loading) return@button
            val q = input.text.toString().trim()
            if (q.isNotBlank()) send(a, q, profile, today)
        }, a))

        if (turns.isEmpty()) {
            col.addView(Ui.section(a, "Try"))
            for (q in Ask.suggestions(profile)) {
                col.addView(Ui.listRow(a, "ic_info", q, null, chevron = true) { send(a, q, profile, today) })
            }
        }

        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }

    private fun send(a: MainActivity, q: String, profile: com.recoverwell.core.model.Profile, today: LocalDate) {
        turns.add(Groq.Message("user", q))
        error = null
        loading = true
        a.refresh()
        val system = AiContext.system(profile, a.store.allLogs(), today, a.store.journalEntries())
        val key = AiScreen.apiKey(a)
        // send recent history only, to keep the payload small
        val history = ArrayList(turns.takeLast(12))
        Thread {
            var reply: String? = null
            var err: String? = null
            try {
                reply = Groq.chat(key, system, history, model = Groq.CHAT_MODEL_FAST)
            } catch (e: Exception) {
                err = e.message ?: "Something went wrong"
            }
            a.runOnUiThread {
                loading = false
                if (reply != null) turns.add(Groq.Message("assistant", reply))
                else {
                    // keep the question, surface the error + offline fallback below it
                    err = (err ?: "") + "  Showing an offline answer instead."
                    turns.add(Groq.Message("assistant", Ask.answer(q, profile, today).let { "${it.title}\n\n${it.body}" }))
                }
                error = err
                if (!a.isFinishing && !a.isDestroyed) a.refresh()
            }
        }.start()
    }

    // ---- offline deterministic mode ----------------------------------------

    private fun buildOffline(a: MainActivity, profile: com.recoverwell.core.model.Profile, today: LocalDate): View {
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Ask my recovery") { a.popOverlay() })
        col.addView(Ui.caption(a, "Answered from your protocol, fully offline. Not a substitute for your physio."))
        col.addView(Ui.spacer(a, 8))

        val input = Forms.editText(a, lastQuestion, "Type a question, e.g. \"Can I drive yet?\"")
        col.addView(input)
        col.addView(Ui.fullWidth(Ui.button(a, "Ask") {
            lastQuestion = input.text.toString()
            lastAnswer = Ask.answer(lastQuestion.ifBlank { "what can I do right now" }, profile, today)
            a.refresh()
        }, a))

        lastAnswer?.let { col.addView(answerCard(a, it, profile, today)) }

        col.addView(Ui.section(a, if (lastAnswer == null) "Try" else "Ask something else"))
        for (q in Ask.suggestions(profile)) {
            col.addView(Ui.listRow(a, "ic_info", q, null, chevron = true) {
                lastAnswer = Ask.answer(q, profile, today); a.refresh()
            })
        }

        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
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
