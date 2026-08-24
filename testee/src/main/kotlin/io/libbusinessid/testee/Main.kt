// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.testee

import io.libbusinessid.BusinessIdEngine
import io.libbusinessid.IdentifierInput
import io.libbusinessid.IdentifierKind
import io.libbusinessid.ValidationOptions
import io.libbusinessid.ValidationProfile
import io.libbusinessid.generator.RulesetGate
import libbusinessid.conformance.v1.Conformance
import libbusinessid.testee.v1.Testee
import java.io.DataInputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Answers one conformance request at a time.
 *
 * It never sees an expectation, never reads the corpus, and never touches the
 * case identifier for anything but echoing it back. Everything it can do is
 * translate a request into a call on the public API and a result into a
 * response — which is what makes the absence of cheating checkable by reading
 * it.
 */
internal object Answering {
    private val engine = BusinessIdEngine.default()

    fun answer(request: Testee.TesteeRequest): Testee.TesteeResponse {
        val response = Testee.TesteeResponse.newBuilder().setCaseId(request.caseId)
        return when (request.operation) {
            Conformance.Operation.OPERATION_CANONICALIZE ->
                response.setCanonicalization(canonicalize(request)).build()
            Conformance.Operation.OPERATION_VALIDATE_FORMAT,
            Conformance.Operation.OPERATION_VALIDATE_CHECKSUM,
            Conformance.Operation.OPERATION_VALIDATE,
            -> response.setValidationReport(validate(request)).build()
            Conformance.Operation.OPERATION_LOAD_RULESET -> response.setLoad(load(request)).build()
            else -> response.setFailure(
                Testee.TesteeFailure.newBuilder()
                    .setKind(Testee.FailureKind.FAILURE_KIND_UNSUPPORTED_OPERATION)
                    .setDetail("operation ${request.operationValue}"),
            ).build()
        }
    }

    private fun input(request: Testee.TesteeRequest) =
        IdentifierInput(
            kind = IdentifierKind(if (request.hasKind()) request.kind else ""),
            value = request.input,
            countryCode = if (request.hasCountryCode()) request.countryCode else null,
        )

    /**
     * The absence of a profile is meaningful: it is what lets a definition apply
     * its own default, so it must never be conflated with a profile named "".
     */
    private fun options(request: Testee.TesteeRequest): ValidationOptions {
        if (!request.hasProfile()) return ValidationOptions()
        val profile = ValidationProfile.entries.firstOrNull { it.wireName == request.profile }
            ?: error("the runner named the profile ${request.profile}, which no version of this API accepts")
        return ValidationOptions(profile)
    }

    private fun canonicalize(request: Testee.TesteeRequest): Testee.ObservedCanonicalization {
        val result = engine.canonicalize(input(request), options(request))
        val observed = Testee.ObservedCanonicalization.newBuilder()
            .setKind(result.kind.value)
            .setCanonicalValue(result.canonicalValue)
            .setStatus(status(result.status.wireName))
            .setReasonCode(reason(result.reasonCode.wireName))
        result.countryCode?.let { observed.setCountryCode(it) }
        return observed.build()
    }

    private fun validate(request: Testee.TesteeRequest): Testee.ObservedValidationReport {
        val identifier = input(request)
        val opts = options(request)
        val report = when (request.operation) {
            Conformance.Operation.OPERATION_VALIDATE_FORMAT -> engine.validateFormat(identifier, opts)
            Conformance.Operation.OPERATION_VALIDATE_CHECKSUM -> engine.validateChecksum(identifier, opts)
            else -> engine.validate(identifier, opts)
        }
        val observed = Testee.ObservedValidationReport.newBuilder()
            .setKind(report.kind.value)
            .setCanonicalValue(report.canonicalValue)
            .setFormat(step(report.format.status.wireName, report.format.reasonCode.wireName, report.format.messageKey))
            .setChecksum(
                step(report.checksum.status.wireName, report.checksum.reasonCode.wireName, report.checksum.messageKey),
            )
        report.countryCode?.let { observed.setCountryCode(it) }
        return observed.build()
    }

    /**
     * A ruleset case addresses the generator, not the engine: the published
     * library loads nothing, so refusing a hostile ruleset is a property of the
     * build. This is what the comment on field 7 of `testee.proto` describes.
     */
    private fun load(request: Testee.TesteeRequest): Testee.ObservedLoad {
        val builder = Testee.ObservedLoad.newBuilder()
        val refusal = RulesetGate.inspect(request.rulesPayload.toByteArray())
            ?: return builder.setAccepted(true).build()
        return builder.setAccepted(false).setEngineError(refusal.errorKind).build()
    }

    private fun step(status: String, reason: String, messageKey: String?): Testee.ObservedStep {
        val builder = Testee.ObservedStep.newBuilder()
            .setStatus(status(status))
            .setReasonCode(reason(reason))
        messageKey?.let { builder.setMessageKey(it) }
        return builder.build()
    }

    private fun status(wireName: String): Conformance.StepStatus =
        Conformance.StepStatus.valueOf("STEP_STATUS_" + wireName.uppercase())

    private fun reason(wireName: String): libbusinessid.ir.v1.Rules.ReasonCode =
        libbusinessid.ir.v1.Rules.ReasonCode.valueOf("REASON_CODE_" + wireName.uppercase())
}

/**
 * The framing: every message is preceded by its length as a 32 bit unsigned
 * integer, little endian. The exchange is strictly synchronous, so one read is
 * followed by exactly one write.
 */
internal class Framing(input: InputStream, private val output: OutputStream) {
    private val stream = DataInputStream(input.buffered())

    fun read(): ByteArray? {
        val header = ByteArray(4)
        var got = 0
        while (got < 4) {
            val n = stream.read(header, got, 4 - got)
            if (n < 0) return if (got == 0) null else error("truncated length prefix")
            got += n
        }
        val length = (header[0].toInt() and 0xFF) or
            ((header[1].toInt() and 0xFF) shl 8) or
            ((header[2].toInt() and 0xFF) shl 16) or
            ((header[3].toInt() and 0xFF) shl 24)
        val payload = ByteArray(length)
        stream.readFully(payload)
        return payload
    }

    fun write(payload: ByteArray) {
        output.write(payload.size and 0xFF)
        output.write((payload.size ushr 8) and 0xFF)
        output.write((payload.size ushr 16) and 0xFF)
        output.write((payload.size ushr 24) and 0xFF)
        output.write(payload)
        output.flush()
    }
}

/**
 * The read-answer loop, over any pair of streams so a test can drive it without
 * starting a process.
 */
internal fun serve(input: InputStream, output: OutputStream) {
    val framing = Framing(input, output)
    while (true) {
        val payload = framing.read() ?: return
        val request = Testee.TesteeRequest.parseFrom(payload)
        framing.write(Answering.answer(request).toByteArray())
    }
}

fun main() {
    serve(System.`in`, System.out)
}
