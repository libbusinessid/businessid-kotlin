// Generated from entid-rules.binpb 2026.08.38. Do not edit by hand:
// run `./gradlew generateEngine`. `./gradlew checkGenerated` fails when this
// file and the pinned bundle disagree.
//
// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint", "MaxLineLength", "LongMethod", "CyclomaticComplexMethod")

package org.entid.generated

import org.entid.ReasonCode
import org.entid.internal.EvalContext
import org.entid.runtime.Alignment
import org.entid.runtime.Arith
import org.entid.runtime.ChecksumOutcome
import org.entid.runtime.Ck
import org.entid.runtime.CpView
import org.entid.runtime.Pred
import org.entid.runtime.Txt

@Suppress("UNUSED_PARAMETER")
internal fun ck_97(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareSlice(Arith.complement(Arith.modDigits(Txt.slice(subject, 0, 8), 97L), 97L), subject, 8, 10, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_98(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    if (Pred.integerIs(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 8), K59, Alignment.LEFT), 11L), 10L)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 8), K60, Alignment.LEFT), 11L), K61), subject, 8, null)
    } else {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 8), K59, Alignment.LEFT), 11L), K61), subject, 8, null)
    }

@Suppress("UNUSED_PARAMETER")
internal fun ck_99(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.allChecks(
        arrayOf<ChecksumOutcome>(
            Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumAlphabet(Txt.slice(subject, 0, 12), K62, Alignment.LEFT, K63), 11L), K64), subject, 12, null),
            Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumAlphabet(Txt.slice(subject, 0, 13), K65, Alignment.LEFT, K63), 11L), K64), subject, 13, null),
        )
    )

@Suppress("UNUSED_PARAMETER")
internal fun ck_100(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareConstant(Arith.modulo(Arith.weightedSumAlphabet(subject, K66, Alignment.LEFT, K67), 31L), 0L, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_101(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 7), K68, Alignment.LEFT), 11L), K69), subject, 7, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_102(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareConstant(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 8), K70, Alignment.LEFT), 11L), 0L, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_103(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    if (Pred.integerIs(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 7), K71, Alignment.LEFT), 11L), 10L)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 7), K72, Alignment.LEFT), 11L), K61), subject, 7, null)
    } else {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 7), K71, Alignment.LEFT), 11L), K61), subject, 7, null)
    }

@Suppress("UNUSED_PARAMETER")
internal fun ck_104(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    if (Pred.charAtIn(subject, 8, K73)) {
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
    Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 7), K74, Alignment.LEFT), 11L), K75), subject, 7, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_121(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.luhn(subject, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_122(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.allChecks(
        arrayOf<ChecksumOutcome>(
            ck_121(Txt.slice(subject, 0, 9), ctx),
            if (Pred.startsWith(subject, K76)) {
                Ck.anyCheck(
                    arrayOf<ChecksumOutcome>(
                        Ck.luhn(subject, null),
                        Ck.compareConstant(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 14), K77, Alignment.CYCLE), 5L), 0L, null),
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
    Ck.compareConstant(Arith.modulo(Arith.weightedSumDigits(subject, K78, Alignment.LEFT), 9L), 0L, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_125(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.iso7064Mod97(subject, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_126(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    if (Pred.integerIs(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 9), K79, Alignment.LEFT), 11L), 10L)) {
        Ck.compareConstant(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 9), K80, Alignment.LEFT), 11L), K61), 0L, null)
    } else {
        Ck.compareConstant(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 9), K79, Alignment.LEFT), 11L), 0L, null)
    }

@Suppress("UNUSED_PARAMETER")
internal fun ck_127(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 10), K81, Alignment.LEFT), 11L), K82), subject, 10, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_128(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 8), K83, Alignment.LEFT), 11L), K64), subject, 8, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_129(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    if (Pred.lengthEq(subject, 2)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 1), K84, Alignment.RIGHT), 11L), K61), subject, 1, null)
    } else if (Pred.lengthEq(subject, 3)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 2), K84, Alignment.RIGHT), 11L), K61), subject, 2, null)
    } else if (Pred.lengthEq(subject, 4)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 3), K84, Alignment.RIGHT), 11L), K61), subject, 3, null)
    } else if (Pred.lengthEq(subject, 5)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 4), K84, Alignment.RIGHT), 11L), K61), subject, 4, null)
    } else if (Pred.lengthEq(subject, 6)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 5), K84, Alignment.RIGHT), 11L), K61), subject, 5, null)
    } else if (Pred.lengthEq(subject, 7)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 6), K84, Alignment.RIGHT), 11L), K61), subject, 6, null)
    } else if (Pred.lengthEq(subject, 8)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 7), K84, Alignment.RIGHT), 11L), K61), subject, 7, null)
    } else if (Pred.lengthEq(subject, 9)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 8), K84, Alignment.RIGHT), 11L), K61), subject, 8, null)
    } else if (Pred.lengthEq(subject, 10)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 9), K84, Alignment.RIGHT), 11L), K61), subject, 9, null)
    } else {
        Ck.noBranch()
    }

