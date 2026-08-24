// Generated from businessid-rules.binpb 2026.09.2. Do not edit by hand:
// run `./gradlew generateEngine`. `./gradlew checkGenerated` fails when this
// file and the pinned bundle disagree.
//
// Copyright The LibBusinessID Authors.
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint", "MaxLineLength", "LongMethod", "CyclomaticComplexMethod")

package io.libbusinessid.generated

import io.libbusinessid.Capability
import io.libbusinessid.ReasonCode
import io.libbusinessid.ValidationProfile
import io.libbusinessid.internal.EvalContext
import io.libbusinessid.runtime.AssertionFailure
import io.libbusinessid.runtime.CanonBuffer
import io.libbusinessid.runtime.ChecksumOutcome
import io.libbusinessid.runtime.CpView
import io.libbusinessid.runtime.Pred

/** The country of a dispatch target, absent for a GLOBAL target. */
internal fun targetCountryView(target: Int): CpView? =
    when (target) {
        0 -> K95
        1 -> K96
        2 -> K97
        3 -> K98
        4 -> K99
        5 -> K100
        6 -> K101
        7 -> K102
        9 -> K103
        10 -> K104
        11 -> K105
        13 -> K106
        14 -> K105
        15 -> K103
        16 -> K107
        17 -> K108
        18 -> K109
        19 -> K102
        20 -> K110
        21 -> K111
        22 -> K112
        23 -> K113
        24 -> K114
        25 -> K115
        26 -> K95
        27 -> K100
        28 -> K97
        29 -> K116
        30 -> K117
        31 -> K118
        32 -> K119
        33 -> K120
        34 -> K121
        35 -> K122
        36 -> K101
        37 -> K123
        38 -> K124
        39 -> K125
        40 -> K106
        41 -> K111
        42 -> K109
        43 -> K107
        44 -> K108
        45 -> K125
        46 -> K116
        47 -> K121
        48 -> K120
        50 -> K124
        51 -> K119
        52 -> K115
        53 -> K112
        54 -> K122
        55 -> K123
        56 -> K117
        57 -> K118
        58 -> K110
        59 -> K114
        60 -> K114
        61 -> K126
        62 -> K106
        63 -> K105
        64 -> K103
        65 -> K107
        66 -> K108
        67 -> K109
        68 -> K102
        69 -> K110
        70 -> K112
        71 -> K113
        72 -> K114
        73 -> K98
        74 -> K127
        75 -> K115
        76 -> K95
        77 -> K100
        78 -> K128
        79 -> K97
        80 -> K129
        81 -> K117
        82 -> K118
        83 -> K119
        84 -> K120
        85 -> K130
        86 -> K121
        87 -> K122
        88 -> K101
        89 -> K123
        90 -> K124
        91 -> K125
        92 -> K131
        93 -> K113
        else -> null
    }

/**
 * `prepend_country_if_missing()`: leaves the value alone when it already
 * starts with one of the accepted prefixes of the selected target, and
 * otherwise prepends its canonical prefix, or its country when it declares
 * no canonical prefix.
 */
