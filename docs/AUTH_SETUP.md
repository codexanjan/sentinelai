# Sentinel authentication setup

Current website: https://sentinel-authenticator.vercel.app/#signin
APK: SentinelAuthenticator-0.4.0.apk (Android 11+; optimized, locally signed development release; approximately 4.7 MB).

## Demo
Email: demo@sentinel.example
Password: SentinelDemo123!
Or choose Try demo. Public RFC test setup keys only. Web demo is isolated in memory; Android demo is read-only and clears on background. No real server demo account is created.

## Status
Sign-up/sign-in/reset UI, email login integration, Google/Apple integration and Android PKCE callback handling are implemented. Real account login is not yet enabled: a Supabase project and provider credentials have not been configured. Provider buttons show an explicit setup message until connected. Local vault and demo remain usable.

## Connect Supabase
Create a dedicated Sentinel project in the selected organization. Configure its URL and publishable key in web/dist/auth-config.js and android/app/src/main/assets/auth-config.json. Never put a secret/service-role key in either app.
Set site URL https://sentinel-authenticator.vercel.app and allow https://sentinel-authenticator.vercel.app/**, http://localhost:4186/**, http://127.0.0.1:4186/** and sentinelauth://auth/callback for authentication redirects. Require email confirmation and passwords of at least 12 characters. Configure production SMTP before opening email registration to general users.

Enable Google using the owner's Google OAuth client ID/secret with the Supabase callback registered. Enable Apple using the owner's Apple Services ID, team/key identifiers and private signing key through the provider dashboard. Store provider secrets in Supabase only. Set googleEnabled/appleEnabled true in both public configuration files only after verification.

## Account and vault separation
Signing in does not decrypt or cloud-sync OTP secrets. Web vaults are encrypted under their own password and stored separately per server user ID. Android uses a separate encrypted Keystore-protected vault per user ID and still requires device authentication. The original local vault is preserved. Password reset changes the account login only. Supabase Auth stores identity information; it receives no OTP seeds from these apps.

## Verification and limits
Web live browser checks passed for sign-in/sign-up pages, invalid demo password, demo login, demo isolation, mobile layout and clear provider setup states. Existing core tests passed before and after auth code changes. Optimized release APK signature verified. Full social login and real email flows cannot be tested without the backend/provider credentials. Account provisioning, production email delivery, deep links, physical-device credentials, comprehensive accessibility and independent security audit remain acceptance work.
