// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

package io.libbusinessid.generator

import libbusinessid.ir.v1.Rules

/** One dispatch target, in the flat index space the emitted code uses. */
internal class TargetRef(
    val index: Int,
    val dispatcherIndex: Int,
    val proto: Rules.DispatchTarget,
) {
    val isGlobal: Boolean get() = !proto.hasCountryCode()
    val countryCode: String? get() = if (proto.hasCountryCode()) proto.countryCode else null
}

/**
 * A ruleset that passed all twenty-five checks, with the indexes the emitter
 * needs. Nothing here is mutable and nothing is re-derived later: the emitter
 * reads a settled model.
 */
internal class LoadedBundle(
    val proto: Rules.RuleBundle,
    val programsById: Map<Int, Rules.Program>,
    val definitionsById: Map<Int, Rules.IdentifierDefinition>,
    val definitionIndexById: Map<Int, Int>,
    val targets: List<TargetRef>,
    val usedCapabilities: Set<Int>,
    val sourceDigestHex: String,
) {
    val rulesVersion: String get() = proto.rulesVersion
    val formatVersion: Int get() = proto.formatVersion
    val dispatchers: List<Rules.IdentifierDispatcher> get() = proto.dispatchersList
}
