// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.generator

import libbusinessid.ir.v1.Rules

/** How a program reads the values it does not receive as an operand. */
internal class Scope(
    val value: String,
    val subject: String,
    val target: String,
    val profile: String,
)

internal val FORMAT_SCOPE = Scope(
    value = "ctx.value",
    subject = "subject",
    target = "ctx.target",
    profile = "ctx.profile",
)

internal val CANON_SCOPE = Scope(
    value = "b.view()",
    subject = "null",
    target = "target",
    profile = "profile",
)

/**
 * Turns a validated ruleset into Kotlin.
 *
 * Operands are inlined where the graph reads them more than once, which check 14
 * has already bounded. Short circuits are preserved: `all`, `any` and the
 * assertion sequence stop at the first decisive operand, exactly as the IR says.
 *
 * Nothing is built when the emitted code starts. Dispatch is a `when`, character
 * sets and weight tables are read only arrays, and control flow is code.
 */
@Suppress("TooManyFunctions")
internal class Emitter(private val bundle: LoadedBundle) {
    private val constants = LinkedHashMap<String, String>()
    private val constantNames = LinkedHashMap<String, String>()

    private val header = buildString {
        appendLine("// Generated from businessid-rules.binpb ${bundle.rulesVersion}. Do not edit by hand:")
        appendLine("// run `./gradlew generateEngine`. `./gradlew checkGenerated` fails when this")
        appendLine("// file and the pinned bundle disagree.")
        appendLine("//")
        appendLine("// Copyright The LibBusinessID Authors.")
        appendLine("// SPDX-License-Identifier: Apache-2.0")
        appendLine()
        appendLine("@file:Suppress(\"ktlint\", \"MaxLineLength\", \"LongMethod\", \"CyclomaticComplexMethod\")")
        appendLine()
        appendLine("package io.libbusinessid.generated")
        appendLine()
    }

    fun emit(): Map<String, String> {
        // Programs are emitted first: doing so fills the constant pool that
        // Constants.kt then declares.
        val canonicalizers = emitCanonicalizers()
        val formats = emitFormats()
        val checksums = emitChecksums()
        val ruleset = emitRuleset()
        return linkedMapOf(
            "Canonicalizers.kt" to canonicalizers,
            "Checksums.kt" to checksums,
            "Constants.kt" to emitConstants(),
            "Formats.kt" to formats,
            "Ruleset.kt" to ruleset,
        )
    }

    // -- constant pool -------------------------------------------------------

    private fun pool(key: String, declaration: (String) -> String): String {
        constantNames[key]?.let { return it }
        val name = "K${constantNames.size}"
        constantNames[key] = name
        constants[name] = declaration(name)
        return name
    }

    private fun codePoints(text: String): String {
        val cp = Cp.of(text)
        return pool("cp:$text") { name ->
            "internal val $name: IntArray = intArrayOf(${cp.joinToString(", ")})"
        }
    }

    private fun view(text: String): String {
        val cp = Cp.of(text)
        return pool("view:$text") { name ->
            "internal val $name: CpView = CpView.of(intArrayOf(${cp.joinToString(", ")}))"
        }
    }

    private fun sortedSet(text: String): String {
        val cp = Cp.of(text).toSortedSet().toIntArray()
        return pool("set:" + cp.joinToString(",")) { name ->
            "internal val $name: IntArray = intArrayOf(${cp.joinToString(", ")})"
        }
    }

    private fun intList(values: List<Int>): String =
        pool("ints:" + values.joinToString(",")) { name ->
            "internal val $name: IntArray = intArrayOf(${values.joinToString(", ")})"
        }

    private fun longList(values: List<Long>): String =
        pool("longs:" + values.joinToString(",")) { name ->
            "internal val $name: LongArray = longArrayOf(${values.joinToString(", ") { "${it}L" }})"
        }

    private fun prefixList(values: List<String>): String =
        pool("prefixes:" + values.joinToString(" ")) { name ->
            val rows = values.joinToString(", ") { "intArrayOf(${Cp.of(it).joinToString(", ")})" }
            "internal val $name: Array<IntArray> = arrayOf($rows)"
        }

