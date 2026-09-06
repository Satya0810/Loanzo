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
                        lastMessage = "Deal chat for Loan #${loan.loanId.takeLast(6)} (${loan.status})",
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
                    Log.e(TAG, "Chat listener error", error)
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
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

                _uiState.update { it.copy(messages = messages, isLoading = false) }
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
                Log.e(TAG, "Failed to send message", e)
                _uiState.update { it.copy(error = "Failed to send message: ${e.message}") }
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

    override fun onCleared() {
        super.onCleared()
        activeChatListener?.remove()
        conversationsListener?.remove()
    }
}
