package com.loanzo.app.ui.agent

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.data.entity.AgentApplicationEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentPendingApprovalScreen(
    application: AgentApplicationEntity?,
    onEnterAgentDashboard: () -> Unit,
    onReapply: () -> Unit,
    onLogout: () -> Unit
) {
    val darkBg = Color(0xFF0D1117)
    val cardBg = Color(0xFF161B22)
    val borderColor = Color(0xFF30363D)
    val goldAccent = Color(0xFFFFB800)
    val emeraldAccent = Color(0xFF10B981)
    val redAccent = Color(0xFFEF4444)

    val isApproved = application?.status == "APPROVED"
    val isRejected = application?.status == "REJECTED"

    LaunchedEffect(isApproved) {
        if (isApproved) {
            onEnterAgentDashboard()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Empanelment Status",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text(
                            text = "Sign Out",
                            color = Color(0xFF9CA3AF),
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF161B22)
                )
            )
        },
        containerColor = darkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Icon
            val statusIconColor = when {
                isApproved -> emeraldAccent
                isRejected -> redAccent
                else -> goldAccent
            }

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(statusIconColor.copy(alpha = 0.15f))
                    .border(2.dp, statusIconColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isApproved -> Icons.Default.Verified
                        isRejected -> Icons.Default.Cancel
                        else -> Icons.Default.HourglassTop
                    },
                    contentDescription = null,
                    tint = statusIconColor,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when {
                    isApproved -> "Empanelment Approved!"
                    isRejected -> "Empanelment Application Rejected"
                    else -> "Dossier Under Admin Review"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = when {
                    isApproved -> "Congratulations! You are certified as a Loanzo Field Officer. Your dispatch console is now active."
                    isRejected -> application?.adminRemarks ?: "Your application did not meet the police clearance or field experience criteria."
                    else -> "Your application has been received and routed to the Master Admin (@satyam_081) queue for background clearance."
                },
                fontSize = 13.sp,
                color = Color(0xFF9CA3AF),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Application Summary Card
            if (application != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "APPLICATION DOSSIER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B949E),
                                letterSpacing = 1.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = statusIconColor.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, statusIconColor.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = application.status,
                                    color = statusIconColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        DetailRow(label = "Application ID", value = application.applicationId)
                        DetailRow(label = "Applicant Name", value = application.applicantName)
                        DetailRow(label = "Domain", value = application.priorDomain)
                        DetailRow(label = "Experience", value = application.experienceYears)
                        DetailRow(label = "PCC Number", value = application.policeVerificationNumber)
                        DetailRow(label = "Police Station", value = application.policeStation)
                        DetailRow(label = "Territory", value = "${application.operatingCity} (${application.serviceRadiusKm} km radius)")
                        DetailRow(label = "Transport", value = application.vehicleType)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Timeline Tracker
            Text(
                text = "Verification Progress Tracker",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            TimelineStep(
                stepNumber = 1,
                title = "Dossier Submitted",
                subtitle = "Empanelment form, address & PCC details recorded",
                isCompleted = true,
                isActive = false
            )

            TimelineStep(
                stepNumber = 2,
                title = "Police Clearance & Identity Check",
                subtitle = "PCC cross-verification against local police jurisdiction",
                isCompleted = isApproved,
                isActive = !isApproved && !isRejected
            )

            TimelineStep(
                stepNumber = 3,
                title = "Master Admin Approval",
                subtitle = "Final review by App Owner (@satyam_081)",
                isCompleted = isApproved,
                isActive = false
            )

            TimelineStep(
                stepNumber = 4,
                title = "Agent Dispatch Console Activated",
                subtitle = "Physical inspection assignments & daily earnings feed",
                isCompleted = isApproved,
                isActive = false,
                isLast = true
            )

            Spacer(modifier = Modifier.height(30.dp))

            if (isApproved) {
                Button(
                    onClick = onEnterAgentDashboard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = emeraldAccent)
                ) {
                    Text(
                        text = "Enter Agent Dashboard",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else if (isRejected) {
                Button(
                    onClick = onReapply,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                ) {
                    Text(
                        text = "Modify & Re-submit Application",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Admin reviews usually complete within 12 - 24 hours. Once approved, this screen will instantly transition to your Agent Console.",
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF8B949E)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TimelineStep(
    stepNumber: Int,
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    isActive: Boolean,
    isLast: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCompleted -> Color(0xFF10B981)
                            isActive -> Color(0xFFFFB800)
                            else -> Color(0xFF30363D)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = "$stepNumber",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) Color.Black else Color(0xFF8B949E)
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(if (isCompleted) Color(0xFF10B981) else Color(0xFF30363D))
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 18.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = if (isActive || isCompleted) FontWeight.Bold else FontWeight.Normal,
                color = if (isActive || isCompleted) Color.White else Color(0xFF6B7280)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF8B949E),
                lineHeight = 15.sp
            )
        }
    }
}
