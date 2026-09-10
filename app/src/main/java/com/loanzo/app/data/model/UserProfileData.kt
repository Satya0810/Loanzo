package com.loanzo.app.data.model

/**
 * Domain model representing a comprehensive, verifiable user profile with
 * dynamic trust score, multi-factor verification badges, and repayment punctuality truth.
 */
data class UserProfileData(
    val userId: String,
    val name: String,
    val username: String,
    val role: String, // "LENDER", "BORROWER", "ADMIN", "AGENT"
    val profilePhotoUri: String? = null,
    val memberSince: Long = System.currentTimeMillis(),
    val verificationStatus: UserVerificationStatus = UserVerificationStatus.NOT_VERIFIED,
    
    // Multi-factor verification indicators
    val isPhoneVerified: Boolean = false,
    val maskedPhone: String = "",
    val isEmailVerified: Boolean = false,
    val maskedEmail: String = "",
    val isKycVerified: Boolean = false,
    val isDigiLockerVerified: Boolean = false,
    val maskedDigiLockerId: String? = null,
    val isBankVerified: Boolean = false,
    
    // Dynamic Trust & Reputation
    val trustScore: Int = 85,
    val trustScoreTier: String = "Tier 2 • Established",
    val repaymentSummary: RepaymentSummary = RepaymentSummary(),
    val trustworthySignals: List<WhyTrustworthySignal> = emptyList(),
    
    val isBlocked: Boolean = false,
    val isOwnProfile: Boolean = false
)

enum class UserVerificationStatus {
    VERIFIED,
    PARTIALLY_VERIFIED,
    NOT_VERIFIED
}

data class RepaymentSummary(
    val completedLoansCount: Int = 0,
    val activeLoansCount: Int = 0,
    val totalAmountRepaid: Double = 0.0,
    val onTimeRepaymentRate: Double = 100.0, // Percentage, e.g. 98.5%
    val defaultsCount: Int = 0,
    val punctualEmisCount: Int = 0,
    val delayedEmisCount: Int = 0
)

data class WhyTrustworthySignal(
    val id: String,
    val title: String,
    val description: String,
    val isPassed: Boolean,
    val iconType: String, // "SHIELD", "PAYMENT", "DIGILOCKER", "IDENTITY", "COMMUNITY"
    val badgeText: String
)
