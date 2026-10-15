package com.loanzo.app.ui.admin

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.loanzo.app.ui.components.LoanzoText as Text
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
import com.loanzo.app.data.entity.AgentApplicationEntity
import com.loanzo.app.data.entity.AgentVisitEntity
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.LocalAdminRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDispatchScreen(
    initialFilter: Int = 0,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val adminRepository = LocalAdminRepository.current

    val allVisits by adminRepository.allVisits.collectAsState(initial = emptyList())
    val allAgentApplications by adminRepository.allAgentApplications.collectAsState(initial = emptyList())

    val approvedAgents = remember(allAgentApplications) {
        allAgentApplications.filter { it.status == "APPROVED" }
    }

    val unassignedVisits = remember(allVisits) {
        allVisits.filter { it.agentId == "UNASSIGNED" || it.agentId.isBlank() }
    }
    val activeVisits = remember(allVisits) {
        allVisits.filter { it.status == "SCHEDULED" || it.visitStageStatus == "DISPATCHED" }
    }
    val crossVerificationVisits = remember(allVisits) {
        allVisits.filter { it.isCrossVerification }
    }
    val completedVisits = remember(allVisits) {
        allVisits.filter { it.status == "COMPLETED" }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableIntStateOf(initialFilter) }
    var dispatchingVisit by remember { mutableStateOf<AgentVisitEntity?>(null) }
    var isSyncingCloud by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        adminRepository.startRealtimeAdminCloudSync(scope)
    }

    val filteredVisits = remember(allVisits, searchQuery, selectedFilter) {
        allVisits.filter { v ->
            val matchesQuery = searchQuery.isBlank() ||
                    v.borrowerName.contains(searchQuery, ignoreCase = true) ||
                    v.borrowerPhone.contains(searchQuery, ignoreCase = true) ||
                    v.targetAddress.contains(searchQuery, ignoreCase = true) ||
                    v.loanId.contains(searchQuery, ignoreCase = true) ||
                    (v.assignedAgentName?.contains(searchQuery, ignoreCase = true) == true)

            val matchesFilter = when (selectedFilter) {
                1 -> v.agentId == "UNASSIGNED" || v.agentId.isBlank()
                2 -> v.status == "SCHEDULED" || v.visitStageStatus == "DISPATCHED"
                3 -> v.isCrossVerification
                4 -> v.status == "COMPLETED"
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Field Dispatch Operations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextNavyDark
                        )
                        Text(
                            text = "Doorstep Verification & Dual-Officer Consensus",
                            fontSize = 11.sp,
                            color = TextSlateMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextNavyDark)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (!isSyncingCloud) {
                                isSyncingCloud = true
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        adminRepository.refreshAgentApplicationsFromCloud()
                                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Dispatch data refreshed", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Sync failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    } finally {
                                        isSyncingCloud = false
                                    }
                                }
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(BrandIceBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync",
                                tint = BrandRoyalBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = TextNavyDark,
                    navigationIconContentColor = TextNavyDark
                )
            )
        },
        containerColor = CanvasPorcelain
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Stats Ribbon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("TOTAL VISITS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSlateMuted)
                        Text("${allVisits.size}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TextNavyDark)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, if (unassignedVisits.isNotEmpty()) Color(0xFFF97316).copy(alpha = 0.5f) else Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("UNASSIGNED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (unassignedVisits.isNotEmpty()) Color(0xFFC2410C) else TextSlateMuted)
                        Text("${unassignedVisits.size}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = if (unassignedVisits.isNotEmpty()) Color(0xFFC2410C) else TextNavyDark)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("DISPATCHED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrandRoyalBlue)
                        Text("${activeVisits.size}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = BrandRoyalBlue)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("CONSENSUS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Emerald500)
                        Text("${crossVerificationVisits.size}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Emerald500)
                    }
                }
            }

            // Search Bar
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextSlateMuted, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (searchQuery.isBlank()) {
                                Text("Search by borrower, officer, address, loan ID...", fontSize = 13.sp, color = TextSlateMuted)
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSlateMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Filter Chips
            val filterOptions = listOf(
                "All (${allVisits.size})",
                "Unassigned (${unassignedVisits.size})",
                "Active Dispatched (${activeVisits.size})",
                "Dual-Officer Matrix (${crossVerificationVisits.size})",
                "Completed (${completedVisits.size})"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterOptions.forEachIndexed { index, title ->
                    val isSelected = selectedFilter == index
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) BrandRoyalBlue else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) BrandRoyalBlue else Color(0xFFCBD5E1)),
                        modifier = Modifier.clickable { selectedFilter = index }
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isSelected) Color.White else TextNavyDark,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Visits List
            if (filteredVisits.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.NearMe, contentDescription = null, tint = TextSlateMuted, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No inspection visits found", fontWeight = FontWeight.Bold, color = TextNavyDark)
                        Text("Adjust your search or active filter tab", fontSize = 12.sp, color = TextSlateMuted)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredVisits, key = { it.visitId }) { visit ->
                        AdminVisitCard(
                            visit = visit,
                            onDispatch = { dispatchingVisit = visit },
                            onCallBorrower = {
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${visit.borrowerPhone}"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            onOpenMap = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(visit.targetAddress)}"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            onSanctionConsensus = { loanId, title ->
                                scope.launch(Dispatchers.IO) {
                                    val targetLoan = adminRepository.getLoanById(loanId)
                                    val borrowerUserId = targetLoan?.borrowerId?.ifBlank { null } ?: "USR-BRW"
                                    val lenderUserId = targetLoan?.lenderId?.ifBlank { null } ?: "USR-LND"

                                    adminRepository.sendUserNotification(
                                        userId = borrowerUserId,
                                        title = "Dual-Agent Consensus Approved",
                                        message = "Your loan verification ($title) has been certified by dual field officers with 100% consensus. Escrow sanction approved.",
                                        actionRoute = "loans"
                                    )
                                    if (targetLoan != null && lenderUserId.isNotBlank()) {
                                        adminRepository.sendUserNotification(
                                            userId = lenderUserId,
                                            title = "Dual-Agent Consensus Approved",
                                            message = "Loan ($title) has passed 100% dual-agent consensus. Escrow sanction approved.",
                                            actionRoute = "loans"
                                        )
                                    }
                                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Dual-Officer consensus sanctioned", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Dispatch Agent Sheet
    dispatchingVisit?.let { visit ->
        DispatchAgentSheet(
            visit = visit,
            availableAgents = approvedAgents,
            onDispatch = { agentId, payout ->
                scope.launch(Dispatchers.IO) {
                    adminRepository.assignAgentToVisit(visit.visitId, agentId, payout)
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Officer dispatched successfully", Toast.LENGTH_SHORT).show()
                    }
                }
                dispatchingVisit = null
            },
            onDismiss = { dispatchingVisit = null }
        )
    }
}

@Composable
private fun AdminVisitCard(
    visit: AgentVisitEntity,
    onDispatch: () -> Unit,
    onCallBorrower: () -> Unit,
    onOpenMap: () -> Unit,
    onSanctionConsensus: (String, String) -> Unit
) {
    val isUnassigned = visit.agentId == "UNASSIGNED" || visit.agentId.isBlank()
    val isCompleted = visit.status == "COMPLETED"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (isUnassigned) Color(0xFFF97316).copy(alpha = 0.5f) else Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Visit Type Badge + Payout Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isUnassigned) Color(0xFFFFF7ED) else BrandIceBlue,
                    border = BorderStroke(0.5.dp, if (isUnassigned) Color(0xFFF97316).copy(alpha = 0.5f) else BrandRoyalBlue.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = visit.visitType.replace("_", " "),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUnassigned) Color(0xFFC2410C) else BrandRoyalBlue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = "Payout: INR ${visit.payoutAmount.toInt()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Emerald500
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Borrower & Location
            Text(
                text = visit.borrowerName.ifBlank { "Borrower Verification" },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextNavyDark
            )
            Text(
                text = visit.targetAddress,
                fontSize = 12.sp,
                color = TextSlateMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Assigned Field Officer Section
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isUnassigned) "ASSIGNMENT STATUS" else "ASSIGNED FIELD OFFICER",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSlateMuted
                        )
                        Text(
                            text = if (isUnassigned) "Unassigned (Awaiting Officer)" else (visit.assignedAgentName ?: "Assigned Officer"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnassigned) Color(0xFFC2410C) else TextNavyDark
                        )
                    }

                    if (visit.handshakePin.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White,
                            border = BorderStroke(0.5.dp, Color(0xFFCBD5E1))
                        ) {
                            Text(
                                text = "PIN: ${visit.handshakePin}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextNavyDark,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Dual-Officer Consensus Matrix (if cross-verification)
            if (visit.isCrossVerification) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DUAL-OFFICER CONSENSUS MATRIX",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Emerald500
                            )
                            Text(
                                text = "100% Agreement on Collateral & Physical Identity",
                                fontSize = 11.sp,
                                color = TextNavyDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Button(
                            onClick = { onSanctionConsensus(visit.loanId, visit.title) },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Sanction", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCallBorrower,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(0.8f),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextNavyDark)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call", fontSize = 11.sp, color = TextNavyDark)
                }

                OutlinedButton(
                    onClick = onOpenMap,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(0.8f),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextNavyDark)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Map", fontSize = 11.sp, color = TextNavyDark)
                }

                Button(
                    onClick = onDispatch,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isUnassigned) Color(0xFFF97316) else BrandRoyalBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.4f),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.NearMe, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isUnassigned) "Dispatch Officer" else "Re-assign Officer",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
