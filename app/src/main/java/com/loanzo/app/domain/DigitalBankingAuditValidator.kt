package com.loanzo.app.domain

import javax.inject.Inject
import javax.inject.Singleton

/**
 * DigitalBankingAuditValidator
 *
 * Enforces compliance with Income Tax Act, 1961:
 * - Section 269SS: Prohibits taking/accepting loans of ₹20,000 or more in cash.
 * - Section 269T: Prohibits repaying loans of ₹20,000 or more in cash.
 * - Sections 271D & 271E: Imposes a 100% tax penalty on cash transactions violating 269SS/T.
 *
 * Mandates electronic Account-to-Account (A2A) settlement with verified UTR trails.
 */
@Singleton
class DigitalBankingAuditValidator @Inject constructor() {

    companion object {
        const val CASH_STATUTORY_LIMIT = 20000.0 // ₹20,000 limit under Section 269SS/T
        private val UTR_REGEX = Regex("^[0-9]{12}$|^[A-Za-z0-9]{12,22}$")
    }

    data class TaxAuditResult(
        val isCompliant: Boolean,
        val isCashBlocked: Boolean,
        val penaltyRiskAmount: Double = 0.0,
        val advisoryMessage: String,
        val statutoryCitation: String
    )

    /**
     * Determines whether cash transactions are strictly barred under Section 269SS/T.
     */
    fun isCashBarred(amount: Double): Boolean {
        return amount >= CASH_STATUTORY_LIMIT
    }

    /**
     * Validates whether a 12-digit or alphanumeric banking UTR is structurally valid.
     */
    fun isValidUtr(utr: String?): Boolean {
        if (utr.isNullOrBlank()) return false
        return UTR_REGEX.matches(utr.trim())
    }

    /**
     * Audits a proposed disbursement or repayment transaction against Income Tax Act statutes.
     */
    fun auditTransaction(
        amount: Double,
        isCash: Boolean,
        utrNumber: String? = null
    ): TaxAuditResult {
        if (isCash) {
            if (amount >= CASH_STATUTORY_LIMIT) {
                return TaxAuditResult(
                    isCompliant = false,
                    isCashBlocked = true,
                    penaltyRiskAmount = amount,
                    advisoryMessage = "Cash transactions of ₹${amount.toLong()} violate Section 269SS/269T of the Income Tax Act. A 100% penalty (₹${amount.toLong()}) will be levied under Section 271D/271E. Disbursement must execute via digital bank transfer.",
                    statutoryCitation = "Income Tax Act 1961, Sections 269SS, 269T, 271D & 271E"
                )
            } else {
                return TaxAuditResult(
                    isCompliant = true,
                    isCashBlocked = false,
                    penaltyRiskAmount = 0.0,
                    advisoryMessage = "Cash transaction is below the ₹20,000 threshold. However, digital banking via UPI is strongly recommended for electronic court admissibility under Section 63 BSA.",
                    statutoryCitation = "Income Tax Act 1961, Section 269SS Safe Harbor (< ₹20,000)"
                )
            }
        }

        // Digital Banking Path (UPI / NEFT / IMPS)
        val cleanUtr = utrNumber?.trim() ?: ""
        if (!isValidUtr(cleanUtr)) {
            return TaxAuditResult(
                isCompliant = false,
                isCashBlocked = false,
                penaltyRiskAmount = 0.0,
                advisoryMessage = "Digital banking requires a verified 12-digit bank UTR (Unique Transaction Reference) for audit trail compliance.",
                statutoryCitation = "RBI Digital Lending Guidelines (2022) & BSA 2023 §63"
            )
        }

        return TaxAuditResult(
            isCompliant = true,
            isCashBlocked = false,
            penaltyRiskAmount = 0.0,
            advisoryMessage = "Transaction fully compliant with digital banking rails. Immutable UTR trail recorded.",
            statutoryCitation = "Income Tax Act 1961 & RBI Direct Account-to-Account Mandate"
        )
    }
}
