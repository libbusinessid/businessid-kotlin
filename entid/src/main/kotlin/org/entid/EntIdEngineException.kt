// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

package org.entid

/**
 * A technical failure of the engine itself.
 *
 * Ordinary user input never raises this: a value the rules reject produces a
 * report, not an exception. This type exists for a broken internal invariant,
 * which the load checks of the generator are supposed to have made impossible.
 */
public class EntIdEngineException internal constructor(message: String, cause: Throwable) :
    RuntimeException(message, cause)
