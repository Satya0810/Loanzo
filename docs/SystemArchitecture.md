# System Architecture

## 1. Architectural Pattern
Loanzo strictly adheres to the **Model-View-ViewModel (MVVM)** architectural pattern combined with **Clean Architecture** principles.

- **Model (Data Layer)**: Handles data retrieval and storage. It abstracts the data sources (Room Database, Firebase, Google Drive APIs) using Repository patterns.
- **ViewModel (Domain/Logic Layer)**: Acts as the intermediary between the View and the Model. It contains the business logic, transforming raw data from the repositories into UI State (`StateFlow`), which is consumed by the UI.
- **View (Presentation Layer)**: Built entirely using **Jetpack Compose**. It is reactive, stateless (where possible), and purely responsible for rendering the UI based on the `StateFlow` emitted by the ViewModel.

## 2. Tech Stack
### Android Frontend
- **Language**: Kotlin 1.9+
- **UI Toolkit**: Jetpack Compose (Material Design 3)
- **Dependency Injection**: Dagger-Hilt
- **Concurrency**: Kotlin Coroutines & Flows
- **Navigation**: Jetpack Navigation Component (Compose)
- **Image Loading**: Coil

### Backend & Cloud Services
- **Database Architecture (Offline-First)**: 
  - **Local Store**: Android Room Database acts as the single source of truth for UI rendering, ensuring the app works perfectly offline.
  - **Cloud Syncing**: `SyncWorker` (powered by Android WorkManager) handles bidirectional data syncing with Firebase Firestore in the background. It queues local modifications in a `SyncQueueEntity` table and flushes them to Firestore when network connectivity is restored.
- **Authentication**: Firebase Auth (Email, Google), OTPless (WhatsApp/SMS OTP), AndroidX Biometric (Local Auth with password-verified on-demand enrollment fallback via `RegisterBiometricsDialog`).
- **Storage**:
  - Local App Storage & Media Downloader: User selfies and KYC documents are cached locally at `context.filesDir/profile_{userId}.jpg`, `pan_{userId}.jpg`, `aadhaar_{userId}.jpg`. On fresh installs, `downloadUserMediaLocally()` automatically downloads remote files from Google Drive via direct stream endpoints (`https://drive.google.com/uc?export=view&id=...`), ensuring permanent offline visibility and zero 404 image errors.
  - Google Drive Cloud Storage: KYC documents (PAN, Aadhaar) are securely uploaded to Google Drive via an authenticated Google Apps Script Web App proxy, bypassing Google Service Account storage quota limitations.
- **Messaging/Notifications**: Firebase Cloud Messaging (FCM), Twilio (SMS).
- **Background Broadcast Receivers**: `LoanzoSmsReceiver` intercepts incoming SMS messages silently in the background. It serves dual purposes: auto-verifying OTP tokens, and intercepting Bank Credit SMSs to natively facilitate our "Manual Penny Drop" Bank Account Verification without an API.

- **Dual Role Loan Origination Engine**: Dynamically routes loan creation into `Grant Loan` (lender binds borrower via Borrower ID) or `Request Loan` (borrower binds lender via Lender ID), with automated multi-attribute counterparty resolution (userId, username, phone, email).
- **DigiLocker**: OAuth-based integration for verified Indian government KYC documents, showing unified verification badges in user profiles.
- **ML Kit**: On-device Face Detection for Liveness checks during KYC and eSign.
- **Google GTX Translation Engine**: Ultra-fast, zero-overhead machine translation engine (`translate.googleapis.com`) with an in-memory LRU cache (`500` entries) for real-time chat and document translation.
- **Dynamic App Localization & Resource Architecture**:
  - Full static localization resource bundles for Hindi (`values-hi/strings.xml`) and Marathi (`values-mr/strings.xml`) compiled directly into the application for zero-latency, 100% offline language support.
  - Complete elimination of placeholder/mock translation prefixes (`[HI]`, `[MR]`), replaced with authentic Devnagari financial terminology (`ऋण डैशबोर्ड`, `सक्रिय ऋण`, `बकाया`, `मूलधन`, `भुगतान करें`).
  - Jetpack Compose runtime localization: `MainActivity.kt` dynamically applies language changes via `resources.updateConfiguration(config, resources.displayMetrics)` preserving foreground Activity references, enabling instant, zero-flicker UI updates across the entire app whenever the language is switched in settings.
  - Dynamic bottom navigation tabs (`Home`, `Loans`, `Alerts`, `Profile`) and profile controls fully localized using `stringResource()`.
