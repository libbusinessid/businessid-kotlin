// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

package org.entid.runtime

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
    fun `it agrees with the encoder on ill formed strings too, which is the choice ir_md asks be stated`() {
        // `ir.md` section 6 step 1 lets an engine either count what its own
        // encoder produces or refuse the input before measuring it. This engine
        // counts, so the count is not a convention but an invariant: equal to
        // `toByteArray(UTF_8).size` for every string a Kotlin `String` can hold.
        val samples = listOf(
            highSurrogate,
            lowSurrogate,
            highSurrogate + highSurrogate,
            lowSurrogate + lowSurrogate,
            lowSurrogate + highSurrogate,
            highSurrogate + "A",
            highSurrogate + "😀" + lowSurrogate,
            "ab" + highSurrogate + "cd",
            highSurrogate + "é",
            highSurrogate.repeat(3),
        )
        for (s in samples) {
            assertEquals(
                s.toByteArray(Charsets.UTF_8).size,
                Utf.utf8Length(s),
                "for ${s.map { "U+%04X".format(it.code) }}",
            )
        }
    }

    @Test
    fun `it agrees with the encoder on any string at all`() {
        // The same statement over generated input rather than chosen examples,
        // with surrogates drawn often enough that ill formed strings dominate.
        val random = java.util.Random(20260831)
        val alphabet = charArrayOf(
            'a', 'Z', '0', ' ', 'é', '€', '\uD83D', '\uDE00', '\uD800', '\uDFFF', '\u0000', '\u07FF',
        )
        repeat(20_000) {
            val out = StringBuilder()
            repeat(random.nextInt(12)) { out.append(alphabet[random.nextInt(alphabet.size)]) }
            val s = out.toString()
            assertEquals(
                s.toByteArray(Charsets.UTF_8).size,
                Utf.utf8Length(s),
                "for ${s.map { "U+%04X".format(it.code) }}",
            )
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
