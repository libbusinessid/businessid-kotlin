// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

package org.entid.generator

/**
 * The normative limits of `ir.md` section 8. An engine may raise an internal
 * limit, never lower one; none of these is raised here.
 */
internal object Limits {
    const val MAX_BUNDLE_BYTES: Int = 16_777_216
    const val MAX_IDENTIFIERS: Int = 10_000
    const val MAX_TOTAL_NODES: Int = 500_000
    const val MAX_NODES_PER_PROGRAM: Int = 4_096
    const val MAX_CALL_DEPTH: Int = 32
    const val MAX_CONSTANT_BYTES: Int = 4_096
    const val MAX_USER_INPUT_BYTES: Int = 1_024
    const val MAX_STEPS: Long = 100_000
    const val MAX_CAPTURES_PER_FORMAT: Int = 128

    const val MIN_MODULUS: Long = 2
    const val MAX_MODULUS: Long = 1_000_000_000
    const val MAX_ABS_WEIGHT: Long = 1_000_000
    const val MIN_WEIGHTS: Int = 1
    const val MAX_WEIGHTS: Int = 256
    const val MIN_REMAINDER_VALUES: Int = 1
    const val MAX_REMAINDER_VALUES: Int = 1_000_000
    const val MAX_SLICE_BOUND: Int = 4_096
    const val MAX_COMPARISON_CONSTANT: Long = 1_000_000_000
    const val MIN_CONCAT_OPERANDS: Int = 1
    const val MAX_CONCAT_OPERANDS: Int = 256
    const val MAX_DIGITS_TO_INTEGER: Int = 18
    const val MIN_ALPHABET_CODE_POINTS: Int = 1
    const val MAX_ALPHABET_CODE_POINTS: Int = 256

    const val MAX_RULES_VERSION_BYTES: Int = 64
    const val SOURCE_DIGEST_BYTES: Int = 32
    const val SUPPORTED_FORMAT_VERSION: Int = 1

    /**
     * The largest number of code points a canonical value can hold.
     *
     * The raw input is bounded at [MAX_USER_INPUT_BYTES], so at most that many
     * code points. A canonicalisation program holds at most
     * [MAX_NODES_PER_PROGRAM] steps, and no single step adds more than
     * [MAX_CONSTANT_BYTES] code points: `prepend`, `append`, `insert` and
     * `replace_prefix` insert a constant, and `left_pad` pads to a length that
     * is itself bounded by [MAX_SLICE_BOUND]. Every other step shrinks the value
     * or leaves its length alone.
     *
     * This bound is what makes the weighted sum provably free of overflow, and
     * it is deliberately generous: nothing depends on it being tight.
     */
    const val MAX_CANONICAL_CODE_POINTS: Long =
        MAX_USER_INPUT_BYTES.toLong() + MAX_NODES_PER_PROGRAM.toLong() * MAX_CONSTANT_BYTES.toLong()
}
