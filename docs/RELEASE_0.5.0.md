# Sentinel 0.5.0 beta — Firebase account login

The account screens now connect to a dedicated Firebase Authentication project. Google sign-in and verified email/password login are enabled on web and implemented with the official Firebase SDKs on Android. A backend-setup placeholder no longer blocks Google login.

- Google popup login on web; Google Credential Manager on Android.
- Email signup, verification before vault entry, and hosted password-reset emails.
- Separate account vaults keyed by Firebase user ID. Signing in still requires unlocking the local encrypted vault.
- Public Firebase configuration, authorized website hostname and registered Android release signing certificates.
- Self-hosted web SDK loaded when account features are opened; immediate landing, isolated demo, folders, Focus mode, Copy fresh and recovery drills retained.
- Kotlin/Compose compiler updated to 2.4.21 and compatible R8 9.4.14 shrinker for Firebase Auth 25.0.0.

## Install and upgrade

Download the APK and compare its SHA256 with the accompanying checksum file. Android 11+ is required. Keep the existing app installed when upgrading: package and signing key are unchanged. Make and test an encrypted backup before upgrading. Google sign-in needs Google Play services and a Google account on Android; local mode remains available without them.

The original local vault is preserved. Signing in opens a separate account vault; use an encrypted backup to transfer accounts deliberately. An account password reset does not decrypt the vault. Apple sign-in and cloud synchronization are not enabled in this release.

## Validation

See [validation results](https://github.com/codexanjan/sentinelai/blob/main/docs/VALIDATION_0.5.0.md) and [authentication details](https://github.com/codexanjan/sentinelai/blob/main/docs/AUTH_SETUP.md). This is a development-signed beta, not an independent security certification. Screenshots use public demonstration data only.

## Website and demo

[Open Sentinel](https://sentinel-authenticator.vercel.app) · [Sign in](https://sentinel-authenticator.vercel.app/#signin) · [Full photo walkthrough](https://github.com/codexanjan/sentinelai#photo-walkthrough)

Demo: `demo@sentinel.example` / `SentinelDemo123!`, or choose **Try demo**. These credentials open an isolated local demo, not a production Firebase user.

## Release photos

<img src="https://raw.githubusercontent.com/codexanjan/sentinelai/main/docs/screenshots/android-demo.png" alt="Android public demo workspace" width="280" /> <img src="https://raw.githubusercontent.com/codexanjan/sentinelai/main/docs/screenshots/web-signin-mobile.png" alt="Web Google and email sign-in" width="280" />

The screenshots ZIP includes the public demo walkthrough, enrollment controls, recovery studio and account screens.
