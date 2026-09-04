# Software Requirements Specification (SRS) for Loanzo

## 1. Introduction
### 1.1 Purpose
The purpose of this document is to outline the software requirements for **Loanzo**, a peer-to-peer (P2P) lending and borrowing application. It details the functional and non-functional requirements to guide the development and maintenance of the application.

### 1.2 Scope
Loanzo is a unified Android application where any registered user can act as a Lender, a Borrower, or both. The platform facilitates the complete lifecycle of a loan, including KYC verification, loan origination, tranche requests, repayment tracking, collateral management, and auditing.

## 2. Overall Description
### 2.1 User Roles
- **Unified User**: A single registered entity who can seamlessly switch contexts between viewing loans they have given (Lender perspective) and loans they have taken (Borrower perspective) from a single Unified Dashboard.
- **Admin / App Owner**: Has elevated privileges for overriding KYC, managing organizational settings, and approving critical platform-level actions.

### 2.2 Core Features
1. **Authentication & Security**
   - Multi-factor authentication (Email/Password, Username-based credentials, Phone OTP, Google Sign-in).
   - **Biometric Authentication & Intelligent Enrollment**: Fingerprint and Face unlock for returning users. If biometric enrollment is missing, cleared, or lost, an on-demand prompt requires confirming the account password to securely verify identity and register device biometrics without forcing a full re-login.
   - Username-first password reset workflow.
2. **KYC Verification, Profile Management & Encrypted Document Vault**
   - **"Click-to-See" Executive Drill-Down Architecture (Cred & Revolut Inspired)**: Replaced sprawling open boxes and permanently visible detailed cards with a compact, uncluttered Executive Hub and categorized navigation tiles. Tapping any item smoothly opens a dedicated sub-page (`AnimatedContent` with Android `BackHandler`):
     - *Personal & Identity Details Sub-Page*: Full contact, demographic, and biometric status breakdown.
     - *Encrypted Document Vault Sub-Page*: Sensitive KYC documents (PAN, Aadhaar, DigiLocker, liveness photos) gated behind an Account Password / Biometric authentication dialog before decryption.
     - *Bank & Payout Accounts Sub-Page*: Metallic debit card preview, IFSC details, and ₹1 penny-drop verification test.
     - *App Preferences Sub-Page*: Segmented theme selector (System/Light/Dark), 11-language runtime switcher, and Telegram bot binding.
     - *Terms & Conditions Sub-Page*: Full legal compliance agreement covering RBI P2P guidelines, IT Act 2000 Section 10A digital contract validity, PMLA compliance, and dispute resolution.
   - **Full-Screen Immersion & Hidden Bottom Navigation**:
     - When navigating to the Profile section or the Alerts (Notifications) section, the lower navigation section (5-slot navigation bar, center yellow Post button, and radial satellite actions) is hidden completely to allow full vertical viewing.
     - Return navigation to the Home Dashboard is handled via the dedicated TopAppBar Back arrow (`Icons.AutoMirrored.Filled.ArrowBack`) and hardware back gesture.
   - **Encrypted KYC Document Vault**:
     - *Encrypted Default State*: PAN cards, Aadhaar cards, DigiLocker government credentials, and ML Kit liveness photos are housed inside an AES-256 encrypted vault to prevent unauthorized screen viewing or shoulder-surfing.
     - *Password & Biometric Verification*: Accessing the vault strictly requires verifying the user's account password (SHA-256 hash validation against `UserEntity.password`) or biometric fingerprint/face authentication via `BiometricAuthManager`.
     - *Decrypted Session & Cloud Access*: Once unlocked, reveals digital ID cards with one-tap "View Cloud Doc" launchers and "Upload / Update" PDF/image pickers. Includes instant "Lock Vault" controls in the top app bar and vault header.
   - **DigiLocker Integration**: Direct integration verifying authentic Aadhaar and PAN credentials against government records with unified "DigiLocker Verification" badge.
   - **Cloud Document Upload**: Direct upload for PAN and Aadhaar documents (Images & PDFs) routed to Google Drive via an authenticated Google Apps Script Web App proxy with live progress feedback.
   - **Liveness Detection & Local Caching**: ML Kit real-time selfie verification, cached locally on-device (`profile_{userId}.jpg`) to ensure instant, pitch-black-free avatar display everywhere in the app.
