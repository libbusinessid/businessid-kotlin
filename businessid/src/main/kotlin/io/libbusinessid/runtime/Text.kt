// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.runtime

/**
 * A possibly absent view over a sequence of Unicode code points.
 *
 * Positions and lengths are counted in code points, never in UTF-16 units: a
 * character outside the Basic Multilingual Plane occupies two `Char`s in a
 * Kotlin `String` and exactly one position here.
 *
 * Absence is expressed by a `null` reference. Every string constructor applied
 * to an absent operand yields absent, and every predicate applied to one yields
 * `false` except `IS_ABSENT`. Absence is never an error.
 */
internal class CpView(
    @JvmField internal val cp: IntArray,
    @JvmField internal val start: Int,
    @JvmField internal val end: Int,
) {
    /** Number of code points in the view. */
    internal val length: Int get() = end - start

    internal operator fun get(index: Int): Int = cp[start + index]

    internal fun contentEquals(other: CpView): Boolean {
        if (length != other.length) return false
        for (i in 0 until length) {
            if (cp[start + i] != other.cp[other.start + i]) return false
        }
        return true
    }

    internal fun startsWith(needle: IntArray): Boolean {
        if (needle.size > length) return false
        for (i in needle.indices) {
            if (cp[start + i] != needle[i]) return false
        }
        return true
    }

    internal fun endsWith(needle: IntArray): Boolean {
        if (needle.size > length) return false
        val base = end - needle.size
        for (i in needle.indices) {
            if (cp[base + i] != needle[i]) return false
        }
        return true
    }

    internal fun indexOf(needle: IntArray): Int {
        if (needle.isEmpty()) return 0
        val last = length - needle.size
        var i = 0
        while (i <= last) {
            var j = 0
            while (j < needle.size && cp[start + i + j] == needle[j]) j++
            if (j == needle.size) return i
            i++
        }
        return -1
    }

    override fun toString(): String = Utf.toStringOf(cp, start, end)

    /** Structural equality over the code points in view, ignoring the backing array. */
    override fun equals(other: Any?): Boolean = other is CpView && contentEquals(other)

    override fun hashCode(): Int {
        var h = 1
        for (i in start until end) h = 31 * h + cp[i]
        return h
    }

    internal companion object {
        private val EMPTY = CpView(IntArray(0), 0, 0)

        fun of(codePoints: IntArray): CpView = CpView(codePoints, 0, codePoints.size)

        fun ofEmpty(): CpView = EMPTY
    }
}

/** Text primitives shared by the emitted rules. */
internal object Txt {
    /** `slice(expr, start, end)`. */
    @JvmStatic
    internal fun slice(v: CpView?, start: Int, end: Int): CpView? {
        if (v == null || start > end || end > v.length) return null
        return CpView(v.cp, v.start + start, v.start + end)
    }

    /** `slice_from(expr, start)`. */
    @JvmStatic
    internal fun sliceFrom(v: CpView?, start: Int): CpView? {
        if (v == null || start > v.length) return null
        return CpView(v.cp, v.start + start, v.end)
    }

    /** `slice_to(expr, end)`. */
    @JvmStatic
    internal fun sliceTo(v: CpView?, end: Int): CpView? {
        if (v == null || end > v.length) return null
        return CpView(v.cp, v.start, v.start + end)
    }

    /** `before_first(expr, delimiter)`. */
    @JvmStatic
    internal fun beforeFirst(v: CpView?, needle: IntArray): CpView? {
        if (v == null) return null
        val at = v.indexOf(needle)
        if (at < 0) return null
        return CpView(v.cp, v.start, v.start + at)
    }

    /** `after_first(expr, delimiter)`. */
    @JvmStatic
    internal fun afterFirst(v: CpView?, needle: IntArray): CpView? {
        if (v == null) return null
        val at = v.indexOf(needle)
        if (at < 0) return null
        return CpView(v.cp, v.start + at + needle.size, v.end)
    }

    /** `strip_prefix(expr, prefix)`. */
    @JvmStatic
    internal fun stripPrefix(v: CpView?, prefix: IntArray): CpView? {
        if (v == null || !v.startsWith(prefix)) return null
        return CpView(v.cp, v.start + prefix.size, v.end)
    }

    /** `concat(expr...)`. */
    @JvmStatic
    internal fun concat(parts: Array<CpView?>): CpView? {
        var total = 0
        // The operands are collected as they are checked, so the copy below has
        // no null case left to handle — and therefore no branch no input takes.
        val present = ArrayList<CpView>(parts.size)
        for (p in parts) {
            if (p == null) return null
            present += p
            total += p.length
        }
        val out = IntArray(total)
        var at = 0
        for (v in present) {
            for (i in 0 until v.length) out[at++] = v[i]
        }
        return CpView(out, 0, total)
    }
}