@Suppress("UNUSED_PARAMETER")
internal fun ck_130(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.luhn(subject, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_131(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 0, 7), K68, Alignment.LEFT), 11L), K69), subject, 7, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_132(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareSlice(Arith.complement(Arith.modDigits(Txt.slice(subject, 2, 10), 97L), 97L), subject, 10, 12, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_133(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    if (!(Pred.lengthEq(subject, 11))) {
        Ck.declaredUnsupported(ReasonCode.CHECKSUM_NOT_PUBLISHED, null)
    } else if (Pred.integerIs(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 8), K59, Alignment.LEFT), 11L), 10L)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 8), K60, Alignment.LEFT), 11L), K61), Txt.sliceFrom(subject, 2), 8, null)
    } else {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 8), K59, Alignment.LEFT), 11L), K61), Txt.sliceFrom(subject, 2), 8, null)
    }

@Suppress("UNUSED_PARAMETER")
internal fun ck_134(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    if (!(Pred.lengthEq(subject, 10))) {
        Ck.declaredUnsupported(ReasonCode.CHECKSUM_NOT_PUBLISHED, null)
    } else {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 7), K68, Alignment.LEFT), 11L), K69), Txt.sliceFrom(subject, 2), 7, null)
    }

@Suppress("UNUSED_PARAMETER")
internal fun ck_135(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareConstant(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 8), K70, Alignment.LEFT), 11L), 0L, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_136(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 8), K85, Alignment.LEFT), 10L), K86), Txt.sliceFrom(subject, 2), 8, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_137(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    if ((Pred.charAtIn(subject, 2, K13) && Pred.charAtIn(subject, 10, K73))) {
        Ck.luhn(Txt.slice(subject, 3, 11), null)
    } else {
        Ck.declaredUnsupported(ReasonCode.CHECKSUM_NOT_PUBLISHED, null)
    }

@Suppress("UNUSED_PARAMETER")
internal fun ck_138(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 7), K74, Alignment.LEFT), 11L), K75), Txt.sliceFrom(subject, 2), 7, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_139(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.allChecks(
        arrayOf<ChecksumOutcome>(
            ck_121(Txt.sliceFrom(subject, 4), ctx),
            if (Pred.asciiDigits(Txt.slice(subject, 2, 4))) {
                Ck.compareSlice(Arith.remainderMap(Arith.modDigits(Txt.sliceFrom(subject, 4), 97L), K87), subject, 2, 4, null)
            } else {
                Ck.declaredUnsupported(ReasonCode.CHECKSUM_NOT_PUBLISHED, null)
            },
        )
    )

@Suppress("UNUSED_PARAMETER")
internal fun ck_140(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.anyCheck(
        arrayOf<ChecksumOutcome>(
            Ck.compareConstant(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 9), K88, Alignment.LEFT), 97L), 0L, null),
            Ck.compareConstant(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 9), K88, Alignment.LEFT), 97L), 42L, null),
        )
    )

@Suppress("UNUSED_PARAMETER")
internal fun ck_141(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(subject, 2, 10), K89, Alignment.LEFT), 11L), K61), subject, 10, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_142(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 7), K90, Alignment.LEFT), 10L), K86), Txt.sliceFrom(subject, 2), 7, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_143(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    if (!(Pred.lengthEq(subject, 12))) {
        Ck.declaredUnsupported(ReasonCode.CHECKSUM_NOT_PUBLISHED, null)
    } else {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 8), K91, Alignment.LEFT), 11L), K75), Txt.sliceFrom(subject, 2), 8, null)
    }