3. **Unified Home Dashboard & Financial Overview**
   - **Header Quick Actions (Three-Dots Overflow Menu)**: Upper-right header features a three-dots menu (`MoreVert`) providing three options:
     - **Chat**: Quick selector bottom sheet (`ChatSelectionBottomSheet`) for instant peer-to-peer loan chats and direct access to the 24/7 Telegram Assistant Bot (`@Loanzo_bot`).
     - **Report (Action & Dispute Center)**: Formal reporting bottom sheet (`ReportActionBottomSheet`) to take platform/legal action on any individual or counterparty for non-payment, default, fraud, harassment, or agreement breach with instant Telegram admin dispatch.
     - **Simulator**: 1-tap navigation directly into the interactive Loan & EMI Simulator (`LoanCalculatorScreen`).
   - **Unified Financial Portfolio Card**: High-density overview showing aggregate metrics across both total lent and total borrowed balances simultaneously, outstanding amounts, and net financial standing without requiring tab switching.
   - Primary quick actions: **"Grant Loan"** (for lenders) and **"Request Loan"** (for borrowers).
   - **Recent Activity Feed**: Curated list of latest active loans with instant navigation into the Loans tab.
4. **Loans Section & Dedicated Lent/Borrowed Capsule Selector**
   - **Segmented Capsule Tab Selector**: Tactile pill-shaped tab switcher (`SegmentedCapsuleTab`) at the top of the Loans screen transitioning between **Lent** (loans given) and **Borrowed** (loans taken).
   - **Contextual Portfolio Cards**: Displays total lent/outstanding when on the Lent tab, and total borrowed/to-repay when on the Borrowed tab.
   - **Contextual FAB**: Dynamically launches "Grant Loan" when under Lent, and "Request Loan" when under Borrowed.
   - **Pocket-Log Inspired Loan Cards**: Category icon badges with soft pastel background tints (Medical, Education, Business, Housing, Agriculture, Personal), dual-attribute counterparty metadata, status badge pills, and inline repayment progress bars (`₹X of ₹Y repaid • Z%`).
5. **Loan Origination & Dual Creation Modes**
   - **Grant Loan Mode (Lender Origination)**: Screen titled "Grant a Loan", captures "Borrower ID", automatically binds the logged-in user as the Lender and the specified counterparty as the Borrower with button "Grant Loan".
   - **Request Loan Mode (Borrower Origination)**: Screen titled "Request a Loan", captures "Lender ID", automatically binds the logged-in user as the Borrower and the specified counterparty as the Lender with button "Request Loan".
   - **Quick-Input Amount Chips (Pocket-Log style)**: One-tap increment chips (`+₹5K`, `+₹10K`, `+₹25K`, `+₹50K`) and preset selectors (`₹10K`, `₹25K`, `₹50K`, `₹1L`, `₹2.5L`) for frictionless calculator-style mobile entry.
   - **Quick Tenure Chips & Live Simulator**: One-tap tenure chips (`3M`, `6M`, `12M`, `24M`, `36M`), slider controls, and real-time interactive donut chart calculating estimated EMI, total interest, and principal proportions.
   - Granular term controls (Simple/Compound/Flat Interest, Penalty Rules, Grace Periods, Repayment Frequency, Tenure).
   - Tranche Requests (disbursing loans in parts).
   - **Slide to Confirm / Repay (`SwipeToConfirmButton`)**: Cred/Jupiter-inspired tactile draggable slider in `MakeRepaymentScreen` that prevents accidental submissions with animated progress and haptic confirmation.
   - **Dynamic UPI Scan & Pay QR Code Modal (`UpiQrCodeDialog`)**: On-demand QR code dialog rendering compliant `upi://pay` deep-links for instant peer-to-peer settlement via PhonePe, Google Pay, or Paytm.
   - **Prepayment & Foreclosure Savings Calculator ("What If I Prepay?")**: Interactive modal bottom sheet (`PrepaymentSimulatorSheet`) in `LoanDetailScreen` computing real-time interest savings, months eliminated, and accelerated debt-free target dates.
   - Repayment logging and tracking.
   - Pledge/Collateral tracking.