internal fun prependCountryIfMissing(target: Int, b: CanonBuffer) {
    when (target) {
        0 -> if (true) b.prepend(K30)
        1 -> if (true) b.prepend(K132)
        2 -> if (true) b.prepend(K32)
        3 -> if (true) b.prepend(K52)
        4 -> if (true) b.prepend(K133)
        5 -> if (true) b.prepend(K31)
        6 -> if (true) b.prepend(K40)
        7 -> if (true) b.prepend(K23)
        9 -> if (true) b.prepend(K17)
        10 -> if (true) b.prepend(K134)
        11 -> if (true) b.prepend(K16)
        13 -> if (!(b.startsWith(K14))) b.prepend(K14)
        14 -> if (!(b.startsWith(K16))) b.prepend(K16)
        15 -> if (!(b.startsWith(K17))) b.prepend(K17)
        16 -> if (!(b.startsWith(K18))) b.prepend(K18)
        17 -> if (!(b.startsWith(K19))) b.prepend(K19)
        18 -> if (!(b.startsWith(K20))) b.prepend(K20)
        19 -> if (!(b.startsWith(K23))) b.prepend(K23)
        20 -> if (!(b.startsWith(K24))) b.prepend(K24)
        21 -> if (!(b.startsWith(K9))) b.prepend(K9)
        22 -> if (!(b.startsWith(K25))) b.prepend(K25)
        23 -> if (!(b.startsWith(K26))) b.prepend(K26)
        24 -> if (!(b.startsWith(K27))) b.prepend(K27)
        25 -> if (!(b.startsWith(K29))) b.prepend(K29)
        26 -> if (!(b.startsWith(K30))) b.prepend(K30)
        27 -> if (!(b.startsWith(K31))) b.prepend(K31)
        28 -> if (!(b.startsWith(K32))) b.prepend(K32)
        29 -> if (!(b.startsWith(K33))) b.prepend(K33)
        30 -> if (!(b.startsWith(K34))) b.prepend(K34)
        31 -> if (!(b.startsWith(K35))) b.prepend(K35)
        32 -> if (!(b.startsWith(K36))) b.prepend(K36)
        33 -> if (!(b.startsWith(K37))) b.prepend(K37)
        34 -> if (!(b.startsWith(K38))) b.prepend(K38)
        35 -> if (!(b.startsWith(K39))) b.prepend(K39)
        36 -> if (!(b.startsWith(K40))) b.prepend(K40)
        37 -> if (!(b.startsWith(K41))) b.prepend(K41)
        38 -> if (!(b.startsWith(K42))) b.prepend(K42)
        39 -> if (!(b.startsWith(K43))) b.prepend(K43)
        40 -> if (true) b.prepend(K14)
        41 -> if (true) b.prepend(K9)
        42 -> if (true) b.prepend(K20)
        43 -> if (true) b.prepend(K18)
        44 -> if (true) b.prepend(K19)
        45 -> if (true) b.prepend(K43)
        46 -> if (true) b.prepend(K33)
        47 -> if (true) b.prepend(K38)
        48 -> if (true) b.prepend(K37)
        50 -> if (true) b.prepend(K42)
        51 -> if (true) b.prepend(K36)
        52 -> if (true) b.prepend(K29)
        53 -> if (true) b.prepend(K25)
        54 -> if (true) b.prepend(K39)
        55 -> if (true) b.prepend(K41)
        56 -> if (true) b.prepend(K34)
        57 -> if (true) b.prepend(K35)
        58 -> if (true) b.prepend(K24)
        59 -> if (true) b.prepend(K27)
        60 -> if (true) b.prepend(K27)
        61 -> if (true) b.prepend(K135)
        62 -> if (!(b.startsWith(K14))) b.prepend(K14)
        63 -> if (!(b.startsWith(K16))) b.prepend(K16)
        64 -> if (!(b.startsWith(K17))) b.prepend(K17)
        65 -> if (!(b.startsWith(K18))) b.prepend(K18)
        66 -> if (!(b.startsWith(K19))) b.prepend(K19)
        67 -> if (!(b.startsWith(K20))) b.prepend(K20)
        68 -> if (!(b.startsWith(K23))) b.prepend(K23)
        69 -> if (!(b.startsWith(K24))) b.prepend(K24)
        70 -> if (!(b.startsWith(K25))) b.prepend(K25)
        71 -> if (!(b.startsWith(K26))) b.prepend(K26)
        72 -> if (!(b.startsWith(K27))) b.prepend(K27)
        73 -> if (!(b.startsWith(K52))) b.prepend(K52)
        74 -> if (!(b.startsWith(K9) || b.startsWith(K8))) b.prepend(K9)
        75 -> if (!(b.startsWith(K29))) b.prepend(K29)
        76 -> if (!(b.startsWith(K30))) b.prepend(K30)
        77 -> if (!(b.startsWith(K31))) b.prepend(K31)
        78 -> if (!(b.startsWith(K53))) b.prepend(K53)
        79 -> if (!(b.startsWith(K32))) b.prepend(K32)
        80 -> if (!(b.startsWith(K54))) b.prepend(K54)
        81 -> if (!(b.startsWith(K34))) b.prepend(K34)
        82 -> if (!(b.startsWith(K35))) b.prepend(K35)
        83 -> if (!(b.startsWith(K36))) b.prepend(K36)
        84 -> if (!(b.startsWith(K37))) b.prepend(K37)
        85 -> if (!(b.startsWith(K56))) b.prepend(K56)
        86 -> if (!(b.startsWith(K38))) b.prepend(K38)
        87 -> if (!(b.startsWith(K39))) b.prepend(K39)
        88 -> if (!(b.startsWith(K40))) b.prepend(K40)
        89 -> if (!(b.startsWith(K41))) b.prepend(K41)
        90 -> if (!(b.startsWith(K42))) b.prepend(K42)
        91 -> if (!(b.startsWith(K43))) b.prepend(K43)
        92 -> if (!(b.startsWith(K58))) b.prepend(K58)
        93 -> if (true) b.prepend(K26)
        else -> Unit
    }
}

/** The compiled ruleset: metadata and every table the pipeline reads. */
internal object Ruleset {
    const val RULES_VERSION: String = "2026.09.2"
    const val FORMAT_VERSION: Int = 1
    const val SOURCE_DIGEST: String = "68de82e8f0fa7557f232566b7ef14b504e0ab80637a0cfbc0e598295357aa998"

    /** Every frozen capability this engine implements. */
    val CAPABILITIES: List<Capability> = listOf(
        Capability(1, "CORE_GRAPH_V1"),
        Capability(2, "ASCII_AND_WHITESPACE_V1"),
        Capability(3, "CANONICALIZATION_BASIC_V1"),
        Capability(4, "CANONICALIZATION_CONDITIONAL_V1"),
        Capability(5, "IDENTIFIER_DISPATCH_V1"),
        Capability(10, "STRING_VIEWS_V1"),
        Capability(11, "CAPTURES_AND_CALLS_V1"),
        Capability(20, "FORMAT_ASSERTIONS_V1"),
        Capability(21, "PROFILES_V1"),
        Capability(30, "CHECKSUM_TRISTATE_V1"),
        Capability(31, "CHECKSUM_LUHN_V1"),
        Capability(32, "CHECKSUM_MOD97_V1"),
        Capability(33, "CHECKSUM_WEIGHTED_V1"),
        Capability(34, "CHECKSUM_COMPARE_CONSTANT_V1"),
        Capability(35, "CHECKSUM_INTEGER_PREDICATE_V1"),
        Capability(40, "PROVENANCE_V1"),
        Capability(41, "PROVENANCE_TIER_V1"),
        Capability(42, "CHECKSUM_CUSTOM_ALPHABET_V1"),
    )

    /** Every kind the compiled ruleset can dispatch. */
    val KINDS: List<String> = listOf(
        "cegjegyzekszam",
        "cnpj",
        "codice_fiscale_impresa",
        "company_number",
        "corporate_number",
        "cro_number",
        "cui",
        "cvr",
        "duns",
        "eik",
        "ein",
        "enterprise_number",
        "eori",
        "euid",
        "firmenbuchnummer",
        "gemi",
        "handelsregisternummer",
        "he_number",
        "ico",
        "juridinio_asmens_kodas",
        "krs",
        "kvk",
        "lei",
        "maticna_stevilka",
        "mbr_number",
        "mbs",
        "nif",
        "nipc",
        "organisationsnummer",
        "rcs_number",
        "registracijas_numurs",
        "registrikood",
        "siren",
        "siret",
        "uscc",
        "vat",
        "y_tunnus",
    )

