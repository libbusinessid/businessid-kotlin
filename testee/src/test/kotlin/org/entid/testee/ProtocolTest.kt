// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

package org.entid.testee

import entid.conformance.v1.Conformance
import entid.ir.v1.Rules
import entid.testee.v1.Testee
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.assertFailsWith

/**
 * The wire protocol: a length prefix of four bytes, little endian, then the
 * message, strictly one answer per request.
 *
 * The requests are built here rather than read from anywhere.
 */
class ProtocolTest {
    private fun frame(message: com.google.protobuf.MessageLite): ByteArray {
        val payload = message.toByteArray()
        return byteArrayOf(
            (payload.size and 0xFF).toByte(),
            ((payload.size ushr 8) and 0xFF).toByte(),
            ((payload.size ushr 16) and 0xFF).toByte(),
            ((payload.size ushr 24) and 0xFF).toByte(),
        ) + payload
    }

    private fun unframeAll(bytes: ByteArray): List<Testee.TesteeResponse> {
        val out = ArrayList<Testee.TesteeResponse>()
        var i = 0
        while (i < bytes.size) {
            val length = (bytes[i].toInt() and 0xFF) or
                ((bytes[i + 1].toInt() and 0xFF) shl 8) or
                ((bytes[i + 2].toInt() and 0xFF) shl 16) or
                ((bytes[i + 3].toInt() and 0xFF) shl 24)
            i += 4
            out += Testee.TesteeResponse.parseFrom(bytes.copyOfRange(i, i + length))
            i += length
        }
        return out
    }

    private fun request(caseId: String, operation: Conformance.Operation, kind: String, value: String) =
        Testee.TesteeRequest.newBuilder()
            .setCaseId(caseId)
            .setOperation(operation)
            .setKind(kind)
            .setInput(value)
            .setProfile("compatible")
            .build()

    @Test
    fun `one request in, one response out, in order`() {
        val requests = listOf(
            request("a", Conformance.Operation.OPERATION_VALIDATE, "siret", "01234567400001"),
            request("b", Conformance.Operation.OPERATION_CANONICALIZE, "lei", "0000-0000-0000-0000-0098"),
            request("c", Conformance.Operation.OPERATION_VALIDATE_FORMAT, "duns", "012345678"),
            request("d", Conformance.Operation.OPERATION_VALIDATE_CHECKSUM, "siret", "01234567400000"),
        )
        val input = ByteArrayInputStream(requests.fold(ByteArray(0)) { acc, r -> acc + frame(r) })
        val output = ByteArrayOutputStream()
        serve(input, output)

        val responses = unframeAll(output.toByteArray())
        assertEquals(requests.map { it.caseId }, responses.map { it.caseId })
        assertEquals(
            Conformance.StepStatus.STEP_STATUS_VALID,
            responses[0].validationReport.format.status,
        )
        assertEquals("00000000000000000098", responses[1].canonicalization.canonicalValue)
        assertEquals(
            Rules.ReasonCode.REASON_CODE_NOT_REQUESTED,
            responses[2].validationReport.checksum.reasonCode,
        )
        assertEquals(
            Rules.ReasonCode.REASON_CODE_INVALID_CHECKSUM,
            responses[3].validationReport.checksum.reasonCode,
        )
    }

    @Test
    fun `an empty input ends the loop`() {
        val output = ByteArrayOutputStream()
        serve(ByteArrayInputStream(ByteArray(0)), output)
        assertEquals(0, output.size())
    }

    @Test
    fun `a truncated length prefix is an error rather than a silent stop`() {
        val output = ByteArrayOutputStream()
        assertFailsWith<IllegalStateException> {
            serve(ByteArrayInputStream(byteArrayOf(1, 2)), output)
        }
    }

    @Test
    fun `a country and a message key travel both ways`() {
        val request = Testee.TesteeRequest.newBuilder()
            .setCaseId("x")
            .setOperation(Conformance.Operation.OPERATION_VALIDATE)
            .setKind("siret")
            .setInput("0123456789012")
            .setCountryCode("FR")
            .setProfile("compatible")
            .build()
        val output = ByteArrayOutputStream()
        serve(ByteArrayInputStream(frame(request)), output)
        val report = unframeAll(output.toByteArray()).single().validationReport
        assertEquals("FR", report.countryCode)
        assertEquals("fr.siret.length", report.format.messageKey)
        assertFalse(report.checksum.hasMessageKey(), "a step produced before any assertion carries no key")
    }

    @Test
    fun `an absent country is absent on the wire, not empty`() {
        val request = request("x", Conformance.Operation.OPERATION_VALIDATE, "lei", "00000000000000000098")
        val output = ByteArrayOutputStream()
        serve(ByteArrayInputStream(frame(request)), output)
        val report = unframeAll(output.toByteArray()).single().validationReport
        assertFalse(report.hasCountryCode())
    }

    @Test
    fun `a ruleset case is answered by the generator`() {
        val hostile = Testee.TesteeRequest.newBuilder()
            .setCaseId("loader")
            .setOperation(Conformance.Operation.OPERATION_LOAD_RULESET)
            .setRulesPayload(com.google.protobuf.ByteString.copyFrom(byteArrayOf(0x08, 0x02)))
            .build()
        val output = ByteArrayOutputStream()
        serve(ByteArrayInputStream(frame(hostile)), output)
        val load = unframeAll(output.toByteArray()).single().load
        assertFalse(load.accepted)
        assertEquals("incompatible_ruleset", load.engineError)
    }

    @Test
    fun `an acceptable ruleset is reported as accepted`() {
        val accepted = Testee.TesteeRequest.newBuilder()
            .setCaseId("loader")
            .setOperation(Conformance.Operation.OPERATION_LOAD_RULESET)
            .setRulesPayload(
                com.google.protobuf.ByteString.copyFrom(
                    java.io.File(System.getProperty("entid.spec.dir"), "entid-rules.binpb").readBytes(),
                ),
            )
            .build()
        val output = ByteArrayOutputStream()
        serve(ByteArrayInputStream(frame(accepted)), output)
        val load = unframeAll(output.toByteArray()).single().load
        assertTrue(load.accepted)
        assertEquals("", load.engineError)
    }

    @Test
    fun `a profile no version of this API accepts is refused rather than guessed`() {
        val request = Testee.TesteeRequest.newBuilder()
            .setCaseId("x")
            .setOperation(Conformance.Operation.OPERATION_VALIDATE)
            .setKind("siret")
            .setInput("01234567400001")
            .setProfile("lenient")
            .build()
        val failure = assertFailsWith<IllegalStateException> { Answering.answer(request) }
        assertTrue("lenient" in (failure.message ?: ""))
    }

    @Test
    fun `a request without a kind still gets an answer`() {
        val request = Testee.TesteeRequest.newBuilder()
            .setCaseId("x")
            .setOperation(Conformance.Operation.OPERATION_VALIDATE)
            .setInput("01234567400001")
            .build()
        val response = Answering.answer(request)
        assertEquals(
            Rules.ReasonCode.REASON_CODE_UNSUPPORTED_KIND,
            response.validationReport.format.reasonCode,
        )
    }
}
