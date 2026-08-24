// Generated from businessid-rules.binpb 2026.08.33. Do not edit by hand:
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
import io.libbusinessid.runtime.AssertionFailure
import io.libbusinessid.runtime.CpView
import io.libbusinessid.runtime.Pred
import io.libbusinessid.runtime.Txt

@Suppress("UNUSED_PARAMETER")
internal fun fmt_157(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "at.firmenbuchnummer.empty")
    if (!(Pred.lengthBetween(subject, 2, 7))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "at.firmenbuchnummer.length")
    if (!(Pred.asciiAlphanumeric(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "at.firmenbuchnummer.characters")
    if (!(((Pred.lengthEq(subject, 2) && Pred.asciiDigits(Txt.slice(subject, 0, 1)) && Pred.charAtIn(subject, 1, K10)) || (Pred.lengthEq(subject, 3) && Pred.asciiDigits(Txt.slice(subject, 0, 2)) && Pred.charAtIn(subject, 2, K10)) || (Pred.lengthEq(subject, 4) && Pred.asciiDigits(Txt.slice(subject, 0, 3)) && Pred.charAtIn(subject, 3, K10)) || (Pred.lengthEq(subject, 5) && Pred.asciiDigits(Txt.slice(subject, 0, 4)) && Pred.charAtIn(subject, 4, K10)) || (Pred.lengthEq(subject, 6) && Pred.asciiDigits(Txt.slice(subject, 0, 5)) && Pred.charAtIn(subject, 5, K10)) || (Pred.lengthEq(subject, 7) && Pred.asciiDigits(Txt.slice(subject, 0, 6)) && Pred.charAtIn(subject, 6, K10))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "at.firmenbuchnummer.shape")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_158(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "be.enterprise_number.empty")
    if (!(Pred.lengthEq(subject, 10))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "be.enterprise_number.length")
    if (!(Pred.asciiDigits(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "be.enterprise_number.characters")
    if (!((Pred.startsWith(subject, K7) || Pred.startsWith(subject, K11)))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "be.enterprise_number.leading")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_159(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "bg.eik.empty")
    if (!((Pred.lengthEq(subject, 9) || Pred.lengthEq(subject, 13)))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "bg.eik.length")
    if (!(Pred.asciiDigits(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "bg.eik.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_160(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "cnpj.empty")
    if (!(Pred.lengthEq(subject, 14))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "cnpj.length")
    if (!(Pred.asciiAlphanumeric(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "cnpj.characters")
    if (!(Pred.asciiDigits(Txt.slice(subject, 12, 14)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "cnpj.check_digits")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_161(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "uscc.empty")
    if (!(Pred.lengthEq(subject, 18))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "uscc.length")
    if (!(Pred.asciiCharset(subject, K12))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "uscc.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_162(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "cy.he_number.empty")
    if (!(Pred.lengthEq(subject, 6))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "cy.he_number.length")
    if (!(Pred.asciiDigits(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "cy.he_number.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_163(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "cz.ico.empty")
    if (!(Pred.lengthEq(subject, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "cz.ico.length")
    if (!(Pred.asciiDigits(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "cz.ico.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_164(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "de.handelsregisternummer.empty")
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "de.handelsregisternummer.empty")
    if (!(Pred.lengthBetween(subject, 1, 20))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "de.handelsregisternummer.length")
    if (!(Pred.asciiAlphanumeric(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "de.handelsregisternummer.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_165(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "dk.cvr.empty")
    if (!(Pred.lengthEq(subject, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "dk.cvr.length")
    if (!(Pred.asciiDigits(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "dk.cvr.characters")
    if (!(!(Pred.startsWith(subject, K7)))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "dk.cvr.leading")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_166(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "ee.registrikood.empty")
    if (!(Pred.lengthEq(subject, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "ee.registrikood.length")
    if (!(Pred.asciiDigits(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "ee.registrikood.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_167(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "el.gemi.empty")
    if (!(Pred.lengthEq(subject, 12))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "el.gemi.length")
    if (!(Pred.asciiDigits(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "el.gemi.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_168(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "es.nif.empty")
    if (!(Pred.lengthEq(subject, 9))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "es.nif.length")
    if (!(Pred.asciiAlphanumeric(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "es.nif.characters")
    if (!(Pred.charAtIn(subject, 0, K13))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "es.nif.legal_entity")
    if (!(Pred.asciiDigits(Txt.slice(subject, 1, 8)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "es.nif.body")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_169(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "euid.at.empty")
    if (!(Pred.startsWith(subject, K14))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.at.prefix")
    if (!(Pred.contains(subject, K15))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.at.separator")
    if (!(!(Pred.isAbsent(Txt.beforeFirst(Txt.afterFirst(subject, K14), K15))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.at.register")
    if (!(Pred.lengthBetween(Txt.beforeFirst(Txt.afterFirst(subject, K14), K15), 1, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "euid.at.register_length")
    if (!(Pred.asciiAlphanumeric(Txt.beforeFirst(Txt.afterFirst(subject, K14), K15)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "euid.at.register_characters")
    fmt_157(Txt.afterFirst(subject, K15), ctx)?.let { return it }
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_170(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "euid.be.empty")
    if (!(Pred.startsWith(subject, K16))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.be.prefix")
    if (!(Pred.contains(subject, K15))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.be.separator")
    if (!(!(Pred.isAbsent(Txt.beforeFirst(Txt.afterFirst(subject, K16), K15))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.be.register")
    if (!(Pred.lengthBetween(Txt.beforeFirst(Txt.afterFirst(subject, K16), K15), 1, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "euid.be.register_length")
    if (!(Pred.asciiAlphanumeric(Txt.beforeFirst(Txt.afterFirst(subject, K16), K15)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "euid.be.register_characters")
    fmt_158(Txt.afterFirst(subject, K15), ctx)?.let { return it }
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_171(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "euid.bg.empty")
    if (!(Pred.startsWith(subject, K17))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.bg.prefix")
    if (!(Pred.contains(subject, K15))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.bg.separator")
    if (!(!(Pred.isAbsent(Txt.beforeFirst(Txt.afterFirst(subject, K17), K15))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.bg.register")
    if (!(Pred.lengthBetween(Txt.beforeFirst(Txt.afterFirst(subject, K17), K15), 1, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "euid.bg.register_length")
    if (!(Pred.asciiAlphanumeric(Txt.beforeFirst(Txt.afterFirst(subject, K17), K15)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "euid.bg.register_characters")
    fmt_159(Txt.afterFirst(subject, K15), ctx)?.let { return it }
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_172(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "euid.cy.empty")
    if (!(Pred.startsWith(subject, K18))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.cy.prefix")
    if (!(Pred.contains(subject, K15))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.cy.separator")
    if (!(!(Pred.isAbsent(Txt.beforeFirst(Txt.afterFirst(subject, K18), K15))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.cy.register")
    if (!(Pred.lengthBetween(Txt.beforeFirst(Txt.afterFirst(subject, K18), K15), 1, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "euid.cy.register_length")
    if (!(Pred.asciiAlphanumeric(Txt.beforeFirst(Txt.afterFirst(subject, K18), K15)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "euid.cy.register_characters")
    fmt_162(Txt.afterFirst(subject, K15), ctx)?.let { return it }
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_173(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "euid.cz.empty")
    if (!(Pred.startsWith(subject, K19))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.cz.prefix")
    if (!(Pred.contains(subject, K15))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.cz.separator")
    if (!(!(Pred.isAbsent(Txt.beforeFirst(Txt.afterFirst(subject, K19), K15))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.cz.register")
    if (!(Pred.lengthBetween(Txt.beforeFirst(Txt.afterFirst(subject, K19), K15), 1, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "euid.cz.register_length")
    if (!(Pred.asciiAlphanumeric(Txt.beforeFirst(Txt.afterFirst(subject, K19), K15)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "euid.cz.register_characters")
    fmt_163(Txt.afterFirst(subject, K15), ctx)?.let { return it }
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_174(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "euid.de.empty")
    if (!(Pred.startsWith(subject, K20))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.de.prefix")
    if (!(Pred.contains(subject, K15))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.de.separator")
    if (!(!(Pred.isAbsent(Txt.beforeFirst(Txt.afterFirst(subject, K20), K15))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.de.register")
    if (!(Pred.lengthBetween(Txt.beforeFirst(Txt.afterFirst(subject, K20), K15), 5, 6))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "euid.de.register_length")
    if (!(Pred.asciiAlphanumeric(Txt.beforeFirst(Txt.afterFirst(subject, K20), K15)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "euid.de.register_characters")
    if (!(((Pred.lengthEq(Txt.beforeFirst(Txt.afterFirst(subject, K20), K15), 5) && (Pred.prefixInPacked(Txt.beforeFirst(Txt.afterFirst(subject, K20), K15), K21, 5, 5))) || (Pred.lengthEq(Txt.beforeFirst(Txt.afterFirst(subject, K20), K15), 6) && (Pred.prefixInPacked(Txt.beforeFirst(Txt.afterFirst(subject, K20), K15), K22, 6, 6)))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.de.register_unknown")
    fmt_164(Txt.afterFirst(subject, K15), ctx)?.let { return it }
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_175(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "euid.dk.empty")
    if (!(Pred.startsWith(subject, K23))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.dk.prefix")
    if (!(Pred.contains(subject, K15))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.dk.separator")
    if (!(!(Pred.isAbsent(Txt.beforeFirst(Txt.afterFirst(subject, K23), K15))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.dk.register")
    if (!(Pred.lengthBetween(Txt.beforeFirst(Txt.afterFirst(subject, K23), K15), 1, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "euid.dk.register_length")
    if (!(Pred.asciiAlphanumeric(Txt.beforeFirst(Txt.afterFirst(subject, K23), K15)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "euid.dk.register_characters")
    fmt_165(Txt.afterFirst(subject, K15), ctx)?.let { return it }
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_176(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "euid.ee.empty")
    if (!(Pred.startsWith(subject, K24))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.ee.prefix")
    if (!(Pred.contains(subject, K15))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.ee.separator")
    if (!(!(Pred.isAbsent(Txt.beforeFirst(Txt.afterFirst(subject, K24), K15))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.ee.register")
    if (!(Pred.lengthBetween(Txt.beforeFirst(Txt.afterFirst(subject, K24), K15), 1, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "euid.ee.register_length")
    if (!(Pred.asciiAlphanumeric(Txt.beforeFirst(Txt.afterFirst(subject, K24), K15)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "euid.ee.register_characters")
    fmt_166(Txt.afterFirst(subject, K15), ctx)?.let { return it }
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_177(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "euid.el.empty")
    if (!(Pred.startsWith(subject, K9))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.el.prefix")
    if (!(Pred.contains(subject, K15))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.el.separator")
    if (!(!(Pred.isAbsent(Txt.beforeFirst(Txt.afterFirst(subject, K9), K15))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.el.register")
    if (!(Pred.lengthBetween(Txt.beforeFirst(Txt.afterFirst(subject, K9), K15), 1, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "euid.el.register_length")
    if (!(Pred.asciiAlphanumeric(Txt.beforeFirst(Txt.afterFirst(subject, K9), K15)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "euid.el.register_characters")
    fmt_167(Txt.afterFirst(subject, K15), ctx)?.let { return it }
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_178(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "euid.es.empty")
    if (!(Pred.startsWith(subject, K25))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.es.prefix")
    if (!(Pred.contains(subject, K15))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.es.separator")
    if (!(!(Pred.isAbsent(Txt.beforeFirst(Txt.afterFirst(subject, K25), K15))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.es.register")
    if (!(Pred.lengthBetween(Txt.beforeFirst(Txt.afterFirst(subject, K25), K15), 1, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "euid.es.register_length")
    if (!(Pred.asciiAlphanumeric(Txt.beforeFirst(Txt.afterFirst(subject, K25), K15)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "euid.es.register_characters")
    fmt_168(Txt.afterFirst(subject, K15), ctx)?.let { return it }
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_179(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "euid.fi.empty")
    if (!(Pred.startsWith(subject, K26))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.fi.prefix")
    if (!(Pred.contains(subject, K15))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.fi.separator")
    if (!(!(Pred.isAbsent(Txt.beforeFirst(Txt.afterFirst(subject, K26), K15))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.fi.register")
    if (!(Pred.lengthBetween(Txt.beforeFirst(Txt.afterFirst(subject, K26), K15), 1, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "euid.fi.register_length")
    if (!(Pred.asciiAlphanumeric(Txt.beforeFirst(Txt.afterFirst(subject, K26), K15)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "euid.fi.register_characters")
    fmt_196(Txt.afterFirst(subject, K15), ctx)?.let { return it }
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_180(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "euid.fr.empty")
    if (!(Pred.startsWith(subject, K27))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.fr.prefix")
    if (!(Pred.contains(subject, K15))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.fr.separator")
    if (!(!(Pred.isAbsent(Txt.beforeFirst(Txt.afterFirst(subject, K27), K15))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.fr.register")
    if (!(Pred.lengthEq(Txt.beforeFirst(Txt.afterFirst(subject, K27), K15), 4))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "euid.fr.register_length")
    if (!(Pred.asciiDigits(Txt.beforeFirst(Txt.afterFirst(subject, K27), K15)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "euid.fr.register_characters")
    if (!((Pred.prefixInPacked(Txt.beforeFirst(Txt.afterFirst(subject, K27), K15), K28, 4, 4)))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.fr.register_unknown")
    fmt_197(Txt.afterFirst(subject, K15), ctx)?.let { return it }
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_181(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "euid.hr.empty")
    if (!(Pred.startsWith(subject, K29))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.hr.prefix")
    if (!(Pred.contains(subject, K15))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.hr.separator")
    if (!(!(Pred.isAbsent(Txt.beforeFirst(Txt.afterFirst(subject, K29), K15))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.hr.register")
    if (!(Pred.lengthBetween(Txt.beforeFirst(Txt.afterFirst(subject, K29), K15), 1, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "euid.hr.register_length")
    if (!(Pred.asciiAlphanumeric(Txt.beforeFirst(Txt.afterFirst(subject, K29), K15)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "euid.hr.register_characters")
    fmt_202(Txt.afterFirst(subject, K15), ctx)?.let { return it }
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_182(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "euid.hu.empty")
    if (!(Pred.startsWith(subject, K30))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.hu.prefix")
    if (!(Pred.contains(subject, K15))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.hu.separator")
    if (!(!(Pred.isAbsent(Txt.beforeFirst(Txt.afterFirst(subject, K30), K15))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.hu.register")
    if (!(Pred.lengthBetween(Txt.beforeFirst(Txt.afterFirst(subject, K30), K15), 1, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "euid.hu.register_length")
    if (!(Pred.asciiAlphanumeric(Txt.beforeFirst(Txt.afterFirst(subject, K30), K15)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "euid.hu.register_characters")
    fmt_203(Txt.afterFirst(subject, K15), ctx)?.let { return it }
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_183(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "euid.ie.empty")
    if (!(Pred.startsWith(subject, K31))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.ie.prefix")
    if (!(Pred.contains(subject, K15))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.ie.separator")
    if (!(!(Pred.isAbsent(Txt.beforeFirst(Txt.afterFirst(subject, K31), K15))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.ie.register")
    if (!(Pred.lengthBetween(Txt.beforeFirst(Txt.afterFirst(subject, K31), K15), 1, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "euid.ie.register_length")
    if (!(Pred.asciiAlphanumeric(Txt.beforeFirst(Txt.afterFirst(subject, K31), K15)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "euid.ie.register_characters")
    fmt_204(Txt.afterFirst(subject, K15), ctx)?.let { return it }
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_184(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "euid.it.empty")
    if (!(Pred.startsWith(subject, K32))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.it.prefix")
    if (!(Pred.contains(subject, K15))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.it.separator")
    if (!(!(Pred.isAbsent(Txt.beforeFirst(Txt.afterFirst(subject, K32), K15))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.it.register")
    if (!(Pred.lengthBetween(Txt.beforeFirst(Txt.afterFirst(subject, K32), K15), 1, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "euid.it.register_length")
    if (!(Pred.asciiAlphanumeric(Txt.beforeFirst(Txt.afterFirst(subject, K32), K15)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "euid.it.register_characters")
    fmt_205(Txt.afterFirst(subject, K15), ctx)?.let { return it }
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_185(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "euid.lt.empty")
    if (!(Pred.startsWith(subject, K33))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.lt.prefix")
    if (!(Pred.contains(subject, K15))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.lt.separator")
    if (!(!(Pred.isAbsent(Txt.beforeFirst(Txt.afterFirst(subject, K33), K15))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.lt.register")
    if (!(Pred.lengthBetween(Txt.beforeFirst(Txt.afterFirst(subject, K33), K15), 1, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "euid.lt.register_length")
    if (!(Pred.asciiAlphanumeric(Txt.beforeFirst(Txt.afterFirst(subject, K33), K15)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "euid.lt.register_characters")
    fmt_208(Txt.afterFirst(subject, K15), ctx)?.let { return it }
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_186(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "euid.lu.empty")
    if (!(Pred.startsWith(subject, K34))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.lu.prefix")
    if (!(Pred.contains(subject, K15))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.lu.separator")
    if (!(!(Pred.isAbsent(Txt.beforeFirst(Txt.afterFirst(subject, K34), K15))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.lu.register")
    if (!(Pred.lengthBetween(Txt.beforeFirst(Txt.afterFirst(subject, K34), K15), 1, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "euid.lu.register_length")
    if (!(Pred.asciiAlphanumeric(Txt.beforeFirst(Txt.afterFirst(subject, K34), K15)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "euid.lu.register_characters")
    fmt_209(Txt.afterFirst(subject, K15), ctx)?.let { return it }
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_187(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "euid.lv.empty")
    if (!(Pred.startsWith(subject, K35))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.lv.prefix")
    if (!(Pred.contains(subject, K15))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.lv.separator")
    if (!(!(Pred.isAbsent(Txt.beforeFirst(Txt.afterFirst(subject, K35), K15))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.lv.register")
    if (!(Pred.lengthBetween(Txt.beforeFirst(Txt.afterFirst(subject, K35), K15), 1, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "euid.lv.register_length")
    if (!(Pred.asciiAlphanumeric(Txt.beforeFirst(Txt.afterFirst(subject, K35), K15)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "euid.lv.register_characters")
    fmt_210(Txt.afterFirst(subject, K15), ctx)?.let { return it }
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_188(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "euid.mt.empty")
    if (!(Pred.startsWith(subject, K36))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.mt.prefix")
    if (!(Pred.contains(subject, K15))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.mt.separator")
    if (!(!(Pred.isAbsent(Txt.beforeFirst(Txt.afterFirst(subject, K36), K15))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.mt.register")
    if (!(Pred.lengthBetween(Txt.beforeFirst(Txt.afterFirst(subject, K36), K15), 1, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "euid.mt.register_length")
    if (!(Pred.asciiAlphanumeric(Txt.beforeFirst(Txt.afterFirst(subject, K36), K15)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "euid.mt.register_characters")
    fmt_211(Txt.afterFirst(subject, K15), ctx)?.let { return it }
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_189(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "euid.nl.empty")
    if (!(Pred.startsWith(subject, K37))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.nl.prefix")
    if (!(Pred.contains(subject, K15))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.nl.separator")
    if (!(!(Pred.isAbsent(Txt.beforeFirst(Txt.afterFirst(subject, K37), K15))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.nl.register")
    if (!(Pred.lengthBetween(Txt.beforeFirst(Txt.afterFirst(subject, K37), K15), 1, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "euid.nl.register_length")
    if (!(Pred.asciiAlphanumeric(Txt.beforeFirst(Txt.afterFirst(subject, K37), K15)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "euid.nl.register_characters")
    fmt_212(Txt.afterFirst(subject, K15), ctx)?.let { return it }
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_190(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "euid.pl.empty")
    if (!(Pred.startsWith(subject, K38))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.pl.prefix")
    if (!(Pred.contains(subject, K15))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.pl.separator")
    if (!(!(Pred.isAbsent(Txt.beforeFirst(Txt.afterFirst(subject, K38), K15))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.pl.register")
    if (!(Pred.lengthBetween(Txt.beforeFirst(Txt.afterFirst(subject, K38), K15), 1, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "euid.pl.register_length")
    if (!(Pred.asciiAlphanumeric(Txt.beforeFirst(Txt.afterFirst(subject, K38), K15)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "euid.pl.register_characters")
    fmt_213(Txt.afterFirst(subject, K15), ctx)?.let { return it }
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_191(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "euid.pt.empty")
    if (!(Pred.startsWith(subject, K39))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.pt.prefix")
    if (!(Pred.contains(subject, K15))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.pt.separator")
    if (!(!(Pred.isAbsent(Txt.beforeFirst(Txt.afterFirst(subject, K39), K15))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.pt.register")
    if (!(Pred.lengthBetween(Txt.beforeFirst(Txt.afterFirst(subject, K39), K15), 1, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "euid.pt.register_length")
    if (!(Pred.asciiAlphanumeric(Txt.beforeFirst(Txt.afterFirst(subject, K39), K15)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "euid.pt.register_characters")
    fmt_214(Txt.afterFirst(subject, K15), ctx)?.let { return it }
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_192(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "euid.ro.empty")
    if (!(Pred.startsWith(subject, K40))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.ro.prefix")
    if (!(Pred.contains(subject, K15))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.ro.separator")
    if (!(!(Pred.isAbsent(Txt.beforeFirst(Txt.afterFirst(subject, K40), K15))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.ro.register")
    if (!(Pred.lengthBetween(Txt.beforeFirst(Txt.afterFirst(subject, K40), K15), 1, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "euid.ro.register_length")
    if (!(Pred.asciiAlphanumeric(Txt.beforeFirst(Txt.afterFirst(subject, K40), K15)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "euid.ro.register_characters")
    fmt_215(Txt.afterFirst(subject, K15), ctx)?.let { return it }
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_193(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "euid.se.empty")
    if (!(Pred.startsWith(subject, K41))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.se.prefix")
    if (!(Pred.contains(subject, K15))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.se.separator")
    if (!(!(Pred.isAbsent(Txt.beforeFirst(Txt.afterFirst(subject, K41), K15))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.se.register")
    if (!(Pred.lengthBetween(Txt.beforeFirst(Txt.afterFirst(subject, K41), K15), 1, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "euid.se.register_length")
    if (!(Pred.asciiAlphanumeric(Txt.beforeFirst(Txt.afterFirst(subject, K41), K15)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "euid.se.register_characters")
    fmt_216(Txt.afterFirst(subject, K15), ctx)?.let { return it }
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_194(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "euid.si.empty")
    if (!(Pred.startsWith(subject, K42))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.si.prefix")
    if (!(Pred.contains(subject, K15))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.si.separator")
    if (!(!(Pred.isAbsent(Txt.beforeFirst(Txt.afterFirst(subject, K42), K15))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.si.register")
    if (!(Pred.lengthBetween(Txt.beforeFirst(Txt.afterFirst(subject, K42), K15), 1, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "euid.si.register_length")
    if (!(Pred.asciiAlphanumeric(Txt.beforeFirst(Txt.afterFirst(subject, K42), K15)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "euid.si.register_characters")
    fmt_217(Txt.afterFirst(subject, K15), ctx)?.let { return it }
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_195(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "euid.sk.empty")
    if (!(Pred.startsWith(subject, K43))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.sk.prefix")
    if (!(Pred.contains(subject, K15))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.sk.separator")
    if (!(!(Pred.isAbsent(Txt.beforeFirst(Txt.afterFirst(subject, K43), K15))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "euid.sk.register")
    if (!(Pred.lengthBetween(Txt.beforeFirst(Txt.afterFirst(subject, K43), K15), 1, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "euid.sk.register_length")
    if (!(Pred.asciiAlphanumeric(Txt.beforeFirst(Txt.afterFirst(subject, K43), K15)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "euid.sk.register_characters")
    fmt_218(Txt.afterFirst(subject, K15), ctx)?.let { return it }
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_196(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "fi.y_tunnus.empty")
    if (!(Pred.lengthEq(subject, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "fi.y_tunnus.length")
    if (!(Pred.asciiDigits(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "fi.y_tunnus.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_197(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "fr.siren.empty")
    if (!(Pred.lengthEq(subject, 9))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "fr.siren.length")
    if (!(Pred.asciiDigits(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "fr.siren.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_198(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "fr.siret.empty")
    if (!(Pred.lengthEq(subject, 14))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "fr.siret.length")
    if (!(Pred.asciiDigits(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "fr.siret.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_199(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "company_number.empty")
    if (!(Pred.lengthEq(subject, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "company_number.length")
    if (!(Pred.asciiAlphanumeric(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "company_number.characters")
    if (!((Pred.asciiDigits(subject) || (Pred.prefixInPacked(subject, K44, 2, 2))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "company_number.prefix")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_200(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "duns.empty")
    if (!(Pred.lengthEq(subject, 9))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "duns.length")
    if (!(Pred.asciiDigits(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "duns.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_201(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "eori.empty")
    if (!(Pred.lengthBetween(subject, 3, 17))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "eori.length")
    if (!(Pred.asciiAlphanumeric(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "eori.characters")
    if (!(Pred.asciiUpperLetters(Txt.slice(subject, 0, 2)))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "eori.country_prefix")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_202(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "hr.mbs.empty")
    if (!(Pred.lengthEq(subject, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "hr.mbs.length")
    if (!(Pred.asciiDigits(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "hr.mbs.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_203(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "hu.cegjegyzekszam.empty")
    if (!(Pred.lengthEq(subject, 10))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "hu.cegjegyzekszam.length")
    if (!(Pred.asciiDigits(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "hu.cegjegyzekszam.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_204(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "ie.cro_number.empty")
    if (!(Pred.lengthBetween(subject, 5, 7))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "ie.cro_number.length")
    if (!(Pred.asciiDigits(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "ie.cro_number.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_205(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "it.codice_fiscale_impresa.empty")
    if (!(Pred.lengthEq(subject, 11))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "it.codice_fiscale_impresa.length")
    if (!(Pred.asciiDigits(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "it.codice_fiscale_impresa.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_206(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "corporate_number.empty")
    if (!(Pred.lengthEq(subject, 13))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "corporate_number.length")
    if (!(Pred.asciiDigits(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "corporate_number.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_207(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "lei.empty")
    if (!(Pred.lengthEq(subject, 20))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "lei.length")
    if (!(Pred.asciiAlphanumeric(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "lei.characters")
    if (!(Pred.asciiDigits(Txt.slice(subject, 18, 20)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "lei.check_digits")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_208(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "lt.juridinio_asmens_kodas.empty")
    if (!(Pred.lengthEq(subject, 9))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "lt.juridinio_asmens_kodas.length")
    if (!(Pred.asciiDigits(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "lt.juridinio_asmens_kodas.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_209(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "lu.rcs_number.empty")
    if (!(Pred.lengthBetween(subject, 5, 7))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "lu.rcs_number.length")
    if (!(Pred.charAtIn(subject, 0, K10))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "lu.rcs_number.prefix")
    if (!(Pred.asciiDigits(Txt.sliceFrom(subject, 1)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "lu.rcs_number.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_210(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "lv.registracijas_numurs.empty")
    if (!(Pred.lengthEq(subject, 11))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "lv.registracijas_numurs.length")
    if (!(Pred.asciiDigits(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "lv.registracijas_numurs.characters")
    if (!(Pred.charAtIn(subject, 0, K45))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "lv.registracijas_numurs.legal_entity")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_211(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "mt.mbr_number.empty")
    if (!(Pred.lengthBetween(subject, 5, 7))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "mt.mbr_number.length")
    if (!(Pred.startsWith(subject, K46))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "mt.mbr_number.prefix")
    if (!(Pred.asciiDigits(Txt.sliceFrom(subject, 1)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "mt.mbr_number.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_212(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "nl.kvk.empty")
    if (!(Pred.lengthEq(subject, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "nl.kvk.length")
    if (!(Pred.asciiDigits(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "nl.kvk.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_213(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "pl.krs.empty")
    if (!(Pred.lengthEq(subject, 10))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "pl.krs.length")
    if (!(Pred.asciiDigits(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "pl.krs.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_214(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "pt.nipc.empty")
    if (!(Pred.lengthEq(subject, 9))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "pt.nipc.length")
    if (!(Pred.asciiDigits(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "pt.nipc.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_215(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "ro.cui.empty")
    if (!(Pred.lengthBetween(subject, 2, 10))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "ro.cui.length")
    if (!(Pred.asciiDigits(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "ro.cui.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_216(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "se.organisationsnummer.empty")
    if (!(Pred.lengthEq(subject, 10))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "se.organisationsnummer.length")
    if (!(Pred.asciiDigits(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "se.organisationsnummer.characters")
    if (!(Pred.charAtIn(subject, 2, K47))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "se.organisationsnummer.group")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_217(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "si.maticna_stevilka.empty")
    if (!(Pred.lengthEq(subject, 7))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "si.maticna_stevilka.length")
    if (!(Pred.asciiDigits(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "si.maticna_stevilka.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_218(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "sk.ico.empty")
    if (!(Pred.lengthEq(subject, 8))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "sk.ico.length")
    if (!(Pred.asciiDigits(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "sk.ico.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_219(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "ein.empty")
    if (!(Pred.lengthEq(subject, 9))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "ein.length")
    if (!(Pred.asciiDigits(subject))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "ein.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_220(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.at.empty")
    if (!(Pred.lengthEq(subject, 11))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.at.length")
    if (!(Pred.startsWith(subject, K14))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.at.prefix")
    if (!(Pred.asciiDigits(Txt.sliceFrom(subject, 3)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.at.characters")
    if (!(Pred.charAtIn(subject, 2, K48))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.at.u_marker")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_221(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.be.empty")
    if (!(Pred.lengthEq(subject, 12))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.be.length")
    if (!(Pred.startsWith(subject, K16))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.be.prefix")
    if (!(Pred.asciiDigits(Txt.sliceFrom(subject, 2)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.be.characters")
    if (!(Pred.charAtIn(subject, 2, K49))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.be.enterprise_prefix")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_222(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.bg.empty")
    if (!((Pred.lengthEq(subject, 11) || Pred.lengthEq(subject, 12)))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.bg.length")
    if (!(Pred.startsWith(subject, K17))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.bg.prefix")
    if (!(Pred.asciiDigits(Txt.sliceFrom(subject, 2)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.bg.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_223(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.cy.empty")
    if (!(Pred.lengthEq(subject, 11))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.cy.length")
    if (!(Pred.startsWith(subject, K18))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.cy.prefix")
    if (!(Pred.charAtIn(subject, 10, K10))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.cy.check_letter")
    if (!(Pred.asciiDigits(Txt.slice(subject, 2, 10)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.cy.body_characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_224(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.cz.empty")
    if (!(Pred.lengthBetween(subject, 10, 12))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.cz.length")
    if (!(Pred.startsWith(subject, K19))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.cz.prefix")
    if (!(Pred.asciiDigits(Txt.sliceFrom(subject, 2)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.cz.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_225(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.de.empty")
    if (!(Pred.lengthEq(subject, 11))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.de.length")
    if (!(Pred.startsWith(subject, K20))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.de.prefix")
    if (!(Pred.asciiDigits(Txt.sliceFrom(subject, 2)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.de.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_226(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.dk.empty")
    if (!(Pred.lengthEq(subject, 10))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.dk.length")
    if (!(Pred.startsWith(subject, K23))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.dk.prefix")
    if (!(Pred.asciiDigits(Txt.sliceFrom(subject, 2)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.dk.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_227(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.ee.empty")
    if (!(Pred.lengthEq(subject, 11))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.ee.length")
    if (!(Pred.startsWith(subject, K24))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.ee.prefix")
    if (!(Pred.asciiDigits(Txt.sliceFrom(subject, 2)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.ee.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_228(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.es.empty")
    if (!(Pred.lengthEq(subject, 11))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.es.length")
    if (!(Pred.startsWith(subject, K25))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.es.prefix")
    if (!(Pred.asciiAlphanumeric(Txt.sliceFrom(subject, 2)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.es.characters")
    if (!(((Pred.charAtIn(subject, 2, K13) && Pred.asciiDigits(Txt.slice(subject, 3, 10))) || (Pred.asciiDigits(Txt.slice(subject, 2, 10)) && Pred.charAtIn(subject, 10, K50)) || (Pred.charAtIn(subject, 2, K51) && Pred.asciiDigits(Txt.slice(subject, 3, 10)) && Pred.charAtIn(subject, 10, K50))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.es.shape")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_229(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.fi.empty")
    if (!(Pred.lengthEq(subject, 10))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.fi.length")
    if (!(Pred.startsWith(subject, K26))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.fi.prefix")
    if (!(Pred.asciiDigits(Txt.sliceFrom(subject, 2)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.fi.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_230(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.fr.empty")
    if (!(Pred.lengthEq(subject, 13))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.fr.length")
    if (!(Pred.startsWith(subject, K27))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.fr.prefix")
    if (!((Pred.asciiDigits(Txt.slice(subject, 2, 4)) || ((ctx.profile == ValidationProfile.COMPATIBLE) && Pred.asciiAlphanumeric(Txt.slice(subject, 2, 4)))))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.fr.key_characters")
    if (!(Pred.asciiDigits(Txt.sliceFrom(subject, 4)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.fr.siren_characters")
    fmt_197(Txt.sliceFrom(subject, 4), ctx)?.let { return it }
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_231(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.gb.empty")
    if (!((Pred.lengthEq(subject, 11) || Pred.lengthEq(subject, 14)))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.gb.length")
    if (!(Pred.startsWith(subject, K52))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.gb.prefix")
    if (!(Pred.asciiDigits(Txt.sliceFrom(subject, 2)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.gb.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_232(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.gr.empty")
    if (!(Pred.lengthEq(subject, 11))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.gr.length")
    if (!(Pred.startsWith(subject, K9))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.gr.prefix")
    if (!(Pred.asciiDigits(Txt.sliceFrom(subject, 2)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.gr.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_233(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.hr.empty")
    if (!(Pred.lengthEq(subject, 13))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.hr.length")
    if (!(Pred.startsWith(subject, K29))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.hr.prefix")
    if (!(Pred.asciiDigits(Txt.sliceFrom(subject, 2)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.hr.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_234(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.hu.empty")
    if (!(Pred.lengthEq(subject, 10))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.hu.length")
    if (!(Pred.startsWith(subject, K30))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.hu.prefix")
    if (!(Pred.asciiDigits(Txt.sliceFrom(subject, 2)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.hu.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_235(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.ie.empty")
    if (!(Pred.lengthBetween(subject, 10, 11))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.ie.length")
    if (!(Pred.startsWith(subject, K31))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.ie.prefix")
    if (!(Pred.asciiAlphanumeric(Txt.sliceFrom(subject, 2)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.ie.characters")
    if (!(((Pred.lengthEq(subject, 10) && Pred.asciiDigits(Txt.slice(subject, 2, 9)) && Pred.charAtIn(subject, 9, K10)) || (Pred.lengthEq(subject, 11) && Pred.asciiDigits(Txt.slice(subject, 2, 9)) && Pred.charAtIn(subject, 9, K10) && Pred.charAtIn(subject, 10, K10)) || (Pred.lengthEq(subject, 10) && Pred.asciiDigits(Txt.slice(subject, 2, 3)) && Pred.charAtIn(subject, 3, K10) && Pred.asciiDigits(Txt.slice(subject, 4, 9)) && Pred.charAtIn(subject, 9, K10))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.ie.shape")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_236(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.is.empty")
    if (!((Pred.lengthEq(subject, 7) || Pred.lengthEq(subject, 8) || Pred.lengthEq(subject, 12)))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.is.length")
    if (!(Pred.startsWith(subject, K53))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.is.prefix")
    if (!(Pred.asciiDigits(Txt.sliceFrom(subject, 2)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.is.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_237(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.it.empty")
    if (!(Pred.lengthEq(subject, 13))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.it.length")
    if (!(Pred.startsWith(subject, K32))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.it.prefix")
    if (!(Pred.asciiDigits(Txt.sliceFrom(subject, 2)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.it.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_238(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.li.empty")
    if (!(Pred.lengthEq(subject, 7))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.li.length")
    if (!(Pred.startsWith(subject, K54))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.li.prefix")
    if (!(Pred.asciiDigits(Txt.sliceFrom(subject, 2)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.li.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_239(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.lu.empty")
    if (!(Pred.lengthEq(subject, 10))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.lu.length")
    if (!(Pred.startsWith(subject, K34))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.lu.prefix")
    if (!(Pred.asciiDigits(Txt.sliceFrom(subject, 2)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.lu.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_240(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.lv.empty")
    if (!(Pred.lengthEq(subject, 13))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.lv.length")
    if (!(Pred.startsWith(subject, K35))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.lv.prefix")
    if (!(Pred.asciiDigits(Txt.sliceFrom(subject, 2)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.lv.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_241(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.mt.empty")
    if (!(Pred.lengthEq(subject, 10))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.mt.length")
    if (!(Pred.startsWith(subject, K36))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.mt.prefix")
    if (!(Pred.asciiDigits(Txt.sliceFrom(subject, 2)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.mt.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_242(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.nl.empty")
    if (!(Pred.lengthEq(subject, 14))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.nl.length")
    if (!(Pred.startsWith(subject, K37))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.nl.prefix")
    if (!(Pred.asciiDigits(Txt.sliceFrom(subject, 12)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.nl.characters")
    if (!(Pred.charAtIn(subject, 11, K55))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.nl.b_marker")
    if (!(Pred.asciiDigits(Txt.slice(subject, 2, 11)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.nl.body_characters")
    if (!(Pred.asciiDigits(Txt.slice(subject, 12, 14)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.nl.sub_characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_243(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.no.empty")
    if (!(Pred.lengthEq(subject, 11))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.no.length")
    if (!(Pred.startsWith(subject, K56))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.no.prefix")
    if (!(Pred.asciiDigits(Txt.sliceFrom(subject, 2)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.no.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_244(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.pl.empty")
    if (!(Pred.lengthEq(subject, 12))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.pl.length")
    if (!(Pred.startsWith(subject, K38))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.pl.prefix")
    if (!(Pred.asciiDigits(Txt.sliceFrom(subject, 2)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.pl.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_245(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.pt.empty")
    if (!(Pred.lengthEq(subject, 11))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.pt.length")
    if (!(Pred.startsWith(subject, K39))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.pt.prefix")
    if (!(Pred.asciiDigits(Txt.sliceFrom(subject, 2)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.pt.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_246(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.ro.empty")
    if (!(Pred.lengthBetween(subject, 4, 12))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.ro.length")
    if (!(Pred.startsWith(subject, K40))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.ro.prefix")
    if (!(Pred.asciiDigits(Txt.sliceFrom(subject, 2)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.ro.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_247(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.se.empty")
    if (!(Pred.lengthEq(subject, 14))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.se.length")
    if (!(Pred.startsWith(subject, K41))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.se.prefix")
    if (!(Pred.asciiDigits(Txt.sliceFrom(subject, 2)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.se.characters")
    if (!(!((Pred.charAtIn(subject, 12, K57) && Pred.charAtIn(subject, 13, K57))))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.se.branch")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_248(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.si.empty")
    if (!(Pred.lengthEq(subject, 10))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.si.length")
    if (!(Pred.startsWith(subject, K42))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.si.prefix")
    if (!(Pred.asciiDigits(Txt.sliceFrom(subject, 2)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.si.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_249(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.sk.empty")
    if (!(Pred.lengthEq(subject, 12))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.sk.length")
    if (!(Pred.startsWith(subject, K43))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.sk.prefix")
    if (!(Pred.asciiDigits(Txt.sliceFrom(subject, 2)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.sk.characters")
    return null
}

@Suppress("UNUSED_PARAMETER")
internal fun fmt_250(subject: CpView?, ctx: EvalContext): AssertionFailure? {
    if (!(!(Pred.isEmpty(subject)))) return AssertionFailure(ReasonCode.EMPTY, "vat.xi.empty")
    if (!((Pred.lengthEq(subject, 11) || Pred.lengthEq(subject, 14)))) return AssertionFailure(ReasonCode.INVALID_LENGTH, "vat.xi.length")
    if (!(Pred.startsWith(subject, K58))) return AssertionFailure(ReasonCode.INVALID_FORMAT, "vat.xi.prefix")
    if (!(Pred.asciiDigits(Txt.sliceFrom(subject, 2)))) return AssertionFailure(ReasonCode.INVALID_CHARACTERS, "vat.xi.characters")
    return null
}
