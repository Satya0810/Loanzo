# Software Testing Strategy & Quality Assurance Specification (STP)

## 1. Overview & Quality Objectives
This document establishes the **Quality Assurance (QA) and Software Testing Plan** for **Loanzo**, adhering to ISO/IEC 25010 software quality standards and IEEE 829 test documentation guidelines.

### Primary Quality Goals:
1. **Financial Precision**: 100% accuracy on all interest, EMI amortization, penalty calculations, and zero-balance discharge verifications.
2. **Security & Cryptographic Integrity**: Strict enforcement of SHA-256 password hashing, biometric signature validation, and secure document token handling.
3. **Offline-First Resilience**: Zero data loss during network disruptions, verified via Room SQLite transactional persistence and WorkManager synchronization.
4. **Compliance & Legal Enforceability**: Proper generation and verification of dual-party eSigned contracts and No Objection Certificates (NOC).

---

## 2. Testing Levels & Methodology

```
                   ┌─────────────────────────┐
                   │    ACCEPTANCE / UAT     │  Manual User Verification,
                   ├─────────────────────────┤  Field Trial Runs
                   │   UI & INSTRUMENTATION  │  Compose Rule Tests,
                   ├─────────────────────────┤  Navigation & E2E Flows
                   │   INTEGRATION TESTING   │  Room + Firestore Sync,
                   ├─────────────────────────┤  Drive Proxy, Telegram Webhooks
                   │      UNIT TESTING       │  ViewModels, PenaltyEngine,
                   └─────────────────────────┘  Amortization, RuleEngine
```

### 2.1 Unit Testing (Local JVM)
* **Scope**: Business logic isolated from Android OS dependencies.
* **Target Components**:
  - `PenaltyEngine.kt`: Simple percentage, flat fees, monthly compound interest, and RBI 2% ceiling compliance.
  - `calculateEMI()` & Amortization: Precision tests comparing against standard financial banking formulas.
  - `PrepaymentSavingsCalculator`: Validation of interest saved and tenure reduction.
  - `FinancialHealthScore`: Calculation weighting across punctuality (40%), utilization (30%), diversity (15%), and age (15%).

### 2.2 Integration Testing
* **Scope**: Interaction between subsystems, database layers, and network proxies.
* **Target Components**:
  - **Room DAOs & Migrations**: Verifying database migrations from v1 through v11 (including `marketplace_posts` and `marketplace_bids`).
  - **Firestore Dual Sync**: Ensuring `SyncWorker` correctly syncs modified entities without overwriting server timestamps.
  - **Google Drive Proxy Web App**: Testing multipart file upload, error retry, and direct stream URL generation.
  - **Telegram Bot Webhook**: Testing command routing (`/start`, `/myloans`, `/repay`) and unauthorized sender isolation.

### 2.3 UI & Instrumentation Testing (Device / Emulator)
* **Scope**: Jetpack Compose rendering, state hoisting, user interactions, and visual regression.
* **Target Components**:
  - **Docked Center Action Button & Radial Satellites**: Animation expanding and collapsing at 140°, 90°, and 40° angles with tactile touch targets.
  - **Segmented Capsule Mode Switchers**: Smooth state transitions on `MarketplaceFeedScreen` and `LoanListScreen`.
  - **SwipeToConfirmButton**: Draggable touch threshold confirmation preventing accidental repayment execution.

---

## 3. Core Test Matrix & Verification Scenarios

| Test ID | Module | Scenario | Expected Outcome | Status |
|---|---|---|---|:---:|
| **TC-AUTH-01** | Authentication | Password reset via Biometrics / OTP | Clears state and successfully updates SHA-256 password hash in Room & Firestore | ✅ Verified |
| **TC-AUTH-02** | Authentication | "Continue with Google" with missing SHA-1 | Handled gracefully with clear DEVELOPER_ERROR explanation instead of raw crash | ✅ Verified |
| **TC-KYC-01** | Verification | DigiLocker credential validation | Validates Aadhaar/PAN against authority records; caches verified avatar locally | ✅ Verified |
| **TC-LOAN-01** | Origination | Smart Counterparty Autocomplete | Displays matching users by phone/@username with `✓ KYC Verified` badge | ✅ Verified |
| **TC-LOAN-02** | Negotiation | Proposal Acceptance Gate | Displays terms review card; requires mutual acceptance before contract creation | ✅ Verified |
| **TC-LOAN-03** | eSign Contract | Dual-Party Agreement Generation | Generates multi-page PDF with canvas signature, camera selfie, and biometrics | ✅ Verified |
| **TC-LOAN-04** | Disbursal | UPI Deep-Link & UTR Recording | Opens installed UPI app (GPay/PhonePe) pre-filled; records UTR; flips to `ACTIVE` | ✅ Verified |
| **TC-LOAN-05** | Servicing | Overdue Days & Penalty Engine | Triggers grace period alert; compounds late fee according to loan penalty model | ✅ Verified |
| **TC-LOAN-06** | Closure | Zero Balance & NOC Certificate | Generates official No Objection Certificate (PDF) with verification seal upon full settlement | ✅ Verified |
| **TC-MKT-01** | Marketplace | Social Post Filtering & Search | Filters by category chips (`#Education`, `#Medical`) and keywords in real time | ✅ Verified |
| **TC-MKT-02** | Marketplace | Lenme-Style Bidding & Conversion | Converts accepted bid into an official `LoanEntity` routing directly into eSign flow | ✅ Verified |

---

## 4. Defect Management & Quality Criteria
* **Critical Defects (P0)**: Financial miscalculation, data loss, crash on launch, or unauthorized data exposure. *Release blocker.*
* **Major Defects (P1)**: Feature failure under specific conditions with existing workaround. *Fix before milestone release.*
* **Minor Defects (P2)**: UI cosmetic misalignment or animation stutter. *Scheduled for polish sprint.*

---

## 5. Build Verification & Release Protocol
Prior to any release APK deployment:
1. `.\gradlew.bat test` — All local JVM unit tests must execute cleanly.
2. `.\gradlew.bat assembleDebug` — Must achieve `BUILD SUCCESSFUL` with zero compilation errors.
3. APK verification: Signature validation, asset size inspection, and Desktop artifact deployment.
4. Living Documentation Update: Synchronize `SRS.md`, `SystemArchitecture.md`, `ProjectStructure.md`, and recompile `Loanzo_Full_Project_Report.pdf`.
