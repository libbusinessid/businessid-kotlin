// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.runtime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The predicates of the IR.
 *
 * Every one of them is false on an absent operand except `is_absent`, which is
 * the only predicate that observes absence as true.
 */
class PredTest {
    @Test
    fun `is_empty is true only for a present empty view`() {
        assertTrue(Pred.isEmpty(view("")))
        assertFalse(Pred.isEmpty(view("a")))
        assertFalse(Pred.isEmpty(null))
    }

    @Test
    fun `is_absent is the only predicate that observes absence`() {
        assertTrue(Pred.isAbsent(null))
        assertFalse(Pred.isAbsent(view("")))
    }

    @Test
    fun `every other predicate is false on an absent operand`() {
        assertFalse(Pred.equal(null, view("a")))
        assertFalse(Pred.equal(view("a"), null))
        assertFalse(Pred.lengthEq(null, 0))
        assertFalse(Pred.lengthIn(null, intArrayOf(0)))
        assertFalse(Pred.lengthBetween(null, 0, 9))
        assertFalse(Pred.asciiDigits(null))
        assertFalse(Pred.asciiUpperLetters(null))
        assertFalse(Pred.asciiAlphanumeric(null))
        assertFalse(Pred.asciiCharset(null, set("AB")))
        assertFalse(Pred.startsWith(null, cp("A")))
        assertFalse(Pred.endsWith(null, cp("A")))
        assertFalse(Pred.prefixIn(null, arrayOf(cp("A"))))
        assertFalse(Pred.charAtIn(null, 0, set("A")))
        assertFalse(Pred.contains(null, cp("A")))
        assertFalse(Pred.integerIs(null, 0))
    }

    @Test
    fun `equals compares code point sequences`() {
        assertTrue(Pred.equal(view("ABC"), view("ABC")))
        assertFalse(Pred.equal(view("ABC"), view("ABD")))
        assertFalse(Pred.equal(view("ABC"), view("AB")))
    }

    @Test
    fun `length predicates`() {
        assertTrue(Pred.lengthEq(view("1234"), 4))
        assertFalse(Pred.lengthEq(view("1234"), 5))
        assertTrue(Pred.lengthIn(view("12"), intArrayOf(2, 4, 8)))
        assertFalse(Pred.lengthIn(view("123"), intArrayOf(2, 4, 8)))
        assertTrue(Pred.lengthBetween(view("123"), 3, 5))
        assertTrue(Pred.lengthBetween(view("12345"), 3, 5))
        assertFalse(Pred.lengthBetween(view("12"), 3, 5))
        assertFalse(Pred.lengthBetween(view("123456"), 3, 5))
    }

    @Test
    fun `the ASCII classes are empty-hostile and reject anything outside them`() {
        assertFalse(Pred.asciiDigits(view("")))
        assertTrue(Pred.asciiDigits(view("0123456789")))
        assertFalse(Pred.asciiDigits(view("12a")))
        assertFalse(Pred.asciiDigits(view("１２")), "full width digits are not ASCII digits")

        assertFalse(Pred.asciiUpperLetters(view("")))
        assertTrue(Pred.asciiUpperLetters(view("ABZ")))
        assertFalse(Pred.asciiUpperLetters(view("Abz")))

        assertFalse(Pred.asciiAlphanumeric(view("")))
        assertTrue(Pred.asciiAlphanumeric(view("A0Z9")))
        assertFalse(Pred.asciiAlphanumeric(view("A-0")))
        assertFalse(Pred.asciiAlphanumeric(view("a0")))
    }

    @Test
    fun `ascii_charset accepts only members of the declared set`() {
        assertTrue(Pred.asciiCharset(view("ABBA"), set("AB")))
        assertFalse(Pred.asciiCharset(view("ABC"), set("AB")))
        assertFalse(Pred.asciiCharset(view(""), set("AB")))
    }

    @Test
    fun `starts_with, ends_with, contains and prefix_in`() {
        assertTrue(Pred.startsWith(view("FR123"), cp("FR")))
        assertFalse(Pred.startsWith(view("BE123"), cp("FR")))
        assertFalse(Pred.startsWith(view("F"), cp("FR")))
        assertTrue(Pred.endsWith(view("123FR"), cp("FR")))
        assertFalse(Pred.endsWith(view("FR123"), cp("FR")))
        assertTrue(Pred.contains(view("aFRb"), cp("FR")))
        assertFalse(Pred.contains(view("abc"), cp("FR")))
        assertTrue(Pred.prefixIn(view("BE0"), arrayOf(cp("FR"), cp("BE"))))
        assertFalse(Pred.prefixIn(view("DE0"), arrayOf(cp("FR"), cp("BE"))))
    }

    @Test
    fun `prefix_in over a packed group finds every member and no other`() {
        // A membership list a register publishes runs to thousands of entries,
        // packed end to end and read by binary search. Every entry must be
        // found, at both ends and in the middle, and nothing between them.
        val entries = listOf("AA", "AB", "BC", "MM", "XY", "ZZ")
        val packed = entries.joinToString("")
        for (entry in entries) {
            assertTrue(Pred.prefixInPacked(view(entry), packed, 2, 2), entry)
            assertTrue(Pred.prefixInPacked(view(entry + "rest"), packed, 2, 2), "$entry with a tail")
        }
        for (absent in listOf("AC", "A0", "BB", "BD", "LL", "NN", "XX", "XZ", "ZY", "00", "zz")) {
            assertFalse(Pred.prefixInPacked(view(absent), packed, 2, 2), absent)
        }
    }

