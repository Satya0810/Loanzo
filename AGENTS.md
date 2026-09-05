# Loanzo Autonomous Agent Rules & Architectural Standards

## 1. Living Documentation Mandate
- **Keep Documenting Current Changes**: Every pull request, commit, feature addition, schema migration, or bug fix must be accompanied by comprehensive documentation updates across:
  - `docs/ProjectStructure.md`
  - `docs/SRS.md`
  - `docs/SystemArchitecture.md`
  - `docs/TestingStrategy.md`
  - Root `README.md`
- **Track & Verify Integrity**: Actively cross-reference code implementations with documentation. Confirm that all new routes, Room entities, DAOs, repositories, and UI components are accurately indexed and verified.
- **Diagrams & Visuals**:
  - Always utilize **Mermaid diagrams** (`flowchart`, `sequenceDiagram`, `stateDiagram-v2`, `erDiagram`) to visualize lifecycles, role permissions, state transitions, and security isolation.
  - Embed or reference UI screenshots, comic artwork, and document previews for maximum clarity.

## 2. Multi-Role Segregation & Security
- **Role Isolation**: Maintain strict compartmentalization between consumer roles (`BORROWER`, `LENDER`) and certified field agent roles (`AGENT`).
- Certified field agents must never have access to consumer borrowing, lending, or community feeds.
- Master Admin console (`AppOwnerVerificationScreen.kt`) is strictly gated for `@satyam0810` / `+917061559039`.

## 3. UI/UX Excellence & Text Formatting
- **Text Wrapping & Truncation**: Strictly apply `maxLines = 1`, `softWrap = false`, and `overflow = TextOverflow.Ellipsis` to chips, badges, and action buttons to prevent mid-word wrapping.
- **Design Language**: Follow the obsidian cyberpunk/fintech theme (Navy900 `#0B111E`, Emerald400 `#34D399`, Gold500 `#F59E0B`, Cyan400 `#22D3EE`).

## 4. Git & Desktop Mirroring
- Ensure all source code changes in `c:\AndroidProjects\loanzo\app\src` are synchronized via Robocopy to `C:\Users\hp\OneDrive\Desktop\Loanzo\app\src` prior to staging and pushing commits to GitHub `main`.
