// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

package org.entid.testee

import libbusinessid.conformance.v1.Conformance
import libbusinessid.testee.v1.Testee
import org.entid.EntIdEngine
import org.entid.IdentifierInput
import org.entid.IdentifierKind
import org.entid.ValidationOptions
import org.entid.ValidationProfile
import org.entid.generator.RulesetGate
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
    private val engine = EntIdEngine.default()

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

    private fun input(request: Testee.TesteeRequest) = IdentifierInput(
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
    private companion object {
        /** A 32 bit unsigned length, little endian. */
        const val PREFIX_BYTES = 4
        const val BYTE_MASK = 0xFF
        const val BITS_PER_BYTE = 8
    }

    private val stream = DataInputStream(input.buffered())

    /** The next message, or null when the runner closed its end. */
    fun read(): ByteArray? {
        val header = ByteArray(PREFIX_BYTES)
        var got = 0
        while (got < PREFIX_BYTES) {
            val n = stream.read(header, got, PREFIX_BYTES - got)
            if (n < 0) return if (got == 0) null else error("truncated length prefix")
            got += n
        }
        var length = 0
        for (i in PREFIX_BYTES - 1 downTo 0) {
            length = (length shl BITS_PER_BYTE) or (header[i].toInt() and BYTE_MASK)
        }
        val payload = ByteArray(length)
        stream.readFully(payload)
        return payload
    }

    /** One message, prefixed by its length and flushed at once. */
    fun write(payload: ByteArray) {
        var length = payload.size
        repeat(PREFIX_BYTES) {
            output.write(length and BYTE_MASK)
            length = length ushr BITS_PER_BYTE
        }
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

/** Reads requests on the standard input and writes responses on the standard output. */
fun main() {
    serve(System.`in`, System.out)
}
