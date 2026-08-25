// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

package org.entid.generator

import entid.ir.v1.Rules

/** The role a program plays, which constrains its shape. */
internal enum class ProgramRole {
    PRE_CANONICALIZATION,
    DEFINITION_CANONICALIZATION,
    GLOBAL_DEFINITION_CANONICALIZATION,
    FORMAT,
    CHECKSUM,
}

/** Helpers over a node, kept in one place so no check reimplements them. */
internal object Nodes {
    fun operationName(node: Rules.Node): String = when (node.operationCase) {
        Rules.Node.OperationCase.STRING_OPERATION -> node.stringOperation.kind.name
        Rules.Node.OperationCase.INTEGER_OPERATION -> node.integerOperation.kind.name
        Rules.Node.OperationCase.PREDICATE_OPERATION -> node.predicateOperation.kind.name
        Rules.Node.OperationCase.CANONICALIZATION_OPERATION -> node.canonicalizationOperation.kind.name
        Rules.Node.OperationCase.ASSERTION_OPERATION -> node.assertionOperation.kind.name
        Rules.Node.OperationCase.CHECKSUM_OPERATION -> node.checksumOperation.kind.name
        Rules.Node.OperationCase.CALL_OPERATION -> node.callOperation.kind.name
        Rules.Node.OperationCase.OPERATION_NOT_SET -> "<absent>"
        else -> "<absent>"
    }

    fun isChecksumWhen(node: Rules.Node): Boolean = node.operationCase == Rules.Node.OperationCase.CHECKSUM_OPERATION &&
        node.checksumOperation.kind == Rules.ChecksumOpKind.CHECKSUM_OP_KIND_WHEN

    fun isSubject(node: Rules.Node): Boolean = node.operationCase == Rules.Node.OperationCase.STRING_OPERATION &&
        node.stringOperation.kind == Rules.StringOpKind.STRING_OP_KIND_SUBJECT

    fun isPrependCountry(node: Rules.Node): Boolean =
        node.operationCase == Rules.Node.OperationCase.CANONICALIZATION_OPERATION &&
            node.canonicalizationOperation.kind ==
            Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_PREPEND_COUNTRY_IF_MISSING

    /** The five operations a pre-canonicalisation program may hold. */
    fun allowedInPreCanonicalization(node: Rules.Node): Boolean {
        if (node.operationCase != Rules.Node.OperationCase.CANONICALIZATION_OPERATION) return false
        return when (node.canonicalizationOperation.kind) {
            Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_SEQUENCE,
            Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_TRIM_WHITESPACE,
            Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_REMOVE_WHITESPACE,
            Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_UPPERCASE_ASCII,
            Rules.CanonicalizationOpKind.CANONICALIZATION_OP_KIND_REMOVE_CHARS,
            -> true

            else -> false
        }
    }
}

/** Code point helpers used by both the checks and the emitter. */
internal object Cp {
    fun of(s: String): IntArray {
        val out = IntArray(s.codePointCount(0, s.length))
        var i = 0
        var n = 0
        while (i < s.length) {
            val c = s.codePointAt(i)
            out[n++] = c
            i += Character.charCount(c)
        }
        return out
    }

    fun count(s: String): Int = s.codePointCount(0, s.length)

    fun utf8Length(s: String): Int = s.toByteArray(Charsets.UTF_8).size
}
