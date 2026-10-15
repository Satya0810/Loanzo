package com.loanzo.app.ui.admin

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
import androidx.compose.material3.*
import com.loanzo.app.ui.components.LoanzoText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.data.entity.ComplaintEntity
import com.loanzo.app.data.entity.LoanEntity
import com.loanzo.app.data.entity.MarketplacePostEntity
import com.loanzo.app.data.entity.UserEntity
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.LocalAdminRepository
import com.loanzo.app.util.toDateString
import com.loanzo.app.util.toInrString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminUserProfileDetailScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val adminRepository = LocalAdminRepository.current

    val user by adminRepository.getUserFlow(userId).collectAsState(initial = null)
    val loans by adminRepository.getUserLoansFlow(userId).collectAsState(initial = emptyList())
    val posts by adminRepository.getUserPostsFlow(userId).collectAsState(initial = emptyList())
    val complaints by adminRepository.getUserComplaintsFlow(userId).collectAsState(initial = emptyList())

    // Dialog & Action States
    var showTempBanDialog by remember { mutableStateOf(false) }
    var tempBanDays by remember { mutableIntStateOf(7) }
    var tempBanReason by remember { mutableStateOf("") }

    var showPermBanDialog by remember { mutableStateOf(false) }
    var permBanReason by remember { mutableStateOf("") }

    var showReuploadDialog by remember { mutableStateOf(false) }
    var reuploadAadhaar by remember { mutableStateOf(false) }
    var reuploadPan by remember { mutableStateOf(false) }
    var reuploadBank by remember { mutableStateOf(false) }
    var reuploadSelfie by remember { mutableStateOf(false) }
    var reuploadRemarks by remember { mutableStateOf("") }

    var showApproveFullKycDialog by remember { mutableStateOf(false) }
    var fullKycApprovalNotes by remember { mutableStateOf("Full KYC verified after manual inspection") }

    var showAppealDecisionDialog by remember { mutableStateOf<Boolean?>(null) } // true = approve, false = reject
    var appealAdminNotes by remember { mutableStateOf("") }

    var selectedLoansTab by remember { mutableIntStateOf(0) } // 0: Ongoing, 1: Requested, 2: Past

    val ongoingLoans = remember(loans) {
        loans.filter { it.status in listOf("ACTIVE", "ACTIVE_SERVICING", "DISBURSED", "RESTRUCTURED", "DELINQUENT", "LEGAL_DISPUTE") }
    }
    val requestedLoans = remember(loans) {
        loans.filter { it.status in listOf("PENDING", "REQUESTED", "OFFERED", "UNDERWRITING", "SUBMITTED", "PENDING_APPROVAL", "APPLIED", "DRAFT", "MARKETPLACE", "BID_ACCEPTED") }
    }
    val pastLoans = remember(loans) {
        loans.filter { it.status in listOf("COMPLETED", "CLOSED", "PAID", "SETTLED", "REJECTED", "CANCELLED") }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (user == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading user record...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val u = user!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Top Header (Zero Icons)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onNavigateBack,
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "< Back to Users",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "User Moderation",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // User Identity Overview (Unboxed)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = (u.name.ifBlank { u.username.ifBlank { "U" } }).take(1).uppercase(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = u.name.ifBlank { "Unnamed User" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "@${u.username.ifBlank { u.userId }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Account Metadata & Status Badges (FlowRow to prevent overflow)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Role Badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Role: ${u.role}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // KYC Status Badge
                    val kycBg = when (u.kycStatus) {
                        "VERIFIED" -> Emerald500.copy(alpha = 0.15f)
                        "RE_KYC_REQUESTED" -> Orange400.copy(alpha = 0.15f)
                        "REJECTED" -> Red400.copy(alpha = 0.15f)
                        else -> Gold500.copy(alpha = 0.15f)
                    }
                    val kycFg = when (u.kycStatus) {
                        "VERIFIED" -> Emerald600
                        "RE_KYC_REQUESTED" -> Color(0xFFC2410C)
                        "REJECTED" -> Red400
                        else -> Color(0xFFB45309)
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = kycBg
                    ) {
                        Text(
                            text = "KYC: ${u.kycStatus}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = kycFg,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Ban Status Badge
                    if (u.isBanned) {
                        val banLabel = if (u.banType == "TEMPORARY") "TEMPORARILY SUSPENDED" else "PERMANENTLY TERMINATED"
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Red400.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = banLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Red400,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Emerald500.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Account: Active",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Emerald600,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Contact details in clean typography
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (u.phone.isNotBlank()) {
                        Text(
                            text = "Phone: ${u.phone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (u.email.isNotBlank()) {
                        Text(
                            text = "Email: ${u.email}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (u.createdAt > 0L) {
                        Text(
                            text = "Member Since: ${u.createdAt.toDateString()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Ban Details Banner (if banned)
                if (u.isBanned) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Red400.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Red400.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = if (u.banType == "TEMPORARY") "Temporary Suspension Details" else "Permanent Account Ban Details",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Red400
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Reason: ${u.banReason.ifBlank { "Platform policy violation" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (u.banType == "TEMPORARY" && u.banExpiresAt > 0L) {
                                val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Suspension Expires: ${sdf.format(Date(u.banExpiresAt))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Red400,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (u.banType == "PERMANENT") {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "All associated credentials (Phone, Email, PAN, Aadhaar) have been permanently blacklisted from creating or accessing Loanzo accounts.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Ban Appeal Review Banner (if appeal is pending)
                if (u.hasPendingBanAppeal && u.banAppealStatus == "PENDING") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Pending Ban Appeal Received",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                                if (u.banAppealSubmittedAt > 0L) {
                                    Text(
                                        text = u.banAppealSubmittedAt.toDateString(),
                                        fontSize = 11.sp,
                                        color = Color(0xFF92400E)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "User Statement: \"${u.banAppealText}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF78350F)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showAppealDecisionDialog = true },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Emerald600),
                                    border = BorderStroke(1.dp, Emerald600),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Overturn Ban", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { showAppealDecisionDialog = false },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Red400),
                                    border = BorderStroke(1.dp, Red400),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Reject Appeal", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 20.dp)
                )

                // ==========================================
                // DOCUMENT-BY-DOCUMENT REVIEW & ANALYSIS
                // ==========================================
                Text(
                    text = "KYC Documents & Verification Review",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Analyze each document individually before attestation. Single-tap blind approvals are prohibited.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Document 1: Aadhaar
                DocumentReviewRow(
                    docTitle = "Aadhaar Card",
                    docIdentifier = if (u.aadhaarNumber.isNotBlank()) "UID: ${u.aadhaarNumber}" else "Aadhaar number not provided",
                    hasImage = u.aadhaarImageUrl.isNotBlank(),
                    imageUrl = u.aadhaarImageUrl,
                    isVerified = u.aadhaarVerified,
                    onToggleVerification = { newStatus ->
                        scope.launch(Dispatchers.IO) {
                            adminRepository.verifyUserDocument(u.userId, "AADHAAR", newStatus, "Manual review by admin")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Document 2: PAN
                DocumentReviewRow(
                    docTitle = "PAN Card",
                    docIdentifier = if (u.panNumber.isNotBlank()) "PAN: ${u.panNumber}" else "PAN number not provided",
                    hasImage = u.panImageUrl.isNotBlank(),
                    imageUrl = u.panImageUrl,
                    isVerified = u.panVerified,
                    onToggleVerification = { newStatus ->
                        scope.launch(Dispatchers.IO) {
                            adminRepository.verifyUserDocument(u.userId, "PAN", newStatus, "Manual review by admin")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Document 3: Bank Account & IFSC
                DocumentReviewRow(
                    docTitle = "Bank Account & IFSC",
                    docIdentifier = if (u.bankAccountNumber.isNotBlank()) "A/C: ${u.bankAccountNumber} | IFSC: ${u.bankIfsc}" else "Bank details not provided",
                    hasImage = false,
                    imageUrl = "",
                    isVerified = u.bankVerified,
                    onToggleVerification = { newStatus ->
                        scope.launch(Dispatchers.IO) {
                            adminRepository.verifyUserDocument(u.userId, "BANK", newStatus, "Manual review by admin")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Document 4: Live Selfie
                DocumentReviewRow(
                    docTitle = "Live Selfie / Photo",
                    docIdentifier = if (u.profilePhotoUri.isNotBlank()) "Photo uploaded" else "Selfie photo pending",
                    hasImage = u.profilePhotoUri.isNotBlank(),
                    imageUrl = u.profilePhotoUri,
                    isVerified = u.selfieVerified,
                    onToggleVerification = { newStatus ->
                        scope.launch(Dispatchers.IO) {
                            adminRepository.verifyUserDocument(u.userId, "SELFIE", newStatus, "Manual review by admin")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // KYC Master Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            reuploadAadhaar = !u.aadhaarVerified
                            reuploadPan = !u.panVerified
                            reuploadBank = !u.bankVerified
                            reuploadSelfie = !u.selfieVerified
                            showReuploadDialog = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Request Re-upload", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { showApproveFullKycDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                    ) {
                        Text("Approve Full KYC", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 20.dp)
                )

                // ==========================================
                // USER LOANS PORTFOLIO
                // ==========================================
                Text(
                    text = "User Loans Portfolio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Tab Filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf(
                        "Ongoing (${ongoingLoans.size})",
                        "Requested (${requestedLoans.size})",
                        "Past (${pastLoans.size})"
                    )
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedLoansTab == index
                        Surface(
                            onClick = { selectedLoansTab = index },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val currentLoanList = when (selectedLoansTab) {
                    0 -> ongoingLoans
                    1 -> requestedLoans
                    else -> pastLoans
                }

                if (currentLoanList.isEmpty()) {
                    Text(
                        text = "No records in this category.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currentLoanList.forEach { loan ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = loan.purpose.ifBlank { "Loan #${loan.loanId.take(8)}" },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = loan.status,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Amount: ${loan.sanctionedAmount.toInrString()} • Rate: ${loan.interestRate}% • Tenure: ${loan.tenureMonths}m",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (loan.createdAt > 0L) {
                                        Text(
                                            text = "Created: ${loan.createdAt.toDateString()}",
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 20.dp)
                )

                // ==========================================
                // USER COMMUNITY POSTS
                // ==========================================
                Text(
                    text = "Community Marketplace Posts (${posts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (posts.isEmpty()) {
                    Text(
                        text = "No community marketplace posts authored by this user.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        posts.forEach { post ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = post.postType,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (post.isAdminVerified) "Verified Post" else "Unverified",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (post.isAdminVerified) Emerald600 else Orange400
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${post.minAmount.toInrString()} - ${post.maxAmount.toInrString()} • ${post.interestRate}% interest",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (post.description.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = post.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 20.dp)
                )

                // ==========================================
                // REPORTS & COMPLAINTS AGAINST USER
                // ==========================================
                Text(
                    text = "Reports & Disputes Filed Against User (${complaints.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (complaints.isEmpty()) {
                    Text(
                        text = "Clean track record. No grievances or dispute reports filed against this user.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        complaints.forEach { c ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Red400.copy(alpha = 0.05f),
                                border = BorderStroke(0.5.dp, Red400.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${c.complaintId} • ${c.category}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Red400
                                        )
                                        Text(
                                            text = c.status,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Reported by: ${c.complainantName} (${c.complainantPhone})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (c.subject.isNotBlank()) {
                                        Text(
                                            text = "Subject: ${c.subject}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (c.description.isNotBlank()) {
                                        Text(
                                            text = c.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 20.dp)
                )

                // ==========================================
                // ACCOUNT MODERATION & BANNING CONTROLS
                // ==========================================
                Text(
                    text = "Account Moderation & Sanctions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Temporary suspension or permanent ban with credential blacklisting.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (u.isBanned) {
                    Button(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                adminRepository.liftBan(u.userId)
                                launch(Dispatchers.Main) {
                                    Toast.makeText(context, "Account ban lifted for ${u.name.ifBlank { u.username }}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Lift Ban & Restore Account", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                tempBanDays = 7
                                tempBanReason = ""
                                showTempBanDialog = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Orange400),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Orange400),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Temporary Ban", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                permBanReason = ""
                                showPermBanDialog = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Red400),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Permanent Ban", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // ==========================================
    // MODALS & DIALOGS
    // ==========================================

    // 1. Temporary Ban Dialog
    if (showTempBanDialog && user != null) {
        val u = user!!
        AlertDialog(
            onDismissRequest = { showTempBanDialog = false },
            title = { Text("Temporary Suspension", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Suspend ${u.name.ifBlank { u.username }} from logging in or conducting transactions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Suspension Duration (Days):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(3, 7, 14, 30).forEach { days ->
                            val isSelected = tempBanDays == days
                            Surface(
                                onClick = { tempBanDays = days },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${days}d",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempBanReason,
                        onValueChange = { tempBanReason = it },
                        label = { Text("Reason for Suspension") },
                        placeholder = { Text("e.g. Investigation into delayed repayment") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempBanReason.isBlank()) {
                            Toast.makeText(context, "Please specify a reason", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        showTempBanDialog = false
                        scope.launch(Dispatchers.IO) {
                            adminRepository.banUserTemporarily(u.userId, tempBanDays, tempBanReason.trim())
                            launch(Dispatchers.Main) {
                                Toast.makeText(context, "Temporary ban applied for $tempBanDays days", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Orange400)
                ) {
                    Text("Apply Suspension")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTempBanDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 2. Permanent Ban Dialog
    if (showPermBanDialog && user != null) {
        val u = user!!
        AlertDialog(
            onDismissRequest = { showPermBanDialog = false },
            title = { Text("Permanent Account Ban", fontWeight = FontWeight.Bold, color = Red400) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "WARNING: Permanent termination is irrevocable. The user will be immediately logged out and barred from logging in.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Red400,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The following credentials will be permanently blacklisted from ever registering again on Loanzo:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (u.phone.isNotBlank()) Text("• Phone: ${u.phone}", fontSize = 11.5.sp)
                    if (u.email.isNotBlank()) Text("• Email: ${u.email}", fontSize = 11.5.sp)
                    if (u.panNumber.isNotBlank()) Text("• PAN: ${u.panNumber}", fontSize = 11.5.sp)
                    if (u.aadhaarNumber.isNotBlank()) Text("• Aadhaar: ${u.aadhaarNumber}", fontSize = 11.5.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = permBanReason,
                        onValueChange = { permBanReason = it },
                        label = { Text("Permanent Ban Reason") },
                        placeholder = { Text("e.g. Identity fraud / falsified documents") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (permBanReason.isBlank()) {
                            Toast.makeText(context, "Please specify a reason", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        showPermBanDialog = false
                        scope.launch(Dispatchers.IO) {
                            adminRepository.banUserPermanently(u.userId, permBanReason.trim())
                            launch(Dispatchers.Main) {
                                Toast.makeText(context, "Account permanently terminated & credentials blacklisted", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red400)
                ) {
                    Text("Confirm Permanent Ban")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermBanDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 3. Document Re-upload Request Dialog
    if (showReuploadDialog && user != null) {
        val u = user!!
        AlertDialog(
            onDismissRequest = { showReuploadDialog = false },
            title = { Text("Request Document Re-upload", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Select which document(s) have deficiencies and need re-submission by ${u.name.ifBlank { u.username }}:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { reuploadAadhaar = !reuploadAadhaar }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = reuploadAadhaar, onCheckedChange = { reuploadAadhaar = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Aadhaar Card", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { reuploadPan = !reuploadPan }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = reuploadPan, onCheckedChange = { reuploadPan = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PAN Card", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { reuploadBank = !reuploadBank }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = reuploadBank, onCheckedChange = { reuploadBank = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bank Account Details", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { reuploadSelfie = !reuploadSelfie }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = reuploadSelfie, onCheckedChange = { reuploadSelfie = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Selfie Photo", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reuploadRemarks,
                        onValueChange = { reuploadRemarks = it },
                        label = { Text("Deficiency Notes for User") },
                        placeholder = { Text("e.g. Photo is blurred / details not readable") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val requested = mutableListOf<String>()
                        if (reuploadAadhaar) requested.add("AADHAAR")
                        if (reuploadPan) requested.add("PAN")
                        if (reuploadBank) requested.add("BANK")
                        if (reuploadSelfie) requested.add("SELFIE")

                        if (requested.isEmpty()) {
                            Toast.makeText(context, "Select at least one document to request", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        showReuploadDialog = false
                        scope.launch(Dispatchers.IO) {
                            for (doc in requested) {
                                adminRepository.verifyUserDocument(u.userId, doc, false, reuploadRemarks.ifBlank { "Re-upload requested by admin" })
                            }
                            launch(Dispatchers.Main) {
                                Toast.makeText(context, "Re-upload notification sent to user", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Send Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReuploadDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 4. Approve Full KYC Confirmation Dialog
    if (showApproveFullKycDialog && user != null) {
        val u = user!!
        AlertDialog(
            onDismissRequest = { showApproveFullKycDialog = false },
            title = { Text("Approve Full Platform KYC", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Are you sure you have analyzed and confirmed all statutory KYC details for ${u.name.ifBlank { u.username }}?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = fullKycApprovalNotes,
                        onValueChange = { fullKycApprovalNotes = it },
                        label = { Text("Attestation Audit Remarks") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showApproveFullKycDialog = false
                        scope.launch(Dispatchers.IO) {
                            adminRepository.verifyFullUserKyc(u.userId, true, fullKycApprovalNotes)
                            launch(Dispatchers.Main) {
                                Toast.makeText(context, "Full KYC Approved for ${u.name.ifBlank { u.username }}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) {
                    Text("Confirm Approval")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApproveFullKycDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 5. Ban Appeal Decision Dialog
    if (showAppealDecisionDialog != null && user != null) {
        val isApprove = showAppealDecisionDialog == true
        val u = user!!
        AlertDialog(
            onDismissRequest = { showAppealDecisionDialog = null },
            title = {
                Text(
                    text = if (isApprove) "Overturn Ban & Restore Account" else "Reject Ban Appeal",
                    fontWeight = FontWeight.Bold,
                    color = if (isApprove) Emerald600 else Red400
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isApprove)
                            "Approving will restore account access for ${u.name.ifBlank { u.username }} and clear blacklisted credentials."
                        else
                            "Rejecting will keep the account banned and notify the user.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = appealAdminNotes,
                        onValueChange = { appealAdminNotes = it },
                        label = { Text("Decision Remarks") },
                        placeholder = { Text("e.g. Identity verified / explanation accepted") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAppealDecisionDialog = null
                        scope.launch(Dispatchers.IO) {
                            adminRepository.resolveBanAppeal(u.userId, isApprove, appealAdminNotes.trim())
                            launch(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    if (isApprove) "Ban overturned & account restored" else "Appeal rejected",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isApprove) Emerald600 else Red400
                    )
                ) {
                    Text(if (isApprove) "Restore Account" else "Confirm Rejection")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAppealDecisionDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Clean, unboxed document inspection row without icons or cartoon boxes.
 */
@Composable
private fun DocumentReviewRow(
    docTitle: String,
    docIdentifier: String,
    hasImage: Boolean,
    imageUrl: String,
    isVerified: Boolean,
    onToggleVerification: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = docTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isVerified) Emerald500.copy(alpha = 0.15f) else Gold500.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isVerified) "Verified" else "Pending Review",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isVerified) Emerald600 else Color(0xFFB45309),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = docIdentifier,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Attestation action
            if (isVerified) {
                OutlinedButton(
                    onClick = { onToggleVerification(false) },
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Red400),
                    border = BorderStroke(0.8.dp, Red400.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Revoke", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = { onToggleVerification(true) },
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Attest", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
