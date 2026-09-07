package com.loanzo.app.domain

import com.loanzo.app.domain.model.PurposeCategory
import com.loanzo.app.domain.model.RuleResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loanzo Rule Engine
 *
 * Implements transparent, rule-based checks and statutory compliance validations:
 * - Amount limit validation
 * - Payee verification check
 * - Purpose-payee category consistency
 * - Auto-approval threshold logic
 * - Pattern-based review triggers
 * - Statutory Usury & State Money Lenders Acts (via CasualLendingGuard)
 * - Income Tax Act Sections 269SS & 269T cash bars (via DigitalBankingAuditValidator)
 * - RBI Circular RBI/2023-24/53 banning compound penal interest
 */
@Singleton
class RuleEngine @Inject constructor(
    val casualLendingGuard: CasualLendingGuard,
    val digitalBankingAuditValidator: DigitalBankingAuditValidator
) {

    companion object {
        /** Default auto-approval threshold in INR */
        const val DEFAULT_AUTO_APPROVAL_THRESHOLD = 5000.0

        /** Purpose-to-payee category consistency mapping */
        private val CATEGORY_CONSISTENCY_MAP: Map<PurposeCategory, Set<PurposeCategory>> = mapOf(
            PurposeCategory.HOSPITAL to setOf(
                PurposeCategory.HOSPITAL, PurposeCategory.PHARMACY
            ),
            PurposeCategory.PHARMACY to setOf(
                PurposeCategory.PHARMACY, PurposeCategory.HOSPITAL
            ),
            PurposeCategory.EDUCATION to setOf(
                PurposeCategory.EDUCATION
            ),
            PurposeCategory.ELECTRONICS to setOf(
                PurposeCategory.ELECTRONICS, PurposeCategory.BUSINESS
            ),
            PurposeCategory.GROCERY to setOf(
                PurposeCategory.GROCERY
            ),
            PurposeCategory.RENT to setOf(
                PurposeCategory.RENT, PurposeCategory.CONSTRUCTION
            ),
            PurposeCategory.UTILITY to setOf(
                PurposeCategory.UTILITY
            ),
            PurposeCategory.CONSTRUCTION to setOf(
                PurposeCategory.CONSTRUCTION, PurposeCategory.RENT
            ),
            PurposeCategory.AGRICULTURE to setOf(
                PurposeCategory.AGRICULTURE
            ),
            PurposeCategory.TRANSPORT to setOf(
                PurposeCategory.TRANSPORT
            ),
            PurposeCategory.BUSINESS to setOf(
                PurposeCategory.BUSINESS, PurposeCategory.ELECTRONICS,
                PurposeCategory.TRANSPORT, PurposeCategory.CONSTRUCTION
            ),
            PurposeCategory.OTHER to PurposeCategory.entries.toSet()
        )
    }

    /**
     * Evaluate a tranche request against the rule engine.
     */
    fun evaluate(
        requestedAmount: Double,
        remainingLimit: Double,
        isPayeeVerified: Boolean,
        purposeCategory: PurposeCategory,
        payeeCategory: PurposeCategory?,
        autoApprovalThreshold: Double = DEFAULT_AUTO_APPROVAL_THRESHOLD,
        previousDisbursementCount: Int = 0,
        hasPriorMismatches: Boolean = false
    ): RuleEvaluation {
        val checks = mutableListOf<RuleCheck>()

        // Rule 1: Amount limit check
        val amountCheck = if (requestedAmount > remainingLimit) {
            RuleCheck(
                name = "Amount Limit",
                passed = false,
                message = "Requested ₹${formatAmount(requestedAmount)} exceeds remaining limit of ₹${formatAmount(remainingLimit)}",
                severity = RuleSeverity.HARD_BLOCK
            )
        } else {
            RuleCheck(
                name = "Amount Limit",
                passed = true,
                message = "Within sanctioned limit (₹${formatAmount(remainingLimit)} remaining)",
                severity = RuleSeverity.INFO
            )
        }
        checks.add(amountCheck)

        // Hard block -> return immediately
        if (!amountCheck.passed) {
            return RuleEvaluation(
                result = RuleResult.BLOCKED,
                checks = checks,
                requiresLenderApproval = false,
                canAutoApprove = false
            )
        }

        // Rule 2: Payee verification check
        val payeeCheck = if (isPayeeVerified) {
            RuleCheck(
                name = "Payee Verification",
                passed = true,
                message = "Payee is verified",
                severity = RuleSeverity.INFO
            )
        } else {
            RuleCheck(
                name = "Payee Verification",
                passed = false,
                message = "Payee could not be verified — requires lender review",
                severity = RuleSeverity.WARNING
            )
        }
        checks.add(payeeCheck)

        // Rule 3: Purpose-payee category consistency
        val categoryConsistent = if (payeeCategory != null) {
            val allowedCategories = CATEGORY_CONSISTENCY_MAP[purposeCategory] ?: emptySet()
            allowedCategories.contains(payeeCategory)
        } else {
            false
        }

        val categoryCheck = if (isPayeeVerified && categoryConsistent) {
            RuleCheck(
                name = "Purpose Consistency",
                passed = true,
                message = "${purposeCategory.displayName} + ${payeeCategory?.displayName} = Consistent",
                severity = RuleSeverity.INFO
            )
        } else if (isPayeeVerified && !categoryConsistent && payeeCategory != null) {
            RuleCheck(
                name = "Purpose Consistency",
                passed = false,
                message = "${purposeCategory.displayName} + ${payeeCategory.displayName} = Mismatch",
                severity = RuleSeverity.WARNING
            )
        } else {
            RuleCheck(
                name = "Purpose Consistency",
                passed = false,
                message = "Cannot verify purpose consistency — payee category unknown",
                severity = RuleSeverity.WARNING
            )
        }
        checks.add(categoryCheck)

        // Rule 4: Pattern check
        if (hasPriorMismatches || previousDisbursementCount > 10) {
            checks.add(
                RuleCheck(
                    name = "Pattern Analysis",
                    passed = false,
                    message = if (hasPriorMismatches) "Prior mismatches detected on this loan"
                    else "High disbursement frequency — manual review recommended",
                    severity = RuleSeverity.WARNING
                )
            )
        }

        // Determine final result
        val result: RuleResult
        val canAutoApprove: Boolean
        val requiresLenderApproval: Boolean

        when {
            !isPayeeVerified -> {
                result = RuleResult.UNVERIFIED
                canAutoApprove = false
                requiresLenderApproval = true
            }
            !categoryConsistent -> {
                result = RuleResult.MISMATCH
                canAutoApprove = false
                requiresLenderApproval = true
            }
            hasPriorMismatches -> {
                result = RuleResult.REVIEW
                canAutoApprove = false
                requiresLenderApproval = true
            }
            requestedAmount <= autoApprovalThreshold && isPayeeVerified && categoryConsistent -> {
                result = RuleResult.AUTO_APPROVED
                canAutoApprove = true
                requiresLenderApproval = false
            }
            else -> {
                result = RuleResult.CONSISTENT
                canAutoApprove = false
                requiresLenderApproval = true
            }
        }

        return RuleEvaluation(
            result = result,
            checks = checks,
            requiresLenderApproval = requiresLenderApproval,
            canAutoApprove = canAutoApprove
        )
    }

    /**
     * Evaluates full statutory legal compliance for a proposed loan under:
     * 1. State Money Lenders Acts & G. Pankajakshi Amma doctrine (via CasualLendingGuard)
     * 2. Income Tax Act 1961 Section 269SS/269T cash limits (via DigitalBankingAuditValidator)
     * 3. RBI Circular RBI/2023-24/53 banning compounding penalty models
     */
    fun evaluateStatutoryCompliance(
        activeLoansCount: Int,
        proposedInterestRate: Double,
        lenderState: String,
        isInstitutionalLender: Boolean,
        licenseNumber: String? = null,
        gstin: String? = null,
        principalAmount: Double,
        isCashDisbursement: Boolean,
        utrNumber: String? = null,
        penaltyModel: String = "PERCENTAGE"
    ): List<RuleCheck> {
        val checks = mutableListOf<RuleCheck>()

        // Check 1: State Usury & Casual Lending Safe Harbor
        val casualResult = casualLendingGuard.validateLoanTerms(
            activeLoansCount = activeLoansCount,
            proposedInterestRate = proposedInterestRate,
            lenderState = lenderState,
            isInstitutionalLender = isInstitutionalLender,
            licenseNumber = licenseNumber,
            gstin = gstin
        )
        if (!casualResult.isAllowed) {
            checks.add(
                RuleCheck(
                    name = "Statutory Usury & Licensing",
                    passed = false,
                    message = casualResult.violationReason ?: "Violates State Money Lenders Act usury ceilings",
                    severity = RuleSeverity.HARD_BLOCK
                )
            )
        } else {
            checks.add(
                RuleCheck(
                    name = "Statutory Usury & Licensing",
                    passed = true,
                    message = "Within legal rate cap (${casualResult.maxAllowedRate}%) under State Law",
                    severity = RuleSeverity.INFO
                )
            )
        }

        // Check 2: Income Tax Act Section 269SS/T (Cash Limit Audit)
        val taxResult = digitalBankingAuditValidator.auditTransaction(
            amount = principalAmount,
            isCash = isCashDisbursement,
            utrNumber = utrNumber
        )
        if (!taxResult.isCompliant) {
            checks.add(
                RuleCheck(
                    name = "Income Tax §269SS/T Compliance",
                    passed = false,
                    message = taxResult.advisoryMessage,
                    severity = if (taxResult.isCashBlocked) RuleSeverity.HARD_BLOCK else RuleSeverity.WARNING
                )
            )
        } else {
            checks.add(
                RuleCheck(
                    name = "Income Tax §269SS/T Compliance",
                    passed = true,
                    message = "Compliant with Section 269SS/T digital banking mandate",
                    severity = RuleSeverity.INFO
                )
            )
        }

        // Check 3: RBI Fair Lending Directive (Prohibition of Compound Penalties)
        if (penaltyModel.equals("COMPOUND", ignoreCase = true)) {
            checks.add(
                RuleCheck(
                    name = "RBI Fair Lending Penal Charges",
                    passed = false,
                    message = "Compounding penal interest is prohibited under RBI Circular RBI/2023-24/53. Only simple interest is permitted.",
                    severity = RuleSeverity.HARD_BLOCK
                )
            )
        } else {
            checks.add(
                RuleCheck(
                    name = "RBI Fair Lending Penal Charges",
                    passed = true,
                    message = "Complies with simple penal interest ceiling under RBI Fair Lending Directions",
                    severity = RuleSeverity.INFO
                )
            )
        }

        return checks
    }

    private fun formatAmount(amount: Double): String {
        return String.format(java.util.Locale.getDefault(), "%,.2f", amount)
    }
}

/** Individual rule check result */
data class RuleCheck(
    val name: String,
    val passed: Boolean,
    val message: String,
    val severity: RuleSeverity
)

/** Rule check severity levels */
enum class RuleSeverity { INFO, WARNING, HARD_BLOCK }

/** Complete evaluation result from the rule engine */
data class RuleEvaluation(
    val result: RuleResult,
    val checks: List<RuleCheck>,
    val requiresLenderApproval: Boolean,
    val canAutoApprove: Boolean
)
