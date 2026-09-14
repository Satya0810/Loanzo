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
        return emptyList()
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
    private val firebaseManager: com.loanzo.app.data.firebase.FirebaseManager
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
        val currentUser = FirebaseAuth.getInstance().currentUser
        val initialUserId = _uiState.value.currentUserId.ifBlank { currentUser?.uid ?: "" }
        val initialUserName = _uiState.value.currentUserName.ifBlank { currentUser?.displayName ?: "You" }

        _uiState.update {
            it.copy(
                isLoading = it.messages.isEmpty() && it.activeChannelId != channelId,
                activeChannelId = channelId,
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
                userDao.getUserById(targetUserId) ?: userRepository.syncUserById(targetUserId)
            } else if (loan != null) {
                val cId = if (loan.lenderId == resolvedUserId) loan.borrowerId else loan.lenderId
                userDao.getUserById(cId) ?: userRepository.syncUserById(cId)
            } else if (channelId.startsWith("direct_")) {
                val parts = channelId.removePrefix("direct_").split("_")
                val otherId = parts.firstOrNull { it != resolvedUserId } ?: parts.lastOrNull() ?: ""
                if (otherId.isNotBlank()) userDao.getUserById(otherId) ?: userRepository.syncUserById(otherId) else null
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

        // Connect real-time Firestore listener to canonical channels/{channelId}/messages
        val collectionRef = firestore.collection("channels").document(channelId).collection("messages")

        activeChatListener = collectionRef
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Chat listener error (channel: $channelId)", error)
                    _uiState.update { it.copy(isLoading = false) }
                    return@addSnapshotListener
                }

                val currentUid = _uiState.value.currentUserId.ifBlank {
                    FirebaseAuth.getInstance().currentUser?.uid ?: ""
                }

                val isSupportChannel = channelId == "support_loanzo_assistant" || channelId.contains("support") || targetUserId == "LOANZO_BOT"

                val remoteMessages = snapshot?.documents?.mapNotNull { doc ->
                    val senderId = doc.getString("senderId") ?: return@mapNotNull null
                    val text = doc.getString("text") ?: ""
                    val senderName = doc.getString("senderName") ?: "User"
                    val senderRole = doc.getString("senderRole") ?: "MEMBER"
                    val recipientId = doc.getString("recipientId") ?: ""
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    val status = doc.getString("status") ?: "DELIVERED"
                    val messageType = doc.getString("messageType") ?: "TEXT"
                    val metaPayload = doc.getString("metaPayload")

                    val fbAuthUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    val isMe = (currentUid.isNotBlank() && senderId == currentUid) ||
                            (fbAuthUid.isNotBlank() && senderId == fbAuthUid) ||
                            (isSupportChannel && senderId != "LOANZO_BOT" && senderRole != "OFFICIAL_BOT")

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
                    ?: "user_member"
            }
            if (userId.isBlank()) userId = "user_member"
            var userName = currentUName.ifBlank { "Member" }
            var role = currentRole.ifBlank { "MEMBER" }

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

            val localUser = if (userId.isNotBlank()) userDao.getUserById(userId) else null
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
                dispatchAiAssistantResponse(channelId, userId, text.trim())
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

    private fun dispatchAiAssistantResponse(channelId: String, userId: String, prompt: String) {
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
            val raceResult = multiAiRaceEngine.generateChatResponse(
                userPrompt = prompt,
                userContext = userContext,
                conversationHistory = history
            )
            val answer = when (raceResult) {
                is com.loanzo.app.data.ai.AiRaceResult.Success -> raceResult.content
                is com.loanzo.app.data.ai.AiRaceResult.Failure -> raceResult.fallbackContent
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

    override fun onCleared() {
        super.onCleared()
        activeChatListener?.remove()
        conversationsListener?.remove()
    }
}
