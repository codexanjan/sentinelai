# Software Requirements Specification for the 2FA Authenticator App

Version 1.0 | Proposed implementation contract | P0 local release and P1 sync extension

## Actors and boundaries
The device owner operates an unlocked vault. Android supplies device authentication, camera access, app sandboxing and key custody. A third-party service provisions secrets and verifies OTPs. An optional cloud account provides authentication and ciphertext storage only. Support staff have no ability to view a secret or recover encryption keys.

## Functional requirements
FR-01 Onboarding P0: show concise education on offline codes, scanning and recovery; allow slide skip; persist completion without retaining sensitive content.

FR-02 Vault creation P0: require a configured secure device credential, generate random vault and wrapping keys, and enter Locked state after creation. When a secure lock is missing, explain the requirement and offer system settings.

FR-03 Unlock P0: use system BiometricPrompt with approved strong biometric or device-credential fallback, and cryptographically authorized key access. Cancellation keeps all account data hidden. UI-only authentication is insufficient.

FR-04 Relock P0: default to immediate lock whenever the app becomes invisible; optional user choices are 30 seconds and 1 minute. Always lock on process restart and screen lock. Sensitive overlays appear before task snapshots. Export and security changes always demand fresh authentication.

FR-05 Scan P0: request camera permission only when scanning. Decode on-device, accept supported otpauth TOTP URIs, stop capture after a valid candidate, and show a review screen before storage. Never open scanned web links or upload images.

FR-06 Manual add P0: require a nonempty account label and valid Base32 secret; permit issuer; default SHA1, 6 digits and 30 seconds; offer advanced fields with clear explanations. Mask the secret and allow an intentional reveal on this screen only.

FR-07 Parameter validation P0: allow SHA1, SHA256 and SHA512; 6 or 8 digits; integer period 15 through 120 seconds as an explicit product limit. Reject unsupported supplied parameters rather than silently substituting defaults. Absent parameters receive defaults. Reject duplicate security-sensitive query parameters, invalid encoding, empty secrets, excessive input and ambiguous issuer mismatches. Base32 accepts case differences and valid optional padding; manual whitespace may be removed before strict decode. Decoded secrets are 10 through 128 bytes; do not truncate.

FR-08 Enrollment confirmation P0: show issuer, label, algorithm, digits and period; saving is a single atomic operation. Warn on identical secret and parameters after decrypting inside the unlocked vault; labels alone are not a duplicate identity. Let the user cancel or knowingly keep a separate entry. No server-visible secret hash is stored.

FR-09 OTP P0: calculate from UTC Unix seconds and configured period using RFC 6238. Display leading zeros and refresh at a boundary using wall time. Timezone changes do not change the OTP; clock changes do. Unsupported HOTP input produces an explanation rather than a TOTP entry.

FR-10 Account list P0: show issuer, account label, code and remaining time; search locally and case-insensitively across issuer and label; sorting is stable and persisted. Locked state hides names as well as codes.

FR-11 Copy P0: copy digits without spaces, mark clipboard data sensitive where supported and show a short confirmation. Schedule best-effort clearing after 30 seconds, only if the clipboard still contains the value written by the app. Explain that other apps may access copied data and expiry is not guaranteed if this app is stopped.

FR-12 Management P0: rename label, reorder and delete without exposing the secret. Deletion requires explicit confirmation including the effect on provider recovery; cancel changes nothing. A persisted transaction updates the encrypted vault and revision together.

FR-13 Export P0: freshly authenticate, require password and confirmation, show unrecoverability warning, create a versioned authenticated encrypted file through system document UI and report success only after write and close succeed. Do not offer plaintext export or share a seed QR in P0.

FR-14 Restore P0: read an encrypted file chosen in system UI, validate envelope limits, authenticate/decrypt, show count and duplicate/conflict preview, and atomically merge after confirmation. Wrong password, invalid tag or malformed file writes nothing. Restore never replaces the current vault automatically.

FR-15 Key loss P0: if device wrapping key is invalidated or unavailable, preserve the ciphertext, lock the vault and explain restore options. Never silently initialize an empty vault over existing data. A reset requires a destructive confirmation.

FR-16 Settings P0: theme, lock timeout, backup and restore, privacy controls, app version and troubleshooting. Do not offer a control that disables cryptographic vault protection. Secret screens remain screenshot-protected on Android.

FR-17 Sync opt-in P1: require explicit consent, cloud authentication independent of vault unlock, and verified recovery-key setup. Upload encrypted payloads only. A cloud-password reset never recovers or replaces the vault encryption key.

