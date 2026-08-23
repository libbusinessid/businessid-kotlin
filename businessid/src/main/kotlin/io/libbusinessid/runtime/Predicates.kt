// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.runtime

/**
 * The predicate primitives shared by the emitted rules.
 *
 * Every one of them yields `false` on an absent operand, except [isAbsent].
 */
internal object Pred {
    /** `is_empty(expr)`. */
    @JvmStatic
    internal fun isEmpty(v: CpView?): Boolean = v != null && v.length == 0

    /** `is_absent(expr)`, the only predicate that observes absence as true. */
    @JvmStatic
    internal fun isAbsent(v: CpView?): Boolean = v == null

    /** `equals(left, right)`. */
    @JvmStatic
    internal fun equal(a: CpView?, b: CpView?): Boolean = a != null && b != null && a.contentEquals(b)

    /** `length_eq(expr, n)`. */
    @JvmStatic
    internal fun lengthEq(v: CpView?, n: Int): Boolean = v != null && v.length == n

    /** `length_in(expr, [n...])`, with [lengths] sorted and deduplicated. */
    @JvmStatic
    internal fun lengthIn(v: CpView?, lengths: IntArray): Boolean = v != null && lengths.binarySearch(v.length) >= 0

    /** `length_between(expr, min, max)`. */
    @JvmStatic
    internal fun lengthBetween(v: CpView?, min: Int, max: Int): Boolean = v != null && v.length in min..max

    /** `ascii_digits(expr)`. */
    @JvmStatic
    internal fun asciiDigits(v: CpView?): Boolean {
        if (v == null || v.length == 0) return false
        for (i in 0 until v.length) if (!Ascii.isDigit(v[i])) return false
        return true
    }

    /** `ascii_upper_letters(expr)`. */
    @JvmStatic
    internal fun asciiUpperLetters(v: CpView?): Boolean {
        if (v == null || v.length == 0) return false
        for (i in 0 until v.length) if (!Ascii.isUpperLetter(v[i])) return false
        return true
    }

    /** `ascii_alphanumeric(expr)`. */
    @JvmStatic
    internal fun asciiAlphanumeric(v: CpView?): Boolean {
        if (v == null || v.length == 0) return false
        for (i in 0 until v.length) if (!Ascii.isAlphanumeric(v[i])) return false
        return true
    }

    /** `ascii_charset(expr, chars)`, with [set] sorted by code point. */
    @JvmStatic
    internal fun asciiCharset(v: CpView?, set: IntArray): Boolean {
        if (v == null || v.length == 0) return false
        for (i in 0 until v.length) if (!Ascii.inSet(v[i], set)) return false
        return true
    }

    /** `starts_with(expr, prefix)`. */
    @JvmStatic
    internal fun startsWith(v: CpView?, prefix: IntArray): Boolean = v != null && v.startsWith(prefix)

    /** `ends_with(expr, suffix)`. */
    @JvmStatic
    internal fun endsWith(v: CpView?, suffix: IntArray): Boolean = v != null && v.endsWith(suffix)

    /** `prefix_in(expr, prefixes)`. */
    @JvmStatic
    internal fun prefixIn(v: CpView?, prefixes: Array<IntArray>): Boolean {
        if (v == null) return false
        for (p in prefixes) if (v.startsWith(p)) return true
        return false
    }

    /** `char_at_in(expr, index, chars)`, with [set] sorted by code point. */
    @JvmStatic
    internal fun charAtIn(v: CpView?, index: Int, set: IntArray): Boolean {
        if (v == null || index >= v.length) return false
        return Ascii.inSet(v[index], set)
    }

    /** `contains(expr, literal)`. */
    @JvmStatic
    internal fun contains(v: CpView?, needle: IntArray): Boolean = v != null && v.indexOf(needle) >= 0

    /** `integer_is(int_expr, constant)`; an indeterminate operand yields `false`. */
    @JvmStatic
    internal fun integerIs(v: Long?, constant: Long): Boolean = v != null && v == constant
}
