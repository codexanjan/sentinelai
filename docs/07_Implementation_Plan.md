# Full Implementation Plan for the 2FA Authenticator App

Version 1.0 | Dependency-based working plan and delivery estimates

## Recommended delivery
Complete the offline, protected authenticator and portable encrypted recovery first. Schedule optional cloud synchronization only after local reliability and the separate E2EE design are approved. This plan creates a development roadmap; it does not mean the app has already been implemented.

Planning estimate: 10-12 calendar weeks for P0 with two Android engineers, part-time design, QA and an independent security reviewer; another 4-6 weeks for P1 with backend support. A solo engineer should budget roughly 16-24 weeks for P0, depending on experience and review availability. These estimates exclude unresolved staffing, procurement, store review and unexpected security findings; reserve 20-30 percent contingency. Actual dates start when the team and scope are confirmed.

## Week 1 scope and security foundation
Owner: product owner plus engineering lead. Approve P0/P1 split, Android-first scope, original brand, minimum API proposal, supported OTP parameters, recovery model and acceptance targets. Create threat model and ADRs. Confirm budget, reviewer, test devices and distribution markets. Set up repository, protected branches, CI, formatting, lint and dependency review. Record exact maintained dependency versions.

Deliverables: approved PRD/SRS, module skeleton, dependency inventory, threat model and draft UI flows. Exit: no unresolved fundamental decision about how secrets are stored or restored. This phase blocks production vault code, not design exploration.

## Weeks 2 and 3 OTP core and vault
Engineer A: implement pure Kotlin OTP engine and strict provisioning parser with fake-clock tests T01-T06. Engineer B: implement platform key adapter, BiometricPrompt/credential paths, encrypted envelopes, atomic repository and Locked/Unlocked states. Designer: create onboarding, home, enrollment and unlock prototype; include denial and error states.

Deliverables: synthetic account produces reference OTP; secrets survive encrypted restart; locked UI and repository cannot access records. Exit: RFC tests pass and key lifecycle prototype works on physical devices. Review key configuration before broad feature construction.

## Weeks 4 and 5 enrollment and daily use
Implement on-device QR capture, runtime camera permission, manual key form, review and save, list/search/countdown, copy, rename/reorder/delete and theme. Connect all routes to session state; implement immediate privacy overlay and lock timeout. Run enrollment/provider interoperability tests. Designer and QA inspect long labels, scale, TalkBack and dark theme.

Deliverables: usable local vertical slice from first launch through provider setup and daily code use. Exit: airplane-mode use, permission denial, expiry rollover and locked deep-link tests pass. Do not add a backend to compensate for local correctness defects.

## Weeks 6 and 7 portable recovery
Implement export envelope, reviewed Argon2id binding, baseline KDF benchmark and password UX. Add system document export/import, bounded envelope validation, restore preview and transactional merge. Add key-invalidation recovery UI and backup/transfer exclusion rules. Document the unrecoverable-password case and provider recovery guidance.

Deliverables: encrypted backup successfully restored on a second physical device. Exit: T14-T18, key-loss test and interrupted-write suite pass; reviewer accepts the frozen backup format. Backup is a mandatory release capability.

## Week 8 verification and hardening
Run physical-device matrix, fuzzing, durability, memory/lifecycle checks, accessibility and performance. Inspect release manifest, exported components, logs, screenshots and OS backup outputs. Measure KDF and vault-list behavior on lowest supported hardware. Conduct 5-8 participant usability sessions with synthetic accounts. Resolve findings and revise UI copy where users misunderstand recovery.

Deliverables: traceability report, measured results, issue list and revised prototype/app. Exit: all P0 functionality has passing evidence and unresolved defects are explicitly triaged.

## Weeks 9 and 10 security review and beta
Independent reviewer examines storage, authentication, backup format, parser and release build against selected MASVS controls. Fix high-risk findings and retest affected paths. Prepare store assets, privacy policy, support FAQ, release notes and verified signing process. Check current store target API/disclosure requirements. Run internal testing then closed beta.

Deliverables: signed candidate, review report, backup/recovery help and beta feedback. Exit: no unresolved critical/high security or data-loss findings; all release gates in Testing pass. Reserve weeks 11-12 for review fixes, beta stabilization and release timing. Do not promise a store approval date.

## P1 weeks 1 and 2 cloud design and service
Require a reviewed E2EE recovery/key protocol and decide identity provider, metadata retention, pairing and rollback limits. Backend engineer implements owner-authorized bounded ciphertext APIs, revision compare-and-swap, idempotency and device/session revocation. Android engineer implements separate cloud authentication, recovery-key generation/confirmation and local sync-key wrapping. Designer supplies opt-in and recovery flows.

Exit: staging server has no plaintext or encryption keys; independent account authentication cannot decrypt a vault; successful new-device recovery is demonstrated. Do not upload real vaults before this gate.

## P1 weeks 3 and 4 sync and conflicts
Implement serialized sync worker, offline queue, tombstones, nonoverlapping merge and risky-conflict review. Add device list/revocation and separate sign out/cloud deletion/local deletion flows. Test stale clients, deletion-versus-edit and interrupted upload. Publish account recovery and metadata disclosure.

Exit: T22-T26 pass with two devices and outage simulation; offline OTP access remains available. Older-client behavior and backup-retention promises are implemented and tested.

