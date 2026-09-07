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

        return when {
            channelId.contains("demo_loan_lent_1") || channelId.contains("lent") -> listOf(
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
                    text = "Sanction Approved: ₹50,000 at 12% p.a. for 6 months. Monthly EMI: ₹8,834. Field Agent Vikas has completed physical inspection.",
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
            channelId.contains("demo_loan_borrowed_1") || channelId.contains("borrowed") -> listOf(
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
            channelId.contains("support_loanzo_assistant") || channelId.contains("support") -> listOf(
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
            channelId.contains("demo_loan_closed_1") || channelId.contains("closed") -> listOf(
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
            channelId.contains("demo_loan_platform_1") || channelId.contains("platform_1") -> listOf(
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
                    text = "Field Agent Vikas has verified machinery serial numbers and property deeds. eSign completed via Aadhaar.",
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
            else -> listOf(
                FirestoreChatMessage(
                    messageId = "demo_msg_generic_1",
                    channelId = channelId,
                    senderId = currentUserId,
                    senderName = "You",
                    senderRole = "MEMBER",
                    recipientId = "",
                    text = "Hello $counterpartyName, let's discuss this loan agreement on Loanzo secure escrow.",
                    timestamp = now - (1 * oneDayMs),
                    isMe = true,
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
    private val loanDao: LoanDao
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
        try {
            val app = com.google.firebase.FirebaseApp.getInstance()
            FirebaseFirestore.getInstance(app, "default")
        } catch (_: Exception) {
            FirebaseFirestore.getInstance()
        }
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
                val demoLastMessage = when {
                    loan.loanId.contains("demo_loan_lent_1") -> "Received full amount! EMI #1 of ₹8,834 paid via UPI. Attached receipt."
                    loan.loanId.contains("demo_loan_borrowed_1") -> "Approved and eSigned! ₹25,000 transferred to your ICICI account. Best wishes for your course!"
                    loan.loanId.contains("demo_loan_closed_1") -> "Loan fully settled ahead of schedule. NOC and pledge release certificate issued."
                    loan.loanId.contains("demo_loan_platform_1") -> "Machinery serial number verified by Agent Vikas. First installment credited."
                    loan.loanId.contains("demo_loan_platform_2") -> "Workstation audio gear delivered and collateral tagged. Thank you!"
                    loan.loanId.contains("demo_loan_platform_3") -> "Warehouse lease escrow agreement eSigned. Disbursement processed."
                    else -> "Deal chat for Loan #${loan.loanId.takeLast(6)} (${loan.status})"
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
            val q = query.trim().lowercase()
            res = res.filter {
                it.targetUserName.lowercase().contains(q) ||
                it.targetUserPhone.lowercase().contains(q) ||
                (it.loanId != null && it.loanId.lowercase().contains(q)) ||
                it.lastMessage.lowercase().contains(q)
            }
        }
        return res
    }

    /**
     * Search all registered Loanzo users to start a new direct chat
     */
    fun searchUsersToChat(query: String) {
        if (query.isBlank()) {
            _uiState.update { it.copy(searchUserResults = emptyList(), isSearchingUsers = false) }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isSearchingUsers = true) }
            val users = userDao.searchUsers(query.trim()).firstOrNull() ?: emptyList()
            val me = _uiState.value.currentUserId
            val filtered = users.filter { it.userId != me }
            _uiState.update { it.copy(searchUserResults = filtered, isSearchingUsers = false) }
        }
    }

    /**
     * Starts or opens a real-time chat channel
     */
    fun loadChat(channelId: String, loanId: String? = null, targetUserId: String? = null) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = _uiState.value.currentUserId.ifBlank { currentUser?.uid ?: "" }
        val userName = _uiState.value.currentUserName.ifBlank { currentUser?.displayName ?: "You" }

        _uiState.update {
            it.copy(
                isLoading = true,
                activeChannelId = channelId,
                currentUserId = userId,
                currentUserName = userName
            )
        }

        activeChatListener?.remove()

        // Load contextual entities in background
        viewModelScope.launch(Dispatchers.IO) {
            val loan = if (!loanId.isNullOrBlank()) {
                loanDao.getLoanById(loanId)
            } else if (channelId.startsWith("loan_")) {
                loanDao.getLoanById(channelId.removePrefix("loan_"))
            } else null

            val counterparty = if (!targetUserId.isNullOrBlank()) {
                userDao.getUserById(targetUserId)
            } else if (loan != null) {
                val cId = if (loan.lenderId == userId) loan.borrowerId else loan.lenderId
                userDao.getUserById(cId)
            } else null

            _uiState.update { it.copy(activeLoan = loan, activeCounterparty = counterparty) }
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
        val userId = state.currentUserId
        val userName = state.currentUserName
        val role = state.currentUserRole

        viewModelScope.launch {
            try {
                val messageData = hashMapOf(
                    "channelId" to channelId,
                    "senderId" to userId,
                    "senderName" to userName,
                    "senderRole" to role,
                    "recipientId" to (state.activeCounterparty?.userId ?: ""),
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

                // Update channel metadata
                val channelDoc = hashMapOf(
                    "channelId" to channelId,
                    "lastMessage" to (if (messageType == "PROPOSAL") "Loan Proposal Sent" else text.trim()),
                    "lastTimestamp" to System.currentTimeMillis(),
                    "lastSenderId" to userId,
                    "participants" to listOfNotNull(userId, state.activeCounterparty?.userId).distinct()
                )
                firestore.collection("channels").document(channelId).set(channelDoc).await()

                Log.d(TAG, "Message sent successfully to channel $channelId")
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
