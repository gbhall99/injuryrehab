package com.recoverwell.app.screens

import android.view.View
import android.widget.Toast
import com.recoverwell.app.MainActivity
import com.recoverwell.app.ai.SecureKey
import com.recoverwell.app.ui.Forms
import com.recoverwell.app.ui.Ui

/**
 * Opt-in AI settings. AI is off by default and stays off until the user pastes a
 * Groq API key AND turns it on - because, unlike the rest of the app, AI sends
 * recovery text to Groq's servers. The privacy trade-off is stated plainly here.
 */
object AiScreen {

    const val KEY_API = "groq_api_key"
    const val KEY_ENABLED = "ai_enabled"

    /** True only when the user has both supplied a key and enabled AI. */
    fun enabled(a: MainActivity): Boolean =
        a.store.setting(KEY_ENABLED, "false") == "true" && a.store.setting(KEY_API, "").isNotBlank()

    /** The decrypted key, for making requests. */
    fun apiKey(a: MainActivity): String = SecureKey.reveal(a.store.setting(KEY_API, ""))

    fun settings(a: MainActivity): View {
        val col = Ui.column(a)
        col.addView(Ui.backRow(a, "AI features") { a.popOverlay() })
        col.addView(Ui.caption(a, "Optional. Lets \"Ask my recovery\" answer in natural language, " +
            "powered by Groq. Everything else in the app stays fully offline."))
        col.addView(Ui.spacer(a, 8))

        // privacy notice - unmissable, because this is the one place data leaves the device
        val notice = Ui.card(a, Ui.WARN_BG)
        notice.addView(Ui.text(a, "Before you turn this on", 14.5f, Ui.WARN, bold = true))
        notice.addView(Ui.spacer(a, 4))
        notice.addView(Ui.text(a, "When AI is on, the question you ask and a short summary of your " +
            "recovery (phase, recent pain/swelling, your goal) are sent to Groq to generate a reply. " +
            "Don't include anything you wouldn't want to leave your phone. AI answers are general " +
            "guidance, not medical advice - always defer to your physio.", 13.5f, Ui.TEXT))
        col.addView(notice)

        // API key
        col.addView(Forms.label(a, "Groq API key"))
        val keyEdit = Forms.editText(a, apiKey(a), "Paste your key (starts with gsk_)")
        col.addView(keyEdit)
        col.addView(Ui.caption(a, "Get a free key at console.groq.com. Stored encrypted on this device only."))

        // a stored-but-undecryptable key (e.g. after a lock-screen change or device
        // restore) blanks the field above - tell the user why, so it isn't mistaken
        // for "never set", and prompt them to paste it again.
        if (SecureKey.isUnreadable(a.store.setting(KEY_API, ""))) {
            val warn = Ui.card(a, Ui.WARN_BG)
            warn.addView(Ui.text(a, "Couldn't read your saved key", 14f, Ui.WARN, bold = true))
            warn.addView(Ui.spacer(a, 4))
            warn.addView(Ui.text(a, "Your encrypted key can no longer be unlocked on this device. " +
                "Please paste it again to keep using AI features.", 13.5f, Ui.TEXT))
            col.addView(warn)
        }

        // enable toggle
        col.addView(Forms.label(a, "AI features"))
        var on = a.store.setting(KEY_ENABLED, "false") == "true"
        col.addView(Forms.choiceRow(a, listOf(false, true), { if (it) "On" else "Off" }, on) { on = it })

        col.addView(Ui.fullWidth(Ui.button(a, "Save") {
            val key = keyEdit.text.toString().trim()
            a.store.saveSetting(KEY_API, SecureKey.protect(key))
            a.store.saveSetting(KEY_ENABLED, if (on && key.isNotBlank()) "true" else "false")
            val msg = when {
                on && key.isBlank() -> "Add a key to turn AI on"
                on -> "AI features on"
                else -> "AI features off"
            }
            Toast.makeText(a, msg, Toast.LENGTH_SHORT).show()
            a.popOverlay()
        }, a))

        col.addView(Ui.spacer(a, 24))
        return Ui.scroll(a, col)
    }
}
