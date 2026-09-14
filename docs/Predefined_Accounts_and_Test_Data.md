# 👥 Loanzo Pre-configured Accounts, Credentials & Test Data

> Complete reference guide for testing, verification, and live evaluation of the 4 operational user roles within **Loanzo**.

---

## 🔐 1. Account Credentials Summary

All 4 accounts are pre-seeded in the local Room Database and synced with Cloud Firestore. Passwords are encrypted using SHA-256 with instantaneous authentication.

| Username | Role | Password | Display Name | Phone | KYC Status | Primary Function |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **`kumar`** | `MEMBER` (`BORROWER`) | `Manish@0810` | Kumar Manish | `+91 98765 43210` | `VERIFIED` | MSME Retail Borrower |
| **`prince25`** | `MEMBER` (`BORROWER`) | `1234567890` | Prince Sharma | `+91 98321 65498` | `VERIFIED` | Education & Upskilling Borrower |
| **`abhisi`** | `AGENT` (`FIELD AGENT`) | `Satyam@0810` | Abhisi | `+91 98100 12345` | `VERIFIED` | Certified Field Verification Officer |
| **`satyam0810`** | `ADMIN` (`SUPER ADMIN`) | `Satyam@0810` | Satyam Kumar | `+91 70615 59039` | `VERIFIED` | Platform Master Admin & Lead Capital Provider |

> [!TIP]
> Each account can also be logged into using their phone number or email address (e.g. `kumar@loanzo.app`, `prince25@loanzo.app`, `abhisi@loanzo.app`, `satyam@loanzo.app`).

---

## 💼 2. User Profiles & Banking Information

### A. Kumar Manish (`@kumar`)
- **Role**: Member (Borrower)
- **Email**: `kumar@loanzo.app` | **Phone**: `+91 98765 43210`
- **Address**: Plot 14, Sector 62, Noida, UP 201309
- **Aadhaar**: `6521 9843 1205` (Verified) | **PAN**: `BNMPK8841F` (Verified)
- **Bank**: HDFC Bank A/C `5010084729103` (IFSC: `HDFC0001024`, Verified)
- **UPI**: `kumar.manish@okhdfc`
- **Trust Score**: 92/100

### B. Prince Sharma (`@prince25`)
- **Role**: Member (Borrower)
- **Email**: `prince25@loanzo.app` | **Phone**: `+91 98321 65498`
- **Address**: Flat 304, Green Glen Layout, Bellandur, Bengaluru, KA 560103
- **Aadhaar**: `7823 4512 9034` (Verified) | **PAN**: `CKTPS9234R` (Verified)
- **Bank**: State Bank of India A/C `39840219482` (IFSC: `SBIN0005423`, Verified)
- **UPI**: `prince.sharma@oksbi`
- **Trust Score**: 89/100

### C. Abhisi (`@abhisi`)
- **Role**: Field Agent (Senior Field Verification Specialist)
- **Status**: `APPROVED` | **Duty**: On-Duty (`isOnDuty = true`)
- **Total Earnings**: ₹8,450.00
- **Email**: `abhisi@loanzo.app` | **Phone**: `+91 98100 12345`
- **Address**: C-42, Sector 18, Noida, UP 201301
- **Aadhaar**: `3219 8765 4321` | **PAN**: `BKPVS4521R`
- **Bank**: SBI A/C `3094829104821` (IFSC: `SBIN0001122`)
- **UPI**: `abhisi.agent@oksbi`

### D. Satyam Kumar (`@satyam0810`)
- **Role**: Master Admin & Capital Provider
- **Email**: `satyam@loanzo.app` | **Phone**: `+91 70615 59039`
- **Address**: B-204, Prateek Laurel, Sector 120, Noida, UP 201301
- **Aadhaar**: `4532 1098 7654` | **PAN**: `ADMKR7892L`
- **Bank**: HDFC Bank A/C `5010049281928` (IFSC: `HDFC0001234`)
- **UPI**: `satyam0810@okhdfc`
- **Capital Deployed**: ₹1,25,000.00 across 2 active loans

---

## 📊 3. Active Loans & Repayment Schedules

