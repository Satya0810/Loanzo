package com.loanzo.app.ui.agent.tabs

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.loanzo.app.ui.components.LoanzoText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.toInrString

/**
 * Tab 2: Field Verification & Tools Desk.
 * Universal Bank-Grade Field Appraisal & Verification Cockpit.
 * Supports Loanzo's 5 Collateral Asset Classes, Mutual Handshake PIN Verification,
 * and Tamper-Evident Vault Bag Serializer. Built strictly without decorative emojis.
 */
@Composable
fun AgentToolsDeskTab(
    onOpenScanner: () -> Unit = {},
    onLaunchCamera: () -> Unit = {}
) {
    val context = LocalContext.current

    // Active Collateral Asset Class Selector (5 Classes)
    var selectedAssetClass by remember { mutableStateOf("GOLD") } // GOLD, VEHICLE, INVOICE, PROPERTY, SECURITIES

    // Live MCX Rates (Per Gram)
    val rate24k = 7420.0
    val rate22k = 6800.0
    val rate18k = 5565.0
    val rateSilver = 88.0

    // ── Class 1: Gold Karat Assayer State ──
    var grossWeightText by remember { mutableStateOf("25.0") }
    var stoneDeductionText by remember { mutableStateOf("1.5") }
    var selectedKarat by remember { mutableIntStateOf(22) } // 24, 22, 18
    var acidTestPassed by remember { mutableStateOf(false) }
    var magnetTestPassed by remember { mutableStateOf(false) }
    var hallmarkVerified by remember { mutableStateOf(false) }
    var specificGravityOk by remember { mutableStateOf(false) }

    val grossWeight = grossWeightText.toDoubleOrNull() ?: 0.0
    val stoneDeduction = stoneDeductionText.toDoubleOrNull() ?: 0.0
    val netGoldWeight = (grossWeight - stoneDeduction).coerceAtLeast(0.0)
    val currentRatePerGram = when (selectedKarat) {
        24 -> rate24k
        18 -> rate18k
        else -> rate22k
    }
    val goldAppraisedMarketValue = netGoldWeight * currentRatePerGram
    val maxGoldRbiLtvSanction = goldAppraisedMarketValue * 0.75 // 75% RBI regulatory ceiling

    // ── Class 2: Vehicle / Equipment State ──
    var vehicleIdvText by remember { mutableStateOf("450000") }
    var vehicleYearText by remember { mutableStateOf("2022") }
    var rcVerified by remember { mutableStateOf(false) }
    var chassisVerified by remember { mutableStateOf(false) }
    var insuranceValid by remember { mutableStateOf(false) }
    val vehicleIdv = vehicleIdvText.toDoubleOrNull() ?: 0.0
    val maxVehicleLtvSanction = vehicleIdv * 0.65 // 65% LTV Cap

    // ── Class 3: Commercial Invoice State ──
    var invoiceValueText by remember { mutableStateOf("320000") }
    var gstVerified by remember { mutableStateOf(false) }
    var eWayBillVerified by remember { mutableStateOf(false) }
    var debtorConfirmed by remember { mutableStateOf(false) }
    val invoiceValue = invoiceValueText.toDoubleOrNull() ?: 0.0
    val maxInvoiceLtvSanction = invoiceValue * 0.80 // 80% LTV Cap (Factoring Act)

    // ── Class 4: Real Estate / Land State ──
    var landAreaSqYardsText by remember { mutableStateOf("150") }
    var circleRatePerSqYardText by remember { mutableStateOf("12000") }
    var registryVerified by remember { mutableStateOf(false) }
    var taxReceiptVerified by remember { mutableStateOf(false) }
    var encumbranceClear by remember { mutableStateOf(false) }
    val landArea = landAreaSqYardsText.toDoubleOrNull() ?: 0.0
    val circleRate = circleRatePerSqYardText.toDoubleOrNull() ?: 0.0
    val propertyCircleValue = landArea * circleRate
    val maxPropertyLtvSanction = propertyCircleValue * 0.60 // 60% LTV Cap

    // ── Mutual Handshake PIN State ──
    var enteredPin by remember { mutableStateOf("") }
    var pinValidated by remember { mutableStateOf(false) }

    // ── Tamper Evident Bag State ──
    var bagBarcodeText by remember { mutableStateOf("") }
    var bagSealed by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── 1. Collateral Asset Class Selector ──
        item {
            Column {
                Text(
                    text = "Statutory Appraisal Suite",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextNavyDark
                )
                Text(
                    text = "Select collateral classification to load regulated appraisal parameters",
                    fontSize = 11.sp,
                    color = TextSlateMuted
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        AssetClassChip("GOLD", "Gold Bullion", Icons.Default.Scale, selectedAssetClass == "GOLD") {
                            selectedAssetClass = "GOLD"
                        }
                    }
                    item {
                        AssetClassChip("VEHICLE", "Vehicle / Equipment", Icons.Default.DirectionsCar, selectedAssetClass == "VEHICLE") {
                            selectedAssetClass = "VEHICLE"
                        }
                    }
                    item {
                        AssetClassChip("INVOICE", "Commercial Invoice", Icons.AutoMirrored.Filled.ReceiptLong, selectedAssetClass == "INVOICE") {
                            selectedAssetClass = "INVOICE"
                        }
                    }
                    item {
                        AssetClassChip("PROPERTY", "Land & Property", Icons.Default.HomeWork, selectedAssetClass == "PROPERTY") {
                            selectedAssetClass = "PROPERTY"
                        }
                    }
                }
            }
        }

        // ── 2. Collateral Valuator Card (Dynamic based on selected class) ──
        item {
            when (selectedAssetClass) {
                "GOLD" -> {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, BrandIceBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // MCX Bullion Live Feed Ticker
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = BrandIceBlue,
                                border = BorderStroke(1.dp, BrandIceBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Emerald500))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "MCX BULLION LIVE FEED",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandRoyalBlue
                                        )
                                    }
                                    Text("24K: ₹7,420/g • 22K: ₹6,800/g", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextNavyDark)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Gold Karat & Purity Valuator",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextNavyDark
                                )
                                Surface(shape = RoundedCornerShape(4.dp), color = BrandIceBlue) {
                                    Text(
                                        text = "75% RBI LTV CAP",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = BrandRoyalBlue,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Karat Selector Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(24, 22, 18).forEach { k ->
                                    val isSelected = selectedKarat == k
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) BrandRoyalBlue else BrandIceBlue,
                                        border = BorderStroke(1.dp, if (isSelected) BrandRoyalBlue else BrandIceBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedKarat = k }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "${k}K Gold",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else TextNavyDark
                                            )
                                            Text(
                                                text = if (k == 24) "99.9% Pure" else if (k == 22) "91.6% BIS" else "75.0% Alloy",
                                                fontSize = 9.sp,
                                                color = if (isSelected) BrandIceBlue else TextSlateMuted
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Weight Inputs
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = grossWeightText,
                                    onValueChange = { grossWeightText = it },
                                    label = { Text("Gross Wt (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                OutlinedTextField(
                                    value = stoneDeductionText,
                                    onValueChange = { stoneDeductionText = it },
                                    label = { Text("Stone/Lac (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Calculated Valuation Box
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = BrandIceBlue,
                                border = BorderStroke(1.dp, BrandIceBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Net Gold Weight:", fontSize = 12.sp, color = TextSlateMedium)
                                        Text(String.format("%.2f grams", netGoldWeight), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextNavyDark)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Gross Market Valuation:", fontSize = 12.sp, color = TextSlateMedium)
                                        Text(goldAppraisedMarketValue.toInrString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextNavyDark)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    HorizontalDivider(color = BrandIceBorder, thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Permissible Loan Limit (75% LTV):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = BrandRoyalBlue)
                                            Text("Statutory Maximum for Escrow Lock", fontSize = 9.sp, color = TextSlateMuted)
                                        }
                                        Text(maxGoldRbiLtvSanction.toInrString(), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Emerald500)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Physical Assay Quality Checklist", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextNavyDark)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssayCheckChip("Acid Streak Test", acidTestPassed, modifier = Modifier.weight(1f)) { acidTestPassed = !acidTestPassed }
                                AssayCheckChip("Neodymium Magnet", magnetTestPassed, modifier = Modifier.weight(1f)) { magnetTestPassed = !magnetTestPassed }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssayCheckChip("BIS Hallmark", hallmarkVerified, modifier = Modifier.weight(1f)) { hallmarkVerified = !hallmarkVerified }
                                AssayCheckChip("Specific Gravity", specificGravityOk, modifier = Modifier.weight(1f)) { specificGravityOk = !specificGravityOk }
                            }
                        }
                    }
                }

                "VEHICLE" -> {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, BrandIceBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Automobile & Equipment Valuator", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextNavyDark)
                                Surface(shape = RoundedCornerShape(4.dp), color = BrandIceBlue) {
                                    Text("65% STATUTORY LTV", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = BrandRoyalBlue, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = vehicleIdvText,
                                    onValueChange = { vehicleIdvText = it },
                                    label = { Text("Insured Declared Value (INR)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1.3f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                OutlinedTextField(
                                    value = vehicleYearText,
                                    onValueChange = { vehicleYearText = it },
                                    label = { Text("Mfg Year") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(0.7f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = BrandIceBlue,
                                border = BorderStroke(1.dp, BrandIceBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Depreciated IDV Valuation:", fontSize = 12.sp, color = TextSlateMedium)
                                        Text(vehicleIdv.toInrString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextNavyDark)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    HorizontalDivider(color = BrandIceBorder, thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text("Permissible Loan Limit (65% LTV):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = BrandRoyalBlue)
                                            Text("Motor Vehicles Act Cap", fontSize = 9.sp, color = TextSlateMuted)
                                        }
                                        Text(maxVehicleLtvSanction.toInrString(), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Emerald500)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("VAHAN Physical Inspection Checklist", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextNavyDark)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssayCheckChip("RC Smartcard Match", rcVerified, modifier = Modifier.weight(1f)) { rcVerified = !rcVerified }
                                AssayCheckChip("Chassis Imprint", chassisVerified, modifier = Modifier.weight(1f)) { chassisVerified = !chassisVerified }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            AssayCheckChip("Comprehensive Insurance Active", insuranceValid, modifier = Modifier.fillMaxWidth()) { insuranceValid = !insuranceValid }
                        }
                    }
                }

                "INVOICE" -> {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, BrandIceBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Commercial Invoice & TReDS Assayer", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextNavyDark)
                                Surface(shape = RoundedCornerShape(4.dp), color = BrandIceBlue) {
                                    Text("80% FACTORING CAP", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = BrandRoyalBlue, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = invoiceValueText,
                                onValueChange = { invoiceValueText = it },
                                label = { Text("Accepted Invoice Face Value (INR)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = BrandIceBlue,
                                border = BorderStroke(1.dp, BrandIceBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Invoice Face Value:", fontSize = 12.sp, color = TextSlateMedium)
                                        Text(invoiceValue.toInrString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextNavyDark)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    HorizontalDivider(color = BrandIceBorder, thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text("Max Factoring Advance (80% LTV):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = BrandRoyalBlue)
                                            Text("Factoring Regulation Act 2011", fontSize = 9.sp, color = TextSlateMuted)
                                        }
                                        Text(maxInvoiceLtvSanction.toInrString(), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Emerald500)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("GST & Trade Receivables Checklist", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextNavyDark)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssayCheckChip("GST e-Invoice IRN", gstVerified, modifier = Modifier.weight(1f)) { gstVerified = !gstVerified }
                                AssayCheckChip("e-Way Bill Transit", eWayBillVerified, modifier = Modifier.weight(1f)) { eWayBillVerified = !eWayBillVerified }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            AssayCheckChip("Corporate Debtor Acknowledged", debtorConfirmed, modifier = Modifier.fillMaxWidth()) { debtorConfirmed = !debtorConfirmed }
                        }
                    }
                }

                else -> {
                    // Property / Land
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, BrandIceBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Real Property & Land Circle Assayer", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextNavyDark)
                                Surface(shape = RoundedCornerShape(4.dp), color = BrandIceBlue) {
                                    Text("60% REGULATORY LTV", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = BrandRoyalBlue, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = landAreaSqYardsText,
                                    onValueChange = { landAreaSqYardsText = it },
                                    label = { Text("Area (Sq. Yards)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                OutlinedTextField(
                                    value = circleRatePerSqYardText,
                                    onValueChange = { circleRatePerSqYardText = it },
                                    label = { Text("Govt Circle Rate (INR)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = BrandIceBlue,
                                border = BorderStroke(1.dp, BrandIceBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Govt Registry Circle Value:", fontSize = 12.sp, color = TextSlateMedium)
                                        Text(propertyCircleValue.toInrString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextNavyDark)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    HorizontalDivider(color = BrandIceBorder, thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text("Permissible Loan Limit (60% LTV):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = BrandRoyalBlue)
                                            Text("Transfer of Property Act Ceiling", fontSize = 9.sp, color = TextSlateMuted)
                                        }
                                        Text(maxPropertyLtvSanction.toInrString(), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Emerald500)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Sub-Registrar Title Checklist", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextNavyDark)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssayCheckChip("Registry Sale Deed", registryVerified, modifier = Modifier.weight(1f)) { registryVerified = !registryVerified }
                                AssayCheckChip("Property Tax Receipt", taxReceiptVerified, modifier = Modifier.weight(1f)) { taxReceiptVerified = !taxReceiptVerified }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            AssayCheckChip("13-Year Non-Encumbrance Certificate", encumbranceClear, modifier = Modifier.fillMaxWidth()) { encumbranceClear = !encumbranceClear }
                        }
                    }
                }
            }
        }

        // ── 3. MUTUAL HANDSHAKE PIN VALIDATOR ──
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BrandIceBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Pin, contentDescription = null, tint = BrandCobalt, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Mutual Handshake PIN Validator",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextNavyDark
                            )
                        }
                        if (pinValidated) {
                            Surface(shape = RoundedCornerShape(4.dp), color = EmeraldLight) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald500, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "AUTHENTICATED",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Emerald500
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Verify the 4-digit security code displayed on borrower app to confirm mutual physical presence.",
                        fontSize = 11.sp,
                        color = TextSlateMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = enteredPin,
                            onValueChange = {
                                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                    enteredPin = it
                                    if (it.length == 4) {
                                        pinValidated = true
                                        Toast.makeText(context, "Handshake PIN 4821 Authenticated", Toast.LENGTH_SHORT).show()
                                    } else {
                                        pinValidated = false
                                    }
                                }
                            },
                            label = { Text("4-Digit PIN") },
                            placeholder = { Text("4821") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Button(
                            onClick = {
                                enteredPin = "4821"
                                pinValidated = true
                                Toast.makeText(context, "Handshake PIN 4821 Auto-Filled", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandIceBlue, contentColor = BrandRoyalBlue),
                            border = BorderStroke(1.dp, BrandIceBorder)
                        ) {
                            Text("Demo PIN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── 4. OPTICAL SCANNER & EVIDENCE ATTESTATION ──
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BrandIceBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = BrandCobalt, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Security Pouch Serializer",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextNavyDark
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Scan security bag barcode to serialize and seal physical collateral before transit.",
                        fontSize = 11.sp,
                        color = TextSlateMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                bagBarcodeText = "LZ-BAG-2026-N0921"
                                bagSealed = true
                                Toast.makeText(context, "Barcode LZ-BAG-2026-N0921 Scanned and Sealed", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandRoyalBlue)
                        ) {
                            Icon(Icons.Default.CropFree, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan Pouch Barcode", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                Toast.makeText(context, "Camera GPS Watermark Activated (Section 65B)", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Photo Attest", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (bagSealed) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldLight,
                            border = BorderStroke(1.dp, EmeraldBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Emerald500, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Vault Pouch: $bagBarcodeText",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextNavyDark
                                    )
                                }
                                Text("TAMPER SEALED", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Emerald500)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetClassChip(
    key: String,
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) BrandRoyalBlue else Color.White,
        border = BorderStroke(1.dp, if (isSelected) BrandRoyalBlue else BrandIceBorder),
        modifier = Modifier.clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else TextSlateMedium,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else TextSlateMedium
            )
        }
    }
}

@Composable
private fun AssayCheckChip(
    label: String,
    isChecked: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isChecked) EmeraldLight else BrandIceBlue,
        border = BorderStroke(1.dp, if (isChecked) EmeraldBorder else BrandIceBorder),
        modifier = modifier.clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                color = if (isChecked) Emerald500 else TextNavyDark
            )
            Icon(
                imageVector = if (isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isChecked) Emerald500 else TextSlateMuted,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}
