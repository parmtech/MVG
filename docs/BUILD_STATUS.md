# MVG v0.6.0 — Verified Android APK Build

Updated 2026-11-05

## ✅ CI result
- **GitHub Actions:** https://github.com/parmtech/MVG/actions/runs/42002079397
- **Status:** SUCCESS, app:testDebugUnitTest, app:assembleDebug and apksigner verify passed.
- **Built source:** cdc6457133b2cef4038cd373350e3da005493c2c
- **Artifact:** MVG-v0.6.0-debug-apk (app-debug.apk + MVG_SIGNING_FINGERPRINTS.txt)
- **APK file size:** 23,036,531 bytes
- **SHA-256:** 3c6561c61ab32399c9b68eba615d06baa82d73c9b7c8dfd1bf85957ecc53b8d1. Verified from actual extracted APK against GitHub Actions SHA-256 output.
- **Signer public SHA-1:** F9:ED:62:D0:03:1D:E3:3D:64:AA:05:4D:A8:9F:40:67:0E:75:97:B5
- **Android package:** com.parmtech.mvg
- **Signing:** ephemeral Android DEBUG certificate. The CI log says no stable signing secrets were configured. Later runner debug certificates may differ.

## New v0.6 features
- Settings → Create complete ZIP backup and Restore ZIP (add as new projects).
- Uses Android Storage Access Framework destination picker; no all-files permission needed.
- Backup snapshots include projects, drafts, AI Director messages, scene order, metadata, locally saved clips, avatars and exported movies. No Google OAuth tokens, API keys, or signing material.
- Archive manifest has checksums, bounded extraction and allowed-entry path validation. Unit tests include ZIP path traversal rejection.
- Restore is add-only: every project, clip and media entry receives a new ID/path; existing content isn't overwritten.
- User sees explicit warning: ZIP is not encrypted and contains personal data; keep it secure.
- CI signing checks now fail if only some of the four optional stable-signing GitHub Secrets are configured.
- UI disallows starting local MP4 export during backup/restore.

## Limits and not yet proven
- No physical Android install, SAF backup/restore, large archive interruption or cross-device restore verification. CI compilation and JVM unit tests alone do NOT prove these work on every device.
- Backup media limit 512 MiB per file, 2 GiB total, up to 1000 files, bounded metadata. Larger backups fail visibly rather than silently truncating.
- v0.6 ZIP format is intentionally unencrypted; an optional password-protected format is a future enhancement.
- If v0.5 is installed using a differently signed debug certificate, installing v0.6 directly may show an Android signing mismatch. **Do not uninstall v0.5 with valuable data without a separate verified preservation method**, because v0.5 cannot create the new portable ZIP.
- Google Cloud $300 promotional credits, Android OAuth sign-in, actual Veo model entitlement and 4K output have NOT been validated on the user's actual account or phone.
- Stable signing is supported using a user-controlled keystore via GitHub Secrets but is NOT configured here. No keystore or private key is stored in this public repo.

## Docs
- [Portable Backup v0.6](PROJECT_BACKUP_V06.md)
- [Google Login and $300 Credit](GOOGLE_LOGIN_AND_300_CREDIT_SETUP.md)
- [Video Export v0.5](VIDEO_EXPORT_V05.md)
