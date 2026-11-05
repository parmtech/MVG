# MVG v0.7 — Password-encrypted portable project backups

Updated: 2026-11-05.

## What changed
- **Settings → Create encrypted project backup** replaces plaintext ZIP export.
- Choose a password of at least 12 characters, type it twice, then choose a location using Android's system document picker.
- The file extension is **.mvgbackup**; the stored content is a password-protected encrypted archive, **not** an ordinary ZIP.
- **Settings → Restore encrypted backup or older ZIP** accepts either a new encrypted .mvgbackup (password required) or a previous v0.6 plaintext ZIP (leave password blank).
- Import is still **add-only**: every imported project and media record gets new IDs/filenames, and existing projects are not overwritten.
- Projects, prompts, film clips, AI Director messages, avatar photos, drafts and available local MP4 media are included. Google OAuth tokens, API keys, keystore data and signing passwords are **never** included.

## Format & security
- Private portable binary format **MVGSEC01** (v1): magic header + random 16-byte PBKDF2 salt + random 8-byte IV prefix.
- Key derivation: PBKDF2-HMAC-SHA256, **250,000 iterations**, 256-bit AES key.
- AEAD encryption: AES-256-GCM with 128-bit tags over streaming 64 KiB blocks, unique counter-derived IV per block.
- Additional authenticated data (AAD) binds header, block number and plaintext block length. An authenticated final empty block verifies that the file wasn't silently truncated; trailing bytes are rejected.
- The archive reader verifies each block **before releasing its plaintext**, then validates all archive paths and individual media SHA-256 checksums, and only then prepares an add-only import.
- The app never writes an unencrypted intermediate ZIP to shared storage. ZIP contents are processed through a verification stream and staged into private app storage for the restore transaction.
- Password is never stored in SharedPreferences, Android KeyStore, GitHub Actions, or the archive. The in-memory working character array is overwritten after the operation. Do not assume rooted devices or memory dumps are secure.
- The encrypted archive protects **confidentiality and integrity**, but there is no password recovery service. A lost password means the file cannot be restored.

## Limits & caveats
- Password must be 12+ characters; use a unique passphrase or password-manager-generated secret. 12 simple characters alone are not guaranteed to defeat offline password guessing.
- Anyone with an unencrypted v0.6 ZIP **already has its plaintext contents**. Creating a new encrypted backup does not erase previous exposed copies.
- An encrypted backup is not a replacement for **verifying it restores successfully**. Keep at least two independent backups and test one before uninstalling a previous debug APK.
- v0.7 supports importing old unencrypted v0.6 files, but no longer creates new plaintext archives.
- The media and metadata limits from v0.6 still apply (512 MiB per file, 2 GiB combined media, bounded record counts). Large archives can fail based on available device disk.
- Android Storage Access Framework and real 2 GiB archive performance remain **untested on a physical device** until the owner verifies them.
- Creating a backup does **not** call Google Cloud or charge the $300 credit. Cloud sign-in must be reauthorized after importing to a different installation.

## Security unit tests
The Java/Kotlin crypto tests verify:
- 200 KiB+ encrypted roundtrip across more than one chunk
- Rejecting a wrong password and altered ciphertext
- Rejecting missing final tags, truncation and untrusted trailing bytes
- Enforcing a minimum password length
The earlier archive-path traversal tests remain in the test suite.

## Stable APK signing remains separate
Public repo source cannot create an owner-owned persistent private key by itself. To preserve Android upgrades and OAuth Android client SHA-1 across future builds:
1. Generate a stable private key **on an owner-controlled machine**.
2. Store its public fingerprint and put its private keystore **only in GitHub Actions encrypted Secrets**.
3. Configure the four secrets documented in [Stable Signing Setup](STABLE_SIGNING_SETUP.md).
4. Re-run CI and register the resulting APK SHA-1 with Google OAuth Android client in Cloud Console.
Never commit, paste or attach keystore bytes/passwords to issues, chat messages or README files.

## Relevant sources
- Source code: app/src/main/java/com/parmtech/mvg/BackupCrypt.kt, ProjectBackup.kt, MainActivity.kt
- Unit tests: app/src/test/java/com/parmtech/mvg/BackupCryptTest.kt
- Previous archive guide: docs/PROJECT_BACKUP_V06.md
