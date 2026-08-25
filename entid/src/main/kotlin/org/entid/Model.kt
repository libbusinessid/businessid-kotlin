// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

package org.entid

/**
 * One identifier submitted for validation.
 *
 * @property kind the family the value is claimed to belong to.
 * @property value the raw string, kept unchanged in every result.
 * @property countryCode optional context. When present it is normalised by the
 *   ruleset's country rules; a proven contradiction with a recognised prefix is
 *   reported as [ReasonCode.COUNTRY_MISMATCH].
 */
public data class IdentifierInput(val kind: IdentifierKind, val value: String, val countryCode: String? = null) {
    /**
     * The kind as a plain token.
     *
     * [kind] is a value class, so its accessor carries a mangled JVM name that
     * Java cannot spell. This reads the same thing and is what a Java caller
     * uses.
     */
    val kindToken: String get() = kind.value

    /** Factory for the kind as a plain string, which reads better from Java. */
    public companion object {
        /**
         * The same input, with [kind] given as a token rather than as an
         * [IdentifierKind]. An unknown token is legal and is reported as
         * [ReasonCode.UNSUPPORTED_KIND].
         */
        @JvmStatic
        @JvmOverloads
        public fun of(kind: String, value: String, countryCode: String? = null): IdentifierInput =
            IdentifierInput(IdentifierKind(kind), value, countryCode)
    }
}

/**
 * Options of one validation.
 *
 * @property profile which variants to accept. Its absence is meaningful: it is
 *   what lets the selected definition apply its own default. Filling it in with
 *   [ValidationProfile.COMPATIBLE] would make that default unreachable, because
 *   the engine could no longer tell a silent caller from one asking for
 *   `compatible`.
 */
public data class ValidationOptions(val profile: ValidationProfile? = null)

/**
 * The outcome of one validation step.
 *
 * @property level which step this describes.
 * @property status how much of the step could be applied.
 * @property reasonCode why it reached that status.
 * @property messageKey the stable key the rule carries, or `null`. Results the
 *   engine produces before any rule assertion never carry one; an assertion or a
 *   checksum declared by the ruleset keeps its key exactly, including when it
 *   has none.
 */
public data class StepResult(
    val level: ValidationLevel,
    val status: StepStatus,
    val reasonCode: ReasonCode,
    val messageKey: String? = null,
)

/**
 * The full result of [EntIdEngine.validate] and its two narrower forms.
 *
 * There is deliberately no `isValid` on the report as a whole: a value whose
 * format is valid and whose checksum is unsupported is neither fully validated
 * nor invalid, and a single boolean would have to lie about one of the two.
 *
 * @property kind the canonical kind once a dispatcher resolved, otherwise the
 *   requested token after ASCII trim and lower casing.
 * @property inputValue the raw input, unchanged.
 * @property canonicalValue the value after the canonicalisation phases that ran.
 * @property countryCode the normalised country context, when one exists.
 * @property profile the profile that was in effect.
 * @property rulesVersion the business version of the compiled ruleset.
 * @property formatVersion the structural version of the compiled ruleset.
 * @property engineVersion the version of this library.
 * @property format the format step.
 * @property checksum the checksum step.
 */
public data class ValidationReport(
    val kind: IdentifierKind,
    val inputValue: String,
    val canonicalValue: String,
    val countryCode: String?,
    val profile: ValidationProfile,
    val rulesVersion: String,
    val formatVersion: Int,
    val engineVersion: String,
    val format: StepResult,
    val checksum: StepResult,
) {
    /**
     * The kind as a plain token.
     *
     * [kind] is a value class, so its accessor carries a mangled JVM name that
     * Java cannot spell. This reads the same thing and is what a Java caller
     * uses.
     */
    val kindToken: String get() = kind.value

    /** True when the shape matches a documented variant. */
    public val isFormatValid: Boolean
        get() = format.status == StepStatus.VALID

    /** True when a documented check algorithm ran and succeeded. */
    public val isChecksumValid: Boolean
        get() = checksum.status == StepStatus.VALID

    /** True when both steps are valid. */
    public val isFullyValidated: Boolean
        get() = isFormatValid && isChecksumValid

    /** True when at least one executed step proved the value wrong. */
    public val isInvalid: Boolean
        get() = format.status == StepStatus.INVALID || checksum.status == StepStatus.INVALID
}

/**
 * The result of [EntIdEngine.canonicalize].
 *
 * [StepStatus.NOT_RUN] is never a final canonicalisation status.
 *
 * @property kind the canonical kind once a dispatcher resolved, otherwise the
 *   requested token after ASCII trim and lower casing.
 * @property inputValue the raw input, unchanged.
 * @property canonicalValue the value after the canonicalisation phases that ran.
 * @property countryCode the normalised country context, when one exists.
 * @property profile the profile that was in effect.
 * @property rulesVersion the business version of the compiled ruleset.
 * @property formatVersion the structural version of the compiled ruleset.
 * @property engineVersion the version of this library.
 * @property status how far canonicalisation got.
 * @property reasonCode why it reached that status.
 * @property messageKey always `null`: canonicalisation runs no rule assertion.
 */
public data class CanonicalizationResult(
    val kind: IdentifierKind,
    val inputValue: String,
    val canonicalValue: String,
    val countryCode: String?,
    val profile: ValidationProfile,
    val rulesVersion: String,
    val formatVersion: Int,
    val engineVersion: String,
    val status: StepStatus,
    val reasonCode: ReasonCode,
    val messageKey: String? = null,
) {
    /**
     * The kind as a plain token.
     *
     * [kind] is a value class, so its accessor carries a mangled JVM name that
     * Java cannot spell. This reads the same thing and is what a Java caller
     * uses.
     */
    val kindToken: String get() = kind.value
}

/**
 * A frozen capability identifier this engine implements.
 *
 * @property id the numeric identifier, never renumbered and never reused.
 * @property capabilityName the registry name of the capability.
 */
public data class Capability(val id: Int, val capabilityName: String)

/**
 * What the engine was built from.
 *
 * @property rulesVersion business version of the compiled ruleset, `YYYY.MM.PATCH`.
 * @property formatVersion structural version of the intermediate representation.
 * @property engineVersion version of this library, independent of the two above.
 * @property sourceDigest lowercase hexadecimal SHA-256 of the canonical rule source stream.
 * @property supportedKinds every kind the compiled ruleset can dispatch.
 */
public data class RulesInfo(
    val rulesVersion: String,
    val formatVersion: Int,
    val engineVersion: String,
    val sourceDigest: String,
    val supportedKinds: List<IdentifierKind>,
)
