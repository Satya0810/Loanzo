package com.loanzo.app.ui.profile

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.blur
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loanzo.app.data.model.UserProfileData
import com.loanzo.app.data.model.UserVerificationStatus
import com.loanzo.app.data.model.WhyTrustworthySignal
import com.loanzo.app.ui.components.GlassCard
import com.loanzo.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    viewModel: UserProfileViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToChat: (channelId: String, loanId: String?, targetUserId: String?) -> Unit,
    onNavigateToKyc: () -> Unit = {}
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showMoreMenu by remember { mutableStateOf(false) }
    val moreMenuBlurRadius by animateDpAsState(
        targetValue = if (showMoreMenu) 20.dp else 0.dp,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "profile_menu_blur"
    )
    val moreMenuScrimAlpha by animateFloatAsState(
        targetValue = if (showMoreMenu) 0.45f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "profile_menu_scrim"
    )

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    LaunchedEffect(state.actionMessage, state.error) {
        state.actionMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearFeedback()
        }
        state.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "User Profile & Verification",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Report User", color = Red400) },
                            leadingIcon = { Icon(Icons.Default.ReportProblem, null, tint = Red400) },
                            onClick = {
                                showMoreMenu = false
                                viewModel.setReportDialogVisible(true)
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (state.profileData?.isBlocked == true) "Unblock User" else "Block User",
                                    color = if (state.profileData?.isBlocked == true) Emerald400 else Red400
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    if (state.profileData?.isBlocked == true) Icons.Default.LockOpen else Icons.Default.Block,
                                    null,
                                    tint = if (state.profileData?.isBlocked == true) Emerald400 else Red400
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                viewModel.setBlockDialogVisible(true)
                            }
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
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Gold500)
            }
        } else {
            val profile = state.profileData
            if (profile == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("User not found or profile unavailable.")
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(moreMenuBlurRadius)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            ProfileHeroCard(profile = profile)
                        }

                        item {
                            VerificationStatusCard(status = profile.verificationStatus)
                        }

                        item {
                            TrustScoreGaugeCard(
                                trustScore = profile.trustScore,
                                tier = profile.trustScoreTier
                            )
                        }

                        item {
                            WhyTrustworthySection(signals = profile.trustworthySignals)
                        }

                        item {
                            MultiFactorAuditCard(profile = profile)
                        }

                        item {
                            RepaymentHistoryGrid(profile = profile)
                        }

                        item {
                            ProfileActionsSection(
                                profile = profile,
                                onChatClick = {
                                    val myId = state.currentUserId ?: ""
                                    val channel = com.loanzo.app.ui.loan.ChatViewModel.getDirectChannelId(myId, profile.userId)
                                    onNavigateToChat(channel, null, profile.userId)
                                },
                                onReportClick = { viewModel.setReportDialogVisible(true) },
                                onBlockClick = { viewModel.setBlockDialogVisible(true) },
                                onReKycClick = onNavigateToKyc
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }

                    if (moreMenuScrimAlpha > 0.01f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = moreMenuScrimAlpha))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    showMoreMenu = false
                                }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (state.showReportDialog) {
        ReportUserDialog(
            userName = state.profileData?.name ?: "User",
            onDismiss = { viewModel.setReportDialogVisible(false) },
            onSubmit = { category, details, reqFreeze ->
                viewModel.submitReport(category, details, reqFreeze)
            }
        )
    }

    if (state.showBlockDialog) {
        val isBlocked = state.profileData?.isBlocked == true
        AlertDialog(
            onDismissRequest = { viewModel.setBlockDialogVisible(false) },
            icon = {
                Icon(
                    if (isBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                    contentDescription = null,
                    tint = if (isBlocked) Emerald400 else Red400
                )
            },
            title = {
                Text(if (isBlocked) "Unblock User?" else "Block User?")
            },
            text = {
                Text(
                    if (isBlocked) {
                        "Unblocking ${state.profileData?.name} will allow you to see their marketplace offers and receive messages from them."
                    } else {
                        "Are you sure you want to block ${state.profileData?.name}? Their marketplace posts, lending offers, and messages will be hidden from you."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.toggleBlockUser() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBlocked) Emerald400 else Red400,
                        contentColor = Color.White
                    )
                ) {
                    Text(if (isBlocked) "Confirm Unblock" else "Block User")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setBlockDialogVisible(false) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT 1: HERO IDENTITY HEADER
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ProfileHeroCard(profile: UserProfileData) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Avatar with Monogram
            Surface(
                shape = CircleShape,
                color = Gold500.copy(alpha = 0.2f),
                border = BorderStroke(2.dp, Gold500),
                modifier = Modifier.size(68.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = profile.name.firstOrNull()?.uppercase() ?: "U",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Gold500
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (profile.verificationStatus == UserVerificationStatus.VERIFIED) {
                        Surface(
                            shape = CircleShape,
                            color = Emerald400.copy(alpha = 0.2f),
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Verified",
                                tint = Emerald400,
                                modifier = Modifier.padding(3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "@${profile.username.removePrefix("@")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Role Pill
                    val isLender = profile.role.uppercase() == "LENDER"
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isLender) Gold500.copy(alpha = 0.15f) else Emerald400.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isLender) "CAPITAL PROVIDER" else "VERIFIED BORROWER",
                            color = if (isLender) Gold500 else Emerald400,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Account Age
                    val dateStr = remember(profile.memberSince) {
                        try {
                            SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(profile.memberSince))
                        } catch (_: Exception) {
                            "Recent"
                        }
                    }
                    Text(
                        text = "Member since $dateStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT 2: VERIFICATION STATUS CARD
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun VerificationStatusCard(status: UserVerificationStatus) {
    val (bgColor, borderColor, icon, title, subtitle) = when (status) {
        UserVerificationStatus.VERIFIED -> {
            Quint(
                Emerald400.copy(alpha = 0.12f),
                Emerald400.copy(alpha = 0.5f),
                Icons.Default.Verified,
                "Fully Verified Identity",
                "Aadhaar, PAN & DigiLocker identity documents have been legally verified."
            )
        }
        UserVerificationStatus.PARTIALLY_VERIFIED -> {
            Quint(
                Gold500.copy(alpha = 0.12f),
                Gold500.copy(alpha = 0.5f),
                Icons.Default.Pending,
                "Partially Verified Member",
                "Phone and basic KYC completed. DigiLocker attestation is recommended."
            )
        }
        UserVerificationStatus.NOT_VERIFIED -> {
            Quint(
                Red400.copy(alpha = 0.12f),
                Red400.copy(alpha = 0.5f),
                Icons.Default.Warning,
                "Identity Not Verified",
                "User has not yet completed government identity verification."
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (status == UserVerificationStatus.VERIFIED) Emerald400 else if (status == UserVerificationStatus.PARTIALLY_VERIFIED) Gold500 else Red400,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT 3: TRUST SCORE GAUGE CARD
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TrustScoreGaugeCard(trustScore: Int, tier: String) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = Gold500,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Loanzo Trust Score",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Gold500.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = tier,
                        color = Gold500,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$trustScore",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (trustScore >= 85) Emerald400 else if (trustScore >= 70) Gold500 else Red400
                    )
                    Text(
                        text = " / 100",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                val percentile = when {
                    trustScore >= 95 -> "Top 1% Borrower Rating"
                    trustScore >= 90 -> "Top 5% Trust Percentile"
                    trustScore >= 80 -> "Top 15% Platform Standard"
                    else -> "Average Trust Tier"
                }
                Text(
                    text = percentile,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Score Bar
            LinearProgressIndicator(
                progress = { (trustScore / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (trustScore >= 85) Emerald400 else if (trustScore >= 70) Gold500 else Red400,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Computed from database records: verified KYC, on-time repayments, completed contracts, and dispute history.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT 4: WHY IS THIS USER TRUSTWORTHY?
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WhyTrustworthySection(signals: List<WhyTrustworthySignal>) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = Emerald400.copy(alpha = 0.15f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Emerald400,
                        modifier = Modifier.padding(6.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Why is this user trustworthy?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Objective verification & reputation signals to help make decisions",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            signals.forEachIndexed { index, sig ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (sig.isPassed) Emerald400.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (sig.isPassed) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (sig.isPassed) Emerald400 else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = sig.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = sig.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (sig.isPassed) Emerald400.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = sig.badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sig.isPassed) Emerald400 else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (index < signals.size - 1) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT 5: MULTI-FACTOR VERIFICATION AUDIT CHECKLIST
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MultiFactorAuditCard(profile: UserProfileData) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Verification Audit Checklist",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            VerificationRow(
                icon = Icons.Default.Phone,
                title = "Phone Verification",
                detail = profile.maskedPhone,
                isVerified = profile.isPhoneVerified
            )

            VerificationRow(
                icon = Icons.Default.Email,
                title = "Email Verification",
                detail = profile.maskedEmail,
                isVerified = profile.isEmailVerified
            )

            VerificationRow(
                icon = Icons.Default.Badge,
                title = "Aadhaar & PAN Identity KYC",
                detail = if (profile.isKycVerified) "Verified via UIDAI / NSDL" else "Pending submission",
                isVerified = profile.isKycVerified
            )

            VerificationRow(
                icon = Icons.Default.FolderShared,
                title = "DigiLocker Attestation",
                detail = profile.maskedDigiLockerId ?: "Not linked",
                isVerified = profile.isDigiLockerVerified
            )

            VerificationRow(
                icon = Icons.Default.AccountBalance,
                title = "Bank Account & UPI VPA",
                detail = if (profile.isBankVerified) "Penny Drop Verified" else "Pending setup",
                isVerified = profile.isBankVerified
            )
        }
    }
}

@Composable
private fun VerificationRow(
    icon: ImageVector,
    title: String,
    detail: String,
    isVerified: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isVerified) Emerald400.copy(alpha = 0.15f) else Red400.copy(alpha = 0.15f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (isVerified) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (isVerified) Emerald400 else Red400,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isVerified) "Verified" else "Unverified",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (isVerified) Emerald400 else Red400
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT 6: REPAYMENT & FINANCIAL HISTORY GRID
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RepaymentHistoryGrid(profile: UserProfileData) {
    val rep = profile.repaymentSummary
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Repayment & Financial History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricItemBox(
                    label = "Completed Loans",
                    value = "${rep.completedLoansCount}",
                    sub = "Settled with 0 disputes",
                    modifier = Modifier.weight(1f)
                )
                MetricItemBox(
                    label = "Active Loans",
                    value = "${rep.activeLoansCount}",
                    sub = "Currently serviced",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricItemBox(
                    label = "On-Time Repayment",
                    value = "${String.format("%.1f", rep.onTimeRepaymentRate)}%",
                    sub = "${rep.punctualEmisCount} EMIs on-time",
                    valueColor = Emerald400,
                    modifier = Modifier.weight(1f)
                )
                MetricItemBox(
                    label = "Defaults",
                    value = "${rep.defaultsCount}",
                    sub = if (rep.defaultsCount == 0) "Clean record" else "Overdue contracts",
                    valueColor = if (rep.defaultsCount == 0) Emerald400 else Red400,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricItemBox(
    label: String,
    value: String,
    sub: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT 7: PROFILE ACTIONS SECTION
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ProfileActionsSection(
    profile: UserProfileData,
    onChatClick: () -> Unit,
    onReportClick: () -> Unit,
    onBlockClick: () -> Unit,
    onReKycClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (profile.isOwnProfile) {
            Button(
                onClick = onReKycClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandRoyalBlue,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.PublishedWithChanges, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Update / Re-KYC Credentials",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        } else {
            // Main Action: Connect / Direct Chat
            Button(
                onClick = onChatClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandRoyalBlue,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Connect / Chat with ${profile.name.split(" ").firstOrNull() ?: "Member"}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onReportClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Red400.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Red400)
                ) {
                    Icon(Icons.Default.ReportProblem, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Report User", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onBlockClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Icon(
                        if (profile.isBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (profile.isBlocked) "Unblock" else "Block",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT 8: REPORT USER DIALOG
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ReportUserDialog(
    userName: String,
    onDismiss: () -> Unit,
    onSubmit: (category: String, details: String, requestFreeze: Boolean) -> Unit
) {
    var category by remember { mutableStateOf("Suspicious Activity") }
    var details by remember { mutableStateOf("") }
    var requestFreeze by remember { mutableStateOf(true) }

    val categories = listOf(
        "Default / Payment Refusal",
        "Fraudulent / Fake Identity",
        "Abuse & Harassment",
        "Agreement Breach",
        "Suspicious Activity"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.ReportProblem, contentDescription = null, tint = Red400)
        },
        title = {
            Text("Report $userName", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Submit formal complaint to Platform Administration for investigation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("Violation Category", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                categories.forEach { cat ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { category = cat }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = category == cat,
                            onClick = { category = cat },
                            colors = RadioButtonDefaults.colors(selectedColor = Red400)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(cat, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Details & Evidence (Optional)") },
                    placeholder = { Text("Describe the incident or evidence...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = requestFreeze,
                        onCheckedChange = { requestFreeze = it },
                        colors = CheckboxDefaults.colors(checkedColor = Red400)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Demand platform account restriction", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(category, details, requestFreeze) },
                colors = ButtonDefaults.buttonColors(containerColor = Red400, contentColor = Color.White)
            ) {
                Text("Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private data class Quint<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
