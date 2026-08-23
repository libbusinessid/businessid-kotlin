// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.generator

/**
 * The two ways a ruleset can be refused.
 *
 * The distinction is deliberate and is what tells an operator to upgrade rather
 * than to suspect the file. An unsupported `format_version` and an unknown
 * capability id announce a version gap; everything else is a structural defect.
 */
internal enum class RulesetErrorKind(val wireName: String) {
    /** Size, structure, arithmetic or graph violation. */
    INVALID_RULESET("invalid_ruleset"),

    /** The ruleset announces a version or a capability this build does not implement. */
    INCOMPATIBLE_RULESET("incompatible_ruleset"),
}

/**
 * A refusal, carrying the number of the load check that produced it.
 *
 * The number is not decoration: the fixtures of the shared corpus each target
 * one check, and a fixture refused by an earlier check than the one it is named
 * for proves nothing. `LoaderFixtureTest` asserts the number, not just the kind.
 */
internal class RulesetException(
    val errorKind: RulesetErrorKind,
    val check: Int,
    override val message: String,
) : Exception(message)

internal fun invalidRuleset(check: Int, message: String): Nothing =
    throw RulesetException(RulesetErrorKind.INVALID_RULESET, check, message)

internal fun incompatibleRuleset(check: Int, message: String): Nothing =
    throw RulesetException(RulesetErrorKind.INCOMPATIBLE_RULESET, check, message)
