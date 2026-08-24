// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.generator

import com.google.protobuf.ByteString
import libbusinessid.ir.v1.Rules

/**
 * A ruleset that uses every one of the sixty-three operations.
 *
 * The published ruleset uses fifty-two of them, so eleven emitter branches and a
 * long tail of load checks would never run against it. This one closes that gap:
 * it is accepted by all twenty-five checks and it exercises every opcode, every
 * alignment, every character mapping, a declared subject node, captures, a call
 * in each family, a country alias, a canonical prefix, an implicit target and a
 * GLOBAL target.
 *
 * Nothing here describes a real identifier. It is a shape, not a rule.
 */
@Suppress("LargeClass", "TooManyFunctions")
object KitchenSink {
    private class Nodes {
        val list = ArrayList<Rules.Node>()

        fun add(node: Rules.Node.Builder): Int {
            list += node.build()
            return list.size - 1
        }
    }

    private fun string(kind: Rules.StringOpKind, build: Rules.StringOperation.Builder.() -> Unit = {}) =
        Rules.Node.newBuilder()
            .setOutputType(Rules.ValueType.VALUE_TYPE_STRING)
            .setStringOperation(Rules.StringOperation.newBuilder().setKind(kind).apply(build))

    private fun integer(kind: Rules.IntegerOpKind, build: Rules.IntegerOperation.Builder.() -> Unit = {}) =
        Rules.Node.newBuilder()
            .setOutputType(Rules.ValueType.VALUE_TYPE_INTEGER)
            .setIntegerOperation(Rules.IntegerOperation.newBuilder().setKind(kind).apply(build))

    private fun predicate(kind: Rules.PredicateOpKind, build: Rules.PredicateOperation.Builder.() -> Unit = {}) =
        Rules.Node.newBuilder()
            .setOutputType(Rules.ValueType.VALUE_TYPE_BOOLEAN)
            .setPredicateOperation(Rules.PredicateOperation.newBuilder().setKind(kind).apply(build))

    private fun canonicalization(
        kind: Rules.CanonicalizationOpKind,
        build: Rules.CanonicalizationOperation.Builder.() -> Unit = {},
    ) = Rules.Node.newBuilder()
        .setOutputType(Rules.ValueType.VALUE_TYPE_CANONICALIZATION_STEP)
        .setCanonicalizationOperation(Rules.CanonicalizationOperation.newBuilder().setKind(kind).apply(build))

    private fun assertion(kind: Rules.AssertionOpKind, build: Rules.AssertionOperation.Builder.() -> Unit = {}) =
        Rules.Node.newBuilder()
            .setOutputType(Rules.ValueType.VALUE_TYPE_ASSERTION)
            .setAssertionOperation(Rules.AssertionOperation.newBuilder().setKind(kind).apply(build))

    private fun checksum(kind: Rules.ChecksumOpKind, build: Rules.ChecksumOperation.Builder.() -> Unit = {}) =
        Rules.Node.newBuilder()
            .setOutputType(Rules.ValueType.VALUE_TYPE_CHECKSUM_OUTCOME)
            .setChecksumOperation(Rules.ChecksumOperation.newBuilder().setKind(kind).apply(build))

    private fun call(kind: Rules.CallOpKind, program: Int, output: Rules.ValueType) = Rules.Node.newBuilder()
        .setOutputType(output)
        .setCallOperation(Rules.CallOperation.newBuilder().setKind(kind).setProgramId(program))

    /** Program 1: the pre-canonicalisation program, restricted to its five operations. */
    private fun preCanonicalization(): Rules.Program {
        val n = Nodes()
        val trim = n.add(canonicalization(Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_TRIM_WHITESPACE))
        val strip = n.add(canonicalization(Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_REMOVE_WHITESPACE))
        val upper = n.add(canonicalization(Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_UPPERCASE_ASCII))
        val drop = n.add(
            canonicalization(Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_REMOVE_CHARS) { text = ".-/" },
        )
        val root = n.add(
            canonicalization(Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_SEQUENCE)
                .addInputNodes(trim).addInputNodes(strip).addInputNodes(upper).addInputNodes(drop),
        )
        return Rules.Program.newBuilder()
            .setId(1)
            .setKind(Rules.ProgramKind.PROGRAM_KIND_CANONICALIZATION)
            .addAllNodes(n.list)
            .setRootNode(root)
            .build()
    }

