package com.loanzo.app.ui.auth

import androidx.compose.ui.res.stringResource
import com.loanzo.app.R

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loanzo.app.data.entity.UserEntity
import com.loanzo.app.ui.components.GlassCard
import com.loanzo.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: UserEntity?,
    onNavigateToKyc: () -> Unit,
    onNavigateToTranslation: () -> Unit,
    themeMode: String,
    onSetThemeMode: (String) -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        if (user == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Surface(
                shape = CircleShape,
                color = Gold500.copy(alpha = 0.2f),
                modifier = Modifier.size(96.dp)
            ) {
                Icon(
                    Icons.Default.Person,
                    null,
                    tint = Gold500,
                    modifier = Modifier.padding(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(user.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(user.role, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(8.dp))

            // KYC status badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = when (user.kycStatus) {
                    "VERIFIED" -> Emerald400
                    "IN_PROGRESS" -> Gold500
                    "REJECTED" -> Red400
                    else -> Gray400
                }.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (user.kycStatus == "VERIFIED") Icons.Default.VerifiedUser
                        else Icons.Default.Shield,
                        null,
                        tint = when (user.kycStatus) {
                            "VERIFIED" -> Emerald400
                            "IN_PROGRESS" -> Gold500
                            else -> Gray400
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "KYC: ${user.kycStatus}",
                        fontWeight = FontWeight.SemiBold,
                        color = when (user.kycStatus) {
                            "VERIFIED" -> Emerald400
                            "IN_PROGRESS" -> Gold500
                            else -> Gray400
                        }
                    )
                }
            }

            if (user.kycStatus != "VERIFIED") {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onNavigateToKyc,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Navy900)
                ) {
                    Text(stringResource(R.string.complete_kyc), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Profile details
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                ProfileRow(Icons.Default.Email, "Email", user.email)
                ProfileRow(Icons.Default.Phone, "Phone", user.phone)
                if (user.panNumber.isNotBlank())
                    ProfileRow(Icons.Default.CreditCard, "PAN", user.panNumber)
                if (user.upiId.isNotBlank())
                    ProfileRow(Icons.Default.Payment, "UPI ID", user.upiId)
                ProfileRow(Icons.Default.Verified, "Aadhaar",
                    if (user.aadhaarVerified) "Verified ✓" else "Not verified")
                ProfileRow(Icons.Default.CameraAlt, "Selfie",
                    if (user.selfieVerified) "Verified ✓" else "Not verified")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Theme Toggle
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Appearance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val options = listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark")
                    options.forEachIndexed { index, (key, label) ->
                        SegmentedButton(
                            selected = themeMode == key,
                            onClick = { onSetThemeMode(key) },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size)
                        ) {
                            Text(label)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI Translation Demo button
            OutlinedButton(
                onClick = onNavigateToTranslation,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Translate, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Translation Demo", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Logout button
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Red400)
            ) {
                Icon(Icons.Default.Logout, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.logout), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProfileRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}
