# Project Structure

Loanzo is organized by feature and layers to ensure a clean, maintainable, and scalable codebase.

```text
com.loanzo.app
├── data/                       # Data Layer (Models, Repositories, Data Sources)
│   ├── entity/                 # Room & Firestore Models (UserEntity, LoanEntity, MarketplacePostEntity, MarketplaceBidEntity)
│   ├── repository/             # Offline-first repositories (UserRepository, LoanRepository, MarketplaceRepository)
│   ├── dao/                    # Room Data Access Objects (UserDao, LoanDao, MarketplaceDao, RepaymentDao)
│   ├── firebase/               # Firebase Managers (Firestore, Storage, Auth wrappers)
│   ├── drive/                  # Google Drive API Integrations (GoogleDriveManager)
│   └── digilocker/             # DigiLocker KYC API services
│
├── ui/                         # Presentation Layer (Jetpack Compose UI & ViewModels)
│   ├── auth/                   # Authentication & Profile (ProfileScreen, KycScreen, AuthViewModel)
│   ├── dashboard/              # Unified Dashboard (DashboardScreens, DashboardViewModel, FinancialHealthScreen, HomeActionSheets)
│   ├── loan/                   # Loan Lifecycle (LoanScreens, CreateLoanScreen, LoanDetailScreen, ChatScreen)
│   ├── marketplace/            # Social Marketplace (MarketplaceFeedScreen, CreateMarketplacePostScreen, PostDetailAndBidsSheet, MarketplaceViewModel)
│   ├── notification/           # Notification Center & Deadlines (NotificationScreen, NotificationViewModel)
│   ├── navigation/             # App Routing (NavGraph, Routes, Bottom Nav Badges, Docked Radial Satellites)
│   ├── components/             # Reusable UI Components (SegmentedCapsuleTab, SwipeToConfirmButton, PrepaymentSimulatorSheet, UpiQrCodeDialog)
│   └── theme/                  # Theming (Colors, Typography, Shapes, Category Pastels)
│
├── domain/                     # Domain / Business Logic Layer
│   ├── PenaltyEngine.kt        # Compound/Simple/Flat penalty calculations with RBI caps
│   ├── RuleEngine.kt           # Disbursement rule evaluation engine
│   └── model/                  # Domain models (PurposeCategory, etc.)
│
├── util/                       # Utilities and Helpers
│   ├── AgreementGenerator.kt   # Dual-party legal eSign PDF & No Objection Certificate (NOC) generator
│   ├── TelegramManager.kt      # Telegram Bot alerts & deep-linking manager
│   ├── TranslationHelper.kt    # Google GTX Translation engine + LRU Caching
│   ├── BiometricAuthManager.kt # Helper for AndroidX Biometrics
│   ├── Utils.kt                # Profile photo resolvers, formatters
│   ├── Formatters.kt           # Date, Time, Currency formatters
│   └── Constants.kt            # App-wide constants
│
├── fcm/                        # Push Notifications (Firebase Cloud Messaging)
│   └── LoanzoMessagingService.kt
│
├── receiver/                   # Broadcast Receivers
│   └── LoanzoSmsReceiver.kt    # Automatic SMS OTP/Notification reading
│
└── LoanzoApplication.kt        # Application class (Hilt Initialization, Configs)
```

## Key Modules Details

- **`data/`**: The single source of truth for the app. Components in the `ui/` layer should never interact directly with Firebase or Room; they must go through the Repositories defined here.
- **`ui/`**: All UI elements are written in Jetpack Compose. Each feature directory (like `auth/` or `loan/`) encapsulates its own screens and ViewModels.
- **`util/`**: Keeps the codebase DRY (Don't Repeat Yourself) by centralizing formatters and static configurations.