    /** The dispatcher of a normalised kind token, or -1. */
    fun dispatcherOf(kind: String): Int =
        when (kind) {
            "cegjegyzekszam", "hu_cegjegyzekszam" -> 0
            "cnpj", "br_cnpj" -> 1
            "codice_fiscale_impresa", "it_codice_fiscale_impresa" -> 2
            "company_number", "company_registration_number", "crn", "gb_company_number", "uk_company_number" -> 3
            "corporate_number", "hojin_bango", "houjin_bangou", "jp_corporate_number" -> 4
            "cro_number", "ie_cro_number" -> 5
            "cui", "cif", "ro_cui" -> 6
            "cvr", "cvr_nummer", "dk_cvr" -> 7
            "duns", "dnb", "duns_number" -> 8
            "eik", "bg_eik", "bulstat" -> 9
            "ein", "federal_tax_id", "us_ein" -> 10
            "enterprise_number", "be_enterprise_number", "numero_entreprise", "ondernemingsnummer" -> 11
            "eori", "eori_number" -> 12
            "euid" -> 13
            "firmenbuchnummer", "at_firmenbuchnummer", "fn" -> 14
            "gemi", "el_gemi", "gr_gemi" -> 15
            "handelsregisternummer", "de_handelsregisternummer", "hrb" -> 16
            "he_number", "cy_he_number" -> 17
            "ico", "cz_ico", "sk_ico" -> 18
            "juridinio_asmens_kodas", "lt_juridinio_asmens_kodas" -> 19
            "krs", "pl_krs" -> 20
            "kvk", "kvk_nummer", "nl_kvk" -> 21
            "lei" -> 22
            "maticna_stevilka", "si_maticna_stevilka" -> 23
            "mbr_number", "mt_mbr_number" -> 24
            "mbs", "hr_mbs" -> 25
            "nif", "es_nif" -> 26
            "nipc", "pt_nipc" -> 27
            "organisationsnummer", "se_organisationsnummer" -> 28
            "rcs_number", "lu_rcs_number" -> 29
            "registracijas_numurs", "lv_registracijas_numurs" -> 30
            "registrikood", "ee_registrikood" -> 31
            "siren", "fr_siren" -> 32
            "siret", "fr_siret" -> 33
            "uscc", "cn_uscc", "unified_social_credit_code", "usci" -> 34
            "vat", "vat_id", "vat_number" -> 35
            "y_tunnus", "business_id", "fi_y_tunnus" -> 36
            else -> -1
        }

    /** The canonical kind of a dispatcher. */
    fun dispatcherKind(dispatcher: Int): String =
        when (dispatcher) {
            0 -> "cegjegyzekszam"
            1 -> "cnpj"
            2 -> "codice_fiscale_impresa"
            3 -> "company_number"
            4 -> "corporate_number"
            5 -> "cro_number"
            6 -> "cui"
            7 -> "cvr"
            8 -> "duns"
            9 -> "eik"
            10 -> "ein"
            11 -> "enterprise_number"
            12 -> "eori"
            13 -> "euid"
            14 -> "firmenbuchnummer"
            15 -> "gemi"
            16 -> "handelsregisternummer"
            17 -> "he_number"
            18 -> "ico"
            19 -> "juridinio_asmens_kodas"
            20 -> "krs"
            21 -> "kvk"
            22 -> "lei"
            23 -> "maticna_stevilka"
            24 -> "mbr_number"
            25 -> "mbs"
            26 -> "nif"
            27 -> "nipc"
            28 -> "organisationsnummer"
            29 -> "rcs_number"
            30 -> "registracijas_numurs"
            31 -> "registrikood"
            32 -> "siren"
            33 -> "siret"
            34 -> "uscc"
            35 -> "vat"
            36 -> "y_tunnus"
            else -> ""
        }

    /** The pre-canonicalisation program of a dispatcher; no target is selected yet. */
    fun preCanonicalize(dispatcher: Int, b: CanonBuffer, profile: ValidationProfile) {
        when (dispatcher) {
            0 -> canon_9(b, profile, -1)
            1 -> canon_9(b, profile, -1)
            2 -> canon_9(b, profile, -1)
            3 -> canon_9(b, profile, -1)
            4 -> canon_9(b, profile, -1)
            5 -> canon_9(b, profile, -1)
            6 -> canon_9(b, profile, -1)
            7 -> canon_9(b, profile, -1)
            8 -> canon_9(b, profile, -1)
            9 -> canon_9(b, profile, -1)
            10 -> canon_9(b, profile, -1)
            11 -> canon_9(b, profile, -1)
            12 -> canon_9(b, profile, -1)
            13 -> canon_10(b, profile, -1)
            14 -> canon_9(b, profile, -1)
            15 -> canon_9(b, profile, -1)
            16 -> canon_9(b, profile, -1)
            17 -> canon_9(b, profile, -1)
            18 -> canon_9(b, profile, -1)
            19 -> canon_9(b, profile, -1)
            20 -> canon_9(b, profile, -1)
            21 -> canon_9(b, profile, -1)
            22 -> canon_9(b, profile, -1)
            23 -> canon_9(b, profile, -1)
            24 -> canon_9(b, profile, -1)
            25 -> canon_9(b, profile, -1)
            26 -> canon_9(b, profile, -1)
            27 -> canon_9(b, profile, -1)
            28 -> canon_9(b, profile, -1)
            29 -> canon_9(b, profile, -1)
            30 -> canon_9(b, profile, -1)
            31 -> canon_9(b, profile, -1)
            32 -> canon_9(b, profile, -1)
            33 -> canon_9(b, profile, -1)
            34 -> canon_9(b, profile, -1)
            35 -> canon_9(b, profile, -1)
            36 -> canon_9(b, profile, -1)
            else -> Unit
        }
    }

    /** The country alias table of a dispatcher, applied to a well formed token. */
    fun countryAlias(dispatcher: Int, country: String): String =
        when (dispatcher) {
            35 ->
                when (country) {
                    "EL" -> "GR"
                    "UK" -> "GB"
                    else -> country
                }
            else -> country
        }

