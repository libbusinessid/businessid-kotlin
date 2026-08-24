// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("MagicNumber")
// The numbers in this file are the twenty-five load checks of `ir.md` section
// 10. Naming them would hide the one thing a reader needs: which check a refusal
// belongs to. Every other constant lives in `Limits`.

package io.libbusinessid.generator

import com.google.protobuf.InvalidProtocolBufferException
import libbusinessid.ir.v1.Rules
import java.util.Locale

/**
 * The twenty-five load checks of `ir.md` section 10, in order.
 *
 * The generator treats the bundle as untrusted input and refuses to emit a
 * single line when any check fails. Refusal is a property of generation time:
 * the emitted engine meets no construction it does not understand, because
 * everything it holds came from a ruleset that passed all of this.
 *
 * Two orderings deserve a word.
 *
 * Check 5 runs after checks 3 and 4. A ruleset built against a later version
 * carries fields this build has never heard of; reporting those as unknown
 * fields would call a legitimate version gap a forgery.
 *
 * Check 15 is evaluated before check 14, which `ir.md` section 2 permits in as
 * many words: 14 counts from the roots 15 validates, and both answer
 * `invalid_ruleset`, so the order is not observable.
 */
@Suppress("LargeClass", "TooManyFunctions")
internal class Loader private constructor(private val bytes: ByteArray, private val bundle: Rules.RuleBundle) {
    private val used = sortedSetOf<Int>()
    private lateinit var programsById: Map<Int, Rules.Program>
    private lateinit var roles: Map<Int, MutableSet<ProgramRole>>
    private lateinit var maxLengths: Map<Int, LongArray>

    companion object {
        private val SUPPORTED_FORMAT_VERSIONS = setOf(Limits.SUPPORTED_FORMAT_VERSION)

        private val REQUIRE_REASONS = setOf(
            Rules.ReasonCode.REASON_CODE_EMPTY,
            Rules.ReasonCode.REASON_CODE_INVALID_LENGTH,
            Rules.ReasonCode.REASON_CODE_INVALID_CHARACTERS,
            Rules.ReasonCode.REASON_CODE_INVALID_FORMAT,
            Rules.ReasonCode.REASON_CODE_COUNTRY_MISMATCH,
        )

        private val ABSENT_CHECKSUM_REASONS = setOf(
            Rules.ReasonCode.REASON_CODE_UNSUPPORTED_CHECKSUM,
            Rules.ReasonCode.REASON_CODE_CHECKSUM_NOT_PUBLISHED,
        )

        private val PROFILES = setOf("compatible", "strict_current")

        /** Loads and validates [bytes]; throws [RulesetException] on any failure. */
        fun load(bytes: ByteArray): LoadedBundle {
            // 1. binary size at most 16 MiB
            if (bytes.size > Limits.MAX_BUNDLE_BYTES) {
                invalidRuleset(1, "bundle of ${bytes.size} bytes exceeds ${Limits.MAX_BUNDLE_BYTES}")
            }
            // 2. complete Protobuf decoding, at the wire level and nothing more
            val decoded =
                try {
                    Rules.RuleBundle.parseFrom(bytes)
                } catch (e: InvalidProtocolBufferException) {
                    invalidRuleset(2, "bundle does not decode: ${e.message}")
                }
            return Loader(bytes, decoded).run()
        }
    }

    private fun run(): LoadedBundle {
        check3FormatVersion()
        check4Features()
        check5UnknownFields()
        check6RulesVersion()
        check7SourceDigest()
        check8Programs()
        check9NodeCounts()
        computeRoles()
        check10Operations()
        check11Operands()
        check12Parameters()
        check13Arithmetic()
        check15Roots()
        check14Expansion()
        check16Shape()
        check17Identifiers()
        check18ChecksumDeclaration()
        check19Dispatchers()
        check20CountryAliases()
        val targets = check21Targets()
        check22GlobalTargets()
        check23DefinitionReferences(targets)
        check24CallGraph()
        check25Capabilities()

        val definitions = bundle.identifiersList.associateBy { it.id }
        val order = bundle.identifiersList.mapIndexed { index, d -> d.id to index }.toMap()
        return LoadedBundle(
            proto = bundle,
            programsById = programsById,
            definitionsById = definitions,
            definitionIndexById = order,
            targets = targets,
            usedCapabilities = used.toSortedSet(),
            sourceDigestHex = bundle.sourceDigest.toByteArray()
                .joinToString("") { String.format(Locale.ROOT, "%02x", it) },
        )
    }

    // -- 3 ------------------------------------------------------------------

    private fun check3FormatVersion() {
        if (bundle.formatVersion !in SUPPORTED_FORMAT_VERSIONS) {
            incompatibleRuleset(3, "format_version ${bundle.formatVersion} is not supported")
        }
    }

    // -- 4 ------------------------------------------------------------------

    private fun check4Features() {
        val ids = bundle.requiredFeatureIdsList
        // Unknown first: the question a version gap answers accurately.
        for (id in ids) {
            if (id !in Capabilities.REGISTRY) {
                incompatibleRuleset(4, "required capability $id is unknown to this build")
            }
        }
        for (i in 1 until ids.size) {
            if (ids[i] <= ids[i - 1]) {
                invalidRuleset(4, "required_feature_ids is not strictly ascending at index $i")
            }
        }
    }

    // -- 5 ------------------------------------------------------------------

    private fun check5UnknownFields() {
        val findings = Wire.scan(bytes, Descriptors.RULE_BUNDLE)
        val first = findings.firstOrNull() ?: return
        val detail = when (first) {
            is Wire.Finding.UnknownField -> "unknown field ${first.number} in ${first.path}"

            is Wire.Finding.RepeatedSingular -> "singular field ${first.number} of ${first.path} encoded twice"

            is Wire.Finding.TwoOneofBranches ->
                "two branches of one oneof in ${first.path}: ${first.first} and ${first.second}"

            is Wire.Finding.Malformed -> "malformed encoding in ${first.path}: ${first.detail}"
        }
        invalidRuleset(5, detail)
    }

    // -- 6 ------------------------------------------------------------------

    private fun check6RulesVersion() {
        val v = bundle.rulesVersion
        if (v.isEmpty()) invalidRuleset(6, "rules_version is empty")
        if (Cp.utf8Length(v) > Limits.MAX_RULES_VERSION_BYTES) {
            invalidRuleset(6, "rules_version exceeds ${Limits.MAX_RULES_VERSION_BYTES} bytes")
        }
        for (c in v) {
            val ok = c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c == '.' || c == '-' || c == '_'
            if (!ok) invalidRuleset(6, "rules_version holds the forbidden character U+%04X".format(c.code))
        }
    }

    // -- 7 ------------------------------------------------------------------

    private fun check7SourceDigest() {
        val size = bundle.sourceDigest.size()
        if (size != Limits.SOURCE_DIGEST_BYTES) {
            invalidRuleset(7, "source_digest is $size bytes, expected ${Limits.SOURCE_DIGEST_BYTES}")
        }
    }

    // -- 8 ------------------------------------------------------------------

    private fun check8Programs() {
        val byId = LinkedHashMap<Int, Rules.Program>()
        var previous = -1L
        for (p in bundle.programsList) {
            if (p.id == 0) invalidRuleset(8, "a program carries id 0")
            if (byId.put(p.id, p) != null) invalidRuleset(8, "program id ${p.id} is declared twice")
            if (p.id.toLong() <= previous) {
                invalidRuleset(8, "programs are not sorted by ascending id at ${p.id}")
            }
            previous = p.id.toLong()
            if (Rules.ProgramKind.forNumber(p.kindValue).let {
                    it == null ||
                        it == Rules.ProgramKind.PROGRAM_KIND_UNSPECIFIED
                }
            ) {
                invalidRuleset(8, "program ${p.id} declares kind ${p.kindValue}")
            }
        }
        programsById = byId
    }

    // -- 9 ------------------------------------------------------------------

    private fun check9NodeCounts() {
        var total = 0L
        for (p in bundle.programsList) {
            if (p.nodesCount > Limits.MAX_NODES_PER_PROGRAM) {
                invalidRuleset(9, "program ${p.id} holds ${p.nodesCount} nodes")
            }
            total += p.nodesCount
        }
        if (total > Limits.MAX_TOTAL_NODES) invalidRuleset(9, "the bundle holds $total nodes")
        if (bundle.identifiersCount > Limits.MAX_IDENTIFIERS) {
            invalidRuleset(9, "the bundle holds ${bundle.identifiersCount} identifiers")
        }
    }

    // -- roles --------------------------------------------------------------

