package com.recoverwell.core

import com.recoverwell.core.json.Json
import com.recoverwell.core.json.JsonException
import com.recoverwell.core.json.JsonValue
import org.junit.Assert.*
import org.junit.Test

class JsonTest {

    @Test
    fun roundTripNested() {
        val v = Json.obj(
            "s" to Json.of("he said \"hi\"\nline2\ttab\\slash"),
            "i" to Json.of(42),
            "d" to Json.of(3.25),
            "b" to Json.of(true),
            "n" to JsonValue.Null,
            "arr" to Json.arr(listOf(Json.of(1), Json.of("two"), JsonValue.Null)),
            "obj" to Json.obj("k" to Json.of("v"))
        )
        val text = Json.write(v)
        val parsed = Json.parse(text)
        assertEquals("he said \"hi\"\nline2\ttab\\slash", parsed.get("s").asString())
        assertEquals(42, parsed.get("i").asInt())
        assertEquals(3.25, parsed.get("d").asDouble(), 0.0)
        assertTrue(parsed.get("b").asBool())
        assertNull(parsed.opt("n"))
        assertEquals(3, parsed.get("arr").asArr().size)
        assertEquals("v", parsed.get("obj").get("k").asString())
        // stable round trip
        assertEquals(text, Json.write(Json.parse(text)))
    }

    @Test
    fun unicodeEscapes() {
        val parsed = Json.parse("\"caf\\u00e9 padel\"")
        assertEquals("café padel", parsed.asString())
    }

    @Test
    fun integersWrittenWithoutDecimalPoint() {
        assertEquals("{\"x\":7}", Json.write(Json.obj("x" to Json.of(7))))
    }

    @Test(expected = JsonException::class)
    fun rejectsTrailingGarbage() {
        Json.parse("{} extra")
    }

    @Test(expected = JsonException::class)
    fun rejectsUnterminatedString() {
        Json.parse("\"abc")
    }

    @Test(expected = JsonException::class)
    fun rejectsBadNumber() {
        Json.parse("[1, 2, 3-]")
    }
}
