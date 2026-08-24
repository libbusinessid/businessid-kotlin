// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Every example the README shows, run.
 *
 * Documentation that is not executed drifts, and an example that no longer does
 * what it says is worse than none: a reader trusts it.
 */
class ReadmeTest {
    private val engine = BusinessIdEngine.default()

    private val readme: String by lazy {
        File(System.getProperty("businessid.spec.dir")).parentFile.resolve("README.md").readText()
    }

    @Test
    fun `the Kotlin example`() {
        val report = engine.validate(IdentifierInput(IdentifierKind.SIRET, "012 345 674 00001"))
        assertEquals("01234567400001", report.canonicalValue)
        assertTrue(report.isFullyValidated)
    }

    @Test
    fun `the format only example`() {
        val report = engine.validateFormat(IdentifierInput(IdentifierKind.SIRET, "01234567400001"))
        assertEquals(StepStatus.VALID, report.format.status)
        assertEquals(StepStatus.NOT_RUN, report.checksum.status)
        assertEquals(ReasonCode.NOT_REQUESTED, report.checksum.reasonCode)
    }

    @Test
    fun `the unpublished algorithm example`() {
        val report = engine.validate(IdentifierInput(IdentifierKind.DUNS, "012345678"))
        assertEquals(StepStatus.VALID, report.format.status)
        assertEquals(StepStatus.UNSUPPORTED, report.checksum.status)
        assertEquals(ReasonCode.CHECKSUM_NOT_PUBLISHED, report.checksum.reasonCode)
        assertFalse(report.isInvalid)
    }

    @Test
    fun `the canonicalisation example`() {
        val result = engine.canonicalize(IdentifierInput(IdentifierKind.LEI, "0000-0000-0000-0000-0098"))
        assertEquals("00000000000000000098", result.canonicalValue)
        assertEquals(StepStatus.VALID, result.status)
    }

    @Test
    fun `the country context example`() {
        val report = engine.validate(IdentifierInput(IdentifierKind.VAT, "012345670", countryCode = "GR"))
        assertEquals("EL012345670", report.canonicalValue)
        assertEquals("GR", report.countryCode)
    }

    @Test
    fun `the country mismatch example`() {
        val report = engine.validate(IdentifierInput(IdentifierKind.VAT, "BE0123456749", countryCode = "FR"))
        assertEquals(ReasonCode.COUNTRY_MISMATCH, report.format.reasonCode)
    }

    @Test
    fun `the profile example`() {
        val input = IdentifierInput(IdentifierKind.SIRET, "01234567400001")
        engine.validate(input)
        engine.validate(input, ValidationOptions(ValidationProfile.STRICT_CURRENT))
    }

    @Test
    fun `the Java example, from Kotlin`() {
        val report = engine.validate(IdentifierInput.of("siret", "01234567400001"))
        assertEquals(StepStatus.VALID, report.format.status)
        assertEquals("siret", report.kindToken)
    }

    @Test
    fun `the figures the README states`() {
        val info = engine.rulesInfo()
        assertTrue("rules ${info.rulesVersion}, format version ${info.formatVersion}" in readme)
        assertTrue("${info.supportedKinds.size} kinds" in readme)
        // Counted rather than repeated: the corpus is one reviewed case per
        // line, and a resync moves the number without touching this test.
        val cases = File(System.getProperty("businessid.spec.dir"), "businessid-conformance.jsonl")
            .readLines()
            .count { it.isNotBlank() }
        assertTrue("$cases of $cases cases matched" in readme, "the README states another conformance count")
        assertTrue("$cases cases, $cases matched, 0 differed" in readme)
        // The sample line of `verify.sh`. Its test count and jar size are
        // illustrative and move with any change, but the two figures that can be
        // checked cheaply are checked, because a README figure repeated by hand
        // has gone stale here before.
        val verifyLine = readme.lineSequence().firstOrNull { it.startsWith("verify ok") }
        assertNotNull(verifyLine, "the README should show what verify.sh prints")
        assertTrue(
            "rules ${info.rulesVersion} " in verifyLine!!,
            "the sample verify line names another rules version: $verifyLine",
        )
        assertTrue(
            "conformance $cases/$cases " in verifyLine,
            "the sample verify line states another conformance tally: $verifyLine",
        )
    }

    @Test
    fun `every identifier the README shows is one this engine answers as the README says`() {
        // The values below are the ones the README prints. A value that stopped
        // behaving as documented would be caught by the tests above; this one
        // catches a value appearing in the README without a case behind it.
        val documented = Regex("""\b\d{9,20}\b""").findAll(readme).map { it.value }.toSet()
        val known = setOf(
            "01234567400001",
            "012345678",
            "00000000000000000098",
            "0123456749",
            "012345670",
            "0000000000000000",
        )
        val unexplained = documented.filterNot { value ->
            known.any { it == value || value in it || it in value }
        }
        assertEquals(emptyList<String>(), unexplained, "an identifier appears in the README with no case behind it")
    }
}
