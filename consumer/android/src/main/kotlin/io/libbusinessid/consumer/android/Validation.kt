// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.consumer.android

import io.libbusinessid.BusinessIdEngine
import io.libbusinessid.IdentifierInput
import io.libbusinessid.IdentifierKind

/**
 * The library as an Android module would use it: no Android API in sight, and
 * nothing to initialise.
 */
object Validation {
    private val engine = BusinessIdEngine.default()

    /** The canonical form of [value], or the raw input when nothing could be resolved. */
    fun canonical(kind: String, value: String): String =
        engine.canonicalize(IdentifierInput(IdentifierKind(kind), value)).canonicalValue

    /** True when both the format and the checksum of [value] hold. */
    fun isFullyValidated(kind: String, value: String): Boolean =
        engine.validate(IdentifierInput(IdentifierKind(kind), value)).isFullyValidated

    /** The version of the ruleset compiled into the library. */
    fun rulesVersion(): String = engine.rulesInfo().rulesVersion
}