    /** The target an explicit country selects in a dispatcher, or -1. */
    fun countryTarget(dispatcher: Int, country: String): Int =
        when (dispatcher) {
            0 ->
                when (country) {
                    "HU" -> 0
                    else -> -1
                }
            1 ->
                when (country) {
                    "BR" -> 1
                    else -> -1
                }
            2 ->
                when (country) {
                    "IT" -> 2
                    else -> -1
                }
            3 ->
                when (country) {
                    "GB" -> 3
                    else -> -1
                }
            4 ->
                when (country) {
                    "JP" -> 4
                    else -> -1
                }
            5 ->
                when (country) {
                    "IE" -> 5
                    else -> -1
                }
            6 ->
                when (country) {
                    "RO" -> 6
                    else -> -1
                }
            7 ->
                when (country) {
                    "DK" -> 7
                    else -> -1
                }
            9 ->
                when (country) {
                    "BG" -> 9
                    else -> -1
                }
            10 ->
                when (country) {
                    "US" -> 10
                    else -> -1
                }
            11 ->
                when (country) {
                    "BE" -> 11
                    else -> -1
                }
            13 ->
                when (country) {
                    "AT" -> 13
                    "BE" -> 14
                    "BG" -> 15
                    "CY" -> 16
                    "CZ" -> 17
                    "DE" -> 18
                    "DK" -> 19
                    "EE" -> 20
                    "EL" -> 21
                    "ES" -> 22
                    "FI" -> 23
                    "FR" -> 24
                    "HR" -> 25
                    "HU" -> 26
                    "IE" -> 27
                    "IT" -> 28
                    "LT" -> 29
                    "LU" -> 30
                    "LV" -> 31
                    "MT" -> 32
                    "NL" -> 33
                    "PL" -> 34
                    "PT" -> 35
                    "RO" -> 36
                    "SE" -> 37
                    "SI" -> 38
                    "SK" -> 39
                    else -> -1
                }
            14 ->
                when (country) {
                    "AT" -> 40
                    else -> -1
                }
            15 ->
                when (country) {
                    "EL" -> 41
                    else -> -1
                }
            16 ->
                when (country) {
                    "DE" -> 42
                    else -> -1
                }
            17 ->
                when (country) {
                    "CY" -> 43
                    else -> -1
                }
            18 ->
                when (country) {
                    "CZ" -> 44
                    "SK" -> 45
                    else -> -1
                }
            19 ->
                when (country) {
                    "LT" -> 46
                    else -> -1
                }
            20 ->
                when (country) {
                    "PL" -> 47
                    else -> -1
                }
            21 ->
                when (country) {
                    "NL" -> 48
                    else -> -1
                }
            23 ->
                when (country) {
                    "SI" -> 50
                    else -> -1
                }
            24 ->
                when (country) {
                    "MT" -> 51
                    else -> -1
                }
            25 ->
                when (country) {
                    "HR" -> 52
                    else -> -1
                }
            26 ->
                when (country) {
                    "ES" -> 53
                    else -> -1
                }
            27 ->
                when (country) {
                    "PT" -> 54
                    else -> -1
                }
            28 ->
                when (country) {
                    "SE" -> 55
                    else -> -1
                }
            29 ->
                when (country) {
                    "LU" -> 56
                    else -> -1
                }
            30 ->
                when (country) {
                    "LV" -> 57
                    else -> -1
                }
            31 ->
                when (country) {
                    "EE" -> 58
                    else -> -1
                }
            32 ->
                when (country) {
                    "FR" -> 59
                    else -> -1
                }
            33 ->
                when (country) {
                    "FR" -> 60
                    else -> -1
                }
            34 ->
                when (country) {
                    "CN" -> 61
                    else -> -1
                }
            35 ->
                when (country) {
                    "AT" -> 62
                    "BE" -> 63
                    "BG" -> 64
                    "CY" -> 65
                    "CZ" -> 66
                    "DE" -> 67
                    "DK" -> 68
                    "EE" -> 69
                    "ES" -> 70
                    "FI" -> 71
                    "FR" -> 72
                    "GB" -> 73
                    "GR" -> 74
                    "HR" -> 75
                    "HU" -> 76
                    "IE" -> 77
                    "IS" -> 78
                    "IT" -> 79
                    "LI" -> 80
                    "LU" -> 81
                    "LV" -> 82
                    "MT" -> 83
                    "NL" -> 84
                    "NO" -> 85
                    "PL" -> 86
                    "PT" -> 87
                    "RO" -> 88
                    "SE" -> 89
                    "SI" -> 90
                    "SK" -> 91
                    "XI" -> 92
                    else -> -1
                }
            36 ->
                when (country) {
                    "FI" -> 93
                    else -> -1
                }
            else -> -1
        }

