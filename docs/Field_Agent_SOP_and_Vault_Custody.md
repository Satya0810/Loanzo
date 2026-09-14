# 🕵️ Field Agent Standard Operating Procedure (SOP) & Institutional Vault Custody

<div align="center">

### **Doorstep Physical Verification, Gold Assaying & Custodial Protocol**
*Standard Operating Procedure (SOP) governing Certified Field Valuers, doorstep asset appraisal, tamper-evident barcode sealing, bank safe deposit custody, and de-hypothecation.*

---

[![Role Isolation](https://img.shields.io/badge/Security-Strict%20Role%20Isolation%20Gate-D32F2F.svg?style=for-the-badge)](../app/src/main/java/com/loanzo/app/ui/navigation/NavGraph.kt)
[![Assaying Standard](https://img.shields.io/badge/Assaying-Calibrated%20Jeweler%20Scales-FFD700.svg?style=for-the-badge)](../app/src/main/java/com/loanzo/app/ui/agent/AgentVisitInspectionScreen.kt)
[![Tamper-Proof](https://img.shields.io/badge/Sealing-Serialized%20Barcode%20Bags-00796B.svg?style=for-the-badge)](../app/src/main/java/com/loanzo/app/ui/admin/VaultTab.kt)
[![Custody](https://img.shields.io/badge/Vault-Dual--Key%20Bank%20Deposit-1976D2.svg?style=for-the-badge)](../app/src/main/java/com/loanzo/app/data/entity/CollateralVaultEntity.kt)

</div>

---

## 📑 Table of Contents
1. [Empanelment & Police Clearance Verification (PCC)](#1-empanelment--police-clearance-verification-pcc)
2. [Strict Role Isolation Gate (`Routes.AGENT_MAIN`)](#2-strict-role-isolation-gate-routesagent_main)
3. [Field Cockpit Architecture & Compensation Engine](#3-field-cockpit-architecture--compensation-engine)
4. [Doorstep Precious Metals Assaying Protocol](#4-doorstep-precious-metals-assaying-protocol)
5. [Tamper-Evident Bagging & Dual-Signature Sealing](#5-tamper-evident-bagging--dual-signature-sealing)
6. [Institutional Bank Vault Custody & CERSAI Lien](#6-institutional-bank-vault-custody--cersai-lien)
7. [Loan Settlement & De-Hypothecation Restitution Protocol](#7-loan-settlement--de-hypothecation-restitution-protocol)

---

## 1. Empanelment & Police Clearance Verification (PCC)

To guarantee counterparty trust and eliminate insider fraud, all Loanzo Field Agents undergo a rigorous multi-factor accreditation process before receiving physical inspection dispatch privileges.

```mermaid
graph TD
    classDef step fill:#EFF6FF,stroke:#3B82F6,stroke-width:2px,color:#1E3A8A;
    classDef gate fill:#FEF3C7,stroke:#D97706,stroke-width:2px,color:#78350F;
    classDef pass fill:#ECFDF5,stroke:#059669,stroke-width:2px,color:#064E3B;

    A[User Completes DigiLocker KYC]:::step --> B[Submits Agent Application Form]:::step
    B --> C{PCC Verification & Clean Record}:::gate
    C -->|Invalid / Missing| D[Application Disqualified]
    C -->|Valid PCC Date & Station| E[Admin Background Interview @satyam0810]:::step
    E -->|Approved| F[Empanelled Certified Field Agent]:::pass
```

### Empanelment Application Parameters (`AgentApplicationEntity`):
1. **Police Clearance Certificate (PCC)**: Mandatory submission of verified PCC issue date and jurisdictional Police Station name.
2. **Operational Radius**: Configurable operating territory between **5 km and 50 km** from the agent's verified home base.
3. **Transport & Driving License**: Verified commercial or private vehicle license (Motorcycle, Scooter, Car) enabling reliable mobility.
4. **Clean Record Declaration**: Formal statutory declaration affirming no pending criminal charges, financial default proceedings, or moral turpitude investigations under the **Indian Penal Code (IPC)** / **Bharatiya Nyaya Sanhita (BNS)**.

---

## 2. Strict Role Isolation Gate (`Routes.AGENT_MAIN`)

To prevent conflicts of interest (e.g., an agent undervaluing a borrower's jewelry to purchase it personally or bidding on loans they inspected), Loanzo enforces **Strict Architectural Role Isolation**:

```kotlin
// NavGraph.kt Architectural Enforcement
val startDestination = when {
    user.role == "AGENT" && user.agentApproved -> Routes.AGENT_MAIN
    user.role == "ADMIN" -> Routes.ADMIN_HUB
    else -> Routes.MAIN
}
```

```mermaid
graph LR
    classDef agent fill:#D1FAE5,stroke:#059669,stroke-width:2px,color:#064E3B;
    classDef consumer fill:#DBEAFE,stroke:#2563EB,stroke-width:2px,color:#1E3A8A;

    Login["User Logs In"] --> Check{"Role == AGENT & Approved?"}
    Check -->|YES| AgentGate["Routes.AGENT_MAIN<br/>(Field Agent Cockpit)"]:::agent
    Check -->|NO| ConsumerGate["Routes.MAIN<br/>(Borrower / Lender Hub)"]:::consumer

    AgentGate -.->|BLOCKED| ConsumerGate
```

When an approved agent logs in, they are redirected away from consumer tabs (`Loans`, `Marketplace`, `Bidding`). They operate exclusively within the **Field Agent Cockpit** (`Routes.AGENT_MAIN`).

---

## 3. Field Cockpit Architecture & Compensation Engine

The Field Agent Cockpit (`AgentDashboardScreen.kt`) provides real-time operational telemetry:

| Feature | Operational Mechanism |
| :--- | :--- |
| **Duty Toggle** | One-tap switch toggling between `ON_DUTY` (available for dispatch) and `ON_BREAK`. |
| **Earnings Meter** | Real-time accumulator tracking daily and cumulative payouts (`UserEntity.totalAgentEarnings`). |
| **Dispatch Feed** | Live queue of assigned visits categorized by priority and distance (`COLLATERAL_VERIFICATION`, `BORROWER_RESIDENCE`, `LENDER_AUDIT`). |
| **1-Tap Navigation** | Deep-links directly to Google Maps / Apple Maps using counterparty geotag coordinates (`addressGeotag`). |
| **Communication Hub** | Native intent launchers for verified phone calls and encrypted WhatsApp business channels. |

### Compensation Structure:
- **Residential Verification Visit**: ₹500 fixed payout.
- **Precious Metals Assaying Visit**: ₹750 – ₹1,200 depending on gross weight and travel distance.
- **Urgent Commercial Escrow Inspection**: ₹1,500 priority rate.

Payouts are automatically credited to the agent's ledger immediately upon administrative submission and photo proof validation.

---

## 4. Doorstep Precious Metals Assaying Protocol

During gold-backed micro-loan origination, the Certified Valuer conducts physical doorstep testing following strict Reserve Bank of India assaying directives:

```mermaid
sequenceDiagram
    autonumber
    actor Agent as 🕵️ Field Valuer
    actor Borrower as 👤 Borrower
    participant App as 📱 Agent Cockpit
    participant Scale as ⚖️ Calibrated Jeweler's Balance

    Agent->>Borrower: Arrives at Residence (GPS Geotag Verified)
    Agent->>App: Clicks "Check-in at Inspection Location"
    Agent->>Scale: Calibrates digital scale using 10.00g certified reference weight
    Agent->>Scale: Measures Gross Weight of Jewelry
    Note over Agent: Example: Gross Weight = 55.40 grams
    Agent->>Agent: Inspects jewelry for non-gold elements (Stones, Wax, Enamel, Solder)
    Note over Agent: Stone & Wax Deductions = 5.30 grams
    Note over Agent: Net Pure Gold Weight = 55.40g - 5.30g = 50.10 grams
    Agent->>App: Inputs Gross: 55.40g | Deductions: 5.30g | Net: 50.10g (22K)
    Agent->>App: Captures high-resolution macro photo of jewelry on scale
    App-->>Agent: Auto-calculates LTV (Loan-to-Value) per RBI 75% Gold Ceiling
```

### Mandatory Deductions Invariant:
Per **RBI Master Direction DNBR.PD.008/03.10.119/2016-17**, loan-to-value calculations must be based **strictly on the net weight of gold content**. The weight of precious stones, lacquer, thread, or solder must be deducted completely before appraisal.

---

## 5. Tamper-Evident Bagging & Dual-Signature Sealing

To eliminate substitution allegations (e.g., *"The lender returned fake gold"*), physical items are sealed at the borrower's residence inside tamper-evident serialized packaging:

```
┌────────────────────────────────────────────────────────┐
│  🔒 LOANZO INSTITUTIONAL VAULT ESCROW SECURITY BAG     │
│  BARCODE SERIAL: #G408459                              │
│                                                        │
│  Asset: 22K Gold Ornament (50.10g Net)                │
│  Loan ID: LN-92841029                                  │
│  Sealed Date: 12-Sep-2026 11:30 AM IST                 │
│                                                        │
│  [TAMPER-EVIDENT ADHESIVE VOID TAPE]                   │
│  WARNING: VOID PATTERN APPEARS IF PEELED               │
│                                                        │
│  Borrower Signature: ____________  Date: ____________  │
│  Agent Signature:    ____________  Date: ____________  │
└────────────────────────────────────────────────────────┘
```

1. **Serialized Tracking**: Each bag features an indelible, pre-printed barcode (e.g., `#G408459`).
2. **Void Tape Chemistry**: The opening seam uses high-security VOID adhesive that permanently leaves a red checkered "VOID OPENED" pattern if peeled or exposed to heat/freezing.
3. **Dual Signature Cross-Seal**: The borrower and field agent sign indelible permanent marker signatures directly across the boundary line of the seal tape and the plastic bag.
4. **Photographic Record**: The agent takes an on-screen photo of the sealed bag showing both signatures and the clear barcode serial number.

---

## 6. Institutional Bank Vault Custody & CERSAI Lien

The field agent never holds collateral overnight. Collateral must be deposited into a partner institutional safe deposit vault within 4 hours of collection.

```mermaid
sequenceDiagram
    autonumber
    actor Agent as 🕵️ Field Agent
    actor Bank as 🏦 Partner Bank Branch (e.g. HDFC / ICICI)
    actor Admin as 👑 Master Admin
    participant App as 📱 Loanzo Command Center
    participant Room as 🗄️ Room Database v22

    Agent->>Bank: Delivers Sealed Barcode Bag #G408459 to Authorized Vault Officer
    Bank->>Bank: Verifies unbroken seal tape & barcode
    Bank->>Bank: Places bag in Safe Deposit Locker (e.g. Locker #LK-704)
    Bank-->>Agent: Issues Physical Safe Custody Acknowledgment Slip
    Agent->>Admin: Hands over Custody Slip
    Admin->>App: Opens Admin Command Center -> Vault Tab
    Admin->>App: Enters Branch: "Connaught Place", Locker: "LK-704", Barcode: "#G408459"
    App->>Room: INSERT INTO collateral_vault (status = 'SAFELY_VAULTED')
    App-->>Borrower: Automated Push: "Collateral Secured in Locker #LK-704. Loan Disbursed!"
```

### CERSAI Registration:
For loans exceeding ₹1,00,000, the security interest is electronically registered with the **Central Registry of Securitisation Asset Reconstruction and Security Interest (CERSAI)**, preventing the borrower from hypothecating the same asset across multiple financial institutions.

---

## 7. Loan Settlement & De-Hypothecation Restitution Protocol

When a borrower pays their final EMI and reduces the outstanding loan balance to exactly **₹0.00**, the de-hypothecation workflow is triggered automatically:

```mermaid
graph TD
    classDef action fill:#EFF6FF,stroke:#3B82F6,stroke-width:2px,color:#1E3A8A;
    classDef cert fill:#FEF3C7,stroke:#D97706,stroke-width:2px,color:#78350F;
    classDef success fill:#ECFDF5,stroke:#059669,stroke-width:2px,color:#064E3B;

    A[Final EMI Paid: Outstanding Balance = ₹0.00]:::action --> B[Automated Debt Clearance Letter & Digital NOC Issued]:::cert
    B --> C[Admin Dispatches De-Hypothecation Release Order]:::action
    C --> D[Bank Vault Officer Retrieves Locker #LK-704]:::action
    D --> E[Borrower Inspects Serial Barcode #G408459 & Unbroken Seal Tape]:::action
    E -->|Seal Intact| F[Physical Restitution Complete: Bag Released to Borrower]:::success
    F --> G[CollateralVaultEntity.status = RELEASED]:::success
```

### Physical Handover Invariants:
1. **Unbroken Seal Audit**: The borrower must personally examine the seal tape in the presence of the vault officer to verify that no tampering has occurred.
2. **Digital Handover OTP**: The vault officer triggers a 6-digit handover OTP dispatched to the borrower's registered phone, entering it into the app to record physical release.
3. **Zero Storage Fees**: In compliance with consumer protection norms, no post-settlement holding fees or custodial penalties are levied against the borrower for prompt collections.

---

<div align="center">
<b>Loanzo Custodial Operations</b> • Certified Field Agent SOP Specification
</div>
