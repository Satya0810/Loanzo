package com.loanzo.app.ui.loan

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.TelegramManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    channelId: String,
    loanId: String? = null,
    targetUserId: String? = null,
    onBack: () -> Unit,
    onViewLoanAgreement: ((String) -> Unit)? = null,
    chatViewModel: ChatViewModel = hiltViewModel(),
    translationViewModel: TranslationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val translationState by translationViewModel.uiState.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDealActionsSheet by remember { mutableStateOf(false) }

    // Hindi translation target
    val targetLanguage = "hi"
    var translatingMessageId by remember { mutableStateOf<String?>(null) }

    // Connect real-time Firestore listener
    LaunchedEffect(channelId, loanId, targetUserId) {
        chatViewModel.loadChat(channelId = channelId, loanId = loanId, targetUserId = targetUserId)
    }

    // Auto-scroll on new messages
    LaunchedEffect(chatState.messages.size) {
        if (chatState.messages.isNotEmpty()) {
            listState.animateScrollToItem(chatState.messages.size - 1)
        }
    }

    // Handle translation responses
    LaunchedEffect(translationState.translatedText) {
        if (translationState.translatedText.isNotBlank() && !translationState.isLoading && translatingMessageId != null) {
            chatViewModel.setTranslatedText(translatingMessageId!!, translationState.translatedText)
            translatingMessageId = null
        }
    }

    // Error handler
    LaunchedEffect(chatState.error) {
        chatState.error?.let {
            snackbarHostState.showSnackbar(it)
            chatViewModel.clearError()
        }
    }

    val isSupport = channelId == "support_loanzo_assistant" || targetUserId == "LOANZO_BOT"
    val counterpartyName = if (isSupport) "Loanzo AI Assistant" else chatState.activeCounterparty?.name ?: chatState.activeCounterparty?.username ?: "Counterparty"
    val counterpartyRole = if (isSupport) "OFFICIAL BOT" else chatState.activeCounterparty?.role ?: "MEMBER"
    val counterpartyPhone = chatState.activeCounterparty?.phone ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            Surface(
                                shape = CircleShape,
                                color = if (isSupport) Color(0xFF8B5CF6).copy(alpha = 0.15f) else Gold500.copy(alpha = 0.15f),
                                border = BorderStroke(1.5.dp, if (isSupport) Color(0xFF8B5CF6) else Gold500),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isSupport) {
                                        Icon(Icons.Default.SmartToy, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(20.dp))
                                    } else {
                                        Text(
                                            counterpartyName.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = Gold500,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                            // Active status dot
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Emerald400)
                                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                    .align(Alignment.BottomEnd)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = counterpartyName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (chatState.activeCounterparty?.kycStatus == "VERIFIED" || isSupport) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.Verified, null, tint = Gold500, modifier = Modifier.size(14.dp))
                                }
                            }

                            Text(
                                text = if (isSupport) "24/7 Smart Legal Assistant • Online" else "$counterpartyRole • P2P Verified",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = if (isSupport) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (counterpartyPhone.isNotBlank() && !isSupport) {
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$counterpartyPhone"))
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.Phone, contentDescription = "Call", tint = Emerald400)
                        }
                    }
                    if (isSupport) {
                        IconButton(onClick = { TelegramManager().openBotForLinking(context, chatState.currentUserId) }) {
                            Icon(Icons.Default.Send, contentDescription = "Open in Telegram", tint = Gold500)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // Quick Suggestion Chips
                val suggestions = listOf(
                    "When can we disburse the tranche?",
                    "Please upload the assaying certificate.",
                    "Payment of EMI initiated via UPI.",
                    "Can we schedule an agent visit?"
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(suggestions) { s ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.clickable {
                                inputText = s
                            }
                        ) {
                            Text(
                                text = s,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Input Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Deal Action / Attachment Button
                    IconButton(
                        onClick = { showDealActionsSheet = true },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Deal Options", tint = Gold500)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message or deal query...", fontSize = 13.sp) },
                        maxLines = 4,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold500,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                chatViewModel.sendMessage(channelId, inputText.trim(), "TEXT", null)
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank(),
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank()) Gold500 else MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank()) Navy900 else Gray400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Contextual Deal Banner
            chatState.activeLoan?.let { loan ->
                Surface(
                    shape = RoundedCornerShape(0.dp),
                    color = Gold500.copy(alpha = 0.08f),
                    border = BorderStroke(0.5.dp, Gold500.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🏷️ Deal #${loan.loanId.takeLast(6)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Gold500)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(shape = RoundedCornerShape(4.dp), color = Emerald400.copy(alpha = 0.2f)) {
                                    Text(loan.status, color = Emerald400, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                            }
                            Text("INR ${loan.sanctionedAmount.toInt()} • ${loan.interestRate}% p.a. • ${loan.tenureMonths}m", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        if (onViewLoanAgreement != null) {
                            OutlinedButton(
                                onClick = { onViewLoanAgreement(loan.loanId) },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Gold500),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Agreement", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Gold500)
                            }
                        }
                    }
                }
            }

            // Message Stream
            if (chatState.isLoading && chatState.messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Gold500)
                }
            } else if (chatState.messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Gold500.copy(alpha = 0.12f),
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ChatBubbleOutline, null, tint = Gold500, modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No messages yet", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isSupport) "Ask anything about loan contracts, EMI, or assaying." else "Send a message to start negotiating terms.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(chatState.messages, key = { it.messageId }) { msg ->
                        RichChatBubble(
                            message = msg,
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

    // Deal Action Quick Sheet
    if (showDealActionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDealActionsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text("Deal Actions & Templates", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(14.dp))

                val actions = listOf(
                    Triple("Official Loan Proposal", "Send verified interest & tenure terms for agreement", "PROPOSAL"),
                    Triple("Payment Transfer Notification", "Share UTR transaction acknowledgement", "RECEIPT"),
                    Triple("Request Assaying Inspection", "Ask certified field agent to visit for gold check", "INSPECTION")
                )

                actions.forEach { (title, desc, type) ->
                    Surface(
                        onClick = {
                            showDealActionsSheet = false
                            val templateText = when (type) {
                                "PROPOSAL" -> "Deal Proposal: Offering funding with legal Promissory Note under Sec 4 NI Act 1881."
                                "RECEIPT" -> "Payment Acknowledged: Transferred funds via UPI/IMPS."
                                "INSPECTION" -> "Doorstep Assaying Request: Please share availability for collateral verification."
                                else -> ""
                            }
                            chatViewModel.sendMessage(channelId, templateText, type, null)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = Gold500.copy(alpha = 0.15f), modifier = Modifier.size(36.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Gold500, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Backward-compatible overload
@Composable
fun ChatScreen(
    loanId: String,
    onBack: () -> Unit,
    chatViewModel: ChatViewModel = hiltViewModel(),
    translationViewModel: TranslationViewModel = hiltViewModel()
) = ChatScreen(
    channelId = if (loanId.startsWith("loan_")) loanId else "loan_$loanId",
    loanId = loanId,
    targetUserId = null,
    onBack = onBack,
    onViewLoanAgreement = null,
    chatViewModel = chatViewModel,
    translationViewModel = translationViewModel
)

@Composable
private fun RichChatBubble(
    message: FirestoreChatMessage,
    onTranslate: () -> Unit
) {
    val alignment = if (message.isMe) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (message.isMe) Gold500 else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (message.isMe) Navy900 else MaterialTheme.colorScheme.onSurface
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier.fillMaxWidth(0.82f),
            horizontalAlignment = if (message.isMe) Alignment.End else Alignment.Start
        ) {
            if (!message.isMe) {
                Text(
                    text = "${message.senderName} (${message.senderRole})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
                )
            }

            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.isMe) 16.dp else 4.dp,
                    bottomEnd = if (message.isMe) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(containerColor = bubbleColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Deal Action Card Header if special type
                    if (message.messageType == "PROPOSAL") {
                        Surface(shape = RoundedCornerShape(6.dp), color = Navy900.copy(alpha = 0.12f)) {
                            Text("OFFICIAL LOAN PROPOSAL", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = textColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    } else if (message.messageType == "RECEIPT") {
                        Surface(shape = RoundedCornerShape(6.dp), color = Emerald400.copy(alpha = 0.2f)) {
                            Text("TRANSACTION ACKNOWLEDGEMENT", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Emerald500, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text(text = message.text, color = textColor, fontSize = 13.sp, lineHeight = 18.sp)

                    // Inline translation
                    if (message.translatedText != null) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = textColor.copy(alpha = 0.2f))
                        Text(
                            text = message.translatedText,
                            color = textColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Timestamp and Ticks row
                    Row(
                        modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formattedTime,
                            fontSize = 9.sp,
                            color = textColor.copy(alpha = 0.7f)
                        )
                        if (message.isMe) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                Icons.Default.DoneAll,
                                contentDescription = "Delivered",
                                tint = textColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // Translation action trigger
            if (!message.isMe && message.translatedText == null) {
                TextButton(
                    onClick = onTranslate,
                    enabled = !message.isTranslating,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    if (message.isTranslating) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                    } else {
                        Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(14.dp), tint = Gold500)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Translate to Hindi", fontSize = 10.sp, color = Gold500)
                    }
                }
            }
        }
    }
}
