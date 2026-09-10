# Security Policy

Security and user trust are foundational to Loanzo. As a decentralized peer-to-peer microfinance protocol handling sensitive financial instruments, collateral records, and identity documents, we maintain strict defensive engineering standards.

---

## Supported Versions

Security patches and hotfixes are applied to the active development branch (`main`) and official release tags.

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

---

## Architectural Security Model

Loanzo incorporates defense-in-depth across the client and network layers:

1. **Biometric Zero-Knowledge Gateway**:
   - Cryptographic keys for the Encrypted Document Vault are secured via Android Keystore (`KeyGenParameterSpec`) backed by hardware StrongBox / TEE.
   - Requires user biometric authentication (`BiometricPrompt`) for every vault decryption event.

2. **Cryptographic e-Contract Immutability**:
   - Digital Promissory Notes are signed with SHA-256 digests and timestamped audit logs.
   - Any document tampering invalidates the verification hash instantly.

3. **Role Compartmentalization**:
   - Strict runtime isolation between Consumer roles (`BORROWER`, `LENDER`) and Field Agent roles (`AGENT`).
   - Consumer data access is permission-scoped and never exposed to unassigned field agents.

4. **Network & Transport Security**:
   - TLS 1.3 enforced across all backend APIs and WebRTC signaling servers.
   - Certificate pinning and token revocation checks on sensitive financial transactions.

---

## Reporting a Vulnerability

If you discover a security vulnerability, please report it responsibly:

- **Email**: `security@loanzo.finance` (or contact project maintainer `@satyam0810`)
- **Subject**: `[SECURITY VULNERABILITY] <Component/Subsystem>`
- **Details to Include**:
  - Description of the vulnerability and its potential impact
  - Step-by-step reproduction instructions or proof-of-concept (PoC)
  - Affected device configurations, Android API levels, or network conditions

### Our Commitment
- We will acknowledge receipt of your vulnerability report within **48 hours**.
- We will provide a status update and remediation timeline within **5 business days**.
- We request that you maintain confidentiality until an official fix is deployed.
- Responsible security researchers will be credited in our release notes.
