package com.loanzo.app.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import com.loanzo.app.data.entity.CollateralVaultEntity
import com.loanzo.app.ui.theme.*
import java.util.*

@Composable
fun AssignVaultLockerDialog(
    item: CollateralVaultEntity,
    onAssign: (lockerNumber: String, barcodeTag: String, sealNumber: String) -> Unit,
    onDismiss: () -> Unit
) {
    var facilityName by remember { mutableStateOf(item.vaultFacilityName) }
    var lockerNumber by remember { mutableStateOf(if (item.lockerNumber != "PENDING_ALLOCATION") item.lockerNumber else "LOCKER-A" + (10..99).random()) }
    var barcodeTag by remember {
        mutableStateOf(if (item.barcodeTag.isNotBlank()) item.barcodeTag else "LNZ-TAG-" + UUID.randomUUID().toString().take(6).uppercase() + "-DEL")
    }
    var sealNumber by remember {
        mutableStateOf(if (item.tamperSealNumber.isNotBlank()) item.tamperSealNumber else "SEAL-" + (1000000..9999999).random())
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(20.dp)),
            color = Color(0xFF0F172A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
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
                                color = Gold500.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "SAFE VAULT CUSTODY",
                                    color = Gold500,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(item.vaultItemId, color = Gray400, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Assign Locker & Tamper Seal",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Asset Summary
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.assetDescription, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Borrower: ${item.borrowerName} • Est: ₹${item.estimatedValue.toInt()}", color = Gold500, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("Appraisal: ${item.appraisedPurityOrCondition}", color = Gray300, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = facilityName,
                    onValueChange = { facilityName = it },
                    label = { Text("Vault Facility", color = Gray400, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Gold500,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = lockerNumber,
                    onValueChange = { lockerNumber = it },
                    label = { Text("Locker / Safe Bay Number", color = Gold500, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.VpnKey, null, tint = Gold500) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Gold500,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = barcodeTag,
                    onValueChange = { barcodeTag = it },
                    label = { Text("Asset Barcode / QR Tag ID", color = Emerald400, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.QrCode, null, tint = Emerald400) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Emerald400,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = sealNumber,
                    onValueChange = { sealNumber = it },
                    label = { Text("Tamper-Evident Security Seal #", color = Color(0xFFF97316), fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFFF97316)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFF97316),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (lockerNumber.isNotBlank() && barcodeTag.isNotBlank() && sealNumber.isNotBlank()) {
                            onAssign(lockerNumber, barcodeTag, sealNumber)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy900)
                ) {
                    Icon(Icons.Default.Shield, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Seal & Deposit in Vault",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
