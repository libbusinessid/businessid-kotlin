// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.generator

import libbusinessid.ir.v1.Rules
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * What `protobuf-javalite` actually does with the encodings the load checks care
 * about — measured, not assumed.
 *
 * `engine-kotlin.md` asks for this choice to be covered by tests, and the answer
 * decides an implementation: lite **preserves** an unknown field across a round
 * trip, but exposes no accessor to observe one, and `getUnknownFields()` does
 * not exist on a lite message at all. Check 5 needs to observe them, so the
 * bounded wire pre-scan of `Wire` is the implementation, exactly as `engine.md`
 * section 7.3 allows when the runtime does not expose them.
 */
class ProtobufRuntimeTest {
    /** A minimal, well formed encoding of a `RuleBundle` header. */
    private fun bundleHeader(): ByteArray = Rules.RuleBundle.newBuilder()
        .setFormatVersion(1)
        .setRulesVersion("2026.08.0")
        .build()
        .toByteArray()

    private fun varintKey(number: Int, wireType: Int): ByteArray {
        val key = (number shl 3) or wireType
        val out = ArrayList<Byte>()
        var v = key
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

    @Test
    fun `the lite runtime preserves an unknown field across a round trip`() {
        val extra = varintKey(1007, 0) + byteArrayOf(1)
        val original = bundleHeader() + extra

        val decoded = Rules.RuleBundle.parseFrom(original)
        val reencoded = decoded.toByteArray()

        assertTrue(
            reencoded.size > bundleHeader().size,
            "javalite dropped the unknown field: re-encoding lost ${original.size - reencoded.size} bytes",
        )
        assertTrue(
            reencoded.toList().windowed(extra.size).any { it == extra.toList() },
            "javalite kept some bytes but not the unknown field itself",
        )
    }

    @Test
    fun `the lite runtime exposes no way to read an unknown field`() {
        val decoded = Rules.RuleBundle.parseFrom(bundleHeader() + varintKey(1007, 0) + byteArrayOf(1))
        val accessors = decoded.javaClass.methods.map { it.name }
        assertFalse(
            "getUnknownFields" in accessors,
            "javalite now exposes getUnknownFields; the wire pre-scan could be replaced by it",
        )
    }

    @Test
    fun `the wire pre-scan sees the unknown field the runtime hides`() {
        val bytes = bundleHeader() + varintKey(1007, 0) + byteArrayOf(1)
        val findings = Wire.scan(bytes, Descriptors.RULE_BUNDLE)
        assertEquals(
            listOf(Wire.Finding.UnknownField("RuleBundle", 1007)),
            findings,
        )
    }

    @Test
    fun `an unrecognised enum value survives decoding as its number`() {
        // Check 2 must stay at the wire level: resolving an enum during decoding
        // would report a newer ruleset as malformed instead of as a version gap.
        val source = Rules.Source.newBuilder().setId("x").setTierValue(47).build()
        val decoded = Rules.Source.parseFrom(source.toByteArray())
        assertEquals(47, decoded.tierValue)
        assertEquals(Rules.SourceTier.UNRECOGNIZED, decoded.tier)
        assertEquals(null, Rules.SourceTier.forNumber(47))
    }

    @Test
    fun `an unknown operation opcode survives decoding as its number`() {
        val node = Rules.Node.newBuilder()
            .setOutputTypeValue(1)
            .setStringOperation(Rules.StringOperation.newBuilder().setKindValue(9999))
            .build()
        val decoded = Rules.Node.parseFrom(node.toByteArray())
        assertEquals(9999, decoded.stringOperation.kindValue)
        assertEquals(null, Rules.StringOpKind.forNumber(9999))
    }

    @Test
    fun `explicit presence tells an absent optional from an empty one`() {
        val absent = Rules.IntegerOperation.newBuilder()
            .setKind(Rules.IntegerOpKind.INTEGER_OP_KIND_WEIGHTED_SUM)
            .build()
        val empty = Rules.IntegerOperation.newBuilder()
            .setKind(Rules.IntegerOpKind.INTEGER_OP_KIND_WEIGHTED_SUM)
            .setAlphabet("")
            .build()
        assertFalse(Rules.IntegerOperation.parseFrom(absent.toByteArray()).hasAlphabet())
        assertTrue(Rules.IntegerOperation.parseFrom(empty.toByteArray()).hasAlphabet())
    }

    @Test
    fun `a truncated encoding fails to decode`() {
        val full = SpecFiles.rulesBundle
        val truncated = full.copyOf(full.size / 2)
        val failure = runCatching { Rules.RuleBundle.parseFrom(truncated) }.exceptionOrNull()
        assertTrue(
            failure is com.google.protobuf.InvalidProtocolBufferException,
            "a truncated ruleset must not decode, got $failure",
        )
    }
}
