// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

package org.entid.generator

import libbusinessid.ir.v1.Rules
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertFailsWith

/**
 * One refusal per rule, over a ruleset rich enough to break in every way.
 *
 * The shared corpus fixes one hostile ruleset per check it exercises, which
 * leaves most of the individual rules inside those checks untested. Each entry
 * below breaks the kitchen sink in exactly one way and records which check must
 * answer.
 */
class LoaderRefusalTest {
    private class Mutation(val name: String, val check: Int, val apply: Rules.RuleBundle.Builder.() -> Unit)

    private fun mutations() = listOf(
        // -- 9: counts ---------------------------------------------------------
        Mutation("more identifiers than the limit allows", 9) {
            val template = getIdentifiers(2)
            repeat(Limits.MAX_IDENTIFIERS) { i ->
                addIdentifiers(Rules.IdentifierDefinition.newBuilder(template).setId(100 + i))
            }
        },

        // -- 6: the business version -------------------------------------------
        Mutation("a version holding a space", 6) { rulesVersion = "2026.08 0" },
        Mutation("a version holding a slash", 6) { rulesVersion = "2026/08/0" },

        // -- 10: operations and their declared output type ----------------------
        Mutation("an unknown string operation", 10) {
            onString(Rules.StringOpKind.STRING_OP_KIND_CONSTANT) { kindValue = 99 }
        },
        Mutation("an unknown integer operation", 10) {
            onInteger(Rules.IntegerOpKind.INTEGER_OP_KIND_MODULO) { kindValue = 99 }
        },
        Mutation("an unknown predicate", 10) {
            onPredicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_CONTAINS) { kindValue = 99 }
        },
        Mutation("an unknown canonicalisation step", 10) {
            onCanonicalization(Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_APPEND) { kindValue = 99 }
        },
        Mutation("an unknown assertion", 10) {
            onAssertion(Rules.AssertionOpKind.ASSERTION_OP_KIND_REQUIRE) { kindValue = 99 }
        },
        Mutation("an unknown checksum operation", 10) {
            onChecksum(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_LUHN) { kindValue = 99 }
        },
        Mutation("an unknown call kind", 10) {
            mutateNode({ it.hasCallOperation() }) {
                setCallOperation(callOperation.toBuilder().setKindValue(99))
            }
        },
        Mutation("a node declaring an output type the operation does not produce", 10) {
            mutateNode({ it.hasCallOperation() && it.callOperation.kind == Rules.CallOpKind.CALL_OP_KIND_CHECKSUM }) {
                outputType = Rules.ValueType.VALUE_TYPE_ASSERTION
            }
        },
        Mutation("a node declaring an unspecified output type", 10) {
            mutateNode({ it.hasStringOperation() }) { clearOutputType() }
        },

        // -- 11: operands -------------------------------------------------------
        Mutation("a string constructor without its operand", 11) {
            mutateNode({
                it.hasStringOperation() && it.stringOperation.kind == Rules.StringOpKind.STRING_OP_KIND_SLICE
            }) {
                clearInputNodes()
            }
        },
        Mutation("an integer constructor reading a string where it wants an integer", 11) {
            mutateNode({
                it.hasIntegerOperation() &&
                    it.integerOperation.kind == Rules.IntegerOpKind.INTEGER_OP_KIND_MODULO
            }) {
                clearInputNodes().addInputNodes(0)
            }
        },
        Mutation("a predicate with two operands where it takes one", 11) {
            mutateNode({
                it.hasPredicateOperation() &&
                    it.predicateOperation.kind == Rules.PredicateOpKind.PREDICATE_OP_KIND_CONTAINS
            }) {
                addInputNodes(0)
            }
        },
        Mutation("an equality with a single operand", 11) {
            mutateNode({
                it.hasPredicateOperation() &&
                    it.predicateOperation.kind == Rules.PredicateOpKind.PREDICATE_OP_KIND_EQUALS
            }) {
                clearInputNodes().addInputNodes(0)
            }
        },
        Mutation("a canonicalisation sequence holding something that is not a step", 11) {
            onProgram(2) { setNodes(rootNode, Rules.Node.newBuilder(getNodes(rootNode)).addInputNodes(0)) }
        },
        Mutation("a conditional step without any step to apply", 11) {
            mutateNode({
                it.hasCanonicalizationOperation() &&
                    it.canonicalizationOperation.kind == Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_WHEN
            }) {
                clearInputNodes().addInputNodes(1)
            }
        },
        Mutation("a conditional step whose first operand is not a predicate", 11) {
            mutateNode({
                it.hasCanonicalizationOperation() &&
                    it.canonicalizationOperation.kind == Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_WHEN
            }) {
                clearInputNodes().addInputNodes(0).addInputNodes(2)
            }
        },
        Mutation("a conditional step whose trailing operand is not a step", 11) {
            mutateNode({
                it.hasCanonicalizationOperation() &&
                    it.canonicalizationOperation.kind == Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_WHEN
            }) {
                clearInputNodes().addInputNodes(1).addInputNodes(0)
            }
        },
        Mutation("an assertion sequence holding a predicate", 11) {
            onProgram(3) {
                setNodes(rootNode, Rules.Node.newBuilder(getNodes(rootNode)).clearInputNodes().addInputNodes(1))
            }
        },
        Mutation("a checksum comparison with its operands the wrong way round", 11) {
            mutateNode({
                it.hasChecksumOperation() &&
                    it.checksumOperation.kind == Rules.ChecksumOpKind.CHECKSUM_OP_KIND_COMPARE_DIGIT
            }) {
                val operands = inputNodesList.toList()
                clearInputNodes().addInputNodes(operands[1]).addInputNodes(operands[0])
            }
        },
        Mutation("a choose with no branch at all", 11) {
            mutateNode({
                it.hasChecksumOperation() &&
                    it.checksumOperation.kind == Rules.ChecksumOpKind.CHECKSUM_OP_KIND_CHOOSE
            }) {
                clearInputNodes()
            }
        },
        Mutation("a call with no operand", 11) {
            mutateNode({ it.hasCallOperation() }) { clearInputNodes() }
        },

