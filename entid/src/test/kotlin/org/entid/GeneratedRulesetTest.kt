// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

package org.entid

import org.entid.generated.Ruleset
import org.entid.internal.EngineVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * The hand written declarations that have to follow the emitted ones.
 *
 * `IdentifierKind` is public API and is written by hand, so nothing but a test
 * keeps it in step with the ruleset the build compiled in.
 */
class GeneratedRulesetTest {
    @Test
    fun `the kind constants are exactly the kinds the ruleset dispatches`() {
        val declared = IdentifierKind.Companion::class.java.declaredMethods
            .filter { it.name.startsWith("get") && it.returnType == String::class.java }
            .map { it.invoke(IdentifierKind.Companion) as String }
            .sorted()
        assertEquals(Ruleset.KINDS.sorted(), declared)
    }

    @Test
    fun `every declared kind dispatches`() {
        for (kind in Ruleset.KINDS) {
            assertTrue(Ruleset.dispatcherOf(kind) >= 0, "$kind does not dispatch")
            assertEquals(kind, Ruleset.dispatcherKind(Ruleset.dispatcherOf(kind)))
        }
    }

    @Test
    fun `the version of the ruleset matches rules dot lock`() {
        val lock = File(System.getProperty("entid.spec.dir")).parentFile.resolve("rules.lock")
            .readLines()
            .mapNotNull { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("#") || "=" !in trimmed) {
                    null
                } else {
                    val (k, v) = trimmed.split("=", limit = 2)
                    k.trim() to v.trim().trim('"')
                }
            }.toMap()
        assertEquals(lock["rules_version"], Ruleset.RULES_VERSION)
        assertEquals(lock["format_version"], Ruleset.FORMAT_VERSION.toString())
    }

    @Test
    fun `the engine version follows the version Gradle publishes`() {
        val published = requireNotNull(System.getProperty("entid.project.version"))
        assertEquals(
            published.removeSuffix("-SNAPSHOT"),
            EngineVersion.VALUE,
            "the constant and the published version drifted apart",
        )
        assertEquals(EngineVersion.VALUE, EntIdEngine.default().rulesInfo().engineVersion)
    }

    @Test
    fun `the rules version and the engine version are independent`() {
        val info = EntIdEngine.default().rulesInfo()
        assertTrue(info.rulesVersion != info.engineVersion)
    }
}
