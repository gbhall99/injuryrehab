package com.recoverwell.core.json

/**
 * Minimal dependency-free JSON model + parser + writer.
 * Exists because this app must work fully offline with no third-party
 * runtime dependencies; it is small, strict and round-trip tested.
 */
sealed class JsonValue {
    object Null : JsonValue()
    data class Bool(val value: Boolean) : JsonValue()
    data class Num(val value: Double) : JsonValue()
    data class Str(val value: String) : JsonValue()
    data class Arr(val items: List<JsonValue>) : JsonValue()
    data class Obj(val fields: Map<String, JsonValue>) : JsonValue()

    fun asString(): String = (this as Str).value
    fun asInt(): Int = (this as Num).value.toInt()
    fun asLong(): Long = (this as Num).value.toLong()
    fun asDouble(): Double = (this as Num).value
    fun asBool(): Boolean = (this as Bool).value
    fun asArr(): List<JsonValue> = (this as Arr).items
    fun asObj(): Map<String, JsonValue> = (this as Obj).fields

    fun opt(key: String): JsonValue? {
        val v = (this as? Obj)?.fields?.get(key)
        return if (v == null || v is Null) null else v
    }

    fun get(key: String): JsonValue =
        opt(key) ?: throw JsonException("Missing key: $key")
}

class JsonException(message: String) : Exception(message)

object Json {

    fun obj(vararg pairs: Pair<String, JsonValue>): JsonValue.Obj = JsonValue.Obj(pairs.toMap())
    fun arr(items: List<JsonValue>): JsonValue.Arr = JsonValue.Arr(items)
    fun of(v: String?): JsonValue = if (v == null) JsonValue.Null else JsonValue.Str(v)
    fun of(v: Int?): JsonValue = if (v == null) JsonValue.Null else JsonValue.Num(v.toDouble())
    fun ofLong(v: Long?): JsonValue = if (v == null) JsonValue.Null else JsonValue.Num(v.toDouble())
    fun of(v: Double?): JsonValue = if (v == null) JsonValue.Null else JsonValue.Num(v)
    fun of(v: Boolean?): JsonValue = if (v == null) JsonValue.Null else JsonValue.Bool(v)
    fun strings(v: List<String>): JsonValue.Arr = JsonValue.Arr(v.map { JsonValue.Str(it) })

    fun write(value: JsonValue): String {
        val sb = StringBuilder()
        writeTo(value, sb)
        return sb.toString()
    }

    private fun writeTo(value: JsonValue, sb: StringBuilder) {
        when (value) {
            is JsonValue.Null -> sb.append("null")
            is JsonValue.Bool -> sb.append(if (value.value) "true" else "false")
            is JsonValue.Num -> {
                val d = value.value
                if (d == Math.floor(d) && !d.isInfinite() && Math.abs(d) < 1e15) {
                    sb.append(d.toLong())
                } else {
                    sb.append(d)
                }
            }
            is JsonValue.Str -> writeString(value.value, sb)
            is JsonValue.Arr -> {
                sb.append('[')
                value.items.forEachIndexed { i, item ->
                    if (i > 0) sb.append(',')
                    writeTo(item, sb)
                }
                sb.append(']')
            }
            is JsonValue.Obj -> {
                sb.append('{')
                var first = true
                for ((k, v) in value.fields) {
                    if (!first) sb.append(',')
                    first = false
                    writeString(k, sb)
                    sb.append(':')
                    writeTo(v, sb)
                }
                sb.append('}')
            }
        }
    }

    private fun writeString(s: String, sb: StringBuilder) {
        sb.append('"')
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                else -> if (c < ' ') sb.append("\\u%04x".format(c.code)) else sb.append(c)
            }
        }
        sb.append('"')
    }

    fun parse(text: String): JsonValue {
        val p = Parser(text)
        val v = p.parseValue()
        p.skipWhitespace()
        if (!p.atEnd()) throw JsonException("Trailing content at ${p.pos}")
        return v
    }

    private class Parser(val text: String) {
        var pos = 0

        fun atEnd() = pos >= text.length

        fun skipWhitespace() {
            while (pos < text.length && text[pos].isWhitespace()) pos++
        }

        fun parseValue(): JsonValue {
            skipWhitespace()
            if (atEnd()) throw JsonException("Unexpected end of input")
            return when (text[pos]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> JsonValue.Str(parseString())
                't' -> { expect("true"); JsonValue.Bool(true) }
                'f' -> { expect("false"); JsonValue.Bool(false) }
                'n' -> { expect("null"); JsonValue.Null }
                else -> parseNumber()
            }
        }

        fun expect(word: String) {
            if (!text.startsWith(word, pos)) throw JsonException("Expected '$word' at $pos")
            pos += word.length
        }

        fun parseObject(): JsonValue.Obj {
            pos++ // {
            val fields = LinkedHashMap<String, JsonValue>()
            skipWhitespace()
            if (!atEnd() && text[pos] == '}') { pos++; return JsonValue.Obj(fields) }
            while (true) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                if (atEnd() || text[pos] != ':') throw JsonException("Expected ':' at $pos")
                pos++
                fields[key] = parseValue()
                skipWhitespace()
                if (atEnd()) throw JsonException("Unterminated object")
                when (text[pos]) {
                    ',' -> pos++
                    '}' -> { pos++; return JsonValue.Obj(fields) }
                    else -> throw JsonException("Expected ',' or '}' at $pos")
                }
            }
        }

        fun parseArray(): JsonValue.Arr {
            pos++ // [
            val items = ArrayList<JsonValue>()
            skipWhitespace()
            if (!atEnd() && text[pos] == ']') { pos++; return JsonValue.Arr(items) }
            while (true) {
                items.add(parseValue())
                skipWhitespace()
                if (atEnd()) throw JsonException("Unterminated array")
                when (text[pos]) {
                    ',' -> pos++
                    ']' -> { pos++; return JsonValue.Arr(items) }
                    else -> throw JsonException("Expected ',' or ']' at $pos")
                }
            }
        }

        fun parseString(): String {
            if (atEnd() || text[pos] != '"') throw JsonException("Expected string at $pos")
            pos++
            val sb = StringBuilder()
            while (true) {
                if (atEnd()) throw JsonException("Unterminated string")
                when (val c = text[pos]) {
                    '"' -> { pos++; return sb.toString() }
                    '\\' -> {
                        pos++
                        if (atEnd()) throw JsonException("Unterminated escape")
                        when (val e = text[pos]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'u' -> {
                                if (pos + 4 >= text.length) throw JsonException("Bad unicode escape")
                                sb.append(text.substring(pos + 1, pos + 5).toInt(16).toChar())
                                pos += 4
                            }
                            else -> throw JsonException("Bad escape '\\$e' at $pos")
                        }
                        pos++
                    }
                    else -> { sb.append(c); pos++ }
                }
            }
        }

        fun parseNumber(): JsonValue.Num {
            val start = pos
            if (!atEnd() && text[pos] == '-') pos++
            while (!atEnd() && (text[pos].isDigit() || text[pos] in ".eE+-")) pos++
            val s = text.substring(start, pos)
            val d = s.toDoubleOrNull() ?: throw JsonException("Bad number '$s' at $start")
            return JsonValue.Num(d)
        }
    }
}
