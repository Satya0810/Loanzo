package com.loanzo.app.domain

import com.loanzo.app.domain.model.PurposeCategory
import com.loanzo.app.domain.model.RuleResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loanzo Rule Engine
 *
 * Implements the transparent, rule-based checks from the project spec:
 * - Amount limit validation
 * - Payee verification check
 * - Purpose-payee category consistency
 * - Auto-approval threshold logic
 * - Pattern-based review triggers
 */
@Singleton
class RuleEngine @Inject constructor() {

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
     *
     * @param requestedAmount Amount requested in this tranche
     * @param remainingLimit Remaining sanctioned limit on the loan
     * @param isPayeeVerified Whether the payee has been verified (UPI name-match / GST)
     * @param purposeCategory The declared purpose category
     * @param payeeCategory The payee's business category
     * @param autoApprovalThreshold Lender-configurable auto-approval threshold
     * @param previousDisbursementCount Number of prior disbursements on this loan
     * @param hasPriorMismatches Whether any prior disbursements had mismatches
     * @return RuleResult indicating the evaluation outcome
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

        // Hard block — return immediately
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
                requiresLenderApproval = true // Above threshold still needs approval
            }
        }

        return RuleEvaluation(
            result = result,
            checks = checks,
            requiresLenderApproval = requiresLenderApproval,
            canAutoApprove = canAutoApprove
        )
    }

    private fun formatAmount(amount: Double): String {
        return String.format("%,.2f", amount)
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
