package com.loanzo.app.data.ai

/**
 * Standardized data models and contract for all AI model providers.
 */
data class AiMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

sealed class AiRaceResult {
    data class Success(
        val providerName: String,
        val providerType: String,
        val content: String,
        val latencyMs: Long,
        val modelUsed: String
    ) : AiRaceResult()

    data class Failure(
        val errors: Map<String, String>,
        val fallbackContent: String
    ) : AiRaceResult()
}

interface AiProvider {
    val name: String
    val providerType: String
    val isEnabled: Boolean

    /**
     * Executes the AI query asynchronously.
     * Must be cooperative with coroutine cancellation.
     */
    suspend fun generateResponse(
        prompt: String,
        systemPrompt: String = "",
        maxTokens: Int = 1024,
        temperature: Double = 0.7
    ): String?
}