- **Telegram Bot Integration (@Loanzo_bot)**:
  - `TelegramManager` dispatches asynchronous alerts directly to admin chat IDs for real-time KYC/loan tracking, and provides 1-tap user account deep-linking.
  - **Identity Resolution & Access Control**: The bot securely verifies the Telegram `chat.id` against registered user profiles (`UserEntity.telegramChatId`) to ensure users only access their own loan portfolios.
  - **Account Binding Handshake**: Accounts are securely linked via cryptographic one-time deep links (`/start OTT_<token>`) or Telegram's native cryptographic phone number verification (`request_contact`), eliminating impersonation risks.
- **Local E-Sign System**: Native, on-device generation of multi-page PDF agreements using `PdfDocument`. Dual-party signing orchestrated with canvas signature capture, liveness selfies, and biometric authentication, backed by Google Drive.
- **In-App Notification Center & Deadline Engine**:
  - `NotificationEntity` (Room, `notifications` table) stores per-user alerts with `type`, `isRead`, `relatedLoanId`, `actionRoute`, and a `dayKey` for deduplication.
  - `NotificationRepository.scanAndGenerateDeadlineNotifications()` iterates all active loans, inspects `RepaymentEntity` due dates and statuses, and generates `DEADLINE` (3-day, 1-day, today), `OVERDUE`, and `AGREEMENT` notifications — skipping duplicates via `dayKey`.
  - Computed EMI fallback: For loans with no explicit repayment schedule, the scanner computes the next EMI date from `createdAt + (paidCount + 1) months`.
  - `NotificationViewModel` exposes `StateFlow<NotificationUiState>` supporting real-time search queries, multi-dimensional category tags (`ACTIONS`, `PAYMENTS`, `DEADLINES`, `AGREEMENTS`), date range filtering (`ALL_TIME`, `TODAY`, `THIS_WEEK`, `THIS_MONTH`), and read status.
  - `NotificationScreen` incorporates an embedded live search bar, date dropdown filter, dynamic category tag strip, interactive in-card tag pills, chronological time grouping (Today, Yesterday, This Week, Earlier), and an Urgent Action Hero Card for immediate settlement actions.
  - Bottom navigation "Alerts" tab with `BadgedBox` showing unread count badge (capped at 99+).
  - System status-bar notification via `NotificationCompat` for the highest-priority alert (overdue > deadline).
- **Modern UI/UX Design Architecture (Pocket-Log & Dunio Inspired)**:
  - Dunio-inspired `HeroPortfolioCard` with aggregate balance, active counters, and animated repayment completion progress bar (`LoanProgressBar`).
  - Tactile capsule tab switcher (`SegmentedCapsuleTab`) at the top of the **Loans** screen with smooth sliding pill animations for "Lent" vs "Borrowed".
  - Unified Home Dashboard featuring a comprehensive portfolio overview card with simultaneous Lent/Borrowed metrics and quick action routing.
  - Pocket-Log inspired category badges with soft pastel background tints (`CategoryVisuals.kt`) for Medical, Education, Business, Housing, Agriculture, and Personal categories.
  - Interactive loan cards featuring dynamic repayment progress bars (`₹X of ₹Y repaid • Z%`).
  - Rapid loan origination with calculator-style quick increment chips (`+₹5K`, `+₹10K`, `+₹25K`, `+₹50K`), tenure selectors (`3M`, `6M`, `12M`, `24M`, `36M`), and real-time interactive donut chart simulation.
  - Curved floating bottom navigation bar with rounded top corners, glass border stroke (`0.5.dp`), and reactive unread badges.
  - **Docked Intersecting Yellow Plus FAB with Circular Radial Menu (`MainScaffold`)**:
    - Round yellow floating action button (`56.dp`, `Gold500`) docked in the upper center, with its lower half (`28.dp`) intersecting the navigation bar surface.
    - Smooth 45° spring rotation animation morphing the `+` into an `✕` close button on click.
    - Fullscreen dimmed scrim backdrop (`55% black`).
    - **Circular Radial Satellite Menu (Fan-out Arc)**: Three circular action bubbles radiate outward along a 100dp orbital arc with bouncy spring physics:
      - 🟡 **Lend Offer** (`140°`, Top-Left): Lender capital deployment.
      - 🟣 **Direct P2P** (`90°`, Straight-Up): Transact directly with known contacts / friends & family.
      - 🟢 **Seek Loan** (`40°`, Top-Right): Borrower funding request.
      - Mini floating label tags appear beneath each satellite bubble.
  - **Slide to Confirm / Repay (`SwipeToConfirmButton`)**: Cred & Jupiter-inspired tactile slider preventing accidental transactions in repayment and disbursement workflows.
  - **Prepayment & Foreclosure Simulator (`PrepaymentSimulatorSheet`)**: Interactive bottom sheet computing real-time interest savings, months saved, and accelerated payoff dates.
  - **Dynamic UPI Scan & Pay QR Generator (`UpiQrCodeDialog`)**: On-screen UPI QR code generator rendering `upi://pay` deep-links for instant peer-to-peer settlement.
  - **Cold-Start & Runtime Crash Resilience**:
    - Preserved genuine Activity context references in Compose avoiding `ClassCastException`.
    - Safe Activity context unwrapping across `ContextWrapper` layers for Biometrics, Status Bars, and Dialogs.
    - Version-gated biometric authenticators (`Build.VERSION_CODES.R` check) preventing `IllegalArgumentException` on older devices.
    - Full Android 11+ Package Visibility (`<queries>` block in `AndroidManifest.xml`) for UPI schemes, SMS, WhatsApp, and Telegram.
    - Runtime Camera permission gating (`rememberLauncherForActivityResult`) in Agreement Signing selfie capture preventing `SecurityException`.
    - Offloaded all HTTP bitmap downloads and PDF generation to `Dispatchers.IO` preventing `NetworkOnMainThreadException`.
    - Safe intent chooser launches with `FLAG_ACTIVITY_NEW_TASK` checks.
    - Global uncaught exception logger writing diagnostics to `crash_log.txt`.

