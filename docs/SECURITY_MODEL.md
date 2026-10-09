# Security and recovery model

Android: random data key, authenticated AES-GCM vault with per-use Android Keystore wrapping-key authentication. Strong biometrics/device credentials authorize decrypt; real vault screens set FLAG_SECURE and lock immediately on background. Public demo intentionally permits screenshots for documentation and contains only public test material. Local payload v2 encrypts folder, favorite, recovery checklist and drill timestamp alongside setup keys. Legacy payload v1 is accepted.

Web: AES-GCM browser vault protected by a separate vault password, with Argon2id 64 MiB/t=3/p=1. Account SDK is loaded only if configured. Demo is memory-only, random-key encrypted and isolated from real local storage. Focus mode hides display and blocks copying hidden accounts; it does not remove keys from an already unlocked session. Code generation uses the device clock, with a session clock-jump warning, not a trusted network time source.

Portable backups retain SENTINEL-BACKUP-1, AES-GCM, fresh salt/nonce and Argon2id. They preserve standard accounts/setup keys and exclude organization/checklist/drill history. Test a restore and service sign-in before deleting an original vault. Account password resets do not decrypt vaults. Clearing browser data or uninstalling can destroy local accounts.

Limitations: no cloud sync, passkeys, phishing-resistant authentication or administrator recovery. Real email/social login is pending provider configuration. Clipboard history, rooted devices and compromised browsers/OS remain outside these protections. Recovery readiness is a self-recorded checklist. Pending fresh copy is cancelled by locking/backgrounding; no code should be written later from a locked session.

Signing key/password, OAuth provider secrets and all real user vaults are excluded from the repository. The release is a development-signed beta, not an independent security certification.