FR-18 Devices P1: pair only through an already trusted device or recovery key; show devices and revoke future server access. Warn that revocation cannot erase secrets previously downloaded by another device.

FR-19 Conflict handling P1: use an opaque vault ID and server revision compare-and-swap. A stale update returns conflict, forcing download, local decrypt and explicit merge before retry. Deletion tombstones prevent routine resurrection. Concurrent secret changes require review; never last-write-wins them silently.

FR-20 Cloud deletion P1: separate sign out, disable sync, remove cloud copy, and erase local vault. Require fresh authentication and clear scope. Cloud account deletion removes live ciphertext and sessions; publish actual backup-retention delay before launch.

## States and workflows
Vault states: Uninitialized -> Locked -> Unlocking -> Unlocked -> Locked. Unlocking failure returns Locked. Decryption/key errors enter RecoveryRequired; only restore or confirmed reset exits it. No screen route may bypass state checks. Scan and enrollment data remain transient until save.

Backup workflow: fresh unlock -> password confirmation -> encrypt -> destination selection -> write -> success. Cancel at any stage creates no success message; cleanup closes handles and removes controllable partial output.

Restore workflow: select -> size and schema validation -> password -> authenticated decrypt -> preview -> confirmed transaction -> success. If interrupted before commit, the original vault remains valid.

Sync states: Off, SetupRequired, Ready, Pending, Syncing, Offline, Conflict, ReauthenticationRequired and RecoveryRequired. Local codes remain available while the network fails. Cloud login never unlocks an existing local vault.

## Nonfunctional requirements
NFR-01 Security: authenticated encryption for all sensitive vault fields; platform-protected key wrapping; no secrets, codes, decrypted labels or QR payloads in logs, crash reports, notifications, screenshots, backups or analytics. Exclude vault files from OS cloud and device-transfer backup rules; test on supported versions and OEM devices.

NFR-02 Availability: core enrollment by manual key, unlock, listing and code generation work offline. Sync outages never block access to intact local secrets.

NFR-03 Integrity: vault mutations, restore, schema migration and revision changes are crash-safe and transactional. Maintain a recoverable previous ciphertext during migrations; never downgrade a schema silently.

NFR-04 Performance: 100-account unlocked rendering p95 <=1 second; search p95 <=150 ms; code rollover visible within 250 ms while foregrounded on the agreed reference device. Crypto and file I/O run away from the UI thread. Measure release builds.

NFR-05 Accessibility: 48 dp minimum touch targets, readable 200 percent text scaling, screen-reader labels, sufficient contrast and no color-only expiry meaning. Screen readers do not announce a code on every timer tick.

NFR-06 Compatibility: proposed minimum Android API 28; target the store-required API at submission after checking current policy. Test oldest supported API, latest stable API, one intermediate API and major OEM physical devices. Pin dependency versions after a maintenance and security review.

NFR-07 Privacy: no advertising identifiers or contact access; local release can omit INTERNET permission. Camera is optional. Use the system document picker rather than broad storage permission. Optional operational telemetry must be explicitly documented and sanitized.

NFR-08 Limits: QR text <=8 KiB; label and issuer <=256 Unicode characters each; backup file <=10 MiB; maximum 1,000 accounts; decrypted schema rejects unknown critical versions. Enforce limits before allocation or expensive KDF work.

## Data dictionary
Encrypted account record: accountId UUID, issuer string, label string, secret bytes, type TOTP, algorithm enum, digits integer, period seconds, createdAt and updatedAt UTC timestamps, sortOrder integer, recordRevision integer and deleted tombstone boolean for P1. OTP values are derived and never persisted. Tombstones must omit the deleted seed.

Encrypted vault payload: schemaVersion, accounts, vaultSettings and migration metadata. Clear envelope: formatVersion, opaque vaultId, keyVersion, nonce and ciphertext including authentication tag. Backup envelope additionally holds salt and allowlisted KDF parameters. Labels are encrypted, not left in a searchable clear database.

## Traceability summary
FR-05 through FR-09 -> add-account screens -> QR, parser and RFC tests T01-T06.
FR-02 through FR-04 and FR-15 -> unlock and recovery screens -> T07-T10.
FR-10 through FR-12 -> home and account detail -> T11-T13.
FR-13 and FR-14 -> backup and restore -> T14-T18.
FR-17 through FR-20 -> sync and device screens -> T22-T26.
NFR-01 through NFR-08 -> security, accessibility, performance and durability suites T19-T21 and T27-T30.
