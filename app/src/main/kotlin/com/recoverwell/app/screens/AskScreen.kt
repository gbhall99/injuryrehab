package com.recoverwell.app.screens

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import com.recoverwell.app.MainActivity
import com.recoverwell.app.ai.AiContext
import com.recoverwell.app.ai.Groq
import com.recoverwell.app.ui.Forms
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.logic.Ask
import java.time.LocalDate

/**
 * "Recovery coach".
 *
 * Offline by default: deterministic answers from the active protocol's data
 * (see [Ask]). When AI is enabled (see [AiScreen]), it becomes a grounded,
 * multi-turn chat via Groq - follow-up questions keep their context - while the
 * deterministic answer remains the fallback if a call fails.
 *
 * The AI conversation is persisted (so it resumes where it left off) and feeds a
 * compact "memory bank" of past exchanges back into the prompt, giving the
 * assistant continuity across separate chats.
 */
object AskScreen {

    // offline mode state
    private var lastAnswer: Ask.Answer? = null

    // AI conversation state (transient view of the persisted transcript)
    private val turns = ArrayList<Groq.Message>()
    private var loading = false
    private var error: String? = null
    private var lastQuestion: String = ""
    // request a jump to the newest message on the next build (after send/receive)
    private var scrollToEnd = false

    /** Clear the transient view when leaving. The transcript itself is persisted,
     *  so reopening resumes it; only the in-memory copy and flags are dropped. */
    fun reset() {
        turns.clear(); loading = false; error = null
        lastAnswer = null; lastQuestion = ""; scrollToEnd = false
    }

    fun build(a: MainActivity): View {
        val today = LocalDate.now()
        val profile = a.store.profile()
        // resume the persisted conversation when re-entering with an empty view
        if (turns.isEmpty() && !loading) {
            turns.addAll(a.store.askTurns().map { Groq.Message(it.first, it.second) })
        }
        return if (AiScreen.enabled(a)) buildAi(a, profile, today) else buildOffline(a, profile, today)
    }

    // ---- AI conversational mode --------------------------------------------

    private fun buildAi(a: MainActivity, profile: com.recoverwell.core.model.Profile, today: LocalDate): View {
        // Three bands: a fixed header, the scrolling transcript (takes the slack),
        // and a fixed input bar pinned to the bottom so it - and the latest answer -
        // are always reachable, even with the keyboard up.
        val root = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Ui.BG)
        }

        val header = Ui.column(a).apply { setPadding(paddingLeft, paddingTop, paddingRight, 0) }
        header.addView(Ui.backRow(a, "Recovery coach") { a.popOverlay() })
        val head = Ui.row(a)
        head.addView(Ui.weight(Ui.caption(a, "A grounded chat about your recovery. General guidance, " +
            "not a substitute for your physio."), 1f))
        if (turns.isNotEmpty()) head.addView(Ui.textButton(a, "Clear") {
            turns.clear(); error = null; loading = false
            a.store.clearAskTurns()
            a.refresh()
        })
        header.addView(head)
        root.addView(header)

        // ---- transcript -----------------------------------------------------
        val col = Ui.column(a).apply { setPadding(paddingLeft, 0, paddingRight, paddingBottom) }
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

        if (turns.isEmpty()) {
            col.addView(Ui.section(a, "Try"))
            for (q in Ask.suggestions(profile)) {
                col.addView(Ui.listRow(a, "ic_info", q, null, chevron = true) { send(a, q, profile, today) })
            }
            if (a.store.askMemory().isNotEmpty()) {
                col.addView(Ui.spacer(a, 8))
                col.addView(Ui.caption(a, "I'll remember the gist of our past chats to keep advice consistent."))
                col.addView(Ui.fullWidth(Ui.textButton(a, "Forget remembered context", Ui.WARN) {
                    Forms.confirm(a, "Forget remembered context?",
                        "I'll clear the summary of your previous chats. Your logs and journal stay untouched.") {
                        a.store.clearAskMemory(); a.refresh()
                    }
                }, a, 2))
            }
        }
        col.addView(Ui.spacer(a, 12))
        val scroll = Ui.scroll(a, col)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        // ---- pinned input bar ----------------------------------------------
        val bar = Ui.column(a).apply {
            setPadding(paddingLeft, Ui.dp(a, 6), paddingRight, Ui.dp(a, 6))
            setBackgroundColor(Ui.BG)
        }
        val input = Forms.editText(a, "", "Ask a question…")
        bar.addView(input)
        bar.addView(Ui.fullWidth(Ui.button(a, if (loading) "…" else "Send") {
            if (loading) return@button
            val q = input.text.toString().trim()
            if (q.isNotBlank()) send(a, q, profile, today)
        }, a, 8))
        root.addView(bar)

        // keep the newest message in view after sending/receiving
        if (scrollToEnd || loading) {
            scrollToEnd = false
            scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
        }
        return root
    }

    private fun send(a: MainActivity, q: String, profile: com.recoverwell.core.model.Profile, today: LocalDate) {
        turns.add(Groq.Message("user", q))
        error = null
        loading = true
        scrollToEnd = true
        a.store.saveAskTurns(turns.map { it.role to it.content })
        a.refresh()
        // feed the persisted memory bank in so the assistant keeps continuity
        val memory = a.store.askMemory()
        val system = AiContext.system(profile, a.store.allLogs(), today, a.store.journalEntries(), memory)
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
                scrollToEnd = true
                if (reply != null) {
                    turns.add(Groq.Message("assistant", reply))
                    // distil this exchange into the long-lived memory bank
                    a.store.saveAskMemory(a.store.askMemory() + memoryLine(q, reply))
                } else {
                    // keep the question, surface the error + offline fallback below it
                    err = (err ?: "") + "  Showing an offline answer instead."
                    turns.add(Groq.Message("assistant", Ask.answer(q, profile, today).let { "${it.title}\n\n${it.body}" }))
                }
                a.store.saveAskTurns(turns.map { it.role to it.content })
                error = err
                if (!a.isFinishing && !a.isDestroyed) a.refresh()
            }
        }.start()
    }

    /** Condense one Q&A into a single memory line for cross-chat continuity. */
    private fun memoryLine(question: String, reply: String): String {
        val q = question.trim().replace(Regex("\\s+"), " ").take(120)
        val r = reply.trim().replace(Regex("\\s+"), " ").take(180)
        return "Asked \"$q\" - replied: $r"
    }

    // ---- offline deterministic mode ----------------------------------------

    private fun buildOffline(a: MainActivity, profile: com.recoverwell.core.model.Profile, today: LocalDate): View {
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "Recovery coach") { a.popOverlay() })
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
