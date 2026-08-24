// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.runtime

import io.libbusinessid.ReasonCode

/**
 * The checksum primitives shared by the emitted rules.
 *
 * An indeterminate operand always yields `unsupported`, never `invalid`.
 */
internal object Ck {
    private const val LUHN_MIN_LENGTH = 2
    private const val MOD97_MIN_LENGTH = 3
    private const val TEN = 10
    private const val NINE = 9

    /** A valid outcome, carrying no key. */
    @JvmStatic
    internal fun valid(): ChecksumOutcome = ChecksumOutcome.VALID

    /** `luhn(expr)`. */
    @JvmStatic
    internal fun luhn(v: CpView?, messageKey: String?): ChecksumOutcome {
        if (v == null || v.length < LUHN_MIN_LENGTH) return ChecksumOutcome.unsupported(messageKey)
        var sum = 0
        var double = false
        for (i in v.length - 1 downTo 0) {
            val d = Ascii.digitValue(v[i])
            if (d < 0) return ChecksumOutcome.unsupported(messageKey)
            var add = d
            if (double) {
                add *= 2
                if (add > NINE) add -= NINE
            }
            sum += add
            double = !double
        }
        return if (sum % TEN == 0) ChecksumOutcome.VALID else ChecksumOutcome.invalid(messageKey)
    }

    /** `iso7064_mod97_10(expr)`. */
    @JvmStatic
    internal fun iso7064Mod97(v: CpView?, messageKey: String?): ChecksumOutcome {
        if (v == null || v.length < MOD97_MIN_LENGTH) return ChecksumOutcome.unsupported(messageKey)
        val r = Arith.mod97OfExpanded(v) ?: return ChecksumOutcome.unsupported(messageKey)
        return if (r == 1L) ChecksumOutcome.VALID else ChecksumOutcome.invalid(messageKey)
    }

    /** `compare_digit(int_expr, string_expr, index)`. */
    @JvmStatic
    internal fun compareDigit(value: Long?, v: CpView?, index: Int, messageKey: String?): ChecksumOutcome {
        if (value == null || v == null || index >= v.length) return ChecksumOutcome.unsupported(messageKey)
        val d = Ascii.digitValue(v[index])
        if (d < 0) return ChecksumOutcome.unsupported(messageKey)
        return if (value == d.toLong()) ChecksumOutcome.VALID else ChecksumOutcome.invalid(messageKey)
    }

    /** `compare_slice(int_expr, string_expr, start, end)`. */
    @JvmStatic
    internal fun compareSlice(
        value: Long?,
        v: CpView?,
        start: Int,
        end: Int,
        messageKey: String?,
    ): ChecksumOutcome {
        if (value == null) return ChecksumOutcome.unsupported(messageKey)
        val slice = Txt.slice(v, start, end) ?: return ChecksumOutcome.unsupported(messageKey)
        val actual = Arith.digitsToInteger(slice) ?: return ChecksumOutcome.unsupported(messageKey)
        return if (value == actual) ChecksumOutcome.VALID else ChecksumOutcome.invalid(messageKey)
    }

    /** `compare_constant(int_expr, constant)`. */
    @JvmStatic
    internal fun compareConstant(value: Long?, constant: Long, messageKey: String?): ChecksumOutcome {
        if (value == null) return ChecksumOutcome.unsupported(messageKey)
        return if (value == constant) ChecksumOutcome.VALID else ChecksumOutcome.invalid(messageKey)
    }

    /** `unsupported_checksum(reason_code)`. */
    @JvmStatic
    internal fun declaredUnsupported(reason: ReasonCode, messageKey: String?): ChecksumOutcome =
        ChecksumOutcome.declaredUnsupported(reason, messageKey)

    /** The outcome of a `choose` whose branches were all inapplicable. */
    @JvmStatic
    internal fun noBranch(): ChecksumOutcome = ChecksumOutcome.unsupported(null)

    /**
     * `all_checks(rule...)`: the first invalid outcome, else the first
     * unsupported one, else valid.
     */
    @JvmStatic
    internal fun allChecks(branches: Array<ChecksumOutcome>): ChecksumOutcome {
        var firstUnsupported: ChecksumOutcome? = null
        for (b in branches) {
            when (b.status) {
                ChecksumStatus.INVALID -> return b
                ChecksumStatus.UNSUPPORTED -> if (firstUnsupported == null) firstUnsupported = b
                ChecksumStatus.VALID -> Unit
            }
        }
        return firstUnsupported ?: ChecksumOutcome.VALID
    }

    /**
     * `any_check(rule...)`: valid as soon as one operand is valid, else the
     * first unsupported outcome, else the first invalid one.
     */
    @JvmStatic
    internal fun anyCheck(branches: Array<ChecksumOutcome>): ChecksumOutcome {
        var firstUnsupported: ChecksumOutcome? = null
        var firstInvalid: ChecksumOutcome? = null
        for (b in branches) {
            when (b.status) {
                ChecksumStatus.VALID -> return ChecksumOutcome.VALID
                ChecksumStatus.UNSUPPORTED -> if (firstUnsupported == null) firstUnsupported = b
                ChecksumStatus.INVALID -> if (firstInvalid == null) firstInvalid = b
            }
        }
        return firstUnsupported ?: firstInvalid ?: ChecksumOutcome.unsupported(null)
    }
}
