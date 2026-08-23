// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.runtime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The canonicalisation steps of the IR.
 *
 * None of them fails and none of them truncates: a canonicalisation program
 * either changes the value or leaves it alone.
 */
class CanonBufferTest {
    private fun buffer(s: String) = CanonBuffer(Utf.codePoints(s))

    private val ideographicSpace = "　"
    private val byteOrderMark = "﻿"

    @Test
    fun `trim removes only the frozen whitespace table, at both ends`() {
        val b = buffer("  \t A B $ideographicSpace")
        b.trimWhitespace()
        assertEquals("A B", b.snapshot())
    }

    @Test
    fun `trim leaves a value with no surrounding whitespace alone`() {
        val b = buffer("AB")
        b.trimWhitespace()
        assertEquals("AB", b.snapshot())
    }

    @Test
    fun `remove_whitespace removes every entry of the frozen table`() {
        val b = buffer("A  B${byteOrderMark}C")
        b.removeWhitespace()
        assertEquals("ABC", b.snapshot())
    }

    @Test
    fun `the frozen table is not the platform's idea of whitespace`() {
        // U+180E was whitespace in an older Unicode version and is not in the
        // frozen table; a runtime that delegated to its own tables would drop it.
        assertFalse(Ascii.isWhitespace(0x180E))
        assertTrue(Ascii.isWhitespace(0x1680))
        assertTrue(Ascii.isWhitespace(0xFEFF))
        assertFalse(Ascii.isWhitespace(0x200B), "the zero width space is not in the table")
    }

    @Test
    fun `uppercase maps only a to z and consults no locale`() {
        val b = buffer("iıéabz")
        b.uppercaseAscii()
        // A Turkish locale would map i to a dotted capital I. The test JVM runs
        // with that locale on purpose, and ASCII case mapping does not care.
        assertEquals("IıéABZ", b.snapshot())
    }

    @Test
    fun `remove_chars drops every member of the declared set`() {
        val b = buffer("12-34.56")
        b.removeChars(set("-."))
        assertEquals("123456", b.snapshot())
    }

    @Test
    fun `replace_prefix replaces only an exact leading constant`() {
        val b = buffer("GR123")
        b.replacePrefix(cp("GR"), cp("EL"))
        assertEquals("EL123", b.snapshot())

        val other = buffer("XGR123")
        other.replacePrefix(cp("GR"), cp("EL"))
        assertEquals("XGR123", other.snapshot())
    }

    @Test
    fun `replace_prefix handles a replacement of a different length`() {
        val shorter = buffer("ABCD")
        shorter.replacePrefix(cp("ABC"), cp("Z"))
        assertEquals("ZD", shorter.snapshot())

        val longer = buffer("ABCD")
        longer.replacePrefix(cp("A"), cp("WXYZ"))
        assertEquals("WXYZBCD", longer.snapshot())
    }

    @Test
    fun `prepend and append`() {
        val b = buffer("123")
        b.prepend(cp("FR"))
        b.append(cp("!"))
        assertEquals("FR123!", b.snapshot())
    }

    @Test
    fun `insert places a constant at a code point position`() {
        val b = buffer("1234")
        b.insert(2, cp("-"))
        assertEquals("12-34", b.snapshot())
    }

    @Test
    fun `insert at the end is an append, and past the end changes nothing`() {
        val atEnd = buffer("12")
        atEnd.insert(2, cp("X"))
        assertEquals("12X", atEnd.snapshot())

        val past = buffer("12")
        past.insert(3, cp("X"))
        assertEquals("12", past.snapshot())
    }

    @Test
    fun `left_pad fills to the declared length and never truncates`() {
        val short = buffer("12")
        short.leftPad(5, '0'.code)
        assertEquals("00012", short.snapshot())

        val long = buffer("1234567")
        long.leftPad(5, '0'.code)
        assertEquals("1234567", long.snapshot())
    }

    @Test
    fun `the buffer grows past its initial capacity`() {
        val b = buffer("1")
        repeat(50) { b.append(cp("0123456789")) }
        assertEquals(501, b.view().length)
    }

    @Test
    fun `starts_with reads the current value`() {
        val b = buffer("fr123")
        assertFalse(b.startsWith(cp("FR")))
        b.uppercaseAscii()
        assertTrue(b.startsWith(cp("FR")))
        assertFalse(b.startsWith(cp("FR1234567")))
    }

    @Test
    fun `the view a step hands out reflects the value at that moment`() {
        val b = buffer(" ab ")
        assertEquals(" ab ", b.view().toString())
        b.trimWhitespace()
        assertEquals("ab", b.view().toString())
        b.uppercaseAscii()
        assertEquals("AB", b.view().toString())
    }

    @Test
    fun `an empty value survives every step`() {
        val b = buffer("")
        b.trimWhitespace()
        b.removeWhitespace()
        b.uppercaseAscii()
        b.removeChars(set("-"))
        b.replacePrefix(cp("A"), cp("B"))
        b.insert(1, cp("X"))
        assertEquals("", b.snapshot())
    }

    @Test
    fun `a value outside the Basic Multilingual Plane is counted in code points`() {
        val b = buffer("𝐀AB")
        assertEquals(3, b.view().length)
        b.insert(1, cp("-"))
        assertEquals("𝐀-AB", b.snapshot())
    }
}
