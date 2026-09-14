package com.loanzo.app.util

import com.loanzo.app.data.dao.VerificationDao
import com.loanzo.app.data.entity.VerificationEntity
import kotlinx.coroutines.flow.Flow
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VerificationManager @Inject constructor(
    private val verificationDao: VerificationDao
) {
    companion object {
        const val APP_OWNER_PHONE = "+917061559039"
        const val OTP_LENGTH = 6

        fun isAppOwner(
            phone: String? = null,
            username: String? = null,
            userId: String? = null,
            email: String? = null
        ): Boolean {
            val u = username?.trim()?.lowercase()?.removePrefix("@") ?: ""
            val uid = userId?.trim()?.lowercase()?.removePrefix("@") ?: ""
            val em = email?.trim()?.lowercase() ?: ""
            val p = phone?.trim()?.removePrefix("+91")?.trim() ?: ""

            // abhisi is strictly the Field Agent, NEVER Admin
            if (u == "abhisi" || uid == "abhisi") {
                return false
            }

            // ONLY satyam0810 is the Platform Admin
            if (u in listOf("satyam0810", "satyam_081", "satyam") ||
                uid in listOf("satyam0810", "satyam_081", "satyam") ||
                em.startsWith("satyam0810") || em.startsWith("satyam_081") || em.startsWith("satyam@loanzo.app") ||
                p == "7061559039" || phone?.trim() == APP_OWNER_PHONE
            ) {
                return true
            }
            return false
        }

        fun isEligibleAppOwner(user: com.loanzo.app.data.entity.UserEntity?): Boolean {
            if (user == null) return false
            val u = user.username.trim().lowercase().removePrefix("@")
            val uid = user.userId.trim().lowercase().removePrefix("@")
            if (u == "abhisi" || uid == "abhisi") return false
            return isAppOwner(
                phone = user.phone,
                username = user.username,
                userId = user.userId,
                email = user.email
            ) || (u in listOf("satyam0810", "satyam_081", "satyam") || user.phone.contains("7061559039"))
        }

        fun isAppOwner(user: com.loanzo.app.data.entity.UserEntity?): Boolean {
            if (user == null) return false
            val u = user.username.trim().lowercase().removePrefix("@")
            val uid = user.userId.trim().lowercase().removePrefix("@")
            if (u == "abhisi" || uid == "abhisi") return false
            val isSatyam = isEligibleAppOwner(user)
            return if (isSatyam) {
                user.role.uppercase() == "ADMIN"
            } else {
                false
            }
        }

        fun isFieldAgent(user: com.loanzo.app.data.entity.UserEntity?): Boolean {
            if (user == null) return false
            val u = user.username.trim().lowercase().removePrefix("@")
            val uid = user.userId.trim().lowercase().removePrefix("@")
            val em = user.email.trim().lowercase()
            val p = user.phone.trim().replace(" ", "").removePrefix("+91").trim()

            // Field Agent account always operates in Field Agent mode
            val isDedicatedAgent = u == "abhisi" || uid == "abhisi" ||
                                   em.startsWith("abhisi") || p == "9810012345"
            if (isDedicatedAgent) return true

            // A user operating in an active Member / Consumer role is strictly NOT in Field Agent mode,
            // regardless of background empanelment status.
            val activeRole = user.role.trim().uppercase()
            if (activeRole in listOf("USER", "MEMBER", "BORROWER", "LENDER")) {
                return false
            }

            // If user is eligible app owner (Satyam), check if his chosen active role is AGENT
            if (isEligibleAppOwner(user) || u in listOf("satyam0810", "satyam_081", "satyam") || p == "7061559039") {
                return activeRole == "AGENT"
            }

            return activeRole == "AGENT"
        }

        fun isFieldAgent(username: String?, userId: String? = null, email: String? = null, phone: String? = null): Boolean {
            val u = username?.trim()?.lowercase()?.removePrefix("@") ?: ""
            val uid = userId?.trim()?.lowercase()?.removePrefix("@") ?: ""
            val em = email?.trim()?.lowercase() ?: ""
            val p = phone?.trim()?.replace(" ", "")?.removePrefix("+91")?.trim() ?: ""

            if (u in listOf("satyam0810", "satyam_081", "satyam") || p == "7061559039") return false
            return u == "abhisi" || uid == "abhisi" || em.startsWith("abhisi") || p == "9810012345"
        }

        fun generateSecureToken(): String {
            val secureRandom = SecureRandom()
            val code = secureRandom.nextInt(900000) + 100000 // 100000 to 999999
            return String.format(java.util.Locale.US, "%06d", code)
        }
    }

    suspend fun createVerificationRequest(phone: String, channel: String): String {
        val token = generateSecureToken()
        val entity = VerificationEntity(
            token = token,
            phone = phone.trim(),
            channel = channel,
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )
        verificationDao.insertVerification(entity)
        return token
    }

    suspend fun verifyToken(token: String, phone: String? = null): Boolean {
        val cleanToken = token.trim()
        val record = verificationDao.getByToken(cleanToken)
        if (record != null) {
            verificationDao.markAsVerified(token = cleanToken, phone = record.phone)
            return true
        }
        if (phone != null) {
            val latest = verificationDao.getLatestForPhone(phone.trim())
            if (latest != null && latest.token == cleanToken) {
                verificationDao.markAsVerified(token = cleanToken, phone = phone.trim())
                return true
            }
        }
        return false
    }

    suspend fun approveVerificationByOwner(token: String, phone: String) {
        verificationDao.markAsVerified(token = token, phone = phone)
    }

    suspend fun isPhoneVerified(phone: String): Boolean {
        val record = verificationDao.getVerifiedByPhone(phone.trim())
        return record != null
    }

    fun getAllVerifications(): Flow<List<VerificationEntity>> {
        return verificationDao.getAllVerifications()
    }
}