6. **Agreements & Auditing**
   - On-device Loan Agreement PDF generation including Loan Terms, Rules, and KYC Verification statuses.
   - Dual-party eSign workflow (Lender and Borrower).
   - 3-factor signature authentication: Signature Canvas + Liveness Selfie (CameraX/ML Kit) + Biometric Prompt.
   - Secure storage of signed agreements in Google Drive.
   - Immutable Audit Trail for every action taken on a loan.
6. **Communication, Multi-Language Translation & Telegram Bot**
   - Real-time in-app chat between Lender and Borrower backed by Firestore.
   - **High-Speed Translation Engine**: Built-in Google GTX translation engine with in-memory LRU caching, delivering <150ms translations without API quotas or storage bloat.
   - **Global App Language Selection**: Searchable Language Picker modal in Profile supporting 21+ Indian (Hindi, Marathi, Bengali, Tamil, Telugu, Gujarati, etc.) and global languages with instant runtime locale switching across the entire UI.
   - **Telegram Bot Assistant (@Loanzo_bot)**:
     - Real-time Admin alerts with inline buttons for instant notification of new KYC document uploads, loan requests, and executed agreements.
     - Zero-cost automated EMI reminders and overdue alerts sent directly to borrowers.
     - **Working Telegram Bot Commands**:
       - `/start` — Welcomes user, initializes assistant, and displays a comprehensive menu of all working commands:
         > `👋 Welcome to Loanzo Bot!`
         > `I am your 24/7 personal loan and EMI notification assistant.`
         > `Available Working Commands:`
         > `• /start — Welcome message & bot initialization`
         > `• /myloans — View your active loans & next EMI due date`
         > `• /repay — Repayment instructions & UPI payment links`
         > `• /statement — Summary of recent repayments & penalty ledger`
         > `• /help — Customer support, FAQs & bot guide`
         > *(Plus Admin section for registered Admin IDs)*
       - `/myloans` — Displays active loans, total balance, interest rate, and next EMI due date.
       - `/repay` — Generates direct payment options and UPI payment links for active loans.
       - `/statement` — Shows a summary of recent repayments, schedule status, and accrued penalties.
       - `/help` — Displays customer support contacts, FAQs, and bot usage guide.
     - **Admin Commands** (Restricted to registered Admin IDs `8234574147` & `7464832770`):
       - `/stats` — Live business snapshot: Total active users, disbursed amount, pending KYCs, overdue loans.
       - `/pendingkyc` — Lists unverified user documents with direct review links.
       - `/admin` — Opens admin dashboard control panel with one-tap action buttons.
     - **Interactive Inline Push Cards**:
       - *KYC Upload Notice*: Includes `[📄 View on Google Drive]`, `[✅ Approve]`, and `[❌ Reject]`.
       - *Agreement Finalization*: Includes `[📜 View Signed Agreement]`.
       - *EMI Reminders*: Sent 3 days prior, on due date, and when overdue with calculated penalty details.
     - **Inbound Identity Verification & Authorization**: When handling commands like `/myloans`, the webhook cryptographically checks the sender's Telegram `chat.id` against the database. Unauthenticated or unlinked requests are strictly rejected with zero loan data disclosed.
     - **Targeted Notification Routing**: Outbound loan notices (disbursals, EMI due dates, overdue warnings) are mapped strictly to the individual user's `telegramChatId` to prevent cross-account exposure.
     - **Secure Account Binding Protocols**:
       1. *Cryptographic One-Time Deep Linking (OTT)*: Authenticated app session generates a 5-minute expiring token that binds the account upon launching `t.me/Loanzo_bot?start=token`.
       2. *Telegram Native Contact Verification*: Bot requests phone number verification via Telegram's cryptographic `request_contact` handshake, matching against registered `UserEntity.phone`.
       3. *In-App 6-Digit PIN Verification*: Time-limited code generated in Profile screen and validated via `/link <code>`.
   - Push Notifications (FCM).
   - SMS reminders for upcoming and overdue repayments.
