// Generated from businessid-rules.binpb 2026.08.26. Do not edit by hand:
// run `./gradlew generateEngine`. `./gradlew checkGenerated` fails when this
// file and the pinned bundle disagree.
//
// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint", "MaxLineLength", "LongMethod", "CyclomaticComplexMethod")

package io.libbusinessid.generated

import io.libbusinessid.ReasonCode
import io.libbusinessid.ValidationProfile
import io.libbusinessid.internal.EvalContext
import io.libbusinessid.runtime.Alignment
import io.libbusinessid.runtime.Arith
import io.libbusinessid.runtime.ChecksumOutcome
import io.libbusinessid.runtime.Ck
import io.libbusinessid.runtime.CpView
import io.libbusinessid.runtime.Pred
import io.libbusinessid.runtime.Txt

@Suppress("UNUSED_PARAMETER")
internal fun ck_97(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareSlice(Arith.complement(Arith.modDigits(Txt.slice(subject, 0, 8), 97L), 97L), subject, 8, 10, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_98(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    if (Pred.integerIs(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 8), K57, Alignment.LEFT), 11L), 10L)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 8), K58, Alignment.LEFT), 11L), K59), subject, 8, null)
    } else {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 8), K57, Alignment.LEFT), 11L), K59), subject, 8, null)
    }

@Suppress("UNUSED_PARAMETER")
internal fun ck_99(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.allChecks(
        arrayOf<ChecksumOutcome>(
            Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumAlphabet(Txt.slice(subject, 0, 12), K60, Alignment.LEFT, K61), 11L), K62), subject, 12, null),
            Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumAlphabet(Txt.slice(subject, 0, 13), K63, Alignment.LEFT, K61), 11L), K62), subject, 13, null),
        )
    )

@Suppress("UNUSED_PARAMETER")
internal fun ck_100(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareConstant(Arith.modulo(Arith.weightedSumAlphabet(subject, K64, Alignment.LEFT, K65), 31L), 0L, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_101(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 7), K66, Alignment.LEFT), 11L), K67), subject, 7, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_102(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareConstant(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 8), K68, Alignment.LEFT), 11L), 0L, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_103(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    if (Pred.integerIs(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 7), K69, Alignment.LEFT), 11L), 10L)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 7), K70, Alignment.LEFT), 11L), K59), subject, 7, null)
    } else {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 7), K69, Alignment.LEFT), 11L), K59), subject, 7, null)
    }

@Suppress("UNUSED_PARAMETER")
internal fun ck_104(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    if (Pred.charAtIn(subject, 8, K71)) {
        Ck.luhn(Txt.slice(subject, 1, 9), null)
    } else {
        Ck.declaredUnsupported(ReasonCode.CHECKSUM_NOT_PUBLISHED, null)
    }

@Suppress("UNUSED_PARAMETER")
internal fun ck_105(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    ck_97(Txt.afterFirst(subject, K15), ctx)

@Suppress("UNUSED_PARAMETER")
internal fun ck_106(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    ck_98(Txt.afterFirst(subject, K15), ctx)

@Suppress("UNUSED_PARAMETER")
internal fun ck_107(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    ck_101(Txt.afterFirst(subject, K15), ctx)

@Suppress("UNUSED_PARAMETER")
internal fun ck_108(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    ck_102(Txt.afterFirst(subject, K15), ctx)

@Suppress("UNUSED_PARAMETER")
internal fun ck_109(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    ck_103(Txt.afterFirst(subject, K15), ctx)

@Suppress("UNUSED_PARAMETER")
internal fun ck_110(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    ck_104(Txt.afterFirst(subject, K15), ctx)

@Suppress("UNUSED_PARAMETER")
internal fun ck_111(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    ck_120(Txt.afterFirst(subject, K15), ctx)

@Suppress("UNUSED_PARAMETER")
internal fun ck_112(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    ck_121(Txt.afterFirst(subject, K15), ctx)

@Suppress("UNUSED_PARAMETER")
internal fun ck_113(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    ck_123(Txt.afterFirst(subject, K15), ctx)

@Suppress("UNUSED_PARAMETER")
internal fun ck_114(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    ck_126(Txt.afterFirst(subject, K15), ctx)

@Suppress("UNUSED_PARAMETER")
internal fun ck_115(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    ck_127(Txt.afterFirst(subject, K15), ctx)

@Suppress("UNUSED_PARAMETER")
internal fun ck_116(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    ck_128(Txt.afterFirst(subject, K15), ctx)

@Suppress("UNUSED_PARAMETER")
internal fun ck_117(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    ck_129(Txt.afterFirst(subject, K15), ctx)

@Suppress("UNUSED_PARAMETER")
internal fun ck_118(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    ck_130(Txt.afterFirst(subject, K15), ctx)

@Suppress("UNUSED_PARAMETER")
internal fun ck_119(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    ck_131(Txt.afterFirst(subject, K15), ctx)

@Suppress("UNUSED_PARAMETER")
internal fun ck_120(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 7), K72, Alignment.LEFT), 11L), K73), subject, 7, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_121(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.luhn(subject, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_122(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.allChecks(
        arrayOf<ChecksumOutcome>(
            ck_121(Txt.slice(subject, 0, 9), ctx),
            if (Pred.startsWith(subject, K74)) {
                Ck.anyCheck(
                    arrayOf<ChecksumOutcome>(
                        Ck.luhn(subject, null),
                        Ck.compareConstant(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 14), K75, Alignment.CYCLE), 5L), 0L, null),
                    )
                )
            } else {
                Ck.luhn(subject, "fr.siret.luhn")
            },
        )
    )

@Suppress("UNUSED_PARAMETER")
internal fun ck_123(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.luhn(subject, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_124(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareConstant(Arith.modulo(Arith.weightedSumDigits(subject, K76, Alignment.LEFT), 9L), 0L, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_125(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.iso7064Mod97(subject, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_126(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    if (Pred.integerIs(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 9), K77, Alignment.LEFT), 11L), 10L)) {
        Ck.compareConstant(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 9), K78, Alignment.LEFT), 11L), K59), 0L, null)
    } else {
        Ck.compareConstant(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 9), K77, Alignment.LEFT), 11L), 0L, null)
    }

@Suppress("UNUSED_PARAMETER")
internal fun ck_127(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 10), K79, Alignment.LEFT), 11L), K80), subject, 10, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_128(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 8), K81, Alignment.LEFT), 11L), K62), subject, 8, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_129(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    if (Pred.lengthEq(subject, 2)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 1), K82, Alignment.RIGHT), 11L), K59), subject, 1, null)
    } else if (Pred.lengthEq(subject, 3)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 2), K82, Alignment.RIGHT), 11L), K59), subject, 2, null)
    } else if (Pred.lengthEq(subject, 4)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 3), K82, Alignment.RIGHT), 11L), K59), subject, 3, null)
    } else if (Pred.lengthEq(subject, 5)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 4), K82, Alignment.RIGHT), 11L), K59), subject, 4, null)
    } else if (Pred.lengthEq(subject, 6)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 5), K82, Alignment.RIGHT), 11L), K59), subject, 5, null)
    } else if (Pred.lengthEq(subject, 7)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 6), K82, Alignment.RIGHT), 11L), K59), subject, 6, null)
    } else if (Pred.lengthEq(subject, 8)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 7), K82, Alignment.RIGHT), 11L), K59), subject, 7, null)
    } else if (Pred.lengthEq(subject, 9)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 8), K82, Alignment.RIGHT), 11L), K59), subject, 8, null)
    } else if (Pred.lengthEq(subject, 10)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 9), K82, Alignment.RIGHT), 11L), K59), subject, 9, null)
    } else {
        Ck.noBranch()
    }