        // -- 12: parameters ------------------------------------------------------
        Mutation("a slice without its bounds", 12) {
            onString(Rules.StringOpKind.STRING_OP_KIND_SLICE) { clearEnd() }
        },
        Mutation("a slice_from without its start", 12) {
            onString(Rules.StringOpKind.STRING_OP_KIND_SLICE_FROM) { clearStart() }
        },
        Mutation("a slice_to without its end", 12) {
            onString(Rules.StringOpKind.STRING_OP_KIND_SLICE_TO) { clearEnd() }
        },
        Mutation("a constant longer than the limit", 12) {
            onString(Rules.StringOpKind.STRING_OP_KIND_CONSTANT) { text = "x".repeat(Limits.MAX_CONSTANT_BYTES + 1) }
        },
        Mutation("a value operation carrying a parameter it does not declare", 12) {
            onString(Rules.StringOpKind.STRING_OP_KIND_VALUE) { text = "x" }
        },
        Mutation("a delimiter that is empty", 12) {
            onString(Rules.StringOpKind.STRING_OP_KIND_BEFORE_FIRST) { text = "" }
        },
        Mutation("a weighted sum with an unspecified alignment", 12) {
            onInteger(Rules.IntegerOpKind.INTEGER_OP_KIND_WEIGHTED_SUM) { clearAlignment() }
        },
        Mutation("a weighted sum with an unspecified mapping", 12) {
            onInteger(Rules.IntegerOpKind.INTEGER_OP_KIND_WEIGHTED_SUM) { clearMapping() }
        },
        Mutation("a weighted sum with a mapping outside the enumeration", 12) {
            onInteger(Rules.IntegerOpKind.INTEGER_OP_KIND_WEIGHTED_SUM) { mappingValue = 47 }
        },
        Mutation("a weighted sum with an alignment outside the enumeration", 12) {
            onInteger(Rules.IntegerOpKind.INTEGER_OP_KIND_WEIGHTED_SUM) { alignmentValue = 47 }
        },
        Mutation("a modulo without its modulus", 12) {
            onInteger(Rules.IntegerOpKind.INTEGER_OP_KIND_MODULO) { clearModulus() }
        },
        Mutation("a remainder map without its table", 12) {
            onInteger(Rules.IntegerOpKind.INTEGER_OP_KIND_REMAINDER_MAP) { clearRemainderValues() }
        },
        Mutation("digits_to_integer carrying a modulus it does not declare", 12) {
            onInteger(Rules.IntegerOpKind.INTEGER_OP_KIND_DIGITS_TO_INTEGER) { modulus = 7 }
        },
        // Check 13 and not 12: section 10 gives check 12 the parameters an
        // operation declares and check 13 "the declared order of a parameter
        // list as section 9 states it". The list named the order at 2026.09.1;
        // this engine had been refusing the same bundles under the wrong number.
        Mutation("lengths that are not ascending", 13) {
            onPredicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_LENGTH_IN) {
                clearLengths()
                addAllLengths(listOf(8, 6))
            }
        },
        Mutation("prefixes that are not sorted", 13) {
            onPredicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_PREFIX_IN) {
                clearValues()
                addAllValues(listOf("Z", "P"))
            }
        },
        // Sorted and deduplicated, so the order rule lets it through; only the
        // one element length rule refuses it. The pair is `ir.md`'s own example.
        Mutation("prefixes of mixed element lengths", 13) {
            onPredicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_PREFIX_IN) {
                clearValues()
                addAllValues(listOf("AB", "ABA"))
            }
        },
        Mutation("the same prefix twice", 13) {
            onPredicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_PREFIX_IN) {
                clearValues()
                addAllValues(listOf("P", "P"))
            }
        },
        Mutation("an empty prefix", 12) {
            onPredicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_PREFIX_IN) {
                clearValues()
                addAllValues(listOf(""))
            }
        },
        Mutation("a profile no version of the IR names", 12) {
            onPredicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_PROFILE_IS) { text = "lenient" }
        },
        Mutation("an integer predicate without its constant", 12) {
            onPredicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_INTEGER_IS) { clearConstant() }
        },
        Mutation("a length_eq without its length", 12) {
            onPredicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_LENGTH_EQ) { clearLength() }
        },
        Mutation("a char_at_in without its index", 12) {
            onPredicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_CHAR_AT_IN) { clearIndex() }
        },
        Mutation("an ascii class carrying a text it does not declare", 12) {
            onPredicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_ASCII_DIGITS) { text = "0123456789" }
        },
        Mutation("a prefix replaced by itself", 12) {
            onCanonicalization(Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_REPLACE_PREFIX) {
                replacement = "ZZ"
            }
        },
        Mutation("a pad of more than one code point", 12) {
            onCanonicalization(Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_LEFT_PAD) { text = "00" }
        },
        Mutation("an insert without its index", 12) {
            onCanonicalization(Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_INSERT) { clearIndex() }
        },
        Mutation("an append with an empty constant", 12) {
            onCanonicalization(Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_APPEND) { text = "" }
        },
        Mutation("a trim carrying a text it does not declare", 12) {
            onCanonicalization(Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_TRIM_WHITESPACE) { text = "x" }
        },
        Mutation("a compare_digit without its index", 12) {
            onChecksum(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_COMPARE_DIGIT) { clearIndex() }
        },
        Mutation("a compare_slice without its bounds", 12) {
            onChecksum(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_COMPARE_SLICE) { clearEnd() }
        },
        Mutation("a compare_constant without its constant", 12) {
            onChecksum(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_COMPARE_CONSTANT) { clearConstant() }
        },
        Mutation("an unsupported outcome without its reason", 12) {
            onChecksum(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_UNSUPPORTED) { clearReasonCode() }
        },
        Mutation("an unsupported outcome with a reason that proves an invalidity", 12) {
            onChecksum(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_UNSUPPORTED) {
                reasonCode = Rules.ReasonCode.REASON_CODE_INVALID_CHECKSUM
            }
        },
        Mutation("a luhn carrying an index it does not declare", 12) {
            onChecksum(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_LUHN) { index = 3 }
        },
        Mutation("an assertion carrying an empty message key", 12) {
            onAssertion(Rules.AssertionOpKind.ASSERTION_OP_KIND_REQUIRE) { messageKey = "" }
        },
        Mutation("an assertion sequence carrying a reason code", 12) {
            onAssertion(Rules.AssertionOpKind.ASSERTION_OP_KIND_SEQUENCE) {
                reasonCode = Rules.ReasonCode.REASON_CODE_EMPTY
            }
        },
        Mutation("a checksum carrying an empty message key", 12) {
            onChecksum(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_COMPARE_DIGIT) { messageKey = "" }
        },
        Mutation("a reason code outside the enumeration", 12) {
            onAssertion(Rules.AssertionOpKind.ASSERTION_OP_KIND_REQUIRE) { reasonCodeValue = 99 }
        },

        // -- 13: arithmetic --------------------------------------------------------
        Mutation("a slice bound past the limit", 13) {
            onString(Rules.StringOpKind.STRING_OP_KIND_SLICE) { end = Limits.MAX_SLICE_BOUND + 1 }
        },
        Mutation("a length past the slice limit", 13) {
            onPredicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_LENGTH_EQ) { length = Limits.MAX_SLICE_BOUND + 1 }
        },
        Mutation("a listed length past the slice limit", 13) {
            onPredicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_LENGTH_IN) {
                clearLengths()
                addLengths(Limits.MAX_SLICE_BOUND + 1)
            }
        },
        Mutation("a minimum length above the maximum", 13) {
            onPredicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_LENGTH_BETWEEN) {
                minLength = 9
                maxLength = 8
            }
        },
        Mutation("an integer predicate constant beyond the comparison range", 13) {
            onPredicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_INTEGER_IS) {
                constant = Limits.MAX_COMPARISON_CONSTANT + 1
            }
        },
        Mutation("an insert index past the slice limit", 13) {
            onCanonicalization(Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_INSERT) {
                index = Limits.MAX_SLICE_BOUND + 1
            }
        },
        Mutation("a pad to length zero", 13) {
            onCanonicalization(Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_LEFT_PAD) { length = 0 }
        },
        Mutation("a compared slice of nineteen code points", 13) {
            onChecksum(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_COMPARE_SLICE) {
                start = 0
                end = 19
            }
        },
        Mutation("a compared slice of no code point at all", 13) {
            onChecksum(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_COMPARE_SLICE) {
                start = 4
                end = 4
            }
        },
        Mutation("a checksum constant beyond the comparison range", 13) {
            onChecksum(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_COMPARE_CONSTANT) {
                constant = Limits.MAX_COMPARISON_CONSTANT + 1
            }
        },
        Mutation("a compare_digit index past the slice limit", 13) {
            onChecksum(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_COMPARE_DIGIT) { index = Limits.MAX_SLICE_BOUND + 1 }
        },
        Mutation("a modulus below two", 13) {
            onInteger(Rules.IntegerOpKind.INTEGER_OP_KIND_MODULO) { modulus = 1 }
        },
        Mutation("a modulus above the accepted range", 13) {
            onInteger(Rules.IntegerOpKind.INTEGER_OP_KIND_MOD_DIGITS) { modulus = Limits.MAX_MODULUS + 1 }
        },
        Mutation("a weight beyond the accepted magnitude", 13) {
            onInteger(Rules.IntegerOpKind.INTEGER_OP_KIND_WEIGHTED_SUM) {
                clearWeights()
                addWeights(Limits.MAX_ABS_WEIGHT + 1)
            }
        },
        Mutation("a negative weight beyond the accepted magnitude", 13) {
            onInteger(Rules.IntegerOpKind.INTEGER_OP_KIND_WEIGHTED_SUM) {
                clearWeights()
                addWeights(-Limits.MAX_ABS_WEIGHT - 1)
            }
        },
        Mutation("a weighted sum with no weight at all", 12) {
            onInteger(Rules.IntegerOpKind.INTEGER_OP_KIND_WEIGHTED_SUM) { clearWeights() }
        },
        Mutation("more weights than the limit allows", 13) {
            onInteger(Rules.IntegerOpKind.INTEGER_OP_KIND_WEIGHTED_SUM) {
                clearWeights()
                repeat(Limits.MAX_WEIGHTS + 1) { addWeights(1) }
            }
        },
        Mutation("a remainder table larger than the limit", 13) {
            onInteger(Rules.IntegerOpKind.INTEGER_OP_KIND_REMAINDER_MAP) {
                clearRemainderValues()
                repeat(Limits.MAX_REMAINDER_VALUES + 1) { addRemainderValues(0) }
            }
        },
        Mutation("an alphabet listing a code point twice", 13) {
            onCustomAlphabet { alphabet = "0011" }
        },
        Mutation("an alphabet of more code points than the limit allows", 13) {
            onCustomAlphabet {
                alphabet = (0 until Limits.MAX_ALPHABET_CODE_POINTS + 1)
                    .joinToString("") { String(Character.toChars(0x100 + it)) }
            }
        },
        Mutation("an alphabet with no code point at all", 13) {
            onCustomAlphabet { alphabet = "" }
        },
        Mutation("digits_to_integer over a view nothing bounds", 13) {
            onInteger(Rules.IntegerOpKind.INTEGER_OP_KIND_DIGITS_TO_INTEGER) { }
            mutateNode({
                it.hasIntegerOperation() &&
                    it.integerOperation.kind == Rules.IntegerOpKind.INTEGER_OP_KIND_DIGITS_TO_INTEGER
            }) {
                clearInputNodes().addInputNodes(0)
            }
        },

        // -- 15: roots, subjects and captures ---------------------------------------
        Mutation("a subject node that is not a string", 15) {
            onProgram(4) { subjectNode = rootNode }
        },
        Mutation("a subject node outside the program", 15) {
            onProgram(4) { subjectNode = 9999 }
        },
        Mutation("a root outside the program", 15) {
            onProgram(4) { rootNode = 9999 }
        },
        Mutation("a capture outside the program", 15) {
            onProgram(4) { setCaptures(0, Rules.Capture.newBuilder().setName("x").setNode(9999)) }
        },
        Mutation("a capture that is not a string", 15) {
            onProgram(4) { setCaptures(0, Rules.Capture.newBuilder().setName("x").setNode(rootNode)) }
        },
        Mutation("a capture without a name", 15) {
            onProgram(4) { setCaptures(0, Rules.Capture.newBuilder().setName("").setNode(4)) }
        },
        Mutation("the same capture name twice", 15) {
            onProgram(4) { setCaptures(1, Rules.Capture.newBuilder().setName("head").setNode(5)) }
        },
        Mutation("more captures than the limit allows", 15) {
            onProgram(4) {
                repeat(Limits.MAX_CAPTURES_PER_FORMAT + 1) { i ->
                    addCaptures(Rules.Capture.newBuilder().setName("c$i").setNode(4))
                }
            }
        },
        Mutation("a capture on a program that is not a format program", 15) {
            onProgram(6) { addCaptures(Rules.Capture.newBuilder().setName("x").setNode(0)) }
        },
        Mutation("a program with no node at all", 15) {
            onProgram(5) { clearNodes().rootNode = 0 }
        },

        // -- 16: shape ----------------------------------------------------------------
        Mutation("a canonicalisation program declaring a subject", 16) {
            // Node 0 of program 2 is a string, so check 15 accepts it and the
            // refusal comes from the shape rule that owns it.
            onProgram(2) { subjectNode = 0 }
        },
        Mutation("a canonicalisation program not rooted at a sequence", 16) {
            onProgram(1) { rootNode = 0 }
        },
        Mutation("a format program not rooted at an assertion sequence", 16) {
            onProgram(3) { rootNode = 2 }
        },
        Mutation("a when branch no choose reads", 16) {
            // Reachable from no root, so section 2 lets it exist and check 14
            // does not count it. It is still not a direct operand of a choose,
            // which is the only place `WHEN` is accepted, and the reference
            // loader enforced that by looking at each node's parents — a node
            // with no parent has none to look at.
            onProgram(6) {
                addNodes(
                    Rules.Node.newBuilder()
                        .setOutputType(Rules.ValueType.VALUE_TYPE_CHECKSUM_OUTCOME)
                        .addInputNodes(
                            (0 until nodesCount).first {
                                getNodes(it).outputType == Rules.ValueType.VALUE_TYPE_BOOLEAN
                            },
                        )
                        .addInputNodes(
                            (0 until nodesCount).first {
                                getNodes(it).hasChecksumOperation() &&
                                    getNodes(it).checksumOperation.kind ==
                                    Rules.ChecksumOpKind.CHECKSUM_OP_KIND_LUHN
                            },
                        )
                        .setChecksumOperation(
                            Rules.ChecksumOperation.newBuilder()
                                .setKind(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_WHEN),
                        ),
                )
            }
        },
        Mutation("a checksum program rooted at a when branch", 16) {
            onProgram(6) {
                rootNode = (0 until nodesCount).first {
                    getNodes(it).hasChecksumOperation() &&
                        getNodes(it).checksumOperation.kind == Rules.ChecksumOpKind.CHECKSUM_OP_KIND_WHEN
                }
            }
        },
        Mutation("a checksum operation inside a format program", 16) {
            onProgram(3) {
                addNodes(
                    Rules.Node.newBuilder()
                        .setOutputType(Rules.ValueType.VALUE_TYPE_CHECKSUM_OUTCOME)
                        .addInputNodes(0)
                        .setChecksumOperation(
                            Rules.ChecksumOperation.newBuilder()
                                .setKind(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_LUHN),
                        ),
                )
            }
        },
        Mutation("a format call inside a checksum program", 16) {
            onProgram(5) {
                addNodes(
                    Rules.Node.newBuilder()
                        .setOutputType(Rules.ValueType.VALUE_TYPE_ASSERTION)
                        .addInputNodes(0)
                        .setCallOperation(
                            Rules.CallOperation.newBuilder()
                                .setKind(Rules.CallOpKind.CALL_OP_KIND_FORMAT)
                                .setProgramId(3),
                        ),
                )
            }
        },
        Mutation("an integer operation inside a format program", 16) {
            onProgram(3) {
                addNodes(
                    Rules.Node.newBuilder()
                        .setOutputType(Rules.ValueType.VALUE_TYPE_INTEGER)
                        .addInputNodes(0)
                        .setIntegerOperation(
                            Rules.IntegerOperation.newBuilder()
                                .setKind(Rules.IntegerOpKind.INTEGER_OP_KIND_MOD_DIGITS)
                                .setModulus(97),
                        ),
                )
            }
        },
        Mutation("a subject read inside a canonicalisation program", 16) {
            onProgram(2) {
                addNodes(
                    Rules.Node.newBuilder()
                        .setOutputType(Rules.ValueType.VALUE_TYPE_STRING)
                        .setStringOperation(
                            Rules.StringOperation.newBuilder().setKind(Rules.StringOpKind.STRING_OP_KIND_SUBJECT),
                        ),
                )
            }
        },
        Mutation("an assertion inside a canonicalisation program", 16) {
            onProgram(2) {
                addNodes(
                    Rules.Node.newBuilder()
                        .setOutputType(Rules.ValueType.VALUE_TYPE_ASSERTION)
                        .addInputNodes(1)
                        .setAssertionOperation(
                            Rules.AssertionOperation.newBuilder()
                                .setKind(Rules.AssertionOpKind.ASSERTION_OP_KIND_REQUIRE)
                                .setReasonCode(Rules.ReasonCode.REASON_CODE_EMPTY),
                        ),
                )
            }
        },
        Mutation("a program used as a canonicalizer that is not one", 16) {
            setIdentifiers(2, Rules.IdentifierDefinition.newBuilder(getIdentifiers(2)).setCanonicalizationProgram(3))
        },

        // -- 17: identifiers ------------------------------------------------------------
        Mutation("an identifier carrying id zero", 17) {
            setIdentifiers(0, Rules.IdentifierDefinition.newBuilder(getIdentifiers(0)).setId(0))
        },
        Mutation("the same identifier id twice", 17) {
            setIdentifiers(1, Rules.IdentifierDefinition.newBuilder(getIdentifiers(1)).setId(1))
        },
        Mutation("identifiers out of the normative order", 17) {
            val first = getIdentifiers(0)
            setIdentifiers(0, getIdentifiers(1))
            setIdentifiers(1, first)
        },
        Mutation("sources out of order", 17) {
            val definition = Rules.IdentifierDefinition.newBuilder(getIdentifiers(0))
            val first = definition.getSources(0)
            definition.setSources(0, definition.getSources(1))
            definition.setSources(1, first)
            setIdentifiers(0, definition)
        },
        Mutation("a source tier outside the enumeration", 17) {
            val definition = Rules.IdentifierDefinition.newBuilder(getIdentifiers(0))
            definition.setSources(0, definition.getSources(0).toBuilder().setTierValue(47))
            setIdentifiers(0, definition)
        },
        Mutation("a kind starting with a digit", 17) {
            setIdentifiers(0, Rules.IdentifierDefinition.newBuilder(getIdentifiers(0)).setKind("1demo"))
        },
        Mutation("a kind holding a forbidden character", 17) {
            setIdentifiers(0, Rules.IdentifierDefinition.newBuilder(getIdentifiers(0)).setKind("de mo"))
        },
        Mutation("a kind longer than sixty-four characters", 17) {
            setIdentifiers(0, Rules.IdentifierDefinition.newBuilder(getIdentifiers(0)).setKind("d".repeat(65)))
        },
        Mutation("an empty kind", 17) {
            setIdentifiers(0, Rules.IdentifierDefinition.newBuilder(getIdentifiers(0)).setKind(""))
        },
        Mutation("a country of the wrong length", 17) {
            setIdentifiers(0, Rules.IdentifierDefinition.newBuilder(getIdentifiers(0)).setCountryCode("BEL"))
        },
        Mutation("a lower case country", 17) {
            setIdentifiers(0, Rules.IdentifierDefinition.newBuilder(getIdentifiers(0)).setCountryCode("be"))
        },
        Mutation("a definition naming a program that does not exist", 17) {
            setIdentifiers(0, Rules.IdentifierDefinition.newBuilder(getIdentifiers(0)).setFormatProgram(99))
        },
        Mutation("a definition naming a checksum program that does not exist", 17) {
            setIdentifiers(0, Rules.IdentifierDefinition.newBuilder(getIdentifiers(0)).setChecksumProgram(99))
        },

        // -- 18: the checksum declaration --------------------------------------------------
        Mutation("a definition declaring both a checksum program and an absence reason", 18) {
            setIdentifiers(
                0,
                Rules.IdentifierDefinition.newBuilder(getIdentifiers(0))
                    .setAbsentChecksumReason(Rules.ReasonCode.REASON_CODE_CHECKSUM_NOT_PUBLISHED),
            )
        },
        Mutation("an absence reason that proves an invalidity", 18) {
            setIdentifiers(
                1,
                Rules.IdentifierDefinition.newBuilder(getIdentifiers(1))
                    .setAbsentChecksumReason(Rules.ReasonCode.REASON_CODE_INVALID_CHECKSUM),
            )
        },

        // -- 19 and 20: dispatchers and country aliases ---------------------------------------
        Mutation("dispatchers out of order", 19) {
            val first = getDispatchers(0)
            setDispatchers(0, getDispatchers(1))
            setDispatchers(1, first)
        },
        Mutation("a malformed dispatcher kind", 19) {
            setDispatchers(0, Rules.IdentifierDispatcher.newBuilder(getDispatchers(0)).setKind("Demo"))
        },
        Mutation("a dispatcher naming a pre-canonicalisation program that does not exist", 19) {
            setDispatchers(
                0,
                Rules.IdentifierDispatcher.newBuilder(getDispatchers(0)).setPreCanonicalizationProgram(99),
            )
        },
        Mutation("aliases out of order", 19) {
            setDispatchers(
                0,
                Rules.IdentifierDispatcher.newBuilder(getDispatchers(0))
                    .clearKindAliases()
                    .addKindAliases("demonstration")
                    .addKindAliases("demo_alias"),
            )
        },
        Mutation("a malformed alias", 19) {
            setDispatchers(
                0,
                Rules.IdentifierDispatcher.newBuilder(getDispatchers(0)).clearKindAliases().addKindAliases("Demo"),
            )
        },
        Mutation("an alias that is also a dispatcher kind", 19) {
            setDispatchers(
                0,
                Rules.IdentifierDispatcher.newBuilder(getDispatchers(0)).clearKindAliases().addKindAliases("glob"),
            )
        },
        Mutation("a malformed country alias", 20) {
            setDispatchers(
                0,
                Rules.IdentifierDispatcher.newBuilder(getDispatchers(0))
                    .clearCountryAliases()
                    .addCountryAliases(Rules.CountryAlias.newBuilder().setAlias("uk").setCountryCode("BE")),
            )
        },
        Mutation("an alias mapping to a malformed country", 20) {
            setDispatchers(
                0,
                Rules.IdentifierDispatcher.newBuilder(getDispatchers(0))
                    .clearCountryAliases()
                    .addCountryAliases(Rules.CountryAlias.newBuilder().setAlias("UK").setCountryCode("BEL")),
            )
        },
        Mutation("an alias mapping to itself", 20) {
            setDispatchers(
                0,
                Rules.IdentifierDispatcher.newBuilder(getDispatchers(0))
                    .clearCountryAliases()
                    .addCountryAliases(Rules.CountryAlias.newBuilder().setAlias("XZ").setCountryCode("XZ")),
            )
        },
        Mutation("an alias shadowing a declared target", 20) {
            setDispatchers(
                0,
                Rules.IdentifierDispatcher.newBuilder(getDispatchers(0))
                    .clearCountryAliases()
                    .addCountryAliases(Rules.CountryAlias.newBuilder().setAlias("BE").setCountryCode("FR")),
            )
        },
        Mutation("the same alias twice", 20) {
            setDispatchers(
                0,
                Rules.IdentifierDispatcher.newBuilder(getDispatchers(0))
                    .clearCountryAliases()
                    .addCountryAliases(Rules.CountryAlias.newBuilder().setAlias("UK").setCountryCode("BE"))
                    .addCountryAliases(Rules.CountryAlias.newBuilder().setAlias("UK").setCountryCode("FR")),
            )
        },
        Mutation("country aliases out of order", 20) {
            setDispatchers(
                0,
                Rules.IdentifierDispatcher.newBuilder(getDispatchers(0))
                    .clearCountryAliases()
                    .addCountryAliases(Rules.CountryAlias.newBuilder().setAlias("XZ").setCountryCode("BE"))
                    .addCountryAliases(Rules.CountryAlias.newBuilder().setAlias("XA").setCountryCode("FR")),
            )
        },

        // -- 21 and 22: targets ---------------------------------------------------------------
        Mutation("a target country of the wrong length", 21) {
            target(0, 0) { countryCode = "BEL" }
        },
        Mutation("the literal GLOBAL as a target country", 21) {
            target(0, 0) { countryCode = "GLOBAL" }
        },
        Mutation("the same country twice", 21) {
            target(0, 1) { countryCode = "BE" }
        },
        Mutation("targets out of order", 21) {
            target(0, 0) { countryCode = "FZ" }
        },
        Mutation("a prefix that is not alphanumeric", 21) {
            target(0, 1) {
                clearAcceptedPrefixes()
                addAcceptedPrefixes("F-R")
            }
        },
        Mutation("a prefix longer than eight characters", 21) {
            target(0, 1) {
                clearAcceptedPrefixes()
                addAcceptedPrefixes("FRANCEFRA")
            }
        },
        Mutation("an empty prefix on a target", 21) {
            target(0, 1) {
                clearAcceptedPrefixes()
                addAcceptedPrefixes("")
            }
        },
        Mutation("prefixes out of order inside one target", 21) {
            target(0, 0) {
                clearAcceptedPrefixes()
                addAcceptedPrefixes("PBE")
                addAcceptedPrefixes("BE")
            }
        },
        Mutation("one prefix claimed by two targets", 21) {
            target(0, 1) {
                clearAcceptedPrefixes()
                addAcceptedPrefixes("BE")
            }
        },
        Mutation("a canonical prefix that is not a prefix", 21) {
            target(0, 0) { canonicalPrefix = "" }
        },
        Mutation("two implicit targets", 21) {
            target(0, 1) { allowUnprefixedWithoutCountry = true }
        },
        Mutation("a GLOBAL target listed after a country target", 21) {
            setDispatchers(
                1,
                Rules.IdentifierDispatcher.newBuilder(getDispatchers(1))
                    .clearTargets()
                    .addTargets(
                        Rules.DispatchTarget.newBuilder().setCountryCode("BE").setIdentifierDefinitionId(3),
                    )
                    .addTargets(Rules.DispatchTarget.newBuilder().setIdentifierDefinitionId(3)),
            )
        },
        Mutation("a GLOBAL target carrying a prefix", 22) {
            target(1, 0) { addAcceptedPrefixes("GG") }
        },
        Mutation("a GLOBAL target carrying a canonical prefix", 22) {
            target(1, 0) { canonicalPrefix = "GG" }
        },
        Mutation("a GLOBAL target beside a country target", 22) {
            setDispatchers(
                1,
                Rules.IdentifierDispatcher.newBuilder(getDispatchers(1))
                    .addTargets(
                        Rules.DispatchTarget.newBuilder().setCountryCode("BE").setIdentifierDefinitionId(3),
                    ),
            )
        },
        Mutation("a country alias beside a GLOBAL target", 22) {
            setDispatchers(
                1,
                Rules.IdentifierDispatcher.newBuilder(getDispatchers(1))
                    .addCountryAliases(Rules.CountryAlias.newBuilder().setAlias("UK").setCountryCode("BE")),
            )
        },

        // -- 23: the link between a definition and a target -------------------------------------
        Mutation("a target naming a definition that does not exist", 23) {
            target(0, 0) { identifierDefinitionId = 99 }
        },
        Mutation("a target whose country differs from its definition", 23) {
            target(0, 0) { identifierDefinitionId = 2 }
        },
        Mutation("a target whose kind differs from its definition", 23) {
            target(1, 0) { identifierDefinitionId = 1 }
        },
        Mutation("a definition no target claims", 23) {
            addIdentifiers(
                Rules.IdentifierDefinition.newBuilder(getIdentifiers(2)).setId(9).setKind("zzz"),
            )
        },

        // -- 24: the call graph -------------------------------------------------------------------
        Mutation("a call towards a program of another kind", 24) {
            mutateNode({ it.hasCallOperation() && it.callOperation.kind == Rules.CallOpKind.CALL_OP_KIND_CHECKSUM }) {
                setCallOperation(callOperation.toBuilder().setProgramId(3))
            }
        },
        Mutation("a call towards a program that does not exist", 24) {
            mutateNode({ it.hasCallOperation() }) {
                setCallOperation(callOperation.toBuilder().setProgramId(99))
            }
        },
    )

    // -- locating a node by what it is, never by where it happens to sit --------

    private fun Rules.RuleBundle.Builder.locate(match: (Rules.Node) -> Boolean): Pair<Int, Int> {
        for (p in 0 until programsCount) {
            val program = getPrograms(p)
            for (n in 0 until program.nodesCount) {
                if (match(program.getNodes(n))) return p to n
            }
        }
        error("the kitchen sink holds no node matching that description")
    }

    private fun Rules.RuleBundle.Builder.mutateNode(
        match: (Rules.Node) -> Boolean,
        mutate: Rules.Node.Builder.() -> Unit,
    ) {
        val (p, n) = locate(match)
        val program = Rules.Program.newBuilder(getPrograms(p))
        program.setNodes(n, Rules.Node.newBuilder(program.getNodes(n)).apply(mutate))
        setPrograms(p, program)
    }

    private fun Rules.RuleBundle.Builder.onString(
        kind: Rules.StringOpKind,
        mutate: Rules.StringOperation.Builder.() -> Unit,
    ) = mutateNode({ it.hasStringOperation() && it.stringOperation.kind == kind }) {
        setStringOperation(stringOperation.toBuilder().apply(mutate))
    }

    private fun Rules.RuleBundle.Builder.onInteger(
        kind: Rules.IntegerOpKind,
        mutate: Rules.IntegerOperation.Builder.() -> Unit,
    ) = mutateNode({ it.hasIntegerOperation() && it.integerOperation.kind == kind }) {
        setIntegerOperation(integerOperation.toBuilder().apply(mutate))
    }

    private fun Rules.RuleBundle.Builder.onPredicate(
        kind: Rules.PredicateOpKind,
        mutate: Rules.PredicateOperation.Builder.() -> Unit,
    ) = mutateNode({ it.hasPredicateOperation() && it.predicateOperation.kind == kind }) {
        setPredicateOperation(predicateOperation.toBuilder().apply(mutate))
    }

    private fun Rules.RuleBundle.Builder.onCanonicalization(
        kind: Rules.CanonicalizationOpKind,
        mutate: Rules.CanonicalizationOperation.Builder.() -> Unit,
    ) = mutateNode({ it.hasCanonicalizationOperation() && it.canonicalizationOperation.kind == kind }) {
        setCanonicalizationOperation(canonicalizationOperation.toBuilder().apply(mutate))
    }

    private fun Rules.RuleBundle.Builder.onAssertion(
        kind: Rules.AssertionOpKind,
        mutate: Rules.AssertionOperation.Builder.() -> Unit,
    ) = mutateNode({ it.hasAssertionOperation() && it.assertionOperation.kind == kind }) {
        setAssertionOperation(assertionOperation.toBuilder().apply(mutate))
    }

    private fun Rules.RuleBundle.Builder.onChecksum(
        kind: Rules.ChecksumOpKind,
        mutate: Rules.ChecksumOperation.Builder.() -> Unit,
    ) = mutateNode({ it.hasChecksumOperation() && it.checksumOperation.kind == kind }) {
        setChecksumOperation(checksumOperation.toBuilder().apply(mutate))
    }

    private fun Rules.RuleBundle.Builder.onCustomAlphabet(mutate: Rules.IntegerOperation.Builder.() -> Unit) =
        mutateNode({
            it.hasIntegerOperation() &&
                it.integerOperation.mapping == Rules.CharMapping.CHAR_MAPPING_CUSTOM_ALPHABET
        }) {
            setIntegerOperation(integerOperation.toBuilder().apply(mutate))
        }

    private fun Rules.RuleBundle.Builder.onProgram(id: Int, mutate: Rules.Program.Builder.() -> Unit) {
        val index = (0 until programsCount).first { getPrograms(it).id == id }
        setPrograms(index, Rules.Program.newBuilder(getPrograms(index)).apply(mutate))
    }

    private fun Rules.RuleBundle.Builder.target(
        dispatcher: Int,
        index: Int,
        mutate: Rules.DispatchTarget.Builder.() -> Unit,
    ) {
        val builder = Rules.IdentifierDispatcher.newBuilder(getDispatchers(dispatcher))
        builder.setTargets(index, Rules.DispatchTarget.newBuilder(builder.getTargets(index)).apply(mutate))
        setDispatchers(dispatcher, builder)
    }

    @Test
    fun `the ruleset every mutation starts from is accepted`() {
        Loader.load(KitchenSink.bytes())
    }

    @Test
    fun `a when branch as the program root keeps its own rule and its own message`() {
        // The program root is excluded from the scan for a branch no choose
        // reads: `root_node` is a reference rather than an operand, and a
        // program rooted in a when branch is refused by the rule that owns it.
        // Were the root included, this would be reported by the wrong rule.
        val rooted = KitchenSink.bundle().toBuilder().apply {
            onProgram(6) {
                rootNode = (0 until nodesCount).first {
                    getNodes(it).hasChecksumOperation() &&
                        getNodes(it).checksumOperation.kind == Rules.ChecksumOpKind.CHECKSUM_OP_KIND_WHEN
                }
            }
        }
        val failure = assertFailsWith<RulesetException> { Loader.load(rooted.build().toByteArray()) }
        assertEquals(16, failure.check)
        assertEquals(
            "checksum program 6 roots at a when branch",
            failure.message,
            "the root case must not be reported as a branch no choose reads",
        )
    }

    @Test
    fun `a when branch nothing reads is reported as one`() {
        val orphan = KitchenSink.bundle().toBuilder().apply {
            mutations().first { it.name == "a when branch no choose reads" }.apply(this)
        }
        val failure = assertFailsWith<RulesetException> { Loader.load(orphan.build().toByteArray()) }
        assertEquals(16, failure.check)
        assertTrue(
            failure.message.endsWith("is a when branch no choose reads"),
            failure.message,
        )
    }

    /**
     * The one element length rule counts UTF-8 bytes, as `ir.md` counts it.
     *
     * `PZ` and `\u00E9` are both two bytes and one is not two code points, so a
     * loader reading the rule as "one code point count" would refuse a bundle
     * the reference accepts. No conformance case covers the difference — the
     * published ruleset is entirely ASCII, where the two readings agree — so it
     * is pinned here.
     */
    @Test
    fun `one element length is counted in bytes, not in code points`() {
        val builder = KitchenSink.bundle().toBuilder().apply {
            Mutation("accepted", 0) {
                onPredicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_PREFIX_IN) {
                    clearValues()
                    addAllValues(listOf("PZ", "\u00E9"))
                }
            }.apply(this)
        }
        val values = listOf("PZ", "\u00E9")
        assertEquals(
            listOf(2, 2),
            values.map { it.toByteArray(Charsets.UTF_8).size },
            "both are two UTF-8 bytes",
        )
        assertEquals(listOf(2, 1), values.map { it.length }, "and they are not the same number of code points")
        Loader.load(builder.build().toByteArray())
    }

    @TestFactory
    fun `each rule refuses on its own`(): List<DynamicTest> = mutations().map { mutation ->
        DynamicTest.dynamicTest("${mutation.name} (check ${mutation.check})") {
            val builder = KitchenSink.bundle().toBuilder().apply(mutation.apply)
            val failure = assertFailsWith<RulesetException> { Loader.load(builder.build().toByteArray()) }
            assertEquals(
                mutation.check,
                failure.check,
                "refused at check ${failure.check}: ${failure.message}",
            )
        }
    }
}
