package com.loanzo.app.ui.marketplace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.loanzo.app.ui.theme.*

/**
 * High-fidelity verified profile data model representing a loan seeker,
 * lender, co-borrower, or community endorser (voucher).
 */
data class UserProfileViewData(
    val userId: String,
    val name: String,
    val username: String = "",
    val roleTitle: String, // "PRIMARY BORROWER (LOAN SEEKER)", "CAPITAL PROVIDER (LENDER)", "CO-BORROWER / GUARANTOR", "COMMUNITY ENDORSER (VOUCHER)"
    val avatarUrl: String = "",
    val locationCity: String = "",
    val trustScore: Int = 92,
    val verificationLevel: String = "Tier 3: Institutional Gold",
    val verificationTier: Int = 3,
    val phoneVerified: Boolean = true,
    val emailVerified: Boolean = true,
    val aadhaarVerified: Boolean = true,
    val panVerified: Boolean = true,
    val selfieVerified: Boolean = true,
    val bankVerified: Boolean = true,
    val upiVerified: Boolean = true,
    val ckycVerified: Boolean = true,
    val onTimeRepaymentRate: Double = 100.0,
    val completedLoansCount: Int = 5,
    val activeLoansCount: Int = 1,
    val defaultCount: Int = 0,
    val vouchesReceivedCount: Int = 18,
    val memberSince: String = "Member since Feb 2024",
    val employmentStatus: String = "Verified Salaried / Business Cashflow",
    val monthlyIncomeFormatted: String = "₹65,000 / month",
    val collateralOrProof: String = "",
    val relationshipToBorrower: String = "", // Used when viewing a Co-Borrower
    val vouchReason: String = "", // Used when viewing a Voucher
    val vouchComment: String = "", // Testimonial left by the voucher
    val phoneMasked: String = "+91 98•••• 1234",
    val emailMasked: String = "v•••••@gmail.com"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileDetailBottomSheet(
    profile: UserProfileViewData,
    onDismiss: () -> Unit,
    onConnectClick: (userId: String) -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 44.dp, height = 4.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.outlineVariant
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row: Avatar, Name & Verification Pill
            Spacer(modifier = Modifier.height(6.dp))

            Box(contentAlignment = Alignment.BottomEnd) {
                if (profile.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = profile.avatarUrl,
                        contentDescription = profile.name,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(2.dp, BrandRoyalBlue, CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        BrandRoyalBlue.copy(alpha = 0.2f),
                                        BrandIceBlue
                                    )
                                )
                            )
                            .border(2.dp, BrandRoyalBlue.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.name.take(2).uppercase(),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandRoyalBlue
                        )
                    }
                }

                // Verified Emblem Badge
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Emerald500)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Verified",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // User Name & Username
            Text(
                text = profile.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            if (profile.username.isNotBlank()) {
                Text(
                    text = "@${profile.username.removePrefix("@")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandRoyalBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Role Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = when {
                    profile.roleTitle.contains("LENDER", ignoreCase = true) -> GoldCoinCream
                    profile.roleTitle.contains("CO-BORROWER", ignoreCase = true) -> Color(0xFFF3E8FF)
                    profile.roleTitle.contains("VOUCHER", ignoreCase = true) -> EmeraldLight
                    else -> BrandIceBlue
                },
                border = BorderStroke(
                    1.dp,
                    when {
                        profile.roleTitle.contains("LENDER", ignoreCase = true) -> GoldCoinBorder
                        profile.roleTitle.contains("CO-BORROWER", ignoreCase = true) -> Color(0xFFD8B4FE)
                        profile.roleTitle.contains("VOUCHER", ignoreCase = true) -> Emerald400.copy(alpha = 0.4f)
                        else -> BrandIceBorder
                    }
                )
            ) {
                Text(
                    text = profile.roleTitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    color = when {
                        profile.roleTitle.contains("LENDER", ignoreCase = true) -> GoldCoinAmber
                        profile.roleTitle.contains("CO-BORROWER", ignoreCase = true) -> Color(0xFF7E22CE)
                        profile.roleTitle.contains("VOUCHER", ignoreCase = true) -> Emerald600
                        else -> BrandRoyalBlue
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Location & Tenure Meta
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (profile.locationCity.isNotBlank()) {
                    Text(
                        text = "📍 ${profile.locationCity}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("•", color = MaterialTheme.colorScheme.outlineVariant)
                }
                Text(
                    text = profile.memberSince,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ─────────────────────────────────────────────────────────────────
            // 1. INSTITUTIONAL MULTI-TIER VERIFICATION LEVEL HERO BANNER
            // ─────────────────────────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = BrandIceBlue,
                border = BorderStroke(1.5.dp, BrandIceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(BrandRoyalBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Verification Tier",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.verificationLevel,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = BrandRoyalBlue
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Authenticated via UIDAI Aadhaar eKYC, NSDL PAN, & NPCI Penny Drop",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSlateMedium,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─────────────────────────────────────────────────────────────────
            // 2. VERIFICATION CREDENTIALS CHECKLIST
            // ─────────────────────────────────────────────────────────────────
            Text(
                text = "VERIFICATION CREDENTIALS & AUDIT TRAIL",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    VerificationItemRow(
                        icon = Icons.Default.PhoneIphone,
                        label = "Phone OTP Verified",
                        detail = profile.phoneMasked,
                        isVerified = profile.phoneVerified
                    )
                    VerificationItemRow(
                        icon = Icons.Default.Email,
                        label = "Email Domain Verified",
                        detail = profile.emailMasked,
                        isVerified = profile.emailVerified
                    )
                    VerificationItemRow(
                        icon = Icons.Default.Badge,
                        label = "Aadhaar eKYC (UIDAI)",
                        detail = "Biometric & Demographic Match",
                        isVerified = profile.aadhaarVerified
                    )
                    VerificationItemRow(
                        icon = Icons.Default.CreditCard,
                        label = "PAN Card (NSDL/ITD)",
                        detail = "Income Tax Database Validated",
                        isVerified = profile.panVerified
                    )
                    VerificationItemRow(
                        icon = Icons.Default.Face,
                        label = "Liveness Selfie & Biometric",
                        detail = "Facial Geometry Authenticated",
                        isVerified = profile.selfieVerified
                    )
                    VerificationItemRow(
                        icon = Icons.Default.AccountBalance,
                        label = "Bank Account (Penny Drop)",
                        detail = "Name Match & IFSC Linked",
                        isVerified = profile.bankVerified
                    )
                    VerificationItemRow(
                        icon = Icons.Default.QrCode,
                        label = "Instant UPI VPA Handle",
                        detail = "Automated Repayment Enabled",
                        isVerified = profile.upiVerified
                    )
                    VerificationItemRow(
                        icon = Icons.Default.Shield,
                        label = "Central RBI CKYC Registry",
                        detail = "Digital Banking Record Cleared",
                        isVerified = profile.ckycVerified
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ─────────────────────────────────────────────────────────────────
            // 3. CREDIT & REPAYMENT TRACK RECORD MATRIX
            // ─────────────────────────────────────────────────────────────────
            Text(
                text = "COMMUNITY TRUST & REPAYMENT METRICS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Trust Score
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, GoldCoinBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TRUST SCORE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldCoinAmber
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${profile.trustScore}/100",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextNavyDark
                        )
                        Text(
                            text = "Top Tier",
                            fontSize = 10.sp,
                            color = Emerald600,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // On-Time Repayment
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.4f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ON-TIME RATE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald600
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${profile.onTimeRepaymentRate.toInt()}%",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextNavyDark
                        )
                        Text(
                            text = "Zero Overdues",
                            fontSize = 10.sp,
                            color = Emerald600,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Closed Loans & Defaults
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, BrandIceBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "LOANS CLOSED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandRoyalBlue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${profile.completedLoansCount} Loans",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextNavyDark
                        )
                        Text(
                            text = "0 Defaults",
                            fontSize = 10.sp,
                            color = Emerald600,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────────
            // 4. CONTEXTUAL CO-BORROWER OR VOUCHER SPECIAL CARD
            // ─────────────────────────────────────────────────────────────────
            if (profile.relationshipToBorrower.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFAF5FF),
                    border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = null,
                            tint = Color(0xFF9333EA),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Co-Borrower Relationship: ${profile.relationshipToBorrower}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF581C87)
                            )
                            Text(
                                text = "Jointly & severally liable under Indian Contract Act § 128",
                                fontSize = 10.5.sp,
                                color = Color(0xFF7E22CE)
                            )
                        }
                    }
                }
            }

            if (profile.vouchReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = EmeraldLight,
                    border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Emerald600,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Endorsement Type: ${profile.vouchReason.replace('_', ' ')}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald600
                            )
                        }
                        if (profile.vouchComment.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\"${profile.vouchComment}\"",
                                fontSize = 12.sp,
                                color = TextNavyDark,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ─────────────────────────────────────────────────────────────────
            // 5. ACTION BUTTONS
            // ─────────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text(
                        text = "Close",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = {
                        onConnectClick(profile.userId)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandRoyalBlue,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1.4f).height(48.dp)
                ) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Connect / Chat",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun VerificationItemRow(
    icon: ImageVector,
    label: String,
    detail: String,
    isVerified: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isVerified) BrandRoyalBlue else TextSlateMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = detail,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isVerified) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = EmeraldLight
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Verified",
                        tint = Emerald600,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Verified",
                        color = Emerald600,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Text(
                text = "Pending",
                color = TextSlateMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
