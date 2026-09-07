package com.loanzo.app.domain

import javax.inject.Inject
import javax.inject.Singleton

/**
 * CasualLendingGuard
 *
 * Enforces Indian statutory safeguards under:
 * 1. State Money Lenders Acts (e.g. Maharashtra, Punjab, Karnataka, Tamil Nadu)
 * 2. Supreme Court Doctrine in G. Pankajakshi Amma v. Mathai Mathew (2004) 12 SCC 83
 *    - Establishes that isolated/casual lending between acquaintances does not constitute
 *      carrying on the commercial business of money lending.
 * 3. State Usury Ceilings (prohibiting unconscionable interest rates)
 */
@Singleton
class CasualLendingGuard @Inject constructor() {

    data class StateUsuryCap(
        val stateName: String,
        val maxSimpleInterestRate: Double,
        val statuteName: String
    )

    data class CasualLendingValidationResult(
        val isAllowed: Boolean,
        val maxAllowedRate: Double,
        val violationReason: String? = null,
        val statutoryDeclaration: String
    )

    companion object {
        const val MAX_CONCURRENT_CASUAL_LOANS = 5
        const val NATIONAL_DEFAULT_MAX_RATE = 18.0

        private val STATE_USURY_MAP = mapOf(
            "MAHARASHTRA" to StateUsuryCap("Maharashtra", 12.0, "Maharashtra Money Lending (Regulation) Act, 2014"),
            "PUNJAB" to StateUsuryCap("Punjab", 12.0, "Punjab Registration of Money Lenders Act, 1938"),
            "HARYANA" to StateUsuryCap("Haryana", 12.0, "Punjab Registration of Money Lenders Act, 1938"),
            "TAMIL NADU" to StateUsuryCap("Tamil Nadu", 12.0, "Tamil Nadu Prohibition of Charging Exorbitant Interest Act, 2003"),
            "KARNATAKA" to StateUsuryCap("Karnataka", 15.0, "Karnataka Money-Lenders Act, 1961"),
            "GUJARAT" to StateUsuryCap("Gujarat", 15.0, "Gujarat Money-Lenders Act, 2011"),
            "DELHI" to StateUsuryCap("Delhi", 18.0, "Usurious Loans Act, 1918"),
            "UTTAR PRADESH" to StateUsuryCap("Uttar Pradesh", 14.0, "UP Regulation of Money-Lending Act, 1976"),
            "WEST BENGAL" to StateUsuryCap("West Bengal", 15.0, "Bengal Money-Lenders Act, 1940"),
            "TELANGANA" to StateUsuryCap("Telangana", 15.0, "Telangana Money Lenders Act, 1349 F"),
            "ANDHRA PRADESH" to StateUsuryCap("Andhra Pradesh", 15.0, "Andhra Pradesh (Telangana Area) Money Lenders Act")
        )
    }

    /**
     * Resolves the statutory usury cap for a given state.
     */
    fun getStateUsuryCap(state: String): StateUsuryCap {
        val normalized = state.trim().uppercase()
        return STATE_USURY_MAP[normalized] ?: StateUsuryCap(
            stateName = state.ifBlank { "National" },
            maxSimpleInterestRate = NATIONAL_DEFAULT_MAX_RATE,
            statuteName = "Usurious Loans Act, 1918 (General Ceilings)"
        )
    }

    /**
     * Validates if the proposed loan terms conform to casual lending safe harbor rules
     * or valid institutional money lending credentials.
     */
    fun validateLoanTerms(
        activeLoansCount: Int,
        proposedInterestRate: Double,
        lenderState: String,
        isInstitutionalLender: Boolean,
        licenseNumber: String? = null,
        gstin: String? = null
    ): CasualLendingValidationResult {
        val stateCap = getStateUsuryCap(lenderState)

        // Path A: Institutional / Licensed NBFC or Registered Money Lender
        if (isInstitutionalLender) {
            val cleanLicense = licenseNumber?.trim() ?: ""
            if (cleanLicense.isBlank()) {
                return CasualLendingValidationResult(
                    isAllowed = false,
                    maxAllowedRate = stateCap.maxSimpleInterestRate,
                    violationReason = "Institutional lending requires an active State Money Lender's License Number or RBI NBFC CoR.",
                    statutoryDeclaration = ""
                )
            }
            return CasualLendingValidationResult(
                isAllowed = true,
                maxAllowedRate = 36.0, // Regulated institutional flexibility under RBI DLG
                statutoryDeclaration = "Executed by licensed commercial entity (Reg: $cleanLicense, GSTIN: ${gstin ?: "N/A"}) under ${stateCap.statuteName}."
            )
        }

        // Path B: Casual / Personal Peer Lending (Under G. Pankajakshi Amma Safe Harbor)
        if (activeLoansCount >= MAX_CONCURRENT_CASUAL_LOANS) {
            return CasualLendingValidationResult(
                isAllowed = false,
                maxAllowedRate = stateCap.maxSimpleInterestRate,
                violationReason = "Casual lending limit reached ($MAX_CONCURRENT_CASUAL_LOANS active loans/yr). To lend further, register as a licensed institutional lender under ${stateCap.statuteName}.",
                statutoryDeclaration = ""
            )
        }

        if (proposedInterestRate > stateCap.maxSimpleInterestRate) {
            return CasualLendingValidationResult(
                isAllowed = false,
                maxAllowedRate = stateCap.maxSimpleInterestRate,
                violationReason = "Interest rate (${proposedInterestRate}%) exceeds the statutory ceiling (${stateCap.maxSimpleInterestRate}%) for ${stateCap.stateName} under ${stateCap.statuteName}.",
                statutoryDeclaration = ""
            )
        }

        val declaration = "STATUTORY DECLARATION: This transaction represents casual, bilateral financial assistance between acquaintances. As held in G. Pankajakshi Amma v. Mathai Mathew (2004) 12 SCC 83, this does not constitute carrying on the commercial business of money lending under ${stateCap.statuteName}."

        return CasualLendingValidationResult(
            isAllowed = true,
            maxAllowedRate = stateCap.maxSimpleInterestRate,
            violationReason = null,
            statutoryDeclaration = declaration
        )
    }
}
