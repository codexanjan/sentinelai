# System Architecture for the 2FA Authenticator App

Version 1.0 | Local release architecture with a separately gated cloud extension

## System context
Third-party service -- provisioning QR or setup key --> Android app.
Android app -- locally generated OTP, entered by user --> third-party service.
Android app -- authenticated encrypted backup --> user-selected file destination.
Android app -- optional ciphertext over TLS --> sync API --> ciphertext store.
Android app -- authentication and wrapping operation --> Android system and Keystore.

The service secret enters the app through an untrusted parser boundary. The camera decoder never stores frames. The cloud boundary contains encrypted payloads and account metadata only. A file destination can be remote storage selected by the user, so export encryption must complete before bytes leave the app.

## Modules and ownership
- app: dependency composition, navigation, lifecycle and Android manifest.
- feature onboarding: education and first-vault setup.
- feature vault: list, search, copy and management.
- feature enrollment: scanner, manual input and preview.
- feature security: system authentication and privacy settings.
- feature backup: file chooser, password UX, restore preview.
- core otp: deterministic OTP generation with injected clock.
- core provisioning: URI parser and validation.
- core crypto: encryption envelopes and platform key adapters.
- data vault: serialization, transactions, migrations and repository.
- feature sync and data sync: P1 authentication, recovery and conflict operations.

UI depends on domain interfaces; domain depends on abstractions, and data adapters implement them. The OTP core has no network, logging, database or UI dependency. An unlocked session is required before data access. Test doubles replace clocks, file storage and authentication for integration tests; physical devices verify real key behavior.

## Local data design
Files: vault.enc, wrapped-key.enc, vault-last-good.enc during controlled recovery, and nonsensitive preferences. Private app storage contains no plaintext seeds. The encrypted payload includes all labels and settings that reveal account identity. Envelope metadata contains version, nonce, opaque vault UUID and key version. Decrypted records exist only inside the unlocked session.

At <=1,000 accounts, an encrypted snapshot is a reasonable proposed starting point. Benchmark full serialization and decrypt under the agreed targets before acceptance. If scale or latency requires a record-level encrypted database, introduce it through an ADR and migration tests; do not leave labels plaintext merely to accelerate search.

## Sequence for enrollment
1. LockController authenticates and opens an unlocked session.
2. Scanner or manual form produces a bounded transient candidate.
3. ProvisioningParser validates and returns a typed configuration.
4. Review screen displays metadata and parameters, excluding seed by default.
5. VaultRepository checks duplicates inside the session, creates UUID and record revision.
6. VaultCrypto encrypts a new snapshot and data layer commits atomically.
7. UI shows the account and asks the user to finish verification on the provider.

## Sequence for unlock and relock
The entry screen is redacted before any data read. System authentication authorizes the wrapping-key operation. Only after unwrap and authenticated vault decryption succeeds does the session publish records to UI. Backgrounding replaces sensitive UI with a privacy overlay, cancels code display and starts the chosen monotonic timeout. Expiry drops the session; process death always requires another unlock. Export/restore cannot reuse a stale authorization indefinitely.

## Sequence for recovery
If key unwrap fails irrecoverably, show RecoveryRequired with Restore backup, P1 Recover encrypted cloud vault, and Help. Keep the unreadable original intact. File recovery decrypts using the export password and creates a new data key and device wrapper after successful preview and confirmation. Cloud recovery uses cloud login plus the separate recovery key. Do not ask support to receive backup passwords, setup keys or recovery keys.

## P1 backend schema and API
users: internal user UUID, identity subject, status, created timestamp.
devices: device UUID, owner UUID, display alias, last seen, revoked timestamp and session binding.
vaults: vault UUID, owner UUID, revision, format version, ciphertext object reference and updated timestamp.
recovery_envelopes: vault UUID, version and client-encrypted key wrapper.
audit_events: event UUID, owner UUID, event type and timestamp; no payload, label, seed, code or recovery key.

POST /v1/devices registers an authenticated device. GET /v1/devices lists the owner's devices. DELETE /v1/devices/{id} revokes that owner's device and sessions.
GET /v1/vault returns the owner's encrypted envelope and revision. PUT /v1/vault accepts an encrypted envelope, idempotency key and expectedRevision; a stale revision returns 409, an unauthorized request 401/403, and an oversized envelope 413. GET /v1/recovery-envelope returns the encrypted wrapper. DELETE /v1/vault removes the cloud copy with fresh account reauthentication. Account deletion is a separate endpoint with a documented retention contract.

Authorize ownership at every object operation; never trust a submitted owner ID. Validate versions and sizes without attempting decryption. Use database transactions for revision updates and idempotency records. Rate-limit session and mutation endpoints and revoke refresh sessions on device removal. Store idempotency results bounded by owner and operation.

## Threat model and mitigations
Lost locked phone: platform-authenticated wrapping, encrypted payload, hidden previews and backup recovery. An unlocked stolen phone remains a risk until relock.
Malicious QR/file: strict input limits, fail-closed parameters, no URL navigation, authenticated decryption and parser fuzzing.
Cloud breach: end-to-end encrypted snapshots; attacker can still learn account/device metadata, traffic and approximate vault size. Use opaque IDs and disclose the residual metadata.
Backup theft: independent strong password with Argon2id and authenticated encryption; weak passwords remain vulnerable to offline guessing.
Clipboard interception: sensitive flag and best-effort expiry; encourage direct visual entry for high-risk accounts.
Dependency/build compromise: reviewed pinned packages, SBOM, protected signing key, reproducible build inputs and CI integrity.
Phishing: education and provider recovery guidance; TOTP is not phishing-resistant.

## Architecture decisions and unresolved work
ADR-01 Native Android first to simplify platform key and lifecycle integration; iOS is a future platform, not a promised shared build.
ADR-02 Offline core with no server dependency; optional sync cannot affect OTP availability.
ADR-03 Whole-vault encryption protects labels and simplifies atomic mutations; performance must be measured.
ADR-04 Platform device credentials rather than a separate short app PIN reduce independent credential handling. A custom PIN feature requires another reviewed key derivation and recovery design.
ADR-05 Portable encrypted file backup is mandatory in P0; P1 cloud recovery adds a separate key and protocol review.
Unresolved before P1: key-pairing protocol, identity provider, tombstone retention, old-client rollback detection, metadata retention and operational backup deletion delay. A malicious server can replay older valid ciphertext; clients should retain their highest observed revision, but a fresh recovery client needs an independently authenticated checkpoint for stronger rollback detection. Document this limit and resolve the policy before making integrity guarantees.

## Operations and release architecture
P0 distributes a signed Android app and maintains no service. CI performs builds, unit tests, lint, dependency review and signed artifact verification. Keep signing credentials in a managed protected store separate from source control.
P1 adds separate development, staging and production projects, least-privilege service roles, metadata/ciphertext backups, migration rehearsals and redacted monitoring. Proposed P1 objectives: 99.9 percent monthly service availability and <=24 hour backup recovery point; these require provider and budget confirmation. Core offline use has no cloud availability SLA dependency. A cloud rollback cannot erase the local good copy.
