package com.loanzo.app.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class TotpHelperTest {

    private val base32Regex = Regex("^[A-Z2-7]+$")

    // ═══════════════════════════════════════════════════════════════
    // Secret Key Generation
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Secret Key Generation")
    inner class SecretKeyTests {

        @Test
        fun `generateSecretKey produces valid 16-character Base32 string`() {
            val secret = TotpHelper.generateSecretKey()
            assertThat(secret).hasLength(16)
            assertThat(secret).matches(base32Regex.pattern)
        }

        @Test
        fun `generateSecretKey generates unique keys on successive calls`() {
            val keys = (1..10).map { TotpHelper.generateSecretKey() }.toSet()
            assertThat(keys).hasSize(10)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // OTPAuth URI Generation
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("OTPAuth URI Generation")
    inner class OtpAuthUriTests {

        @Test
        fun `getOtpAuthUri formats URI correctly with default issuer`() {
            val secret = "JBSWY3DPEHPK3PXP"
            val account = "borrower@loanzo.in"
            val uri = TotpHelper.getOtpAuthUri(account, secret)

            assertThat(uri).startsWith("otpauth://totp/Loanzo:borrower%40loanzo.in?")
            assertThat(uri).contains("secret=JBSWY3DPEHPK3PXP")
            assertThat(uri).contains("issuer=Loanzo")
            assertThat(uri).contains("algorithm=SHA1")
            assertThat(uri).contains("digits=6")
            assertThat(uri).contains("period=30")
        }

        @Test
        fun `getOtpAuthUri formats URI with custom issuer and spaces`() {
            val secret = "JBSWY3DPEHPK3PXP"
            val account = "+91 9876543210"
            val uri = TotpHelper.getOtpAuthUri(account, secret, issuer = "Loanzo Dev")

            assertThat(uri).contains("Loanzo%20Dev")
            assertThat(uri).contains("%2B91%209876543210")
            assertThat(uri).contains("secret=$secret")
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Code Generation & Verification
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Code Verification & Validation")
    inner class CodeVerificationTests {

        @Test
        fun `generateCurrentCode generates 6-digit numeric string`() {
            val secret = TotpHelper.generateSecretKey()
            val code = TotpHelper.generateCurrentCode(secret)

            assertThat(code).hasLength(6)
            assertThat(code.all { it.isDigit() }).isTrue()
        }

        @Test
        fun `verifyCode returns true for current generated code`() {
            val secret = TotpHelper.generateSecretKey()
            val code = TotpHelper.generateCurrentCode(secret)

            assertThat(TotpHelper.verifyCode(secret, code)).isTrue()
        }

        @Test
        fun `verifyCode ignores surrounding whitespace and spaces within code`() {
            val secret = TotpHelper.generateSecretKey()
            val code = TotpHelper.generateCurrentCode(secret)
            val formatted = " ${code.take(3)} ${code.drop(3)} "

            assertThat(TotpHelper.verifyCode(secret, formatted)).isTrue()
        }

        @Test
        fun `verifyCode returns false for incorrect code`() {
            val secret = TotpHelper.generateSecretKey()
            val wrongCode = "000000"
            val correctCode = TotpHelper.generateCurrentCode(secret)
            val candidate = if (correctCode == wrongCode) "111111" else wrongCode

            assertThat(TotpHelper.verifyCode(secret, candidate, window = 0)).isFalse()
        }

        @ParameterizedTest(name = "Invalid input: \"{0}\" should be rejected")
        @ValueSource(strings = ["", "12345", "1234567", "abcdef", "12345a", " "])
        fun `verifyCode returns false for invalid code format`(invalidCode: String) {
            val secret = TotpHelper.generateSecretKey()
            assertThat(TotpHelper.verifyCode(secret, invalidCode)).isFalse()
        }

        @Test
        fun `verifyCode returns false for wrong secret`() {
            val secretA = "JBSWY3DPEHPK3PXP"
            val secretB = "NBSWY3DPEHPK3PXP"
            val codeA = TotpHelper.generateCurrentCode(secretA)

            assertThat(TotpHelper.verifyCode(secretB, codeA, window = 0)).isFalse()
        }
    }
}
