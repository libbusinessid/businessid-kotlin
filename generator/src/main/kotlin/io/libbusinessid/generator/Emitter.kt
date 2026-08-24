// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.generator

import libbusinessid.ir.v1.Rules

/** How a program reads the values it does not receive as an operand. */
internal class Scope(val value: String, val subject: String, val target: String, val profile: String)

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
 * The membership entries of one group, checked to be in the order the packed
 * search reads them, and returned unchanged.
 *
 * `ir.md` says the values of `prefix_in` are sorted and deduplicated by the
 * compiler, and says so precisely so that an engine can search them without
 * reordering anything. It does not name the order; check 12 does, refusing any
 * list `compareUtf8` does not find sorted. UTF-8 byte order is code point order,
 * which is what the packed search compares, so a list that loads is already
 * arranged for it.
 *
 * Sorting here again would be free and would also be a lie: it would hide the
 * day the compiler, or check 12, stopped agreeing with the search. So this
 * checks and reports instead.
 */
internal fun orderedForSearch(where: String, entries: List<String>): List<String> {
    for (i in 1 until entries.size) {
        check(compareByCodePoint(entries[i - 1], entries[i]) < 0) {
            "$where: membership entries reached the emitter out of code point order, " +
                "${entries[i - 1]} before ${entries[i]}"
        }
    }
    return entries
}

