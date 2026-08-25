package com.loanzo.app.data.didit

/**
 * Configuration for Didit KYC integration.
 *
 * NOTE FOR PRODUCTION:
 * For maximum security in production, session creation (using CLIENT_SECRET) should ideally
 * happen on your trusted backend server, which returns the session URL/token to the Android app.
 * For development & direct mobile integration, you can place your Didit credentials here.
 */
object DiditConfig {
    // Workflow ID from your Didit Console
    var WORKFLOW_ID: String = "d6badbce-c9f7-4331-ac71-8a24398679f2"

    // Didit API Key / Secret Token
    var API_KEY: String = "TBRycf8XDZ-ZHNQaZd_mvmnB6sY59N1MU4Bok3ltHH4"

    // Client ID (optional)
    var CLIENT_ID: String = ""

    // Didit API Session endpoint
    const val API_SESSION_URL = "https://verification.didit.me/v1/session/"

    // Custom deep link & App Link callback URLs
    const val CALLBACK_SCHEME = "loanzo"
    const val CALLBACK_HOST = "kyc-callback"
    const val REDIRECT_URL = "https://loanzo.app/kyc-callback"
}
