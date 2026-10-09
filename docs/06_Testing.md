# Testing and Release Plan for the 2FA Authenticator App

Version 1.0 | Planned tests and acceptance evidence

## Verification approach
This is a test plan; no app tests have been executed because this request produces planning documents. Test deterministic OTP and parsing logic in unit tests, data/key workflows in integration tests, navigation and accessibility in UI tests, and real Keystore, lifecycle and OEM behavior on physical devices. Use only synthetic enrollment secrets and example.test labels. Security-sensitive release gates require retained evidence and reviewer signoff.

## Acceptance test catalogue
T01 RFC TOTP P0: run every RFC 6238 Appendix B time and algorithm vector with its specified seed and 8-digit output. Expected: exact reference values, including timestamps beyond 32-bit range. Covers FR-09.

T02 Truncation P0: run RFC 4226 Appendix D HOTP core vectors and additional leading-zero cases. Expected: exact outputs; this validates the shared core without advertising HOTP product support. Covers FR-09.

T03 Boundaries P0: fake clock at period-1, period and period+1, across timezone/DST changes and a backward wall-clock adjustment. Expected: correct UTC-derived codes and remaining time, no stale code after resume. Covers FR-09 and NFR-04.

T04 QR enrollment P0: scan standard URIs for each supported algorithm, 6/8 digits and several accepted periods. Confirm metadata and save; verify against an independent trusted implementation and a provider setup. Expected: correct code, no uploaded frame, persistence survives restart. Covers FR-05 to FR-09.

T05 Parser negative P0: empty/invalid Base32, duplicate secret/algorithm fields, issuer mismatch, malformed percent encoding, unknown type, HOTP, zero/negative period, unsupported digits, huge input and Unicode. Expected: clear rejection or explicit noncritical-extension notice, no silent critical fallback, no persistence and no leaked payload. Covers FR-07.

T06 Manual and duplicates P0: valid formatted key, invalid key, missing label and identical secret/parameters under a different label. Expected: validation and review; intentional duplicate handling; no secret hash leaves the vault. Covers FR-06 to FR-08.

T07 Lock bypass P0: cold start, deep link, activity recreation, cancelled biometrics and repository operation invoked while locked. Expected: no names, codes, plaintext vault or usable unwrapped key. Covers FR-02 to FR-04.

T08 Relock P0: background, screen lock, process termination and return at 0/29/30/59/60 seconds for each policy. Expected: monotonic timeout behavior and fresh unlock when expired; immediate privacy overlay. Covers FR-04.

T09 Authentication matrix P0: physical devices with strong biometric, only device credential, no credential, lockout, cancelled prompt and authentication timeout. Expected: approved system fallback authorizes actual unwrap; UI success alone never grants access. Covers FR-02 and FR-03.

T10 Key invalidation P0: remove secure lock and exercise biometric-change behavior for selected key settings. Expected: preserve ciphertext and show recovery; never overwrite with an empty vault. Covers FR-15.

T11 Search and list P0: 0, 1, 100 and 1,000 entries, long labels, Unicode and case variants. Expected: stable order, correct identity/code association and responsive search. Covers FR-10 and NFR-04.

T12 Copy P0: grouped display, leading zero, expiry boundary and intervening clipboard write by another app. Expected: ungrouped digits, sensitive marker where supported, best-effort expiry and no deletion of another app's newer value. Covers FR-11.

T13 Management P0: rename, reorder, cancel delete, confirm delete and restart. Expected: only intended local change, provider-warning copy, no incorrect seed association. Covers FR-12.

T14 Backup round trip P0: export 1 and 1,000 accounts with supported Unicode passwords; restore on a different physical device. Expected: exact secrets and parameters, matching codes at fixed time and no dependency on old Keystore. Covers FR-13/14.

T15 Backup integrity P0: wrong password, truncated ciphertext, tag change, header modification, unsupported version and malformed records. Expected: generic error, no partial restore and current vault unchanged. Covers FR-14.

T16 Malicious envelope P0: excessive KDF memory/iterations, huge file, duplicate account UUIDs and unknown critical schema. Expected: bounded rejection before expensive allocation and no crash. Covers NFR-08.

T17 Backup failures P0: cancel picker, destination denied, write failure, low disk, interruption and unavailable external provider. Expected: no false success; plaintext never written; original vault intact. Covers FR-13 and NFR-03.

T18 Restore merge P0: new records, exact duplicates and UUID/content conflict. Expected: preview and explicit merge, no accidental replacement, crash-safe commit. Covers FR-14.

