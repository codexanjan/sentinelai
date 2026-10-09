# UI and UX Specification for the 2FA Authenticator App

Version 1.0 | Screen plan and interaction contract

## Design direction
Use the references for a calm, spacious interface with large codes, one clear add action and simple onboarding. Create an original app name and graphics. Proposed tokens: teal primary #006A7C, light background #F6FAFC, primary text #161C20 and warning text #8A3C00. Dark theme uses #10181C background and #E0F7FF code text. Verify contrast for every foreground/background and state combination before design approval; these are proposed tokens, not certified combinations.

Use 16 dp page padding, 8 dp spacing increments, 16 sp body text, 24 sp screen titles and 32-40 sp tabular OTP text. Minimum target size is 48 dp. Code groups use visual spacing, while copied digits contain no spaces. Support long issuer names, right-to-left layout and 200 percent text scaling without hiding controls. Never rely only on a circular indicator or color to convey expiration.

## Navigation
Onboarding -> Device protection setup -> Unlock -> Home.
Home -> Add method -> Scan or Manual -> Enrollment review -> Home.
Home -> Account actions -> Rename, reorder or delete confirmation.
Home -> Settings -> Privacy, Backup and restore, Appearance, Help and About.
P1 Settings -> Encrypted sync setup -> Cloud sign in -> Recovery key verification -> Sync status and devices.

There are no admin, vendor or support portals in the local product. Help is a privacy-safe information screen. A future support portal must never expose account secrets.

## Screen specifications
S01 Onboarding: three concise slides headed Codes for your accounts, Add by scanning, and Keep a recovery copy. Primary action Get started remains available; secondary Skip moves to protection setup. State that enabling 2FA happens on the service and that local codes work offline. P0 onboarding does not promise cloud sync.

S02 Protection setup: explain device lock requirement; show Open device security settings when unavailable and Continue when valid. If the user returns without configuring a lock, show the same explanatory state. Do not display a misleading successful setup.

S03 Unlock: app mark, Unlock authenticator button and system prompt. No issuer, label, code or search history is visible. Cancel keeps the user here. For unavailable key, replace unlock action with recovery guidance without deleting data.

S04 Empty home: title Authenticator, settings action and an illustration without account identifiers. Text Add an account from its 2FA settings. Buttons Scan QR code and Enter setup key. Include Create a recovery backup guidance after the first account exists.

S05 Populated home: search bar, account list and floating Add button. Each row has issuer, account label, grouped OTP, Copy action and numeric remaining seconds plus a visual ring. P1 adds an explicitly labeled Sync status icon; P0 omits profile/avatar/cloud icons. Account identity is visually stronger than countdown.

S06 Add method: bottom sheet with Scan QR code and Enter setup key. Explain that setup keys should come from the service's 2FA settings. Dismiss returns home without a mutation.

S07 Scanner: camera preview, frame, instructions, torch control if available, Cancel and Enter key instead. Camera permission explanation appears at first use. Denied permission keeps manual entry accessible; permanently denied state links to system settings. Invalid QR displays This QR code is not a supported authenticator setup code and allows retry. Do not display the secret in an error.

S08 Manual entry: issuer optional, account required, masked setup key required and collapsed Advanced options. Default 6 digits, SHA1 and 30 seconds. Field errors stay adjacent to their fields. Preview is enabled only after validation. On back, discard transient secret with a confirmation if entered.

S09 Enrollment review: show issuer, account, type and advanced parameters. Buttons Add account and Cancel. Duplicate warning explains the candidate match without exposing seed material. After save, message Finish setup on the service by entering this code. Do not label the provider account verified.

S10 Account actions: Rename account, Move and Delete. Do not expose an export QR or seed reveal in P0. Rename affects only the app label. Delete confirmation: Removing this account here does not turn off 2FA on the service. Make sure you can still sign in. Buttons Cancel and Delete account.

S11 Settings: Privacy, Encrypted backup, Restore backup, Appearance, Help and About. Show exact app version. Privacy has immediate default relock and explicitly chosen 30-second/1-minute alternatives. Describe the longer unlocked interval beside the control. No privacy-off toggle.

S12 Export backup: reauthenticate, then password and confirm-password fields with accessible reveal controls. Copy: You need this password to restore the file. We cannot reset it. Choose destination, then progress and success. On storage failure: Backup was not saved. Try another location. Do not show a success message until completion.

S13 Restore backup: select file, enter password, show progress, then preview number of new entries and duplicates. Merge is explicit. Wrong password or altered file: Cannot open this backup. Check the password and file. Cancel returns without writes. Unsupported version tells the user to use a compatible app version, never guesses a schema.

S14 Recovery required: explain that the device protection key is unavailable. Offer restore and provider-recovery help. A reset action is separate and destructive, with a confirmation of local data loss. P1 may add recovery-key restore.

S15 Help: Codes rejected? Check automatic date and time, verify the selected account, and wait for a fresh code. Lost access? Use a verified backup or provider recovery codes. Include the difference between app deletion and turning off provider 2FA. Never ask the user to submit setup keys to support.

S16 P1 Sync setup: opt-in explanation covers encrypted content, server-visible metadata and recovery limits. Cloud sign-in precedes a locally generated recovery key. Ask the user to confirm selected words/segments or a checksum-backed entry before upload. Show recovery key only behind fresh authentication; prohibit screenshots. If abandoned, no sync-success state appears.

S17 P1 Sync status and devices: last successful sync time, pending changes, offline/retry state, device aliases and revoke action. Conflict screen lists affected account labels only inside the unlocked vault and requires an explicit choice. A revoked device may retain past secrets; explain this in the confirmation.

## Home wireframe
Authenticator                           Settings
[ Search accounts                              ]
Example service
person@example.test
123 456                         Copy    18 sec
------------------------------------------------
Another service
work@example.test
054 321                         Copy    18 sec
                                      [ + Add ]

Codes here are illustrative only. Never use the attached screenshots' real labels or current codes as test data.

## Interaction and privacy details
The list updates without moving rows. Copy does not navigate. Search result zero state says No matching accounts and offers Clear search. At <=5 seconds show Expires in 5 seconds using text as well as the ring. Do not forcibly wait for the next code. Unlock preserves user intent without retaining decrypted secrets across a locked session.

Android sensitive windows use FLAG_SECURE; add a background privacy overlay before recents capture. Verify actual OEM behavior and do not promise universal protection on compromised systems. No sensitive notification content. Haptics are subtle and respect accessibility preferences; reduce motion removes animated ring transitions.

## Accessibility and usability validation
Test TalkBack reading order, control names and focus after dialogs. Code announcement happens on explicit focus or action, not every second. Provide Remaining time 18 seconds as a separate accessible label. Validate keyboard traversal where supported, 200 percent text, display-size settings, landscape layouts, dark theme and contrast.

Run moderated sessions with 5-8 representative participants including an assistive-technology user. Tasks: enroll, find/copy, understand an expiry warning, deny camera, delete safely, export, restore and explain recovery limits. Record completion, time, mistakes and misunderstood wording without recording secrets. Revise flows that cause accidental loss before beta.

## Design handoff
Deliver component variants, screen states, navigation map, error copy, font scaling examples, theme tokens and a clickable prototype. Each screen links to its SRS requirement IDs. Designers and engineers jointly inspect scanner denial, key invalidation, export failure, zero search results and P1 conflicts rather than only happy paths.
