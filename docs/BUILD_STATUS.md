# MVG v0.7.0 — Verified Android APK Build

Updated: 2026-11-05

## ✅ Source / compilation / integrity
- **Successful GitHub Actions:** https://github.com/parmtech/MVG/actions/runs/42004882683
- **Source commit compiled:** ccb58d681b03cdb52e7895dc0fd42b06a3fe630f
- **Checks:** app:testDebugUnitTest, app:assembleDebug, apksigner verification, checksum and artifact upload PASS.
- **Artifact:** MVG-v0.7.0-debug-apk (app-debug.apk, MVG_SIGNING_FINGERPRINTS.txt).
- **APK size:** 23,052,915 bytes.
- **SHA-256:** 4bdf1eff8643bd7c914bb15d1d1cc72c8728c9c7746e3c46c03258620ecbe8c6 (matching downloaded APK and GitHub CI).
- **Signing SHA-1, exact APK:** B6:C9:C8:8A:EA:2F:04:69:58:BF:19:10:7E:8F:DF:F4:7B:65:1D:67
- **Package name:** com.parmtech.mvg
- **Build signature:** ephemeral Android DEBUG signer; **stable signing GitHub Secrets are still NOT configured**.

## What's new in v0.7
- Password-protected, locally encrypted MVG project archive export in Settings.
- AES-256-GCM with 64-KiB authenticated blocks, unique per-file salt/nonce prefix, PBKDF2-HMAC-SHA256 (250,000 iterations) and authenticated final marker.
- Backup passwords of 12 or more characters entered twice during export and required on import. Passwords not saved in local preferences or archive. Forgotten passwords cannot be recovered.
- ZIP manifest, media SHA-256, entry/path limits and additive restore from v0.6 retained.
- v0.6 **unencrypted ZIPs** can still be imported by leaving restore password blank. v0.7 no longer creates unencrypted backups.
- New automated JVM crypto tests: roundtrip, wrong passwords, corruption, truncation, trailing bytes and password-length rules. Archive traversal/path tests continue to pass.
- File picker saves new archives as .mvgbackup, using app-specific MIME application/octet-stream.
- Google tokens/API keys and app-signing private material are excluded from all project archives.

## Not claimed / still pending
- **A successful CI build does not prove end-to-end user-device behavior.** Actual Android document-provider backup/restore, huge video file handling, device media playback and Google account OAuth have NOT been tested on a physical Android device in this session.
- The owner's $300 Google Cloud credit is tied to the linked billing account and eligibility; neither this build nor signing in confirms Veo 3.1 entitlement, quota or 4K support.
- Google Android OAuth requires registering THIS APK's signing SHA-1 for package com.parmtech.mvg.
- **Stable signing is NOT automatic.** User must generate/retain own keystore and configure all four encrypted GitHub Actions Secrets. See [Stable Signing Setup](STABLE_SIGNING_SETUP.md).
- Earlier v0.6 debug APKs may be signed differently and cannot necessarily be upgraded in place. Preserve important local data before uninstall. v0.6 can export a legacy ZIP; v0.7 imports it after installation, but restore itself must be tested.
- Encrypted backups provide confidentiality and integrity of the archive; they do not recover missing old private files or forgotten passwords.

## Reading and setup
- [Encrypted backup and import details](ENCRYPTED_BACKUP_V07.md)
- [Stable signing GitHub Secrets setup](STABLE_SIGNING_SETUP.md)
- [Original v0.6 archive guide](PROJECT_BACKUP_V06.md)
- [Google Login and $300 credit setup](GOOGLE_LOGIN_AND_300_CREDIT_SETUP.md)