### Loan 1: Kumar Manish — Inventory Expansion (`loan_kumar_biz_50k`)
- **Borrower**: `@kumar` | **Lender**: `@satyam0810`
- **Sanctioned & Disbursed**: ₹50,000.00 | **Outstanding**: ₹38,500.00
- **Category**: `BUSINESS` | **Terms**: 11.5% Simple Interest, 12 Months
- **Disbursement Ref**: `TXN_DISB_LZ_KUMAR_50K`
- **Repayment Status**:
  - `repay_kumar_1`: ₹4,427.00 — **PAID** (Ref: `TXN_LZ_KUMAR_01`)
  - `repay_kumar_2`: ₹4,427.00 — **PAID** (Ref: `TXN_LZ_KUMAR_02`)
  - `repay_kumar_3`: ₹4,427.00 — **PAID** (Ref: `TXN_LZ_KUMAR_03`)
  - `repay_kumar_4`: ₹4,427.00 — **SCHEDULED** (Upcoming due in 29 days)
  - `repay_kumar_5..12`: ₹4,427.00 — **SCHEDULED**

### Loan 2: Prince Sharma — Cloud Certification (`loan_prince_edu_75k`)
- **Borrower**: `@prince25` | **Lender**: `@satyam0810`
- **Sanctioned & Disbursed**: ₹75,000.00 | **Outstanding**: ₹52,000.00
- **Category**: `EDUCATION` | **Terms**: 9.5% Simple Interest, 18 Months
- **Disbursement Ref**: `TXN_DISB_LZ_PRINCE_75K`
- **Repayment Status**:
  - `repay_prince_1..5`: ₹4,490.00 each — **PAID**
  - `repay_prince_6`: ₹4,490.00 — **SCHEDULED** (Upcoming due in 15 days)
  - `repay_prince_7..18`: ₹4,490.00 — **SCHEDULED**

---

## 📍 4. Field Agent Inspections & Assignments (`@abhisi`)

1. **Completed Business Site Audit (`visit_abhisi_kumar_completed`)**:
   - **Target**: Kumar Manish (`@kumar`), Sector 62, Noida
   - **Coordinates**: `28.6280° N, 77.3780° E`
   - **Type**: `BORROWER_VERIFICATION`
   - **Status**: `COMPLETED` ✅
   - **Payout Earned**: ₹450.00
   - **Remarks**: *"Physical verification complete. Shop trade certificate and stock physically inspected. Verified genuine borrower."*

2. **Scheduled Residence Verification (`visit_abhisi_prince_scheduled`)**:
   - **Target**: Prince Sharma (`@prince25`), Bellandur, Bengaluru
   - **Coordinates**: `12.9260° N, 77.6762° E`
   - **Type**: `BORROWER_VERIFICATION`
   - **Status**: `SCHEDULED` ⏳
   - **Handshake PIN**: `4821` (Displayed on borrower's app)
   - **Payout**: ₹500.00

---

## 🛒 5. Community Marketplace Posts & Active Bids

1. **Kumar's Expansion Request (`post_kumar_equipment`)**:
   - **Author**: Kumar Manish (`@kumar`)
   - **Title**: *"Expansion Loan for POS Hardware & Storage"*
   - **Amount**: ₹80,000 – ₹1,20,000 @ 10.5% (12 Months)
   - **Active Bid**: `@satyam0810` offered ₹1,20,000 @ 10.5% (Status: `PENDING`)

2. **Prince's Workstation Financing (`post_prince_laptop`)**:
   - **Author**: Prince Sharma (`@prince25`)
   - **Title**: *"Workstation Hardware Financing"*
   - **Amount**: ₹30,000 – ₹35,000 @ 9.0% (6 Months)
   - **Active Bid**: `@satyam0810` offered ₹35,000 @ 9.0% (Status: `PENDING`)

---

## 🗄️ 6. Document Vault & Generated PDF Documents

Real, production-grade PDF documents are automatically rendered on device storage (`context.filesDir/vault_documents/`) with official Loanzo crests, KPI tiles, legal covenants, e-Signatures, and SHA-256 verification seals:

| Document File Name | Owner | Document Type | Verification SHA-256 Checksum |
| :--- | :--- | :--- | :--- |
| `Sanction_Letter_Kumar_50k.pdf` | `@kumar` | `SANCTION_LETTER` | `b7a4892c908f12d8a4392f44001bcde6...` |
| `Agreement_Kumar_Manish.pdf` | `@kumar` | `LOAN_AGREEMENT` | `cf83ac10879e96f18546523910f54511...` |
| `Sanction_Letter_Prince_75k.pdf` | `@prince25` | `SANCTION_LETTER` | `89acde1058204910fbc239104820dbe4...` |
| `Agreement_Prince_Sharma.pdf` | `@prince25` | `LOAN_AGREEMENT` | `19203847561928374650192837465019...` |

These documents can be viewed, downloaded, or shared directly from the **Document Vault** tab in the user profile.
