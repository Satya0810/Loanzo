package com.loanzo.app.domain

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Unit tests for CasualLendingGuard — enforces Indian statutory
 * safeguards under State Money Lenders Acts, Supreme Court doctrine,
 * and usury ceilings.
 *
 * Tests cover:
 * - State-specific usury rate caps
 * - Casual lending concurrent loan limits (max 5 under G. Pankajakshi Amma)
 * - Institutional lender license validation
 * - Interest rate ceiling enforcement per state
 * - Unknown state fallback to national default
 */
class CasualLendingGuardTest {

    private lateinit var guard: CasualLendingGuard

    @BeforeEach
    fun setup() {
        guard = CasualLendingGuard()
    }

    // ═══════════════════════════════════════════════════════════════
    // State Usury Cap Resolution
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("State Usury Cap Lookup")
    inner class StateUsuryCapsTests {

        @ParameterizedTest(name = "{0} should have max rate {1}%")
        @CsvSource(
            "MAHARASHTRA, 12.0",
            "PUNJAB, 12.0",
            "HARYANA, 12.0",
            "TAMIL NADU, 12.0",
            "KARNATAKA, 15.0",
            "GUJARAT, 15.0",
            "DELHI, 18.0",
            "UTTAR PRADESH, 14.0",
            "WEST BENGAL, 15.0",
            "TELANGANA, 15.0",
            "ANDHRA PRADESH, 15.0"
        )
        fun `known states should return correct usury cap`(state: String, expectedRate: Double) {
            val cap = guard.getStateUsuryCap(state)
            assertThat(cap.maxSimpleInterestRate).isEqualTo(expectedRate)
            assertThat(cap.stateName).isNotEmpty()
            assertThat(cap.statuteName).isNotEmpty()
        }

        @Test
        fun `unknown state should fallback to national default of 18 percent`() {
            val cap = guard.getStateUsuryCap("MANIPUR")
            assertThat(cap.maxSimpleInterestRate).isEqualTo(18.0)
            assertThat(cap.statuteName).contains("Usurious Loans Act, 1918")
        }

        @Test
        fun `state lookup should be case-insensitive`() {
            val cap1 = guard.getStateUsuryCap("maharashtra")
            val cap2 = guard.getStateUsuryCap("MAHARASHTRA")
            val cap3 = guard.getStateUsuryCap("Maharashtra")
            assertThat(cap1.maxSimpleInterestRate).isEqualTo(cap2.maxSimpleInterestRate)
            assertThat(cap2.maxSimpleInterestRate).isEqualTo(cap3.maxSimpleInterestRate)
        }

        @Test
        fun `blank state should return national default`() {
            val cap = guard.getStateUsuryCap("")
            assertThat(cap.maxSimpleInterestRate).isEqualTo(18.0)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Casual Lending (Path B) — G. Pankajakshi Amma Safe Harbor
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Casual Lending Validation")
    inner class CasualLendingTests {

        @Test
        fun `casual lender within limits should be allowed`() {
            val result = guard.validateLoanTerms(
                activeLoansCount = 2,
                proposedInterestRate = 10.0,
                lenderState = "MAHARASHTRA",
                isInstitutionalLender = false
            )
            assertThat(result.isAllowed).isTrue()
            assertThat(result.maxAllowedRate).isEqualTo(12.0)
            assertThat(result.violationReason).isNull()
            assertThat(result.statutoryDeclaration).contains("G. Pankajakshi Amma")
        }

        @Test
        fun `casual lender at max concurrent loans should be blocked`() {
            val result = guard.validateLoanTerms(
                activeLoansCount = 5, // MAX_CONCURRENT_CASUAL_LOANS is 5
                proposedInterestRate = 10.0,
                lenderState = "MAHARASHTRA",
                isInstitutionalLender = false
            )
            assertThat(result.isAllowed).isFalse()
            assertThat(result.violationReason).contains("Casual lending limit reached")
        }

        @Test
        fun `casual lender exceeding concurrent limit should be blocked`() {
            val result = guard.validateLoanTerms(
                activeLoansCount = 7,
                proposedInterestRate = 8.0,
                lenderState = "DELHI",
                isInstitutionalLender = false
            )
            assertThat(result.isAllowed).isFalse()
            assertThat(result.violationReason).contains("5 active loans")
        }

        @Test
        fun `casual lender exceeding state usury cap should be blocked`() {
            val result = guard.validateLoanTerms(
                activeLoansCount = 2,
                proposedInterestRate = 15.0, // Maharashtra cap is 12%
                lenderState = "MAHARASHTRA",
                isInstitutionalLender = false
            )
            assertThat(result.isAllowed).isFalse()
            assertThat(result.violationReason).contains("exceeds the statutory ceiling")
            assertThat(result.violationReason).contains("12.0%")
        }

        @Test
        fun `casual lender at exactly state cap rate should be allowed`() {
            val result = guard.validateLoanTerms(
                activeLoansCount = 2,
                proposedInterestRate = 12.0,
                lenderState = "MAHARASHTRA",
                isInstitutionalLender = false
            )
            assertThat(result.isAllowed).isTrue()
        }

        @Test
        fun `casual lender with zero interest should be allowed`() {
            val result = guard.validateLoanTerms(
                activeLoansCount = 0,
                proposedInterestRate = 0.0,
                lenderState = "DELHI",
                isInstitutionalLender = false
            )
            assertThat(result.isAllowed).isTrue()
        }

        @Test
        fun `MAX_CONCURRENT_CASUAL_LOANS should be 5`() {
            assertThat(CasualLendingGuard.MAX_CONCURRENT_CASUAL_LOANS).isEqualTo(5)
        }

        @Test
        fun `NATIONAL_DEFAULT_MAX_RATE should be 18 percent`() {
            assertThat(CasualLendingGuard.NATIONAL_DEFAULT_MAX_RATE).isEqualTo(18.0)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Institutional Lender (Path A)
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Institutional Lender Validation")
    inner class InstitutionalLenderTests {

        @Test
        fun `institutional lender with valid license should be allowed with 36 percent cap`() {
            val result = guard.validateLoanTerms(
                activeLoansCount = 20, // No casual limit for institutional
                proposedInterestRate = 24.0,
                lenderState = "MAHARASHTRA",
                isInstitutionalLender = true,
                licenseNumber = "MH-ML-2024-001",
                gstin = "27AADCB2230M1ZP"
            )
            assertThat(result.isAllowed).isTrue()
            assertThat(result.maxAllowedRate).isEqualTo(36.0) // Institutional flexibility
            assertThat(result.statutoryDeclaration).contains("licensed commercial entity")
            assertThat(result.statutoryDeclaration).contains("MH-ML-2024-001")
        }

        @Test
        fun `institutional lender without license should be blocked`() {
            val result = guard.validateLoanTerms(
                activeLoansCount = 10,
                proposedInterestRate = 15.0,
                lenderState = "MAHARASHTRA",
                isInstitutionalLender = true,
                licenseNumber = null
            )
            assertThat(result.isAllowed).isFalse()
            assertThat(result.violationReason).contains("License Number")
        }

        @Test
        fun `institutional lender with blank license should be blocked`() {
            val result = guard.validateLoanTerms(
                activeLoansCount = 10,
                proposedInterestRate = 15.0,
                lenderState = "DELHI",
                isInstitutionalLender = true,
                licenseNumber = "   "
            )
            assertThat(result.isAllowed).isFalse()
        }

        @Test
        fun `institutional lender without GSTIN should still show N-A`() {
            val result = guard.validateLoanTerms(
                activeLoansCount = 5,
                proposedInterestRate = 20.0,
                lenderState = "KARNATAKA",
                isInstitutionalLender = true,
                licenseNumber = "KA-NBFC-001",
                gstin = null
            )
            assertThat(result.isAllowed).isTrue()
            assertThat(result.statutoryDeclaration).contains("N/A")
        }
    }
}
