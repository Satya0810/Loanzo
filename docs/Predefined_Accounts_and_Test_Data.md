# 👥 Loanzo Presentation Accounts, Credentials & Multi-Role Test Data

> Complete evaluation reference guide for demonstrating the 4 core operational roles (**Lender, Borrower, Co-Borrower, Guarantor**) in **Loanzo** during presentations and evaluations.

---

## 🔐 1. Presentation Credentials Summary

All 4 accounts are pre-seeded in the local Room Database and automatically primed on startup.

| Account Name | Username / Login ID | Password | Primary Role | Secondary Roles Demonstrated | KYC Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Devesh Singh** | **`devesh123`** | `Devesh@0810` | **`LENDER`** | Capital Provider, Syndicate Pool Bidder, Vouch Endorser | `VERIFIED` ✅ |
| **Romesh Singh** | **`romesh321`** | `Romesh@0810` | **`CO-BORROWER`** | Joint Legal Obligor (§128 ICA), Guarantor, Field Officer | `VERIFIED` ✅ |
| **Surya Yadav** | **`surya111`** | `Surya@0810` | **`BORROWER` / `AGENT`** | MSME Borrower, Field Verification Officer, 5-Class Pledger | `VERIFIED` ✅ |
| **Suman Yadav** | **`suman321`** | `Suman@0810` | **`BORROWER`** | Fully Repaid Borrower, Golden NOC Holder, Guarantor | `VERIFIED` ✅ |

> [!TIP]
> You can log into any account using their **username** (e.g. `devesh123`), **phone number** (e.g. `9876512340`), or **email** (e.g. `devesh123@loanzo.app`).

---

## 💼 2. User Profiles & Banking Information

### A. Devesh Singh (`@devesh123`) — The Capital Provider (Lender)
- **Role**: `LENDER`
- **Email**: `devesh123@loanzo.app` | **Phone**: `+91 98765 12340`
- **Address**: Plot 24, Sector 62, Noida, UP 201309
- **Aadhaar**: `5412 8765 1234` | **PAN**: `DVSSH1234K`
- **Bank**: HDFC Bank A/C `5010049281928` (IFSC: `HDFC0001234`)
- **UPI ID**: `devesh123@okhdfc`
- **Trust Score**: 98/100
- **Portfolio Summary**: ₹1,50,000 deployed across 2 loans; 12.0% net yield; ₹77,600 principal recovered to date.

### B. Romesh Singh (`@romesh321`) — Co-Borrower & Guarantor
- **Role**: `CO-BORROWER` & `GUARANTOR` (Member)
- **Email**: `romesh321@loanzo.app` | **Phone**: `+91 98234 56789`
- **Address**: Flat 402, Sector 18, Noida, UP 201301
- **Aadhaar**: `6789 0123 4567` | **PAN**: `RMSGH3210R`
- **Bank**: ICICI Bank A/C `1029384756102` (IFSC: `ICIC0002938`)
- **UPI ID**: `romesh321@okicici`
- **Trust Score**: 94/100
- **Key Legal Function**: Jointly and severally liable under **Section 128 Indian Contract Act (ICA)** for Shivam's ₹1,00,000 loan; formal guarantor on Suman's agricultural loan.

### C. Surya Yadav (`@surya111`) — Dual Role: MSME Borrower & Accredited Field Agent
- **Role**: `BORROWER` & `FIELD AGENT` (Dual-Role Enabled)
- **Email**: `surya111@loanzo.app` | **Phone**: `+91 98123 45678`
- **Address**: Weavers Colony, Chowk, Varanasi, UP 221001
- **Aadhaar**: `9012 3456 7890` | **PAN**: `SHVYD8811P`
- **Bank**: State Bank of India A/C `39840219482` (IFSC: `SBIN0005423`)
- **UPI ID**: `surya111@oksbi`
- **Trust Score**: 91/100
- **Borrower Facility**: ₹1,00,000 MSME loan from Devesh Singh; 3 EMIs paid; 22K Gold & IT equipment in central vault.
- **Agent Facility**: Empanelled Field Verification Officer (`agentStatus = APPROVED`), `isOnDuty = true`, ₹4,500 cumulative field earnings.
- **Agent Itinerary**: Pre-seeded with 4 active stops across Noida & Delhi NCR, plus unassigned stops in Master Admin queue.

