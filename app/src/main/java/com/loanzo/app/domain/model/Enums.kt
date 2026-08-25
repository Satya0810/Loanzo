package com.loanzo.app.domain.model

/** User roles in the Loanzo platform */
enum class UserRole { BORROWER, LENDER }

/** KYC verification status */
enum class KycStatus { PENDING, IN_PROGRESS, VERIFIED, REJECTED }

/** Loan lifecycle status */
enum class LoanStatus { DRAFT, ACTIVE, CLOSED, DEFAULTED }

/** Loan type / purpose category */
enum class LoanType { PERSONAL, BUSINESS, EDUCATION, MEDICAL, AGRICULTURE, OTHER }

/** Interest calculation model */
enum class InterestModel { SIMPLE, COMPOUND, FLAT, NONE }

/** Repayment frequency */
enum class RepaymentFrequency { MONTHLY, WEEKLY, BI_WEEKLY, CUSTOM }

/** Repayment installment status */
enum class RepaymentStatus { SCHEDULED, PAID, OVERDUE, PARTIAL }

/** Payee verification status */
enum class PayeeVerificationStatus { VERIFIED, UNVERIFIED, PENDING }

/** Business / purpose category for payees and disbursements */
enum class PurposeCategory(val displayName: String) {
    HOSPITAL("Hospital / Clinic"),
    PHARMACY("Pharmacy"),
    EDUCATION("Educational Institution"),
    ELECTRONICS("Electronics Store"),
    GROCERY("Grocery / Provisions"),
    RENT("Rent / Housing"),
    UTILITY("Utilities"),
    CONSTRUCTION("Construction / Materials"),
    AGRICULTURE("Agriculture / Farm Supplies"),
    TRANSPORT("Transport / Vehicle"),
    BUSINESS("Business / Trade"),
    OTHER("Other / Miscellaneous")
}

/** Disbursement verification status */
enum class VerificationStatus { PENDING, VERIFIED, FLAGGED, MISMATCH, UNVERIFIED }

/** Disbursement approval status */
enum class ApprovalStatus { PENDING, APPROVED, REJECTED, CLARIFICATION_NEEDED }

/** Rule engine evaluation result */
enum class RuleResult(val displayName: String, val description: String) {
    CONSISTENT("Consistent", "Payee verified and purpose matches category"),
    MISMATCH("Mismatch", "Purpose category differs from payee category — lender review required"),
    UNVERIFIED("Unverified", "Payee cannot be verified — lender review required"),
    AUTO_APPROVED("Auto-Approved", "Low amount, verified payee, no mismatch, below threshold"),
    BLOCKED("Blocked", "Amount exceeds remaining sanctioned limit"),
    REVIEW("Review", "Repeated or inconsistent pattern detected — manual review")
}

/** Pledge asset type */
enum class AssetType { GOLD, PROPERTY, VEHICLE, EQUIPMENT, OTHER }

/** Pledge receipt status */
enum class ReceiptStatus { SUBMITTED, VERIFIED, PENDING }

/** Audit event types */
enum class AuditEventType {
    CREATED, UPDATED, STATUS_CHANGED, APPROVED, REJECTED,
    VERIFIED, DISBURSED, REPAID, FLAGGED, CLOSED
}
