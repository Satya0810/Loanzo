package com.loanzo.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Centralized registry for Android notification channels across Loanzo.
 * Guarantees uniform sound, vibration, and priority behaviors.
 */
object NotificationChannelHelper {
    const val CHANNEL_ALERTS = "loanzo_alerts"
    const val CHANNEL_ADMIN = "loanzo_admin_alerts"

    fun setupNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        // 1. General User Alerts Channel: Deadlines, repayments, tranches, agreements
        val alertsChannel = NotificationChannel(
            CHANNEL_ALERTS,
            "Loanzo Alerts & Deadlines",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Real-time notifications for EMI deadlines, repayment receipts, and loan lifecycle updates"
            enableVibration(true)
            enableLights(true)
            setShowBadge(true)
        }

        // 2. Admin & Field Agent Channel: Immediate action approvals, empanelments, dispatches
        val adminChannel = NotificationChannel(
            CHANNEL_ADMIN,
            "Admin Approvals & Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "High-priority alerts for Agent Empanelment dossiers, KYC clearance, complaints, and inspection dispatches"
            enableVibration(true)
            enableLights(true)
            setShowBadge(true)
        }

        notificationManager.createNotificationChannels(listOf(alertsChannel, adminChannel))
    }
}
