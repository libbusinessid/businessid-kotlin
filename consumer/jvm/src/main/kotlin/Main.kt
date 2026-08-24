// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.consumer

import io.libbusinessid.BusinessIdEngine
import io.libbusinessid.IdentifierInput
import io.libbusinessid.IdentifierKind

/**
 * The shortest useful program a consumer can write.
 *
 * The value is synthetic and comes from the conformance case
 * `siret-synthetic-valid-001`.
 */
fun main() {
    val engine = BusinessIdEngine.default()
    val report = engine.validate(IdentifierInput(IdentifierKind.SIRET, "012 345 674 00001"))
    println("canonical: ${report.canonicalValue}")
    println("format:    ${report.format.status.wireName}/${report.format.reasonCode.wireName}")
    println("checksum:  ${report.checksum.status.wireName}/${report.checksum.reasonCode.wireName}")
    println("rules:     ${engine.rulesInfo().rulesVersion}")
}
