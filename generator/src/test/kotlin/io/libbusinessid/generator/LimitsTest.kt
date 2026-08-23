// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.generator

import com.google.protobuf.ByteString
import libbusinessid.ir.v1.Rules
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

/**
 * The load checks the shared corpus does not reach.
 *
 * The corpus fixes one hostile ruleset per check it exercises; the limits and
 * table rules below have no fixture, and would otherwise be enforced by code no
 * test ever runs.
 */
class LimitsTest {
    private fun refusal(mutate: Rules.RuleBundle.Builder.() -> Unit): RulesetException =
        assertFailsWith<RulesetException> {
            Loader.load(Fixtures.bytes(Fixtures.valid().apply(mutate)))
        }

    private fun assertRefused(check: Int, kind: RulesetErrorKind, mutate: Rules.RuleBundle.Builder.() -> Unit) {
        val failure = refusal(mutate)
        assertEquals(check, failure.check, "refused at check ${failure.check}: ${failure.message}")
        assertEquals(kind, failure.errorKind, failure.message)
    }

    private fun invalid(check: Int, mutate: Rules.RuleBundle.Builder.() -> Unit) =
        assertRefused(check, RulesetErrorKind.INVALID_RULESET, mutate)

    @Test
    fun `the baseline ruleset is accepted`() {
        // Without this the whole file would pass on a loader that refused
        // everything.
        val loaded = Loader.load(Fixtures.bytes(Fixtures.valid()))
        assertEquals("2026.08.0", loaded.rulesVersion)
        assertEquals(1, loaded.targets.size)
    }

    @Test
    fun `a ruleset above sixteen mebibytes is refused before it is decoded`() {
        val huge = ByteArray(Limits.MAX_BUNDLE_BYTES + 1)
        val failure = assertFailsWith<RulesetException> { Loader.load(huge) }
        assertEquals(1, failure.check)
        assertEquals(RulesetErrorKind.INVALID_RULESET, failure.errorKind)
    }

    @Test
    fun `a program above four thousand and ninety-six nodes is refused`() {
        invalid(9) {
            val program = Rules.Program.newBuilder()
                .setId(4)
                .setKind(Rules.ProgramKind.PROGRAM_KIND_CANONICALIZATION)
            repeat(Limits.MAX_NODES_PER_PROGRAM + 1) {
                program.addNodes(
                    Rules.Node.newBuilder()
                        .setOutputType(Rules.ValueType.VALUE_TYPE_CANONICALIZATION_STEP)
                        .setCanonicalizationOperation(
                            Rules.CanonicalizationOperation.newBuilder()
                                .setKind(Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_SEQUENCE),
                        ),
                )
            }
            program.rootNode = Limits.MAX_NODES_PER_PROGRAM
            addPrograms(program)
        }
    }

    @Test
    fun `a call chain deeper than thirty-two is refused`() {
        invalid(24) {
            // Chain program 100 -> 101 -> ... each calling the next.
            val depth = Limits.MAX_CALL_DEPTH + 1
            for (i in 0 until depth) {
                val program = Rules.Program.newBuilder()
                    .setId(100 + i)
                    .setKind(Rules.ProgramKind.PROGRAM_KIND_CHECKSUM)
                    .addNodes(Fixtures.subject())
                if (i < depth - 1) {
                    program.addNodes(
                        Rules.Node.newBuilder()
                            .setOutputType(Rules.ValueType.VALUE_TYPE_CHECKSUM_OUTCOME)
                            .addInputNodes(0)
                            .setCallOperation(
                                Rules.CallOperation.newBuilder()
                                    .setKind(Rules.CallOpKind.CALL_OP_KIND_CHECKSUM)
                                    .setProgramId(100 + i + 1),
                            ),
                    )
                } else {
                    program.addNodes(
                        Rules.Node.newBuilder()
                            .setOutputType(Rules.ValueType.VALUE_TYPE_CHECKSUM_OUTCOME)
                            .addInputNodes(0)
                            .setChecksumOperation(
                                Rules.ChecksumOperation.newBuilder()
                                    .setKind(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_LUHN),
                            ),
                    )
                }
                program.rootNode = 1
                addPrograms(program)
            }
            addAllRequiredFeatureIds(listOf(11))
            clearRequiredFeatureIds()
            addAllRequiredFeatureIds(listOf(1, 2, 3, 5, 11, 20, 21, 30, 31, 40, 41))
        }
    }

