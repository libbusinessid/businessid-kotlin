// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.generator

import libbusinessid.ir.v1.Rules.AssertionOpKind
import libbusinessid.ir.v1.Rules.CallOpKind
import libbusinessid.ir.v1.Rules.CanonicalizationOpKind
import libbusinessid.ir.v1.Rules.ChecksumOpKind
import libbusinessid.ir.v1.Rules.IntegerOpKind
import libbusinessid.ir.v1.Rules.PredicateOpKind
import libbusinessid.ir.v1.Rules.StringOpKind

/**
 * The frozen capability registry of `features.md`.
 *
 * A capability id designates an exact and frozen set of operations, fields,
 * bounds and semantics. Ids are never renumbered and never reused, so this table
 * only ever grows.
 */
internal object Capabilities {
    const val CORE_GRAPH_V1 = 1
    const val ASCII_AND_WHITESPACE_V1 = 2
    const val CANONICALIZATION_BASIC_V1 = 3
    const val CANONICALIZATION_CONDITIONAL_V1 = 4
    const val IDENTIFIER_DISPATCH_V1 = 5
    const val STRING_VIEWS_V1 = 10
    const val CAPTURES_AND_CALLS_V1 = 11
    const val FORMAT_ASSERTIONS_V1 = 20
    const val PROFILES_V1 = 21
    const val CHECKSUM_TRISTATE_V1 = 30
    const val CHECKSUM_LUHN_V1 = 31
    const val CHECKSUM_MOD97_V1 = 32
    const val CHECKSUM_WEIGHTED_V1 = 33
    const val CHECKSUM_COMPARE_CONSTANT_V1 = 34
    const val CHECKSUM_INTEGER_PREDICATE_V1 = 35
    const val PROVENANCE_V1 = 40
    const val PROVENANCE_TIER_V1 = 41
    const val CHECKSUM_CUSTOM_ALPHABET_V1 = 42

    /** Every capability this generator implements, with its registry name. */
    val REGISTRY: Map<Int, String> = linkedMapOf(
        CORE_GRAPH_V1 to "CORE_GRAPH_V1",
        ASCII_AND_WHITESPACE_V1 to "ASCII_AND_WHITESPACE_V1",
        CANONICALIZATION_BASIC_V1 to "CANONICALIZATION_BASIC_V1",
        CANONICALIZATION_CONDITIONAL_V1 to "CANONICALIZATION_CONDITIONAL_V1",
        IDENTIFIER_DISPATCH_V1 to "IDENTIFIER_DISPATCH_V1",
        STRING_VIEWS_V1 to "STRING_VIEWS_V1",
        CAPTURES_AND_CALLS_V1 to "CAPTURES_AND_CALLS_V1",
        FORMAT_ASSERTIONS_V1 to "FORMAT_ASSERTIONS_V1",
        PROFILES_V1 to "PROFILES_V1",
        CHECKSUM_TRISTATE_V1 to "CHECKSUM_TRISTATE_V1",
        CHECKSUM_LUHN_V1 to "CHECKSUM_LUHN_V1",
        CHECKSUM_MOD97_V1 to "CHECKSUM_MOD97_V1",
        CHECKSUM_WEIGHTED_V1 to "CHECKSUM_WEIGHTED_V1",
        CHECKSUM_COMPARE_CONSTANT_V1 to "CHECKSUM_COMPARE_CONSTANT_V1",
        CHECKSUM_INTEGER_PREDICATE_V1 to "CHECKSUM_INTEGER_PREDICATE_V1",
        PROVENANCE_V1 to "PROVENANCE_V1",
        PROVENANCE_TIER_V1 to "PROVENANCE_TIER_V1",
        CHECKSUM_CUSTOM_ALPHABET_V1 to "CHECKSUM_CUSTOM_ALPHABET_V1",
    )

    private val CORE = intArrayOf(CORE_GRAPH_V1)
    private val CORE_VIEWS = intArrayOf(CORE_GRAPH_V1, STRING_VIEWS_V1)
    private val CORE_ASCII = intArrayOf(CORE_GRAPH_V1, ASCII_AND_WHITESPACE_V1)
    private val CORE_ASSERT = intArrayOf(CORE_GRAPH_V1, FORMAT_ASSERTIONS_V1)
    private val CORE_TRISTATE = intArrayOf(CORE_GRAPH_V1, CHECKSUM_TRISTATE_V1)
    private val CORE_CANON = intArrayOf(CORE_GRAPH_V1, CANONICALIZATION_BASIC_V1)
    private val CORE_ASCII_CANON =
        intArrayOf(CORE_GRAPH_V1, ASCII_AND_WHITESPACE_V1, CANONICALIZATION_BASIC_V1)

