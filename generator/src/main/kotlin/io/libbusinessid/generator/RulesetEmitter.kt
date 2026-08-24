// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.generator

/**
 * Emits the tables: kind dispatch, country aliases, prefixes, targets and the
 * per definition entry points.
 *
 * Every lookup is a `when`, which the Kotlin compiler turns into a switch on a
 * hash or on an integer. Nothing is constructed when the class initialises
 * beyond the few arrays the constants file declares, so a first call costs
 * nothing a later one does not.
 */
@Suppress("TooManyFunctions")
internal class RulesetEmitter(private val bundle: LoadedBundle, private val emitter: Emitter) {
    private val definitions = bundle.proto.identifiersList
    private val dispatchers = bundle.dispatchers

    private fun definitionIndex(id: Int): Int = bundle.definitionIndexById.getValue(id)

    fun emit(header: String): String = buildString {
        append(header)
        appendLine("import io.libbusinessid.Capability")
        appendLine("import io.libbusinessid.ReasonCode")
        appendLine("import io.libbusinessid.ValidationProfile")
        appendLine("import io.libbusinessid.internal.EvalContext")
        appendLine("import io.libbusinessid.runtime.AssertionFailure")
        appendLine("import io.libbusinessid.runtime.CanonBuffer")
        appendLine("import io.libbusinessid.runtime.ChecksumOutcome")
        appendLine("import io.libbusinessid.runtime.CpView")
        appendLine("import io.libbusinessid.runtime.Pred")
        appendLine()
        appendCountryHelpers()
        appendLine("/** The compiled ruleset: metadata and every table the pipeline reads. */")
        appendLine("internal object Ruleset {")
        appendMetadata()
        appendDispatcherOf()
        appendDispatcherKind()
        appendPreCanonicalize()
        appendCountryAlias()
        appendCountryTarget()
        appendPrefixTarget()
        appendGlobalTarget()
        appendUnprefixedTarget()
        appendDefinitionOf()
        appendTargetCountry()
        appendDefaultProfile()
        appendCanonicalize()
        appendFormat()
        appendChecksum()
        appendAbsentChecksumReason()
        appendLine("}")
    }

    private fun StringBuilder.appendCountryHelpers() {
        appendLine("/** The country of a dispatch target, absent for a GLOBAL target. */")
        appendLine("internal fun targetCountryView(target: Int): CpView? =")
        appendLine("    when (target) {")
        for (t in bundle.targets) {
            val country = t.countryCode ?: continue
            appendLine("        ${t.index} -> ${emitter.viewConstant(country)}")
        }
        appendLine("        else -> null")
        appendLine("    }")
        appendLine()
        appendLine("/**")
        appendLine(" * `prepend_country_if_missing()`: leaves the value alone when it already")
        appendLine(" * starts with one of the accepted prefixes of the selected target, and")
        appendLine(" * otherwise prepends its canonical prefix, or its country when it declares")
        appendLine(" * no canonical prefix.")
        appendLine(" */")
        appendLine("internal fun prependCountryIfMissing(target: Int, b: CanonBuffer) {")
        appendLine("    when (target) {")
        for (t in bundle.targets) {
            val country = t.countryCode ?: continue
            val prepend = if (t.proto.hasCanonicalPrefix()) t.proto.canonicalPrefix else country
            val guard = t.proto.acceptedPrefixesList.joinToString(" || ") {
                "b.startsWith(${emitter.codePointConstant(it)})"
            }
            val condition = if (guard.isEmpty()) "true" else "!($guard)"
            appendLine("        ${t.index} -> if ($condition) b.prepend(${emitter.codePointConstant(prepend)})")
        }
        appendLine("        else -> Unit")
        appendLine("    }")
        appendLine("}")
        appendLine()
    }

    private fun StringBuilder.appendMetadata() {
        appendLine("    const val RULES_VERSION: String = \"${bundle.rulesVersion}\"")
        appendLine("    const val FORMAT_VERSION: Int = ${bundle.formatVersion}")
        appendLine("    const val SOURCE_DIGEST: String = \"${bundle.sourceDigestHex}\"")
        appendLine()
        appendLine("    /** Every frozen capability this engine implements. */")
        appendLine("    val CAPABILITIES: List<Capability> = listOf(")
        for ((id, name) in Capabilities.REGISTRY) {
            appendLine("        Capability($id, \"$name\"),")
        }
        appendLine("    )")
        appendLine()
        appendLine("    /** Every kind the compiled ruleset can dispatch. */")
        appendLine("    val KINDS: List<String> = listOf(")
        for (d in dispatchers) appendLine("        \"${d.kind}\",")
        appendLine("    )")
        appendLine()
    }

