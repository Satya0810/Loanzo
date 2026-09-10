package com.loanzo.app.domain

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

/**
 * Unit tests for DigitalBankingAuditValidator — enforces compliance
 * with Income Tax Act 1961 Sections 269SS/T and RBI digital banking mandates.
 *
 * Tests cover:
 * - Cash statutory limit (₹20,000 threshold)
 * - Cash transaction blocking above threshold
 * - Cash transaction below threshold (allowed with advisory)
 * - UTR validation (12-digit numeric and alphanumeric formats)
 * - Digital transaction with valid UTR (fully compliant)
 * - Digital transaction with invalid/missing UTR (non-compliant)
 * - Penalty risk amount calculations
 */
class DigitalBankingAuditValidatorTest {

    private lateinit var validator: DigitalBankingAuditValidator

    @BeforeEach
    fun setup() {
        validator = DigitalBankingAuditValidator()
    }

    // ═══════════════════════════════════════════════════════════════
    // Cash Statutory Limit
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Cash Statutory Limit (isCashBarred)")
    inner class CashBarredTests {

        @Test
        fun `amount at 20000 should be cash barred`() {
            assertThat(validator.isCashBarred(20000.0)).isTrue()
        }

        @Test
        fun `amount above 20000 should be cash barred`() {
            assertThat(validator.isCashBarred(50000.0)).isTrue()
        }

        @Test
        fun `amount below 20000 should NOT be cash barred`() {
            assertThat(validator.isCashBarred(19999.0)).isFalse()
        }

        @Test
        fun `zero amount should NOT be cash barred`() {
            assertThat(validator.isCashBarred(0.0)).isFalse()
        }

        @Test
        fun `CASH_STATUTORY_LIMIT constant should be 20000`() {
            assertThat(DigitalBankingAuditValidator.CASH_STATUTORY_LIMIT).isEqualTo(20000.0)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // UTR Validation
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("UTR Validation")
    inner class UtrValidationTests {

        @ParameterizedTest(name = "valid UTR: {0}")
        @ValueSource(strings = [
            "123456789012",          // 12-digit numeric
            "ABCD12345678",          // 12-char alphanumeric
            "IMPS123456789012",      // 16-char IMPS format
            "NEFT1234567890123456"   // 20-char NEFT format
        ])
        fun `valid UTR formats should pass`(utr: String) {
            assertThat(validator.isValidUtr(utr)).isTrue()
        }

        @ParameterizedTest(name = "invalid UTR: {0}")
        @ValueSource(strings = [
            "12345",          // Too short
            "123",            // Way too short
            "12345678901",    // 11 digits (one short)
            ""                // Empty
        ])
        fun `invalid UTR formats should fail`(utr: String) {
            assertThat(validator.isValidUtr(utr)).isFalse()
        }

        @Test
        fun `null UTR should fail`() {
            assertThat(validator.isValidUtr(null)).isFalse()
        }

        @Test
        fun `blank UTR should fail`() {
            assertThat(validator.isValidUtr("   ")).isFalse()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Cash Transaction Audit
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Cash Transaction Auditing")
    inner class CashAuditTests {

        @Test
        fun `cash transaction at or above 20000 should be blocked`() {
            val result = validator.auditTransaction(
                amount = 25000.0,
                isCash = true
            )
            assertThat(result.isCompliant).isFalse()
            assertThat(result.isCashBlocked).isTrue()
            assertThat(result.penaltyRiskAmount).isEqualTo(25000.0)
            assertThat(result.advisoryMessage).contains("269SS")
            assertThat(result.advisoryMessage).contains("100% penalty")
            assertThat(result.statutoryCitation).contains("Income Tax Act")
        }

        @Test
        fun `cash transaction below 20000 should be compliant but with advisory`() {
            val result = validator.auditTransaction(
                amount = 15000.0,
                isCash = true
            )
            assertThat(result.isCompliant).isTrue()
            assertThat(result.isCashBlocked).isFalse()
            assertThat(result.penaltyRiskAmount).isEqualTo(0.0)
            assertThat(result.advisoryMessage).contains("below the ₹20,000 threshold")
            assertThat(result.advisoryMessage).contains("UPI is strongly recommended")
        }

        @Test
        fun `cash transaction at exactly 20000 should be blocked`() {
            val result = validator.auditTransaction(
                amount = 20000.0,
                isCash = true
            )
            assertThat(result.isCompliant).isFalse()
            assertThat(result.isCashBlocked).isTrue()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Digital Transaction Audit
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Digital Transaction Auditing")
    inner class DigitalAuditTests {

        @Test
        fun `digital transaction with valid UTR should be fully compliant`() {
            val result = validator.auditTransaction(
                amount = 100000.0,
                isCash = false,
                utrNumber = "123456789012"
            )
            assertThat(result.isCompliant).isTrue()
            assertThat(result.isCashBlocked).isFalse()
            assertThat(result.penaltyRiskAmount).isEqualTo(0.0)
            assertThat(result.advisoryMessage).contains("fully compliant")
            assertThat(result.advisoryMessage).contains("UTR trail recorded")
        }

        @Test
        fun `digital transaction without UTR should be non-compliant`() {
            val result = validator.auditTransaction(
                amount = 50000.0,
                isCash = false,
                utrNumber = null
            )
            assertThat(result.isCompliant).isFalse()
            assertThat(result.isCashBlocked).isFalse()
            assertThat(result.advisoryMessage).contains("12-digit bank UTR")
        }

        @Test
        fun `digital transaction with invalid UTR should be non-compliant`() {
            val result = validator.auditTransaction(
                amount = 50000.0,
                isCash = false,
                utrNumber = "12345"
            )
            assertThat(result.isCompliant).isFalse()
            assertThat(result.isCashBlocked).isFalse()
        }

        @Test
        fun `digital transaction with blank UTR should be non-compliant`() {
            val result = validator.auditTransaction(
                amount = 50000.0,
                isCash = false,
                utrNumber = "   "
            )
            assertThat(result.isCompliant).isFalse()
        }

        @Test
        fun `large digital transaction with valid UTR should be compliant (no cash limit for digital)`() {
            val result = validator.auditTransaction(
                amount = 1000000.0, // ₹10 lakh
                isCash = false,
                utrNumber = "IMPS123456789012"
            )
            assertThat(result.isCompliant).isTrue()
            assertThat(result.penaltyRiskAmount).isEqualTo(0.0)
        }
    }
}
