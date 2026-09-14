# 💻 Loanzo Developer Onboarding & Architecture Guide

> Practical guide for developers, contributors, and maintainers building and extending the **Loanzo** Android client.

---

## 🛠️ Tech Stack & Dependencies

- **Language**: Kotlin `2.0.21` (K2 Compiler Enabled)
- **UI Framework**: Jetpack Compose (`2024.09.00` BoM) + Material 3
- **Architecture**: MVI / MVVM + Clean Architecture + Single Source of Truth
- **Dependency Injection**: Dagger Hilt `2.51.1`
- **Local Persistence**: Room Database `v18` (SQLite with Room KSP)
- **Asynchronous Flow**: Kotlin Coroutines & `StateFlow`
- **Networking & API**: OkHttp3 + Retrofit + Gson
- **Real-Time Data**: Firebase Cloud Firestore & Firebase Auth
- **Push Messaging**: Firebase Cloud Messaging (FCM) + Telegram Bot Webhooks
- **Image Loading**: Coil Compose `2.7.0`
- **Target SDK**: Android 14 / 15 (API 34/35), Min SDK: Android 8.0 (API 26)

---

## 📂 Source Code Directory Map

```text
app/src/main/java/com/loanzo/app/
├── data/                       # Data Layer (Single Source of Truth)
│   ├── ai/                     # MultiAiRaceEngine & LLM Providers (LLM7, SambaNova, Cloudflare)
│   ├── dao/                    # Room DAOs (UserDao, LoanDao, MarketplaceDao, AgentDao, etc.)
│   ├── database/               # LoanzoDatabase Room Database implementation (v18)
│   ├── entity/                 # Room Entities (UserEntity, LoanEntity, MarketplacePostEntity)
│   ├── firebase/               # FirestoreProvider & Firebase synchronization helpers
│   └── repository/             # Repositories mediating Room DB and Remote Services
├── di/                         # Dagger Hilt Dependency Injection Modules
├── fcm/                        # Push Notification Services & Background Handlers
├── receiver/                   # BroadcastReceivers (SmsReceiver, NotificationListener)
├── ui/                         # Presentation Layer (Jetpack Compose UI)
│   ├── admin/                  # App Owner & Master Admin Hub
│   ├── agent/                  # Field Agent Portal & On-Duty Inspection Tools
│   ├── auth/                   # Authentication, KYC, Biometrics, and Profile screens
│   ├── components/             # Reusable UI primitives (Buttons, Avatars, Cards, Tabs)
│   ├── dashboard/              # Executive Dashboard, Smart Portfolio, Financial Health
│   ├── loan/                   # Loan Detail, Key Fact Statements, Loan Calculator, Chat
│   ├── marketplace/            # Community Loan Wall, Bidding, Social Vouches, Posts
│   ├── navigation/             # NavGraph, Route Definitions, and Screen Routing
│   ├── notification/           # Activity & Notification Center
│   ├── support/                # Smart Mediation Desk & Customer Support Tickets
│   └── theme/                  # Brand Color Schemes, Typography, Shapes, Edge-to-Edge
├── util/                       # Helpers (Security, Encryption, LocaleHelper, Telegram)
└── worker/                     # WorkManager Workers for background offline data sync
```

---

## ⚡ Build & Run Commands

### 1. Fast Kotlin Compilation Check
To verify that all Kotlin code compiles with zero errors without assembling the full APK:
```bash
.\gradlew.bat compileDebugKotlin --no-daemon
```

### 2. Assemble Debug APK
To compile and package the installable `.apk` file:
```bash
.\gradlew.bat assembleDebug --no-daemon
```
The resulting APK is generated at:
```text
app/build/outputs/apk/debug/app-debug.apk
```

### 3. Install on Connected Device / Emulator
```bash
.\gradlew.bat installDebug
```

---

## 📏 Architecture Rules & Code Conventions

1. **Unidirectional Data Flow (UDF)**:
   - Screens only observe immutable `UiState` via `collectAsStateWithLifecycle()`.
   - User interactions trigger explicit ViewModel events (e.g. `viewModel.submitBid(...)`).
2. **Offline-First Resilience**:
   - Room Database is always the single source of truth for the UI.
   - Remote mutations update local DB first or synchronize in the background via `SyncWorker`.
3. **Edge-to-Edge Display**:
   - Outer Scaffold handles system bars and window insets.
   - Nested Scaffolds must declare `contentWindowInsets = WindowInsets(0, 0, 0, 0)` to prevent duplicate insets and bottom navigation gaps.
4. **Material 3 Theming**:
   - Use dynamic tokens from `MaterialTheme.colorScheme` and brand tokens from `Color.kt` (`Gold500`, `BrandCobalt`, `Emerald400`).
   - Avoid hardcoded raw hex colors in composables.