    @Test
    fun `a rules version above sixty-four bytes is refused`() {
        invalid(6) { rulesVersion = "a".repeat(Limits.MAX_RULES_VERSION_BYTES + 1) }
    }

    @Test
    fun `a constant above four thousand and ninety-six bytes is refused`() {
        invalid(12) {
            val program = Rules.Program.newBuilder(getPrograms(1))
            program.setNodes(
                1,
                Rules.Node.newBuilder()
                    .setOutputType(Rules.ValueType.VALUE_TYPE_BOOLEAN)
                    .addInputNodes(0)
                    .setPredicateOperation(
                        Rules.PredicateOperation.newBuilder()
                            .setKind(Rules.PredicateOpKind.PREDICATE_OP_KIND_STARTS_WITH)
                            .setText("x".repeat(Limits.MAX_CONSTANT_BYTES + 1)),
                    ),
            )
            setPrograms(1, program)
        }
    }

    @Test
    fun `a concat above two hundred and fifty-six operands is refused`() {
        invalid(11) {
            val program = Rules.Program.newBuilder(getPrograms(2))
            val concat = Rules.Node.newBuilder()
                .setOutputType(Rules.ValueType.VALUE_TYPE_STRING)
                .setStringOperation(
                    Rules.StringOperation.newBuilder().setKind(Rules.StringOpKind.STRING_OP_KIND_CONCAT),
                )
            repeat(Limits.MAX_CONCAT_OPERANDS + 1) { concat.addInputNodes(0) }
            program.addNodes(concat)
            setPrograms(2, program)
            clearRequiredFeatureIds()
            addAllRequiredFeatureIds(listOf(1, 2, 3, 5, 10, 20, 21, 30, 31, 40, 41))
        }
    }

    @Test
    fun `more than two hundred and fifty-six weights are refused`() {
        invalid(13) {
            val program = Rules.Program.newBuilder(getPrograms(2))
            val weighted = Rules.IntegerOperation.newBuilder()
                .setKind(Rules.IntegerOpKind.INTEGER_OP_KIND_WEIGHTED_SUM)
                .setAlignment(Rules.WeightAlignment.WEIGHT_ALIGNMENT_LEFT)
                .setMapping(Rules.CharMapping.CHAR_MAPPING_DIGIT_VALUE)
            repeat(Limits.MAX_WEIGHTS + 1) { weighted.addWeights(1) }
            program.addNodes(
                Rules.Node.newBuilder()
                    .setOutputType(Rules.ValueType.VALUE_TYPE_INTEGER)
                    .addInputNodes(0)
                    .setIntegerOperation(weighted),
            )
            setPrograms(2, program)
            clearRequiredFeatureIds()
            addAllRequiredFeatureIds(listOf(1, 2, 3, 5, 20, 21, 30, 31, 33, 40, 41))
        }
    }

    @Test
    fun `more than one hundred and twenty-eight captures are refused`() {
        invalid(15) {
            val program = Rules.Program.newBuilder(getPrograms(1))
            repeat(Limits.MAX_CAPTURES_PER_FORMAT + 1) { i ->
                program.addCaptures(Rules.Capture.newBuilder().setName("c$i").setNode(0))
            }
            setPrograms(1, program)
            clearRequiredFeatureIds()
            addAllRequiredFeatureIds(listOf(1, 2, 3, 5, 11, 20, 21, 30, 31, 40, 41))
        }
    }

    @Test
    fun `a source digest of the wrong length is refused`() {
        invalid(7) { sourceDigest = ByteString.copyFrom(ByteArray(31)) }
    }

    @Test
    fun `required feature ids that are not strictly ascending are refused`() {
        invalid(4) {
            clearRequiredFeatureIds()
            addAllRequiredFeatureIds(listOf(1, 3, 2, 5, 20, 21, 30, 31, 40, 41))
        }
    }

