// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

package org.entid.fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import org.entid.EntIdEngine
import org.entid.IdentifierInput
import org.entid.IdentifierKind
import org.entid.ReasonCode
import org.entid.StepStatus
import org.entid.ValidationOptions
import org.entid.ValidationProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals

/**
 * The engine against hostile text.
 *
 * Ordinary user input never raises: a value the rules reject produces a report.
 * The fuzzer's job is to find a string for which that is not true — an
 * exception, a hang, or an unbounded allocation — and to check that the contract
 * still holds on whatever it does produce.
 *
 * Without `JAZZER_FUZZ` these run in regression mode over the recorded corpus,
 * which keeps them cheap in an ordinary build. The `fuzz` task turns fuzzing on.
 */
class EngineFuzzTest {
    private val engine = EntIdEngine.default()

    @FuzzTest(maxDuration = "10s")
    fun `no input makes the engine throw or break its contract`(data: FuzzedDataProvider) {
        val kind = data.consumeString(80)
        val value = data.consumeString(2048)
        val country = if (data.consumeBoolean()) data.consumeString(16) else null
        val profile = when (data.consumeInt(0, 2)) {
            0 -> null
            1 -> ValidationProfile.COMPATIBLE
            else -> ValidationProfile.STRICT_CURRENT
        }
        val input = IdentifierInput(IdentifierKind(kind), value, country)
        val options = ValidationOptions(profile)

        val canonical = engine.canonicalize(input, options)
        assertNotEquals(StepStatus.NOT_RUN, canonical.status, "canonicalisation never ends in not_run")
        assertEquals(value, canonical.inputValue, "the raw input is kept unchanged")

        val report = engine.validate(input, options)
        assertEquals(value, report.inputValue)
        if (report.format.status != StepStatus.VALID) {
            assertEquals(StepStatus.NOT_RUN, report.checksum.status, "a checksum ran after a failing format")
        }
        if (report.format.status == StepStatus.VALID) {
            assertEquals(ReasonCode.OK, report.format.reasonCode)
        }
        assertEquals(report, engine.validateChecksum(input, options))
        assertEquals(report, engine.validate(input, options), "the same call twice gave two answers")

        val formatOnly = engine.validateFormat(input, options)
        assertEquals(report.format, formatOnly.format)
    }
}
