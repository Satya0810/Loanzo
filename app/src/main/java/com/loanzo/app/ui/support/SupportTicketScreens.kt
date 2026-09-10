package com.loanzo.app.ui.support

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.data.entity.LoanEntity
import com.loanzo.app.data.entity.SupportTicketEntity
import com.loanzo.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// ────────────────────────────────────────────────────────────────────────────────
// Category & Priority Data Models
// ────────────────────────────────────────────────────────────────────────────────

data class TicketCategory(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val bgColor: Color,
    val defaultPriority: String
)

val ticketCategories = listOf(
    TicketCategory("LOAN_ISSUE", "Loan Issue", Icons.Default.Receipt, BrandRoyalBlue, BrandIceBlue, "HIGH"),
    TicketCategory("PAYMENT_DISPUTE", "Payment Dispute", Icons.Default.CurrencyRupee, Red400, RedLight, "HIGH"),
    TicketCategory("KYC_HELP", "KYC Help", Icons.Default.Badge, CategoryBusiness, Color(0xFFFFF7ED), "MEDIUM"),
    TicketCategory("APP_BUG", "App Bug", Icons.Default.BugReport, CategoryHousing, Color(0xFFF5F3FF), "MEDIUM"),
    TicketCategory("ACCOUNT_ISSUE", "Account Issue", Icons.Default.PersonOff, Orange400, OrangeLight, "HIGH"),
    TicketCategory("COLLATERAL", "Collateral", Icons.Default.Diamond, Emerald400, EmeraldLight, "HIGH"),
    TicketCategory("AGENT_COMPLAINT", "Agent Complaint", Icons.Default.SupportAgent, CategoryPersonal, Color(0xFFFDF2F8), "MEDIUM"),
    TicketCategory("GENERAL_QUERY", "General Query", Icons.Default.HelpOutline, Gray500, Gray100, "LOW")
)

val priorityOptions = listOf("LOW", "MEDIUM", "HIGH", "URGENT")

fun priorityColor(priority: String): Color = when (priority) {
    "URGENT" -> Red500
    "HIGH" -> Orange400
    "MEDIUM" -> GoldCoinRich
    "LOW" -> Emerald400
    else -> Gray500
}

fun priorityEmoji(priority: String): String = when (priority) {
    "URGENT" -> "🔴"
    "HIGH" -> "🟠"
    "MEDIUM" -> "🟡"
    "LOW" -> "🟢"
    else -> "⚪"
}

fun statusColor(status: String): Color = when (status) {
    "OPEN" -> Blue400
    "UNDER_REVIEW" -> GoldCoinRich
    "CALLBACK_SCHEDULED" -> BrandSapphire
    "IN_PROGRESS" -> Orange400
    "RESOLVED" -> Emerald400
    "CLOSED" -> Gray500
    "ESCALATED" -> Red400
    "REJECTED" -> Red500
    else -> Gray500
}

fun statusLabel(status: String): String = when (status) {
    "OPEN" -> "Open"
    "UNDER_REVIEW" -> "Under Review"
    "CALLBACK_SCHEDULED" -> "Callback Scheduled"
    "IN_PROGRESS" -> "In Progress"
    "RESOLVED" -> "Resolved"
    "CLOSED" -> "Closed"
    "ESCALATED" -> "Escalated"
    "REJECTED" -> "Rejected"
    else -> status
}

fun statusIcon(status: String): ImageVector = when (status) {
    "OPEN" -> Icons.Default.FiberNew
    "UNDER_REVIEW" -> Icons.Default.Visibility
    "CALLBACK_SCHEDULED" -> Icons.Default.PhoneCallback
    "IN_PROGRESS" -> Icons.Default.Engineering
    "RESOLVED" -> Icons.Default.CheckCircle
    "CLOSED" -> Icons.Default.DoneAll
    "ESCALATED" -> Icons.Default.PriorityHigh
    "REJECTED" -> Icons.Default.Cancel
    else -> Icons.Default.Info
}

