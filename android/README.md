# Sentinel Authenticator

A native Android authenticator built from the accompanying planning pack. Android 11 (API 30) and newer. This is the first offline implementation; cloud synchronization is not implemented.

## What is implemented

- Original dark/light interface with onboarding, account search and live countdowns.
- Standard TOTP QR enrollment and manual Base32 enrollment, with review and duplicate warnings.
- SHA1, SHA256 and SHA512; 6/8 digits; 15–120 second periods. Unsupported parameters fail validation.
- Codes generated locally from the device clock without network access for code generation (login uses internet).
- Device authentication authorizes a real Android Keystore wrapping operation. A random data key encrypts the complete vault, including labels, using AES-GCM.
- Immediate lock on background, protected screenshots and hidden account names while locked.
- Copy codes with a sensitive clipboard marker and best-effort 30-second clearing.
- Rename, move up and confirmed local deletion.
- Portable encrypted backup with Argon2id (64 MiB, 3 iterations, parallelism 1) and AES-256-GCM. Restore previews, skips identical entries and preserves different entries separately.
- Recovery from unavailable device wrapping keys with fresh device authentication and encrypted retention of the unreadable original.
- Bounded backup parsing, atomic writes and explicit Android cloud/device-transfer backup exclusions.

## Install and use

Install the APK supplied beside this source project on an Android 11+ device. Android may ask you to permit installation from the file-opening app. Configure a secure device PIN/password/pattern before creating the vault.

Open a service's two-factor setup on another screen, choose Add account, then scan its standard otpauth QR or enter its setup key. Review the fields, add, and enter the first code on that service to complete setup. This app cannot enable 2FA remotely.

Settings → Create encrypted backup requires a password of at least 12 characters. Save the file and restore it on another device before relying on it for recovery. Forgotten backup passwords cannot be reset. The app recognizes its own `.sentinel` format; it does not import Google Authenticator's proprietary migration QR or other apps' encrypted backups.

## Build

Open this folder in Android Studio with JDK 21 and Android SDK 35 available. Set `sdk.dir` in your own `local.properties` or use `ANDROID_HOME`. The Gradle wrapper pins Gradle 8.11.1; Kotlin/Compose compiler is 2.4.21 and the compatible R8 shrinker is pinned to 9.4.14. First build needs internet for dependencies. The app has internet permission for optional account login; OTP generation remains local.

Windows terminal with JAVA_HOME pointing to JDK 21:

```powershell
./gradlew.bat assembleDebug testDebugUnitTest lintDebug
./gradlew.bat connectedDebugAndroidTest
```

The second command needs a connected test device/emulator. Do not use personal enrollment secrets as test fixtures. Device UI test setup and executed results are listed in `VALIDATION.md`.

Release signing uses optional environment variables `SENTINEL_KEYSTORE`, `SENTINEL_STORE_PASSWORD` and `SENTINEL_KEY_PASSWORD`; key alias is `sentinel`. Without them, release output is unsigned. The included APK uses a local development signing identity; signing credentials are excluded from the source package. Keep a consistent identity to install updates without uninstalling and losing local data.

## Project layout

`core/Otp.kt`: pure Kotlin provisioning, Base32 and RFC-compatible OTP engine.
`core/PortableBackup.kt`: strict binary record schema and portable backup encryption.
`data/VaultStore.kt`: platform key lifecycle, encrypted file envelope and atomic persistence.
`MainActivity.kt`: authentication, session lifecycle, file picker and transactional commands.
`Screens.kt`: Compose screens and app navigation.
`ScannerScreen.kt`: on-device CameraX/ZXing decoding.
Unit tests validate official vectors, malformed provisioning, rollover and backup integrity. Instrumentation tests validate Android cryptography, locked-write rejection and manifest permissions.

## Differences from the planning baseline

Minimum API is 30 instead of proposed 28, to use per-operation strong biometric/device-credential cryptographic authorization consistently. Immediate background lock ships; configurable grace periods are deferred. Folders, favorites, Focus mode and recovery drills are implemented. Cloud sync, HOTP, iOS and proprietary migration imports are deferred.

This implementation needs independent security review, physical-device/OEM testing, full accessibility checks and beta feedback before a production launch. Managed-runtime memory wiping is best effort. Clipboard expiry cannot be guaranteed after process termination. Device compromise and phishing remain outside the protection provided by local vault encryption.

## Version 0.5.0

Google and verified email/password login connect to Firebase Authentication. Native Google sign-in uses Credential Manager and the registered release signing certificate. `app/src/main/assets/auth-config.json` contains an empty template. Fill the project/app options and web OAuth client ID in a private build copy; Firebase is initialized programmatically. A debug build needs its own SHA certificate registered to use Google sign-in.

Email signup sends a verification link; unverified accounts cannot enter the account vault. Password resets use Firebase's hosted email action page. Apple sign-in still requires provider credentials. Account sessions are managed by the Firebase SDK in private app storage. OTP secrets remain in separate local Keystore-protected vaults, and signing in never decrypts or syncs them. Local vaults and the public read-only demo remain available. See the repository's `docs/AUTH_SETUP.md` and `docs/VALIDATION_0.5.0.md` for verification limits.
