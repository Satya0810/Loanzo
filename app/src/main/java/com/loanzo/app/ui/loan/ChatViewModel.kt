package com.loanzo.app.ui.loan

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.loanzo.app.data.dao.LoanDao
import com.loanzo.app.data.dao.UserDao
import com.loanzo.app.data.entity.LoanEntity
import com.loanzo.app.data.entity.UserEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class FirestoreChatMessage(
    val messageId: String = "",
    val channelId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "MEMBER",
    val recipientId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isMe: Boolean = false,
    val status: String = "DELIVERED", // "SENT", "DELIVERED", "READ"
    val messageType: String = "TEXT", // "TEXT", "PROPOSAL", "RECEIPT", "INSPECTION"
    val metaPayload: String? = null,
    // Translation support
    val translatedText: String? = null,
    val isTranslating: Boolean = false
)

data class ChatConversationSummary(
    val channelId: String,
    val channelType: String, // "LOAN", "DIRECT", "SUPPORT"
    val targetUserId: String,
    val targetUserName: String,
    val targetUserRole: String,
    val targetUserPhone: String,
    val targetUserKycStatus: String,
    val loanId: String? = null,
    val loanTitle: String? = null,
    val loanPrincipal: Double? = null,
    val lastMessage: String = "",
    val lastTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isOnline: Boolean = true
)

data class ChatUiState(
    val conversations: List<ChatConversationSummary> = emptyList(),
    val filteredConversations: List<ChatConversationSummary> = emptyList(),
    val selectedFilter: String = "ALL", // "ALL", "DEALS", "DIRECT", "SUPPORT"
    val searchQuery: String = "",
    val searchUserResults: List<UserEntity> = emptyList(),
    val isSearchingUsers: Boolean = false,

    // Active Conversation
    val activeChannelId: String = "",
    val activeCounterparty: UserEntity? = null,
    val activeLoan: LoanEntity? = null,
    val messages: List<FirestoreChatMessage> = emptyList(),

    val isLoading: Boolean = false,
    val error: String? = null,
    val currentUserId: String = "",
    val currentUserName: String = "",
    val currentUserRole: String = "MEMBER"
)


