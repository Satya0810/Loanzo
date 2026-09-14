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
import com.loanzo.app.ui.components.LoanzoText as Text
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
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.loanzo.app.util.getDisplayProfilePhoto
import com.loanzo.app.util.getInitials
import com.loanzo.app.util.getEffectiveAvatarUrl
import com.loanzo.app.util.getCartoonAvatarRes
import com.loanzo.app.util.CartoonAvatarHelper

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
    user: UserEntity? = null,
    avatarModel: Any? = null,
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
    val effectivePhoto: Any? = androidx.compose.runtime.remember(avatarModel, user?.userId, user?.profilePhotoUri) {
        if (avatarModel != null) {
            if (avatarModel is String && avatarModel.contains("drive.google.com")) {
                com.loanzo.app.util.convertGoogleDriveUrlToDirectStream(avatarModel)
            } else {
                avatarModel
            }
        } else if (user != null) {
            val localFile = java.io.File(context.filesDir, "profile_${user.userId}.jpg")
            if (localFile.exists() && localFile.length() > 0) {
                localFile
            } else if (user.profilePhotoUri.startsWith("file:") || user.profilePhotoUri.startsWith("/")) {
                val path = user.profilePhotoUri.removePrefix("file://")
                val f = java.io.File(path)
                if (f.exists() && f.length() > 0) f else user.getEffectiveAvatarUrl()
            } else {
                user.getEffectiveAvatarUrl()
            }
        } else {
            CartoonAvatarHelper.getCartoonAvatarUrl("guest")
        }
    }
    val cartoonRes = user?.getCartoonAvatarRes() ?: CartoonAvatarHelper.getCartoonAvatarDrawableRes(avatarModel?.toString() ?: "guest")
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
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(effectivePhoto)
                    .crossfade(true)
                    .build(),
                contentDescription = user?.name ?: "2D Cartoon Avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = {
                    CartoonAvatarFallback(cartoonRes = cartoonRes)
                },
                error = {
                    CartoonAvatarFallback(cartoonRes = cartoonRes)
                }
            )
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
 * 2D Cartoon Person Avatar vector fallback (offline-first, zero-latency).
 */
@Composable
fun CartoonAvatarFallback(
    cartoonRes: Int,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = cartoonRes),
        contentDescription = "2D Cartoon Person Avatar",
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
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
