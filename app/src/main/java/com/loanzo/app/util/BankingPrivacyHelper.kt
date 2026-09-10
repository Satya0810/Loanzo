package com.loanzo.app.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Enables or disables banking privacy (FLAG_SECURE) on the activity window.
 * When enabled, screenshots/screen recordings are blocked and the app switcher
 * thumbnail is blanked out to prevent visual eavesdropping.
 */
fun Activity.setBankingPrivacy(enabled: Boolean) {
    if (enabled) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}

/**
 * Helper to unwrap ContextWrapper to find parent Activity.
 */
fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/**
 * Composable lifecycle effect that automatically enforces [WindowManager.LayoutParams.FLAG_SECURE]
 * while this screen is active, and safely releases it upon exit/navigation.
 *
 * Recommended on:
 * - Document Vault & Collateral viewing
 * - KYC Document uploads (Aadhaar/PAN preview)
 * - Loan Agreement review & eSign screens
 */
@Composable
fun SecureScreenEffect(enabled: Boolean = true) {
    val context = LocalContext.current
    DisposableEffect(enabled) {
        val activity = context.findActivity()
        if (enabled) {
            activity?.setBankingPrivacy(true)
        }
        onDispose {
            if (enabled) {
                activity?.setBankingPrivacy(false)
            }
        }
    }
}
