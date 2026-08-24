// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.internal

import io.libbusinessid.BusinessIdEngineException

/**
 * Turns a broken internal invariant into an engine error.
 *
 * The load checks of the generator prove that no emitted arithmetic can overflow
 * and that no view can be addressed out of bounds, so neither of these can
 * happen. They are caught rather than left to escape because the difference
 * matters if one ever does: an engine error says the engine is wrong, where an
 * `ArithmeticException` reaching a caller says nothing at all.
 *
 * Ordinary user input never reaches here: a value the rules reject produces a
 * report.
 */
internal inline fun <T> guardEngineErrors(step: String, body: () -> T): T =
    try {
        body()
    } catch (e: ArithmeticException) {
        throw BusinessIdEngineException("the $step program overflowed a checked integer", e)
    } catch (e: IndexOutOfBoundsException) {
        throw BusinessIdEngineException("the $step program addressed outside a view", e)
    }
