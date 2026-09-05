package com.loanzo.app.ui.auth

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.loanzo.app.ui.components.LoanzoAcademySimulatorSheet
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
    TERMS_AND_CONDITIONS
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
    onBack: () -> Unit
) {
    var currentSubPage by remember { mutableStateOf(ProfileSubPage.MAIN) }
    var showBankDialog by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showDemoDataDialog by remember { mutableStateOf(false) }
    var isSeedingDemo by remember { mutableStateOf(false) }
    var isVaultUnlocked by remember { mutableStateOf(false) }
    var showVaultPasswordDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val userRepository = com.loanzo.app.util.LocalUserRepository.current
    val profileGuideSeen by userRepository.isGuideSeen(com.loanzo.app.data.repository.UserRepository.GUIDE_PROFILE_SEEN)
        .collectAsStateWithLifecycle(initialValue = true)
    val scope = rememberCoroutineScope()
    var isGuideMeExpanded by remember { mutableStateOf(false) }
    var showAcademySheet by remember { mutableStateOf(false) }
    val guideChevronRotation by animateFloatAsState(
        targetValue = if (isGuideMeExpanded) 180f else 0f,
        label = "guide_chevron"
    )

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

    // Role Switcher Simulator is STRICTLY EXCLUSIVE to username satyam0810
    val canSwitchRoles = remember(user.username, user.phone) {
        val u = user.username.trim().lowercase()
        u == "satyam0810" || u == "satyam_081" || u == "satyam" || user.phone == "+917061559039" || user.phone == "7061559039"
    }

    val profilePhotoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val targetFile = java.io.File(context.filesDir, "profile_${user.userId}.jpg")
                    targetFile.outputStream().use { output ->
                        inputStream?.copyTo(output)
                    }
                    val localPhotoUri = "file://${targetFile.absolutePath}"
                    val updatedUser = user.copy(profilePhotoUri = localPhotoUri)
                    userRepository.updateUser(updatedUser)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        Toast.makeText(context, "Profile photo updated successfully!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        Toast.makeText(context, "Failed to save photo: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
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
                        // Compact Executive Hero Card
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LoanzoAvatar(
                                    user = user,
                                    size = 94.dp,
                                    showVerifiedBadge = true,
                                    showEditBadge = true,
                                    borderColor = if (isOwner) Gold500 else MaterialTheme.colorScheme.primary,
                                    borderWidth = 2.5.dp,
                                    onClick = { profilePhotoPickerLauncher.launch("image/*") },
                                    onEditClick = { profilePhotoPickerLauncher.launch("image/*") }
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Tap to change photo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = user.name.ifBlank { "Loanzo Member" },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (user.username.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Text(
                                            text = "@${user.username}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isOwner) Gold500.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = if (isOwner) "👑 APP OWNER / MASTER ADMIN" else "VERIFIED ${user.role.uppercase()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOwner) Gold500 else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // 3-Metric Trust Chips
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ProfileMetricChip(
                                        label = "Trust Score",
                                        value = "820 Prime",
                                        color = Gold500,
                                        modifier = Modifier.weight(1f)
                                    )
                                    ProfileMetricChip(
                                        label = "DigiLocker",
                                        value = if (user.aadhaarVerified || user.kycStatus == "VERIFIED") "Verified ✓" else "Pending",
                                        color = if (user.aadhaarVerified || user.kycStatus == "VERIFIED") Emerald400 else Orange400,
                                        modifier = Modifier.weight(1f)
                                    )
                                    ProfileMetricChip(
                                        label = "Vault",
                                        value = if (isVaultUnlocked) "Unlocked 🔓" else "Locked 🔒",
                                        color = if (isVaultUnlocked) Emerald400 else Blue400,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
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
                                                text = "In-App Role Switcher",
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
                                                text = "@${user.username.ifBlank { "satyam0810" }}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Gold500,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Switch active role seamlessly. Core navigation (Home, Loans, Alerts, Profile) stays unified while features dynamically adapt.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))

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
                                                            userRepository.updateUser(user.copy(role = "USER"))
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
                                                    text = "Member",
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
                                                        agentRepository.seedSampleVisits(user.userId)
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
                                                    text = "Agent",
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
                                                    text = "Admin",
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

                        Spacer(modifier = Modifier.height(18.dp))

                        // Loanzo Field Agent Program Tile
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Gold500.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onNavigateToAgent() }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Gold500.copy(alpha = 0.15f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = Gold500, modifier = Modifier.size(24.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Field Agent Program",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val agentBadge = when (user.agentStatus) {
                                            "APPROVED" -> "CERTIFIED ✓"
                                            "PENDING" -> "IN REVIEW"
                                            else -> "EARN ₹1500"
                                        }
                                        val badgeTint = when (user.agentStatus) {
                                            "APPROVED" -> Emerald400
                                            "PENDING" -> Gold500
                                            else -> Color(0xFF38BDF8)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = badgeTint.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = agentBadge,
                                                color = badgeTint,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = when (user.agentStatus) {
                                            "APPROVED" -> "Certified Officer • Open Agent Console"
                                            "PENDING" -> "Under background check • Check status"
                                            else -> "Empanel as verification officer & earn per visit"
                                        },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Gray400, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // ================= GROUP 1: IDENTITY & LEGAL =================
                        ProfileSectionHeader(title = "IDENTITY & CREDENTIALS")
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                ProfileActionRow(
                                    icon = Icons.Default.Badge,
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    title = "Personal & Identity Details",
                                    subtitle = user.email.ifBlank { user.phone.ifBlank { "Contact & verification details" } },
                                    onClick = { currentSubPage = ProfileSubPage.PERSONAL_INFO }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.8.dp)
                                ProfileActionRow(
                                    icon = if (isVaultUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                    iconTint = if (isVaultUnlocked) Emerald400 else MaterialTheme.colorScheme.primary,
                                    title = "Encrypted Document Vault",
                                    subtitle = if (isVaultUnlocked) "Session active • Tap to view documents" else "PAN, Aadhaar & Gov KYC • Password protected",
                                    statusBadge = if (isVaultUnlocked) "UNLOCKED" else "LOCKED",
                                    badgeColor = if (isVaultUnlocked) Emerald400 else MaterialTheme.colorScheme.primary,
                                    onClick = {
                                        if (isVaultUnlocked) {
                                            currentSubPage = ProfileSubPage.DOCUMENT_VAULT
                                        } else {
                                            showVaultPasswordDialog = true
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // ================= GROUP 2: FINANCES & BANKING =================
                        ProfileSectionHeader(title = "FINANCES & PAYOUTS")
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                ProfileActionRow(
                                    icon = Icons.Default.AccountBalance,
                                    iconTint = Blue400,
                                    title = "Bank & Payout Accounts",
                                    subtitle = if (user.bankAccountNumber.isNotBlank()) "•••• ${user.bankAccountNumber.takeLast(4)} • Verified Payout Account" else "No account linked • Tap to add",
                                    statusBadge = if (user.bankVerified) "VERIFIED ✓" else if (user.bankAccountNumber.isNotBlank()) "PENDING" else "ADD",
                                    badgeColor = if (user.bankVerified) Emerald400 else MaterialTheme.colorScheme.primary,
                                    onClick = { currentSubPage = ProfileSubPage.BANK_ACCOUNTS }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // ================= GROUP 3: PREFERENCES & CONNECTIVITY =================
                        ProfileSectionHeader(title = "PREFERENCES & CONNECTED SERVICES")
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
                                    subtitle = "Theme: ${themeMode.lowercase().replaceFirstChar { it.uppercase() }} • Lang: ${com.loanzo.app.ui.components.getLanguageNameByCode(currentLanguageCode).substringBefore(" (")}",
                                    onClick = { currentSubPage = ProfileSubPage.PREFERENCES }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.8.dp)
                                ProfileActionRow(
                                    icon = Icons.AutoMirrored.Filled.Send,
                                    iconTint = Emerald400,
                                    title = "Telegram Assistant & Alerts",
                                    subtitle = "@Loanzo_bot • Instant EMI reminders & updates",
                                    onClick = { com.loanzo.app.util.TelegramManager().openBotForLinking(context, user.userId) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // ================= GROUP 4: LEGAL & COMPLIANCE =================
                        ProfileSectionHeader(title = "LEGAL & COMPLIANCE")
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
                                    title = "Terms & Conditions & Legal",
                                    subtitle = "RBI P2P guidelines, eSign legality & privacy rules",
                                    statusBadge = "COMPLIANT",
                                    badgeColor = Emerald400,
                                    onClick = { currentSubPage = ProfileSubPage.TERMS_AND_CONDITIONS }
                                )
                            }
                        }

                        // ================= GROUP 5: APP OWNER ADMIN (Conditional) =================
                        if (isOwner) {
                            Spacer(modifier = Modifier.height(18.dp))
                            ProfileSectionHeader(title = "SYSTEM ADMINISTRATION")
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ProfileActionRow(
                                    icon = Icons.Default.AdminPanelSettings,
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    title = "App Owner Control Center",
                                    subtitle = "Master KYC, user verification, system ledger",
                                    statusBadge = "MASTER",
                                    badgeColor = MaterialTheme.colorScheme.primary,
                                    onClick = onNavigateToAdminHub
                                )
                            }
                        }

                        // ================= GROUP: DEMO & TESTING PLAYGROUND =================
                        Spacer(modifier = Modifier.height(18.dp))
                        ProfileSectionHeader(title = "DEMO & TESTING PLAYGROUND")
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, BrandAmberGold.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                ProfileActionRow(
                                    icon = Icons.Default.PlayArrow,
                                    iconTint = BrandAmberGold,
                                    title = "Push Demo Data Everywhere",
                                    subtitle = "Populate realistic active loans, community wall, EMIs, KYC & notifications",
                                    statusBadge = "DEMO MODE",
                                    badgeColor = BrandAmberGold,
                                    onClick = { showDemoDataDialog = true }
                                )
                            }
                        }

                        // ================= GUIDE ME HUB =================
                        Spacer(modifier = Modifier.height(18.dp))
                        ProfileSectionHeader(title = "INTERACTIVE GUIDES")
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                // Clickable header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isGuideMeExpanded = !isGuideMeExpanded }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Gold500.copy(alpha = 0.15f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Explore,
                                                contentDescription = "Guide Me",
                                                tint = Gold500,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Guide Me 🧭",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Gold500.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "5 TOURS",
                                                    color = Gold500,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Interactive step-by-step walkthroughs of key features",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (isGuideMeExpanded) "Collapse" else "Expand",
                                        tint = Gold500,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .rotate(guideChevronRotation)
                                    )
                                }

                                // Expandable tour list
                                AnimatedVisibility(
                                    visible = isGuideMeExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .padding(bottom = 12.dp)
                                    ) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                            thickness = 0.5.dp,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                         // 1. Featured Loanzo Academy Live Simulator Card
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable { showAcademySheet = true },
                                            color = BrandAmberGold.copy(alpha = 0.12f),
                                            border = BorderStroke(1.dp, BrandAmberGold.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .border(1.dp, BrandAmberGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                                ) {
                                                    Image(
                                                        painter = painterResource(R.drawable.guide_hero_shield),
                                                        contentDescription = "Academy",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(14.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("Loanzo Academy", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, maxLines = 1, softWrap = false)
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(shape = CircleShape, color = BrandAmberGold.copy(alpha = 0.2f)) {
                                                            Text("LIVE", color = BrandAmberGold, fontWeight = FontWeight.Bold, fontSize = 9.sp, maxLines = 1, softWrap = false, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                        }
                                                    }
                                                    Text("Interactive Loan & EMI calculations simulator", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = BrandAmberGold, modifier = Modifier.size(18.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        AppTours.all.forEachIndexed { index, tour ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        scope.launch {
                                                            userRepository.setActiveTour(tour.id, 0)
                                                            onBack()
                                                        }
                                                    }
                                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .border(1.dp, BrandAmberGold.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                                ) {
                                                    Image(
                                                        painter = painterResource(tour.coverImageRes),
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = tour.title,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = tour.subtitle,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = null,
                                                    tint = Gray400,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            if (index < AppTours.all.size - 1) {
                                                HorizontalDivider(
                                                    color = MaterialTheme.colorScheme.outlineVariant,
                                                    thickness = 0.5.dp,
                                                    modifier = Modifier.padding(vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // ================= GROUP 6: ACCOUNT SIGN OUT =================
                        OutlinedButton(
                            onClick = { showLogoutConfirmDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Red400),
                            border = BorderStroke(1.dp, Red400.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.logout), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(36.dp))
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
                    if (showAcademySheet) {
                        LoanzoAcademySimulatorSheet(
                            onDismiss = { showAcademySheet = false }
                        )
                    }
                    }
                }
            }

            ProfileSubPage.DOCUMENT_VAULT -> {
                // ==========================================
                // 2. ENCRYPTED DOCUMENT VAULT SUB-PAGE
                // ==========================================
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Document Vault", fontWeight = FontWeight.Bold) },
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
                        // Security Banner
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
                                Icon(Icons.Default.VerifiedUser, null, tint = Emerald400, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("AES-256 Verified Session Active", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                    Text("Decrypted for this active session only. Documents will re-encrypt when you leave.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                }
                            }
                        }

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

                        // Telegram Assistant Bot
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Telegram Bot Notifications", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Receive instant real-time EMI reminders, disbursal alerts, and loan status updates via our Telegram Bot (@Loanzo_bot).",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { com.loanzo.app.util.TelegramManager().openBotForLinking(context, user.userId) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Color.White)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open @Loanzo_bot on Telegram", fontWeight = FontWeight.Bold)
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
                            title = { Text("Terms & Conditions", fontWeight = FontWeight.Bold) },
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
                                    "Version 2026.9 • Compliant with Reserve Bank of India (RBI) P2P Lending Directives and the Information Technology Act, 2000.",
                                    color = Gray300,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        LegalClauseCard(
                            clauseNumber = "1",
                            title = "Decentralized P2P Facilitation",
                            content = "Loanzo acts as a technology platform and marketplace facilitating direct peer-to-peer lending contracts between verified individuals. Loanzo is not a Non-Banking Financial Company (NBFC) or a bank, does not accept public deposits, and does not provide principal guarantees. All transactions are direct legal contracts between the Lender and Borrower."
                        )

                        LegalClauseCard(
                            clauseNumber = "2",
                            title = "Digital KYC & AML Verification",
                            content = "Users explicitly consent to the verification of PAN card, Aadhaar records via DigiLocker API, and real-time facial liveness. Documents are stored in an AES-256 encrypted Document Vault. Any fraudulent, doctored, or falsified KYC submission constitutes a legal offense under the Prevention of Money Laundering Act (PMLA) and will result in immediate platform restriction and law enforcement reporting."
                        )

                        LegalClauseCard(
                            clauseNumber = "3",
                            title = "Dual-Party eSign & Contract Legality",
                            content = "Loan agreements executed on Loanzo are legally valid and enforceable in courts of law under Section 10A of the Indian Information Technology Act, 2000. Agreements are sealed with dual-party digital signatures, CameraX ML Kit biometric authentication timestamps, and immutable audit hashes."
                        )

                        LegalClauseCard(
                            clauseNumber = "4",
                            title = "Disbursements, Penny Drop & Payouts",
                            content = "Loan capital is disbursed directly from Lender to Borrower or designated third-party merchants via IMPS/UPI. Before any disbursement, Loanzo conducts an automated ₹1 penny-drop bank verification to ensure recipient account authenticity."
                        )

                        LegalClauseCard(
                            clauseNumber = "5",
                            title = "Repayments, Grace Periods & Penalties",
                            content = "Borrowers agree to honor the repayment schedule, tenure, and agreed interest rate. Overdue repayments incur penalties strictly calculated by Loanzo's Compound Penalty Engine and capped in accordance with RBI fair practice codes. Repayment delays trigger automatic alerts to guarantors and Telegram recovery desks."
                        )

                        LegalClauseCard(
                            clauseNumber = "6",
                            title = "Dispute Resolution & Legal Action",
                            content = "Users agree to resolve disputes amicably through the built-in Dispute Center. In cases of persistent default, contract breach, or harassment, the aggrieved counterparty reserves full rights to initiate formal recovery proceedings under Section 138 of the Negotiable Instruments Act and the Indian Contract Act, 1872."
                        )

                        LegalClauseCard(
                            clauseNumber = "7",
                            title = "Data Privacy & Zero-Knowledge Vault",
                            content = "Your sensitive identification records are stored in a password-protected Document Vault with zero-knowledge protocols. Loanzo will never sell, lease, or share your financial data with third-party telemarketers or unauthorized lenders."
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // ================= DIALOGS & MODALS =================

    // 1. Password Verification Dialog for Document Vault
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

    // Demo Data Seeder Dialog
    if (showDemoDataDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSeedingDemo) showDemoDataDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = BrandAmberGold,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Demo Testing Playground",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Push realistic demo data everywhere across the app to test features, UI flows, and lifecycle interactions:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val items = listOf(
                        "✅ Full KYC & Bank Verification" to "Unlocks loan creation, borrowing, and community wall posting without restrictions.",
                        "💰 3 Diverse Active Loans" to "1 Lent loan (₹50k), 1 Borrowed loan (₹25k), and 1 Closed pristine loan.",
                        "📅 EMI Schedules & Statements" to "Paid and upcoming scheduled installments with real UPI transaction references.",
                        "🌐 Community Loan Wall Posts" to "4 rich posts (Lending offers & Borrowing requests) with interactive proposals/bids.",
                        "🔔 Alerts & Audit Logs" to "EMI reminder notifications, disbursal receipts, and audit trail ledger."
                    )
                    
                    items.forEach { (heading, desc) ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Column {
                                Text(
                                    text = heading,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (isSeedingDemo) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = BrandAmberGold
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Applying demo records...",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandAmberGold,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSeedingDemo = true
                        onPushDemoData { success, msg ->
                            isSeedingDemo = false
                            showDemoDataDialog = false
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = !isSeedingDemo,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandAmberGold, contentColor = Color.Black)
                ) {
                    Text("Push Demo Data", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    OutlinedButton(
                        onClick = {
                            isSeedingDemo = true
                            onClearDemoData { success, msg ->
                                isSeedingDemo = false
                                showDemoDataDialog = false
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isSeedingDemo
                    ) {
                        Text("Reset Demo Data")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { showDemoDataDialog = false },
                        enabled = !isSeedingDemo
                    ) {
                        Text("Cancel")
                    }
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
    subtitle: String,
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
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
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