7. **Reporting**
   - Export Loan Summaries and Interest Certificates as PDFs.
   - Export Repayment schedules as CSVs.
8. **Expense Categorization & Financial Health**
   - Automated categorization of disbursements by purpose (Medical, Education, Business, Housing, Agriculture, Other).
   - **180° Speedometer Credit Health Gauge**: Authentic semi-circular speedometer arc with multi-stop color gradients (Red ➔ Orange ➔ Gold ➔ Emerald), an animated needle pointer, and risk tier badges (`PRIME TIER`, `VERY GOOD`, `FAIR TIER`, `SUBPRIME`).
   - Financial Health Score (0–100) computed from four weighted factors: Repayment Punctuality (40pts), Credit Utilization (30pts), Spending Diversity (15pts), and Credit History Age (15pts).
   - Monthly spending trend chart showing disbursement totals over time.
   - Smart, actionable insights engine that generates personalized tips based on the user's payment record, utilization ratio, spending concentration, and month-over-month changes.
   - Spending breakdown donut chart with percentage labels.
9. **Penalty & Late Fee Engine**
   - Automatic detection of overdue repayments (SCHEDULED → OVERDUE when past due date).
   - Three penalty models: Simple Percentage, Compound Interest, and Flat Fee.
   - Configurable grace days and penalty cap per loan.
   - Compound interest formula: `P × ((1 + r)^n - 1)` where r is the monthly rate and n is months overdue.
   - RBI-compliant cap at 2% per month maximum.
   - Penalty waiver capability for lenders.
   - Detailed penalty breakdown display in the Loan Detail screen.
   - Penalty summary card in the Financial Health screen showing total accrued penalties.
10. **Bank Account Verification**
   - Native "Manual Penny Drop" verification via SMS interception.
   - Users can link their bank account and trigger verification by receiving a ₹1 transfer.
   - Background SMS receiver parses incoming bank messages to automatically verify the account based on matching account digits and credit amounts.
11. **In-App Notification Center & Deadline Reminder Engine**
    - **Dedicated "Alerts" Tab**: A 4th bottom navigation tab ("Alerts" with bell icon) providing a full-screen notification center.
    - **Deadline Scanner**: On every tab visit, the engine scans all active loans for upcoming EMI due dates (3 days, 1 day, today), overdue installments, and unsigned agreements.
    - **Deduplication**: Each notification carries a `dayKey` (e.g., `2026-09-03_DEADLINE_3D_loanId`) to prevent duplicate alerts for the same event on the same day.
    - **Notification Types**: `DEADLINE`, `OVERDUE`, `AGREEMENT`, `DISBURSEMENT`, `REPAYMENT`, `SYSTEM` — each rendered with a distinct icon and color badge.
    - **Unread Badge**: The bottom nav "Alerts" icon displays a red badge with the unread count (capped at 99+).
    - **Filter Chips**: Users can filter by All, Deadlines, Overdue, or Unread.
    - **Actions**: Tap-to-navigate to related loan detail, mark as read, mark all as read, delete individual, or clear all.
    - **System Notification**: The highest-priority alert (overdue > deadline) also posts an Android status-bar notification via `NotificationCompat` with a `IMPORTANCE_HIGH` channel.
    - **Computed EMI Fallback**: For loans with no explicit `RepaymentEntity` schedule, the scanner computes the next EMI date from `createdAt + (paidCount + 1) months` and generates deadline/overdue alerts accordingly.

