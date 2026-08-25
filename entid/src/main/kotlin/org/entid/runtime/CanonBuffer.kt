// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

package org.entid.runtime

/**
 * The value a canonicalisation program transforms, held as code points.
 *
 * A canonicalisation program never truncates and never fails: every step here
 * either changes the value or leaves it alone.
 *
 * A view handed out by [view] aliases the working array, and a step that
 * follows may rewrite it. That is sound because a view never outlives the
 * evaluation of the one predicate that asked for it: the graph produces a
 * boolean before any step runs.
 */
internal class CanonBuffer internal constructor(initial: IntArray) {
    private var buf: IntArray = initial
    private var size: Int = initial.size
    private var cached: CpView? = null

    /** The value current at this point of the program. */
    internal fun view(): CpView {
        var v = cached
        if (v == null) {
            v = CpView(buf, 0, size)
            cached = v
        }
        return v
    }

    /** The value as a string. */
    internal fun snapshot(): String = Utf.toStringOf(buf, 0, size)

    /** True when the current value starts with [prefix]. */
    internal fun startsWith(prefix: IntArray): Boolean {
        if (prefix.size > size) return false
        for (i in prefix.indices) if (buf[i] != prefix[i]) return false
        return true
    }

    /** `trim_whitespace()`. */
    internal fun trimWhitespace() {
        var lo = 0
        var hi = size
        while (lo < hi && Ascii.isWhitespace(buf[lo])) lo++
        while (hi > lo && Ascii.isWhitespace(buf[hi - 1])) hi--
        if (lo == 0 && hi == size) return
        System.arraycopy(buf, lo, buf, 0, hi - lo)
        size = hi - lo
        cached = null
    }

    /** `remove_whitespace()`. */
    internal fun removeWhitespace() {
        filterOut { Ascii.isWhitespace(it) }
    }

    /** `remove_chars(list)`, with [set] sorted by code point. */
    internal fun removeChars(set: IntArray) {
        filterOut { Ascii.inSet(it, set) }
    }

    /** `uppercase_ascii()`. */
    internal fun uppercaseAscii() {
        var changed = false
        for (i in 0 until size) {
            val u = Ascii.upper(buf[i])
            if (u != buf[i]) {
                buf[i] = u
                changed = true
            }
        }
        if (changed) cached = null
    }

    /** `replace_prefix(from, to)`. */
    internal fun replacePrefix(from: IntArray, to: IntArray) {
        if (!startsWith(from)) return
        val newSize = size - from.size + to.size
        ensure(newSize)
        System.arraycopy(buf, from.size, buf, to.size, size - from.size)
        System.arraycopy(to, 0, buf, 0, to.size)
        size = newSize
        cached = null
    }

    /** `prepend(value)`. */
    internal fun prepend(text: IntArray) {
        insertAt(0, text)
    }

    /** `append(value)`. */
    internal fun append(text: IntArray) {
        insertAt(size, text)
    }

    /** `insert(index, value)`; an index past the end leaves the value unchanged. */
    internal fun insert(index: Int, text: IntArray) {
        if (index > size) return
        insertAt(index, text)
    }

    /** `left_pad(length, char)`; a longer value is never truncated. */
    internal fun leftPad(length: Int, pad: Int) {
        if (size >= length) return
        val missing = length - size
        ensure(length)
        System.arraycopy(buf, 0, buf, missing, size)
        for (i in 0 until missing) buf[i] = pad
        size = length
        cached = null
    }

    private fun insertAt(index: Int, text: IntArray) {
        if (text.isEmpty()) return
        ensure(size + text.size)
        System.arraycopy(buf, index, buf, index + text.size, size - index)
        System.arraycopy(text, 0, buf, index, text.size)
        size += text.size
        cached = null
    }

    private inline fun filterOut(drop: (Int) -> Boolean) {
        var out = 0
        for (i in 0 until size) {
            val c = buf[i]
            if (!drop(c)) buf[out++] = c
        }
        if (out != size) {
            size = out
            cached = null
        }
    }

    private fun ensure(capacity: Int) {
        if (buf.size >= capacity) return
        var n = if (buf.size == 0) capacity else buf.size
        while (n < capacity) n *= 2
        buf = buf.copyOf(n)
        cached = null
    }
}