### Backend Server (Vercel)
Loanzo has a **Node.js/Express backend** deployed on **Vercel** at `https://backend-blond-sigma-66.vercel.app`. It handles:
- **Telegram Bot Webhook (`/api/telegram/webhook`)**:
  - Inbound Command Dispatcher:
    - `/start`: Returns welcome greeting containing the complete list of all working commands (`/start`, `/myloans`, `/repay`, `/statement`, `/help`, plus Admin commands if caller is admin), and handles deep-link account binding via `user_<id>`.
    - `/myloans`: Queries borrower loan portfolio, showing active balance and next EMI date.
    - `/repay`: Dispatches repayment instructions and UPI payment options.
    - `/statement`: Summarizes recent repayments, on-time status, and accrued late penalties.
    - `/help`: Returns customer support contacts and platform guide.
    - *Admin Commands* (Exclusively restricted to `@satyam_081`): `/stats` (live metrics), `/pendingkyc` (verification queue), `/admin` (control panel).
    - Active Fallback: Handles unrecognized text gracefully by returning the available command menu so the bot never stays silent.
  - Cryptographic Identity Verifier: Enforces role-based access based on verified `chat.id`.
- **Notification Router (`/api/telegram/notify`)**:
  - Routes targeted EMI reminders, disbursal notices, and overdue alerts to the recipient's authenticated `telegramChatId`.
  - Dispatches interactive push cards to Admins with inline action buttons (`[📄 View on Google Drive]`, `[📜 View Signed Agreement]`).
- **Truecaller OAuth** — Token exchange and profile retrieval.
- **User Sync** — Upsert user profiles to an online database.
- **Email OTP** — Dispatching verification emails via Nodemailer/SMTP.
- **DigiLocker KYC** — Proxying calls to the Sandbox.co.in API for Aadhaar/PAN verification, keeping API secrets secure on the server side.

This backend can be extended to support future integrations like Razorpay payments, eSign document generation, and Credit Bureau pulls without requiring any new infrastructure.

## 3. Data Flow
1. **User Action**: User clicks a button in a Composable (e.g., `DashboardScreen.kt`).
2. **Intent**: The Composable triggers a function on the ViewModel (e.g., `dashboardViewModel.loadData()`).
3. **Repository Call**: The ViewModel requests data from the Repository (e.g., `userRepository.getUser()`).
4. **Data Source**: The Repository checks the local Room database. If data is stale or missing, it queries Firestore.
5. **State Update**: The Repository returns a Flow of data to the ViewModel. The ViewModel maps this to a `UiState` data class and updates its `StateFlow`.
6. **Recomposition**: The Composable observes the `StateFlow` and automatically recomposes the UI to reflect the new state.

---

## 4. Community Loan Marketplace ("Social Media of the Loan World")

