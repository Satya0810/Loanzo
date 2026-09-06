package com.loanzo.app.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest
import java.util.Locale

/**
 * Bank-Grade Hardware Device Binding Engine.
 *
 * Implements hardware device fingerprinting compliant with modern Android 10+ (API 29+)
 * and Google Play security policies (which prohibit raw telephony IMEI access for third-party apps).
 * Produces a stable, unique Hardware Device Identifier (Device UID / IMEI Equivalent).
 */
object DeviceSecurityHelper {

    @SuppressLint("HardwareIds")
    fun getHardwareDeviceId(context: Context): String {
        return try {
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "UNKNOWN_DEVICE"

            val hardwareSignature = "${Build.MANUFACTURER}-${Build.MODEL}-${Build.HARDWARE}-${Build.BOARD}"
            val combined = "$androidId:$hardwareSignature"

            // Compute SHA-256 hash and format into an institutional UID
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(combined.toByteArray(Charsets.UTF_8))
            val hexString = hashBytes.joinToString("") { "%02x".format(it) }

            // Clean 16-character device binding token
            "UID-" + hexString.take(12).uppercase(Locale.ROOT)
        } catch (_: Exception) {
            "UID-FALLBACK-" + (Build.MODEL.hashCode().toString()).take(8)
        }
    }

    fun getDeviceModelName(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }
        val model = Build.MODEL
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }
    }

    fun isDeviceMatched(registeredId: String?, currentId: String): Boolean {
        if (registeredId.isNullOrBlank()) return true // Legacy user initial auto-binding
        return registeredId.trim().equals(currentId.trim(), ignoreCase = true)
    }
}