T19 Data leakage P0: inspect private storage, logs, crash output, recents snapshots, screenshots, notifications and backup/transfer artifacts. Expected: no plaintext secret, code or identifying vault labels outside authorized memory/UI. Covers NFR-01/07.

T20 Accessibility P0: TalkBack, 200 percent font scaling, large display size, contrast, reduce motion, landscape and RTL. Expected: readable controls, logical focus, no per-second announcements and usable complete flows. Covers NFR-05.

T21 Performance P0: release build on agreed low/midrange device; measure cold prompt, unlocked list, search, rollover and battery behavior with 100/1,000 records. Expected: documented SRS targets at 100; acceptable bounded behavior at 1,000 with no sustained background ticker. Covers NFR-04.

T22 Sync E2EE P1: inspect network requests and server storage, attempt recovery with account login only, then with login plus recovery key. Expected: no plaintext/keys in traffic/server; login alone cannot decrypt. Covers FR-17.

T23 Sync conflicts P1: two offline devices edit separate entries, same entry parameters, and delete-versus-edit. Expected: safe nonoverlapping merge, explicit risky conflict, no routine tombstone resurrection. Covers FR-19.

T24 Sync retry P1: dropped response after successful write, repeated idempotency key, expired session and stale expectedRevision. Expected: no duplicate operation; 409 conflicts; reauthentication does not block offline codes. Covers FR-19.

T25 Authorization P1: another user's vault/device IDs, revoked tokens, missing credentials, oversized payloads and abusive requests. Expected: access denied, revocation effective for future service access, bounded resource use. Covers FR-18 and architecture API contract.

T26 Cloud recovery/deletion P1: new-device restore, lost recovery key, sign out, disable sync, cloud removal and account deletion. Expected: clear distinct scopes; no fake recovery; retention matches published contract. Covers FR-20.

T27 Durability P0: terminate at each write/migration step, disk-full, corrupted last write, concurrent rename/add and schema upgrade. Expected: old or new valid snapshot, never mixed plaintext or silently lost records. Covers NFR-03.

T28 Permission and compatibility P0: deny/revoke camera; inspect manifest permissions/exported components; test supported APIs and OEMs. Expected: manual fallback, no broad storage/contact permission and no route bypass. Covers FR-05 and NFR-06/07.

T29 Security review P0/P1: apply selected MASVS controls and MASTG techniques to storage, crypto, auth, network, platform and privacy; inspect dependency/SBOM and signed release. Expected: all critical/high findings resolved or release withheld. No certification claim.

T30 Fuzz and soak P0: bounded parser/file fuzzing, repeated lock/unlock, 24-hour foreground/background cycle, rotation and clock changes. Expected: no crashes, leaks, unbounded allocations or code/account mismatch.

## Environments and evidence
Unit/integration suite uses deterministic clocks and synthetic secrets. Emulators cover supported API levels; physical devices cover credential and hardware-key behavior on at least two OEMs. Use separate dev/staging cloud accounts for P1. Attach build commit, device/API, steps, expected/actual outcome and sanitized evidence to each test run. Do not attach real QR setup images or recovery keys.

Defect severity: critical means secret exposure or broad security bypass; high means data loss, wrong codes or recovery failure; medium means an impaired flow with a reasonable workaround; low means cosmetic issues. Any severity involving unsafe account identity/code matching blocks release.

## Release checklist
- Product/SRS baseline approved and every P0 requirement has test evidence.
- OTP vectors, parser negatives, backup cross-device restore and interruption tests all pass.
- Physical-device key lifecycle and system-credential fallback verified.
- Permissions, backup exclusions and sensitive-screen behavior inspected in the signed build.
- Accessibility and target-device performance pass.
- No unresolved critical/high security or data-loss defects.
- Independent vault/backup review completed; P1 sync review separately completed if enabled.
- Signing key protected, dependencies pinned, SBOM retained, release checksum verified.
- Privacy policy, support guidance, backup limits and recovery warnings match actual behavior.
- Check current Play target API and disclosure rules at submission; do not rely on this plan as a current policy statement.
- Beta staged rollout, feedback channel, crash redaction and rollback/migration plan ready.

## Rollout and incident response
Internal test -> closed beta -> small staged production rollout -> expansion after reviewing crash, correctness and recovery feedback. No fixed rollout percentage is a store-policy claim. If wrong codes, secret exposure or corruption occurs, halt rollout and preserve sanitized evidence. Disable optional sync through a safe feature gate if needed; keep the offline vault accessible. Notify affected users with factual recovery steps through an approved channel. Do not roll back to an app that cannot read a migrated vault; fix forward or restore compatible encrypted snapshots. Practice this before production.
