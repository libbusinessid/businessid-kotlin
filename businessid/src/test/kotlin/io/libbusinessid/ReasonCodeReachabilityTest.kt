// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Every reason code the engine can produce, produced.
 *
 * The four it cannot are named here with the reason, so that a code becoming
 * reachable without a test noticing is not possible: the sets are compared, not
 * just contained.
 */
class ReasonCodeReachabilityTest {
    private val engine = BusinessIdEngine.default()

    private fun input(kind: String, value: String, country: String? = null) =
        IdentifierInput(IdentifierKind(kind), value, country)

    /**
     * One call per reachable code. Every value is synthetic and comes from the
     * shared corpus.
     */
    private val reaching: Map<ReasonCode, () -> StepResult> = mapOf(
        ReasonCode.OK to { engine.validate(input("siret", "01234567400001")).format },
        ReasonCode.EMPTY to { engine.validate(input("lei", "")).format },
        ReasonCode.INVALID_LENGTH to { engine.validate(input("siret", "0123456789012")).format },
        ReasonCode.INVALID_CHARACTERS to { engine.validate(input("siret", "0123456789012A")).format },
        ReasonCode.INVALID_FORMAT to { engine.validate(input("eori", "12345678")).format },
        ReasonCode.INVALID_CHECKSUM to { engine.validate(input("siret", "01234567400000")).checksum },
        ReasonCode.MISSING_COUNTRY_CODE to { engine.validate(input("vat", "0123456749")).format },
        ReasonCode.COUNTRY_MISMATCH to { engine.validate(input("vat", "BE0123456749", "FR")).format },
        ReasonCode.UNSUPPORTED_KIND to { engine.validate(input("nope", "X")).format },
        ReasonCode.UNSUPPORTED_COUNTRY to { engine.validate(input("siren", "012345674", "DE")).format },
        ReasonCode.UNSUPPORTED_CHECKSUM to { engine.validate(input("vat", "123456789", "DE")).checksum },
        ReasonCode.CHECKSUM_NOT_PUBLISHED to { engine.validate(input("duns", "012345678")).checksum },
        ReasonCode.NOT_REQUESTED to { engine.validateFormat(input("siret", "01234567400001")).checksum },
        ReasonCode.NOT_RUN_FORMAT_INVALID to { engine.validate(input("siret", "0123456789012")).checksum },
        ReasonCode.NOT_RUN_FORMAT_UNSUPPORTED to { engine.validate(input("nope", "X")).checksum },
        ReasonCode.INPUT_TOO_LONG to { engine.validate(input("siren", "1".repeat(1025))).format },
        ReasonCode.INVALID_ENCODING to { engine.validate(input("siren", "\uD83D")).format },
    )

    /**
     * The codes no local validation can produce.
     *
     * `unsupported_format` has no operation that yields it: a format program
     * either holds or names the assertion that failed. `registry_not_configured`
     * is reserved for a remote lookup this version defers entirely.
     * `incompatible_ruleset` and `invalid_ruleset` belong to the generator, which
     * refuses a ruleset at build time; the published library loads none.
     */
    private val unreachableFromTheEngine = setOf(
        ReasonCode.UNSUPPORTED_FORMAT,
        ReasonCode.REGISTRY_NOT_CONFIGURED,
        ReasonCode.INCOMPATIBLE_RULESET,
        ReasonCode.INVALID_RULESET,
    )

    @Test
    fun `the two sets together are the whole registry`() {
        assertEquals(
            ReasonCode.entries.toSet(),
            reaching.keys + unreachableFromTheEngine,
            "a reason code is neither reached nor explained",
        )
        assertEquals(emptySet<ReasonCode>(), reaching.keys intersect unreachableFromTheEngine)
    }

    @Test
    fun `every reachable reason code is actually produced`() {
        for ((expected, call) in reaching) {
            assertEquals(expected, call().reasonCode, "reaching $expected")
        }
    }

    @Test
    fun `every produced pair of status and reason is one the contract allows`() {
        for ((_, call) in reaching) {
            val step = call()
            val allowed = when (step.status) {
                StepStatus.VALID -> setOf(ReasonCode.OK)
                StepStatus.NOT_RUN -> setOf(
                    ReasonCode.NOT_REQUESTED,
                    ReasonCode.NOT_RUN_FORMAT_INVALID,
                    ReasonCode.NOT_RUN_FORMAT_UNSUPPORTED,
                )
                StepStatus.INVALID -> setOf(
                    ReasonCode.EMPTY,
                    ReasonCode.INVALID_LENGTH,
                    ReasonCode.INVALID_CHARACTERS,
                    ReasonCode.INVALID_FORMAT,
                    ReasonCode.INVALID_CHECKSUM,
                    ReasonCode.COUNTRY_MISMATCH,
                )
                StepStatus.UNSUPPORTED -> ReasonCode.entries.toSet() - setOf(ReasonCode.OK)
            }
            assert(step.reasonCode in allowed) { "${step.status} carries ${step.reasonCode}" }
        }
    }

    @Test
    fun `a wire name round trips for every code, status, level and profile`() {
        for (code in ReasonCode.entries) assertEquals(code, ReasonCode.ofWireName(code.wireName))
        assertEquals(null, ReasonCode.ofWireName("no_such_reason"))
        for (profile in ValidationProfile.entries) {
            assertEquals(profile, ValidationProfile.ofWireName(profile.wireName))
        }
        assertEquals(null, ValidationProfile.ofWireName("lenient"))
        assertEquals(setOf("valid", "invalid", "unsupported", "not_run"), StepStatus.entries.map { it.wireName }.toSet())
        assertEquals(setOf("format", "checksum", "registry"), ValidationLevel.entries.map { it.wireName }.toSet())
    }
}