@Suppress("UNUSED_PARAMETER")
internal fun ck_130(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.luhn(subject, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_131(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 7), K66, Alignment.LEFT), 11L), K67), subject, 7, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_132(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareSlice(Arith.complement(Arith.modDigits(Txt.slice(subject, 2, 10), 97L), 97L), subject, 10, 12, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_133(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    if (!(Pred.lengthEq(subject, 11))) {
        Ck.declaredUnsupported(ReasonCode.CHECKSUM_NOT_PUBLISHED, null)
    } else if (Pred.integerIs(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 8), K57, Alignment.LEFT), 11L), 10L)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 8), K58, Alignment.LEFT), 11L), K59), Txt.sliceFrom(subject, 2), 8, null)
    } else {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 8), K57, Alignment.LEFT), 11L), K59), Txt.sliceFrom(subject, 2), 8, null)
    }

@Suppress("UNUSED_PARAMETER")
internal fun ck_134(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    if (!(Pred.lengthEq(subject, 10))) {
        Ck.declaredUnsupported(ReasonCode.CHECKSUM_NOT_PUBLISHED, null)
    } else {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 7), K66, Alignment.LEFT), 11L), K67), Txt.sliceFrom(subject, 2), 7, null)
    }

@Suppress("UNUSED_PARAMETER")
internal fun ck_135(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareConstant(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 8), K68, Alignment.LEFT), 11L), 0L, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_136(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 8), K83, Alignment.LEFT), 10L), K84), Txt.sliceFrom(subject, 2), 8, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_137(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    if ((Pred.charAtIn(subject, 2, K13) && Pred.charAtIn(subject, 10, K71))) {
        Ck.luhn(Txt.slice(subject, 3, 11), null)
    } else {
        Ck.declaredUnsupported(ReasonCode.CHECKSUM_NOT_PUBLISHED, null)
    }

@Suppress("UNUSED_PARAMETER")
internal fun ck_138(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 7), K72, Alignment.LEFT), 11L), K73), Txt.sliceFrom(subject, 2), 7, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_139(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.allChecks(
        arrayOf<ChecksumOutcome>(
            ck_121(Txt.sliceFrom(subject, 4), ctx),
            if (Pred.asciiDigits(Txt.slice(subject, 2, 4))) {
                Ck.compareSlice(Arith.remainderMap(Arith.modDigits(Txt.sliceFrom(subject, 4), 97L), K85), subject, 2, 4, null)
            } else {
                Ck.declaredUnsupported(ReasonCode.CHECKSUM_NOT_PUBLISHED, null)
            },
        )
    )

@Suppress("UNUSED_PARAMETER")
internal fun ck_140(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.anyCheck(
        arrayOf<ChecksumOutcome>(
            Ck.compareConstant(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 9), K86, Alignment.LEFT), 97L), 0L, null),
            Ck.compareConstant(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 9), K86, Alignment.LEFT), 97L), 42L, null),
        )
    )

