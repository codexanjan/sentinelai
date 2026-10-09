# Product Requirements Document for the 2FA Authenticator App

Version 1.0 | 6 October 2026 | Proposed baseline for product and engineering review

## Product decision and scope
Build an Android-first authenticator that generates standards-based verification codes offline and helps users protect and recover their account secrets. The first release includes QR enrollment, manual enrollment, a searchable code list, device authentication, account management, and encrypted file backup and restore. Optional end-to-end encrypted cloud synchronization follows as a separately gated release. These are planning decisions, not confirmed user specifications or completed software.

The supplied screenshots inform the onboarding sequence, search bar, large grouped codes, countdown indicators, add action, privacy settings and future sync status. They do not establish the security design of this app. Use original branding, illustrations and text. The unrelated identity-card attachment is excluded; no personal details from it belong in the product or documents. The screenshot mentioning four portals is treated as a documentation example, not a requirement to create four portals.

## Problem and users
Users need to obtain the right code quickly, without internet access, and avoid losing access when a phone is replaced. Primary users are individuals with several personal or work accounts. Secondary users are people moving from another authenticator and users who rely on assistive technology. A service provider creates the enrollment secret and verifies codes; this app stores that secret and computes codes. It does not automatically enable 2FA on a service.

## Outcomes and proposed measures
- Reliability: all supported RFC test vectors pass; code generation works in airplane mode after enrollment.
- Enrollment: at least 90 percent of moderated participants complete QR enrollment unaided within 90 seconds, excluding the provider's setup flow.
- Retrieval: at least 90 percent find and copy a requested account code within 10 seconds in a 20-account test vault.
- Recovery: every supported backup-format test restores the same secrets and parameters; wrong passwords and corrupt files cause no partial writes.
- Performance: unlocked list renders within 1 second at the 95th percentile on the agreed midrange reference device with 100 accounts; cold launch to unlock prompt within 2 seconds.
- Release quality: no unresolved critical or high security findings and no unresolved defects that lose secrets or show incorrect codes. These are targets to validate, not observed results.

## Feature priorities
P0 release essentials: local TOTP generation; QR and manual add; enrollment preview and confirmation; code copy; search; rename, reorder and delete; privacy lock using system biometrics or device credential; immediate relock by default; encrypted backup and restore; camera-denial fallback; light and dark themes; accessibility; help and recovery guidance.

P1 improvements after the local release: optional encrypted cloud sync, device list and revocation, recovery-key enrollment, multi-device conflicts, and verified migration adapters for named third-party formats. Standard otpauth QR import is P0; proprietary Google migration QR support is not implied.

P2 possibilities: HOTP with explicit counter advancement, iOS app, folders, widgets that preserve lock policy, and advanced bulk migration. HOTP must not be released until atomic counter persistence and multi-device policy are resolved.

Out of scope for the first release: SMS interception, email OTP reading, push approval for third-party services, password storage, passkey-provider implementation, browser extension, enterprise administration and code generation on a server. Do not add advertising SDKs to the vault.

## User journeys
First use: short onboarding explains codes, scanning and recovery; user sets a device screen lock if absent; app activates privacy protection; empty state offers Scan QR and Enter setup key. The user can skip slides but cannot bypass the vault protection requirement.

Add account: user opens the service's 2FA setup, scans its QR or enters its secret, reviews provider and parameters, saves locally, and enters the app's first code on the service to finish enrollment. The app cannot confirm provider activation by itself.

Daily use: unlock, search if needed, inspect issuer and account, copy the code, and enter it on the provider. Close to rollover, display the remaining seconds without preventing copy.

Replace phone: create an encrypted backup, verify it on the new device, test a real service login, and only then remove the old device or app. If no usable backup or enrolled second device exists, use the provider's recovery codes or account-recovery process.

## Product rules and tradeoffs
No app account is required for the local release. Device screen lock is required to protect the vault; manual enrollment works without camera permission. Codes depend on the device clock. Help recommends automatic time settings; the app never silently changes system time or OTP parameters.

Deleting an entry deletes the app's stored secret, not the provider's 2FA configuration. Warn that the user may still need recovery codes to access that service. Uninstalling can remove all locally stored entries; onboarding and settings must explain this before a user relies on the vault.

Encrypted export uses a user-held password independent of device keys. There is no password reset for the export. Local backup precedes cloud sync so recovery is possible without a backend.

## Delivery and commercial assumptions
Working name: 2FA Authenticator. Suggested stack: Kotlin and Jetpack Compose for native Android. Budget, final brand, team availability, supported OS versions, distribution markets and business model remain owner decisions. Plan assumes two Android engineers, one part-time designer and QA/security support. A solo developer should plan a longer schedule and a separate security review.

Cloud sync is not required to deliver the P0 product. If funded, offer it only after cryptographic design, recovery and conflict behavior pass review. Never market TOTP as phishing-resistant: users can still be tricked into submitting a current code.

## Acceptance and signoff
Product owner approves the feature baseline, designer approves accessibility and flows, engineering approves feasibility, QA signs off the test evidence, and a security reviewer approves vault and recovery design. All P0 requirements in the SRS must map to passing tests. Any deferral must identify its user impact and update onboarding, documentation and store descriptions.
