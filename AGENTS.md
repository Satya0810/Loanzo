# Loanzo Engineering & Collaboration Standards (AGENTS.md)

Welcome to the **Loanzo Protocol** workspace. This repository contains both the **Loanzo Android FinTech Application** (`/app`) and the official **VibeCon 2.0 / Hackathon Pitch Decks and Specifications** (`/scratch`, `/docs`).

---

## 1. Core Operating Principles

### A. Plan & Confirm Before Large Visual or Structural Changes
* **No Surprises**: Before rewriting a slide deck, reorganizing screen flows, or adding complex diagrams, present a concise outline/plan to the user.
* **Preserve Working Baselines**: Never overwrite an approved presentation or stable codebase without preserving a rollback baseline.

### B. Aesthetic Identity: Clean Institutional FinTech
* **Presentations & Diagrams**:
  - Background: Soft Slate White (`#F8FAFC`).
  - Cards: Crisp White (`#FFFFFF`) with thin slate borders (`#CBD5E1`).
  - Typography: Deep Navy Headings (`#0F172A`), Slate Body (`#475569`).
  - Primary Accent: Royal Institutional Navy (`#1E40AF`).
  - Pill Badges: Ice Blue Tint (`#EFF6FF`).
* **Strict Anti-Patterns**:
  - **NO neon, magenta, or loud jewel-tone fills** on cards or banners.
  - **NO AI-generated raster flowcharts** for pitch decks. All flowcharts and system trees must be built with crisp native PowerPoint vector shapes (`MSO_SHAPE.ROUNDED_RECTANGLE` + arrows).
  - **NO text overflows, tiny unreadable fonts, or overlapping containers**.
  - All slides must be visually verified via image export (`SlideX.JPG`) before completion.

---

## 2. Codebase Architecture (Android & Kotlin)
* **Pattern**: Clean MVVM (Jetpack Compose UI + ViewModels + Room DAOs + Repository/Utility layers).
* **State**: Unidirectional data flow using Kotlin `StateFlow<T>` with immutable UI exposure.
* **Theming**: Strict adherence to Material 3 design tokens.
* **Financial Precision**: Decimal arithmetic, immutable transaction ledgers, Section 4 NI Act legal promissory notes, and offline-first Room persistence.

---

## 3. Documentation & Verification Guardrails
* **Living Documentation**: Any structural update to entities, DAOs, or architecture must be immediately reflected in `docs/SRS.md`, `docs/SystemArchitecture.md`, and `docs/ProjectStructure.md`.
* **Diagrammatic Clarity**: Complex multi-agent or state flows must use clear Mermaid diagrams.
* **Audit on Completion**: Always verify that all changes compile, render cleanly, and fulfill the user's specific goals.

---

## 4. Active Custom Skills
* [`presentation-design`](file:///c:/AndroidProjects/loanzo/.agents/skills/presentation-design/SKILL.md): Strict guidelines and coordinate math for PowerPoint pitch decks.
* [`compose-android-architecture`](file:///c:/AndroidProjects/loanzo/.agents/skills/compose-android-architecture/SKILL.md): Jetpack Compose, Room DB, and FinTech business logic standards.
* [`fintech-security-and-crypto`](file:///c:/AndroidProjects/loanzo/.agents/skills/fintech-security-and-crypto/SKILL.md): SHA-256 signatures, Section 4 NI Act note tamper-proofing, Aadhaar masking, biometrics, and RBAC.
* [`fintech-math-and-escrow`](file:///c:/AndroidProjects/loanzo/.agents/skills/fintech-math-and-escrow/SKILL.md): BigDecimal financial arithmetic, EMI formulas, 5-class collateral haircuts/LTV, and Order XXXVII CPC legal state machines.
* [`android-unit-testing-and-mocking`](file:///c:/AndroidProjects/loanzo/.agents/skills/android-unit-testing-and-mocking/SKILL.md): Coroutine test dispatchers, StateFlow testing, MockK, and Room in-memory database testing.
* [`speech-and-vernacular-nlp`](file:///c:/AndroidProjects/loanzo/.agents/skills/speech-and-vernacular-nlp/SKILL.md): VoiceTextSanitizer rules, Indian currency/acronym TTS pronunciation, and Hinglish voice intent parsing.
* [`mermaid-diagramming`](file:///c:/AndroidProjects/loanzo/.agents/skills/mermaid-diagramming/SKILL.md): Syntax-safe Mermaid architectural diagrams, multi-party sequence flows, and state machines.
* [`documentation`](file:///c:/AndroidProjects/loanzo/.agents/rules/documentation.md): Real-time documentation tracking and living architecture updates.
* [`design-and-presentation`](file:///c:/AndroidProjects/loanzo/.agents/rules/design-and-presentation.md): Aesthetics and color rules for visual deliverables.
