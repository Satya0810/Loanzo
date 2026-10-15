package com.loanzo.app.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.loanzo.app.ui.components.LoanzoText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.ui.theme.*

@Composable
fun AdminCategorySection(
    title: String,
    badgeCount: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp,
                color = Color(0xFF64748B)
            )

            if (badgeCount > 0) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Red400.copy(alpha = 0.12f),
                    border = BorderStroke(0.5.dp, Red400.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "$badgeCount PENDING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Red400,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        content()
    }
}

@Composable
fun AdminActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    badgeCount: Int = 0,
    badgeColor: Color = Red400,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFFFFFF),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 0.dp,
        modifier = modifier
            .width(148.dp)
            .height(130.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                            .clip(CircleShape)
                            .background(badgeColor)
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Column {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Row 1: Dual Hero Pastel Feature Cards (2 Columns)
 * Left: Users & Directory (soft blush rose)
 * Right: KYC Desk (soft periwinkle lavender)
 */
@Composable
fun AdminBentoDualHeroRow(
    userCount: Int,
    pendingAppealsCount: Int = 0,
    pendingKycCount: Int = 0,
    onUsersClick: () -> Unit,
    onKycClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Left Card: Users / Attendance equivalent
        Surface(
            onClick = onUsersClick,
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFFFF1F2), // Soft Rose Pastel
            border = BorderStroke(1.dp, Color(0xFFFECDD3)),
            modifier = Modifier
                .weight(1f)
                .height(115.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background accent graphic bubble on right
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 12.dp, y = 14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFE4E6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PeopleAlt,
                        contentDescription = "Users",
                        tint = Color(0xFFE11D48),
                        modifier = Modifier.size(34.dp).offset(x = (-4).dp, y = (-4).dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Users",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.85f),
                        border = BorderStroke(0.5.dp, Color(0xFFFDA4AF))
                    ) {
                        Text(
                            text = if (pendingAppealsCount > 0) "$userCount • $pendingAppealsCount appeals" else "$userCount registered",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFBE123C),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Right Card: KYC Desk / Timetable equivalent
        Surface(
            onClick = onKycClick,
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFEEF2FF), // Soft Lavender/Periwinkle Pastel
            border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
            modifier = Modifier
                .weight(1f)
                .height(115.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background accent graphic bubble on right
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 12.dp, y = 14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0E7FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "KYC Verification",
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(34.dp).offset(x = (-4).dp, y = (-4).dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "KYC Desk",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.85f),
                        border = BorderStroke(0.5.dp, Color(0xFFA5B4FC))
                    ) {
                        Text(
                            text = if (pendingKycCount > 0) "$pendingKycCount pending" else "All verified",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (pendingKycCount > 0) Color(0xFF4338CA) else Color(0xFF059669),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Row 2: Secondary Pastel Action Strip (3 Columns)
 * Fees / Holidays / Notifications equivalent
 * Left: Vault & Lockers (mint green)
 * Center: Field Dispatches (sky blue)
 * Right: Disputes & SOS (warm peach/amber)
 */
@Composable
fun AdminBentoTrioActionStrip(
    vaultCount: Int,
    unassignedVisitsCount: Int,
    openComplaintsCount: Int,
    onVaultClick: () -> Unit,
    onDispatchClick: () -> Unit,
    onDisputesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Vault
        Surface(
            onClick = onVaultClick,
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFECFDF5), // Soft Mint
            border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
            modifier = Modifier
                .weight(1f)
                .height(88.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 8.dp, y = 10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD1FAE5)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Vault",
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(24.dp).offset(x = (-3).dp, y = (-3).dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Vault",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "$vaultCount custody",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF047857)
                    )
                }
            }
        }

        // 2. Dispatch
        Surface(
            onClick = onDispatchClick,
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFF0F9FF), // Soft Sky Blue
            border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
            modifier = Modifier
                .weight(1f)
                .height(88.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 8.dp, y = 10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0E7FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = "Dispatch",
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(24.dp).offset(x = (-3).dp, y = (-3).dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Dispatch",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = if (unassignedVisitsCount > 0) "$unassignedVisitsCount unassigned" else "Dispatched",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0369A1)
                    )
                }
            }
        }

        // 3. Disputes / SOS
        Surface(
            onClick = onDisputesClick,
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFFFFBEB), // Soft Warm Amber
            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
            modifier = Modifier
                .weight(1f)
                .height(88.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 8.dp, y = 10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEF3C7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = "Disputes",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(24.dp).offset(x = (-3).dp, y = (-3).dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Disputes",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = if (openComplaintsCount > 0) "$openComplaintsCount open" else "Resolved",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFB45309)
                    )
                }
            }
        }
    }
}

