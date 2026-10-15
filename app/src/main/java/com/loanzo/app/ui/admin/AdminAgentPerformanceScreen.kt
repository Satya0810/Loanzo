package com.loanzo.app.ui.admin

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.loanzo.app.data.entity.AgentVisitEntity
import com.loanzo.app.data.entity.UserEntity
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.LocalAdminRepository
import com.loanzo.app.util.toInrString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAgentPerformanceScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDispatch: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val adminRepository = LocalAdminRepository.current

    val allUsers by adminRepository.allUsers.collectAsState(initial = emptyList())
    val allVisits by adminRepository.allVisits.collectAsState(initial = emptyList())

    val agents = remember(allUsers) {
        allUsers.filter { it.role.equals("AGENT", ignoreCase = true) || it.agentStatus == "APPROVED" }
    }

    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    var agentForNotification by remember { mutableStateOf<UserEntity?>(null) }
    var alertTitle by remember { mutableStateOf("") }
    var alertMessage by remember { mutableStateOf("") }

    val onDutyCount = remember(agents) { agents.count { it.isOnDuty } }
    val completedVisitsCount = remember(allVisits) { allVisits.count { it.status == "COMPLETED" || it.visitStageStatus == "COMPLETED" } }
    val totalPayoutsSum = remember(allVisits) {
        allVisits.filter { it.status == "COMPLETED" || it.visitStageStatus == "COMPLETED" }.sumOf { it.payoutAmount }
    }

    val filteredAgents = remember(agents, searchQuery, selectedFilter) {
        agents.filter { agent ->
            val matchesQuery = searchQuery.isBlank() ||
                agent.name.contains(searchQuery, ignoreCase = true) ||
                agent.username.contains(searchQuery, ignoreCase = true) ||
                agent.phone.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "ALL" -> true
                "ON_DUTY" -> agent.isOnDuty
                "OFF_DUTY" -> !agent.isOnDuty
                "SUSPENDED" -> agent.agentStatus == "SUSPENDED" || agent.agentStatus == "REJECTED"
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
                            text = "Agent Operations & Performance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextNavyDark
                        )
                        Text(
                            text = "Field Force Monitoring and Verification Ledger",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextNavyDark
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToDispatch) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = "Dispatch Console",
                            tint = BrandCobalt
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Metrics KPI Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    KpiStat(label = "Empaneled Agents", value = agents.size.toString(), color = Color(0xFF0F172A))
                    KpiStat(label = "On-Duty Now", value = onDutyCount.toString(), color = Emerald500)
                    KpiStat(label = "Visits Completed", value = completedVisitsCount.toString(), color = BrandCobalt)
                    KpiStat(label = "Disbursed Payouts", value = totalPayoutsSum.toInrString(), color = Color(0xFF6366F1))
                }
            }

            // Search Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Search agents by name, username, phone...",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    "ALL" to "All Agents (${agents.size})",
                    "ON_DUTY" to "On Duty (${onDutyCount})",
                    "OFF_DUTY" to "Off Duty (${agents.size - onDutyCount})",
                    "SUSPENDED" to "Suspended (${agents.count { it.agentStatus == "SUSPENDED" }})"
                )

                filters.forEach { (key, label) ->
                    val isSelected = selectedFilter == key
                    Surface(
                        onClick = { selectedFilter = key },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) BrandNavyBlue else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) BrandNavyBlue else Color(0xFFE2E8F0))
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Agents List
            if (filteredAgents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No field agents match the criteria",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredAgents, key = { it.userId }) { agent ->
                        val agentVisits = remember(allVisits, agent.userId) {
                            allVisits.filter { it.agentId == agent.userId }
                        }
                        val agentCompletedCount = agentVisits.count { it.status == "COMPLETED" || it.visitStageStatus == "COMPLETED" }
                        val agentEarnings = agentVisits.filter { it.status == "COMPLETED" || it.visitStageStatus == "COMPLETED" }.sumOf { it.payoutAmount }

                        AgentPerformanceCard(
                            agent = agent,
                            completedVisits = agentCompletedCount,
                            totalVisits = agentVisits.size,
                            earnings = if (agentEarnings > 0) agentEarnings else agent.totalAgentEarnings,
                            onToggleDuty = { newDuty ->
                                scope.launch(Dispatchers.IO) {
                                    adminRepository.setAgentDutyStatus(agent.userId, newDuty)
                                    launch(Dispatchers.Main) {
                                        Toast.makeText(context, "Duty status updated for ${agent.name}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onToggleSuspension = {
                                val newStatus = if (agent.agentStatus == "SUSPENDED") "APPROVED" else "SUSPENDED"
                                scope.launch(Dispatchers.IO) {
                                    adminRepository.setAgentStatus(agent.userId, newStatus)
                                    launch(Dispatchers.Main) {
                                        Toast.makeText(context, "Agent is now $newStatus", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onSendAlert = {
                                agentForNotification = agent
                                alertTitle = ""
                                alertMessage = ""
                            }
                        )
                    }
                }
            }
        }
    }

    // Direct Notification Dialog
    if (agentForNotification != null) {
        val targetAgent = agentForNotification!!
        AlertDialog(
            onDismissRequest = { agentForNotification = null },
            title = {
                Text(
                    text = "Dispatch Directive to ${targetAgent.name.ifBlank { targetAgent.username }}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextNavyDark
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = alertTitle,
                        onValueChange = { alertTitle = it },
                        label = { Text("Directive Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = alertMessage,
                        onValueChange = { alertMessage = it },
                        label = { Text("Directive Instructions") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (alertTitle.isNotBlank() && alertMessage.isNotBlank()) {
                            scope.launch(Dispatchers.IO) {
                                adminRepository.sendUserNotification(
                                    userId = targetAgent.userId,
                                    title = alertTitle,
                                    message = alertMessage,
                                    actionRoute = "agent_main"
                                )
                                launch(Dispatchers.Main) {
                                    Toast.makeText(context, "Directive transmitted", Toast.LENGTH_SHORT).show()
                                    agentForNotification = null
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandNavyBlue)
                ) {
                    Text("Transmit", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { agentForNotification = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun KpiStat(label: String, value: String, color: Color) {
    Column {
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = color)
    }
}

@Composable
private fun AgentPerformanceCard(
    agent: UserEntity,
    completedVisits: Int,
    totalVisits: Int,
    earnings: Double,
    onToggleDuty: (Boolean) -> Unit,
    onToggleSuspension: () -> Unit,
    onSendAlert: () -> Unit
) {
    val isSuspended = agent.agentStatus == "SUSPENDED"

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, if (isSuspended) Red400.copy(alpha = 0.4f) else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (agent.isOnDuty) Emerald400.copy(alpha = 0.15f) else Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = null,
                            tint = if (agent.isOnDuty) Emerald500 else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = agent.name.ifBlank { agent.username },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${agent.phone} • ID: ${agent.userId.take(8)}",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Duty & Status Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSuspended) Red400.copy(alpha = 0.12f) else if (agent.isOnDuty) Emerald400.copy(alpha = 0.12f) else Color(0xFF64748B).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = if (isSuspended) "SUSPENDED" else if (agent.isOnDuty) "ON DUTY" else "OFF DUTY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSuspended) Red400 else if (agent.isOnDuty) Emerald500 else Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Visits", fontSize = 10.sp, color = Color(0xFF64748B))
                    Text("$totalVisits Assigned", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                }
                Column {
                    Text("Completed", fontSize = 10.sp, color = Color(0xFF64748B))
                    Text("$completedVisits Completed", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emerald500)
                }
                Column {
                    Text("Total Payout", fontSize = 10.sp, color = Color(0xFF64748B))
                    Text(earnings.toInrString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandCobalt)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onToggleDuty(!agent.isOnDuty) },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Text(
                        text = if (agent.isOnDuty) "Set Off-Duty" else "Set On-Duty",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155)
                    )
                }

                OutlinedButton(
                    onClick = onToggleSuspension,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isSuspended) Emerald500 else Red400),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Text(
                        text = if (isSuspended) "Reactivate" else "Suspend",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSuspended) Emerald500 else Red400
                    )
                }

                OutlinedButton(
                    onClick = onSendAlert,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BrandCobalt.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Text("Directive", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = BrandCobalt)
                }
            }
        }
    }
}
