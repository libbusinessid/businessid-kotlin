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

    /** `length_in(expr, lengths)`, with [lengths] sorted and deduplicated. */
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

    /** `prefix_in(expr, prefixes)`, for a list short enough to walk. */
    @JvmStatic
    internal fun prefixIn(v: CpView?, prefixes: Array<IntArray>): Boolean {
        if (v == null) return false
        for (p in prefixes) if (v.startsWith(p)) return true
        return false
    }

    /**
     * `prefix_in(expr, prefixes)` over one group of equally long prefixes, packed
     * end to end into [packed] and sorted.
     *
     * A membership list of a few thousand entries is a different problem from a
     * list of three. Emitted as an array of arrays it costs one allocation per
     * entry before the first call and a linear walk on every call, and past
     * about fifteen hundred entries the class initialiser exceeds the sixty-four
     * kilobyte limit and the library stops compiling — which is how this arrived.
     * A string literal lives in the constant pool: no initialiser bytecode, no
     * allocation, and the order lets the lookup be a binary search.
     *
     * [codePointLength] is how many code points one entry holds and [stride] how
     * many UTF-16 units, which differ only when an entry reaches outside the
     * Basic Multilingual Plane. The generator groups the list so that both are
     * constant within a group.
     */
    @JvmStatic
    internal fun prefixInPacked(v: CpView?, packed: String, codePointLength: Int, stride: Int): Boolean {
        if (v == null || v.length < codePointLength) return false
        var low = 0
        var high = packed.length / stride - 1
        while (low <= high) {
            val middle = (low + high) ushr 1
            val order = compareEntry(packed, middle * stride, stride, v, codePointLength)
            when {
                order == 0 -> return true
                order < 0 -> low = middle + 1
                else -> high = middle - 1
            }
        }
        return false
    }

    /** Orders the entry at [from] against the first [codePointLength] code points of [v]. */
    private fun compareEntry(packed: String, from: Int, stride: Int, v: CpView, codePointLength: Int): Int {
        var at = from
        val end = from + stride
        for (i in 0 until codePointLength) {
            if (at >= end) return -1
            val c = packed[at]
            val entry: Int
            if (Character.isHighSurrogate(c) && at + 1 < end && Character.isLowSurrogate(packed[at + 1])) {
                entry = Character.toCodePoint(c, packed[at + 1])
                at += 2
            } else {
                entry = c.code
                at++
            }
            val order = entry - v[i]
            if (order != 0) return order
        }
        return if (at == end) 0 else 1
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
