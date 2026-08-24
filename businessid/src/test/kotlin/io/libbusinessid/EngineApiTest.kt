// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

/**
 * The four public operations and the shapes they return.
 *
 * Every value used here is synthetic and comes from the shared conformance
 * corpus; the case it is drawn from is named beside it.
 */
class EngineApiTest {
    private val engine = BusinessIdEngine.default()

    /** siret-synthetic-valid-001, synthetic. */
    private val siret = IdentifierInput(IdentifierKind.SIRET, "01234567400001")

    /** duns-valid-001, synthetic; the authority publishes no check algorithm. */
    private val duns = IdentifierInput(IdentifierKind.DUNS, "012345678")

    @Test
    fun `the default engine is shared and cannot fail`() {
        assertSame(BusinessIdEngine.default(), BusinessIdEngine.default())
    }

    @Test
    fun `validate runs both steps`() {
        val report = engine.validate(siret)
        assertEquals(StepStatus.VALID, report.format.status)
        assertEquals(ReasonCode.OK, report.format.reasonCode)
        assertEquals(ValidationLevel.FORMAT, report.format.level)
        assertEquals(StepStatus.VALID, report.checksum.status)
        assertEquals(ValidationLevel.CHECKSUM, report.checksum.level)
        assertTrue(report.isFullyValidated)
        assertFalse(report.isInvalid)
    }

    @Test
    fun `validateFormat stops after the format and says so`() {
        val report = engine.validateFormat(siret)
        assertEquals(StepStatus.VALID, report.format.status)
        assertEquals(StepStatus.NOT_RUN, report.checksum.status)
        assertEquals(ReasonCode.NOT_REQUESTED, report.checksum.reasonCode)
        assertFalse(report.isFullyValidated)
    }

    @Test
    fun `validateChecksum returns exactly the report of validate`() {
        assertEquals(engine.validate(siret), engine.validateChecksum(siret))
        val wrong = IdentifierInput(IdentifierKind.SIRET, "01234567400000")
        assertEquals(engine.validate(wrong), engine.validateChecksum(wrong))
    }

    @Test
    fun `a failing format guards the checksum, whichever operation asked`() {
        val tooShort = IdentifierInput(IdentifierKind.SIRET, "0123456789012")
        for (report in listOf(engine.validate(tooShort), engine.validateChecksum(tooShort))) {
            assertEquals(StepStatus.INVALID, report.format.status)
            assertEquals(ReasonCode.INVALID_LENGTH, report.format.reasonCode)
            assertEquals(StepStatus.NOT_RUN, report.checksum.status)
            assertEquals(ReasonCode.NOT_RUN_FORMAT_INVALID, report.checksum.reasonCode)
            assertTrue(report.isInvalid)
        }
    }

    @Test
    fun `validateFormat reports a failing format exactly as validate does`() {
        val tooShort = IdentifierInput(IdentifierKind.SIRET, "0123456789012")
        assertEquals(engine.validate(tooShort), engine.validateFormat(tooShort))
    }

    @Test
    fun `an unpublished algorithm is unsupported, never invalid`() {
        val report = engine.validate(duns)
        assertEquals(StepStatus.VALID, report.format.status)
        assertEquals(StepStatus.UNSUPPORTED, report.checksum.status)
        assertEquals(ReasonCode.CHECKSUM_NOT_PUBLISHED, report.checksum.reasonCode)
        assertFalse(report.isInvalid)
        assertFalse(report.isFullyValidated)
        assertTrue(report.isFormatValid)
        assertFalse(report.isChecksumValid)
    }

    @Test
    fun `canonicalize runs neither format nor checksum`() {
        val result = engine.canonicalize(IdentifierInput(IdentifierKind.SIRET, "012 345 674 00001"))
        assertEquals("01234567400001", result.canonicalValue)
        assertEquals("012 345 674 00001", result.inputValue)
        assertEquals(StepStatus.VALID, result.status)
        assertEquals(ReasonCode.OK, result.reasonCode)
        assertNull(result.messageKey, "canonicalisation runs no rule assertion")
    }

    @Test
    fun `canonicalize never answers not_run`() {
        val inputs = listOf(
            IdentifierInput(IdentifierKind("nope"), "X"),
            IdentifierInput(IdentifierKind.VAT, "0123456749"),
            IdentifierInput(IdentifierKind.VAT, "BE0123456749", "FR"),
            IdentifierInput(IdentifierKind.SIREN, "012345674", "DE"),
            IdentifierInput(IdentifierKind.SIREN, "1".repeat(1025)),
        )
        for (input in inputs) {
            val status = engine.canonicalize(input).status
            assertFalse(status == StepStatus.NOT_RUN, "$input answered $status")
        }
    }

    @Test
    fun `the raw input is kept unchanged in every result`() {
        val raw = "  01234567400001  "
        assertEquals(raw, engine.validate(IdentifierInput(IdentifierKind.SIRET, raw)).inputValue)
        assertEquals(raw, engine.canonicalize(IdentifierInput(IdentifierKind.SIRET, raw)).inputValue)
    }