    @Test
    fun `an unknown capability id closes the ruleset as a version gap`() {
        assertRefused(4, RulesetErrorKind.INCOMPATIBLE_RULESET) {
            addRequiredFeatureIds(9999)
        }
    }

    @Test
    fun `a kind token claimed by two dispatchers is refused`() {
        invalid(19) {
            addDispatchers(
                Rules.IdentifierDispatcher.newBuilder()
                    .setKind("demo")
                    .setPreCanonicalizationProgram(1)
                    .addTargets(Rules.DispatchTarget.newBuilder().setIdentifierDefinitionId(1)),
            )
        }
    }

    @Test
    fun `an alias colliding with another dispatcher's kind is refused`() {
        invalid(19) {
            val second = Rules.IdentifierDispatcher.newBuilder()
                .setKind("other")
                .addKindAliases("demo")
                .setPreCanonicalizationProgram(1)
                .addTargets(Rules.DispatchTarget.newBuilder().setIdentifierDefinitionId(1))
            addDispatchers(second)
        }
    }

    @Test
    fun `a country alias that maps to itself is refused`() {
        invalid(20) {
            countryTargets()
            val d = Rules.IdentifierDispatcher.newBuilder(getDispatchers(0))
                .addCountryAliases(Rules.CountryAlias.newBuilder().setAlias("FR").setCountryCode("FR"))
            setDispatchers(0, d)
        }
    }

    @Test
    fun `a country alias shadowing a declared target is refused`() {
        invalid(20) {
            countryTargets()
            val d = Rules.IdentifierDispatcher.newBuilder(getDispatchers(0))
                .addCountryAliases(Rules.CountryAlias.newBuilder().setAlias("FR").setCountryCode("BE"))
            setDispatchers(0, d)
        }
    }

    @Test
    fun `two GLOBAL targets in one dispatcher are refused`() {
        invalid(21) {
            val d = Rules.IdentifierDispatcher.newBuilder(getDispatchers(0))
                .addTargets(Rules.DispatchTarget.newBuilder().setIdentifierDefinitionId(1))
            setDispatchers(0, d)
        }
    }

    @Test
    fun `a country alias beside a GLOBAL target is refused`() {
        invalid(22) {
            val d = Rules.IdentifierDispatcher.newBuilder(getDispatchers(0))
                .addCountryAliases(Rules.CountryAlias.newBuilder().setAlias("UK").setCountryCode("GB"))
            setDispatchers(0, d)
        }
    }

    @Test
    fun `two implicit targets make routing ambiguous and are refused`() {
        invalid(21) {
            countryTargets(allowUnprefixed = true)
        }
    }

    @Test
    fun `a definition whose declared kind differs from its dispatcher is refused`() {
        invalid(23) {
            val definition = Rules.IdentifierDefinition.newBuilder(getIdentifiers(0)).setKind("other")
            setIdentifiers(0, definition)
            val d = Rules.IdentifierDispatcher.newBuilder(getDispatchers(0)).setKind("other")
            setDispatchers(0, d)
            // The dispatcher and the definition now agree on `other`, so restore
            // the disagreement on the definition alone.
            setIdentifiers(0, Rules.IdentifierDefinition.newBuilder(getIdentifiers(0)).setKind("demo").build())
        }
    }

    @Test
    fun `a definition declaring neither a checksum program nor an absence reason is refused`() {
        invalid(18) {
            setIdentifiers(0, Rules.IdentifierDefinition.newBuilder(getIdentifiers(0)).clearChecksumProgram())
        }
    }

    @Test
    fun `a definition declaring both a checksum program and an absence reason is refused`() {
        invalid(18) {
            setIdentifiers(
                0,
                Rules.IdentifierDefinition.newBuilder(getIdentifiers(0))
                    .setAbsentChecksumReason(Rules.ReasonCode.REASON_CODE_CHECKSUM_NOT_PUBLISHED),
            )
        }
    }

