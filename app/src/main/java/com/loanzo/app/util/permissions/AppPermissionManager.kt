package com.loanzo.app.util.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sms
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat

data class AppPermissionItem(
    val id: String,
    val permissions: List<String>,
    val title: String,
    val purpose: String,
    val icon: ImageVector,
    val isMandatory: Boolean,
    val isGranted: Boolean
)

object AppPermissionManager {

    const val PERM_CAMERA = "camera"
    const val PERM_NOTIFICATIONS = "notifications"
    const val PERM_SMS = "sms"

    /**
     * Inspects current runtime status of all key permissions used in the application.
     */
    fun getAppPermissions(context: Context): List<AppPermissionItem> {
        val list = mutableListOf<AppPermissionItem>()

        // 1. Camera (KYC Liveness & Document Capture)
        val cameraGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        list.add(
            AppPermissionItem(
                id = PERM_CAMERA,
                permissions = listOf(Manifest.permission.CAMERA),
                title = "Camera Access",
                purpose = "Required for statutory KYC selfie verification, CameraX facial liveness detection, and QR repayments.",
                icon = Icons.Default.CameraAlt,
                isMandatory = true,
                isGranted = cameraGranted
            )
        )

        // 2. Notifications (API 33+ Post Notifications)
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Auto-granted below Android 13
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(
                AppPermissionItem(
                    id = PERM_NOTIFICATIONS,
                    permissions = listOf(Manifest.permission.POST_NOTIFICATIONS),
                    title = "Push Notifications",
                    purpose = "Delivers instant P2P chat messages, counterparty loan offers, and timely EMI due reminders.",
                    icon = Icons.Default.Notifications,
                    isMandatory = true,
                    isGranted = notifGranted
                )
            )
        }

        // 3. SMS Banking (Automatic UTR & OTP Reconciliation)
        val receiveSms = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        val readSms = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        val smsGranted = receiveSms && readSms

        list.add(
            AppPermissionItem(
                id = PERM_SMS,
                permissions = listOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS),
                title = "Financial SMS & UTR Parsing",
                purpose = "Enables automatic bank payment confirmation & 1-tap OTP verification. Personal chats are never accessed.",
                icon = Icons.Default.Sms,
                isMandatory = false,
                isGranted = smsGranted
            )
        )

        return list
    }

    /**
     * Checks if all essential core permissions (Camera & Notifications) are currently granted.
     */
    fun hasAllEssentialPermissions(context: Context): Boolean {
        val permissions = getAppPermissions(context)
        return permissions.filter { it.isMandatory }.all { it.isGranted }
    }

    /**
     * Collects all flat permission strings that are currently pending grant.
     */
    fun getPendingPermissionsList(context: Context): Array<String> {
        return getAppPermissions(context)
            .filter { !it.isGranted }
            .flatMap { it.permissions }
            .distinct()
            .toTypedArray()
    }

    /**
     * Opens the app's system details settings screen so users can manually grant denied permissions.
     */
    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