## 3. Non-Functional Requirements
- **Offline-First Resilience**: All core features (Loans, Users, Repayments) are backed by a local Room Database. 
- **Cloud Synchronization**: A robust `SyncWorker` (via Android WorkManager) handles bidirectional sync with Firebase Firestore, ensuring data is eventually consistent even with poor network conditions.
- **Scalability**: The backend is powered by Firebase Firestore and Realtime Database, allowing it to scale automatically with user growth.
- **Security**: KYC documents are stored securely in a private Google Drive accessible only via Service Account. No sensitive PDFs are exposed publicly.
- **Performance**: The app uses Jetpack Compose for a highly responsive, modern, and reactive UI architecture.
- **Maintainability**: The codebase adheres to Clean Architecture and MVVM design patterns.
12. **Multi-Tier Authentication & Password Resilience**
    - Universal Identifier Login: Users can seamlessly log in with their **Username, Email Address, or Registered Phone Number** without artificial format restrictions.
    - Multi-format password matching: strict SHA-256 hash, legacy untrimmed hash, plaintext legacy migration, and direct Firebase Authentication fallback (`signInWithEmailAndPassword`).
    - Automatic SHA-256 hash upgrading and synchronous real-time Cloud Firestore persistence on login, registration, and password reset.
    - Deadlock-free Forgot Password flow: supports official Firebase Password Reset email links, on-device Biometrics (Fingerprint/Face/PIN), and 6-digit OTP codes. Any verified method immediately unlocks Step 5 (Create New Password).
    - Clear diagnostic guidance for empty-password accounts (e.g., users created via Google Sign-In or external imports).
    - Step 2 "Continue with Google" Profile Binding: Users entering their username in Step 1 can tap "Continue with Google" in Step 2 to authenticate via Google OAuth. The system maps the Google credential directly to the specified username and cloud profile, restoring loans and KYC files seamlessly.
13. **Cloud KYC Document Sync & Auto-Recovery**
    - Google Drive proxy upload via Google Apps Script Web App.
    - Synchronous Cloud Firestore persistence of `panImageUrl` and `aadhaarImageUrl` preventing data loss across app reinstalls.
    - Cloud document auto-recovery on login: if local Room holds empty document links, the app automatically checks Cloud Firestore and Google Drive storage to restore the user's KYC files.
    - Automated Background Media Cache & Direct Stream Resolver: On app login or session restore, the app automatically downloads remote Google Drive profile photos and KYC documents into local storage (`profile_{userId}.jpg`, `pan_{userId}.jpg`, `aadhaar_{userId}.jpg`), and transforms Drive URLs into active direct stream endpoints (`https://drive.google.com/uc?export=view&id=...`) to ensure 100% visibility offline and across fresh reinstalls.

14. **Community Social Loan Marketplace ("The Social Media of Loan World")**
    - **Public Community Discovery Timeline**: Dual-mode social timeline allowing verified lenders to broadcast capital deployment pools and borrowers to publish peer funding pitches.
    - **Revolut-Style Feed**: Segmented tabs (`All Offers`, `Lenders`, `Borrowers`, `My Posts`), embedded real-time keyword search, and category filter chips (`#Education`, `#Medical`, `#Business`, `#Emergency`, `#Personal`).
    - **Interactive Social Post Cards**: Rich author information with avatar, `✓ DigiLocker KYC Verified` badge, `⭐ Loanzo Trust Score`, financial terms capsule (amount, interest rate, tenure), expandable pitch story, community vouches counter, and active bids indicator.
    - **Lenme-Style Competitive Bidding Engine**: Counterparties submit competing proposals with custom interest rates and tenures. The post creator can compare bids side-by-side and execute a 1-tap **[Accept Bid & Finalize Legal Agreement]** converting any proposal directly into an active loan.
    - **Intersecting Center Speed-Dial Satellites**: Docked center yellow button with 3 animated radial satellites (`Lend Offer`, `Direct P2P`, `Seek Loan`) for instant origination.

