// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.consumer

import io.libbusinessid.BusinessIdEngine
import io.libbusinessid.IdentifierInput
import io.libbusinessid.IdentifierKind
import io.libbusinessid.ReasonCode
import io.libbusinessid.StepStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * That the published artefact works from a build that knows nothing about this
 * repository beyond its coordinates.
 *
 * Every value is synthetic and comes from the shared conformance corpus.
 */
class ConsumerTest {
    private val engine = BusinessIdEngine.default()

    @Test
    fun `it validates from a consumer project`() {
        val report = engine.validate(IdentifierInput(IdentifierKind.SIRET, "012 345 674 00001"))
        assertEquals("01234567400001", report.canonicalValue)
        assertTrue(report.isFullyValidated)
    }

    @Test
    fun `an unpublished algorithm is unsupported rather than invalid`() {
        val report = engine.validate(IdentifierInput(IdentifierKind.DUNS, "012345678"))
        assertEquals(ReasonCode.CHECKSUM_NOT_PUBLISHED, report.checksum.reasonCode)
        assertFalse(report.isInvalid)
    }

    @Test
    fun `the ruleset version is the one the engine was built from`() {
        assertEquals("2026.08.26", engine.rulesInfo().rulesVersion)
        assertEquals(StepStatus.VALID, engine.validate(IdentifierInput.of("lei", "00000000000000000098")).format.status)
    }

}
