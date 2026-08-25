// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

package org.entid.runtime

/** Builds the views the IR operations read, from ordinary strings. */
internal fun view(s: String): CpView = CpView.of(Utf.codePoints(s))

/** The code points of a constant, as the emitted rules carry them. */
internal fun cp(s: String): IntArray = Utf.codePoints(s)

/** A sorted character set, as the generator emits one. */
internal fun set(s: String): IntArray = Utf.codePoints(s).toSortedSet().toIntArray()

internal fun CpView?.text(): String? = this?.toString()