object DemoChatMessages {
    fun getDemoMessagesForChannel(channelId: String, currentUserId: String, counterpartyName: String): List<FirestoreChatMessage> {
        val now = System.currentTimeMillis()
        val oneHourMs = 3600_000L
        val oneDayMs = 86400_000L
        val lowerCh = channelId.lowercase()
        val lowerCp = counterpartyName.lowercase()

        return when {
            // =========================================================================
            // 1. LOAN DEALS
            // =========================================================================
            lowerCh.contains("demo_loan_lent_1") || lowerCh.contains("lent") || lowerCh.contains("50k") -> listOf(
                FirestoreChatMessage(
                    messageId = "demo_msg_lent_1",
                    channelId = channelId,
                    senderId = currentUserId,
                    senderName = "You",
                    senderRole = "LENDER",
                    recipientId = "demo_borrower_rahul",
                    text = "Hello Rahul, I have reviewed your loan application of ₹50,000 for retail inventory expansion. Can you share shop photos and GST registration?",
                    timestamp = now - (30 * oneDayMs),
                    isMe = true,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "demo_msg_lent_2",
                    channelId = channelId,
                    senderId = "demo_borrower_rahul",
                    senderName = "Rahul Sharma",
                    senderRole = "BORROWER",
                    recipientId = currentUserId,
                    text = "Hello sir! Yes, GSTIN 07AAAAA0000A1Z5 is verified. Gold collateral (48.5g bangles & necklace, 24K) is also ready for appraisal by your field agent.",
                    timestamp = now - (30 * oneDayMs) + (2 * oneHourMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "demo_msg_lent_3",
                    channelId = channelId,
                    senderId = currentUserId,
                    senderName = "You",
                    senderRole = "LENDER",
                    recipientId = "demo_borrower_rahul",
                    text = "Sanction Approved: ₹50,000 at 12% p.a. for 6 months. Monthly EMI: ₹8,834. Field Agent Abhisi has completed physical inspection.",
                    timestamp = now - (29 * oneDayMs),
                    isMe = true,
                    status = "READ",
                    messageType = "PROPOSAL",
                    metaPayload = "LOAN_PROPOSAL|50000|12.0|6|8834"
                ),
                FirestoreChatMessage(
                    messageId = "demo_msg_lent_4",
                    channelId = channelId,
                    senderId = "demo_borrower_rahul",
                    senderName = "Rahul Sharma",
                    senderRole = "BORROWER",
                    recipientId = currentUserId,
                    text = "Proposal accepted! I have eSigned the tripartite agreement using Aadhaar OTP. Gold collateral sealed in central vault with tag #DEL-VAULT-042.",
                    timestamp = now - (28 * oneDayMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "demo_msg_lent_5",
                    channelId = channelId,
                    senderId = currentUserId,
                    senderName = "You",
                    senderRole = "LENDER",
                    recipientId = "demo_borrower_rahul",
                    text = "Disbursement confirmed! ₹50,000 sent via IMPS to your HDFC Bank account (Ref: IMPS/2026/09/882194). First EMI due on 5th.",
                    timestamp = now - (28 * oneDayMs) + (1 * oneHourMs),
                    isMe = true,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "demo_msg_lent_6",
                    channelId = channelId,
                    senderId = "demo_borrower_rahul",
                    senderName = "Rahul Sharma",
                    senderRole = "BORROWER",
                    recipientId = currentUserId,
                    text = "Received full amount! EMI #1 of ₹8,834 paid via UPI (Ref: UPI/329481928491). Attached receipt.",
                    timestamp = now - (25 * oneDayMs),
                    isMe = false,
                    status = "READ",
                    messageType = "RECEIPT",
                    metaPayload = "PAYMENT_RECEIPT|8834|UPI/329481928491|PAID"
                )
            )
            lowerCh.contains("demo_loan_borrowed_1") || lowerCh.contains("borrowed") || lowerCh.contains("25k") -> listOf(
                FirestoreChatMessage(
                    messageId = "demo_msg_borr_1",
                    channelId = channelId,
                    senderId = currentUserId,
                    senderName = "You",
                    senderRole = "BORROWER",
                    recipientId = "demo_lender_priya",
                    text = "Hi Priya, requesting ₹25,000 education loan for professional certification. 12 months tenure at 10.5% p.a.",
                    timestamp = now - (60 * oneDayMs),
                    isMe = true,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "demo_msg_borr_2",
                    channelId = channelId,
                    senderId = "demo_lender_priya",
                    senderName = "Priya Patel",
                    senderRole = "LENDER",
                    recipientId = currentUserId,
                    text = "Reviewed your profile and credit score (780). Happy to fund this. Please upload guarantor details to finalize.",
                    timestamp = now - (60 * oneDayMs) + (3 * oneHourMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "demo_msg_borr_3",
                    channelId = channelId,
                    senderId = currentUserId,
                    senderName = "You",
                    senderRole = "BORROWER",
                    recipientId = "demo_lender_priya",
                    text = "Guarantor Nirmala Devi (Mother) consent form attached along with 3 months salary slips. Digital signature completed.",
                    timestamp = now - (59 * oneDayMs),
                    isMe = true,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "demo_msg_borr_4",
                    channelId = channelId,
                    senderId = "demo_lender_priya",
                    senderName = "Priya Patel",
                    senderRole = "LENDER",
                    recipientId = currentUserId,
                    text = "Approved and eSigned! ₹25,000 transferred to your ICICI account. Best wishes for your course!",
                    timestamp = now - (59 * oneDayMs) + (2 * oneHourMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                )
            )
            lowerCh.contains("support_loanzo_assistant") || lowerCh.contains("support") -> listOf(
                FirestoreChatMessage(
                    messageId = "demo_msg_supp_1",
                    channelId = channelId,
                    senderId = "LOANZO_BOT",
                    senderName = "Loanzo AI Assistant",
                    senderRole = "OFFICIAL_BOT",
                    recipientId = currentUserId,
                    text = "Welcome to Loanzo Smart Legal Escrow & P2P Finance Assistant! I'm here 24/7 to assist with deals, repayments, vault documents, and dispute mediation.",
                    timestamp = now - (10 * oneDayMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "demo_msg_supp_2",
                    channelId = channelId,
                    senderId = "LOANZO_BOT",
                    senderName = "Loanzo AI Assistant",
                    senderRole = "OFFICIAL_BOT",
                    recipientId = currentUserId,
                    text = "Portfolio summary: You have 1 active loan lent (₹50,000 to Rahul Sharma, next EMI in 5 days), 1 loan borrowed (₹25,000 from Priya Patel), and 1 closed loan with issued NOC certificate. All legal contracts are encrypted in your Vault.",
                    timestamp = now - (2 * oneDayMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "demo_msg_supp_3",
                    channelId = channelId,
                    senderId = "LOANZO_BOT",
                    senderName = "Loanzo AI Assistant",
                    senderRole = "OFFICIAL_BOT",
                    recipientId = currentUserId,
                    text = "How can I help you today? You can ask me to generate a Financial Dossier, calculate EMI amortization, or request field agent verification.",
                    timestamp = now - (5 * 60_000L),
                    isMe = false,
                    status = "DELIVERED",
                    messageType = "TEXT"
                )
            )
            lowerCh.contains("demo_loan_closed_1") || lowerCh.contains("closed") || lowerCh.contains("15k") -> listOf(
                FirestoreChatMessage(
                    messageId = "demo_msg_closed_1",
                    channelId = channelId,
                    senderId = currentUserId,
                    senderName = "You",
                    senderRole = "LENDER",
                    recipientId = "demo_borrower_rahul",
                    text = "Rahul, confirming that your Medical Emergency loan has been fully settled ahead of schedule. Total repaid: ₹15,440.",
                    timestamp = now - (120 * oneDayMs),
                    isMe = true,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "demo_msg_closed_2",
                    channelId = channelId,
                    senderId = "demo_borrower_rahul",
                    senderName = "Rahul Sharma",
                    senderRole = "BORROWER",
                    recipientId = currentUserId,
                    text = "Thank you so much! I have downloaded the official NOC & Lien Release Certificate from my Vault. Collateral received in pristine condition.",
                    timestamp = now - (120 * oneDayMs) + (2 * oneHourMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                )
            )
            lowerCh.contains("demo_loan_platform_1") || lowerCh.contains("biz") || lowerCh.contains("150k") -> listOf(
                FirestoreChatMessage(
                    messageId = "demo_msg_p1_1",
                    channelId = channelId,
                    senderId = "demo_vikram_malhotra",
                    senderName = "Vikram Malhotra",
                    senderRole = "LENDER",
                    recipientId = "demo_amit_verma",
                    text = "Proposal for CNC Machinery Expansion (₹1,50,000, 12 Months at 11.5%). Approved upon site inspection.",
                    timestamp = now - (45 * oneDayMs),
                    isMe = false,
                    status = "READ",
                    messageType = "PROPOSAL",
                    metaPayload = "LOAN_PROPOSAL|150000|11.5|12|13295"
                ),
                FirestoreChatMessage(
                    messageId = "demo_msg_p1_2",
                    channelId = channelId,
                    senderId = "demo_amit_verma",
                    senderName = "Amit Verma",
                    senderRole = "BORROWER",
                    recipientId = "demo_vikram_malhotra",
                    text = "Field Agent Abhisi has verified machinery serial numbers and property deeds. eSign completed via Aadhaar.",
                    timestamp = now - (44 * oneDayMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "demo_msg_p1_3",
                    channelId = channelId,
                    senderId = "demo_vikram_malhotra",
                    senderName = "Vikram Malhotra",
                    senderRole = "LENDER",
                    recipientId = "demo_amit_verma",
                    text = "₹1,50,000 disbursed to payee account. Amortization schedule active in Loanzo app.",
                    timestamp = now - (44 * oneDayMs) + (3 * oneHourMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                )
            )
            lowerCh.contains("demo_loan_platform_2") || lowerCh.contains("gadget") || lowerCh.contains("80k") -> listOf(
                FirestoreChatMessage(
                    messageId = "demo_msg_p2_1",
                    channelId = channelId,
                    senderId = "demo_rajesh_gupta",
                    senderName = "Rajesh Gupta",
                    senderRole = "LENDER",
                    recipientId = "demo_sneha_roy",
                    text = "Reviewed equipment financing proposal: ₹80,000 at 12% p.a. for 8 months for Studio Workstation & Audio Gear.",
                    timestamp = now - (90 * oneDayMs),
                    isMe = false,
                    status = "READ",
                    messageType = "PROPOSAL",
                    metaPayload = "LOAN_PROPOSAL|80000|12.0|8|10458"
                ),
                FirestoreChatMessage(
                    messageId = "demo_msg_p2_2",
                    channelId = channelId,
                    senderId = "demo_sneha_roy",
                    senderName = "Sneha Roy",
                    senderRole = "BORROWER",
                    recipientId = "demo_rajesh_gupta",
                    text = "Hardware encumbered in platform collateral registry. MacBook Pro M2 Max serial tagged by Field Agent Abhisi.",
                    timestamp = now - (89 * oneDayMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "demo_msg_p2_3",
                    channelId = channelId,
                    senderId = "demo_rajesh_gupta",
                    senderName = "Rajesh Gupta",
                    senderRole = "LENDER",
                    recipientId = "demo_sneha_roy",
                    text = "Funds disbursed directly to Apple Authorized Reseller invoice escrow. All documents secured in Vault.",
                    timestamp = now - (89 * oneDayMs) + (2 * oneHourMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                )
            )
            lowerCh.contains("demo_loan_platform_3") || lowerCh.contains("super") || lowerCh.contains("200k") -> listOf(
                FirestoreChatMessage(
                    messageId = "demo_msg_p3_1",
                    channelId = channelId,
                    senderId = "demo_rajesh_gupta",
                    senderName = "Rajesh Gupta",
                    senderRole = "LENDER",
                    recipientId = "demo_borrower_rahul",
                    text = "Commercial warehouse lease expansion facility of ₹2,00,000 for 24 months sanctioned. Commercial lease deed registered.",
                    timestamp = now - (150 * oneDayMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "demo_msg_p3_2",
                    channelId = channelId,
                    senderId = "demo_borrower_rahul",
                    senderName = "Rahul Sharma",
                    senderRole = "BORROWER",
                    recipientId = "demo_rajesh_gupta",
                    text = "Aadhaar eSign tripartite contract completed. Security deposit of 3 months escrowed.",
                    timestamp = now - (149 * oneDayMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "demo_msg_p3_3",
                    channelId = channelId,
                    senderId = "demo_rajesh_gupta",
                    senderName = "Rajesh Gupta",
                    senderRole = "LENDER",
                    recipientId = "demo_borrower_rahul",
                    text = "Escrow release complete. Monthly automated NACH debit active.",
                    timestamp = now - (149 * oneDayMs) + (4 * oneHourMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                )
            )

            // =========================================================================
            // 2. DIRECT CONVERSATIONS WITH REGISTERED PERSONAS
            // =========================================================================
            lowerCh.contains("priya") || lowerCp.contains("priya") -> listOf(
                FirestoreChatMessage(
                    messageId = "dir_priya_1",
                    channelId = channelId,
                    senderId = "demo_lender_priya",
                    senderName = "Priya Patel",
                    senderRole = "LENDER",
                    recipientId = currentUserId,
                    text = "Hello! I noticed your excellent repayment history on Loanzo. I have ₹3 Lakhs in liquid capital allocated for verified community loans.",
                    timestamp = now - (35 * 60_000L),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_priya_2",
                    channelId = channelId,
                    senderId = currentUserId,
                    senderName = "You",
                    senderRole = "BORROWER",
                    recipientId = "demo_lender_priya",
                    text = "Thank you Priya! Yes, I keep all KYC and income statements updated via DigiLocker and Account Aggregator.",
                    timestamp = now - (25 * 60_000L),
                    isMe = true,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_priya_3",
                    channelId = channelId,
                    senderId = "demo_lender_priya",
                    senderName = "Priya Patel",
                    senderRole = "LENDER",
                    recipientId = currentUserId,
                    text = "If you or your network ever need co-financing for equipment or working capital, please share terms directly on the app.",
                    timestamp = now - (10 * 60_000L),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_priya_4",
                    channelId = channelId,
                    senderId = currentUserId,
                    senderName = "You",
                    senderRole = "BORROWER",
                    recipientId = "demo_lender_priya",
                    text = "Will do! The legal escrow and automated EMI mandate on Loanzo make deals completely stress-free.",
                    timestamp = now - (2 * 60_000L),
                    isMe = true,
                    status = "DELIVERED",
                    messageType = "TEXT"
                )
            )
            lowerCh.contains("vikram") || lowerCp.contains("vikram") -> listOf(
                FirestoreChatMessage(
                    messageId = "dir_vikram_1",
                    channelId = channelId,
                    senderId = "demo_vikram_malhotra",
                    senderName = "Vikram Malhotra",
                    senderRole = "LENDER",
                    recipientId = currentUserId,
                    text = "Greetings! Field Agent Abhisi just submitted the geo-tagged inspection report for the CNC lathe shop. Everything checked out 100% compliant with our collateral guidelines.",
                    timestamp = now - (3 * oneHourMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_vikram_2",
                    channelId = channelId,
                    senderId = currentUserId,
                    senderName = "You",
                    senderRole = "MEMBER",
                    recipientId = "demo_vikram_malhotra",
                    text = "That's reassuring Vikram. Fast turnaround on physical verification is exactly why Loanzo's hybrid model works so well.",
                    timestamp = now - (2 * oneHourMs),
                    isMe = true,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_vikram_3",
                    channelId = channelId,
                    senderId = "demo_vikram_malhotra",
                    senderName = "Vikram Malhotra",
                    senderRole = "LENDER",
                    recipientId = currentUserId,
                    text = "Agreed. Once the borrower eSigns with Aadhaar OTP, I'll authorize immediate RTGS transfer from escrow. Happy to co-lend on similar high-ticket MSME tickets.",
                    timestamp = now - (1 * oneHourMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_vikram_4",
                    channelId = channelId,
                    senderId = currentUserId,
                    senderName = "You",
                    senderRole = "MEMBER",
                    recipientId = "demo_vikram_malhotra",
                    text = "Understood. The legal contract PDF will be encrypted in the Vault for both parties.",
                    timestamp = now - (15 * 60_000L),
                    isMe = true,
                    status = "DELIVERED",
                    messageType = "TEXT"
                )
            )
            lowerCh.contains("rajesh") || lowerCp.contains("rajesh") -> listOf(
                FirestoreChatMessage(
                    messageId = "dir_rajesh_1",
                    channelId = channelId,
                    senderId = "demo_rajesh_gupta",
                    senderName = "Rajesh Gupta",
                    senderRole = "LENDER",
                    recipientId = currentUserId,
                    text = "Namaste! Confirming that Rahul's medical emergency loan has been completely liquidated with zero penalty. I've signed the digital NOC.",
                    timestamp = now - (6 * oneHourMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_rajesh_2",
                    channelId = channelId,
                    senderId = currentUserId,
                    senderName = "You",
                    senderRole = "MEMBER",
                    recipientId = "demo_rajesh_gupta",
                    text = "Thank you Rajesh ji! The lien on his collateral has been formally released in the app registry. He downloaded the digitally signed discharge certificate.",
                    timestamp = now - (5 * oneHourMs),
                    isMe = true,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_rajesh_3",
                    channelId = channelId,
                    senderId = "demo_rajesh_gupta",
                    senderName = "Rajesh Gupta",
                    senderRole = "LENDER",
                    recipientId = currentUserId,
                    text = "Excellent escrow governance. Looking forward to deploying further capital in secured equipment and retail inventory loans this quarter.",
                    timestamp = now - (4 * oneHourMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_rajesh_4",
                    channelId = channelId,
                    senderId = currentUserId,
                    senderName = "You",
                    senderRole = "MEMBER",
                    recipientId = "demo_rajesh_gupta",
                    text = "Will notify you as soon as verified borrowers post on the Community Wall!",
                    timestamp = now - (2 * oneHourMs),
                    isMe = true,
                    status = "DELIVERED",
                    messageType = "TEXT"
                )
            )
            lowerCh.contains("rahul") || lowerCp.contains("rahul") -> listOf(
                FirestoreChatMessage(
                    messageId = "dir_rahul_1",
                    channelId = channelId,
                    senderId = "demo_borrower_rahul",
                    senderName = "Rahul Sharma",
                    senderRole = "BORROWER",
                    recipientId = currentUserId,
                    text = "Namaste bhaiya! Festive shopping has kicked off and footfall in my shop has doubled. The ₹50,000 inventory loan arrived right on time.",
                    timestamp = now - (24 * oneHourMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_rahul_2",
                    channelId = channelId,
                    senderId = currentUserId,
                    senderName = "You",
                    senderRole = "LENDER",
                    recipientId = "demo_borrower_rahul",
                    text = "Glad to hear that, Rahul! Keep your sales records up to date. Remember, your next EMI of ₹8,834 is due on the 5th.",
                    timestamp = now - (23 * oneHourMs),
                    isMe = true,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_rahul_3",
                    channelId = channelId,
                    senderId = "demo_borrower_rahul",
                    senderName = "Rahul Sharma",
                    senderRole = "BORROWER",
                    recipientId = currentUserId,
                    text = "Yes bhaiya, UPI auto-mandate is set up through SBI. Receipt will automatically sync to our deal chat.",
                    timestamp = now - (22 * oneHourMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_rahul_4",
                    channelId = channelId,
                    senderId = currentUserId,
                    senderName = "You",
                    senderRole = "LENDER",
                    recipientId = "demo_borrower_rahul",
                    text = "Perfect. Keep building that pristine repayment score!",
                    timestamp = now - (20 * oneHourMs),
                    isMe = true,
                    status = "DELIVERED",
                    messageType = "TEXT"
                )
            )
            lowerCh.contains("sneha") || lowerCp.contains("sneha") -> listOf(
                FirestoreChatMessage(
                    messageId = "dir_sneha_1",
                    channelId = channelId,
                    senderId = "demo_sneha_roy",
                    senderName = "Sneha Roy",
                    senderRole = "BORROWER",
                    recipientId = currentUserId,
                    text = "Hi! Just wanted to share that the Apple M2 Max workstation and Neumann studio mics were delivered today. Serial numbers match the mortgage schedule.",
                    timestamp = now - (48 * oneHourMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_sneha_2",
                    channelId = channelId,
                    senderId = currentUserId,
                    senderName = "You",
                    senderRole = "MEMBER",
                    recipientId = "demo_sneha_roy",
                    text = "Awesome Sneha! Has the platform collateral lien been registered in your Vault?",
                    timestamp = now - (47 * oneHourMs),
                    isMe = true,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_sneha_3",
                    channelId = channelId,
                    senderId = "demo_sneha_roy",
                    senderName = "Sneha Roy",
                    senderRole = "BORROWER",
                    recipientId = currentUserId,
                    text = "Yes! Digilocker verification and equipment pledge agreement are both signed. This upgrade will help me deliver 3 animation client projects this month!",
                    timestamp = now - (46 * oneHourMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_sneha_4",
                    channelId = channelId,
                    senderId = currentUserId,
                    senderName = "You",
                    senderRole = "MEMBER",
                    recipientId = "demo_sneha_roy",
                    text = "Proud of your progress. Let me know if you need any assistance with EMI schedules.",
                    timestamp = now - (40 * oneHourMs),
                    isMe = true,
                    status = "DELIVERED",
                    messageType = "TEXT"
                )
            )
            lowerCh.contains("rohan") || lowerCp.contains("rohan") -> listOf(
                FirestoreChatMessage(
                    messageId = "dir_rohan_1",
                    channelId = channelId,
                    senderId = "demo_coborrower_rohan",
                    senderName = "Dr. Rohan Patil",
                    senderRole = "BORROWER",
                    recipientId = currentUserId,
                    text = "Hello Satyam, as co-borrower for the clinic diagnostics equipment, I have reviewed the joint liability clauses in the contract.",
                    timestamp = now - (72 * oneHourMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_rohan_2",
                    channelId = channelId,
                    senderId = currentUserId,
                    senderName = "You",
                    senderRole = "MEMBER",
                    recipientId = "demo_coborrower_rohan",
                    text = "Thank you Dr. Rohan. Your co-signature significantly strengthened the underwriting score for the sanction.",
                    timestamp = now - (71 * oneHourMs),
                    isMe = true,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_rohan_3",
                    channelId = channelId,
                    senderId = "demo_coborrower_rohan",
                    senderName = "Dr. Rohan Patil",
                    senderRole = "BORROWER",
                    recipientId = currentUserId,
                    text = "Happy to support. Healthcare infrastructure upgrades save lives. I've completed Aadhaar OTP authentication on my end.",
                    timestamp = now - (70 * oneHourMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_rohan_4",
                    channelId = channelId,
                    senderId = currentUserId,
                    senderName = "You",
                    senderRole = "MEMBER",
                    recipientId = "demo_coborrower_rohan",
                    text = "Acknowledged doctor. The tripartite agreement is now legally enforceable under Section 65B of the Indian Evidence Act.",
                    timestamp = now - (68 * oneHourMs),
                    isMe = true,
                    status = "DELIVERED",
                    messageType = "TEXT"
                )
            )
            lowerCh.contains("nirmala") || lowerCp.contains("nirmala") -> listOf(
                FirestoreChatMessage(
                    messageId = "dir_nirmala_1",
                    channelId = channelId,
                    senderId = "demo_guarantor_nirmala",
                    senderName = "Nirmala Devi",
                    senderRole = "USER",
                    recipientId = currentUserId,
                    text = "Beta Satyam, I received the SMS regarding guarantor consent for the education loan. I clicked the secure link and verified with my Aadhaar OTP.",
                    timestamp = now - (96 * oneHourMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_nirmala_2",
                    channelId = channelId,
                    senderId = currentUserId,
                    senderName = "You",
                    senderRole = "BORROWER",
                    recipientId = "demo_guarantor_nirmala",
                    text = "Thank you so much Nirmala ji! Your parental guarantee was approved by the lender immediately.",
                    timestamp = now - (95 * oneHourMs),
                    isMe = true,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_nirmala_3",
                    channelId = channelId,
                    senderId = "demo_guarantor_nirmala",
                    senderName = "Nirmala Devi",
                    senderRole = "USER",
                    recipientId = currentUserId,
                    text = "Blessings beta. Education is the best investment. Please make sure EMIs are paid punctually each month.",
                    timestamp = now - (94 * oneHourMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_nirmala_4",
                    channelId = channelId,
                    senderId = currentUserId,
                    senderName = "You",
                    senderRole = "BORROWER",
                    recipientId = "demo_guarantor_nirmala",
                    text = "Promise Nirmala ji, auto-debit is active and tracked on the app dashboard.",
                    timestamp = now - (90 * oneHourMs),
                    isMe = true,
                    status = "DELIVERED",
                    messageType = "TEXT"
                )
            )
            lowerCh.contains("amit") || lowerCp.contains("amit") -> listOf(
                FirestoreChatMessage(
                    messageId = "dir_amit_1",
                    channelId = channelId,
                    senderId = "demo_amit_verma",
                    senderName = "Amit Verma",
                    senderRole = "BORROWER",
                    recipientId = currentUserId,
                    text = "Satyam ji, our CNC lathe machine is now fully installed and running double shifts for the precision railway engineering contract.",
                    timestamp = now - (50 * oneHourMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_amit_2",
                    channelId = channelId,
                    senderId = currentUserId,
                    senderName = "You",
                    senderRole = "MEMBER",
                    recipientId = "demo_amit_verma",
                    text = "Congratulations Amit! That machinery loan from Vikram Malhotra really scaled your production capacity.",
                    timestamp = now - (49 * oneHourMs),
                    isMe = true,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_amit_3",
                    channelId = channelId,
                    senderId = "demo_amit_verma",
                    senderName = "Amit Verma",
                    senderRole = "BORROWER",
                    recipientId = currentUserId,
                    text = "Indeed. Having Agent Abhisi do on-site verification gave Vikram sir confidence. Our first month's invoice will cover the upcoming EMI comfortably.",
                    timestamp = now - (48 * oneHourMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                )
            )
            lowerCh.contains("meera") || lowerCp.contains("meera") -> listOf(
                FirestoreChatMessage(
                    messageId = "dir_meera_1",
                    channelId = channelId,
                    senderId = "demo_meera_sen",
                    senderName = "Meera Sen",
                    senderRole = "BORROWER",
                    recipientId = currentUserId,
                    text = "Hello! I am setting up my handloom boutique profile on Loanzo. I submitted my trade license and GST certificate for verification.",
                    timestamp = now - (12 * oneHourMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_meera_2",
                    channelId = channelId,
                    senderId = currentUserId,
                    senderName = "You",
                    senderRole = "MEMBER",
                    recipientId = "demo_meera_sen",
                    text = "Welcome Meera! Our field team reviews artisan applications within 24 hours. Are you seeking working capital for raw silk procurement?",
                    timestamp = now - (10 * oneHourMs),
                    isMe = true,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_meera_3",
                    channelId = channelId,
                    senderId = "demo_meera_sen",
                    senderName = "Meera Sen",
                    senderRole = "BORROWER",
                    recipientId = currentUserId,
                    text = "Yes, ₹60,000 for 6 months. Many backer lenders on Loanzo support women-led micro-enterprises, which gives me great hope.",
                    timestamp = now - (8 * oneHourMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                )
            )
            lowerCh.contains("kunal") || lowerCp.contains("kunal") -> listOf(
                FirestoreChatMessage(
                    messageId = "dir_kunal_1",
                    channelId = channelId,
                    senderId = "demo_kunal_rawat",
                    senderName = "Kunal Rawat",
                    senderRole = "BORROWER",
                    recipientId = currentUserId,
                    text = "Bhai, I uploaded my commercial vehicle RC and fitness certificate for the fleet maintenance loan request.",
                    timestamp = now - (16 * oneHourMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_kunal_2",
                    channelId = channelId,
                    senderId = currentUserId,
                    senderName = "You",
                    senderRole = "MEMBER",
                    recipientId = "demo_kunal_rawat",
                    text = "Checked Kunal. The documents look in order. Did you complete the re-KYC video verification?",
                    timestamp = now - (14 * oneHourMs),
                    isMe = true,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_kunal_3",
                    channelId = channelId,
                    senderId = "demo_kunal_rawat",
                    senderName = "Kunal Rawat",
                    senderRole = "BORROWER",
                    recipientId = currentUserId,
                    text = "Done this morning! Route earnings from Mumbai-Pune logistics are consistent. Need ₹75,000 for tyre replacement and engine overhaul.",
                    timestamp = now - (12 * oneHourMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                )
            )
            lowerCh.contains("alok") || lowerCp.contains("alok") -> listOf(
                FirestoreChatMessage(
                    messageId = "dir_alok_1",
                    channelId = channelId,
                    senderId = "demo_alok_trivedi",
                    senderName = "Alok Trivedi",
                    senderRole = "BORROWER",
                    recipientId = currentUserId,
                    text = "Hello team, bidding for a state electricity board electrical contract. We require an EMD backing facility of ₹1,20,000.",
                    timestamp = now - (28 * oneHourMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_alok_2",
                    channelId = channelId,
                    senderId = currentUserId,
                    senderName = "You",
                    senderRole = "MEMBER",
                    recipientId = "demo_alok_trivedi",
                    text = "Understood Alok. State contractor tenders are eligible under our Escrow Working Capital window with purchase order hypothecation.",
                    timestamp = now - (26 * oneHourMs),
                    isMe = true,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_alok_3",
                    channelId = channelId,
                    senderId = "demo_alok_trivedi",
                    senderName = "Alok Trivedi",
                    senderRole = "BORROWER",
                    recipientId = currentUserId,
                    text = "Attached work order draft and 3-year audited balance sheets. Looking forward to closing this with an institutional or angel backer.",
                    timestamp = now - (24 * oneHourMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                )
            )
            lowerCh.contains("agent") || lowerCh.contains("abhisi") || lowerCp.contains("abhisi") || lowerCh.contains("vikas") -> listOf(
                FirestoreChatMessage(
                    messageId = "dir_agent_1",
                    channelId = channelId,
                    senderId = "demo_agent_abhisi",
                    senderName = "Abhisi (Field Agent)",
                    senderRole = "AGENT",
                    recipientId = currentUserId,
                    text = "Field Officer Abhisi reporting: Completed 3 on-site verifications in Noida Sector 62 and Greater Noida today.",
                    timestamp = now - (120 * oneHourMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_agent_2",
                    channelId = channelId,
                    senderId = currentUserId,
                    senderName = "You",
                    senderRole = "ADMIN",
                    recipientId = "demo_agent_abhisi",
                    text = "Great job Abhisi. Did you capture the physical GPS coordinates and borrower biometric confirmations?",
                    timestamp = now - (119 * oneHourMs),
                    isMe = true,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_agent_3",
                    channelId = channelId,
                    senderId = "demo_agent_abhisi",
                    senderName = "Abhisi (Field Agent)",
                    senderRole = "AGENT",
                    recipientId = currentUserId,
                    text = "Yes sir, all photos with geo-stamps and collateral serial numbers are uploaded to the secure audit trail. Ready for underwriter sign-off.",
                    timestamp = now - (118 * oneHourMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "dir_agent_4",
                    channelId = channelId,
                    senderId = currentUserId,
                    senderName = "You",
                    senderRole = "ADMIN",
                    recipientId = "demo_agent_abhisi",
                    text = "Approved. Agent incentive fee credited to your Loanzo agent earnings wallet.",
                    timestamp = now - (115 * oneHourMs),
                    isMe = true,
                    status = "DELIVERED",
                    messageType = "TEXT"
                )
            )
            else -> listOf(
                FirestoreChatMessage(
                    messageId = "demo_msg_dyn_1",
                    channelId = channelId,
                    senderId = currentUserId,
                    senderName = "You",
                    senderRole = "MEMBER",
                    recipientId = "",
                    text = "Hello $counterpartyName! Glad to connect with you on the Loanzo P2P financial network.",
                    timestamp = now - (2 * oneHourMs),
                    isMe = true,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "demo_msg_dyn_2",
                    channelId = channelId,
                    senderId = "peer_contact",
                    senderName = counterpartyName,
                    senderRole = "MEMBER",
                    recipientId = currentUserId,
                    text = "Hi there! I'm active on Loanzo for secure, transparent peer loans. What terms are you looking to discuss?",
                    timestamp = now - (1 * oneHourMs),
                    isMe = false,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "demo_msg_dyn_3",
                    channelId = channelId,
                    senderId = currentUserId,
                    senderName = "You",
                    senderRole = "MEMBER",
                    recipientId = "",
                    text = "I'm interested in competitive interest rates with full escrow protection and legally binding digital contracts.",
                    timestamp = now - (30 * 60_000L),
                    isMe = true,
                    status = "READ",
                    messageType = "TEXT"
                ),
                FirestoreChatMessage(
                    messageId = "demo_msg_dyn_4",
                    channelId = channelId,
                    senderId = "peer_contact",
                    senderName = counterpartyName,
                    senderRole = "MEMBER",
                    recipientId = currentUserId,
                    text = "Sounds great! We can draft a proposal anytime with Aadhaar eSign and automated EMI scheduling.",
                    timestamp = now - (5 * 60_000L),
                    isMe = false,
                    status = "DELIVERED",
                    messageType = "TEXT"
                )
            )
        }
    }
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val userDao: UserDao,
    private val loanDao: LoanDao,
    private val userRepository: com.loanzo.app.data.repository.UserRepository,
    private val translationHelper: com.loanzo.app.util.TranslationHelper
) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"

        fun getDirectChannelId(u1: String, u2: String): String {
            val list = listOf(u1.trim(), u2.trim()).sorted()
            return "direct_${list[0]}_${list[1]}"
        }

        fun getLoanChannelId(loanId: String): String {
            return if (loanId.startsWith("loan_")) loanId else "loan_$loanId"
        }
    }

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var activeChatListener: ListenerRegistration? = null
    private var conversationsListener: ListenerRegistration? = null

    /**
     * Initializes the user identity for chats
     */
    fun initUser(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val user = userDao.getUserById(userId)
            val authUser = FirebaseAuth.getInstance().currentUser
            val currentId = userId.ifBlank { authUser?.uid ?: "" }
            val currentName = user?.name ?: user?.username ?: authUser?.displayName ?: "Member"
            val currentRole = user?.role ?: "MEMBER"

            _uiState.update {
                it.copy(
                    currentUserId = currentId,
                    currentUserName = currentName,
                    currentUserRole = currentRole
                )
            }

            loadUserConversations(currentId)
        }
    }

    /**
     * Loads all conversations for the user:
     * 1. Loan deal conversations (from LoanDao)
     * 2. Loanzo Support Assistant
     * 3. Syncs last message previews from Firestore
     */
    fun loadUserConversations(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }

            // 1. Gather loan counterparties
            val userLoans = loanDao.getAllLoansForUser(userId).firstOrNull() ?: emptyList()
            val list = mutableListOf<ChatConversationSummary>()

            // Default Assistant Channel
            list.add(
                ChatConversationSummary(
                    channelId = "support_loanzo_assistant",
                    channelType = "SUPPORT",
                    targetUserId = "LOANZO_BOT",
                    targetUserName = "Loanzo AI Assistant",
                    targetUserRole = "OFFICIAL_BOT",
                    targetUserPhone = "+910000000000",
                    targetUserKycStatus = "VERIFIED",
                    lastMessage = "24/7 Smart Legal Escrow & Loan Agreement Assistant is ready.",
                    lastTimestamp = System.currentTimeMillis(),
                    unreadCount = 0,
                    isOnline = true
                )
            )

            // Loan Deal Channels
            for (loan in userLoans) {
                val isLender = loan.lenderId == userId
                val counterpartyId = if (isLender) loan.borrowerId else loan.lenderId
                val counterparty = userDao.getUserById(counterpartyId)
                val counterpartyName = counterparty?.name ?: counterparty?.username ?: if (isLender) "Borrower (${loan.borrowerId.take(6)})" else "Lender (${loan.lenderId.take(6)})"
                val counterpartyRole = if (isLender) "BORROWER" else "LENDER"
                val channelId = getLoanChannelId(loan.loanId)
                val lowerLoanId = loan.loanId.lowercase()
                val demoLastMessage = when {
                    lowerLoanId.contains("lent") || lowerLoanId.contains("50k") -> "Received full amount! EMI #1 of ₹8,834 paid via UPI. Attached receipt."
                    lowerLoanId.contains("borrowed") || lowerLoanId.contains("25k") -> "Approved and eSigned! ₹25,000 transferred to your ICICI account. Best wishes for your course!"
                    lowerLoanId.contains("closed") || lowerLoanId.contains("15k") -> "Loan fully settled ahead of schedule. NOC and pledge release certificate issued."
                    lowerLoanId.contains("biz") || lowerLoanId.contains("150k") -> "Machinery serial number verified by Agent Abhisi. First installment credited."
                    lowerLoanId.contains("gadget") || lowerLoanId.contains("80k") -> "Workstation audio gear delivered and collateral tagged. Thank you!"
                    lowerLoanId.contains("super") || lowerLoanId.contains("200k") -> "Warehouse lease escrow agreement eSigned. Disbursement processed."
                    else -> "Deal chat active: ₹${loan.sanctionedAmount.toInt()} (${loan.purpose}) - ${loan.status}"
                }

                list.add(
                    ChatConversationSummary(
                        channelId = channelId,
                        channelType = "LOAN",
                        targetUserId = counterpartyId,
                        targetUserName = counterpartyName,
                        targetUserRole = counterpartyRole,
                        targetUserPhone = counterparty?.phone ?: "",
                        targetUserKycStatus = counterparty?.kycStatus ?: "PENDING",
                        loanId = loan.loanId,
                        loanTitle = "${if (isLender) "Lent" else "Borrowed"} INR ${loan.sanctionedAmount.toInt()} (${loan.status})",
                        loanPrincipal = loan.sanctionedAmount,
                        lastMessage = demoLastMessage,
                        lastTimestamp = loan.createdAt,
                        unreadCount = 0,
                        isOnline = true
                    )
                )
            }

            // Pre-seed unique, rich direct conversations with real personas
            val now = System.currentTimeMillis()
            val directDemoSeeds = listOf(
                Triple("demo_lender_priya", "Priya Patel", "Priya: I have ₹3L liquid capital ready for verified community loans.") to Pair("LENDER", 35 * 60_000L),
                Triple("demo_vikram_malhotra", "Vikram Malhotra", "Vikram: Field verification for MSME lathe was approved by Agent Abhisi. Ready to fund.") to Pair("LENDER", 3 * 3600_000L),
                Triple("demo_rajesh_gupta", "Rajesh Gupta", "Rajesh: Settlement received for the medical loan. DigiLocker NOC certificate uploaded to vault.") to Pair("LENDER", 6 * 3600_000L),
                Triple("demo_borrower_rahul", "Rahul Sharma", "Rahul: Paid EMI #1 of ₹8,834 via UPI. Festive inventory procurement is complete!") to Pair("BORROWER", 24 * 3600_000L),
                Triple("demo_sneha_roy", "Sneha Roy", "Sneha: Studio workstation setup is complete and collateral registry lien is active.") to Pair("BORROWER", 48 * 3600_000L),
                Triple("demo_coborrower_rohan", "Dr. Rohan Patil", "Dr. Rohan: Countersigned clinic diagnostics co-borrower agreement via Aadhaar OTP.") to Pair("BORROWER", 72 * 3600_000L),
                Triple("demo_guarantor_nirmala", "Nirmala Devi", "Nirmala: Digital guarantor consent form completed. All the best with the education program.") to Pair("USER", 96 * 3600_000L),
                Triple("demo_agent_abhisi", "Abhisi (Field Agent)", "Agent Abhisi: Completed geo-tagged physical collateral appraisal for today's assigned borrowers.") to Pair("AGENT", 120 * 3600_000L)
            )

            for ((partner, meta) in directDemoSeeds) {
                val (partnerId, partnerName, lastMsg) = partner
                val (partnerRole, timeOffset) = meta
                if (partnerId != userId) {
                    val partnerUser = userDao.getUserById(partnerId)
                    val chId = "direct_${userId}_${partnerId}"
                    list.add(
                        ChatConversationSummary(
                            channelId = chId,
                            channelType = "DIRECT",
                            targetUserId = partnerId,
                            targetUserName = partnerUser?.name ?: partnerName,
                            targetUserRole = partnerUser?.role ?: partnerRole,
                            targetUserPhone = partnerUser?.phone ?: "",
                            targetUserKycStatus = partnerUser?.kycStatus ?: "VERIFIED",
                            lastMessage = lastMsg,
                            lastTimestamp = now - timeOffset,
                            unreadCount = 0,
                            isOnline = true
                        )
                    )
                }
            }

            _uiState.update { state ->
                val updated = list.distinctBy { it.channelId }.sortedByDescending { it.lastTimestamp }
                state.copy(
                    conversations = updated,
                    filteredConversations = applyFilterAndSearch(updated, state.selectedFilter, state.searchQuery),
                    isLoading = false
                )
            }

            // Sync latest messages from Firestore metadata
            syncConversationsMetadata(userId)
        }
    }

    private fun syncConversationsMetadata(userId: String) {
        conversationsListener?.remove()
        try {
            conversationsListener = firestore.collection("channels")
                .whereArrayContains("participants", userId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot == null) return@addSnapshotListener
                    viewModelScope.launch(Dispatchers.IO) {
                        val currentList = _uiState.value.conversations.toMutableList()

                        for (doc in snapshot.documents) {
                            val chId = doc.id
                            val lastMsg = doc.getString("lastMessage") ?: continue
                            val lastTs = doc.getLong("lastTimestamp") ?: System.currentTimeMillis()
                            val unread = (doc.getLong("unread_$userId") ?: 0L).toInt()

                            val idx = currentList.indexOfFirst { it.channelId == chId }
                            if (idx != -1) {
                                val existing = currentList[idx]
                                currentList[idx] = existing.copy(
                                    lastMessage = lastMsg,
                                    lastTimestamp = lastTs,
                                    unreadCount = unread
                                )
                            } else {
                                val participants = doc.get("participants") as? List<*> ?: emptyList<Any>()
                                val otherUserId = participants.mapNotNull { it?.toString() }.firstOrNull { it != userId } ?: ""
                                val otherUser = if (otherUserId.isNotBlank()) userDao.getUserById(otherUserId) else null
                                currentList.add(
                                    ChatConversationSummary(
                                        channelId = chId,
                                        channelType = if (chId.startsWith("direct_")) "DIRECT" else "LOAN",
                                        targetUserId = otherUserId,
                                        targetUserName = otherUser?.name ?: otherUser?.username ?: "Community Member",
                                        targetUserRole = otherUser?.role ?: "MEMBER",
                                        targetUserPhone = otherUser?.phone ?: "",
                                        targetUserKycStatus = otherUser?.kycStatus ?: "VERIFIED",
                                        lastMessage = lastMsg,
                                        lastTimestamp = lastTs,
                                        unreadCount = unread,
                                        isOnline = true
                                    )
                                )
                            }
                        }

                        _uiState.update { state ->
                            val sorted = currentList.sortedByDescending { it.lastTimestamp }
                            state.copy(
                                conversations = sorted,
                                filteredConversations = applyFilterAndSearch(sorted, state.selectedFilter, state.searchQuery)
                            )
                        }
                    }
                }
        } catch (_: Exception) {}
    }

    /**
     * Filters and searches conversations
     */
    fun setFilter(filter: String) {
        _uiState.update { state ->
            state.copy(
                selectedFilter = filter,
                filteredConversations = applyFilterAndSearch(state.conversations, filter, state.searchQuery)
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredConversations = applyFilterAndSearch(state.conversations, state.selectedFilter, query)
            )
        }
    }

    private fun applyFilterAndSearch(
        list: List<ChatConversationSummary>,
        filter: String,
        query: String
    ): List<ChatConversationSummary> {
        var res = list
        if (filter == "DEALS") {
            res = res.filter { it.channelType == "LOAN" }
        } else if (filter == "DIRECT") {
            res = res.filter { it.channelType == "DIRECT" }
        } else if (filter == "SUPPORT") {
            res = res.filter { it.channelType == "SUPPORT" }
        }

        if (query.isNotBlank()) {
            val q = query.trim().removePrefix("@").lowercase()
            res = res.filter {
                it.targetUserName.lowercase().contains(q) ||
                it.targetUserPhone.lowercase().contains(q) ||
                (it.loanId != null && it.loanId.lowercase().contains(q)) ||
                it.lastMessage.lowercase().contains(q) ||
                it.targetUserId.lowercase().contains(q)
            }
        }
        return res
    }

    /**
     * Search all registered Loanzo users to start a new direct chat
     * Supports matching by @username, name, phone, email, and role.
     */
    fun searchUsersToChat(query: String) {
        val cleanQuery = query.trim().removePrefix("@").lowercase()
        if (cleanQuery.isBlank()) {
            _uiState.update { it.copy(searchUserResults = emptyList(), isSearchingUsers = false) }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isSearchingUsers = true) }
            val roomUsers = userDao.searchUsers(cleanQuery).firstOrNull() ?: emptyList()
            val me = _uiState.value.currentUserId

            // Also search DEFAULT_DEMO_CANDIDATE_USERS so any search matches immediately
            val demoMatches = com.loanzo.app.ui.components.DEFAULT_DEMO_CANDIDATE_USERS.filter { user ->
                user.name.lowercase().contains(cleanQuery) ||
                user.username.lowercase().contains(cleanQuery) ||
                user.phone.lowercase().contains(cleanQuery) ||
                user.role.lowercase().contains(cleanQuery) ||
                user.email.lowercase().contains(cleanQuery)
            }

            val merged = (roomUsers + demoMatches)
                .distinctBy { it.userId }
                .filter { it.userId != me }

            _uiState.update { it.copy(searchUserResults = merged, isSearchingUsers = false) }
        }
    }

    /**
     * Starts or opens a real-time chat channel
     */
    fun loadChat(channelId: String, loanId: String? = null, targetUserId: String? = null) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val initialUserId = _uiState.value.currentUserId.ifBlank { currentUser?.uid ?: "" }
        val initialUserName = _uiState.value.currentUserName.ifBlank { currentUser?.displayName ?: "You" }
        val userId = initialUserId

        _uiState.update {
            it.copy(
                isLoading = true,
                activeChannelId = channelId,
                currentUserId = initialUserId,
                currentUserName = initialUserName
            )
        }

        activeChatListener?.remove()

        // Load contextual entities in background
        viewModelScope.launch(Dispatchers.IO) {
            val resolvedUserId = initialUserId.ifBlank {
                userRepository.getCurrentUserIdSync()
                    ?: FirebaseAuth.getInstance().currentUser?.uid
                    ?: ""
            }
            val localUser = if (resolvedUserId.isNotBlank()) userDao.getUserById(resolvedUserId) else null
            val resolvedUserName = initialUserName.takeIf { it != "You" } ?: localUser?.name ?: localUser?.username ?: "You"
            val resolvedUserRole = localUser?.role ?: "MEMBER"

            val loan = if (!loanId.isNullOrBlank()) {
                loanDao.getLoanById(loanId)
            } else if (channelId.startsWith("loan_")) {
                loanDao.getLoanById(channelId.removePrefix("loan_"))
            } else null

            val counterparty = if (!targetUserId.isNullOrBlank()) {
                userDao.getUserById(targetUserId)
            } else if (loan != null) {
                val cId = if (loan.lenderId == resolvedUserId) loan.borrowerId else loan.lenderId
                userDao.getUserById(cId)
            } else if (channelId.startsWith("direct_")) {
                val parts = channelId.removePrefix("direct_").split("_")
                val otherId = parts.firstOrNull { it != resolvedUserId } ?: parts.lastOrNull() ?: ""
                if (otherId.isNotBlank()) userDao.getUserById(otherId) else null
            } else null

            _uiState.update {
                it.copy(
                    currentUserId = resolvedUserId,
                    currentUserName = resolvedUserName,
                    currentUserRole = resolvedUserRole,
                    activeLoan = loan,
                    activeCounterparty = counterparty
                )
            }
        }

        // Connect real-time Firestore listener
        val isLegacyLoan = !channelId.startsWith("direct_") && !channelId.startsWith("support_")
        val collectionRef = if (isLegacyLoan && loanId != null) {
            firestore.collection("loans").document(loanId).collection("chat")
        } else {
            firestore.collection("channels").document(channelId).collection("messages")
        }

        activeChatListener = collectionRef
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Chat listener error (using local demo fallback)", error)
                    val fallback = DemoChatMessages.getDemoMessagesForChannel(channelId, userId, _uiState.value.activeCounterparty?.name ?: "Member")
                    _uiState.update { it.copy(isLoading = false, messages = fallback) }
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    val senderId = doc.getString("senderId") ?: return@mapNotNull null
                    val text = doc.getString("text") ?: ""
                    val senderName = doc.getString("senderName") ?: "User"
                    val senderRole = doc.getString("senderRole") ?: "MEMBER"
                    val recipientId = doc.getString("recipientId") ?: ""
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    val status = doc.getString("status") ?: "DELIVERED"
                    val messageType = doc.getString("messageType") ?: "TEXT"
                    val metaPayload = doc.getString("metaPayload")

                    FirestoreChatMessage(
                        messageId = doc.id,
                        channelId = channelId,
                        senderId = senderId,
                        senderName = senderName,
                        senderRole = senderRole,
                        recipientId = recipientId,
                        text = text,
                        timestamp = timestamp,
                        isMe = senderId == userId,
                        status = status,
                        messageType = messageType,
                        metaPayload = metaPayload
                    )
                } ?: emptyList()

                val finalMessages = if (messages.isEmpty()) {
                    DemoChatMessages.getDemoMessagesForChannel(channelId, userId, _uiState.value.activeCounterparty?.name ?: "Member")
                } else {
                    messages
                }

                _uiState.update { it.copy(messages = finalMessages, isLoading = false) }
            }
    }

    /**
     * Sends a rich message
     */
    fun sendMessage(
        channelId: String,
        text: String,
        messageType: String = "TEXT",
        metaPayload: String? = null
    ) {
        if (text.isBlank() && metaPayload.isNullOrBlank()) return
        val state = _uiState.value
        val currentUid = state.currentUserId
        val currentUName = state.currentUserName
        val currentRole = state.currentUserRole

        viewModelScope.launch {
            var userId = currentUid.ifBlank {
                userRepository.getCurrentUserIdSync()
                    ?: FirebaseAuth.getInstance().currentUser?.uid
                    ?: ""
            }
            var userName = currentUName.ifBlank { "Member" }
            var role = currentRole.ifBlank { "MEMBER" }

            try {
                val localUser = if (userId.isNotBlank()) userDao.getUserById(userId) else null
                if (localUser != null) {
                    if (userName == "Member" || userName.isBlank()) userName = localUser.name.ifBlank { localUser.username }
                    if (role == "MEMBER") role = localUser.role
                }

                // Resolve other participant ID
                val otherId = state.activeCounterparty?.userId?.takeIf { it.isNotBlank() }
                    ?: if (channelId.startsWith("direct_")) {
                        val parts = channelId.removePrefix("direct_").split("_")
                        parts.firstOrNull { it != userId } ?: parts.lastOrNull() ?: ""
                    } else ""

                val messageData = hashMapOf(
                    "channelId" to channelId,
                    "senderId" to userId,
                    "senderName" to userName,
                    "senderRole" to role,
                    "recipientId" to otherId,
                    "text" to text.trim(),
                    "timestamp" to System.currentTimeMillis(),
                    "status" to "SENT",
                    "messageType" to messageType,
                    "metaPayload" to metaPayload
                )

                // Write message
                val isLegacyLoan = !channelId.startsWith("direct_") && !channelId.startsWith("support_")
                if (isLegacyLoan && state.activeLoan != null) {
                    firestore.collection("loans").document(state.activeLoan.loanId).collection("chat").add(messageData).await()
                } else {
                    firestore.collection("channels").document(channelId).collection("messages").add(messageData).await()
                }

                // Update channel metadata with both participants guaranteed
                val allParticipants = buildList {
                    if (userId.isNotBlank()) add(userId)
                    if (otherId.isNotBlank()) add(otherId)
                }.distinct()

                val channelDoc = hashMapOf(
                    "channelId" to channelId,
                    "lastMessage" to (if (messageType == "PROPOSAL") "Loan Proposal Sent" else text.trim()),
                    "lastTimestamp" to System.currentTimeMillis(),
                    "lastSenderId" to userId,
                    "participants" to allParticipants
                )
                firestore.collection("channels").document(channelId).set(channelDoc).await()

                Log.d(TAG, "Message sent successfully to channel $channelId with participants $allParticipants")
            } catch (e: Exception) {
                Log.w(TAG, "Cloud send message failed (applying local instant delivery): ${e.message}")
                val localMsg = FirestoreChatMessage(
                    messageId = "local_" + java.util.UUID.randomUUID().toString().take(8),
                    channelId = channelId,
                    senderId = userId,
                    senderName = userName,
                    senderRole = role,
                    recipientId = (state.activeCounterparty?.userId ?: ""),
                    text = text.trim(),
                    timestamp = System.currentTimeMillis(),
                    isMe = true,
                    status = "SENT",
                    messageType = messageType,
                    metaPayload = metaPayload
                )
                _uiState.update { it.copy(messages = it.messages + localMsg) }
            }
        }
    }

    fun setTranslatedText(messageId: String, translated: String) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { msg ->
                    if (msg.messageId == messageId) msg.copy(translatedText = translated, isTranslating = false)
                    else msg
                }
            )
        }
    }

    fun setTranslating(messageId: String) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { msg ->
                    if (msg.messageId == messageId) msg.copy(isTranslating = true)
                    else msg
                }
            )
        }
    }

    /**
     * Translates a specific chat message into the target language with robust multi-tier fallback.
     */
    fun translateMessage(messageId: String, text: String, targetLang: String = "hi") {
        if (text.isBlank()) return
        setTranslating(messageId)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val translated = translationHelper.translateText(text, targetLang)
                setTranslatedText(messageId, translated ?: "Translation unavailable.")
            } catch (e: Exception) {
                Log.e(TAG, "Translation failed for message $messageId: ${e.message}", e)
                setTranslatedText(messageId, "Translation failed.")
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    
    /**
     * Seeds realistic Firestore demo conversations for all demo loan channels.
     */
    fun seedDemoConversations(currentUserId: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userId = currentUserId.ifBlank { _uiState.value.currentUserId }
                val channels = listOf(
                    "loan_demo_loan_lent_1",
                    "loan_demo_loan_borrowed_1",
                    "loan_demo_loan_closed_1",
                    "loan_demo_loan_platform_1",
                    "support_loanzo_assistant"
                )
                for (chId in channels) {
                    val msgs = DemoChatMessages.getDemoMessagesForChannel(chId, userId, "Member")
                    val batch = firestore.batch()
                    for (m in msgs) {
                        val docRef = firestore.collection("channels").document(chId).collection("messages").document(m.messageId)
                        val data = hashMapOf(
                            "channelId" to m.channelId,
                            "senderId" to m.senderId,
                            "senderName" to m.senderName,
                            "senderRole" to m.senderRole,
                            "recipientId" to m.recipientId,
                            "text" to m.text,
                            "timestamp" to m.timestamp,
                            "status" to m.status,
                            "messageType" to m.messageType,
                            "metaPayload" to m.metaPayload
                        )
                        batch.set(docRef, data)
                    }
                    val chMetaRef = firestore.collection("channels").document(chId)
                    val last = msgs.lastOrNull()
                    batch.set(chMetaRef, hashMapOf(
                        "channelId" to chId,
                        "lastMessage" to (last?.text ?: "Demo Conversation"),
                        "lastTimestamp" to (last?.timestamp ?: System.currentTimeMillis()),
                        "participants" to listOfNotNull(userId, last?.senderId, last?.recipientId).distinct()
                    ))
                    batch.commit().await()
                }
                Log.d(TAG, "Demo conversations seeded to Firestore successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Firestore demo conversation seeding skipped (offline mode): ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        activeChatListener?.remove()
        conversationsListener?.remove()
    }
}