    @Test
    fun `an absent profile lets the selected definition apply its own default`() {
        // Every definition of this ruleset declares `compatible`, so the two
        // agree today; what matters is that the API can express the difference.
        val silent = engine.validate(siret)
        val explicit = engine.validate(siret, ValidationOptions(ValidationProfile.COMPATIBLE))
        assertEquals(ValidationProfile.COMPATIBLE, silent.profile)
        assertEquals(silent, explicit)
        assertNull(ValidationOptions().profile, "the default options state no preference")
    }

    @Test
    fun `an explicit strict profile is reported back`() {
        val report = engine.validate(siret, ValidationOptions(ValidationProfile.STRICT_CURRENT))
        assertEquals(ValidationProfile.STRICT_CURRENT, report.profile)
    }

    @Test
    fun `an unknown kind is unsupported and keeps the input verbatim`() {
        val report = engine.validate(IdentifierInput(IdentifierKind(" NOT_A_KIND "), "X"))
        assertEquals(StepStatus.UNSUPPORTED, report.format.status)
        assertEquals(ReasonCode.UNSUPPORTED_KIND, report.format.reasonCode)
        assertEquals(StepStatus.NOT_RUN, report.checksum.status)
        assertEquals(ReasonCode.NOT_RUN_FORMAT_UNSUPPORTED, report.checksum.reasonCode)
        assertEquals("not_a_kind", report.kind.value, "the requested token after ASCII trim and lower casing")
        assertEquals("X", report.canonicalValue, "no dispatcher resolved, so the value is the raw input")
    }

    @Test
    fun `a kind alias resolves to its canonical kind`() {
        val report = engine.validate(IdentifierInput(IdentifierKind("vat_number"), "BE0123456749"))
        assertEquals("vat", report.kind.value)
        assertEquals(StepStatus.VALID, report.format.status)
    }

    @Test
    fun `an input above the byte limit is unsupported without being processed`() {
        val long = IdentifierInput(IdentifierKind.SIREN, "1".repeat(1025))
        val report = engine.validate(long)
        assertEquals(StepStatus.UNSUPPORTED, report.format.status)
        assertEquals(ReasonCode.INPUT_TOO_LONG, report.format.reasonCode)
        assertEquals(long.value, report.canonicalValue, "nothing was processed")

        val atTheLimit = IdentifierInput(IdentifierKind.SIREN, "1".repeat(1024))
        assertEquals(ReasonCode.INVALID_LENGTH, engine.validate(atTheLimit).format.reasonCode)
    }

    @Test
    fun `the limit is measured in UTF-8 bytes, not in characters`() {
        // 512 code points, 1024 UTF-8 bytes: right at the bound.
        val atTheLimit = IdentifierInput(IdentifierKind.SIREN, "é".repeat(512))
        assertEquals(ReasonCode.INVALID_LENGTH, engine.validate(atTheLimit).format.reasonCode)
        val over = IdentifierInput(IdentifierKind.SIREN, "é".repeat(513))
        assertEquals(ReasonCode.INPUT_TOO_LONG, engine.validate(over).format.reasonCode)
    }

    @Test
    fun `rulesInfo reports what the engine was built from`() {
        val info = engine.rulesInfo()
        assertEquals(System.getProperty("businessid.rules.version"), info.rulesVersion)
        assertEquals(1, info.formatVersion)
        assertEquals(64, info.sourceDigest.length, "a SHA-256 in lowercase hexadecimal")
        assertTrue(info.sourceDigest.all { it in '0'..'9' || it in 'a'..'f' })
        assertEquals(37, info.supportedKinds.size)
        assertTrue(IdentifierKind.SIRET in info.supportedKinds)
    }

    @Test
    fun `capabilities lists the frozen identifiers this engine implements`() {
        val capabilities = engine.capabilities()
        assertEquals(18, capabilities.size)
        assertEquals(capabilities.map { it.id }.sorted(), capabilities.map { it.id })
        assertEquals(Capability(1, "CORE_GRAPH_V1"), capabilities.first())
        assertEquals(Capability(42, "CHECKSUM_CUSTOM_ALPHABET_V1"), capabilities.last())
    }

    @Test
    fun `the collections the API hands out cannot be modified`() {
        // Read-only by Kotlin's type system is not the same as unmodifiable, and
        // a Java caller can cast the first away.
        @Suppress("UNCHECKED_CAST")
        val capabilities = engine.capabilities() as MutableList<Capability>
        assertFailsWith<UnsupportedOperationException> { capabilities.clear() }

        @Suppress("UNCHECKED_CAST")
        val kinds = engine.rulesInfo().supportedKinds as MutableList<IdentifierKind>
        assertFailsWith<UnsupportedOperationException> { kinds.removeAt(0) }
    }

    @Test
    fun `an input built from a plain kind token behaves the same`() {
        assertEquals(
            engine.validate(siret),
            engine.validate(IdentifierInput.of("siret", "01234567400001")),
        )
    }

    @Test
    fun `every result is a value that compares by content`() {
        assertEquals(engine.validate(siret), engine.validate(siret))
        assertEquals(engine.validate(siret).hashCode(), engine.validate(siret).hashCode())
        assertEquals(engine.canonicalize(siret), engine.canonicalize(siret))
    }
}