## P1 weeks 5 and 6 review and staged launch
Review the sync protocol and backend authorization independently; rehearse restore, service outage and incident response. Complete monitoring with redacted data and retention controls, performance/load tests against the agreed pilot size, beta feedback and staged rollout. Exit: sync has its own explicit release approval; P0 can ship without it if this gate is not met.

## Work breakdown and dependencies
WP01 Scope, threat model and ADRs -> WP02 OTP/parser and WP03 vault/key lifecycle.
WP02 plus WP03 -> WP04 enrollment and WP05 list/account management.
WP03 -> WP06 backup/restore and WP07 lifecycle/privacy.
WP04-WP07 -> WP08 accessibility/usability and WP09 full QA/security review.
WP09 -> WP10 signed local beta and production release.
Reviewed P1 protocol plus WP06 -> WP11 sync backend and WP12 recovery/device setup.
WP11 plus WP12 -> WP13 conflicts/offline sync -> WP14 sync review/beta.

Critical path: key lifecycle -> encrypted repository -> portable recovery -> independent review -> release. A polished list screen cannot substitute for these security dependencies.

## Backlog tickets and definitions of done
AUTH-001 Project/CI: reproducible debug/release builds, reviewed dependency list and protected signing path.
AUTH-002 OTP engine: T01-T03 pass, injected clock, leading-zero preservation, no persistent OTP values.
AUTH-003 Parser: SRS limits and T05 fixtures pass; no silent unsupported-parameter fallback.
AUTH-004 Vault crypto: versioned AES-GCM envelopes, unique nonces, authenticated metadata and reviewed wrapping policy.
AUTH-005 Repository: atomic commits, mutation mutex, migration and failure-injection evidence.
AUTH-006 Unlock/lifecycle: real cryptographic authorization, credential fallback, privacy overlay and T07-T10.
AUTH-007 Enrollment: camera denial/manual path, preview, duplicate handling and provider verification guidance.
AUTH-008 Home/actions: identity-safe list, local search, timer, copy and management tests.
AUTH-009 Backup export: frozen reviewed format, bounded KDF, no plaintext output and failure-safe UX.
AUTH-010 Restore/recovery: cross-device round trip, no partial merge, key-loss guidance and no silent reset.
AUTH-011 Accessibility/theme: screen-reader, font-scale and contrast evidence.
AUTH-012 Release/privacy: manifest and backup rules, leak review, signed candidate and user recovery help.
AUTH-013 P1 protocol: recovery model, rollback limits and threat review approved.
AUTH-014 P1 backend: owner checks, CAS, idempotency, revocation and ciphertext-only storage.
AUTH-015 P1 sync client: offline queue, merge, conflicts and tombstone tests.
AUTH-016 P1 operational readiness: retention/deletion contract, incident rehearsal and independent review.

Every ticket needs linked requirement IDs, code review, relevant passing tests, updated help/error states and a sanitized demonstration. Security tickets additionally require a reviewer who did not implement the design. Completion is evidence-based, not merely a merged change.

## Team and working cadence
Product owner resolves scope and approves user-facing tradeoffs. Engineering lead owns architecture and cryptographic review coordination. Android A owns OTP, parsing and enrollment; Android B owns vault, lifecycle and backup. Designer owns flows and accessibility; QA owns test evidence and device matrix. A backend engineer joins for P1. Independent reviewer owns review findings, not implementation delivery. In a small team roles may overlap, but independent security assessment remains separate.

Use weekly working-software demos and backlog review; review risks after each gate. Track correctness, enrollment usability, recovery success, crash causes and privacy regressions without collecting account labels, QR payloads or codes. Maintain one requirement-to-ticket-to-test register as the source of completion status.

## Costs and procurement inputs
Budget lines: engineer/designer/QA time, independent security review, representative Android devices, developer distribution account, signing-key custody and support content. P0 has no required hosting cost. P1 adds identity service, database/object storage, compute, backups, monitoring, email only if needed for identity flows, and operational support. Request vendor quotes after pilot volume and retention are known; no unverified current prices are included.

## Risks and responses
Key invalidation -> preserve ciphertext and prove backup recovery before beta.
Incorrect OTP or clock -> reference vectors, independent comparison and time-settings help.
Weak backup password -> clear warning, memory-hard KDF and verified recovery practice.
Scope growth -> keep P1 separate and update PRD/SRS for new formats/platforms.
Sync conflict loss -> explicit revisions/tombstones and conflict review, no blind overwrite.
Review delay -> reserve contingency and keep cloud feature gated.
Platform/API variation -> physical-device tests and capability-aware code paths.
User misunderstands enrollment -> state that the provider must verify the first code.

## First ten working days
Day 1: approve scope and roles; create repository and issue tracker.
Day 2: threat model, vault/recovery ADR and physical-device inventory.
Day 3: OTP module and RFC tests; CI baseline.
Day 4: parser fixtures and supported-parameter validation.
Day 5: Keystore/BiometricPrompt prototype on a physical device.
Day 6: device-credential fallback and invalidation experiments.
Day 7: encrypted vault envelope and atomic-file prototype.
Day 8: locked/unlocked repository integration and failure tests.
Day 9: onboarding/home/enrollment prototype review with error states.
Day 10: demo reference OTP, encrypted restart and locked-state protection; review evidence and refine estimates.

## Owner decisions before implementation freeze
Confirm the app name, Android-only first release, required encrypted backup, device-lock requirement, supported OTP parameter limits, team/budget, release markets and whether P1 sync is funded. The recommended plan remains usable while these are decided, but they are assumptions rather than user-approved commitments.
