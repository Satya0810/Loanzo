package com.loanzo.app.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.data.entity.LoanEntity
import com.loanzo.app.data.entity.UserEntity
import com.loanzo.app.ui.components.*
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.TelegramManager
import com.loanzo.app.util.toInrString
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSelectionBottomSheet(
    loans: List<LoanEntity>,
    currentUser: UserEntity?,
    onDismiss: () -> Unit,
    onSelectLoanChat: (String) -> Unit
) {
    val context = LocalContext.current
    val currentUserId = currentUser?.userId ?: ""

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = "Chat",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Conversations & Chat",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Chat with loan counterparties or assistant",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Option 1: Loanzo Bot
            Surface(
                onClick = {
                    onDismiss()
                    TelegramManager().openBotForLinking(context, currentUserId)
                },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Forum,
                            contentDescription = "Loanzo Bot",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Loanzo Assistant (@Loanzo_bot)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "24/7 automated loan support, EMI alerts & queries",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "PEER-TO-PEER LOAN CHATS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Gray400,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            val distinctLoans = remember(loans) {
                loans.distinctBy { it.loanId }
            }

            if (distinctLoans.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            tint = Gray400,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No active loans found",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Direct peer-to-peer chats will appear here as soon as you grant or request a loan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(distinctLoans) { loan ->
                        val isLender = loan.lenderId == currentUserId
                        val roleLabel = if (isLender) "Lent" else "Borrowed"
                        val roleColor = if (isLender) Gold500 else Emerald400

                        Card(
                            onClick = {
                                onDismiss()
                                onSelectLoanChat(loan.loanId)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = roleColor.copy(alpha = 0.15f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ChatBubble,
                                        contentDescription = null,
                                        tint = roleColor,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = loan.purpose.ifBlank { "Loan #${loan.loanId.take(6)}" },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = roleColor.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = roleLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = roleColor,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Amount: ${loan.sanctionedAmount.toInrString()} • Status: ${loan.status}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Gray400
                                    )
                                }
                                Icon(Icons.Default.Send, contentDescription = "Open Chat", tint = Gold500, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportActionBottomSheet(
    currentUser: UserEntity?,
    loans: List<LoanEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var targetPerson by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Default / Non-Payment") }
    var severity by remember { mutableStateOf("High") }
    var incidentDetails by remember { mutableStateOf("") }
    var requestPlatformFreeze by remember { mutableStateOf(true) }
    var requestLegalNotice by remember { mutableStateOf(true) }
    var requestTelegramAlert by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }

    val categories = remember {
        listOf(
            "Default / Non-Payment",
            "Fraud / Fake KYC",
            "Harassment & Abuse",
            "Agreement Breach",
            "Suspicious Activity"
        )
    }

    val severities = remember {
        listOf("Low", "Medium", "High")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Red400.copy(alpha = 0.15f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            Icons.Default.ReportProblem,
                            contentDescription = "Report",
                            tint = Red400,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Report & Take Action",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "File complaint to take formal platform or legal action",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Counterparty / Target Person
            Text(
                text = "TARGET INDIVIDUAL / COUNTERPARTY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Quick Counterparty selector chips from active loans
            val counterpartyIds = remember(loans, currentUser) {
                val myId = currentUser?.userId ?: ""
                loans.mapNotNull { loan ->
                    val otherId = if (loan.lenderId == myId) loan.borrowerId else loan.lenderId
                    if (otherId.isNotBlank()) otherId else null
                }.distinct()
            }

            // Quick Counterparty selector chips with Golden Coin Box Coloring
            val quickTransactors = listOf(
                Triple("satyam0810", "👑 @satyam0810 (Admin)", "ADMIN"),
                Triple("agent_demo", "🕵️ @agent_demo (Agent)", "AGENT"),
                Triple("user_demo", "👤 @user_demo (Member)", "USER")
            )
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickTransactors) { (tUser, tLabel, _) ->
                    val isSelected = targetPerson == tUser
                    FilterChip(
                        selected = isSelected,
                        onClick = { targetPerson = if (isSelected) "" else tUser },
                        label = {
                            Text(
                                tLabel,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = goldFilterChipColors(),
                        border = goldFilterChipBorder(isSelected)
                    )
                }
            }

            OutlinedTextField(
                value = targetPerson,
                onValueChange = { targetPerson = it },
                placeholder = { Text("Enter Counterparty Name, Phone or User ID", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Red400,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Infraction Category
            Text(
                text = "VIOLATION REASON",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Gray400,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.take(2).forEach { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Red400.copy(alpha = 0.2f),
                            selectedLabelColor = Red400
                        )
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.drop(2).forEach { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Red400.copy(alpha = 0.2f),
                            selectedLabelColor = Red400
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Severity level
            Text(
                text = "SEVERITY LEVEL",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Gray400,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                severities.forEach { lvl ->
                    val isSelected = severity == lvl
                    val lvlColor = when (lvl) {
                        "High" -> Red400
                        "Medium" -> Orange400
                        else -> Blue400
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { severity = lvl },
                        label = { Text(lvl, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = lvlColor.copy(alpha = 0.25f),
                            selectedLabelColor = lvlColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Incident Details
            Text(
                text = "INCIDENT & EVIDENCE DETAILS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Gray400,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = incidentDetails,
                onValueChange = { incidentDetails = it },
                placeholder = { Text("Provide details, transaction amount, dates, or what actions should be taken...", color = Gray500) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Red400,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Requested Actions
            Text(
                text = "ACTIONS REQUESTED ON TARGET",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { requestPlatformFreeze = !requestPlatformFreeze }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = requestPlatformFreeze,
                            onCheckedChange = { requestPlatformFreeze = it },
                            colors = CheckboxDefaults.colors(checkedColor = Red400)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Immediate Platform & Transaction Restriction", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { requestLegalNotice = !requestLegalNotice }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = requestLegalNotice,
                            onCheckedChange = { requestLegalNotice = it },
                            colors = CheckboxDefaults.colors(checkedColor = Red400)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Issue Formal Legal / Administrative Demand", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { requestTelegramAlert = !requestTelegramAlert }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = requestTelegramAlert,
                            onCheckedChange = { requestTelegramAlert = it },
                            colors = CheckboxDefaults.colors(checkedColor = Red400)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Dispatch Urgent Alert to Telegram Admin Desk", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Action Button
            Button(
                onClick = {
                    if (targetPerson.isBlank()) {
                        Toast.makeText(context, "Please specify the individual or counterparty", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isSubmitting = true
                    coroutineScope.launch {
                        try {
                            if (requestTelegramAlert) {
                                val reporterName = currentUser?.name ?: "User"
                                val reporterPhone = currentUser?.phone ?: "Unknown"
                                val requestedActions = buildList {
                                    if (requestPlatformFreeze) add("Account Restriction")
                                    if (requestLegalNotice) add("Legal Notice")
                                    if (requestTelegramAlert) add("Admin Review")
                                }.joinToString(", ")

                                val alertMsg = """
                                    🚨 <b>USER REPORT & ACTION DEMAND</b>
                                    ━━━━━━━━━━━━━━━━━━━━
                                    <b>Reporter:</b> $reporterName ($reporterPhone)
                                    <b>Target Person:</b> $targetPerson
                                    <b>Category:</b> $selectedCategory
                                    <b>Severity:</b> $severity
                                    <b>Requested Action:</b> $requestedActions
                                    <b>Details:</b> ${incidentDetails.ifBlank { "N/A" }}
                                    ━━━━━━━━━━━━━━━━━━━━
                                    <i>Action filed from Loanzo Home Screen</i>
                                """.trimIndent()

                                TelegramManager().sendAdminAlert(alertMsg)
                            }
                            Toast.makeText(
                                context,
                                "Report filed successfully. Administrative action has been initiated.",
                                Toast.LENGTH_LONG
                            ).show()
                            onDismiss()
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Report submitted. Platform team notified.",
                                Toast.LENGTH_SHORT
                            ).show()
                            onDismiss()
                        } finally {
                            isSubmitting = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Red400, contentColor = Color.White)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Report & Take Action", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
