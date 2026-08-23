// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import io.libbusinessid.generator.Emitter
import io.libbusinessid.generator.Loader
import io.libbusinessid.generator.RulesetException
import io.libbusinessid.generator.SpecFiles
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * The generator against hostile bytes.
 *
 * A ruleset is untrusted input. Whatever it holds, the generator either accepts
 * it — in which case it must be able to emit from it — or refuses it with a
 * typed refusal. Anything else escaping is a defect.
 */
class LoaderFuzzTest {
    @FuzzTest(maxDuration = "10s")
    fun `arbitrary bytes are either accepted or refused, never anything else`(data: FuzzedDataProvider) {
        val bytes = data.consumeRemainingAsBytes()
        try {
            val loaded = Loader.load(bytes)
            // Acceptance is a promise that code can be emitted from it.
            Emitter(loaded).emit()
        } catch (e: RulesetException) {
            assertTrue(e.check in 1..25, "a refusal named check ${e.check}")
        }
    }

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
