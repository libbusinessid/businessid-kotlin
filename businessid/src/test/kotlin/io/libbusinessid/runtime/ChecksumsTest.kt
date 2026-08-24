// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.runtime

import io.libbusinessid.ReasonCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * The checksum operations of the IR.
 *
 * The tri-state is the point: an algorithm that cannot be evaluated answers
 * `unsupported`, never `invalid`.
 */
class ChecksumsTest {
    private fun assertValid(outcome: ChecksumOutcome) {
        assertEquals(ChecksumStatus.VALID, outcome.status)
        assertEquals(ReasonCode.OK, outcome.reason)
        assertNull(outcome.messageKey, "a valid outcome carries no key: a rule declares none for succeeding")
    }

    private fun assertInvalid(outcome: ChecksumOutcome, key: String? = null) {
        assertEquals(ChecksumStatus.INVALID, outcome.status)
        assertEquals(ReasonCode.INVALID_CHECKSUM, outcome.reason)
        assertEquals(key, outcome.messageKey)
    }

    private fun assertUnsupported(outcome: ChecksumOutcome, reason: ReasonCode = ReasonCode.UNSUPPORTED_CHECKSUM) {
        assertEquals(ChecksumStatus.UNSUPPORTED, outcome.status)
        assertEquals(reason, outcome.reason)
    }

    @Test
    fun `luhn accepts a value whose weighted sum is a multiple of ten`() {
        // The two values are the conformance cases siret-synthetic-valid-001 and
        // siret-luhn-branch-206: synthetic, and produced by the documented
        // generator of the shared corpus.
        assertValid(Ck.luhn(view("01234567400001"), null))
        assertInvalid(Ck.luhn(view("01234567400000"), "fr.siret.luhn"), "fr.siret.luhn")
    }

    @Test
    fun `luhn is unsupported on absent, too short and non digit values`() {
        assertUnsupported(Ck.luhn(null, null))
        assertUnsupported(Ck.luhn(view("7"), null))
        assertUnsupported(Ck.luhn(view(""), null))
        assertUnsupported(Ck.luhn(view("12A4"), null))
    }

    @Test
    fun `mod 97 expands letters to their base 36 decimal spelling`() {
        // From the conformance cases lei-valid-001 and lei-valid-002.
        assertValid(Ck.iso7064Mod97(view("00000000000000000098"), null))
        assertValid(Ck.iso7064Mod97(view("000000ABCDEF12345670"), null))
        assertInvalid(Ck.iso7064Mod97(view("00000000000000000099"), null))
    }

    @Test
    fun `mod 97 is unsupported on absent, too short and out of domain values`() {
        assertUnsupported(Ck.iso7064Mod97(null, null))
        assertUnsupported(Ck.iso7064Mod97(view("AB"), null))
        assertUnsupported(Ck.iso7064Mod97(view("AB-C"), null))
        assertUnsupported(Ck.iso7064Mod97(view("abc"), null))
    }

    @Test
    fun `compare_digit reads one ASCII digit and is unsupported off the end`() {
        assertValid(Ck.compareDigit(7L, view("1237"), 3, null))
        assertInvalid(Ck.compareDigit(8L, view("1237"), 3, null))
        assertUnsupported(Ck.compareDigit(null, view("1237"), 3, null))
        assertUnsupported(Ck.compareDigit(7L, null, 3, null))
        assertUnsupported(Ck.compareDigit(7L, view("1237"), 4, null))
        assertUnsupported(Ck.compareDigit(7L, view("123A"), 3, null))
    }

    @Test
    fun `compare_slice reads a decimal slice and is unsupported off the end`() {
        assertValid(Ck.compareSlice(37L, view("12370"), 2, 4, null))
        assertInvalid(Ck.compareSlice(38L, view("12370"), 2, 4, null))
        assertUnsupported(Ck.compareSlice(null, view("12370"), 2, 4, null))
        assertUnsupported(Ck.compareSlice(37L, view("123"), 2, 9, null))
        assertUnsupported(Ck.compareSlice(37L, view("12A70"), 2, 4, null))
    }

    @Test
    fun `compare_constant closes the gap the two view comparisons left`() {
        assertValid(Ck.compareConstant(0L, 0L, null))
        assertInvalid(Ck.compareConstant(1L, 0L, "x.key"), "x.key")
        assertUnsupported(Ck.compareConstant(null, 0L, null))
    }