    /** Program 2: a country canonicalizer using every remaining canonicalisation step. */
    private fun countryCanonicalization(): Rules.Program {
        val n = Nodes()
        val value = n.add(string(Rules.StringOpKind.STRING_OP_KIND_VALUE))
        val isShort = n.add(
            predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_LENGTH_BETWEEN) {
                minLength = 0
                maxLength = 3
            }.addInputNodes(value),
        )
        val pad = n.add(
            canonicalization(Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_LEFT_PAD) {
                length = 4
                text = "0"
            },
        )
        val insert = n.add(
            canonicalization(Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_INSERT) {
                index = 0
                text = "Z"
            },
        )
        val whenShort = n.add(
            canonicalization(Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_WHEN)
                .addInputNodes(isShort).addInputNodes(pad).addInputNodes(insert),
        )
        val replace = n.add(
            canonicalization(Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_REPLACE_PREFIX) {
                text = "ZZ"
                replacement = "Z"
            },
        )
        val prepend = n.add(
            canonicalization(Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_PREPEND) { text = "P" },
        )
        val append = n.add(
            canonicalization(Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_APPEND) { text = "S" },
        )
        val country = n.add(
            canonicalization(
                Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_PREPEND_COUNTRY_IF_MISSING,
            ),
        )
        val root = n.add(
            canonicalization(Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_SEQUENCE)
                .addInputNodes(whenShort).addInputNodes(replace).addInputNodes(prepend)
                .addInputNodes(append).addInputNodes(country),
        )
        return Rules.Program.newBuilder()
            .setId(2)
            .setKind(Rules.ProgramKind.PROGRAM_KIND_CANONICALIZATION)
            .addAllNodes(n.list)
            .setRootNode(root)
            .build()
    }

    /** Program 3: a format program called by another one. */
    private fun calledFormat(): Rules.Program {
        val n = Nodes()
        val subject = n.add(string(Rules.StringOpKind.STRING_OP_KIND_SUBJECT))
        val digits = n.add(predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_ASCII_DIGITS).addInputNodes(subject))
        val require = n.add(
            assertion(Rules.AssertionOpKind.ASSERTION_OP_KIND_REQUIRE) {
                reasonCode = Rules.ReasonCode.REASON_CODE_INVALID_CHARACTERS
                // Every character the emitter has to escape, in ruleset data:
                // a quote would end the literal, a backslash would start an
                // escape, a dollar would open a template, and a control
                // character has no printable spelling.
                messageKey = "kitchen.\"quote\".back\\slash.${'$'}dollar.bell\u0007"
            }.addInputNodes(digits),
        )
        val root = n.add(
            assertion(Rules.AssertionOpKind.ASSERTION_OP_KIND_SEQUENCE).addInputNodes(require),
        )
        return Rules.Program.newBuilder()
            .setId(3)
            .setKind(Rules.ProgramKind.PROGRAM_KIND_FORMAT)
            .addAllNodes(n.list)
            .setRootNode(root)
            .build()
    }

    /** Program 4: the format program, using every predicate and every string constructor. */
    @Suppress("LongMethod")
    private fun format(): Rules.Program {
        val n = Nodes()
        val subject = n.add(string(Rules.StringOpKind.STRING_OP_KIND_SUBJECT))
        val value = n.add(string(Rules.StringOpKind.STRING_OP_KIND_VALUE))
        val constant = n.add(string(Rules.StringOpKind.STRING_OP_KIND_CONSTANT) { text = "Z" })
        val country = n.add(string(Rules.StringOpKind.STRING_OP_KIND_COUNTRY_CODE))
        val head = n.add(
            string(Rules.StringOpKind.STRING_OP_KIND_SLICE) {
                start = 0
                end = 2
            }.addInputNodes(subject),
        )
        val tail = n.add(string(Rules.StringOpKind.STRING_OP_KIND_SLICE_FROM) { start = 2 }.addInputNodes(subject))
        val front = n.add(string(Rules.StringOpKind.STRING_OP_KIND_SLICE_TO) { end = 2 }.addInputNodes(subject))
        val before = n.add(string(Rules.StringOpKind.STRING_OP_KIND_BEFORE_FIRST) { text = "Z" }.addInputNodes(value))
        val after = n.add(string(Rules.StringOpKind.STRING_OP_KIND_AFTER_FIRST) { text = "Z" }.addInputNodes(value))
        val stripped = n.add(
            string(Rules.StringOpKind.STRING_OP_KIND_STRIP_PREFIX) { text = "P" }.addInputNodes(value),
        )
        val joined = n.add(
            string(Rules.StringOpKind.STRING_OP_KIND_CONCAT).addInputNodes(head).addInputNodes(constant),
        )

        val notEmpty = n.add(
            predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_NOT).addInputNodes(
                n.add(predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_IS_EMPTY).addInputNodes(subject)),
            ),
        )
        val requireNotEmpty = n.add(
            assertion(Rules.AssertionOpKind.ASSERTION_OP_KIND_REQUIRE) {
                reasonCode = Rules.ReasonCode.REASON_CODE_EMPTY
                messageKey = "kitchen.empty"
            }.addInputNodes(notEmpty),
        )

        val absent = n.add(predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_IS_ABSENT).addInputNodes(before))
        val equal = n.add(
            predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_EQUALS).addInputNodes(head).addInputNodes(front),
        )
        val lengthEq = n.add(
            predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_LENGTH_EQ) { length = 8 }
                .addInputNodes(subject),
        )
        val lengthIn = n.add(
            predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_LENGTH_IN) { addAllLengths(listOf(6, 8, 10)) }
                .addInputNodes(subject),
        )
        val lengthBetween = n.add(
            predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_LENGTH_BETWEEN) {
                minLength = 1
                maxLength = 40
            }
                .addInputNodes(subject),
        )
        val upper = n.add(
            predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_ASCII_UPPER_LETTERS).addInputNodes(country),
        )
        val alnum = n.add(
            predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_ASCII_ALPHANUMERIC)
                .addInputNodes(subject),
        )
        val charset = n.add(
            predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_ASCII_CHARSET) { text = "0123456789PSZ" }
                .addInputNodes(subject),
        )
        val startsWith = n.add(
            predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_STARTS_WITH) { text = "P" }.addInputNodes(value),
        )
        val endsWith = n.add(
            predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_ENDS_WITH) { text = "S" }.addInputNodes(value),
        )
        val prefixIn = n.add(
            predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_PREFIX_IN) { addAllValues(listOf("P", "Z")) }
                .addInputNodes(value),
        )
        // Membership lists long enough to be packed into string constants rather
        // than walked. One `prefix_in` per element length under an `any`, which
        // is the shape `ir.md` prescribes and the only one check 13 accepts: a
        // single list of mixed lengths answers wrongly, not slowly.
        val membershipTwoBytes = n.add(
            predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_PREFIX_IN) {
                addAllValues(MEMBERSHIP_2)
            }.addInputNodes(value),
        )
        val membershipFourBytes = n.add(
            predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_PREFIX_IN) {
                addAllValues(MEMBERSHIP_4)
            }.addInputNodes(value),
        )
        val membershipSevenBytes = n.add(
            predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_PREFIX_IN) {
                addAllValues(MEMBERSHIP_7)
            }.addInputNodes(value),
        )
        val membership = n.add(
            predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_ANY)
                .addInputNodes(membershipTwoBytes)
                .addInputNodes(membershipFourBytes)
                .addInputNodes(membershipSevenBytes),
        )
        val charAtIn = n.add(
            predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_CHAR_AT_IN) {
                index = 0
                text = "PZ"
            }
                .addInputNodes(value),
        )
        val contains = n.add(
            predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_CONTAINS) { text = "Z" }.addInputNodes(value),
        )
        // A constant long enough that its code points cannot be emitted as one
        // array literal: past a few hundred elements the enclosing method
        // approaches the sixty-four kilobyte limit the JVM caps it at.
        val longConstant = n.add(
            predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_CONTAINS) {
                text = LONG_CONSTANT
            }.addInputNodes(value),
        )
        val profileIs = n.add(
            predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_PROFILE_IS) { text = "strict_current" },
        )
        val joinedPresent = n.add(
            predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_NOT).addInputNodes(
                n.add(predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_IS_ABSENT).addInputNodes(joined)),
            ),
        )
        val strippedPresent = n.add(
            predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_NOT).addInputNodes(
                n.add(predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_IS_ABSENT).addInputNodes(stripped)),
            ),
        )
        val afterPresent = n.add(
            predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_NOT).addInputNodes(
                n.add(predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_IS_ABSENT).addInputNodes(after)),
            ),
        )
        val tailPresent = n.add(
            predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_NOT).addInputNodes(
                n.add(predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_IS_ABSENT).addInputNodes(tail)),
            ),
        )

        val any = n.add(
            predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_ANY)
                .addInputNodes(lengthEq).addInputNodes(lengthIn).addInputNodes(absent).addInputNodes(profileIs),
        )
        val all = n.add(
            predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_ALL)
                .addInputNodes(lengthBetween).addInputNodes(alnum).addInputNodes(charset)
                .addInputNodes(equal).addInputNodes(upper).addInputNodes(startsWith)
                .addInputNodes(endsWith).addInputNodes(prefixIn).addInputNodes(charAtIn)
                .addInputNodes(
                    contains,
                ).addInputNodes(longConstant).addInputNodes(membership).addInputNodes(any).addInputNodes(joinedPresent)
                .addInputNodes(strippedPresent).addInputNodes(afterPresent).addInputNodes(tailPresent),
        )
        val requireShape = n.add(
            assertion(Rules.AssertionOpKind.ASSERTION_OP_KIND_REQUIRE) {
                reasonCode = Rules.ReasonCode.REASON_CODE_INVALID_FORMAT
            }.addInputNodes(all),
        )
        val called = n.add(
            call(Rules.CallOpKind.CALL_OP_KIND_FORMAT, 3, Rules.ValueType.VALUE_TYPE_ASSERTION)
                .addInputNodes(tail),
        )
        val root = n.add(
            assertion(Rules.AssertionOpKind.ASSERTION_OP_KIND_SEQUENCE)
                .addInputNodes(requireNotEmpty).addInputNodes(requireShape).addInputNodes(called),
        )
        return Rules.Program.newBuilder()
            .setId(4)
            .setKind(Rules.ProgramKind.PROGRAM_KIND_FORMAT)
            .addAllNodes(n.list)
            .setRootNode(root)
            .addCaptures(Rules.Capture.newBuilder().setName("head").setNode(head))
            .addCaptures(Rules.Capture.newBuilder().setName("tail").setNode(tail))
            .setSubjectNode(value)
            .build()
    }

    /** Program 5: a checksum program called by another one. */
    private fun calledChecksum(): Rules.Program {
        val n = Nodes()
        val subject = n.add(string(Rules.StringOpKind.STRING_OP_KIND_SUBJECT))
        val root = n.add(checksum(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_LUHN).addInputNodes(subject))
        return Rules.Program.newBuilder()
            .setId(5)
            .setKind(Rules.ProgramKind.PROGRAM_KIND_CHECKSUM)
            .addAllNodes(n.list)
            .setRootNode(root)
            .build()
    }

    /** Program 6: the checksum program, using every integer and checksum operation. */
    @Suppress("LongMethod")
    private fun checksumProgram(): Rules.Program {
        val n = Nodes()
        val subject = n.add(string(Rules.StringOpKind.STRING_OP_KIND_SUBJECT))
        val body = n.add(
            string(Rules.StringOpKind.STRING_OP_KIND_SLICE) {
                start = 0
                end = 6
            }
                .addInputNodes(subject),
        )
        val short = n.add(
            string(Rules.StringOpKind.STRING_OP_KIND_SLICE) {
                start = 0
                end = 4
            }
                .addInputNodes(subject),
        )

        val digitsToInteger = n.add(
            integer(Rules.IntegerOpKind.INTEGER_OP_KIND_DIGITS_TO_INTEGER).addInputNodes(short),
        )
        val modDigits = n.add(
            integer(Rules.IntegerOpKind.INTEGER_OP_KIND_MOD_DIGITS) { modulus = 97 }.addInputNodes(body),
        )
        val leftSum = n.add(
            integer(Rules.IntegerOpKind.INTEGER_OP_KIND_WEIGHTED_SUM) {
                addAllWeights(listOf(1L, 2L, 3L))
                alignment = Rules.WeightAlignment.WEIGHT_ALIGNMENT_LEFT
                mapping = Rules.CharMapping.CHAR_MAPPING_DIGIT_VALUE
            }.addInputNodes(body),
        )
        val rightSum = n.add(
            integer(Rules.IntegerOpKind.INTEGER_OP_KIND_WEIGHTED_SUM) {
                addAllWeights(listOf(7L, 3L, 1L))
                alignment = Rules.WeightAlignment.WEIGHT_ALIGNMENT_RIGHT
                mapping = Rules.CharMapping.CHAR_MAPPING_ALNUM_BASE36
            }.addInputNodes(body),
        )
        val cycleSum = n.add(
            integer(Rules.IntegerOpKind.INTEGER_OP_KIND_WEIGHTED_SUM) {
                addAllWeights(listOf(1L, 2L))
                alignment = Rules.WeightAlignment.WEIGHT_ALIGNMENT_CYCLE
                mapping = Rules.CharMapping.CHAR_MAPPING_CUSTOM_ALPHABET
                alphabet = "0123456789ABCDEFGHJKLMNPQRTUWXY"
            }.addInputNodes(body),
        )
        val modulo = n.add(
            integer(Rules.IntegerOpKind.INTEGER_OP_KIND_MODULO) { modulus = 11 }
                .addInputNodes(leftSum),
        )
        val complement = n.add(
            integer(Rules.IntegerOpKind.INTEGER_OP_KIND_COMPLEMENT) { modulus = 11 }
                .addInputNodes(modulo),
        )
        val remainder = n.add(
            integer(Rules.IntegerOpKind.INTEGER_OP_KIND_REMAINDER_MAP) {
                // Longer than one array literal may hold, for the same reason.
                addAllRemainderValues(List(LONG_TABLE) { (it % 10).toLong() })
            }.addInputNodes(complement),
        )

        val isOne = n.add(
            predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_INTEGER_IS) { constant = 1 }.addInputNodes(modDigits),
        )
        val compareDigit = n.add(
            checksum(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_COMPARE_DIGIT) {
                index = 7
                messageKey = "kitchen.digit"
            }.addInputNodes(remainder).addInputNodes(subject),
        )
        val compareSlice = n.add(
            checksum(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_COMPARE_SLICE) {
                start = 6
                end = 8
            }
                .addInputNodes(digitsToInteger).addInputNodes(subject),
        )
        val compareConstant = n.add(
            checksum(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_COMPARE_CONSTANT) { constant = 0 }
                .addInputNodes(rightSum),
        )
        val mod97 = n.add(checksum(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_ISO7064_MOD97_10).addInputNodes(subject))
        val luhn = n.add(checksum(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_LUHN).addInputNodes(subject))
        val cycleCheck = n.add(
            checksum(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_COMPARE_CONSTANT) { constant = 3 }
                .addInputNodes(cycleSum),
        )
        val called = n.add(
            call(Rules.CallOpKind.CALL_OP_KIND_CHECKSUM, 5, Rules.ValueType.VALUE_TYPE_CHECKSUM_OUTCOME)
                .addInputNodes(short),
        )
        val unsupported = n.add(
            checksum(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_UNSUPPORTED) {
                reasonCode = Rules.ReasonCode.REASON_CODE_CHECKSUM_NOT_PUBLISHED
                messageKey = "kitchen.unpublished"
            },
        )
        val anyCheck = n.add(
            checksum(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_ANY_CHECK)
                .addInputNodes(luhn).addInputNodes(compareConstant),
        )
        val allChecks = n.add(
            checksum(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_ALL_CHECKS)
                .addInputNodes(called).addInputNodes(compareSlice).addInputNodes(cycleCheck),
        )
        val whenOne = n.add(
            checksum(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_WHEN).addInputNodes(isOne).addInputNodes(mod97),
        )
        val whenPresent = n.add(
            checksum(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_WHEN)
                .addInputNodes(
                    n.add(predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_ASCII_DIGITS).addInputNodes(subject)),
                )
                .addInputNodes(anyCheck),
        )
        // A choose with only conditional branches: when none applies the outcome
        // is unsupported, which is the branch the published ruleset never takes.
        val guarded = n.add(
            checksum(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_CHOOSE)
                .addInputNodes(whenOne).addInputNodes(whenPresent),
        )
        // A choose whose first branch is unconditional: it always applies, so
        // the chain has no condition at all.
        val unconditional = n.add(
            checksum(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_CHOOSE)
                .addInputNodes(luhn).addInputNodes(unsupported),
        )
        val root = n.add(
            checksum(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_CHOOSE)
                .addInputNodes(
                    n.add(
                        checksum(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_WHEN)
                            .addInputNodes(
                                n.add(
                                    predicate(Rules.PredicateOpKind.PREDICATE_OP_KIND_STARTS_WITH) { text = "P" }
                                        .addInputNodes(subject),
                                ),
                            )
                            .addInputNodes(
                                n.add(
                                    checksum(Rules.ChecksumOpKind.CHECKSUM_OP_KIND_ALL_CHECKS)
                                        .addInputNodes(guarded).addInputNodes(allChecks)
                                        .addInputNodes(unconditional).addInputNodes(compareDigit),
                                ),
                            ),
                    ),
                )
                .addInputNodes(unsupported),
        )
        // A subject built from the value, never from the subject it defines.
        val declaredSubject = n.add(string(Rules.StringOpKind.STRING_OP_KIND_VALUE))
        return Rules.Program.newBuilder()
            .setId(6)
            .setKind(Rules.ProgramKind.PROGRAM_KIND_CHECKSUM)
            .addAllNodes(n.list)
            .setRootNode(root)
            .setSubjectNode(declaredSubject)
            .build()
    }

    private fun source(id: String, tier: Rules.SourceTier) = Rules.Source.newBuilder()
        .setId(id)
        .setUrl("https://example.invalid/$id")
        .setAuthority("Kitchen sink")
        .setTitle("Every operation of the IR")
        .setAccessedAt("2026-08-24")
        .setJurisdiction("GLOBAL")
        .setLanguage("en")
        .setNotes("A shape, not a rule.")
        .setLicenseOrTerms("Apache-2.0")
        .setTier(tier)
        .build()

    /** Past [MAX_ELEMENTS_PER_METHOD] in the emitter, so the literal is split. */
    private const val LONG_TABLE = 600

    /** Six hundred code points, for the same reason. */
    private val LONG_CONSTANT: String = (0 until 600).joinToString("") { "abcdefghij"[it % 10].toString() }

    /**
     * Twenty prefixes of two UTF-8 bytes, holding every character the emitter
     * must escape.
     *
     * `U+0007`, `"`, `$` and `\` are the four a Kotlin string literal cannot
     * carry raw; the rest fill the list past the threshold above which it is
     * packed rather than walked, so the escaping is exercised in a real string
     * constant and not only in an array literal.
     */
    private val MEMBERSHIP_2: List<String> = buildList {
        add("\u0007A")
        add("\"A")
        add("${'$'}A")
        add("\\A")
        for (c in 'a'..'p') add("" + c + "A")
    }.sortedWith { a, b -> compareUtf8Bytes(a, b) }

    /**
     * Twenty prefixes of four UTF-8 bytes, in two shapes.
     *
     * Sixteen supplementary characters, one code point in two UTF-16 units
     * each, and four of four ASCII characters. One element length, and still two
     * shapes: the emitter groups by code point count and UTF-16 length, which is
     * finer than the length `ir.md` fixes, so a single accepted list still
     * becomes more than one packed search.
     */
    private val MEMBERSHIP_4: List<String> = buildList {
        for (i in 0 until 16) add(String(Character.toChars(0x10100 + i)))
        for (c in 'a'..'d') add("" + c + "AAA")
    }.sortedWith { a, b -> compareUtf8Bytes(a, b) }

    /**
     * Sixteen prefixes of seven UTF-8 bytes, one shape, in both arrangements of
     * a supplementary character and a three byte one.
     *
     * Two code points and three UTF-16 units either way, so all sixteen land in
     * one packed table — and the two arrangements are exactly the pair whose
     * code point order and UTF-16 order disagree. An entry opening at `U+FFFD`
     * precedes a supplementary one by code point and follows it by code unit, so
     * a table packed in Kotlin's own ordering would step past a member and call
     * it absent. This is what makes that failure reachable end to end rather
     * than only in a unit test of the comparator.
     */
    private val MEMBERSHIP_7: List<String> = buildList {
        for (i in 0 until 8) add("\uFFFD" + String(Character.toChars(0x10200 + i)))
        for (i in 0 until 8) add(String(Character.toChars(0x10300 + i)) + "\uFFFC")
    }.sortedWith { a, b -> compareUtf8Bytes(a, b) }

    private fun compareUtf8Bytes(left: String, right: String): Int {
        val a = left.toByteArray(Charsets.UTF_8)
        val b = right.toByteArray(Charsets.UTF_8)
        for (i in 0 until minOf(a.size, b.size)) {
            val order = (a[i].toInt() and 0xFF) - (b[i].toInt() and 0xFF)
            if (order != 0) return order
        }
        return a.size - b.size
    }

    /** The ruleset. Every check accepts it, and every opcode appears in it. */
    fun bundle(): Rules.RuleBundle = Rules.RuleBundle.newBuilder()
        .setFormatVersion(1)
        .setRulesVersion("2026.08.0")
        .addAllRequiredFeatureIds(listOf(1, 2, 3, 4, 5, 10, 11, 20, 21, 30, 31, 32, 33, 34, 35, 40, 41, 42))
        .setSourceDigest(ByteString.copyFrom(ByteArray(32) { it.toByte() }))
        .addIdentifiers(
            Rules.IdentifierDefinition.newBuilder()
                .setId(1)
                .setKind("demo")
                .setCountryCode("BE")
                .setCanonicalizationProgram(2)
                .setFormatProgram(4)
                .setChecksumProgram(6)
                .setDefaultProfile("strict_current")
                .addSources(source("kitchen-a", Rules.SourceTier.SOURCE_TIER_PRIMARY))
                .addSources(source("kitchen-b", Rules.SourceTier.SOURCE_TIER_SECONDARY)),
        )
        .addIdentifiers(
            Rules.IdentifierDefinition.newBuilder()
                .setId(2)
                .setKind("demo")
                .setCountryCode("FR")
                .setCanonicalizationProgram(2)
                .setFormatProgram(4)
                .setDefaultProfile("compatible")
                .setAbsentChecksumReason(Rules.ReasonCode.REASON_CODE_UNSUPPORTED_CHECKSUM)
                .addSources(source("kitchen-c", Rules.SourceTier.SOURCE_TIER_UNSPECIFIED)),
        )
        .addIdentifiers(
            Rules.IdentifierDefinition.newBuilder()
                .setId(3)
                .setKind("glob")
                .setCanonicalizationProgram(1)
                .setFormatProgram(3)
                .setDefaultProfile("compatible")
                .setAbsentChecksumReason(Rules.ReasonCode.REASON_CODE_CHECKSUM_NOT_PUBLISHED)
                .addSources(source("kitchen-d", Rules.SourceTier.SOURCE_TIER_PRIMARY)),
        )
        .addPrograms(preCanonicalization())
        .addPrograms(countryCanonicalization())
        .addPrograms(calledFormat())
        .addPrograms(format())
        .addPrograms(calledChecksum())
        .addPrograms(checksumProgram())
        .addDispatchers(
            Rules.IdentifierDispatcher.newBuilder()
                .setKind("demo")
                .addKindAliases("demo_alias")
                .addKindAliases("demonstration")
                .setPreCanonicalizationProgram(1)
                .addCountryAliases(Rules.CountryAlias.newBuilder().setAlias("UK").setCountryCode("BE"))
                .addTargets(
                    Rules.DispatchTarget.newBuilder()
                        .setCountryCode("BE")
                        .addAcceptedPrefixes("BE")
                        .addAcceptedPrefixes("PBE")
                        .setCanonicalPrefix("BE")
                        .setIdentifierDefinitionId(1)
                        .setAllowUnprefixedWithoutCountry(true),
                )
                .addTargets(
                    Rules.DispatchTarget.newBuilder()
                        .setCountryCode("FR")
                        .addAcceptedPrefixes("FR")
                        .setIdentifierDefinitionId(2),
                ),
        )
        .addDispatchers(
            Rules.IdentifierDispatcher.newBuilder()
                .setKind("glob")
                .setPreCanonicalizationProgram(1)
                .addTargets(Rules.DispatchTarget.newBuilder().setIdentifierDefinitionId(3)),
        )
        .build()

    fun bytes(): ByteArray = bundle().toByteArray()
}