15. **End-to-End Loan Lifecycle Flow & No Objection Certificate (NOC)**
    - **Stage 1: Smart Counterparty Picker**: Frictionless user selection in `CreateLoanScreen` via quick-pick transactor chips and real-time autocomplete across `@username`, phone number, and full name with verified KYC badge indicators.
    - **Stage 2: Mutual Review & Acceptance Gate**: Newly created loan proposals enforce a formal review stage where counterparties inspect terms and must tap **[Accept Terms]** before digital contracts are drawn.
    - **Stage 3: Dual-Party Legally Binding eSign**: Canvas signature + front-camera liveness selfie + biometric verification with on-device PDF contract generation and Google Drive cloud backup.
    - **Stage 4: Guided UPI Disbursal & Proof**: Post-signing disbursal card for lenders that deep-links directly into system UPI apps (Google Pay, PhonePe, Paytm) pre-filled with the borrower's payment parameters, with manual UTR / bank reference number confirmation.
    - **Stage 5: Servicing & Repayment Engine**: Dynamic countdown to next EMI, automated late penalties, grace period enforcement, and instant UPI QR scan-to-pay.
    - **Stage 6: Settlement & Official No Objection Certificate (NOC)**: When outstanding balance hits ₹0.00, the system displays a celebration clearance card and generates an official, legally compliant **No Objection Certificate (NOC) & Debt Clearance Letter (PDF)** with decorative border, settlement timestamp, and decentralized verification seal.

16. **Doorstep Agent Collateral Valuation & Custodial Safekeeping Protocol**
    - **Physical Asset Assaying**: When a borrower pledges physical collateral (`PledgeEntity` with `assetType = GOLD/VEHICLE/PROPERTY`), a certified Loanzo Field Valuer is dispatched to the borrower's location with standard assaying tools.
    - **Net Weight & Stone Deduction**: Following RBI gold loan guidelines, non-precious stones, lacquer, solder, and enamel are subtracted, recording exact net weight.
    - **Loanzo Agent App Verification**: The agent uses the dedicated **Loanzo Agent App** to authenticate the borrower's identity, input certified net weight, fetch real-time IBJA market valuation rates, and calculate the RBI-compliant LTV limit (up to 75%–85%).
    - **Tamper-Evident Security Seal**: The physical collateral is placed in a barcode-serialized, tamper-evident security pouch signed jointly by the borrower and the agent before being transferred into a dual-custody fireproof vault.
    - **Instant Secured Status Activation**: System flips collateral status to `VERIFIED & SAFELY VAULTED`, notifying the lender to release loan funds via UPI immediately.

17. **In-App Verified Co-Borrower & Multi-Party Legal Binding**
    - **Registered User Requirement**: Co-borrowers cannot be unverified third parties or SMS-only recipients; they must hold an active, DigiLocker KYC-verified account on Loanzo.
    - **In-App Authorization Card**: When selected by a primary borrower (`@username`), an in-app authorization request appears directly in the co-borrower's Loanzo Notification Center.
    - **Biometric Consent Handshake**: The co-borrower reviews the loan terms and authorizes joint legal liability using their own on-device biometric fingerprint/face authentication (`consentStatus: ACCEPTED`), binding them as a secondary obligor.