    /** The target owning the longest declared prefix that starts the value, or -1. */
    fun prefixTarget(dispatcher: Int, value: CpView): Int =
        when (dispatcher) {
            13 ->
                when {
                    Pred.startsWith(value, K14) -> 13
                    Pred.startsWith(value, K16) -> 14
                    Pred.startsWith(value, K17) -> 15
                    Pred.startsWith(value, K18) -> 16
                    Pred.startsWith(value, K19) -> 17
                    Pred.startsWith(value, K20) -> 18
                    Pred.startsWith(value, K23) -> 19
                    Pred.startsWith(value, K24) -> 20
                    Pred.startsWith(value, K9) -> 21
                    Pred.startsWith(value, K25) -> 22
                    Pred.startsWith(value, K26) -> 23
                    Pred.startsWith(value, K27) -> 24
                    Pred.startsWith(value, K29) -> 25
                    Pred.startsWith(value, K30) -> 26
                    Pred.startsWith(value, K31) -> 27
                    Pred.startsWith(value, K32) -> 28
                    Pred.startsWith(value, K33) -> 29
                    Pred.startsWith(value, K34) -> 30
                    Pred.startsWith(value, K35) -> 31
                    Pred.startsWith(value, K36) -> 32
                    Pred.startsWith(value, K37) -> 33
                    Pred.startsWith(value, K38) -> 34
                    Pred.startsWith(value, K39) -> 35
                    Pred.startsWith(value, K40) -> 36
                    Pred.startsWith(value, K41) -> 37
                    Pred.startsWith(value, K42) -> 38
                    Pred.startsWith(value, K43) -> 39
                    else -> -1
                }
            35 ->
                when {
                    Pred.startsWith(value, K14) -> 62
                    Pred.startsWith(value, K16) -> 63
                    Pred.startsWith(value, K17) -> 64
                    Pred.startsWith(value, K18) -> 65
                    Pred.startsWith(value, K19) -> 66
                    Pred.startsWith(value, K20) -> 67
                    Pred.startsWith(value, K23) -> 68
                    Pred.startsWith(value, K24) -> 69
                    Pred.startsWith(value, K9) -> 74
                    Pred.startsWith(value, K25) -> 70
                    Pred.startsWith(value, K26) -> 71
                    Pred.startsWith(value, K27) -> 72
                    Pred.startsWith(value, K52) -> 73
                    Pred.startsWith(value, K8) -> 74
                    Pred.startsWith(value, K29) -> 75
                    Pred.startsWith(value, K30) -> 76
                    Pred.startsWith(value, K31) -> 77
                    Pred.startsWith(value, K53) -> 78
                    Pred.startsWith(value, K32) -> 79
                    Pred.startsWith(value, K54) -> 80
                    Pred.startsWith(value, K34) -> 81
                    Pred.startsWith(value, K35) -> 82
                    Pred.startsWith(value, K36) -> 83
                    Pred.startsWith(value, K37) -> 84
                    Pred.startsWith(value, K56) -> 85
                    Pred.startsWith(value, K38) -> 86
                    Pred.startsWith(value, K39) -> 87
                    Pred.startsWith(value, K40) -> 88
                    Pred.startsWith(value, K41) -> 89
                    Pred.startsWith(value, K42) -> 90
                    Pred.startsWith(value, K43) -> 91
                    Pred.startsWith(value, K58) -> 92
                    else -> -1
                }
            else -> -1
        }

    /** The single GLOBAL target of a dispatcher, or -1. */
    fun globalTarget(dispatcher: Int): Int =
        when (dispatcher) {
            8 -> 8
            12 -> 12
            22 -> 49
            else -> -1
        }

    /** The single target a dispatcher accepts without prefix and without country, or -1. */
    fun unprefixedTarget(dispatcher: Int): Int =
        when (dispatcher) {
            0 -> 0
            1 -> 1
            2 -> 2
            3 -> 3
            4 -> 4
            5 -> 5
            6 -> 6
            7 -> 7
            9 -> 9
            10 -> 10
            11 -> 11
            14 -> 40
            15 -> 41
            16 -> 42
            17 -> 43
            19 -> 46
            20 -> 47
            21 -> 48
            23 -> 50
            24 -> 51
            25 -> 52
            26 -> 53
            27 -> 54
            28 -> 55
            29 -> 56
            30 -> 57
            31 -> 58
            32 -> 59
            33 -> 60
            34 -> 61
            36 -> 93
            else -> -1
        }

    /** The definition a target routes to. */
    fun definitionOf(target: Int): Int =
        when (target) {
            0 -> 0
            1 -> 1
            2 -> 2
            3 -> 3
            4 -> 4
            5 -> 5
            6 -> 6
            7 -> 7
            8 -> 8
            9 -> 9
            10 -> 10
            11 -> 11
            12 -> 12
            13 -> 13
            14 -> 14
            15 -> 15
            16 -> 16
            17 -> 17
            18 -> 18
            19 -> 19
            20 -> 20
            21 -> 21
            22 -> 22
            23 -> 23
            24 -> 24
            25 -> 25
            26 -> 26
            27 -> 27
            28 -> 28
            29 -> 29
            30 -> 30
            31 -> 31
            32 -> 32
            33 -> 33
            34 -> 34
            35 -> 35
            36 -> 36
            37 -> 37
            38 -> 38
            39 -> 39
            40 -> 40
            41 -> 41
            42 -> 42
            43 -> 43
            44 -> 44
            45 -> 45
            46 -> 46
            47 -> 47
            48 -> 48
            49 -> 49
            50 -> 50
            51 -> 51
            52 -> 52
            53 -> 53
            54 -> 54
            55 -> 55
            56 -> 56
            57 -> 57
            58 -> 58
            59 -> 59
            60 -> 60
            61 -> 61
            62 -> 62
            63 -> 63
            64 -> 64
            65 -> 65
            66 -> 66
            67 -> 67
            68 -> 68
            69 -> 69
            70 -> 70
            71 -> 71
            72 -> 72
            73 -> 73
            74 -> 74
            75 -> 75
            76 -> 76
            77 -> 77
            78 -> 78
            79 -> 79
            80 -> 80
            81 -> 81
            82 -> 82
            83 -> 83
            84 -> 84
            85 -> 85
            86 -> 86
            87 -> 87
            88 -> 88
            89 -> 89
            90 -> 90
            91 -> 91
            92 -> 92
            93 -> 93
            else -> -1
        }

