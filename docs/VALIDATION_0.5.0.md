# Validation — 0.5.0

## Passed web checks

- Official Firebase Auth emulator: signup, email-verification action, rejection of unverified login, verified email login, invalid password, reset action, Google popup result, Firebase UID mapping and sign-out.
- Full web UI against that emulator: signup, verification gate, email login, encrypted account-vault creation, preservation of the original local vault and sign-out.
- Production-origin candidate under the deployed response policy: authorized domain, real Google OAuth account page with the registered client and callback, cancellation, SDK-ready buttons, mobile forms, no horizontal overflow and preserved typed input while initialization completes.
- Core regression: all 18 RFC 6238 vectors, Unicode portable backup round trip and tamper rejection; folders, favorites, recovery checklist, backup download, duplicate restore review and mobile layout.
- Startup: landing and demo work with optional libraries blocked; local measured landing 384 ms (not a network performance guarantee); static intro survives app-module failure; offline reopen and demo.
- Landing motion: moving strip, pause/resume, section visibility, reduced-motion behavior, navigation and vault lock.
- Live production website: SDK readiness, response policy, correct real Google OAuth client/account-page launch, popup cancellation, mobile demo and local-storage isolation.

## Android and deployment

- Optimized signed release build (`assembleRelease`), Android unit tests (`testDebugUnitTest`) and release lint (`lintRelease`) completed successfully.
- 11 unit tests passed with no failures or errors. Lint reported zero errors and 19 warnings; this is not a warning-free build.
- Kotlin/Compose compiler 2.4.21 and R8 9.4.14 produced the release, with code/resource shrinking retained. The mapping header confirms R8 9.4.14.
- Task-owned Android API 37 emulator: fresh installation, intro, configured Firebase login screen, Google account-provider launch, public read-only demo and demo clearing after the app goes into the background. No personal Google account was added or authenticated.
- APK signature verified. The release certificate SHA-1 and SHA-256 match the registered Firebase Android application and the existing Sentinel signing identity.
- Package `app.sentinel.authenticator`, version 0.5.0 (code 5), Android 11 minimum. APK size: 6,286,641 bytes.
- SHA256: `12d8b571779cc5f9b3a60d59f8e88071e15e0c402680ac0201cc26d29590d890`.
- Firebase providers and authorized production domain are configured. The live website's Google account-page launch was verified; account OTP vaults remain local. Release assets are supplied with a checksum and public screenshots.

## Limits

Google account-page launch is not an end-to-end personal Google login. Emulator authentication uses test identities. Real mailbox delivery, personal Google login, physical Android Credential Manager, comprehensive accessibility and independent security review remain acceptance work. OTP vaults remain local; there is no cloud synchronization or Apple provider configuration.
