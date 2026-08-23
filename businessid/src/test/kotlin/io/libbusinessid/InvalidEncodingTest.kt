// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * `invalid_encoding`, which no conformance case can carry.
 *
 * A proto3 `string` is valid UTF-8 by definition, on the wire and in the corpus,
 * so the shared suite cannot reach this branch. Nor is there one portable
 * malformed value to carry: an invalid byte in a language whose strings are
 * bytes, an unpaired surrogate in a language whose strings are UTF-16 code
 * units, and nothing at all in a language whose strings are always well formed.
 *
 * Kotlin's `String` is UTF-16, so the malformed form it admits is the unpaired
 * surrogate, and that is what this file pins.
 */
class InvalidEncodingTest {
    private val engine = BusinessIdEngine.default()

    private val highSurrogate = "\uD83D"
    private val lowSurrogate = "\uDE00"

    private fun report(value: String) = engine.validate(IdentifierInput(IdentifierKind.SIREN, value))

    @Test
    fun `a lone high surrogate is refused as ill formed text`() {
        val report = report(highSurrogate)
        assertEquals(StepStatus.UNSUPPORTED, report.format.status)
        assertEquals(ReasonCode.INVALID_ENCODING, report.format.reasonCode)
        assertEquals(StepStatus.NOT_RUN, report.checksum.status)
        assertEquals(ReasonCode.NOT_RUN_FORMAT_UNSUPPORTED, report.checksum.reasonCode)
    }

    @Test
    fun `a lone low surrogate is refused as ill formed text`() {
        assertEquals(ReasonCode.INVALID_ENCODING, report(lowSurrogate).format.reasonCode)
    }

    @Test
    fun `a reversed surrogate pair is refused`() {
        assertEquals(ReasonCode.INVALID_ENCODING, report("$lowSurrogate$highSurrogate").format.reasonCode)
    }

    @Test
    fun `an unpaired surrogate inside otherwise valid text is refused`() {
        assertEquals(ReasonCode.INVALID_ENCODING, report("012${highSurrogate}345674").format.reasonCode)
    }

    @Test
    fun `a well formed pair is not refused`() {
        // The same two code units, this time in the order that encodes.
        val report = report("$highSurrogate$lowSurrogate")
        assertEquals(StepStatus.INVALID, report.format.status)
        assertEquals(ReasonCode.INVALID_LENGTH, report.format.reasonCode)
    }

    @Test
    fun `the value is reported verbatim, since it holds no code points to evaluate`() {
        val value = "012${highSurrogate}345674"
        val report = report(value)
        assertEquals(value, report.inputValue)
        assertEquals(value, report.canonicalValue)
        assertNull(report.format.messageKey, "no rule assertion ran")
    }

    @Test
    fun `canonicalize answers the same way`() {
        val result = engine.canonicalize(IdentifierInput(IdentifierKind.SIREN, highSurrogate))
        assertEquals(StepStatus.UNSUPPORTED, result.status)
        assertEquals(ReasonCode.INVALID_ENCODING, result.reasonCode)
        assertEquals(highSurrogate, result.canonicalValue)
    }

    @Test
    fun `the length bound is answered before the encoding, as the pipeline orders them`() {
        // Both faults at once: the input is above the byte limit and ill formed.
        // Step 1 of the pipeline is the length bound, so that is the answer.
        val value = highSurrogate + "1".repeat(1030)
        assertEquals(ReasonCode.INPUT_TOO_LONG, report(value).format.reasonCode)
    }

    @Test
    fun `an ill formed kind token is an unsupported kind, not an encoding failure`() {
        // The encoding check reads the value, which is what an identifier is.
        val report = engine.validate(IdentifierInput(IdentifierKind(highSurrogate), "012345674"))
        assertEquals(ReasonCode.UNSUPPORTED_KIND, report.format.reasonCode)
    }
}
