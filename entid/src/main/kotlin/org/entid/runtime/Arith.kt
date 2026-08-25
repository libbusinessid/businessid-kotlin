// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

package org.entid.runtime

/**
 * Alignment of a weight list against the code points it multiplies.
 */
internal enum class Alignment {
    /** Position `i` pairs with `weights[i]`. */
    LEFT,

    /** The last position pairs with the last weight. */
    RIGHT,

    /** Position `i` pairs with `weights[i mod len(weights)]`. */
    CYCLE,
}

/**
 * The integer primitives shared by the emitted rules.
 *
 * `null` is the indeterminate integer. It propagates through every operation
 * and makes the enclosing checksum node `unsupported`; it never proves an
 * identifier wrong. Arithmetic is checked: the load checks prove no operation
 * can overflow, so an [ArithmeticException] here would be a broken invariant
 * rather than a business outcome, and it surfaces as an engine error.
 */
internal object Arith {
    private const val DECIMAL_BASE = 10L
    private const val BASE36_TENS = 10

    /** `digits_to_integer(expr)`; the caller has proved the view holds at most 18 digits. */
    @JvmStatic
    internal fun digitsToInteger(v: CpView?): Long? {
        if (v == null || v.length == 0) return null
        var acc = 0L
        for (i in 0 until v.length) {
            val d = Ascii.digitValue(v[i])
            if (d < 0) return null
            acc = Math.addExact(Math.multiplyExact(acc, DECIMAL_BASE), d.toLong())
        }
        return acc
    }

    /** `mod_digits(expr, modulus)`, computed digit by digit without any wide conversion. */
    @JvmStatic
    internal fun modDigits(v: CpView?, modulus: Long): Long? {
        if (v == null || v.length == 0) return null
        var acc = 0L
        for (i in 0 until v.length) {
            val d = Ascii.digitValue(v[i])
            if (d < 0) return null
            acc = (acc * DECIMAL_BASE + d) % modulus
        }
        return acc
    }

    /** `weighted_sum(expr, weights, alignment, DIGIT_VALUE)`. */
    @JvmStatic
    internal fun weightedSumDigits(v: CpView?, weights: LongArray, alignment: Alignment): Long? =
        weightedSum(v, weights, alignment, null, base36 = false)

    /** `weighted_sum(expr, weights, alignment, ALNUM_BASE36)`. */
    @JvmStatic
    internal fun weightedSumBase36(v: CpView?, weights: LongArray, alignment: Alignment): Long? =
        weightedSum(v, weights, alignment, null, base36 = true)

    /**
     * `weighted_sum(expr, weights, alignment, CUSTOM_ALPHABET, alphabet)`.
     *
     * The value of a code point is its index in [alphabet]. A code point absent
     * from it makes the sum indeterminate, exactly as a letter does under
     * `DIGIT_VALUE`.
     */
    @JvmStatic
    internal fun weightedSumAlphabet(v: CpView?, weights: LongArray, alignment: Alignment, alphabet: IntArray): Long? =
        weightedSum(v, weights, alignment, alphabet, base36 = false)

    private fun weightedSum(
        v: CpView?,
        weights: LongArray,
        alignment: Alignment,
        alphabet: IntArray?,
        base36: Boolean,
    ): Long? {
        if (v == null || v.length == 0) return null
        val n = v.length
        val w = weights.size

        // Every code point of the view must map, even at a position no weight
        // pairs with: a value holding a letter where the algorithm expects a
        // digit is not one the algorithm can speak about.
        val values = IntArray(n)
        for (i in 0 until n) {
            val mapped = mapValue(v[i], alphabet, base36)
            if (mapped < 0) return null
            values[i] = mapped
        }

        var acc = 0L
        when (alignment) {
            Alignment.LEFT -> {
                val pairs = if (n < w) n else w
                for (i in 0 until pairs) acc = accumulate(acc, values[i], weights[i])
            }

            Alignment.RIGHT -> {
                val pairs = if (n < w) n else w
                for (k in 0 until pairs) acc = accumulate(acc, values[n - 1 - k], weights[w - 1 - k])
            }

            Alignment.CYCLE -> {
                for (i in 0 until n) acc = accumulate(acc, values[i], weights[i % w])
            }
        }
        return acc
    }

    private fun accumulate(acc: Long, value: Int, weight: Long): Long =
        Math.addExact(acc, Math.multiplyExact(value.toLong(), weight))

    private fun mapValue(c: Int, alphabet: IntArray?, base36: Boolean): Int = when {
        alphabet != null -> indexIn(alphabet, c)
        base36 -> Ascii.base36Value(c)
        else -> Ascii.digitValue(c)
    }

    private fun indexIn(alphabet: IntArray, c: Int): Int {
        for (i in alphabet.indices) if (alphabet[i] == c) return i
        return -1
    }

    /** `modulo(int_expr, modulus)`, Euclidean, so the result always lies in `[0, modulus)`. */
    @JvmStatic
    internal fun modulo(v: Long?, modulus: Long): Long? {
        if (v == null) return null
        val r = v % modulus
        return if (r < 0) r + modulus else r
    }

    /** `complement(int_expr, modulus)`; indeterminate outside `[0, modulus]`. */
    @JvmStatic
    internal fun complement(v: Long?, modulus: Long): Long? {
        if (v == null || v < 0 || v > modulus) return null
        return modulus - v
    }

    /** `remainder_map(int_expr, values)`; indeterminate outside the table. */
    @JvmStatic
    internal fun remainderMap(v: Long?, values: LongArray): Long? {
        if (v == null || v < 0 || v >= values.size) return null
        return values[v.toInt()]
    }

    /** Expands ASCII letters to their base 36 decimal spelling, then reduces modulo 97. */
    @JvmStatic
    internal fun mod97OfExpanded(v: CpView): Long? {
        var acc = 0L
        for (i in 0 until v.length) {
            val value = Ascii.base36Value(v[i])
            if (value < 0) return null
            acc = if (value < BASE36_TENS) {
                (acc * DECIMAL_BASE + value) % MOD97
            } else {
                ((acc * DECIMAL_BASE + value / BASE36_TENS) % MOD97 * DECIMAL_BASE + value % BASE36_TENS) % MOD97
            }
        }
        return acc
    }

    internal const val MOD97 = 97L
}
