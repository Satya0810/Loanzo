package com.loanzo.app.ui.agent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import com.loanzo.app.ui.components.LoanzoText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loanzo.app.ui.theme.*

/**
 * Loanzo Official Field Operations Cockpit — Agent Experience.
 *
 * This screen now consumes state exclusively from [AgentViewModel] via [AgentUiState],
 * following strict unidirectional data flow. All user interactions dispatch through
 * [AgentIntent] sealed intents.
 *
 * Navigation-level callbacks (onNavigateToChat, onLogout, etc.) are still accepted
 * as parameters since they require NavController access which belongs to the NavGraph.
 */
@Composable
fun AgentDashboardScreen(
    viewModel: AgentViewModel = hiltViewModel(),
    onNavigateToChat: (channelId: String, loanId: String?, targetUserId: String?) -> Unit = { _, _, _ -> },
    onNavigateToCommsHub: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onSwitchToConsumer: () -> Unit = {},
    onSwitchToAdmin: () -> Unit = {},
    onLogout: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Dedicated Standalone Agent Scaffold
    AgentScaffold(
        user = state.user,
        visits = state.visits,
        isOnDuty = state.isOnDuty,
        onToggleDutyStatus = { isOnDuty ->
            viewModel.dispatch(AgentIntent.ToggleDutyStatus(isOnDuty))
        },
        onStartInspection = { visit ->
            viewModel.dispatch(AgentIntent.StartInspection(visit))
        },
        onUpdateVisitStage = { visitId, stage ->
            viewModel.dispatch(AgentIntent.UpdateVisitStage(visitId, stage))
        },
        onNavigateToChat = onNavigateToChat,
        onLogout = onLogout,
        onSwitchToConsumer = onSwitchToConsumer,
        onSwitchToAdmin = onSwitchToAdmin
    )

    // Modal In-Person Physical Inspection Attestation Sheet
    state.activeInspectionVisit?.let { visit ->
        AgentInspectionSheet(
            visit = visit,
            onDismiss = {
                viewModel.dispatch(AgentIntent.DismissInspection)
            },
            onCompleteInspection = { remarks, isCollateralAuthentic, isBorrowerVerified, isLenderVerified, photoProof ->
                viewModel.dispatch(
                    AgentIntent.CompleteVisit(
                        visitId = visit.visitId,
                        remarks = remarks,
                        isCollateralAuthentic = isCollateralAuthentic,
                        isBorrowerVerified = isBorrowerVerified,
                        isLenderVerified = isLenderVerified,
                        photoProof = photoProof,
                        appraisedValue = null,
                        officerRecommendation = "RECOMMEND_APPROVAL"
                    )
                )
            },
            onCompleteDetailedInspection = { remarks, isCollateralAuthentic, isBorrowerVerified, isLenderVerified, photoProof, appraisedValue, recommendation ->
                viewModel.dispatch(
                    AgentIntent.CompleteVisit(
                        visitId = visit.visitId,
                        remarks = remarks,
                        isCollateralAuthentic = isCollateralAuthentic,
                        isBorrowerVerified = isBorrowerVerified,
                        isLenderVerified = isLenderVerified,
                        photoProof = photoProof,
                        appraisedValue = appraisedValue,
                        officerRecommendation = recommendation
                    )
                )
            },
            onCompleteDetailedInspectionWithGps = { remarks, isCollateralAuthentic, isBorrowerVerified, isLenderVerified, photoProof, appraisedValue, recommendation, lat, lng ->
                viewModel.dispatch(
                    AgentIntent.CompleteVisitWithGps(
                        visitId = visit.visitId,
                        remarks = remarks,
                        isCollateralAuthentic = isCollateralAuthentic,
                        isBorrowerVerified = isBorrowerVerified,
                        isLenderVerified = isLenderVerified,
                        photoProof = photoProof,
                        appraisedValue = appraisedValue,
                        officerRecommendation = recommendation,
                        latitude = lat,
                        longitude = lng
                    )
                )
            }
        )
    }

    // Payout Confirmation Dialog upon Inspection Attestation
    state.showPayoutSuccessDialog?.let { amount ->
        AlertDialog(
            onDismissRequest = {
                viewModel.dispatch(AgentIntent.DismissPayoutDialog(null))
            },
            containerColor = Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Emerald500,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Attestation Successfully Logged!",
                        color = TextNavyDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Physical verification report and proof have been attested and saved to immutable ledger.",
                        color = TextSlateMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GoldCoinCream,
                        border = BorderStroke(1.dp, GoldCoinBorder)
                    ) {
                        Text(
                            text = "+ INR ${amount.toInt()} Credited to Officer Balance",
                            color = GoldCoinAmber,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dispatch(AgentIntent.DismissPayoutDialog(null))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue)
                ) {
                    Text("Continue Shift", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