/**
 * Single Tile in the 4-Column Grid Card
 */
@Composable
fun AdminBentoGridActionTile(
    title: String,
    icon: ImageVector,
    iconTint: Color = Color(0xFF2563EB),
    badgeCount: Int = 0,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFFFFFF),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .height(86.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Red400)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF334155),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Row 3: Primary Quick Actions 4x2 Grid White Card
 * Matches Enrollment, Feedback, Undertaking, Grievance, Mentorship, NEFT Form, Performance, Request Center
 * with expandable arrow pill at bottom.
 */
@Composable
fun AdminBentoActionsGridCard(
    pendingAgentAppsCount: Int,
    unverifiedPostsCount: Int,
    openTicketsCount: Int,
    onEmpanelClick: () -> Unit,
    onFeedbackClick: () -> Unit,
    onPromissoryClick: () -> Unit,
    onGrievanceClick: () -> Unit,
    onPerformanceClick: () -> Unit,
    onClearanceClick: () -> Unit,
    onLedgerClick: () -> Unit,
    onDirectoryClick: () -> Unit,
    onTelegramClick: () -> Unit = {},
    onAuditClick: () -> Unit = {},
    onSeederClick: () -> Unit = {},
    onCockpitClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFFFFFFF),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Row 1 (4 items)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminBentoGridActionTile(
                    title = "Empanel",
                    icon = Icons.Default.Badge,
                    iconTint = Color(0xFF2563EB),
                    badgeCount = pendingAgentAppsCount,
                    modifier = Modifier.weight(1f),
                    onClick = onEmpanelClick
                )
                AdminBentoGridActionTile(
                    title = "Feedback",
                    icon = Icons.Default.RateReview,
                    iconTint = Color(0xFF0284C7),
                    badgeCount = unverifiedPostsCount,
                    modifier = Modifier.weight(1f),
                    onClick = onFeedbackClick
                )
                AdminBentoGridActionTile(
                    title = "Promissory",
                    icon = Icons.Default.ReceiptLong,
                    iconTint = Color(0xFF0D9488),
                    modifier = Modifier.weight(1f),
                    onClick = onPromissoryClick
                )
                AdminBentoGridActionTile(
                    title = "Grievance",
                    icon = Icons.Default.SupportAgent,
                    iconTint = Color(0xFFE11D48),
                    badgeCount = openTicketsCount,
                    modifier = Modifier.weight(1f),
                    onClick = onGrievanceClick
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2 (4 items)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminBentoGridActionTile(
                    title = "Performance",
                    icon = Icons.Default.TrendingUp,
                    iconTint = Color(0xFF7C3AED),
                    modifier = Modifier.weight(1f),
                    onClick = onPerformanceClick
                )
                AdminBentoGridActionTile(
                    title = "Clearance",
                    icon = Icons.Default.AssignmentTurnedIn,
                    iconTint = Color(0xFF059669),
                    modifier = Modifier.weight(1f),
                    onClick = onClearanceClick
                )
                AdminBentoGridActionTile(
                    title = "Ledger",
                    icon = Icons.Default.AccountBalanceWallet,
                    iconTint = Color(0xFF2563EB),
                    modifier = Modifier.weight(1f),
                    onClick = onLedgerClick
                )
                AdminBentoGridActionTile(
                    title = "Directory",
                    icon = Icons.Default.Contacts,
                    iconTint = Color(0xFF475569),
                    modifier = Modifier.weight(1f),
                    onClick = onDirectoryClick
                )
            }

            // Expandable Row
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AdminBentoGridActionTile(
                            title = "Telegram",
                            icon = Icons.Default.Send,
                            iconTint = Color(0xFF0284C7),
                            modifier = Modifier.weight(1f),
                            onClick = onTelegramClick
                        )
                        AdminBentoGridActionTile(
                            title = "Audit Logs",
                            icon = Icons.Default.Shield,
                            iconTint = Color(0xFF059669),
                            modifier = Modifier.weight(1f),
                            onClick = onAuditClick
                        )
                        AdminBentoGridActionTile(
                            title = "Demo Push",
                            icon = Icons.Default.Bolt,
                            iconTint = Color(0xFFD97706),
                            modifier = Modifier.weight(1f),
                            onClick = onSeederClick
                        )
                        AdminBentoGridActionTile(
                            title = "Cockpit",
                            icon = Icons.Default.Security,
                            iconTint = Color(0xFF4F46E5),
                            modifier = Modifier.weight(1f),
                            onClick = onCockpitClick
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Expandable Chevron Button (matching the reference UI)
            Surface(
                onClick = { isExpanded = !isExpanded },
                shape = CircleShape,
                color = Color(0xFFF1F5F9),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle More",
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Row 4: Statutory & Verification Hub (Assessment Hub equivalent)
 * 4 Double-Ringed Circular Action Buttons:
 * 1. Promissory Note (Rose)
 * 2. Order 37 CPC (Purple)
 * 3. BNS §351 SOS (Cyan)
 * 4. NOC Release (Emerald)
 */
@Composable
fun AdminBentoStatutoryHubSection(
    onPromissoryClick: () -> Unit,
    onCpcClick: () -> Unit,
    onSosClick: () -> Unit,
    onNocClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "Statutory & Verification Hub",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFFFFFFF),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.Top
            ) {
                // Button 1: Promissory Note (Rose)
                AdminBentoCircularButton(
                    title = "Promissory\nNote",
                    icon = Icons.Default.Description,
                    ringColor = Color(0xFFFDA4AF),
                    tintColor = Color(0xFFE11D48),
                    bgColor = Color(0xFFFFF1F2),
                    onClick = onPromissoryClick
                )

                // Button 2: Order 37 CPC (Purple)
                AdminBentoCircularButton(
                    title = "Order 37\nCPC",
                    icon = Icons.Default.Gavel,
                    ringColor = Color(0xFFC4B5FD),
                    tintColor = Color(0xFF7C3AED),
                    bgColor = Color(0xFFF5F3FF),
                    onClick = onCpcClick
                )

                // Button 3: BNS §351 SOS (Cyan)
                AdminBentoCircularButton(
                    title = "BNS §351\nShield",
                    icon = Icons.Default.Shield,
                    ringColor = Color(0xFF67E8F9),
                    tintColor = Color(0xFF0891B2),
                    bgColor = Color(0xFFECFEFF),
                    onClick = onSosClick
                )

                // Button 4: NOC Release (Emerald)
                AdminBentoCircularButton(
                    title = "NOC\nRelease",
                    icon = Icons.Default.FactCheck,
                    ringColor = Color(0xFF6EE7B7),
                    tintColor = Color(0xFF059669),
                    bgColor = Color(0xFFECFDF5),
                    onClick = onNocClick
                )
            }
        }
    }
}

@Composable
fun AdminBentoCircularButton(
    title: String,
    icon: ImageVector,
    ringColor: Color,
    tintColor: Color,
    bgColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(74.dp)
            .clickable(onClick = onClick)
    ) {
        Surface(
            shape = CircleShape,
            color = bgColor,
            border = BorderStroke(2.dp, ringColor),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tintColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1E293B),
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

/**
 * Elevated Institutional Evidence Banner
 * Deep royal navy card leading to the Research & Statutory Evidence Hub
 */
@Composable
fun AdminBentoEvidenceBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF1E3A8A), // Deep Royal Institutional Navy
        border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF2563EB),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified Grounds",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Research & Evidence Grounds",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF059669)
                        ) {
                            Text(
                                text = "VERIFIED",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "World Bank, RBI & High Court official data & links",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF93C5FD),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open Evidence",
                tint = Color(0xFF93C5FD),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

