// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The public API called the way a Java project would call it.
 *
 * {@code IdentifierKind} is a Kotlin value class, so Java sees the token itself.
 * {@code IdentifierInput.of} exists for exactly that reason, and the overloads
 * that stand in for Kotlin's default arguments are what keep the happy path one
 * line long here too.
 *
 * Every value is synthetic and drawn from the shared conformance corpus.
 */
class JavaApiTest {

    private final BusinessIdEngine engine = BusinessIdEngine.defaultEngine();

    @Test
    void validatesFromJavaWithoutOptions() {
        ValidationReport report = engine.validate(IdentifierInput.of("siret", "01234567400001"));
        assertEquals(StepStatus.VALID, report.getFormat().getStatus());
        assertEquals(StepStatus.VALID, report.getChecksum().getStatus());
        assertTrue(report.isFullyValidated());
        assertFalse(report.isInvalid());
    }

    @Test
    void readsTheReasonOfAFailingStep() {
        ValidationReport report = engine.validate(IdentifierInput.of("siret", "0123456789012"));
        assertEquals(StepStatus.INVALID, report.getFormat().getStatus());
        assertEquals(ReasonCode.INVALID_LENGTH, report.getFormat().getReasonCode());
        assertEquals("invalid_length", report.getFormat().getReasonCode().getWireName());
        assertEquals(StepStatus.NOT_RUN, report.getChecksum().getStatus());
    }

    @Test
    void passesACountryContextAndAProfile() {
        IdentifierInput input = IdentifierInput.of("vat", "0123456749", "BE");
        ValidationReport report = engine.validate(input, new ValidationOptions(ValidationProfile.COMPATIBLE));
        assertEquals("BE0123456749", report.getCanonicalValue());
        assertEquals("BE", report.getCountryCode());
    }

    @Test
    void canonicalisesWithoutValidating() {
        CanonicalizationResult result = engine.canonicalize(IdentifierInput.of("siret", "012 345 674 00001"));
        assertEquals("01234567400001", result.getCanonicalValue());
        assertEquals(StepStatus.VALID, result.getStatus());
    }

    @Test
    void namesAKindByItsToken() {
        // IdentifierKind is a Kotlin value class, which the specification
        // requires so that an unknown kind stays representable. Java sees the
        // erased String and reads the constants through IdentifierInput.of,
        // which is the entry point written for it.
        assertEquals("siret", engine.validate(IdentifierInput.of("siret", "01234567400001")).getKindToken());
        assertEquals("siret", IdentifierInput.of("siret", "x").getKindToken());
    }

    @Test
    void readsTheMetadata() {
        RulesInfo info = engine.rulesInfo();
        assertEquals("2026.08.26", info.getRulesVersion());
        assertEquals(1, info.getFormatVersion());
        assertFalse(engine.capabilities().isEmpty());
    }

    @Test
    void anUnpublishedAlgorithmIsUnsupportedRatherThanInvalid() {
        ValidationReport report = engine.validate(IdentifierInput.of("duns", "012345678"));
        assertEquals(StepStatus.UNSUPPORTED, report.getChecksum().getStatus());
        assertEquals(ReasonCode.CHECKSUM_NOT_PUBLISHED, report.getChecksum().getReasonCode());
        assertFalse(report.isInvalid());
    }
}
