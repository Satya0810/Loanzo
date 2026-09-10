package com.loanzo.app.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

// ═══════════════════════════════════════════════════════════════════════════
// 1. Indian Currency Visual Transformation (₹1,00,000 notation)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Transforms raw numeric string into Indian Rupee notation with '₹' prefix
 * and commas formatted in the Indian grouping system (Lakhs & Crores).
 *
 * Example:
 *  "500"     -> "₹500"
 *  "12000"   -> "₹12,000"
 *  "1500000" -> "₹15,00,000"
 */
class IndianCurrencyVisualTransformation(
    private val prefix: String = "₹"
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.filter { it.isDigit() }
        if (raw.isEmpty()) {
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }

        val formattedNumber = formatIndianNumber(raw)
        val formatted = "$prefix$formattedNumber"

        // Build precise bidirectional offset mapping based on digit positions
        val origToTrans = IntArray(raw.length + 1)
        origToTrans[0] = formatted.indexOfFirst { it.isDigit() }.coerceAtLeast(prefix.length)

        var digitCount = 0
        for (i in formatted.indices) {
            if (formatted[i].isDigit()) {
                digitCount++
                // If immediately followed by a separator (like comma), place cursor after it
                var nextPos = i + 1
                while (nextPos < formatted.length && !formatted[nextPos].isDigit()) {
                    nextPos++
                }
                origToTrans[digitCount] = nextPos
            }
        }
        origToTrans[raw.length] = formatted.length

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, raw.length)
                return origToTrans[clamped]
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, formatted.length)
                // The number of digits in formatted[0 until clamped] is the raw offset
                var digitsBefore = 0
                for (i in 0 until clamped) {
                    if (formatted[i].isDigit()) digitsBefore++
                }
                return digitsBefore.coerceIn(0, raw.length)
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }

    companion object {
        fun formatIndianNumber(digits: String): String {
            if (digits.length <= 3) return digits
            val lastThree = digits.takeLast(3)
            val rest = digits.dropLast(3)
            val sb = StringBuilder()
            var count = 0
            for (i in rest.length - 1 downTo 0) {
                sb.append(rest[i])
                count++
                if (count == 2 && i != 0) {
                    sb.append(',')
                    count = 0
                }
            }
            return sb.reverse().toString() + "," + lastThree
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 2. PAN Card Visual Transformation (Auto-Uppercase & Regex Guide)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Transforms PAN input to uppercase automatically.
 * Full PAN format: [A-Z]{5}[0-9]{4}[A-Z]{1} (e.g. ABCDE1234F).
 */
class PanVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val uppercase = text.text.take(10).uppercase(Locale.ROOT)
        return TransformedText(AnnotatedString(uppercase), OffsetMapping.Identity)
    }

    companion object {
        val PAN_REGEX = Regex("^[A-Z]{5}[0-9]{4}[A-Z]{1}$")

        fun isValidPan(pan: String): Boolean {
            return pan.trim().matches(PAN_REGEX)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 3. Aadhaar Visual Transformation (4-4-4 Spacing with Mask option)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Formats 12-digit Aadhaar input into 4-4-4 spacing: "1234 5678 9012".
 * If [masked] is true, obscures first 8 digits: "•••• •••• 9012".
 */
class AadhaarVisualTransformation(
    private val masked: Boolean = false
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.filter { it.isDigit() }.take(12)
        if (raw.isEmpty()) {
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }

        val formattedBuilder = StringBuilder()
        for (i in raw.indices) {
            if (i > 0 && i % 4 == 0) {
                formattedBuilder.append(' ')
            }
            if (masked && i < 8) {
                formattedBuilder.append('•')
            } else {
                formattedBuilder.append(raw[i])
            }
        }
        val formatted = formattedBuilder.toString()

        val origToTrans = IntArray(raw.length + 1)
        val transToOrig = IntArray(formatted.length + 1)

        var rawIdx = 0
        for (i in formatted.indices) {
            val char = formatted[i]
            if (char != ' ') {
                origToTrans[rawIdx] = i
                transToOrig[i] = rawIdx
                rawIdx++
            } else {
                transToOrig[i] = rawIdx
            }
        }
        origToTrans[raw.length] = formatted.length
        transToOrig[formatted.length] = raw.length

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return origToTrans[offset.coerceIn(0, raw.length)]
            }

            override fun transformedToOriginal(offset: Int): Int {
                return transToOrig[offset.coerceIn(0, formatted.length)]
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }

    companion object {
        /**
         * Validates 12-digit Aadhaar using Verhoeff Checksum Algorithm.
         */
        fun isValidAadhaar(aadhaar: String): Boolean {
            val digits = aadhaar.filter { it.isDigit() }
            if (digits.length != 12) return false
            return validateVerhoeff(digits)
        }

        private val d = arrayOf(
            intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
            intArrayOf(1, 2, 3, 4, 0, 6, 7, 8, 9, 5),
            intArrayOf(2, 3, 4, 0, 1, 7, 8, 9, 5, 6),
            intArrayOf(3, 4, 0, 1, 2, 8, 9, 5, 6, 7),
            intArrayOf(4, 0, 1, 2, 3, 9, 5, 6, 7, 8),
            intArrayOf(5, 9, 8, 7, 6, 0, 4, 3, 2, 1),
            intArrayOf(6, 5, 9, 8, 7, 1, 0, 4, 3, 2),
            intArrayOf(7, 6, 5, 9, 8, 2, 1, 0, 4, 3),
            intArrayOf(8, 7, 6, 5, 9, 3, 2, 1, 0, 4),
            intArrayOf(9, 8, 7, 6, 5, 4, 3, 2, 1, 0)
        )

        private val p = arrayOf(
            intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
            intArrayOf(1, 5, 7, 6, 2, 8, 3, 0, 9, 4),
            intArrayOf(5, 8, 0, 3, 7, 9, 6, 1, 4, 2),
            intArrayOf(8, 9, 1, 6, 0, 4, 3, 5, 2, 7),
            intArrayOf(9, 4, 5, 3, 1, 2, 6, 8, 7, 0),
            intArrayOf(4, 2, 8, 6, 5, 7, 3, 9, 0, 1),
            intArrayOf(2, 7, 9, 3, 8, 0, 6, 4, 1, 5),
            intArrayOf(7, 0, 4, 6, 9, 1, 3, 2, 5, 8)
        )

        private fun validateVerhoeff(num: String): Boolean {
            var c = 0
            val reversed = num.reversed()
            for (i in reversed.indices) {
                val digit = reversed[i] - '0'
                c = d[c][p[i % 8][digit]]
            }
            return c == 0
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 4. Phone Number Visual Transformation (+91 prefix & 5-5 spacing)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Formats 10-digit Indian mobile number into: "+91 98765 43210".
 */
class PhoneVisualTransformation(
    private val countryCode: String = "+91 "
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.filter { it.isDigit() }.take(10)
        if (raw.isEmpty()) {
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }

        val formattedBuilder = StringBuilder(countryCode)
        for (i in raw.indices) {
            if (i == 5) formattedBuilder.append(' ')
            formattedBuilder.append(raw[i])
        }
        val formatted = formattedBuilder.toString()

        val origToTrans = IntArray(raw.length + 1)
        origToTrans[0] = countryCode.length

        var digitCount = 0
        for (i in countryCode.length until formatted.length) {
            if (formatted[i].isDigit()) {
                digitCount++
                var nextPos = i + 1
                while (nextPos < formatted.length && !formatted[nextPos].isDigit()) {
                    nextPos++
                }
                origToTrans[digitCount] = nextPos
            }
        }
        origToTrans[raw.length] = formatted.length

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return origToTrans[offset.coerceIn(0, raw.length)]
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, formatted.length)
                if (clamped <= countryCode.length) return 0
                var digitsBefore = 0
                for (i in countryCode.length until clamped) {
                    if (formatted[i].isDigit()) digitsBefore++
                }
                return digitsBefore.coerceIn(0, raw.length)
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 5. Percentage & Interest Rate Visual Transformation (% p.a. suffix)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Appends '% p.a.' suffix to rate inputs without disrupting decimal cursor navigation.
 * Example: "12" -> "12% p.a.", "14.5" -> "14.5% p.a."
 */
class PercentageVisualTransformation(
    private val suffix: String = "% p.a."
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }

        val formatted = "$raw$suffix"

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return offset.coerceIn(0, raw.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                return offset.coerceIn(0, raw.length)
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 6. UTR & Transaction Reference Visual Transformation
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Forces uppercase and cleans transaction reference inputs for UPI (12 digits),
 * NEFT (16 chars), and RTGS (22 chars).
 */
class UtrVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val uppercase = text.text.filter { it.isLetterOrDigit() }.uppercase(Locale.ROOT)
        return TransformedText(AnnotatedString(uppercase), OffsetMapping.Identity)
    }
}
