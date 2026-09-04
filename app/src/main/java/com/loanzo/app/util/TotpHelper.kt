package com.loanzo.app.util

import java.net.URLEncoder
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

object TotpHelper {

    private const val BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private const val TIME_STEP_SECONDS = 30L
    private const val CODE_DIGITS = 6

    /**
     * Generates a secure random 16-character Base32 secret key for TOTP setup.
     */
    fun generateSecretKey(): String {
        val random = SecureRandom()
        val bytes = ByteArray(10) // 80 bits is standard for 16 Base32 characters
        random.nextBytes(bytes)
        return encodeBase32(bytes)
    }

    /**
     * Builds standard otpauth URI to open in Microsoft Authenticator or any TOTP app.
     * Example: otpauth://totp/Loanzo:user@email.com?secret=JBSWY3DPEHPK3PXP&issuer=Loanzo
     */
    fun getOtpAuthUri(account: String, secret: String, issuer: String = "Loanzo"): String {
        val encodedIssuer = URLEncoder.encode(issuer, "UTF-8").replace("+", "%20")
        val encodedAccount = URLEncoder.encode(account, "UTF-8").replace("+", "%20")
        return "otpauth://totp/$encodedIssuer:$encodedAccount?secret=$secret&issuer=$encodedIssuer&algorithm=SHA1&digits=$CODE_DIGITS&period=$TIME_STEP_SECONDS"
    }

    /**
     * Verifies a 6-digit TOTP code against a secret key with clock drift tolerance (±1 step).
     */
    fun verifyCode(secret: String, code: String, window: Int = 1): Boolean {
        val cleanCode = code.trim().replace(" ", "")
        if (cleanCode.length != CODE_DIGITS || !cleanCode.all { it.isDigit() }) {
            return false
        }

        val currentCounter = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS
        for (i in -window..window) {
            val expectedCode = generateCodeForCounter(secret, currentCounter + i)
            if (expectedCode == cleanCode) {
                return true
            }
        }
        return false
    }

    /**
     * Generates the current 6-digit TOTP code for the given secret (useful for validation/testing).
     */
    fun generateCurrentCode(secret: String): String {
        val counter = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS
        return generateCodeForCounter(secret, counter)
    }

    private fun generateCodeForCounter(secret: String, counter: Long): String {
        val keyBytes = decodeBase32(secret)
        if (keyBytes.isEmpty()) return ""

        val data = ByteBuffer.allocate(8).putLong(counter).array()
        val mac = Mac.getInstance("HmacSHA1")
        val secretKey = SecretKeySpec(keyBytes, "HmacSHA1")
        mac.init(secretKey)
        val hash = mac.doFinal(data)

        val offset = hash[hash.size - 1].toInt() and 0x0F
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)

        val otp = binary % (10.0.pow(CODE_DIGITS).toInt())
        return String.format("%0${CODE_DIGITS}d", otp)
    }

    private fun encodeBase32(data: ByteArray): String {
        val sb = StringBuilder()
        var buffer = 0
        var bitsLeft = 0

        for (b in data) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                val index = (buffer shr (bitsLeft - 5)) and 0x1F
                sb.append(BASE32_CHARS[index])
                bitsLeft -= 5
            }
        }
        if (bitsLeft > 0) {
            val index = (buffer shl (5 - bitsLeft)) and 0x1F
            sb.append(BASE32_CHARS[index])
        }
        return sb.toString()
    }

    private fun decodeBase32(input: String): ByteArray {
        val cleanInput = input.trim().uppercase().replace("=", "")
        val bytes = mutableListOf<Byte>()
        var buffer = 0
        var bitsLeft = 0

        for (char in cleanInput) {
            val value = BASE32_CHARS.indexOf(char)
            if (value < 0) continue
            buffer = (buffer shl 5) or value
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bytes.add(((buffer shr (bitsLeft - 8)) and 0xFF).toByte())
                bitsLeft -= 8
            }
        }
        return bytes.toByteArray()
    }
}
