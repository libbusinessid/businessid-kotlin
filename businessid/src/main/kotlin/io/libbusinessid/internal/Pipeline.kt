// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.internal

import io.libbusinessid.CanonicalizationResult
import io.libbusinessid.IdentifierInput
import io.libbusinessid.IdentifierKind
import io.libbusinessid.ReasonCode
import io.libbusinessid.StepResult
import io.libbusinessid.StepStatus
import io.libbusinessid.ValidationLevel
import io.libbusinessid.ValidationProfile
import io.libbusinessid.ValidationReport
import io.libbusinessid.generated.Ruleset
import io.libbusinessid.runtime.CanonBuffer
import io.libbusinessid.runtime.ChecksumOutcome
import io.libbusinessid.runtime.ChecksumStatus
import io.libbusinessid.runtime.CpView
import io.libbusinessid.runtime.Utf

/** Which of the four public operations is being run. */
internal enum class Operation {
    CANONICALIZE,
    VALIDATE_FORMAT,
    VALIDATE,
}

/**
 * The normative pipeline: input bound, dispatch, canonicalisation, format,
 * checksum. It owns no mutable state and keeps nothing between two calls.
 */
internal object Pipeline {
    /** Safety bound on the raw input, in UTF-8 bytes. */
    const val MAX_INPUT_BYTES: Int = 1024

    private val NOT_RUN_AFTER_UNSUPPORTED =
        StepResult(ValidationLevel.CHECKSUM, StepStatus.NOT_RUN, ReasonCode.NOT_RUN_FORMAT_UNSUPPORTED)
    private val NOT_RUN_AFTER_INVALID =
        StepResult(ValidationLevel.CHECKSUM, StepStatus.NOT_RUN, ReasonCode.NOT_RUN_FORMAT_INVALID)
    private val NOT_REQUESTED =
        StepResult(ValidationLevel.CHECKSUM, StepStatus.NOT_RUN, ReasonCode.NOT_REQUESTED)
    private val FORMAT_OK =
        StepResult(ValidationLevel.FORMAT, StepStatus.VALID, ReasonCode.OK)
    private val CHECKSUM_OK =
        StepResult(ValidationLevel.CHECKSUM, StepStatus.VALID, ReasonCode.OK)

    fun canonicalize(input: IdentifierInput, profile: ValidationProfile?): CanonicalizationResult {
        val d = dispatch(input, profile)
        return CanonicalizationResult(
            kind = IdentifierKind(d.kind),
            inputValue = input.value,
            canonicalValue = d.canonicalValue,
            countryCode = d.countryCode,
            profile = d.profile,
            rulesVersion = Ruleset.RULES_VERSION,
            formatVersion = Ruleset.FORMAT_VERSION,
            engineVersion = EngineVersion.VALUE,
            status = d.status,
            reasonCode = d.reason,
            messageKey = null,
        )
    }

    fun validate(input: IdentifierInput, profile: ValidationProfile?, operation: Operation): ValidationReport {
        val d = dispatch(input, profile)
        if (d.status != StepStatus.VALID) {
            val level = ValidationLevel.FORMAT
            return report(
                d,
                StepResult(level, d.status, d.reason),
                if (d.status == StepStatus.INVALID) NOT_RUN_AFTER_INVALID else NOT_RUN_AFTER_UNSUPPORTED,
            )
        }

        val ctx = EvalContext(d.canonicalView, d.profile, d.target)
        val failure = runFormat(d.definition, ctx)
        if (failure != null) {
            return report(
                d,
                StepResult(ValidationLevel.FORMAT, StepStatus.INVALID, failure.reason, failure.messageKey),
                NOT_RUN_AFTER_INVALID,
            )
        }

        if (operation == Operation.VALIDATE_FORMAT) {
            return report(d, FORMAT_OK, NOT_REQUESTED)
        }

        return report(d, FORMAT_OK, checksumStep(d, ctx))
    }

    private fun checksumStep(d: Dispatched, ctx: EvalContext): StepResult {
        val outcome = runChecksum(d.definition, ctx)
            ?: return StepResult(
                ValidationLevel.CHECKSUM,
                StepStatus.UNSUPPORTED,
                Ruleset.absentChecksumReason(d.definition),
            )
        return if (outcome.status == ChecksumStatus.VALID) {
            CHECKSUM_OK
        } else {
            StepResult(ValidationLevel.CHECKSUM, outcome.status.step, outcome.reason, outcome.messageKey)
        }
    }

    private fun runFormat(definition: Int, ctx: EvalContext) =
        guardEngineErrors("format") { Ruleset.format(definition, ctx) }

    private fun runChecksum(definition: Int, ctx: EvalContext): ChecksumOutcome? =
        guardEngineErrors("checksum") { Ruleset.checksum(definition, ctx) }

    private fun report(d: Dispatched, format: StepResult, checksum: StepResult) = ValidationReport(
        kind = IdentifierKind(d.kind),
        inputValue = d.rawInput,
        canonicalValue = d.canonicalValue,
        countryCode = d.countryCode,
        profile = d.profile,
        rulesVersion = Ruleset.RULES_VERSION,
        formatVersion = Ruleset.FORMAT_VERSION,
        engineVersion = EngineVersion.VALUE,
        format = format,
        checksum = checksum,
    )

