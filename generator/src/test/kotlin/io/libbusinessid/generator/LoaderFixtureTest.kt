// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.generator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertFailsWith

/**
 * Every hostile ruleset of the shared corpus, refused for the reason its case is
 * named for.
 *
 * The error kind alone is not enough. `ir.md` section 10 gives the checks an
 * order, and a fixture refused by an earlier check than the one it targets
 * proves nothing about that check: it would pass while the check it exists to
 * exercise was never reached. So the expected check number is asserted too, and
 * `checkOf` below records which check each case is meant to reach.
 */
class LoaderFixtureTest {
    /**
     * The check each fixture is named for, read from its id and description.
     *
     * A number here that the loader disagrees with is a finding, not a licence
     * to change the number: either the loader refuses too early, or the fixture
     * is invalid for a second reason.
     */
    private val expectedCheck = mapOf(
        "loader-truncated-001" to 2,
        "loader-empty-002" to 3,
        "loader-unknown-field-root-003" to 5,
        "loader-unsupported-format-version-004" to 3,
        "loader-unknown-feature-005" to 4,
        "loader-undeclared-feature-006" to 25,
        "loader-short-digest-007" to 7,
        "loader-empty-rules-version-008" to 6,
        "loader-missing-operation-009" to 10,
        "loader-node-forward-reference-010" to 11,
        "loader-node-out-of-range-011" to 15,
        "loader-type-mismatch-012" to 10,
        "loader-unspecified-enum-013" to 8,
        "loader-call-cycle-014" to 24,
        "loader-unknown-call-target-015" to 24,
        "loader-orphan-definition-016" to 23,
        "loader-duplicate-prefix-017" to 21,
        "loader-forbidden-reason-code-018" to 12,
        "loader-stray-parameter-019" to 12,
        "loader-unbounded-digits-to-integer-020" to 13,
        "loader-modulus-out-of-range-021" to 13,
        "loader-stray-when-branch-022" to 16,
        "loader-global-target-with-prefix-023" to 22,
        "loader-left-pad-length-026" to 13,
        "loader-empty-message-key-027" to 12,
        "loader-predicate-constant-028" to 13,
        "loader-rules-version-shape-029" to 6,
        "loader-alphabet-repeated-030" to 13,
        "loader-alphabet-empty-031" to 13,
        "loader-alphabet-too-many-032" to 13,
        "loader-alphabet-missing-033" to 12,
        "loader-alphabet-unread-034" to 12,
        "loader-source-tier-unknown-035" to 17,
        "loader-program-expansion-036" to 14,
        "loader-subject-node-circular-037" to 15,
    )

    @TestFactory
    fun `every hostile ruleset is refused at the check it targets`(): List<DynamicTest> =
        SpecFiles.loaderCases.map { case ->
            DynamicTest.dynamicTest("${case.id} — ${case.description}") {
                val failure = assertFailsWith<RulesetException> {
                    Loader.load(case.rulesPayload.toByteArray())
                }
                assertEquals(
                    case.expectedEngineError,
                    failure.errorKind.wireName,
                    "${case.id} was refused as ${failure.errorKind.wireName} at check ${failure.check}: " +
                        failure.message,
                )
                val expected = expectedCheck[case.id]
                assertNotNull(expected, "${case.id} has no expected check number recorded")
                assertEquals(
                    expected,
                    failure.check,
                    "${case.id} was refused at check ${failure.check} (${failure.message}) rather than at " +
                        "check $expected, the one its name targets",
                )
            }
        }

    @Test
    fun `the corpus carries every loader case this test knows about`() {
        assertEquals(
            expectedCheck.keys.sorted(),
            SpecFiles.loaderCases.map { it.id }.sorted(),
            "the recorded fixtures and the corpus disagree",
        )
    }

    @Test
    fun `a refusal always names a check between one and twenty-five`() {
        for (case in SpecFiles.loaderCases) {
            val failure = assertFailsWith<RulesetException> { Loader.load(case.rulesPayload.toByteArray()) }
            assert(failure.check in 1..25) { "${case.id} names check ${failure.check}" }
        }
    }
}
