// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.generator

import libbusinessid.ir.v1.Rules
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The eleven operations the published ruleset does not use, and the emitter
 * branches only they reach.
 *
 * A generator that only ever sees the current ruleset would ship eleven opcodes
 * nothing has ever run. Loading and emitting the kitchen sink is what makes them
 * real.
 */
class KitchenSinkTest {
    private val loaded by lazy { Loader.load(KitchenSink.bytes()) }
    private val emitted by lazy { Emitter(loaded).emit() }

    private fun all(): String = emitted.values.joinToString("\n")

    @Test
    fun `the kitchen sink passes every load check`() {
        assertEquals("2026.08.0", loaded.rulesVersion)
        assertEquals(3, loaded.proto.identifiersCount)
        assertEquals(3, loaded.targets.size)
    }

    @Test
    fun `it uses every one of the sixty-three operations`() {
        val used = mutableSetOf<String>()
        for (program in loaded.proto.programsList) {
            for (node in program.nodesList) used += Nodes.operationName(node)
        }
        val declared = buildSet {
            addAll(Rules.StringOpKind.entries.map { it.name })
            addAll(Rules.IntegerOpKind.entries.map { it.name })
            addAll(Rules.PredicateOpKind.entries.map { it.name })
            addAll(Rules.CanonicalizationOpKind.entries.map { it.name })
            addAll(Rules.AssertionOpKind.entries.map { it.name })
            addAll(Rules.ChecksumOpKind.entries.map { it.name })
            addAll(Rules.CallOpKind.entries.map { it.name })
        }.filterNot { it.endsWith("_UNSPECIFIED") || it == "UNRECOGNIZED" }.toSet()
        assertEquals(63, declared.size)
        assertEquals(emptySet<String>(), declared - used, "operations the kitchen sink still does not reach")
    }

    @Test
    fun `it declares every capability, and uses every one it declares`() {
        assertEquals(
            loaded.proto.requiredFeatureIdsList.toList(),
            loaded.usedCapabilities.toList(),
        )
        assertEquals(Capabilities.REGISTRY.keys.toList(), loaded.usedCapabilities.toList())
    }

    @Test
    fun `every string constructor maps to its primitive`() {
        val text = all()
        for (call in listOf(
            "Txt.slice(",
            "Txt.sliceFrom(",
            "Txt.sliceTo(",
            "Txt.beforeFirst(",
            "Txt.afterFirst(",
            "Txt.stripPrefix(",
            "Txt.concat(arrayOf<CpView?>(",
            "targetCountryView(ctx.target)",
            "ctx.value",
            "subject",
        )) {
            assertTrue(call in text, "the emitter never produced $call")
        }
    }

    @Test
    fun `every predicate maps to its primitive`() {
        val text = all()
        for (call in listOf(
            "Pred.isEmpty(",
            "Pred.isAbsent(",
            "Pred.equal(",
            "Pred.lengthEq(",
            "Pred.lengthIn(",
            "Pred.lengthBetween(",
            "Pred.asciiDigits(",
            "Pred.asciiUpperLetters(",
            "Pred.asciiAlphanumeric(",
            "Pred.asciiCharset(",
            "Pred.startsWith(",
            "Pred.endsWith(",
            "Pred.prefixIn(",
            "Pred.charAtIn(",
            "Pred.contains(",
            "Pred.integerIs(",
            "ctx.profile == ValidationProfile.STRICT_CURRENT",
        )) {
            assertTrue(call in text, "the emitter never produced $call")
        }
    }

    @Test
    fun `every integer and checksum operation maps to its primitive`() {
        val text = all()
        for (call in listOf(
            "Arith.digitsToInteger(",
            "Arith.modDigits(",
            "Arith.weightedSumDigits(",
            "Arith.weightedSumBase36(",
            "Arith.weightedSumAlphabet(",
            "Arith.modulo(",
            "Arith.complement(",
            "Arith.remainderMap(",
            "Alignment.LEFT",
            "Alignment.RIGHT",
            "Alignment.CYCLE",
            "Ck.luhn(",
            "Ck.iso7064Mod97(",
            "Ck.compareDigit(",
            "Ck.compareSlice(",
            "Ck.compareConstant(",
            "Ck.declaredUnsupported(ReasonCode.CHECKSUM_NOT_PUBLISHED",
            "Ck.allChecks(",
            "Ck.anyCheck(",
            "Ck.noBranch()",
        )) {
            assertTrue(call in text, "the emitter never produced $call")
        }
    }

