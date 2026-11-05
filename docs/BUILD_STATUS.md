# MVG v0.8.0 — Verified Android Debug APK

Updated: 2026-11-05

## Build and artifact verification
- **GitHub Actions SUCCESS:** https://github.com/parmtech/MVG/actions/runs/42008578198
- **Built source commit:** 0f3ef58d96a01feda6e8a45d604dfd9bdde6853b
- **Tasks:** app:testDebugUnitTest and app:assembleDebug PASS; apksigner verification PASS; artifact uploaded.
- **Artifact:** MVG-v0.8.0-debug-apk (app-debug.apk and MVG_SIGNING_FINGERPRINTS.txt)
- **APK size:** 23,052,915 bytes
- **APK SHA-256:** dde2b99202e5f767608b85009cdeab339bc3965589a3e731aa2ee1652fc67879
- **Integrity:** extracted APK locally and SHA-256 matched verbatim with GitHub Actions job log.
- **Package name:** com.parmtech.mvg
- **Public signing SHA-1 (exact APK):** 7A:15:26:87:28:3E:11:0F:94:9B:0D:FB:AB:A1:FC:A5:9D:A4:DC:EB
- **Signing:** ephemeral Android DEBUG certificate. Job log explicitly confirms **stable signing not configured**. No private key/keystore is in the public repo.

## New features and corrections
- Read-only **Verify backup before uninstall** in Settings. Password or legacy ZIP import compatibility preserved; no media files copied, no project records changed.
- Verification checks AES-GCM authenticated stream through final block, ZIP path whitelist, duplicates, SHA-256 of each media file, manifest completeness, orphan references and counts.
- Distinguishes a valid archive from one **missing original media**. The backup snapshot now reports missing local files instead of silently claiming full completeness; UI warns explicitly.
- Veo video background checks refuse to use stale Google OAuth tokens or API-key mode, marking video **Auth required** while preserving the existing provider operation ID.
- JVM tests added for password failure, large encrypted file verification, legacy ZIP, checksum mismatch, orphan assets, missing files and missing-media warnings.

## Not yet verified
- No physical Android device test of Google OAuth, backup and restore, gallery export, actual Veo 3.1 eligibility, paid video generation or 4K playback.
- An internally valid backup may still lack a video that was never present locally. **An actual test restore is essential before data deletion.**
- Legacy v0.6 plain ZIP is NOT authenticated against malicious modification, only internally checksum-consistent. Keep it private.
- This version does not automatically migrate data between different Android signing identities. Back up/verify existing local data before uninstalling a differently signed prior APK.
- Stable signing is possible via GitHub Actions Secrets but must be completed with user-owned key material. See [stable-signing instructions](STABLE_SIGNING_SETUP.md).
- Google Cloud $300 promotional credit, account entitlements and IAM are separate from this APK build.

## Documentation
- [v0.8 Backup Verification and Safe Upgrade](BACKUP_VERIFY_V08.md)
- [Encrypted Backup v0.7](ENCRYPTED_BACKUP_V07.md)
- [Stable Signing Setup](STABLE_SIGNING_SETUP.md)
- [Google Login and $300 Cloud Credit](GOOGLE_LOGIN_AND_300_CREDIT_SETUP.md)
