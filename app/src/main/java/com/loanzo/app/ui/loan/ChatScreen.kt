package com.loanzo.app.ui.loan

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loanzo.app.ui.theme.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.loanzo.app.data.voice.VoiceState
import com.loanzo.app.ui.components.VoiceAssistantBottomSheet
import com.loanzo.app.ui.components.LoanzoAvatar
import java.text.SimpleDateFormat
import java.util.*
import com.loanzo.app.util.TelegramManager
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.vector.ImageVector

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

    val isSupport = channelId == "support_loanzo_assistant" || targetUserId == "LOANZO_BOT" || channelId.contains("support")

    // AI Voice Assistant & Audio Recording State
    val voiceState by chatViewModel.voiceState.collectAsStateWithLifecycle()
    var showVoiceAssistantSheet by remember { mutableStateOf(false) }
    var isRecordingVoiceNote by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(isRecordingVoiceNote) {
        if (isRecordingVoiceNote) {
            recordingSeconds = 0
            while (isRecordingVoiceNote) {
                delay(1000L)
                recordingSeconds++
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (isSupport) {
                showVoiceAssistantSheet = true
                chatViewModel.startVoiceAssistant(channelId, chatState.currentUserId)
            } else {
                isRecordingVoiceNote = true
            }
        } else {
            coroutineScope.launch {
                val msg = if (isSupport) "Microphone permission is required to use Voice Assistant." else "Microphone permission is required to record voice notes."
                snackbarHostState.showSnackbar(msg)
            }
        }
    }


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

    val counterpartyName = remember(isSupport, chatState.activeCounterparty, chatState.messages, chatState.activeLoan, chatState.currentUserId) {
        if (isSupport) {
            "Loanzo AI Assistant"
        } else {
            chatState.activeCounterparty?.name?.takeIf { it.isNotBlank() && !it.equals("counterparty", ignoreCase = true) }
                ?: chatState.activeCounterparty?.username?.takeIf { it.isNotBlank() && !it.equals("counterparty", ignoreCase = true) }
                ?: chatState.messages.firstOrNull { !it.isMe && it.senderName.isNotBlank() && it.senderName != "You" && !it.senderName.equals("counterparty", ignoreCase = true) }?.senderName
                ?: if (chatState.activeLoan != null) {
                    val isMeLender = chatState.currentUserId == chatState.activeLoan?.lenderId
                    if (isMeLender) "Borrower" else "Lender"
                } else "Member"
        }
    }
    val counterpartyRole = if (isSupport) "OFFICIAL BOT" else chatState.activeCounterparty?.role ?: if (chatState.activeLoan != null) (if (chatState.currentUserId == chatState.activeLoan?.lenderId) "BORROWER" else "LENDER") else "MEMBER"
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
                            if (isSupport) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                                    border = BorderStroke(1.5.dp, Color(0xFF8B5CF6)),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.SmartToy, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(20.dp))
                                    }
                                }
                            } else {
                                LoanzoAvatar(
                                    user = chatState.activeCounterparty,
                                    avatarModel = if (!chatState.activeCounterparty?.profilePhotoUri.isNullOrBlank()) chatState.activeCounterparty?.profilePhotoUri else targetUserId,
                                    size = 38.dp,
                                    showVerifiedBadge = false
                                )
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
                if (isRecordingVoiceNote) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(26.dp))
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "rec_pulse")
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "rec_alpha"
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .graphicsLayer { alpha = pulseAlpha }
                                .clip(CircleShape)
                                .background(Red400)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Recording audio: %02d:%02d".format(recordingSeconds / 60, recordingSeconds % 60),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                isRecordingVoiceNote = false
                                recordingSeconds = 0
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Cancel Recording", tint = Red400, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                val sec = recordingSeconds.coerceAtLeast(1)
                                isRecordingVoiceNote = false
                                recordingSeconds = 0
                                chatViewModel.sendAudioMessage(channelId, sec)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Gold500)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Audio Note", tint = Navy900, modifier = Modifier.size(18.dp))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
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

                        Spacer(modifier = Modifier.width(6.dp))

                        // Microphone Button
                        IconButton(
                            onClick = {
                                val hasAudioPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.RECORD_AUDIO
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                if (hasAudioPermission) {
                                    if (isSupport) {
                                        showVoiceAssistantSheet = true
                                        chatViewModel.startVoiceAssistant(channelId, chatState.currentUserId)
                                    } else {
                                        isRecordingVoiceNote = true
                                    }
                                } else {
                                    audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Gold500.copy(alpha = 0.15f))
                                .semantics {
                                    role = Role.Button
                                    contentDescription = if (isSupport) "Open AI Voice Assistant" else "Record Audio Message"
                                }
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = null,
                                tint = Gold500,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

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
                            onReadAloud = {
                                chatViewModel.readAloudMessage(msg.text)
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

    // Accessible AI Voice Assistant Bottom Sheet (restricted strictly to Support chat)
    if (showVoiceAssistantSheet && isSupport) {
        VoiceAssistantBottomSheet(
            voiceState = voiceState,
            onDismiss = {
                showVoiceAssistantSheet = false
                chatViewModel.resetVoiceState()
            },
            onMicClick = {
                if (voiceState is VoiceState.Listening) {
                    chatViewModel.stopVoiceListening()
                } else {
                    chatViewModel.startVoiceAssistant(channelId, chatState.currentUserId)
                }
            },
            onStopSpeaking = {
                chatViewModel.stopVoiceSpeaking()
            },
            onReplayAudio = {
                (voiceState as? VoiceState.Speaking)?.let { state ->
                    chatViewModel.readAloudMessage(state.spokenText.ifBlank { state.displayText })
                }
            },
            onNavigateToCalculator = onNavigateToCalculator,
            onNavigateToKyc = onNavigateToKyc,
            onNavigateToMarketplace = onNavigateToMarketplace,
            onNavigateToPortfolio = onNavigateToPortfolio,
            onNavigateToCreatePost = onNavigateToCreatePost
        )
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
    onReadAloud: (() -> Unit)? = null,
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
        Row(
            modifier = Modifier.fillMaxWidth(0.90f),
            horizontalArrangement = if (message.isMe) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            // Counterparty / Bot Avatar on the Left
            if (!message.isMe) {
                val isBot = message.senderId == "LOANZO_BOT" || message.senderRole == "OFFICIAL_BOT"
                if (isBot) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF8B5CF6).copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, Color(0xFF8B5CF6)),
                        modifier = Modifier
                            .size(32.dp)
                            .padding(bottom = 2.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.SmartToy, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    LoanzoAvatar(
                        avatarModel = message.senderId,
                        size = 32.dp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Column(
                modifier = Modifier.weight(1f, fill = false),
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

                    if (message.messageType == "AUDIO") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (message.isMe) Navy900.copy(alpha = 0.15f) else Gold500.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { onReadAloud?.invoke() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Play voice message",
                                        tint = textColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    val waveBars = listOf(8, 14, 20, 12, 18, 22, 16, 10, 18, 12, 22, 14, 8)
                                    waveBars.forEach { h ->
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(h.dp)
                                                .clip(RoundedCornerShape(1.5.dp))
                                                .background(textColor.copy(alpha = 0.65f))
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = displayText,
                                    color = textColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else if (isThinking) {
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



                    // Timestamp, Ticks, and Read-Aloud Speaker row
                    Row(
                        modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isThinking && message.text.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable { onReadAloud?.invoke() }
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = "Read message aloud"
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = textColor.copy(alpha = 0.75f),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(2.dp))
                        }

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

            // Translation button removed
        }

        // Outgoing message avatar on the right
        if (message.isMe) {
            Spacer(modifier = Modifier.width(6.dp))
            LoanzoAvatar(
                avatarModel = message.senderId,
                size = 28.dp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}
}
