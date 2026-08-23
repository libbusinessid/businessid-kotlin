// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.runtime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * The string constructors of the IR.
 *
 * Absence propagates through every one of them, and an out of range view yields
 * absence rather than an exception: refusing a valid identifier because a rule
 * addressed past its end is the failure mode this file exists to prevent.
 */
class TextTest {
    @Test
    fun `slice yields the code points in the half open range`() {
        assertEquals("234", Txt.slice(view("0123456"), 2, 5).text())
        assertEquals("", Txt.slice(view("0123456"), 3, 3).text())
        assertEquals("0123456", Txt.slice(view("0123456"), 0, 7).text())
    }

    @Test
    fun `slice is absent past the end, on a reversed range, and on an absent operand`() {
        assertNull(Txt.slice(view("012"), 0, 4))
        assertNull(Txt.slice(view("012"), 2, 1))
        assertNull(Txt.slice(null, 0, 1))
    }

    @Test
    fun `slice_from and slice_to`() {
        assertEquals("456", Txt.sliceFrom(view("0123456"), 4).text())
        assertEquals("", Txt.sliceFrom(view("0123456"), 7).text())
        assertNull(Txt.sliceFrom(view("0123456"), 8))
        assertEquals("0123", Txt.sliceTo(view("0123456"), 4).text())
        assertNull(Txt.sliceTo(view("0123456"), 8))
        assertNull(Txt.sliceFrom(null, 0))
        assertNull(Txt.sliceTo(null, 0))
    }

    @Test
    fun `before_first and after_first split on the first occurrence only`() {
        assertEquals("a", Txt.beforeFirst(view("a.b.c"), cp(".")).text())
        assertEquals("b.c", Txt.afterFirst(view("a.b.c"), cp(".")).text())
        assertEquals("", Txt.beforeFirst(view(".b"), cp(".")).text())
        assertEquals("", Txt.afterFirst(view("b."), cp(".")).text())
    }

    @Test
    fun `before_first and after_first are absent when the delimiter does not occur`() {
        assertNull(Txt.beforeFirst(view("abc"), cp(".")))
        assertNull(Txt.afterFirst(view("abc"), cp(".")))
        assertNull(Txt.beforeFirst(null, cp(".")))
        assertNull(Txt.afterFirst(null, cp(".")))
    }

    @Test
    fun `strip_prefix removes an exact leading constant`() {
        assertEquals("123", Txt.stripPrefix(view("FR123"), cp("FR")).text())
        assertNull(Txt.stripPrefix(view("BE123"), cp("FR")))
        assertNull(Txt.stripPrefix(null, cp("FR")))
    }

    @Test
    fun `concat joins in order and is absent when any operand is`() {
        assertEquals("abc", Txt.concat(arrayOf<CpView?>(view("a"), view("bc"))).text())
        assertEquals("", Txt.concat(arrayOf<CpView?>(view(""), view(""))).text())
        assertNull(Txt.concat(arrayOf<CpView?>(view("a"), null)))
        assertNull(Txt.concat(arrayOf<CpView?>(null, view("a"))))
    }

    @Test
    fun `positions are counted in code points, not in UTF-16 units`() {
        // U+1D400 is outside the Basic Multilingual Plane and occupies two Chars.
        val v = view("𝐀AB")
        assertEquals(3, v.length)
        assertEquals("AB", Txt.sliceFrom(v, 1).text())
        assertEquals("𝐀", Txt.sliceTo(v, 1).text())
    }

    @Test
    fun `views compare by content and not by backing array`() {
        val shared = view("XABCX")
        assertEquals(Txt.slice(shared, 1, 4), view("ABC"))
        assertEquals(Txt.slice(shared, 1, 4).hashCode(), view("ABC").hashCode())
        assertEquals(false, Txt.slice(shared, 1, 4)!!.equals("ABC"))
    }
}
