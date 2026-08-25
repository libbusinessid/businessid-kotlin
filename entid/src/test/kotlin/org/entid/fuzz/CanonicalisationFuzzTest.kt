// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

package org.entid.fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import org.entid.EntIdEngine
import org.entid.IdentifierInput
import org.entid.IdentifierKind
import org.entid.StepStatus
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * Canonicalisation against hostile text.
 *
 * A class of its own because libFuzzer takes over the process: Jazzer fuzzes one
 * target per JVM and skips the rest, so two targets in one class means one of
 * them never runs.
 */
class CanonicalisationFuzzTest {
    private val engine = EntIdEngine.default()

    @FuzzTest(maxDuration = "10s")
    fun `canonicalisation is idempotent on any input`(data: FuzzedDataProvider) {
        val kind = data.consumeString(40)
        val value = data.consumeString(1024)
        val once = engine.canonicalize(IdentifierInput(IdentifierKind(kind), value))
        if (once.status != StepStatus.VALID) return
        val twice = engine.canonicalize(
            IdentifierInput(IdentifierKind(once.kind.value), once.canonicalValue, once.countryCode),
        )
        assertEquals(once.canonicalValue, twice.canonicalValue)
    }
}
