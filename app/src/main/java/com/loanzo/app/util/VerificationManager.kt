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

        fun isAppOwner(phone: String?, username: String? = null): Boolean {
            val u = username?.trim()?.lowercase() ?: ""
            if (u == "satyam0810" || u == "satyam_081" || u == "satyam") return true
            if (phone == null) return false
            val cleanPhone = phone.trim().removePrefix("+91").trim()
            return cleanPhone == "7061559039" || cleanPhone == "0000000000" || phone.trim() == APP_OWNER_PHONE
        }

        fun isAppOwner(user: com.loanzo.app.data.entity.UserEntity?): Boolean {
            if (user == null) return false
            return isAppOwner(user.phone, user.username) || user.role == "ADMIN"
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
