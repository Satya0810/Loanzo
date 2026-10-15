package com.loanzo.app.ui.loan

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.loanzo.app.data.dao.LoanDao
import com.loanzo.app.data.dao.UserDao
import com.loanzo.app.data.entity.LoanEntity
import com.loanzo.app.data.entity.UserEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
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
    val targetUserProfilePhotoUri: String = "",
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
        val oneHour = 3_600_000L
        val list = mutableListOf<FirestoreChatMessage>()

        when {
            channelId.contains("shivam") || channelId.contains("surya111") -> {
                val isDevesh = currentUserId == "devesh123"
                list.add(
                    FirestoreChatMessage(
                        messageId = "msg_shivam_1",
                        channelId = channelId,
                        senderId = "surya111",
                        senderName = "Shivam Yadav",
                        senderRole = "BORROWER",
                        text = "Namaste Devesh ji, Milestone 1 raw fabric materials have been successfully delivered by Bharat Tex directly to our Varanasi loom unit.",
                        timestamp = now - (20 * oneHour),
                        isMe = !isDevesh,
                        status = "READ"
                    )
                )
                list.add(
                    FirestoreChatMessage(
                        messageId = "msg_shivam_2",
                        channelId = channelId,
                        senderId = "devesh123",
                        senderName = "Devesh Singh",
                        senderRole = "LENDER",
                        text = "Hello Shivam. Yes, the GST e-invoice OCR was verified by the escrow engine and ₹50,000 was disbursed directly to Bharat Tex via UPI. How is the saree production volume?",
                        timestamp = now - (18 * oneHour),
                        isMe = isDevesh,
                        status = "READ"
                    )
                )
                list.add(
                    FirestoreChatMessage(
                        messageId = "msg_shivam_3",
                        channelId = channelId,
                        senderId = "surya111",
                        senderName = "Shivam Yadav",
                        senderRole = "BORROWER",
                        text = "Production is exceeding targets! The Jacquard setup is producing 80 sarees per day. I also paid EMI #3 (₹9,200) yesterday on schedule.",
                        timestamp = now - (10 * oneHour),
                        isMe = !isDevesh,
                        status = "READ"
                    )
                )
                list.add(
                    FirestoreChatMessage(
                        messageId = "msg_shivam_4",
                        channelId = channelId,
                        senderId = "devesh123",
                        senderName = "Devesh Singh",
                        senderRole = "LENDER",
                        text = "Received the ₹9,200 credit. Great punctuality. Romesh also completed the field assay report and our 22K gold collateral is safely lodged in Central Locker #42.",
                        timestamp = now - (2 * oneHour),
                        isMe = isDevesh,
                        status = "DELIVERED"
                    )
                )
            }
            channelId.contains("suman") || channelId.contains("suman321") -> {
                val isDevesh = currentUserId == "devesh123"
                list.add(
                    FirestoreChatMessage(
                        messageId = "msg_suman_1",
                        channelId = channelId,
                        senderId = "suman321",
                        senderName = "Suman Yadav",
                        senderRole = "BORROWER",
                        text = "Namaste Devesh ji, thank you for placing a ₹75,000 syndication bid on my Dairy Bulk Milk Chiller marketplace post.",
                        timestamp = now - (15 * oneHour),
                        isMe = !isDevesh,
                        status = "READ"
                    )
                )
                list.add(
                    FirestoreChatMessage(
                        messageId = "msg_suman_2",
                        channelId = channelId,
                        senderId = "devesh123",
                        senderName = "Devesh Singh",
                        senderRole = "LENDER",
                        text = "Hello Suman ji! You completed your ₹50,000 solar irrigation loan with zero defaults and earned your Golden NOC Certificate. I am very glad to back your dairy chilling unit.",
                        timestamp = now - (12 * oneHour),
                        isMe = isDevesh,
                        status = "READ"
                    )
                )
                list.add(
                    FirestoreChatMessage(
                        messageId = "msg_suman_3",
                        channelId = channelId,
                        senderId = "suman321",
                        senderName = "Suman Yadav",
                        senderRole = "BORROWER",
                        text = "Thank you! Romesh has endorsed as co-borrower as well. We are ready to accept the bid with direct vendor payment to the chiller manufacturer.",
                        timestamp = now - (1 * oneHour),
                        isMe = !isDevesh,
                        status = "DELIVERED"
                    )
                )
            }
            channelId.contains("romesh") || channelId.contains("romesh321") -> {
                val isRomesh = currentUserId == "romesh321"
                list.add(
                    FirestoreChatMessage(
                        messageId = "msg_romesh_1",
                        channelId = channelId,
                        senderId = "romesh321",
                        senderName = "Romesh Singh",
                        senderRole = "OFFICER",
                        text = "Field inspection report for Shivam's textile loom unit and 22K gold hallmarking assay has been cryptographically uploaded to Loanzo Vault.",
                        timestamp = now - (16 * oneHour),
                        isMe = isRomesh,
                        status = "READ"
                    )
                )
                list.add(
                    FirestoreChatMessage(
                        messageId = "msg_romesh_2",
                        channelId = channelId,
                        senderId = "devesh123",
                        senderName = "Devesh Singh",
                        senderRole = "LENDER",
                        text = "Thanks Romesh. Your joint obligor guarantee under Section 128 ICA is also verified on the contract. Equipment toolkit tranche was disbursed to Precision Instruments.",
                        timestamp = now - (8 * oneHour),
                        isMe = !isRomesh,
                        status = "READ"
                    )
                )
                list.add(
                    FirestoreChatMessage(
                        messageId = "msg_romesh_3",
                        channelId = channelId,
                        senderId = "romesh321",
                        senderName = "Romesh Singh",
                        senderRole = "OFFICER",
                        text = "Confirmed! The digital optical sensor toolkit arrived and has been tagged with Barcode LZ-EQP-5512. Field operations are fully active.",
                        timestamp = now - (30 * 60_000L),
                        isMe = isRomesh,
                        status = "DELIVERED"
                    )
                )
            }
            channelId.contains("support") || channelId.contains("bot") -> {
                list.add(
                    FirestoreChatMessage(
                        messageId = "msg_bot_1",
                        channelId = channelId,
                        senderId = "LOANZO_BOT",
                        senderName = "Loanzo AI Assistant",
                        senderRole = "OFFICIAL_BOT",
                        text = "Welcome to Loanzo Smart Legal Escrow & Contract Support. Every transaction on Loanzo is backed by Section 4 Promissory Notes, DigiLocker verified identity, and purpose-bound escrow tranches with 0% fund diversion. How can I assist you?",
                        timestamp = now - (2 * oneHour),
                        isMe = false,
                        status = "DELIVERED"
                    )
                )
                list.add(
                    FirestoreChatMessage(
                        messageId = "msg_bot_2",
                        channelId = channelId,
                        senderId = currentUserId,
                        senderName = "You",
                        senderRole = "MEMBER",
                        text = "Can you confirm the legal validity of the Golden NOC Certificate and collateral release?",
                        timestamp = now - (oneHour),
                        isMe = true,
                        status = "READ"
                    )
                )
                list.add(
                    FirestoreChatMessage(
                        messageId = "msg_bot_3",
                        channelId = channelId,
                        senderId = "LOANZO_BOT",
                        senderName = "Loanzo AI Assistant",
                        senderRole = "OFFICIAL_BOT",
                        text = "Upon 100% repayment clearance, Loanzo's automated settlement engine discharges all registered liens under Section 171 of the Indian Contract Act. The Golden NOC is cryptographically signed with SHA-256 hash and legally admissible under Section 65B of the Indian Evidence Act.",
                        timestamp = now - (15 * 60_000L),
                        isMe = false,
                        status = "DELIVERED"
                    )
                )
            }
            else -> {
                list.add(
                    FirestoreChatMessage(
                        messageId = "msg_generic_1",
                        channelId = channelId,
                        senderId = "counterparty",
                        senderName = counterpartyName.ifBlank { "Loan Counterparty" },
                        senderRole = "MEMBER",
                        text = "Hello! Looking forward to reviewing the loan terms and purpose-bound escrow disbursement on Loanzo.",
                        timestamp = now - (5 * oneHour),
                        isMe = false,
                        status = "DELIVERED"
                    )
                )
            }
        }
        return list
    }
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val userDao: UserDao,
    private val loanDao: LoanDao,
    private val userRepository: com.loanzo.app.data.repository.UserRepository,
    private val translationHelper: com.loanzo.app.util.TranslationHelper,
    val multiAiRaceEngine: com.loanzo.app.data.ai.MultiAiRaceEngine,
    val aiConfigManager: com.loanzo.app.data.ai.AiConfigManager,
    private val firebaseManager: com.loanzo.app.data.firebase.FirebaseManager,
    val voiceAssistantManager: com.loanzo.app.data.voice.VoiceAssistantManager
) : ViewModel() {

    val voiceState = voiceAssistantManager.voiceState

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

    private val firestore: FirebaseFirestore
        get() = com.loanzo.app.data.firebase.FirestoreProvider.get()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var activeChatListener: ListenerRegistration? = null
    private var conversationsListener: ListenerRegistration? = null

    /**
     * Initializes the user identity for chats
     */
    fun initUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val resolvedId = userId.ifBlank {
                userRepository.getCurrentUserIdSync() ?: ""
            }
            if (resolvedId.isBlank()) return@launch
            val user = userDao.getUserById(resolvedId) ?: userRepository.syncUserById(resolvedId)
            val currentName = user?.name?.ifBlank { user.username } ?: user?.username ?: "Member"
            val currentRole = user?.role ?: "MEMBER"

            _uiState.update {
                it.copy(
                    currentUserId = resolvedId,
                    currentUserName = currentName,
                    currentUserRole = currentRole
                )
            }

            loadUserConversations(resolvedId)
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
                val conversationPreview = "Deal chat active: ₹${loan.sanctionedAmount.toInt()} (${loan.purpose}) - ${loan.status}"

                list.add(
                    ChatConversationSummary(
                        channelId = channelId,
                        channelType = "LOAN",
                        targetUserId = counterpartyId,
                        targetUserName = counterpartyName,
                        targetUserRole = counterpartyRole,
                        targetUserPhone = counterparty?.phone ?: "",
                        targetUserKycStatus = counterparty?.kycStatus ?: "PENDING",
                        targetUserProfilePhotoUri = counterparty?.profilePhotoUri ?: "",
                        loanId = loan.loanId,
                        loanTitle = "${if (isLender) "Lent" else "Borrowed"} INR ${loan.sanctionedAmount.toInt()} (${loan.status})",
                        loanPrincipal = loan.sanctionedAmount,
                        lastMessage = conversationPreview,
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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                firebaseManager.ensureFirebaseAuthSession()
            } catch (_: Exception) {}
        }
        try {
            conversationsListener = firestore.collection("channels")
                .whereArrayContains("participants", userId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot == null) return@addSnapshotListener
                    viewModelScope.launch(Dispatchers.IO) {
                        val currentList = _uiState.value.conversations.toMutableList()

                        for (doc in snapshot.documents) {
                            val rawChId = doc.id
                            val chId = if (!rawChId.startsWith("loan_") && !rawChId.startsWith("direct_") && !rawChId.startsWith("support_") && (rawChId.startsWith("LZ-") || doc.getString("channelType") == "LOAN")) {
                                getLoanChannelId(rawChId)
                            } else {
                                rawChId
                            }
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
                                val otherUserId = participants.mapNotNull { it?.toString() }.firstOrNull { it != userId }
                                    ?: if (chId.startsWith("direct_")) {
                                        val parts = chId.removePrefix("direct_").split("_")
                                        parts.firstOrNull { it != userId } ?: parts.lastOrNull() ?: ""
                                    } else ""
                                val otherUser = if (otherUserId.isNotBlank()) {
                                    userDao.getUserById(otherUserId) ?: userRepository.syncUserById(otherUserId)
                                } else null
                                currentList.add(
                                    ChatConversationSummary(
                                        channelId = chId,
                                        channelType = if (chId.startsWith("direct_")) "DIRECT" else "LOAN",
                                        targetUserId = otherUserId,
                                        targetUserName = otherUser?.name ?: otherUser?.username ?: "Community Member",
                                        targetUserRole = otherUser?.role ?: "MEMBER",
                                        targetUserPhone = otherUser?.phone ?: "",
                                        targetUserKycStatus = otherUser?.kycStatus ?: "VERIFIED",
                                        targetUserProfilePhotoUri = otherUser?.profilePhotoUri ?: "",
                                        lastMessage = lastMsg,
                                        lastTimestamp = lastTs,
                                        unreadCount = unread,
                                        isOnline = true
                                    )
                                )
                            }
                        }

                        _uiState.update { state ->
                            val sorted = currentList.distinctBy { it.channelId }.sortedByDescending { it.lastTimestamp }
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
            val onlineAndLocalUsers = userRepository.searchUsersOnline(cleanQuery)
            val me = _uiState.value.currentUserId

            val merged = onlineAndLocalUsers
                .distinctBy { it.userId }
                .filter { it.userId != me }

            _uiState.update { it.copy(searchUserResults = merged, isSearchingUsers = false) }
        }
    }

    /**
     * Starts or opens a real-time chat channel
     */
    fun loadChat(channelId: String, loanId: String? = null, targetUserId: String? = null) {
        val canonicalChannelId = if (!channelId.startsWith("loan_") && !channelId.startsWith("direct_") && !channelId.startsWith("support_")) {
            if (loanId != null || channelId.startsWith("LZ-")) getLoanChannelId(channelId) else channelId
        } else {
            channelId
        }
        val effectiveLoanId = loanId ?: if (canonicalChannelId.startsWith("loan_")) canonicalChannelId.removePrefix("loan_") else null

        val initialUserId = _uiState.value.currentUserId
        val initialUserName = _uiState.value.currentUserName.ifBlank { "You" }

        _uiState.update {
            it.copy(
                isLoading = it.messages.isEmpty() && it.activeChannelId != canonicalChannelId,
                activeChannelId = canonicalChannelId,
                currentUserId = initialUserId,
                currentUserName = initialUserName
            )
        }

        activeChatListener?.remove()

        // Load contextual entities in background
        viewModelScope.launch(Dispatchers.IO) {
            try {
                firebaseManager.ensureFirebaseAuthSession()
            } catch (_: Exception) {}
            val resolvedUserId = initialUserId.ifBlank {
                userRepository.getCurrentUserIdSync() ?: ""
            }
            val localUser = if (resolvedUserId.isNotBlank()) userDao.getUserById(resolvedUserId) ?: userRepository.syncUserById(resolvedUserId) else null
            val resolvedUserName = initialUserName.takeIf { it != "You" } ?: localUser?.name?.ifBlank { localUser.username } ?: localUser?.username ?: "You"
            val resolvedUserRole = localUser?.role ?: "MEMBER"

            val loan = if (!effectiveLoanId.isNullOrBlank()) {
                loanDao.getLoanById(effectiveLoanId)
            } else if (canonicalChannelId.startsWith("loan_")) {
                loanDao.getLoanById(canonicalChannelId.removePrefix("loan_"))
            } else {
                loanDao.getLoanById(canonicalChannelId)
            }

            val candidateCounterpartyId = if (!targetUserId.isNullOrBlank()) {
                targetUserId
            } else if (loan != null) {
                if (loan.lenderId == resolvedUserId) loan.borrowerId else loan.lenderId
            } else if (canonicalChannelId.startsWith("direct_")) {
                val parts = canonicalChannelId.removePrefix("direct_").split("_")
                parts.firstOrNull { it != resolvedUserId } ?: parts.lastOrNull() ?: ""
            } else null

            val counterparty = if (!candidateCounterpartyId.isNullOrBlank()) {
                userDao.getUserById(candidateCounterpartyId)
                    ?: userRepository.syncUserById(candidateCounterpartyId)
                    ?: when (candidateCounterpartyId.lowercase().trim()) {
                        "suman321", "user_suman", "suman" -> com.loanzo.app.data.entity.UserEntity(userId = "suman321", name = "Suman Yadav", username = "suman321", role = "BORROWER")
                        "devesh123", "user_devesh", "devesh" -> com.loanzo.app.data.entity.UserEntity(userId = "devesh123", name = "Devesh Singh", username = "devesh123", role = "LENDER")
                        "romesh321", "user_romesh", "romesh" -> com.loanzo.app.data.entity.UserEntity(userId = "romesh321", name = "Romesh Singh", username = "romesh321", role = "OFFICER")
                        "shivam123", "user_shivam", "shivam" -> com.loanzo.app.data.entity.UserEntity(userId = "shivam123", name = "Shivam Yadav", username = "shivam123", role = "BORROWER")
                        else -> {
                            val formattedName = candidateCounterpartyId.replace('_', ' ').replace('-', ' ')
                                .split(' ')
                                .filter { it.isNotBlank() }
                                .joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() } }
                            if (formattedName.isNotBlank() && !formattedName.equals("counterparty", ignoreCase = true)) {
                                com.loanzo.app.data.entity.UserEntity(
                                    userId = candidateCounterpartyId,
                                    name = formattedName,
                                    username = candidateCounterpartyId,
                                    role = if (loan?.borrowerId == candidateCounterpartyId) "BORROWER" else "MEMBER"
                                )
                            } else null
                        }
                    }
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

        // Connect real-time Firestore listener to canonical channels/{canonicalChannelId}/messages
        val collectionRef = firestore.collection("channels").document(canonicalChannelId).collection("messages")

        activeChatListener = collectionRef
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Chat listener error (channel: $channelId)", error)
                    val fallback = DemoChatMessages.getDemoMessagesForChannel(
                        channelId,
                        _uiState.value.currentUserId,
                        _uiState.value.activeCounterparty?.name ?: ""
                    )
                    _uiState.update { curr ->
                        val pendingLocal = curr.messages.filter { it.messageId.startsWith("local_") || it.messageId.startsWith("ai_") }
                        val combined = (fallback + pendingLocal).distinctBy { it.messageId }.sortedBy { it.timestamp }
                        curr.copy(messages = combined, isLoading = false)
                    }
                    return@addSnapshotListener
                }

                val currentUid = _uiState.value.currentUserId
                val currentUName = _uiState.value.currentUserName

                val isSupportChannel = channelId == "support_loanzo_assistant" || channelId.contains("support") || targetUserId == "LOANZO_BOT"

                val parsedMessages = snapshot?.documents?.mapNotNull { doc ->
                    val senderId = doc.getString("senderId") ?: return@mapNotNull null
                    val text = doc.getString("text") ?: ""
                    val senderName = doc.getString("senderName") ?: "User"
                    val senderRole = doc.getString("senderRole") ?: "MEMBER"
                    val recipientId = doc.getString("recipientId") ?: ""
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    val status = doc.getString("status") ?: "DELIVERED"
                    val messageType = doc.getString("messageType") ?: "TEXT"
                    val metaPayload = doc.getString("metaPayload")

                    val isMe = if (isSupportChannel) {
                        senderId != "LOANZO_BOT" && senderRole != "OFFICIAL_BOT"
                    } else {
                        currentUid.isNotBlank() && (senderId == currentUid || (currentUName.isNotBlank() && currentUName != "Member" && currentUName != "You" && senderId == currentUName))
                    }

                    FirestoreChatMessage(
                        messageId = doc.id,
                        channelId = channelId,
                        senderId = senderId,
                        senderName = senderName,
                        senderRole = senderRole,
                        recipientId = recipientId,
                        text = text,
                        timestamp = timestamp,
                        isMe = isMe,
                        status = status,
                        messageType = messageType,
                        metaPayload = metaPayload
                    )
                } ?: emptyList()

                val remoteMessages = if (parsedMessages.isEmpty()) {
                    DemoChatMessages.getDemoMessagesForChannel(
                        channelId,
                        currentUid,
                        _uiState.value.activeCounterparty?.name ?: ""
                    )
                } else {
                    parsedMessages
                }

                _uiState.update { currState ->
                    // Preserve in-flight local messages (such as thinking indicators or pending AI responses)
                    val pendingLocal = currState.messages.filter { localMsg ->
                        localMsg.messageId.startsWith("ai_thinking_") ||
                        (localMsg.messageId.startsWith("local_") && remoteMessages.none { rm -> rm.text == localMsg.text }) ||
                        (localMsg.messageId.startsWith("ai_resp_") && remoteMessages.none { rm -> rm.messageId == localMsg.messageId || (rm.senderId == "LOANZO_BOT" && rm.text == localMsg.text) })
                    }
                    val allMessages = (remoteMessages + pendingLocal)
                        .distinctBy { it.messageId }
                        .sortedBy { it.timestamp }

                    currState.copy(messages = allMessages, isLoading = false)
                }
            }
    }

    /**
     * Sends a rich message
     */
    fun sendMessage(
        channelId: String,
        text: String,
        messageType: String = "TEXT",
        metaPayload: String? = null,
        isVoiceQuery: Boolean = false
    ) {
        if (text.isBlank() && metaPayload.isNullOrBlank()) return
        val state = _uiState.value
        val currentUid = state.currentUserId
        val currentUName = state.currentUserName
        val currentRole = state.currentUserRole

        viewModelScope.launch {
            var userId = currentUid.ifBlank {
                userRepository.getCurrentUserIdSync() ?: ""
            }
            if (userId.isBlank()) userId = "user_member"
            val localUser = if (userId.isNotBlank()) userDao.getUserById(userId) ?: userRepository.syncUserById(userId) else null
            var userName = currentUName.takeIf { it != "You" && it.isNotBlank() } ?: localUser?.name?.ifBlank { localUser.username } ?: "Member"
            var role = currentRole.takeIf { it.isNotBlank() && it != "MEMBER" } ?: localUser?.role ?: "MEMBER"

            // Resolve other participant ID
            val otherId = state.activeCounterparty?.userId?.takeIf { it.isNotBlank() }
                ?: state.activeLoan?.let { loan ->
                    if (loan.lenderId == userId) loan.borrowerId else loan.lenderId
                }?.takeIf { it.isNotBlank() }
                ?: if (channelId == "support_loanzo_assistant" || channelId.contains("support")) {
                    "LOANZO_BOT"
                } else if (channelId.startsWith("direct_")) {
                    val parts = channelId.removePrefix("direct_").split("_")
                    parts.firstOrNull { it != userId } ?: parts.lastOrNull() ?: ""
                } else if (channelId.startsWith("loan_")) {
                    val lId = channelId.removePrefix("loan_")
                    val l = loanDao.getLoanById(lId)
                    if (l != null) {
                        if (l.lenderId == userId) l.borrowerId else l.lenderId
                    } else ""
                } else ""

            if (localUser != null) {
                if (userName == "Member" || userName.isBlank()) userName = localUser.name.ifBlank { localUser.username }
                if (role == "MEMBER") role = localUser.role
            }

            // Optimistically insert user's message immediately into UI state
            val localNow = System.currentTimeMillis()
            val localMsgId = "local_" + java.util.UUID.randomUUID().toString().take(8)
            val optimisticMsg = FirestoreChatMessage(
                messageId = localMsgId,
                channelId = channelId,
                senderId = userId,
                senderName = userName,
                senderRole = role,
                recipientId = otherId,
                text = text.trim(),
                timestamp = localNow,
                isMe = true,
                status = "SENT",
                messageType = messageType,
                metaPayload = metaPayload
            )
            _uiState.update { currState ->
                currState.copy(messages = (currState.messages + optimisticMsg).distinctBy { it.messageId })
            }

            // If messaging LOANZO_BOT or support channel, dispatch 3-way Multi-AI race immediately
            if (channelId == "support_loanzo_assistant" || otherId == "LOANZO_BOT" || channelId.contains("support")) {
                dispatchAiAssistantResponse(channelId, userId, text.trim(), isVoiceQuery = isVoiceQuery)
            }

            try {
                firebaseManager.ensureFirebaseAuthSession()
                withTimeoutOrNull(8000L) {
                    val messageData = hashMapOf(
                        "channelId" to channelId,
                        "senderId" to userId,
                        "senderName" to userName,
                        "senderRole" to role,
                        "recipientId" to otherId,
                        "text" to text.trim(),
                        "timestamp" to localNow,
                        "status" to "SENT",
                        "messageType" to messageType,
                        "metaPayload" to metaPayload
                    )

                    // Write message to canonical channels collection
                    firestore.collection("channels").document(channelId).collection("messages").add(messageData).await()

                    // If active loan exists, mirror to loans/{loanId}/chat for backwards compatibility
                    if (state.activeLoan != null) {
                        try {
                            firestore.collection("loans").document(state.activeLoan.loanId).collection("chat").add(messageData).await()
                        } catch (mirrorErr: Exception) {
                            Log.d(TAG, "Loan mirror chat note: ${mirrorErr.message}")
                        }
                    }

                    // Update channel metadata with both participants guaranteed
                    val allParticipants = buildList {
                        if (userId.isNotBlank()) add(userId)
                        if (otherId.isNotBlank()) add(otherId)
                        if (channelId.startsWith("direct_")) {
                            val parts = channelId.removePrefix("direct_").split("_")
                            addAll(parts.filter { it.isNotBlank() })
                        }
                        if (channelId == "support_loanzo_assistant" || channelId.contains("support")) {
                            add("LOANZO_BOT")
                        }
                    }.distinct()

                    val channelDoc = hashMapOf(
                        "channelId" to channelId,
                        "channelType" to (if (channelId.startsWith("direct_")) "DIRECT" else if (channelId.contains("support")) "SUPPORT" else "LOAN"),
                        "loanId" to (state.activeLoan?.loanId ?: (if (channelId.startsWith("loan_")) channelId.removePrefix("loan_") else null)),
                        "lastMessage" to (if (messageType == "PROPOSAL") "Loan Proposal Sent" else text.trim()),
                        "lastTimestamp" to localNow,
                        "lastSenderId" to userId,
                        "participants" to allParticipants
                    )
                    firestore.collection("channels").document(channelId).set(channelDoc, SetOptions.merge()).await()

                    // Push direct cloud notification for recipient
                    if (otherId.isNotBlank() && otherId != "LOANZO_BOT" && otherId != userId) {
                        try {
                            val notifDocId = "notif_msg_${channelId}_${localNow}"
                            firestore.collection("notifications").document(notifDocId).set(
                                hashMapOf(
                                    "notificationId" to notifDocId,
                                    "userId" to otherId,
                                    "title" to "💬 Message from $userName",
                                    "message" to text.trim().take(120),
                                    "type" to "CHAT",
                                    "actionRoute" to "chat/$channelId?targetUserId=$userId",
                                    "isRead" to false,
                                    "timestamp" to localNow
                                )
                            )
                        } catch (notifErr: Exception) {
                            Log.d(TAG, "Chat notification note: ${notifErr.message}")
                        }
                    }

                    Log.d(TAG, "Message sent successfully to channel $channelId with participants $allParticipants")
                }

                // Immediately update local conversation summary so ChatHub reflects the new message without latency
                val lastPreview = if (messageType == "PROPOSAL") "Loan Proposal Sent" else text.trim()
                _uiState.update { currState ->
                    val currentList = currState.conversations.toMutableList()
                    val idx = currentList.indexOfFirst { it.channelId == channelId }
                    if (idx != -1) {
                        val existing = currentList[idx]
                        currentList[idx] = existing.copy(
                            lastMessage = lastPreview,
                            lastTimestamp = localNow
                        )
                    } else {
                        val cParty = currState.activeCounterparty
                        currentList.add(
                            ChatConversationSummary(
                                channelId = channelId,
                                channelType = if (channelId.startsWith("direct_")) "DIRECT" else if (channelId.startsWith("support_")) "SUPPORT" else "LOAN",
                                targetUserId = cParty?.userId ?: otherId,
                                targetUserName = cParty?.name ?: cParty?.username ?: "Community Member",
                                targetUserRole = cParty?.role ?: "MEMBER",
                                targetUserPhone = cParty?.phone ?: "",
                                targetUserKycStatus = cParty?.kycStatus ?: "VERIFIED",
                                targetUserProfilePhotoUri = cParty?.profilePhotoUri ?: "",
                                loanId = currState.activeLoan?.loanId,
                                loanTitle = currState.activeLoan?.let { "Loan INR ${it.sanctionedAmount.toInt()} (${it.status})" },
                                loanPrincipal = currState.activeLoan?.sanctionedAmount ?: 0.0,
                                lastMessage = lastPreview,
                                lastTimestamp = localNow,
                                unreadCount = 0,
                                isOnline = true
                            )
                        )
                    }
                    val sorted = currentList.sortedByDescending { it.lastTimestamp }
                    currState.copy(
                        conversations = sorted,
                        filteredConversations = applyFilterAndSearch(sorted, currState.selectedFilter, currState.searchQuery)
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cloud send message notice (applying local instant delivery): ${e.message}")
                val lastPreview = if (messageType == "PROPOSAL") "Loan Proposal Sent" else text.trim()
                _uiState.update { currState ->
                    val currentList = currState.conversations.toMutableList()
                    val idx = currentList.indexOfFirst { it.channelId == channelId }
                    if (idx != -1) {
                        val existing = currentList[idx]
                        currentList[idx] = existing.copy(
                            lastMessage = lastPreview,
                            lastTimestamp = localNow
                        )
                    }
                    val sorted = currentList.sortedByDescending { it.lastTimestamp }
                    currState.copy(
                        conversations = sorted,
                        filteredConversations = applyFilterAndSearch(sorted, currState.selectedFilter, currState.searchQuery)
                    )
                }
            }
        }
    }

    fun sendAudioMessage(channelId: String, durationSec: Int, audioFilePath: String? = null) {
        val display = "Voice message (${durationSec}s)"
        val payload = audioFilePath ?: durationSec.toString()
        sendMessage(channelId, display, "AUDIO", payload)
    }

    private fun dispatchAiAssistantResponse(
        channelId: String,
        userId: String,
        prompt: String,
        isVoiceQuery: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val thinkingMsgId = "ai_thinking_" + System.currentTimeMillis()
            val thinkingMsg = FirestoreChatMessage(
                messageId = thinkingMsgId,
                channelId = channelId,
                senderId = "LOANZO_BOT",
                senderName = "Loanzo AI Assistant",
                senderRole = "OFFICIAL_BOT",
                recipientId = userId,
                text = "Loanzo AI Assistant is thinking...",
                timestamp = System.currentTimeMillis(),
                isMe = false,
                status = "SENDING",
                messageType = "TEXT"
            )
            _uiState.update { it.copy(messages = (it.messages + thinkingMsg).distinctBy { m -> m.messageId }) }

            try {
                // 1. Gather User Account Grounding Context
                val user = if (userId.isNotBlank()) userDao.getUserById(userId) else null
                val loans = if (userId.isNotBlank()) {
                    withTimeoutOrNull(1500) { loanDao.getAllLoansForUser(userId).firstOrNull() } ?: emptyList()
                } else {
                    emptyList()
                }

                val userName = user?.name?.ifBlank { user.username } ?: "Member"
                val userRole = user?.role ?: "MEMBER"
                val kycStatus = user?.kycStatus ?: "PENDING"
                val activeLoans = loans.filter {
                    it.status == "ACTIVE" || it.status == "ACTIVE_SERVICING" || it.status == "TRANCHE_DISBURSEMENT" || it.status == "RESTRUCTURED"
                }

                val contextBuilder = StringBuilder()
                contextBuilder.append("User Name: $userName | Role: $userRole\n")
                contextBuilder.append("KYC Status: $kycStatus\n")
                contextBuilder.append("Total Loans on File: ${loans.size} (${activeLoans.size} currently active)\n")
                if (activeLoans.isNotEmpty()) {
                    contextBuilder.append("Active Loans Summary:\n")
                    activeLoans.take(3).forEach { loan ->
                        val userSide = if (loan.borrowerId == userId) "Borrower" else "Lender"
                        contextBuilder.append("• Loan #${loan.loanId.takeLast(6)} as $userSide: Principal ₹${loan.sanctionedAmount.toInt()}, Outstanding ₹${loan.outstandingAmount.toInt()}, Rate: ${loan.interestRate}% p.a., Tenure: ${loan.tenureMonths} mos, Status: ${loan.status}, Purpose: ${loan.purpose}\n")
                    }
                } else {
                    contextBuilder.append("Active Loans: None currently active. User has 0 outstanding debt.\n")
                }
                val userContext = contextBuilder.toString()

                // 2. Extract Recent Conversation History (up to 6 messages)
                val history = _uiState.value.messages
                    .filter { !it.messageId.startsWith("ai_thinking_") && it.text.isNotBlank() }
                    .takeLast(6)
                    .map { msg ->
                        val role = if (msg.senderId == "LOANZO_BOT" || msg.senderRole == "OFFICIAL_BOT") "assistant" else "user"
                        com.loanzo.app.data.ai.AiMessage(role = role, content = msg.text)
                    }

                // 3. Dispatch to Multi-AI Engine with user context & history
                val effectivePrompt = if (isVoiceQuery) {
                    "$prompt\n[Voice Assistant Mode: User is listening via audio. Respond conversationally in 1-2 crisp spoken sentences, followed by appropriate action tags.]"
                } else {
                    prompt
                }

                val raceResult = multiAiRaceEngine.generateChatResponse(
                    userPrompt = effectivePrompt,
                    userContext = userContext,
                    conversationHistory = history
                )
                val answer = when (raceResult) {
                    is com.loanzo.app.data.ai.AiRaceResult.Success -> raceResult.content
                    is com.loanzo.app.data.ai.AiRaceResult.Failure -> raceResult.fallbackContent
                }

                // If query came from voice assistant, synthesize and speak response out loud
                if (isVoiceQuery) {
                    voiceAssistantManager.speak(answer, answer)
                }

                // Allocate a deterministic Firestore document reference ID so doc.id matches finalAiMsg.messageId
                val botDocRef = firestore.collection("channels").document(channelId).collection("messages").document()
                val finalAiMsgId = botDocRef.id
                val finalTimestamp = System.currentTimeMillis()

                val finalAiMsg = FirestoreChatMessage(
                    messageId = finalAiMsgId,
                    channelId = channelId,
                    senderId = "LOANZO_BOT",
                    senderName = "Loanzo AI Assistant",
                    senderRole = "OFFICIAL_BOT",
                    recipientId = userId,
                    text = answer,
                    timestamp = finalTimestamp,
                    isMe = false,
                    status = "DELIVERED",
                    messageType = "TEXT",
                    metaPayload = null
                )

                // Immediately update UI with zero network latency
                _uiState.update { state ->
                    val newMessages = state.messages.filter { !it.messageId.startsWith("ai_thinking_") } + finalAiMsg
                    val currentList = state.conversations.toMutableList()
                    val idx = currentList.indexOfFirst { it.channelId == channelId }
                    if (idx != -1) {
                        val existing = currentList[idx]
                        currentList[idx] = existing.copy(
                            lastMessage = answer,
                            lastTimestamp = finalTimestamp,
                            unreadCount = 0
                        )
                    }
                    val sorted = currentList.sortedByDescending { it.lastTimestamp }
                    state.copy(
                        messages = newMessages.distinctBy { it.messageId },
                        conversations = sorted,
                        filteredConversations = applyFilterAndSearch(sorted, state.selectedFilter, state.searchQuery)
                    )
                }

                // Persist bot response into Cloud Firestore asynchronously without blocking local delivery
                val botMessageData = hashMapOf(
                    "channelId" to channelId,
                    "senderId" to "LOANZO_BOT",
                    "senderName" to "Loanzo AI Assistant",
                    "senderRole" to "OFFICIAL_BOT",
                    "recipientId" to userId,
                    "text" to answer,
                    "timestamp" to finalTimestamp,
                    "status" to "DELIVERED",
                    "messageType" to "TEXT",
                    "metaPayload" to null
                )

                try {
                    botDocRef.set(botMessageData).await()
                    firestore.collection("channels").document(channelId).set(
                        mapOf(
                            "channelId" to channelId,
                            "channelType" to "SUPPORT",
                            "lastMessage" to answer,
                            "lastTimestamp" to finalTimestamp,
                            "lastSenderId" to "LOANZO_BOT",
                            "participants" to listOf(userId, "LOANZO_BOT").filter { it.isNotBlank() }.distinct()
                        ),
                        SetOptions.merge()
                    ).await()
                    Log.d(TAG, "Successfully persisted AI assistant response to Cloud Firestore for channel $channelId (doc: $finalAiMsgId)")
                } catch (fbErr: Exception) {
                    Log.w(TAG, "Notice saving bot response to Cloud Firestore: ${fbErr.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in dispatchAiAssistantResponse", e)
                val fallback = "I encountered an issue processing your request. Please check your connection or tap mic to try again."
                if (isVoiceQuery) {
                    voiceAssistantManager.speak(fallback, fallback)
                }
                _uiState.update { state ->
                    val filtered = state.messages.filter { !it.messageId.startsWith("ai_thinking_") }
                    state.copy(messages = filtered)
                }
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

    
    fun seedDemoConversations(currentUserId: String = "") {
        // No-op: Demo chat seeding disabled completely.
    }

    /**
     * Starts listening for user's voice query.
     * If invoked in support/bot channel, posts query and response into chat.
     * If invoked in peer deal/direct chat, routes query directly to AI copilot without leaking to counterparty.
     */
    fun startVoiceAssistant(channelId: String, userId: String) {
        val isSupportChannel = channelId == "support_loanzo_assistant" || channelId.contains("support")
        voiceAssistantManager.startListening { transcribedText ->
            if (transcribedText.isNotBlank()) {
                if (isSupportChannel) {
                    sendMessage(
                        channelId = channelId,
                        text = transcribedText,
                        messageType = "TEXT",
                        metaPayload = null,
                        isVoiceQuery = true
                    )
                } else {
                    dispatchDirectVoiceQuery(userId = userId, prompt = transcribedText)
                }
            }
        }
    }

    private val voiceSessionHistory = mutableListOf<com.loanzo.app.data.ai.AiMessage>()

    private fun dispatchDirectVoiceQuery(userId: String, prompt: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Gather User Account Grounding Context + Active Loan Context
                val user = if (userId.isNotBlank()) userDao.getUserById(userId) else null
                val loans = if (userId.isNotBlank()) {
                    withTimeoutOrNull(1500) { loanDao.getAllLoansForUser(userId).firstOrNull() } ?: emptyList()
                } else {
                    emptyList()
                }
                val activeLoan = _uiState.value.activeLoan

                val userName = user?.name?.ifBlank { user.username } ?: "Member"
                val userRole = user?.role ?: "MEMBER"
                val kycStatus = user?.kycStatus ?: "PENDING"
                val kycBadges = buildList {
                    if (user?.panVerified == true) add("PAN Verified")
                    if (user?.aadhaarVerified == true) add("Aadhaar Verified")
                    if (user?.selfieVerified == true) add("Biometric Selfie Verified")
                    if (user?.upiVerified == true) add("UPI Linked (${user.upiId})")
                }
                val kycDetails = if (kycBadges.isNotEmpty()) "$kycStatus (${kycBadges.joinToString(", ")})" else kycStatus

                val activeBorrowerLoans = loans.filter {
                    it.borrowerId == userId && (it.status in listOf("ACTIVE", "ACTIVE_SERVICING", "TRANCHE_DISBURSEMENT", "RESTRUCTURED"))
                }
                val activeLenderLoans = loans.filter {
                    it.lenderId == userId && (it.status in listOf("ACTIVE", "ACTIVE_SERVICING", "TRANCHE_DISBURSEMENT", "RESTRUCTURED"))
                }

                val contextBuilder = StringBuilder()
                contextBuilder.append("User Name: $userName | Role: $userRole\n")
                contextBuilder.append("KYC Standing: $kycDetails\n")

                if (activeLoan != null) {
                    val userSide = if (activeLoan.borrowerId == userId) "Borrower" else "Lender"
                    val approxEmi = if (activeLoan.tenureMonths > 0) {
                        ((activeLoan.outstandingAmount * (1.0 + (activeLoan.interestRate / 100.0) * (activeLoan.tenureMonths / 12.0))) / activeLoan.tenureMonths).toInt()
                    } else 0
                    contextBuilder.append("Currently Viewed Loan #${activeLoan.loanId.takeLast(6)} ($userSide): Principal ₹${activeLoan.sanctionedAmount.toInt()}, Outstanding ₹${activeLoan.outstandingAmount.toInt()}, Approx EMI ₹$approxEmi/mo, Rate: ${activeLoan.interestRate}% p.a., Tenure: ${activeLoan.tenureMonths} mos, Status: ${activeLoan.status}, Purpose: ${activeLoan.purpose}\n")
                }

                if (activeBorrowerLoans.isNotEmpty()) {
                    val totalDebt = activeBorrowerLoans.sumOf { it.outstandingAmount }.toInt()
                    contextBuilder.append("Active Borrower Debt: ₹$totalDebt across ${activeBorrowerLoans.size} active loan(s):\n")
                    activeBorrowerLoans.take(3).forEach { l ->
                        val emi = if (l.tenureMonths > 0) {
                            ((l.outstandingAmount * (1.0 + (l.interestRate / 100.0) * (l.tenureMonths / 12.0))) / l.tenureMonths).toInt()
                        } else 0
                        contextBuilder.append("• Loan #${l.loanId.takeLast(6)} (${l.purpose}): Balance ₹${l.outstandingAmount.toInt()}, Monthly EMI ₹$emi, Rate: ${l.interestRate}%, Status: ${l.status}\n")
                    }
                } else if (userRole == "BORROWER" || userRole == "MEMBER") {
                    contextBuilder.append("Active Borrower Debt: None. User has ₹0 outstanding obligations.\n")
                }

                if (activeLenderLoans.isNotEmpty()) {
                    val totalDeployed = activeLenderLoans.sumOf { it.outstandingAmount }.toInt()
                    contextBuilder.append("Active Lending Capital: ₹$totalDeployed across ${activeLenderLoans.size} peer borrower(s).\n")
                }

                val userContext = contextBuilder.toString()

                val effectivePrompt = """
                    $prompt
                    [Voice Assistant Direct Query:
                    - You are Loanzo's in-app personal voice concierge.
                    - Respond conversationally in 1-2 crisp, human spoken sentences.
                    - Use user account context (loans, EMIs, KYC, role) to provide exact personalized answers.
                    - Append relevant action tags at the very end when applicable:
                      [ACTION:CALCULATOR] for EMI/interest calculation,
                      [ACTION:KYC] for identity/KYC check,
                      [ACTION:PORTFOLIO] for viewing portfolio/loans/EMIs,
                      [ACTION:MARKETPLACE] for browsing community offers,
                      [ACTION:CREATE_POST] for creating loan requests or lender capital offers,
                      [ACTION:SUPPORT] for support tickets or mediation desk,
                      [ACTION:NOTIFICATIONS] for alerts.
                    ]
                """.trimIndent()

                // Multi-turn conversation memory window (remembers prior session turns)
                val currentHistory = synchronized(voiceSessionHistory) { voiceSessionHistory.takeLast(6).toList() }

                val raceResult = multiAiRaceEngine.generateChatResponse(
                    userPrompt = effectivePrompt,
                    userContext = userContext,
                    conversationHistory = currentHistory
                )
                val answer = when (raceResult) {
                    is com.loanzo.app.data.ai.AiRaceResult.Success -> raceResult.content
                    is com.loanzo.app.data.ai.AiRaceResult.Failure -> raceResult.fallbackContent
                }

                // Update session memory with this turn
                synchronized(voiceSessionHistory) {
                    voiceSessionHistory.add(com.loanzo.app.data.ai.AiMessage("user", prompt))
                    voiceSessionHistory.add(com.loanzo.app.data.ai.AiMessage("assistant", answer))
                    while (voiceSessionHistory.size > 8) {
                        voiceSessionHistory.removeAt(0)
                    }
                }

                voiceAssistantManager.speak(answer, answer)

                // Archive Q&A in user's support channel for persistent record
                try {
                    val supportChannelId = "support_loanzo_assistant"
                    val now = System.currentTimeMillis()
                    val userMsgData = hashMapOf(
                        "channelId" to supportChannelId,
                        "senderId" to userId,
                        "senderName" to userName,
                        "senderRole" to userRole,
                        "recipientId" to "LOANZO_BOT",
                        "text" to prompt,
                        "timestamp" to now - 1,
                        "status" to "DELIVERED",
                        "messageType" to "TEXT"
                    )
                    val botMsgData = hashMapOf(
                        "channelId" to supportChannelId,
                        "senderId" to "LOANZO_BOT",
                        "senderName" to "Loanzo AI Assistant",
                        "senderRole" to "OFFICIAL_BOT",
                        "recipientId" to userId,
                        "text" to answer,
                        "timestamp" to now,
                        "status" to "DELIVERED",
                        "messageType" to "TEXT"
                    )
                    firestore.collection("channels").document(supportChannelId).collection("messages").add(userMsgData)
                    firestore.collection("channels").document(supportChannelId).collection("messages").add(botMsgData)
                } catch (e: Exception) {
                    Log.d(TAG, "Notice saving direct voice query to support log: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in dispatchDirectVoiceQuery", e)
                val fallback = "I encountered an issue processing your voice request. Please check your connection or tap mic to try again."
                voiceAssistantManager.speak(fallback, fallback)
            }
        }
    }

    fun stopVoiceListening() {
        voiceAssistantManager.stopListening()
    }

    fun stopVoiceSpeaking() {
        voiceAssistantManager.stopSpeaking()
    }

    fun readAloudMessage(rawText: String) {
        voiceAssistantManager.speak(rawText)
    }

    fun resetVoiceState() {
        voiceAssistantManager.resetState()
    }

    override fun onCleared() {
        super.onCleared()
        activeChatListener?.remove()
        conversationsListener?.remove()
        voiceAssistantManager.resetState()
    }
}