    fun of(kind: StringOpKind): IntArray = when (kind) {
        StringOpKind.STRING_OP_KIND_CONSTANT,
        StringOpKind.STRING_OP_KIND_VALUE,
        StringOpKind.STRING_OP_KIND_SUBJECT,
        -> CORE
        StringOpKind.STRING_OP_KIND_COUNTRY_CODE -> intArrayOf(CORE_GRAPH_V1, IDENTIFIER_DISPATCH_V1)
        StringOpKind.STRING_OP_KIND_SLICE,
        StringOpKind.STRING_OP_KIND_SLICE_FROM,
        StringOpKind.STRING_OP_KIND_SLICE_TO,
        StringOpKind.STRING_OP_KIND_BEFORE_FIRST,
        StringOpKind.STRING_OP_KIND_AFTER_FIRST,
        StringOpKind.STRING_OP_KIND_STRIP_PREFIX,
        StringOpKind.STRING_OP_KIND_CONCAT,
        -> CORE_VIEWS
        else -> CORE
    }

    fun of(kind: IntegerOpKind): IntArray = when (kind) {
        IntegerOpKind.INTEGER_OP_KIND_WEIGHTED_SUM ->
            intArrayOf(CORE_GRAPH_V1, CHECKSUM_TRISTATE_V1, CHECKSUM_WEIGHTED_V1)
        else -> CORE_TRISTATE
    }

    fun of(kind: PredicateOpKind): IntArray = when (kind) {
        PredicateOpKind.PREDICATE_OP_KIND_IS_ABSENT -> CORE_VIEWS
        PredicateOpKind.PREDICATE_OP_KIND_ASCII_DIGITS,
        PredicateOpKind.PREDICATE_OP_KIND_ASCII_UPPER_LETTERS,
        PredicateOpKind.PREDICATE_OP_KIND_ASCII_ALPHANUMERIC,
        PredicateOpKind.PREDICATE_OP_KIND_ASCII_CHARSET,
        -> CORE_ASCII
        PredicateOpKind.PREDICATE_OP_KIND_PROFILE_IS -> intArrayOf(CORE_GRAPH_V1, PROFILES_V1)
        PredicateOpKind.PREDICATE_OP_KIND_INTEGER_IS ->
            intArrayOf(CORE_GRAPH_V1, CHECKSUM_TRISTATE_V1, CHECKSUM_INTEGER_PREDICATE_V1)
        else -> CORE_ASSERT
    }

    fun of(kind: CanonicalizationOpKind): IntArray = when (kind) {
        CanonicalizationOpKind.CANONICALIZATION_OP_KIND_TRIM_WHITESPACE,
        CanonicalizationOpKind.CANONICALIZATION_OP_KIND_REMOVE_WHITESPACE,
        CanonicalizationOpKind.CANONICALIZATION_OP_KIND_UPPERCASE_ASCII,
        -> CORE_ASCII_CANON
        CanonicalizationOpKind.CANONICALIZATION_OP_KIND_PREPEND_COUNTRY_IF_MISSING ->
            intArrayOf(CORE_GRAPH_V1, CANONICALIZATION_BASIC_V1, IDENTIFIER_DISPATCH_V1)
        CanonicalizationOpKind.CANONICALIZATION_OP_KIND_WHEN ->
            intArrayOf(CORE_GRAPH_V1, CANONICALIZATION_CONDITIONAL_V1)
        else -> CORE_CANON
    }

    @Suppress("UNUSED_PARAMETER")
    fun of(kind: AssertionOpKind): IntArray = CORE_ASSERT

    fun of(kind: ChecksumOpKind): IntArray = when (kind) {
        ChecksumOpKind.CHECKSUM_OP_KIND_LUHN ->
            intArrayOf(CORE_GRAPH_V1, CHECKSUM_TRISTATE_V1, CHECKSUM_LUHN_V1)
        ChecksumOpKind.CHECKSUM_OP_KIND_ISO7064_MOD97_10 ->
            intArrayOf(CORE_GRAPH_V1, CHECKSUM_TRISTATE_V1, CHECKSUM_MOD97_V1)
        ChecksumOpKind.CHECKSUM_OP_KIND_COMPARE_CONSTANT ->
            intArrayOf(CORE_GRAPH_V1, CHECKSUM_TRISTATE_V1, CHECKSUM_COMPARE_CONSTANT_V1)
        else -> CORE_TRISTATE
    }

    fun of(kind: CallOpKind): IntArray = when (kind) {
        CallOpKind.CALL_OP_KIND_FORMAT ->
            intArrayOf(CORE_GRAPH_V1, CAPTURES_AND_CALLS_V1, FORMAT_ASSERTIONS_V1)
        else -> intArrayOf(CORE_GRAPH_V1, CAPTURES_AND_CALLS_V1, CHECKSUM_TRISTATE_V1)
    }
}