### D. Suman Yadav (`@suman321`) — Fully Repaid Borrower & Golden NOC
- **Role**: `BORROWER` & `GUARANTOR`
- **Email**: `suman321@loanzo.app` | **Phone**: `+91 98345 67890`
- **Address**: Krishi Vikas Kendra, Barabanki, UP 225001
- **Aadhaar**: `3456 7890 1234` | **PAN**: `SMNYD3219M`
- **Bank**: Punjab National Bank A/C `4019283746501` (IFSC: `PUNB0012340`)
- **UPI ID**: `suman321@okpnb`
- **Trust Score**: 96/100
- **Demonstration Milestone**: 100% completed loan (`loan_suman_agri_50k`); automated **Golden NOC Certificate** issued; active marketplace post with bids.

---

## 📊 3. Active Demonstration Loans & Tranches

### Loan 1: Shivam Yadav — MSME Working Capital (`loan_shivam_msme_100k`)
- **Borrower**: `@surya111` (Shivam Yadav)
- **Lender**: `@devesh123` (Devesh Singh)
- **Co-Borrower**: `@romesh321` (Romesh Singh — Section 128 ICA Joint Obligor)
- **Guarantor**: `@suman321` (Suman Yadav — Social Guarantor)
- **Sanctioned Amount**: ₹1,00,000.00 | **Disbursed**: ₹50,000.00 | **Outstanding**: ₹43,400.00
- **Terms**: 12.5% Simple Interest, 12 Months
- **Purpose-Bound Escrow Tranche 1**:
  - Amount: ₹50,000.00 disbursed **directly to verified supplier UPI VPA** (`supplier.textiles@okaxis`, Bharat Tex Fabrics Ltd) upon invoice OCR validation. **0% cash leakage**.
  - Tranche 2: ₹50,000.00 locked in milestone escrow pending fabric processing completion.
- **Collateral in 5-Class Vault**:
  - `vault_shivam_gold_01`: 22K Hallmarked Gold (30g), Value: ₹1,50,000 (75% LTV), Locker #42, Barcode `LZ-GLD-7721`.
  - `pledge_shivam_it`: Precision Workstation (Value: ₹40,000, 50% LTV).
- **Repayment Track Record**:
  - EMI 1, 2, 3: ₹9,200 each — **PAID** on time via UPI.
  - EMI 4: ₹9,200 — **SCHEDULED** (Upcoming in 14 days).
  - EMI 5..12: ₹9,200 — **SCHEDULED**.

### Loan 2: Suman Yadav — Solar Irrigation Pump (`loan_suman_agri_50k`)
- **Borrower**: `@suman321` (Suman Yadav)
- **Lender**: `@devesh123` (Devesh Singh)
- **Guarantor**: `@romesh321` (Romesh Singh)
- **Sanctioned & Disbursed**: ₹50,000.00 | **Outstanding**: ₹0.00 (Fully Settled)
- **Repayment**: All 6 EMIs **PAID IN FULL** (Total Repaid: ₹52,625).
- **Status**: `COMPLETED` / `CLOSED` ✅
- **Automated Golden NOC**: Issued with SHA-256 digital signature hash (`7f83b1657ff1fc53b92dc181...`).

