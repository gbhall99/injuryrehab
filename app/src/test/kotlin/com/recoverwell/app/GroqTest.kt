package com.recoverwell.app

import com.recoverwell.app.ai.Groq
import com.recoverwell.core.json.Json
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the pure request/response plumbing of the Groq client - no
 * network involved (the actual HTTP call is exercised on-device).
 */
class GroqTest {

    @Test
    fun requestBodyHasModelAndBothMessages() {
        val body = Groq.requestBody("be helpful", "can I drive?", "test-model")
        val json = Json.parse(body)
        assertEquals("test-model", json.get("model").asString())
        val messages = json.get("messages").asArr()
        assertEquals(2, messages.size)
        assertEquals("system", messages[0].get("role").asString())
        assertEquals("be helpful", messages[0].get("content").asString())
        assertEquals("user", messages[1].get("role").asString())
        assertEquals("can I drive?", messages[1].get("content").asString())
    }

    @Test
    fun requestBodyEscapesSpecialCharacters() {
        // a question with quotes/newlines must round-trip, not corrupt the JSON
        val body = Groq.requestBody("sys", "she said \"hi\"\nthen left", Groq.CHAT_MODEL)
        val parsed = Json.parse(body) // would throw if escaping were wrong
        assertEquals("she said \"hi\"\nthen left", parsed.get("messages").asArr()[1].get("content").asString())
    }

    @Test
    fun parseReplyExtractsAndTrimsContent() {
        val resp = """{"choices":[{"message":{"role":"assistant","content":"  Yes, usually around week 6.  "}}]}"""
        assertEquals("Yes, usually around week 6.", Groq.parseReply(resp))
    }

    @Test
    fun parseReplyRejectsEmptyChoices() {
        try {
            Groq.parseReply("""{"choices":[]}""")
            fail("expected GroqException for empty choices")
        } catch (e: Groq.GroqException) {
            assertTrue(e.message!!.contains("Empty"))
        }
    }

    @Test
    fun extractErrorPrefersApiMessage() {
        val text = """{"error":{"message":"Invalid API Key","type":"invalid_request_error"}}"""
        assertEquals("Invalid API Key", Groq.extractError(text, 401))
    }

    @Test
    fun extractErrorFallsBackToStatusOnGarbage() {
        assertTrue(Groq.extractError("<html>502 Bad Gateway</html>", 502).contains("502"))
    }
}
