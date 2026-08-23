// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.runtime

import io.libbusinessid.ReasonCode
import io.libbusinessid.StepStatus

/**
 * The failure of the first assertion of a format program that did not hold.
 *
 * A format program yields `null` when every assertion held.
 *
 * @property reason the reason the assertion declares; always one that proves invalidity.
 * @property messageKey the stable key the assertion declares, or `null`.
 */
internal class AssertionFailure internal constructor(
    @JvmField internal val reason: ReasonCode,
    @JvmField internal val messageKey: String?,
)

/**
 * The tri-state result of a checksum program.
 *
 * @property status `valid`, `invalid` or `unsupported`; never `not_run`.
 * @property reason the reason that accompanies the status.
 * @property messageKey the key the producing node declares. A valid outcome
 *   carries none: the key belongs to the outcome the rule declares, and a rule
 *   declares no key for succeeding.
 */
internal class ChecksumOutcome internal constructor(
    @JvmField internal val status: StepStatus,
    @JvmField internal val reason: ReasonCode,
    @JvmField internal val messageKey: String?,
) {
    internal companion object {
        val VALID = ChecksumOutcome(StepStatus.VALID, ReasonCode.OK, null)
        private val INVALID = ChecksumOutcome(StepStatus.INVALID, ReasonCode.INVALID_CHECKSUM, null)
        private val UNSUPPORTED = ChecksumOutcome(StepStatus.UNSUPPORTED, ReasonCode.UNSUPPORTED_CHECKSUM, null)
        private val NOT_PUBLISHED =
            ChecksumOutcome(StepStatus.UNSUPPORTED, ReasonCode.CHECKSUM_NOT_PUBLISHED, null)

        fun invalid(messageKey: String?): ChecksumOutcome =
            if (messageKey == null) INVALID else ChecksumOutcome(StepStatus.INVALID, ReasonCode.INVALID_CHECKSUM, messageKey)

        fun unsupported(messageKey: String?): ChecksumOutcome =
            if (messageKey == null) {
                UNSUPPORTED
            } else {
                ChecksumOutcome(StepStatus.UNSUPPORTED, ReasonCode.UNSUPPORTED_CHECKSUM, messageKey)
            }

        fun declaredUnsupported(reason: ReasonCode, messageKey: String?): ChecksumOutcome =
            when {
                messageKey != null -> ChecksumOutcome(StepStatus.UNSUPPORTED, reason, messageKey)
                reason == ReasonCode.CHECKSUM_NOT_PUBLISHED -> NOT_PUBLISHED
                else -> UNSUPPORTED
            }
    }
}
