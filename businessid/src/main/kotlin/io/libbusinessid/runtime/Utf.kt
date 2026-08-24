// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.runtime

/**
 * Conversions between a Kotlin `String` and the code point model the rules use.
 *
 * A Kotlin `String` is a sequence of UTF-16 code units and admits an unpaired
 * surrogate, which no UTF-8 byte sequence encodes. That is the only ill formed
 * text this API can receive, and it is what [isWellFormed] detects.
 */
internal object Utf {
    private const val SURROGATE_OFFSET = 0x10000
    private const val LEAD_SHIFT = 10
    private const val TRAIL_MASK = 0x3FF
    private const val ONE_BYTE_MAX = 0x7F
    private const val TWO_BYTE_MAX = 0x7FF
    private const val THREE_BYTE_MAX = 0xFFFF
    private const val TWO_BYTE_LENGTH = 2
    private const val THREE_BYTE_LENGTH = 3
    private const val FOUR_BYTE_LENGTH = 4

    /**
     * UTF-8 length of [s] in bytes, without materialising the encoded form.
     *
     * An unpaired surrogate has no UTF-8 encoding; it is counted as the three
     * bytes its code unit would take, which is the only count that does not
     * depend on a replacement policy. Such input is refused by the encoding
     * check that follows, so the choice is observable only for text that is both
     * ill formed and within a few bytes of the limit.
     */
    @JvmStatic
    internal fun utf8Length(s: String): Int {
        var n = 0
        var i = 0
        val len = s.length
        while (i < len) {
            val c = s[i]
            when {
                c.code <= ONE_BYTE_MAX -> n += 1

                c.code <= TWO_BYTE_MAX -> n += TWO_BYTE_LENGTH

                Character.isHighSurrogate(c) && i + 1 < len && Character.isLowSurrogate(s[i + 1]) -> {
                    n += FOUR_BYTE_LENGTH
                    i++
                }

                else -> n += THREE_BYTE_LENGTH
            }
            i++
        }
        return n
    }

    /** True when [s] holds no unpaired surrogate, hence encodes to UTF-8. */
    @JvmStatic
    internal fun isWellFormed(s: String): Boolean {
        var i = 0
        val len = s.length
        while (i < len) {
            val c = s[i]
            if (Character.isHighSurrogate(c)) {
                if (i + 1 >= len || !Character.isLowSurrogate(s[i + 1])) return false
                i += 2
                continue
            }
            if (Character.isLowSurrogate(c)) return false
            i++
        }
        return true
    }

    /** Code points of a well formed [s]. */
    @JvmStatic
    internal fun codePoints(s: String): IntArray {
        val out = IntArray(s.length)
        var n = 0
        var i = 0
        val len = s.length
        while (i < len) {
            val c = s[i]
            if (Character.isHighSurrogate(c) && i + 1 < len && Character.isLowSurrogate(s[i + 1])) {
                out[n++] = SURROGATE_OFFSET +
                    ((c.code - Character.MIN_HIGH_SURROGATE.code) shl LEAD_SHIFT) +
                    (s[i + 1].code - Character.MIN_LOW_SURROGATE.code)
                i += 2
            } else {
                out[n++] = c.code
                i++
            }
        }
        return if (n == out.size) out else out.copyOf(n)
    }

    /** The string spelt by `codePoints[start until end]`. */
    @JvmStatic
    internal fun toStringOf(codePoints: IntArray, start: Int, end: Int): String {
        val sb = StringBuilder(end - start)
        for (i in start until end) {
            val v = codePoints[i]
            if (v <= THREE_BYTE_MAX) {
                sb.append(v.toChar())
            } else {
                val u = v - SURROGATE_OFFSET
                sb.append((Character.MIN_HIGH_SURROGATE.code + (u shr LEAD_SHIFT)).toChar())
                sb.append((Character.MIN_LOW_SURROGATE.code + (u and TRAIL_MASK)).toChar())
            }
        }
        return sb.toString()
    }
}
