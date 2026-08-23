// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

/** Values shared by every convention plugin, kept in one place so a bump is one edit. */
object BuildConstants {
    /** JDK used to compile. Bytecode target is set separately and is lower. */
    const val TOOLCHAIN_JDK: Int = 17
}
