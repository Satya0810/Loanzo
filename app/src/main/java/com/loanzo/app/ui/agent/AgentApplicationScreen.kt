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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.loanzo.app.data.entity.AgentApplicationEntity
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentApplicationScreen(
    userId: String,
    userName: String,
    userPhone: String,
    userEmail: String,
    onNavigateBack: () -> Unit,
    onSubmitSuccess: () -> Unit,
    onSubmitApplication: suspend (AgentApplicationEntity) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Section 1: Experience
    val experienceOptions = listOf("1-2 Years", "3-5 Years", "5+ Years")
    var selectedExperience by remember { mutableStateOf(experienceOptions[1]) }

    val domainOptions = listOf(
        "Banking / NBFC Field Officer",
        "Microfinance (MFI) Credit Officer",
        "Certified Gold Valuer & Appraiser",
        "Real Estate & Vehicle Appraiser",
        "Legal Recovery & Verification"
    )
    var selectedDomain by remember { mutableStateOf(domainOptions[0]) }

    // Section 2: Police Verification
    var pccNumber by remember { mutableStateOf("") }
    var policeStation by remember { mutableStateOf("") }
    var pccDate by remember { mutableStateOf("") }
    var pccUploaded by remember { mutableStateOf(false) }

    // Section 3: Territory & Transport
    var permanentAddress by remember { mutableStateOf("") }
    var operatingCity by remember { mutableStateOf("") }
    var operatingPincode by remember { mutableStateOf("") }
    var serviceRadiusKm by remember { mutableFloatStateOf(15f) }

    val transportOptions = listOf("Two-Wheeler", "Four-Wheeler", "Public Transit")
    var selectedTransport by remember { mutableStateOf(transportOptions[0]) }
    var dlNumber by remember { mutableStateOf("") }

    // Section 4: Legal Undertaking
    var decl1 by remember { mutableStateOf(false) }
    var decl2 by remember { mutableStateOf(false) }
    var decl3 by remember { mutableStateOf(false) }

    val darkBg = Color(0xFF0D1117)
    val cardBg = Color(0xFF161B22)
    val borderColor = Color(0xFF30363D)
    val goldAccent = Color(0xFFFFB800)
    val emeraldAccent = Color(0xFF10B981)

    val isFormValid = pccNumber.isNotBlank() &&
            policeStation.isNotBlank() &&
            pccDate.isNotBlank() &&
            permanentAddress.isNotBlank() &&
            operatingCity.isNotBlank() &&
            operatingPincode.isNotBlank() &&
            decl1 && decl2 && decl3

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Agent Empanelment",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "Bank-Grade Field Officer Application",
                            fontSize = 11.sp,
                            color = goldAccent,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
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
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            // Empanelment Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, goldAccent.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFFD97706), goldAccent))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Official Empanelment Dossier",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Applicant: $userName (${if (userPhone.isNotBlank()) userPhone else userEmail})",
                            fontSize = 12.sp,
                            color = Color(0xFF9CA3AF),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 1: Professional Experience
            SectionHeader(
                stepNumber = "1",
                title = "Professional Experience & Domain",
                subtitle = "Appraisal, banking, or field verification background"
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Years of Field Experience",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE5E7EB)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                experienceOptions.forEach { exp ->
                    val selected = selectedExperience == exp
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedExperience = exp },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) goldAccent.copy(alpha = 0.2f) else cardBg,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selected) goldAccent else borderColor
                        )
                    ) {
                        Text(
                            text = exp,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) goldAccent else Color(0xFFD1D5DB),
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Primary Appraisal Domain",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE5E7EB)
            )
            Spacer(modifier = Modifier.height(8.dp))
            domainOptions.forEach { domain ->
                val selected = selectedDomain == domain
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedDomain = domain },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) Color(0xFF1E293B) else cardBg,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selected) Color(0xFF38BDF8) else borderColor
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { selectedDomain = domain },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF38BDF8),
                                unselectedColor = Color(0xFF4B5563)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = domain,
                            fontSize = 13.sp,
                            color = if (selected) Color.White else Color(0xFF9CA3AF),
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 2: Police Clearance (PCC)
            SectionHeader(
                stepNumber = "2",
                title = "Police Verification & Background (PCC)",
                subtitle = "Mandatory for banking & collateral custody empanelment"
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = pccNumber,
                onValueChange = { pccNumber = it },
                label = { Text("Police Clearance Certificate (PCC) Number") },
                placeholder = { Text("e.g. PCC/DL/2026/89421") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = textFieldColors(cardBg, borderColor, goldAccent),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = policeStation,
                onValueChange = { policeStation = it },
                label = { Text("Issuing Police Station Name") },
                placeholder = { Text("e.g. Sector 18 Police Station, Noida") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = textFieldColors(cardBg, borderColor, goldAccent),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = pccDate,
                onValueChange = { pccDate = it },
                label = { Text("PCC Issue Date (DD/MM/YYYY)") },
                placeholder = { Text("e.g. 15/08/2026") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = textFieldColors(cardBg, borderColor, goldAccent),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { pccUploaded = !pccUploaded },
                shape = RoundedCornerShape(10.dp),
                color = if (pccUploaded) emeraldAccent.copy(alpha = 0.15f) else cardBg,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (pccUploaded) emeraldAccent else borderColor
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (pccUploaded) Icons.Default.CheckCircle else Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = if (pccUploaded) emeraldAccent else goldAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (pccUploaded) "PCC Document Attached (Verified)" else "Upload PCC Document Copy (PDF / JPG)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = if (pccUploaded) "Original police seal & signature detected" else "Tap to attach digital certificate copy",
                            fontSize = 11.sp,
                            color = Color(0xFF9CA3AF),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 3: Territory & Transport
            SectionHeader(
                stepNumber = "3",
                title = "Permanent Address & Service Territory",
                subtitle = "Locality coverage for physical visit dispatch"
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = permanentAddress,
                onValueChange = { permanentAddress = it },
                label = { Text("Permanent Residential Address") },
                placeholder = { Text("House/Flat No, Street, Landmark") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = textFieldColors(cardBg, borderColor, goldAccent),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = operatingCity,
                    onValueChange = { operatingCity = it },
                    label = { Text("Operating City") },
                    placeholder = { Text("e.g. New Delhi") },
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(10.dp),
                    colors = textFieldColors(cardBg, borderColor, goldAccent),
                    singleLine = true
                )

                OutlinedTextField(
                    value = operatingPincode,
                    onValueChange = { if (it.length <= 6) operatingPincode = it },
                    label = { Text("Pincode") },
                    placeholder = { Text("110001") },
                    modifier = Modifier.weight(0.8f),
                    shape = RoundedCornerShape(10.dp),
                    colors = textFieldColors(cardBg, borderColor, goldAccent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Service Radius: ${serviceRadiusKm.toInt()} km",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE5E7EB)
            )
            Slider(
                value = serviceRadiusKm,
                onValueChange = { serviceRadiusKm = it },
                valueRange = 5f..30f,
                steps = 4,
                colors = SliderDefaults.colors(
                    thumbColor = goldAccent,
                    activeTrackColor = goldAccent,
                    inactiveTrackColor = borderColor
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Primary Mode of Field Transport",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE5E7EB)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                transportOptions.forEach { transport ->
                    val selected = selectedTransport == transport
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedTransport = transport },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) goldAccent.copy(alpha = 0.2f) else cardBg,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selected) goldAccent else borderColor
                        )
                    ) {
                        Text(
                            text = transport,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) goldAccent else Color(0xFFD1D5DB),
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = dlNumber,
                onValueChange = { dlNumber = it },
                label = { Text("Driving License (DL) Number") },
                placeholder = { Text("e.g. DL-0420230012345") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = textFieldColors(cardBg, borderColor, goldAccent),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 4: Legal Undertaking
            SectionHeader(
                stepNumber = "4",
                title = "Legal Undertaking & Banking Declaration",
                subtitle = "Binding legal code under IPC & Banking Regulation Norms"
            )

            Spacer(modifier = Modifier.height(12.dp))

            LegalCheckbox(
                checked = decl1,
                onCheckedChange = { decl1 = it },
                text = "I solemnly declare that no criminal case, financial fraud inquiry, or IPC complaint is registered or pending against me."
            )

            LegalCheckbox(
                checked = decl2,
                onCheckedChange = { decl2 = it },
                text = "I agree to strict physical asset appraisal standards, continuous GPS geo-fencing during scheduled visits, and spontaneous supervisor audits."
            )

            LegalCheckbox(
                checked = decl3,
                onCheckedChange = { decl3 = it },
                text = "I understand that any collusive, fraudulent, or inaccurate inspection report will result in immediate disqualification, forfeiture of balance, and prosecution."
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = Color(0xFFEF4444),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Submit Button
            Button(
                onClick = {
                    if (!isFormValid) {
                        errorMessage = "Please fill all required fields and accept legal declarations."
                        return@Button
                    }
                    errorMessage = null
                    isSubmitting = true

                    val application = AgentApplicationEntity(
                        applicationId = "AGENT-APP-" + UUID.randomUUID().toString().take(8).uppercase(),
                        userId = userId,
                        applicantName = userName,
                        applicantPhone = userPhone,
                        applicantEmail = userEmail,
                        experienceYears = selectedExperience,
                        priorDomain = selectedDomain,
                        policeVerificationNumber = pccNumber,
                        policeStation = policeStation,
                        policeVerificationDate = pccDate,
                        policeDocUri = if (pccUploaded) "content://police_clearance_verified" else "",
                        permanentAddress = permanentAddress,
                        operatingCity = operatingCity,
                        operatingPincode = operatingPincode,
                        serviceRadiusKm = serviceRadiusKm.toInt(),
                        vehicleType = selectedTransport,
                        drivingLicenseNumber = dlNumber,
                        status = "PENDING"
                    )

                    coroutineScope.launch {
                        try {
                            onSubmitApplication(application)
                            isSubmitting = false
                            onSubmitSuccess()
                        } catch (e: Exception) {
                            isSubmitting = false
                            errorMessage = e.localizedMessage ?: "Failed to submit application. Try again."
                        }
                    }
                },
                enabled = isFormValid && !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = goldAccent,
                    disabledContainerColor = Color(0xFF21262D)
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Submit Empanelment Dossier to Master Admin",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(stepNumber: String, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFF38BDF8)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                color = Color.Black,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF8B949E),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun LegalCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFFFFB800),
                uncheckedColor = Color(0xFF4B5563)
            )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = Color(0xFFD1D5DB),
            lineHeight = 16.sp,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun textFieldColors(container: Color, border: Color, focused: Color) =
    OutlinedTextFieldDefaults.colors(
        focusedContainerColor = container,
        unfocusedContainerColor = container,
        focusedBorderColor = focused,
        unfocusedBorderColor = border,
        focusedLabelColor = focused,
        unfocusedLabelColor = Color(0xFF8B949E),
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        cursorColor = focused
    )
