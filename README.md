# Sentinel Authenticator

**Your codes. Your rules.** A native Android authenticator and a responsive web vault, built around local control and practical recovery.

[Download Android 0.4.0 beta](https://github.com/codexanjan/sentinelai/releases/tag/v0.4.0) · [Open web app](https://sentinel-authenticator.vercel.app) · [Release notes](docs/RELEASE_0.4.0.md)

![Sentinel web workspace — public test accounts](docs/screenshots/web-workspace.png)

## What you can do

| Feature | Android | Web |
| --- | --- | --- |
| Offline TOTP, SHA1/256/512, 6/8 digits | Yes | Yes, after required assets are cached |
| Manual setup key and QR enrollment | Camera + manual | Camera, QR image + manual |
| Focus mode: reveal one account at a time | Yes | Yes |
| Copy fresh: wait at the end of a code period | On by default | Optional switch |
| Clock-change warning during an open session | Yes | Yes |
| Recovery studio / guided sign-in drill | Yes | Yes |
| Favorite accounts and folder filtering | Yes | Yes |
| Create/rename/remove empty folders | Folder names are edited per account | Yes |
| Device credential / strong biometric unlock | Yes | Separate vault password |
| Password-encrypted portable backup | Yes | Yes |
| Public demo using RFC test keys | Read-only, clears on background | Isolated in memory |

Recovery drills are self-recorded checklists. They do not verify a service login or recovery codes. Focus mode controls what is displayed; it is not an additional encryption boundary. Clipboard access can be denied by the browser, in which case you can read the displayed code.

## Faster first screen

The old waiting card has been replaced with an immediate, lightweight intro. QR, account SDK and Argon2 libraries are loaded only when their features are needed. The demo uses an ephemeral random encryption key, without deriving a password key. Real vaults and backups retain Argon2id (64 MiB, three iterations) and AES-GCM; encryption was not weakened to speed up startup. Android initializes device storage away from the UI thread and shows its intro immediately.

## Install the APK

1. Download `SentinelAuthenticator-0.4.0.apk` and its SHA256 file from Releases.
2. Use Android 11 or newer. Allow your browser/file manager to install this APK when prompted.
3. Keep the existing app installed when upgrading so local data remains in place. This release uses the same package and signing key as previous Sentinel builds.
4. Choose **Open my vault**. Set a secure device PIN/password if requested, then unlock. Or choose **Try demo** without adding real secrets.
5. Enable 2FA in each service, add its QR/setup key, and confirm a generated code at that service.

Make and test a password-encrypted backup before upgrades or changing phones. Account organization is now stored in Android local payload v2, with v1 read compatibility. Downgrading to 0.3.0 cannot read that enriched local vault; the portable v1 backup remains compatible and omits folders, favorites and drill history.

## Demo

Web demo: `demo@sentinel.example` / `SentinelDemo123!`, or use **Try demo**. Android has a direct read-only demo. All displayed demo keys are public RFC test material. Android screenshots are allowed in this public demo; real vault screens keep screenshot protection and immediate background locking.

## Screenshots

All images show public demo accounts, never private setup keys.

<img src="docs/screenshots/android-demo.png" alt="Native Android vault demo" width="310" /> <img src="docs/screenshots/android-recovery.png" alt="Native Android Recovery studio" width="310" />

![Focus mode on web](docs/screenshots/web-focus.png)
![Recovery drill on web](docs/screenshots/web-recovery-drill.png)

<img src="docs/screenshots/web-switches-mobile.png" alt="Web account switches on mobile" width="310" /> <img src="docs/screenshots/web-add-mobile.png" alt="Web plus menu on mobile" width="310" />

## Build from source

`android/` contains Kotlin, Jetpack Compose, CameraX/ZXing and the Gradle wrapper. Install Android SDK 35 and JDK 17 or newer, then:

```sh
cd android
./gradlew testDebugUnitTest lintRelease assembleDebug
```

Windows: use `gradlew.bat`. Debug APKs use a different key from the published beta. For a signed release, supply `SENTINEL_KEYSTORE`, `SENTINEL_STORE_PASSWORD` and `SENTINEL_KEY_PASSWORD`; key alias is `sentinel`. Signing material is not committed. APK code and resources are shrunk in release builds.

The web app has no build step:

```sh
python -m http.server 4186 --directory web/dist
```

Use localhost or HTTPS. The existing Vercel deployment serves `web/dist` as static files. Release APKs are stored on GitHub Releases; a copy is also made available through the live website.

## Authentication and privacy

Local vaults require no account. Email/Google/Apple integration and screens are present, but real login is **not enabled** until a backend and provider credentials are configured. There is no cloud sync in this edition. See [authentication setup](docs/AUTH_SETUP.md) and [security model](docs/SECURITY_MODEL.md).

No ads, analytics or AI processing of OTP secrets. `INTERNET` is used for optional account login; codes are generated locally. Never commit service-role keys, OAuth secrets, vaults or real QR/setup keys.

## Status and validation

This is a **beta**, signed with the existing local development release key. Core correctness and browser workflows are tested; comprehensive physical-device, accessibility and independent security review are still pending. Exact results and limits are in [validation](docs/VALIDATION_0.4.0.md).

The product documents in `docs/01_*` through `docs/07_*` are the original plans, not a statement that every planned feature is implemented. Current implemented behavior is described here and in the release notes.

## Feature research

Folders, pinning, offline codes and backups exist in other authenticators. This release adds a recovery-drill workflow and fresh-copy/Focus controls to Sentinel; it does not claim market-wide exclusivity. See [feature comparison sources](docs/FEATURE_RESEARCH.md).

## License

Original Sentinel source is MIT licensed. Vendored libraries and the Gradle wrapper retain their own license notices.
