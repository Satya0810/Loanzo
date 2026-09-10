package com.loanzo.app.domain

import com.loanzo.app.domain.model.PurposeCategory
import com.loanzo.app.domain.model.RuleResult
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource

/**
 * Comprehensive unit tests for the RuleEngine — the core business logic
 * governing tranche disbursement approval and statutory compliance.
 *
 * Tests cover:
 * - Amount limit enforcement (hard blocks)
 * - Payee verification checks
 * - Purpose-payee category consistency mapping
 * - Auto-approval threshold logic
 * - Pattern-based review triggers
 * - Statutory compliance (Usury, Income Tax 269SS/T, RBI penal interest)
 */
class RuleEngineTest {

    private lateinit var casualLendingGuard: CasualLendingGuard
    private lateinit var auditValidator: DigitalBankingAuditValidator
    private lateinit var ruleEngine: RuleEngine

    @BeforeEach
    fun setup() {
        casualLendingGuard = CasualLendingGuard()
        auditValidator = DigitalBankingAuditValidator()
        ruleEngine = RuleEngine(casualLendingGuard, auditValidator)
    }

    // ═══════════════════════════════════════════════════════════════
    // Rule 1: Amount Limit Enforcement
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Amount Limit Checks")
    inner class AmountLimitTests {

        @Test
        fun `amount exceeding remaining limit should be BLOCKED`() {
            val result = ruleEngine.evaluate(
                requestedAmount = 10000.0,
                remainingLimit = 5000.0,
                isPayeeVerified = true,
                purposeCategory = PurposeCategory.HOSPITAL,
                payeeCategory = PurposeCategory.HOSPITAL
            )
            assertThat(result.result).isEqualTo(RuleResult.BLOCKED)
            assertThat(result.canAutoApprove).isFalse()
            assertThat(result.checks.first().name).isEqualTo("Amount Limit")
            assertThat(result.checks.first().passed).isFalse()
            assertThat(result.checks.first().severity).isEqualTo(RuleSeverity.HARD_BLOCK)
        }

        @Test
        fun `amount equal to remaining limit should pass`() {
            val result = ruleEngine.evaluate(
                requestedAmount = 5000.0,
                remainingLimit = 5000.0,
                isPayeeVerified = true,
                purposeCategory = PurposeCategory.HOSPITAL,
                payeeCategory = PurposeCategory.HOSPITAL
            )
            assertThat(result.result).isNotEqualTo(RuleResult.BLOCKED)
            assertThat(result.checks.first().passed).isTrue()
        }

        @Test
        fun `amount within remaining limit should pass`() {
            val result = ruleEngine.evaluate(
                requestedAmount = 2000.0,
                remainingLimit = 10000.0,
                isPayeeVerified = true,
                purposeCategory = PurposeCategory.GROCERY,
                payeeCategory = PurposeCategory.GROCERY
            )
            assertThat(result.result).isNotEqualTo(RuleResult.BLOCKED)
            assertThat(result.checks.first().passed).isTrue()
        }

        @Test
        fun `blocked evaluation should return immediately with only amount check`() {
            val result = ruleEngine.evaluate(
                requestedAmount = 50000.0,
                remainingLimit = 1000.0,
                isPayeeVerified = true,
                purposeCategory = PurposeCategory.HOSPITAL,
                payeeCategory = PurposeCategory.HOSPITAL
            )
            // When BLOCKED, the engine short-circuits — only the amount check is returned
            assertThat(result.checks).hasSize(1)
            assertThat(result.requiresLenderApproval).isFalse()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Rule 2: Payee Verification
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Payee Verification Checks")
    inner class PayeeVerificationTests {

        @Test
        fun `unverified payee should result in UNVERIFIED and require lender approval`() {
            val result = ruleEngine.evaluate(
                requestedAmount = 3000.0,
                remainingLimit = 10000.0,
                isPayeeVerified = false,
                purposeCategory = PurposeCategory.HOSPITAL,
                payeeCategory = PurposeCategory.HOSPITAL
            )
            assertThat(result.result).isEqualTo(RuleResult.UNVERIFIED)
            assertThat(result.requiresLenderApproval).isTrue()
            assertThat(result.canAutoApprove).isFalse()
        }

        @Test
        fun `verified payee check should pass`() {
            val result = ruleEngine.evaluate(
                requestedAmount = 3000.0,
                remainingLimit = 10000.0,
                isPayeeVerified = true,
                purposeCategory = PurposeCategory.GROCERY,
                payeeCategory = PurposeCategory.GROCERY
            )
            val payeeCheck = result.checks.find { it.name == "Payee Verification" }
            assertThat(payeeCheck).isNotNull()
            assertThat(payeeCheck!!.passed).isTrue()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Rule 3: Purpose-Payee Category Consistency
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Category Consistency Checks")
    inner class CategoryConsistencyTests {

        @Test
        fun `matching hospital purpose and payee should be consistent`() {
            val result = ruleEngine.evaluate(
                requestedAmount = 10000.0,
                remainingLimit = 50000.0,
                isPayeeVerified = true,
                purposeCategory = PurposeCategory.HOSPITAL,
                payeeCategory = PurposeCategory.HOSPITAL
            )
            val categoryCheck = result.checks.find { it.name == "Purpose Consistency" }
            assertThat(categoryCheck!!.passed).isTrue()
        }

        @Test
        fun `hospital purpose with pharmacy payee should be consistent (cross-category allowed)`() {
            val result = ruleEngine.evaluate(
                requestedAmount = 5000.0,
                remainingLimit = 50000.0,
                isPayeeVerified = true,
                purposeCategory = PurposeCategory.HOSPITAL,
                payeeCategory = PurposeCategory.PHARMACY
            )
            val categoryCheck = result.checks.find { it.name == "Purpose Consistency" }
            assertThat(categoryCheck!!.passed).isTrue()
        }

        @Test
        fun `hospital purpose with grocery payee should MISMATCH`() {
            val result = ruleEngine.evaluate(
                requestedAmount = 5000.0,
                remainingLimit = 50000.0,
                isPayeeVerified = true,
                purposeCategory = PurposeCategory.HOSPITAL,
                payeeCategory = PurposeCategory.GROCERY
            )
            assertThat(result.result).isEqualTo(RuleResult.MISMATCH)
            assertThat(result.requiresLenderApproval).isTrue()
        }

        @Test
        fun `OTHER purpose should be consistent with any payee category`() {
            val result = ruleEngine.evaluate(
                requestedAmount = 3000.0,
                remainingLimit = 50000.0,
                isPayeeVerified = true,
                purposeCategory = PurposeCategory.OTHER,
                payeeCategory = PurposeCategory.ELECTRONICS
            )
            val categoryCheck = result.checks.find { it.name == "Purpose Consistency" }
            assertThat(categoryCheck!!.passed).isTrue()
        }

        @Test
        fun `null payee category should result in mismatch`() {
            val result = ruleEngine.evaluate(
                requestedAmount = 3000.0,
                remainingLimit = 50000.0,
                isPayeeVerified = true,
                purposeCategory = PurposeCategory.EDUCATION,
                payeeCategory = null
            )
            assertThat(result.result).isEqualTo(RuleResult.MISMATCH)
        }

        @Test
        fun `business purpose should accept electronics, transport, and construction payees`() {
            listOf(
                PurposeCategory.ELECTRONICS,
                PurposeCategory.TRANSPORT,
                PurposeCategory.CONSTRUCTION,
                PurposeCategory.BUSINESS
            ).forEach { payeeCat ->
                val result = ruleEngine.evaluate(
                    requestedAmount = 3000.0,
                    remainingLimit = 50000.0,
                    isPayeeVerified = true,
                    purposeCategory = PurposeCategory.BUSINESS,
                    payeeCategory = payeeCat
                )
                val categoryCheck = result.checks.find { it.name == "Purpose Consistency" }
                assertWithMessage("BUSINESS -> ${payeeCat.name} should be consistent")
                    .that(categoryCheck!!.passed)
                    .isTrue()
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Rule 4: Auto-Approval Threshold
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Auto-Approval Logic")
    inner class AutoApprovalTests {

        @Test
        fun `small amount with verified payee and consistent category should AUTO_APPROVE`() {
            val result = ruleEngine.evaluate(
                requestedAmount = 3000.0,
                remainingLimit = 50000.0,
                isPayeeVerified = true,
                purposeCategory = PurposeCategory.GROCERY,
                payeeCategory = PurposeCategory.GROCERY,
                autoApprovalThreshold = 5000.0
            )
            assertThat(result.result).isEqualTo(RuleResult.AUTO_APPROVED)
            assertThat(result.canAutoApprove).isTrue()
            assertThat(result.requiresLenderApproval).isFalse()
        }

        @Test
        fun `amount exactly at threshold should AUTO_APPROVE`() {
            val result = ruleEngine.evaluate(
                requestedAmount = 5000.0,
                remainingLimit = 50000.0,
                isPayeeVerified = true,
                purposeCategory = PurposeCategory.GROCERY,
                payeeCategory = PurposeCategory.GROCERY,
                autoApprovalThreshold = 5000.0
            )
            assertThat(result.result).isEqualTo(RuleResult.AUTO_APPROVED)
            assertThat(result.canAutoApprove).isTrue()
        }

        @Test
        fun `amount above threshold should be CONSISTENT but require lender approval`() {
            val result = ruleEngine.evaluate(
                requestedAmount = 10000.0,
                remainingLimit = 50000.0,
                isPayeeVerified = true,
                purposeCategory = PurposeCategory.GROCERY,
                payeeCategory = PurposeCategory.GROCERY,
                autoApprovalThreshold = 5000.0
            )
            assertThat(result.result).isEqualTo(RuleResult.CONSISTENT)
            assertThat(result.canAutoApprove).isFalse()
            assertThat(result.requiresLenderApproval).isTrue()
        }

        @Test
        fun `default auto-approval threshold should be 5000`() {
            assertThat(RuleEngine.DEFAULT_AUTO_APPROVAL_THRESHOLD).isEqualTo(5000.0)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Rule 5: Pattern Analysis
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Pattern Analysis Checks")
    inner class PatternTests {

        @Test
        fun `prior mismatches should trigger REVIEW`() {
            val result = ruleEngine.evaluate(
                requestedAmount = 3000.0,
                remainingLimit = 50000.0,
                isPayeeVerified = true,
                purposeCategory = PurposeCategory.GROCERY,
                payeeCategory = PurposeCategory.GROCERY,
                hasPriorMismatches = true
            )
            assertThat(result.result).isEqualTo(RuleResult.REVIEW)
            assertThat(result.requiresLenderApproval).isTrue()
        }

        @Test
        fun `high disbursement count above 10 should add pattern warning`() {
            val result = ruleEngine.evaluate(
                requestedAmount = 3000.0,
                remainingLimit = 50000.0,
                isPayeeVerified = true,
                purposeCategory = PurposeCategory.GROCERY,
                payeeCategory = PurposeCategory.GROCERY,
                previousDisbursementCount = 11
            )
            val patternCheck = result.checks.find { it.name == "Pattern Analysis" }
            assertThat(patternCheck).isNotNull()
            assertThat(patternCheck!!.passed).isFalse()
            assertThat(patternCheck.severity).isEqualTo(RuleSeverity.WARNING)
        }

        @Test
        fun `disbursement count of 10 or less should NOT trigger pattern warning`() {
            val result = ruleEngine.evaluate(
                requestedAmount = 3000.0,
                remainingLimit = 50000.0,
                isPayeeVerified = true,
                purposeCategory = PurposeCategory.GROCERY,
                payeeCategory = PurposeCategory.GROCERY,
                previousDisbursementCount = 10
            )
            val patternCheck = result.checks.find { it.name == "Pattern Analysis" }
            assertThat(patternCheck).isNull()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Statutory Compliance Evaluation
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Statutory Compliance")
    inner class StatutoryComplianceTests {

        @Test
        fun `compliant loan should pass all statutory checks`() {
            val checks = ruleEngine.evaluateStatutoryCompliance(
                activeLoansCount = 2,
                proposedInterestRate = 10.0,
                lenderState = "MAHARASHTRA",
                isInstitutionalLender = false,
                principalAmount = 50000.0,
                isCashDisbursement = false,
                utrNumber = "123456789012",
                penaltyModel = "PERCENTAGE"
            )
            assertThat(checks).hasSize(3)
            assertThat(checks.all { it.passed }).isTrue()
        }

        @Test
        fun `compound penalty model should be HARD_BLOCKED by RBI directive`() {
            val checks = ruleEngine.evaluateStatutoryCompliance(
                activeLoansCount = 1,
                proposedInterestRate = 10.0,
                lenderState = "DELHI",
                isInstitutionalLender = false,
                principalAmount = 10000.0,
                isCashDisbursement = false,
                utrNumber = "123456789012",
                penaltyModel = "COMPOUND"
            )
            val rbiCheck = checks.find { it.name == "RBI Fair Lending Penal Charges" }
            assertThat(rbiCheck).isNotNull()
            assertThat(rbiCheck!!.passed).isFalse()
            assertThat(rbiCheck.severity).isEqualTo(RuleSeverity.HARD_BLOCK)
        }

        @Test
        fun `cash disbursement above 20000 should be blocked by Income Tax 269SS`() {
            val checks = ruleEngine.evaluateStatutoryCompliance(
                activeLoansCount = 1,
                proposedInterestRate = 10.0,
                lenderState = "DELHI",
                isInstitutionalLender = false,
                principalAmount = 25000.0,
                isCashDisbursement = true,
                utrNumber = null,
                penaltyModel = "PERCENTAGE"
            )
            val taxCheck = checks.find { it.name == "Income Tax §269SS/T Compliance" }
            assertThat(taxCheck).isNotNull()
            assertThat(taxCheck!!.passed).isFalse()
            assertThat(taxCheck.severity).isEqualTo(RuleSeverity.HARD_BLOCK)
        }

        @Test
        fun `interest rate exceeding state usury cap should be blocked`() {
            val checks = ruleEngine.evaluateStatutoryCompliance(
                activeLoansCount = 1,
                proposedInterestRate = 20.0, // Maharashtra cap is 12%
                lenderState = "MAHARASHTRA",
                isInstitutionalLender = false,
                principalAmount = 50000.0,
                isCashDisbursement = false,
                utrNumber = "123456789012",
                penaltyModel = "PERCENTAGE"
            )
            val usuryCheck = checks.find { it.name == "Statutory Usury & Licensing" }
            assertThat(usuryCheck).isNotNull()
            assertThat(usuryCheck!!.passed).isFalse()
            assertThat(usuryCheck.severity).isEqualTo(RuleSeverity.HARD_BLOCK)
        }
    }
}