### 4.1 Architecture Overview
The Social Loan Marketplace provides an open community timeline where individual lenders post capital deployment pools and borrowers post loan funding pitches, mirroring the social dynamics of SoLo Funds and the competitive bidding mechanics of Lenme.

```
                    ┌──────────────────────────────────────────────┐
                    │            DUAL-TRACK LENDING UX             │
                    └──────────────────────┬───────────────────────┘
                                           │
                    ┌──────────────────────┴───────────────────────┐
                    ▼                                              ▼
    ┌───────────────────────────────┐              ┌───────────────────────────────┐
    │ 🤝 TRACK 1: KNOWN CONTACTS    │              │ 🌐 TRACK 2: SOCIAL NETWORK    │
    │ • Deal 1-to-1 with friends/fam│              │ • Public Community Feed       │
    │ • Private & confidential      │              │ • Lender Capital Offers       │
    │ • Direct grant / request flow │              │ • Borrower Loan Requests      │
    └───────────────┬───────────────┘              │ • Lenme-style Competitive Bids│
                    │                              └───────────────┬───────────────┘
                    │                                              │
                    └──────────────────────┬───────────────────────┘
                                           ▼
                    ┌──────────────────────────────────────────────┐
                    │ 📜 LOANZO AUDITED CONTRACT ENGINE            │
                    │ • On-device PDF agreement                    │
                    │ • Dual-party biometric eSign + camera selfie │
                    │ • Google Drive encrypted backup              │
                    │ • Automated EMI tracker & UPI QR generator   │
                    └──────────────────────────────────────────────┘
```

### 4.2 Data & Persistence Layer
1. **`MarketplacePostEntity` (Table: `marketplace_posts`)**:
   - `postId`: UUID Primary Key
   - `authorId`, `authorName`, `authorKycVerified`, `authorTrustScore`
   - `postType`: `"OFFER_TO_LEND"` vs `"SEEKING_LOAN"`
   - `title`, `description`, `minAmount`, `maxAmount`, `interestRate`, `tenureMonths`
   - `purposeCategory`: `EDUCATION`, `MEDICAL`, `BUSINESS`, `EMERGENCY`, `PERSONAL`
   - `locationCity`, `collateralOffered`, `vouchCount`, `bidsCount`, `status` (`OPEN`, `IN_NEGOTIATION`, `FUNDED`)
2. **`MarketplaceBidEntity` (Table: `marketplace_bids`)**:
   - `bidId`: UUID Primary Key
   - `postId`: Foreign key to `marketplace_posts`
   - `bidderId`, `bidderName`, `bidderTrustScore`
   - `proposedAmount`, `proposedInterestRate`, `proposedTenureMonths`, `message`, `status`
3. **`MarketplaceRepository`**:
   - Implements offline-first caching via `MarketplaceDao` with background synchronization to Cloud Firestore collections `marketplace_posts` and `marketplace_bids`.
   - Automatic seeding of initial community posts for vibrant first-launch experience.

### 4.3 User Interface Components
1. **`MarketplaceFeedScreen`**:
   - Segmented capsule mode switcher: `🌐 All Offers` | `💰 Lenders` | `🙋 Borrowers` | `⭐ My Posts`.
   - Revolut-style embedded live search bar with real-time text matching and category tag scrolling strip (`#Education`, `#Medical`, `#Business`, `#Emergency`).
   - Rich `SocialPostCard` with author avatar, `✅ DigiLocker KYC Verified` badge, financial terms capsule, expandable pitch statement, social vouch counter, active bids indicator, and primary call-to-action button.
2. **`CreateMarketplacePostScreen`**:
   - Interactive role selector (Offer Capital vs Seek Loan).
   - Financial scope sliders (Amount bounds, interest rate slider 6%–24%, tenure chips `3M` to `36M`).
   - Category picker and security/pledge proof declarations.
3. **`PostDetailAndBidsSheet`**:
   - Displays competing proposals side-by-side.
   - One-tap **[Accept Bid & Finalize Legal Agreement ➔]** to convert any proposal into an active `LoanEntity`.
4. **Docked Intersecting Radial Integration**:
   - 🟡 **Lend Offer** satellite (Top-Left, 140°) ➔ routes to `CreateMarketplacePostScreen?mode=OFFER_TO_LEND`.
   - 🟣 **Direct P2P** satellite (Center, 90°) ➔ routes to private direct lending (`CREATE_LOAN?mode=GRANT`).
   - 🟢 **Seek Loan** satellite (Top-Right, 40°) ➔ routes to `CreateMarketplacePostScreen?mode=SEEKING_LOAN`.
   - Community loan wall banner embedded in `LoanListScreen`.

