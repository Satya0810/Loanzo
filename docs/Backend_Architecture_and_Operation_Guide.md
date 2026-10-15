# ⚙️ Loanzo Protocol: Backend Architecture & Operational Specification
**Document ID:** `LZ-DOC-BND-2026-V1`  
**Classification:** Institutional Architecture Specification & Developer Operating Manual  
**Production Gateway:** `https://backend-blond-sigma-66.vercel.app`  
**Runtime:** Node.js v18+ / Express.js on Vercel Serverless Functions (`@vercel/node`)  
**Telegram Bot Service:** `@Loanzo_bot` (Webhook & Long-Polling Engine)  

---

## Executive Summary

The **Loanzo Backend** is an event-driven, hybrid serverless microservice designed to handle identity verification, government KYC orchestration, administrative notification dispatches, and third-party API gateways without holding customer funds. 

Adhering strictly to **Non-Custodial FinTech Safe Harbor** standards under Reserve Bank of India (RBI) guidelines, the backend facilitates cryptographically signed loan agreements and offloads money transfer entirely to regulated NPCI UPI and banking rails.

```
+-----------------------------------------------------------------------------------------+
|                               LOANZO ANDROID APPLICATION                                |
|                   (Jetpack Compose UI + Room DB Offline Persistence)                    |
+----------------------------+-------------------------------+----------------------------+
                             |                               |
          Direct HTTPS REST  |                Cloud Firestore| Direct Realtime Sync
          (Payloads & KYC)   |                (P2P Messages, | (Loans, Users, Pledges)
                             v                               v
+--------------------------------------------+  +-----------------------------------------+
|         VERCEL NODE.JS SERVERLESS          |  |         GOOGLE FIREBASE CLOUD           |
|  (https://backend-blond-sigma-66.vercel.app)|  |   - Cloud Firestore (Data Collections)  |
|                                            |  |   - Cloud Storage (Encrypted KYC Vault) |
|  - User Profile Sync & In-Memory Store     |  |   - Firebase Auth (Phone / Google OAuth)|
|  - Sandbox.co.in DigiLocker KYC Proxy      |  |   - Firebase Cloud Messaging (Push)     |
|  - Nodemailer SMTP Email OTP Dispatcher    |  +-----------------------------------------+
|  - Truecaller OAuth Token Exchange         |
|  - Telegram Bot Webhook & Event Router     |
|  - Meta WhatsApp Business Cloud Webhook    |
+-----+----------------------+---------------+
      |                      |
      v                      v
+-------------------+  +---------------------+
|  SANDBOX.CO.IN    |  |  TELEGRAM BOT API   |
| (Govt DigiLocker) |  |   (@Loanzo_bot)     |
+-------------------+  +---------------------+
```

---

## 1. Hosting, Deployment & Infrastructure Topology

