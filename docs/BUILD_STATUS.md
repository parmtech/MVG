# MVG v0.9.0 — Verified Android APK & Emulator Tests

Updated: 2026-11-05.

## APK build: SUCCESS
- **GitHub Actions:** https://github.com/parmtech/MVG/actions/runs/42011919796
- **Compiled commit:** 1f34ad0cae6b6ca1276dd85c56791eaecaf02a46
- **Tasks:** app:testDebugUnitTest, app:assembleDebug, apksigner verification and artifact upload passed.
- **Artifact:** MVG-v0.9.0-debug-apk — contains app-debug.apk, MVG_SIGNING_FINGERPRINTS.txt and MVG_INSTALL_NOTES.txt.
- **Size of APK:** 23,052,915 bytes
- **Verified APK SHA-256:** 025115f0f40116d57101dee31e966a770c3f8057a8f78392df01a38dbc71bd22 (locally extracted, matched GitHub CI).
- **Public certificate SHA-1 for this APK:** 87:36:FD:A0:A5:25:F3:61:A7:DC:74:DC:7D:EA:18:F3:80:8D:36:5B
- **Package:** com.parmtech.mvg.
- **Signing mode:** ephemeral Android debug. CI explicitly reports stable signing Secrets were NOT configured.

## Android emulator testing: SUCCESS (separate workflow)
- https://github.com/parmtech/MVG/actions/runs/42011138522
- Android 35 Google APIs emulator, 3 tests completed.
- Encrypted project backup via FileProvider, full archive verification and additive restore verified against actual saved local media.
- Incorrect-password restore blocked, original projects kept.
- Older plaintext v0.6 ZIP verify/import compatibility checked.
- This is Android-emulator coverage, NOT a physical user-device test.

## Implemented in v0.9
- Safe Google OAuth recovery: previously saved, unfinished Veo long-running operations resume **status checks** after reauthorization; completed jobs, failed jobs or uncertain submissions lacking operation IDs are not automatically re-submitted.
- Existing job status polling checks Google auth mode and token freshness; user sees manual reauthorization guidance rather than futile background checks.
- MP4 download hardening: plausible MP4 media header required; GCS authenticated redirects disabled; atomically rename temporary downloaded MP4 into place without first deleting previously saved video.
- Separate Android emulator integration test workflow now responds to app code changes.
- APK artifact includes signer/OAuth and upgrade-safety notes to avoid confusion about changing temporary SHA-1.

## Important limits
- **No end-to-end Google OAuth / actual Google Cloud project IAM/credit eligibility/paid Veo video generation on owner's account has been performed.** The $300 Welcome Credit may have service and quota restrictions.
- No real physical-phone tests of MVG UI, music/video playback, sharing or quality/4K output.
- Valid MP4 header is not proof of fully decodable media.
- Signing remains temporary: older APKs might have different certificates and cannot be upgraded in place. Never uninstall the older app with valuable local data unless backup integrity and restore have been tested.
- A user-owned stable signing key must be created/kept private, and all four GitHub Secrets configured manually. No private key has been committed.

## Guides
- [v0.9 Google Login and MP4 safety](GOOGLE_RECONNECT_AND_MEDIA_SAFETY_V09.md)
- [Encrypted backup](ENCRYPTED_BACKUP_V07.md)
- [Verify backup before uninstall](BACKUP_VERIFY_V08.md)
- [Stable APK signing](STABLE_SIGNING_SETUP.md)
- [Google Login & $300 credits](GOOGLE_LOGIN_AND_300_CREDIT_SETUP.md)
