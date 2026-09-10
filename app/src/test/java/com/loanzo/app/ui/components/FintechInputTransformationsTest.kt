package com.loanzo.app.ui.components

import androidx.compose.ui.text.AnnotatedString
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FintechInputTransformationsTest {

    // ═══════════════════════════════════════════════════════════════
    // Indian Currency Visual Transformation
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Indian Currency Visual Transformation")
    inner class CurrencyTests {

        private val transformation = IndianCurrencyVisualTransformation()

        @Test
        fun `empty input returns empty transformed text`() {
            val result = transformation.filter(AnnotatedString(""))
            assertThat(result.text.text).isEmpty()
        }

        @ParameterizedTest(name = "\"{0}\" should format to \"{1}\"")
        @CsvSource(
            delimiter = ';',
            value = [
                "5; ₹5",
                "50; ₹50",
                "500; ₹500",
                "1000; ₹1,000",
                "10000; ₹10,000",
                "100000; ₹1,00,000",
                "1500000; ₹15,00,000",
                "10000000; ₹1,00,00,000"
            ]
        )
        fun `correctly formats numbers in Indian Lakhs and Crores system`(raw: String, expected: String) {
            val result = transformation.filter(AnnotatedString(raw))
            assertThat(result.text.text).isEqualTo(expected)
        }

        @Test
        fun `offset mapping preserves cursor roundtrip for Lakhs`() {
            val raw = "100000" // Formats to ₹1,00,000
            val result = transformation.filter(AnnotatedString(raw))

            assertThat(result.text.text).isEqualTo("₹1,00,000")

            // Test original to transformed
            val mapping = result.offsetMapping
            assertThat(mapping.originalToTransformed(0)).isEqualTo(1) // after '₹'
            assertThat(mapping.originalToTransformed(1)).isEqualTo(3) // after '1,'
            assertThat(mapping.originalToTransformed(3)).isEqualTo(6) // after '1,00,'
            assertThat(mapping.originalToTransformed(6)).isEqualTo(9) // after '1,00,000'

            // Roundtrip check: original -> transformed -> original
            for (offset in 0..raw.length) {
                val trans = mapping.originalToTransformed(offset)
                val orig = mapping.transformedToOriginal(trans)
                assertThat(orig).isEqualTo(offset)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // PAN Card Visual Transformation
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PAN Card Visual Transformation")
    inner class PanTests {

        private val transformation = PanVisualTransformation()

        @Test
        fun `automatically capitalizes lowercase input`() {
            val result = transformation.filter(AnnotatedString("abcde1234f"))
            assertThat(result.text.text).isEqualTo("ABCDE1234F")
        }

        @Test
        fun `limits text to 10 characters`() {
            val result = transformation.filter(AnnotatedString("abcde1234fEXTRA"))
            assertThat(result.text.text).isEqualTo("ABCDE1234F")
        }

        @ParameterizedTest(name = "PAN: {0} isValid should be {1}")
        @CsvSource(
            "ABCDE1234F, true",
            "BLZPA1092K, true",
            "12345ABCDE, false",
            "ABC123456F, false",
            "ABCDE12345, false",
            "ABCDEF1234, false"
        )
        fun `validates PAN regex format accurately`(pan: String, expected: Boolean) {
            assertThat(PanVisualTransformation.isValidPan(pan)).isEqualTo(expected)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Aadhaar Visual Transformation
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Aadhaar Visual Transformation")
    inner class AadhaarTests {

        @Test
        fun `formats 12 digits into 4-4-4 spacing`() {
            val transformation = AadhaarVisualTransformation(masked = false)
            val result = transformation.filter(AnnotatedString("123456789012"))
            assertThat(result.text.text).isEqualTo("1234 5678 9012")
        }

        @Test
        fun `masked option obscures first 8 digits`() {
            val transformation = AadhaarVisualTransformation(masked = true)
            val result = transformation.filter(AnnotatedString("123456789012"))
            assertThat(result.text.text).isEqualTo("•••• •••• 9012")
        }

        @Test
        fun `partial input formats with intermediate spaces`() {
            val transformation = AadhaarVisualTransformation(masked = false)
            val result = transformation.filter(AnnotatedString("12345"))
            assertThat(result.text.text).isEqualTo("1234 5")
        }

        @Test
        fun `Aadhaar offset mapping roundtrips properly`() {
            val transformation = AadhaarVisualTransformation(masked = false)
            val raw = "123456789012"
            val result = transformation.filter(AnnotatedString(raw))
            val mapping = result.offsetMapping

            // 0..12 digits
            for (offset in 0..raw.length) {
                val trans = mapping.originalToTransformed(offset)
                val back = mapping.transformedToOriginal(trans)
                assertThat(back).isEqualTo(offset)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Phone Number Visual Transformation
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Phone Number Visual Transformation")
    inner class PhoneTests {

        private val transformation = PhoneVisualTransformation()

        @Test
        fun `formats 10 digits with country code and 5-5 spacing`() {
            val result = transformation.filter(AnnotatedString("9876543210"))
            assertThat(result.text.text).isEqualTo("+91 98765 43210")
        }

        @Test
        fun `offset mapping maps initial digit after country code prefix`() {
            val raw = "9876543210"
            val result = transformation.filter(AnnotatedString(raw))
            val mapping = result.offsetMapping

            // First digit starts at index 4 (after "+91 ")
            assertThat(mapping.originalToTransformed(0)).isEqualTo(4)
            // 5th digit is at index 10 (after "+91 98765 ")
            assertThat(mapping.originalToTransformed(5)).isEqualTo(10)
            // 6th digit is at index 11
            assertThat(mapping.originalToTransformed(6)).isEqualTo(11)
            // Last digit ends at index 15
            assertThat(mapping.originalToTransformed(10)).isEqualTo(15)

            // Roundtrip check
            for (offset in 0..raw.length) {
                val trans = mapping.originalToTransformed(offset)
                val orig = mapping.transformedToOriginal(trans)
                assertThat(orig).isEqualTo(offset)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Percentage & UTR Visual Transformations
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Percentage & UTR Transformations")
    inner class MiscTransformationsTests {

        @Test
        fun `PercentageVisualTransformation appends per annum suffix`() {
            val transformation = PercentageVisualTransformation()
            val result = transformation.filter(AnnotatedString("14.5"))
            assertThat(result.text.text).isEqualTo("14.5% p.a.")
        }

        @Test
        fun `UtrVisualTransformation strips whitespace and forces uppercase`() {
            val transformation = UtrVisualTransformation()
            val result = transformation.filter(AnnotatedString("upi 1234 abcd 99"))
            assertThat(result.text.text).isEqualTo("UPI1234ABCD99")
        }
    }
}
