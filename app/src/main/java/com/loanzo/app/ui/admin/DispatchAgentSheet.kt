package com.loanzo.app.ui.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.Send
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatchAgentSheet(
    visit: AgentVisitEntity,
    availableAgents: List<AgentApplicationEntity>,
    onDispatch: (agentId: String, payoutAmount: Double) -> Unit,
    onDispatchDual: ((agent1Id: String, agent2Id: String, payoutAmount: Double) -> Unit)? = null,
    agentWorkload: Map<String, Int> = emptyMap(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Score and rank available agents based on territorial proximity and workload
    val rankedAgents = remember(availableAgents, visit.targetAddress, agentWorkload) {
        availableAgents.sortedWith(
            compareByDescending<AgentApplicationEntity> { agent ->
                var score = 0
                val pinMatch = agent.operatingPincode.isNotBlank() && visit.targetAddress.contains(agent.operatingPincode)
                val cityMatch = agent.operatingCity.isNotBlank() && visit.targetAddress.contains(agent.operatingCity, ignoreCase = true)
                if (pinMatch) score += 100
                if (cityMatch) score += 50
                if (agent.priorDomain.contains("Bank", ignoreCase = true) || agent.priorDomain.contains("NBFC", ignoreCase = true)) score += 20
                val currentLoad = agentWorkload[agent.userId] ?: 0
                score -= (currentLoad * 15) // Balance load
                score
            }.thenBy { agentWorkload[it.userId] ?: 0 }
        )
    }

    var isDualProtocol by remember { mutableStateOf(onDispatchDual != null && rankedAgents.size >= 2) }
    var activeSlot by remember { mutableIntStateOf(1) } // 1 for Officer 1, 2 for Officer 2
    var selectedAgentId by remember { mutableStateOf(rankedAgents.firstOrNull()?.userId ?: "") }
    var selectedAgent2Id by remember { mutableStateOf(rankedAgents.getOrNull(1)?.userId ?: "") }
    var payoutAmount by remember { mutableDoubleStateOf(visit.payoutAmount) }
    var searchQuery by remember { mutableStateOf("") }
    val payoutPresets = listOf(550.0, 750.0, 950.0, 1200.0)

    val filteredAgents = remember(rankedAgents, searchQuery) {
        if (searchQuery.isBlank()) rankedAgents
        else rankedAgents.filter {
            it.applicantName.contains(searchQuery, ignoreCase = true) ||
            it.operatingCity.contains(searchQuery, ignoreCase = true) ||
            it.operatingPincode.contains(searchQuery, ignoreCase = true) ||
            it.priorDomain.contains(searchQuery, ignoreCase = true) ||
            it.applicantPhone.contains(searchQuery, ignoreCase = true)
        }
    }

    val selectedAgent1Obj = remember(rankedAgents, selectedAgentId) {
        rankedAgents.firstOrNull { it.userId == selectedAgentId }
    }
    val selectedAgent2Obj = remember(rankedAgents, selectedAgent2Id) {
        rankedAgents.firstOrNull { it.userId == selectedAgent2Id }
    }

    val isSameAgentDualError = isDualProtocol && selectedAgentId.isNotBlank() && selectedAgentId == selectedAgent2Id

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
                        text = if (isDualProtocol) "Dispatch Blind Dual-Officer Audit" else "Map Inspection to Certified Field Officer",
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
                            text = if (isDualProtocol) "🛡️ Blind Dual-Officer Swapped Protocol (Active)" else "Standard Single-Officer Verification",
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

            Spacer(modifier = Modifier.height(10.dp))

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
                        Text("Target Party: ${visit.borrowerName}", color = TextSlateMuted, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dual Protocol Slot Selection Cards
            if (isDualProtocol) {
                Text(
                    text = "Assign Officers to Audit Roles",
                    color = TextNavyDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Slot 1 Card
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = if (activeSlot == 1) BrandIceBlue else CanvasPorcelain),
                        border = androidx.compose.foundation.BorderStroke(if (activeSlot == 1) 1.5.dp else 1.dp, if (activeSlot == 1) BrandRoyalBlue else BrandIceBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeSlot = 1 }
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (activeSlot == 1) BrandRoyalBlue else TextSlateMuted,
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("1", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Officer 1 (Borrower)", color = TextNavyDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = selectedAgent1Obj?.applicantName?.ifBlank { "Choose Officer" } ?: "Choose Officer",
                                color = if (selectedAgentId.isNotBlank()) BrandRoyalBlue else TextSlateMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Slot 2 Card
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = if (activeSlot == 2) GoldCoinCream else CanvasPorcelain),
                        border = androidx.compose.foundation.BorderStroke(if (activeSlot == 2) 1.5.dp else 1.dp, if (activeSlot == 2) GoldCoinAmber else BrandIceBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeSlot = 2 }
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (activeSlot == 2) GoldCoinAmber else TextSlateMuted,
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("2", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Officer 2 (Lender)", color = TextNavyDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = selectedAgent2Obj?.applicantName?.ifBlank { "Choose Officer" } ?: "Choose Officer",
                                color = if (selectedAgent2Id.isNotBlank()) GoldCoinRich else TextSlateMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (isSameAgentDualError) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Red500.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Red500.copy(alpha = 0.3f))
                    ) {
                        Row(modifier = Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = Red500, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Anti-Collusion Rule: Officer 1 and Officer 2 must be different agents.",
                                color = Red500,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Quick Auto-Assign & Search Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isDualProtocol) "Select for Officer $activeSlot" else "Select Certified Field Officer",
                    color = TextNavyDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                // ⚡ Smart Auto-Assign Button
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = BrandRoyalBlue.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandRoyalBlue.copy(alpha = 0.3f)),
                    modifier = Modifier.clickable {
                        if (isDualProtocol) {
                            if (rankedAgents.size >= 2) {
                                selectedAgentId = rankedAgents[0].userId
                                selectedAgent2Id = rankedAgents[1].userId
                            }
                        } else {
                            rankedAgents.firstOrNull()?.let { selectedAgentId = it.userId }
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Bolt, null, tint = BrandRoyalBlue, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("⚡ Auto-Assign Nearest", color = BrandRoyalBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Search Filter Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search officer by name, city, pin, or domain...", color = TextSlateMuted, fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = BrandRoyalBlue, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, null, tint = TextSlateMuted, modifier = Modifier.size(14.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = CanvasPorcelain,
                    focusedBorderColor = BrandRoyalBlue,
                    unfocusedBorderColor = BrandIceBorder,
                    focusedTextColor = TextNavyDark,
                    unfocusedTextColor = TextNavyDark
                ),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Available Agents Roster
            if (filteredAgents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CanvasPorcelain)
                        .border(1.dp, BrandIceBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No officers match search criteria", color = TextSlateMuted, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 190.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredAgents) { agent ->
                        val isSelectedAsSlot1 = selectedAgentId == agent.userId
                        val isSelectedAsSlot2 = selectedAgent2Id == agent.userId
                        val isCurrentlySelected = if (isDualProtocol) {
                            if (activeSlot == 1) isSelectedAsSlot1 else isSelectedAsSlot2
                        } else isSelectedAsSlot1

                        val isPinMatch = agent.operatingPincode.isNotBlank() && visit.targetAddress.contains(agent.operatingPincode)
                        val isCityMatch = agent.operatingCity.isNotBlank() && visit.targetAddress.contains(agent.operatingCity, ignoreCase = true)
                        val isTopRanked = rankedAgents.firstOrNull()?.userId == agent.userId
                        val activeLoad = agentWorkload[agent.userId] ?: 0

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrentlySelected) BrandIceBlue 
                                                 else if (isDualProtocol && (isSelectedAsSlot1 || isSelectedAsSlot2)) CanvasPorcelain.copy(alpha = 0.6f) 
                                                 else CanvasPorcelain
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isCurrentlySelected) 1.5.dp else 1.dp,
                                if (isCurrentlySelected) BrandRoyalBlue else BrandIceBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isDualProtocol) {
                                        if (activeSlot == 1) {
                                            selectedAgentId = agent.userId
                                            if (selectedAgent2Id.isBlank() && rankedAgents.size >= 2) {
                                                activeSlot = 2
                                            }
                                        } else {
                                            selectedAgent2Id = agent.userId
                                        }
                                    } else {
                                        selectedAgentId = agent.userId
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isCurrentlySelected) BrandRoyalBlue.copy(alpha = 0.12f) else Gray200,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.DirectionsBike,
                                            null,
                                            tint = if (isCurrentlySelected) BrandRoyalBlue else TextSlateMedium,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = agent.applicantName.ifBlank { "Certified Field Officer" },
                                            color = TextNavyDark,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        if (isTopRanked) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = GoldCoinCream,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, GoldCoinBorder)
                                            ) {
                                                Text(
                                                    text = "⭐ Best Match",
                                                    color = GoldCoinAmber,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        if (isPinMatch) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = EmeraldLight,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Emerald400.copy(alpha = 0.5f))
                                            ) {
                                                Text(
                                                    text = "🎯 Exact PIN",
                                                    color = Emerald600,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        } else if (isCityMatch) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = BrandIceBlue,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, BrandIceBorder)
                                            ) {
                                                Text(
                                                    text = "🏙️ Same City",
                                                    color = BrandCobalt,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${agent.operatingCity} (${agent.operatingPincode}) • ${agent.priorDomain} • ${agent.vehicleType}",
                                        color = if (isCurrentlySelected) BrandCobalt else TextSlateMuted,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (activeLoad == 0) "🟢 Available (0 active tasks)" else "🟡 $activeLoad active task(s)",
                                            color = if (activeLoad == 0) Emerald600 else GoldCoinAmber,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (isDualProtocol && isSelectedAsSlot1) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("• [Assigned Officer 1]", color = BrandRoyalBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        } else if (isDualProtocol && isSelectedAsSlot2) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("• [Assigned Officer 2]", color = GoldCoinRich, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Quick Contact buttons (Call & WhatsApp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (agent.applicantPhone.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                try {
                                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${agent.applicantPhone}"))
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {}
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Phone, contentDescription = "Call", tint = Emerald500, modifier = Modifier.size(15.dp))
                                        }
                                    }

                                    if (isCurrentlySelected) {
                                        Icon(Icons.Default.CheckCircle, null, tint = BrandRoyalBlue, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Payout Configurator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Field Inspection Bounty / Payout (₹)",
                    color = TextNavyDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isDualProtocol) {
                    Text("2 x ₹${payoutAmount.toInt()} = ₹${(payoutAmount * 2).toInt()}", color = GoldCoinAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
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
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            Text(
                                text = "₹${amount.toInt()}",
                                color = if (isSelected) Color.White else TextSlateMedium,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Confirm Dispatch Button
            Button(
                onClick = {
                    if (isDualProtocol && onDispatchDual != null && selectedAgentId.isNotBlank() && selectedAgent2Id.isNotBlank() && !isSameAgentDualError) {
                        onDispatchDual(selectedAgentId, selectedAgent2Id, payoutAmount)
                    } else if (selectedAgentId.isNotBlank()) {
                        onDispatch(selectedAgentId, payoutAmount)
                    }
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedAgentId.isNotBlank() && (!isDualProtocol || (selectedAgent2Id.isNotBlank() && !isSameAgentDualError)),
                colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue, contentColor = Color.White)
            ) {
                Icon(if (isDualProtocol) Icons.Default.Security else Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(16.dp))
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