### Loan 3: Romesh Singh — Field Toolkit & Sensors (`loan_romesh_tech_35k`)
- **Borrower**: `@romesh321` (Romesh Singh)
- **Lender**: `@devesh123` (Devesh Singh)
- **Co-Borrower**: `@suman321` (Suman Yadav)
- **Guarantor**: `@surya111` (Shivam Yadav)
- **Sanctioned & Disbursed**: ₹35,000.00 | **Outstanding**: ₹28,700.00
- **Purpose**: Field Inspection Equipment & Solar Radiance Toolkit
- **Direct Payee**: Precision Instruments India Ltd (`orders.precision@okaxis`)
- **Pledge**: Optical Spectrometer & Digital Density Scale (Vault: `FIELD-RIG-09`)
- **Repayment**: 2 EMIs Paid (₹3,150 each), 1 Upcoming, 9 Scheduled.
- **Status**: `ACTIVE_SERVICING`

---

## 📂 4. Document Vault & Legal Certificates (Profile Tab)

Every user has authentic tamper-evident PDFs in their **Document Vault** (accessible directly from **Profile**):

| Account | Seeded Documents in Vault | Document Type | Key Legal / Regulatory Feature |
| :--- | :--- | :--- | :--- |
| **Devesh Singh** (`devesh123`) | `Dossier_Devesh_Lending_2026.pdf` | Financial Dossier | Capital reserve allocation & audit report |
| | `Sanction_Shivam_MSME_100K.pdf` | Sanction Letter | Official sanction advice & escrow milestones |
| | `Agreement_Shivam_Devesh_100K.pdf` | Loan Agreement | E-Contract & Section 4 Promissory Note |
| | `Golden_NOC_Suman_Devesh.pdf` | NOC Certificate | Discharge acknowledgment for Suman's loan |
| | `Form16A_TDS_Devesh_2026.pdf` | Account Statement | Quarterly Section 194A TDS certificate |
| **Romesh Singh** (`romesh321`) | `Field_Officer_Credential_Romesh.pdf` | Financial Dossier | Accredited Senior Field Verification Officer ID |
| | `Joint_Obligor_Guarantee_Romesh.pdf` | Loan Agreement | Section 128 ICA Joint Obligor agreement |
| | `Agreement_Romesh_Precision_Tech.pdf` | Loan Agreement | Equipment toolkit loan contract |
| | `Custody_Intake_Protocol_Noida.pdf` | Financial Dossier | Dual-Officer physical intake custody protocol |
| **Shivam Yadav** (`surya111`) | `DigiLocker_KYC_Dossier_Shivam.pdf` | Financial Dossier | UIDAI Aadhaar & NSDL PAN DigiLocker snapshot |
| | `Promissory_Note_Agreement_Shivam.pdf` | Loan Agreement | Signed Section 4 Promissory Note (₹1,00,000) |
| | `Disbursement_Tranche1_BharatTex.pdf` | Payment Receipt | Direct UPI disbursement voucher to Bharat Tex |
| | `Gold_Assay_Valuation_Certificate.pdf` | Financial Dossier | Certified 22K gold assayer appraisal report |
| | `Receipt_EMI3_Shivam_MSME.pdf` | Payment Receipt | UPI installment #3 clearance receipt |
| **Suman Yadav** (`suman321`) | `Golden_NOC_Certificate_Suman.pdf` | NOC Certificate | Debt-free Golden NOC with SHA-256 seal 🏆 |
| | `WDRA_ENWR_Lien_Discharge_Suman.pdf` | Financial Dossier | WDRA electronic repository lien discharge |
| | `Loan_Full_Closure_Certificate_Suman.pdf` | Account Statement | Formal account closure advice |
| | `Full_Repayment_Statement_Suman.pdf` | Account Statement | Complete 6-month on-time repayment ledger |

---

## 🕵️‍♂️ 5. Field Agent Operations & Dual-OTP Security System

Loanzo enforces a **Bank-Grade Dual-OTP Verification Protocol** for field agent workflows:

### A. Dual-OTP Protocol Architecture
1. **Agent Shift Activation OTP (6 Digits)**:
   - When an agent toggles "Duty: On", an institutional security modal opens (`AgentShiftOtpDialog`).
   - Requires entering the 6-digit authorization OTP sent to the agent's registered phone number (`+91 98123 45678`).
   - Quick-Fill Demo OTP: **`882190`** (tap-to-fill available in modal).
   - Once verified, GPS tracking, real-time itinerary updates, and dispatch capabilities unlock.