    /** The ISO country of a target, or null for a GLOBAL one. */
    fun targetCountry(target: Int): String? =
        when (target) {
            0 -> "HU"
            1 -> "BR"
            2 -> "IT"
            3 -> "GB"
            4 -> "JP"
            5 -> "IE"
            6 -> "RO"
            7 -> "DK"
            9 -> "BG"
            10 -> "US"
            11 -> "BE"
            13 -> "AT"
            14 -> "BE"
            15 -> "BG"
            16 -> "CY"
            17 -> "CZ"
            18 -> "DE"
            19 -> "DK"
            20 -> "EE"
            21 -> "EL"
            22 -> "ES"
            23 -> "FI"
            24 -> "FR"
            25 -> "HR"
            26 -> "HU"
            27 -> "IE"
            28 -> "IT"
            29 -> "LT"
            30 -> "LU"
            31 -> "LV"
            32 -> "MT"
            33 -> "NL"
            34 -> "PL"
            35 -> "PT"
            36 -> "RO"
            37 -> "SE"
            38 -> "SI"
            39 -> "SK"
            40 -> "AT"
            41 -> "EL"
            42 -> "DE"
            43 -> "CY"
            44 -> "CZ"
            45 -> "SK"
            46 -> "LT"
            47 -> "PL"
            48 -> "NL"
            50 -> "SI"
            51 -> "MT"
            52 -> "HR"
            53 -> "ES"
            54 -> "PT"
            55 -> "SE"
            56 -> "LU"
            57 -> "LV"
            58 -> "EE"
            59 -> "FR"
            60 -> "FR"
            61 -> "CN"
            62 -> "AT"
            63 -> "BE"
            64 -> "BG"
            65 -> "CY"
            66 -> "CZ"
            67 -> "DE"
            68 -> "DK"
            69 -> "EE"
            70 -> "ES"
            71 -> "FI"
            72 -> "FR"
            73 -> "GB"
            74 -> "GR"
            75 -> "HR"
            76 -> "HU"
            77 -> "IE"
            78 -> "IS"
            79 -> "IT"
            80 -> "LI"
            81 -> "LU"
            82 -> "LV"
            83 -> "MT"
            84 -> "NL"
            85 -> "NO"
            86 -> "PL"
            87 -> "PT"
            88 -> "RO"
            89 -> "SE"
            90 -> "SI"
            91 -> "SK"
            92 -> "XI"
            93 -> "FI"
            else -> null
        }

    /** The profile a definition applies when the caller states none. */
    fun defaultProfile(definition: Int): ValidationProfile =
        // Every definition of this ruleset declares `compatible`.
        ValidationProfile.COMPATIBLE

    /** The canonicalisation program of a definition, run on the pre-canonical value. */
    fun canonicalize(definition: Int, target: Int, b: CanonBuffer, profile: ValidationProfile) {
        when (definition) {
            0 -> canon_49(b, profile, target)
            1 -> canon_4(b, profile, target)
            2 -> canon_51(b, profile, target)
            3 -> canon_45(b, profile, target)
            4 -> canon_52(b, profile, target)
            5 -> canon_50(b, profile, target)
            6 -> canon_61(b, profile, target)
            7 -> canon_11(b, profile, target)
            8 -> canon_46(b, profile, target)
            9 -> canon_3(b, profile, target)
            10 -> canon_65(b, profile, target)
            11 -> canon_2(b, profile, target)
            12 -> canon_47(b, profile, target)
            13 -> canon_15(b, profile, target)
            14 -> canon_16(b, profile, target)
            15 -> canon_17(b, profile, target)
            16 -> canon_18(b, profile, target)
            17 -> canon_19(b, profile, target)
            18 -> canon_20(b, profile, target)
            19 -> canon_21(b, profile, target)
            20 -> canon_22(b, profile, target)
            21 -> canon_23(b, profile, target)
            22 -> canon_24(b, profile, target)
            23 -> canon_25(b, profile, target)
            24 -> canon_26(b, profile, target)
            25 -> canon_27(b, profile, target)
            26 -> canon_28(b, profile, target)
            27 -> canon_29(b, profile, target)
            28 -> canon_30(b, profile, target)
            29 -> canon_31(b, profile, target)
            30 -> canon_32(b, profile, target)
            31 -> canon_33(b, profile, target)
            32 -> canon_34(b, profile, target)
            33 -> canon_35(b, profile, target)
            34 -> canon_36(b, profile, target)
            35 -> canon_37(b, profile, target)
            36 -> canon_38(b, profile, target)
            37 -> canon_39(b, profile, target)
            38 -> canon_40(b, profile, target)
            39 -> canon_41(b, profile, target)
            40 -> canon_1(b, profile, target)
            41 -> canon_13(b, profile, target)
            42 -> canon_8(b, profile, target)
            43 -> canon_6(b, profile, target)
            44 -> canon_7(b, profile, target)
            45 -> canon_64(b, profile, target)
            46 -> canon_54(b, profile, target)
            47 -> canon_59(b, profile, target)
            48 -> canon_58(b, profile, target)
            49 -> canon_53(b, profile, target)
            50 -> canon_63(b, profile, target)
            51 -> canon_57(b, profile, target)
            52 -> canon_48(b, profile, target)
            53 -> canon_14(b, profile, target)
            54 -> canon_60(b, profile, target)
            55 -> canon_62(b, profile, target)
            56 -> canon_55(b, profile, target)
            57 -> canon_56(b, profile, target)
            58 -> canon_12(b, profile, target)
            59 -> canon_43(b, profile, target)
            60 -> canon_44(b, profile, target)
            61 -> canon_5(b, profile, target)
            62 -> canon_66(b, profile, target)
            63 -> canon_67(b, profile, target)
            64 -> canon_68(b, profile, target)
            65 -> canon_69(b, profile, target)
            66 -> canon_70(b, profile, target)
            67 -> canon_71(b, profile, target)
            68 -> canon_72(b, profile, target)
            69 -> canon_73(b, profile, target)
            70 -> canon_74(b, profile, target)
            71 -> canon_75(b, profile, target)
            72 -> canon_76(b, profile, target)
            73 -> canon_77(b, profile, target)
            74 -> canon_78(b, profile, target)
            75 -> canon_79(b, profile, target)
            76 -> canon_80(b, profile, target)
            77 -> canon_81(b, profile, target)
            78 -> canon_82(b, profile, target)
            79 -> canon_83(b, profile, target)
            80 -> canon_84(b, profile, target)
            81 -> canon_85(b, profile, target)
            82 -> canon_86(b, profile, target)
            83 -> canon_87(b, profile, target)
            84 -> canon_88(b, profile, target)
            85 -> canon_89(b, profile, target)
            86 -> canon_90(b, profile, target)
            87 -> canon_91(b, profile, target)
            88 -> canon_92(b, profile, target)
            89 -> canon_93(b, profile, target)
            90 -> canon_94(b, profile, target)
            91 -> canon_95(b, profile, target)
            92 -> canon_96(b, profile, target)
            93 -> canon_42(b, profile, target)
            else -> Unit
        }
    }

