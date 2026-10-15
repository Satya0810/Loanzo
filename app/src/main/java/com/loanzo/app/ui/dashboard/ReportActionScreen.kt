package com.loanzo.app.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loanzo.app.data.entity.ComplaintEntity
import com.loanzo.app.ui.components.UserPickerDropdown
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.LocalAdminRepository
import com.loanzo.app.util.LocalUserRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportActionScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val adminRepository = LocalAdminRepository.current
    val userRepository = LocalUserRepository.current
    val coroutineScope = rememberCoroutineScope()

    val currentUserId by userRepository.getCurrentUserId().collectAsStateWithLifecycle(initialValue = null)
    val currentUser by (if (!currentUserId.isNullOrBlank()) userRepository.observeUser(currentUserId!!) else kotlinx.coroutines.flow.flowOf(null)).collectAsStateWithLifecycle(initialValue = null)

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Report & Take Action",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header Warning Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Red400.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, Red400.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Red400.copy(alpha = 0.15f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReportProblem,
                            contentDescription = null,
                            tint = Red400,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Formal Legal & Platform Filing",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Complaints are digitally logged, submitted to compliance officers, and dispatched to legal enforcement desks.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 1. Counterparty / Target Person
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TARGET INDIVIDUAL / COUNTERPARTY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val quickTransactors = remember {
                        listOf(
                            Triple("satyam0810", "@satyam0810 (Admin)", "ADMIN"),
                            Triple("abhisi", "@abhisi (Field Agent)", "AGENT"),
                            Triple("compliance", "@compliance (Compliance Desk)", "SYSTEM")
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickTransactors.forEach { (tUser, tLabel, _) ->
                            val isSelected = targetPerson == tUser
                            FilterChip(
                                selected = isSelected,
                                onClick = { targetPerson = if (isSelected) "" else tUser },
                                label = {
                                    Text(
                                        tLabel,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Gold500,
                                    selectedLabelColor = Navy900
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) Gold500 else MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }
                    }

                    UserPickerDropdown(
                        selectedUserId = targetPerson,
                        onUserSelected = { user ->
                            targetPerson = if (user.userId.isNotBlank()) user.name.ifBlank { user.username.ifBlank { user.userId } } else ""
                        },
                        label = "Target Member to Report",
                        placeholder = "Scroll down to select or search user...",
                        onSearchOnline = { query -> userRepository.searchUsersOnline(query) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = targetPerson,
                        onValueChange = { targetPerson = it },
                        label = { Text("Or Type Username / Phone / Name") },
                        placeholder = { Text("e.g. John Doe or +91 98765 43210") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // 2. Infraction Category
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "VIOLATION REASON",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    categories.chunked(2).forEach { rowCats ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowCats.forEach { cat ->
                                val isSelected = selectedCategory == cat
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategory = cat },
                                    label = {
                                        Text(
                                            cat,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Gold500,
                                        selectedLabelColor = Navy900
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = if (isSelected) Gold500 else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowCats.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // 3. Severity Level
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "INCIDENT SEVERITY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        severities.forEach { lvl ->
                            val isSelected = severity == lvl
                            val activeColor = when (lvl) {
                                "High" -> Red400
                                "Medium" -> Gold500
                                else -> Emerald500
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { severity = lvl },
                                label = {
                                    Text(
                                        lvl,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = activeColor,
                                    selectedLabelColor = if (lvl == "Medium") Navy900 else Color.White
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) activeColor else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 4. Narrative / Evidence Statement
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "STATEMENT & EVIDENCE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = incidentDetails,
                        onValueChange = { incidentDetails = it },
                        placeholder = {
                            Text(
                                "Describe what happened, dates, amounts, transaction reference numbers, or broken agreement clauses..."
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // 5. Requested Enforcement Actions
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "REQUESTED ENFORCEMENT ACTIONS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { requestPlatformFreeze = !requestPlatformFreeze }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = requestPlatformFreeze,
                            onCheckedChange = { requestPlatformFreeze = it },
                            colors = CheckboxDefaults.colors(checkedColor = Red400)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Account Restriction / Freeze",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Temporarily pause user's access to new loans and marketplace deals",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { requestLegalNotice = !requestLegalNotice }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = requestLegalNotice,
                            onCheckedChange = { requestLegalNotice = it },
                            colors = CheckboxDefaults.colors(checkedColor = Gold500)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Digital Legal Notice Demand",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Issue registered compliance notice under Section 138 / P2P covenants",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { requestTelegramAlert = !requestTelegramAlert }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = requestTelegramAlert,
                            onCheckedChange = { requestTelegramAlert = it },
                            colors = CheckboxDefaults.colors(checkedColor = BrandRoyalBlue)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Compliance Officer Priority Review",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Route report directly to platform administrators and fraud cell",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Submit Button
            Button(
                onClick = {
                    if (targetPerson.isBlank()) {
                        Toast.makeText(context, "Please specify the counterparty name or ID.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (incidentDetails.isBlank()) {
                        Toast.makeText(context, "Please provide statement details of the incident.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    coroutineScope.launch {
                        isSubmitting = true
                        try {
                            val reporterName = currentUser?.name?.ifBlank { currentUser?.username } ?: "Anonymous"
                            val reporterPhone = currentUser?.phone ?: "Unknown"
                            val requestedActions = buildList {
                                if (requestPlatformFreeze) add("Account Restriction")
                                if (requestLegalNotice) add("Legal Notice")
                                if (requestTelegramAlert) add("Admin Review")
                            }.joinToString(", ")

                            val descriptionText = buildString {
                                appendLine("Category: $selectedCategory")
                                appendLine("Severity: $severity")
                                appendLine("Target: $targetPerson")
                                appendLine("Requested Actions: $requestedActions")
                                if (incidentDetails.isNotBlank()) {
                                    appendLine("Details: $incidentDetails")
                                }
                            }

                            val complaint = ComplaintEntity(
                                complaintId = "comp_${System.currentTimeMillis()}",
                                complainantId = currentUser?.userId ?: "anonymous",
                                complainantName = reporterName,
                                complainantPhone = reporterPhone,
                                complainantRole = currentUser?.role ?: "BORROWER",
                                targetPartyName = targetPerson,
                                subject = "[$severity] $selectedCategory: $targetPerson",
                                description = descriptionText,
                                category = selectedCategory,
                                priority = severity.uppercase(),
                                status = "OPEN",
                                createdAt = System.currentTimeMillis()
                            )

                            adminRepository.submitComplaint(complaint)

                            Toast.makeText(context, "Complaint filed successfully with Compliance Desk.", Toast.LENGTH_LONG).show()
                            onBack()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to submit: ${e.message}", Toast.LENGTH_SHORT).show()
                            isSubmitting = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp),
                enabled = !isSubmitting,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Red400,
                    contentColor = Color.White
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Submitting Complaint...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Formal Report & Take Action", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