## 5. End-to-End Loan Lifecycle Flow Architecture

### 5.1 Lifecycle State Progression
The complete loan lifecycle follows an unambiguous 6-stage progression:
```
 ┌──────────────────────┐       ┌────────────────────────┐       ┌────────────────────────┐
 │ 1. ORIGINATION       │ ───►  │ 2. REVIEW & ACCEPTANCE │ ───►  │ 3. DUAL-PARTY eSIGN    │
 │ (Smart User Picker)  │       │ (Mutual Agreement Gate)│       │ (Selfie + Biometrics)  │
 └──────────────────────┘       └────────────────────────┘       └────────────────────────┘
                                                                             │
 ┌──────────────────────┐       ┌────────────────────────┐                   │
 │ 6. SETTLED & NOC     │ ◄───  │ 5. REPAYMENT SCHEDULE  │ ◄───  ┌───────────▼────────────┐
 │ (Clearance PDF Cert) │       │ (Amortization & Pay QR)│       │ 4. GUIDED UPI DISBURSAL│
 └──────────────────────┘       └────────────────────────┘       │ (Deep-link & UTR Proof)│
                                                                 └────────────────────────┘
```

1. **Origination & Smart Picker (`CreateLoanScreen`)**:
   - Provides quick-pick transactor chips for recent contacts.
   - Real-time user autocomplete searching across `@username`, phone number, and full name.
   - Displays real-time `✓ KYC Verified` badge for selected counterparty.
2. **Review & Acceptance Gate (`LoanDetailScreen`)**:
   - Proposed loans display a high-priority acceptance banner with **Accept Terms** and **Decline** actions before agreement contracts are initiated.
3. **Dual-Party Legally Binding eSign (`AgreementSigningScreen`)**:
   - Generates multi-page agreement PDF with canvas digital signature, front-camera liveness selfie, and biometric authentication backed up to Google Drive.
4. **Guided Disbursal & Proof (`LoanDetailScreen`)**:
   - Once signed, the Lender is presented with a **Disburse via UPI** action.
   - Deep-links to system UPI apps (Google Pay, PhonePe, Paytm) with pre-filled borrower UPI parameters.
   - Records transaction reference (UTR) and flips status to `ACTIVE`.
5. **Servicing & Repayments**:
   - Real-time amortization schedule, overdue day tracking, grace periods, and late penalty calculations.
6. **Settlement & No Objection Certificate (`AgreementGenerator.kt`)**:
   - When outstanding balance reaches ₹0.00, generates an official, legally formatted **No Objection Certificate (NOC) & Debt Clearance Letter (PDF)** with decorative security border, facility particulars, and verification seal.

### 5.2 Collateral Assaying, Vault Custody & Restitution Flow
```
 ┌──────────────────────┐       ┌────────────────────────┐       ┌────────────────────────┐
 │ 1. DOORSTEP ASSAYING │ ───►  │ 2. TAMPER-PROOF SEAL   │ ───►  │ 3. INSTITUTIONAL VAULT │
 │ (Scale & Deductions) │       │ (Bag #G408459 & Sign)  │       │ (Dual-Key Bank Custody)│
 └──────────────────────┘       └────────────────────────┘       └────────────────────────┘
                                                                              │
 ┌──────────────────────┐                                                     │
 │ 5. DE-HYPOTHECATION  │ ◄───────────────────────────────────────────────────┘
 │ (Intact Bag Return)  │  (Triggered on Loan Balance = ₹0.00 Settlement)
 └──────────────────────┘
```
- **Precision Assaying**: Field Valuer uses calibrated jeweler's scales, deducting non-precious weight (stones, wax, solder) per RBI norms (e.g., gross 55.4g - 5.3g = 50.1g net pure 22K gold).
- **Tamper-Evident Bagging**: Signed jointly across seal tag on serialized barcode bag `#G408459`.
- **Institutional Bank Safe Deposit**: Vaulted under dual-key security, CERSAI lien, and 100% replacement insurance.
- **De-Hypothecation Return**: Automated dispatch within 24 hours of loan clearance; borrower verifies unbroken seal before release.

### 5.3 Mascot Storyteller ("Loanzo") Architecture
- **Narrator Pattern**: Living smartphone character "Loanzo" connects all system touchpoints, guiding user comprehension across Borrower, Lender, Agent, and Wholesaler roles in `Loanzo_Unified_Master_Comic_Book.pdf` and `Loanzo_Comic_Reader.html`.
