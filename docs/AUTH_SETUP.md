# Sentinel authentication

[Sign in on web](https://sentinel-authenticator.vercel.app/#signin). The published Android and web apps use Firebase Authentication. Repository configuration files are blank templates; deployed configuration is maintained outside Git.

## Available methods

- Google: use **Continue with Google** on sign-in or sign-up. A first Google sign-in creates a Firebase account. Web opens Google's account selector in a popup. Android uses Credential Manager and requires a Google account/Google Play services on the device.
- Email: create an account with your name, email and a password of at least 12 characters. Open the verification email before signing in. Forgot password sends a Firebase-hosted password-reset link.
- Demo: `demo@sentinel.example` / `SentinelDemo123!`, or **Try demo**. This is a local, isolated demo using public test keys, not a shared Firebase user.
- Local vault: no account is required. It remains usable independently of account sign-in.
- Apple: the screen is present, but this provider remains unavailable until the owner's Apple developer credentials and provider configuration are supplied.

## Connected configuration

The published website domain `sentinel-authenticator.vercel.app` is authorized in Firebase. Both email/password and Google providers are enabled. The Android release's SHA-1 and SHA-256 certificates are registered. Firebase web SDK 13.0.0 is self-hosted and loaded when account features are opened. Android uses the official Firebase Authentication SDK and Credential Manager.

Configuration templates are in `web/dist/auth-config.js` and `android/app/src/main/assets/auth-config.json`. Fill these only in a private deployment/build copy; the public repository does not contain the owner's project settings. They identify the project and apps; they are not administrator credentials. Provider secrets, Firebase CLI credentials and signing material are never bundled or committed. Android initializes Firebase from these public options; no Google Services Gradle plugin is required.

For a fork, create your own Firebase project and register a web and Android app. Enable Google/email providers and authorize your exact website hostname. Download your public SDK configuration with `npx -y firebase-tools@latest apps:sdkconfig`. Set the Android web OAuth client ID, and register the SHA certificates of the key signing your APK. A debug APK has a different signing certificate and needs its own registration. Do not reuse this release's project for an unrelated app.

Replace `support@example.invalid` in a private copy of the auth-only CLI configuration with your own support address before deployment. The auth-only CLI configuration can enable providers; authorized domains must also be verified in the Authentication settings. `connect-firebase.mjs` checks the live public domain configuration before writing the website settings and restrictive response policy. No database or cloud vault storage is provisioned.

## Account and vault separation

Signing in does not decrypt or sync OTP setup keys. Web vaults use a separate encryption password and storage namespace per Firebase user ID. Android uses a separate Keystore-protected local vault per Firebase user ID and still requires device authentication. The original local vault remains separate; it is not automatically migrated into an account vault.

Web account sessions use Firebase session persistence. Android's Firebase SDK manages account-session data in private app storage. Account passwords and OAuth credentials are handled by the official SDKs; the apps never log them. Firebase receives identity information for login, not OTP setup keys. Account password reset cannot recover a vault password or a deleted vault.

## Verification limits

The official Firebase Auth emulator verifies signup, email verification, verified/unverified email sign-in, wrong-password handling, reset, Google popup results, UID mapping and sign-out. The production-origin website candidate verifies the real Google account-page URL/client/callback, domain authorization, popup cancellation, response policy and mobile forms. Completing a personal Google account sign-in and checking real email delivery require the account owner. Physical Android Google sign-in and a broader independent security review remain release acceptance work; emulator results are not claims of physical-device coverage.
