package com.loanzo.app.ui.loan

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loanzo.app.data.entity.UserEntity
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.TelegramManager
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHubScreen(
    chatViewModel: ChatViewModel,
    currentUserId: String,
    onBack: () -> Unit,
    onOpenChat: (channelId: String, loanId: String?, targetUserId: String?) -> Unit
) {
    val context = LocalContext.current
    val state by chatViewModel.uiState.collectAsStateWithLifecycle()
    var showNewChatDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotBlank()) {
            chatViewModel.initUser(currentUserId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Messages & Deals",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "P2P Social Lending Communications",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { chatViewModel.loadUserConversations(currentUserId) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { TelegramManager().openBotForLinking(context, currentUserId) }) {
                        Icon(Icons.Default.SupportAgent, contentDescription = "Telegram Assistant", tint = Gold500)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNewChatDialog = true },
                containerColor = Gold500,
                contentColor = Navy900,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.AddComment, contentDescription = null) },
                text = { Text("New Chat", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Box
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { chatViewModel.setSearchQuery(it) },
                placeholder = { Text("Search chats, people, loans...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Gray400) },
                trailingIcon = {
                    if (state.searchQuery.isNotBlank()) {
                        IconButton(onClick = { chatViewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, null, tint = Gray400)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Gold500,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Filter Chips Row
            val filters = listOf("ALL" to "All", "DEALS" to "Deal Chats", "DIRECT" to "Direct P2P", "SUPPORT" to "Support")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                items(filters) { (key, label) ->
                    val isSelected = state.selectedFilter == key
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Gold500 else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (isSelected) Gold500 else MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.clickable { chatViewModel.setFilter(key) }
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Navy900 else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

            // Conversations List
            if (state.isLoading && state.conversations.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Gold500)
                }
            } else if (state.filteredConversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Gold500.copy(alpha = 0.12f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Forum, null, tint = Gold500, modifier = Modifier.size(36.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (state.searchQuery.isNotBlank()) "No conversations match '${state.searchQuery}'" else "No conversations yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Start a 1-on-1 chat with another Loanzo member, inspect deals, or message support.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showNewChatDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Navy900)
                        ) {
                            Icon(Icons.Default.AddComment, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start New Chat", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(state.filteredConversations, key = { it.channelId }) { conv ->
                        ConversationItem(
                            conversation = conv,
                            onClick = {
                                onOpenChat(conv.channelId, conv.loanId, conv.targetUserId)
                            }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 76.dp)
                        )
                    }
                }
            }
        }
    }

    if (showNewChatDialog) {
        NewChatBottomSheet(
            chatViewModel = chatViewModel,
            currentUserId = currentUserId,
            onDismiss = { showNewChatDialog = false },
            onSelectUser = { targetUser ->
                showNewChatDialog = false
                val directChannel = ChatViewModel.getDirectChannelId(currentUserId, targetUser.userId)
                onOpenChat(directChannel, null, targetUser.userId)
            },
            onSelectSupport = {
                showNewChatDialog = false
                onOpenChat("support_loanzo_assistant", null, "LOANZO_BOT")
            }
        )
    }
}

@Composable
private fun ConversationItem(
    conversation: ChatConversationSummary,
    onClick: () -> Unit
) {
    val roleColor = when (conversation.targetUserRole) {
        "LENDER" -> Gold500
        "BORROWER" -> Emerald400
        "AGENT" -> BrandRoyalBlue
        "OFFICIAL_BOT" -> Color(0xFF8B5CF6)
        else -> MaterialTheme.colorScheme.primary
    }

    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
    val formattedTime = remember(conversation.lastTimestamp) {
        val now = System.currentTimeMillis()
        val diff = now - conversation.lastTimestamp
        if (diff < 24 * 60 * 60 * 1000) {
            timeFormat.format(Date(conversation.lastTimestamp))
        } else {
            dateFormat.format(Date(conversation.lastTimestamp))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar Box with Online indicator
        Box {
            Surface(
                shape = CircleShape,
                color = roleColor.copy(alpha = 0.15f),
                border = BorderStroke(1.5.dp, roleColor.copy(alpha = 0.6f)),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (conversation.channelType == "SUPPORT") {
                        Icon(Icons.Default.SmartToy, null, tint = roleColor, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = conversation.targetUserName.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = roleColor
                        )
                    }
                }
            }
            if (conversation.isOnline) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Emerald400)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = conversation.targetUserName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (conversation.targetUserKycStatus == "VERIFIED") {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = "Verified KYC",
                            tint = Gold500,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = formattedTime,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Optional Loan context pill
            if (conversation.loanTitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Gold500.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = conversation.loanTitle,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Gold500,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.lastMessage.ifBlank { "Tap to start messaging" },
                    fontSize = 12.sp,
                    color = if (conversation.unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (conversation.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = Gold500,
                        modifier = Modifier.size(18.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${conversation.unreadCount}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewChatBottomSheet(
    chatViewModel: ChatViewModel,
    currentUserId: String,
    onDismiss: () -> Unit,
    onSelectUser: (UserEntity) -> Unit,
    onSelectSupport: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val state by chatViewModel.uiState.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Start a New Conversation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    chatViewModel.searchUsersToChat(it)
                },
                placeholder = { Text("Search by name, username, or phone...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Option: Loanzo AI Assistant
            Surface(
                onClick = onSelectSupport,
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF5F3FF),
                border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = CircleShape, color = Color(0xFF8B5CF6), modifier = Modifier.size(36.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.SmartToy, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Loanzo Assistant (@Loanzo_bot)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF4C1D95))
                        Text("24/7 Smart Legal Escrow & Loan AI", fontSize = 11.sp, color = Color(0xFF6D28D9))
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF8B5CF6))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("REGISTERED MEMBERS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Gray400)
            Spacer(modifier = Modifier.height(6.dp))

            if (state.isSearchingUsers) {
                Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Gold500)
                }
            } else if (state.searchUserResults.isEmpty() && searchQuery.isNotBlank()) {
                Text(
                    "No members found matching '$searchQuery'",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                val displayUsers = if (searchQuery.isNotBlank()) {
                    state.searchUserResults
                } else {
                    state.conversations.filter { it.channelType != "SUPPORT" }.map {
                        UserEntity(
                            userId = it.targetUserId,
                            username = it.targetUserName,
                            name = it.targetUserName,
                            email = "",
                            phone = it.targetUserPhone,
                            role = it.targetUserRole,
                            kycStatus = it.targetUserKycStatus
                        )
                    }.distinctBy { it.userId }
                }

                if (displayUsers.isEmpty()) {
                    Text(
                        "Type a name, username, or phone number above to find any member.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)) {
                        items(displayUsers) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectUser(user) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Gold500.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(user.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Gold500)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(user.name.ifBlank { user.username }, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("${user.role} • ${user.phone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Default.ChatBubbleOutline, null, tint = Gold500, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
