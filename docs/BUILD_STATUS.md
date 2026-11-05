# MVG v0.10.0 — Verified Android APK and Emulator Tests

Updated: 2026-11-05.

## ✅ Android APK build
- [Successful GitHub Actions v0.10 APK](https://github.com/parmtech/MVG/actions/runs/42032608687)
- **Built source commit:** c2511001bb6419ad7b18b003fa663351c3b97bc8
- **Package name:** com.parmtech.mvg
- **Artifact name:** MVG-v0.10.0-debug-apk
- **APK file size:** 23,069,299 bytes
- **APK SHA-256:** `2d74ce79670ea340d034a81d230876a2376863ddb564285c282adb509b7d4886` (locally extracted and checked against CI job log).
- **APK signing SHA-1:** `6F:4F:1F:DF:DA:67:8A:AA:BD:C0:90:11:DD:AA:F0:01:5B:96:5B:0E` (from MVG_SIGNING_FINGERPRINTS.txt included in CI artifact).
- **Checks passed:** app:testDebugUnitTest, app:assembleDebug, apksigner signature verification, artifact upload.
- **Signing:** Android ephemeral debug certificate. Stable owner-owned keystore secrets are NOT configured.

## ✅ Android emulator integration results
- [Successful Android emulator run](https://github.com/parmtech/MVG/actions/runs/42032581802)
- Test report: 5 tests, 0 failures, 0 errors, 0 skipped.
- Executed Android emulator API 35 with app's FileProvider/MediaStore.
- Tests completed:
  1. Encrypted project backup through app FileProvider, read-only verification, add-only restore into new project and original video byte checks.
  2. Wrong backup password rejects without importing/overwriting projects.
  3. Legacy v0.6 plain ZIP import/verification without triggering a paid video generation.
  4. Saved scene MP4 copies into Android MediaStore Movies with byte-for-byte match and original source preserved.
  5. Invalid MP4 data is rejected rather than being inserted in the Android Gallery.
- The Gallery test uses a synthetic structural MP4, not a real Veo video. It tests storage bytes, not video decoder playback.

## New v0.10 app features
- Complete, downloaded scene clips now have Share and Save scene to Movies actions in Generate, Scenes, Assets and Library.
- Save to Movies uses MediaStore for Android API 29+ and avoids broad storage permissions.
- Share sends a FileProvider content URI through the system chooser with temporary read access. Android 8/9 still has Share.
- Both paths validate the local MP4 header. Save does not make a new Google Cloud generation request.
- Project deletion copy warns about MVG-private merged export removal and lost library references; save or backup individual scene files before deleting projects.

## Remaining limitations / release gates
- No real user-phone tests for Google OAuth, Veo entitlement, actual paid generation, live GCS downloads, original 4K/HDR codec playback or user-device MediaStore provider. CI/device-emulator success does not establish those.
- Project backups are encrypted with a password, but the password cannot be recovered.
- Google Cloud $300 trial credits belong to the linked Cloud billing account, not merely the Google Login; model eligibility and quotas are not confirmed here.
- Stable Android private signing is NOT configured. APKs signed with another certificate may not upgrade in place. Preserve data before uninstalling any prior MVG build.
- See [Stable Signing Setup](STABLE_SIGNING_SETUP.md), [Encrypted Backup](ENCRYPTED_BACKUP_V07.md), [Backup Verification](BACKUP_VERIFY_V08.md), [Single Scene Export](SINGLE_CLIP_EXPORT_V010.md).
