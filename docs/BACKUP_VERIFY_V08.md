# MVG v0.8 — Verify Before Uninstall

Updated 2026-11-05.

## New reliability tools
- In Settings → Project Backup & Restore, tap **Verify backup before uninstall**.
- Enter the password used for your encrypted .mvgbackup file (or leave blank to check a legacy v0.6 plain ZIP).
- Choose the existing archive in Android's system file picker.
- The app streams and checks the backup **without importing any projects and without writing any recovered videos**.
- The verifier checks every encrypted block's authentication tag and the authenticated final end marker, all ZIP path/entry rules, media SHA-256 and sizes, a complete file manifest, project IDs, media attachment references, draft metadata and orphan chat messages.
- When completed, the app reports project/clip/file counts and highlights **complete scene MP4s or avatar/movie references that are absent from the archive**. A valid encrypted archive can still be incomplete if original local media files were unavailable when the backup was made.

## Meaning of a successful result
**ARCHIVE VERIFIED** means the stored archive is readable with the provided password, its encrypted data and included checksums match, and its project structure is internally consistent. It does NOT mean all expected media was included; check for a missing-media warning. It does NOT prove a full restore into a new device will succeed; perform an actual restore test on a trusted test installation before deleting an older app.

The older unencrypted v0.6 ZIP has internal checksums but **no password-derived authentication**. A malicious party could replace its content and checksums. Treat plaintext legacy backups as untrusted; never share them.

## Upgrade-safe procedure
1. While the old app installation still works, create a backup **using that installed version**: v0.6 generates a plaintext ZIP; v0.7/v0.8 generates an encrypted .mvgbackup.
2. Keep the original archive in a private location, preferably with a redundant second copy. For v0.6 plain ZIPs, avoid exposed/shared folders.
3. On the same signed version if possible, use v0.8 Verify or perform a test restore to verify that the archive actually contains the intended movies, clips and images. **Do not uninstall merely because an APK build passed.**
4. A v0.7 APK signed with an ephemeral CI key may have a **different signing certificate** than a v0.8 APK. Android does not support replacing apps signed with unrelated private keys in place. The backup will not automatically change that signing identity.
5. If Android reports an upgrade conflict, preserve the backup, then only uninstall when the data has been verifiably saved and a restoration path exists. Reinstall the new APK and restore your archive add-only. Google authorization must be redone; it is never inside the backup.
6. For stable future Android upgrades, configure a persistent **owner-controlled** signing key through all four GitHub Actions Secrets; see STABLE_SIGNING_SETUP.md. The public source must never contain private keys.

## Safer Google Cloud background checks
- The background video checker now refuses to poll with an expired Google OAuth access token, API-key connection mode, or missing authorization.
- It marks the clip **Auth required**, with instructions to use Continue with Google and then Check / Download.
- It does not resubmit generation: the existing Vertex long-running operation identifier remains linked to the clip.
- Google OAuth/Billing/Veo quota and physical-device integration still require live owner testing.

## Test coverage
- JVM tests exercise encrypted .mvgbackup verification with media crossing multiple encryption blocks, import-free structure checks, legacy ZIP support, incorrect SHA-256, missing media, missing source assets and wrong-password rejection.
- All verification reads are bounded by the same 2 GiB total-media and 512 MiB per-file limits as restore.
- Unit tests are not an emulator or real Android Storage Access Framework test: files on Google Drive or third-party document providers may behave differently.

## Support scope
- MVG backup only stores available user project data, not Google Cloud account billing state, OAuth secrets, provider quota, Play Store signing material, or past copies of files already lost before backup.
- No recurring backup automation or cloud storage upload is included; the owner chooses a location every time.
- The archive password cannot be recovered if forgotten.
