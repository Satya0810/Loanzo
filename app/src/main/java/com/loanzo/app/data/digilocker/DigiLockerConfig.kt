package com.loanzo.app.data.digilocker

/**
 * DigiLocker KYC Configuration via Sandbox.co.in API.
 * 
 * Sandbox.co.in acts as a registered DigiLocker partner and handles:
 * - Real UIDAI e-Aadhaar verification with live OTP.
 * - Real Income Tax Department PAN Card fetching and validation.
 * 
 * The backend (Vercel) communicates with Sandbox.co.in API,
 * the Android app only interacts with our backend + the DigiLocker consent URL.
 */
object DigiLockerConfig {
    // Loanzo Backend endpoints (Vercel) — these call Sandbox.co.in internally
    const val BACKEND_BASE_URL = "https://backend-blond-sigma-66.vercel.app"
    const val INIT_SESSION_URL = "$BACKEND_BASE_URL/api/kyc/digilocker/init"
    const val CHECK_STATUS_URL = "$BACKEND_BASE_URL/api/kyc/digilocker/status"
    const val VERIFY_URL = "$BACKEND_BASE_URL/api/kyc/digilocker/verify"

    // Deep link scheme registered in AndroidManifest.xml
    const val REDIRECT_URI = "loanzo://digilocker-callback"
    const val CALLBACK_SCHEME = "loanzo"
    const val CALLBACK_HOST = "digilocker-callback"
}
