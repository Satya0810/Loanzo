package com.loanzo.app.data.ai

import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MultiAiRaceEngineTest {

    private val llm7Provider = mockk<Llm7Provider>()
    private val sambaNovaProvider = mockk<SambaNovaProvider>()
    private val cloudflareAiProvider = mockk<CloudflareAiProvider>()
    private val offlineHeuristicProvider = mockk<OfflineHeuristicProvider>()

    private lateinit var raceEngine: MultiAiRaceEngine

    @BeforeEach
    fun setUp() {
        clearAllMocks()

        every { llm7Provider.name } returns "LLM7.io Fast Engine"
        every { llm7Provider.providerType } returns "LLM7"
        every { llm7Provider.isEnabled } returns true

        every { sambaNovaProvider.name } returns "SambaNova AI Engine"
        every { sambaNovaProvider.providerType } returns "SAMBANOVA"
        every { sambaNovaProvider.isEnabled } returns true

        every { cloudflareAiProvider.name } returns "Cloudflare Workers AI"
        every { cloudflareAiProvider.providerType } returns "CLOUDFLARE"
        every { cloudflareAiProvider.isEnabled } returns true

        every { offlineHeuristicProvider.name } returns "Loanzo Offline Heuristic Guard"
        every { offlineHeuristicProvider.providerType } returns "OFFLINE"
        every { offlineHeuristicProvider.isEnabled } returns true
        coEvery { offlineHeuristicProvider.generateResponse(any(), any(), any(), any()) } returns "Offline fallback guidance."

        raceEngine = MultiAiRaceEngine(
            llm7Provider = llm7Provider,
            sambaNovaProvider = sambaNovaProvider,
            cloudflareAiProvider = cloudflareAiProvider,
            offlineHeuristicProvider = offlineHeuristicProvider
        )
    }

    @Nested
    @DisplayName("Concurrent Multi-AI Racing")
    inner class RacingTests {

        @Test
        fun `fastest provider wins the race and returns valid response`() = runTest {
            coEvery { llm7Provider.generateResponse(any(), any(), any(), any()) } coAnswers {
                delay(30)
                "LLM7 ultra fast response"
            }
            coEvery { sambaNovaProvider.generateResponse(any(), any(), any(), any()) } coAnswers {
                delay(150)
                "SambaNova slower response"
            }
            coEvery { cloudflareAiProvider.generateResponse(any(), any(), any(), any()) } coAnswers {
                delay(200)
                "Cloudflare response"
            }

            val result = raceEngine.raceQuery("Explain interest rate caps")

            assertThat(result).isInstanceOf(AiRaceResult.Success::class.java)
            val success = result as AiRaceResult.Success
            assertThat(success.content).isEqualTo("LLM7 ultra fast response")
            assertThat(success.providerName).isEqualTo("LLM7.io Fast Engine")
            assertThat(success.providerType).isEqualTo("LLM7")
        }

        @Test
        fun `when fastest provider fails with error second fastest provider takes victory`() = runTest {
            coEvery { llm7Provider.generateResponse(any(), any(), any(), any()) } coAnswers {
                delay(10)
                throw RuntimeException("HTTP 402: Insufficient balance")
            }
            coEvery { sambaNovaProvider.generateResponse(any(), any(), any(), any()) } coAnswers {
                delay(50)
                "SambaNova successful recovery answer"
            }
            coEvery { cloudflareAiProvider.generateResponse(any(), any(), any(), any()) } coAnswers {
                delay(150)
                "Cloudflare slower answer"
            }

            val result = raceEngine.raceQuery("Check usury limits")

            assertThat(result).isInstanceOf(AiRaceResult.Success::class.java)
            val success = result as AiRaceResult.Success
            assertThat(success.content).isEqualTo("SambaNova successful recovery answer")
            assertThat(success.providerName).isEqualTo("SambaNova AI Engine")
        }

        @Test
        fun `when all providers fail engine falls back to offline heuristic rulebook`() = runTest {
            coEvery { llm7Provider.generateResponse(any(), any(), any(), any()) } coAnswers {
                throw RuntimeException("Network unreachable")
            }
            coEvery { sambaNovaProvider.generateResponse(any(), any(), any(), any()) } coAnswers {
                throw RuntimeException("HTTP 429 Too Many Requests")
            }
            coEvery { cloudflareAiProvider.generateResponse(any(), any(), any(), any()) } coAnswers {
                throw RuntimeException("504 Gateway Timeout")
            }

            val result = raceEngine.raceQuery("How does Section 4 Promissory Note work?")

            assertThat(result).isInstanceOf(AiRaceResult.Failure::class.java)
            val failure = result as AiRaceResult.Failure
            assertThat(failure.fallbackContent).isEqualTo("Offline fallback guidance.")
            assertThat(failure.errors).hasSize(3)
        }

        @Test
        fun `when no remote providers are enabled immediately returns offline heuristic`() = runTest {
            every { llm7Provider.isEnabled } returns false
            every { sambaNovaProvider.isEnabled } returns false
            every { cloudflareAiProvider.isEnabled } returns false

            val result = raceEngine.raceQuery("What is Section 269SS limit?")

            assertThat(result).isInstanceOf(AiRaceResult.Success::class.java)
            val success = result as AiRaceResult.Success
            assertThat(success.providerType).isEqualTo("OFFLINE")
            assertThat(success.content).isEqualTo("Offline fallback guidance.")
        }
    }

    @Nested
    @DisplayName("Domain AI Helpers")
    inner class DomainHelperTests {

        @Test
        fun `simplifyLegalAgreement returns plain bullet points`() = runTest {
            coEvery { llm7Provider.generateResponse(any(), any(), any(), any()) } returns
                    "• Total ₹50,000 across 6 months.\n• 3-day penalty grace.\n• Legally enforceable."

            val explanation = raceEngine.simplifyLegalAgreement("Clause 1: The borrower covenants...")

            assertThat(explanation).contains("Total ₹50,000")
            assertThat(explanation).contains("Legally enforceable")
        }

        @Test
        fun `enhanceMarketplacePitchResult returns race success with provider metadata`() = runTest {
            coEvery { llm7Provider.generateResponse(any(), any(), any(), any()) } returns
                    "Professional loan request: Urgently expanding grocery inventory with steady daily UPI receipts."

            val result = raceEngine.enhanceMarketplacePitchResult(
                rawDraft = "need 50k for shop",
                category = "BUSINESS",
                amount = 50000.0,
                tenureMonths = 6
            )

            assertThat(result).isInstanceOf(AiRaceResult.Success::class.java)
            val success = result as AiRaceResult.Success
            assertThat(success.content).contains("Professional loan request")
            assertThat(success.providerType).isEqualTo("LLM7")
        }

        @Test
        fun `generatePdfDossierSummary returns executive credit appraisal`() = runTest {
            coEvery { llm7Provider.generateResponse(any(), any(), any(), any()) } returns
                    "Appraisal: Low-risk prime candidate with consistent 100% on-time repayment history across all bilateral tranches."

            val summary = raceEngine.generatePdfDossierSummary(
                userName = "Rahul Sharma",
                userRole = "BORROWER",
                totalLent = 0.0,
                totalBorrowed = 40000.0,
                kycStatus = "VERIFIED"
            )

            assertThat(summary).contains("Low-risk prime candidate")
        }

        @Test
        fun `recommendMediationRestructuring generates fair workout plan`() = runTest {
            coEvery { llm7Provider.generateResponse(any(), any(), any(), any()) } returns
                    "Recommended: 60-day principal moratorium followed by 3-month tenure extension at original interest rate."

            val plan = raceEngine.recommendMediationRestructuring(
                loanId = "loan_abc",
                principal = 100000.0,
                tenure = 12,
                hardshipReason = "Medical emergency hospital bills"
            )

            assertThat(plan).contains("moratorium")
        }
    }
}
