// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.coverage

import io.libbusinessid.BusinessIdEngine
import io.libbusinessid.IdentifierInput
import io.libbusinessid.IdentifierKind
import io.libbusinessid.ValidationOptions
import io.libbusinessid.ValidationProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Replays the inputs of the shared corpus so the emitted rules are measured.
 *
 * **This judges nothing.** It is not a conformance suite and it is not a runner:
 * the only program that reads an expected result is the runner that lives in the
 * `spec` repository, and this file is structurally incapable of reading one. The
 * reader below decodes five field numbers of a conformance case — the kind, the
 * country, the input, the profile and the operation — and no others. Field 8,
 * which carries the expectation, is skipped like any field it does not know.
 *
 * It exists because `engine.md` section 12.2 asks for the coverage of the
 * emitted code to be measured and published, and the conformance run itself
 * happens in another process where no coverage agent is attached. What that
 * figure describes is the corpus, not the engine, which is exactly why it is
 * reported and never turned into a threshold.
 */
class EmittedCoverageDriverTest {
    private val engine = BusinessIdEngine.default()

    private class Case(
        val kind: String,
        val country: String?,
        val input: String,
        val profile: String?,
        val operation: Int,
    )

    private fun readVarint(b: ByteArray, from: Int): Pair<Long, Int> {
        var result = 0L
        var shift = 0
        var i = from
        while (true) {
            val byte = b[i].toInt()
            i++
            result = result or ((byte.toLong() and 0x7F) shl shift)
            if (byte and 0x80 == 0) return result to i
            shift += 7
        }
    }

    /** Yields `(fieldNumber, wireType, start, end)` for every field of a message. */
    private fun fields(b: ByteArray, from: Int, to: Int): Sequence<IntArray> = sequence {
        var i = from
        while (i < to) {
            val (key, afterKey) = readVarint(b, i)
            val number = (key ushr 3).toInt()
            val wireType = (key and 7L).toInt()
            i = afterKey
            when (wireType) {
                0 -> {
                    val (_, next) = readVarint(b, i)
                    yield(intArrayOf(number, wireType, i, next))
                    i = next
                }

                1 -> {
                    yield(intArrayOf(number, wireType, i, i + 8))
                    i += 8
                }

                5 -> {
                    yield(intArrayOf(number, wireType, i, i + 4))
                    i += 4
                }

                2 -> {
                    val (length, afterLength) = readVarint(b, i)
                    val end = afterLength + length.toInt()
                    yield(intArrayOf(number, wireType, afterLength, end))
                    i = end
                }

                else -> error("wire type $wireType")
            }
        }
    }

    private fun cases(): List<Case> {
        val root = File(requireNotNull(System.getProperty("businessid.spec.dir")))
        val bytes = File(root, "businessid-conformance.binpb").readBytes()
        val out = ArrayList<Case>()
        for (field in fields(bytes, 0, bytes.size)) {
            if (field[0] != 4) continue
            var kind = ""
            var country: String? = null
            var input = ""
            var profile: String? = null
            var operation = 0
            for (inner in fields(bytes, field[2], field[3])) {
                val text = { String(bytes, inner[2], inner[3] - inner[2], Charsets.UTF_8) }
                when (inner[0]) {
                    3 -> kind = text()

                    4 -> country = text()

                    5 -> input = text()

                    6 -> profile = text()

                    7 -> operation = readVarint(bytes, inner[2]).first.toInt()

                    // Every other field number, field 8 included, is skipped.
                    else -> Unit
                }
            }
            out += Case(kind, country, input, profile, operation)
        }
        return out
    }

    @Test
    fun `every input of the corpus goes through the engine`() {
        val all = cases()
        // Counted from the JSONL rather than repeated here: it is one reviewed
        // case per line and an independent rendering of the same corpus, so it
        // cross checks the varint walk above instead of merely agreeing with a
        // number a resync would have to move by hand.
        val jsonl = File(System.getProperty("businessid.spec.dir"), "businessid-conformance.jsonl")
            .readLines()
            .filter { it.isNotBlank() }
        assertEquals(jsonl.size, all.size, "the binpb and the JSONL disagree on how many cases there are")
        var driven = 0
        for (case in all) {
            // Operation 5 is the ruleset case, which addresses the generator.
            if (case.operation == 5 || case.operation == 0) continue
            val input = IdentifierInput(IdentifierKind(case.kind), case.input, case.country)
            val options = ValidationOptions(
                case.profile?.let { name -> ValidationProfile.entries.firstOrNull { it.wireName == name } },
            )
            when (case.operation) {
                1 -> engine.canonicalize(input, options)
                2 -> engine.validateFormat(input, options)
                3 -> engine.validateChecksum(input, options)
                else -> engine.validate(input, options)
            }
            driven++
        }
        val business = jsonl.count { "\"operation\":\"load_ruleset\"" !in it }
        assertEquals(business, driven, "the business operations of the corpus")
    }

    @Test
    fun `this driver decodes no field that could carry an expectation`() {
        val source = File("src/test/kotlin/io/libbusinessid/coverage/EmittedCoverageDriverTest.kt")
        assertTrue(source.isFile, "the driver should find its own source at ${source.absolutePath}")
        val body = source.readText().substringAfter("private fun cases()").substringBefore("@Test")
        val decoded = Regex("""^\s+(\d+) ->""", RegexOption.MULTILINE)
            .findAll(body)
            .map { it.groupValues[1].toInt() }
            .toSortedSet()
        assertEquals(
            setOf(3, 4, 5, 6, 7),
            decoded,
            "the driver decodes a conformance field beyond kind, country, input, profile and operation",
        )
    }
}