2. **Doorstep Physical Presence Handshake OTP (4 Digits)**:
   - Displayed **only** on the Borrower/Party screen (`FieldInspectionTrackerCard` in Loan Details).
   - When the agent arrives at the premises, they open the verification sheet (`AgentInspectionSheet`).
   - The agent requests the 4-digit code shown on the borrower's screen and enters it into Step 1.
   - Verification unlocks Step 2 (Statutory KYC & Aadhaar QR) and prevents fraudulent off-site check-ins.

### B. Pre-Seeded Field Stops for Surya Yadav (`surya111`)
| Stop ID | Target Borrower / User | Visit Type | Address | Stage | Handshake OTP | Payout |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **`visit_surya_01`** | Romesh Singh (`romesh321`) | Collateral Verification (24K Gold Bullion) | C-18, Sector 62, Noida, UP | `ARRIVED` | **`8821`** | ₹1,200 |
| **`visit_surya_02`** | Suman Yadav (`suman321`) | Borrower KYC & Workplace Verification | Plot 45, Indirapuram, Ghaziabad, UP | `SCHEDULED` | **`4190`** | ₹850 |
| **`visit_surya_03`** | Devesh Singh (`devesh123`) | Warehouse Inventory Audit | B-4, Okhla Phase III, New Delhi | `COMPLETED` | **`7721`** | ₹1,500 |
| **`visit_surya_04`** | Romesh Singh (`romesh321`) | Land Title Demarcation | Sector 1, Greater Noida West, UP | `SCHEDULED` | **`9023`** | ₹1,800 |
| **`visit_unassigned_noida`** | Devesh Singh (`devesh123`) | Electronics Consignment Inspection | Sector 18, Noida, UP | `PENDING_ASSIGNMENT` | **`6201`** | ₹1,400 |

### C. Legacy Officer Romesh Singh (`romesh321`)
- **Application Status**: `APPROVED` (3-5 Years Banking/MFI experience, verified DL, police clearance).
- **Earnings**: ₹3,200 total bounty credited.
- **Completed Visit 1**: Physical verification and 22K gold assay of Varanasi weaving workshop (Verified, Handshake PIN authenticated).
- **Completed Visit 2**: Physical verification of Barabanki farmland and solar irrigation pump (Verified, running plot confirmed).

---

## 💬 6. Chat Hub & Live Peer Dialogue

Real, role-tailored conversation history is primed across all channels:
- **Shivam & Devesh** (`direct_devesh123_surya111` & `loan_loan_shivam_msme_100k`): Discussion on invoice verification, raw fabric delivery, Jacquard loom production, and EMI #3 payment.
- **Suman & Devesh** (`direct_devesh123_suman321` & `loan_loan_suman_agri_50k`): Discussion on Golden NOC clearance and Devesh's ₹75,000 syndicate bid on Suman's Dairy Chiller post.
- **Romesh & Devesh** (`direct_devesh123_romesh321`): Updates on workshop assay report upload and equipment toolkit activation.
- **Loanzo AI Assistant** (`support_loanzo_assistant`): 24/7 Smart Legal Escrow assistant explaining Section 4 Promissory Notes and Section 171 collateral lien discharge.

---

## 🎫 7. Support & Mediation Center

- **Devesh Singh**: Syndicate Lender Registration & TDS Certificate Schedule (**RESOLVED**, 5-star rating).
- **Shivam Yadav**: Milestone 2 Raw Material Supplier Advance Verification (**IN_PROGRESS**).
- **Romesh Singh**: Field Agent GPS Geofence Check-in Calibration (**RESOLVED**, 5-star rating).
- **Suman Yadav**: WDRA e-NWR Electronic Warehouse Receipt Lien Discharge (**RESOLVED**, 5-star rating).
- **Mediation**: Amicable 5-day harvest grace period resolution between Suman Yadav and Devesh Singh (**RESOLVED / COMPLETED**).

---