    @Test
    fun `every canonicalisation step maps to its primitive`() {
        val text = emitted.getValue("Canonicalizers.kt")
        for (call in listOf(
            "b.trimWhitespace()",
            "b.removeWhitespace()",
            "b.uppercaseAscii()",
            "b.removeChars(",
            "b.replacePrefix(",
            "b.prepend(",
            "b.append(",
            "b.insert(",
            "b.leftPad(",
            "prependCountryIfMissing(target, b)",
            "if (Pred.lengthBetween(b.view()",
        )) {
            assertTrue(call in text, "the emitter never produced $call")
        }
    }

    @Test
    fun `a declared subject node becomes its own entry point`() {
        assertTrue("internal fun fmt_4_subject(ctx: EvalContext)" in emitted.getValue("Formats.kt"))
        assertTrue("internal fun ck_6_subject(ctx: EvalContext)" in emitted.getValue("Checksums.kt"))
        assertTrue("fmt_4(fmt_4_subject(ctx), ctx)" in emitted.getValue("Ruleset.kt"))
        assertTrue("ck_6(ck_6_subject(ctx), ctx)" in emitted.getValue("Ruleset.kt"))
        // A program without one is entered on the canonical value.
        assertTrue("fmt_3(ctx.value, ctx)" in emitted.getValue("Ruleset.kt"))
    }

    @Test
    fun `a call in each family becomes a call to the emitted callee`() {
        assertTrue("fmt_3(" in emitted.getValue("Formats.kt"))
        assertTrue("ck_5(" in emitted.getValue("Checksums.kt"))
    }

    @Test
    fun `the tables carry the aliases, the prefixes and the implicit target`() {
        val ruleset = emitted.getValue("Ruleset.kt")
        assertTrue("\"demo\", \"demo_alias\", \"demonstration\" -> 0" in ruleset)
        assertTrue("\"UK\" -> \"BE\"" in ruleset)
        assertTrue("unprefixedTarget" in ruleset)

        // The longest declared prefix is tested first, so PBE cannot be shadowed
        // by BE. The emitted code names pooled constants, so their lengths are
        // read back from the constants file.
        val lengths = Regex("""internal val (K\d+): IntArray = intArrayOf\(([^)]*)\)""")
            .findAll(emitted.getValue("Constants.kt"))
            .associate { it.groupValues[1] to it.groupValues[2].split(",").count { part -> part.isNotBlank() } }
        val tried = Regex("""Pred\.startsWith\(value, (K\d+)\)""")
            .findAll(ruleset.substringAfter("fun prefixTarget").substringBefore("fun globalTarget"))
            .map { lengths.getValue(it.groupValues[1]) }
            .toList()
        assertEquals(listOf(3, 2, 2), tried, "prefixes must be tried from the longest down")
    }

    @Test
    fun `a strict default profile reaches the table`() {
        assertTrue("ValidationProfile.STRICT_CURRENT" in emitted.getValue("Ruleset.kt"))
    }

    @Test
    fun `a definition without a checksum program keeps its absence reason`() {
        val ruleset = emitted.getValue("Ruleset.kt")
        assertTrue("ReasonCode.UNSUPPORTED_CHECKSUM" in ruleset)
        assertTrue("ReasonCode.CHECKSUM_NOT_PUBLISHED" in ruleset)
    }

    @Test
    fun `a GLOBAL target has no country to prepend and none to report`() {
        val ruleset = emitted.getValue("Ruleset.kt")
        val prepend = ruleset.substringAfter("fun prependCountryIfMissing").substringBefore("internal object Ruleset")
        assertTrue("2 ->" !in prepend, "the GLOBAL target must not appear in the prepend table")
    }

    @Test
    fun `emission is deterministic`() {
        val again = Emitter(Loader.load(KitchenSink.bytes())).emit()
        assertEquals(emitted, again)
    }

    @Test
    fun `a message key is escaped exactly as declared`() {
        assertTrue("\"kitchen.empty\"" in emitted.getValue("Formats.kt"))
        assertTrue("\"kitchen.digit\"" in emitted.getValue("Checksums.kt"))
        assertTrue("\"kitchen.unpublished\"" in emitted.getValue("Checksums.kt"))
    }

    @Test
    fun `ruleset data cannot escape the literal it is emitted into`() {
        // A message key is data: whatever it holds must come out as one Kotlin
        // string literal. A quote that ended the literal, a dollar that opened a
        // template, or a raw control character would each turn ruleset data into
        // ruleset-controlled code.
        val formats = emitted.getValue("Formats.kt")
        val line = formats.lines().single { "kitchen." in it && "quote" in it }
        assertTrue("""\"quote\"""" in line, line)
        assertTrue("""back\\slash""" in line, line)
        assertTrue("""\${'$'}dollar""" in line, line)
        assertTrue("""\u0007""" in line, line)
        // And nothing raw survives anywhere in what is emitted: no control
        // character, and no template a ruleset could have planted.
        for ((name, content) in emitted) {
            val control = content.filter { it.code < 0x20 && it != '\n' }
            assertEquals("", control, "$name holds a raw control character")
            assertTrue("\${'$'}{" !in content.replace("\\\${'$'}", ""), "$name holds a string template")
        }
    }

