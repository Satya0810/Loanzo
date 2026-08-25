package com.loanzo.app.ui.loan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loanzo.app.ui.theme.Gold500
import com.loanzo.app.ui.theme.Navy900
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String,
    val text: String,
    val isMe: Boolean,
    val translatedText: String? = null,
    val isTranslating: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    loanId: String,
    onBack: () -> Unit,
    viewModel: TranslationViewModel = hiltViewModel()
) {
    var messages by remember { mutableStateOf(listOf(
        ChatMessage("1", "Hello, please upload the signed document for tranche 1.", isMe = false),
        ChatMessage("2", "I will upload it by tomorrow morning.", isMe = true)
    )) }
    var inputText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    
    // For demonstration, we'll translate to Hindi by default when requested
    val targetLanguage = "hi"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat with Lender") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("< Back") }
                }
            )
        },
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
                            messages = messages + ChatMessage(
                                id = System.currentTimeMillis().toString(),
                                text = inputText,
                                isMe = true
                            )
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(
                    message = msg,
                    onTranslate = {
                        val index = messages.indexOf(msg)
                        if (index != -1) {
                            val updated = messages.toMutableList()
                            updated[index] = msg.copy(isTranslating = true)
                            messages = updated
                            
                            coroutineScope.launch {
                                // Direct API call to translate this specific message
                                viewModel.translate(msg.text, targetLanguage)
                            }
                        }
                    }
                )
            }
        }
    }
    
    // Watch for translation results in the ViewModel and apply them to the translating message
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state.translatedText) {
        if (state.translatedText.isNotBlank() && !state.isLoading) {
            val translatingIndex = messages.indexOfFirst { it.isTranslating }
            if (translatingIndex != -1) {
                val updated = messages.toMutableList()
                updated[translatingIndex] = updated[translatingIndex].copy(
                    translatedText = state.translatedText,
                    isTranslating = false
                )
                messages = updated
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
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
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = textColor.copy(alpha = 0.2f))
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