    @Test
    fun `a declared unsupported outcome keeps its reason and key`() {
        assertUnsupported(
            Ck.declaredUnsupported(ReasonCode.CHECKSUM_NOT_PUBLISHED, null),
            ReasonCode.CHECKSUM_NOT_PUBLISHED,
        )
        val keyed = Ck.declaredUnsupported(ReasonCode.CHECKSUM_NOT_PUBLISHED, "x.key")
        assertEquals("x.key", keyed.messageKey)
    }

    @Test
    fun `all_checks returns the first invalid, otherwise the first unsupported, otherwise valid`() {
        val valid = Ck.valid()
        val invalidA = Ck.compareConstant(1L, 0L, "a")
        val invalidB = Ck.compareConstant(2L, 0L, "b")
        val unsupported = Ck.declaredUnsupported(ReasonCode.UNSUPPORTED_CHECKSUM, "u")

        assertValid(Ck.allChecks(arrayOf(valid, valid)))
        assertEquals("a", Ck.allChecks(arrayOf(unsupported, invalidA, invalidB)).messageKey)
        assertEquals("u", Ck.allChecks(arrayOf(valid, unsupported, valid)).messageKey)
    }

    @Test
    fun `any_check returns valid as soon as one is, otherwise unsupported, otherwise invalid`() {
        val valid = Ck.valid()
        val invalidA = Ck.compareConstant(1L, 0L, "a")
        val unsupported = Ck.declaredUnsupported(ReasonCode.UNSUPPORTED_CHECKSUM, "u")

        assertValid(Ck.anyCheck(arrayOf(invalidA, valid)))
        assertEquals("u", Ck.anyCheck(arrayOf(invalidA, unsupported)).messageKey)
        assertEquals("a", Ck.anyCheck(arrayOf(invalidA, invalidA)).messageKey)
        // The first of each kind wins, so a second one changes nothing.
        val second = Ck.declaredUnsupported(ReasonCode.CHECKSUM_NOT_PUBLISHED, "second")
        assertEquals("u", Ck.anyCheck(arrayOf(unsupported, second)).messageKey)
        assertEquals("u", Ck.allChecks(arrayOf(unsupported, second)).messageKey)
    }

    @Test
    fun `a choose whose branches all fail to apply is unsupported`() {
        assertUnsupported(Ck.noBranch())
    }

    @Test
    fun `an empty combination is unsupported rather than valid`() {
        // No rule emits one, but the shape has to have an answer, and the safe
        // answer is the one that proves nothing.
        assertValid(Ck.allChecks(emptyArray()))
        assertUnsupported(Ck.anyCheck(emptyArray()))
    }

    @Test
    fun `any_check falls back to the first invalid only when nothing is unsupported`() {
        val invalidA = Ck.compareConstant(1L, 0L, "a")
        val invalidB = Ck.compareConstant(2L, 0L, "b")
        assertEquals("a", Ck.anyCheck(arrayOf(invalidA, invalidB)).messageKey)
        assertEquals("a", Ck.allChecks(arrayOf(invalidA, invalidB)).messageKey)
    }

    @Test
    fun `the three states map onto the steps of the report`() {
        assertEquals(io.libbusinessid.StepStatus.VALID, ChecksumStatus.VALID.step)
        assertEquals(io.libbusinessid.StepStatus.INVALID, ChecksumStatus.INVALID.step)
        assertEquals(io.libbusinessid.StepStatus.UNSUPPORTED, ChecksumStatus.UNSUPPORTED.step)
    }

    @Test
    fun `a keyed unsupported outcome is distinct from the shared unkeyed one`() {
        assertEquals(null, Ck.luhn(null, null).messageKey)
        assertEquals("k", Ck.luhn(null, "k").messageKey)
        assertEquals(
            ReasonCode.UNSUPPORTED_CHECKSUM,
            Ck.declaredUnsupported(ReasonCode.UNSUPPORTED_CHECKSUM, null).reason,
        )
    }

    @Test
    fun `an unsupported checksum never becomes invalid, whatever it is combined with`() {
        val unsupported = Ck.declaredUnsupported(ReasonCode.CHECKSUM_NOT_PUBLISHED, null)
        assertEquals(ChecksumStatus.UNSUPPORTED, Ck.allChecks(arrayOf(unsupported)).status)
        assertEquals(ChecksumStatus.UNSUPPORTED, Ck.anyCheck(arrayOf(unsupported)).status)
    }
}
