// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.generator

import libbusinessid.ir.v1.Rules
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The bounded wire scan: what it sees, and what it lets through.
 *
 * Two of the three things it looks for are invisible to any Protobuf runtime — a
 * singular field encoded twice, and two branches of one `oneof` — so nothing but
 * this scan can report them.
 */
class WireScanTest {
    private fun key(number: Int, wireType: Int): ByteArray {
        val out = ArrayList<Byte>()
        var v = (number shl 3) or wireType
        while (true) {
            val b = v and 0x7F
            v = v ushr 7
            if (v == 0) {
                out += b.toByte()
                break
            }
            out += (b or 0x80).toByte()
        }
        return out.toByteArray()
    }

    private fun lengthDelimited(number: Int, payload: ByteArray): ByteArray {
        val header = key(number, Wire.LENGTH_DELIMITED)
        val size = ArrayList<Byte>()
        var v = payload.size
        while (true) {
            val b = v and 0x7F
            v = v ushr 7
            if (v == 0) {
                size += b.toByte()
                break
            }
            size += (b or 0x80).toByte()
        }
        return header + size.toByteArray() + payload
    }

    @Test
    fun `the published ruleset carries no finding at all`() {
        assertEquals(emptyList<Wire.Finding>(), Wire.scan(SpecFiles.rulesBundle, Descriptors.RULE_BUNDLE))
    }

    @Test
    fun `an unknown field is reported with its path`() {
        val bytes = Fixtures.bytes(Fixtures.valid()) + key(1007, Wire.VARINT) + byteArrayOf(1)
        assertEquals(
            listOf(Wire.Finding.UnknownField("RuleBundle", 1007)),
            Wire.scan(bytes, Descriptors.RULE_BUNDLE),
        )
    }

    @Test
    fun `an unknown field nested six levels deep is reported`() {
        // RuleBundle.programs.nodes.string_operation, with a field the schema
        // does not declare.
        val operation = key(1, Wire.VARINT) + byteArrayOf(2) + key(77, Wire.VARINT) + byteArrayOf(9)
        val node = key(1, Wire.VARINT) + byteArrayOf(1) + lengthDelimited(10, operation)
        val program = key(1, Wire.VARINT) + byteArrayOf(9) + lengthDelimited(3, node)
        val bundle = lengthDelimited(7, program)
        val findings = Wire.scan(bundle, Descriptors.RULE_BUNDLE)
        assertEquals(
            listOf(Wire.Finding.UnknownField("RuleBundle.programs.nodes.string_operation", 77)),
            findings,
        )
    }

    @Test
    fun `a reserved field number reads as unknown`() {
        val bytes = Fixtures.bytes(Fixtures.valid()) + key(5, Wire.VARINT) + byteArrayOf(1)
        assertEquals(
            listOf(Wire.Finding.UnknownField("RuleBundle", 5)),
            Wire.scan(bytes, Descriptors.RULE_BUNDLE),
        )
    }

    @Test
    fun `a singular field encoded twice is reported`() {
        val bytes = Fixtures.bytes(Fixtures.valid()) + key(1, Wire.VARINT) + byteArrayOf(1)
        assertEquals(
            listOf(Wire.Finding.RepeatedSingular("RuleBundle", 1)),
            Wire.scan(bytes, Descriptors.RULE_BUNDLE),
        )
    }

    @Test
    fun `two branches of one oneof are reported`() {
        val string = key(1, Wire.VARINT) + byteArrayOf(2)
        val integer = key(1, Wire.VARINT) + byteArrayOf(1)
        val node = key(1, Wire.VARINT) + byteArrayOf(1) +
            lengthDelimited(10, string) +
            lengthDelimited(11, integer)
        val program = key(1, Wire.VARINT) + byteArrayOf(9) + lengthDelimited(3, node)
        val findings = Wire.scan(lengthDelimited(7, program), Descriptors.RULE_BUNDLE)
        assertEquals(
            listOf(Wire.Finding.TwoOneofBranches("RuleBundle.programs.nodes", 10, 11)),
            findings,
        )
    }

    @Test
    fun `a repeated field encoded many times is not a finding`() {
        val bundle = Fixtures.valid()
            .addRequiredFeatureIds(42)
            .addIdentifiers(Rules.IdentifierDefinition.newBuilder(Fixtures.valid().getIdentifiers(0)).setId(2))
        assertEquals(emptyList<Wire.Finding>(), Wire.scan(Fixtures.bytes(bundle), Descriptors.RULE_BUNDLE))
    }

    @Test
    fun `a packed repeated numeric field is not a finding`() {
        // required_feature_ids is packed by default; the scan must accept both
        // the packed form and a run of individual varints.
        val unpacked = key(3, Wire.VARINT) + byteArrayOf(1) + key(3, Wire.VARINT) + byteArrayOf(2)
        assertEquals(emptyList<Wire.Finding>(), Wire.scan(unpacked, Descriptors.RULE_BUNDLE))
    }

    @Test
    fun `a truncated length prefix is reported rather than crashing`() {
        val findings = Wire.scan(byteArrayOf(0x3A), Descriptors.RULE_BUNDLE)
        assertTrue(findings.single() is Wire.Finding.Malformed, "got $findings")
    }

    @Test
    fun `a length running past the message is reported`() {
        val bytes = key(7, Wire.LENGTH_DELIMITED) + byteArrayOf(0x7F)
        val findings = Wire.scan(bytes, Descriptors.RULE_BUNDLE)
        assertTrue(findings.single() is Wire.Finding.Malformed, "got $findings")
    }

    @Test
    fun `the group wire types are reported`() {
        val findings = Wire.scan(key(1, Wire.START_GROUP), Descriptors.RULE_BUNDLE)
        assertTrue(findings.single() is Wire.Finding.Malformed, "got $findings")
    }

    @Test
    fun `field number zero is reported`() {
        val findings = Wire.scan(byteArrayOf(0x00), Descriptors.RULE_BUNDLE)
        assertTrue(findings.single() is Wire.Finding.Malformed, "got $findings")
    }

    @Test
    fun `arbitrary bytes never throw`() {
        val random = java.util.Random(20260824)
        repeat(2000) {
            val bytes = ByteArray(random.nextInt(64)).also { random.nextBytes(it) }
            Wire.scan(bytes, Descriptors.RULE_BUNDLE)
        }
    }
}
