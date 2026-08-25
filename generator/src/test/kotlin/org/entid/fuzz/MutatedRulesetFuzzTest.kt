// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

package org.entid.fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import org.entid.generator.Emitter
import org.entid.generator.Loader
import org.entid.generator.RulesetException
import org.entid.generator.SpecFiles
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * The published ruleset, mutated byte by byte.
 *
 * A class of its own because libFuzzer takes over the process: Jazzer fuzzes one
 * target per JVM and skips the rest, so two targets in one class means one of
 * them never runs.
 */
class MutatedRulesetFuzzTest {
    @FuzzTest(maxDuration = "10s")
    fun `a mutated published ruleset is either accepted or refused`(data: FuzzedDataProvider) {
        val original = SpecFiles.rulesBundle
        val mutated = original.copyOf()
        val edits = data.consumeInt(1, 16)
        repeat(edits) {
            val at = data.consumeInt(0, mutated.size - 1)
            mutated[at] = data.consumeByte()
        }
        try {
            val loaded = Loader.load(mutated)
            Emitter(loaded).emit()
        } catch (e: RulesetException) {
            assertTrue(e.check in 1..25, "a refusal named check ${e.check}")
        }
    }
}
