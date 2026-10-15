package com.loanzo.app.data.voice

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class VoiceTextSanitizerTest {

    @Test
    @DisplayName("cleanForSpeech strips action tags cleanly")
    fun cleanForSpeechStripsActionTagsCleanly() {
        val raw = "Your next EMI of ₹9,200 is due on October 5th. [ACTION:PORTFOLIO] [ACTION:CALCULATOR]"
        val cleaned = VoiceTextSanitizer.cleanForSpeech(raw)

        assertThat(cleaned).doesNotContain("[ACTION:")
        assertThat(cleaned).contains("9,200 rupees")
        assertThat(cleaned).contains("E M I")
    }

    @Test
    @DisplayName("cleanForSpeech converts financial abbreviations and symbols")
    fun cleanForSpeechConvertsFinancialAbbreviationsAndSymbols() {
        val raw = "The interest rate is 12% p.a. for your loan under RBI and NBFC guidelines."
        val cleaned = VoiceTextSanitizer.cleanForSpeech(raw)

        assertThat(cleaned).contains("12 percent per annum")
        assertThat(cleaned).contains("R B I")
        assertThat(cleaned).contains("N B F C")
    }

    @Test
    @DisplayName("cleanForSpeech removes markdown formatting and headers")
    fun cleanForSpeechRemovesMarkdownFormattingAndHeaders() {
        val raw = """
            ### Important Notice
            • **Step 1**: Complete your *KYC* verification.
            • **Step 2**: Pay via UPI.
        """.trimIndent()
        val cleaned = VoiceTextSanitizer.cleanForSpeech(raw)

        assertThat(cleaned).doesNotContain("###")
        assertThat(cleaned).doesNotContain("**")
        assertThat(cleaned).doesNotContain("*")
        assertThat(cleaned).doesNotContain("•")
        assertThat(cleaned).contains("Step 1: Complete your K Y C verification")
        assertThat(cleaned).contains("Step 2: Pay via U P I")
    }

    @Test
    @DisplayName("extractActions parses all action types properly")
    fun extractActionsParsesAllActionTypesProperly() {
        val raw = "Here are your options: [ACTION:CALCULATOR] and [ACTION:KYC] and [ACTION:PORTFOLIO]."
        val actions = VoiceTextSanitizer.extractActions(raw)

        assertThat(actions).containsExactly("CALCULATOR", "KYC", "PORTFOLIO").inOrder()
    }

    @Test
    @DisplayName("cleanForSpeech handles decimal currencies and Rs prefix")
    fun cleanForSpeechHandlesDecimalCurrenciesAndRsPrefix() {
        val raw = "Monthly payment is Rs. 4,500.50 at ₹1,200.00 p.m. with Golden NOC."
        val cleaned = VoiceTextSanitizer.cleanForSpeech(raw)

        assertThat(cleaned).contains("4,500.50 rupees")
        assertThat(cleaned).contains("1,200.00 rupees")
        assertThat(cleaned).contains("per month")
        assertThat(cleaned).contains("N O C")
    }

    @Test
    @DisplayName("cleanForSpeech strips emojis and backtick code snippets")
    fun cleanForSpeechStripsEmojisAndBacktickCodeSnippets() {
        val raw = "🎉 Congratulations! Your active loan balance is `₹25,000` 🚀"
        val cleaned = VoiceTextSanitizer.cleanForSpeech(raw)

        assertThat(cleaned).doesNotContain("🎉")
        assertThat(cleaned).doesNotContain("🚀")
        assertThat(cleaned).doesNotContain("`")
        assertThat(cleaned).contains("Congratulations! Your active loan balance is 25,000 rupees")
    }
}
