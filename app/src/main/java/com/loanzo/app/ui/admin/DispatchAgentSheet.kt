package com.loanzo.app.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.data.entity.AgentApplicationEntity
import com.loanzo.app.data.entity.AgentVisitEntity
import com.loanzo.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatchAgentSheet(
    visit: AgentVisitEntity,
    availableAgents: List<AgentApplicationEntity>,
    onDispatch: (agentId: String, payoutAmount: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedAgentId by remember { mutableStateOf(availableAgents.firstOrNull()?.userId ?: "") }
    var payoutAmount by remember { mutableDoubleStateOf(visit.payoutAmount) }
    val payoutPresets = listOf(550.0, 750.0, 950.0, 1200.0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Gold500.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "FIELD DISPATCH ENGINE",
                                color = Gold500,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(visit.visitId, color = Gray400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Map Inspection to Certified Agent",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Visit Overview Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = visit.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = Emerald400, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = visit.targetAddress,
                            color = Gray300,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Slot: ${visit.scheduledTimeSlot}", color = Gold500, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("Counterparty: ${visit.borrowerName}", color = Gray400, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Payout Configurator
            Text(
                text = "Field Inspection Bounty / Payout (₹)",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                payoutPresets.forEach { amount ->
                    val isSelected = payoutAmount == amount
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Gold500 else Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Gold500 else Color(0xFF334155)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { payoutAmount = amount }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "₹${amount.toInt()}",
                                color = if (isSelected) Navy900 else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Available Agents Roster
            Text(
                text = "Select Active Certified Agent",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (availableAgents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No empaneled agents currently registered", color = Gray400, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableAgents) { agent ->
                        val isSelected = selectedAgentId == agent.userId
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF141D2E)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Emerald400 else Color(0xFF243247)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAgentId = agent.userId }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) Emerald400.copy(alpha = 0.2f) else Color(0xFF334155),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.DirectionsBike,
                                            null,
                                            tint = if (isSelected) Emerald400 else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = agent.permanentAddress.take(20) + " Officer",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Emerald500.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "🟢 Active",
                                                color = Emerald400,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${agent.operatingCity} • Radius: ${agent.serviceRadiusKm} km • ${agent.vehicleType}",
                                        color = Gray400,
                                        fontSize = 10.sp
                                    )
                                }

                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Emerald400, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (selectedAgentId.isNotBlank()) {
                        onDispatch(selectedAgentId, payoutAmount)
                    }
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedAgentId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy900)
            ) {
                Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Confirm Dispatch (₹${payoutAmount.toInt()})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
