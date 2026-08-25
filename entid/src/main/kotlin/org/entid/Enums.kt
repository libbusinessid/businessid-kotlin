// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

package org.entid

/**
 * How much of a documented rule an engine was able to apply to one step.
 *
 * The distinction between [INVALID] and [UNSUPPORTED] is the whole point of the
 * type: refusing a legitimate identifier is the most serious defect this project
 * recognises, so absence of knowledge never becomes a verdict of invalidity.
 *
 * @property wireName the lowercase token used by the shared conformance corpus.
 */
public enum class StepStatus(public val wireName: String) {
    /** Every applicable rule of the step succeeded. */
    VALID("valid"),

    /** An applicable rule proved the value wrong. */
    INVALID("invalid"),

    /** No conclusion is possible with this ruleset. */
    UNSUPPORTED("unsupported"),

    /** An earlier step forbids or makes pointless running this one. */
    NOT_RUN("not_run"),
}

/**
 * The step a [StepResult] belongs to.
 *
 * @property wireName the lowercase token used by the shared conformance corpus.
 */
public enum class ValidationLevel(public val wireName: String) {
    /** Shape of the canonical value. */
    FORMAT("format"),

    /** Internal check digit or check characters. */
    CHECKSUM("checksum"),

    /**
     * Lookup in an external register.
     *
     * Reserved. No operation of this version produces a result at this level;
     * remote lookup is deferred to a later version and lives outside this module
     * when it arrives.
     */
    REGISTRY("registry"),
}

/**
 * The machine readable reason a step reached its status.
 *
 * The registry is frozen: an engine never invents a business reason code.
 *
 * @property wireName the lowercase token used by the shared conformance corpus.
 */
@Suppress("EnumNaming")
public enum class ReasonCode(public val wireName: String) {
    /** The step succeeded. */
    OK("ok"),

    /** The canonical value holds no characters. */
    EMPTY("empty"),

    /** The canonical value has a length no documented variant accepts. */
    INVALID_LENGTH("invalid_length"),

    /** The canonical value holds a character no documented variant accepts. */
    INVALID_CHARACTERS("invalid_characters"),

    /** The canonical value matches no documented variant. */
    INVALID_FORMAT("invalid_format"),

    /** The documented check digit does not match. */
    INVALID_CHECKSUM("invalid_checksum"),

    /** A country is needed to choose a rule and none was given or implied. */
    MISSING_COUNTRY_CODE("missing_country_code"),

    /** An explicit country contradicts a recognised prefix. */
    COUNTRY_MISMATCH("country_mismatch"),

    /** The kind token is malformed or unknown to this ruleset. */
    UNSUPPORTED_KIND("unsupported_kind"),

    /** The country token is malformed, or has no rule in this ruleset. */
    UNSUPPORTED_COUNTRY("unsupported_country"),

    /** No format rule could be applied. */
    UNSUPPORTED_FORMAT("unsupported_format"),

    /** No applicable checksum algorithm could be evaluated. */
    UNSUPPORTED_CHECKSUM("unsupported_checksum"),

    /** The authority publishes no check algorithm for this identifier. */
    CHECKSUM_NOT_PUBLISHED("checksum_not_published"),

    /** The caller asked for format only. */
    NOT_REQUESTED("not_requested"),

    /** The format step proved the value wrong, so nothing downstream ran. */
    NOT_RUN_FORMAT_INVALID("not_run_format_invalid"),

    /** The format step reached no conclusion, so nothing downstream ran. */
    NOT_RUN_FORMAT_UNSUPPORTED("not_run_format_unsupported"),

    /**
     * No register was configured.
     *
     * Reserved. No operation of this version produces it.
     */
    REGISTRY_NOT_CONFIGURED("registry_not_configured"),

    /** The ruleset announces a version or capability this build does not implement. */
    INCOMPATIBLE_RULESET("incompatible_ruleset"),

    /** The ruleset is structurally wrong. */
    INVALID_RULESET("invalid_ruleset"),

    /** The raw input exceeds the safety bound of 1024 UTF-8 bytes. */
    INPUT_TOO_LONG("input_too_long"),

    /** The raw input is not well formed text, so it holds no code points to evaluate. */
    INVALID_ENCODING("invalid_encoding"),
    ;

    internal companion object {
        private val BY_WIRE_NAME: Map<String, ReasonCode> = entries.associateBy { it.wireName }

        fun ofWireName(name: String): ReasonCode? = BY_WIRE_NAME[name]
    }
}

/**
 * Which documented variants a validation accepts.
 *
 * @property wireName the lowercase token used by the shared conformance corpus.
 */
public enum class ValidationProfile(public val wireName: String) {
    /**
     * Accepts current and historical variants that still legitimately circulate.
     *
     * This is the normative default when neither the caller nor the selected
     * definition states one.
     */
    COMPATIBLE("compatible"),

    /** Accepts only the variants an authority issues today. */
    STRICT_CURRENT("strict_current"),
    ;

    internal companion object {
        fun ofWireName(name: String): ValidationProfile? = entries.firstOrNull { it.wireName == name }
    }
}
