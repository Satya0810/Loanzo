package com.loanzo.app.domain

import com.loanzo.app.data.entity.LoanEntity
import com.loanzo.app.data.entity.RepaymentEntity
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Unit tests for PenaltyEngine — the financial penalty calculation engine.
 *
 * Tests cover:
 * - PERCENTAGE (simple interest) penalty calculation
 * - COMPOUND penalty calculation
 * - FLAT fee penalty calculation
 * - NONE model (no penalty)
 * - Grace period enforcement
 * - RBI max penalty rate cap (2% monthly)
 * - Penalty cap as percentage of EMI amount
 * - Penalty waiver handling
 * - Loan servicing state machine transitions
 */
class PenaltyEngineTest {

    private lateinit var penaltyEngine: PenaltyEngine

    @BeforeEach
    fun setup() {
        penaltyEngine = PenaltyEngine()
    }

    // Helper to create an overdue repayment with a specific number of days overdue
    private fun createOverdueRepayment(
        amount: Double = 10000.0,
        daysOverdue: Int = 30,
        penaltyWaived: Boolean = false
    ): RepaymentEntity {
        val dueDate = System.currentTimeMillis() - (daysOverdue.toLong() * 24 * 60 * 60 * 1000)
        return RepaymentEntity(
            repaymentId = "rep-test",
            loanId = "loan-test",
            amount = amount,
            status = "OVERDUE",
            dueDate = dueDate,
            penaltyWaived = penaltyWaived
        )
    }

