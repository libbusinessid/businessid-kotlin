// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.runtime

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
