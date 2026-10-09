# Sentinel Authenticator 0.4.0 beta

Your codes. Your rules. Native Android plus a responsive web workspace.

## What changed

- Removed the stalled web loading card. A lightweight intro appears immediately; QR, authentication and Argon2 modules load on demand.
- Instant public demo without waiting for password-key derivation. Real vault and backup encryption settings are unchanged.
- Android initializes device storage away from the UI thread, with an immediate branded intro.
- Focus mode reveals one account at a time, with copying disabled for hidden accounts.
- Copy fresh waits for the next code when five seconds or less remain, and cancels when the vault locks.
- Recovery studio guides a self-recorded sign-in drill and tracks saved service recovery codes.
- Clock-change warnings help diagnose codes after device time changes.
- Android now keeps folder, favorite, recovery-checklist and drill metadata inside its encrypted vault.
- Purple/mint cards, visible actions, compact switches and a bottom-right add menu with QR/manual choices.

## Install

Android 11+ required. Download the APK and compare its SHA256 with the included file. Upgrade over an existing Sentinel installation; do not uninstall a real vault to upgrade. The APK uses the same package/signing key as previous Sentinel releases.

Choose **Open my vault** to use your device credentials, or **Try demo** for public sample accounts. Web demo credentials: `demo@sentinel.example` / `SentinelDemo123!`.

Make and test an encrypted backup before upgrading. Android local metadata now uses payload v2 with v1 read compatibility; older 0.3.0 builds cannot read an enriched local vault. Portable `SENTINEL-BACKUP-1` remains Android/web compatible and omits folder/favorite/checklist/drill metadata.

## Details and screenshots

[Website](https://sentinel-authenticator.vercel.app) · [Source and screenshots](https://github.com/codexanjan/sentinelai#readme) · [Security model](https://github.com/codexanjan/sentinelai/blob/main/docs/SECURITY_MODEL.md)

![Web Focus mode — public demo](https://raw.githubusercontent.com/codexanjan/sentinelai/main/docs/screenshots/web-focus.png)
![Web recovery drill — public demo](https://raw.githubusercontent.com/codexanjan/sentinelai/main/docs/screenshots/web-recovery-drill.png)

![Native Android vault — public demo](https://raw.githubusercontent.com/codexanjan/sentinelai/main/docs/screenshots/android-demo.png)
![Native Android Recovery studio — public demo](https://raw.githubusercontent.com/codexanjan/sentinelai/main/docs/screenshots/android-recovery.png)

## Beta limits

Development-signed beta; independent security review and broad physical-device/accessibility testing are still pending. Local vault and demo work without account login. Real email/Google/Apple login is not enabled until backend/provider configuration is completed. Cloud sync is not included. Recovery drills are user checklists, not verification of a service login. Browser clipboard permissions can prevent copying. No claim is made that these additions are absent from every competing authenticator.

Full validation results are in the repository’s `docs/VALIDATION_0.4.0.md`.