    /**
     * Which role each program plays, read from the raw references before the
     * tables are validated. A reference that names no program is left out and a
     * later check reports it.
     */
    private fun computeRoles() {
        val map = HashMap<Int, MutableSet<ProgramRole>>()
        fun add(id: Int, role: ProgramRole) {
            if (id in programsById) map.getOrPut(id) { mutableSetOf() }.add(role)
        }
        for (d in bundle.dispatchersList) add(d.preCanonicalizationProgram, ProgramRole.PRE_CANONICALIZATION)
        for (i in bundle.identifiersList) {
            add(
                i.canonicalizationProgram,
                if (i.hasCountryCode()) {
                    ProgramRole.DEFINITION_CANONICALIZATION
                } else {
                    ProgramRole.GLOBAL_DEFINITION_CANONICALIZATION
                },
            )
            add(i.formatProgram, ProgramRole.FORMAT)
            if (i.hasChecksumProgram()) add(i.checksumProgram, ProgramRole.CHECKSUM)
        }
        roles = map
    }

    // -- 10 -----------------------------------------------------------------

    /**
     * Every operation known, with its declared output type.
     *
     * An unknown operation is `invalid_ruleset` rather than
     * `incompatible_ruleset`, and the distinction is deliberate: a ruleset that
     * legitimately uses a newer operation declares the capability that
     * introduced it, so a build too old to understand it stopped at check 4.
     * Reaching here with an unknown operation means the ruleset used one without
     * declaring it.
     */
    private fun check10Operations() {
        for (p in bundle.programsList) {
            p.nodesList.forEachIndexed { index, node ->
                val where = "program ${p.id} node $index"
                val declared = Rules.ValueType.forNumber(node.outputTypeValue)
                if (declared == null || declared == Rules.ValueType.VALUE_TYPE_UNSPECIFIED) {
                    invalidRuleset(10, "$where declares output type ${node.outputTypeValue}")
                }
                val produced = producedType(node, where)
                if (produced != declared) {
                    invalidRuleset(10, "$where declares $declared but ${Nodes.operationName(node)} produces $produced")
                }
            }
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun producedType(node: Rules.Node, where: String): Rules.ValueType = when (node.operationCase) {
        Rules.Node.OperationCase.STRING_OPERATION -> {
            requireKnown(node.stringOperation.kindValue, Rules.StringOpKind::forNumber, where, "string")
            used += Capabilities.of(node.stringOperation.kind).toSet()
            Rules.ValueType.VALUE_TYPE_STRING
        }

        Rules.Node.OperationCase.INTEGER_OPERATION -> {
            requireKnown(node.integerOperation.kindValue, Rules.IntegerOpKind::forNumber, where, "integer")
            used += Capabilities.of(node.integerOperation.kind).toSet()
            Rules.ValueType.VALUE_TYPE_INTEGER
        }

        Rules.Node.OperationCase.PREDICATE_OPERATION -> {
            requireKnown(node.predicateOperation.kindValue, Rules.PredicateOpKind::forNumber, where, "predicate")
            used += Capabilities.of(node.predicateOperation.kind).toSet()
            Rules.ValueType.VALUE_TYPE_BOOLEAN
        }

        Rules.Node.OperationCase.CANONICALIZATION_OPERATION -> {
            requireKnown(
                node.canonicalizationOperation.kindValue,
                Rules.CanonicalizationOpKind::forNumber,
                where,
                "canonicalization",
            )
            used += Capabilities.of(node.canonicalizationOperation.kind).toSet()
            Rules.ValueType.VALUE_TYPE_CANONICALIZATION_STEP
        }

        Rules.Node.OperationCase.ASSERTION_OPERATION -> {
            requireKnown(node.assertionOperation.kindValue, Rules.AssertionOpKind::forNumber, where, "assertion")
            used += Capabilities.of(node.assertionOperation.kind).toSet()
            Rules.ValueType.VALUE_TYPE_ASSERTION
        }

        Rules.Node.OperationCase.CHECKSUM_OPERATION -> {
            requireKnown(node.checksumOperation.kindValue, Rules.ChecksumOpKind::forNumber, where, "checksum")
            used += Capabilities.of(node.checksumOperation.kind).toSet()
            Rules.ValueType.VALUE_TYPE_CHECKSUM_OUTCOME
        }

        Rules.Node.OperationCase.CALL_OPERATION -> {
            requireKnown(node.callOperation.kindValue, Rules.CallOpKind::forNumber, where, "call")
            used += Capabilities.of(node.callOperation.kind).toSet()
            when (node.callOperation.kind) {
                Rules.CallOpKind.CALL_OP_KIND_FORMAT -> Rules.ValueType.VALUE_TYPE_ASSERTION
                else -> Rules.ValueType.VALUE_TYPE_CHECKSUM_OUTCOME
            }
        }

        else -> invalidRuleset(10, "$where carries no operation")
    }

    private fun <E : Enum<E>> requireKnown(value: Int, lookup: (Int) -> E?, where: String, family: String) {
        val resolved = lookup(value)
        if (resolved == null || resolved.ordinal == 0) {
            invalidRuleset(10, "$where declares $family operation $value")
        }
    }

    // -- 11 -----------------------------------------------------------------

    private fun check11Operands() {
        for (p in bundle.programsList) {
            p.nodesList.forEachIndexed { index, node ->
                val where = "program ${p.id} node $index"
                for (operand in node.inputNodesList) {
                    if (operand >= index) {
                        invalidRuleset(11, "$where reads node $operand, which is not a strictly lower index")
                    }
                }
                checkArity(p, node, where)
            }
        }
    }

    private fun typeOf(p: Rules.Program, index: Int): Rules.ValueType =
        Rules.ValueType.forNumber(p.getNodes(index).outputTypeValue) ?: Rules.ValueType.VALUE_TYPE_UNSPECIFIED

    private fun operandTypes(p: Rules.Program, node: Rules.Node): List<Rules.ValueType> =
        node.inputNodesList.map { typeOf(p, it) }

    // One branch per operation family, which is the shape check 11 is stated in.
    @Suppress("CyclomaticComplexMethod", "LongMethod", "NestedBlockDepth")
    private fun checkArity(p: Rules.Program, node: Rules.Node, where: String) {
        val types = operandTypes(p, node)
        val n = types.size
        val string = Rules.ValueType.VALUE_TYPE_STRING
        val int = Rules.ValueType.VALUE_TYPE_INTEGER
        val bool = Rules.ValueType.VALUE_TYPE_BOOLEAN
        val step = Rules.ValueType.VALUE_TYPE_CANONICALIZATION_STEP
        val assertion = Rules.ValueType.VALUE_TYPE_ASSERTION
        val outcome = Rules.ValueType.VALUE_TYPE_CHECKSUM_OUTCOME

        fun exact(count: Int, vararg expected: Rules.ValueType) {
            if (n != count) invalidRuleset(11, "$where takes $count operands, $n given")
            expected.forEachIndexed { i, t ->
                if (types[i] != t) invalidRuleset(11, "$where operand $i is ${types[i]}, expected $t")
            }
        }

        fun homogeneous(min: Int, max: Int, t: Rules.ValueType) {
            if (n < min || n > max) invalidRuleset(11, "$where takes $min..$max operands, $n given")
            types.forEachIndexed { i, actual ->
                if (actual != t) invalidRuleset(11, "$where operand $i is $actual, expected $t")
            }
        }

        when (node.operationCase) {
            Rules.Node.OperationCase.STRING_OPERATION -> when (node.stringOperation.kind) {
                Rules.StringOpKind.STRING_OP_KIND_CONSTANT,
                Rules.StringOpKind.STRING_OP_KIND_VALUE,
                Rules.StringOpKind.STRING_OP_KIND_SUBJECT,
                Rules.StringOpKind.STRING_OP_KIND_COUNTRY_CODE,
                -> exact(0)

                Rules.StringOpKind.STRING_OP_KIND_CONCAT ->
                    homogeneous(Limits.MIN_CONCAT_OPERANDS, Limits.MAX_CONCAT_OPERANDS, string)

                else -> exact(1, string)
            }

            Rules.Node.OperationCase.INTEGER_OPERATION -> when (node.integerOperation.kind) {
                Rules.IntegerOpKind.INTEGER_OP_KIND_DIGITS_TO_INTEGER,
                Rules.IntegerOpKind.INTEGER_OP_KIND_MOD_DIGITS,
                Rules.IntegerOpKind.INTEGER_OP_KIND_WEIGHTED_SUM,
                -> exact(1, string)

                else -> exact(1, int)
            }

            Rules.Node.OperationCase.PREDICATE_OPERATION -> when (node.predicateOperation.kind) {
                Rules.PredicateOpKind.PREDICATE_OP_KIND_EQUALS -> exact(2, string, string)

                Rules.PredicateOpKind.PREDICATE_OP_KIND_ALL,
                Rules.PredicateOpKind.PREDICATE_OP_KIND_ANY,
                -> homogeneous(1, Int.MAX_VALUE, bool)

                Rules.PredicateOpKind.PREDICATE_OP_KIND_NOT -> exact(1, bool)

                Rules.PredicateOpKind.PREDICATE_OP_KIND_PROFILE_IS -> exact(0)

                Rules.PredicateOpKind.PREDICATE_OP_KIND_INTEGER_IS -> exact(1, int)

                else -> exact(1, string)
            }

            Rules.Node.OperationCase.CANONICALIZATION_OPERATION -> when (node.canonicalizationOperation.kind) {
                Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_SEQUENCE ->
                    homogeneous(0, Int.MAX_VALUE, step)

                Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_WHEN -> {
                    if (n < 2) invalidRuleset(11, "$where takes a predicate and at least one step")
                    if (types[0] != bool) invalidRuleset(11, "$where operand 0 is ${types[0]}, expected $bool")
                    for (i in 1 until n) {
                        if (types[i] != step) invalidRuleset(11, "$where operand $i is ${types[i]}, expected $step")
                    }
                }

                else -> exact(0)
            }

            Rules.Node.OperationCase.ASSERTION_OPERATION -> when (node.assertionOperation.kind) {
                Rules.AssertionOpKind.ASSERTION_OP_KIND_SEQUENCE -> homogeneous(1, Int.MAX_VALUE, assertion)
                else -> exact(1, bool)
            }

            Rules.Node.OperationCase.CHECKSUM_OPERATION -> when (node.checksumOperation.kind) {
                Rules.ChecksumOpKind.CHECKSUM_OP_KIND_LUHN,
                Rules.ChecksumOpKind.CHECKSUM_OP_KIND_ISO7064_MOD97_10,
                -> exact(1, string)

                Rules.ChecksumOpKind.CHECKSUM_OP_KIND_COMPARE_DIGIT,
                Rules.ChecksumOpKind.CHECKSUM_OP_KIND_COMPARE_SLICE,
                -> exact(2, int, string)

                Rules.ChecksumOpKind.CHECKSUM_OP_KIND_COMPARE_CONSTANT -> exact(1, int)

                Rules.ChecksumOpKind.CHECKSUM_OP_KIND_CHOOSE,
                Rules.ChecksumOpKind.CHECKSUM_OP_KIND_ALL_CHECKS,
                Rules.ChecksumOpKind.CHECKSUM_OP_KIND_ANY_CHECK,
                -> homogeneous(1, Int.MAX_VALUE, outcome)

                Rules.ChecksumOpKind.CHECKSUM_OP_KIND_WHEN -> exact(2, bool, outcome)

                else -> exact(0)
            }

            Rules.Node.OperationCase.CALL_OPERATION -> exact(1, string)

            else -> invalidRuleset(11, "$where carries no operation")
        }
    }

    // -- 12 -----------------------------------------------------------------

    /**
     * Only the parameters the operation declares, and every required parameter.
     *
     * A parameter foreign to the operation is refused rather than ignored: it
     * states something no runtime acts on, and two engines could disagree on
     * whether it mattered.
     */
    private fun check12Parameters() {
        for (p in bundle.programsList) {
            p.nodesList.forEachIndexed { index, node ->
                val where = "program ${p.id} node $index"
                when (node.operationCase) {
                    Rules.Node.OperationCase.STRING_OPERATION -> stringParameters(node.stringOperation, where)

                    Rules.Node.OperationCase.INTEGER_OPERATION -> integerParameters(node.integerOperation, where)

                    Rules.Node.OperationCase.PREDICATE_OPERATION ->
                        predicateParameters(node.predicateOperation, where)

                    Rules.Node.OperationCase.CANONICALIZATION_OPERATION ->
                        canonicalizationParameters(node.canonicalizationOperation, where)

                    Rules.Node.OperationCase.ASSERTION_OPERATION ->
                        assertionParameters(node.assertionOperation, where)

                    Rules.Node.OperationCase.CHECKSUM_OPERATION ->
                        checksumParameters(node.checksumOperation, where)

                    Rules.Node.OperationCase.CALL_OPERATION -> Unit

                    else -> invalidRuleset(12, "$where carries no operation")
                }
            }
        }
    }

    private fun expect(where: String, present: Set<String>, required: Set<String>, optional: Set<String> = emptySet()) {
        for (name in required) {
            if (name !in present) invalidRuleset(12, "$where lacks the required parameter $name")
        }
        for (name in present) {
            if (name !in required && name !in optional) {
                invalidRuleset(12, "$where carries the parameter $name, which it does not declare")
            }
        }
    }

    private fun constantText(where: String, text: String, nonEmpty: Boolean) {
        if (Cp.utf8Length(text) > Limits.MAX_CONSTANT_BYTES) {
            invalidRuleset(12, "$where carries a constant longer than ${Limits.MAX_CONSTANT_BYTES} bytes")
        }
        if (nonEmpty && text.isEmpty()) invalidRuleset(12, "$where carries an empty constant")
    }

    private fun messageKey(where: String, has: Boolean, key: String) {
        // A present but empty key cannot be told apart from an absent one in an
        // idiomatic API, so two engines could report differently on it.
        if (has && key.isEmpty()) invalidRuleset(12, "$where declares an empty message_key")
    }

    private fun stringParameters(op: Rules.StringOperation, where: String) {
        val present = buildSet {
            if (op.hasText()) add("text")
            if (op.hasStart()) add("start")
            if (op.hasEnd()) add("end")
        }
        when (op.kind) {
            Rules.StringOpKind.STRING_OP_KIND_CONSTANT -> {
                expect(where, present, setOf("text"))
                constantText(where, op.text, nonEmpty = false)
            }

            Rules.StringOpKind.STRING_OP_KIND_VALUE,
            Rules.StringOpKind.STRING_OP_KIND_SUBJECT,
            Rules.StringOpKind.STRING_OP_KIND_COUNTRY_CODE,
            Rules.StringOpKind.STRING_OP_KIND_CONCAT,
            -> expect(where, present, emptySet())

            Rules.StringOpKind.STRING_OP_KIND_SLICE -> expect(where, present, setOf("start", "end"))

            Rules.StringOpKind.STRING_OP_KIND_SLICE_FROM -> expect(where, present, setOf("start"))

            Rules.StringOpKind.STRING_OP_KIND_SLICE_TO -> expect(where, present, setOf("end"))

            else -> {
                expect(where, present, setOf("text"))
                constantText(where, op.text, nonEmpty = true)
            }
        }
    }

    // Each branch is one integer operation, and merging them would hide which parameters each declares.
    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "ComplexCondition")
    private fun integerParameters(op: Rules.IntegerOperation, where: String) {
        val present = buildSet {
            if (op.hasModulus()) add("modulus")
            if (op.weightsCount > 0) add("weights")
            if (op.hasAlignment()) add("alignment")
            if (op.hasMapping()) add("mapping")
            if (op.remainderValuesCount > 0) add("remainder_values")
            if (op.hasAlphabet()) add("alphabet")
        }
        when (op.kind) {
            Rules.IntegerOpKind.INTEGER_OP_KIND_DIGITS_TO_INTEGER -> expect(where, present, emptySet())

            Rules.IntegerOpKind.INTEGER_OP_KIND_MOD_DIGITS,
            Rules.IntegerOpKind.INTEGER_OP_KIND_MODULO,
            Rules.IntegerOpKind.INTEGER_OP_KIND_COMPLEMENT,
            -> expect(where, present, setOf("modulus"))

            Rules.IntegerOpKind.INTEGER_OP_KIND_REMAINDER_MAP -> expect(where, present, setOf("remainder_values"))

            Rules.IntegerOpKind.INTEGER_OP_KIND_WEIGHTED_SUM -> {
                val alignment = Rules.WeightAlignment.forNumber(op.alignmentValue)
                if (alignment == null || alignment == Rules.WeightAlignment.WEIGHT_ALIGNMENT_UNSPECIFIED) {
                    invalidRuleset(12, "$where declares alignment ${op.alignmentValue}")
                }
                val mapping = Rules.CharMapping.forNumber(op.mappingValue)
                if (mapping == null || mapping == Rules.CharMapping.CHAR_MAPPING_UNSPECIFIED) {
                    invalidRuleset(12, "$where declares mapping ${op.mappingValue}")
                }
                // The alphabet is required by CUSTOM_ALPHABET and forbidden by
                // the others, so a ruleset cannot state an alphabet nothing reads.
                if (mapping == Rules.CharMapping.CHAR_MAPPING_CUSTOM_ALPHABET) {
                    expect(where, present, setOf("weights", "alignment", "mapping", "alphabet"))
                    used += Capabilities.CHECKSUM_CUSTOM_ALPHABET_V1
                } else {
                    expect(where, present, setOf("weights", "alignment", "mapping"))
                }
            }

            else -> invalidRuleset(12, "$where declares an unknown integer operation")
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun predicateParameters(op: Rules.PredicateOperation, where: String) {
        val present = buildSet {
            if (op.hasText()) add("text")
            if (op.valuesCount > 0) add("values")
            if (op.lengthsCount > 0) add("lengths")
            if (op.hasLength()) add("length")
            if (op.hasMinLength()) add("min_length")
            if (op.hasMaxLength()) add("max_length")
            if (op.hasIndex()) add("index")
            if (op.hasConstant()) add("constant")
        }
        when (op.kind) {
            Rules.PredicateOpKind.PREDICATE_OP_KIND_LENGTH_EQ -> expect(where, present, setOf("length"))

            Rules.PredicateOpKind.PREDICATE_OP_KIND_LENGTH_IN -> {
                expect(where, present, setOf("lengths"))
                requireAscending(where, op.lengthsList.map { it.toLong() }, "lengths")
            }

            Rules.PredicateOpKind.PREDICATE_OP_KIND_LENGTH_BETWEEN ->
                expect(where, present, setOf("min_length", "max_length"))

            Rules.PredicateOpKind.PREDICATE_OP_KIND_ASCII_CHARSET,
            Rules.PredicateOpKind.PREDICATE_OP_KIND_STARTS_WITH,
            Rules.PredicateOpKind.PREDICATE_OP_KIND_ENDS_WITH,
            Rules.PredicateOpKind.PREDICATE_OP_KIND_CONTAINS,
            -> {
                expect(where, present, setOf("text"))
                constantText(where, op.text, nonEmpty = true)
            }

            Rules.PredicateOpKind.PREDICATE_OP_KIND_PREFIX_IN -> {
                expect(where, present, setOf("values"))
                for (v in op.valuesList) constantText(where, v, nonEmpty = true)
                val sorted = op.valuesList.sortedWith(Comparator { a, b -> compareUtf8(a, b) })
                if (sorted != op.valuesList || op.valuesList.distinct().size != op.valuesCount) {
                    invalidRuleset(12, "$where declares prefixes that are not sorted and deduplicated")
                }
            }

            Rules.PredicateOpKind.PREDICATE_OP_KIND_CHAR_AT_IN -> {
                expect(where, present, setOf("index", "text"))
                constantText(where, op.text, nonEmpty = true)
            }

            Rules.PredicateOpKind.PREDICATE_OP_KIND_PROFILE_IS -> {
                expect(where, present, setOf("text"))
                if (op.text !in PROFILES) invalidRuleset(12, "$where names the profile ${op.text}")
            }

            Rules.PredicateOpKind.PREDICATE_OP_KIND_INTEGER_IS -> expect(where, present, setOf("constant"))

            else -> expect(where, present, emptySet())
        }
    }

    private fun requireAscending(where: String, values: List<Long>, name: String) {
        for (i in 1 until values.size) {
            if (values[i] <= values[i - 1]) {
                invalidRuleset(12, "$where declares $name that is not ascending and deduplicated")
            }
        }
    }

    private fun canonicalizationParameters(op: Rules.CanonicalizationOperation, where: String) {
        val present = buildSet {
            if (op.hasText()) add("text")
            if (op.hasReplacement()) add("replacement")
            if (op.hasIndex()) add("index")
            if (op.hasLength()) add("length")
        }
        when (op.kind) {
            Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_REMOVE_CHARS,
            Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_PREPEND,
            Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_APPEND,
            -> {
                expect(where, present, setOf("text"))
                constantText(where, op.text, nonEmpty = true)
            }

            Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_REPLACE_PREFIX -> {
                expect(where, present, setOf("text", "replacement"))
                constantText(where, op.text, nonEmpty = true)
                constantText(where, op.replacement, nonEmpty = false)
                if (op.text == op.replacement) invalidRuleset(12, "$where replaces a prefix by itself")
            }

            Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_INSERT -> {
                expect(where, present, setOf("index", "text"))
                constantText(where, op.text, nonEmpty = true)
            }

            Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_LEFT_PAD -> {
                expect(where, present, setOf("length", "text"))
                if (Cp.count(op.text) != 1) invalidRuleset(12, "$where pads with ${Cp.count(op.text)} code points")
            }

            else -> expect(where, present, emptySet())
        }
    }

    private fun assertionParameters(op: Rules.AssertionOperation, where: String) {
        val present = buildSet {
            if (op.hasReasonCode()) add("reason_code")
            if (op.hasMessageKey()) add("message_key")
        }
        messageKey(where, op.hasMessageKey(), op.messageKey)
        when (op.kind) {
            Rules.AssertionOpKind.ASSERTION_OP_KIND_REQUIRE -> {
                expect(where, present, setOf("reason_code"), setOf("message_key"))
                val reason = Rules.ReasonCode.forNumber(op.reasonCodeValue)
                if (reason == null || reason !in REQUIRE_REASONS) {
                    invalidRuleset(12, "$where asserts with reason ${op.reasonCodeValue}, which proves no invalidity")
                }
            }

            else -> expect(where, present, emptySet())
        }
    }

    // Each branch is one checksum operation, and merging them would hide which parameters each declares.
    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "ComplexCondition")
    private fun checksumParameters(op: Rules.ChecksumOperation, where: String) {
        val present = buildSet {
            if (op.hasIndex()) add("index")
            if (op.hasStart()) add("start")
            if (op.hasEnd()) add("end")
            if (op.hasReasonCode()) add("reason_code")
            if (op.hasMessageKey()) add("message_key")
            if (op.hasConstant()) add("constant")
        }
        messageKey(where, op.hasMessageKey(), op.messageKey)
        val key = setOf("message_key")
        when (op.kind) {
            Rules.ChecksumOpKind.CHECKSUM_OP_KIND_LUHN,
            Rules.ChecksumOpKind.CHECKSUM_OP_KIND_ISO7064_MOD97_10,
            -> expect(where, present, emptySet(), key)

            Rules.ChecksumOpKind.CHECKSUM_OP_KIND_COMPARE_DIGIT -> expect(where, present, setOf("index"), key)

            Rules.ChecksumOpKind.CHECKSUM_OP_KIND_COMPARE_SLICE ->
                expect(where, present, setOf("start", "end"), key)

            Rules.ChecksumOpKind.CHECKSUM_OP_KIND_COMPARE_CONSTANT ->
                expect(where, present, setOf("constant"), key)

            Rules.ChecksumOpKind.CHECKSUM_OP_KIND_UNSUPPORTED -> {
                expect(where, present, setOf("reason_code"), key)
                val reason = Rules.ReasonCode.forNumber(op.reasonCodeValue)
                if (reason == null || reason !in ABSENT_CHECKSUM_REASONS) {
                    invalidRuleset(12, "$where declares the unsupported reason ${op.reasonCodeValue}")
                }
            }

            else -> expect(where, present, emptySet(), key)
        }
    }

    private fun compareUtf8(a: String, b: String): Int {
        val x = a.toByteArray(Charsets.UTF_8)
        val y = b.toByteArray(Charsets.UTF_8)
        val n = minOf(x.size, y.size)
        for (i in 0 until n) {
            val d = (x[i].toInt() and 0xFF) - (y[i].toInt() and 0xFF)
            if (d != 0) return d
        }
        return x.size - y.size
    }

    // -- 13 -----------------------------------------------------------------

    /**
     * Arithmetic bounds: moduli, weights, remainder tables, indices, provable
     * integer widths and the alphabet of a custom mapping.
     *
     * Nothing here saturates or wraps. Where a bound has to be proved rather
     * than read, it is proved conservatively and in arbitrary precision, so the
     * prover cannot itself overflow.
     */
    private fun check13Arithmetic() {
        maxLengths = bundle.programsList.associate { it.id to inferMaxLengths(it) }
        for (p in bundle.programsList) {
            val lengths = maxLengths.getValue(p.id)
            p.nodesList.forEachIndexed { index, node ->
                val where = "program ${p.id} node $index"
                when (node.operationCase) {
                    Rules.Node.OperationCase.STRING_OPERATION -> stringBounds(node.stringOperation, where)

                    Rules.Node.OperationCase.INTEGER_OPERATION ->
                        integerBounds(node.integerOperation, where, lengths[node.getInputNodes(0)])

                    Rules.Node.OperationCase.PREDICATE_OPERATION -> predicateBounds(node.predicateOperation, where)

                    Rules.Node.OperationCase.CANONICALIZATION_OPERATION ->
                        canonicalizationBounds(node.canonicalizationOperation, where)

                    Rules.Node.OperationCase.CHECKSUM_OPERATION -> checksumBounds(node.checksumOperation, where)

                    else -> Unit
                }
            }
        }
    }

    private fun sliceBound(where: String, name: String, value: Int) {
        if (value < 0 || value > Limits.MAX_SLICE_BOUND) {
            invalidRuleset(13, "$where declares $name = $value, outside 0..${Limits.MAX_SLICE_BOUND}")
        }
    }

    private fun stringBounds(op: Rules.StringOperation, where: String) {
        if (op.hasStart()) sliceBound(where, "start", op.start)
        if (op.hasEnd()) sliceBound(where, "end", op.end)
    }

    private fun predicateBounds(op: Rules.PredicateOperation, where: String) {
        if (op.hasLength()) sliceBound(where, "length", op.length)
        if (op.hasIndex()) sliceBound(where, "index", op.index)
        if (op.hasMinLength()) sliceBound(where, "min_length", op.minLength)
        if (op.hasMaxLength()) sliceBound(where, "max_length", op.maxLength)
        if (op.hasMinLength() && op.hasMaxLength() && op.minLength > op.maxLength) {
            invalidRuleset(13, "$where declares min_length ${op.minLength} above max_length ${op.maxLength}")
        }
        for (v in op.lengthsList) sliceBound(where, "lengths entry", v)
        if (op.hasConstant() &&
            (op.constant < -Limits.MAX_COMPARISON_CONSTANT || op.constant > Limits.MAX_COMPARISON_CONSTANT)
        ) {
            invalidRuleset(13, "$where compares against ${op.constant}, outside the comparison range")
        }
    }

    private fun canonicalizationBounds(op: Rules.CanonicalizationOperation, where: String) {
        if (op.hasIndex()) sliceBound(where, "index", op.index)
        if (op.hasLength()) {
            sliceBound(where, "length", op.length)
            if (op.kind == Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_LEFT_PAD && op.length < 1) {
                invalidRuleset(13, "$where pads to length ${op.length}")
            }
        }
    }

    private fun checksumBounds(op: Rules.ChecksumOperation, where: String) {
        if (op.hasIndex()) sliceBound(where, "index", op.index)
        if (op.hasStart()) sliceBound(where, "start", op.start)
        if (op.hasEnd()) sliceBound(where, "end", op.end)
        if (op.kind == Rules.ChecksumOpKind.CHECKSUM_OP_KIND_COMPARE_SLICE) {
            val width = op.end.toLong() - op.start.toLong()
            if (width < 1 || width > Limits.MAX_DIGITS_TO_INTEGER) {
                invalidRuleset(13, "$where compares a slice of $width code points")
            }
        }
        if (op.hasConstant() &&
            (op.constant < -Limits.MAX_COMPARISON_CONSTANT || op.constant > Limits.MAX_COMPARISON_CONSTANT)
        ) {
            invalidRuleset(13, "$where compares against ${op.constant}, outside the comparison range")
        }
    }

    private fun modulus(where: String, value: Long) {
        if (value < Limits.MIN_MODULUS || value > Limits.MAX_MODULUS) {
            invalidRuleset(13, "$where declares modulus $value, outside ${Limits.MIN_MODULUS}..${Limits.MAX_MODULUS}")
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun integerBounds(op: Rules.IntegerOperation, where: String, operandMaxLength: Long) {
        when (op.kind) {
            Rules.IntegerOpKind.INTEGER_OP_KIND_DIGITS_TO_INTEGER ->
                if (operandMaxLength > Limits.MAX_DIGITS_TO_INTEGER) {
                    invalidRuleset(
                        13,
                        "$where reads a view of up to $operandMaxLength code points, " +
                            "above the ${Limits.MAX_DIGITS_TO_INTEGER} an int64 can hold",
                    )
                }

            Rules.IntegerOpKind.INTEGER_OP_KIND_MOD_DIGITS,
            Rules.IntegerOpKind.INTEGER_OP_KIND_MODULO,
            Rules.IntegerOpKind.INTEGER_OP_KIND_COMPLEMENT,
            -> modulus(where, op.modulus)

            Rules.IntegerOpKind.INTEGER_OP_KIND_REMAINDER_MAP ->
                if (op.remainderValuesCount < Limits.MIN_REMAINDER_VALUES ||
                    op.remainderValuesCount > Limits.MAX_REMAINDER_VALUES
                ) {
                    invalidRuleset(13, "$where declares a remainder table of ${op.remainderValuesCount} entries")
                }

            Rules.IntegerOpKind.INTEGER_OP_KIND_WEIGHTED_SUM -> weightedSumBounds(op, where, operandMaxLength)

            else -> Unit
        }
    }

    private fun weightedSumBounds(op: Rules.IntegerOperation, where: String, operandMaxLength: Long) {
        if (op.weightsCount < Limits.MIN_WEIGHTS || op.weightsCount > Limits.MAX_WEIGHTS) {
            invalidRuleset(13, "$where declares ${op.weightsCount} weights")
        }
        var maxAbsWeight = 0L
        for (w in op.weightsList) {
            val abs = if (w < 0) -w else w
            if (abs > Limits.MAX_ABS_WEIGHT) invalidRuleset(13, "$where declares the weight $w")
            if (abs > maxAbsWeight) maxAbsWeight = abs
        }

        val maxMapped: Long = when (op.mapping) {
            Rules.CharMapping.CHAR_MAPPING_DIGIT_VALUE -> 9
            Rules.CharMapping.CHAR_MAPPING_ALNUM_BASE36 -> 35
            Rules.CharMapping.CHAR_MAPPING_CUSTOM_ALPHABET -> alphabetBounds(op, where).toLong() - 1
            else -> invalidRuleset(13, "$where declares an unknown mapping")
        }

        val pairings = when (op.alignment) {
            Rules.WeightAlignment.WEIGHT_ALIGNMENT_CYCLE -> operandMaxLength
            else -> minOf(operandMaxLength, op.weightsCount.toLong())
        }
        val bound = java.math.BigInteger.valueOf(pairings)
            .multiply(java.math.BigInteger.valueOf(maxAbsWeight))
            .multiply(java.math.BigInteger.valueOf(maxMapped))
        if (bound > java.math.BigInteger.valueOf(Long.MAX_VALUE)) {
            invalidRuleset(13, "$where can reach $bound, which no checked int64 addition can hold")
        }
    }

    private fun alphabetBounds(op: Rules.IntegerOperation, where: String): Int {
        val points = Cp.of(op.alphabet)
        if (points.size < Limits.MIN_ALPHABET_CODE_POINTS || points.size > Limits.MAX_ALPHABET_CODE_POINTS) {
            invalidRuleset(13, "$where declares an alphabet of ${points.size} code points")
        }
        if (points.toSet().size != points.size) {
            // A repeated code point would carry two values, and which one an
            // engine returned would depend on how it searched.
            invalidRuleset(13, "$where declares an alphabet listing a code point twice")
        }
        return points.size
    }

    /**
     * A conservative upper bound, in code points, on every string node of [p].
     *
     * `value()` and `subject()` are bounded by what a canonicalisation program
     * can produce from a bounded input, which is [Limits.MAX_CANONICAL_CODE_POINTS].
     * A caller supplied subject is a view of that same value, so the same bound
     * holds inside a called program without any per call site analysis.
     */
    @Suppress("CyclomaticComplexMethod")
    private fun inferMaxLengths(p: Rules.Program): LongArray {
        val out = LongArray(p.nodesCount)
        p.nodesList.forEachIndexed { index, node ->
            if (node.operationCase != Rules.Node.OperationCase.STRING_OPERATION) return@forEachIndexed
            val op = node.stringOperation
            fun operand(i: Int): Long {
                val ref = node.getInputNodes(i)
                return if (ref in 0 until index) out[ref] else Limits.MAX_CANONICAL_CODE_POINTS
            }
            out[index] = when (op.kind) {
                Rules.StringOpKind.STRING_OP_KIND_CONSTANT -> Cp.count(op.text).toLong()

                Rules.StringOpKind.STRING_OP_KIND_COUNTRY_CODE -> 2

                Rules.StringOpKind.STRING_OP_KIND_SLICE ->
                    if (op.end >= op.start) (op.end - op.start).toLong() else 0

                Rules.StringOpKind.STRING_OP_KIND_SLICE_FROM ->
                    maxOf(0L, operand(0) - op.start.toLong())

                Rules.StringOpKind.STRING_OP_KIND_SLICE_TO -> minOf(operand(0), op.end.toLong())

                Rules.StringOpKind.STRING_OP_KIND_BEFORE_FIRST,
                Rules.StringOpKind.STRING_OP_KIND_AFTER_FIRST,
                Rules.StringOpKind.STRING_OP_KIND_STRIP_PREFIX,
                -> operand(0)

                Rules.StringOpKind.STRING_OP_KIND_CONCAT -> {
                    var sum = 0L
                    for (i in node.inputNodesList.indices) {
                        sum += operand(i)
                        if (sum > Limits.MAX_CANONICAL_CODE_POINTS) {
                            sum = Limits.MAX_CANONICAL_CODE_POINTS
                            break
                        }
                    }
                    sum
                }

                else -> Limits.MAX_CANONICAL_CODE_POINTS
            }
        }
        return out
    }

    // -- 15 -----------------------------------------------------------------

    // One statement per rule of check 15; splitting it would scatter a checklist the specification states as one.
    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "ComplexCondition")
    private fun check15Roots() {
        for (p in bundle.programsList) {
            val n = p.nodesCount
            if (n == 0) invalidRuleset(15, "program ${p.id} holds no node")
            if (p.rootNode >= n) invalidRuleset(15, "program ${p.id} roots at node ${p.rootNode} of $n")
            val expectedRoot = when (p.kind) {
                Rules.ProgramKind.PROGRAM_KIND_CANONICALIZATION -> Rules.ValueType.VALUE_TYPE_CANONICALIZATION_STEP
                Rules.ProgramKind.PROGRAM_KIND_FORMAT -> Rules.ValueType.VALUE_TYPE_ASSERTION
                else -> Rules.ValueType.VALUE_TYPE_CHECKSUM_OUTCOME
            }
            if (typeOf(p, p.rootNode) != expectedRoot) {
                invalidRuleset(15, "program ${p.id} roots at a ${typeOf(p, p.rootNode)}, expected $expectedRoot")
            }
            if (p.hasSubjectNode()) {
                if (p.subjectNode >= n) {
                    invalidRuleset(15, "program ${p.id} declares subject node ${p.subjectNode} of $n")
                }
                if (typeOf(p, p.subjectNode) != Rules.ValueType.VALUE_TYPE_STRING) {
                    invalidRuleset(15, "program ${p.id} declares a subject node that is not a string")
                }
                // A subject built from the subject it defines makes a generator
                // recurse forever.
                if (readsSubject(p, p.subjectNode)) {
                    invalidRuleset(15, "program ${p.id} builds its subject node from the subject it defines")
                }
            }
            val names = HashSet<String>()
            for (c in p.capturesList) {
                if (c.node >= n) invalidRuleset(15, "program ${p.id} captures node ${c.node} of $n")
                if (typeOf(p, c.node) != Rules.ValueType.VALUE_TYPE_STRING) {
                    invalidRuleset(15, "program ${p.id} captures a node that is not a string")
                }
                if (c.name.isEmpty()) invalidRuleset(15, "program ${p.id} declares a capture without a name")
                if (!names.add(c.name)) invalidRuleset(15, "program ${p.id} declares the capture ${c.name} twice")
            }
            if (p.capturesCount > Limits.MAX_CAPTURES_PER_FORMAT) {
                invalidRuleset(15, "program ${p.id} declares ${p.capturesCount} captures")
            }
            if (p.capturesCount > 0 && p.kind != Rules.ProgramKind.PROGRAM_KIND_FORMAT) {
                invalidRuleset(15, "program ${p.id} declares captures but is not a format program")
            }
            // Captures and a declared subject both belong to the capability that
            // carries program reuse, beside the two call operations.
            if (p.capturesCount > 0 || p.hasSubjectNode()) used += Capabilities.CAPTURES_AND_CALLS_V1
        }
    }

    private fun readsSubject(p: Rules.Program, index: Int): Boolean {
        val seen = HashSet<Int>()
        val stack = ArrayDeque<Int>()
        stack.addLast(index)
        while (stack.isNotEmpty()) {
            val at = stack.removeLast()
            if (!seen.add(at)) continue
            val node = p.getNodes(at)
            if (Nodes.isSubject(node)) return true
            for (operand in node.inputNodesList) if (operand < at) stack.addLast(operand)
        }
        return false
    }

    // -- 14 -----------------------------------------------------------------

    /**
     * The emitted expansion stays inside the evaluation budget.
     *
     * A program is a DAG, and a DAG whose every node reads the previous one
     * twice expands exponentially while passing every other check. Without this
     * bound such a ruleset is a denial of service against the generator.
     *
     * The count starts at the emission roots and follows operands, so a node no
     * root reaches costs nothing. The roots are the program root, the subject
     * node, and every capture no other root already reaches — taken from the
     * highest index down, because an operand always sits at a lower index than
     * the node reading it, so one pass settles a capture reached by another.
     * Their costs are summed, because a generator emits all of them.
     *
     * The arithmetic saturates rather than wrapping: a chain two hundred levels
     * deep reaches 2^201 instances, and an accumulator that overflows lands on a
     * small number that would pass.
     */
    private fun check14Expansion() {
        for (p in bundle.programsList) {
            val reached = HashSet<Int>()
            val emissionRoots = ArrayList<Int>()
            emissionRoots += p.rootNode
            reach(p, p.rootNode, reached)
            if (p.hasSubjectNode()) {
                emissionRoots += p.subjectNode
                reach(p, p.subjectNode, reached)
            }
            for (c in p.capturesList.map { it.node }.sortedDescending()) {
                if (c !in reached) {
                    emissionRoots += c
                    reach(p, c, reached)
                }
            }
            var total = 0L
            val memo = HashMap<Int, Long>()
            for (root in emissionRoots) {
                total = saturatingAdd(total, instances(p, root, memo))
                if (total > Limits.MAX_STEPS) {
                    invalidRuleset(
                        14,
                        "program ${p.id} expands to at least $total operation instances, " +
                            "above the budget of ${Limits.MAX_STEPS}",
                    )
                }
            }
        }
    }

    private fun reach(p: Rules.Program, index: Int, seen: MutableSet<Int>) {
        if (!seen.add(index)) return
        for (operand in p.getNodes(index).inputNodesList) {
            if (operand < index) reach(p, operand, seen)
        }
    }

    private fun instances(p: Rules.Program, index: Int, memo: MutableMap<Int, Long>): Long {
        memo[index]?.let { return it }
        var total = 1L
        for (operand in p.getNodes(index).inputNodesList) {
            if (operand >= index) continue
            total = saturatingAdd(total, instances(p, operand, memo))
        }
        memo[index] = total
        return total
    }

    private fun saturatingAdd(a: Long, b: Long): Long {
        val sum = a + b
        return if (sum < 0) Long.MAX_VALUE else sum
    }

    // -- 16 -----------------------------------------------------------------

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun check16Shape() {
        for (p in bundle.programsList) {
            val programRoles = roles[p.id].orEmpty()
            val root = p.getNodes(p.rootNode)
            when (p.kind) {
                Rules.ProgramKind.PROGRAM_KIND_CANONICALIZATION -> {
                    if (root.operationCase != Rules.Node.OperationCase.CANONICALIZATION_OPERATION ||
                        root.canonicalizationOperation.kind !=
                        Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_SEQUENCE
                    ) {
                        invalidRuleset(16, "canonicalization program ${p.id} does not root at a sequence")
                    }
                    if (p.hasSubjectNode()) invalidRuleset(16, "canonicalization program ${p.id} declares a subject")
                }

                Rules.ProgramKind.PROGRAM_KIND_FORMAT ->
                    if (root.operationCase != Rules.Node.OperationCase.ASSERTION_OPERATION ||
                        root.assertionOperation.kind != Rules.AssertionOpKind.ASSERTION_OP_KIND_SEQUENCE
                    ) {
                        invalidRuleset(16, "format program ${p.id} does not root at an assertion sequence")
                    }

                else ->
                    if (Nodes.isChecksumWhen(root)) {
                        invalidRuleset(16, "checksum program ${p.id} roots at a when branch")
                    }
            }

            val chooseOperands = HashSet<Int>()
            for (node in p.nodesList) {
                if (node.operationCase == Rules.Node.OperationCase.CHECKSUM_OPERATION &&
                    node.checksumOperation.kind == Rules.ChecksumOpKind.CHECKSUM_OP_KIND_CHOOSE
                ) {
                    chooseOperands += node.inputNodesList
                }
            }

            p.nodesList.forEachIndexed { index, node ->
                val where = "program ${p.id} node $index"
                if (!categoryAllowed(p.kind, node)) {
                    invalidRuleset(16, "$where holds ${Nodes.operationName(node)}, forbidden in a ${p.kind}")
                }
                if (Nodes.isChecksumWhen(node)) {
                    for (reader in p.nodesList.indices) {
                        val readerNode = p.getNodes(reader)
                        if (index in readerNode.inputNodesList &&
                            !(
                                readerNode.operationCase == Rules.Node.OperationCase.CHECKSUM_OPERATION &&
                                    readerNode.checksumOperation.kind == Rules.ChecksumOpKind.CHECKSUM_OP_KIND_CHOOSE
                                )
                        ) {
                            invalidRuleset(16, "$where is a when branch read by something other than a choose")
                        }
                    }
                    // Accepted only as a direct operand of a choose, which a
                    // branch nothing reads is not. Looking at a node's parents
                    // alone misses it: a node with no parent has none to look
                    // at, and section 2 lets an unreachable node exist.
                    //
                    // The program root is not part of this scan. `root_node` is
                    // a reference and not an operand, and a program rooted in a
                    // when branch is already refused above, by the rule that
                    // owns it and with its own message.
                    if (index !in chooseOperands) {
                        invalidRuleset(16, "$where is a when branch no choose reads")
                    }
                }
                if (ProgramRole.PRE_CANONICALIZATION in programRoles && !Nodes.allowedInPreCanonicalization(node)) {
                    invalidRuleset(
                        16,
                        "$where holds ${Nodes.operationName(node)}, " +
                            "which a pre-canonicalization program may not hold",
                    )
                }
                if (Nodes.isPrependCountry(node) &&
                    ProgramRole.GLOBAL_DEFINITION_CANONICALIZATION in programRoles
                ) {
                    invalidRuleset(16, "$where prepends a country in a canonicalizer of a GLOBAL definition")
                }
            }

            checkRole(p, programRoles)
        }
    }

    private fun checkRole(p: Rules.Program, programRoles: Set<ProgramRole>) {
        for (role in programRoles) {
            val expected = when (role) {
                ProgramRole.PRE_CANONICALIZATION,
                ProgramRole.DEFINITION_CANONICALIZATION,
                ProgramRole.GLOBAL_DEFINITION_CANONICALIZATION,
                -> Rules.ProgramKind.PROGRAM_KIND_CANONICALIZATION

                ProgramRole.FORMAT -> Rules.ProgramKind.PROGRAM_KIND_FORMAT

                ProgramRole.CHECKSUM -> Rules.ProgramKind.PROGRAM_KIND_CHECKSUM
            }
            if (p.kind != expected) {
                invalidRuleset(16, "program ${p.id} is used as $role but declares ${p.kind}")
            }
        }
    }

    private fun categoryAllowed(kind: Rules.ProgramKind, node: Rules.Node): Boolean = when (kind) {
        Rules.ProgramKind.PROGRAM_KIND_CANONICALIZATION -> when (node.operationCase) {
            Rules.Node.OperationCase.STRING_OPERATION -> !Nodes.isSubject(node)

            Rules.Node.OperationCase.PREDICATE_OPERATION,
            Rules.Node.OperationCase.CANONICALIZATION_OPERATION,
            -> true

            else -> false
        }

        Rules.ProgramKind.PROGRAM_KIND_FORMAT -> when (node.operationCase) {
            Rules.Node.OperationCase.STRING_OPERATION,
            Rules.Node.OperationCase.PREDICATE_OPERATION,
            Rules.Node.OperationCase.ASSERTION_OPERATION,
            -> true

            Rules.Node.OperationCase.CALL_OPERATION ->
                node.callOperation.kind == Rules.CallOpKind.CALL_OP_KIND_FORMAT

            else -> false
        }

        else -> when (node.operationCase) {
            Rules.Node.OperationCase.STRING_OPERATION,
            Rules.Node.OperationCase.PREDICATE_OPERATION,
            Rules.Node.OperationCase.INTEGER_OPERATION,
            Rules.Node.OperationCase.CHECKSUM_OPERATION,
            -> true

            Rules.Node.OperationCase.CALL_OPERATION ->
                node.callOperation.kind == Rules.CallOpKind.CALL_OP_KIND_CHECKSUM

            else -> false
        }
    }

    // -- 17 -----------------------------------------------------------------

    private fun check17Identifiers() {
        val ids = HashSet<Int>()
        var previous: List<ByteArray>? = null
        for (d in bundle.identifiersList) {
            val where = "identifier ${d.id}"
            if (d.id == 0) invalidRuleset(17, "$where carries id 0")
            if (!ids.add(d.id)) invalidRuleset(17, "identifier id ${d.id} is declared twice")
            if (!isKindToken(d.kind)) invalidRuleset(17, "$where declares the kind ${d.kind}")
            if (d.hasCountryCode()) {
                val c = d.countryCode
                if (c == "GLOBAL" || !isCountryToken(c)) invalidRuleset(17, "$where declares the country $c")
            }
            if (d.defaultProfile !in PROFILES) {
                invalidRuleset(17, "$where declares the profile ${d.defaultProfile}")
            }
            used += Capabilities.PROFILES_V1
            requireProgram(17, where, "canonicalization_program", d.canonicalizationProgram)
            requireProgram(17, where, "format_program", d.formatProgram)
            if (d.hasChecksumProgram()) requireProgram(17, where, "checksum_program", d.checksumProgram)

            val key = listOf(
                d.kind.toByteArray(Charsets.UTF_8),
                byteArrayOf(if (d.hasCountryCode()) 1 else 0),
                (if (d.hasCountryCode()) d.countryCode else "").toByteArray(Charsets.UTF_8),
            )
            previous?.let {
                if (compareKeys(it, key) >= 0) {
                    invalidRuleset(17, "identifiers are not in the normative order at $where")
                }
            }
            previous = key
            checkSources(d, where)
        }
    }

    private fun checkSources(d: Rules.IdentifierDefinition, where: String) {
        if (d.sourcesCount > 0) used += Capabilities.PROVENANCE_V1
        var previousId: String? = null
        for (s in d.sourcesList) {
            val tier = Rules.SourceTier.forNumber(s.tierValue)
                ?: invalidRuleset(17, "$where cites a source with tier ${s.tierValue}")
            // UNSPECIFIED means the source states no tier, which is legal and
            // does not require the capability that carries a stated one.
            if (tier != Rules.SourceTier.SOURCE_TIER_UNSPECIFIED) used += Capabilities.PROVENANCE_TIER_V1
            previousId?.let {
                if (compareUtf8(it, s.id) >= 0) invalidRuleset(17, "$where lists sources out of order at ${s.id}")
            }
            previousId = s.id
        }
    }

    private fun requireProgram(check: Int, where: String, field: String, id: Int) {
        if (id !in programsById) invalidRuleset(check, "$where names $field $id, which no program declares")
    }

    private fun compareKeys(a: List<ByteArray>, b: List<ByteArray>): Int {
        for (i in a.indices) {
            val d = compareBytes(a[i], b[i])
            if (d != 0) return d
        }
        return 0
    }

    private fun compareBytes(x: ByteArray, y: ByteArray): Int {
        val n = minOf(x.size, y.size)
        for (i in 0 until n) {
            val d = (x[i].toInt() and 0xFF) - (y[i].toInt() and 0xFF)
            if (d != 0) return d
        }
        return x.size - y.size
    }

    private fun isKindToken(s: String): Boolean {
        if (s.isEmpty() || s.length > 64) return false
        if (s[0] !in 'a'..'z') return false
        for (i in 1 until s.length) {
            if (!isKindCharacter(s[i])) return false
        }
        return true
    }

    private fun isKindCharacter(c: Char) = c in 'a'..'z' || c in '0'..'9' || c == '_' || c == '-'

    private fun isCountryToken(s: String): Boolean = s.length == 2 && s[0] in 'A'..'Z' && s[1] in 'A'..'Z'

    // -- 18 -----------------------------------------------------------------

    private fun check18ChecksumDeclaration() {
        for (d in bundle.identifiersList) {
            val where = "identifier ${d.id}"
            if (d.hasChecksumProgram() == d.hasAbsentChecksumReason()) {
                invalidRuleset(
                    18,
                    "$where must declare exactly one of checksum_program and absent_checksum_reason",
                )
            }
            if (d.hasAbsentChecksumReason()) {
                val reason = Rules.ReasonCode.forNumber(d.absentChecksumReasonValue)
                if (reason == null || reason !in ABSENT_CHECKSUM_REASONS) {
                    invalidRuleset(18, "$where declares the absence reason ${d.absentChecksumReasonValue}")
                }
                used += Capabilities.CHECKSUM_TRISTATE_V1
            }
        }
    }

    // -- 19 -----------------------------------------------------------------

    // Two nested loops over dispatchers and their aliases, which is the shape of the rule.
    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "ComplexCondition")
    private fun check19Dispatchers() {
        if (bundle.dispatchersCount > 0) used += Capabilities.IDENTIFIER_DISPATCH_V1
        val tokens = HashMap<String, String>()
        var previous: String? = null
        for (d in bundle.dispatchersList) {
            val where = "dispatcher ${d.kind}"
            if (!isKindToken(d.kind)) invalidRuleset(19, "$where declares a malformed kind")
            previous?.let {
                if (compareUtf8(it, d.kind) >= 0) invalidRuleset(19, "dispatchers are not sorted at ${d.kind}")
            }
            previous = d.kind
            requireProgram(19, where, "pre_canonicalization_program", d.preCanonicalizationProgram)

            tokens.put(d.kind, d.kind)?.let { invalidRuleset(19, "the kind token ${d.kind} is claimed twice") }
            var previousAlias: String? = null
            for (alias in d.kindAliasesList) {
                if (!isKindToken(alias)) invalidRuleset(19, "$where declares the malformed alias $alias")
                previousAlias?.let {
                    if (compareUtf8(it, alias) >= 0) invalidRuleset(19, "$where lists aliases out of order at $alias")
                }
                previousAlias = alias
                tokens.put(alias, d.kind)?.let {
                    invalidRuleset(19, "the kind token $alias is claimed by both $it and ${d.kind}")
                }
            }
        }
    }

    // -- 20 -----------------------------------------------------------------

    // One loop over the aliases of each dispatcher, which is the shape of the rule.
    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "ComplexCondition")
    private fun check20CountryAliases() {
        for (d in bundle.dispatchersList) {
            val where = "dispatcher ${d.kind}"
            val targetCountries = d.targetsList.filter { it.hasCountryCode() }.map { it.countryCode }.toSet()
            var previous: String? = null
            val seen = HashSet<String>()
            for (a in d.countryAliasesList) {
                if (!isCountryToken(a.alias)) invalidRuleset(20, "$where declares the country alias ${a.alias}")
                if (!isCountryToken(a.countryCode)) {
                    invalidRuleset(20, "$where maps ${a.alias} to the malformed country ${a.countryCode}")
                }
                if (a.alias == a.countryCode) invalidRuleset(20, "$where maps ${a.alias} to itself")
                if (a.alias in targetCountries) {
                    invalidRuleset(20, "$where declares the alias ${a.alias}, which already names a target")
                }
                if (!seen.add(a.alias)) invalidRuleset(20, "$where declares the alias ${a.alias} twice")
                previous?.let {
                    if (compareUtf8(it, a.alias) >= 0) {
                        invalidRuleset(20, "$where lists country aliases out of order at ${a.alias}")
                    }
                }
                previous = a.alias
            }
        }
    }

    // -- 21 -----------------------------------------------------------------

    // One statement per rule of check 21; splitting it would scatter a checklist the specification states as one.
    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "ComplexCondition")
    private fun check21Targets(): List<TargetRef> {
        val flat = ArrayList<TargetRef>()
        bundle.dispatchersList.forEachIndexed { dispatcherIndex, d ->
            val where = "dispatcher ${d.kind}"
            var previous: String? = null
            var globalSeen = false
            val countries = HashSet<String>()
            val prefixOwner = HashMap<String, Int>()
            var unprefixed = 0
            d.targetsList.forEachIndexed { targetIndex, t ->
                if (t.hasCountryCode()) {
                    val c = t.countryCode
                    if (c == "GLOBAL" || !isCountryToken(c)) invalidRuleset(21, "$where declares the target $c")
                    if (!countries.add(c)) invalidRuleset(21, "$where declares the target $c twice")
                    previous?.let {
                        if (compareUtf8(it, c) >= 0) invalidRuleset(21, "$where lists targets out of order at $c")
                    }
                    previous = c
                } else {
                    if (globalSeen) invalidRuleset(21, "$where declares two GLOBAL targets")
                    if (targetIndex != 0) invalidRuleset(21, "$where lists its GLOBAL target after a country target")
                    globalSeen = true
                }
                var previousPrefix: String? = null
                for (p in t.acceptedPrefixesList) {
                    if (!isPrefix(p)) invalidRuleset(21, "$where declares the prefix $p")
                    previousPrefix?.let {
                        if (compareUtf8(it, p) >= 0) invalidRuleset(21, "$where lists prefixes out of order at $p")
                    }
                    previousPrefix = p
                    prefixOwner.put(p, targetIndex)?.let {
                        invalidRuleset(21, "$where lets the prefix $p be claimed by two targets")
                    }
                }
                if (t.hasCanonicalPrefix() && !isPrefix(t.canonicalPrefix)) {
                    invalidRuleset(21, "$where declares the canonical prefix ${t.canonicalPrefix}")
                }
                if (t.allowUnprefixedWithoutCountry) unprefixed++
                flat += TargetRef(flat.size, dispatcherIndex, t)
            }
            if (unprefixed > 1) {
                invalidRuleset(21, "$where declares $unprefixed implicit targets, which makes routing ambiguous")
            }
        }
        return flat
    }