    /** The format program of a definition; null means every assertion held. */
    fun format(definition: Int, ctx: EvalContext): AssertionFailure? =
        when (definition) {
            0 -> fmt_203(ctx.value, ctx)
            1 -> fmt_160(ctx.value, ctx)
            2 -> fmt_205(ctx.value, ctx)
            3 -> fmt_199(ctx.value, ctx)
            4 -> fmt_206(ctx.value, ctx)
            5 -> fmt_204(ctx.value, ctx)
            6 -> fmt_215(ctx.value, ctx)
            7 -> fmt_165(ctx.value, ctx)
            8 -> fmt_200(ctx.value, ctx)
            9 -> fmt_159(ctx.value, ctx)
            10 -> fmt_219(ctx.value, ctx)
            11 -> fmt_158(ctx.value, ctx)
            12 -> fmt_201(ctx.value, ctx)
            13 -> fmt_169(ctx.value, ctx)
            14 -> fmt_170(ctx.value, ctx)
            15 -> fmt_171(ctx.value, ctx)
            16 -> fmt_172(ctx.value, ctx)
            17 -> fmt_173(ctx.value, ctx)
            18 -> fmt_174(ctx.value, ctx)
            19 -> fmt_175(ctx.value, ctx)
            20 -> fmt_176(ctx.value, ctx)
            21 -> fmt_177(ctx.value, ctx)
            22 -> fmt_178(ctx.value, ctx)
            23 -> fmt_179(ctx.value, ctx)
            24 -> fmt_180(ctx.value, ctx)
            25 -> fmt_181(ctx.value, ctx)
            26 -> fmt_182(ctx.value, ctx)
            27 -> fmt_183(ctx.value, ctx)
            28 -> fmt_184(ctx.value, ctx)
            29 -> fmt_185(ctx.value, ctx)
            30 -> fmt_186(ctx.value, ctx)
            31 -> fmt_187(ctx.value, ctx)
            32 -> fmt_188(ctx.value, ctx)
            33 -> fmt_189(ctx.value, ctx)
            34 -> fmt_190(ctx.value, ctx)
            35 -> fmt_191(ctx.value, ctx)
            36 -> fmt_192(ctx.value, ctx)
            37 -> fmt_193(ctx.value, ctx)
            38 -> fmt_194(ctx.value, ctx)
            39 -> fmt_195(ctx.value, ctx)
            40 -> fmt_157(ctx.value, ctx)
            41 -> fmt_167(ctx.value, ctx)
            42 -> fmt_164(ctx.value, ctx)
            43 -> fmt_162(ctx.value, ctx)
            44 -> fmt_163(ctx.value, ctx)
            45 -> fmt_218(ctx.value, ctx)
            46 -> fmt_208(ctx.value, ctx)
            47 -> fmt_213(ctx.value, ctx)
            48 -> fmt_212(ctx.value, ctx)
            49 -> fmt_207(ctx.value, ctx)
            50 -> fmt_217(ctx.value, ctx)
            51 -> fmt_211(ctx.value, ctx)
            52 -> fmt_202(ctx.value, ctx)
            53 -> fmt_168(ctx.value, ctx)
            54 -> fmt_214(ctx.value, ctx)
            55 -> fmt_216(ctx.value, ctx)
            56 -> fmt_209(ctx.value, ctx)
            57 -> fmt_210(ctx.value, ctx)
            58 -> fmt_166(ctx.value, ctx)
            59 -> fmt_197(ctx.value, ctx)
            60 -> fmt_198(ctx.value, ctx)
            61 -> fmt_161(ctx.value, ctx)
            62 -> fmt_220(ctx.value, ctx)
            63 -> fmt_221(ctx.value, ctx)
            64 -> fmt_222(ctx.value, ctx)
            65 -> fmt_223(ctx.value, ctx)
            66 -> fmt_224(ctx.value, ctx)
            67 -> fmt_225(ctx.value, ctx)
            68 -> fmt_226(ctx.value, ctx)
            69 -> fmt_227(ctx.value, ctx)
            70 -> fmt_228(ctx.value, ctx)
            71 -> fmt_229(ctx.value, ctx)
            72 -> fmt_230(ctx.value, ctx)
            73 -> fmt_231(ctx.value, ctx)
            74 -> fmt_232(ctx.value, ctx)
            75 -> fmt_233(ctx.value, ctx)
            76 -> fmt_234(ctx.value, ctx)
            77 -> fmt_235(ctx.value, ctx)
            78 -> fmt_236(ctx.value, ctx)
            79 -> fmt_237(ctx.value, ctx)
            80 -> fmt_238(ctx.value, ctx)
            81 -> fmt_239(ctx.value, ctx)
            82 -> fmt_240(ctx.value, ctx)
            83 -> fmt_241(ctx.value, ctx)
            84 -> fmt_242(ctx.value, ctx)
            85 -> fmt_243(ctx.value, ctx)
            86 -> fmt_244(ctx.value, ctx)
            87 -> fmt_245(ctx.value, ctx)
            88 -> fmt_246(ctx.value, ctx)
            89 -> fmt_247(ctx.value, ctx)
            90 -> fmt_248(ctx.value, ctx)
            91 -> fmt_249(ctx.value, ctx)
            92 -> fmt_250(ctx.value, ctx)
            93 -> fmt_196(ctx.value, ctx)
            else -> null
        }

