package com.loanzo.app.ui.loan

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class FirestoreChatMessage(
    val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isMe: Boolean = false,
    // Translation support
    val translatedText: String? = null,
    val isTranslating: Boolean = false
)

data class ChatUiState(
    val messages: List<FirestoreChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentUserId: String = "",
    val currentUserName: String = ""
)

@HiltViewModel
class ChatViewModel @Inject constructor() : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
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

    private var chatListener: ListenerRegistration? = null

    /**
     * Starts a real-time Firestore snapshot listener on the chat collection for a given loan.
     * Messages are stored under: loans/{loanId}/chat, ordered by timestamp.
     */
    fun loadChat(loanId: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: ""
        val userName = currentUser?.displayName ?: currentUser?.email ?: "You"

        _uiState.update { it.copy(isLoading = true, currentUserId = userId, currentUserName = userName) }

        // Remove any existing listener before attaching a new one
        chatListener?.remove()

        chatListener = firestore
            .collection("loans")
            .document(loanId)
            .collection("chat")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Chat listener error", error)
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    val senderId = doc.getString("senderId") ?: return@mapNotNull null
                    val text = doc.getString("text") ?: return@mapNotNull null
                    val senderName = doc.getString("senderName") ?: "Unknown"
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                    FirestoreChatMessage(
                        messageId = doc.id,
                        senderId = senderId,
                        senderName = senderName,
                        text = text,
                        timestamp = timestamp,
                        isMe = senderId == userId
                    )
                } ?: emptyList()

                _uiState.update { it.copy(messages = messages, isLoading = false) }
            }
    }

    /**
     * Sends a new message to the Firestore chat collection for a given loan.
     */
    fun sendMessage(loanId: String, text: String) {
        if (text.isBlank()) return
        val state = _uiState.value

        viewModelScope.launch {
            try {
                val messageData = hashMapOf(
                    "senderId" to state.currentUserId,
                    "senderName" to state.currentUserName,
                    "text" to text.trim(),
                    "timestamp" to System.currentTimeMillis()
                )

                firestore
                    .collection("loans")
                    .document(loanId)
                    .collection("chat")
                    .add(messageData)
                    .await()

                Log.d(TAG, "Message sent successfully to loan $loanId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message", e)
                _uiState.update { it.copy(error = "Failed to send message: ${e.message}") }
            }
        }
    }

    /**
     * Updates a specific message with its translated text.
     */
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

    /**
     * Marks a specific message as currently being translated.
     */
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
        chatListener?.remove()
    }
}
