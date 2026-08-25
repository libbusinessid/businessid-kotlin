// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

package org.entid

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * The three membership lists of rules `2026.08.31`, through the public API.
 *
 * They are the first rules to carry thousands of entries rather than a handful,
 * and the emitted form changed for them: a sorted string constant read by binary
 * search rather than an array walked from the front. The shared corpus covers
 * every case below; what these add is the reason each one matters, next to the
 * property the emitted form has to preserve.
 *
 * Every value is synthetic unless its case says otherwise.
 */
class MembershipTest {
    private val engine = EntIdEngine.default()

    private fun report(kind: String, value: String, country: String? = null) =
        engine.validate(IdentifierInput(IdentifierKind(kind), value, country))

    @Test
    fun `starting with a court code is not being one`() {
        // euid-de-register-longer-than-a-code-005. Of the 818 six character
        // court codes, 782 begin with a five character one, so over a single
        // list a prefix test would accept `B1000X` on the strength of `B1000`.
        // The lists are split by length, and the emitted search reads one group
        // per length: a six character register identifier is looked up among the
        // six character codes and nowhere else.
        val report = report("euid", "DEB1000X.HRB12345")
        assertEquals(StepStatus.INVALID, report.format.status)
        assertEquals(ReasonCode.INVALID_FORMAT, report.format.reasonCode)
        assertEquals("euid.de.register_unknown", report.format.messageKey)
    }

    @Test
    fun `a five character court code is accepted, and its six character neighbours are not`() {
        // euid-de-valid-001 and euid-de-real-003. The second is a real German
        // EUID, published by the register itself.
        assertEquals(StepStatus.VALID, report("euid", "DEF1103.HRB12345").format.status)
        assertEquals(StepStatus.VALID, report("euid", "DEK1101R.HRB116737").format.status)
    }

    @Test
    fun `a well shaped register identifier no court carries is refused`() {
        // euid-de-register-unknown-004. The shape was never the question: the
        // list is what says whether a court carries it.
        val report = report("euid", "DEZZZZZ.HRB12345")
        assertEquals(ReasonCode.INVALID_FORMAT, report.format.reasonCode)
        assertEquals("euid.de.register_unknown", report.format.messageKey)
    }

    @Test
    fun `a French greffe code is looked up in the registrars' list`() {
        // euid-fr-register-toulouse-041 and euid-fr-register-unknown-040. 3102
        // is Toulouse, one of the 148 codes the registrars publish; 9999 has the
        // right shape and belongs to no greffe.
        assertEquals(StepStatus.VALID, report("euid", "FR3102.012345674").format.status)
        assertEquals(ReasonCode.INVALID_FORMAT, report("euid", "FR9999.012345674").format.reasonCode)
    }

    @Test
    fun `a membership answer does not depend on where in the list an entry sits`() {
        // A binary search that was subtly wrong would find the entries it
        // happens to land on and miss the rest. Both ends and the middle of the
        // French list are accepted, and the values just outside each are not.
        for (accepted in listOf("FR0101.012345674", "FR3102.012345674", "FR9761.012345674")) {
            assertEquals(StepStatus.VALID, report("euid", accepted).format.status, accepted)
        }
        for (refused in listOf("FR0100.012345674", "FR3103.012345674", "FR9762.012345674")) {
            assertEquals(
                ReasonCode.INVALID_FORMAT,
                report("euid", refused).format.reasonCode,
                refused,
            )
        }
    }

    @Test
    fun `the Luxembourg section letter is a letter rather than the letter B`() {
        // The constraint used to be `B`, which refused a sole trader's number.
        // Any letter is now accepted, and a digit still is not.
        assertEquals(StepStatus.VALID, report("rcs_number", "B12345").format.status)
        assertEquals(StepStatus.VALID, report("rcs_number", "A12345").format.status)
        assertEquals(StepStatus.INVALID, report("rcs_number", "112345").format.status)
    }
}