    private fun createDefaultLoan(
        penaltyRate: Double = 2.0,
        penaltyModel: String = "PERCENTAGE",
        graceDays: Int = 3,
        capPercent: Double = 100.0
    ): LoanEntity {
        return LoanEntity(
            loanId = "loan-test",
            lenderId = "lender-1",
            borrowerId = "borrower-1",
            sanctionedAmount = 100000.0,
            outstandingAmount = 50000.0,
            interestRate = 12.0,
            penaltyRate = penaltyRate,
            penaltyModel = penaltyModel,
            penaltyGraceDays = graceDays,
            penaltyCapPercent = capPercent,
            status = "ACTIVE"
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // NONE Model
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("NONE Penalty Model")
    inner class NonePenaltyTests {

        @Test
        fun `NONE penalty model should return zero penalty`() {
            val repayment = createOverdueRepayment(daysOverdue = 60)
            val penalty = penaltyEngine.calculatePenalty(
                repayment = repayment,
                penaltyRate = 2.0,
                penaltyModel = "NONE"
            )
            assertThat(penalty).isEqualTo(0.0)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Grace Period
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Grace Period Enforcement")
    inner class GracePeriodTests {

        @Test
        fun `repayment within grace period should have zero penalty`() {
            val repayment = createOverdueRepayment(daysOverdue = 2)
            val penalty = penaltyEngine.calculatePenalty(
                repayment = repayment,
                penaltyRate = 2.0,
                penaltyModel = "PERCENTAGE",
                graceDays = 3
            )
            assertThat(penalty).isEqualTo(0.0)
        }

        @Test
        fun `repayment exactly at grace period boundary should have zero penalty`() {
            val repayment = createOverdueRepayment(daysOverdue = 3)
            val penalty = penaltyEngine.calculatePenalty(
                repayment = repayment,
                penaltyRate = 2.0,
                penaltyModel = "PERCENTAGE",
                graceDays = 3
            )
            assertThat(penalty).isEqualTo(0.0)
        }

        @Test
        fun `repayment one day past grace period should incur penalty`() {
            val repayment = createOverdueRepayment(daysOverdue = 4)
            val penalty = penaltyEngine.calculatePenalty(
                repayment = repayment,
                penaltyRate = 2.0,
                penaltyModel = "PERCENTAGE",
                graceDays = 3
            )
            assertThat(penalty).isGreaterThan(0.0)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // PERCENTAGE (Simple Interest) Model
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PERCENTAGE Penalty Model (Simple Interest)")
    inner class PercentagePenaltyTests {

        @Test
        fun `30 days overdue with 3 day grace at 2 pct should calculate correctly`() {
            // effectiveDays = 30 - 3 = 27 days = 0.9 months
            // penalty = 10000 * (2/100) * 0.9 = 180.0
            val repayment = createOverdueRepayment(amount = 10000.0, daysOverdue = 30)
            val penalty = penaltyEngine.calculatePenalty(
                repayment = repayment,
                penaltyRate = 2.0,
                penaltyModel = "PERCENTAGE",
                graceDays = 3
            )
            assertThat(penalty).isWithin(1.0).of(180.0)
        }

        @Test
        fun `60 days overdue should calculate proportionally higher penalty`() {
            // effectiveDays = 60 - 3 = 57 days = 1.9 months
            // penalty = 10000 * (2/100) * 1.9 = 380.0
            val repayment = createOverdueRepayment(amount = 10000.0, daysOverdue = 60)
            val penalty = penaltyEngine.calculatePenalty(
                repayment = repayment,
                penaltyRate = 2.0,
                penaltyModel = "PERCENTAGE",
                graceDays = 3
            )
            assertThat(penalty).isWithin(1.0).of(380.0)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // COMPOUND Model
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("COMPOUND Penalty Model")
    inner class CompoundPenaltyTests {

        @Test
        fun `compound penalty should be higher than simple for same period`() {
            val repayment = createOverdueRepayment(amount = 10000.0, daysOverdue = 60)
            val simplePenalty = penaltyEngine.calculatePenalty(
                repayment = repayment,
                penaltyRate = 2.0,
                penaltyModel = "PERCENTAGE",
                graceDays = 3
            )
            val compoundPenalty = penaltyEngine.calculatePenalty(
                repayment = repayment,
                penaltyRate = 2.0,
                penaltyModel = "COMPOUND",
                graceDays = 3
            )
            assertThat(compoundPenalty).isGreaterThan(simplePenalty)
        }

        @Test
        fun `compound penalty formula should use P times bracket 1 plus r power n minus 1`() {
            // effectiveDays = 30 - 3 = 27 = 0.9 months
            // compound = 10000 * ((1 + 0.02)^0.9 - 1) ≈ 10000 * 0.01799 ≈ 179.9
            val repayment = createOverdueRepayment(amount = 10000.0, daysOverdue = 30)
            val penalty = penaltyEngine.calculatePenalty(
                repayment = repayment,
                penaltyRate = 2.0,
                penaltyModel = "COMPOUND",
                graceDays = 3
            )
            assertThat(penalty).isGreaterThan(0.0)
            assertThat(penalty).isWithin(5.0).of(180.0)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // FLAT Model
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("FLAT Penalty Model")
    inner class FlatPenaltyTests {

        @Test
        fun `flat penalty should charge fixed rate per month`() {
            // effectiveDays = 60 - 3 = 57, months = 57/30 = 1 (integer div, coerced to at least 1)
            // penalty = penaltyRate * months = 500 * 1 = 500
            val repayment = createOverdueRepayment(amount = 10000.0, daysOverdue = 60)
            val penalty = penaltyEngine.calculatePenalty(
                repayment = repayment,
                penaltyRate = 500.0, // ₹500 flat fee per month
                penaltyModel = "FLAT",
                graceDays = 3
            )
            assertThat(penalty).isEqualTo(500.0)
        }

        @Test
        fun `flat penalty for 90 days overdue should charge 2 months`() {
            // effectiveDays = 90 - 3 = 87, months = 87/30 = 2 (integer div)
            // penalty = 500 * 2 = 1000
            val repayment = createOverdueRepayment(amount = 10000.0, daysOverdue = 90)
            val penalty = penaltyEngine.calculatePenalty(
                repayment = repayment,
                penaltyRate = 500.0,
                penaltyModel = "FLAT",
                graceDays = 3
            )
            assertThat(penalty).isEqualTo(1000.0)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Rate Cap & Penalty Cap
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Rate Cap & Penalty Cap Enforcement")
    inner class CapTests {

        @Test
        fun `penalty rate should be capped at 2 percent monthly max`() {
            val repayment = createOverdueRepayment(amount = 10000.0, daysOverdue = 33)
            // Using rate = 5.0 (exceeds MAX_PENALTY_RATE_MONTHLY of 2.0)
            // Should be coerced to 2.0
            val penalty = penaltyEngine.calculatePenalty(
                repayment = repayment,
                penaltyRate = 5.0,
                penaltyModel = "PERCENTAGE",
                graceDays = 3
            )
            // effectiveDays = 33 - 3 = 30 = 1.0 month
            // With capped rate 2%: penalty = 10000 * 0.02 * 1.0 = 200
            assertThat(penalty).isWithin(1.0).of(200.0)
        }

        @Test
        fun `penalty should not exceed cap percent of EMI amount`() {
            // Cap at 50% of EMI amount (10000), so max penalty = 5000
            val repayment = createOverdueRepayment(amount = 10000.0, daysOverdue = 365)
            val penalty = penaltyEngine.calculatePenalty(
                repayment = repayment,
                penaltyRate = 2.0,
                penaltyModel = "PERCENTAGE",
                graceDays = 0,
                capPercent = 50.0
            )
            assertThat(penalty).isAtMost(5000.0)
        }

        @Test
        fun `MAX_PENALTY_RATE_MONTHLY should be 2 percent`() {
            assertThat(PenaltyEngine.MAX_PENALTY_RATE_MONTHLY).isEqualTo(2.0)
        }

        @Test
        fun `MAX_TOTAL_PENALTY_PERCENT should be 100 percent`() {
            assertThat(PenaltyEngine.MAX_TOTAL_PENALTY_PERCENT).isEqualTo(100.0)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Waiver Handling
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Penalty Waiver")
    inner class WaiverTests {

        @Test
        fun `waived penalty should return zero`() {
            val repayment = createOverdueRepayment(daysOverdue = 60, penaltyWaived = true)
            val penalty = penaltyEngine.calculatePenalty(
                repayment = repayment,
                penaltyRate = 2.0,
                penaltyModel = "PERCENTAGE"
            )
            assertThat(penalty).isEqualTo(0.0)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Non-OVERDUE Status
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Non-Overdue Status Handling")
    inner class StatusTests {

        @Test
        fun `PAID repayment should have zero penalty`() {
            val repayment = RepaymentEntity(
                repaymentId = "rep-paid",
                loanId = "loan-test",
                amount = 10000.0,
                status = "PAID",
                dueDate = System.currentTimeMillis() - (60L * 24 * 60 * 60 * 1000)
            )
            val penalty = penaltyEngine.calculatePenalty(
                repayment = repayment,
                penaltyRate = 2.0,
                penaltyModel = "PERCENTAGE"
            )
            assertThat(penalty).isEqualTo(0.0)
        }

        @Test
        fun `SCHEDULED repayment should have zero penalty`() {
            val repayment = RepaymentEntity(
                repaymentId = "rep-sched",
                loanId = "loan-test",
                amount = 10000.0,
                status = "SCHEDULED",
                dueDate = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
            )
            val penalty = penaltyEngine.calculatePenalty(
                repayment = repayment,
                penaltyRate = 2.0,
                penaltyModel = "PERCENTAGE"
            )
            assertThat(penalty).isEqualTo(0.0)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // applyPenalties (Batch Processing)
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Batch Penalty Application")
    inner class ApplyPenaltiesTests {

        @Test
        fun `applyPenalties should auto-mark past-due SCHEDULED as OVERDUE`() {
            val pastDueScheduled = RepaymentEntity(
                repaymentId = "rep-1",
                loanId = "loan-test",
                amount = 5000.0,
                status = "SCHEDULED",
                dueDate = System.currentTimeMillis() - (10L * 24 * 60 * 60 * 1000) // 10 days ago
            )
            val loan = createDefaultLoan()
            val result = penaltyEngine.applyPenalties(listOf(pastDueScheduled), loan)

            assertThat(result).hasSize(1)
            assertThat(result[0].status).isEqualTo("OVERDUE")
            assertThat(result[0].penalty).isGreaterThan(0.0)
        }

        @Test
        fun `applyPenalties should NOT change future SCHEDULED repayments`() {
            val futureScheduled = RepaymentEntity(
                repaymentId = "rep-future",
                loanId = "loan-test",
                amount = 5000.0,
                status = "SCHEDULED",
                dueDate = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000) // 30 days from now
            )
            val loan = createDefaultLoan()
            val result = penaltyEngine.applyPenalties(listOf(futureScheduled), loan)

            assertThat(result).hasSize(1)
            assertThat(result[0].status).isEqualTo("SCHEDULED")
            assertThat(result[0].penalty).isEqualTo(0.0)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Servicing State Machine
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Loan Servicing State Machine")
    inner class ServicingStateTests {

        @Test
        fun `loan with all PAID repayments should be COMPLETED`() {
            val loan = createDefaultLoan().copy(status = "ACTIVE")
            val repayments = listOf(
                RepaymentEntity(repaymentId = "r1", loanId = "loan-test", amount = 5000.0, status = "PAID", dueDate = System.currentTimeMillis() - 86400000),
                RepaymentEntity(repaymentId = "r2", loanId = "loan-test", amount = 5000.0, status = "PAID", dueDate = System.currentTimeMillis() - 86400000)
            )
            val state = penaltyEngine.evaluateServicingState(loan, repayments)
            assertThat(state).isEqualTo("COMPLETED")
        }

        @Test
        fun `loan with zero outstanding should be COMPLETED`() {
            val loan = createDefaultLoan().copy(status = "ACTIVE", outstandingAmount = 0.0)
            val state = penaltyEngine.evaluateServicingState(loan, emptyList())
            assertThat(state).isEqualTo("COMPLETED")
        }

        @Test
        fun `CLOSED loan should be COMPLETED`() {
            val loan = createDefaultLoan().copy(status = "CLOSED")
            val state = penaltyEngine.evaluateServicingState(loan, emptyList())
            assertThat(state).isEqualTo("COMPLETED")
        }

        @Test
        fun `restructured loan with no delinquent items should be RESTRUCTURED`() {
            val loan = createDefaultLoan().copy(status = "ACTIVE", isRestructured = true)
            val repayments = listOf(
                RepaymentEntity(repaymentId = "r1", loanId = "loan-test", amount = 5000.0, status = "SCHEDULED",
                    dueDate = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000))
            )
            val state = penaltyEngine.evaluateServicingState(loan, repayments)
            assertThat(state).isEqualTo("RESTRUCTURED")
        }

        @Test
        fun `loan in non-servicing status should return its own status`() {
            val loan = createDefaultLoan().copy(status = "CONTRACT_SIGNING")
            val state = penaltyEngine.evaluateServicingState(loan, emptyList())
            assertThat(state).isEqualTo("CONTRACT_SIGNING")
        }

        @Test
        fun `active loan with no overdue should be ACTIVE_SERVICING`() {
            val loan = createDefaultLoan().copy(status = "ACTIVE")
            val repayments = listOf(
                RepaymentEntity(repaymentId = "r1", loanId = "loan-test", amount = 5000.0, status = "SCHEDULED",
                    dueDate = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000))
            )
            val state = penaltyEngine.evaluateServicingState(loan, repayments)
            assertThat(state).isEqualTo("ACTIVE_SERVICING")
        }
    }
}
