// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

package org.entid.generator

/**
 * Why a ruleset was refused.
 *
 * @property errorKind `invalid_ruleset` or `incompatible_ruleset`.
 * @property check the number of the load check that refused it, from `ir.md` section 10.
 * @property detail a human readable explanation, not normative.
 */
class RulesetRefusal internal constructor(val errorKind: String, val check: Int, val detail: String)

/**
 * The only entry point outside this module: submit bytes, learn whether the
 * generator would emit from them.
 *
 * It exists for the conformance testee, whose `load_ruleset` cases address the
 * generator rather than the engine — the published library loads nothing at all.
 */
object RulesetGate {
    /** Returns null when the ruleset is acceptable, or the refusal that stops generation. */
    fun inspect(bytes: ByteArray): RulesetRefusal? = try {
        Loader.load(bytes)
        null
    } catch (e: RulesetException) {
        RulesetRefusal(e.errorKind.wireName, e.check, e.message)
    }
}