    private fun isPrefix(s: String): Boolean {
        if (s.length !in 1..8) return false
        for (c in s) if (!(c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9')) return false
        return true
    }

    // -- 22 -----------------------------------------------------------------

    private fun check22GlobalTargets() {
        for (d in bundle.dispatchersList) {
            val where = "dispatcher ${d.kind}"
            val global = d.targetsList.firstOrNull { !it.hasCountryCode() } ?: continue
            if (d.targetsCount != 1) {
                invalidRuleset(22, "$where mixes a GLOBAL target with ${d.targetsCount - 1} country targets")
            }
            if (global.acceptedPrefixesCount > 0) invalidRuleset(22, "$where gives its GLOBAL target a prefix")
            if (global.hasCanonicalPrefix()) {
                invalidRuleset(22, "$where gives its GLOBAL target a canonical prefix")
            }
            if (d.countryAliasesCount > 0) invalidRuleset(22, "$where declares a country alias beside a GLOBAL target")
        }
    }

    // -- 23 -----------------------------------------------------------------

    private fun check23DefinitionReferences(targets: List<TargetRef>) {
        val referencedBy = HashMap<Int, Int>()
        for (t in targets) {
            val definitionId = t.proto.identifierDefinitionId
            val definition = bundle.identifiersList.firstOrNull { it.id == definitionId }
                ?: invalidRuleset(23, "a dispatch target names definition $definitionId, which does not exist")
            val dispatcher = bundle.getDispatchers(t.dispatcherIndex)
            if (definition.kind != dispatcher.kind) {
                invalidRuleset(
                    23,
                    "definition $definitionId declares kind ${definition.kind} " +
                        "but is routed by the dispatcher ${dispatcher.kind}",
                )
            }
            val targetCountry = t.countryCode
            val definitionCountry = if (definition.hasCountryCode()) definition.countryCode else null
            if (targetCountry != definitionCountry) {
                invalidRuleset(
                    23,
                    "definition $definitionId declares country $definitionCountry " +
                        "but is routed by the target $targetCountry",
                )
            }
            referencedBy.put(definitionId, t.index)?.let {
                invalidRuleset(23, "definition $definitionId is referenced by two dispatch targets")
            }
        }
        for (d in bundle.identifiersList) {
            if (d.id !in referencedBy) invalidRuleset(23, "definition ${d.id} is referenced by no dispatch target")
        }
    }

    // -- 24 -----------------------------------------------------------------

    private fun check24CallGraph() {
        val callees = HashMap<Int, List<Int>>()
        for (p in bundle.programsList) {
            val out = ArrayList<Int>()
            for (node in p.nodesList) {
                if (node.operationCase != Rules.Node.OperationCase.CALL_OPERATION) continue
                val target = node.callOperation.programId
                val callee = programsById[target]
                    ?: invalidRuleset(24, "program ${p.id} calls program $target, which does not exist")
                if (callee.kind != p.kind) {
                    invalidRuleset(24, "program ${p.id} of kind ${p.kind} calls a ${callee.kind}")
                }
                out += target
            }
            callees[p.id] = out
        }
        val depth = HashMap<Int, Int>()
        val onStack = HashSet<Int>()
        fun visit(id: Int): Int {
            depth[id]?.let { return it }
            if (!onStack.add(id)) invalidRuleset(24, "the call graph cycles through program $id")
            var deepest = 0
            for (callee in callees.getValue(id)) {
                val d = visit(callee)
                if (d > deepest) deepest = d
            }
            onStack.remove(id)
            val result = deepest + 1
            if (result > Limits.MAX_CALL_DEPTH) {
                invalidRuleset(24, "program $id sits at call depth $result, above ${Limits.MAX_CALL_DEPTH}")
            }
            depth[id] = result
            return result
        }
        for (p in bundle.programsList) visit(p.id)
    }

    // -- 25 -----------------------------------------------------------------

    private fun check25Capabilities() {
        val declared = bundle.requiredFeatureIdsList.toSet()
        val missing = used.filter { it !in declared }
        if (missing.isNotEmpty()) {
            invalidRuleset(
                25,
                "the ruleset uses ${missing.joinToString(", ") { "$it (${Capabilities.REGISTRY[it]})" }} " +
                    "without declaring it",
            )
        }
    }
}
