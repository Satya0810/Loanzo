package com.loanzo.app.ui.admin

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
import com.loanzo.app.ui.components.LoanzoText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.loanzo.app.ui.theme.*

@Composable
fun DocumentInspectionDialog(
    docTitle: String,
    docCategory: String, // "USER_KYC" or "AGENT_EMPANELMENT"
    subjectName: String,
    subjectPhone: String,
    documentNumber: String,
    issuingAuthority: String,
    photoUri: String?,
    onApprove: () -> Unit,
    onReject: (reason: String) -> Unit,
    onDismiss: () -> Unit
) {
    var checkNameMatch by remember { mutableStateOf(true) }
    var checkValidityActive by remember { mutableStateOf(true) }
    var checkNoTampering by remember { mutableStateOf(true) }
    var checkClarityReadable by remember { mutableStateOf(true) }
    var rejectionReason by remember { mutableStateOf("") }
    var showRejectInput by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(20.dp)),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, BrandIceBorder),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (docCategory == "AGENT_EMPANELMENT") GoldCoinCream else EmeraldLight,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (docCategory == "AGENT_EMPANELMENT") GoldCoinBorder else Emerald400.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = if (docCategory == "AGENT_EMPANELMENT") "AGENT EMPANELMENT" else "USER KYC AUDIT",
                                    color = if (docCategory == "AGENT_EMPANELMENT") GoldCoinAmber else Emerald600,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Bank-Grade Inspector",
                                color = TextSlateMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = docTitle,
                            color = TextNavyDark,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BrandIceBlue)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSlateMedium, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // High-Res Document Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CanvasPorcelain)
                        .border(1.dp, BrandIceBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val validUri = photoUri?.ifBlank { null }
                    if (validUri != null) {
                        AsyncImage(
                            model = validUri,
                            contentDescription = docTitle,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = BrandCobalt,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Digital Document Attestation on File",
                                color = TextNavyDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Doc ID: $documentNumber",
                                color = GoldCoinAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Watermark / Tamper check badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Security, null, tint = Emerald400, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tamper-Proof Audit Lock", color = Color(0xFF34D399), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Metadata Details Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CanvasPorcelain),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandIceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Applicant Name", color = TextSlateMuted, fontSize = 12.sp)
                            Text(subjectName, color = TextNavyDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Contact Number", color = TextSlateMuted, fontSize = 12.sp)
                            Text(subjectPhone, color = TextNavyDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Document / Certificate #", color = TextSlateMuted, fontSize = 12.sp)
                            Text(documentNumber, color = GoldCoinAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Issuing Authority", color = TextSlateMuted, fontSize = 12.sp)
                            Text(issuingAuthority, color = TextNavyDark, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Verification Checklist
                Text(
                    text = "Mandatory Audit Verification Checklist",
                    color = TextNavyDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                ChecklistRow("Name & Details Match National Records", checkNameMatch) { checkNameMatch = it }
                ChecklistRow("Document Validity is Active & Non-Expired", checkValidityActive) { checkValidityActive = it }
                ChecklistRow("Zero Alteration, Tampering, or Blurring", checkNoTampering) { checkNoTampering = it }
                ChecklistRow("All Holograms, Watermarks, and Signatures Visible", checkClarityReadable) { checkClarityReadable = it }

                if (showRejectInput) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        label = { Text("Reason for Rejection / Deficiency Notice", color = Red500, fontSize = 12.sp) },
                        placeholder = { Text("e.g. Station seal missing, please re-upload clear copy", color = TextSlateMuted, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Red500,
                            unfocusedBorderColor = Red500.copy(alpha = 0.5f),
                            focusedTextColor = TextNavyDark,
                            unfocusedTextColor = TextNavyDark,
                            focusedContainerColor = RedLight,
                            unfocusedContainerColor = RedLight
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(1f, fill = false))
                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!showRejectInput) {
                        OutlinedButton(
                            onClick = { showRejectInput = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Red500),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Red500)
                        ) {
                            Text("Reject", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (rejectionReason.isNotBlank()) {
                                    onReject(rejectionReason)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            enabled = rejectionReason.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Red500, contentColor = Color.White)
                        ) {
                            Text("Confirm Reject", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onApprove,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.4f),
                        enabled = checkNameMatch && checkValidityActive && checkNoTampering && checkClarityReadable,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Verified, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verify & Attest", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = BrandCobalt,
                uncheckedColor = Gray400,
                checkmarkColor = Color.White
            )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = if (checked) TextNavyDark else TextSlateMuted,
            fontSize = 11.sp,
            fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