    private fun emitConstants(): String = buildString {
        append(header)
        appendLine("import io.libbusinessid.runtime.CpView")
        appendLine()
        appendLine("// Read only tables the emitted rules index into.")
        appendLine()
        for (declaration in constants.values) appendLine(declaration)
    }

    // -- expressions ---------------------------------------------------------

    private fun quote(s: String): String = buildString {
        append('"')
        for (c in s) {
            when {
                c == '"' -> append("\\\"")
                c == '\\' -> append("\\\\")
                c == '$' -> append("\\$")
                c.code < 0x20 || c.code == 0x7F -> append("\\u%04x".format(c.code))
                else -> append(c)
            }
        }
        append('"')
    }

    private fun optionalKey(has: Boolean, key: String): String = if (has) quote(key) else "null"

    private fun reason(value: Rules.ReasonCode): String = "ReasonCode." + value.name.removePrefix("REASON_CODE_")

    @Suppress("CyclomaticComplexMethod")
    private fun stringExpr(p: Rules.Program, index: Int, scope: Scope): String {
        val node = p.getNodes(index)
        val op = node.stringOperation
        fun operand(i: Int) = stringExpr(p, node.getInputNodes(i), scope)
        return when (op.kind) {
            Rules.StringOpKind.STRING_OP_KIND_CONSTANT -> view(op.text)
            Rules.StringOpKind.STRING_OP_KIND_VALUE -> scope.value
            Rules.StringOpKind.STRING_OP_KIND_SUBJECT -> scope.subject
            Rules.StringOpKind.STRING_OP_KIND_COUNTRY_CODE -> "targetCountryView(${scope.target})"
            Rules.StringOpKind.STRING_OP_KIND_SLICE -> "Txt.slice(${operand(0)}, ${op.start}, ${op.end})"
            Rules.StringOpKind.STRING_OP_KIND_SLICE_FROM -> "Txt.sliceFrom(${operand(0)}, ${op.start})"
            Rules.StringOpKind.STRING_OP_KIND_SLICE_TO -> "Txt.sliceTo(${operand(0)}, ${op.end})"
            Rules.StringOpKind.STRING_OP_KIND_BEFORE_FIRST ->
                "Txt.beforeFirst(${operand(0)}, ${codePoints(op.text)})"
            Rules.StringOpKind.STRING_OP_KIND_AFTER_FIRST ->
                "Txt.afterFirst(${operand(0)}, ${codePoints(op.text)})"
            Rules.StringOpKind.STRING_OP_KIND_STRIP_PREFIX ->
                "Txt.stripPrefix(${operand(0)}, ${codePoints(op.text)})"
            else -> {
                val parts = node.inputNodesList.indices.joinToString(", ") { operand(it) }
                "Txt.concat(arrayOf<CpView?>($parts))"
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun boolExpr(p: Rules.Program, index: Int, scope: Scope): String {
        val node = p.getNodes(index)
        val op = node.predicateOperation
        fun str(i: Int) = stringExpr(p, node.getInputNodes(i), scope)
        fun bool(i: Int) = boolExpr(p, node.getInputNodes(i), scope)
        return when (op.kind) {
            Rules.PredicateOpKind.PREDICATE_OP_KIND_IS_EMPTY -> "Pred.isEmpty(${str(0)})"
            Rules.PredicateOpKind.PREDICATE_OP_KIND_IS_ABSENT -> "Pred.isAbsent(${str(0)})"
            Rules.PredicateOpKind.PREDICATE_OP_KIND_EQUALS -> "Pred.equal(${str(0)}, ${str(1)})"
            Rules.PredicateOpKind.PREDICATE_OP_KIND_LENGTH_EQ -> "Pred.lengthEq(${str(0)}, ${op.length})"
            Rules.PredicateOpKind.PREDICATE_OP_KIND_LENGTH_IN ->
                "Pred.lengthIn(${str(0)}, ${intList(op.lengthsList)})"
            Rules.PredicateOpKind.PREDICATE_OP_KIND_LENGTH_BETWEEN ->
                "Pred.lengthBetween(${str(0)}, ${op.minLength}, ${op.maxLength})"
            Rules.PredicateOpKind.PREDICATE_OP_KIND_ASCII_DIGITS -> "Pred.asciiDigits(${str(0)})"
            Rules.PredicateOpKind.PREDICATE_OP_KIND_ASCII_UPPER_LETTERS -> "Pred.asciiUpperLetters(${str(0)})"
            Rules.PredicateOpKind.PREDICATE_OP_KIND_ASCII_ALPHANUMERIC -> "Pred.asciiAlphanumeric(${str(0)})"
            Rules.PredicateOpKind.PREDICATE_OP_KIND_ASCII_CHARSET ->
                "Pred.asciiCharset(${str(0)}, ${sortedSet(op.text)})"
            Rules.PredicateOpKind.PREDICATE_OP_KIND_STARTS_WITH ->
                "Pred.startsWith(${str(0)}, ${codePoints(op.text)})"
            Rules.PredicateOpKind.PREDICATE_OP_KIND_ENDS_WITH ->
                "Pred.endsWith(${str(0)}, ${codePoints(op.text)})"
            Rules.PredicateOpKind.PREDICATE_OP_KIND_PREFIX_IN ->
                "Pred.prefixIn(${str(0)}, ${prefixList(op.valuesList)})"
            Rules.PredicateOpKind.PREDICATE_OP_KIND_CHAR_AT_IN ->
                "Pred.charAtIn(${str(0)}, ${op.index}, ${sortedSet(op.text)})"
            Rules.PredicateOpKind.PREDICATE_OP_KIND_CONTAINS ->
                "Pred.contains(${str(0)}, ${codePoints(op.text)})"
            Rules.PredicateOpKind.PREDICATE_OP_KIND_ALL ->
                node.inputNodesList.indices.joinToString(" && ", "(", ")") { bool(it) }
            Rules.PredicateOpKind.PREDICATE_OP_KIND_ANY ->
                node.inputNodesList.indices.joinToString(" || ", "(", ")") { bool(it) }
            Rules.PredicateOpKind.PREDICATE_OP_KIND_NOT -> "!(${bool(0)})"
            Rules.PredicateOpKind.PREDICATE_OP_KIND_PROFILE_IS -> {
                val profile = if (op.text == "strict_current") "STRICT_CURRENT" else "COMPATIBLE"
                "(${scope.profile} == ValidationProfile.$profile)"
            }
            else -> "Pred.integerIs(${intExpr(p, node.getInputNodes(0), scope)}, ${op.constant}L)"
        }
    }

    private fun intExpr(p: Rules.Program, index: Int, scope: Scope): String {
        val node = p.getNodes(index)
        val op = node.integerOperation
        val operand = node.getInputNodes(0)
        return when (op.kind) {
            Rules.IntegerOpKind.INTEGER_OP_KIND_DIGITS_TO_INTEGER ->
                "Arith.digitsToInteger(${stringExpr(p, operand, scope)})"
            Rules.IntegerOpKind.INTEGER_OP_KIND_MOD_DIGITS ->
                "Arith.modDigits(${stringExpr(p, operand, scope)}, ${op.modulus}L)"
            Rules.IntegerOpKind.INTEGER_OP_KIND_WEIGHTED_SUM -> weightedSum(p, operand, op, scope)
            Rules.IntegerOpKind.INTEGER_OP_KIND_MODULO ->
                "Arith.modulo(${intExpr(p, operand, scope)}, ${op.modulus}L)"
            Rules.IntegerOpKind.INTEGER_OP_KIND_COMPLEMENT ->
                "Arith.complement(${intExpr(p, operand, scope)}, ${op.modulus}L)"
            else -> "Arith.remainderMap(${intExpr(p, operand, scope)}, ${longList(op.remainderValuesList)})"
        }
    }

    private fun weightedSum(p: Rules.Program, operand: Int, op: Rules.IntegerOperation, scope: Scope): String {
        val alignment = "Alignment." + op.alignment.name.removePrefix("WEIGHT_ALIGNMENT_")
        val weights = longList(op.weightsList)
        val value = stringExpr(p, operand, scope)
        return when (op.mapping) {
            Rules.CharMapping.CHAR_MAPPING_DIGIT_VALUE ->
                "Arith.weightedSumDigits($value, $weights, $alignment)"
            Rules.CharMapping.CHAR_MAPPING_ALNUM_BASE36 ->
                "Arith.weightedSumBase36($value, $weights, $alignment)"
            else ->
                "Arith.weightedSumAlphabet($value, $weights, $alignment, ${codePoints(op.alphabet)})"
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun checksumExpr(p: Rules.Program, index: Int, scope: Scope, indent: String): String {
        val node = p.getNodes(index)
        if (node.operationCase == Rules.Node.OperationCase.CALL_OPERATION) {
            val subject = stringExpr(p, node.getInputNodes(0), scope)
            return "ck_${node.callOperation.programId}($subject, ctx)"
        }
        val op = node.checksumOperation
        val key = optionalKey(op.hasMessageKey(), op.messageKey)
        fun str(i: Int) = stringExpr(p, node.getInputNodes(i), scope)
        fun int(i: Int) = intExpr(p, node.getInputNodes(i), scope)
        return when (op.kind) {
            Rules.ChecksumOpKind.CHECKSUM_OP_KIND_LUHN -> "Ck.luhn(${str(0)}, $key)"
            Rules.ChecksumOpKind.CHECKSUM_OP_KIND_ISO7064_MOD97_10 -> "Ck.iso7064Mod97(${str(0)}, $key)"
            Rules.ChecksumOpKind.CHECKSUM_OP_KIND_COMPARE_DIGIT ->
                "Ck.compareDigit(${int(0)}, ${str(1)}, ${op.index}, $key)"
            Rules.ChecksumOpKind.CHECKSUM_OP_KIND_COMPARE_SLICE ->
                "Ck.compareSlice(${int(0)}, ${str(1)}, ${op.start}, ${op.end}, $key)"
            Rules.ChecksumOpKind.CHECKSUM_OP_KIND_COMPARE_CONSTANT ->
                "Ck.compareConstant(${int(0)}, ${op.constant}L, $key)"
            Rules.ChecksumOpKind.CHECKSUM_OP_KIND_UNSUPPORTED ->
                "Ck.declaredUnsupported(${reason(op.reasonCode)}, $key)"
            Rules.ChecksumOpKind.CHECKSUM_OP_KIND_ALL_CHECKS ->
                branches("Ck.allChecks", p, node, scope, indent)
            Rules.ChecksumOpKind.CHECKSUM_OP_KIND_ANY_CHECK ->
                branches("Ck.anyCheck", p, node, scope, indent)
            else -> choose(p, node, scope, indent)
        }
    }

    private fun branches(call: String, p: Rules.Program, node: Rules.Node, scope: Scope, indent: String): String {
        val inner = "$indent    "
        val items = node.inputNodesList.joinToString(",\n$inner    ") { checksumExpr(p, it, scope, "$inner    ") }
        return "$call(\n${inner}arrayOf<ChecksumOutcome>(\n$inner    $items,\n$inner)\n$indent)"
    }

    /**
     * `choose(branches...)`: the first applicable branch wins. A `when` branch
     * whose predicate is false is not applicable; any other branch always is, so
     * it closes the chain and anything after it can never be reached.
     */
    private fun choose(p: Rules.Program, node: Rules.Node, scope: Scope, indent: String): String {
        val inner = "$indent    "
        val conditional = ArrayList<Pair<String, String>>()
        var fallback: String? = null
        for (branch in node.inputNodesList) {
            val branchNode = p.getNodes(branch)
            if (Nodes.isChecksumWhen(branchNode)) {
                conditional += boolExpr(p, branchNode.getInputNodes(0), scope) to
                    checksumExpr(p, branchNode.getInputNodes(1), scope, inner)
            } else {
                fallback = checksumExpr(p, branch, scope, inner)
                break
            }
        }
        val tail = fallback ?: "Ck.noBranch()"
        if (conditional.isEmpty()) return tail
        return buildString {
            for ((predicate, outcome) in conditional) {
                append("if ($predicate) {\n$inner$outcome\n$indent} else ")
            }
            append("{\n$inner$tail\n$indent}")
        }
    }

    private fun assertionStatements(p: Rules.Program, index: Int, scope: Scope, out: StringBuilder) {
        val node = p.getNodes(index)
        if (node.operationCase == Rules.Node.OperationCase.CALL_OPERATION) {
            val subject = stringExpr(p, node.getInputNodes(0), scope)
            out.appendLine("    fmt_${node.callOperation.programId}($subject, ctx)?.let { return it }")
            return
        }
        val op = node.assertionOperation
        if (op.kind == Rules.AssertionOpKind.ASSERTION_OP_KIND_SEQUENCE) {
            for (operand in node.inputNodesList) assertionStatements(p, operand, scope, out)
            return
        }
        val predicate = boolExpr(p, node.getInputNodes(0), scope)
        val key = optionalKey(op.hasMessageKey(), op.messageKey)
        out.appendLine("    if (!($predicate)) return AssertionFailure(${reason(op.reasonCode)}, $key)")
    }

    @Suppress("CyclomaticComplexMethod")
    private fun canonicalizationStatements(
        p: Rules.Program,
        index: Int,
        scope: Scope,
        indent: String,
        out: StringBuilder,
    ) {
        val node = p.getNodes(index)
        val op = node.canonicalizationOperation
        when (op.kind) {
            Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_SEQUENCE ->
                for (operand in node.inputNodesList) canonicalizationStatements(p, operand, scope, indent, out)
            Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_TRIM_WHITESPACE ->
                out.appendLine("${indent}b.trimWhitespace()")
            Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_REMOVE_WHITESPACE ->
                out.appendLine("${indent}b.removeWhitespace()")
            Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_UPPERCASE_ASCII ->
                out.appendLine("${indent}b.uppercaseAscii()")
            Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_REMOVE_CHARS ->
                out.appendLine("${indent}b.removeChars(${sortedSet(op.text)})")
            Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_REPLACE_PREFIX ->
                out.appendLine("${indent}b.replacePrefix(${codePoints(op.text)}, ${codePoints(op.replacement)})")
            Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_PREPEND ->
                out.appendLine("${indent}b.prepend(${codePoints(op.text)})")
            Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_APPEND ->
                out.appendLine("${indent}b.append(${codePoints(op.text)})")
            Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_INSERT ->
                out.appendLine("${indent}b.insert(${op.index}, ${codePoints(op.text)})")
            Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_LEFT_PAD ->
                out.appendLine("${indent}b.leftPad(${op.length}, ${Cp.of(op.text)[0]})")
            Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_PREPEND_COUNTRY_IF_MISSING ->
                out.appendLine("${indent}prependCountryIfMissing(${scope.target}, b)")
            else -> {
                val predicate = boolExpr(p, node.getInputNodes(0), scope)
                out.appendLine("${indent}if ($predicate) {")
                for (i in 1 until node.inputNodesCount) {
                    canonicalizationStatements(p, node.getInputNodes(i), scope, "$indent    ", out)
                }
                out.appendLine("$indent}")
            }
        }
    }

    // -- files ---------------------------------------------------------------

    private fun programsOfKind(kind: Rules.ProgramKind): List<Rules.Program> =
        bundle.proto.programsList.filter { it.kind == kind }

    private fun emitCanonicalizers(): String = buildString {
        append(header)
        appendLine("import io.libbusinessid.ValidationProfile")
        appendLine("import io.libbusinessid.runtime.Ascii")
        appendLine("import io.libbusinessid.runtime.CanonBuffer")
        appendLine("import io.libbusinessid.runtime.CpView")
        appendLine("import io.libbusinessid.runtime.Pred")
        appendLine("import io.libbusinessid.runtime.Txt")
        appendLine()
        for (p in programsOfKind(Rules.ProgramKind.PROGRAM_KIND_CANONICALIZATION)) {
            val body = StringBuilder()
            canonicalizationStatements(p, p.rootNode, CANON_SCOPE, "    ", body)
            appendLine("@Suppress(\"UNUSED_PARAMETER\")")
            appendLine("internal fun canon_${p.id}(b: CanonBuffer, profile: ValidationProfile, target: Int) {")
            append(body)
            appendLine("}")
            appendLine()
        }
    }

    private fun emitFormats(): String = buildString {
        append(header)
        appendLine("import io.libbusinessid.ReasonCode")
        appendLine("import io.libbusinessid.ValidationProfile")
        appendLine("import io.libbusinessid.internal.EvalContext")
        appendLine("import io.libbusinessid.runtime.Arith")
        appendLine("import io.libbusinessid.runtime.AssertionFailure")
        appendLine("import io.libbusinessid.runtime.CpView")
        appendLine("import io.libbusinessid.runtime.Pred")
        appendLine("import io.libbusinessid.runtime.Txt")
        appendLine()
        for (p in programsOfKind(Rules.ProgramKind.PROGRAM_KIND_FORMAT)) {
            val body = StringBuilder()
            assertionStatements(p, p.rootNode, FORMAT_SCOPE, body)
            appendLine("@Suppress(\"UNUSED_PARAMETER\")")
            appendLine("internal fun fmt_${p.id}(subject: CpView?, ctx: EvalContext): AssertionFailure? {")
            append(body)
            appendLine("    return null")
            appendLine("}")
            appendLine()
            if (p.hasSubjectNode()) {
                appendLine("internal fun fmt_${p.id}_subject(ctx: EvalContext): CpView? =")
                appendLine("    " + stringExpr(p, p.subjectNode, FORMAT_SCOPE.copyWithoutSubject()))
                appendLine()
            }
        }
    }

    private fun emitChecksums(): String = buildString {
        append(header)
        appendLine("import io.libbusinessid.ReasonCode")
        appendLine("import io.libbusinessid.ValidationProfile")
        appendLine("import io.libbusinessid.internal.EvalContext")
        appendLine("import io.libbusinessid.runtime.Alignment")
        appendLine("import io.libbusinessid.runtime.Arith")
        appendLine("import io.libbusinessid.runtime.ChecksumOutcome")
        appendLine("import io.libbusinessid.runtime.Ck")
        appendLine("import io.libbusinessid.runtime.CpView")
        appendLine("import io.libbusinessid.runtime.Pred")
        appendLine("import io.libbusinessid.runtime.Txt")
        appendLine()
        for (p in programsOfKind(Rules.ProgramKind.PROGRAM_KIND_CHECKSUM)) {
            appendLine("@Suppress(\"UNUSED_PARAMETER\")")
            appendLine("internal fun ck_${p.id}(subject: CpView?, ctx: EvalContext): ChecksumOutcome =")
            appendLine("    " + checksumExpr(p, p.rootNode, FORMAT_SCOPE, "    "))
            appendLine()
            if (p.hasSubjectNode()) {
                appendLine("internal fun ck_${p.id}_subject(ctx: EvalContext): CpView? =")
                appendLine("    " + stringExpr(p, p.subjectNode, FORMAT_SCOPE.copyWithoutSubject()))
                appendLine()
            }
        }
    }

    private fun Scope.copyWithoutSubject(): Scope = Scope(value, "null", target, profile)

    private fun emitRuleset(): String = RulesetEmitter(bundle, this).emit(header)

    internal fun viewConstant(text: String): String = view(text)

    internal fun codePointConstant(text: String): String = codePoints(text)
}
