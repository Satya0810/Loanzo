package com.loanzo.app.ui.notification

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.loanzo.app.ui.components.LoanzoText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
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
    onNavigateToActionRoute: (String) -> Unit = {},
    onBack: () -> Unit = {},
    showBackButton: Boolean = true
) {
    var isDateMenuExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // Intercept hardware and gesture back navigation
    BackHandler {
        if (state.searchQuery.isNotEmpty()) {
            onSearchQueryChange("")
        } else {
            onBack()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                title = {
                    Text(
                        text = "Alerts & Notifications",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Gray400
                        )
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

            // ─── 2. DEDICATED SEARCH BAR CARD (BELOW HEADER, NOT INLINE) ───
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Gray400,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = state.searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        ),
                        cursorBrush = SolidColor(Gold500),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (state.searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search alerts, loan ID, #tag...",
                                        color = Gray400,
                                        fontSize = 13.5.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = Gray400,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            // ULTRA-COMPACT SMART TRIAGE RIBBON (~36dp height)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset Button (visible if any filter is active)
                if (state.activeFilterCount > 0) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Red400.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Red400.copy(alpha = 0.35f)),
                            modifier = Modifier.clickable {
                                onClearAllFilters()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Reset ✕",
                                    color = Red400,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 1. All Filter Pill
                item {
                    val isAllSelected = state.selectedFilter == NotificationFilter.ALL && state.selectedCategoryTag == null
                    FilterChip(
                        selected = isAllSelected,
                        onClick = {
                            onFilterChange(NotificationFilter.ALL)
                            onCategoryTagChange(null)
                        },
                        label = {
                            Text(
                                text = if (state.rawNotifications.isNotEmpty() && isAllSelected) "All (${state.rawNotifications.size})" else "All",
                                fontSize = 11.5.sp,
                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Gold500.copy(alpha = 0.18f),
                            selectedLabelColor = Gold500,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isAllSelected,
                            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            selectedBorderColor = Gold500.copy(alpha = 0.5f),
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(32.dp)
                    )
                }

                // 2. Unread Filter Pill
                item {
                    val isUnreadSelected = state.selectedFilter == NotificationFilter.UNREAD
                    FilterChip(
                        selected = isUnreadSelected,
                        onClick = {
                            onFilterChange(if (isUnreadSelected) NotificationFilter.ALL else NotificationFilter.UNREAD)
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (state.unreadCount > 0) Gold500 else Gray400)
                            )
                        },
                        label = {
                            Text(
                                text = if (state.unreadCount > 0) "Unread (${state.unreadCount})" else "Unread",
                                fontSize = 11.5.sp,
                                fontWeight = if (isUnreadSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Gold500.copy(alpha = 0.18f),
                            selectedLabelColor = Gold500,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isUnreadSelected,
                            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            selectedBorderColor = Gold500.copy(alpha = 0.5f),
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(32.dp)
                    )
                }

                // 3. Actions Filter Pill
                item {
                    val isActionSelected = state.selectedCategoryTag == "ACTIONS"
                    FilterChip(
                        selected = isActionSelected,
                        onClick = {
                            onCategoryTagChange(if (isActionSelected) null else "ACTIONS")
                        },
                        label = {
                            Text(
                                text = "⚡ Actions",
                                fontSize = 11.5.sp,
                                fontWeight = if (isActionSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Red400.copy(alpha = 0.18f),
                            selectedLabelColor = Red400,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isActionSelected,
                            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            selectedBorderColor = Red400.copy(alpha = 0.5f),
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(32.dp)
                    )
                }

                // 4. Financial / Payments Filter Pill
                item {
                    val isPaymentsSelected = state.selectedCategoryTag == "PAYMENTS"
                    FilterChip(
                        selected = isPaymentsSelected,
                        onClick = {
                            onCategoryTagChange(if (isPaymentsSelected) null else "PAYMENTS")
                        },
                        label = {
                            Text(
                                text = "💰 Financial",
                                fontSize = 11.5.sp,
                                fontWeight = if (isPaymentsSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Emerald400.copy(alpha = 0.18f),
                            selectedLabelColor = Emerald400,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isPaymentsSelected,
                            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            selectedBorderColor = Emerald400.copy(alpha = 0.5f),
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(32.dp)
                    )
                }

                // 5. Legal & Agreements Filter Pill
                item {
                    val isAgreementsSelected = state.selectedCategoryTag == "AGREEMENTS"
                    FilterChip(
                        selected = isAgreementsSelected,
                        onClick = {
                            onCategoryTagChange(if (isAgreementsSelected) null else "AGREEMENTS")
                        },
                        label = {
                            Text(
                                text = "📜 Legal",
                                fontSize = 11.5.sp,
                                fontWeight = if (isAgreementsSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Blue400.copy(alpha = 0.18f),
                            selectedLabelColor = Blue400,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isAgreementsSelected,
                            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            selectedBorderColor = Blue400.copy(alpha = 0.5f),
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(32.dp)
                    )
                }

                // 6. Date Range Dropdown Filter Pill
                item {
                    Box {
                        val dateLabel = when (state.selectedDateFilter) {
                            DateRangeFilter.ALL_TIME -> "📅 Date ▾"
                            DateRangeFilter.TODAY -> "📅 Today ▾"
                            DateRangeFilter.THIS_WEEK -> "📅 Week ▾"
                            DateRangeFilter.THIS_MONTH -> "📅 Month ▾"
                        }
                        val isDateActive = state.selectedDateFilter != DateRangeFilter.ALL_TIME

                        FilterChip(
                            selected = isDateActive,
                            onClick = { isDateMenuExpanded = true },
                            label = {
                                Text(
                                    text = dateLabel,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isDateActive) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Gold500.copy(alpha = 0.18f),
                                selectedLabelColor = Gold500,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isDateActive,
                                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                selectedBorderColor = Gold500.copy(alpha = 0.5f),
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.dp
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(32.dp)
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
            }

            Spacer(modifier = Modifier.height(2.dp))

            // CONTENT AREA
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Gold500)
                }
            } else if (state.notifications.isEmpty()) {
                // Inbox Zero Empty State
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
                                imageVector = if (state.activeFilterCount > 0) Icons.Default.SearchOff else Icons.Default.NotificationsNone,
                                contentDescription = null,
                                tint = Gold500,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (state.activeFilterCount > 0) "No matching notifications" else "Inbox Zero • All Caught Up!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (state.activeFilterCount > 0)
                                "Try adjusting or clearing your search, tags, or date filter."
                            else
                                "No pending deadlines or activity alerts require your attention.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray400,
                            textAlign = TextAlign.Center
                        )
                        if (state.activeFilterCount > 0) {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = {
                                    onClearAllFilters()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold500),
                                border = BorderStroke(1.dp, Gold500.copy(alpha = 0.5f))
                            ) {
                                Text("Reset Filters", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            } else {
                // Partition chronologically into Today, Yesterday, This Week, Earlier
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
                    // Urgent Action Hero Card (pinned when unread urgent alert exists and not specifically filtered)
                    if (urgentHeroNotification != null && state.selectedCategoryTag == null && state.searchQuery.isBlank()) {
                        item {
                            UrgentActionHeroCard(
                                notification = urgentHeroNotification,
                                onAction = {
                                    onMarkAsRead(urgentHeroNotification.notificationId)
                                    if (!urgentHeroNotification.actionRoute.isNullOrBlank()) {
                                        onNavigateToActionRoute(urgentHeroNotification.actionRoute)
                                    } else {
                                        urgentHeroNotification.relatedLoanId?.let { onNavigateToLoan(it) }
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
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
                                    style = MaterialTheme.typography.labelMedium,
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
                                    if (!notification.actionRoute.isNullOrBlank()) {
                                        onNavigateToActionRoute(notification.actionRoute)
                                    } else {
                                        notification.relatedLoanId?.let { onNavigateToLoan(it) }
                                    }
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
                        Spacer(modifier = Modifier.height(28.dp))
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Red400.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, Red400.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Red400.copy(alpha = 0.18f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Red400)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ACTION REQUIRED",
                            color = Red400,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 9.5.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Text(
                    text = getRelativeTime(notification.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Gray400
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = notification.title.replace(Regex("[⏰⚡🔔⚠️📜]"), "").trim(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onAction,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Red400,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            ) {
                Text(
                    text = if (notification.type == "OVERDUE") "Pay / Settle Overdue Loan ➔" else "Review Loan Details ➔",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                )
            }
        }
    }
}

/**
 * Modern Linear/Revolut-styled notification card with unread accent indicator.
 */
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (!notification.isRead) iconTint.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onRead()
                onAction()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Unread accent indicator stripe
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(42.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(iconTint)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Category Icon glyph box
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Top Row: Interactive Category Tags & Relative Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = iconTint.copy(alpha = 0.12f),
                            modifier = Modifier.clickable { onTagClick("#${notification.type}") }
                        ) {
                            Text(
                                text = "#${notification.type.lowercase().replace('_', ' ')}",
                                color = iconTint,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (notification.relatedLoanId != null) {
                            val shortId = notification.relatedLoanId.take(8)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                modifier = Modifier.clickable { onTagClick("#$shortId") }
                            ) {
                                Text(
                                    text = "#$shortId",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = timeAgo,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (!notification.isRead) MaterialTheme.colorScheme.onSurfaceVariant else Gray500,
                            fontSize = 11.sp
                        )
                        if (!notification.isRead) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(iconTint)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = notification.title.replace(Regex("[⏰⚡🔔⚠️📜]"), "").trim(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (notification.isRead) FontWeight.SemiBold else FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.5.sp
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom CTA and Dismiss Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!notification.actionRoute.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = iconTint.copy(alpha = 0.14f),
                            modifier = Modifier.clickable { onAction() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (notification.type) {
                                        "AGENT_APPLICATION" -> "Review in Hub"
                                        "ADMIN_REQUEST" -> "Manage Access"
                                        "COMPLAINT" -> "Inspect Grievance"
                                        else -> "Open Action"
                                    },
                                    color = iconTint,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    } else if (notification.relatedLoanId != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = iconTint.copy(alpha = 0.14f),
                            modifier = Modifier.clickable { onAction() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
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
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Gray500,
                            modifier = Modifier.size(15.dp)
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
    "AGENT_APPLICATION", "AGENT_VERIFICATION" -> NotificationStyle(Icons.Default.Badge, Emerald500, Emerald500.copy(alpha = 0.15f))
    "ADMIN_REQUEST", "ADMIN_ANNOUNCEMENT" -> NotificationStyle(Icons.Default.AdminPanelSettings, Color(0xFF6366F1), Color(0xFF6366F1).copy(alpha = 0.15f))
    "COMPLAINT" -> NotificationStyle(Icons.Default.Gavel, Red400, Red400.copy(alpha = 0.15f))
    "KYC_STATUS" -> NotificationStyle(Icons.Default.VerifiedUser, Blue400, Blue400.copy(alpha = 0.15f))
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
