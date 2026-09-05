package com.loanzo.app.ui.loan

import androidx.compose.ui.res.stringResource
import com.loanzo.app.R

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.loanzo.app.data.entity.*
import com.loanzo.app.ui.components.*
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.toDateString
import com.loanzo.app.util.toInrString
import com.loanzo.app.util.toRelativeTime
import kotlinx.coroutines.launch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.sp
import com.loanzo.app.util.calculateEMI
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateLoanScreen(
    isGrantMode: Boolean = false,
    isKycCompleted: Boolean = true,
    onNavigateToKyc: () -> Unit = {},
    onCreateLoan: (counterpartyId: String, amount: Double, purpose: String, loanType: String,
                   interestRate: Double, interestModel: String, tenureMonths: Int,
                   repaymentFrequency: String, notes: String,
                   penaltyRate: Double, penaltyModel: String, penaltyGraceDays: Int) -> Unit,
    onBack: () -> Unit,
    loanCreated: Boolean = false,
    registeredUsers: List<com.loanzo.app.data.entity.UserEntity> = emptyList()
) {
    var counterpartyUserId by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("50000") }
    var purpose by remember { mutableStateOf("") }
    var selectedLoanType by remember { mutableStateOf("PERSONAL") }
    var interestRate by remember { mutableStateOf("12.0") }
    var selectedInterestModel by remember { mutableStateOf("SIMPLE") }
    var tenure by remember { mutableStateOf("12") }
    var selectedFrequency by remember { mutableStateOf("MONTHLY") }
    var notes by remember { mutableStateOf("") }
    var penaltyRate by remember { mutableStateOf("2.0") }
    var selectedPenaltyModel by remember { mutableStateOf("PERCENTAGE") }
    var penaltyGraceDays by remember { mutableStateOf("3") }

    val loanTypes = listOf("PERSONAL", "BUSINESS", "EDUCATION", "MEDICAL", "AGRICULTURE", "OTHER")
    val interestModels = listOf("SIMPLE", "COMPOUND", "FLAT", "NONE")
    val frequencies = listOf("MONTHLY", "WEEKLY", "BI_WEEKLY")
    val penaltyModels = listOf("PERCENTAGE", "FLAT", "NONE")

    val quickAmounts = listOf(10000.0, 25000.0, 50000.0, 100000.0, 250000.0, 500000.0)
    val quickTenures = listOf(3, 6, 12, 24, 36)

    // Real-time calculations
    val principalNum = amount.toDoubleOrNull() ?: 0.0
    val rateNum = interestRate.toDoubleOrNull() ?: 0.0
    val tenureNum = (tenure.toIntOrNull() ?: 1).coerceAtLeast(1)

    val emi = if (principalNum > 0 && tenureNum > 0) {
        if (selectedInterestModel == "NONE" || rateNum == 0.0) {
            principalNum / tenureNum
        } else {
            calculateEMI(principalNum, rateNum, tenureNum)
        }
    } else 0.0

    val totalPayment = if (selectedInterestModel == "NONE" || rateNum == 0.0) {
        principalNum
    } else if (selectedInterestModel == "FLAT") {
        principalNum + (principalNum * (rateNum / 100.0) * (tenureNum / 12.0))
    } else {
        emi * tenureNum
    }

    val totalInterest = (totalPayment - principalNum).coerceAtLeast(0.0)

    val installmentAmount = when (selectedFrequency) {
        "WEEKLY" -> totalPayment / (tenureNum * 4.33).coerceAtLeast(1.0)
        "BI_WEEKLY" -> totalPayment / (tenureNum * 2.16).coerceAtLeast(1.0)
        else -> emi
    }

    LaunchedEffect(loanCreated) {
        if (loanCreated) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isGrantMode) "Grant a Loan" else "Request a Loan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (!isKycCompleted) {
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
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Red400,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "KYC Verification Required",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Red400
                            )
                            Text(
                                "You must complete identity verification before taking, borrowing, or granting loans.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onNavigateToKyc,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Red400, contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Verify", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 1. DYNAMIC LIVE CALCULATOR PREVIEW CARD
            GradientCard(
                gradientColors = listOf(Navy700, Navy900),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = GoldCoinRich.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Calculate, null, tint = GoldCoinRich, modifier = Modifier.padding(8.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Live Loan Simulator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Real-time repayment calculation", style = MaterialTheme.typography.labelSmall, color = Gray400)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Emerald400.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "${selectedFrequency.replace("_", " ")}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Emerald400,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                when (selectedFrequency) {
                                    "WEEKLY" -> "Estimated Weekly Due"
                                    "BI_WEEKLY" -> "Estimated Bi-Weekly Due"
                                    else -> "Estimated Monthly EMI"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = Gray300
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                installmentAmount.toInrString(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldCoinBright
                            )
                        }

                        // Mini Interactive Donut Chart
                        Box(
                            modifier = Modifier.size(72.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val principalAngle = if (totalPayment > 0) ((principalNum / totalPayment) * 360f).toFloat() else 360f
                            val interestAngle = if (totalPayment > 0) ((totalInterest / totalPayment) * 360f).toFloat() else 0f

                            Canvas(modifier = Modifier.size(64.dp)) {
                                drawArc(
                                    color = Emerald400,
                                    startAngle = -90f,
                                    sweepAngle = principalAngle,
                                    useCenter = false,
                                    style = Stroke(width = 16f, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = GoldCoinRich,
                                    startAngle = -90f + principalAngle,
                                    sweepAngle = interestAngle,
                                    useCenter = false,
                                    style = Stroke(width = 16f, cap = StrokeCap.Round)
                                )
                            }
                            Icon(Icons.Default.PieChart, null, tint = Gray400, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Gray700.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Emerald400))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Principal", style = MaterialTheme.typography.labelSmall, color = Gray400, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(principalNum.toInrString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Emerald400, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                        }

                        Column(modifier = Modifier.weight(1.1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(GoldCoinRich))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Total Interest", style = MaterialTheme.typography.labelSmall, color = Gray400, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(totalInterest.toInrString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GoldCoinAmber, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                        }

                        Column(modifier = Modifier.weight(1.1f), horizontalAlignment = Alignment.End) {
                            Text("Total Payable", style = MaterialTheme.typography.labelSmall, color = Gray400, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(totalPayment.toInrString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            // 2. COUNTERPARTY & DETAILS
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(if (isGrantMode) "Borrower Information & Purpose" else "Lender Information & Purpose", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = counterpartyUserId,
                    onValueChange = { counterpartyUserId = it.trim() },
                    label = { Text(if (isGrantMode) "Borrower User ID" else "Lender User ID") },
                    placeholder = { Text(if (isGrantMode) "Enter Borrower's User ID, Phone, or @username" else "Enter Lender's User ID, Phone, or @username") },
                    leadingIcon = { Icon(if (isGrantMode) Icons.Default.Person else Icons.Default.Badge, null, tint = Gold500) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                // 1. Comprehensive Candidate Transactors with Role & Username Mapping
                val allCandidateUsers = remember(registeredUsers) {
                    val defaultSeedList = listOf(
                        UserEntity(
                            userId = "usr_satyam_owner",
                            name = "Satyam Kumar",
                            email = "satyam@loanzo.app",
                            phone = "+91 70615 59039",
                            username = "satyam0810",
                            role = "ADMIN",
                            kycStatus = "VERIFIED"
                        ),
                        UserEntity(
                            userId = "usr_agent_field_01",
                            name = "Vikas Sharma",
                            email = "vikas.agent@loanzo.app",
                            phone = "+91 98100 12345",
                            username = "agent_demo",
                            role = "AGENT",
                            kycStatus = "VERIFIED"
                        ),
                        UserEntity(
                            userId = "usr_demo_consumer",
                            name = "Arjun Mehta",
                            email = "arjun.mehta@demo.loanzo.app",
                            phone = "+91 98200 54321",
                            username = "user_demo",
                            role = "USER",
                            kycStatus = "VERIFIED"
                        ),
                        UserEntity(
                            userId = "usr_rahul_borrower",
                            name = "Rahul Sharma",
                            email = "rahul.sharma@demo.loanzo.app",
                            phone = "+91 98765 43210",
                            username = "rahul_sharma",
                            role = "BORROWER",
                            kycStatus = "VERIFIED"
                        ),
                        UserEntity(
                            userId = "usr_priya_lender",
                            name = "Priya Patel",
                            email = "priya.patel@demo.loanzo.app",
                            phone = "+91 91234 56789",
                            username = "priya_invest",
                            role = "LENDER",
                            kycStatus = "VERIFIED"
                        )
                    )
                    (registeredUsers + defaultSeedList).distinctBy { it.userId }
                }

                var transactorRoleFilter by remember { mutableStateOf("ALL") }
                val transactorRoles = listOf("ALL", "ADMIN", "AGENT", "MEMBER", "BORROWER", "LENDER")

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick Pick Transactor & Role:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (counterpartyUserId.isNotBlank()) {
                        Text(
                            text = "Clear ✕",
                            style = MaterialTheme.typography.labelSmall,
                            color = Red400,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { counterpartyUserId = "" }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                // Role Category Filter with Golden Coin Box Coloring
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(transactorRoles) { rRole ->
                        val isRoleSelected = transactorRoleFilter == rRole
                        FilterChip(
                            selected = isRoleSelected,
                            onClick = { transactorRoleFilter = rRole },
                            label = {
                                Text(
                                    when (rRole) {
                                        "ADMIN" -> "👑 Admin"
                                        "AGENT" -> "🕵️ Agent"
                                        "MEMBER" -> "👤 Member"
                                        "BORROWER" -> "Borrower"
                                        "LENDER" -> "Lender"
                                        else -> "🌟 All Roles"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = if (isRoleSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = goldFilterChipColors(),
                            border = goldFilterChipBorder(isRoleSelected)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Transactor User Chips with username (@satyam0810, etc.) and role badges
                val filteredTransactors = remember(allCandidateUsers, transactorRoleFilter) {
                    if (transactorRoleFilter == "ALL") allCandidateUsers
                    else allCandidateUsers.filter {
                        when (transactorRoleFilter) {
                            "ADMIN" -> it.role.equals("ADMIN", ignoreCase = true)
                            "AGENT" -> it.role.equals("AGENT", ignoreCase = true)
                            "MEMBER" -> it.role.equals("USER", ignoreCase = true) || it.role.equals("MEMBER", ignoreCase = true)
                            "BORROWER" -> it.role.equals("BORROWER", ignoreCase = true)
                            "LENDER" -> it.role.equals("LENDER", ignoreCase = true)
                            else -> true
                        }
                    }
                }

                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredTransactors) { user ->
                        val isUserSelected = counterpartyUserId == user.userId || counterpartyUserId == user.username
                        val roleTag = when (user.role.uppercase()) {
                            "ADMIN" -> "👑 Admin"
                            "AGENT" -> "🕵️ Agent"
                            "BORROWER" -> "Borrower"
                            "LENDER" -> "Lender"
                            else -> "Member"
                        }
                        val userChipLabel = if (user.username.isNotBlank()) "@${user.username} ($roleTag)" else "${user.name.split(" ").firstOrNull() ?: user.phone} ($roleTag)"

                        FilterChip(
                            selected = isUserSelected,
                            onClick = {
                                counterpartyUserId = if (isUserSelected) "" else user.userId
                            },
                            leadingIcon = {
                                Icon(
                                    when (user.role.uppercase()) {
                                        "ADMIN" -> Icons.Default.AdminPanelSettings
                                        "AGENT" -> Icons.Default.Engineering
                                        else -> Icons.Default.AccountCircle
                                    },
                                    contentDescription = null,
                                    tint = if (isUserSelected) GoldCoinAmber else GoldCoinRich,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    userChipLabel,
                                    fontWeight = if (isUserSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = goldFilterChipColors(),
                            border = goldFilterChipBorder(isUserSelected)
                        )
                    }
                }

                // 2. Real-time matching suggestions
                val matchedUsers = remember(counterpartyUserId, allCandidateUsers) {
                    if (counterpartyUserId.length >= 2) {
                        allCandidateUsers.filter {
                            it.name.contains(counterpartyUserId, ignoreCase = true) ||
                            it.phone.contains(counterpartyUserId, ignoreCase = true) ||
                            it.username.contains(counterpartyUserId, ignoreCase = true) ||
                            it.userId.equals(counterpartyUserId, ignoreCase = true)
                        }.take(4)
                    } else emptyList()
                }

                if (matchedUsers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Select Matching User:", style = MaterialTheme.typography.labelSmall, color = Gold500)
                    Spacer(modifier = Modifier.height(4.dp))
                    matchedUsers.forEach { user ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { counterpartyUserId = user.userId }
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Gold500.copy(alpha = 0.2f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Gold500, modifier = Modifier.padding(8.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(user.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("${user.phone} • @${user.username.ifBlank { "user" }}", style = MaterialTheme.typography.labelSmall, color = Gray400)
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Emerald400.copy(alpha = 0.15f)
                                ) {
                                    Text("✓ KYC Verified", color = Emerald400, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    label = { Text("Purpose of Loan") },
                    placeholder = { Text("e.g., Home renovation, medical, business") },
                    leadingIcon = { Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Loan Category", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    loanTypes.forEach { type ->
                        FilterChip(
                            selected = selectedLoanType == type,
                            onClick = { selectedLoanType = type },
                            label = { Text(type.replace("_", " "), fontWeight = if (selectedLoanType == type) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(10.dp),
                            colors = goldFilterChipColors(),
                            border = goldFilterChipBorder(selectedLoanType == type)
                        )
                    }
                }
            }

            // 3. INTERACTIVE AMOUNT & PRESETS
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Loan Amount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Principal Amount (₹)") },
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, null, tint = Emerald400) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Amount Presets
                Text("Quick Select Amount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickAmounts.forEach { amt ->
                        val isSelected = amount == amt.toInt().toString()
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Emerald400 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = if (isSelected) Navy900 else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { amount = amt.toInt().toString() }
                        ) {
                            Text(
                                if (amt >= 100000) "₹${(amt / 100000).toInt()}L" else "₹${(amt / 1000).toInt()}K",
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Add (+) Increment Chips (Pocket-Log style)
                Text("Quick Increment (+)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(5000, 10000, 25000, 50000).forEach { inc ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Gold500.copy(alpha = 0.15f),
                            contentColor = Gold500,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val current = amount.toDoubleOrNull() ?: 0.0
                                    amount = (current + inc).toInt().toString()
                                }
                        ) {
                            Text(
                                "+₹${inc / 1000}K",
                                modifier = Modifier.padding(vertical = 7.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Amount Slider
                Slider(
                    value = principalNum.toFloat().coerceIn(5000f, 1000000f),
                    onValueChange = { amount = it.roundToInt().toString() },
                    valueRange = 5000f..1000000f,
                    steps = 199,
                    colors = SliderDefaults.colors(thumbColor = Emerald400, activeTrackColor = Emerald400)
                )
            }

            // 4. INTEREST RATE & TENURE SLIDERS
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Interest Rate & Tenure", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = interestRate,
                        onValueChange = { interestRate = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Interest Rate (% p.a.)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = tenure,
                        onValueChange = { tenure = it.filter { c -> c.isDigit() } },
                        label = { Text("Tenure (Months)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Interest Rate Slider
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Interest Rate Slider", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${String.format(java.util.Locale.getDefault(), "%.1f", rateNum)}%", fontWeight = FontWeight.Bold, color = Gold500)
                }
                Slider(
                    value = rateNum.toFloat().coerceIn(0f, 36f),
                    onValueChange = { interestRate = String.format(java.util.Locale.getDefault(), "%.1f", it) },
                    valueRange = 0f..36f,
                    steps = 71,
                    colors = SliderDefaults.colors(thumbColor = Gold500, activeTrackColor = Gold500)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Tenure Chips
                Text("Quick Select Tenure", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickTenures.forEach { mo ->
                        val isSelected = tenure == mo.toString()
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Gold500 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = if (isSelected) Navy900 else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { tenure = mo.toString() }
                        ) {
                            Text(
                                "$mo Mo",
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Interest Model selection
                Text("Interest Calculation Model", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    interestModels.forEach { model ->
                        FilterChip(
                            selected = selectedInterestModel == model,
                            onClick = { selectedInterestModel = model },
                            label = { Text(model) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Repayment Frequency selection
                Text("Repayment Frequency", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    frequencies.forEach { freq ->
                        FilterChip(
                            selected = selectedFrequency == freq,
                            onClick = { selectedFrequency = freq },
                            label = { Text(freq.replace("_", " ")) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // 5. PENALTY & GRACE PERIOD
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Late Payment & Penalty Rules", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    penaltyModels.forEach { model ->
                        FilterChip(
                            selected = selectedPenaltyModel == model,
                            onClick = { selectedPenaltyModel = model },
                            label = { Text(model) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                if (selectedPenaltyModel != "NONE") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = penaltyRate,
                            onValueChange = { penaltyRate = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text(if (selectedPenaltyModel == "PERCENTAGE") "Penalty Rate (%/mo)" else "Flat Fee (₹/mo)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        OutlinedTextField(
                            value = penaltyGraceDays,
                            onValueChange = { penaltyGraceDays = it.filter { c -> c.isDigit() } },
                            label = { Text("Grace Period (Days)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
            }

            // 6. NOTES
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Notes & Terms (Optional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("Add any special terms, payment conditions or remarks...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 7. SUBMIT BUTTON
            Button(
                onClick = {
                    if (!isKycCompleted) {
                        onNavigateToKyc()
                        return@Button
                    }
                    onCreateLoan(
                        counterpartyUserId,
                        principalNum,
                        purpose,
                        selectedLoanType,
                        rateNum,
                        selectedInterestModel,
                        tenureNum,
                        selectedFrequency,
                        notes,
                        penaltyRate.toDoubleOrNull() ?: 2.0,
                        selectedPenaltyModel,
                        penaltyGraceDays.toIntOrNull() ?: 3
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isKycCompleted) Red400 else (if (isGrantMode) Gold500 else Emerald400),
                    contentColor = if (!isKycCompleted) Color.White else Navy900
                ),
                enabled = !isKycCompleted || (principalNum > 0 && purpose.isNotBlank() && counterpartyUserId.isNotBlank())
            ) {
                Icon(if (!isKycCompleted) Icons.Default.Lock else (if (isGrantMode) Icons.Default.Upload else Icons.Default.Send), null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (!isKycCompleted) {
                        "Complete KYC to ${if (isGrantMode) "Grant" else "Request"} Loan"
                    } else if (isGrantMode) {
                        if (principalNum > 0) "Grant ${principalNum.toInrString()} Loan" else "Grant Loan"
                    } else {
                        if (principalNum > 0) "Request ${principalNum.toInrString()} Loan" else "Request Loan"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    state: LoanUiState,
    isLender: Boolean = false,
    onBack: () -> Unit,
    onRequestTranche: () -> Unit,
    onMakeRepayment: () -> Unit,
    onAddPledge: () -> Unit,
    onViewAuditTrail: () -> Unit,
    onSignAgreement: (String) -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToDocument: () -> Unit,
    onNavigateToGuarantors: () -> Unit = {},
    onSendReminder: () -> Unit = {},
    onExportLoanSummaryPdf: () -> Unit = {},
    onExportInterestCertPdf: () -> Unit = {},
    onExportRepaymentsCsv: () -> Unit = {},
    onExportPitchDeckPdf: () -> Unit = {},
    onWaivePenalty: (RepaymentEntity) -> Unit = {},
    onRestructureLoan: (Int, Int) -> Unit = { _, _ -> },
    onAcceptProposal: () -> Unit = {},
    onDeclineProposal: () -> Unit = {},
    onDisburseLoan: (amount: Double, utr: String) -> Unit = { _, _ -> },
    onDownloadNocCertificate: () -> Unit = {},
    onExportAgreementPdf: () -> Unit = {}
) {
    val loan = state.selectedLoan
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAllSchedule by remember { mutableStateOf(false) }
    var showAllDisbursements by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showRestructureDialog by remember { mutableStateOf(false) }
    var showPrepaymentSheet by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showDisburseDialog by remember { mutableStateOf(false) }
    var utrText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.loan_dashboard), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showQrDialog = true }) {
                        Icon(Icons.Default.QrCodeScanner, "UPI QR Code", tint = Emerald400)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LaunchedEffect(state.message) {
            state.message?.let {
                snackbarHostState.showSnackbar(it)
            }
        }

        if (loan == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 0A. PROPOSAL REVIEW GATE
            if (loan.status == "DRAFT" || loan.status == "PROPOSED") {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Gold500.copy(alpha = 0.08f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Gold500.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Gold500, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Pending Mutual Acceptance",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Review the proposed loan terms. Once accepted, both parties proceed to sign the binding legal agreement.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onAcceptProposal,
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Accept Terms", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }
                                OutlinedButton(
                                    onClick = onDeclineProposal,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Red400),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Decline", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                }
            }

            // 0B. DISBURSAL ACTION GATE
            if (loan.isAgreementSigned && loan.disbursedAmount < loan.sanctionedAmount && isLender) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Emerald400.copy(alpha = 0.08f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Emerald400.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald400, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Agreement Signed — Ready to Disburse",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Both parties have completed biometric & digital eSignatures. Transfer ${loan.sanctionedAmount.toInrString()} to borrower via UPI to activate loan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showDisburseDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Disburse ${loan.sanctionedAmount.toInrString()} via UPI ➔", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 0C. SETTLED & NOC CERTIFICATE
            if (loan.status == "CLOSED" || (loan.disbursedAmount > 0 && loan.outstandingAmount <= 0.0)) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Emerald400.copy(alpha = 0.15f)),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.linearGradient(listOf(Gold500, Emerald400))
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = Gold500, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "🎉 Loan Fully Repaid & Cleared",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Zero outstanding balance remaining. Your official No Objection Certificate (NOC) is ready for download.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray300
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onDownloadNocCertificate,
                                colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Navy900),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(46.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download Official NOC Certificate (PDF) 📜", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            //  1. NEXT PAYMENT DUE CARD 
            val nextDue = state.nextDueRepayment
            if (nextDue != null) {
                item {
                    val isOverdue = state.daysUntilNextDue < 0
                    val gradientColors = if (isOverdue) listOf(Red500, Red400) else listOf(Navy700, Navy900)
                    GradientCard(gradientColors = gradientColors) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            ) {
                                Text(
                                    if (isOverdue) "Payment Overdue" else "Next Payment Due",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isOverdue) Color.White.copy(alpha = 0.8f) else Gray300
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    nextDue.amount.toInrString(),
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOverdue) Color.White else GoldCoinBright
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (isOverdue) "Overdue by ${-state.daysUntilNextDue} days • Due ${nextDue.dueDate.toDateString()}"
                                    else "Due in ${state.daysUntilNextDue} days • ${nextDue.dueDate.toDateString()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                            Button(
                                onClick = if (isLender) onSendReminder else onMakeRepayment,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isOverdue) Color.White else GoldCoinRich,
                                    contentColor = if (isOverdue) Red500 else Color.White
                                )
                            ) {
                                Icon(
                                    if (isLender) Icons.Default.Notifications else Icons.Default.Payment, 
                                    null, 
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (isLender) "Remind" else stringResource(R.string.pay_now), 
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            //  2. LOAN OVERVIEW CARD (Pocket-Log & Dunio elevated)
            item {
                val catStyle = getCategoryStyle(loan.loanType.ifBlank { loan.purpose })
                val repaidRatio = if (loan.sanctionedAmount > 0) (state.totalPaid / loan.sanctionedAmount).toFloat() else 0f
                val progressPercent = (repaidRatio * 100).toInt().coerceIn(0, 100)

                GradientCard(gradientColors = listOf(Navy700, Navy900)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(catStyle.bgColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = catStyle.icon,
                                    contentDescription = catStyle.label,
                                    tint = catStyle.iconColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    loan.purpose,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    StatusBadge(text = loan.status, color = when (loan.status) {
                                        "ACTIVE" -> Emerald400
                                        "CLOSED" -> Gray400
                                        "DEFAULTED" -> Red400
                                        else -> GoldCoinRich
                                    })
                                    if (loan.isRestructured) {
                                        StatusBadge(text = "RESTRUCTURED", color = GoldCoinRich)
                                    }
                                    if (loan.moratoriumMonths > 0) {
                                        StatusBadge(text = "MORATORIUM: ${loan.moratoriumMonths}M", color = GoldCoinAmber)
                                    }
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.sanctioned), style = MaterialTheme.typography.labelSmall, color = Gray400)
                            Text(
                                loan.sanctionedAmount.toInrString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = GoldCoinBright
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress Bar inside overview
                    LoanProgressBar(
                        progress = repaidRatio,
                        fillColors = listOf(Emerald400, Emerald500),
                        height = 6
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${state.totalPaid.toInrString()} Repaid",
                            fontSize = 11.sp,
                            color = Emerald400,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "$progressPercent%",
                            fontSize = 11.sp,
                            color = Gray300,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(stringResource(R.string.disbursed), style = MaterialTheme.typography.labelSmall, color = Gray400)
                            Text(state.totalDisbursed.toInrString(), style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold, color = GoldCoinBright)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.outstanding), style = MaterialTheme.typography.labelSmall, color = Gray400)
                            Text(loan.outstandingAmount.toInrString(), style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold, color = if (loan.outstandingAmount > 0) GoldCoinAmber else Emerald400)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.repaid), style = MaterialTheme.typography.labelSmall, color = Gray400)
                            Text(state.totalPaid.toInrString(), style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold, color = Emerald400)
                        }
                    }
                }
            }

            // 2.1. PREPAYMENT & FORECLOSURE SIMULATOR (Jupiter & Cred inspired)
            if (loan.outstandingAmount > 0) {
                item {
                    Card(
                        onClick = { showPrepaymentSheet = true },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Emerald400.copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    Icons.Default.Savings,
                                    contentDescription = null,
                                    tint = Emerald400,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Prepayment Simulator",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Gold500.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            "SAVE ₹",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Gold500
                                        )
                                    }
                                }
                                Text(
                                    "Simulate lump sum or EMI boost to slash interest & tenure",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Emerald400,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Utilization ring
            if (state.totalDisbursed > 0) {
                item {
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(stringResource(R.string.verified_utilization), style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Verified: ${state.totalVerified.toInrString()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Total Disbursed: ${state.totalDisbursed.toInrString()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            CircularProgressIndicator(
                                progress = { (if (state.totalDisbursed > 0) state.totalVerified / state.totalDisbursed else 0.0).toFloat() },
                                modifier = Modifier.size(64.dp),
                                color = Emerald400,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    }
                }
            }

            //  3. INTERACTIVE CHARTS 
            if (state.balanceHistory.size >= 2 || state.totalPaid > 0) {
                item {
                    SectionHeader(title = " Insights")
                }

                // Balance Over Time chart
                if (state.balanceHistory.size >= 2) {
                    item {
                        GlassCard {
                            Text(stringResource(R.string.balance_over_time), style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.outstanding_balance_trend), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                            BalanceLineChart(
                                balanceHistory = state.balanceHistory,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Payment Breakdown donut
                if (state.totalPrincipalPaid > 0 || state.totalInterestPaid > 0) {
                    item {
                        GlassCard {
                            Text(stringResource(R.string.payment_breakdown), style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.principal_vs_interest_split), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                            PaymentDonutChart(
                                principalPaid = state.totalPrincipalPaid,
                                interestPaid = state.totalInterestPaid,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            //  4. REPAYMENT SCHEDULE / AMORTIZATION TIMELINE 
            if (state.amortizationSchedule.isNotEmpty()) {
                item {
                    SectionHeader(title = " Repayment Schedule")
                }
                val visibleSchedule = if (showAllSchedule) state.amortizationSchedule
                    else state.amortizationSchedule.take(4)

                items(visibleSchedule) { schedule ->
                    ScheduleTimelineItem(schedule)
                }

                if (state.amortizationSchedule.size > 4) {
                    item {
                        TextButton(
                            onClick = { showAllSchedule = !showAllSchedule },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                if (showAllSchedule) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null, modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (showAllSchedule) "Show Less" else "Show All ${state.amortizationSchedule.size} Installments",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            //  5. TRANCHES & DISBURSEMENT HISTORY 
            if (state.disbursements.isNotEmpty()) {
                item {
                    SectionHeader(title = " Disbursements")
                }
                val visibleDisb = if (showAllDisbursements) state.disbursements
                    else state.disbursements.take(3)

                items(visibleDisb) { disb ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (disb.approvalStatus == "PENDING")
                                Orange400.copy(alpha = 0.06f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(disb.payeeName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(disb.purpose, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        disb.timestamp.toRelativeTime(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                Text(disb.amount.toInrString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatusBadge(disb.approvalStatus, color = when (disb.approvalStatus) {
                                    "APPROVED" -> Emerald400; "REJECTED" -> Red400; else -> Orange400
                                })
                                StatusBadge(disb.ruleEngineResult, color = when (disb.ruleEngineResult) {
                                    "CONSISTENT", "AUTO_APPROVED" -> Emerald400
                                    "MISMATCH" -> Orange400; "BLOCKED" -> Red400; else -> Gray400
                                })
                            }
                        }
                    }
                }

                if (state.disbursements.size > 3) {
                    item {
                        TextButton(
                            onClick = { showAllDisbursements = !showAllDisbursements },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (showAllDisbursements) "Show Less" else "View All ${state.disbursements.size} Disbursements",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            //  6. REPAYMENT HISTORY 
            if (state.repayments.isNotEmpty()) {
                item { SectionHeader(title = " Repayments") }
                items(state.repayments) { repayment ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = when (repayment.status) {
                                        "PAID" -> Emerald400; "OVERDUE" -> Red400; else -> Gold500
                                    }.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        when (repayment.status) {
                                            "PAID" -> Icons.Default.CheckCircle
                                            "OVERDUE" -> Icons.Default.Warning
                                            else -> Icons.Default.Schedule
                                        },
                                        null,
                                        modifier = Modifier.padding(8.dp),
                                        tint = when (repayment.status) {
                                            "PAID" -> Emerald400; "OVERDUE" -> Red400; else -> Gold500
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(repayment.amount.toInrString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        if (repayment.penalty > 0.0) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("+ ${repayment.penalty.toInrString()} penalty", style = MaterialTheme.typography.labelSmall, color = Red400)
                                            if (isLender && !repayment.penaltyWaived) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                TextButton(
                                                    onClick = { onWaivePenalty(repayment) },
                                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                                ) {
                                                    Text("Waive", style = MaterialTheme.typography.labelSmall, color = Gold500, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                                }
                                            }
                                        }
                                        if (repayment.penaltyWaived) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("(Penalty Waived)", style = MaterialTheme.typography.labelSmall, color = Emerald400)
                                        }
                                    }
                                    Text(
                                        if (repayment.paidDate != null) "Paid ${repayment.paidDate.toDateString()}"
                                        else "Due ${repayment.dueDate.toDateString()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            StatusBadge(repayment.status, color = when (repayment.status) {
                                "PAID" -> Emerald400; "OVERDUE" -> Red400; else -> Gold500
                            })
                        }
                    }
                }
            }

            //  7. COLLATERAL & PLEDGES OVERVIEW 
            item {
                SectionHeader(title = " Collateral & Pledges")
            }

            if (state.pledges.isEmpty()) {
                item {
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            ) {
                                Text(stringResource(R.string.no_collateral_pledged), style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(stringResource(R.string.adding_pledges_can_improve_loan_terms), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                            OutlinedButton(
                                onClick = onAddPledge,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.add), maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            } else {
                // LTV Health Summary
                item {
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.loan_to_value_ltv), style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Pledge: ${state.totalPledgeValue.toInrString()} vs Outstanding: ${loan.outstandingAmount.toInrString()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            // LTV Badge
                            val ltvColor = when {
                                state.ltvRatio < 60f -> Emerald400
                                state.ltvRatio < 80f -> Orange400
                                else -> Red400
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ltvColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "${state.ltvRatio.toInt()}%",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ltvColor
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        // LTV progress bar
                        val ltvColor = when {
                            state.ltvRatio < 60f -> Emerald400
                            state.ltvRatio < 80f -> Orange400
                            else -> Red400
                        }
                        val animatedLtv by animateFloatAsState(
                            targetValue = (state.ltvRatio / 100f).coerceIn(0f, 1f),
                            animationSpec = tween(1000, easing = FastOutSlowInEasing),
                            label = "ltv_bar"
                        )
                        LinearProgressIndicator(
                            progress = { animatedLtv },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = ltvColor,
                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.str_0), style = MaterialTheme.typography.labelSmall, color = Gray400)
                            Text(stringResource(R.string.safe), style = MaterialTheme.typography.labelSmall, color = Emerald400)
                            Text(stringResource(R.string.risk), style = MaterialTheme.typography.labelSmall, color = Orange400)
                            Text(stringResource(R.string.str_100), style = MaterialTheme.typography.labelSmall, color = Red400)
                        }
                    }
                }

                // Individual pledge items
                items(state.pledges) { pledge ->
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = when (pledge.assetType) {
                                        "GOLD" -> Gold500; "VEHICLE" -> Blue400
                                        "PROPERTY" -> Emerald400; else -> Gray400
                                    }.copy(alpha = 0.15f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        when (pledge.assetType) {
                                            "GOLD" -> Icons.Default.Diamond
                                            "VEHICLE" -> Icons.Default.DirectionsCar
                                            "PROPERTY" -> Icons.Default.Home
                                            "EQUIPMENT" -> Icons.Default.Build
                                            else -> Icons.Default.Security
                                        },
                                        null,
                                        modifier = Modifier.padding(8.dp),
                                        tint = when (pledge.assetType) {
                                            "GOLD" -> Gold500; "VEHICLE" -> Blue400
                                            "PROPERTY" -> Emerald400; else -> Gray400
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(pledge.assetDescription, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text("${pledge.assetType}  ${pledge.receiptStatus}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(pledge.estimatedValue.toInrString(), fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }

            //  7b. GUARANTORS & CO-BORROWERS 
            item {
                SectionHeader(title = " Co-Borrowers & Guarantors")
            }
            item {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Text(
                                if (state.guarantors.isNotEmpty()) "${state.guarantors.size} Guarantor(s) Attached" else "No Guarantor Added",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                if (state.guarantors.isNotEmpty()) "Consent: ${state.guarantors.count { it.consentStatus == "ACCEPTED" }} Accepted, ${state.guarantors.count { it.consentStatus == "PENDING" }} Pending"
                                else "Add a guarantor or co-borrower to strengthen terms",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onNavigateToGuarantors,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.People, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Manage", maxLines = 1, softWrap = false)
                        }
                    }
                }
            }

            //  8. DOCUMENT VAULT 
            item {
                SectionHeader(title = " Documents & Reports")
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DocumentVaultItem(
                        icon = Icons.Default.FileDownload,
                        title = "Export Portfolio Reports",
                        subtitle = "Download Loan Summary PDF, Interest Certificate, CSV",
                        accentColor = Gold500,
                        onClick = { showExportSheet = true }
                    )
                    DocumentVaultItem(
                        icon = Icons.Default.Description,
                        title = if (loan.isAgreementSigned) "Digital Loan Agreement (Signed)" else "Sign Loan Agreement",
                        subtitle = if (loan.isAgreementSigned) "Signed digitally • Tap to View PDF" else "Action Required",
                        accentColor = if (loan.isAgreementSigned) Emerald400 else Blue400,
                        onClick = {
                            if (loan.isAgreementSigned) {
                                onExportAgreementPdf()
                            } else {
                                onSignAgreement(loan.loanId)
                            }
                        }
                    )
                    DocumentVaultItem(
                        icon = Icons.Default.Assignment,
                        title = "Sanction Letter & Summary",
                        subtitle = "${loan.sanctionedAmount.toInrString()} sanctioned • Tap for Report",
                        accentColor = Emerald400,
                        onClick = onExportLoanSummaryPdf
                    )
                    DocumentVaultItem(
                        icon = Icons.Default.Gavel,
                        title = "Terms & Conditions",
                        subtitle = "${loan.interestRate}% ${loan.interestModel}  ${loan.tenureMonths} months",
                        accentColor = Gold500,
                        onClick = onNavigateToDocument
                    )
                }
            }

            //  9. LOAN TERMS 
            item {
                GlassCard {
                    Text(stringResource(R.string.loan_terms), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    LoanTermRow("Type", loan.loanType)
                    LoanTermRow("Interest", "${loan.interestRate}% (${loan.interestModel})")
                    LoanTermRow("Tenure", "${loan.tenureMonths} months" + if (loan.originalTenureMonths > 0) " (Orig: ${loan.originalTenureMonths}m)" else "")
                    if (loan.moratoriumMonths > 0) {
                        LoanTermRow("Moratorium", "${loan.moratoriumMonths} months")
                    }
                    LoanTermRow("Repayment", loan.repaymentFrequency)
                    LoanTermRow("Created", loan.createdAt.toDateString())
                    if (loan.penaltyModel != "NONE") {
                        LoanTermRow("Penalty", "${loan.penaltyRate}%/mo (${loan.penaltyModel}, ${loan.penaltyGraceDays}d grace)")
                    }
                    if (loan.notes.isNotBlank()) {
                        LoanTermRow("Notes", loan.notes)
                    }
                }
            }

            //  10. ACTION BUTTONS 
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onRequestTranche,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Navy900),
                        enabled = loan.status == "ACTIVE"
                    ) {
                        Icon(Icons.Default.Send, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.tranche), fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                    OutlinedButton(
                        onClick = onMakeRepayment,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = loan.status == "ACTIVE"
                    ) {
                        Icon(Icons.Default.Payment, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.repay), maxLines = 1, softWrap = false)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onAddPledge,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Security, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.pledge), maxLines = 1, softWrap = false)
                    }
                    OutlinedButton(
                        onClick = onViewAuditTrail,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.History, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.audit), maxLines = 1, softWrap = false)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToChat,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Chat, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Chat with Lender", maxLines = 1, softWrap = false)
                    }
                }
                if (isLender) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showRestructureDialog = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold500)
                        ) {
                            Icon(Icons.Default.Tune, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restructure Loan / Moratorium", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        if (showExportSheet) {
            ExportReportBottomSheet(
                onDismiss = { showExportSheet = false },
                onExportLoanSummaryPdf = onExportLoanSummaryPdf,
                onExportInterestCertPdf = onExportInterestCertPdf,
                onExportRepaymentsCsv = onExportRepaymentsCsv
            )
        }

        if (showRestructureDialog && loan != null) {
            RestructureLoanDialog(
                loan = loan,
                onDismiss = { showRestructureDialog = false },
                onRestructure = { newTenure, moratorium ->
                    onRestructureLoan(newTenure, moratorium)
                }
            )
        }

        if (showPrepaymentSheet && loan != null) {
            val monthlyEmi = if (loan.tenureMonths > 0) loan.outstandingAmount / loan.tenureMonths else 0.0
            PrepaymentSimulatorSheet(
                outstandingPrincipal = loan.outstandingAmount,
                annualInterestRate = loan.interestRate,
                tenureRemainingMonths = loan.tenureMonths.coerceAtLeast(1),
                currentMonthlyEmi = monthlyEmi.coerceAtLeast(500.0),
                onDismiss = { showPrepaymentSheet = false }
            )
        }

        if (showQrDialog && loan != null) {
            UpiQrCodeDialog(
                payeeName = "Loanzo Repayments",
                payeeUpiId = "merchant@upi",
                amount = state.nextDueRepayment?.amount ?: loan.outstandingAmount,
                loanPurpose = loan.purpose,
                onDismiss = { showQrDialog = false }
            )
        }

        if (showDisburseDialog && loan != null) {
            val context = androidx.compose.ui.platform.LocalContext.current
            AlertDialog(
                onDismissRequest = { showDisburseDialog = false },
                title = { Text("Disburse Loan Funds", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            "Transfer ${loan.sanctionedAmount.toInrString()} to borrower via any UPI app (GPay, PhonePe, Paytm), then record the UTR reference number below.",
                            fontSize = 13.sp,
                            color = Gray300
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                val uri = android.net.Uri.parse("upi://pay?pn=Borrower&am=${loan.sanctionedAmount}&cu=INR&tn=Loanzo+Disbursal+${loan.loanId.take(8)}")
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                try {
                                    context.startActivity(android.content.Intent.createChooser(intent, "Pay via UPI"))
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open UPI App to Transfer")
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = utrText,
                            onValueChange = { utrText = it },
                            label = { Text("UPI UTR / Bank Reference Number") },
                            placeholder = { Text("e.g. 423589102456") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDisburseLoan(loan.sanctionedAmount, utrText.ifBlank { "MANUAL_UPI_DISBURSE" })
                            showDisburseDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                    ) {
                        Text("Confirm Disbursal")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDisburseDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

//  HELPER COMPOSABLES 

/** Single timeline item in the amortization schedule */
@Composable
private fun ScheduleTimelineItem(schedule: ScheduleItem) {
    val statusColor = when (schedule.status) {
        "PAID" -> Emerald400
        "UPCOMING" -> Gold500
        "OVERDUE" -> Red400
        else -> Gray400
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (schedule.status == "UPCOMING")
                Gold500.copy(alpha = 0.06f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            Surface(
                shape = CircleShape,
                color = statusColor.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    when (schedule.status) {
                        "PAID" -> Icons.Default.CheckCircle
                        "UPCOMING" -> Icons.Default.Upcoming
                        "OVERDUE" -> Icons.Default.Warning
                        else -> Icons.Default.Schedule
                    },
                    null,
                    modifier = Modifier.padding(8.dp),
                    tint = statusColor
                )
            }
            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "EMI #${schedule.installmentNumber}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    StatusBadge(schedule.status, color = statusColor)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    schedule.dueDate.toDateString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        schedule.amount.toInrString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "P: ${"%.0f".format(schedule.principal)}  I: ${"%.0f".format(schedule.interest)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Clickable document item for the Document Vault */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentVaultItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left accent bar
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = accentColor,
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
            ) {}
            Spacer(modifier = Modifier.width(12.dp))

            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(icon, null, modifier = Modifier.padding(8.dp), tint = accentColor)
            }
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Icon(
                Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun LoanTermRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
