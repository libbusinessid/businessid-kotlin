// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

package org.entid.internal

/**
 * Version of this library, independent of the rules version and of the format
 * version.
 *
 * It is a constant rather than a manifest lookup because the published library
 * reads no resource at all. `EngineVersionTest` compares it with the version
 * Gradle publishes, so the two cannot drift.
 */
internal object EngineVersion {
    const val VALUE: String = "0.1.0"
}
