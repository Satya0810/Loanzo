package com.loanzo.app.ui.notification

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.data.entity.NotificationEntity
import com.loanzo.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    state: NotificationUiState,
    onFilterChange: (NotificationFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit = {},
    onDateFilterChange: (DateRangeFilter) -> Unit = {},
    onCategoryTagChange: (String?) -> Unit = {},
    onClearAllFilters: () -> Unit = {},
    onMarkAsRead: (String) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateToLoan: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    var isDateMenuExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Activity & Alerts",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        if (state.unreadCount > 0) {
                            Text(
                                text = "${state.unreadCount} unread • ${state.notifications.size} shown",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gold500,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = "${state.notifications.size} updates",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray400
                            )
                        }
                    }
                },
                actions = {
                    if (state.unreadCount > 0) {
                        TextButton(onClick = onMarkAllAsRead) {
                            Text("Read All", color = Gold500, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Gray400)
                    }
                    if (state.notifications.isNotEmpty()) {
                        IconButton(onClick = onClearAll) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All", tint = Gray400)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. EMBEDDED SEARCH BAR (Revolut & Linear style)
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = "Search alerts, person, loan ID...",
                        color = Gray400,
                        fontSize = 13.5.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (state.searchQuery.isNotBlank()) Gold500 else Gray400,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = Gray400,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDarkElevated,
                    unfocusedContainerColor = SurfaceDarkElevated,
                    focusedBorderColor = Gold500.copy(alpha = 0.6f),
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // 2. MULTI-DIMENSIONAL FILTER STRIP (Date, Tags, Status)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clear all filters if active
                if (state.activeFilterCount > 0) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Red400.copy(alpha = 0.15f),
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(Red400, Red400))),
                            modifier = Modifier.clickable { onClearAllFilters() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Clear (${state.activeFilterCount}) ✕",
                                    color = Red400,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Date Dropdown Filter
                item {
                    Box {
                        val dateLabel = when (state.selectedDateFilter) {
                            DateRangeFilter.ALL_TIME -> "📅 All Time ▾"
                            DateRangeFilter.TODAY -> "📅 Today ▾"
                            DateRangeFilter.THIS_WEEK -> "📅 This Week ▾"
                            DateRangeFilter.THIS_MONTH -> "📅 This Month ▾"
                        }
                        val isDateActive = state.selectedDateFilter != DateRangeFilter.ALL_TIME

                        FilterChip(
                            selected = isDateActive,
                            onClick = { isDateMenuExpanded = true },
                            label = { Text(dateLabel, fontSize = 12.sp, fontWeight = if (isDateActive) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Gold500.copy(alpha = 0.2f),
                                selectedLabelColor = Gold500
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )

                        DropdownMenu(
                            expanded = isDateMenuExpanded,
                            onDismissRequest = { isDateMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Time") },
                                onClick = {
                                    onDateFilterChange(DateRangeFilter.ALL_TIME)
                                    isDateMenuExpanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.DateRange, null, modifier = Modifier.size(18.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Today") },
                                onClick = {
                                    onDateFilterChange(DateRangeFilter.TODAY)
                                    isDateMenuExpanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.Today, null, modifier = Modifier.size(18.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("This Week") },
                                onClick = {
                                    onDateFilterChange(DateRangeFilter.THIS_WEEK)
                                    isDateMenuExpanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.ViewWeek, null, modifier = Modifier.size(18.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("This Month") },
                                onClick = {
                                    onDateFilterChange(DateRangeFilter.THIS_MONTH)
                                    isDateMenuExpanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }
                }

                // Action Required Tag
                item {
                    val isActionActive = state.selectedCategoryTag == "ACTIONS"
                    FilterChip(
                        selected = isActionActive,
                        onClick = { onCategoryTagChange("ACTIONS") },
                        label = { Text("⚡ Actions", fontSize = 12.sp, fontWeight = if (isActionActive) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Red400.copy(alpha = 0.2f),
                            selectedLabelColor = Red400
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                // Payments Tag
                item {
                    val isPaymentsActive = state.selectedCategoryTag == "PAYMENTS"
                    FilterChip(
                        selected = isPaymentsActive,
                        onClick = { onCategoryTagChange("PAYMENTS") },
                        label = { Text("💰 Payments", fontSize = 12.sp, fontWeight = if (isPaymentsActive) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Emerald400.copy(alpha = 0.2f),
                            selectedLabelColor = Emerald400
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                // Deadlines Tag
                item {
                    val isDeadlinesActive = state.selectedFilter == NotificationFilter.DEADLINES
                    FilterChip(
                        selected = isDeadlinesActive,
                        onClick = {
                            onFilterChange(if (isDeadlinesActive) NotificationFilter.ALL else NotificationFilter.DEADLINES)
                        },
                        label = { Text("⏰ Deadlines", fontSize = 12.sp, fontWeight = if (isDeadlinesActive) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Gold500.copy(alpha = 0.2f),
                            selectedLabelColor = Gold500
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                // Agreements Tag
                item {
                    val isAgreementsActive = state.selectedCategoryTag == "AGREEMENTS"
                    FilterChip(
                        selected = isAgreementsActive,
                        onClick = { onCategoryTagChange("AGREEMENTS") },
                        label = { Text("📜 Agreements", fontSize = 12.sp, fontWeight = if (isAgreementsActive) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Blue400.copy(alpha = 0.2f),
                            selectedLabelColor = Blue400
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                // Unread Only
                item {
                    val isUnreadActive = state.selectedFilter == NotificationFilter.UNREAD
                    FilterChip(
                        selected = isUnreadActive,
                        onClick = {
                            onFilterChange(if (isUnreadActive) NotificationFilter.ALL else NotificationFilter.UNREAD)
                        },
                        label = { Text("📬 Unread", fontSize = 12.sp, fontWeight = if (isUnreadActive) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Gold500.copy(alpha = 0.2f),
                            selectedLabelColor = Gold500
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. CONTENT AREA
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Gold500)
                }
            } else if (state.notifications.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Gold500.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (state.activeFilterCount > 0) Icons.Default.SearchOff else Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = Gold500,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (state.activeFilterCount > 0) "No matching notifications" else "You're all caught up!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (state.activeFilterCount > 0) "Try adjusting your search query, tags, or date filter." else "No pending deadlines or activity alerts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray400,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (state.activeFilterCount > 0) {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = onClearAllFilters,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold500)
                            ) {
                                Text("Reset Filters", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // Group chronologically into Today, Yesterday, This Week, Earlier
                val groupedNotifications = remember(state.notifications) {
                    groupNotificationsChronologically(state.notifications)
                }

                // Check for most urgent actionable notification for Hero Card
                val urgentHeroNotification = remember(state.notifications) {
                    state.notifications.firstOrNull { it.type == "OVERDUE" && !it.isRead }
                        ?: state.notifications.firstOrNull { it.type == "DEADLINE" && !it.isRead }
                }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Urgent Action Hero Card (if present and not specifically filtered out)
                    if (urgentHeroNotification != null && state.selectedCategoryTag == null && state.searchQuery.isBlank()) {
                        item {
                            UrgentActionHeroCard(
                                notification = urgentHeroNotification,
                                onAction = {
                                    onMarkAsRead(urgentHeroNotification.notificationId)
                                    urgentHeroNotification.relatedLoanId?.let { onNavigateToLoan(it) }
                                }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    // Chronological Sections
                    groupedNotifications.forEach { (sectionHeader, sectionItems) ->
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sectionHeader,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Gold500
                                )
                                Text(
                                    text = "${sectionItems.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Gray400
                                )
                            }
                        }

                        items(
                            items = sectionItems,
                            key = { it.notificationId }
                        ) { notification ->
                            NotificationCard(
                                notification = notification,
                                onRead = { onMarkAsRead(notification.notificationId) },
                                onDelete = { onDelete(notification.notificationId) },
                                onAction = {
                                    onMarkAsRead(notification.notificationId)
                                    notification.relatedLoanId?.let { onNavigateToLoan(it) }
                                },
                                onTagClick = { tag ->
                                    if (tag.startsWith("#LN-")) {
                                        onSearchQueryChange(tag.removePrefix("#"))
                                    } else {
                                        onCategoryTagChange(tag.removePrefix("#"))
                                    }
                                }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

/**
 * Prominent Hero Card for critical overdue / impending deadlines.
 */
@Composable
fun UrgentActionHeroCard(
    notification: NotificationEntity,
    onAction: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Red400.copy(alpha = 0.1f)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(Red400.copy(alpha = 0.6f), Orange400.copy(alpha = 0.4f)))
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Red400.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Red400)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ACTION REQUIRED",
                            color = Red400,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }

                Text(
                    text = getRelativeTime(notification.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Gray400
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = notification.title.replace(Regex("[⏰⚡🔔⚠️📜]"), "").trim(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodyMedium,
                color = Gray300,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Red400,
                    contentColor = Navy900
                ),
                modifier = Modifier.fillMaxWidth().height(42.dp)
            ) {
                Text(
                    text = if (notification.type == "OVERDUE") "Pay / Settle Overdue Loan ➔" else "Review Loan Details ➔",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: NotificationEntity,
    onRead: () -> Unit,
    onDelete: () -> Unit,
    onAction: () -> Unit,
    onTagClick: (String) -> Unit
) {
    val (icon, iconTint, bgTint) = getNotificationStyle(notification.type)
    val timeAgo = getRelativeTime(notification.timestamp)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) SurfaceDarkCard.copy(alpha = 0.55f) else SurfaceDarkElevated
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (!notification.isRead) 1.dp else 0.5.dp,
                color = if (!notification.isRead) iconTint.copy(alpha = 0.35f) else GlassBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onRead(); onAction() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Type Badge Icon (Pocket-Log style)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(bgTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Top Row: Interactive Tags & Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tag Group
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Clickable Category Pill
                        Surface(
                            shape = CircleShape,
                            color = iconTint.copy(alpha = 0.15f),
                            modifier = Modifier.clickable { onTagClick("#${notification.type}") }
                        ) {
                            Text(
                                text = "#${notification.type.lowercase().replace('_', ' ')}",
                                color = iconTint,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        // Clickable Loan ID tag if available
                        if (notification.relatedLoanId != null) {
                            val shortId = notification.relatedLoanId.take(8)
                            Surface(
                                shape = CircleShape,
                                color = Gold500.copy(alpha = 0.12f),
                                modifier = Modifier.clickable { onTagClick("#$shortId") }
                            ) {
                                Text(
                                    text = "#$shortId",
                                    color = Gold500,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Time and Unread dot
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = timeAgo,
                            style = MaterialTheme.typography.labelSmall,
                            color = Gray400
                        )
                        if (!notification.isRead) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(iconTint)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = notification.title.replace(Regex("[⏰⚡🔔⚠️📜]"), "").trim(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (notification.isRead) FontWeight.SemiBold else FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (notification.isRead) Gray400 else Gray300,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (notification.relatedLoanId != null) {
                        Surface(
                            shape = CircleShape,
                            color = iconTint.copy(alpha = 0.15f),
                            modifier = Modifier.clickable { onAction() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (notification.type) {
                                        "OVERDUE" -> "Pay Now"
                                        "DEADLINE" -> "View Schedule"
                                        "AGREEMENT" -> "Sign Agreement"
                                        else -> "View Details"
                                    },
                                    color = iconTint,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete",
                            tint = Gray500,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class NotificationStyle(
    val icon: ImageVector,
    val iconTint: Color,
    val bgTint: Color
)

private fun getNotificationStyle(type: String): NotificationStyle = when (type) {
    "DEADLINE" -> NotificationStyle(Icons.Default.Alarm, Gold500, Gold500.copy(alpha = 0.15f))
    "OVERDUE" -> NotificationStyle(Icons.Default.Warning, Red400, Red400.copy(alpha = 0.15f))
    "DISBURSEMENT", "REPAYMENT" -> NotificationStyle(Icons.Default.CheckCircle, Emerald400, Emerald400.copy(alpha = 0.15f))
    "AGREEMENT" -> NotificationStyle(Icons.Default.Description, Blue400, Blue400.copy(alpha = 0.15f))
    "SYSTEM" -> NotificationStyle(Icons.Default.Info, Gray400, Gray400.copy(alpha = 0.15f))
    else -> NotificationStyle(Icons.Default.Notifications, Gold500, Gold500.copy(alpha = 0.15f))
}

private fun getRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days < 7 -> "${days}d ago"
        days < 30 -> "${days / 7}w ago"
        else -> "${days / 30}mo ago"
    }
}

/**
 * Partitions notifications into chronological groups: Today, Yesterday, This Week, Earlier
 */
private fun groupNotificationsChronologically(
    notifications: List<NotificationEntity>
): Map<String, List<NotificationEntity>> {
    val now = System.currentTimeMillis()
    val oneDayMs = 24 * 60 * 60 * 1000L
    val twoDaysMs = 2 * oneDayMs
    val sevenDaysMs = 7 * oneDayMs

    val groups = linkedMapOf<String, MutableList<NotificationEntity>>()

    notifications.forEach { notif ->
        val diff = now - notif.timestamp
        val header = when {
            diff <= oneDayMs -> "🌟 Today"
            diff <= twoDaysMs -> "📅 Yesterday"
            diff <= sevenDaysMs -> "🗓️ This Week"
            else -> "📜 Earlier"
        }
        groups.getOrPut(header) { mutableListOf() }.add(notif)
    }

    return groups
}
