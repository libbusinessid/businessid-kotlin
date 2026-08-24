// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.generator

import libbusinessid.ir.v1.Rules

/**
 * A minimal ruleset that passes all twenty-five checks, and the machinery to
 * break it one way at a time.
 *
 * It has the shape of the reference ruleset the shared corpus builds its hostile
 * fixtures from — a synthetic `demo` kind of four ASCII digits closed by a Luhn
 * check digit — so a mutation here reaches the same checks the corpus reaches.
 *
 * The accepted baseline matters as much as the refusals: a loader that refused
 * everything would pass every negative test.
 */
object Fixtures {
    fun canonicalizationProgram(id: Int): Rules.Program = Rules.Program.newBuilder()
        .setId(id)
        .setKind(Rules.ProgramKind.PROGRAM_KIND_CANONICALIZATION)
        .addNodes(canonicalizationStep(Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_TRIM_WHITESPACE))
        .addNodes(canonicalizationStep(Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_UPPERCASE_ASCII))
        .addNodes(
            Rules.Node.newBuilder()
                .setOutputType(Rules.ValueType.VALUE_TYPE_CANONICALIZATION_STEP)
                .addInputNodes(0)
                .addInputNodes(1)
                .setCanonicalizationOperation(
                    Rules.CanonicalizationOperation.newBuilder()
                        .setKind(Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_SEQUENCE),
                ),
        )
        .setRootNode(2)
        .build()

    private fun canonicalizationStep(kind: Rules.CanonicalizationOpKind): Rules.Node = Rules.Node.newBuilder()
        .setOutputType(Rules.ValueType.VALUE_TYPE_CANONICALIZATION_STEP)
        .setCanonicalizationOperation(Rules.CanonicalizationOperation.newBuilder().setKind(kind))
        .build()

    fun formatProgram(id: Int): Rules.Program = Rules.Program.newBuilder()
        .setId(id)
        .setKind(Rules.ProgramKind.PROGRAM_KIND_FORMAT)
        .addNodes(subject())
        .addNodes(
            Rules.Node.newBuilder()
                .setOutputType(Rules.ValueType.VALUE_TYPE_BOOLEAN)
                .addInputNodes(0)
                .setPredicateOperation(
                    Rules.PredicateOperation.newBuilder()
                        .setKind(Rules.PredicateOpKind.PREDICATE_OP_KIND_LENGTH_EQ)
                        .setLength(4),
                ),
        )
        .addNodes(require(1, Rules.ReasonCode.REASON_CODE_INVALID_LENGTH, "demo.length"))
        .addNodes(
            Rules.Node.newBuilder()
                .setOutputType(Rules.ValueType.VALUE_TYPE_BOOLEAN)
                .addInputNodes(0)
                .setPredicateOperation(
                    Rules.PredicateOperation.newBuilder()
                        .setKind(Rules.PredicateOpKind.PREDICATE_OP_KIND_ASCII_DIGITS),
                ),
        )
        .addNodes(require(3, Rules.ReasonCode.REASON_CODE_INVALID_CHARACTERS, "demo.characters"))
        .addNodes(
            Rules.Node.newBuilder()
                .setOutputType(Rules.ValueType.VALUE_TYPE_ASSERTION)
                .addInputNodes(2)
                .addInputNodes(4)
                .setAssertionOperation(
                    Rules.AssertionOperation.newBuilder()
                        .setKind(Rules.AssertionOpKind.ASSERTION_OP_KIND_SEQUENCE),
                ),
        )
        .setRootNode(5)
        .build()

    private fun require(operand: Int, reason: Rules.ReasonCode, key: String): Rules.Node = Rules.Node.newBuilder()
        .setOutputType(Rules.ValueType.VALUE_TYPE_ASSERTION)
        .addInputNodes(operand)
        .setAssertionOperation(
            Rules.AssertionOperation.newBuilder()
                .setKind(Rules.AssertionOpKind.ASSERTION_OP_KIND_REQUIRE)
                .setReasonCode(reason)
                .setMessageKey(key),
        )
        .build()

    fun subject(): Rules.Node = Rules.Node.newBuilder()
        .setOutputType(Rules.ValueType.VALUE_TYPE_STRING)
        .setStringOperation(
            Rules.StringOperation.newBuilder().setKind(Rules.StringOpKind.STRING_OP_KIND_SUBJECT),
        )
        .build()

    fun checksumProgram(id: Int): Rules.Program = Rules.Program.newBuilder()
        .setId(id)
        .setKind(Rules.ProgramKind.PROGRAM_KIND_CHECKSUM)
        .addNodes(subject())
        .addNodes(
            Rules.Node.newBuilder()
                .setOutputType(Rules.ValueType.VALUE_TYPE_CHECKSUM_OUTCOME)
                .addInputNodes(0)
                .setChecksumOperation(
                    Rules.ChecksumOperation.newBuilder().setKind(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_LUHN),
                ),
        )
        .setRootNode(1)
        .build()

    private fun source(): Rules.Source = Rules.Source.newBuilder()
        .setId("libbusinessid-reference-bundle")
        .setUrl("https://github.com/libbusinessid/spec")
        .setAuthority("LibBusinessID")
        .setTitle("Minimal reference bundle")
        .setAccessedAt("2026-08-18")
        .setJurisdiction("GLOBAL")
        .setLanguage("en")
        .setNotes("Synthetic demonstration rule: four ASCII digits closed by a Luhn check digit.")
        .setLicenseOrTerms("Apache-2.0")
        .setTier(Rules.SourceTier.SOURCE_TIER_PRIMARY)
        .build()

    /** A ruleset that passes every check. Mutate a copy of this, never this. */
    fun valid(): Rules.RuleBundle.Builder = Rules.RuleBundle.newBuilder()
        .setFormatVersion(1)
        .setRulesVersion("2026.08.0")
        .addAllRequiredFeatureIds(listOf(1, 2, 3, 5, 20, 21, 30, 31, 40, 41))
        .setSourceDigest(com.google.protobuf.ByteString.copyFrom(ByteArray(32)))
        .addIdentifiers(
            Rules.IdentifierDefinition.newBuilder()
                .setId(1)
                .setKind("demo")
                .setCanonicalizationProgram(1)
                .setFormatProgram(2)
                .setChecksumProgram(3)
                .setDefaultProfile("compatible")
                .addSources(source()),
        )
        .addPrograms(canonicalizationProgram(1))
        .addPrograms(formatProgram(2))
        .addPrograms(checksumProgram(3))
        .addDispatchers(
            Rules.IdentifierDispatcher.newBuilder()
                .setKind("demo")
                .setPreCanonicalizationProgram(1)
                .addTargets(Rules.DispatchTarget.newBuilder().setIdentifierDefinitionId(1)),
        )

    fun bytes(builder: Rules.RuleBundle.Builder): ByteArray = builder.build().toByteArray()
}
