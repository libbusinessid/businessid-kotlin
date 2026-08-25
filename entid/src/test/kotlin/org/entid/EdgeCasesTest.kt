// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

package org.entid

import org.entid.internal.guardEngineErrors
import org.entid.runtime.Alignment
import org.entid.runtime.Arith
import org.entid.runtime.CanonBuffer
import org.entid.runtime.CpView
import org.entid.runtime.Pred
import org.entid.runtime.Txt
import org.entid.runtime.Utf
import org.entid.runtime.cp
import org.entid.runtime.view
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

/**
 * The corners the ordinary paths never reach.
 *
 * Each of these exists because the code has a branch for it. A branch nothing
 * takes is either wrong or unnecessary, and the point of finding them is to
 * decide which.
 */
class EdgeCasesTest {
    private val engine = EntIdEngine.default()

    @Test
    fun `an engine error carries its message and its cause`() {
        val cause = ArithmeticException("long overflow")
        val failure = assertFailsWith<EntIdEngineException> {
            guardEngineErrors("checksum") { throw cause }
        }
        assertEquals("the checksum program overflowed a checked integer", failure.message)
        assertSame(cause, failure.cause)
    }

    @Test
    fun `an out of bounds access inside a program is an engine error, not a verdict`() {
        val cause = IndexOutOfBoundsException("42")
        val failure = assertFailsWith<EntIdEngineException> {
            guardEngineErrors("format") { throw cause }
        }
        assertEquals("the format program addressed outside a view", failure.message)
        assertSame(cause, failure.cause)
    }

    @Test
    fun `the guard lets an ordinary answer through untouched`() {
        assertEquals(7, guardEngineErrors("format") { 7 })
    }

    @Test
    fun `inserting nothing changes nothing`() {
        val buffer = CanonBuffer(Utf.codePoints("AB"))
        buffer.prepend(IntArray(0))
        buffer.append(IntArray(0))
        buffer.insert(1, IntArray(0))
        assertEquals("AB", buffer.snapshot())
    }

    @Test
    fun `ends_with is false when the suffix is longer than the value`() {
        assertFalse(Pred.endsWith(view("F"), cp("FR")))
        assertTrue(Pred.endsWith(view("FR"), cp("FR")))
    }

    @Test
    fun `searching for nothing finds it at the start`() {
        assertTrue(Pred.contains(view("ABC"), IntArray(0)))
        assertEquals("", Txt.beforeFirst(view("ABC"), IntArray(0)).toString())
    }

    @Test
    fun `a right aligned weighted sum pairs only what both sides have`() {
        // Fewer code points than weights: the leading weights pair with nothing.
        assertEquals(2L, Arith.weightedSumDigits(view("2"), longArrayOf(5, 7, 1), Alignment.RIGHT))
        assertEquals(10L, Arith.weightedSumDigits(view("2"), longArrayOf(5, 7, 1), Alignment.LEFT))
    }

    @Test
    fun `code points of ill formed text degrade rather than throw`() {
        // codePoints is only called on text isWellFormed accepted, but a lone
        // surrogate must not make it fail: it yields the code unit itself.
        assertEquals(1, Utf.codePoints("\uD83D").size)
        assertEquals(0xD83D, Utf.codePoints("\uD83D")[0])
        assertEquals(2, Utf.codePoints("\uD83DA").size)
        assertEquals(1, Utf.codePoints("\uDE00").size)
    }

    @Test
    fun `a report knows when only one of its two steps failed`() {
        val checksumFailed = engine.validate(IdentifierInput(IdentifierKind.SIRET, "01234567400000"))
        assertTrue(checksumFailed.isFormatValid)
        assertFalse(checksumFailed.isChecksumValid)
        assertFalse(checksumFailed.isFullyValidated)
        assertTrue(checksumFailed.isInvalid)

        val formatFailed = engine.validate(IdentifierInput(IdentifierKind.SIRET, "0123456789012"))
        assertFalse(formatFailed.isFormatValid)
        assertFalse(formatFailed.isChecksumValid)
        assertFalse(formatFailed.isFullyValidated)
        assertTrue(formatFailed.isInvalid)

        val neither = engine.validate(IdentifierInput(IdentifierKind.DUNS, "012345678"))
        assertTrue(neither.isFormatValid)
        assertFalse(neither.isChecksumValid)
        assertFalse(neither.isFullyValidated)
        assertFalse(neither.isInvalid, "unsupported is not a verdict of invalidity")
    }

    @Test
    fun `a canonicalisation result reads its kind as a plain token too`() {
        val result = engine.canonicalize(IdentifierInput(IdentifierKind.LEI, "00000000000000000098"))
        assertEquals("lei", result.kindToken)
        assertEquals(result.kind.value, result.kindToken)
    }

    @Test
    fun `an identifier kind prints as its token`() {
        assertEquals("siret", IdentifierKind.SIRET.toString())
        assertEquals("anything", IdentifierKind("anything").toString())
    }

    @Test
    fun `a view compares unequal to a value of another type and to a different length`() {
        val v = view("AB")
        assertFalse(v.equals(2))
        assertFalse(v == view("ABC"))
        assertTrue(v == view("AB"))
    }

    @Test
    fun `slicing an absent view stays absent through every constructor`() {
        val absent: CpView? = null
        assertNull(Txt.slice(absent, 0, 1))
        assertNull(Txt.sliceFrom(absent, 0))
        assertNull(Txt.sliceTo(absent, 0))
        assertNull(Txt.beforeFirst(absent, cp("A")))
        assertNull(Txt.afterFirst(absent, cp("A")))
        assertNull(Txt.stripPrefix(absent, cp("A")))
        assertNull(Txt.concat(arrayOf(absent)))
    }
}