    @Test
    fun `prefix_in over a packed group is false on absent and on a value too short`() {
        val packed = "AABBCC"
        assertFalse(Pred.prefixInPacked(null, packed, 2, 2))
        assertFalse(Pred.prefixInPacked(view(""), packed, 2, 2))
        assertFalse(Pred.prefixInPacked(view("A"), packed, 2, 2))
        assertTrue(Pred.prefixInPacked(view("AA"), packed, 2, 2))
    }

    @Test
    fun `a packed group of one entry is searched correctly`() {
        assertTrue(Pred.prefixInPacked(view("AB"), "AB", 2, 2))
        assertFalse(Pred.prefixInPacked(view("AA"), "AB", 2, 2))
        assertFalse(Pred.prefixInPacked(view("AC"), "AB", 2, 2))
    }

    @Test
    fun `a packed group whose entries reach outside the Basic Multilingual Plane`() {
        // One code point, two UTF-16 units. The search compares code points, so
        // the stride and the code point count part company here.
        val entries = listOf("\uD800\uDC00", "\uD800\uDC01", "\uD83D\uDE00")
        val packed = entries.joinToString("")
        for (entry in entries) {
            assertTrue(Pred.prefixInPacked(view(entry), packed, 1, 2), entry)
        }
        assertFalse(Pred.prefixInPacked(view("\uD800\uDC02"), packed, 1, 2))
        assertFalse(Pred.prefixInPacked(view("A"), packed, 1, 2))
    }

    @Test
    fun `a packed group mixing a BMP and a supplementary code point per entry`() {
        // Two code points, three units. The search reads the group in code point
        // order, which the generator guarantees: U+FFFD comes before U+10002.
        // Sorted by UTF-16 unit the two would swap, and the search would walk
        // past a member and answer that it is not one — which is what packing
        // them the other way round below demonstrates.
        val entries = listOf("\uFFFD\uD800\uDC03", "\uD800\uDC02Z")
        val packed = entries.joinToString("")
        assertTrue(Pred.prefixInPacked(view("\uFFFD\uD800\uDC03"), packed, 2, 3))
        assertTrue(Pred.prefixInPacked(view("\uD800\uDC02Z"), packed, 2, 3))
        assertFalse(Pred.prefixInPacked(view("\uFFFD\uD800\uDC04"), packed, 2, 3))

        // The same two entries in UTF-16 order, which is what Kotlin's own
        // `sorted()` produces. A member becomes unfindable.
        val misordered = entries.sorted().joinToString("")
        assertFalse(
            Pred.prefixInPacked(view("\uFFFD\uD800\uDC03"), misordered, 2, 3),
            "the ordering the generator has to get right",
        )
    }

    @Test
    fun `the packed search answers rather than throws when the shape disagrees`() {
        // The generator groups a list so that every entry holds exactly the
        // declared number of code points and units, and `KitchenSinkTest`
        // asserts that over what it emits. These are the guards that make the
        // search total anyway: told a stride the content does not have, it
        // answers, and it answers false rather than matching by accident.
        val packed = "ABCDEF"

        // Entries shorter than the declared code point length: each block runs
        // out before the probe does.
        assertFalse(Pred.prefixInPacked(view("ABC"), packed, 3, 2))
        assertFalse(Pred.prefixInPacked(view("AB"), packed, 3, 2))

        // Entries longer than the probe consumes: each block has code points
        // left over after the comparison.
        assertFalse(Pred.prefixInPacked(view("A"), packed, 1, 3))
        assertFalse(Pred.prefixInPacked(view("D"), packed, 1, 3))

        // And a lone high surrogate at the end of a block, which no entry the
        // generator packs can hold.
        assertFalse(Pred.prefixInPacked(view("A"), "A\uD800", 1, 2))
    }

    @Test
    fun `the packed search agrees with the walk it replaced, over every pair`() {
        // The two forms answer the same question; the packed one only answers it
        // faster. Stated over every two character value there is.
        val alphabet = "ABMXZ0"
        val entries = alphabet.flatMap { a -> alphabet.map { b -> "" + a + b } }
            .filterIndexed { i, _ -> i % 3 == 0 }
            .sorted()
        val packed = entries.joinToString("")
        val arrays = entries.map { cp(it) }.toTypedArray()
        for (a in alphabet) {
            for (b in alphabet) {
                val probe = view("" + a + b + "tail")
                assertEquals(
                    Pred.prefixIn(probe, arrays),
                    Pred.prefixInPacked(probe, packed, 2, 2),
                    "for $a$b",
                )
            }
        }
    }

    @Test
    fun `char_at_in reads a code point position and is false past the end`() {
        assertTrue(Pred.charAtIn(view("A1B"), 1, set("012")))
        assertFalse(Pred.charAtIn(view("A1B"), 0, set("012")))
        assertFalse(Pred.charAtIn(view("A1B"), 3, set("012")))
        assertFalse(Pred.charAtIn(view(""), 0, set("012")))
    }

    @Test
    fun `integer_is is false on an indeterminate operand, which lets a branch fall through`() {
        assertTrue(Pred.integerIs(7L, 7L))
        assertFalse(Pred.integerIs(7L, 8L))
        assertFalse(Pred.integerIs(null, 7L))
    }
}