    /**
     * The outcome of the dispatch phase.
     *
     * [status] is `valid` when a definition was selected. Everything else is a
     * terminal answer, and the identity fields already hold what the report must
     * carry for that branch.
     */
    @Suppress("LongParameterList")
    private class Dispatched(
        val rawInput: String,
        val kind: String,
        val canonicalValue: String,
        val countryCode: String?,
        val profile: ValidationProfile,
        val status: StepStatus,
        val reason: ReasonCode,
        val definition: Int = -1,
        val target: Int = -1,
        val canonicalView: CpView = EMPTY_VIEW,
    )

    @Suppress("CyclomaticComplexMethod", "ReturnCount", "LongMethod")
    private fun dispatch(input: IdentifierInput, requested: ValidationProfile?): Dispatched {
        val raw = input.value
        val dispatchProfile = requested ?: ValidationProfile.COMPATIBLE
        val kindToken = Tokens.asciiLower(Tokens.asciiTrim(input.kind.value))

        // 1. Safety bound, before anything is processed.
        if (Utf.utf8Length(raw) > MAX_INPUT_BYTES) {
            return unresolved(input, kindToken, dispatchProfile, ReasonCode.INPUT_TOO_LONG)
        }

        // 2. Text that does not encode has no code points to evaluate.
        if (!Utf.isWellFormed(raw)) {
            return unresolved(input, kindToken, dispatchProfile, ReasonCode.INVALID_ENCODING)
        }

        // 3. Kind normalisation and dispatcher selection.
        val d = Ruleset.dispatcherOf(kindToken)
        if (d < 0) {
            return unresolved(input, kindToken, dispatchProfile, ReasonCode.UNSUPPORTED_KIND)
        }
        val canonicalKind = Ruleset.dispatcherKind(d)

        // 4. Pre-canonicalisation, before any country decision, so a result that
        //    stops below still carries the pre-canonical value.
        val buffer = CanonBuffer(Utf.codePoints(raw))
        Ruleset.preCanonicalize(d, buffer, dispatchProfile)

        // 5. Country normalisation.
        var normalizedCountry: String? = null
        var countryTarget = -1
        val rawCountry = input.countryCode
        if (rawCountry != null) {
            val token = Tokens.asciiUpper(Tokens.asciiTrim(rawCountry))
            if (token.isNotEmpty()) {
                if (!Tokens.isCountryToken(token)) {
                    return partial(
                        input,
                        canonicalKind,
                        buffer.snapshot(),
                        rawCountry,
                        dispatchProfile,
                        StepStatus.UNSUPPORTED,
                        ReasonCode.UNSUPPORTED_COUNTRY,
                    )
                }
                normalizedCountry = Ruleset.countryAlias(d, token)
                countryTarget = Ruleset.countryTarget(d, normalizedCountry)
                if (countryTarget < 0 && Ruleset.globalTarget(d) < 0) {
                    return partial(
                        input,
                        canonicalKind,
                        buffer.snapshot(),
                        normalizedCountry,
                        dispatchProfile,
                        StepStatus.UNSUPPORTED,
                        ReasonCode.UNSUPPORTED_COUNTRY,
                    )
                }
            }
        }

        // 6 and 7. Longest declared prefix, then the proven contradiction.
        val prefixTarget = Ruleset.prefixTarget(d, buffer.view())
        if (countryTarget >= 0 && prefixTarget >= 0 && countryTarget != prefixTarget) {
            return partial(
                input,
                canonicalKind,
                buffer.snapshot(),
                normalizedCountry,
                dispatchProfile,
                StepStatus.INVALID,
                ReasonCode.COUNTRY_MISMATCH,
            )
        }

        // 8 and 9. Target selection.
        var target = countryTarget
        if (target < 0) target = prefixTarget
        if (target < 0) target = Ruleset.globalTarget(d)
        if (target < 0) target = Ruleset.unprefixedTarget(d)
        if (target < 0) {
            return partial(
                input,
                canonicalKind,
                buffer.snapshot(),
                normalizedCountry,
                dispatchProfile,
                StepStatus.UNSUPPORTED,
                ReasonCode.MISSING_COUNTRY_CODE,
            )
        }

        // 10. The definition owns the resolved profile and its canonicalisation.
        val definition = Ruleset.definitionOf(target)
        val profile = requested ?: Ruleset.defaultProfile(definition)
        Ruleset.canonicalize(definition, target, buffer, profile)

        val canonicalCodePoints = buffer.view()
        return Dispatched(
            rawInput = raw,
            kind = canonicalKind,
            canonicalValue = buffer.snapshot(),
            countryCode = Ruleset.targetCountry(target) ?: normalizedCountry,
            profile = profile,
            status = StepStatus.VALID,
            reason = ReasonCode.OK,
            definition = definition,
            target = target,
            canonicalView = canonicalCodePoints,
        )
    }

    private fun unresolved(input: IdentifierInput, kindToken: String, profile: ValidationProfile, reason: ReasonCode) =
        Dispatched(
            rawInput = input.value,
            kind = kindToken,
            canonicalValue = input.value,
            countryCode = input.countryCode,
            profile = profile,
            status = StepStatus.UNSUPPORTED,
            reason = reason,
        )

    @Suppress("LongParameterList")
    private fun partial(
        input: IdentifierInput,
        kind: String,
        preCanonical: String,
        country: String?,
        profile: ValidationProfile,
        status: StepStatus,
        reason: ReasonCode,
    ) = Dispatched(
        rawInput = input.value,
        kind = kind,
        canonicalValue = preCanonical,
        countryCode = country,
        profile = profile,
        status = status,
        reason = reason,
    )

    private val EMPTY_VIEW: CpView = CpView.ofEmpty()
}