18. **Default Escalation Protocols & 100% Scam-Free Guarantee**
    - **Multi-Stage Delinquency Escalation**:
      - *Days 1–3*: 3-day grace period with soft in-app reminder banners.
      - *Days 4–30*: Automated compound penalty engine activating with RBI-mandated 2.0% monthly ceiling cap. Urgent push alerts dispatched to **BOTH Borrower AND Co-Borrower**.
      - *Days 31–60*: Delinquency reported to credit bureaus, dropping both parties' credit scores by 45–80 points.
      - *Day 90+ (NPA)*: Legal demand notice under Indian Contract Act, 1872, and 14-day public auction notice for vaulted collateral.
    - **Scam-Free Safeguards**:
      - *For Lenders*: Zero ghost borrowers (DigiLocker + CameraX liveness), physical gold vaulted with barcode tracking, direct-to-payee tranches eliminating fund diversion, and court-enforceable 3-factor eSign contract.
      - *For Borrowers*: No loan sharks (bidding brings interest down), RBI-capped penalties, insured vaulting with guaranteed release within 7 days, and instant official legal NOC.

19. **Social Trust (Community Vouches) & Real-Time Categorized Expenditure Transparency**
    - **Community Social Vouches (`vouchCount`)**: Verified marketplace members (local business peers, past counterparties) can publicly endorse a borrower's loan post. Each vouch acts as social collateral, giving lenders heightened confidence and lowering interest bids.
    - **Categorized Expenditure Tracking**: Rather than releasing untracked cash, tranches are categorized (`ELECTRONICS`, `BUSINESS`, `HOSPITAL`, `EDUCATION`, `CONSTRUCTION`, `AGRICULTURE`, `TRANSPORT`, `RENT`, `UTILITY`, `GROCERY`).
    - **Lender Audit Dashboard**: The lender has real-time, live visibility into the borrower's itemized utilization ledger, inspecting exact payee UPI IDs, invoices, and bank UTR numbers, guaranteeing zero fund diversion.

20. **Complete Collateral Lifecycle, Institutional Vault Custody & Restitution Protocols**
    - **Doorstep Assaying & Stone Deduction**: Certified field valuer conducts precision weighing using calibrated electronic jeweler's scales. Following RBI norms, non-precious stones, lacquer, wax, and solder are explicitly deducted (e.g., gross 55.4g - 5.3g stones = 50.1g net pure 22K gold).
    - **Tamper-Evident Barcode Bagging**: The assayed collateral is enclosed in a heavy-duty security bag bearing unique alphanumeric barcode tracking (e.g., `#G408459`) with tamper-evident VOID tape. The primary borrower and field valuer physically countersign across the seal tag.
    - **Institutional Bank Vault Custody**: The sealed pouch is transferred to Loanzo's partner bank safe deposit vault (e.g., ICICI/HDFC branch) under dual-key dual-custody access, CERSAI security registry lien, and 100% replacement transit and storage insurance coverage.
    - **Mandatory Safe Restitution (De-Hypothecation)**: Upon full settlement (balance reaching ₹0.00), an automated release trigger dispatches the sealed pouch within 24 hours (mandated within 7 working days by RBI). The borrower personally inspects the unbroken barcode seal before de-hypothecation is finalized.

21. **Educational Mascot ("Loanzo") & Cinematic Graphic Novel Architecture**
    - **"Loanzo" Living Mascot**: An anthropomorphic modern smartphone mascot featuring expressive eyes, cartoon hands, sneakers, and dynamic chest display reflecting live app states. Loanzo acts as the omniscient narrator breaking the fourth wall to explain technical architecture and regulatory compliance.
    - **Continuous Cinematic Timeline**: An interconnected 8-scene graphic novel seamlessly relaying between Borrower (Rohan & Priya), Field Agent (Vikram Rao), Lender (Aisha Khan), and Wholesaler (Raj Electronics).
    - **Dual Publication Output**: Integrated into both high-resolution PDF publications (`Loanzo_Unified_Master_Comic_Book.pdf`) and an interactive HTML5 web reader (`Loanzo_Comic_Reader.html`) featuring Movie Mode and perspective filters.
