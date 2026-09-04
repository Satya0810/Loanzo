package com.loanzo.app.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.security.MessageDigest

/** Hash password securely using SHA-256 */
fun hashPassword(password: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

/** Format amount in Indian Rupee notation with commas */
fun Double.toInrString(): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.maximumFractionDigits = if (this == this.toLong().toDouble()) 0 else 2
    return format.format(this)
}

/** Format amount without currency symbol */
fun Double.toFormattedString(): String {
    val format = NumberFormat.getNumberInstance(Locale("en", "IN"))
    format.maximumFractionDigits = if (this == this.toLong().toDouble()) 0 else 2
    return format.format(this)
}

/** Format timestamp to readable date string */
fun Long.toDateString(pattern: String = "dd MMM yyyy"): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))
}

/** Format timestamp to readable date-time string */
fun Long.toDateTimeString(): String {
    return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(this))
}

/** Format timestamp to relative time (e.g., "2 days ago") */
fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> toDateString()
    }
}

/** Calculate number of days between two timestamps */
fun daysBetween(from: Long, to: Long): Int {
    return ((to - from) / 86_400_000).toInt()
}

/** Calculate days overdue from a due date */
fun Long.daysOverdue(): Int {
    val now = System.currentTimeMillis()
    return if (now > this) daysBetween(this, now) else 0
}

/** Calculate days until a due date. Positive = days remaining, negative = days overdue */
fun Long.daysUntilDue(): Int {
    val now = System.currentTimeMillis()
    return daysBetween(now, this)
}

/** Generate repayment schedule dates */
fun generateScheduleDates(
    startDate: Long,
    tenureMonths: Int,
    frequency: String
): List<Long> {
    val calendar = Calendar.getInstance().apply { timeInMillis = startDate }
    val dates = mutableListOf<Long>()

    val increment = when (frequency) {
        "WEEKLY" -> Calendar.WEEK_OF_YEAR to 1
        "BI_WEEKLY" -> Calendar.WEEK_OF_YEAR to 2
        "MONTHLY" -> Calendar.MONTH to 1
        else -> Calendar.MONTH to 1
    }

    val totalInstallments = when (frequency) {
        "WEEKLY" -> tenureMonths * 4
        "BI_WEEKLY" -> tenureMonths * 2
        else -> tenureMonths
    }

    repeat(totalInstallments) {
        calendar.add(increment.first, increment.second)
        dates.add(calendar.timeInMillis)
    }
    return dates
}

/** Calculate simple interest */
fun calculateSimpleInterest(principal: Double, rate: Double, tenureMonths: Int): Double {
    return principal * rate * tenureMonths / (12 * 100)
}

/** Calculate EMI (Equated Monthly Installment) for compound interest */
fun calculateEMI(principal: Double, annualRate: Double, tenureMonths: Int): Double {
    if (tenureMonths <= 0) return principal
    if (annualRate <= 0.0) return principal / tenureMonths
    val monthlyRate = annualRate / (12 * 100)
    val factor = Math.pow(1 + monthlyRate, tenureMonths.toDouble())
    if (factor <= 1.0) return principal / tenureMonths
    return principal * monthlyRate * factor / (factor - 1)
}

/**
 * Resolves the profile photo into a model that Coil can actually render.
 * Prioritizes local persistent file storage, handles file URIs, and transforms
 * Google Drive HTML view URLs into direct image stream download URLs.
 */
fun com.loanzo.app.data.entity.UserEntity?.getDisplayProfilePhoto(context: android.content.Context): Any? {
    if (this == null) return null
    // 1. Check local persistent file storage first (instant, works offline, highest quality)
    val localFile = java.io.File(context.filesDir, "profile_${this.userId}.jpg")
    if (localFile.exists() && localFile.length() > 0) {
        return localFile
    }

    // 2. If profilePhotoUri is a file URI or path
    if (this.profilePhotoUri.startsWith("file:") || this.profilePhotoUri.startsWith("/")) {
        val path = this.profilePhotoUri.removePrefix("file://")
        val f = java.io.File(path)
        if (f.exists() && f.length() > 0) return f
    }

    // 3. If profilePhotoUri is a Google Drive link, convert to working direct stream URL
    if (this.profilePhotoUri.contains("drive.google.com")) {
        val fileId = if (this.profilePhotoUri.contains("/d/")) {
            this.profilePhotoUri.substringAfter("/d/").substringBefore("/")
        } else if (this.profilePhotoUri.contains("id=")) {
            this.profilePhotoUri.substringAfter("id=").substringBefore("&")
        } else null
        if (!fileId.isNullOrBlank()) {
            return "https://drive.google.com/uc?export=view&id=$fileId"
        }
    }

    // 4. Any other http/https URL (e.g. Google avatar)
    if (this.profilePhotoUri.isNotBlank()) {
        return this.profilePhotoUri
    }

    return null
}

/**
 * Resolves any KYC document or image URL into a local File or direct image stream.
 */
fun String?.toDisplayDocumentModel(context: android.content.Context, fallbackPrefix: String, userId: String): Any? {
    if (this.isNullOrBlank()) return null
    val localFile = java.io.File(context.filesDir, "${fallbackPrefix}_${userId}.jpg")
    if (localFile.exists() && localFile.length() > 0) return localFile

    if (this.startsWith("file:") || this.startsWith("/")) {
        val f = java.io.File(this.removePrefix("file://"))
        if (f.exists() && f.length() > 0) return f
    }

    if (this.contains("drive.google.com")) {
        val fileId = if (this.contains("/d/")) {
            this.substringAfter("/d/").substringBefore("/")
        } else if (this.contains("id=")) {
            this.substringAfter("id=").substringBefore("&")
        } else null
        if (!fileId.isNullOrBlank()) {
            return "https://drive.google.com/uc?export=view&id=$fileId"
        }
    }
    return this
}
