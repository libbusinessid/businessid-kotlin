// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

package org.entid.internal

import org.entid.EntIdEngineException

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
@Suppress("TooGenericExceptionCaught")
internal inline fun <T> guardEngineErrors(step: String, body: () -> T): T = try {
    body()
} catch (e: ArithmeticException) {
    throw EntIdEngineException("the $step program overflowed a checked integer", e)
} catch (e: IndexOutOfBoundsException) {
    throw EntIdEngineException("the $step program addressed outside a view", e)
}
