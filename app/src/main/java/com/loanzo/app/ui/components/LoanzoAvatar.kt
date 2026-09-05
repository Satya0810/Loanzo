package com.loanzo.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.loanzo.app.data.entity.UserEntity
import com.loanzo.app.ui.theme.DarkNavy
import com.loanzo.app.ui.theme.Emerald500
import com.loanzo.app.ui.theme.Gold400
import com.loanzo.app.ui.theme.Gold500
import com.loanzo.app.util.getDisplayProfilePhoto
import com.loanzo.app.util.getInitials

/**
 * Universal Loanzo Profile Picture / Avatar Composable.
 * Features:
 * - Direct image stream & local persistent storage resolution
 * - Smooth fallback to UI-Avatars / monogram gradient (NEVER blank or absent)
 * - Optional verified badge (Emerald for KYC verified, Gold for verified member)
 * - Optional camera edit badge for direct photo updates
 */
@Composable
fun LoanzoAvatar(
    user: UserEntity?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    showVerifiedBadge: Boolean = false,
    showEditBadge: Boolean = false,
    borderColor: Color = Gold500.copy(alpha = 0.5f),
    borderWidth: Dp = 1.5.dp,
    onClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val avatarModel = user.getDisplayProfilePhoto(context)
    val initials = user.getInitials()
    val isKycVerified = user?.kycStatus == "VERIFIED"

    val baseModifier = modifier
        .size(size)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Box(
        modifier = baseModifier,
        contentAlignment = Alignment.BottomEnd
    ) {
        // Main Avatar Circle
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(borderWidth, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (avatarModel != null) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(avatarModel)
                        .crossfade(true)
                        .build(),
                    contentDescription = user?.name ?: "Profile Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        MonogramInitialsAvatar(initials = initials, size = size)
                    },
                    error = {
                        MonogramInitialsAvatar(initials = initials, size = size)
                    }
                )
            } else {
                MonogramInitialsAvatar(initials = initials, size = size)
            }
        }

        // Optional Verified Badge
        if (showVerifiedBadge && !showEditBadge) {
            val badgeSize = (size.value * 0.32f).coerceIn(14f, 24f).dp
            Surface(
                shape = CircleShape,
                color = if (isKycVerified) Emerald500 else Gold500,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .size(badgeSize)
                    .offset(x = 1.dp, y = 1.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Verified",
                    tint = Color.White,
                    modifier = Modifier.padding(2.dp)
                )
            }
        }

        // Optional Camera Edit Badge
        if (showEditBadge) {
            val badgeSize = (size.value * 0.34f).coerceIn(22f, 30f).dp
            Surface(
                shape = CircleShape,
                color = Gold500,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .size(badgeSize)
                    .offset(x = 2.dp, y = 2.dp)
                    .then(if (onEditClick != null) Modifier.clickable(onClick = onEditClick) else Modifier)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Change Profile Picture",
                    tint = DarkNavy,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

/**
 * High-end monogram initials fallback with rich obsidian-gold gradient.
 */
@Composable
fun MonogramInitialsAvatar(
    initials: String,
    size: Dp
) {
    val fontSize = (size.value * 0.38f).coerceIn(10f, 32f).sp
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF0F172A), // Slate 900
                        Color(0xFF1E293B)  // Slate 800
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = Gold400,
            letterSpacing = 0.5.sp
        )
    }
}
