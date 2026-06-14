package com.recoverwell.app.ai

import com.recoverwell.core.json.Json
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal Groq client (OpenAI-compatible REST). Blocking - always call from a
 * background thread, never the UI thread.
 *
 * Deliberately dependency-free: uses [HttpURLConnection] and the core JSON
 * helper rather than a networking library, to stay within the app's offline
 * dx/no-invokedynamic build constraints.
 */
object Groq {

    /** Fast, capable general model on Groq; good enough for short rehab answers. */
    const val CHAT_MODEL = "llama-3.3-70b-versatile"
    private const val CHAT_URL = "https://api.groq.com/openai/v1/chat/completions"

    class GroqException(message: String) : Exception(message)

    /** Single-turn chat completion. Returns the assistant's reply text. */
    fun chat(apiKey: String, system: String, user: String, model: String = CHAT_MODEL): String {
        if (apiKey.isBlank()) throw GroqException("No API key set")
        return parseReply(post(CHAT_URL, apiKey, requestBody(system, user, model)))
    }

    /** Build the chat-completion request JSON. Pure - unit-tested. */
    internal fun requestBody(system: String, user: String, model: String = CHAT_MODEL): String =
        Json.write(
            Json.obj(
                "model" to Json.of(model),
                "temperature" to Json.of(0.4),
                "messages" to Json.arr(
                    listOf(
                        Json.obj("role" to Json.of("system"), "content" to Json.of(system)),
                        Json.obj("role" to Json.of("user"), "content" to Json.of(user))
                    )
                )
            )
        )

    /** Extract the assistant text from a chat-completion response. Pure - unit-tested. */
    internal fun parseReply(responseJson: String): String {
        val choices = Json.parse(responseJson).opt("choices")?.asArr().orEmpty()
        if (choices.isEmpty()) throw GroqException("Empty response from Groq")
        return choices[0].get("message").get("content").asString().trim()
    }

    private fun post(urlStr: String, apiKey: String, body: String): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 60000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
        }
        try {
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            if (code !in 200..299) throw GroqException(extractError(text, code))
            return text
        } finally {
            conn.disconnect()
        }
    }

    /** Pull Groq's human-readable error message out of the JSON body when present. */
    internal fun extractError(text: String, code: Int): String =
        try {
            Json.parse(text).opt("error")?.opt("message")?.asString() ?: "Request failed (HTTP $code)"
        } catch (e: Exception) {
            "Request failed (HTTP $code)"
        }
}
