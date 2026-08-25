// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

package org.entid.runtime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * The integer constructors of the IR.
 *
 * `null` is the indeterminate integer. It propagates through every operation and
 * never proves an identifier wrong: an indeterminate value can only make the
 * enclosing checksum `unsupported`.
 */
class ArithTest {
    @Test
    fun `digits_to_integer reads a non negative decimal`() {
        assertEquals(0L, Arith.digitsToInteger(view("0")))
        assertEquals(123456L, Arith.digitsToInteger(view("123456")))
        assertEquals(999999999999999999L, Arith.digitsToInteger(view("999999999999999999")))
    }

    @Test
    fun `digits_to_integer is indeterminate on absent, empty and non digit views`() {
        assertNull(Arith.digitsToInteger(null))
        assertNull(Arith.digitsToInteger(view("")))
        assertNull(Arith.digitsToInteger(view("12A")))
        assertNull(Arith.digitsToInteger(view("-1")))
    }

    @Test
    fun `mod_digits reduces digit by digit, without any wide conversion`() {
        assertEquals(0L, Arith.modDigits(view("0"), 97))
        assertEquals(1L, Arith.modDigits(view("98"), 97))
        // Far beyond what an int64 could hold as a whole number.
        val long = "9".repeat(400)
        assertEquals(
            java.math.BigInteger(long).mod(java.math.BigInteger.valueOf(97)).toLong(),
            Arith.modDigits(view(long), 97),
        )
    }

    @Test
    fun `mod_digits is indeterminate on absent, empty and non digit views`() {
        assertNull(Arith.modDigits(null, 97))
        assertNull(Arith.modDigits(view(""), 97))
        assertNull(Arith.modDigits(view("1A"), 97))
    }

    @Test
    fun `weighted_sum LEFT pairs position i with weight i`() {
        // 1*1 + 2*2 + 3*3 = 14; the fourth position pairs with nothing.
        assertEquals(
            14L,
            Arith.weightedSumDigits(view("1234"), longArrayOf(1, 2, 3), Alignment.LEFT),
        )
    }

    @Test
    fun `weighted_sum RIGHT pairs the last position with the last weight`() {
        // "1234" against [1,2,3]: 2*1 + 3*2 + 4*3 = 20.
        assertEquals(
            20L,
            Arith.weightedSumDigits(view("1234"), longArrayOf(1, 2, 3), Alignment.RIGHT),
        )
    }

    @Test
    fun `weighted_sum CYCLE repeats the weights over every position`() {
        // "1234" against [1,2]: 1*1 + 2*2 + 3*1 + 4*2 = 16.
        assertEquals(
            16L,
            Arith.weightedSumDigits(view("1234"), longArrayOf(1, 2), Alignment.CYCLE),
        )
    }

    @Test
    fun `weighted_sum is indeterminate on a code point outside the mapping, even where no weight pairs`() {
        // The letter sits at a position LEFT alignment never multiplies, and the
        // sum is still indeterminate: a value the algorithm cannot speak about is
        // not one it may judge.
        assertNull(Arith.weightedSumDigits(view("12A"), longArrayOf(1, 2), Alignment.LEFT))
        assertNull(Arith.weightedSumDigits(null, longArrayOf(1), Alignment.LEFT))
        assertNull(Arith.weightedSumDigits(view(""), longArrayOf(1), Alignment.LEFT))
    }

    @Test
    fun `weighted_sum base 36 maps letters after digits`() {
        // A = 10, Z = 35.
        assertEquals(45L, Arith.weightedSumBase36(view("AZ"), longArrayOf(1, 1), Alignment.LEFT))
        assertNull(Arith.weightedSumBase36(view("a"), longArrayOf(1), Alignment.LEFT))
        assertNull(Arith.weightedSumBase36(view("-"), longArrayOf(1), Alignment.LEFT))
    }

    @Test
    fun `weighted_sum over a custom alphabet takes the index of a code point`() {
        // The unified social credit code alphabet drops I, O, S, V and Z, so J is
        // 18 where base 36 makes it 19.
        val alphabet = cp("0123456789ABCDEFGHJKLMNPQRTUWXY")
        assertEquals(18L, Arith.weightedSumAlphabet(view("J"), longArrayOf(1), Alignment.LEFT, alphabet))
        assertEquals(19L, Arith.weightedSumBase36(view("J"), longArrayOf(1), Alignment.LEFT))
        assertNull(Arith.weightedSumAlphabet(view("I"), longArrayOf(1), Alignment.LEFT, alphabet))
    }

    @Test
    fun `weighted_sum honours a negative weight`() {
        assertEquals(-3L, Arith.weightedSumDigits(view("3"), longArrayOf(-1), Alignment.LEFT))
    }

    @Test
    fun `modulo is Euclidean, so the result always lies in the half open range`() {
        assertEquals(3L, Arith.modulo(13L, 5))
        assertEquals(2L, Arith.modulo(-13L, 5))
        assertEquals(0L, Arith.modulo(0L, 5))
        assertNull(Arith.modulo(null, 5))
    }

    @Test
    fun `complement is indeterminate outside the closed range`() {
        assertEquals(90L, Arith.complement(7L, 97))
        assertEquals(97L, Arith.complement(0L, 97))
        assertEquals(0L, Arith.complement(97L, 97))
        assertNull(Arith.complement(98L, 97))
        assertNull(Arith.complement(-1L, 97))
        assertNull(Arith.complement(null, 97))
    }

    @Test
    fun `remainder_map is indeterminate outside the table`() {
        val table = longArrayOf(5, 4, 3, 2)
        assertEquals(5L, Arith.remainderMap(0L, table))
        assertEquals(2L, Arith.remainderMap(3L, table))
        assertNull(Arith.remainderMap(4L, table))
        assertNull(Arith.remainderMap(-1L, table))
        assertNull(Arith.remainderMap(null, table))
    }
}