### 1.1 Where the Backend Lives
1. **Source Code Location**:
   - The complete backend resides in the `/backend` directory of the official repository.
   - Core entry point: [`backend/server.js`](file:///c:/AndroidProjects/loanzo/backend/server.js).
2. **Production Hosting Platform (Vercel Serverless)**:
   - Hosted serverlessly on **Vercel** under the production URL:
     ```
     https://backend-blond-sigma-66.vercel.app
     ```
   - Managed via [`backend/vercel.json`](file:///c:/AndroidProjects/loanzo/backend/vercel.json) using the `@vercel/node` builder. All incoming route requests (`/(.*)`) are routed directly to `server.js`.
   - **High Availability**: Automatically scales across global edge regions with zero server management overhead.
3. **Local Daemon / Polling Service**:
   - [`backend/bot_polling_service.js`](file:///c:/AndroidProjects/loanzo/backend/bot_polling_service.js): Runs locally or on background development workers. It utilizes Telegram's long-polling API to fetch messages and pipe them directly into the webhook handler when running behind firewalls or local test rigs.

---

## 2. Core Functional Modules

```
                           +---------------------------+
                           |     backend/server.js     |
                           +-------------+-------------+
                                         |
     +-----------------+-----------------+-----------------+-----------------+
     |                 |                 |                 |                 |
     v                 v                 v                 v                 v
+---------+       +---------+       +---------+       +---------+       +---------+
| Identity|       |  Auth   |       |Govt KYC |       |Telegram |       |Legal /  |
|  Sync   |       | Gateways|       | Gateway |       | Engine  |       |Webhooks |
+---------+       +---------+       +---------+       +---------+       +---------+
```

### Module A: User Identity Synchronization (`/api/users/*`)
* **Purpose**: Synchronizes user profile states between the local Android SQLite Room DB and the online backend runtime.
* **Mechanism**:
  - Uses an internal, in-memory Map structure (`usersDb`) indexed by canonical `userId`.
  - Normalizes international phone formats (`+91...`).
  - Supports role switching, KYC status updates, bank verification flags, and Telegram Chat ID associations.
* **Key Endpoints**:
  - `POST /api/users/sync`: Upserts user profiles from Android clients.
  - `GET /api/users/:userId`: Retrieves a user profile by their canonical platform ID.
  - `GET /api/users/by-phone/:phone`: Looks up user credentials by normalized phone number.

### Module B: Authentication & Token Gateways
1. **Truecaller OAuth 2.0 Gateway (`POST /api/auth/truecaller`)**:
   - Performs server-to-server authorization code exchange with `https://oauth-account-noneu.truecaller.com/v1/token`.
   - Uses `code_verifier` (PKCE) and `CLIENT_ID` to receive access tokens.
   - Fetches verified name and phone from `https://oauth-account-noneu.truecaller.com/v1/userinfo`.
   - Automatically auto-provisions a `BORROWER` profile if the user is first-time.
2. **Nodemailer SMTP Email OTP Dispatcher (`POST /api/auth/send-email-otp`)**:
   - Dispatches a 6-digit cryptographic verification OTP to user email addresses.
   - Connected via Gmail SMTP (`smtp.gmail.com:465` SSL) using environment credentials (`GMAIL_USER`, `GMAIL_APP_PASSWORD`).
   - HTML template styled with Loanzo institutional navy and slate branding.

### Module C: DigiLocker KYC Gateway (`/api/kyc/digilocker/*`)
* **Integration Partner**: [Sandbox.co.in](https://api.sandbox.co.in) (Official API Aggregator for Government of India DigiLocker).
* **Workflow**:
  1. **Session Initialization (`POST /api/kyc/digilocker/init`)**:
     - Client requests session initialization.
     - Backend authenticates with Sandbox via `x-api-key` and `x-api-secret`.
     - Generates an authorized government DigiLocker consent redirect URL (`sandbox.co.in/kyc/...`).
  2. **Status Polling (`GET /api/kyc/digilocker/status/:sessionId`)**:
     - Android app WebView checks if the user has completed biometric/OTP authentication on UIDAI servers.
  3. **Verification & Data Extraction (`POST /api/kyc/digilocker/verify`)**:
     - Pulls certified Aadhaar XML / PAN data from the government repository.
     - Extracts masked Aadhaar (`XXXX-XXXX-1234`), legal name, date of birth, and permanent address.
     - Marks `kycStatus = "VERIFIED"` and records hardware audit timestamps.

### Module D: Telegram Command & Notification Engine (`@Loanzo_bot`)
* **Purpose**: Provides administrative command control, real-time borrower/lender alerts, and institutional monitoring.
* **Endpoints**:
  - `POST /api/telegram/webhook`: Receives incoming Telegram update events (`message`, `callback_query`).
  - `POST /api/telegram/notify`: Allows Android client to send out-of-band alerts (disbursal notifications, overdue warnings, agent dispatches).
* **Interactive Bot Commands**:
  - `/start`: Welcomes user and links their Telegram account to their Loanzo user ID.
  - `/myloans`: Fetches active borrowings, reducing-balance EMIs, and due dates.
  - `/statement`: Returns an itemized PDF/text summary of past transactions.
  - `/admin`: Unlocks SuperAdmin console for verified admin accounts.
  - `/pendingkyc`: Lists unverified users for administrative audit.
  - `/broadcast`: Sends priority alerts to all registered channel participants.
* **Inline Action Buttons**:
  - `pay_loan_<id>`: Generates instant NPCI UPI deep-link QR code.
  - `dl_note_<id>`: Downloads signed Section 4 NI Act legal Promissory Note.
  - `open_dispute_<id>`: Escalates loan to a human arbitrator or field agent.

### Module E: Meta WhatsApp Business Cloud Webhook (`/webhook`)
* **Purpose**: Supports WhatsApp-based OTP verification and repayment alerts.
* **Endpoints**:
  - `GET /webhook`: Hub verification challenge for Meta API webhook configuration (`hub.challenge`).
  - `POST /webhook`: Handles incoming WhatsApp messages and status receipts (`sent`, `delivered`, `read`).

---

## 3. Complete Endpoint Reference Catalog

| HTTP Method | Route Endpoint | Purpose | Request Body / Params | Expected Response |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/users/sync` | Sync local Room user to cloud memory | JSON: `UserEntity` payload | `{ success: true, message: "..." }` |
| `GET` | `/api/users/:userId` | Fetch user profile by ID | Path param: `userId` | `{ success: true, user: { ... } }` |
| `GET` | `/api/users/by-phone/:phone`| Lookup profile by phone | Path param: `phone` | `{ success: true, user: { ... } }` |
| `POST` | `/api/auth/truecaller` | Exchange Truecaller OAuth code | `{ authorization_code, code_verifier }` | `{ success: true, user: { ... } }` |
| `POST` | `/api/auth/send-email-otp`| Send 6-digit email OTP | `{ email, otp }` | `{ success: true, messageId: "..." }` |
| `POST` | `/api/kyc/digilocker/init`| Start DigiLocker verification | `{ userId }` | `{ success: true, sessionId, url }` |
| `GET` | `/api/kyc/digilocker/status/:sessionId` | Poll DigiLocker status | Path param: `sessionId` | `{ status: "COMPLETED" \| "PENDING" }`|
| `POST` | `/api/kyc/digilocker/verify` | Finalize KYC and fetch Aadhaar | `{ sessionId, userId }` | `{ success: true, kycData: { ... } }` |
| `POST` | `/api/telegram/webhook` | Telegram Bot update handler | Standard Telegram Update JSON | `{ ok: true }` |
| `POST` | `/api/telegram/notify` | Dispatch message to Telegram user | `{ chatId, message, inlineKeyboard }` | `{ success: true, messageId }` |
| `GET` | `/webhook` | Meta WhatsApp verification | Query: `hub.mode, hub.challenge` | Returns plain `hub.challenge` |
| `POST` | `/webhook` | Meta WhatsApp event receiver | WhatsApp Webhook JSON | `{ status: "EVENT_RECEIVED" }` |
| `GET` | `/` | Health check & landing page | None | Institutional HTML dashboard |
| `GET` | `/privacy` | Legal Privacy Policy | None | Plain-English privacy disclosures |
| `GET` | `/terms` | Legal Terms of Service | None | Section 4 NI Act legal terms |

---

## 4. Environment Variables & Secret Resolution

The backend implements a **fail-safe 3-tier secret resolution hierarchy**:
1. **Tier 1 (Host Environment)**: `process.env` (Configured in Vercel Dashboard).
2. **Tier 2 (Local Development)**: [`backend/.env.local`](file:///c:/AndroidProjects/loanzo/backend/.env.local).
3. **Tier 3 (Android Project Config)**: [`local.properties`](file:///c:/AndroidProjects/loanzo/local.properties).

```
+-----------------------------------------------------------------------------------------+
|                                ENVIRONMENT SECRET CATALOG                               |
+----------------------+------------------------------------------------------------------+
| Variable Name        | Description & Usage                                              |
+----------------------+------------------------------------------------------------------+
| TELEGRAM_BOT_TOKEN   | HTTP API token issued by @BotFather for @Loanzo_bot operations  |
| SANDBOX_API_KEY      | API Key for api.sandbox.co.in DigiLocker authentication          |
| SANDBOX_API_SECRET   | API Secret for api.sandbox.co.in DigiLocker authentication       |
| CLIENT_ID            | Truecaller OAuth Application Client ID                           |
| GMAIL_USER           | Google Workspace / Gmail address for sending system OTPs         |
| GMAIL_APP_PASSWORD   | 16-character Google App Password (bypasses 2FA for SMTP)        |
| WHATSAPP_VERIFY_TOKEN| Custom verification token for Meta WhatsApp Webhook handshake    |
| PORT                 | Server port (defaults to 3000 locally, dynamically set by Vercel)|
+----------------------+------------------------------------------------------------------+
```

---

## 5. Security & Regulatory Compliance Guardrails

1. **Non-Custodial Architecture (Rs 0.00 Fund Storage)**:
   - The backend does **not** maintain customer wallets, virtual balances, or escrow pooling accounts.
   - All transactions are routed directly between bank accounts via NPCI UPI VPAs.
   - Complies 100% with **Section 45-IA of the RBI Act (1934)** and **RBI Digital Lending Guidelines (2022)**.
2. **Zero Plaintext Sensitive Storage**:
   - Aadhaar numbers are never stored in full; only the last 4 digits are retained for verification receipts (`XXXX-XXXX-1234`).
   - Passwords and PINs are hashed using SHA-256 before transmission.
3. **CORS & Network Hardening**:
   - Standard CORS policy enabled with strict HTTP header validation.
   - Rate-limiting headers and timeouts enforced on external Sandbox and Telegram API calls to prevent DDoS cascading failures.

---

## 6. How to Run, Test & Deploy

### Running the Backend Locally
```bash
# 1. Navigate to backend directory
cd c:\AndroidProjects\loanzo\backend

# 2. Install dependencies
npm install

# 3. Start local development server (Port 3000)
node server.js

# 4. (Optional) Run Telegram Long-Polling Daemon for active bot updates
node bot_polling_service.js
```

### Running Backend Diagnostics & Bot Tests
```bash
# Execute standalone test suite verifying Telegram Bot API connectivity
node test_telegram_bot.js
```

### Deploying Updates to Production (Vercel)
Since the repository is linked to Vercel:
1. Ensure changes are tested locally:
   ```bash
   node -e "require('./server.js')"
   ```
2. Commit and push to the `main` branch on GitHub:
   ```bash
   git add backend/
   git commit -m "feat(backend): update endpoint and webhook handler"
   git push origin main
   ```
3. Vercel automatically detects the push and deploys the new serverless build to:
   `https://backend-blond-sigma-66.vercel.app`
