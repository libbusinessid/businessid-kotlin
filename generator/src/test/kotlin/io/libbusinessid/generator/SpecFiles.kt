// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.generator

import libbusinessid.conformance.v1.Conformance
import java.io.File

/** Access to the pinned artefacts under `spec/`, which are build inputs. */
object SpecFiles {
    private val root = File(
        requireNotNull(System.getProperty("businessid.spec.dir")) {
            "the test task must set businessid.spec.dir"
        },
    )

    val rulesBundle: ByteArray by lazy { File(root, "businessid-rules.binpb").readBytes() }

    val conformance: Conformance.ConformanceBundle by lazy {
        Conformance.ConformanceBundle.parseFrom(File(root, "businessid-conformance.binpb").readBytes())
    }

    val lock: Map<String, String> by lazy {
        File(root.parentFile, "rules.lock").readLines()
            .mapNotNull { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("#") || "=" !in trimmed) {
                    null
                } else {
                    val (k, v) = trimmed.split("=", limit = 2)
                    k.trim() to v.trim().trim('"')
                }
            }.toMap()
    }

    fun file(name: String): File = File(root, name)

    /** The `load_ruleset` cases, which address the generator rather than the engine. */
    val loaderCases: List<Conformance.ConformanceCase> by lazy {
        conformance.casesList.filter { it.operation == Conformance.Operation.OPERATION_LOAD_RULESET }
    }
}
