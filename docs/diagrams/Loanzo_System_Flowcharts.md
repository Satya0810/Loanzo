# 🏛️ Loanzo System Architecture & Core Flowcharts

> Comprehensive technical blueprints, operational lifecycle flowcharts, and multi-model AI architecture documentation for **Loanzo** — India's premier P2P social lending and micro-credit community platform.

---

## 📑 Table of Contents
1. [End-to-End P2P Lending Lifecycle](#1-end-to-end-p2p-lending-lifecycle)
2. [AI Copilot & Multi-AI Race Engine Architecture](#2-ai-copilot--multi-ai-race-engine-architecture)
3. [Identity & 3-Step KYC Verification Flow](#3-identity--3-step-kyc-verification-flow)
4. [Certified Agent Field Inspection & Collateral Assaying](#4-certified-agent-field-inspection--collateral-assaying)
5. [Smart Mediation Desk & RBI Hardship Restructuring](#5-smart-mediation-desk--rbi-hardship-restructuring)
6. [Statutory Compliance & Legal Security Matrix](#6-statutory-compliance--legal-security-matrix)

---

## 1. End-to-End P2P Lending Lifecycle

```mermaid
graph TD
    classDef startEnd fill:#2563EB,stroke:#1D4ED8,stroke-width:2px,color:#fff;
    classDef process fill:#F3F4F6,stroke:#D1D5DB,stroke-width:1.5px,color:#111827;
    classDef decision fill:#FEF3C7,stroke:#F59E0B,stroke-width:2px,color:#92400E;
    classDef success fill:#D1FAE5,stroke:#10B981,stroke-width:2px,color:#065F46;

    Start([👤 Borrower Initiates Loan Request]):::startEnd --> KYC{KYC Status Verified?}:::decision
    KYC -- No --> DoKYC[🛡️ Complete 3-Step KYC Verification]:::process
    DoKYC --> KYC
    KYC -- Yes --> PostReq[📝 Publish Listing in Community Marketplace]:::process

    PostReq --> RuleCheck{Usury Rate Check<br/>9%-18% State Cap}:::decision
    RuleCheck -- Exceeds Cap --> AdjRate[⚠️ Adjust Rate to State Statutory Limit]:::process
    AdjRate --> PostReq
    RuleCheck -- Compliant --> Wall[🛒 Live on Marketplace Wall]:::process

    Lender([💼 Verified Lender]):::startEnd --> Wall
    Lender --> ProposeBid[🤝 Submit Competitive Loan Bid]:::process
    ProposeBid --> BorrowRev{Borrower Accepts Bid?}:::decision
    
    BorrowRev -- Rejects/Counter --> ProposeBid
    BorrowRev -- Accepts --> GenContract[📄 Generate Key Fact Statement & Contract]:::process

    GenContract --> ESign[✍️ Dual-Party Aadhaar eSignature]:::process
    ESign --> AuditLog[🔒 SHA-256 Hash Archived to Vault]:::process
    
    AuditLog --> Disburse[🏦 Tranche Disbursed via Electronic Banking UPI/IMPS]:::process
    Disburse --> Servicing[📊 Active Servicing & Smart Portfolio Tracking]:::process

    Servicing --> Repay[💳 Borrower Pays EMI & Inputs UTR Ref]:::process
    Repay --> AllPaid{All EMIs Completed?}:::decision
    AllPaid -- No --> Servicing
    AllPaid -- Yes --> NOC[🎉 Digital No Objection Certificate Issued]:::success
    NOC --> End([🏁 Loan Contract Successfully Closed]):::startEnd
```

---

## 2. AI Copilot & Multi-AI Race Engine Architecture

```mermaid
sequenceDiagram
    autonumber
    actor User as 👤 App User (Borrower/Lender)
    participant UI as 📱 ChatScreen & Action Bar
    participant VM as ⚙️ ChatViewModel
    participant DB as 🗄️ Room DB (UserDao & LoanDao)
    participant Race as 🏁 MultiAiRaceEngine
    participant AI1 as ⚡ LLM7.io (Llama-3.3-70B)
    participant AI2 as 🚀 SambaNova (Fast Inference)
    participant AI3 as ☁️ Cloudflare Workers AI
    participant Fallback as 🛡️ Offline Heuristic Guard

    User->>UI: Types: "What is my next EMI & how to repay?"
    UI->>VM: dispatchAiAssistantResponse(channelId, userId, prompt)
    
    rect rgb(240, 249, 255)
        Note over VM,DB: Account RAG Context Gathering
        VM->>DB: Fetch user profile (Role, KYC Status)
        VM->>DB: Fetch active loans & repayment schedule
        DB-->>VM: Returns live account financial metrics
        VM->>VM: Compile structured userContext & conversation history
    end

    rect rgb(254, 243, 199)
        Note over VM,Race: Parallel Multi-Model Race
        VM->>Race: raceQuery(prompt, userContext, history)
        par Concurrent Dispatch
            Race->>AI1: POST /v1/chat/completions (Stream)
        and
            Race->>AI2: POST /v1/chat/completions (Fast)
        and
            Race->>AI3: POST @cf/meta/llama-3.1-8b-instruct
        end
        AI1-->>Race: ⚡ Winner (Responded first in 420ms)
        Race->>Race: Cancel slower requests (AI2, AI3)
        alt All Providers Timeout/Fail (>7000ms)
            Race->>Fallback: generateResponse(prompt, systemPrompt)
            Fallback-->>Race: In-App Structured Guidance + Action Tags
        end
    end

    Race-->>VM: Returns answer with [ACTION:PORTFOLIO] [ACTION:CALCULATOR]
    VM->>UI: Update Firestore & Chat State
    UI->>UI: Parse action tags & strip from speech text
    UI-->>User: Displays clean AI answer + Clickable [📊 Smart Portfolio] Pill
    User->>UI: Taps [📊 Smart Portfolio]
    UI->>UI: Instant deep-link navigation to SmartPortfolioScreen!
```

---

## 3. Identity & 3-Step KYC Verification Flow

```mermaid
graph LR
    classDef step fill:#EFF6FF,stroke:#3B82F6,stroke-width:1.5px,color:#1E3A8A;
    classDef success fill:#ECFDF5,stroke:#10B981,stroke-width:2px,color:#065F46;

    subgraph Step 1: PAN Validation
        A1[💳 Enter PAN Card Number]:::step --> A2[Verify via Income Tax Route]:::step
        A2 --> A3[Validate Legal Name & DOB Match]:::step
    end

    subgraph Step 2: Aadhaar XML
        B1[🆔 UIDAI DigiLocker / Aadhaar OTP]:::step --> B2[Download Signed eKYC XML]:::step
        B2 --> B3[Verify Digital Signature & Address]:::step
    end

    subgraph Step 3: Facial Liveness
        C1[🤳 Real-Time Camera Scan]:::step --> C2[Blink & Micro-Movement Detection]:::step
        C2 --> C3[Penny-Drop Bank Verification]:::step
    end

    A3 --> B1
    B3 --> C1
    C3 --> Approved[🛡️ Full KYC Completed & Badge Assigned]:::success
```

---

## 4. Certified Agent Field Inspection & Collateral Assaying

```mermaid
graph TD
    classDef agent fill:#FEF3C7,stroke:#D97706,stroke-width:1.5px,color:#78350F;
    classDef flow fill:#F8FAFC,stroke:#94A3B8,stroke-width:1.5px,color:#0F172A;
    classDef seal fill:#D1FAE5,stroke:#059669,stroke-width:2px,color:#064E3B;

    Req[📑 Loan Application Requires Physical Collateral / Valuation]:::flow --> Dispatch[📍 Auto-Dispatch Certified Local Agent by Pincode]:::agent
    Dispatch --> Visit[🏠 Doorstep Field Visit Scheduled]:::agent

    Visit --> Geo[📡 Geo-Fenced GPS Check-In at Premises]:::flow
    Geo --> Inspection{Type of Appraisal}:::flow

    Inspection -- Gold / Ornaments --> Assaying[🔬 Carat Karatometer Purity Test & Weight Log]:::flow
    Inspection -- Vehicle / Property --> Docs[📋 Registration RC / Title Deed Inspection]:::flow
    Inspection -- Business --> Books[📊 Inventory Verification & Bank Statement Check]:::flow

    Assaying --> Photo[📸 Tamper-Proof Photos with Timestamp Overlay]:::flow
    Docs --> Photo
    Books --> Photo

    Photo --> SignReport[✍️ Field Agent Cryptographically Signs Report]:::agent
    SignReport --> Vault[🔒 Appraisal Dossier Sealed in Loanzo Vault]:::seal
    Vault --> Lenders[💼 Available to Verified Lenders for Underwriting]:::seal
```

---

## 5. Smart Mediation Desk & RBI Hardship Restructuring

```mermaid
graph TD
    classDef distress fill:#FEE2E2,stroke:#DC2626,stroke-width:2px,color:#991B1B;
    classDef med fill:#FEF3C7,stroke:#F59E0B,stroke-width:1.5px,color:#78350F;
    classDef plan fill:#E0E7FF,stroke:#4F46E5,stroke-width:1.5px,color:#312E81;
    classDef resolved fill:#D1FAE5,stroke:#10B981,stroke-width:2px,color:#065F46;

    Hardship([⚠️ Borrower Faces Unforeseen Financial Distress]):::distress --> Trigger[Tap 'Request Mediation' on Active Loan]:::med
    Trigger --> CodeGuard[🛡️ RBI Fair Practices Code Protection Activated<br/>Zero Coercive Calls & Harassment Freeze]:::med

    CodeGuard --> SelectCause{Distress Cause}:::med
    SelectCause -- Medical Emergency --> MedEv[🏥 Hospital Bills & Proof Uploaded]:::med
    SelectCause -- Job Loss / Layoff --> JobEv[💼 Termination Letter Uploaded]:::med
    SelectCause -- Crop / Business Loss --> BizEv[🌾 Market Loss Documentation]:::med

    MedEv --> Propose[🤝 Smart Mediation Desk Formulates Restructure Plan]:::plan
    JobEv --> Propose
    BizEv --> Propose

    Propose --> Options[1. Moratorium: 1-3 Months Payment Freeze<br/>2. Tenure Extension: Reduced Monthly EMI<br/>3. Interest Rate Concession]:::plan

    Options --> LenderVote{Lender Reviews Proposal}:::med
    LenderVote -- Agree --> DigitalAddendum[✍️ Digital Contract Addendum Signed]:::resolved
    LenderVote -- Counter --> CounterTerms[🔄 Agent Facilitates Mutual Compromise]:::plan
    CounterTerms --> DigitalAddendum

    DigitalAddendum --> NewSchedule[📊 Updated Repayment Schedule Reflected in Smart Portfolio]:::resolved
```

---

## 6. Statutory Compliance & Legal Security Matrix

| Regulatory Requirement | Governing Statute / Rule | Loanzo Technical Implementation |
| :--- | :--- | :--- |
| **P2P Interest Rate Caps** | State Money Lenders Acts (e.g., Maharashtra 9%-12%, Karnataka 14%-18%) | `RuleEngine.kt` auto-caps loan listings and bids. Listings exceeding caps are rejected. |
| **Cash Transaction Ceiling** | Income Tax Act, 1961 (Sections 269SS & 269T) | Loans & repayments ≥ ₹20,000 strictly enforced via electronic banking routes (UPI/NEFT/RTGS). |
| **Digital Contract Enforceability** | Indian Contract Act, 1872 & Information Technology Act, 2000 (Sec 10A) | Digital loan agreements backed by Aadhaar OTP eSignatures and tamper-evident SHA-256 audit trails. |
| **Key Fact Statement (KFS)** | RBI Digital Lending Regulatory Framework (2022 Guidelines) | Automated instant KFS sheet summarizing all-inclusive APR, EMI schedule, and penal charges upfront. |
| **Fair Recovery Practices** | RBI Fair Practices Code for NBFCs & Digital Lenders | In-app Smart Mediation Desk for distress restructuring. Strict prohibition of third-party intimidation. |
| **Data Privacy & Vault Security** | Digital Personal Data Protection Act, 2023 (DPDPA) | End-to-end encrypted Document Vault; biometric templates and PAN/Aadhaar never shared with counterparties. |
