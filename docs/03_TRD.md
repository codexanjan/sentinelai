# Technical Requirements Document for the 2FA Authenticator App

Version 1.0 | Engineering implementation baseline

## Platform and build decisions
Use Kotlin, Jetpack Compose, Android ViewModel and coroutines with unidirectional UI state. Keep OTP and URI logic in plain Kotlin modules independent of Android. Use the JCA HMAC primitives and platform AES-GCM; do not implement cryptographic primitives. Select an actively maintained on-device QR decoder and audited Argon2id binding after checking licenses, maintenance and supported ABIs. Exact library versions and target SDK are build-time decisions requiring current verification.

For P0, use an encrypted whole-vault file with a small repository rather than plaintext Room rows. The expected vault is small, and an atomic snapshot makes encryption, export and later sync easier to reason about. Maintain an in-memory search index only while unlocked. Public preferences such as theme can use DataStore; never put seed material or vault keys there. No backend is necessary for local operation.

## OTP calculation contract
Use a 64-bit Unix-second clock and T = floor((unixSeconds - T0) / period), with T0=0 for the supported provisioning format. Encode T as an unsigned 8-byte big-endian counter. Compute HMAC using the imported algorithm, apply standard dynamic truncation, reduce modulo 10^digits and pad with zeros. Use RFC 6238 Appendix B vectors including SHA1, SHA256 and SHA512, and RFC 4226 vectors to verify the shared truncation core. These details follow the RFCs; product parameter limits are defined in SRS rather than claimed as RFC limits.

remainingSeconds = period - (unixSeconds modulo period); at the boundary display the new code and a full new period. Derive updates from current wall time, not a decremented persisted timer. Recompute on resume and time-change events; use one foreground ticker for the whole list. Use a monotonic clock for lock-timeout duration, distinct from the OTP clock.

## Provisioning parser
Accept only scheme otpauth and host totp for P0. Strictly decode label and query, cap input size, canonicalize Base32 and validate all supplied critical parameters before preview. If issuer prefix and issuer query disagree, require correction instead of silently attributing the account. Reject HOTP with a clear unsupported message. No URI is handed to a browser. Unknown noncritical extension parameters may be ignored with a preview notice; unsupported algorithm/digits/period cannot be ignored. Keep exact parser fixtures for percent-encoded labels, Unicode, duplicate parameters, invalid padding and missing fields.

Do not infer current Google Authenticator behavior from its archived URI wiki. Use that page as a format reference and verify interoperability on actual services during testing. Do not claim proprietary migration QR compatibility unless the adapter is explicitly built and tested.

## Local encryption and key lifecycle
Generate a 256-bit random vault data key with the platform secure RNG. Encrypt the complete vault payload using AES-256-GCM with a unique random 96-bit nonce per encryption. Authenticate envelope version, opaque vault ID and key version as associated data using a canonical encoding. Reuse neither nonce nor ciphertext envelope across different plaintext writes.

Generate a nonexportable Android Keystore wrapping key whose use requires strong biometric or device credential authentication. Use BiometricPrompt and CryptoObject where compatible; implement and test API-specific credential fallback so success authorizes actual unwrap, not just navigation. The wrapping key encrypts the data key, with its own unique nonce and associated data. Prefer hardware backing when available but do not assume every device provides it. Record capability without storing private identifiers.

While unlocked, keep the decrypted data key and minimum needed records in memory. On lock clear references, cancel work that can access secrets, discard search indexes and wipe mutable byte buffers where possible. Managed runtimes cannot guarantee complete memory erasure; the threat model must state this limit. Never persist an unwrapped key. Changing lock policy rewraps safely before committing and requires fresh authentication.

Removing screen lock or changing biometric enrollment may invalidate keys depending on the selected configuration. Test each supported path. Preserve unreadable ciphertext and route to restore; never pretend cloud login can fix the local wrapping key. A rooted or fully compromised operating system can read data during authorized use; platform protection is not a cure for device compromise.

## Crash-safe persistence
Serialize the validated payload, encrypt off the UI thread, write a sibling temporary file, flush, then atomically replace the active vault using a platform-supported atomic-file strategy. Serialize all mutations behind one repository mutex. Keep a last-good encrypted revision during migration and restore until commit verification succeeds. Ensure backups and legacy artifacts are covered by OS exclusion rules. No plaintext temporary file is permitted.

Use a monotonic local revision for mutations. Validate a read by authenticated decryption and schema checks before publishing UI state. Corruption recovery must never overwrite the only copy. Test interruption at every write step and low-space failure. Persistence success precedes enrollment success UI.

