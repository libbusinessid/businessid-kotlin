// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid

import io.libbusinessid.internal.Tokens
import io.libbusinessid.runtime.Ascii
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The normalisation of a kind and of a country token, character by character.
 *
 * `trim ASCII` removes `U+0009..U+000D` and `U+0020` — those six and nothing
 * else. Both bounds are asserted here because mutation testing showed a slipped
 * boundary surviving every other test: neither the carriage return at the top of
 * the range nor the character just below the tab was exercised anywhere.
 */
class TokensTest {
    @Test
    fun `the dispatch trim set is exactly the six code points the specification names`() {
        val trimmed = (0..0x100).filter { Ascii.isDispatchTrim(it) }
        assertEquals(listOf(0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x20), trimmed)
    }

    @Test
    fun `the character below the tab is not trimmed`() {
        assertFalse(Ascii.isDispatchTrim(0x08))
        val token = "x"
        assertEquals(token, Tokens.asciiTrim(token))
    }

    @Test
    fun `the carriage return at the top of the range is trimmed`() {
        assertTrue(Ascii.isDispatchTrim(0x0D))
        assertEquals("x", Tokens.asciiTrim("\r\rx\r"))
    }

    @Test
    fun `the character just above the range is not trimmed`() {
        assertFalse(Ascii.isDispatchTrim(0x0E))
        val token = "x"
        assertEquals(token, Tokens.asciiTrim(token))
    }

    @Test
    fun `no whitespace beyond the six is trimmed, however much it looks like one`() {
        // The frozen whitespace table a canonicalisation program removes is a
        // different set: a dispatch trim removes six code points and no more.
        for (c in listOf(0x00A0, 0x2000, 0x3000, 0xFEFF, 0x0085)) {
            assertFalse(Ascii.isDispatchTrim(c), "U+%04X".format(c))
            val token = String(Character.toChars(c)) + "x"
            assertEquals(token, Tokens.asciiTrim(token))
        }
    }

    @Test
    fun `a trim removes from both ends and nothing in between`() {
        assertEquals("a b", Tokens.asciiTrim(" \t\n\r a b \r\n\t "))
        assertEquals("", Tokens.asciiTrim("   "))
        assertEquals("", Tokens.asciiTrim(""))
        assertEquals("ab", Tokens.asciiTrim("ab"))
    }

    @Test
    fun `case mapping touches only the twenty-six ASCII letters`() {
        assertEquals("abz", Tokens.asciiLower("ABZ"))
        assertEquals("ABZ", Tokens.asciiUpper("abz"))
        // The characters on both sides of each range.
        assertEquals("@[`{", Tokens.asciiLower("@[`{"))
        assertEquals("@[`{", Tokens.asciiUpper("@[`{"))
        // And a locale that would map them differently. The test JVM runs with
        // a Turkish locale on purpose.
        assertEquals("i", Tokens.asciiLower("I"))
        assertEquals("I", Tokens.asciiUpper("i"))
        assertEquals("ı", Tokens.asciiUpper("ı"))
        assertEquals("É", Tokens.asciiUpper("É"))
        assertEquals("É", Tokens.asciiLower("É"))
    }

    @Test
    fun `a token already normalised is returned unchanged`() {
        val already = "siret"
        assertEquals(already, Tokens.asciiLower(already))
        assertEquals("SIRET", Tokens.asciiUpper(already))
        assertEquals(already, Tokens.asciiTrim(already))
    }

    @Test
    fun `a country token is two ASCII upper case letters and nothing else`() {
        assertTrue(Tokens.isCountryToken("FR"))
        assertTrue(Tokens.isCountryToken("AA"))
        assertTrue(Tokens.isCountryToken("ZZ"))
        assertFalse(Tokens.isCountryToken("F"))
        assertFalse(Tokens.isCountryToken("FRA"))
        assertFalse(Tokens.isCountryToken("fr"))
        assertFalse(Tokens.isCountryToken("F1"))
        assertFalse(Tokens.isCountryToken("@A"))
        assertFalse(Tokens.isCountryToken("A["))
        assertFalse(Tokens.isCountryToken(""))
    }

    @Test
    fun `a kind token carrying a control character below the trim range stays unresolved`() {
        // Reaching the engine rather than the helper: if that character were
        // trimmed, this would resolve to `siret`.
        val report = BusinessIdEngine.default()
            .validate(IdentifierInput(IdentifierKind("siret"), "01234567400001"))
        assertEquals(ReasonCode.UNSUPPORTED_KIND, report.format.reasonCode)
        assertEquals("siret", report.kind.value)
    }

    @Test
    fun `a kind token wrapped in carriage returns resolves`() {
        val report = BusinessIdEngine.default()
            .validate(IdentifierInput(IdentifierKind("\rsiret\r"), "01234567400001"))
        assertEquals("siret", report.kind.value)
        assertEquals(StepStatus.VALID, report.format.status)
    }
}
