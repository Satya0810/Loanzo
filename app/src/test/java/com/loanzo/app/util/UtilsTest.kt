package com.loanzo.app.util

import com.google.common.truth.Truth.assertThat
import com.loanzo.app.data.entity.UserEntity
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.Calendar

class UtilsTest {

    // ═══════════════════════════════════════════════════════════════
    // Password Hashing
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Password Hashing (SHA-256)")
    inner class PasswordHashingTests {

        @Test
        fun `hashPassword produces 64-character lowercase hex string`() {
            val hash = hashPassword("LoanzoSecurePassword123!")
            assertThat(hash).hasLength(64)
            assertThat(hash).matches("^[0-9a-f]{64}$")
        }

        @Test
        fun `hashPassword is deterministic`() {
            val hash1 = hashPassword("TestPassword")
            val hash2 = hashPassword("TestPassword")
            assertThat(hash1).isEqualTo(hash2)
        }

        @Test
        fun `different passwords produce different hashes`() {
            val hash1 = hashPassword("PasswordOne")
            val hash2 = hashPassword("PasswordTwo")
            assertThat(hash1).isNotEqualTo(hash2)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Financial Calculations (Interest & EMI)
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Financial Math: Simple Interest & EMI")
    inner class FinancialMathTests {

        @Test
        fun `calculateSimpleInterest computes correct values`() {
            // 10,000 at 12% p.a. for 12 months = 1,200
            val interest12m = calculateSimpleInterest(10000.0, 12.0, 12)
            assertThat(interest12m).isWithin(0.01).of(1200.0)

            // 50,000 at 18% p.a. for 6 months = 4,500
            val interest6m = calculateSimpleInterest(50000.0, 18.0, 6)
            assertThat(interest6m).isWithin(0.01).of(4500.0)

            // Zero interest rate
            val interestZero = calculateSimpleInterest(50000.0, 0.0, 12)
            assertThat(interestZero).isWithin(0.01).of(0.0)
        }

        @Test
        fun `calculateEMI handles zero interest rate`() {
            // 12,000 principal at 0% for 12 months = 1,000/month
            val emi = calculateEMI(12000.0, 0.0, 12)
            assertThat(emi).isWithin(0.01).of(1000.0)
        }

        @Test
        fun `calculateEMI handles zero tenure`() {
            val emi = calculateEMI(50000.0, 12.0, 0)
            assertThat(emi).isEqualTo(50000.0)
        }

        @Test
        fun `calculateEMI computes standard amortization correctly`() {
            // 100,000 at 12% p.a. for 12 months:
            // r = 0.01 monthly, factor = 1.01^12 = 1.126825
            // EMI = 100000 * 0.01 * 1.126825 / 0.126825 ~ 8884.88
            val emi = calculateEMI(100000.0, 12.0, 12)
            assertThat(emi).isWithin(1.0).of(8884.88)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Date & Repayment Schedule Generation
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Schedule Dates & Interval Math")
    inner class DateScheduleTests {

        @Test
        fun `daysBetween computes difference correctly`() {
            val oneDayMs = 86_400_000L
            val now = System.currentTimeMillis()
            val fiveDaysLater = now + (5 * oneDayMs)

            assertThat(daysBetween(now, fiveDaysLater)).isEqualTo(5)
        }

        @Test
        fun `generateScheduleDates generates correct count for MONTHLY`() {
            val start = System.currentTimeMillis()
            val dates = generateScheduleDates(start, tenureMonths = 6, frequency = "MONTHLY")

            assertThat(dates).hasSize(6)
            // Dates should be strictly monotonically increasing
            for (i in 0 until dates.size - 1) {
                assertThat(dates[i + 1]).isGreaterThan(dates[i])
            }
        }

        @Test
        fun `generateScheduleDates generates 4x installments for WEEKLY`() {
            val start = System.currentTimeMillis()
            val dates = generateScheduleDates(start, tenureMonths = 3, frequency = "WEEKLY")

            assertThat(dates).hasSize(12) // 3 * 4
        }

        @Test
        fun `generateScheduleDates generates 2x installments for BI_WEEKLY`() {
            val start = System.currentTimeMillis()
            val dates = generateScheduleDates(start, tenureMonths = 3, frequency = "BI_WEEKLY")

            assertThat(dates).hasSize(6) // 3 * 2
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // User Initials & Display Helpers
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("User Initials")
    inner class UserInitialsTests {

        private fun buildUser(name: String, username: String = ""): UserEntity {
            return UserEntity(
                userId = "test_user_id",
                name = name,
                username = username,
                email = "user@test.com",
                phone = "9876543210"
            )
        }

        @Test
        fun `null user returns U`() {
            val nullUser: UserEntity? = null
            assertThat(nullUser.getInitials()).isEqualTo("U")
        }

        @Test
        fun `two-word name returns two initials`() {
            val user = buildUser("Rahul Sharma")
            assertThat(user.getInitials()).isEqualTo("RS")
        }

        @Test
        fun `single-word name returns first two letters uppercase`() {
            val user = buildUser("Loanzo")
            assertThat(user.getInitials()).isEqualTo("LO")
        }

        @Test
        fun `multi-word name returns first letters of first two words`() {
            val user = buildUser("Dr. Vikram Aditya Singh")
            assertThat(user.getInitials()).isEqualTo("DV")
        }

        @Test
        fun `empty name falls back to username initials`() {
            val user = buildUser(name = "", username = "Priya Verma")
            assertThat(user.getInitials()).isEqualTo("PV")
        }
    }
}
