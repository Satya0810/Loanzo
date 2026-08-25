package com.loanzo.app.data.didit

import com.google.gson.annotations.SerializedName

/**
 * Request payload to create a new Didit Verification Session.
 * Docs: https://docs.didit.me/
 */
data class DiditCreateSessionRequest(
    @SerializedName("workflow_id")
    val workflowId: String? = DiditConfig.WORKFLOW_ID.ifBlank { null },
    @SerializedName("vendor_data")
    val vendorData: String, // e.g. User ID or internal reference
    @SerializedName("callback")
    val callback: String = DiditConfig.REDIRECT_URL,
    @SerializedName("features")
    val features: String? = if (DiditConfig.WORKFLOW_ID.isBlank()) "ocr+liveness+aml" else null
)

/**
 * Response returned from Didit when a session is created.
 */
data class DiditSessionResponse(
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("session_token")
    val sessionToken: String? = null,
    @SerializedName("url")
    val url: String,
    @SerializedName("status")
    val status: String? = "Created"
)

/**
 * Verification decision status returned by Didit.
 */
data class DiditSessionStatusResponse(
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("status")
    val status: String, // Approved, Declined, In Review, Abandoned
    @SerializedName("vendor_data")
    val vendorData: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null
)
