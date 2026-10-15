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
import com.loanzo.app.data.entity.ComplaintEntity
import com.loanzo.app.ui.components.LoanzoText as Text
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.LocalAdminRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsDeskScreen(
    onBack: () -> Unit = {}
) {
    val adminRepository = LocalAdminRepository.current
    val complaints by adminRepository.allComplaints.collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    var selectedRoleFilter by remember { mutableStateOf("ALL") }

    var resolvingComplaint by remember { mutableStateOf<ComplaintEntity?>(null) }
    var dismissingComplaint by remember { mutableStateOf<ComplaintEntity?>(null) }
    var inspectingComplaint by remember { mutableStateOf<ComplaintEntity?>(null) }

    val statusFilters = listOf("ALL", "OPEN", "INVESTIGATING", "RESOLVED", "DISMISSED")
    val roleFilters = listOf("ALL", "BORROWER", "LENDER", "AGENT")

    val filteredComplaints = remember(complaints, searchQuery, selectedStatusFilter, selectedRoleFilter) {
        complaints.filter { comp ->
            val matchesStatus = if (selectedStatusFilter == "ALL") true else comp.status.equals(selectedStatusFilter, ignoreCase = true)
            val matchesRole = if (selectedRoleFilter == "ALL") true else comp.complainantRole.equals(selectedRoleFilter, ignoreCase = true)
            val q = searchQuery.trim().lowercase()
            val matchesSearch = if (q.isBlank()) true else {
                comp.complainantName.lowercase().contains(q) ||
                comp.complainantPhone.contains(q) ||
                comp.subject.lowercase().contains(q) ||
                comp.description.lowercase().contains(q) ||
                (comp.targetPartyName?.lowercase()?.contains(q) == true) ||
                (comp.loanId?.lowercase()?.contains(q) == true) ||
                comp.complaintId.lowercase().contains(q) ||
                comp.category.lowercase().contains(q)
            }
            matchesStatus && matchesRole && matchesSearch
        }
    }

    val totalCount = complaints.size
    val openCount = remember(complaints) { complaints.count { it.status.equals("OPEN", ignoreCase = true) } }
    val investigatingCount = remember(complaints) { complaints.count { it.status.equals("INVESTIGATING", ignoreCase = true) } }
    val resolvedCount = remember(complaints) { complaints.count { it.status.equals("RESOLVED", ignoreCase = true) } }

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
                                text = "Dispute & Reports Desk",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "User & agent dispute reports, fraud allegations, and grievances",
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReportMetricChip(
                            label = "Total Reports",
                            value = totalCount.toString(),
                            accentColor = Color(0xFF475569),
                            modifier = Modifier.weight(1f)
                        )
                        ReportMetricChip(
                            label = "Open",
                            value = openCount.toString(),
                            accentColor = Color(0xFFEF4444),
                            modifier = Modifier.weight(1f)
                        )
                        ReportMetricChip(
                            label = "Investigating",
                            value = investigatingCount.toString(),
                            accentColor = Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f)
                        )
                        ReportMetricChip(
                            label = "Resolved",
                            value = resolvedCount.toString(),
                            accentColor = Color(0xFF10B981),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Search by user, phone, loan ID, subject...", fontSize = 12.sp, color = Color(0xFF94A3B8))
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

                    Spacer(modifier = Modifier.height(4.dp))

                    // Complainant Role Filters
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(roleFilters) { role ->
                            val isSelected = selectedRoleFilter == role
                            val displayLabel = if (role == "ALL") "All Parties" else "$role Only"
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedRoleFilter = role },
                                label = { Text(displayLabel, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0F172A),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color.White,
                                    labelColor = Color(0xFF64748B)
                                ),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF0F172A) else Color(0xFFCBD5E1))
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        if (filteredComplaints.isEmpty()) {
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
                            Icon(Icons.Default.DoneAll, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No Reports Matching Filters",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "All user and agent dispute queues are clear or no submissions match your query.",
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
                items(filteredComplaints, key = { it.complaintId }) { complaint ->
                    ComplaintDeskCard(
                        complaint = complaint,
                        onCallComplainant = { phone ->
                            if (phone.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "No phone number available", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onMarkInvestigating = {
                            scope.launch(Dispatchers.IO) {
                                adminRepository.updateComplaintStatus(complaint.complaintId, "INVESTIGATING")
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Status set to INVESTIGATING", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onResolve = { resolvingComplaint = complaint },
                        onDismiss = { dismissingComplaint = complaint },
                        onInspect = { inspectingComplaint = complaint }
                    )
                }
            }
        }
    }

    // Resolve Report Dialog
    resolvingComplaint?.let { complaint ->
        var notes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { resolvingComplaint = null },
            title = {
                Text("Resolve Report #${complaint.complaintId.takeLast(6)}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column {
                    Text(
                        text = "Provide official Master Admin resolution remarks for ${complaint.complainantName}:",
                        fontSize = 12.sp,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Terms arbitrated, refund initiated, or penalty waived.", fontSize = 12.sp) },
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
                            adminRepository.resolveComplaint(complaint.complaintId, textNotes)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Report marked as RESOLVED", Toast.LENGTH_SHORT).show()
                                resolvingComplaint = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                ) {
                    Text("Confirm Resolution", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { resolvingComplaint = null }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }

    // Dismiss Report Dialog
    dismissingComplaint?.let { complaint ->
        var notes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { dismissingComplaint = null },
            title = {
                Text("Dismiss Report #${complaint.complaintId.takeLast(6)}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column {
                    Text(
                        text = "Provide rationale for dismissing this complaint:",
                        fontSize = 12.sp,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Unsubstantiated claims or duplicate ticket.", fontSize = 12.sp) },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val textNotes = notes.ifBlank { "Dismissed by Master Admin" }
                        scope.launch(Dispatchers.IO) {
                            adminRepository.dismissComplaint(complaint.complaintId, textNotes)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Report marked as DISMISSED", Toast.LENGTH_SHORT).show()
                                dismissingComplaint = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Dismiss Report", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { dismissingComplaint = null }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }

    // Inspect Detailed Dossier Dialog
    inspectingComplaint?.let { complaint ->
        val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
        Dialog(onDismissRequest = { inspectingComplaint = null }) {
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
                            text = "Report Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        IconButton(onClick = { inspectingComplaint = null }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE2E8F0))

                    Text("Subject", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text(complaint.subject, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Description", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text(complaint.description, fontSize = 13.sp, color = Color(0xFF334155), lineHeight = 18.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Complainant", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            Text("${complaint.complainantName} (${complaint.complainantRole})", fontSize = 12.sp, color = Color(0xFF0F172A))
                            Text(complaint.complainantPhone, fontSize = 11.sp, color = Color(0xFF475569))
                        }
                        if (!complaint.targetPartyName.isNullOrBlank()) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Target Party", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                Text("${complaint.targetPartyName} (${complaint.targetPartyRole ?: "N/A"})", fontSize = 12.sp, color = Color(0xFF0F172A))
                                if (!complaint.loanId.isNullOrBlank()) {
                                    Text("Loan: ${complaint.loanId}", fontSize = 11.sp, color = BrandRoyalBlue)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Filed At: ${dateFormat.format(Date(complaint.createdAt))}", fontSize = 11.sp, color = Color(0xFF94A3B8))

                    if (!complaint.resolutionNotes.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Resolution Remarks:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text(complaint.resolutionNotes, fontSize = 12.sp, color = Color(0xFF334155))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { inspectingComplaint = null },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close Dossier")
                    }
                }
            }
        }
    }
}

@Composable
private fun ComplaintDeskCard(
    complaint: ComplaintEntity,
    onCallComplainant: (String) -> Unit,
    onMarkInvestigating: () -> Unit,
    onResolve: () -> Unit,
    onDismiss: () -> Unit,
    onInspect: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(complaint.createdAt) { dateFormat.format(Date(complaint.createdAt)) }

    val statusColor = when (complaint.status.uppercase()) {
        "OPEN" -> Color(0xFFEF4444)
        "INVESTIGATING" -> Color(0xFFF59E0B)
        "RESOLVED" -> Color(0xFF10B981)
        "DISMISSED" -> Color(0xFF64748B)
        else -> Color(0xFF3B82F6)
    }

    val priorityColor = when (complaint.priority.uppercase()) {
        "CRITICAL_LEGAL", "URGENT" -> Color(0xFFEF4444)
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
            // Header Row: Complainant + Status & Priority Badges
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
                                text = complaint.complainantName.take(1).uppercase().ifBlank { "U" },
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
                                text = complaint.complainantName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (complaint.complainantRole == "AGENT") Emerald500.copy(alpha = 0.12f) else BrandRoyalBlue.copy(alpha = 0.12f),
                                border = BorderStroke(0.5.dp, if (complaint.complainantRole == "AGENT") Emerald500.copy(alpha = 0.5f) else BrandRoyalBlue.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = complaint.complainantRole,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (complaint.complainantRole == "AGENT") Emerald500 else BrandRoyalBlue,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = complaint.complainantPhone.ifBlank { "ID: ${complaint.complainantId.takeLast(6)}" },
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
                            text = complaint.priority.replace("_", " "),
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
                            text = complaint.status.replace("_", " "),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category & Target Party Ribbon
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
                    text = "Category: ${complaint.category.replace("_", " ")}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )
                if (!complaint.targetPartyName.isNullOrBlank()) {
                    Text(
                        text = "Against: ${complaint.targetPartyName} (${complaint.targetPartyRole ?: "Party"})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subject & Description
            Text(
                text = complaint.subject,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = complaint.description,
                fontSize = 12.sp,
                color = Color(0xFF475569),
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!complaint.loanId.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Related Loan: ${complaint.loanId}",
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Call Complainant
                    if (complaint.complainantPhone.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.clickable { onCallComplainant(complaint.complainantPhone) }
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

                    if (complaint.status.equals("OPEN", ignoreCase = true)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                            modifier = Modifier.clickable { onMarkInvestigating() }
                        ) {
                            Text(
                                text = "Investigate",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (!complaint.status.equals("RESOLVED", ignoreCase = true)) {
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

                    if (!complaint.status.equals("DISMISSED", ignoreCase = true) && !complaint.status.equals("RESOLVED", ignoreCase = true)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEE2E2),
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            modifier = Modifier.clickable { onDismiss() }
                        ) {
                            Text(
                                text = "Dismiss",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB91C1C),
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
private fun ReportMetricChip(
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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
        }
    }
}
