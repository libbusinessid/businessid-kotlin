// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.testee

import libbusinessid.conformance.v1.Conformance
import libbusinessid.testee.v1.Testee
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * That the testee does not cheat, stated as properties rather than as intent.
 *
 * A conformance verdict means something only if the program under test cannot
 * have recognised the case it was given. `engine.md` section 11.3 fixes the form
 * these tests must take, and this file is that table:
 *
 * | what is asserted | what it excludes |
 * | --- | --- |
 * | the testee names neither the corpus nor anything that reads one | reading the expectations directly |
 * | it reaches no file system | the corpus is a file; whoever opens nothing reads none |
 * | it answers identically whatever the case identifier | recognising a case |
 * | it answers identically whatever the order of requests | behaviour that depends on history |
 * | it answers identically to a repeated request | non-determinism |
 *
 * The requests below are invented here. This file does not open the corpus
 * either — doing so would demonstrate the opposite of what it asserts.
 */
class TesteeHonestyTest {
    private fun request(
        caseId: String,
        operation: Conformance.Operation,
        kind: String,
        value: String,
        country: String? = null,
        profile: String? = "compatible",
    ): Testee.TesteeRequest {
        val builder = Testee.TesteeRequest.newBuilder()
            .setCaseId(caseId)
            .setOperation(operation)
            .setInput(value)
            .setKind(kind)
        country?.let { builder.setCountryCode(it) }
        profile?.let { builder.setProfile(it) }
        return builder.build()
    }

    /** Invented on the spot: plausible, absurd and empty alike. */
    private fun inventedRequests(caseId: String): List<Testee.TesteeRequest> = listOf(
        request(caseId, Conformance.Operation.OPERATION_VALIDATE, "siret", "01234567400001"),
        request(caseId, Conformance.Operation.OPERATION_VALIDATE, "siret", "01234567400000"),
        request(caseId, Conformance.Operation.OPERATION_VALIDATE_FORMAT, "lei", "00000000000000000098"),
        request(caseId, Conformance.Operation.OPERATION_VALIDATE_CHECKSUM, "vat", "BE0123456749"),
        request(caseId, Conformance.Operation.OPERATION_CANONICALIZE, "vat", "  el 012345670 "),
        request(caseId, Conformance.Operation.OPERATION_VALIDATE, "vat", "0123456749", country = "FR"),
        request(caseId, Conformance.Operation.OPERATION_VALIDATE, "no_such_kind", "X"),
        request(caseId, Conformance.Operation.OPERATION_VALIDATE, "", ""),
        request(caseId, Conformance.Operation.OPERATION_VALIDATE, "siren", "012345674", profile = null),
        request(caseId, Conformance.Operation.OPERATION_VALIDATE, "siren", "012345674", profile = "strict_current"),
    )

    private fun withoutCaseId(response: Testee.TesteeResponse): Testee.TesteeResponse =
        response.toBuilder().clearCaseId().build()

    @Test
    fun `the sources name neither the corpus nor anything that reads one`() {
        val sources = File("src/main/kotlin").walkTopDown().filter { it.extension == "kt" }.toList()
        assertTrue(sources.isNotEmpty(), "no testee source found from ${File(".").absolutePath}")
        val forbidden = listOf(
            "businessid-conformance",
            "ConformanceBundle",
            "ConformanceCase",
            "ExpectedOutcome",
            "ExpectedValidationReport",
            "ExpectedCanonicalization",
            "expectedEngineError",
            "java.io.File",
            "java.nio.file",
            "readBytes",
            "readText",
            "getResource",
        )
        for (source in sources) {
            val text = source.readText()
            for (symbol in forbidden) {
                assertTrue(symbol !in text, "${source.name} names $symbol")
            }
        }
    }

    @Test
    fun `the compiled testee reaches no file system`() {
        // From the build rather than from a path relative to the working
        // directory: `-Pbusinessid.toolchain` builds elsewhere, and a literal
        // here read the classes of whichever build happened to have run last.
        val directory = File(requireNotNull(System.getProperty("businessid.main.classes")))
        val classes = directory.walkTopDown().filter { it.extension == "class" }.toList()
        assertTrue(classes.isNotEmpty(), "no compiled testee class found under $directory")
        val forbidden = listOf(
            "java/io/File",
            "java/io/FileInputStream",
            "java/nio/file/Files",
            "java/nio/file/Paths",
            "getResourceAsStream",
        )
        for (file in classes) {
            val text = file.readBytes().decodeToString()
            for (symbol in forbidden) {
                assertTrue(symbol !in text, "${file.name} references $symbol")
            }
        }
    }

    @Test
    fun `it answers identically whatever the case identifier`() {
        val identifiers = listOf(
            "siret-synthetic-valid-001",
            "an-identifier-no-corpus-has-ever-carried",
            "",
            "0",
            "../../etc/passwd",
            "😀",
        )
        val baseline = inventedRequests(identifiers.first()).map { withoutCaseId(Answering.answer(it)) }
        for (identifier in identifiers) {
            val answers = inventedRequests(identifier).map { withoutCaseId(Answering.answer(it)) }
            assertEquals(baseline, answers, "the answers changed with the case identifier $identifier")
        }
    }

    @Test
    fun `it echoes the case identifier and uses it for nothing else`() {
        for (identifier in listOf("a", "b", "")) {
            for (request in inventedRequests(identifier)) {
                assertEquals(identifier, Answering.answer(request).caseId)
            }
        }
    }

    @Test
    fun `it answers identically whatever the order of requests`() {
        val requests = inventedRequests("order")
        val forwards = requests.map { withoutCaseId(Answering.answer(it)) }
        val backwards = requests.reversed().map { withoutCaseId(Answering.answer(it)) }.reversed()
        assertEquals(forwards, backwards)

        val shuffled = requests.shuffled(
            java.util.Random(20260824).let { rng ->
                kotlin.random.Random(rng.nextLong())
            },
        )
        val byShuffle = shuffled.associateWith { withoutCaseId(Answering.answer(it)) }
        for ((index, request) in requests.withIndex()) {
            assertEquals(forwards[index], byShuffle.getValue(request))
        }
    }

    @Test
    fun `it answers identically to a repeated request`() {
        for (request in inventedRequests("repeat")) {
            val first = Answering.answer(request)
            repeat(5) { assertEquals(first, Answering.answer(request)) }
        }
    }

    @Test
    fun `an operation it does not implement is reported as such, not guessed`() {
        val response = Answering.answer(
            request("x", Conformance.Operation.OPERATION_UNSPECIFIED, "siret", "01234567400001"),
        )
        assertEquals(
            Testee.FailureKind.FAILURE_KIND_UNSUPPORTED_OPERATION,
            response.failure.kind,
        )
    }

    @Test
    fun `an absent profile is not the same request as an empty one`() {
        // Section 5.2 of ir.md makes the absence meaningful, and the protocol
        // carries explicit presence so the two sides cannot read it differently.
        val absent = request("x", Conformance.Operation.OPERATION_VALIDATE, "siren", "012345674", profile = null)
        val named = request("x", Conformance.Operation.OPERATION_VALIDATE, "siren", "012345674", profile = "compatible")
        assertTrue(!absent.hasProfile())
        assertTrue(named.hasProfile())
        // They agree today because every definition declares `compatible`; what
        // matters is that the testee passes the distinction through untouched.
        assertEquals(Answering.answer(absent), Answering.answer(named))
    }
}
