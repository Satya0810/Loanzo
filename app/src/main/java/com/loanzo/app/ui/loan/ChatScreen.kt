package com.loanzo.app.ui.loan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loanzo.app.ui.theme.Gold500
import com.loanzo.app.ui.theme.Navy900
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    loanId: String,
    onBack: () -> Unit,
    chatViewModel: ChatViewModel = hiltViewModel(),
    translationViewModel: TranslationViewModel = hiltViewModel()
) {
    val chatState by chatViewModel.uiState.collectAsState()
    val translationState by translationViewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // For demonstration, translate to Hindi by default when requested
    val targetLanguage = "hi"

    // Track which message is currently being translated
    var translatingMessageId by remember { mutableStateOf<String?>(null) }

    // Start the real-time listener
    LaunchedEffect(loanId) {
        chatViewModel.loadChat(loanId)
    }

    // Auto-scroll to the bottom when new messages arrive
    LaunchedEffect(chatState.messages.size) {
        if (chatState.messages.isNotEmpty()) {
            listState.animateScrollToItem(chatState.messages.size - 1)
        }
    }

    // Watch for translation results and apply them to the correct message
    LaunchedEffect(translationState.translatedText) {
        if (translationState.translatedText.isNotBlank() && !translationState.isLoading && translatingMessageId != null) {
            chatViewModel.setTranslatedText(translatingMessageId!!, translationState.translatedText)
            translatingMessageId = null
        }
    }

    // Show errors
    LaunchedEffect(chatState.error) {
        chatState.error?.let {
            snackbarHostState.showSnackbar(it)
            chatViewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            chatViewModel.sendMessage(loanId, inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier.background(Gold500, RoundedCornerShape(24.dp))
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Navy900)
                }
            }
        }
    ) { padding ->
        if (chatState.isLoading && chatState.messages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (chatState.messages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No messages yet.\nStart a conversation!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(chatState.messages, key = { it.messageId }) { msg ->
                    ChatBubble(
                        message = ChatMessage(
                            id = msg.messageId,
                            text = msg.text,
                            isMe = msg.isMe,
                            translatedText = msg.translatedText,
                            isTranslating = msg.isTranslating
                        ),
                        senderName = if (!msg.isMe) msg.senderName else null,
                        onTranslate = {
                            translatingMessageId = msg.messageId
                            chatViewModel.setTranslating(msg.messageId)
                            coroutineScope.launch {
                                translationViewModel.translate(msg.text, targetLanguage)
                            }
                        }
                    )
                }
            }
        }
    }
}

data class ChatMessage(
    val id: String,
    val text: String,
    val isMe: Boolean,
    val translatedText: String? = null,
    val isTranslating: Boolean = false
)

@Composable
fun ChatBubble(
    message: ChatMessage,
    senderName: String? = null,
    onTranslate: () -> Unit
) {
    val alignment = if (message.isMe) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (message.isMe) Gold500 else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (message.isMe) Navy900 else MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier.fillMaxWidth(0.8f),
            horizontalAlignment = if (message.isMe) Alignment.End else Alignment.Start
        ) {
            // Show sender name for messages from the other party
            if (senderName != null) {
                Text(
                    text = senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                )
            }

            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.isMe) 16.dp else 4.dp,
                    bottomEnd = if (message.isMe) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(containerColor = bgColor)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(message.text, color = textColor)
                    
                    if (message.translatedText != null) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = textColor.copy(alpha = 0.2f))
                        Text(message.translatedText, color = textColor)
                    }
                }
            }
            
            // Translate action button
            if (message.translatedText == null) {
                TextButton(onClick = onTranslate, enabled = !message.isTranslating) {
                    if (message.isTranslating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Translate", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