## Encrypted portable backup
Proposed envelope: magic AUTHVAULT, formatVersion=1, cipher=AES-256-GCM, KDF=Argon2id, salt 16 random bytes, nonce 12 random bytes, KDF parameters, ciphertext and tag. Export a fresh snapshot encrypted directly under a password-derived 256-bit export key; do not export the device wrapping key. Authenticate all interpretation-critical envelope fields.

Baseline KDF proposal: Argon2id memory 64 MiB, iterations 3, parallelism 1 and output 32 bytes. Benchmark on the lowest supported device before freezing the format; target approximately 0.5-2 seconds. Import only profiles actually shipped, initially this baseline, so malicious headers cannot request unbounded work. Password minimum 12 characters with a recommendation for a long unique passphrase; no arbitrary character-class rules. Specify UTF-8 with no hidden normalization and test Unicode round trips. These are proposed engineering policies and require crypto review.

Wrong password and altered data produce one generic integrity/password error. Read a bounded file, validate its header and supported profile, decrypt to memory, validate records, preview and atomically merge. No auto-upload, password hint, password reset or plaintext recovery channel exists. App files remain decryptable on another device with the password, which is why successful restoration must be demonstrated before release.

## Optional cloud technical contract
Use a conventional authenticated API and relational metadata store plus object storage or a bounded ciphertext column. Suggested implementation: TypeScript service and PostgreSQL with managed OIDC authentication. Providers and pricing are undecided. Backend receives only authenticated ciphertext, opaque identifiers, revisions, encrypted recovery wrappers and minimum device/session metadata; it never generates OTPs.

A separate random vault sync key encrypts cloud snapshots. It is wrapped locally under the authenticated device key and wrapped for recovery using a high-entropy user-held recovery key. Freshly authenticated users must confirm the recovery key before first upload. Existing-device pairing can use a reviewed ephemeral-key protocol with authenticated key agreement and matching short authentication strings; do not ship a homegrown unauthenticated QR key exchange. Recovery key entry on a new device is the simpler first P1 path. Account authentication alone cannot decrypt cloud data.

All networking uses TLS with normal certificate verification. Short-lived access sessions and refresh-token rotation belong to the identity service; store refresh tokens under platform protection. Never require the same authenticator vault as the only login factor for the sync account: provide an independent supported factor and recovery flow to avoid a circular lockout.

Whole-vault sync is serialized: fetch server revision, decrypt locally, merge, upload with expectedRevision. A conflict does not overwrite remote data. Canonical record UUIDs, per-record revisions and tombstones support merges. Independent nonoverlapping record edits merge; divergent seed/parameter changes and delete-versus-edit prompt review. Keep opaque envelope versions stable across clients. An offline client older than the supported tombstone-retention window must perform a full resync rather than replay stale writes.

## Engineering interfaces
Clock.nowUnixSeconds(); OtpEngine.generate(secret, algorithm, digits, period, time); ProvisioningParser.parse(uri); VaultCrypto.encrypt/decrypt/wrap/unwrap; VaultRepository.open/add/update/delete/snapshot/restore; LockController.authenticate/lock/onVisibilityChange; BackupService.export/importPreview/commit; SyncRepository.fetch/merge/push.

Every repository operation verifies unlocked state inside the operation. Domain errors include Locked, PermissionDenied, InvalidProvisioning, UnsupportedParameters, KeyUnavailable, CorruptVault, WrongPasswordOrCorruptBackup, StorageFull and SyncConflict. Return typed errors to UI; redact underlying sensitive input.

## Security and dependency gates
Create a threat model before implementation, pin reviewed dependencies, generate an SBOM, scan for secret leakage and known vulnerable packages, review permissions and exported Android components, and inspect release builds. Use OWASP MASVS/MASTG as a review framework; this is not a claim of certification. A separate reviewer must examine key lifecycle, backup format and sync protocol before production.

## Technical sources
[RFC 6238](https://www.rfc-editor.org/info/rfc6238/) and [RFC 4226](https://www.rfc-editor.org/info/rfc4226/) define the algorithm and test vectors.
[Android Keystore](https://developer.android.com/privacy-and-security/keystore) describes platform key protection and authentication constraints.
[Android backup controls](https://developer.android.com/identity/data/autobackup) inform explicit backup and transfer exclusions.
[Google key URI format](https://github.com/google/google-authenticator/wiki/Key-Uri-Format) is an archived provisioning-format reference.
[OWASP MASVS](https://mas.owasp.org/MASVS/) provides the mobile security verification framework. Sources reviewed 6 October 2026; architecture choices and estimates in this pack are recommendations.
