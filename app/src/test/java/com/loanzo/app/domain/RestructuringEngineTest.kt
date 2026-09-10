package com.loanzo.app.domain

import com.loanzo.app.data.entity.LoanEntity
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for RestructuringEngine — handles loan restructuring
 * (tenure extension) and moratorium (EMI holiday) calculations.
 *
 * Tests cover:
 * - Tenure restructuring with originalTenureMonths preservation
 * - Moratorium application with and without interest capitalization
 * - New EMI recalculation after restructuring
 * - Edge cases (zero tenure, double restructuring)
 */
class RestructuringEngineTest {

    private lateinit var engine: RestructuringEngine

    @BeforeEach
    fun setup() {
        engine = RestructuringEngine()
    }

    private fun createTestLoan(
        tenureMonths: Int = 12,
        outstandingAmount: Double = 100000.0,
        interestRate: Double = 12.0,
        originalTenureMonths: Int = 0,
        isRestructured: Boolean = false,
        moratoriumMonths: Int = 0
    ): LoanEntity {
        return LoanEntity(
            loanId = "loan-test",
            lenderId = "lender-1",
            borrowerId = "borrower-1",
            sanctionedAmount = 100000.0,
            outstandingAmount = outstandingAmount,
            interestRate = interestRate,
            tenureMonths = tenureMonths,
            originalTenureMonths = originalTenureMonths,
            isRestructured = isRestructured,
            moratoriumMonths = moratoriumMonths,
            status = "ACTIVE"
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // Tenure Restructuring
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Loan Tenure Restructuring")
    inner class RestructureTests {

        @Test
        fun `restructuring should update tenure to new value`() {
            val loan = createTestLoan(tenureMonths = 12)
            val result = engine.restructureLoan(loan, newTenureMonths = 24)
            assertThat(result.tenureMonths).isEqualTo(24)
        }

        @Test
        fun `first restructuring should preserve original tenure`() {
            val loan = createTestLoan(tenureMonths = 12, originalTenureMonths = 0)
            val result = engine.restructureLoan(loan, newTenureMonths = 24)
            assertThat(result.originalTenureMonths).isEqualTo(12)
        }

        @Test
        fun `subsequent restructuring should NOT overwrite original tenure`() {
            val loan = createTestLoan(tenureMonths = 24, originalTenureMonths = 12)
            val result = engine.restructureLoan(loan, newTenureMonths = 36)
            assertThat(result.originalTenureMonths).isEqualTo(12)
            assertThat(result.tenureMonths).isEqualTo(36)
        }

        @Test
        fun `restructuring should mark loan as restructured`() {
            val loan = createTestLoan()
            val result = engine.restructureLoan(loan, newTenureMonths = 18)
            assertThat(result.isRestructured).isTrue()
        }

        @Test
        fun `restructuring should set restructuredAt timestamp`() {
            val loan = createTestLoan()
            val before = System.currentTimeMillis()
            val result = engine.restructureLoan(loan, newTenureMonths = 18)
            val after = System.currentTimeMillis()
            assertThat(result.restructuredAt).isNotNull()
            assertThat(result.restructuredAt).isAtLeast(before)
            assertThat(result.restructuredAt).isAtMost(after)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Moratorium (EMI Holiday)
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Moratorium Application")
    inner class MoratoriumTests {

        @Test
        fun `moratorium should extend tenure by moratorium period`() {
            val loan = createTestLoan(tenureMonths = 12)
            val result = engine.applyMoratorium(loan, moratoriumMonths = 3)
            assertThat(result.tenureMonths).isEqualTo(15) // 12 + 3
        }

        @Test
        fun `moratorium should accumulate moratorium months`() {
            val loan = createTestLoan(moratoriumMonths = 2)
            val result = engine.applyMoratorium(loan, moratoriumMonths = 3)
            assertThat(result.moratoriumMonths).isEqualTo(5) // 2 + 3
        }

        @Test
        fun `moratorium without interest capitalization should NOT change outstanding`() {
            val loan = createTestLoan(outstandingAmount = 100000.0)
            val result = engine.applyMoratorium(loan, moratoriumMonths = 3, capitalizeInterest = false)
            assertThat(result.outstandingAmount).isEqualTo(100000.0)
        }

        @Test
        fun `moratorium with interest capitalization should increase outstanding`() {
            val loan = createTestLoan(outstandingAmount = 100000.0, interestRate = 12.0)
            val result = engine.applyMoratorium(loan, moratoriumMonths = 3, capitalizeInterest = true)
            // Monthly rate = 12 / 12 / 100 = 0.01
            // After 3 months: 100000 * (1.01)^3 = ~103030.1
            assertThat(result.outstandingAmount).isGreaterThan(100000.0)
            assertThat(result.outstandingAmount).isWithin(1.0).of(103030.1)
        }

        @Test
        fun `moratorium should mark loan as restructured`() {
            val loan = createTestLoan()
            val result = engine.applyMoratorium(loan, moratoriumMonths = 2)
            assertThat(result.isRestructured).isTrue()
            assertThat(result.restructuredAt).isNotNull()
        }

        @Test
        fun `first moratorium should preserve original tenure`() {
            val loan = createTestLoan(tenureMonths = 12, originalTenureMonths = 0)
            val result = engine.applyMoratorium(loan, moratoriumMonths = 3)
            assertThat(result.originalTenureMonths).isEqualTo(12)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // New EMI Calculation
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("New EMI Calculation After Restructuring")
    inner class NewEmiTests {

        @Test
        fun `new EMI should be lower with extended tenure`() {
            val loan = createTestLoan(outstandingAmount = 100000.0, interestRate = 12.0, tenureMonths = 12)
            val originalEmi = engine.calculateNewEMI(loan, remainingTenureMonths = 12)
            val extendedEmi = engine.calculateNewEMI(loan, remainingTenureMonths = 24)
            assertThat(extendedEmi).isLessThan(originalEmi)
        }

        @Test
        fun `new EMI should be positive for valid inputs`() {
            val loan = createTestLoan(outstandingAmount = 50000.0, interestRate = 10.0, tenureMonths = 6)
            val emi = engine.calculateNewEMI(loan)
            assertThat(emi).isGreaterThan(0.0)
        }

        @Test
        fun `EMI with zero tenure should coerce to at least 1 month`() {
            val loan = createTestLoan(outstandingAmount = 10000.0, interestRate = 12.0, tenureMonths = 0)
            val emi = engine.calculateNewEMI(loan, remainingTenureMonths = 0)
            assertThat(emi).isGreaterThan(0.0)
        }
    }
}
