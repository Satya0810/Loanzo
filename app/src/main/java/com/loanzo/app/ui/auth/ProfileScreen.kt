package com.loanzo.app.ui.auth

import androidx.hilt.navigation.compose.hiltViewModel
import com.loanzo.app.ui.vault.DocumentVaultViewModel
import com.loanzo.app.data.entity.VaultDocumentEntity
import com.loanzo.app.util.t

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import com.loanzo.app.ui.components.LoanzoText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.loanzo.app.R
import com.loanzo.app.data.entity.UserEntity
import com.loanzo.app.ui.components.GlassCard
import com.loanzo.app.ui.components.LoanzoAvatar
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.BiometricAuthManager
import com.loanzo.app.util.getDisplayProfilePhoto
import com.loanzo.app.util.hashPassword
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.loanzo.app.ui.components.ContextualGuideCard
import com.loanzo.app.ui.components.AppTours
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween

/**
 * Navigation state for the professional Click-to-See Profile architecture
 */
enum class ProfileSubPage {
    MAIN,
    PERSONAL_INFO,
    DOCUMENT_VAULT,
    BANK_ACCOUNTS,
    PREFERENCES,
    TERMS_AND_CONDITIONS,
    ABOUT_US
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: UserEntity?,
    onNavigateToKyc: () -> Unit,
    onNavigateToAdminHub: () -> Unit = {},
    themeMode: String,
    onSetThemeMode: (String) -> Unit,
    currentLanguageCode: String = "en",
    onSelectLanguage: (String) -> Unit = {},
    onUploadProfilePhoto: (android.net.Uri) -> Unit = {},
    onUploadKycDocument: (android.net.Uri, String) -> Unit = { _, _ -> },
    onUpdateBankDetails: (String, String) -> Unit = { _, _ -> },
    onPushDemoData: ((Boolean, String) -> Unit) -> Unit = { _ -> },
    onClearDemoData: ((Boolean, String) -> Unit) -> Unit = { _ -> },
    isUploadingPan: Boolean = false,
    isUploadingAadhaar: Boolean = false,
    uploadMessage: String? = null,
    onClearUploadMessage: () -> Unit = {},
    onNavigateToAgent: () -> Unit = {},
    onLogout: () -> Unit,
    onBack: () -> Unit,
    vaultViewModel: DocumentVaultViewModel = hiltViewModel()
) {
    var currentSubPage by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(ProfileSubPage.MAIN) }
    var showBankDialog by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var isVaultUnlocked by remember { mutableStateOf(false) }
    var showPermissionsSheet by remember { mutableStateOf(false) }
    var showVaultPasswordDialog by remember { mutableStateOf(false) }
    var showRoleUpgradeDialog by remember { mutableStateOf(false) }
    var showRegisterTelegramDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val userRepository = com.loanzo.app.util.LocalUserRepository.current
    val profileGuideSeen by userRepository.isGuideSeen(com.loanzo.app.data.repository.UserRepository.GUIDE_PROFILE_SEEN)
        .collectAsStateWithLifecycle(initialValue = true)
    val isFloatingBotEnabled by userRepository.isFloatingChatbotEnabled().collectAsStateWithLifecycle(initialValue = true)
    val vaultState by vaultViewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    // Hardware/Gesture Back navigation handler
    BackHandler(enabled = currentSubPage != ProfileSubPage.MAIN) {
        currentSubPage = ProfileSubPage.MAIN
    }

    LaunchedEffect(uploadMessage) {
        uploadMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            onClearUploadMessage()
        }
    }

    if (user == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Gold500)
        }
        return
    }

    val isOwner = remember(user.phone, user.username, user.role) {
        com.loanzo.app.util.VerificationManager.isAppOwner(user)
    }
    val isFieldAgent = remember(user.phone, user.username, user.role, user.agentStatus) {
        com.loanzo.app.util.VerificationManager.isFieldAgent(user)
    }

    // Role Switcher Simulator is STRICTLY EXCLUSIVE to username satyam0810
    val canSwitchRoles = remember(user.username, user.phone, user.userId, user.email) {
        com.loanzo.app.util.VerificationManager.isEligibleAppOwner(user)
    }

    val profilePhotoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            onUploadProfilePhoto(uri)
        }
    }

    val panPdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onUploadKycDocument(it, "PAN") }
    }

    val aadhaarPdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onUploadKycDocument(it, "AADHAAR") }
    }

    // Animated transition across sub-pages ("Click to see and page appears")
    AnimatedContent(
        targetState = currentSubPage,
        transitionSpec = {
            if (targetState == ProfileSubPage.MAIN) {
                // Back transition to Main Hub: main slides in from left (-25% parallax), subpage slides out to right
                (slideInHorizontally(
                    initialOffsetX = { -it / 4 },
                    animationSpec = tween(280, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(280))) togetherWith
                (slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(280, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(220)))
            } else {
                // Forward transition into Sub-Page: subpage slides in from right, main slides out to left (-25% parallax)
                (slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(280, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(280))) togetherWith
                (slideOutHorizontally(
                    targetOffsetX = { -it / 4 },
                    animationSpec = tween(280, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(220)))
            }
        },
        label = "profile_subpage_transition"
    ) { subPage ->
        when (subPage) {
            ProfileSubPage.MAIN -> {
                // ==========================================
                // 1. MAIN PROFILE EXECUTIVE HUB (Revolut / Cred Style)
                // ==========================================
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.profile), fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                            actions = {
                                IconButton(onClick = { showLogoutConfirmDialog = true }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Sign Out",
                                        tint = Red400
                                    )
                                }
                            }
                        )
                    }
                ) { padding ->
                    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Modern Executive Horizontal Hero Card
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left: 72dp Avatar with role-based border
                                    Box(
                                        contentAlignment = Alignment.BottomEnd,
                                        modifier = Modifier.size(72.dp)
                                    ) {
                                        val avatarBorderColor = when {
                                            isOwner -> Gold500
                                            user.role.uppercase() == "ADMIN" -> Color(0xFF6366F1)
                                            isFieldAgent || user.role.uppercase() == "AGENT" -> Emerald400
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                        LoanzoAvatar(
                                            user = user,
                                            size = 72.dp,
                                            showVerifiedBadge = true,
                                            showEditBadge = true,
                                            borderColor = avatarBorderColor,
                                            borderWidth = 2.dp,
                                            onClick = { profilePhotoPickerLauncher.launch("image/*") },
                                            onEditClick = { profilePhotoPickerLauncher.launch("image/*") }
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    // Right: Identity Details, UID Copy, and Role Badge
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = user.name.ifBlank { "Loanzo Member" },
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )

                                            // Copyable UID pill
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                                modifier = Modifier.clickable {
                                                    clipboardManager.setText(AnnotatedString(user.userId))
                                                    Toast.makeText(context, "UID copied to clipboard", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "UID: ${user.userId.take(6)}...",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "COPY",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }

                                        if (user.username.isNotBlank()) {
                                            Text(
                                                text = "@${user.username}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Role & Status Pill Bar
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            val roleLabel = when {
                                                isOwner -> "👑 APP OWNER"
                                                user.role.uppercase() == "ADMIN" -> "🛡️ MASTER ADMIN"
                                                isFieldAgent || user.role.uppercase() == "AGENT" -> "🕵️ FIELD AGENT"
                                                user.role.uppercase() == "LENDER" -> "💼 CAPITAL LENDER"
                                                else -> "👤 BORROWER"
                                            }
                                            val roleColor = when {
                                                isOwner -> Gold500
                                                user.role.uppercase() == "ADMIN" -> Color(0xFF6366F1)
                                                isFieldAgent || user.role.uppercase() == "AGENT" -> Emerald400
                                                user.role.uppercase() == "LENDER" -> Blue400
                                                else -> MaterialTheme.colorScheme.primary
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = roleColor.copy(alpha = 0.15f),
                                                border = BorderStroke(0.5.dp, roleColor.copy(alpha = 0.4f))
                                            ) {
                                                Text(
                                                    text = roleLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = roleColor,
                                                    fontSize = 9.5.sp,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                                                )
                                            }

                                            // Duty or Verification Pill
                                            if (isFieldAgent || user.role.uppercase() == "AGENT") {
                                                val dutyOn = user.isOnDuty
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (dutyOn) Emerald400.copy(alpha = 0.15f) else Gray400.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = if (dutyOn) "● ON DUTY" else "○ OFF DUTY",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (dutyOn) Emerald400 else Gray400,
                                                        fontSize = 9.sp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                                    )
                                                }
                                            } else if (user.kycStatus == "VERIFIED") {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Emerald400.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "VERIFIED ✓",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Emerald400,
                                                        fontSize = 9.sp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), thickness = 0.7.dp)
                                Spacer(modifier = Modifier.height(12.dp))

                                // Bottom Trust & Status strip
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ProfileMetricChip(
                                        label = if (isFieldAgent) "Inspection Rating".t() else "Trust Score".t(),
                                        value = if (isFieldAgent) "4.9 ★ Elite" else "820 Prime",
                                        color = Gold500,
                                        modifier = Modifier.weight(1f)
                                    )
                                    ProfileMetricChip(
                                        label = "DigiLocker".t(),
                                        value = if (user.aadhaarVerified || user.kycStatus == "VERIFIED") "Verified ✓" else "Pending",
                                        color = if (user.aadhaarVerified || user.kycStatus == "VERIFIED") Emerald400 else Orange400,
                                        modifier = Modifier.weight(1f)
                                    )
                                    ProfileMetricChip(
                                        label = "Account Tier".t(),
                                        value = if (isOwner) "Owner 👑" else if (isFieldAgent) "Officer 🛡️" else "Tier-1 Prime",
                                        color = Emerald400,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Role Upgrade Promotion Banner (Promotes Lender & Certified Agent)
                        if (user != null && user.role.uppercase() != "ADMIN") {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.2.dp, Brush.horizontalGradient(listOf(Gold500, Color(0xFF3B82F6)))),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showRoleUpgradeDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Gold500.copy(alpha = 0.15f),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.TrendingUp, null, tint = Gold500, modifier = Modifier.size(22.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Upgrade User Role",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Gold500.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "PROMOTION",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Gold500,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Become a Capital Provider (Lender) or Certified Field Agent to earn passive returns & commissions.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp,
                                            lineHeight = 14.sp
                                        )
                                    }
                                    Icon(Icons.Default.ChevronRight, null, tint = Gold500, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // ==========================================
                        // ⚡ QUICK ACTIONS RIBBON (Role Adaptive)
                        // ==========================================
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1. Bank & UPI
                            QuickActionTile(
                                icon = Icons.Default.AccountBalance,
                                iconTint = Blue400,
                                title = "Bank & UPI".t(),
                                subtitle = if (user.bankAccountNumber.isNotBlank()) "•••• ${user.bankAccountNumber.takeLast(4)}" else "Link Acct",
                                onClick = { currentSubPage = ProfileSubPage.BANK_ACCOUNTS },
                                modifier = Modifier.weight(1f)
                            )

                            // 2. Doc Vault
                            QuickActionTile(
                                icon = if (isVaultUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                iconTint = if (isVaultUnlocked) Emerald400 else Gold500,
                                title = "Doc Vault".t(),
                                subtitle = if (isVaultUnlocked) "Unlocked" else "Protected",
                                onClick = {
                                    if (isVaultUnlocked) {
                                        currentSubPage = ProfileSubPage.DOCUMENT_VAULT
                                    } else {
                                        showVaultPasswordDialog = true
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            // 3. Security
                            QuickActionTile(
                                icon = Icons.Default.Fingerprint,
                                iconTint = Emerald400,
                                title = "Security".t(),
                                subtitle = "PIN & Bio",
                                onClick = { currentSubPage = ProfileSubPage.PERSONAL_INFO },
                                modifier = Modifier.weight(1f)
                            )

                            // 4. Dynamic Role Tile
                            if (isOwner || user.role.uppercase() == "ADMIN") {
                                QuickActionTile(
                                    icon = Icons.Default.AdminPanelSettings,
                                    iconTint = Color(0xFF6366F1),
                                    title = "Admin Hub".t(),
                                    subtitle = "Master",
                                    onClick = onNavigateToAdminHub,
                                    modifier = Modifier.weight(1f)
                                )
                            } else if (isFieldAgent || user.role.uppercase() == "AGENT") {
                                QuickActionTile(
                                    icon = Icons.Default.Security,
                                    iconTint = Emerald400,
                                    title = "Agent Hub".t(),
                                    subtitle = if (user.isOnDuty) "On Duty" else "Console",
                                    onClick = onNavigateToAgent,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                QuickActionTile(
                                    icon = Icons.Default.VerifiedUser,
                                    iconTint = if (user.kycStatus == "VERIFIED") Emerald400 else Gold500,
                                    title = "KYC Status".t(),
                                    subtitle = if (user.kycStatus == "VERIFIED") "Verified" else "Review",
                                    onClick = onNavigateToKyc,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // ==========================================
                        // 👑 IN-APP ROLE SWITCHER (Strictly Exclusive to @satyam0810)
                        // ==========================================
                        if (canSwitchRoles) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.5.dp, Gold500.copy(alpha = 0.8f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("👑", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "In-App Role Switcher".t(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Gold500.copy(alpha = 0.15f),
                                            border = BorderStroke(0.5.dp, Gold500.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = user.username.ifBlank { "App Owner" }.let { if (it.startsWith("@") || it == "App Owner") it else "@$it" },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Gold500,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // 3 Segmented Role Chips
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val currentRole = user.role.uppercase()
                                        val agentRepository = com.loanzo.app.util.LocalAgentRepository.current

                                        // 1. Normal User
                                        val isNormalSelected = currentRole == "USER" || currentRole == "BORROWER" || currentRole == "LENDER"
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    if (!isNormalSelected) {
                                                        scope.launch {
                                                            userRepository.updateUser(user.copy(role = "USER", isOnDuty = false))
                                                            Toast.makeText(context, "Switched to Member role", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isNormalSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            border = BorderStroke(1.dp, if (isNormalSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(vertical = 10.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text("👤", fontSize = 16.sp)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Member".t(),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isNormalSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }

                                        // 2. Field Agent
                                        val isAgentSelected = currentRole == "AGENT"
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    scope.launch {
                                                        userRepository.updateUser(user.copy(role = "AGENT", agentStatus = "APPROVED", isOnDuty = true))
                                                        Toast.makeText(context, "Switched to Field Agent role", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isAgentSelected) Emerald500 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            border = BorderStroke(1.dp, if (isAgentSelected) Emerald500 else MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(vertical = 10.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text("🕵️", fontSize = 16.sp)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Field Agent".t(),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isAgentSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }

                                        // 3. Master Admin
                                        val isAdminSelected = currentRole == "ADMIN"
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    scope.launch {
                                                        userRepository.updateUser(user.copy(role = "ADMIN"))
                                                        Toast.makeText(context, "Switched to Master Admin role", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isAdminSelected) Color(0xFF6366F1) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            border = BorderStroke(1.dp, if (isAdminSelected) Color(0xFF6366F1) else MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(vertical = 10.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text("🛡️", fontSize = 16.sp)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Master Admin".t(),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isAdminSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // App Preferences & Display Card
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                ProfileActionRow(
                                    icon = Icons.Default.Palette,
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    title = "App Preferences & Display",
                                    onClick = { currentSubPage = ProfileSubPage.PREFERENCES }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Legal & Compliance Card
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                ProfileActionRow(
                                    icon = Icons.Default.Description,
                                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    title = "Terms, Compliance & Legal",
                                    onClick = { currentSubPage = ProfileSubPage.TERMS_AND_CONDITIONS }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // About Us Card
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                ProfileActionRow(
                                    icon = Icons.Default.Info,
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    title = "About Loanzo",
                                    onClick = { currentSubPage = ProfileSubPage.ABOUT_US }
                                )
                            }
                        }

                        // Administrative Controls
                        if (isOwner || canSwitchRoles) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ProfileActionRow(
                                    icon = Icons.Default.AdminPanelSettings,
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    title = "App Owner Control Center",
                                    onClick = onNavigateToAdminHub
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))
                    }

                    if (!profileGuideSeen) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            ContextualGuideCard(
                                visible = true,
                                icon = Icons.Default.Person,
                                title = "Your Profile & Security Hub",
                                body = "Manage your identity, personal info, encrypted document vault, bank details, and app preferences here.",
                                onDismiss = {
                                    scope.launch {
                                        userRepository.markGuideSeen(com.loanzo.app.data.repository.UserRepository.GUIDE_PROFILE_SEEN)
                                    }
                                },
                                autoDismissSeconds = 8
                            )
                        }
                    }

                    }
                }
            }

            ProfileSubPage.DOCUMENT_VAULT -> {
                // ==========================================
                // 2. ENCRYPTED DOCUMENT VAULT SUB-PAGE
                // ==========================================
                val dateFormat = remember { java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text("Encrypted Document Vault", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    Text("Official Certifications, Dossiers & Legal Archives", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { currentSubPage = ProfileSubPage.MAIN }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                            actions = {
                                TextButton(onClick = {
                                    isVaultUnlocked = false
                                    currentSubPage = ProfileSubPage.MAIN
                                    Toast.makeText(context, "Document Vault Locked", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Gold500, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Lock Vault", color = Gold500, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Hardware Encryption & Security Banner
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Emerald400.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.VerifiedUser, null, tint = Emerald400, modifier = Modifier.size(26.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("AES-256 Vault Session Active", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                    Text("Cryptographically sealed with SHA-256 tamper-evident checksums. All documents carry official Loanzo institution branding.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                }
                            }
                        }

                        // 2. Master Dossier Generator Card (OpenPDF Engine with App Logo)
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.5.dp, Gold500.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Gold500.copy(alpha = 0.15f),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Gold500, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Master Financial & Legal Dossier",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "Official Loanzo crested PDF documenting your KYC, CIBIL rating, loans, repayments & custody.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        if (user != null) {
                                            vaultViewModel.generateMasterDossier(user.userId)
                                        }
                                    },
                                    enabled = !vaultState.isGenerating,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Navy900),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (vaultState.isGenerating) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Navy900, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Generating Certified Dossier...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Generate Complete Financial Dossier (PDF)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        // Message Banner if any
                        vaultState.message?.let { msg ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (vaultState.isSuccess) Emerald400.copy(alpha = 0.15f) else Gold500.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, if (vaultState.isSuccess) Emerald400 else Gold500),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    IconButton(onClick = { vaultViewModel.clearMessage() }, modifier = Modifier.size(20.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        // 3. Section: Archived Vault Documents
                        ProfileSectionHeader(title = "ARCHIVED VAULT DOCUMENTS (${vaultState.documents.size})")

                        if (vaultState.documents.isEmpty()) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("No Documents Vaulted Yet", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Tap 'Generate Complete Financial Dossier' above, or sign digital loan agreements to store certified copies in your vault.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            for (doc in vaultState.documents) {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val badgeColor = when (doc.documentType) {
                                                "FINANCIAL_DOSSIER" -> Gold500
                                                "LOAN_AGREEMENT" -> Emerald400
                                                "NOC_CERTIFICATE" -> Blue400
                                                else -> MaterialTheme.colorScheme.primary
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = badgeColor.copy(alpha = 0.15f),
                                                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
                                            ) {
                                                Text(
                                                    text = doc.documentType.replace("_", " "),
                                                    color = badgeColor,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.weight(1f))

                                            Text(
                                                text = dateFormat.format(java.util.Date(doc.generatedAt)),
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = doc.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        if (doc.description.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = doc.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Meta row: size + SHA256
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${(doc.fileSizeBytes / 1024).coerceAtLeast(1)} KB",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald400, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "SHA-256: ${doc.checksumSha256.take(12)}...",
                                                fontSize = 10.sp,
                                                color = Emerald400,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.6.dp)
                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Action buttons
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = { vaultViewModel.deleteDocument(doc.documentId) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            OutlinedButton(
                                                onClick = { vaultViewModel.shareDocument(context, doc) },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Share", fontSize = 11.sp)
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Button(
                                                onClick = { vaultViewModel.openDocument(context, doc) },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("View PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // 4. Section: Statutory KYC & Identity Documents
                        ProfileSectionHeader(title = "STATUTORY KYC & IDENTITY VERIFICATION")

                        // PAN Card
                        VaultDocumentCard(
                            documentType = "PAN Card",
                            documentNumber = user.panNumber.ifBlank { "PAN on file" },
                            isVerified = user.panVerified || user.kycStatus == "VERIFIED",
                            viewUrl = user.panImageUrl,
                            isUploading = isUploadingPan,
                            onUploadClick = { panPdfLauncher.launch("*/*") }
                        )

                        // Aadhaar Card
                        VaultDocumentCard(
                            documentType = "Aadhaar Card",
                            documentNumber = user.aadhaarNumber.ifBlank { "Aadhaar on file" },
                            isVerified = user.aadhaarVerified || user.kycStatus == "VERIFIED",
                            viewUrl = user.aadhaarImageUrl,
                            isUploading = isUploadingAadhaar,
                            onUploadClick = { aadhaarPdfLauncher.launch("*/*") }
                        )

                        // DigiLocker Gov Status
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Emerald400.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = Emerald400, modifier = Modifier.padding(8.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("DigiLocker Government KYC", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Direct API integration verified with official UIDAI/ITD records", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Emerald400.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.3f))
                                ) {
                                    Text("VALID", color = Emerald400, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }
                        }

                        // Liveness Selfie
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Gold500.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Gold500, modifier = Modifier.padding(8.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("CameraX ML Kit Liveness", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Real-time facial geometry & active liveness verified", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Emerald400.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.3f))
                                ) {
                                    Text("VERIFIED ✓", color = Emerald400, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }
                        }
                    }
                }
            }

            ProfileSubPage.PERSONAL_INFO -> {
                // ==========================================
                // 3. PERSONAL INFORMATION SUB-PAGE
                // ==========================================
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Personal Information", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { currentSubPage = ProfileSubPage.MAIN }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Contact & Communication", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(10.dp))
                            ProfileRow(
                                icon = Icons.Default.Email,
                                label = "Email Address",
                                value = if (user.email.isNotBlank()) user.email + if (user.emailVerified) " (Verified ✓)" else "" else "Not provided"
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.8.dp)
                            ProfileRow(
                                icon = Icons.Default.Phone,
                                label = "Phone Number",
                                value = if (user.phone.isNotBlank()) user.phone + if (user.phoneVerified) " (Verified ✓)" else "" else "Not provided"
                            )
                        }

                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Demographics & Residence", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(10.dp))
                            ProfileRow(
                                icon = Icons.Default.Cake,
                                label = "Date of Birth",
                                value = user.dateOfBirth.ifBlank { "Not specified" }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.8.dp)
                            ProfileRow(
                                icon = Icons.Default.Home,
                                label = "Permanent Address",
                                value = user.address.ifBlank { "Not provided" }
                            )
                        }

                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Account Security & Role", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(10.dp))
                            ProfileRow(
                                icon = Icons.Default.Person,
                                label = "User Role",
                                value = user.role.uppercase()
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.8.dp)
                            ProfileRow(
                                icon = Icons.Default.Fingerprint,
                                label = "Biometric Authentication",
                                value = if (user.selfieVerified) "Enrolled & Active ✓" else "Setup Available"
                            )
                        }

                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Government KYC & Identity", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(10.dp))
                            ProfileRow(
                                icon = Icons.Default.VerifiedUser,
                                label = "KYC Status",
                                value = when (user.kycStatus) {
                                    "VERIFIED" -> "Fully Verified ✓"
                                    "REJECTED" -> "Action Required / Rejected ⚠️"
                                    "PENDING", "IN_PROGRESS" -> "Verification Pending ⏳"
                                    else -> "Not Verified"
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.8.dp)
                            ProfileRow(
                                icon = Icons.Default.CreditCard,
                                label = "PAN Status",
                                value = if (user.panVerified) "Verified ITD ✓" else if (user.panNumber.isNotBlank()) "Recorded" else "Not Linked"
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.8.dp)
                            ProfileRow(
                                icon = Icons.Default.Fingerprint,
                                label = "Aadhaar Status",
                                value = if (user.aadhaarVerified) "Verified UIDAI ✓" else if (user.aadhaarNumber.isNotBlank()) "Recorded" else "Not Linked"
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onNavigateToKyc,
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (user.kycStatus == "REJECTED") Red400 else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.PublishedWithChanges, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (user.kycStatus == "VERIFIED") "Update / Re-KYC Credentials" else if (user.kycStatus == "REJECTED") "Fix & Re-submit KYC Now" else "Complete Identity KYC",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            ProfileSubPage.BANK_ACCOUNTS -> {
                // ==========================================
                // 4. BANK & PAYOUT ACCOUNTS SUB-PAGE
                // ==========================================
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Bank & Payout Accounts", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { currentSubPage = ProfileSubPage.MAIN }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                            actions = {
                                TextButton(onClick = { showBankDialog = true }) {
                                    Text(if (user.bankAccountNumber.isNotBlank()) "Edit" else "Add", color = Gold500, fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Executive Obsidian Bank Card
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Gray900),
                            border = BorderStroke(1.dp, Gold500.copy(alpha = 0.35f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("PRIMARY DISBURSEMENT ACCOUNT", style = MaterialTheme.typography.labelSmall, color = Gold500, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (user.bankVerified) Emerald400.copy(alpha = 0.15f) else Gold500.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, if (user.bankVerified) Emerald400.copy(alpha = 0.3f) else Gold500.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = if (user.bankVerified) "VERIFIED ✓" else "₹1 PENNY DROP PENDING",
                                            color = if (user.bankVerified) Emerald400 else Gold500,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                if (user.bankAccountNumber.isNotBlank()) {
                                    Text(
                                        text = "•••• •••• •••• ${user.bankAccountNumber.takeLast(4)}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        letterSpacing = 2.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Text("IFSC CODE", style = MaterialTheme.typography.labelSmall, color = Gray400, fontSize = 10.sp)
                                            Text(user.bankIfsc.ifBlank { "N/A" }, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("SETTLEMENT TYPE", style = MaterialTheme.typography.labelSmall, color = Gray400, fontSize = 10.sp)
                                            Text("IMPS / Direct UPI", style = MaterialTheme.typography.bodyMedium, color = Emerald400, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "No Bank Account Linked",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Add your bank account to receive peer-to-peer disbursements and automated repayments.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Gray400,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { showBankDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                        ) {
                            Icon(Icons.Default.AccountBalance, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (user.bankAccountNumber.isNotBlank()) "Update Bank Details" else "Link Bank Account", fontWeight = FontWeight.Bold)
                        }

                        // Settlement Security Info
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, null, tint = Emerald400, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Bank-Grade Settlement Guarantee", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Payouts are transferred directly into this account via real-time IMPS. Before any loan disbursement is released, Loanzo executes an automated ₹1 penny-drop validation to confirm name matching.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            ProfileSubPage.PREFERENCES -> {
                // ==========================================
                // 5. APP PREFERENCES SUB-PAGE
                // ==========================================
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("App Preferences", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { currentSubPage = ProfileSubPage.MAIN }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Appearance
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.appearance), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(12.dp))
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                val systemLabel = stringResource(R.string.theme_system)
                                val lightLabel = stringResource(R.string.theme_light)
                                val darkLabel = stringResource(R.string.theme_dark)
                                val options = listOf("LIGHT" to "$lightLabel (Default)", "DARK" to darkLabel)
                                options.forEachIndexed { index, (key, label) ->
                                    SegmentedButton(
                                        selected = themeMode == key,
                                        onClick = { onSetThemeMode(key) },
                                        shape = SegmentedButtonDefaults.itemShape(index, options.size)
                                    ) {
                                        Text(label, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Language Selection
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.app_language), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                    .clickable { showLanguageSheet = true }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = com.loanzo.app.ui.components.getLanguageNameByCode(currentLanguageCode),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.ChevronRight, null, tint = Gray500, modifier = Modifier.size(20.dp))
                            }
                        }

                        // Floating AI Assistant Bubble Toggle
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Floating AI Assistant", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Display quick-access floating AI Assistant bubble on screen for instant answers and financial guidance.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Switch(
                                    checked = isFloatingBotEnabled,
                                    onCheckedChange = { enabled ->
                                        scope.launch { userRepository.setFloatingChatbotEnabled(enabled) }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Emerald500
                                    )
                                )
                            }
                        }

                        // Telegram Assistant Bot
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Telegram Bot Notifications", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Receive instant real-time EMI reminders, disbursal alerts, and loan status updates via our Telegram Bot (@Loanzo_bot).",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                            if (user.telegramUsername.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Emerald500.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, null, tint = Emerald500, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Linked: @${user.telegramUsername}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Emerald500
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = "Edit",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable { showRegisterTelegramDialog = true }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (user.telegramUsername.isBlank()) {
                                        showRegisterTelegramDialog = true
                                    } else {
                                        com.loanzo.app.util.TelegramManager.instance.openBotForLinking(context, user.userId)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Color.White)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (user.telegramUsername.isBlank()) "Register & Open @Loanzo_bot" else "Open @Loanzo_bot on Telegram",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            ProfileSubPage.TERMS_AND_CONDITIONS -> {
                // ==========================================
                // 6. TERMS & CONDITIONS SUB-PAGE
                // ==========================================
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Terms, Compliance & Legal", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { currentSubPage = ProfileSubPage.MAIN }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Compliance Header
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Gold500.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Gold500.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Gavel, null, tint = Gold500, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Legally Enforceable P2P Agreement", fontWeight = FontWeight.Bold, color = Gold500, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Version 2026.9 • Fully aligned with Reserve Bank of India (RBI) NBFC-P2P Directives, Information Technology Act 2000, and DPDP Act 2023.",
                                    color = Gray300,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        LegalClauseCard(
                            clauseNumber = "1",
                            title = "Decentralized P2P Marketplace Facilitation",
                            content = "Loanzo operates strictly as a financial technology platform and peer-to-peer marketplace facilitating direct bilateral loan contracts between verified participants. Loanzo is not a bank or deposit-taking entity, does not accept public deposits, and does not provide principal guarantees. All transactions represent direct legal contracts between the Capital Provider (Lender) and Borrower."
                        )

                        LegalClauseCard(
                            clauseNumber = "2",
                            title = "Digital KYC & PMLA Verification",
                            content = "Users explicitly consent to the verification of PAN card, Aadhaar records via DigiLocker, and real-time facial liveness. Financial credentials are held in an AES-256 encrypted Document Vault with zero-knowledge derivation. Any fraudulent, doctored, or falsified KYC submission constitutes an offense under the Prevention of Money Laundering Act (PMLA) and will result in immediate account freeze and law enforcement escalation."
                        )

                        LegalClauseCard(
                            clauseNumber = "3",
                            title = "Dual-Party eSign & Contract Legality",
                            content = "Loan agreements executed on Loanzo are legally binding and enforceable in courts of law under Section 10A of the Indian Information Technology Act, 2000. Agreements are sealed with dual-party digital signatures, CameraX ML Kit biometric authentication timestamps, and immutable SHA-256 audit hashes."
                        )

                        LegalClauseCard(
                            clauseNumber = "4",
                            title = "Disbursements, Penny Drop & Payouts",
                            content = "Loan capital is disbursed directly from Lender to Borrower or designated third-party merchants via IMPS/UPI. Before any disbursement, Loanzo conducts an automated ₹1 penny-drop bank verification to ensure recipient account authenticity."
                        )

                        LegalClauseCard(
                            clauseNumber = "5",
                            title = "RBI Fair Practices Code & Penalty Capping",
                            content = "Borrowers agree to honor the repayment schedule and agreed interest rate. Overdue repayments incur penalties strictly calculated by Loanzo's Compound Penalty Engine, capped in compliance with RBI fair practice codes. Usurious compounding and hidden processing deductions are strictly prohibited."
                        )

                        LegalClauseCard(
                            clauseNumber = "6",
                            title = "Anti-Harassment & Recovery Code of Conduct",
                            content = "Loanzo enforces a zero-tolerance policy against borrower harassment. In accordance with RBI Fair Practices Code, recovery agents and lenders may only initiate communications between 8:00 AM and 7:00 PM. Threatening language, workplace intimidation, or contacting third parties without consent will result in immediate permanent banning and police prosecution. Borrowers have 24/7 access to the Anti-Harassment SOS Desk."
                        )

                        LegalClauseCard(
                            clauseNumber = "7",
                            title = "Digital Personal Data Protection (DPDP) Act 2023",
                            content = "Your sensitive identification records are stored in a password-protected Document Vault with zero-knowledge protocols. Under the DPDP Act 2023, you retain the right to access your stored data, request rectification, or withdraw non-statutory processing consent upon complete loan settlement."
                        )

                        LegalClauseCard(
                            clauseNumber = "8",
                            title = "Dispute Redressal & Legal Escalation",
                            content = "Users agree to resolve disputes amicably through the built-in Dispute Center. In cases of persistent wilful default or fraudulent evasion, the counterparty reserves full rights to initiate formal legal recovery proceedings under Section 138 of the Negotiable Instruments Act and the Indian Contract Act, 1872."
                        )

                        LegalClauseCard(
                            clauseNumber = "9",
                            title = "Statutory Disclosures & Grievance Redressal",
                            content = "For escalations, dispute arbitration, or legal notices, contact our Nodal Grievance Officer: Satyam Kumar (grievance@loanzo.in). Grievances are formally acknowledged within 24 hours and addressed within 48 business hours pursuant to RBI grievance redressal directives."
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            ProfileSubPage.ABOUT_US -> {
                // ==========================================
                // 7. ABOUT US SUB-PAGE
                // ==========================================
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("About Loanzo", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { currentSubPage = ProfileSubPage.MAIN }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Brand Hero Card
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, Gold500.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Navy900,
                                    border = BorderStroke(2.dp, Gold500),
                                    modifier = Modifier.size(68.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.AccountBalance, null, tint = Gold500, modifier = Modifier.size(36.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text("LOANZO", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Gold500, letterSpacing = 2.sp)
                                Text("Direct Peer-to-Peer Financial Inclusion", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Loanzo empowers communities across India with transparent, direct lending. By replacing predatory informal loan sharks with verifiable peer trust networks and legally binding contracts, we ensure credit is accessible, fair, and secure.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Core Architecture & Security Pillars
                        Text("Security & Regulatory Pillars", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                        AboutPillarCard(
                            icon = Icons.Default.Shield,
                            iconTint = Gold500,
                            title = "Zero-Knowledge Encryption",
                            description = "Sensitive documents and biometric signatures in the Document Vault are safeguarded with AES-256-GCM encryption."
                        )

                        AboutPillarCard(
                            icon = Icons.Default.Badge,
                            iconTint = Emerald400,
                            title = "Direct DigiLocker & PAN KYC",
                            description = "Real-time government API integrations with DigiLocker and PAN ensure every counterparty is verified with facial liveness."
                        )

                        AboutPillarCard(
                            icon = Icons.Default.Gavel,
                            iconTint = Color(0xFF3B82F6),
                            title = "Legally Enforceable e-Contracts",
                            description = "Every peer loan is backed by a Section 10A IT Act 2000 digital agreement signed with biometric audit trails."
                        )

                        AboutPillarCard(
                            icon = Icons.Default.VerifiedUser,
                            iconTint = Color(0xFF8B5CF6),
                            title = "RBI P2P Regulatory Alignment",
                            description = "Engineered pursuant to Reserve Bank of India (RBI) NBFC-P2P platform guidelines and Fair Practices Code."
                        )

                        // Nodal Grievance Office
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Grievance Redressal & Support", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("Nodal Officer: Satyam Kumar\nEmail: grievance@loanzo.in • support@loanzo.app\nTelegram Bot: @Loanzo_bot\nHeadquarters: Sector 120, Noida, Uttar Pradesh 201301", fontSize = 11.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Footer
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text("Loanzo v2.4.0 (Build 42) • Made with ❤️ in India", style = MaterialTheme.typography.labelSmall, color = Gray400)
                        }
                    }
                }
            }
        }
    }

    // ================= DIALOGS & MODALS =================

    // 0. Role Upgrade Promotion Dialog
    if (showRoleUpgradeDialog && user != null) {
        RoleUpgradePromotionDialog(
            currentRole = user.role,
            onDismiss = { showRoleUpgradeDialog = false },
            onUpgradeToLender = {
                showRoleUpgradeDialog = false
                scope.launch {
                    userRepository.updateUser(user.copy(role = "LENDER"))
                    Toast.makeText(context, "Role updated to Capital Provider (Lender)!", Toast.LENGTH_LONG).show()
                }
            },
            onApplyAsAgent = {
                showRoleUpgradeDialog = false
                onNavigateToAgent()
            }
        )
    }

    // 0.1 Register Telegram Username Dialog
    if (showRegisterTelegramDialog && user != null) {
        RegisterTelegramDialog(
            initialUsername = user.telegramUsername,
            onDismiss = { showRegisterTelegramDialog = false },
            onSaveAndOpen = { cleanUsername ->
                showRegisterTelegramDialog = false
                scope.launch {
                    userRepository.updateTelegramUsername(user.userId, cleanUsername)
                    Toast.makeText(context, "Registered @$cleanUsername! Opening Telegram...", Toast.LENGTH_SHORT).show()
                    com.loanzo.app.util.TelegramManager.instance.openBotForLinking(context, user.userId)
                }
            }
        )
    }

    // 1. Password Verification Dialog for Document Vault
    if (showPermissionsSheet) {
        com.loanzo.app.ui.components.RequiredPermissionsDialog(
            onDismiss = { showPermissionsSheet = false },
            onAllGranted = { showPermissionsSheet = false }
        )
    }

    if (showVaultPasswordDialog) {
        UnlockVaultDialog(
            user = user,
            onDismiss = { showVaultPasswordDialog = false },
            onUnlocked = {
                isVaultUnlocked = true
                showVaultPasswordDialog = false
                currentSubPage = ProfileSubPage.DOCUMENT_VAULT
                Toast.makeText(context, "Document Vault Unlocked", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 2. Bank Details Edit Dialog
    if (showBankDialog) {
        var inputAccNum by remember { mutableStateOf(user.bankAccountNumber) }
        var inputIfsc by remember { mutableStateOf(user.bankIfsc) }

        AlertDialog(
            onDismissRequest = { showBankDialog = false },
            title = { Text("Bank Account Details", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Enter your payout account details carefully. We will verify this account by sending ₹1 via IMPS/NEFT.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = inputAccNum,
                        onValueChange = { inputAccNum = it.filter { char -> char.isDigit() } },
                        label = { Text("Account Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputIfsc,
                        onValueChange = { inputIfsc = it.uppercase() },
                        label = { Text("IFSC Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputAccNum.isNotBlank() && inputIfsc.isNotBlank()) {
                            onUpdateBankDetails(inputAccNum, inputIfsc)
                            showBankDialog = false
                        }
                    }
                ) {
                    Text("Save & Verify")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBankDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 3. Logout Confirmation Dialog
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text("Confirm Logout", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to sign out of your Loanzo account? You will need to verify your credentials to log in again.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red400, contentColor = Color.White)
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }



    // 4. Language Selection Sheet
    if (showLanguageSheet) {
        com.loanzo.app.ui.components.LanguageSelectionBottomSheet(
            currentLanguageCode = currentLanguageCode,
            onLanguageSelected = { newLang ->
                onSelectLanguage(newLang)
                showLanguageSheet = false
                val langName = com.loanzo.app.ui.components.getLanguageNameByCode(newLang).substringBefore(" (")
                Toast.makeText(context, "$langName language selected. Internal translation active.", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showLanguageSheet = false }
        )
    }
}

/**
 * Modern Security Dialog to verify user password (or biometrics) before unlocking Document Vault
 */
@Composable
private fun UnlockVaultDialog(
    user: UserEntity,
    onDismiss: () -> Unit,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!isVerifying) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Gold500.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Gold500,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Unlock Document Vault",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Enter your Loanzo account password to access your encrypted KYC documents.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Red400.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, null, tint = Red400, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = Red400,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = {
                        passwordInput = it
                        errorMessage = null
                    },
                    label = { Text("Account Password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = Gray400
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (passwordInput.isBlank()) {
                            errorMessage = "Please enter your password"
                            return@Button
                        }
                        isVerifying = true
                        val hashed = hashPassword(passwordInput.trim())
                        val legacyHashed = hashPassword(passwordInput)

                        if (user.password.isBlank() || user.password == hashed || user.password == legacyHashed) {
                            isVerifying = false
                            onUnlocked()
                        } else {
                            isVerifying = false
                            errorMessage = "Incorrect password. Please try again."
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Navy900),
                    enabled = !isVerifying
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(color = Navy900, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Key, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verify & Unlock", fontWeight = FontWeight.Bold)
                    }
                }

                // Optional Biometric Quick Unlock
                val activity = remember(context) { BiometricAuthManager.getActivity(context) }
                if (activity != null && BiometricAuthManager.isBiometricAvailable(context)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            BiometricAuthManager.authenticate(
                                activity = activity,
                                title = "Unlock Document Vault",
                                subtitle = "Scan fingerprint or face to view KYC documents",
                                onSuccess = { onUnlocked() },
                                onError = { err -> errorMessage = err }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Emerald400),
                        border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Fingerprint, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unlock with Biometrics", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel", color = Gray400)
                }
            }
        }
    }
}

@Composable
private fun ProfileActionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String? = null,
    statusBadge: String? = null,
    badgeColor: Color = Gold500,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = iconTint.copy(alpha = 0.14f),
            modifier = Modifier.size(38.dp)
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.padding(9.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
        }
        if (statusBadge != null) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = badgeColor.copy(alpha = 0.15f),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(statusBadge, color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ProfileSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
    )
}

@Composable
private fun VaultDocumentCard(
    documentType: String,
    documentNumber: String,
    isVerified: Boolean,
    viewUrl: String?,
    isUploading: Boolean,
    onUploadClick: () -> Unit
) {
    val context = LocalContext.current
    val isUploaded = !viewUrl.isNullOrBlank()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(7.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(documentType, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                        Text(documentNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isVerified) Emerald400.copy(alpha = 0.15f) else Gold500.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isVerified) "Verified ✓" else "Under Review",
                        color = if (isVerified) Emerald400 else Gold500,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isUploaded && !isUploading) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(viewUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open cloud document", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp).padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Visibility, null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Cloud Doc", fontSize = 11.sp)
                    }
                }

                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                } else {
                    Button(
                        onClick = onUploadClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) {
                        Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isUploaded) "Update" else "Upload", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LegalClauseCard(
    clauseNumber: String,
    title: String,
    content: String
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(clauseNumber, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun ProfileMetricChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 11.5.sp,
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.12f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ProfileRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            modifier = Modifier.size(34.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
        }
    }
}

@Composable
fun RoleUpgradePromotionDialog(
    currentRole: String,
    onDismiss: () -> Unit,
    onUpgradeToLender: () -> Unit,
    onApplyAsAgent: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                border = BorderStroke(1.2.dp, Gold500.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Gold500.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Gold500.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "ROLE UPGRADE PROMOTION",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Gold500,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                        }
                    }

                    Text(
                        text = "Unlock Higher Financial Capabilities",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Elevate your account from borrower to capital provider or certified field partner to earn returns and commissions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp
                    )

                    // Card 1: Capital Provider (Lender)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, Gold500.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Gold500.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.AccountBalance, null, tint = Gold500, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Capital Provider (Lender)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Earn 12% - 24% p.a. on P2P Loans", fontSize = 11.sp, color = Gold500, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "• Direct bilateral contracts under Section 10A IT Act\n• Automated ₹1 penny-drop borrower bank verification\n• Diversify across multiple vetted borrower risk tiers",
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onUpgradeToLender,
                                modifier = Modifier.fillMaxWidth().height(38.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Navy900),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(if (currentRole.equals("LENDER", ignoreCase = true)) "Active Role ✓" else "Switch to Capital Provider", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    // Card 2: Certified Field Agent
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Emerald400.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Badge, null, tint = Emerald400, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Certified Field Agent", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Earn ₹250 - ₹500 per Field Verification", fontSize = 11.sp, color = Emerald400, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "• Conduct physical borrower KYC and doorstep verification\n• On-Duty/Off-Duty status toggle with live GPS radar\n• Weekly instant payouts to your registered UPI ID",
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onApplyAsAgent,
                                modifier = Modifier.fillMaxWidth().height(38.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald400, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(if (currentRole.equals("AGENT", ignoreCase = true)) "Agent Active ✓" else "Apply as Field Agent", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RegisterTelegramDialog(
    initialUsername: String,
    onDismiss: () -> Unit,
    onSaveAndOpen: (String) -> Unit
) {
    var inputUsername by remember { mutableStateOf(initialUsername.removePrefix("@")) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.AutoMirrored.Filled.Send, null, tint = Emerald500, modifier = Modifier.size(32.dp))
        },
        title = {
            Text("Register Telegram Username", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "To enable our 24/7 Telegram Assistant (@Loanzo_bot) to recognize your account and send instant alerts, please enter your Telegram username.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
                OutlinedTextField(
                    value = inputUsername,
                    onValueChange = {
                        inputUsername = it.trim().removePrefix("@")
                        errorMsg = null
                    },
                    label = { Text("Telegram Username") },
                    prefix = { Text("@", fontWeight = FontWeight.Bold, color = Emerald500) },
                    singleLine = true,
                    isError = errorMsg != null,
                    supportingText = errorMsg?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = inputUsername.trim().removePrefix("@")
                    if (clean.length < 4) {
                        errorMsg = "Username must be at least 4 characters"
                        return@Button
                    }
                    onSaveAndOpen(clean)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Register & Open Bot", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AboutPillarCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(2.dp))
                Text(description, fontSize = 11.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

