// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

/** Values shared by every convention plugin, kept in one place so a bump is one edit. */
object BuildConstants {
    /** JDK used to compile. Bytecode target is set separately and is lower. */
    const val TOOLCHAIN_JDK: Int = 17

    /**
     * The far end of the supported JDK range.
     *
     * `scripts/verify.sh` compiles and runs the code on it as well, through
     * `-Pbusinessid.toolchain`, and reads this number out of this file so that
     * the range has one definition rather than one per workflow.
     */
    const val TOOLCHAIN_JDK_MAX: Int = 25
}
