// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.generator

import com.google.protobuf.DescriptorProtos
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * The hand written wire scan table against the schema protoc reads.
 *
 * The table exists because the lite runtime preserves unknown fields but exposes
 * no way to read them. It is only worth anything while it describes the real
 * schema, so this test compares it with the descriptor set protoc produces from
 * the very `rules.proto` that `rules.lock` pins.
 */
class DescriptorTableTest {
    private val messages: Map<String, DescriptorProtos.DescriptorProto> by lazy {
        val path = requireNotNull(System.getProperty("businessid.descriptor.set")) {
            "the test task must set businessid.descriptor.set"
        }
        DescriptorProtos.FileDescriptorSet.parseFrom(File(path).readBytes())
            .fileList
            .filter { it.getPackage() == "libbusinessid.ir.v1" }
            .flatMap { it.messageTypeList }
            .associateBy { it.name }
    }

    private val table = listOf(
        Descriptors.RULE_BUNDLE,
        Descriptors.IDENTIFIER_DEFINITION,
        Descriptors.SOURCE,
        Descriptors.IDENTIFIER_DISPATCHER,
        Descriptors.COUNTRY_ALIAS,
        Descriptors.DISPATCH_TARGET,
        Descriptors.PROGRAM,
        Descriptors.CAPTURE,
        Descriptors.NODE,
        Descriptors.STRING_OPERATION,
        Descriptors.INTEGER_OPERATION,
        Descriptors.PREDICATE_OPERATION,
        Descriptors.CANONICALIZATION_OPERATION,
        Descriptors.ASSERTION_OPERATION,
        Descriptors.CHECKSUM_OPERATION,
        Descriptors.CALL_OPERATION,
    )

    @Test
    fun `the table describes every message of the schema`() {
        assertEquals(messages.keys.sorted(), table.map { it.name }.sorted())
    }

    @Test
    fun `every field number, name and cardinality matches the schema`() {
        for (message in table) {
            val schema = requireNotNull(messages[message.name]) { "${message.name} is not in the schema" }
            assertEquals(
                schema.fieldList.map { it.number }.sorted(),
                message.fields.keys.sorted(),
                "field numbers of ${message.name}",
            )
            for (field in schema.fieldList) {
                val declared = requireNotNull(message.fields[field.number])
                assertEquals(field.name, declared.name, "name of ${message.name} field ${field.number}")
                val repeated = field.label == DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED
                assertEquals(repeated, declared.repeated, "cardinality of ${message.name}.${field.name}")
            }
        }
    }

    @Test
    fun `a nested message field points at the right message`() {
        for (message in table) {
            val schema = requireNotNull(messages[message.name])
            for (field in schema.fieldList) {
                val declared = requireNotNull(message.fields[field.number])
                val isMessage = field.type == DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE
                assertEquals(
                    isMessage,
                    declared.message != null,
                    "${message.name}.${field.name} is a message in the schema but not in the table, or the reverse",
                )
                if (isMessage) {
                    assertEquals(
                        field.typeName.substringAfterLast('.'),
                        declared.message?.name,
                        "${message.name}.${field.name} points at the wrong message",
                    )
                }
            }
        }
    }

    @Test
    fun `oneof membership matches the schema`() {
        for (message in table) {
            val schema = requireNotNull(messages[message.name])
            for (field in schema.fieldList) {
                val declared = requireNotNull(message.fields[field.number])
                // proto3 `optional` is modelled as a synthetic one-field oneof.
                // Only a real oneof, holding more than one field, is one the scan
                // must reject a second branch of.
                val realOneof = field.hasOneofIndex() &&
                    schema.fieldList.count { it.hasOneofIndex() && it.oneofIndex == field.oneofIndex } > 1
                assertEquals(
                    realOneof,
                    declared.oneof != null,
                    "${message.name}.${field.name} oneof membership",
                )
            }
        }
    }

    @Test
    fun `the reserved field number is absent from the table`() {
        val bundle = requireNotNull(messages["RuleBundle"])
        assertTrue(bundle.reservedRangeList.any { 5 in it.start until it.end }, "field 5 should be reserved")
        assertEquals(null, Descriptors.RULE_BUNDLE.fields[5], "a reserved number must read as unknown")
    }
}