    /**
     * The checksum program of a definition, or null when it declares none. A
     * null answer is not a failure: the definition states why no algorithm
     * applies, and [absentChecksumReason] carries that reason.
     */
    fun checksum(definition: Int, ctx: EvalContext): ChecksumOutcome? =
        when (definition) {
            1 -> ck_99(ctx.value, ctx)
            2 -> ck_123(ctx.value, ctx)
            4 -> ck_124(ctx.value, ctx)
            6 -> ck_129(ctx.value, ctx)
            7 -> ck_102(ctx.value, ctx)
            9 -> ck_98(ctx.value, ctx)
            11 -> ck_97(ctx.value, ctx)
            14 -> ck_105(ctx.value, ctx)
            15 -> ck_106(ctx.value, ctx)
            17 -> ck_107(ctx.value, ctx)
            19 -> ck_108(ctx.value, ctx)
            20 -> ck_109(ctx.value, ctx)
            22 -> ck_110(ctx.value, ctx)
            23 -> ck_111(ctx.value, ctx)
            24 -> ck_112(ctx.value, ctx)
            28 -> ck_113(ctx.value, ctx)
            29 -> ck_114(ctx.value, ctx)
            31 -> ck_115(ctx.value, ctx)
            35 -> ck_116(ctx.value, ctx)
            36 -> ck_117(ctx.value, ctx)
            37 -> ck_118(ctx.value, ctx)
            39 -> ck_119(ctx.value, ctx)
            44 -> ck_101(ctx.value, ctx)
            45 -> ck_131(ctx.value, ctx)
            46 -> ck_126(ctx.value, ctx)
            49 -> ck_125(ctx.value, ctx)
            53 -> ck_104(ctx.value, ctx)
            54 -> ck_128(ctx.value, ctx)
            55 -> ck_130(ctx.value, ctx)
            57 -> ck_127(ctx.value, ctx)
            58 -> ck_103(ctx.value, ctx)
            59 -> ck_121(ctx.value, ctx)
            60 -> ck_122(ctx.value, ctx)
            61 -> ck_100(ctx.value, ctx)
            63 -> ck_132(ctx.value, ctx)
            64 -> ck_133(ctx.value, ctx)
            66 -> ck_134(ctx.value, ctx)
            68 -> ck_135(ctx.value, ctx)
            69 -> ck_136(ctx.value, ctx)
            70 -> ck_137(ctx.value, ctx)
            71 -> ck_138(ctx.value, ctx)
            72 -> ck_139(ctx.value, ctx)
            73 -> ck_140(ctx.value, ctx)
            74 -> ck_141(ctx.value, ctx)
            76 -> ck_142(ctx.value, ctx)
            78 -> ck_143(ctx.value, ctx)
            79 -> ck_144(ctx.value, ctx)
            81 -> ck_145(ctx.value, ctx)
            82 -> ck_146(ctx.value, ctx)
            83 -> ck_147(ctx.value, ctx)
            84 -> ck_148(ctx.value, ctx)
            85 -> ck_149(ctx.value, ctx)
            86 -> ck_150(ctx.value, ctx)
            87 -> ck_151(ctx.value, ctx)
            88 -> ck_152(ctx.value, ctx)
            89 -> ck_153(ctx.value, ctx)
            90 -> ck_154(ctx.value, ctx)
            91 -> ck_155(ctx.value, ctx)
            92 -> ck_156(ctx.value, ctx)
            93 -> ck_120(ctx.value, ctx)
            else -> null
        }

    /** Why a definition without a checksum program publishes no algorithm. */
    fun absentChecksumReason(definition: Int): ReasonCode =
        when (definition) {
            0 -> ReasonCode.CHECKSUM_NOT_PUBLISHED
            3 -> ReasonCode.CHECKSUM_NOT_PUBLISHED
            5 -> ReasonCode.CHECKSUM_NOT_PUBLISHED
            8 -> ReasonCode.CHECKSUM_NOT_PUBLISHED
            10 -> ReasonCode.CHECKSUM_NOT_PUBLISHED
            12 -> ReasonCode.CHECKSUM_NOT_PUBLISHED
            13 -> ReasonCode.CHECKSUM_NOT_PUBLISHED
            16 -> ReasonCode.CHECKSUM_NOT_PUBLISHED
            18 -> ReasonCode.CHECKSUM_NOT_PUBLISHED
            21 -> ReasonCode.CHECKSUM_NOT_PUBLISHED
            25 -> ReasonCode.CHECKSUM_NOT_PUBLISHED
            26 -> ReasonCode.CHECKSUM_NOT_PUBLISHED
            27 -> ReasonCode.CHECKSUM_NOT_PUBLISHED
            30 -> ReasonCode.CHECKSUM_NOT_PUBLISHED
            32 -> ReasonCode.CHECKSUM_NOT_PUBLISHED
            33 -> ReasonCode.CHECKSUM_NOT_PUBLISHED
            34 -> ReasonCode.CHECKSUM_NOT_PUBLISHED
            38 -> ReasonCode.CHECKSUM_NOT_PUBLISHED
            40 -> ReasonCode.CHECKSUM_NOT_PUBLISHED
            41 -> ReasonCode.CHECKSUM_NOT_PUBLISHED
            42 -> ReasonCode.CHECKSUM_NOT_PUBLISHED
            43 -> ReasonCode.CHECKSUM_NOT_PUBLISHED
            47 -> ReasonCode.CHECKSUM_NOT_PUBLISHED
            48 -> ReasonCode.CHECKSUM_NOT_PUBLISHED
            50 -> ReasonCode.CHECKSUM_NOT_PUBLISHED
            51 -> ReasonCode.CHECKSUM_NOT_PUBLISHED
            52 -> ReasonCode.CHECKSUM_NOT_PUBLISHED
            56 -> ReasonCode.CHECKSUM_NOT_PUBLISHED
            62 -> ReasonCode.UNSUPPORTED_CHECKSUM
            65 -> ReasonCode.UNSUPPORTED_CHECKSUM
            67 -> ReasonCode.UNSUPPORTED_CHECKSUM
            75 -> ReasonCode.UNSUPPORTED_CHECKSUM
            77 -> ReasonCode.UNSUPPORTED_CHECKSUM
            80 -> ReasonCode.CHECKSUM_NOT_PUBLISHED
            else -> ReasonCode.UNSUPPORTED_CHECKSUM
        }
}