internal fun compareByCodePoint(left: String, right: String): Int {
    val a = Cp.of(left)
    val b = Cp.of(right)
    for (i in 0 until minOf(a.size, b.size)) {
        val order = a[i] - b[i]
        if (order != 0) return order
    }
    return a.size - b.size
}

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
    private companion object {
        /** Below this, a code point has no printable spelling in a Kotlin literal. */
        const val FIRST_PRINTABLE = 0x20
        const val DELETE = 0x7F

        /**
         * How many elements one array literal may hold before it is split.
         *
         * Conservative: the heaviest element this emitter produces is a nested
         * `intArrayOf` of a few values, at roughly fifty bytes of bytecode, so
         * five hundred of them sit near ten per cent of the sixty-four kilobyte
         * method limit.
         */
        const val MAX_ELEMENTS_PER_METHOD = 500

        /**
         * Above this many entries, a prefix list is packed into a string
         * constant and looked up by binary search rather than walked.
         */
        const val MIN_PACKED_PREFIXES = 16
    }

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
        // Exactly one trailing newline per file: the emitted sources are read by
        // the same tools as the hand written ones.
        return linkedMapOf(
            "Canonicalizers.kt" to canonicalizers,
            "Checksums.kt" to checksums,
            "Constants.kt" to emitConstants(),
            "Formats.kt" to formats,
            "Ruleset.kt" to ruleset,
        ).mapValues { (_, content) -> content.trimEnd() + "\n" }
    }

    // -- constant pool -------------------------------------------------------

    private fun pool(key: String, declaration: (String) -> String): String {
        constantNames[key]?.let { return it }
        val name = "K${constantNames.size}"
        constantNames[key] = name
        constants[name] = declaration(name)
        return name
    }

    /**
     * Emits an array literal, split across helper functions when it is long.
     *
     * Every element of an array literal costs bytecode in the enclosing method,
     * and a file level `val` is initialised in the class initialiser, which the
     * JVM caps at sixty-four kilobytes. One long enough table stops the library
     * compiling — a ruleset that grew by two thousand entries is what found it.
     * Splitting the literal keeps each method far below the cap whatever a
     * future ruleset carries.
     */
    private fun arrayLiteral(name: String, type: String, factory: String, elements: List<String>): String {
        if (elements.size <= MAX_ELEMENTS_PER_METHOD) {
            return "internal val $name: $type = $factory(${elements.joinToString(", ")})"
        }
        val chunks = elements.chunked(MAX_ELEMENTS_PER_METHOD)
        return buildString {
            chunks.forEachIndexed { index, chunk ->
                appendLine("private fun ${name.lowercase()}p$index(): $type = $factory(${chunk.joinToString(", ")})")
            }
            val parts = chunks.indices.joinToString(" + ") { "${name.lowercase()}p$it()" }
            append("internal val $name: $type = $parts")
        }
    }

    private fun codePoints(text: String): String {
        val cp = Cp.of(text)
        return pool("cp:$text") { name ->
            arrayLiteral(name, "IntArray", "intArrayOf", cp.map { it.toString() })
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
            arrayLiteral(name, "IntArray", "intArrayOf", cp.map { it.toString() })
        }
    }

    private fun intList(values: List<Int>): String = pool("ints:" + values.joinToString(",")) { name ->
        "internal val $name: IntArray = intArrayOf(${values.joinToString(", ")})"
    }

    private fun longList(values: List<Long>): String = pool("longs:" + values.joinToString(",")) { name ->
        "internal val $name: LongArray = longArrayOf(${values.joinToString(", ") { "${it}L" }})"
    }

    private fun prefixList(values: List<String>): String = pool("prefixes:" + values.joinToString(" ")) { name ->
        arrayLiteral(
            name,
            "Array<IntArray>",
            "arrayOf",
            values.map { "intArrayOf(${Cp.of(it).joinToString(", ")})" },
        )
    }

    /** One group of equally shaped prefixes, packed end to end in the order given. */
    private fun packedPrefixes(values: List<String>): String = pool("packed:" + values.joinToString(" ")) { name ->
        "internal const val $name: String = ${quote(values.joinToString(""))}"
    }

    /**
     * Emits only the imports the body actually names. An import nothing uses is
     * noise in a file no one edits, and lint would have to be told to ignore it.
     */
    private fun withImports(body: String): String = buildString {
        append(header)
        for (import in runtimeImports) {
            val symbol = import.substringAfterLast('.')
            val used = Regex("\\b" + Regex.escape(symbol) + "\\b").containsMatchIn(body)
            if (used) appendLine("import $import")
        }
        appendLine()
        append(body)
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
                c.code < FIRST_PRINTABLE || c.code == DELETE -> append("\\u%04x".format(c.code))
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

            Rules.PredicateOpKind.PREDICATE_OP_KIND_PREFIX_IN -> prefixIn(str(0), op.valuesList)

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

    /**
     * `prefix_in(expr, prefixes)`.
     *
     * A short list is walked. A long one is grouped by shape — how many code
     * points an entry holds, and how many UTF-16 units, which differ only for an
     * entry reaching outside the Basic Multilingual Plane — and each group is
     * packed into one sorted string constant that a binary search reads. The
     * groups are combined with `||`, which keeps the short circuit the IR asks
     * for: the first group that matches decides.
     *
     * The membership lists a register publishes run to thousands of entries, and
     * emitted one array literal per entry they cost an allocation each before the
     * first call, a linear walk on every call, and eventually a class initialiser
     * the JVM refuses.
     */
    private fun prefixIn(value: String, values: List<String>): String {
        if (values.size < MIN_PACKED_PREFIXES) {
            return "Pred.prefixIn($value, ${prefixList(values)})"
        }
        val groups = values
            .groupBy { Cp.count(it) to it.length }
            .toSortedMap(compareBy({ it.first }, { it.second }))
        val tests = groups.map { (shape, entries) ->
            val (codePoints, stride) = shape
            // Sorted by code point, which is what the search compares. Kotlin's
            // own ordering is over UTF-16 units, and the two disagree above the
            // Basic Multilingual Plane: a surrogate pair starts at 0xD800 where
            // the code point it spells is above 0xFFFF, so an entry beginning
            // with U+FFFD sorts before a supplementary one by code point and
            // after it by code unit. Packed in that order the binary search
            // would walk past a member and answer that it is not one.
            val packed = packedPrefixes(orderedForSearch("prefix_in", entries))
            "Pred.prefixInPacked($value, $packed, $codePoints, $stride)"
        }
        return tests.joinToString(" || ", "(", ")")
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

    private val runtimeImports = listOf(
        "io.libbusinessid.ReasonCode",
        "io.libbusinessid.ValidationProfile",
        "io.libbusinessid.internal.EvalContext",
        "io.libbusinessid.runtime.Alignment",
        "io.libbusinessid.runtime.Arith",
        "io.libbusinessid.runtime.AssertionFailure",
        "io.libbusinessid.runtime.CanonBuffer",
        "io.libbusinessid.runtime.ChecksumOutcome",
        "io.libbusinessid.runtime.Ck",
        "io.libbusinessid.runtime.CpView",
        "io.libbusinessid.runtime.Pred",
        "io.libbusinessid.runtime.Txt",
    )

    private fun emitCanonicalizers(): String = withImports(
        buildString {
            for (p in programsOfKind(Rules.ProgramKind.PROGRAM_KIND_CANONICALIZATION)) {
                val body = StringBuilder()
                canonicalizationStatements(p, p.rootNode, CANON_SCOPE, "    ", body)
                appendLine("@Suppress(\"UNUSED_PARAMETER\")")
                appendLine("internal fun canon_${p.id}(b: CanonBuffer, profile: ValidationProfile, target: Int) {")
                append(body)
                appendLine("}")
                appendLine()
            }
        },
    )

    private fun emitFormats(): String = withImports(
        buildString {
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
        },
    )

    private fun emitChecksums(): String = withImports(
        buildString {
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
        },
    )

    private fun Scope.copyWithoutSubject(): Scope = Scope(value, "null", target, profile)

    private fun emitRuleset(): String = RulesetEmitter(bundle, this).emit(header)

    internal fun viewConstant(text: String): String = view(text)

    internal fun codePointConstant(text: String): String = codePoints(text)
}
