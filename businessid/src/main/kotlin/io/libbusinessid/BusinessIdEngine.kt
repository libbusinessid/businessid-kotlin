// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid

import io.libbusinessid.generated.Ruleset
import io.libbusinessid.internal.EngineVersion
import io.libbusinessid.internal.Operation
import io.libbusinessid.internal.Pipeline

/**
 * Canonicalises and validates business identifiers offline.
 *
 * The engine holds no mutable state, performs no I/O and makes no network call.
 * One instance is safe to share across threads, and every operation is
 * synchronous — permanently: a remote register lookup, when it exists, will be a
 * separate asynchronous operation in a separate module, never a mode of these.
 *
 * The rules are compiled into this library as source code at build time. There
 * is no factory taking a ruleset as bytes: a custom ruleset goes through the
 * generator, at build time, which is what keeps the decoder, the twenty-five
 * load checks and the execution machinery out of every caller.
 *
 * ```
 * val engine = BusinessIdEngine.default()
 * val report = engine.validate(IdentifierInput(IdentifierKind.SIRET, "01234567400001"))
 * report.isFullyValidated // true
 * ```
 */
public class BusinessIdEngine private constructor() {
    /**
     * Applies the input bound, the dispatch and the two canonicalisation phases,
     * and stops there. Neither format nor checksum runs.
     */
    @JvmOverloads
    public fun canonicalize(
        input: IdentifierInput,
        options: ValidationOptions = ValidationOptions(),
    ): CanonicalizationResult = Pipeline.canonicalize(input, options.profile)

    /**
     * Runs the whole pipeline: dispatch, canonicalisation, format, then checksum
     * when the format holds.
     */
    @JvmOverloads
    public fun validate(input: IdentifierInput, options: ValidationOptions = ValidationOptions()): ValidationReport =
        Pipeline.validate(input, options.profile, Operation.VALIDATE)

    /**
     * Stops after the format step.
     *
     * On a valid format the checksum step is `not_run` with `not_requested`. On a
     * failing dispatch or format both steps read exactly as they would from
     * [validate].
     */
    @JvmOverloads
    public fun validateFormat(
        input: IdentifierInput,
        options: ValidationOptions = ValidationOptions(),
    ): ValidationReport = Pipeline.validate(input, options.profile, Operation.VALIDATE_FORMAT)

    /**
     * Runs the checksum, with the format as a mandatory guard.
     *
     * Returns the same report as [validate] for the same input and options: the
     * separate name exists for readability at the call site, not to bypass the
     * format.
     */
    @JvmOverloads
    public fun validateChecksum(
        input: IdentifierInput,
        options: ValidationOptions = ValidationOptions(),
    ): ValidationReport = Pipeline.validate(input, options.profile, Operation.VALIDATE)

    /** What this engine was built from. */
    public fun rulesInfo(): RulesInfo = RULES_INFO

    /** The frozen capability identifiers this engine implements. */
    public fun capabilities(): List<Capability> = Ruleset.CAPABILITIES

    /** Access to the shared engine. */
    public companion object {
        private val INSTANCE = BusinessIdEngine()

        private val RULES_INFO = RulesInfo(
            rulesVersion = Ruleset.RULES_VERSION,
            formatVersion = Ruleset.FORMAT_VERSION,
            engineVersion = EngineVersion.VALUE,
            sourceDigest = Ruleset.SOURCE_DIGEST,
            supportedKinds = Ruleset.KINDS.map { IdentifierKind(it) },
        )

        /**
         * The engine built from the ruleset compiled into this library.
         *
         * It cannot fail and returns no error: there is nothing to load. A
         * defective ruleset stops the build of this library, not a call to this
         * method.
         *
         * Java calls it `BusinessIdEngine.defaultEngine()`: `default` is a
         * keyword there, so the name Kotlin reads best is one Java cannot spell.
         * The alias is the name `engine.md` section 15.1 uses for this operation.
         */
        @JvmStatic
        @JvmName("defaultEngine")
        public fun default(): BusinessIdEngine = INSTANCE
    }
}
