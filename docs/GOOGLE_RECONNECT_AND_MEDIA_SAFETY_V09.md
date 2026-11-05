# MVG v0.9 — Google reconnect and downloaded MP4 safety

Updated: 2026-11-05.

## Existing Veo job recovery
MVG preserves each Google Vertex long-running operation ID returned by an accepted video-generation request. **Reauthorization must not create a second charge.**

- In Settings, tap Continue with Google and approve cloud-platform permission.
- After OAuth authorization is successfully stored, MVG checks saved clips for **existing, nonterminal provider operation IDs**.
- Running, Auth required, OutputReady, Waiting for network and Check again states can resume background **polling**. Completed/failed jobs and uncertain submissions without an operation ID are never automatically resubmitted.
- App startup only queues checks when Google authorization is current; API Key mode and expired tokens do not trigger futile worker runs.
- Check / Download with expired authorization asks for Google Login instead of pretending a provider check occurred.
- The video background worker **never makes paid generation POST requests**: it checks existing operations and retrieves completed files only.
- OAuth is conservatively treated as expired after 45 minutes. Longer tasks may require renewed Google authorization manually.

## MP4 download safeguards
- MVG checks that downloaded bytes start with a plausible MP4 ftyp/styp container header before marking a video Complete. HTML error pages or empty downloads will be rejected.
- Cloud Storage downloads refuse redirects while carrying a Google bearer credential.
- New MP4 contents are written to a temporary file beside the existing file, then **atomically renamed** into place. An old successful MP4 is not deleted before the replacement succeeds.
- Temporary files are deleted after failed downloads. Provider job IDs remain available for safe rechecks.
- A plausible MP4 header is **not** complete codec or playback validation.

## Android emulator tests
A GitHub-hosted Android API 35 emulator has successfully completed 3 device-level tests:
1. FileProvider encrypted project backup, verification, add-only restore and byte comparison of recovered video media.
2. Wrong password rejection without altering projects.
3. Legacy unencrypted ZIP verification/import without initiating a paid video generation.

Passed: https://github.com/parmtech/MVG/actions/runs/42011138522

These are **emulator tests**, not a test on the owner's phone. They do not verify Google OAuth, the account's $300 credit, Vertex permissions, Veo entitlement, actual video generation, 4K playback or real-world Storage Access Framework providers.

## APK signing and update safety
The owner must provide a private persistent signing keystore via GitHub Actions Secrets to get reliable in-place updates and a stable OAuth SHA-1. Otherwise, debug certificates remain ephemeral and new APKs may not install over an older different-signed package. Never remove an installation holding unique media without a **verified backup and successful test restore**.

The APK download ZIP now includes MVG_SIGNING_FINGERPRINTS.txt and MVG_INSTALL_NOTES.txt, showing signer details and whether private stable signing was configured. Do not publish a signing private key, credentials or OAuth tokens in this public source repo.

See [Stable Signing Setup](STABLE_SIGNING_SETUP.md), [Backup Verification v0.8](BACKUP_VERIFY_V08.md) and [Google Login and $300 Credit Setup](GOOGLE_LOGIN_AND_300_CREDIT_SETUP.md).