## 🛒 8. Community Marketplace, Bids & Social Vouches

1. **Suman's Dairy Chiller Request (`post_suman_dairy_75k`)**:
   - **Author**: Suman Yadav (`@suman321`)
   - **Title**: *"Dairy Bulk Milk Chiller & Solar Setup"*
   - **Amount**: ₹50,000 – ₹75,000 @ 11.5% APR
   - **Co-Borrower**: Romesh Singh (`@romesh321`)
   - **Active Bid**: Devesh Singh (`@devesh123`) placed a bid offering ₹75,000 @ 11.5% APR with direct vendor disbursement terms.
   - **Vouches**: Vouched by Devesh Singh (*Past Repayment Record*) and Romesh Singh (*Community Reference*).

2. **Devesh's Syndicate Lending Pool (`post_devesh_lending_pool`)**:
   - **Author**: Devesh Singh (`@devesh123`)
   - **Title**: *"Verified MSME & Agricultural Working Capital Syndicate"*
   - **Pool Size**: Up to ₹2,00,000 @ 12.0% Simple APR.

---

## 🎯 9. Comprehensive Presentation Walkthrough Matrix

| Screen / Feature | What Devesh Singh (`devesh123`) Sees | What Romesh Singh (`romesh321`) Sees | What Shivam Yadav (`surya111`) Sees | What Suman Yadav (`suman321`) Sees |
| :--- | :--- | :--- | :--- | :--- |
| **Home Dashboard** | ₹1,85,000 deployed across 3 loans; ₹83,900 recovered; active syndicate pool | Personal loan active + Field Agent earnings (₹3,200) + co-borrower status | Active MSME loan (₹1,00,000) with 3 paid EMIs + upcoming EMI reminder | 100% Repaid status + Golden NOC badge + active marketplace bid |
| **Loans Tab** | 3 Loans in Lent tab (Shivam MSME, Suman Agri, Romesh Tech) | Personal equipment loan in Borrowed tab + joint obligor on Shivam's loan | MSME loan in Borrowed tab (Active Servicing) + Guarantor on Romesh loan | Completed loan in Borrowed tab (Debt-Free) + Co-Borrower on Romesh loan |
| **Loan Details** | Full escrow milestones, OCR verified invoices, 5-class collateral, repayments | Direct vendor UPI transfer to Precision Instruments; 22K gold assay | Bharat Tex direct UPI payment; 22K Gold in Central Locker #42 | Kisan Agro solar pump direct payment; WDRA e-NWR wheat lien discharged |
| **Document Vault** | 5 Documents (Master Dossier, Sanction Letter, E-Contract, NOC, Form 16A) | 4 Documents (Field Officer Credential, Joint Obligor Agreement, Tech Agreement) | 5 Documents (DigiLocker KYC, Promissory Note, Tranche 1 Receipt, Gold Cert, EMI 3) | 4 Documents (Golden NOC Certificate 🏆, WDRA Lien Discharge, Closure Advice) |
| **Field Agent Portal** | N/A (Investor view) | Approved Agent dashboard, 2 completed physical inspections, ₹3,200 payout | View inspection report verified by Officer Romesh Singh | View farmland verification verified by Officer Romesh Singh |
| **Chat Hub** | Active conversations with Shivam, Suman, Romesh, and Loanzo Bot | Active conversations with Devesh, Shivam, and Loanzo Bot | Active conversations with Devesh, Romesh, and Loanzo Bot | Active conversations with Devesh and Loanzo Bot |
| **Help & Support** | Resolved TDS & syndicate registration ticket (5 stars) | Resolved rural GPS geofencing ticket (5 stars) | In-progress Milestone 2 yarn advance ticket | Resolved WDRA lien release ticket (5 stars) |
| **Marketplace** | Active liquidity pool post + ₹75,000 bid on Suman's dairy chiller | Community vouch for Suman + Co-borrower guarantee | Community browsing and supplier discovery | Active Dairy Chiller post (₹75k) with 2 vouches and Devesh's bid |
