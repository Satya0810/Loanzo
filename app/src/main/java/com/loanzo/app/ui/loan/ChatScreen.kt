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
import com.loanzo.app.ui.components.LoanzoText as Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.SimpleDateFormat
import java.util.*

data class ChatAction(
    val type: String,
    val label: String,
    val icon: ImageVector
)

fun parseChatActions(rawText: String): Pair<String, List<ChatAction>> {
    val actions = mutableListOf<ChatAction>()
    var text = rawText

    if (text.contains("[ACTION:CALCULATOR]")) {
        actions.add(ChatAction("CALCULATOR", "Open Loan Calculator", Icons.Default.Calculate))
        text = text.replace("[ACTION:CALCULATOR]", "")
    }
    if (text.contains("[ACTION:KYC]")) {
        actions.add(ChatAction("KYC", "Complete KYC", Icons.Default.VerifiedUser))
        text = text.replace("[ACTION:KYC]", "")
    }
    if (text.contains("[ACTION:MARKETPLACE]")) {
        actions.add(ChatAction("MARKETPLACE", "Explore Marketplace", Icons.Default.Groups))
        text = text.replace("[ACTION:MARKETPLACE]", "")
    }
    if (text.contains("[ACTION:PORTFOLIO]")) {
        actions.add(ChatAction("PORTFOLIO", "Smart Portfolio", Icons.Default.PieChart))
        text = text.replace("[ACTION:PORTFOLIO]", "")
    }
    if (text.contains("[ACTION:CREATE_POST]")) {
        actions.add(ChatAction("CREATE_POST", "Post Loan Request", Icons.Default.AddCircle))
        text = text.replace("[ACTION:CREATE_POST]", "")
    }
    if (text.contains("[ACTION:TELEGRAM]")) {
        actions.add(ChatAction("TELEGRAM", "Telegram Alerts", Icons.Default.Send))
        text = text.replace("[ACTION:TELEGRAM]", "")
    }

    return Pair(text.trim(), actions)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    channelId: String,
    loanId: String? = null,
    targetUserId: String? = null,
    onBack: () -> Unit,
    onViewLoanAgreement: ((String) -> Unit)? = null,
    onNavigateToCalculator: (() -> Unit)? = null,
    onNavigateToKyc: (() -> Unit)? = null,
    onNavigateToMarketplace: (() -> Unit)? = null,
    onNavigateToPortfolio: (() -> Unit)? = null,
    onNavigateToCreatePost: (() -> Unit)? = null,
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

    // Error handler
    LaunchedEffect(chatState.error) {
        chatState.error?.let {
            snackbarHostState.showSnackbar(it)
            chatViewModel.clearError()
        }
    }

    val isSupport = channelId == "support_loanzo_assistant" || targetUserId == "LOANZO_BOT" || channelId.contains("support")
    val counterpartyName = if (isSupport) "Loanzo AI Assistant" else chatState.activeCounterparty?.name ?: chatState.activeCounterparty?.username ?: "Counterparty"
    val counterpartyRole = if (isSupport) "OFFICIAL BOT" else chatState.activeCounterparty?.role ?: "MEMBER"
    val counterpartyPhone = chatState.activeCounterparty?.phone ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
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
                        IconButton(onClick = { TelegramManager.instance.openBotForLinking(context, chatState.currentUserId) }) {
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
                val suggestions = if (isSupport) {
                    listOf(
                        "📊 My Active Loans & Next EMI",
                        "🚀 How do I apply for a loan?",
                        "💼 How to lend & earn interest?",
                        "🧮 Open Loan Calculator",
                        "🛡️ KYC Verification Guide",
                        "🤝 How does Marketplace work?"
                    )
                } else {
                    listOf(
                        "When can we disburse the tranche?",
                        "Please upload the assaying certificate.",
                        "Payment of EMI initiated via UPI.",
                        "Can we schedule an agent visit?"
                    )
                }
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
                    items(chatState.messages, key = { it.messageId.ifBlank { "${it.timestamp}_${it.senderId}" } }) { msg ->
                        RichChatBubble(
                            message = msg,
                            onTranslate = {
                                chatViewModel.translateMessage(msg.messageId, msg.text, targetLanguage)
                            },
                            onActionClick = { actionType ->
                                when (actionType) {
                                    "CALCULATOR" -> onNavigateToCalculator?.invoke()
                                    "KYC" -> onNavigateToKyc?.invoke()
                                    "MARKETPLACE" -> onNavigateToMarketplace?.invoke()
                                    "PORTFOLIO" -> onNavigateToPortfolio?.invoke()
                                    "CREATE_POST" -> onNavigateToCreatePost?.invoke()
                                    "TELEGRAM" -> TelegramManager.instance.openBotForLinking(context, chatState.currentUserId)
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
    onNavigateToCalculator: (() -> Unit)? = null,
    onNavigateToKyc: (() -> Unit)? = null,
    onNavigateToMarketplace: (() -> Unit)? = null,
    onNavigateToPortfolio: (() -> Unit)? = null,
    onNavigateToCreatePost: (() -> Unit)? = null,
    chatViewModel: ChatViewModel = hiltViewModel(),
    translationViewModel: TranslationViewModel = hiltViewModel()
) = ChatScreen(
    channelId = if (loanId.startsWith("loan_")) loanId else "loan_$loanId",
    loanId = loanId,
    targetUserId = null,
    onBack = onBack,
    onViewLoanAgreement = null,
    onNavigateToCalculator = onNavigateToCalculator,
    onNavigateToKyc = onNavigateToKyc,
    onNavigateToMarketplace = onNavigateToMarketplace,
    onNavigateToPortfolio = onNavigateToPortfolio,
    onNavigateToCreatePost = onNavigateToCreatePost,
    chatViewModel = chatViewModel,
    translationViewModel = translationViewModel
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RichChatBubble(
    message: FirestoreChatMessage,
    onTranslate: () -> Unit,
    onActionClick: (String) -> Unit = {}
) {
    val alignment = if (message.isMe) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (message.isMe) Gold500 else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (message.isMe) Navy900 else MaterialTheme.colorScheme.onSurface
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }
    val isThinking = message.messageId.startsWith("ai_thinking_") || (message.senderId == "LOANZO_BOT" && message.status == "SENDING")

    val (displayText, actions) = remember(message.text) {
        if (message.senderId == "LOANZO_BOT" || message.senderRole == "OFFICIAL_BOT") {
            parseChatActions(message.text)
        } else {
            Pair(message.text, emptyList())
        }
    }

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

                    if (isThinking) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF8B5CF6)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = displayText,
                                color = textColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 18.sp
                            )
                        }
                    } else {
                        Text(text = displayText, color = textColor, fontSize = 13.sp, lineHeight = 18.sp)
                    }

                    // Inline translation
                    if (message.translatedText != null && !isThinking) {
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

            // Interactive Action Buttons for Bot Responses
            if (actions.isNotEmpty() && !isThinking) {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                ) {
                    actions.forEach { action ->
                        Surface(
                            onClick = { onActionClick(action.type) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                            tonalElevation = 2.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = action.icon,
                                    contentDescription = action.label,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = action.label,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Translation action trigger
            if (!message.isMe && message.translatedText == null && !isThinking) {
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
