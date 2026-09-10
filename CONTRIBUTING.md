# Contributing to Loanzo

Thank you for your interest in contributing to Loanzo! We welcome contributions from developers, security researchers, financial engineers, and designers.

To maintain code quality, security standards, and architectural consistency, please review the guidelines below before submitting a pull request.

---

## Code of Conduct

We are committed to providing a welcoming, constructive, and harassment-free environment for all contributors. Respectful collaboration, rigorous technical review, and professional communication are expected at all times.

---

## Getting Started

### Prerequisites
- **Android Studio Ladybug (2024.2.1+)** or higher
- **JDK 17** (Temurin or OpenJDK recommended)
- **Android SDK Platform 34** (API 34) & Build Tools
- **Git**

### Fork & Clone
1. Fork the repository on GitHub.
2. Clone your fork locally:
   ```bash
   git clone https://github.com/your-username/loanzo.git
   cd loanzo
   ```
3. Open the project in Android Studio and let Gradle sync complete.
4. Verify your local setup builds cleanly:
   ```bash
   ./gradlew assembleDebug
   ```

---

## Development Workflow

### 1. Branch Naming Conventions
Always create a feature or bugfix branch off `main`:
- `feature/<feature-name>` (e.g., `feature/upi-autopay-ledger`)
- `fix/<bug-description>` (e.g., `fix/biometric-button-padding`)
- `refactor/<scope>` (e.g., `refactor/vault-crypto-engine`)
- `docs/<doc-topic>` (e.g., `docs/architecture-update`)

### 2. Coding Standards
- **Language**: Kotlin 2.0+ with modern idioms.
- **UI Toolkit**: 100% Jetpack Compose using Material 3 and the design system in `com.loanzo.app.ui.theme`.
- **Architecture**: Single Activity (`MainActivity`), unidirectional data flow with `ViewModel`, Kotlin `StateFlow`, and Room Database DAOs.
- **Dependency Injection**: Dagger Hilt (`@HiltViewModel`, `@Inject`, `@Singleton`).
- **Text & Layout**: Always enforce `maxLines = 1`, `softWrap = false`, and `overflow = TextOverflow.Ellipsis` for chips, metric tiles, and badges to prevent mid-word layout wrapping.
- **Security & Privacy**: Never commit private API keys, service account JSON credentials with production secrets, or unmasked biometric data.

### 3. Running Verification
Before opening a pull request, ensure all checks pass:
```bash
./gradlew compileDebugKotlin
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

---

## Submitting a Pull Request

1. Push your branch to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```
2. Open a Pull Request against the `main` branch.
3. Fill out the **Pull Request Template** completely, detailing:
   - Problem statement & summary of changes
   - Impacted components (Data / Domain / UI / Backend)
   - Testing steps and verification evidence
4. Link any related issues (`Fixes #123`).

---

## Security Vulnerabilities
Please do **NOT** file public GitHub issues for security vulnerabilities. Review our [Security Policy](SECURITY.md) for responsible disclosure guidelines.
