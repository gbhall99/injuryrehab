package com.recoverwell.app.ai

import com.recoverwell.core.json.Json
import java.io.File
import java.io.OutputStream
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
    /** Groq-hosted Whisper; fast and accurate enough for short spoken check-ins. */
    const val TRANSCRIBE_MODEL = "whisper-large-v3-turbo"
    private const val CHAT_URL = "https://api.groq.com/openai/v1/chat/completions"
    private const val TRANSCRIBE_URL = "https://api.groq.com/openai/v1/audio/transcriptions"

    class GroqException(message: String) : Exception(message)

    /** Single-turn chat completion. Returns the assistant's reply text. */
    fun chat(apiKey: String, system: String, user: String,
             model: String = CHAT_MODEL, jsonMode: Boolean = false): String {
        if (apiKey.isBlank()) throw GroqException("No API key set")
        return parseReply(post(CHAT_URL, apiKey, requestBody(system, user, model, jsonMode)))
    }

    /** Transcribe a recorded audio file via Whisper. Returns the spoken text. */
    fun transcribe(apiKey: String, audio: File, model: String = TRANSCRIBE_MODEL): String {
        if (apiKey.isBlank()) throw GroqException("No API key set")
        return parseTranscript(postMultipart(apiKey, audio, model))
    }

    /** Build the chat-completion request JSON. Pure - unit-tested. */
    internal fun requestBody(system: String, user: String,
                             model: String = CHAT_MODEL, jsonMode: Boolean = false): String {
        val fields = mutableListOf(
            "model" to Json.of(model),
            "temperature" to Json.of(0.4),
            "messages" to Json.arr(
                listOf(
                    Json.obj("role" to Json.of("system"), "content" to Json.of(system)),
                    Json.obj("role" to Json.of("user"), "content" to Json.of(user))
                )
            )
        )
        if (jsonMode) fields.add("response_format" to Json.obj("type" to Json.of("json_object")))
        return Json.write(Json.obj(*fields.toTypedArray()))
    }

    /** Extract the transcript text from a Whisper response. Pure - unit-tested. */
    internal fun parseTranscript(responseJson: String): String =
        Json.parse(responseJson).opt("text")?.asString()?.trim()
            ?: throw GroqException("No transcription returned")

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

    private fun postMultipart(apiKey: String, audio: File, model: String): String {
        val boundary = "----recoverwell" + System.nanoTime()
        val conn = (URL(TRANSCRIBE_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 120000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }
        try {
            conn.outputStream.use { out ->
                writeField(out, boundary, "model", model)
                writeField(out, boundary, "response_format", "json")
                out.write("--$boundary\r\n".toByteArray(Charsets.UTF_8))
                out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"${audio.name}\"\r\n")
                    .toByteArray(Charsets.UTF_8))
                out.write("Content-Type: audio/m4a\r\n\r\n".toByteArray(Charsets.UTF_8))
                audio.inputStream().use { it.copyTo(out) }
                out.write("\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8))
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            if (code !in 200..299) throw GroqException(extractError(text, code))
            return text
        } finally {
            conn.disconnect()
        }
    }

    private fun writeField(out: OutputStream, boundary: String, name: String, value: String) {
        out.write("--$boundary\r\n".toByteArray(Charsets.UTF_8))
        out.write("Content-Disposition: form-data; name=\"$name\"\r\n\r\n".toByteArray(Charsets.UTF_8))
        out.write((value + "\r\n").toByteArray(Charsets.UTF_8))
    }

    /** Pull Groq's human-readable error message out of the JSON body when present. */
    internal fun extractError(text: String, code: Int): String =
        try {
            Json.parse(text).opt("error")?.opt("message")?.asString() ?: "Request failed (HTTP $code)"
        } catch (e: Exception) {
            "Request failed (HTTP $code)"
        }
}
