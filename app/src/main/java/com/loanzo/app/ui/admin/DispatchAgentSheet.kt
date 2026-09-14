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
import com.loanzo.app.ui.components.LoanzoText as Text
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
    onDispatchDual: ((agent1Id: String, agent2Id: String, payoutAmount: Double) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sortedAgents = remember(availableAgents, visit.targetAddress) {
        availableAgents.sortedByDescending { agent ->
            val pinMatch = agent.operatingPincode.isNotBlank() && visit.targetAddress.contains(agent.operatingPincode)
            val cityMatch = agent.operatingCity.isNotBlank() && visit.targetAddress.contains(agent.operatingCity, ignoreCase = true)
            if (pinMatch) 2 else if (cityMatch) 1 else 0
        }
    }
    var isDualProtocol by remember { mutableStateOf(onDispatchDual != null && sortedAgents.size >= 2) }
    var selectedAgentId by remember { mutableStateOf(sortedAgents.firstOrNull()?.userId ?: "") }
    var selectedAgent2Id by remember { mutableStateOf(sortedAgents.getOrNull(1)?.userId ?: "") }
    var payoutAmount by remember { mutableDoubleStateOf(visit.payoutAmount) }
    val payoutPresets = listOf(550.0, 750.0, 950.0, 1200.0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
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
                            color = GoldCoinCream,
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldCoinBorder)
                        ) {
                            Text(
                                text = "FIELD DISPATCH ENGINE",
                                color = GoldCoinAmber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(visit.visitId, color = TextSlateMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isDualProtocol) "Dispatch Blind Dual-Agent Pair" else "Map Inspection to Certified Agent",
                        color = TextNavyDark,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(BrandIceBlue)
                ) {
                    Icon(Icons.Default.Close, null, tint = TextSlateMedium, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Protocol Switcher Banner
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDualProtocol) GoldCoinCream else CanvasPorcelain),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDualProtocol) GoldCoinBorder else BrandIceBorder),
                modifier = Modifier.fillMaxWidth().clickable {
                    if (onDispatchDual != null && availableAgents.size >= 2) {
                        isDualProtocol = !isDualProtocol
                    }
                }
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isDualProtocol) Icons.Default.Security else Icons.Default.Person,
                        null,
                        tint = if (isDualProtocol) GoldCoinAmber else BrandCobalt,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isDualProtocol) "🛡️ Blind Dual-Agent Swapped Protocol (Active)" else "Standard Single-Officer Verification",
                            color = TextNavyDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isDualProtocol) "2 independent officers verify Borrower & Lender separately, then swap. Anonymized to eliminate collusion." else "Tap to switch to 2-Officer Anti-Collusion Cross-Verification.",
                            color = TextSlateMuted,
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = isDualProtocol,
                        onCheckedChange = {
                            if (onDispatchDual != null && availableAgents.size >= 2) {
                                isDualProtocol = it
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = GoldCoinRich, checkedTrackColor = GoldCoinBorder)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Visit Overview Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CanvasPorcelain),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandIceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = visit.title,
                        color = TextNavyDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = BrandCobalt, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = visit.targetAddress,
                            color = TextSlateMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Slot: ${visit.scheduledTimeSlot}", color = GoldCoinAmber, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("Counterparty: ${visit.borrowerName}", color = TextSlateMuted, fontSize = 11.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Handshake PIN: ${visit.handshakePin.ifBlank { "Auto-assigned on dispatch" }}", color = Emerald500, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Stage: ${visit.visitStageStatus}", color = GoldCoinAmber, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Payout Configurator
            Text(
                text = "Field Inspection Bounty / Payout (₹)",
                color = TextNavyDark,
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
                        color = if (isSelected) BrandRoyalBlue else CanvasPorcelain,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) BrandRoyalBlue else BrandIceBorder),
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
                                color = if (isSelected) Color.White else TextSlateMedium,
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
                color = TextNavyDark,
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
                        .background(CanvasPorcelain)
                        .border(1.dp, BrandIceBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No empaneled agents currently registered", color = TextSlateMuted, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedAgents) { agent ->
                        val isSelected = selectedAgentId == agent.userId
                        val isProximityMatch = (agent.operatingCity.isNotBlank() && visit.targetAddress.contains(agent.operatingCity, ignoreCase = true)) ||
                                (agent.operatingPincode.isNotBlank() && visit.targetAddress.contains(agent.operatingPincode))

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) BrandIceBlue else CanvasPorcelain),
                            border = androidx.compose.foundation.BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) BrandRoyalBlue else BrandIceBorder),
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
                                    color = if (isSelected) BrandRoyalBlue.copy(alpha = 0.12f) else Gray200,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.DirectionsBike,
                                            null,
                                            tint = if (isSelected) BrandRoyalBlue else TextSlateMedium,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = agent.applicantName.ifBlank { "Certified Field Officer" },
                                            color = TextNavyDark,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        if (isProximityMatch) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = EmeraldLight,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Emerald400.copy(alpha = 0.5f))
                                            ) {
                                                Text(
                                                    text = "🎯 Territory Match",
                                                    color = Emerald600,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        } else {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = BrandIceBlue,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, BrandIceBorder)
                                            ) {
                                                Text(
                                                    text = "🟢 Active",
                                                    color = BrandCobalt,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${agent.operatingCity} (${agent.operatingPincode}) • Radius: ${agent.serviceRadiusKm} km • ${agent.vehicleType} • PCC Verified",
                                        color = if (isSelected) BrandCobalt else TextSlateMuted,
                                        fontSize = 10.sp
                                    )
                                }

                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, null, tint = BrandRoyalBlue, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isDualProtocol && onDispatchDual != null && selectedAgentId.isNotBlank() && selectedAgent2Id.isNotBlank()) {
                        onDispatchDual(selectedAgentId, selectedAgent2Id, payoutAmount)
                    } else if (selectedAgentId.isNotBlank()) {
                        onDispatch(selectedAgentId, payoutAmount)
                    }
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedAgentId.isNotBlank() && (!isDualProtocol || selectedAgent2Id.isNotBlank()),
                colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue, contentColor = Color.White)
            ) {
                Icon(if (isDualProtocol) Icons.Default.Security else Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isDualProtocol) "Dispatch Dual Cross-Audit (2 x ₹${payoutAmount.toInt()})" else "Confirm Dispatch (₹${payoutAmount.toInt()})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