    private fun StringBuilder.appendDispatcherOf() {
        appendLine("    /** The dispatcher of a normalised kind token, or -1. */")
        appendLine("    fun dispatcherOf(kind: String): Int =")
        appendLine("        when (kind) {")
        dispatchers.forEachIndexed { index, d ->
            val tokens = (listOf(d.kind) + d.kindAliasesList).joinToString(", ") { "\"$it\"" }
            appendLine("            $tokens -> $index")
        }
        appendLine("            else -> -1")
        appendLine("        }")
        appendLine()
    }

    private fun StringBuilder.appendDispatcherKind() {
        appendLine("    /** The canonical kind of a dispatcher. */")
        appendLine("    fun dispatcherKind(dispatcher: Int): String =")
        appendLine("        when (dispatcher) {")
        dispatchers.forEachIndexed { index, d -> appendLine("            $index -> \"${d.kind}\"") }
        appendLine("            else -> \"\"")
        appendLine("        }")
        appendLine()
    }

    private fun StringBuilder.appendPreCanonicalize() {
        appendLine("    /** The pre-canonicalisation program of a dispatcher; no target is selected yet. */")
        appendLine("    fun preCanonicalize(dispatcher: Int, b: CanonBuffer, profile: ValidationProfile) {")
        appendLine("        when (dispatcher) {")
        dispatchers.forEachIndexed { index, d ->
            appendLine("            $index -> canon_${d.preCanonicalizationProgram}(b, profile, -1)")
        }
        appendLine("            else -> Unit")
        appendLine("        }")
        appendLine("    }")
        appendLine()
    }

    private fun StringBuilder.appendCountryAlias() {
        appendLine("    /** The country alias table of a dispatcher, applied to a well formed token. */")
        appendLine("    fun countryAlias(dispatcher: Int, country: String): String =")
        appendLine("        when (dispatcher) {")
        dispatchers.forEachIndexed { index, d ->
            if (d.countryAliasesCount == 0) return@forEachIndexed
            appendLine("            $index ->")
            appendLine("                when (country) {")
            for (a in d.countryAliasesList) {
                appendLine("                    \"${a.alias}\" -> \"${a.countryCode}\"")
            }
            appendLine("                    else -> country")
            appendLine("                }")
        }
        appendLine("            else -> country")
        appendLine("        }")
        appendLine()
    }

    private fun StringBuilder.appendCountryTarget() {
        appendLine("    /** The target an explicit country selects in a dispatcher, or -1. */")
        appendLine("    fun countryTarget(dispatcher: Int, country: String): Int =")
        appendLine("        when (dispatcher) {")
        dispatchers.forEachIndexed { index, _ ->
            val owned = bundle.targets.filter { it.dispatcherIndex == index && !it.isGlobal }
            if (owned.isEmpty()) return@forEachIndexed
            appendLine("            $index ->")
            appendLine("                when (country) {")
            for (t in owned) appendLine("                    \"${t.countryCode}\" -> ${t.index}")
            appendLine("                    else -> -1")
            appendLine("                }")
        }
        appendLine("            else -> -1")
        appendLine("        }")
        appendLine()
    }

    private fun StringBuilder.appendPrefixTarget() {
        appendLine("    /** The target owning the longest declared prefix that starts the value, or -1. */")
        appendLine("    fun prefixTarget(dispatcher: Int, value: CpView): Int =")
        appendLine("        when (dispatcher) {")
        dispatchers.forEachIndexed { index, _ ->
            val owned = bundle.targets.filter { it.dispatcherIndex == index }
            val prefixes = owned.flatMap { t -> t.proto.acceptedPrefixesList.map { it to t.index } }
                .sortedWith(compareByDescending<Pair<String, Int>> { Cp.count(it.first) }.thenBy { it.first })
            if (prefixes.isEmpty()) return@forEachIndexed
            appendLine("            $index ->")
            appendLine("                when {")
            for ((prefix, target) in prefixes) {
                appendLine(
                    "                    Pred.startsWith(value, ${emitter.codePointConstant(prefix)}) -> $target",
                )
            }
            appendLine("                    else -> -1")
            appendLine("                }")
        }
        appendLine("            else -> -1")
        appendLine("        }")
        appendLine()
    }

    private fun StringBuilder.appendGlobalTarget() {
        appendLine("    /** The single GLOBAL target of a dispatcher, or -1. */")
        appendLine("    fun globalTarget(dispatcher: Int): Int =")
        appendLine("        when (dispatcher) {")
        for (t in bundle.targets) {
            if (t.isGlobal) appendLine("            ${t.dispatcherIndex} -> ${t.index}")
        }
        appendLine("            else -> -1")
        appendLine("        }")
        appendLine()
    }