    @Test
    fun `a long membership list is packed into sorted string constants, one per shape`() {
        val formats = emitted.getValue("Formats.kt")
        val constants = emitted.getValue("Constants.kt")

        // Three shapes: (2 code points, 2 units), (1, 2) and (2, 3).
        val calls = Regex("""Pred\.prefixInPacked\([^,]+, (K\d+), (\d+), (\d+)\)""")
            .findAll(formats)
            .map { Triple(it.groupValues[1], it.groupValues[2].toInt(), it.groupValues[3].toInt()) }
            .toList()
        assertEquals(
            listOf(1 to 2, 2 to 2, 2 to 3),
            calls.map { it.second to it.third }.sortedWith(compareBy({ it.first }, { it.second })),
            "one call per shape, ordered by shape",
        )

        // Every packed constant holds a whole number of entries, and its entries
        // are in the code point order the binary search compares.
        for ((name, codePoints, stride) in calls) {
            val literal = Regex("""internal const val $name: String = "((?:[^"\\]|\\.)*)"""")
                .find(constants)
                ?.groupValues?.get(1)
                ?: error("$name is not a string constant")
            val decoded = unescape(literal)
            assertEquals(0, decoded.length % stride, "$name is not a whole number of entries")
            val entries = decoded.chunked(stride)
            assertEquals(
                entries.sortedWith { a, b -> compareCodePoints(a, b) },
                entries,
                "$name is not sorted by code point",
            )
            for (entry in entries) {
                assertEquals(codePoints, entry.codePointCount(0, entry.length), "an entry of $name")
                assertEquals(stride, entry.length, "an entry of $name")
            }
        }
    }

    @Test
    fun `a table too long for one array literal is split across methods`() {
        // Every element of an array literal costs bytecode in the enclosing
        // method, and a file level `val` is initialised in the class
        // initialiser, which the JVM caps at sixty-four kilobytes. A ruleset that
        // grew by two thousand entries is what found that; splitting keeps the
        // library compiling whatever a future one carries.
        val constants = emitted.getValue("Constants.kt")
        val chunks = Regex("""private fun k\d+p(\d+)\(\)""").findAll(constants).toList()
        assertTrue(chunks.isNotEmpty(), "no literal was split")
        // Each split constant is the concatenation of its parts.
        val joins = Regex("""internal val (K\d+): \S+ = (k\d+p\d+\(\)(?: \+ k\d+p\d+\(\))+)""")
            .findAll(constants)
            .toList()
        assertTrue(joins.isNotEmpty(), "a split literal is never put back together")
        for (join in joins) {
            val parts = Regex("""k\d+p(\d+)\(\)""").findAll(join.groupValues[2])
                .map { it.groupValues[1].toInt() }
                .toList()
            assertEquals(parts.indices.toList(), parts, "the parts of ${join.groupValues[1]} are out of order")
        }
    }

    @Test
    fun `a packed membership list escapes the ruleset data it carries`() {
        // The packed form turns membership entries into string literals, which
        // the array of arrays it replaced never did. Whatever a register
        // publishes has to survive that as data.
        val constants = emitted.getValue("Constants.kt")
        assertTrue("""\u0007""" in constants, "a control character is spelt, not carried")
        assertTrue("""\"""" in constants, "a quote is escaped")
        assertTrue("""\\""" in constants, "a backslash is escaped")
        assertTrue("""\${'$'}""" in constants, "a dollar is escaped")
    }

    private fun unescape(literal: String): String = buildString {
        var i = 0
        while (i < literal.length) {
            val c = literal[i]
            if (c != '\\') {
                append(c)
                i++
                continue
            }
            when (val next = literal[i + 1]) {
                'u' -> {
                    append(literal.substring(i + 2, i + 6).toInt(16).toChar())
                    i += 6
                }

                else -> {
                    append(next)
                    i += 2
                }
            }
        }
    }

    private fun compareCodePoints(left: String, right: String): Int {
        val a = left.codePoints().toArray()
        val b = right.codePoints().toArray()
        for (i in 0 until minOf(a.size, b.size)) {
            if (a[i] != b[i]) return a[i] - b[i]
        }
        return a.size - b.size
    }

    @Test
    fun `a choose whose first branch always applies emits no condition`() {
        val checksums = emitted.getValue("Checksums.kt")
        // The unconditional branch closes the chain, so nothing after it is
        // emitted and `Ck.noBranch()` is not reachable from it.
        assertTrue("Ck.luhn(" in checksums)
        assertTrue(checksums.contains("Ck.noBranch()"), "the guarded choose still needs its fallback")
    }
}
