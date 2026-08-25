// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

package org.entid

import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The properties `engine.md` section 12.3 requires, stated over generated input
 * rather than over the examples a person would have thought of.
 *
 * The seed is fixed so a failure is reproducible; Kotest still shrinks a
 * counter-example down to its smallest form before reporting it.
 *
 * Every body goes through [property], which returns `Unit`. Writing
 * `= runBlocking { checkAll(...) }` instead returns a `PropertyContext`, and
 * JUnit drops a test method that returns a value without running it — which it
 * did here, in silence, until `TestHygieneTest` was written to catch it.
 */
@OptIn(io.kotest.common.ExperimentalKotest::class)
class EnginePropertyTest {
    private val engine = EntIdEngine.default()

    private val config = PropTestConfig(seed = 20260824)

    private fun property(block: suspend () -> Unit) {
        runBlocking { block() }
    }

    private val kinds = Arb.of(
        listOf("siret", "siren", "vat", "lei", "duns", "eori", "euid", "cnpj", "uscc", "nope", "", "  VAT  "),
    )

    private val countries = Arb.of(listOf(null, "", "FR", "BE", "fr", "GR", "EL", "UK", "JP", "belgium", "F"))

    /** Strings a caller might realistically hand over, plus a few nobody would. */
    private val values = arbitrary { rs ->
        val alphabet = "0123456789ABCDEFabcdef .-/\t 　é😀"
        val length = rs.random.nextInt(0, 24)
        buildString { repeat(length) { append(alphabet[rs.random.nextInt(alphabet.length)]) } }
    }

    private val inputs = Arb.bind(kinds, values, countries) { kind, value, country ->
        IdentifierInput(IdentifierKind(kind), value, country)
    }

    @Test
    fun `no user string ever throws`() = property {
        checkAll(config, Arb.string(0..64), kinds, countries) { value, kind, country ->
            val input = IdentifierInput(IdentifierKind(kind), value, country)
            engine.canonicalize(input)
            engine.validate(input)
            engine.validateFormat(input)
            engine.validateChecksum(input)
        }
    }

    @Test
    fun `canonicalisation is idempotent`() = property {
        checkAll(config, inputs) { input ->
            val once = engine.canonicalize(input)
            if (once.status == StepStatus.VALID) {
                val twice = engine.canonicalize(
                    IdentifierInput(IdentifierKind(once.kind.value), once.canonicalValue, once.countryCode),
                )
                assertEquals(once.canonicalValue, twice.canonicalValue, "for ${input.value}")
            }
        }
    }

    @Test
    fun `adding an accepted separator does not change the canonical value`() = property {
        // Separators every definition of this ruleset removes, taken from the
        // frozen whitespace table.
        val separators = listOf(" ", "\t", " ", "　")
        checkAll(config, inputs) { input ->
            val base = engine.canonicalize(input)
            for (separator in separators) {
                val spaced = input.value.toCharArray().joinToString(separator)
                val padded = engine.canonicalize(input.copy(value = separator + spaced + separator))
                if (base.status == StepStatus.VALID && padded.status == StepStatus.VALID) {
                    assertEquals(base.canonicalValue, padded.canonicalValue, "for ${input.value} with $separator")
                }
            }
        }
    }

    @Test
    fun `an unsupported checksum never becomes invalid when the input is decorated`() = property {
        checkAll(config, inputs) { input ->
            val base = engine.validate(input)
            if (base.checksum.status == StepStatus.UNSUPPORTED) {
                val spaced = engine.validate(input.copy(value = " ${input.value} "))
                assertNotEquals(
                    StepStatus.INVALID,
                    spaced.checksum.status,
                    "decorating ${input.value} turned an unsupported checksum into an invalid one",
                )
            }
        }
    }

    @Test
    fun `mutating the check digit invalidates a value the algorithm closes`() {
        // Only over values whose checksum the ruleset actually validates: a digit
        // change on an unpublished algorithm proves nothing. Every value is
        // synthetic and drawn from the shared corpus.
        val valid = listOf(
            IdentifierInput(IdentifierKind.SIRET, "01234567400001"),
            IdentifierInput(IdentifierKind.SIREN, "012345674"),
            IdentifierInput(IdentifierKind.LEI, "00000000000000000098"),
            IdentifierInput(IdentifierKind.VAT, "BE0123456749"),
        )
        for (input in valid) {
            val original = engine.validate(input)
            assertEquals(
                StepStatus.VALID,
                original.checksum.status,
                "${input.value} was chosen because its checksum is validated",
            )
            var proved = 0
            for (digit in '0'..'9') {
                if (digit == input.value.last()) continue
                val mutated = engine.validate(input.copy(value = input.value.dropLast(1) + digit))
                if (mutated.format.status != StepStatus.VALID) continue
                assertEquals(
                    StepStatus.INVALID,
                    mutated.checksum.status,
                    "${input.value} with a $digit check digit stayed acceptable",
                )
                proved++
            }
            assertTrue(proved > 0, "no mutation of ${input.value} reached the checksum")
        }
    }

    @Test
    fun `validate and validateChecksum agree on every generated input`() = property {
        checkAll(config, inputs) { input ->
            assertEquals(engine.validate(input), engine.validateChecksum(input))
        }
    }

    @Test
    fun `validateFormat differs from validate only in the checksum step`() = property {
        checkAll(config, inputs) { input ->
            val full = engine.validate(input)
            val formatOnly = engine.validateFormat(input)
            assertEquals(full.format, formatOnly.format)
            assertEquals(full.canonicalValue, formatOnly.canonicalValue)
            assertEquals(full.kind, formatOnly.kind)
            assertEquals(full.countryCode, formatOnly.countryCode)
            if (full.format.status == StepStatus.VALID) {
                assertEquals(StepStatus.NOT_RUN, formatOnly.checksum.status)
                assertEquals(ReasonCode.NOT_REQUESTED, formatOnly.checksum.reasonCode)
            } else {
                assertEquals(full.checksum, formatOnly.checksum)
            }
        }
    }

    @Test
    fun `the raw input is never modified in place`() = property {
        checkAll(config, inputs) { input ->
            val before = input.value
            engine.validate(input)
            engine.canonicalize(input)
            assertEquals(before, input.value)
        }
    }

    @Test
    fun `a checksum is never run after a format that did not hold`() = property {
        checkAll(config, inputs) { input ->
            val report = engine.validate(input)
            if (report.format.status != StepStatus.VALID) {
                assertEquals(StepStatus.NOT_RUN, report.checksum.status, "for ${input.value}")
            }
        }
    }

    @Test
    fun `every call is deterministic`() = property {
        checkAll(config, inputs) { input ->
            assertEquals(engine.validate(input), engine.validate(input))
            assertEquals(engine.canonicalize(input), engine.canonicalize(input))
        }
    }
}
