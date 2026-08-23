// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.runtime

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The boundary between a Kotlin `String` and the code point model the rules use.
 *
 * A `String` is UTF-16 and admits an unpaired surrogate, which no UTF-8 byte
 * sequence encodes. That is the only ill formed text this API can receive.
 */
class UtfTest {
    private val highSurrogate = "\uD83D"
    private val lowSurrogate = "\uDE00"

    @Test
    fun `the UTF-8 length is measured without materialising the encoded form`() {
        assertEquals(0, Utf.utf8Length(""))
        assertEquals(3, Utf.utf8Length("abc"))
        assertEquals(2, Utf.utf8Length("é"))
        assertEquals(3, Utf.utf8Length("€"))
        assertEquals(4, Utf.utf8Length("😀"))
    }

    @Test
    fun `the measured length agrees with the encoder on every well formed string`() {
        val samples = listOf("", "a", "é", "€", "😀", "0123456789", "a€b😀c", "𝐀AB")
        for (s in samples) {
            assertEquals(s.toByteArray(Charsets.UTF_8).size, Utf.utf8Length(s), "for $s")
        }
    }

    @Test
    fun `an unpaired surrogate is ill formed, in both orders`() {
        assertTrue(Utf.isWellFormed(""))
        assertTrue(Utf.isWellFormed("abc"))
        assertTrue(Utf.isWellFormed("😀"))
        assertFalse(Utf.isWellFormed(highSurrogate), "a lone high surrogate")
        assertFalse(Utf.isWellFormed(lowSurrogate), "a lone low surrogate")
        assertFalse(Utf.isWellFormed("a${highSurrogate}b"))
        assertFalse(Utf.isWellFormed("$lowSurrogate$highSurrogate"), "a reversed pair")
        assertTrue(Utf.isWellFormed("$highSurrogate$lowSurrogate"))
    }

    @Test
    fun `code points combine a surrogate pair into one position`() {
        assertArrayEquals(intArrayOf(0x61, 0x1F600, 0x62), Utf.codePoints("a😀b"))
        assertEquals(3, Utf.codePoints("a😀b").size)
        assertEquals(4, "a😀b".length, "the same string is four UTF-16 units")
    }

    @Test
    fun `a round trip through code points is the identity on well formed text`() {
        for (s in listOf("", "a", "é€😀", "0123456789", "𝐀AB")) {
            val cp = Utf.codePoints(s)
            assertEquals(s, Utf.toStringOf(cp, 0, cp.size), "for $s")
        }
    }

    @Test
    fun `toStringOf spells a sub range`() {
        val cp = Utf.codePoints("a😀b")
        assertEquals("😀", Utf.toStringOf(cp, 1, 2))
        assertEquals("", Utf.toStringOf(cp, 1, 1))
    }
}