// ────────────────────────────────────────────────────────────────────────────────
// My Tickets Screen (User)
// ────────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTicketsScreen(
    tickets: List<SupportTicketEntity>,
    pendingFeedbackCount: Int = 0,
    selectedFilter: String = "ALL",
    onFilterChange: (String) -> Unit = {},
    onNavigateToRaiseTicket: () -> Unit = {},
    onNavigateToTicketDetail: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val filters = listOf("ALL", "OPEN", "CALLBACK_SCHEDULED", "RESOLVED", "CLOSED", "PENDING_FEEDBACK")
    val filterLabels = mapOf(
        "ALL" to "All",
        "OPEN" to "Open",
        "CALLBACK_SCHEDULED" to "Scheduled",
        "RESOLVED" to "Resolved",
        "CLOSED" to "Closed",
        "PENDING_FEEDBACK" to "Feedback"
    )

    var searchQuery by remember { mutableStateOf("") }

    val filteredTickets = remember(tickets, selectedFilter, searchQuery) {
        val base = when (selectedFilter) {
            "ALL" -> tickets
            "PENDING_FEEDBACK" -> tickets.filter { it.status == "RESOLVED" && it.feedbackRating == null }
            else -> tickets.filter { it.status == selectedFilter }
        }
        if (searchQuery.isBlank()) base
        else base.filter {
            it.subject.contains(searchQuery, ignoreCase = true) ||
            it.ticketId.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Help & Support",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            "${tickets.size} ticket${if (tickets.size != 1) "s" else ""}" +
                                    if (pendingFeedbackCount > 0) " · $pendingFeedbackCount awaiting feedback" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToRaiseTicket,
                containerColor = BrandRoyalBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Raise Ticket")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Raise Ticket", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by ticket #, subject, issue...", color = Color(0xFF94A3B8), fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BrandRoyalBlue, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color(0xFFF8FAFC),
                    focusedBorderColor = BrandRoyalBlue,
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Filter chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFilterChange(filter) },
                        label = {
                            Text(
                                filterLabels[filter] ?: filter,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandRoyalBlue.copy(alpha = 0.12f),
                            selectedLabelColor = BrandRoyalBlue
                        )
                    )
                }
            }

            if (filteredTickets.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = BrandIceBlue,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Icon(
                                Icons.Default.SupportAgent,
                                contentDescription = null,
                                tint = BrandRoyalBlue,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            "No Tickets Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (selectedFilter == "ALL") "Need help? Raise a support ticket and our team will call you back."
                            else "No ${filterLabels[selectedFilter]?.lowercase() ?: ""} tickets found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTickets, key = { it.ticketId }) { ticket ->
                        TicketCard(
                            ticket = ticket,
                            onClick = { onNavigateToTicketDetail(ticket.ticketId) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun TicketCard(
    ticket: SupportTicketEntity,
    onClick: () -> Unit
) {
    val category = ticketCategories.find { it.id == ticket.category }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val hasPendingFeedback = ticket.status == "RESOLVED" && ticket.feedbackRating == null

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (hasPendingFeedback) GoldCoinRich.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category icon
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = category?.bgColor ?: Gray100,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = category?.icon ?: Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = category?.color ?: Gray500,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                // Status badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor(ticket.status).copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            statusIcon(ticket.status),
                            contentDescription = null,
                            tint = statusColor(ticket.status),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            statusLabel(ticket.status),
                            color = statusColor(ticket.status),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                ticket.subject,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                ticket.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority badge
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(priorityEmoji(ticket.priority), fontSize = 12.sp)
                    Text(
                        ticket.priority,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = priorityColor(ticket.priority)
                    )
                }

                // Ticket ID & date
                Text(
                    "${ticket.ticketId} · ${dateFormat.format(Date(ticket.createdAt))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }

            // Pending feedback banner
            if (hasPendingFeedback) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GoldCoinCream,
                    border = BorderStroke(1.dp, GoldCoinBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⭐", fontSize = 16.sp)
                        Text(
                            "We'd love your feedback! Tap to rate.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = GoldCoinAmber
                        )
                    }
                }
            }

            // Scheduled callback info
            if (ticket.status == "CALLBACK_SCHEDULED" && ticket.scheduledCallbackAt != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BrandIceBlue,
                    border = BorderStroke(1.dp, BrandIceBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.PhoneCallback, contentDescription = null, tint = BrandRoyalBlue, modifier = Modifier.size(16.dp))
                        Text(
                            "Callback: ${dateFormat.format(Date(ticket.scheduledCallbackAt))}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandRoyalBlue
                        )
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// Raise Ticket Screen
// ────────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaiseTicketScreen(
    userLoans: List<LoanEntity> = emptyList(),
    ticketSubmitted: Boolean = false,
    errorMessage: String? = null,
    onSubmit: (category: String, priority: String, subject: String, description: String, relatedLoanId: String?, preferredCallbackAt: Long?) -> Unit = { _, _, _, _, _, _ -> },
    onTicketSubmittedAck: () -> Unit = {},
    onClearError: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf<TicketCategory?>(null) }
    var selectedPriority by remember { mutableStateOf("MEDIUM") }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedLoanId by remember { mutableStateOf<String?>(null) }
    var showLoanPicker by remember { mutableStateOf(false) }

    // Auto-navigate back on submission
    LaunchedEffect(ticketSubmitted) {
        if (ticketSubmitted) {
            onTicketSubmittedAck()
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Raise a Support Ticket", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Error message
            if (errorMessage != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = RedLight,
                        border = BorderStroke(1.dp, Red400.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Error, null, tint = Red400, modifier = Modifier.size(20.dp))
                            Text(errorMessage, color = Red500, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = onClearError, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = Red400, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Step 1: Category Selection
            item {
                Text(
                    "What do you need help with?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Category grid (2 columns)
                val rows = ticketCategories.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { cat ->
                                val isSelected = selectedCategory?.id == cat.id
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) cat.color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) cat.color else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedCategory = cat
                                            selectedPriority = cat.defaultPriority
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = cat.bgColor,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                cat.icon, null,
                                                tint = cat.color,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            cat.label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) cat.color else MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            // Fill empty slot if odd count
                            if (row.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Step 2: Priority
            item {
                Text(
                    "Priority Level",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    priorityOptions.forEach { priority ->
                        val isSelected = selectedPriority == priority
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPriority = priority },
                            label = {
                                Text(
                                    "${priorityEmoji(priority)} $priority",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = priorityColor(priority).copy(alpha = 0.15f),
                                selectedLabelColor = priorityColor(priority)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Step 3: Subject
            item {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { if (it.length <= 120) subject = it },
                    label = { Text("Subject") },
                    placeholder = { Text("Brief summary of your issue") },
                    singleLine = true,
                    supportingText = { Text("${subject.length}/120") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // Step 4: Description
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("Describe your issue in detail. Our team will read this before calling you back.") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // Step 5: Link Loan (Optional)
            if (userLoans.isNotEmpty()) {
                item {
                    Text(
                        "Link to a Loan (Optional)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val selectedLoan = userLoans.find { it.loanId == selectedLoanId }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLoanPicker = !showLoanPicker }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Link, null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    if (selectedLoan != null) "Loan: ${selectedLoan.loanId.takeLast(8)}"
                                    else "Select a loan",
                                    color = if (selectedLoan != null) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (selectedLoanId != null) {
                                IconButton(
                                    onClick = { selectedLoanId = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, null, tint = Gray500, modifier = Modifier.size(16.dp))
                                }
                            } else {
                                Icon(
                                    if (showLoanPicker) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    null, tint = Gray500
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = showLoanPicker) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            userLoans.take(5).forEach { loan ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selectedLoanId == loan.loanId) BrandIceBlue else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, if (selectedLoanId == loan.loanId) BrandRoyalBlue.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedLoanId = loan.loanId
                                            showLoanPicker = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Receipt, null, tint = BrandRoyalBlue, modifier = Modifier.size(18.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                loan.loanId.takeLast(12),
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                "${loan.status} · ₹${String.format("%,.0f", loan.sanctionedAmount)}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Submit Button
            item {
                Spacer(modifier = Modifier.height(8.dp))

                val isValid = selectedCategory != null && subject.isNotBlank() && description.isNotBlank()

                Button(
                    onClick = {
                        if (isValid && selectedCategory != null) {
                            onSubmit(
                                selectedCategory!!.id,
                                selectedPriority,
                                subject.trim(),
                                description.trim(),
                                selectedLoanId,
                                null
                            )
                        }
                    },
                    enabled = isValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandRoyalBlue,
                        disabledContainerColor = Gray300
                    )
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Ticket", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Our team will review your ticket and call you back at your registered phone number.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// Ticket Detail Screen (User)
// ────────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailScreen(
    ticket: SupportTicketEntity?,
    onSubmitFeedback: (ticketId: String, rating: Int, comment: String?) -> Unit = { _, _, _ -> },
    feedbackSubmitted: Boolean = false,
    onFeedbackSubmittedAck: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    if (ticket == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BrandRoyalBlue)
        }
        return
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val category = ticketCategories.find { it.id == ticket.category }
    var feedbackRating by remember { mutableStateOf(0) }
    var feedbackComment by remember { mutableStateOf("") }
    val context = LocalContext.current

    val hasPendingFeedback = ticket.status == "RESOLVED" && ticket.feedbackRating == null
    val hasSubmittedFeedback = ticket.feedbackRating != null

    LaunchedEffect(feedbackSubmitted) {
        if (feedbackSubmitted) {
            Toast.makeText(context, "Thank you for your feedback! ⭐", Toast.LENGTH_SHORT).show()
            onFeedbackSubmittedAck()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Ticket Details", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            ticket.ticketId,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Timeline Card
            item {
                StatusTimelineCard(ticket)
            }

            // Ticket Info Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = category?.bgColor ?: Gray100,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    category?.icon ?: Icons.Default.HelpOutline,
                                    null,
                                    tint = category?.color ?: Gray500,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    category?.label ?: ticket.category,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(priorityEmoji(ticket.priority), fontSize = 12.sp)
                                    Text(
                                        "${ticket.priority} Priority",
                                        fontSize = 12.sp,
                                        color = priorityColor(ticket.priority),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Subject", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(ticket.subject, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Description", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(ticket.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Created", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(dateFormat.format(Date(ticket.createdAt)), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)

                        if (ticket.relatedLoanId != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Linked Loan", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BrandIceBlue,
                                border = BorderStroke(1.dp, BrandIceBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Receipt, null, tint = BrandRoyalBlue, modifier = Modifier.size(16.dp))
                                    Text(ticket.relatedLoanId, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = BrandRoyalBlue)
                                }
                            }
                        }
                    }
                }
            }

            // Scheduled Callback Card
            if (ticket.status == "CALLBACK_SCHEDULED" && ticket.scheduledCallbackAt != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = BrandIceBlue),
                        border = BorderStroke(1.dp, BrandIceBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Surface(
                                    shape = CircleShape,
                                    color = BrandRoyalBlue.copy(alpha = 0.15f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Default.PhoneCallback, null, tint = BrandRoyalBlue, modifier = Modifier.padding(8.dp))
                                }
                                Column {
                                    Text("Callback Scheduled", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BrandRoyalBlue)
                                    Text(
                                        dateFormat.format(Date(ticket.scheduledCallbackAt)),
                                        fontSize = 13.sp,
                                        color = BrandCobalt
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Our team will call you at your registered phone number. Please keep your phone reachable.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandRoyalBlue.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Resolution Notes Card
            if ((ticket.status == "RESOLVED" || ticket.status == "CLOSED") && !ticket.resolutionNotes.isNullOrBlank()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = EmeraldLight),
                        border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Surface(
                                    shape = CircleShape,
                                    color = Emerald400.copy(alpha = 0.15f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Emerald500, modifier = Modifier.padding(8.dp))
                                }
                                Column {
                                    Text("Resolution", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Emerald600)
                                    if (ticket.resolvedAt != null) {
                                        Text(
                                            "Resolved on ${dateFormat.format(Date(ticket.resolvedAt))}",
                                            fontSize = 12.sp,
                                            color = Emerald500
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(ticket.resolutionNotes, fontSize = 14.sp, color = Emerald600)
                        }
                    }
                }
            }

            // Feedback Card (pending)
            if (hasPendingFeedback) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = GoldCoinCream),
                        border = BorderStroke(1.5.dp, GoldCoinBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("⭐", fontSize = 28.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "How was your experience?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = GoldCoinAmber
                            )
                            Text(
                                "Your feedback helps us improve!",
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldCoinAmber.copy(alpha = 0.7f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Star Rating
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                (1..5).forEach { star ->
                                    IconButton(
                                        onClick = { feedbackRating = star },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            if (star <= feedbackRating) Icons.Default.Star else Icons.Outlined.StarOutline,
                                            contentDescription = "$star star",
                                            tint = if (star <= feedbackRating) GoldCoinRich else Gray400,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                            }

                            if (feedbackRating > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    when (feedbackRating) {
                                        1 -> "Poor 😞"
                                        2 -> "Fair 😐"
                                        3 -> "Good 🙂"
                                        4 -> "Very Good 😊"
                                        5 -> "Excellent! 🤩"
                                        else -> ""
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = GoldCoinAmber,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = feedbackComment,
                                onValueChange = { feedbackComment = it },
                                label = { Text("Comment (optional)") },
                                placeholder = { Text("Tell us more about your experience...") },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldCoinRich,
                                    unfocusedBorderColor = GoldCoinBorder
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (feedbackRating > 0) {
                                        onSubmitFeedback(
                                            ticket.ticketId,
                                            feedbackRating,
                                            feedbackComment.ifBlank { null }
                                        )
                                    }
                                },
                                enabled = feedbackRating > 0,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldCoinRich,
                                    contentColor = Color.White,
                                    disabledContainerColor = Gray300
                                )
                            ) {
                                Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Submit Feedback", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Submitted Feedback Card
            if (hasSubmittedFeedback) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.RateReview, null, tint = GoldCoinRich)
                                Text("Your Feedback", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                (1..5).forEach { star ->
                                    Icon(
                                        if (star <= (ticket.feedbackRating ?: 0)) Icons.Default.Star else Icons.Outlined.StarOutline,
                                        contentDescription = null,
                                        tint = if (star <= (ticket.feedbackRating ?: 0)) GoldCoinRich else Gray300,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            if (!ticket.feedbackComment.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "\"${ticket.feedbackComment}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (ticket.feedbackSubmittedAt != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Submitted ${dateFormat.format(Date(ticket.feedbackSubmittedAt))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun StatusTimelineCard(ticket: SupportTicketEntity) {
    val stages = listOf("OPEN", "UNDER_REVIEW", "CALLBACK_SCHEDULED", "IN_PROGRESS", "RESOLVED", "CLOSED")
    val currentIndex = stages.indexOf(ticket.status).let { if (it >= 0) it else 0 }
    // Handle special statuses
    val isEscalated = ticket.status == "ESCALATED"
    val isRejected = ticket.status == "REJECTED"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Timeline, null, tint = BrandRoyalBlue, modifier = Modifier.size(20.dp))
                Text("Ticket Status", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isEscalated || isRejected) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isEscalated) Orange400.copy(alpha = 0.12f) else RedLight,
                    border = BorderStroke(1.dp, if (isEscalated) Orange400.copy(alpha = 0.3f) else Red400.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            if (isEscalated) Icons.Default.PriorityHigh else Icons.Default.Cancel,
                            null,
                            tint = if (isEscalated) Orange400 else Red400
                        )
                        Column {
                            Text(
                                if (isEscalated) "Escalated" else "Rejected",
                                fontWeight = FontWeight.Bold,
                                color = if (isEscalated) Orange500 else Red500
                            )
                            if (!ticket.adminNotes.isNullOrBlank()) {
                                Text(
                                    ticket.adminNotes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isEscalated) Orange400 else Red400
                                )
                            }
                        }
                    }
                }
            } else {
                stages.forEachIndexed { index, stage ->
                    val isPast = index < currentIndex
                    val isCurrent = index == currentIndex
                    val isFuture = index > currentIndex
                    val stageColor = when {
                        isCurrent -> statusColor(stage)
                        isPast -> Emerald400
                        else -> Gray300
                    }

                    Row(verticalAlignment = Alignment.Top) {
                        // Timeline dot & line
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(32.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = stageColor,
                                modifier = Modifier.size(if (isCurrent) 14.dp else 10.dp)
                            ) {}
                            if (index < stages.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(28.dp)
                                        .background(if (isPast) Emerald400.copy(alpha = 0.5f) else Gray200)
                                )
                            }
                        }

                        // Label
                        Text(
                            statusLabel(stage),
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            fontSize = if (isCurrent) 14.sp else 12.sp,
                            color = when {
                                isCurrent -> stageColor
                                isPast -> Emerald500
                                else -> Gray400
                            },
                            modifier = Modifier.padding(start = 8.dp, bottom = if (index < stages.size - 1) 12.dp else 0.dp)
                        )
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// Admin Ticket Management Console (Tab 9 Content)
// ────────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTicketConsole(
    tickets: List<SupportTicketEntity>,
    onScheduleCallback: (ticketId: String, callbackAt: Long, notes: String?) -> Unit = { _, _, _ -> },
    onMarkUnderReview: (ticketId: String) -> Unit = {},
    onMarkInProgress: (ticketId: String) -> Unit = {},
    onResolve: (ticketId: String, resolutionNotes: String) -> Unit = { _, _ -> },
    onEscalate: (ticketId: String, notes: String?) -> Unit = { _, _ -> },
    onReject: (ticketId: String, notes: String?) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var expandedTicketId by remember { mutableStateOf<String?>(null) }
    var showResolveDialog by remember { mutableStateOf<SupportTicketEntity?>(null) }
    var showEscalateDialog by remember { mutableStateOf<SupportTicketEntity?>(null) }
    var showRejectDialog by remember { mutableStateOf<SupportTicketEntity?>(null) }
    var showScheduleDialog by remember { mutableStateOf<SupportTicketEntity?>(null) }

    val adminFilters = listOf("ALL", "OPEN", "UNDER_REVIEW", "CALLBACK_SCHEDULED", "IN_PROGRESS", "ESCALATED", "RESOLVED")

    val filteredTickets = remember(tickets, selectedFilter, searchQuery) {
        tickets.filter { t ->
            val matchesFilter = if (selectedFilter == "ALL") true else t.status == selectedFilter
            val matchesQuery = if (searchQuery.isBlank()) true else {
                t.userName.contains(searchQuery, ignoreCase = true) ||
                t.userPhone.contains(searchQuery, ignoreCase = true) ||
                t.userId.contains(searchQuery, ignoreCase = true) ||
                t.subject.contains(searchQuery, ignoreCase = true) ||
                t.ticketId.contains(searchQuery, ignoreCase = true) ||
                t.description.contains(searchQuery, ignoreCase = true) ||
                t.category.contains(searchQuery, ignoreCase = true)
            }
            matchesFilter && matchesQuery
        }
    }

    val openCount = tickets.count { it.status == "OPEN" }
    val escalatedCount = tickets.count { it.status == "ESCALATED" }
    val todayCallbacks = tickets.count {
        it.status == "CALLBACK_SCHEDULED" && it.scheduledCallbackAt != null &&
                isSameDay(it.scheduledCallbackAt, System.currentTimeMillis())
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // KPI Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminKpiChip("🆕 Open", "$openCount", Blue400, Modifier.weight(1f))
            AdminKpiChip("📞 Today", "$todayCallbacks", BrandSapphire, Modifier.weight(1f))
            AdminKpiChip("⚠️ Escalated", "$escalatedCount", Orange400, Modifier.weight(1f))
            AdminKpiChip("📊 Total", "${tickets.size}", Gray500, Modifier.weight(1f))
        }

        // Search by User / Ticket bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by user, phone, ticket #, subject...", color = Color(0xFF94A3B8), fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFF8FAFC),
                focusedBorderColor = Color(0xFF2563EB),
                unfocusedBorderColor = Color(0xFFCBD5E1),
                focusedTextColor = Color(0xFF0F172A),
                unfocusedTextColor = Color(0xFF0F172A)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // Filter Chips
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(adminFilters) { filter ->
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = {
                        Text(
                            if (filter == "ALL") "All" else statusLabel(filter),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandRoyalBlue.copy(alpha = 0.12f),
                        selectedLabelColor = BrandRoyalBlue
                    )
                )
            }
        }

        if (filteredTickets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inbox, null, tint = Gray400, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No tickets", color = Gray500, fontWeight = FontWeight.Medium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTickets, key = { it.ticketId }) { ticket ->
                    val isExpanded = expandedTicketId == ticket.ticketId

                    AdminTicketCard(
                        ticket = ticket,
                        isExpanded = isExpanded,
                        onToggleExpand = {
                            expandedTicketId = if (isExpanded) null else ticket.ticketId
                        },
                        onCallUser = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${ticket.userPhone}")))
                            } catch (_: Exception) {
                                Toast.makeText(context, "Cannot open dialer", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onWhatsApp = {
                            try {
                                val phone = ticket.userPhone.replace("+", "").replace(" ", "")
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$phone")))
                            } catch (_: Exception) {
                                Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onMarkUnderReview = { onMarkUnderReview(ticket.ticketId) },
                        onMarkInProgress = { onMarkInProgress(ticket.ticketId) },
                        onScheduleCallback = { showScheduleDialog = ticket },
                        onResolve = { showResolveDialog = ticket },
                        onEscalate = { showEscalateDialog = ticket },
                        onReject = { showRejectDialog = ticket }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    // Resolve Dialog
    showResolveDialog?.let { ticket ->
        AdminActionDialog(
            title = "Resolve Ticket",
            subtitle = ticket.ticketId,
            icon = Icons.Default.CheckCircle,
            iconColor = Emerald400,
            fieldLabel = "Resolution Notes",
            fieldPlaceholder = "Describe how the issue was resolved...",
            confirmText = "Resolve",
            confirmColor = Emerald500,
            onConfirm = { notes ->
                onResolve(ticket.ticketId, notes)
                showResolveDialog = null
            },
            onDismiss = { showResolveDialog = null }
        )
    }

    // Escalate Dialog
    showEscalateDialog?.let { ticket ->
        AdminActionDialog(
            title = "Escalate Ticket",
            subtitle = ticket.ticketId,
            icon = Icons.Default.PriorityHigh,
            iconColor = Orange400,
            fieldLabel = "Escalation Notes",
            fieldPlaceholder = "Reason for escalation...",
            confirmText = "Escalate",
            confirmColor = Orange500,
            onConfirm = { notes ->
                onEscalate(ticket.ticketId, notes.ifBlank { null })
                showEscalateDialog = null
            },
            onDismiss = { showEscalateDialog = null }
        )
    }

    // Reject Dialog
    showRejectDialog?.let { ticket ->
        AdminActionDialog(
            title = "Reject Ticket",
            subtitle = ticket.ticketId,
            icon = Icons.Default.Cancel,
            iconColor = Red400,
            fieldLabel = "Rejection Reason",
            fieldPlaceholder = "Reason for rejection...",
            confirmText = "Reject",
            confirmColor = Red500,
            onConfirm = { notes ->
                onReject(ticket.ticketId, notes.ifBlank { null })
                showRejectDialog = null
            },
            onDismiss = { showRejectDialog = null }
        )
    }

    // Schedule Callback Dialog
    showScheduleDialog?.let { ticket ->
        ScheduleCallbackDialog(
            ticket = ticket,
            onSchedule = { callbackAt, notes ->
                onScheduleCallback(ticket.ticketId, callbackAt, notes)
                showScheduleDialog = null
            },
            onDismiss = { showScheduleDialog = null }
        )
    }
}

@Composable
private fun AdminKpiChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
            Text(label, fontSize = 10.sp, color = color.copy(alpha = 0.8f), maxLines = 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminTicketCard(
    ticket: SupportTicketEntity,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onCallUser: () -> Unit,
    onWhatsApp: () -> Unit,
    onMarkUnderReview: () -> Unit,
    onMarkInProgress: () -> Unit,
    onScheduleCallback: () -> Unit,
    onResolve: () -> Unit,
    onEscalate: () -> Unit,
    onReject: () -> Unit
) {
    val category = ticketCategories.find { it.id == ticket.category }
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Card(
        onClick = onToggleExpand,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (ticket.priority == "URGENT") Red400.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = category?.bgColor ?: Gray100,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        category?.icon ?: Icons.Default.HelpOutline, null,
                        tint = category?.color ?: Gray500,
                        modifier = Modifier.padding(7.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(ticket.subject, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "${ticket.userName} · ${ticket.ticketId}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusColor(ticket.status).copy(alpha = 0.12f)
                    ) {
                        Text(
                            statusLabel(ticket.status),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            color = statusColor(ticket.status),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "${priorityEmoji(ticket.priority)} ${ticket.priority}",
                        fontSize = 10.sp,
                        color = priorityColor(ticket.priority),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Expanded Content
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Description
                    Text(ticket.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(12.dp))

                    // User Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Person, null, tint = Gray500, modifier = Modifier.size(16.dp))
                        Text(ticket.userName, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("·", color = Gray400)
                        Icon(Icons.Default.Phone, null, tint = Gray500, modifier = Modifier.size(14.dp))
                        Text(ticket.userPhone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("·", color = Gray400)
                        Text(dateFormat.format(Date(ticket.createdAt)), fontSize = 11.sp, color = Gray500)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Contact Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCallUser,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Emerald400)
                        ) {
                            Icon(Icons.Default.Phone, null, tint = Emerald500, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("📞 Call", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emerald500)
                        }
                        OutlinedButton(
                            onClick = onWhatsApp,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Emerald400)
                        ) {
                            Text("💬 WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emerald500)
                        }
                    }

                    // Feedback display
                    if (ticket.feedbackRating != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GoldCoinCream,
                            border = BorderStroke(1.dp, GoldCoinBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("User Feedback:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldCoinAmber)
                                (1..5).forEach { s ->
                                    Icon(
                                        if (s <= ticket.feedbackRating) Icons.Default.Star else Icons.Outlined.StarOutline,
                                        null,
                                        tint = if (s <= ticket.feedbackRating) GoldCoinRich else Gray300,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                if (!ticket.feedbackComment.isNullOrBlank()) {
                                    Text("· ${ticket.feedbackComment}", fontSize = 10.sp, color = GoldCoinAmber, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Buttons (status-dependent)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        when (ticket.status) {
                            "OPEN" -> {
                                AdminActionButton("Review", BrandRoyalBlue, Modifier.weight(1f)) { onMarkUnderReview() }
                                AdminActionButton("Schedule", BrandSapphire, Modifier.weight(1f)) { onScheduleCallback() }
                                AdminActionButton("Reject", Red400, Modifier.weight(1f)) { onReject() }
                            }
                            "UNDER_REVIEW" -> {
                                AdminActionButton("Schedule Call", BrandSapphire, Modifier.weight(1f)) { onScheduleCallback() }
                                AdminActionButton("Escalate", Orange400, Modifier.weight(1f)) { onEscalate() }
                            }
                            "CALLBACK_SCHEDULED" -> {
                                AdminActionButton("In Progress", Orange400, Modifier.weight(1f)) { onMarkInProgress() }
                                AdminActionButton("📞 Call Now", Emerald500, Modifier.weight(1f)) { onCallUser() }
                            }
                            "IN_PROGRESS" -> {
                                AdminActionButton("Resolve ✅", Emerald500, Modifier.weight(1f)) { onResolve() }
                                AdminActionButton("Escalate", Orange400, Modifier.weight(1f)) { onEscalate() }
                            }
                            "ESCALATED" -> {
                                AdminActionButton("Resolve ✅", Emerald500, Modifier.weight(1f)) { onResolve() }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminActionButton(label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminActionDialog(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    fieldLabel: String,
    fieldPlaceholder: String,
    confirmText: String,
    confirmColor: Color,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(icon, null, tint = iconColor, modifier = Modifier.size(32.dp)) },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(fieldLabel) },
                placeholder = { Text(fieldPlaceholder) },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(notes) },
                colors = ButtonDefaults.buttonColors(containerColor = confirmColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(confirmText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleCallbackDialog(
    ticket: SupportTicketEntity,
    onSchedule: (callbackAt: Long, notes: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var notes by remember { mutableStateOf("") }
    // Default to 1 hour from now
    var selectedHoursFromNow by remember { mutableIntStateOf(1) }

    val timeOptions = listOf(
        1 to "In 1 hour",
        2 to "In 2 hours",
        4 to "In 4 hours",
        8 to "In 8 hours",
        24 to "Tomorrow",
        48 to "In 2 days"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.PhoneCallback, null, tint = BrandRoyalBlue, modifier = Modifier.size(32.dp)) },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Schedule Callback", fontWeight = FontWeight.Bold)
                Text("${ticket.userName} · ${ticket.userPhone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("When to call back?", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                // Time option chips (2x3 grid)
                val rows = timeOptions.chunked(3)
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { (hours, label) ->
                            val isSelected = selectedHoursFromNow == hours
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedHoursFromNow = hours },
                                label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandRoyalBlue.copy(alpha = 0.12f),
                                    selectedLabelColor = BrandRoyalBlue
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    placeholder = { Text("Any internal notes...") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val callbackAt = System.currentTimeMillis() + (selectedHoursFromNow * 3600000L)
                    onSchedule(callbackAt, notes.ifBlank { null })
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Schedule", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
