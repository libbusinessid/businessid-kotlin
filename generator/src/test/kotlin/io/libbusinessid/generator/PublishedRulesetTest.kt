// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.generator

import libbusinessid.ir.v1.Rules
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.MessageDigest
import java.util.Locale

/**
 * The published ruleset, measured.
 *
 * These figures are not decoration. The expansion count in particular is stated
 * in `ir.md` section 2, together with the wrong figure two engines reported when
 * they summed every capture instead of only the ones no other root reaches, so
 * it is a direct check that check 14 counts what the specification says.
 */
class PublishedRulesetTest {
    private val loaded by lazy { Loader.load(SpecFiles.rulesBundle) }

    private fun hex(bytes: ByteArray) = bytes.joinToString("") { String.format(Locale.ROOT, "%02x", it) }

    @Test
    fun `the pinned digests match the files under spec`() {
        val expected = mapOf(
            "rules_sha256" to "businessid-rules.binpb",
            "conformance_sha256" to "businessid-conformance.binpb",
            "conformance_jsonl_sha256" to "businessid-conformance.jsonl",
            "rules_proto_sha256" to "rules.proto",
            "conformance_proto_sha256" to "conformance.proto",
            "testee_proto_sha256" to "testee.proto",
            "ir_doc_sha256" to "ir.md",
            "features_doc_sha256" to "features.md",
        )
        for ((key, name) in expected) {
            val declared = requireNotNull(SpecFiles.lock[key]) { "rules.lock declares no $key" }
            val actual = hex(MessageDigest.getInstance("SHA-256").digest(SpecFiles.file(name).readBytes()))
            assertEquals(declared, actual, "$name does not match the $key that rules.lock declares")
        }
    }

    @Test
    fun `the ruleset version and format version match rules dot lock`() {
        assertEquals(SpecFiles.lock["rules_version"], loaded.rulesVersion)
        assertEquals(1, loaded.formatVersion)
        assertEquals(SpecFiles.lock["rules_version"], SpecFiles.conformance.rulesVersion)
    }

    @Test
    fun `the shape of the published ruleset`() {
        assertEquals(94, loaded.proto.identifiersCount, "identifier definitions")
        assertEquals(250, loaded.proto.programsCount, "programs")
        assertEquals(37, loaded.proto.dispatchersCount, "dispatchers")
        assertEquals(2386, loaded.proto.programsList.sumOf { it.nodesCount }, "IR nodes")
        // Check 23 makes this an identity: every definition is referenced by
        // exactly one target, and every target names an existing definition.
        assertEquals(94, loaded.proto.targets(), "dispatch targets")
        assertEquals(loaded.proto.identifiersCount, loaded.targets.size)
    }

    private fun Rules.RuleBundle.targets() = dispatchersList.sumOf { it.targetsCount }

    @Test
    fun `the ruleset declares every capability it uses, and uses every one it declares`() {
        assertEquals(
            loaded.proto.requiredFeatureIdsList.toList(),
            loaded.usedCapabilities.toList(),
            "declared and used capabilities differ",
        )
        assertEquals(18, loaded.usedCapabilities.size)
    }

    @Test
    fun `it uses fifty-two of the sixty-three opcodes`() {
        val used = mutableSetOf<String>()
        for (program in loaded.proto.programsList) {
            for (node in program.nodesList) used += Nodes.operationName(node)
        }
        assertEquals(52, used.size, "opcodes used: ${used.sorted()}")
        val total = Rules.StringOpKind.entries.size - 2 +
            Rules.IntegerOpKind.entries.size - 2 +
            Rules.PredicateOpKind.entries.size - 2 +
            Rules.CanonicalizationOpKind.entries.size - 2 +
            Rules.AssertionOpKind.entries.size - 2 +
            Rules.ChecksumOpKind.entries.size - 2 +
            Rules.CallOpKind.entries.size - 2
        assertEquals(63, total, "the IR declares sixty-three opcodes")
    }

    /**
     * `ir.md` section 2 states both numbers: 3069 is what the reachable-root
     * reading yields on the published ruleset, and 3204 is what two engines
     * reported by summing every capture, including the ones a root already
     * reaches.
     */
    @Test
    fun `the whole ruleset expands to three thousand and sixty-nine operation instances`() {
        var reachableRoots = 0L
        var everyCapture = 0L
        for (p in loaded.proto.programsList) {
            reachableRoots += instances(p, emissionRoots(p))
            val naive = buildList {
                add(p.rootNode)
                if (p.hasSubjectNode()) add(p.subjectNode)
                addAll(p.capturesList.map { it.node })
            }
            everyCapture += instances(p, naive)
        }
        assertEquals(3094L, reachableRoots)
        assertEquals(3229L, everyCapture, "the reading ir.md section 2 warns about")
        assertTrue(reachableRoots < Limits.MAX_STEPS, "the ruleset fits in the budget")
    }

    private fun emissionRoots(p: Rules.Program): List<Int> {
        val reached = HashSet<Int>()
        val roots = ArrayList<Int>()
        fun reach(index: Int) {
            if (!reached.add(index)) return
            for (operand in p.getNodes(index).inputNodesList) if (operand < index) reach(operand)
        }
        roots += p.rootNode
        reach(p.rootNode)
        if (p.hasSubjectNode()) {
            roots += p.subjectNode
            reach(p.subjectNode)
        }
        for (capture in p.capturesList.map { it.node }.sortedDescending()) {
            if (capture !in reached) {
                roots += capture
                reach(capture)
            }
        }
        return roots
    }

    private fun instances(p: Rules.Program, roots: List<Int>): Long {
        val memo = HashMap<Int, Long>()
        fun cost(index: Int): Long = memo.getOrPut(index) {
            1L + p.getNodes(index).inputNodesList.filter { it < index }.sumOf { cost(it) }
        }
        return roots.sumOf { cost(it) }
    }

    @Test
    fun `every program of the published ruleset stays far below the per program budget`() {
        val worst = loaded.proto.programsList.maxOf { instances(it, emissionRoots(it)) }
        assertEquals(118L, worst, "the largest program expansion")
    }
}
