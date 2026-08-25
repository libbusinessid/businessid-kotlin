// Copyright The EntID Authors.
// SPDX-License-Identifier: Apache-2.0

package org.entid

/**
 * The family of identifier a value is claimed to belong to, such as `vat` or `siren`.
 *
 * This is an open string type rather than a closed enumeration on purpose: a
 * ruleset published after this release may carry a kind this build has never
 * heard of, and the engine has to be able to report [ReasonCode.UNSUPPORTED_KIND]
 * for it rather than fail to represent it at all. Constructing an arbitrary
 * value is legal and well defined.
 *
 * The constants on the companion are exactly the kinds the compiled ruleset
 * declares; a test compares them against the generated ruleset so the two
 * cannot drift apart.
 *
 * @property value the raw kind token, exactly as supplied.
 */
@JvmInline
public value class IdentifierKind(public val value: String) {
    override fun toString(): String = value

    /** The kind tokens the compiled ruleset declares. */
    public companion object {
        /**
         * The `cegjegyzekszam` kind, defined for HU.
         *
         * Accepted aliases: `hu_cegjegyzekszam`.
         */
        public val CEGJEGYZEKSZAM: IdentifierKind = IdentifierKind("cegjegyzekszam")

        /**
         * The `cnpj` kind, defined for BR.
         *
         * Accepted aliases: `br_cnpj`.
         */
        public val CNPJ: IdentifierKind = IdentifierKind("cnpj")

        /**
         * The `codice_fiscale_impresa` kind, defined for IT.
         *
         * Accepted aliases: `it_codice_fiscale_impresa`.
         */
        public val CODICE_FISCALE_IMPRESA: IdentifierKind =
            IdentifierKind("codice_fiscale_impresa")

        /**
         * The `company_number` kind, defined for GB.
         *
         * Accepted aliases: `company_registration_number`, `crn`, `gb_company_number`, `uk_company_number`.
         */
        public val COMPANY_NUMBER: IdentifierKind = IdentifierKind("company_number")

        /**
         * The `corporate_number` kind, defined for JP.
         *
         * Accepted aliases: `hojin_bango`, `houjin_bangou`, `jp_corporate_number`.
         */
        public val CORPORATE_NUMBER: IdentifierKind = IdentifierKind("corporate_number")

        /**
         * The `cro_number` kind, defined for IE.
         *
         * Accepted aliases: `ie_cro_number`.
         */
        public val CRO_NUMBER: IdentifierKind = IdentifierKind("cro_number")

        /**
         * The `cui` kind, defined for RO.
         *
         * Accepted aliases: `cif`, `ro_cui`.
         */
        public val CUI: IdentifierKind = IdentifierKind("cui")

        /**
         * The `cvr` kind, defined for DK.
         *
         * Accepted aliases: `cvr_nummer`, `dk_cvr`.
         */
        public val CVR: IdentifierKind = IdentifierKind("cvr")

        /**
         * The `duns` kind, defined for GLOBAL.
         *
         * Accepted aliases: `dnb`, `duns_number`.
         */
        public val DUNS: IdentifierKind = IdentifierKind("duns")

        /**
         * The `eik` kind, defined for BG.
         *
         * Accepted aliases: `bg_eik`, `bulstat`.
         */
        public val EIK: IdentifierKind = IdentifierKind("eik")

        /**
         * The `ein` kind, defined for US.
         *
         * Accepted aliases: `federal_tax_id`, `us_ein`.
         */
        public val EIN: IdentifierKind = IdentifierKind("ein")

        /**
         * The `enterprise_number` kind, defined for BE.
         *
         * Accepted aliases: `be_enterprise_number`, `numero_entreprise`, `ondernemingsnummer`.
         */
        public val ENTERPRISE_NUMBER: IdentifierKind = IdentifierKind("enterprise_number")

        /**
         * The `eori` kind, defined for GLOBAL.
         *
         * Accepted aliases: `eori_number`.
         */
        public val EORI: IdentifierKind = IdentifierKind("eori")

        /**
         * The `euid` kind, defined for AT, BE, BG, CY, CZ, DE, DK, EE, EL, ES, FI, FR, HR, HU, IE, IT, LT, LU, LV, MT,
         * NL, PL, PT, RO, SE, SI, SK.
         */
        public val EUID: IdentifierKind = IdentifierKind("euid")

        /**
         * The `firmenbuchnummer` kind, defined for AT.
         *
         * Accepted aliases: `at_firmenbuchnummer`, `fn`.
         */
        public val FIRMENBUCHNUMMER: IdentifierKind = IdentifierKind("firmenbuchnummer")

        /**
         * The `gemi` kind, defined for EL.
         *
         * Accepted aliases: `el_gemi`, `gr_gemi`.
         */
        public val GEMI: IdentifierKind = IdentifierKind("gemi")

        /**
         * The `handelsregisternummer` kind, defined for DE.
         *
         * Accepted aliases: `de_handelsregisternummer`, `hrb`.
         */
        public val HANDELSREGISTERNUMMER: IdentifierKind = IdentifierKind("handelsregisternummer")

        /**
         * The `he_number` kind, defined for CY.
         *
         * Accepted aliases: `cy_he_number`.
         */
        public val HE_NUMBER: IdentifierKind = IdentifierKind("he_number")

        /**
         * The `ico` kind, defined for CZ, SK.
         *
         * Accepted aliases: `cz_ico`, `sk_ico`.
         */
        public val ICO: IdentifierKind = IdentifierKind("ico")

        /**
         * The `juridinio_asmens_kodas` kind, defined for LT.
         *
         * Accepted aliases: `lt_juridinio_asmens_kodas`.
         */
        public val JURIDINIO_ASMENS_KODAS: IdentifierKind =
            IdentifierKind("juridinio_asmens_kodas")

        /**
         * The `krs` kind, defined for PL.
         *
         * Accepted aliases: `pl_krs`.
         */
        public val KRS: IdentifierKind = IdentifierKind("krs")

        /**
         * The `kvk` kind, defined for NL.
         *
         * Accepted aliases: `kvk_nummer`, `nl_kvk`.
         */
        public val KVK: IdentifierKind = IdentifierKind("kvk")

        /**
         * The `lei` kind, defined for GLOBAL.
         */
        public val LEI: IdentifierKind = IdentifierKind("lei")

        /**
         * The `maticna_stevilka` kind, defined for SI.
         *
         * Accepted aliases: `si_maticna_stevilka`.
         */
        public val MATICNA_STEVILKA: IdentifierKind = IdentifierKind("maticna_stevilka")

        /**
         * The `mbr_number` kind, defined for MT.
         *
         * Accepted aliases: `mt_mbr_number`.
         */
        public val MBR_NUMBER: IdentifierKind = IdentifierKind("mbr_number")

        /**
         * The `mbs` kind, defined for HR.
         *
         * Accepted aliases: `hr_mbs`.
         */
        public val MBS: IdentifierKind = IdentifierKind("mbs")

        /**
         * The `nif` kind, defined for ES.
         *
         * Accepted aliases: `es_nif`.
         */
        public val NIF: IdentifierKind = IdentifierKind("nif")

        /**
         * The `nipc` kind, defined for PT.
         *
         * Accepted aliases: `pt_nipc`.
         */
        public val NIPC: IdentifierKind = IdentifierKind("nipc")

        /**
         * The `organisationsnummer` kind, defined for SE.
         *
         * Accepted aliases: `se_organisationsnummer`.
         */
        public val ORGANISATIONSNUMMER: IdentifierKind = IdentifierKind("organisationsnummer")

        /**
         * The `rcs_number` kind, defined for LU.
         *
         * Accepted aliases: `lu_rcs_number`.
         */
        public val RCS_NUMBER: IdentifierKind = IdentifierKind("rcs_number")

        /**
         * The `registracijas_numurs` kind, defined for LV.
         *
         * Accepted aliases: `lv_registracijas_numurs`.
         */
        public val REGISTRACIJAS_NUMURS: IdentifierKind = IdentifierKind("registracijas_numurs")

        /**
         * The `registrikood` kind, defined for EE.
         *
         * Accepted aliases: `ee_registrikood`.
         */
        public val REGISTRIKOOD: IdentifierKind = IdentifierKind("registrikood")

        /**
         * The `siren` kind, defined for FR.
         *
         * Accepted aliases: `fr_siren`.
         */
        public val SIREN: IdentifierKind = IdentifierKind("siren")

        /**
         * The `siret` kind, defined for FR.
         *
         * Accepted aliases: `fr_siret`.
         */
        public val SIRET: IdentifierKind = IdentifierKind("siret")

        /**
         * The `uscc` kind, defined for CN.
         *
         * Accepted aliases: `cn_uscc`, `unified_social_credit_code`, `usci`.
         */
        public val USCC: IdentifierKind = IdentifierKind("uscc")

        /**
         * The `vat` kind, defined for AT, BE, BG, CY, CZ, DE, DK, EE, ES, FI, FR, GB, GR, HR, HU, IE, IS, IT, LI, LU,
         * LV, MT, NL, NO, PL, PT, RO, SE, SI, SK, XI.
         *
         * Accepted aliases: `vat_id`, `vat_number`.
         */
        public val VAT: IdentifierKind = IdentifierKind("vat")

        /**
         * The `y_tunnus` kind, defined for FI.
         *
         * Accepted aliases: `business_id`, `fi_y_tunnus`.
         */
        public val Y_TUNNUS: IdentifierKind = IdentifierKind("y_tunnus")
    }
}