    private fun StringBuilder.appendUnprefixedTarget() {
        appendLine("    /** The single target a dispatcher accepts without prefix and without country, or -1. */")
        appendLine("    fun unprefixedTarget(dispatcher: Int): Int =")
        appendLine("        when (dispatcher) {")
        for (t in bundle.targets) {
            if (t.proto.allowUnprefixedWithoutCountry) {
                appendLine("            ${t.dispatcherIndex} -> ${t.index}")
            }
        }
        appendLine("            else -> -1")
        appendLine("        }")
        appendLine()
    }

    private fun StringBuilder.appendDefinitionOf() {
        appendLine("    /** The definition a target routes to. */")
        appendLine("    fun definitionOf(target: Int): Int =")
        appendLine("        when (target) {")
        for (t in bundle.targets) {
            appendLine("            ${t.index} -> ${definitionIndex(t.proto.identifierDefinitionId)}")
        }
        appendLine("            else -> -1")
        appendLine("        }")
        appendLine()
    }

    private fun StringBuilder.appendTargetCountry() {
        appendLine("    /** The ISO country of a target, or null for a GLOBAL one. */")
        appendLine("    fun targetCountry(target: Int): String? =")
        appendLine("        when (target) {")
        for (t in bundle.targets) {
            val country = t.countryCode ?: continue
            appendLine("            ${t.index} -> \"$country\"")
        }
        appendLine("            else -> null")
        appendLine("        }")
        appendLine()
    }

    private fun StringBuilder.appendDefaultProfile() {
        val strict = definitions.withIndex()
            .filter { it.value.defaultProfile == "strict_current" }
            .map { it.index }
        appendLine("    /** The profile a definition applies when the caller states none. */")
        appendLine("    fun defaultProfile(definition: Int): ValidationProfile =")
        if (strict.isEmpty()) {
            appendLine("        // Every definition of this ruleset declares `compatible`.")
            appendLine("        ValidationProfile.COMPATIBLE")
        } else {
            appendLine("        when (definition) {")
            for (index in strict) appendLine("            $index -> ValidationProfile.STRICT_CURRENT")
            appendLine("            else -> ValidationProfile.COMPATIBLE")
            appendLine("        }")
        }
        appendLine()
    }

    private fun StringBuilder.appendCanonicalize() {
        appendLine("    /** The canonicalisation program of a definition, run on the pre-canonical value. */")
        appendLine("    fun canonicalize(definition: Int, target: Int, b: CanonBuffer, profile: ValidationProfile) {")
        appendLine("        when (definition) {")
        definitions.forEachIndexed { index, d ->
            appendLine("            $index -> canon_${d.canonicalizationProgram}(b, profile, target)")
        }
        appendLine("            else -> Unit")
        appendLine("        }")
        appendLine("    }")
        appendLine()
    }

    private fun StringBuilder.appendFormat() {
        appendLine("    /** The format program of a definition; null means every assertion held. */")
        appendLine("    fun format(definition: Int, ctx: EvalContext): AssertionFailure? =")
        appendLine("        when (definition) {")
        definitions.forEachIndexed { index, d ->
            val program = bundle.programsById.getValue(d.formatProgram)
            val subject = if (program.hasSubjectNode()) "fmt_${program.id}_subject(ctx)" else "ctx.value"
            appendLine("            $index -> fmt_${d.formatProgram}($subject, ctx)")
        }
        appendLine("            else -> null")
        appendLine("        }")
        appendLine()
    }

    private fun StringBuilder.appendChecksum() {
        appendLine("    /**")
        appendLine("     * The checksum program of a definition, or null when it declares none. A")
        appendLine("     * null answer is not a failure: the definition states why no algorithm")
        appendLine("     * applies, and [absentChecksumReason] carries that reason.")
        appendLine("     */")
        appendLine("    fun checksum(definition: Int, ctx: EvalContext): ChecksumOutcome? =")
        appendLine("        when (definition) {")
        definitions.forEachIndexed { index, d ->
            if (!d.hasChecksumProgram()) return@forEachIndexed
            val program = bundle.programsById.getValue(d.checksumProgram)
            val subject = if (program.hasSubjectNode()) "ck_${program.id}_subject(ctx)" else "ctx.value"
            appendLine("            $index -> ck_${d.checksumProgram}($subject, ctx)")
        }
        appendLine("            else -> null")
        appendLine("        }")
        appendLine()
    }

    private fun StringBuilder.appendAbsentChecksumReason() {
        appendLine("    /** Why a definition without a checksum program publishes no algorithm. */")
        appendLine("    fun absentChecksumReason(definition: Int): ReasonCode =")
        appendLine("        when (definition) {")
        definitions.forEachIndexed { index, d ->
            if (!d.hasAbsentChecksumReason()) return@forEachIndexed
            val name = d.absentChecksumReason.name.removePrefix("REASON_CODE_")
            appendLine("            $index -> ReasonCode.$name")
        }
        appendLine("            else -> ReasonCode.UNSUPPORTED_CHECKSUM")
        appendLine("        }")
    }
}