@Suppress("UNUSED_PARAMETER")
internal fun ck_141(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 2, 10), K87, Alignment.LEFT), 11L), K59), subject, 10, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_142(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 7), K88, Alignment.LEFT), 10L), K84), Txt.sliceFrom(subject, 2), 7, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_143(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    if (!(Pred.lengthEq(subject, 12))) {
        Ck.declaredUnsupported(ReasonCode.CHECKSUM_NOT_PUBLISHED, null)
    } else {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 8), K89, Alignment.LEFT), 11L), K73), Txt.sliceFrom(subject, 2), 8, null)
    }

@Suppress("UNUSED_PARAMETER")
internal fun ck_144(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.luhn(Txt.sliceFrom(subject, 2), null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_145(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareSlice(Arith.modulo(Arith.digitsToInteger(Txt.slice(Txt.sliceFrom(subject, 2), 0, 6)), 89L), Txt.sliceFrom(subject, 2), 6, 8, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_146(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    if (!(Pred.charAtIn(Txt.sliceFrom(subject, 2), 0, K43))) {
        Ck.declaredUnsupported(ReasonCode.CHECKSUM_NOT_PUBLISHED, null)
    } else {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 10), K79, Alignment.LEFT), 11L), K80), Txt.sliceFrom(subject, 2), 10, null)
    }

@Suppress("UNUSED_PARAMETER")
internal fun ck_147(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareSlice(Arith.complement(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 6), K90, Alignment.LEFT), 37L), 37L), Txt.sliceFrom(subject, 2), 6, 8, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_148(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 8), K81, Alignment.LEFT), 11L), K91), Txt.sliceFrom(subject, 2), 8, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_149(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 8), K89, Alignment.LEFT), 11L), K73), Txt.sliceFrom(subject, 2), 8, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_150(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 9), K92, Alignment.LEFT), 11L), K91), Txt.sliceFrom(subject, 2), 9, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_151(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 8), K81, Alignment.LEFT), 11L), K62), Txt.sliceFrom(subject, 2), 8, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_152(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    if (Pred.lengthEq(Txt.sliceFrom(subject, 2), 2)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 1), K82, Alignment.RIGHT), 11L), K59), Txt.sliceFrom(subject, 2), 1, null)
    } else if (Pred.lengthEq(Txt.sliceFrom(subject, 2), 3)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 2), K82, Alignment.RIGHT), 11L), K59), Txt.sliceFrom(subject, 2), 2, null)
    } else if (Pred.lengthEq(Txt.sliceFrom(subject, 2), 4)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 3), K82, Alignment.RIGHT), 11L), K59), Txt.sliceFrom(subject, 2), 3, null)
    } else if (Pred.lengthEq(Txt.sliceFrom(subject, 2), 5)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 4), K82, Alignment.RIGHT), 11L), K59), Txt.sliceFrom(subject, 2), 4, null)
    } else if (Pred.lengthEq(Txt.sliceFrom(subject, 2), 6)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 5), K82, Alignment.RIGHT), 11L), K59), Txt.sliceFrom(subject, 2), 5, null)
    } else if (Pred.lengthEq(Txt.sliceFrom(subject, 2), 7)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 6), K82, Alignment.RIGHT), 11L), K59), Txt.sliceFrom(subject, 2), 6, null)
    } else if (Pred.lengthEq(Txt.sliceFrom(subject, 2), 8)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 7), K82, Alignment.RIGHT), 11L), K59), Txt.sliceFrom(subject, 2), 7, null)
    } else if (Pred.lengthEq(Txt.sliceFrom(subject, 2), 9)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 8), K82, Alignment.RIGHT), 11L), K59), Txt.sliceFrom(subject, 2), 8, null)
    } else if (Pred.lengthEq(Txt.sliceFrom(subject, 2), 10)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 9), K82, Alignment.RIGHT), 11L), K59), Txt.sliceFrom(subject, 2), 9, null)
    } else {
        Ck.noBranch()
    }

@Suppress("UNUSED_PARAMETER")
internal fun ck_153(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.luhn(Txt.slice(Txt.sliceFrom(subject, 2), 0, 10), null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_154(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 7), K66, Alignment.LEFT), 11L), K73), Txt.sliceFrom(subject, 2), 7, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_155(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareConstant(Arith.modulo(Arith.digitsToInteger(Txt.slice(Txt.sliceFrom(subject, 2), 0, 10)), 11L), 0L, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_156(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.anyCheck(
        arrayOf<ChecksumOutcome>(
            Ck.compareConstant(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 9), K86, Alignment.LEFT), 97L), 0L, null),
            Ck.compareConstant(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 9), K86, Alignment.LEFT), 97L), 42L, null),
        )
    )