    @Test
    fun `a profile no version of this API accepts is refused`() {
        invalid(17) {
            setIdentifiers(0, Rules.IdentifierDefinition.newBuilder(getIdentifiers(0)).setDefaultProfile("lenient"))
        }
    }

    @Test
    fun `a malformed kind token is refused`() {
        invalid(17) {
            setIdentifiers(0, Rules.IdentifierDefinition.newBuilder(getIdentifiers(0)).setKind("Demo"))
        }
    }

    @Test
    fun `the literal GLOBAL as a country code is refused`() {
        invalid(17) {
            setIdentifiers(0, Rules.IdentifierDefinition.newBuilder(getIdentifiers(0)).setCountryCode("GLOBAL"))
        }
    }

    @Test
    fun `an unknown call target is refused`() {
        invalid(24) {
            val program = Rules.Program.newBuilder(getPrograms(2))
            program.addNodes(
                Rules.Node.newBuilder()
                    .setOutputType(Rules.ValueType.VALUE_TYPE_CHECKSUM_OUTCOME)
                    .addInputNodes(0)
                    .setCallOperation(
                        Rules.CallOperation.newBuilder()
                            .setKind(Rules.CallOpKind.CALL_OP_KIND_CHECKSUM)
                            .setProgramId(999),
                    ),
            )
            setPrograms(2, program)
        }
    }

    @Test
    fun `prepend_country_if_missing in a GLOBAL canonicalizer is refused`() {
        invalid(16) {
            val program = Rules.Program.newBuilder(getPrograms(0))
            program.setNodes(
                0,
                Rules.Node.newBuilder()
                    .setOutputType(Rules.ValueType.VALUE_TYPE_CANONICALIZATION_STEP)
                    .setCanonicalizationOperation(
                        Rules.CanonicalizationOperation.newBuilder()
                            .setKind(
                                Rules.CanonicalizationOpKind
                                    .CANONICALIZATION_OP_KIND_PREPEND_COUNTRY_IF_MISSING,
                            ),
                    ),
            )
            setPrograms(0, program)
            // Give the definition its own canonicalizer so the dispatcher's
            // pre-canonicalisation restriction is not what refuses it.
            val canonicalizer = Rules.Program.newBuilder(program.build()).setId(4)
            addPrograms(canonicalizer)
            setPrograms(0, Fixtures.canonicalizationProgram(1))
            setIdentifiers(
                0,
                Rules.IdentifierDefinition.newBuilder(getIdentifiers(0)).setCanonicalizationProgram(4),
            )
        }
    }

    @Test
    fun `an operation a pre-canonicalization program may not hold is refused`() {
        invalid(16) {
            val program = Rules.Program.newBuilder(getPrograms(0))
            program.setNodes(
                0,
                Rules.Node.newBuilder()
                    .setOutputType(Rules.ValueType.VALUE_TYPE_CANONICALIZATION_STEP)
                    .setCanonicalizationOperation(
                        Rules.CanonicalizationOperation.newBuilder()
                            .setKind(Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_APPEND)
                            .setText("X"),
                    ),
            )
            setPrograms(0, program)
        }
    }

    /** Replaces the single GLOBAL target by two country targets. */
    private fun Rules.RuleBundle.Builder.countryTargets(allowUnprefixed: Boolean = false) {
        setIdentifiers(0, Rules.IdentifierDefinition.newBuilder(getIdentifiers(0)).setCountryCode("BE"))
        addIdentifiers(
            Rules.IdentifierDefinition.newBuilder(getIdentifiers(0)).setId(2).setCountryCode("FR"),
        )
        val d = Rules.IdentifierDispatcher.newBuilder(getDispatchers(0))
            .clearTargets()
            .addTargets(
                Rules.DispatchTarget.newBuilder()
                    .setCountryCode("BE")
                    .setIdentifierDefinitionId(1)
                    .setAllowUnprefixedWithoutCountry(allowUnprefixed),
            )
            .addTargets(
                Rules.DispatchTarget.newBuilder()
                    .setCountryCode("FR")
                    .setIdentifierDefinitionId(2)
                    .setAllowUnprefixedWithoutCountry(allowUnprefixed),
            )
        setDispatchers(0, d)
    }
}
