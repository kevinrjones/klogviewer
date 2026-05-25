# ADR-036: Secret Storage Decisions for Remote Connections

## Status
Accepted

## Date
2026-05-25

## Context
Sprint 8 introduced persistent remote connection profiles for SFTP and AWS S3. These profiles can contain sensitive values:
- SFTP password (`SftpAuth.Password.password`)
- SFTP key passphrase (`SftpAuth.KeyPair.passphrase`)
- S3 secret key (`S3Auth.Explicit.secretKey`)

Persisting these secrets directly in `preferences.json` would expose credentials at rest. We also need behavior that works across macOS, Linux, and Windows while preserving compatibility with existing preference and connection models.

## Decision
1. **Protect secrets at the persistence boundary**
   - Secret protection is performed inside `JsonPreferencesRepository` via `CredentialProtectionService`.
   - Runtime models remain unchanged; secrets are transformed only when saving/loading preferences.

2. **Use OS-native secure stores first**
   - `SecureCredentialStore` defines the abstraction.
   - `OsKeychainCredentialStore` provides platform handlers:
     - macOS: `security`
     - Linux: `secret-tool`
     - Windows: PowerShell `PasswordVault`

3. **Persist marker values instead of raw secrets when secure storage succeeds**
   - Persist `__KLOGVIEWER_KEYCHAIN__` in `preferences.json`.
   - Resolve marker values back to real secrets on load.

4. **Use deterministic credential references**
   - Credential account keys are derived from connection names (`CredentialReferences`).
   - This supports idempotent put/get/delete behavior and stable lookup across restarts.

5. **Require explicit user consent for plaintext fallback**
   - If secure storage is unavailable/fails, saving returns `PreferencesSaveResult.RequiresPlaintextSecretConfirmation`.
   - Plaintext secret persistence is only allowed when `PreferencesSaveOptions.allowPlaintextSecretFallback = true` after explicit UI confirmation.

6. **Clean up stale secrets**
   - When saved connection profiles are removed, `CredentialProtectionService.cleanupRemovedCredentials(...)` deletes corresponding keychain entries.

## Consequences
### Positive
- Secrets are not silently written in plaintext when secure storage is unavailable.
- Credential handling is consistent across macOS/Linux/Windows using a shared contract.
- Existing SFTP/S3 connection models and call sites remain stable.
- Preferences files remain portable and readable while sensitive values stay externalized when secure storage works.

### Negative / Trade-offs
- Platform implementations rely on OS tooling/commands; behavior depends on host configuration.
- User flow is more complex when secure storage is unavailable (consent prompt required).
- Marker-based persistence introduces additional load/save resolution logic.

## Alternatives Considered
1. **Always fail save when keychain is unavailable**
   - Rejected: too disruptive for users in constrained environments.

2. **Always fallback silently to plaintext**
   - Rejected: unacceptable security posture and no user control.

3. **Build an application-managed encryption layer for fallback**
   - Deferred: increases key-management complexity and is outside current sprint scope.

## Follow-up
- Evaluate replacing command-line based OS integrations with safer native APIs where feasible to further reduce local exposure risk.