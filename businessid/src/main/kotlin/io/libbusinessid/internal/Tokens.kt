// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.internal

import io.libbusinessid.runtime.Ascii

/**
 * Normalisation of the kind and country tokens, before any rule runs.
 *
 * `trim ASCII` removes only `U+0009..U+000D` and `U+0020`, at both ends and
 * nowhere else, and case mapping touches only `a..z` and `A..Z`. Delegating
 * either to the platform would make the result depend on a locale.
 */
internal object Tokens {
    fun asciiTrim(s: String): String {
        var lo = 0
        var hi = s.length
        while (lo < hi && Ascii.isDispatchTrim(s[lo].code)) lo++
        while (hi > lo && Ascii.isDispatchTrim(s[hi - 1].code)) hi--
        return if (lo == 0 && hi == s.length) s else s.substring(lo, hi)
    }

    fun asciiLower(s: String): String {
        var i = 0
        while (i < s.length && s[i].code !in Ascii.UPPER_A..Ascii.UPPER_Z) i++
        if (i == s.length) return s
        val out = CharArray(s.length)
        for (j in 0 until s.length) {
            val c = s[j].code
            out[j] = if (c in Ascii.UPPER_A..Ascii.UPPER_Z) (c + (Ascii.LOWER_A - Ascii.UPPER_A)).toChar() else s[j]
        }
        return String(out)
    }

    fun asciiUpper(s: String): String {
        var i = 0
        while (i < s.length && s[i].code !in Ascii.LOWER_A..Ascii.LOWER_Z) i++
        if (i == s.length) return s
        val out = CharArray(s.length)
        for (j in 0 until s.length) {
            out[j] = Ascii.upper(s[j].code).toChar()
        }
        return String(out)
    }

    /** True when [s] is exactly two ASCII upper case letters. */
    fun isCountryToken(s: String): Boolean =
        s.length == 2 && Ascii.isUpperLetter(s[0].code) && Ascii.isUpperLetter(s[1].code)
}
