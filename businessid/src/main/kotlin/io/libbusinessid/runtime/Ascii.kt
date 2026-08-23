// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.runtime

/**
 * The frozen ASCII classes and the frozen whitespace table.
 *
 * None of this is delegated to the platform's Unicode tables or to a locale:
 * two engines that consulted their own would disagree the day one of them
 * shipped a newer Unicode version.
 */
internal object Ascii {
    internal const val DIGIT_0 = 0x30
    internal const val DIGIT_9 = 0x39
    internal const val UPPER_A = 0x41
    internal const val UPPER_Z = 0x5A
    internal const val LOWER_A = 0x61
    internal const val LOWER_Z = 0x7A
    private const val CASE_DELTA = LOWER_A - UPPER_A
    private const val BASE36_LETTER_OFFSET = 10

    /** True when [c] is `U+0030..U+0039`. */
    @JvmStatic
    internal fun isDigit(c: Int): Boolean = c in DIGIT_0..DIGIT_9

    /** True when [c] is `U+0041..U+005A`. */
    @JvmStatic
    internal fun isUpperLetter(c: Int): Boolean = c in UPPER_A..UPPER_Z

    /** True when [c] is an ASCII digit or an ASCII upper case letter. */
    @JvmStatic
    internal fun isAlphanumeric(c: Int): Boolean = isDigit(c) || isUpperLetter(c)

    /** Maps only `a..z` to `A..Z`, never consulting a locale. */
    @JvmStatic
    internal fun upper(c: Int): Int = if (c in LOWER_A..LOWER_Z) c - CASE_DELTA else c

    /** Decimal value of an ASCII digit, or `-1`. */
    @JvmStatic
    internal fun digitValue(c: Int): Int = if (isDigit(c)) c - DIGIT_0 else -1

    /** Base 36 value of an ASCII digit or upper case letter, or `-1`. */
    @JvmStatic
    internal fun base36Value(c: Int): Int =
        when {
            isDigit(c) -> c - DIGIT_0
            isUpperLetter(c) -> c - UPPER_A + BASE36_LETTER_OFFSET
            else -> -1
        }

    /**
     * The frozen `whitespace_v1` table, sorted so a lookup is a binary search.
     *
     * `U+0009..U+000D, U+0020, U+0085, U+00A0, U+1680, U+2000..U+200A, U+2028,
     * U+2029, U+202F, U+205F, U+3000, U+FEFF`.
     */
    private val WHITESPACE_V1: IntArray = intArrayOf(
        0x0009, 0x000A, 0x000B, 0x000C, 0x000D,
        0x0020,
        0x0085,
        0x00A0,
        0x1680,
        0x2000, 0x2001, 0x2002, 0x2003, 0x2004, 0x2005,
        0x2006, 0x2007, 0x2008, 0x2009, 0x200A,
        0x2028, 0x2029,
        0x202F,
        0x205F,
        0x3000,
        0xFEFF,
    )

    /** True when [c] belongs to the frozen `whitespace_v1` table. */
    @JvmStatic
    internal fun isWhitespace(c: Int): Boolean = WHITESPACE_V1.binarySearch(c) >= 0

    /**
     * True when [c] is one of the code points a dispatch trim removes:
     * `U+0009..U+000D` and `U+0020`, and nothing else.
     */
    @JvmStatic
    internal fun isDispatchTrim(c: Int): Boolean = c in 0x0009..0x000D || c == 0x0020

    /** True when [c] belongs to the sorted set [set]. */
    @JvmStatic
    internal fun inSet(c: Int, set: IntArray): Boolean = set.binarySearch(c) >= 0
}
