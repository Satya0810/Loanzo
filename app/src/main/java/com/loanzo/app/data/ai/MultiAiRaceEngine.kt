package com.loanzo.app.data.ai

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MultiAiRaceEngine @Inject constructor(
    private val llm7Provider: Llm7Provider,
    private val sambaNovaProvider: SambaNovaProvider,
    private val cloudflareAiProvider: CloudflareAiProvider,
    private val offlineHeuristicProvider: OfflineHeuristicProvider
) {
    companion object {
        private const val TAG = "MultiAiRaceEngine"
    }

    /**
     * Simultaneously dispatches requests across all 3 AI providers in parallel.
     * Takes the first valid (non-null, non-blank) response and immediately cancels the others.
     * If all 3 fail or time out, falls back to the deterministic OfflineHeuristicProvider.
     */
    suspend fun raceQuery(
        prompt: String,
        systemPrompt: String = "",
        maxTokens: Int = 1024,
        temperature: Double = 0.7,
        timeoutMs: Long = 8000L
    ): AiRaceResult = withContext(Dispatchers.IO) {
        val activeProviders = listOf(llm7Provider, sambaNovaProvider, cloudflareAiProvider).filter { it.isEnabled }

        if (activeProviders.isEmpty()) {
            val fallback = offlineHeuristicProvider.generateResponse(prompt, systemPrompt)
            return@withContext AiRaceResult.Success(
                providerName = offlineHeuristicProvider.name,
                providerType = offlineHeuristicProvider.providerType,
                content = fallback,
                latencyMs = 0L,
                modelUsed = "offline-rules-v1"
            )
        }

        val channel = Channel<AiRaceResult.Success>(Channel.BUFFERED)
        val errors = java.util.concurrent.ConcurrentHashMap<String, String>()
        val hasWon = AtomicBoolean(false)
        val completedCount = AtomicInteger(0)
        val totalProviders = activeProviders.size

        val jobs = activeProviders.map { provider ->
            launch {
                val start = System.currentTimeMillis()
                try {
                    Log.d(TAG, "🏁 Racing dispatched to: ${provider.name}")
                    val response = provider.generateResponse(prompt, systemPrompt, maxTokens, temperature)
                    val elapsed = System.currentTimeMillis() - start

                    if (!response.isNullOrBlank() && hasWon.compareAndSet(false, true)) {
                        Log.d(TAG, "⚡ WINNER: ${provider.name} responded first in ${elapsed}ms!")
                        channel.send(
                            AiRaceResult.Success(
                                providerName = provider.name,
                                providerType = provider.providerType,
                                content = response,
                                latencyMs = elapsed,
                                modelUsed = provider.name
                            )
                        )
                    } else if (response.isNullOrBlank()) {
                        errors[provider.name] = "Empty response"
                    }
                } catch (e: Exception) {
                    val elapsed = System.currentTimeMillis() - start
                    Log.w(TAG, "${provider.name} failed after ${elapsed}ms: ${e.message}")
                    errors[provider.name] = e.message ?: "Unknown error"
                } finally {
                    if (completedCount.incrementAndGet() >= totalProviders && !hasWon.get()) {
                        channel.close()
                    }
                }
            }
        }

        // Await the winner or timeout
        val winner = withTimeoutOrNull(timeoutMs) {
            try {
                channel.receiveCatching().getOrNull()
            } catch (e: Exception) {
                null
            }
        }

        // Cancel all remaining slower/pending provider coroutines
        jobs.forEach { job ->
            if (job.isActive) {
                job.cancel()
            }
        }
        channel.close()

        if (winner != null) {
            return@withContext winner
        }

        // If no provider won within timeout or all errored, execute offline heuristic guard
        Log.w(TAG, "All remote AI providers failed or timed out. Falling back to offline heuristics.")
        val fallback = offlineHeuristicProvider.generateResponse(prompt, systemPrompt)
        AiRaceResult.Failure(
            errors = errors,
            fallbackContent = fallback
        )
    }

    // ==========================================
    // SPECIALIZED APPLICATION-WIDE DOMAIN API
    // ==========================================

    /**
     * 1. App-Grounded Chatbot & Financial Copilot
     */
    suspend fun generateChatResponse(
        userPrompt: String,
        userContext: String = "",
        conversationHistory: List<AiMessage> = emptyList()
    ): AiRaceResult {
        val systemPrompt = """
You are "Loanzo Assistant", the official, friendly, and expert in-app AI copilot for Loanzo — India's premier peer-to-peer (P2P) social lending community platform.

### CORE PURPOSE & IDENTITY:
- You help everyday Indian borrowers, lenders, and certified field agents navigate and use the Loanzo app safely and effectively.
- You are strictly grounded in Loanzo's features, Indian legal compliance (Money Lenders Acts usury rate caps 9%-18% p.a., Sections 269SS/269T ₹20,000 cash transaction limits, RBI digital lending guidelines), and the user's live account context.
- Keep answers warm, concise (2-4 sentences or crisp bullet points), encouraging, and free of unnecessary legalese.

### COMPREHENSIVE LOANZO APP WORKFLOWS & FEATURES:
1. Community Marketplace:
   - "Borrowers" tab: Borrowers post loan requests specifying purpose, amount, and preferred tenure.
   - "Lenders" tab: Capital providers publish available lending pools with target return rates.
   - Proposing Bids: Lenders bid on borrower requests by offering amount, rate, and tenure.
   - Social Vouches: Community members vouch for each other to boost credit trust scores.
2. Smart Portfolio & Dashboard:
   - Real-time tracking of total capital lent, total borrowed, active loans, and repayment health score.
   - Accessed from Dashboard → "View Smart Portfolio".
3. Loan Calculator:
   - Interactive EMI tool for testing principal, interest rate, and tenure with monthly amortization schedules.
4. KYC Verification:
   - Mandatory for lending or borrowing: 3-step check via PAN validation, Aadhaar OTP XML, and on-device facial liveness check (Profile → KYC).
5. Certified Agent Field Inspection:
   - Certified local agents perform physical asset appraisals (gold, vehicle, business) and document verification.
6. Digital Contracts & KFS:
   - Instant Key Fact Statements (KFS) and legally binding agreements under the Indian Contract Act, 1872, with Aadhaar eSign and SHA-256 vault logs.
7. UPI Repayments & NOC:
   - Direct peer repayment via UPI/IMPS; user enters UTR reference number. Full payoff auto-generates a digital No Objection Certificate (NOC).
8. Smart Mediation Desk & Support:
   - Built-in dispute mediation under RBI Fair Practices Code for hardship restructuring (moratoriums, tenure extension).

### ACTION PILL TAGS:
Whenever you recommend an action or screen in the app, append one or more of these action tags at the very end of your response so the user gets interactive tap buttons:
- [ACTION:CALCULATOR] -> If user wants to calculate EMI or interest.
- [ACTION:KYC] -> If user needs to verify or check KYC.
- [ACTION:MARKETPLACE] -> If user wants to browse or post offers/requests.
- [ACTION:PORTFOLIO] -> If user asks about their overall exposure, returns, or portfolio.
- [ACTION:CREATE_POST] -> If user wants to create a new loan listing or offer.
- [ACTION:TELEGRAM] -> If user wants real-time Telegram bot alerts.

${if (userContext.isNotBlank()) "### USER LIVE ACCOUNT CONTEXT:\n$userContext\n" else ""}
""".trimIndent()

        // Assemble full prompt with conversation history if available
        val fullPrompt = if (conversationHistory.isNotEmpty()) {
            val historySnippet = conversationHistory.takeLast(6).joinToString("\n") { msg ->
                val speaker = if (msg.role == "user") "User" else "Loanzo Assistant"
                "$speaker: ${msg.content}"
            }
            "Previous conversation:\n$historySnippet\n\nUser: $userPrompt\nLoanzo Assistant:"
        } else {
            userPrompt
        }

        return raceQuery(fullPrompt, systemPrompt)
    }

    /**
     * 2. Making of PDF & Legal Dossiers: Credit Risk & Behavioral Appraisal
     */
    suspend fun generatePdfDossierSummary(
        userName: String,
        userRole: String,
        totalLent: Double,
        totalBorrowed: Double,
        kycStatus: String
    ): String {
        val prompt = "Generate a concise 3-sentence executive creditworthiness appraisal for $userName ($userRole). " +
                "KYC Status: $kycStatus. Total portfolio lent: INR ${totalLent.toInt()}, total borrowed: INR ${totalBorrowed.toInt()}. " +
                "Highlight repayment discipline, risk rating (Low/Medium), and legal suitability for P2P contracts."

        val systemPrompt = "You are a professional credit risk underwriter for Indian banking and legal dossiers. Be factual, concise, and executive."
        val result = raceQuery(prompt, systemPrompt, maxTokens = 250, timeoutMs = 7000L)
        return when (result) {
            is AiRaceResult.Success -> result.content
            is AiRaceResult.Failure -> result.fallbackContent
        }
    }

    /**
     * 3. Loan Agreements & KFS: Legalese to Plain Language Simplifier
     */
    suspend fun simplifyLegalAgreementResult(
        clauseText: String,
        targetLanguage: String = "en"
    ): AiRaceResult {
        val prompt = "Convert this legal loan agreement clause into 3 clear, easy-to-understand bullet points for an everyday Indian borrower/lender:\n\"$clauseText\""
        val systemPrompt = "You are a legal transparency simplifier. Output exactly 3 bullet points in clear language without jargon."
        return raceQuery(prompt, systemPrompt, maxTokens = 350, timeoutMs = 7000L)
    }

    suspend fun simplifyLegalAgreement(
        clauseText: String,
        targetLanguage: String = "en"
    ): String {
        return when (val result = simplifyLegalAgreementResult(clauseText, targetLanguage)) {
            is AiRaceResult.Success -> result.content
            is AiRaceResult.Failure -> result.fallbackContent
        }
    }

    /**
     * 4. P2P Marketplace: Enhance Borrower Loan Request Pitch
     */
    suspend fun enhanceMarketplacePitchResult(
        rawDraft: String,
        category: String,
        amount: Double,
        tenureMonths: Int
    ): AiRaceResult {
        val prompt = "Enhance this borrower's loan pitch for an Indian P2P marketplace:\n" +
                "Raw note: \"$rawDraft\"\n" +
                "Loan Amount: ₹${amount.toInt()}, Tenure: $tenureMonths Months, Category: $category.\n" +
                "Write a compelling, professional, 2-3 paragraph listing description highlighting purpose, repayment capability, and transparency."

        val systemPrompt = "You are a fintech copywriter helping micro-entrepreneurs and students present genuine, creditworthy loan requests."
        return raceQuery(prompt, systemPrompt, maxTokens = 500, timeoutMs = 7000L)
    }

    suspend fun enhanceMarketplacePitch(
        rawDraft: String,
        category: String,
        amount: Double,
        tenureMonths: Int
    ): String {
        return when (val result = enhanceMarketplacePitchResult(rawDraft, category, amount, tenureMonths)) {
            is AiRaceResult.Success -> result.content
            is AiRaceResult.Failure -> result.fallbackContent
        }
    }

    /**
     * 5. Dispute Mediation: Restructuring & Settlement Recommendation
     */
    suspend fun recommendMediationRestructuring(
        loanId: String,
        principal: Double,
        tenure: Int,
        hardshipReason: String
    ): String {
        val prompt = "A borrower under loan $loanId (₹${principal.toInt()} for $tenure months) is facing distress due to: \"$hardshipReason\". " +
                "Propose a fair restructuring plan under RBI Fair Practices Code (e.g. 2-month moratorium, tenure extension, or reduced penal rate) that balances lender recovery with borrower viability."

        val systemPrompt = "You are an independent commercial mediator for debt restructuring in India."
        val result = raceQuery(prompt, systemPrompt, maxTokens = 400, timeoutMs = 7000L)
        return when (result) {
            is AiRaceResult.Success -> result.content
            is AiRaceResult.Failure -> result.fallbackContent
        }
    }
}
