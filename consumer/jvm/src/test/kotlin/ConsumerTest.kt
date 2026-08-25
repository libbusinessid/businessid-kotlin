// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

package org.entid.consumer

import org.entid.EntIdEngine
import org.entid.IdentifierInput
import org.entid.IdentifierKind
import org.entid.ReasonCode
import org.entid.StepStatus
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
    private val engine = EntIdEngine.default()

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
        assertEquals(System.getProperty("entid.rules.version"), engine.rulesInfo().rulesVersion)
        assertEquals(StepStatus.VALID, engine.validate(IdentifierInput.of("lei", "00000000000000000098")).format.status)
    }

}
