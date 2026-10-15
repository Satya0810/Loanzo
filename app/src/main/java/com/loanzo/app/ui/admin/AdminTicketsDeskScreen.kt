package com.loanzo.app.ui.admin

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loanzo.app.data.entity.SupportTicketEntity
import com.loanzo.app.ui.components.LoanzoText as Text
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.LocalSupportTicketRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTicketsDeskScreen(
    onBack: () -> Unit = {}
) {
    val ticketRepository = LocalSupportTicketRepository.current
    val tickets by ticketRepository.allTickets.collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }

    var resolvingTicket by remember { mutableStateOf<SupportTicketEntity?>(null) }
    var escalatingTicket by remember { mutableStateOf<SupportTicketEntity?>(null) }
    var schedulingTicket by remember { mutableStateOf<SupportTicketEntity?>(null) }
    var inspectingTicket by remember { mutableStateOf<SupportTicketEntity?>(null) }

    val statusFilters = listOf("ALL", "OPEN", "UNDER_REVIEW", "CALLBACK_SCHEDULED", "IN_PROGRESS", "ESCALATED", "RESOLVED")

    val filteredTickets = remember(tickets, searchQuery, selectedStatusFilter) {
        tickets.filter { tkt ->
            val matchesStatus = if (selectedStatusFilter == "ALL") true else tkt.status.equals(selectedStatusFilter, ignoreCase = true)
            val q = searchQuery.trim().lowercase()
            val matchesSearch = if (q.isBlank()) true else {
                tkt.userName.lowercase().contains(q) ||
                tkt.userPhone.contains(q) ||
                tkt.userEmail.lowercase().contains(q) ||
                tkt.subject.lowercase().contains(q) ||
                tkt.description.lowercase().contains(q) ||
                tkt.ticketId.lowercase().contains(q) ||
                tkt.category.lowercase().contains(q) ||
                (tkt.relatedLoanId?.lowercase()?.contains(q) == true)
            }
            matchesStatus && matchesSearch
        }
    }

    val totalCount = tickets.size
    val openCount = remember(tickets) { tickets.count { it.status.equals("OPEN", ignoreCase = true) } }
    val underReviewCount = remember(tickets) { tickets.count { it.status.equals("UNDER_REVIEW", ignoreCase = true) } }
    val callbackCount = remember(tickets) { tickets.count { it.status.equals("CALLBACK_SCHEDULED", ignoreCase = true) } }
    val escalatedCount = remember(tickets) { tickets.count { it.status.equals("ESCALATED", ignoreCase = true) } }
    val resolvedCount = remember(tickets) { tickets.count { it.status.equals("RESOLVED", ignoreCase = true) } }

    Scaffold(
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 3.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF1F5F9))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Support & Help Tickets",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Customer & agent support inquiries and platform assistance requests",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Metrics Ribbon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TicketMetricChip("Total", totalCount.toString(), Color(0xFF475569), Modifier.weight(1f))
                        TicketMetricChip("Open", openCount.toString(), Color(0xFFEF4444), Modifier.weight(1f))
                        TicketMetricChip("Review", underReviewCount.toString(), Color(0xFF3B82F6), Modifier.weight(1f))
                        TicketMetricChip("Callback", callbackCount.toString(), Color(0xFF8B5CF6), Modifier.weight(1f))
                        TicketMetricChip("Escalated", escalatedCount.toString(), Color(0xFFF97316), Modifier.weight(1f))
                        TicketMetricChip("Resolved", resolvedCount.toString(), Color(0xFF10B981), Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Search by user, phone, ticket ID, subject...", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Status Filters
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(statusFilters) { status ->
                            val isSelected = selectedStatusFilter == status
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedStatusFilter = status },
                                label = { Text(status.replace("_", " "), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandRoyalBlue,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFFF1F5F9),
                                    labelColor = Color(0xFF475569)
                                ),
                                border = BorderStroke(1.dp, if (isSelected) BrandRoyalBlue else Color(0xFFE2E8F0))
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        if (filteredTickets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE2E8F0),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No Support Tickets Found",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "All support queues are clear or no tickets match your search filters.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filteredTickets, key = { it.ticketId }) { ticket ->
                    TicketDeskCard(
                        ticket = ticket,
                        onCallUser = { phone ->
                            if (phone.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "No phone number available", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onMarkUnderReview = {
                            scope.launch(Dispatchers.IO) {
                                ticketRepository.markUnderReview(ticket.ticketId)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Ticket marked UNDER REVIEW", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onMarkInProgress = {
                            scope.launch(Dispatchers.IO) {
                                ticketRepository.markInProgress(ticket.ticketId)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Ticket marked IN PROGRESS", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onScheduleCallback = { schedulingTicket = ticket },
                        onResolve = { resolvingTicket = ticket },
                        onEscalate = { escalatingTicket = ticket },
                        onInspect = { inspectingTicket = ticket }
                    )
                }
            }
        }
    }

    // Schedule Callback Dialog
    schedulingTicket?.let { ticket ->
        var notes by remember { mutableStateOf("") }
        var hoursAhead by remember { mutableIntStateOf(2) }
        AlertDialog(
            onDismissRequest = { schedulingTicket = null },
            title = {
                Text("Schedule Callback #${ticket.ticketId}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column {
                    Text(
                        text = "Schedule dedicated customer assistance call for ${ticket.userName}:",
                        fontSize = 12.sp,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Select Timeframe:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 2, 4, 24).forEach { hrs ->
                            val isSelected = hoursAhead == hrs
                            FilterChip(
                                selected = isSelected,
                                onClick = { hoursAhead = hrs },
                                label = { Text(if (hrs == 24) "Tomorrow" else "In $hrs hr", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Callback agenda / notes...", fontSize = 12.sp) },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val callbackAt = System.currentTimeMillis() + (hoursAhead * 3600 * 1000L)
                        scope.launch(Dispatchers.IO) {
                            ticketRepository.scheduleCallback(ticket.ticketId, callbackAt, notes.ifBlank { null })
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Callback scheduled", Toast.LENGTH_SHORT).show()
                                schedulingTicket = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue)
                ) {
                    Text("Confirm Schedule", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { schedulingTicket = null }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }

    // Resolve Ticket Dialog
    resolvingTicket?.let { ticket ->
        var notes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { resolvingTicket = null },
            title = {
                Text("Resolve Ticket #${ticket.ticketId}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column {
                    Text(
                        text = "Enter official resolution explanation sent to ${ticket.userName}:",
                        fontSize = 12.sp,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Issue investigated, account limit restored, app cache reset.", fontSize = 12.sp) },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val textNotes = notes.ifBlank { "Resolved by Master Admin" }
                        scope.launch(Dispatchers.IO) {
                            ticketRepository.resolveTicket(ticket.ticketId, textNotes)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Ticket RESOLVED", Toast.LENGTH_SHORT).show()
                                resolvingTicket = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                ) {
                    Text("Resolve Ticket", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { resolvingTicket = null }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }

    // Escalate Ticket Dialog
    escalatingTicket?.let { ticket ->
        var notes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { escalatingTicket = null },
            title = {
                Text("Escalate Ticket #${ticket.ticketId}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column {
                    Text(
                        text = "Escalate this ticket to senior oversight:",
                        fontSize = 12.sp,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Reason for escalation...", fontSize = 12.sp) },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            ticketRepository.escalateTicket(ticket.ticketId, notes.ifBlank { "Escalated for senior review" })
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Ticket ESCALATED", Toast.LENGTH_SHORT).show()
                                escalatingTicket = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316))
                ) {
                    Text("Escalate", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { escalatingTicket = null }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }

    // Inspect Ticket Details Dialog
    inspectingTicket?.let { ticket ->
        val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
        Dialog(onDismissRequest = { inspectingTicket = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ticket #${ticket.ticketId}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        IconButton(onClick = { inspectingTicket = null }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE2E8F0))

                    Text("Subject", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text(ticket.subject, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Description", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text(ticket.description, fontSize = 13.sp, color = Color(0xFF334155), lineHeight = 18.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("User Details", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            Text(ticket.userName, fontSize = 12.sp, color = Color(0xFF0F172A))
                            Text(ticket.userPhone, fontSize = 11.sp, color = Color(0xFF475569))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Category & Priority", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            Text(ticket.category.replace("_", " "), fontSize = 12.sp, color = Color(0xFF0F172A))
                            Text("Priority: ${ticket.priority}", fontSize = 11.sp, color = BrandRoyalBlue)
                        }
                    }

                    if (!ticket.relatedLoanId.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Related Loan: ${ticket.relatedLoanId}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = BrandRoyalBlue)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Created At: ${dateFormat.format(Date(ticket.createdAt))}", fontSize = 11.sp, color = Color(0xFF94A3B8))

                    if (!ticket.resolutionNotes.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Resolution Notes:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text(ticket.resolutionNotes, fontSize = 12.sp, color = Color(0xFF334155))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { inspectingTicket = null },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close Details")
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketDeskCard(
    ticket: SupportTicketEntity,
    onCallUser: (String) -> Unit,
    onMarkUnderReview: () -> Unit,
    onMarkInProgress: () -> Unit,
    onScheduleCallback: () -> Unit,
    onResolve: () -> Unit,
    onEscalate: () -> Unit,
    onInspect: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(ticket.createdAt) { dateFormat.format(Date(ticket.createdAt)) }

    val statusColor = when (ticket.status.uppercase()) {
        "OPEN" -> Color(0xFFEF4444)
        "UNDER_REVIEW" -> Color(0xFF3B82F6)
        "CALLBACK_SCHEDULED" -> Color(0xFF8B5CF6)
        "IN_PROGRESS" -> Color(0xFFF59E0B)
        "ESCALATED" -> Color(0xFFF97316)
        "RESOLVED" -> Color(0xFF10B981)
        else -> Color(0xFF64748B)
    }

    val priorityColor = when (ticket.priority.uppercase()) {
        "URGENT" -> Color(0xFFEF4444)
        "HIGH" -> Color(0xFFF97316)
        "MEDIUM" -> Color(0xFFEAB308)
        else -> Color(0xFF10B981)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onInspect() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: User Info + Status & Priority Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = ticket.userName.take(1).uppercase().ifBlank { "U" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = ticket.userName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BrandRoyalBlue.copy(alpha = 0.12f),
                                border = BorderStroke(0.5.dp, BrandRoyalBlue.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = ticket.ticketId,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandRoyalBlue,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = ticket.userPhone.ifBlank { ticket.userEmail.ifBlank { "User ID: ${ticket.userId.takeLast(6)}" } },
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Priority Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = priorityColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, priorityColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = ticket.priority,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = priorityColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    // Status Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = ticket.status.replace("_", " "),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Ribbon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category: ${ticket.category.replace("_", " ")}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )
                if (ticket.scheduledCallbackAt != null) {
                    val cbFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                    Text(
                        text = "Callback: ${cbFormat.format(Date(ticket.scheduledCallbackAt))}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B5CF6)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subject & Description
            Text(
                text = ticket.subject,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = ticket.description,
                fontSize = 12.sp,
                color = Color(0xFF475569),
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!ticket.relatedLoanId.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Related Loan: ${ticket.relatedLoanId}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandRoyalBlue
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer: Timestamp & Action Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Call User
                    if (ticket.userPhone.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.clickable { onCallUser(ticket.userPhone) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF334155), modifier = Modifier.size(12.dp))
                                Text("Call", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                            }
                        }
                    }

                    if (ticket.status.equals("OPEN", ignoreCase = true)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFDBEAFE),
                            border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                            modifier = Modifier.clickable { onMarkUnderReview() }
                        ) {
                            Text(
                                text = "Review",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D4ED8),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (ticket.status.equals("UNDER_REVIEW", ignoreCase = true) || ticket.status.equals("OPEN", ignoreCase = true)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEDE9FE),
                            border = BorderStroke(1.dp, Color(0xFF8B5CF6)),
                            modifier = Modifier.clickable { onScheduleCallback() }
                        ) {
                            Text(
                                text = "Callback",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6D28D9),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (!ticket.status.equals("RESOLVED", ignoreCase = true)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFD1FAE5),
                            border = BorderStroke(1.dp, Color(0xFF10B981)),
                            modifier = Modifier.clickable { onResolve() }
                        ) {
                            Text(
                                text = "Resolve",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (!ticket.status.equals("ESCALATED", ignoreCase = true) && !ticket.status.equals("RESOLVED", ignoreCase = true)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFEDD5),
                            border = BorderStroke(1.dp, Color(0xFFF97316)),
                            modifier = Modifier.clickable { onEscalate() }
                        ) {
                            Text(
                                text = "Escalate",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC2410C),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketMetricChip(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
        }
    }
}
