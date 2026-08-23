// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * The dispatch algorithm and the identity every branch of it reports.
 *
 * The identity table of `ir.md` section 5.1 is what makes an output fully
 * determined even when dispatch fails before a definition is selected, and the
 * order of steps 4 and 5 is observable: pre-canonicalisation runs before the
 * country decision, so a country that cannot be used is reported beside the
 * value that was already pre-canonicalised.
 *
 * Every value here is synthetic and drawn from the shared corpus.
 */
class DispatchTest {
    private val engine = BusinessIdEngine.default()

    private fun canonicalize(kind: String, value: String, country: String? = null) =
        engine.canonicalize(IdentifierInput(IdentifierKind(kind), value, country))

    private fun validate(kind: String, value: String, country: String? = null) =
        engine.validate(IdentifierInput(IdentifierKind(kind), value, country))

    @Test
    fun `a kind token is trimmed and lower cased before anything else`() {
        val report = validate("  VAT\t", "BE0123456749")
        assertEquals("vat", report.kind.value)
        assertEquals(StepStatus.VALID, report.format.status)
    }

    @Test
    fun `a malformed kind keeps its normalised token in the result`() {
        val report = validate("VAT!", "X")
        assertEquals("vat!", report.kind.value)
        assertEquals(ReasonCode.UNSUPPORTED_KIND, report.format.reasonCode)
    }

    @Test
    fun `pre-canonicalisation runs before the country decision`() {
        // The country cannot be used, and the value reported is already
        // pre-canonical rather than raw. This is the observable half of the
        // order of steps 4 and 5.
        val result = canonicalize("siren", "012 345 674", "DE")
        assertEquals("012345674", result.canonicalValue)
        assertEquals("DE", result.countryCode)
        assertEquals(StepStatus.UNSUPPORTED, result.status)
        assertEquals(ReasonCode.UNSUPPORTED_COUNTRY, result.reasonCode)
    }

    @Test
    fun `a syntactically invalid country keeps the raw context`() {
        val report = validate("vat", "0123456749", "belgium")
        assertEquals(ReasonCode.UNSUPPORTED_COUNTRY, report.format.reasonCode)
        assertEquals("belgium", report.countryCode, "the raw context, since nothing normalised")
    }

    @Test
    fun `a country of the wrong length is invalid even when it looks plausible`() {
        val report = validate("lei", "00000000000000000098", "FRA")
        assertEquals(ReasonCode.UNSUPPORTED_COUNTRY, report.format.reasonCode)
        assertEquals("FRA", report.countryCode)
    }

    @Test
    fun `an empty country behaves like an absent context`() {
        val report = validate("vat", "BE0123456749", "")
        assertEquals(StepStatus.VALID, report.format.status)
        assertEquals("BE", report.countryCode, "the country comes from the prefix")
    }

    @Test
    fun `a country alias resolves to the ISO code of its target`() {
        val report = validate("vat", "0123456749", "UK")
        assertEquals("GB", report.countryCode)
        assertEquals("GB0123456749", report.canonicalValue)
    }

    @Test
    fun `the country of a target is its ISO code, not its business prefix`() {
        // Greece routes VAT under the prefix EL and reports the country GR.
        val fromContext = validate("vat", "012345670", "GR")
        assertEquals("GR", fromContext.countryCode)
        assertEquals("EL012345670", fromContext.canonicalValue)

        val fromAlias = validate("vat", "012345670", "EL")
        assertEquals("GR", fromAlias.countryCode)

        val fromPrefix = validate("vat", "EL012345670")
        assertEquals("GR", fromPrefix.countryCode)
    }

    @Test
    fun `a country with no target in a country specific dispatcher is unsupported`() {
        val report = validate("vat", "0123456749", "JP")
        assertEquals(ReasonCode.UNSUPPORTED_COUNTRY, report.format.reasonCode)
        assertEquals("JP", report.countryCode, "normalised, because the token is well formed")
        assertEquals("0123456749", report.canonicalValue, "pre-canonical, not raw")
    }

    @Test
    fun `a GLOBAL target keeps a well formed country without routing on it`() {
        val report = validate("lei", "00000000000000000098", "fr")
        assertEquals(StepStatus.VALID, report.format.status)
        assertEquals("FR", report.countryCode, "normalised and kept, but not used")
    }

    @Test
    fun `a proven contradiction between country and prefix is invalid`() {
        val report = validate("vat", "BE0123456749", "FR")
        assertEquals(StepStatus.INVALID, report.format.status)
        assertEquals(ReasonCode.COUNTRY_MISMATCH, report.format.reasonCode)
        assertNull(report.format.messageKey, "a result produced before a rule assertion carries no key")
        assertEquals(StepStatus.NOT_RUN, report.checksum.status)
        assertEquals(ReasonCode.NOT_RUN_FORMAT_INVALID, report.checksum.reasonCode)
        assertEquals("FR", report.countryCode)
        assertEquals("BE0123456749", report.canonicalValue)
    }

    @Test
    fun `an unusable country is answered before a contradiction is looked for`() {
        // JP has no EUID target, so the answer is unsupported_country and not the
        // mismatch the FR prefix would otherwise prove.
        val unusable = validate("euid", "FRTVX.012345674", "JP")
        assertEquals(ReasonCode.UNSUPPORTED_COUNTRY, unusable.format.reasonCode)

        val contradicted = validate("euid", "FRTVX.012345674", "DE")
        assertEquals(ReasonCode.COUNTRY_MISMATCH, contradicted.format.reasonCode)
    }

    @Test
    fun `nothing selectable is a missing country, not an invalidity`() {
        val report = validate("vat", "0123456749")
        assertEquals(StepStatus.UNSUPPORTED, report.format.status)
        assertEquals(ReasonCode.MISSING_COUNTRY_CODE, report.format.reasonCode)
        assertNull(report.countryCode)
    }

    @Test
    fun `a prefix only routes when it starts the value`() {
        val report = validate("vat", "0BE0123456749")
        assertEquals(ReasonCode.MISSING_COUNTRY_CODE, report.format.reasonCode)
    }

    @Test
    fun `the longest declared prefix wins`() {
        val report = validate("vat", "EL012345670")
        assertEquals(StepStatus.VALID, report.format.status)
        assertEquals("GR", report.countryCode)
    }

    @Test
    fun `a dispatcher with a single implicit target routes an unprefixed value`() {
        val report = validate("siren", "012345674")
        assertEquals(StepStatus.VALID, report.format.status)
        assertEquals("FR", report.countryCode, "the country of the selected target")
    }

    @Test
    fun `a definition prepends its canonical prefix when the value lacks one`() {
        assertEquals("BE0123456749", validate("vat", "0123456749", "BE").canonicalValue)
        assertEquals("BE0123456749", validate("vat", "BE0123456749", "BE").canonicalValue)
        assertEquals("FRTVX.012345674", validate("euid", "TVX.012345674", "FR").canonicalValue)
    }

    @Test
    fun `an unresolved dispatcher reports the raw value and the raw context`() {
        val result = canonicalize("unknown_kind", " X ")
        assertEquals(" X ", result.canonicalValue)
        assertEquals(ReasonCode.UNSUPPORTED_KIND, result.reasonCode)
        assertNull(result.countryCode)
    }

    @Test
    fun `an input above the limit reports the raw value and the raw context`() {
        val result = canonicalize("siren", "1".repeat(1025), "fr ")
        assertEquals("1".repeat(1025), result.canonicalValue)
        assertEquals("fr ", result.countryCode, "the raw context, untouched")
        assertEquals(ReasonCode.INPUT_TOO_LONG, result.reasonCode)
    }
}
