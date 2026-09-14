package com.loanzo.app.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.loanzo.app.data.entity.MediationMeetingEntity
import com.loanzo.app.ui.theme.*
import java.util.*

@Composable
fun ScheduleMediationDialog(
    initialComplaintId: String? = null,
    initialLoanId: String? = null,
    borrowerName: String? = null,
    borrowerPhone: String? = null,
    lenderName: String? = null,
    lenderPhone: String? = null,
    onSchedule: (MediationMeetingEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var meetingType by remember { mutableStateOf("GOOGLE_MEET") } // GOOGLE_MEET or PHYSICAL_VAULT
    var title by remember { mutableStateOf(if (initialLoanId != null) "Hearing: Loan Restructuring ($initialLoanId)" else "Grievance Mediation Session") }
    var agenda by remember { mutableStateOf("Address dispute, review repayment milestones, and establish binding settlement agreement.") }
    var selectedTimeSlot by remember { mutableStateOf("Today, 04:30 PM - 05:15 PM") }
    var meetLink by remember {
        val randomSuffix = UUID.randomUUID().toString().take(6).lowercase()
        mutableStateOf("https://meet.google.com/lnz-$randomSuffix-med")
    }
    var physicalLocation by remember { mutableStateOf("Loanzo Central Vault, Barakhamba Road, Connaught Place, New Delhi") }

    val timePresets = listOf(
        "Today, 04:30 PM - 05:15 PM",
        "Tomorrow, 11:30 AM - 12:15 PM",
        "Tomorrow, 03:00 PM - 03:45 PM",
        "In 2 Days, 10:00 AM - 11:00 AM"
    )

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
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EmeraldLight,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Emerald400.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "DISPUTE RESOLUTION",
                                    color = Emerald600,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Official Arbitration", color = TextSlateMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Schedule Mediation Hearing",
                            color = TextNavyDark,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(BrandIceBlue)
                    ) {
                        Icon(Icons.Default.Close, null, tint = TextSlateMedium, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Meeting Type Selector
                Text("Hearing Mode", color = TextNavyDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isVideo = meetingType == "GOOGLE_MEET"
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isVideo) BrandIceBlue else CanvasPorcelain),
                        border = androidx.compose.foundation.BorderStroke(if (isVideo) 1.5.dp else 1.dp, if (isVideo) BrandRoyalBlue else BrandIceBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { meetingType = "GOOGLE_MEET" }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.VideoCall, null, tint = if (isVideo) BrandRoyalBlue else TextSlateMedium, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Google Meet", color = TextNavyDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Virtual Video", color = if (isVideo) BrandCobalt else TextSlateMuted, fontSize = 10.sp)
                            }
                        }
                    }

                    val isPhysical = meetingType == "PHYSICAL_VAULT"
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isPhysical) BrandIceBlue else CanvasPorcelain),
                        border = androidx.compose.foundation.BorderStroke(if (isPhysical) 1.5.dp else 1.dp, if (isPhysical) BrandRoyalBlue else BrandIceBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { meetingType = "PHYSICAL_VAULT" }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AccountBalance, null, tint = if (isPhysical) GoldCoinAmber else TextSlateMedium, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Central Vault", color = TextNavyDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("In-Person Office", color = if (isPhysical) BrandCobalt else TextSlateMuted, fontSize = 10.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title & Agenda
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Hearing Title", color = TextSlateMuted, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandCobalt,
                        unfocusedBorderColor = BrandIceBorder,
                        focusedTextColor = TextNavyDark,
                        unfocusedTextColor = TextNavyDark,
                        focusedContainerColor = CanvasPorcelain,
                        unfocusedContainerColor = CanvasPorcelain
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = agenda,
                    onValueChange = { agenda = it },
                    label = { Text("Agenda & Topics to Settle", color = TextSlateMuted, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandCobalt,
                        unfocusedBorderColor = BrandIceBorder,
                        focusedTextColor = TextNavyDark,
                        unfocusedTextColor = TextNavyDark,
                        focusedContainerColor = CanvasPorcelain,
                        unfocusedContainerColor = CanvasPorcelain
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Time Slot Picker
                Text("Select Scheduled Time Slot", color = TextNavyDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    timePresets.forEach { slot ->
                        val isSelected = selectedTimeSlot == slot
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) BrandIceBlue else CanvasPorcelain,
                            border = androidx.compose.foundation.BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) BrandRoyalBlue else BrandIceBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTimeSlot = slot }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AccessTime,
                                        null,
                                        tint = if (isSelected) BrandRoyalBlue else TextSlateMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(slot, color = if (isSelected) TextNavyDark else TextSlateMedium, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, null, tint = BrandRoyalBlue, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Link / Location Details
                if (meetingType == "GOOGLE_MEET") {
                    OutlinedTextField(
                        value = meetLink,
                        onValueChange = { meetLink = it },
                        label = { Text("Google Meet Video Link", color = Emerald500, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.VideoCall, null, tint = Emerald500) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Emerald500,
                            unfocusedBorderColor = BrandIceBorder,
                            focusedTextColor = TextNavyDark,
                            unfocusedTextColor = TextNavyDark,
                            focusedContainerColor = CanvasPorcelain,
                            unfocusedContainerColor = CanvasPorcelain
                        )
                    )
                } else {
                    OutlinedTextField(
                        value = physicalLocation,
                        onValueChange = { physicalLocation = it },
                        label = { Text("Vault Office Location", color = GoldCoinAmber, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Place, null, tint = GoldCoinAmber) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldCoinRich,
                            unfocusedBorderColor = BrandIceBorder,
                            focusedTextColor = TextNavyDark,
                            unfocusedTextColor = TextNavyDark,
                            focusedContainerColor = CanvasPorcelain,
                            unfocusedContainerColor = CanvasPorcelain
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(1f, fill = false))
                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val meetingId = "MEET-" + UUID.randomUUID().toString().take(6).uppercase()
                        val newMeeting = MediationMeetingEntity(
                            meetingId = meetingId,
                            title = title,
                            agenda = agenda,
                            loanId = initialLoanId,
                            complaintId = initialComplaintId,
                            borrowerName = borrowerName ?: "Borrower Party",
                            borrowerPhone = borrowerPhone,
                            lenderName = lenderName ?: "Lender Party",
                            lenderPhone = lenderPhone,
                            meetingType = meetingType,
                            meetingLinkOrLocation = if (meetingType == "GOOGLE_MEET") meetLink else physicalLocation,
                            scheduledDateTime = System.currentTimeMillis() + 86400000L,
                            scheduledTimeSlotStr = selectedTimeSlot,
                            status = "SCHEDULED"
                        )
                        onSchedule(newMeeting)
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Issue Summons & Confirm Hearing",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
