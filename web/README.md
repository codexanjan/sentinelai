# Sentinel web 0.4.0 beta

[Live website](https://sentinel-authenticator.vercel.app). See the [root README](../README.md), [release notes](../docs/RELEASE_0.4.0.md) and [security model](../docs/SECURITY_MODEL.md).

Serve `dist` from localhost or HTTPS; no build step is required. Example: `python -m http.server 4186 --directory dist`. Deploy dist as static files on Vercel.

Immediate landing/intro, on-demand Argon2/QR/auth SDK loading, offline cached assets, local encrypted TOTP vaults, folders/favorites, focus mode, fresh-code copying and self-recorded recovery drills. No cloud sync. Real email/Google/Apple login requires backend/provider configuration; [setup instructions](../docs/AUTH_SETUP.md). Web vault passwords are separate from account passwords.

Browser screenshots/history cannot be blocked reliably. Clearing browser storage removes a local vault. Keep a tested password-encrypted backup. This beta has not undergone independent security review.

Pinned vendored libraries retain their license notices in dist/vendor.
