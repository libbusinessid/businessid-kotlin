// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("MagicNumber")
// The numbers here are the field numbers of `rules.proto`. Naming them would put
// a second name on something the schema already names, and DescriptorTableTest
// compares them against protoc's own descriptor set.

package org.entid.generator

/**
 * The shape of `rules.proto`, as the wire scan needs to know it.
 *
 * It records only what a scan uses: which field numbers exist, which are
 * repeated, which belong to a `oneof`, and which carry a nested message. Types
 * are deliberately absent — resolving a value is the job of a later check, not
 * of the scan.
 *
 * `DescriptorTableTest` compares this table against the descriptor set protoc
 * produces from the very schema `rules.lock` pins, so a schema change that this
 * file did not follow fails the build rather than passing silently.
 */
internal object Descriptors {
    class Field(
        val name: String,
        val number: Int,
        val repeated: Boolean = false,
        val message: Message? = null,
        val oneof: String? = null,
    )

    class Message(val name: String, vararg entries: Field) {
        val fields: Map<Int, Field> = entries.associateBy { it.number }
    }

    val SOURCE = Message(
        "Source",
        Field("id", 1),
        Field("url", 2),
        Field("authority", 3),
        Field("title", 4),
        Field("accessed_at", 5),
        Field("jurisdiction", 6),
        Field("language", 7),
        Field("notes", 8),
        Field("license_or_terms", 9),
        Field("archive_url", 10),
        Field("tier", 11),
    )

    val IDENTIFIER_DEFINITION = Message(
        "IdentifierDefinition",
        Field("id", 1),
        Field("kind", 2),
        Field("country_code", 3),
        Field("canonicalization_program", 4),
        Field("format_program", 5),
        Field("checksum_program", 6),
        Field("default_profile", 7),
        Field("sources", 8, repeated = true, message = SOURCE),
        Field("absent_checksum_reason", 9),
    )

    val COUNTRY_ALIAS = Message(
        "CountryAlias",
        Field("alias", 1),
        Field("country_code", 2),
    )

    val DISPATCH_TARGET = Message(
        "DispatchTarget",
        Field("country_code", 1),
        Field("accepted_prefixes", 2, repeated = true),
        Field("canonical_prefix", 3),
        Field("identifier_definition_id", 4),
        Field("allow_unprefixed_without_country", 5),
    )

    val IDENTIFIER_DISPATCHER = Message(
        "IdentifierDispatcher",
        Field("kind", 1),
        Field("kind_aliases", 2, repeated = true),
        Field("pre_canonicalization_program", 3),
        Field("country_aliases", 4, repeated = true, message = COUNTRY_ALIAS),
        Field("targets", 5, repeated = true, message = DISPATCH_TARGET),
    )

    val STRING_OPERATION = Message(
        "StringOperation",
        Field("kind", 1),
        Field("text", 2),
        Field("start", 3),
        Field("end", 4),
    )

    val INTEGER_OPERATION = Message(
        "IntegerOperation",
        Field("kind", 1),
        Field("modulus", 2),
        Field("weights", 3, repeated = true),
        Field("alignment", 4),
        Field("mapping", 5),
        Field("remainder_values", 6, repeated = true),
        Field("alphabet", 7),
    )

    val PREDICATE_OPERATION = Message(
        "PredicateOperation",
        Field("kind", 1),
        Field("text", 2),
        Field("values", 3, repeated = true),
        Field("lengths", 4, repeated = true),
        Field("length", 5),
        Field("min_length", 6),
        Field("max_length", 7),
        Field("index", 8),
        Field("constant", 9),
    )

    val CANONICALIZATION_OPERATION = Message(
        "CanonicalizationOperation",
        Field("kind", 1),
        Field("text", 2),
        Field("replacement", 3),
        Field("index", 4),
        Field("length", 5),
    )

    val ASSERTION_OPERATION = Message(
        "AssertionOperation",
        Field("kind", 1),
        Field("reason_code", 2),
        Field("message_key", 3),
    )

    val CHECKSUM_OPERATION = Message(
        "ChecksumOperation",
        Field("kind", 1),
        Field("index", 2),
        Field("start", 3),
        Field("end", 4),
        Field("reason_code", 5),
        Field("message_key", 6),
        Field("constant", 7),
    )

    val CALL_OPERATION = Message(
        "CallOperation",
        Field("kind", 1),
        Field("program_id", 2),
    )

    val NODE = Message(
        "Node",
        Field("output_type", 1),
        Field("input_nodes", 2, repeated = true),
        Field("string_operation", 10, message = STRING_OPERATION, oneof = "operation"),
        Field("integer_operation", 11, message = INTEGER_OPERATION, oneof = "operation"),
        Field("predicate_operation", 12, message = PREDICATE_OPERATION, oneof = "operation"),
        Field("canonicalization_operation", 13, message = CANONICALIZATION_OPERATION, oneof = "operation"),
        Field("assertion_operation", 14, message = ASSERTION_OPERATION, oneof = "operation"),
        Field("checksum_operation", 15, message = CHECKSUM_OPERATION, oneof = "operation"),
        Field("call_operation", 16, message = CALL_OPERATION, oneof = "operation"),
    )

    val CAPTURE = Message(
        "Capture",
        Field("name", 1),
        Field("node", 2),
    )

    val PROGRAM = Message(
        "Program",
        Field("id", 1),
        Field("kind", 2),
        Field("nodes", 3, repeated = true, message = NODE),
        Field("root_node", 4),
        Field("captures", 5, repeated = true, message = CAPTURE),
        Field("subject_node", 6),
    )

    val RULE_BUNDLE = Message(
        "RuleBundle",
        Field("format_version", 1),
        Field("rules_version", 2),
        Field("required_feature_ids", 3, repeated = true),
        Field("source_digest", 4),
        // Field 5 is reserved: `generated_at` was removed, and an encoding that
        // still carries it is an unknown field, which is the point.
        Field("identifiers", 6, repeated = true, message = IDENTIFIER_DEFINITION),
        Field("programs", 7, repeated = true, message = PROGRAM),
        Field("dispatchers", 8, repeated = true, message = IDENTIFIER_DISPATCHER),
    )
}