@Suppress("UNUSED_PARAMETER")
internal fun ck_144(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.luhn(Txt.sliceFrom(subject, 2), null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_145(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareSlice(Arith.modulo(Arith.digitsToInteger(Txt.slice(Txt.sliceFrom(subject, 2), 0, 6)), 89L), Txt.sliceFrom(subject, 2), 6, 8, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_146(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    if (!(Pred.charAtIn(Txt.sliceFrom(subject, 2), 0, K45))) {
        Ck.declaredUnsupported(ReasonCode.CHECKSUM_NOT_PUBLISHED, null)
    } else {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 10), K81, Alignment.LEFT), 11L), K82), Txt.sliceFrom(subject, 2), 10, null)
    }

@Suppress("UNUSED_PARAMETER")
internal fun ck_147(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareSlice(Arith.complement(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 6), K92, Alignment.LEFT), 37L), 37L), Txt.sliceFrom(subject, 2), 6, 8, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_148(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 8), K83, Alignment.LEFT), 11L), K93), Txt.sliceFrom(subject, 2), 8, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_149(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 8), K91, Alignment.LEFT), 11L), K75), Txt.sliceFrom(subject, 2), 8, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_150(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 9), K94, Alignment.LEFT), 11L), K93), Txt.sliceFrom(subject, 2), 9, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_151(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 8), K83, Alignment.LEFT), 11L), K64), Txt.sliceFrom(subject, 2), 8, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_152(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    if (Pred.lengthEq(Txt.sliceFrom(subject, 2), 2)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 1), K84, Alignment.RIGHT), 11L), K61), Txt.sliceFrom(subject, 2), 1, null)
    } else if (Pred.lengthEq(Txt.sliceFrom(subject, 2), 3)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 2), K84, Alignment.RIGHT), 11L), K61), Txt.sliceFrom(subject, 2), 2, null)
    } else if (Pred.lengthEq(Txt.sliceFrom(subject, 2), 4)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 3), K84, Alignment.RIGHT), 11L), K61), Txt.sliceFrom(subject, 2), 3, null)
    } else if (Pred.lengthEq(Txt.sliceFrom(subject, 2), 5)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 4), K84, Alignment.RIGHT), 11L), K61), Txt.sliceFrom(subject, 2), 4, null)
    } else if (Pred.lengthEq(Txt.sliceFrom(subject, 2), 6)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 5), K84, Alignment.RIGHT), 11L), K61), Txt.sliceFrom(subject, 2), 5, null)
    } else if (Pred.lengthEq(Txt.sliceFrom(subject, 2), 7)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 6), K84, Alignment.RIGHT), 11L), K61), Txt.sliceFrom(subject, 2), 6, null)
    } else if (Pred.lengthEq(Txt.sliceFrom(subject, 2), 8)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 7), K84, Alignment.RIGHT), 11L), K61), Txt.sliceFrom(subject, 2), 7, null)
    } else if (Pred.lengthEq(Txt.sliceFrom(subject, 2), 9)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 8), K84, Alignment.RIGHT), 11L), K61), Txt.sliceFrom(subject, 2), 8, null)
    } else if (Pred.lengthEq(Txt.sliceFrom(subject, 2), 10)) {
        Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 9), K84, Alignment.RIGHT), 11L), K61), Txt.sliceFrom(subject, 2), 9, null)
    } else {
        Ck.noBranch()
    }

@Suppress("UNUSED_PARAMETER")
internal fun ck_153(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.luhn(Txt.slice(Txt.sliceFrom(subject, 2), 0, 10), null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_154(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareDigit(Arith.remainderMap(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 7), K68, Alignment.LEFT), 11L), K75), Txt.sliceFrom(subject, 2), 7, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_155(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.compareConstant(Arith.modulo(Arith.digitsToInteger(Txt.slice(Txt.sliceFrom(subject, 2), 0, 10)), 11L), 0L, null)

@Suppress("UNUSED_PARAMETER")
internal fun ck_156(subject: CpView?, ctx: EvalContext): ChecksumOutcome =
    Ck.anyCheck(
        arrayOf<ChecksumOutcome>(
            Ck.compareConstant(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 9), K88, Alignment.LEFT), 97L), 0L, null),
            Ck.compareConstant(Arith.modulo(Arith.weightedSumDigits(Txt.slice(Txt.sliceFrom(subject, 2), 0, 9), K88, Alignment.LEFT), 97L), 42L, null),
        )
    )
