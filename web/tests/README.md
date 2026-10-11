# Firebase account integration tests

These use the official Firebase SDK and the Auth emulator with a `demo-` project. They never create production users or use personal credentials.

1. In `web/tests`, run `npm install`, then `npx playwright install chromium`. Return to the repository root.
2. From the repository root, serve the website: `python -m http.server 4186 --bind 127.0.0.1 --directory web/dist`.
3. In another terminal, start the emulator: `npx -y firebase-tools@latest emulators:start --only auth --project demo-sentinel-auth --config web/tests/firebase.json`.
4. Run `npm --prefix web/tests test` from the repository root.

The tests inject emulator settings only inside an isolated browser context. Published `auth-config.js` is never modified. Tests cover verification, email signup/login/reset, invalid credentials, Google emulator popup results, UID mapping, sign-out and the full UI's separate encrypted account-vault creation. A production Google account login and real email delivery are not covered by the emulator.
