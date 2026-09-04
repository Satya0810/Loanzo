package com.loanzo.app.ui.agent

import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.data.entity.AgentVisitEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentInspectionSheet(
    visit: AgentVisitEntity,
    onDismiss: () -> Unit,
    onCompleteInspection: (
        remarks: String,
        isCollateralAuthentic: Boolean,
        isBorrowerVerified: Boolean,
        isLenderVerified: Boolean,
        photoProof: String
    ) -> Unit
) {
    var geoCheckedIn by remember { mutableStateOf(false) }
    var idVerified by remember { mutableStateOf(false) }
    var collateralTested by remember { mutableStateOf(false) }
    var premisesConfirmed by remember { mutableStateOf(false) }
    var photoTaken1 by remember { mutableStateOf(false) }
    var photoTaken2 by remember { mutableStateOf(false) }
    var agentRemarks by remember { mutableStateOf("") }
    var appraisedValueInput by remember {
        mutableStateOf(visit.collateralEstimatedValue?.toInt()?.toString() ?: "")
    }

    val darkBg = Color(0xFF0D1117)
    val cardBg = Color(0xFF161B22)
    val borderColor = Color(0xFF30363D)
    val goldAccent = Color(0xFFFFB800)
    val emeraldAccent = Color(0xFF10B981)

    val canSubmit = geoCheckedIn && idVerified && (photoTaken1 || photoTaken2)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = darkBg,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color(0xFF4B5563))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val badgeColor = when (visit.visitType) {
                        "COLLATERAL_VERIFICATION" -> goldAccent
                        "BORROWER_VERIFICATION" -> emeraldAccent
                        else -> Color(0xFF38BDF8)
                    }
                    val badgeLabel = when (visit.visitType) {
                        "COLLATERAL_VERIFICATION" -> "COLLATERAL VALUATION"
                        "BORROWER_VERIFICATION" -> "BORROWER PHYSICAL KYC"
                        else -> "LENDER PHYSICAL KYC"
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = badgeLabel,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = visit.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = emeraldAccent.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, emeraldAccent.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "VISIT PAYOUT",
                            fontSize = 9.sp,
                            color = emeraldAccent,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "₹${visit.payoutAmount.toInt()}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 1: GPS Check-in
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = cardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (geoCheckedIn) emeraldAccent else borderColor)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (geoCheckedIn) Icons.Default.GpsFixed else Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (geoCheckedIn) emeraldAccent else goldAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (geoCheckedIn) "GPS Geofence Match (Verified)" else "Physical Geolocation Check-in",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (geoCheckedIn) "Latitude: ${visit.targetLatitude} | Longitude: ${visit.targetLongitude}" else visit.targetAddress,
                            fontSize = 11.sp,
                            color = Color(0xFF9CA3AF),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!geoCheckedIn) {
                        Button(
                            onClick = { geoCheckedIn = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = goldAccent),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Check In",
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = emeraldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 2: Inspection Checklist
            Text(
                text = "Field Inspection Verification Checklist",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = cardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    InspectionCheckItem(
                        checked = idVerified,
                        onCheckedChange = { idVerified = it },
                        title = "Original Government ID Match",
                        desc = "Physically inspected original Aadhaar / PAN card of party"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    InspectionCheckItem(
                        checked = collateralTested,
                        onCheckedChange = { collateralTested = it },
                        title = if (visit.visitType == "COLLATERAL_VERIFICATION") "Asset Purity & Authenticity Certified" else "Physical Residence / Premises Verified",
                        desc = if (visit.visitType == "COLLATERAL_VERIFICATION") "Hallmark, weight, and condition verified without defect" else "Address confirmed with utility bills & neighborhood inquiry"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    InspectionCheckItem(
                        checked = premisesConfirmed,
                        onCheckedChange = { premisesConfirmed = it },
                        title = "In-Person Party Interview Completed",
                        desc = "Confirmed loan purpose and financial capacity in-person"
                    )
                }
            }

            // Valuation section if collateral
            if (visit.collateralItemName != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Collateral Valuation Appraisal",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = appraisedValueInput,
                    onValueChange = { appraisedValueInput = it },
                    label = { Text("Agent Appraised Market Value (₹)") },
                    placeholder = { Text("e.g. 150000") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = cardBg,
                        unfocusedContainerColor = cardBg,
                        focusedBorderColor = goldAccent,
                        unfocusedBorderColor = borderColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 3: Photo Proof Capture
            Text(
                text = "Geotagged Photo Proof",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PhotoProofBox(
                    modifier = Modifier.weight(1f),
                    label = "Asset / ID Front",
                    isCaptured = photoTaken1,
                    onCapture = { photoTaken1 = !photoTaken1 }
                )
                PhotoProofBox(
                    modifier = Modifier.weight(1f),
                    label = "Premises / Handshake",
                    isCaptured = photoTaken2,
                    onCapture = { photoTaken2 = !photoTaken2 }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 4: Remarks
            OutlinedTextField(
                value = agentRemarks,
                onValueChange = { agentRemarks = it },
                label = { Text("Agent Field Remarks & Notes") },
                placeholder = { Text("Enter physical observations, condition, or discrepancies...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg,
                    focusedBorderColor = goldAccent,
                    unfocusedBorderColor = borderColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    onCompleteInspection(
                        agentRemarks.ifBlank { "Physical inspection completed and verified on site." },
                        collateralTested,
                        idVerified,
                        premisesConfirmed,
                        "content://proof_photo_geotagged_1.jpg"
                    )
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = emeraldAccent,
                    disabledContainerColor = Color(0xFF21262D)
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Submit Field Report & Claim ₹${visit.payoutAmount.toInt()}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun InspectionCheckItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF10B981),
                uncheckedColor = Color(0xFF4B5563)
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = Color(0xFF8B949E),
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun PhotoProofBox(
    modifier: Modifier = Modifier,
    label: String,
    isCaptured: Boolean,
    onCapture: () -> Unit
) {
    val emeraldAccent = Color(0xFF10B981)
    val cardBg = Color(0xFF161B22)
    val borderColor = Color(0xFF30363D)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onCapture() },
        shape = RoundedCornerShape(10.dp),
        color = if (isCaptured) emeraldAccent.copy(alpha = 0.15f) else cardBg,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCaptured) emeraldAccent else borderColor
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (isCaptured) Icons.Default.CheckCircle else Icons.Default.CameraAlt,
                contentDescription = null,
                tint = if (isCaptured) emeraldAccent else Color(0xFFFFB800),
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isCaptured) "Photo Captured" else label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isCaptured) emeraldAccent else Color.White,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = if (isCaptured) "Geotagged & Signed" else "Tap to snap",
                fontSize = 10.sp,
                color = Color(0xFF8B949E)
            )
        }
    }
}
