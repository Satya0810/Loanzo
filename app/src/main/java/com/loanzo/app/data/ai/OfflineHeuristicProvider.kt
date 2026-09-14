package com.loanzo.app.data.ai

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineHeuristicProvider @Inject constructor() : AiProvider {

    override val name: String = "Loanzo Offline Heuristic Guard"
    override val providerType: String = "OFFLINE"
    override val isEnabled: Boolean = true

    override suspend fun generateResponse(
        prompt: String,
        systemPrompt: String,
        maxTokens: Int,
        temperature: Double
    ): String {
        val p = prompt.lowercase()

        return when {
            p.contains("apply") || p.contains("borrow") || p.contains("need loan") || p.contains("get money") || p.contains("request loan") -> {
                "To apply for a loan on Loanzo:\n" +
                        "1. Ensure your KYC is completed (Profile → KYC Verification).\n" +
                        "2. Tap the '+' button in the bottom navigation bar or visit the Community Marketplace.\n" +
                        "3. Select 'Borrower Request', set your amount, tenure, and purpose.\n" +
                        "4. Verified community lenders will bid on your request with customized rates!\n\n[ACTION:MARKETPLACE] [ACTION:CREATE_POST]"
            }
            p.contains("lend") || p.contains("invest") || p.contains("earn") || p.contains("offer capital") || p.contains("give loan") -> {
                "To lend and earn interest on Loanzo:\n" +
                        "1. Complete your KYC verification.\n" +
                        "2. Go to Community Marketplace and filter by '🤝 Borrowers' to view verified borrower requests.\n" +
                        "3. Inspect borrower trust scores, KYC status, and community vouches, then submit your loan bid.\n" +
                        "4. All rates adhere to legal state usury caps (9%–18% p.a.).\n\n[ACTION:MARKETPLACE]"
            }
            p.contains("emi") || p.contains("calculate") || p.contains("calculator") || p.contains("amortization") -> {
                "You can use Loanzo's interactive Loan Calculator to test principal amounts, interest rates, and tenures.\n" +
                        "• Formula: Standard Reducing Balance EMI\n" +
                        "• Legal caps: Maharashtra (9-12%), Karnataka (14-18%), Delhi/TN (9-12% p.a.).\n" +
                        "Tap below to open the calculator:\n\n[ACTION:CALCULATOR]"
            }
            p.contains("kyc") || p.contains("aadhaar") || p.contains("pan") || p.contains("verify") || p.contains("identity") -> {
                "Loanzo requires a quick 3-step KYC check to protect borrowers and lenders:\n" +
                        "1. Permanent Account Number (PAN) validation.\n" +
                        "2. UIDAI Aadhaar XML / DigiLocker verification with OTP.\n" +
                        "3. On-device facial liveness selfie check.\n" +
                        "Tap below to complete or review your KYC status:\n\n[ACTION:KYC]"
            }
            p.contains("portfolio") || p.contains("my loan") || p.contains("my emi") || p.contains("balance") || p.contains("active loan") -> {
                "You can track your active loans, total capital exposed, next EMI due date, and repayment health in your Smart Portfolio.\n" +
                        "Tap below to view your full portfolio:\n\n[ACTION:PORTFOLIO]"
            }
            p.contains("pay") || p.contains("repay") || p.contains("upi") || p.contains("settle") || p.contains("noc") -> {
                "To make an EMI payment or settle a loan:\n" +
                        "1. Open the loan from your Dashboard or Smart Portfolio.\n" +
                        "2. Pay the lender directly via verified UPI / IMPS (statutory ₹20,000 electronic limit under Sec 269SS).\n" +
                        "3. Enter the UPI transaction UTR reference number to record the payment.\n" +
                        "Full settlement automatically issues a digital No Objection Certificate (NOC)!\n\n[ACTION:PORTFOLIO]"
            }
            p.contains("telegram") || p.contains("bot") || p.contains("alert") || p.contains("reminder") -> {
                "Link your Loanzo account to our Telegram Assistant bot to receive instant push alerts for bids, loan updates, and EMI reminders.\n" +
                        "Tap below to link your Telegram:\n\n[ACTION:TELEGRAM]"
            }
            p.contains("agreement") || p.contains("contract") || p.contains("esign") || p.contains("kfs") -> {
                "Loanzo digital loan contracts are legally binding under the Indian Contract Act, 1872 and IT Act, 2000.\n" +
                        "They feature instant Key Fact Statements (KFS), Aadhaar eSignatures, and SHA-256 vault logs stored in your secure Document Vault."
            }
            p.contains("dispute") || p.contains("mediation") || p.contains("hardship") || p.contains("restructur") -> {
                "If you face distress, Loanzo's Smart Mediation Desk provides fair restructuring under the RBI Fair Practices Code:\n" +
                        "• Moratorium extensions and revised EMI schedules.\n" +
                        "• Zero coercive recovery practices or third-party harassment.\n" +
                        "• Certified local field agent inspection to verify genuine hardship."
            }
            else -> {
                "Hello! I am your Loanzo AI Assistant. I can help you with:\n" +
                        "• 🚀 Applying for a loan or finding borrowers\n" +
                        "• 🧮 Calculating EMIs and interest rates\n" +
                        "• 📊 Checking your active loans & Smart Portfolio\n" +
                        "• 🛡️ KYC verification steps\n" +
                        "• 🤝 How the P2P Marketplace works\n\n[ACTION:MARKETPLACE] [ACTION:CALCULATOR]"
            }
        }
    }
}
