package com.revjet.sdk.internal

import org.junit.Assert.assertEquals
import org.junit.Test

class JsLiteralTest {
    @Test
    fun `wraps a plain string in single quotes`() {
        assertEquals("'gold'", JsLiteral.string("gold"))
    }

    @Test
    fun `escapes a quote so it cannot end the literal`() {
        assertEquals("""'it\'s'""", JsLiteral.string("it's"))
    }

    @Test
    fun `escapes a backslash before anything else`() {
        assertEquals("""'a\\b'""", JsLiteral.string("""a\b"""))
        assertEquals("""'a\\\'b'""", JsLiteral.string("""a\'b"""))
    }

    @Test
    fun `escapes the characters that would end the statement`() {
        // Built from code points: Kotlin expands \uXXXX even inside a raw string, so a written
        // out expectation would hold the character instead of the escape the SDK emits
        val backslash = 0x5C.toChar()
        val lineSeparator = 0x2028.toChar()
        val paragraphSeparator = 0x2029.toChar()

        assertEquals("""'a\nb'""", JsLiteral.string("a\nb"))
        assertEquals("""'a\rb'""", JsLiteral.string("a\rb"))
        assertEquals("'a${backslash}u2028b'", JsLiteral.string("a${lineSeparator}b"))
        assertEquals("'a${backslash}u2029b'", JsLiteral.string("a${paragraphSeparator}b"))
    }

    @Test
    fun `escapes a less-than so it cannot close the script element`() {
        assertEquals("""'\x3C/script>'""", JsLiteral.string("</script>"))
    }
}
