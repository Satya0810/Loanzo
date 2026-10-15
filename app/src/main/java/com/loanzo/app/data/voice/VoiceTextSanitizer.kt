package com.loanzo.app.data.voice

/**
 * Sanitizes raw LLM output text for natural, conversational speech synthesis via Android TextToSpeech.
 * Strips markdown symbols, converts financial abbreviations into spoken terms, and extracts action tags.
 */
object VoiceTextSanitizer {

    private val ACTION_TAG_REGEX = Regex("\\[ACTION:[A-Z_]+\\]")
    private val MARKDOWN_BOLD_REGEX = Regex("\\*\\*(.*?)\\*\\*")
    private val MARKDOWN_ITALIC_REGEX = Regex("\\*(.*?)\\*")
    private val MARKDOWN_HEADER_REGEX = Regex("#{1,6}\\s*")
    private val BULLET_POINT_REGEX = Regex("^[\\s•\\-\\*]+\\s*", RegexOption.MULTILINE)
    private val LINK_REGEX = Regex("\\[(.*?)\\]\\(.*?\\)")

    /**
     * Cleans raw text into conversational spoken prose.
     * E.g. "₹50,000 at 12% p.a. [ACTION:CALCULATOR]" -> "50,000 rupees at 12 percent per annum."
     */
    fun cleanForSpeech(rawText: String): String {
        if (rawText.isBlank()) return ""

        var cleaned = rawText

        // 1. Remove all [ACTION:...] tags
        cleaned = cleaned.replace(ACTION_TAG_REGEX, "")

        // 2. Remove markdown links, keeping label: [Label](url) -> Label
        cleaned = cleaned.replace(LINK_REGEX, "$1")

        // 3. Remove markdown headers (### Header -> Header)
        cleaned = cleaned.replace(MARKDOWN_HEADER_REGEX, "")

        // 4. Remove bold, italic, and code formatting
        cleaned = cleaned.replace(MARKDOWN_BOLD_REGEX, "$1")
        cleaned = cleaned.replace(MARKDOWN_ITALIC_REGEX, "$1")
        cleaned = cleaned.replace("`", "")

        // 5. Replace bullet points and blockquotes with gentle spacing
        cleaned = cleaned.replace(BULLET_POINT_REGEX, "")
        cleaned = cleaned.replace(Regex("^[\\s>]+", RegexOption.MULTILINE), "")

        // 6. Strip emojis and miscellaneous symbols so TTS doesn't read symbol names aloud
        cleaned = cleaned.replace(Regex("[\\p{So}\\p{Cn}\\uD83C-\\uDBFF\\uDC00-\\uDFFF]"), "")

        // 7. Convert Indian financial symbols and abbreviations to spoken words
        cleaned = cleaned.replace(Regex("₹\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"), "$1 rupees")
        cleaned = cleaned.replace(Regex("(?i)INR\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"), "$1 rupees")
        cleaned = cleaned.replace(Regex("(?i)\\bRs\\.?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"), "$1 rupees")
        cleaned = cleaned.replace("₹", " rupees ")
        cleaned = cleaned.replace("INR", " rupees ")
        cleaned = cleaned.replace(Regex("(?i)\\bRs\\.?\\b"), " rupees ")
        cleaned = cleaned.replace(Regex("(?i)\\bp\\.a\\.?"), "per annum")
        cleaned = cleaned.replace(Regex("(?i)\\bp\\.m\\.?"), "per month")
        cleaned = cleaned.replace(Regex("(?i)\\b([0-9]+)\\s*mos?\\b"), "$1 months")
        cleaned = cleaned.replace(Regex("(?i)\\bEMI\\b"), "E M I")
        cleaned = cleaned.replace(Regex("(?i)\\bKYC\\b"), "K Y C")
        cleaned = cleaned.replace(Regex("(?i)\\bUPI\\b"), "U P I")
        cleaned = cleaned.replace(Regex("(?i)\\bP2P\\b"), "peer to peer")
        cleaned = cleaned.replace(Regex("(?i)\\bNBFC\\b"), "N B F C")
        cleaned = cleaned.replace(Regex("(?i)\\bRBI\\b"), "R B I")
        cleaned = cleaned.replace(Regex("(?i)\\bKFS\\b"), "Key Fact Statement")
        cleaned = cleaned.replace(Regex("(?i)\\bAPR\\b"), "A P R")
        cleaned = cleaned.replace(Regex("(?i)\\bNOC\\b"), "N O C")
        cleaned = cleaned.replace("%", " percent")

        // 8. Clean up multiple newlines or spaces into single spaces
        cleaned = cleaned.replace(Regex("\\s+"), " ").trim()

        return cleaned
    }

    /**
     * Cleans raw text for on-screen display by stripping [ACTION:...] tags
     * while preserving markdown formatting, currency symbols, and readability.
     */
    fun cleanForDisplay(rawText: String): String {
        if (rawText.isBlank()) return ""
        return rawText.replace(ACTION_TAG_REGEX, "").trim()
    }

    /**
     * Extracts all raw action tags (e.g. listOf("CALCULATOR", "KYC")) for UI deep links.
     */
    fun extractActions(rawText: String): List<String> {
        return ACTION_TAG_REGEX.findAll(rawText).map { match ->
            match.value.removePrefix("[ACTION:").removeSuffix("]")
        }.toList()
    }
}
