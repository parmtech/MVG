# CHANGELOG

## [0.12.0] — 2026-11-06
### Improved mobile Google sign-in
- Prominent Continue with Google on project Home and Settings; loading status and duplicate-tap prevention.
- Reuse existing Google consent silently when AuthorizationClient can issue a token without opening UI; request user action if permission is not already granted.
- Read-only Cloud Project discovery on successful login: one unambiguous project auto-selected, multiple displayed in a picker, manual Project ID fallback.
- Hide OAuth SHA-1 client setup and manual Cloud region/project fields behind optional Advanced sections; keep API-key mode for users who need it.
### Safety and checks
- Cloud project listing is bounded, pagination-safe, token-safe and read-only. Never changes billing, creates a Cloud project or submits paid Veo generation.
- Pure JVM tests for project listing, invalid pages and error/permission scenarios; conservative auto-select policy avoids changing an existing user's project.
- APK source compile and unit tests PASS: https://github.com/parmtech/MVG/actions/runs/42117626505
- Android emulator integration tests **5/5 PASS**: https://github.com/parmtech/MVG/actions/runs/42117626478
- APK checksum: `ea26e48994945da14b637df35b90cdf1bd9f88f5bb27af47127c176a41a0ff90`.
### Still required
- Real device Google consent, Android OAuth client setup and Google Cloud owner authorization must be validated. Stable signing key not configured.
- Live Veo model eligibility, quota and $300 billing promotional credit not confirmed.

## [0.11.0] — 2026-11-06
### New
- Read-only Cloud setup diagnostic: Cloud Project ACTIVE + Vertex AI API ENABLED, using two HTTPS OAuth GETs without starting a paid video generation.
- Settings displays PASS/BLOCKED/UNKNOWN with actionable troubleshooting and a direct link to Vertex API in Cloud Console.
- Defensive transport: allowlisted Google API endpoints, no redirects, request timeout, bounded JSON response, no access token or raw error payload displayed.
- Invalidate cached results when owner changes Cloud Project or Google authorization, discarding stale in-flight checks.
### Tests and build
- JVM unit tests cover valid/disabled API response, IAM 403, expired login, API-key mode, malformed identifiers and no unauthorized requests.
- [APK CI success](https://github.com/parmtech/MVG/actions/runs/42039405898): compilation, unit tests, signature and checksum verified.
- SHA-256 `90c4d7ca248bcf86a5729ead0eac6acf481d8f6fcb832b88b8fbb2410bd53ba8`; public certificate SHA-1 `F5:C7:A2:1B:97:01:EA:90:13:5D:6D:4E:DE:77:0D:B4:FD:7D:3D:66`.
### Remaining
- Real OAuth authorization, quota and eligibility for the owner's Google Cloud $300 billing credit and real Veo generation are not verified.
- Stable signing secrets are not configured; debug signer may differ each APK build.
- [Android v0.11 emulator tests SUCCESS](https://github.com/parmtech/MVG/actions/runs/42039406133): **5/5 passed, 0 failures/errors/skipped** (Gallery save, corrupted MP4 rejection, encrypted backup/restore, legacy ZIP). Cloud OAuth and Veo live calls are not tested.

## [0.10.0] — 2026-11-05
### New
- Save a finished, locally downloaded scene MP4 to Android's Movies/MVG gallery via MediaStore (Android 10+).
- Share individual completed scenes using Android FileProvider and chooser.
- New actions available in Generate, Scenes, Assets and Media Library, alongside Play.
### Quality and safety
- Reject malformed or missing MP4 files before saving/sharing.
- Clarify project deletion may remove private merged exports and detach downloaded scene records. Keep originals outside the app through explicit Save or Share.
- Saving an existing clip locally never sends a new paid Veo generation request.
### Verified
- [GitHub APK build](https://github.com/parmtech/MVG/actions/runs/42032608687) passed unit tests, compilation and signature verification.
- [Android emulator integration tests](https://github.com/parmtech/MVG/actions/runs/42032581802): **5/5 passed** including MediaStore save/read-back, invalid MP4 rejection and previous encrypted backup tests.
- APK SHA-256: `2d74ce79670ea340d034a81d230876a2376863ddb564285c282adb509b7d4886`.
### Known limits
- Emulator Gallery test checks bytes with synthetic structurally valid MP4, not playback of real Veo content.
- Google Login, live Veo generation/4K decoding and physical-device testing remain unverified.
- Stable user-owned signing still unconfigured; debug APKs may carry different signatures.

## [0.9.0] — 2026-11-05
### Google Login & background recovery
- Google reauthorization resumes WorkManager checks only for existing, nonterminal long-running provider operations; avoids resubmitting paid generation.
- On app resume, no stale token/API-key-mode Veo polling. Check / Download requests fresh Google authorization when needed.
- Secure token persistence failures are reported rather than crashing app.
### MP4 safety
- Reject suspicious/non-MP4 file headers before marking a generated video Complete.
- Prevent forwarding Google bearer credentials to unexpected redirects during Cloud Storage downloads.
- Atomically promote complete temporary MP4 output instead of deleting an existing clip first.
### Tests & artifacts
- Android API 35 emulator tests for FileProvider encrypted backup→verify→restore, wrong-password protection, legacy ZIP compatibility: 3 passed.
- Emulator workflow also triggers on main code changes; JVM regression tests for existing-job policies and MP4 header signatures.
- Build and signing success: https://github.com/parmtech/MVG/actions/runs/42011919796.
- APK artifact now includes explicit signing and install notes.
### Remaining
- Stable signing secrets still absent; debug APK signature ephemeral. Real phone Google OAuth, Veo access, 4K and account $300 credit eligibility unverified.

## [0.9.0] — 2026-11-05
### Added and verified
- Android instrumented integration tests for FileProvider/ContentResolver backup and add-only restore.
- CI emulator workflow API 35 **passed 3/3 tests, no failures**: https://github.com/parmtech/MVG/actions/runs/42011138522
- Verifies encrypted backup media/draft roundtrip, wrong-password rejection and legacy ZIP handling.
### Recovery and safety
- Google authorization renew can resume checks on existing Veo operations without submitting any new paid generation.
- MP4 retrieval hardening prevents malformed content from being promoted to a completed video.
- No private signing key or token committed.
### Builds and limitations
- APK GitHub Actions SUCCESS: https://github.com/parmtech/MVG/actions/runs/42011197591
- SHA-256: fd03382e0590213d40427c2642e7f4939eae82aef83dbb406b85bcb2205d02fb
- Stable signing is still not configured; real phone OAuth, billing and Veo generation remain untested.

## [0.8.0] — 2026-11-05
### Added
- **Verify backup before uninstall** in Settings: read-only authenticated stream verification without importing or overwriting media/projects.
- Encrypted and legacy plain-ZIP archive verification, full media SHA-256 matching and cross-record consistency checks.
- Warnings when original locally generated video, avatar photo or exported movie is not included in backup.
- JVM regression tests for valid, damaged, incomplete and wrong-password archives.
### Fixed
- WorkManager video polling no longer reuses an expired Google OAuth token or polls in API-key mode; shows **Auth required** without resubmitting a paid Veo generation.
- Clear distinction between integrity-verified archive and guaranteed complete/physically restored data.
### Build and limitations
- GitHub Actions SUCCESS: https://github.com/parmtech/MVG/actions/runs/42008578198
- APK SHA-256 verified against CI: dde2b99202e5f767608b85009cdeab339bc3965589a3e731aa2ee1652fc67879
- Still an ephemeral debug signing key; no physical device, Google OAuth or paid Veo generation tested.

## [0.7.0] — 2026-11-05
### Added
- Password-encrypted v1 MVGSEC01 portable backups with AES-256-GCM, PBKDF2-HMAC-SHA256 and authenticated 64 KiB streaming chunks.
- Android Settings password/confirm dialog and private .mvgbackup save picker.
- Restore encrypted archive with owner password, or import v0.6 plaintext legacy ZIP add-only.
- Archive tampering, truncation, trailing data and wrong-password detection; working password arrays cleared after use.
- JVM tests for password strength, encryption/decryption, integrity, truncation and legacy ZIP path validation.
- Owner-controlled stable signing setup documentation (not enabled without keystore secrets).
### Verified
- Unit tests, APK compile and signature checks pass: https://github.com/parmtech/MVG/actions/runs/42004882683
- Downloaded APK SHA-256 checked against GitHub CI; v0.7 signer SHA-1 in docs/BUILD_STATUS.md.
### Still pending
- Physical-device encrypted backup export/import and live Google Cloud integration tests.
- Stable signing private keystore GitHub Secrets. Debug APK has an ephemeral signing certificate.

## [0.6.0] — 2026-11-05
### Added
- Portable owner-triggered ZIP backup for all MVG project metadata and available local media using Android Storage Access Framework.
- SHA-256 verified bounded archive, allowed path validation, temp staging and additive restore that never overwrites existing project IDs.
- Import restores drafts, AI Director chats, local clips, avatar photos and exported MP4 movies while excluding tokens, API keys and signing secrets.
- Save/Restore confirmation and progress in Settings with explicit *unencrypted ZIP* privacy warning.
- Unit tests for ZIP entry names and traversal/path-rejection checks.
### Build / safety
- CI success: https://github.com/parmtech/MVG/actions/runs/42002079397; Gradle unit tests and APK compiled, SHA-256 verified.
- Optional stable GitHub Actions signing now fails fast if only some of four secrets are present.
- Stable signing secrets are NOT set. This v0.6 APK uses its own ephemeral debug certificate.
### Limitations
- Real Android backup/restore and cloud login tests still pending.
- Older differently signed debug APKs cannot simply be replaced without data-migration risks. v0.5 itself does not include the ZIP backup system.
- Unencrypted backups require private storage; encryption is a future improvement.

## [0.5.0] — 2026-11-05
### Added
- Media3 Transformer 1.11.1 multi-scene local MP4 export from complete downloaded project clips.
- Scenes storyboard export progress and Cancel button; exported movie Preview, Save to Movies, Share and Delete with confirmation.
- Persist exported MP4 records per project; safe deletion leaves original generated clips untouched.
### Build & safety
- Verified Gradle unit tests, APK compilation, Android signature and SHA-256: https://github.com/parmtech/MVG/actions/runs/41998640289
- Local export uses phone processing only, with **no new paid Veo generation request**.
- Android device codec, 4K export, physical Google login and actual cloud model access still untested.
- This debug APK has a new signing SHA-1; see docs/VIDEO_EXPORT_V05.md. Stable owner signing remains pending.

## [0.4.0] — 2026-11-05
### Added
- In-app readout and copy action for the **installed** APK's public SHA-1, plus a link to Google OAuth client setup.
- Direct link to Cloud Billing for $300 Welcome Credit remaining/expiry; no claim that app sees billing balance.
- Expiration/freshness handling for short-lived Google OAuth token, reauthorization prompt when stale.
- Optional stable CI debug signing with user-controlled GitHub Secrets and published APK signing certificate report.
- [Detailed Cloud Google Login/$300 billing setup](GOOGLE_LOGIN_AND_300_CREDIT_SETUP.md).
### Build
- Successful unit tests, debug APK and apksigner verification: https://github.com/parmtech/MVG/actions/runs/41995652362
- v0.4 APK SHA-256 verified; Android OAuth SHA-1 for **this build only**: A7:19:BE:35:58:DD:94:8C:43:B1:BF:38:2B:EF:1D:17:E4:1C:0C:41.
### Limits
- Real device login and user's Google Cloud project/Veo $300 eligibility not yet tested.
- Debug sign may vary across runner builds until user configures stable signing.

## [0.3.0] — 2026-11-05
### Added
- Two authentication options in Android Settings: native **Continue with Google** and encrypted **API Key**.
- Google Identity Services cloud-platform permission flow; account returns on-device encrypted short-lived token.
- API key stored in a separate Android Keystore-backed vault and routed via x-goog-api-key for compatible Gemini AI Director calls.
- Model endpoint guard prevents using API Key for Veo predictLongRunning requests.
- AuthModeTest unit tests for both paths, credentials, and video restriction.
- Android APK and unit tests built successfully: https://github.com/parmtech/MVG/actions/runs/41990960893
### Removed
- Manual OAuth token paste field in Settings.
### Limitations
- Google Android OAuth client (package + SHA-1) and actual live device consent still need setup and testing.
- API Key mode is NOT equivalent to Veo video generation.
- Existing timeline MP4 export and other advanced features remain unimplemented.

## [0.2.0] — 2026-11-05
### Added
- Android WorkManager 2.12.0 job reconciliation for already-submitted Veo operations with network constraints, retry/backoff, and output download recovery.
- Foreground UI sync of worker-updated clip states without reissuing paid video requests.
- Unit tests for 4K duration constraints, Lite/Fast resolution gating, and public-list-price calculations.
### Fixed
- Lost updates between background job and UI through per-record storage writes and clip refresh.
- Gradle and CI build with tests; GitHub Actions artifact MVG-v0.2.0-debug-apk verified by SHA-256.
### Known limitations
- Android device install, live Vertex generation and 4K output untested.
- Owner must still supply short-lived OAuth bearer token manually; background retry stops for invalid/expired credentials.
- No timeline MP4 render, image generation or multi-reference native avatar support yet.
- CI debug signing is not stable for guaranteed in-place upgrades; do not uninstall an existing app that contains valuable projects.

## [0.0.0-blueprint] — 2026-11-05
### Added
- Private MVG product specification and permanent AI engineering contract.
- Cinematic Android UX design tokens, screen and interaction spec.
- Kotlin/Jetpack Compose offline-first architecture with real provider adapters and durable jobs.
- Google Vertex AI Veo 3.1 / Fast / Lite model compatibility and estimated-cost design.
- OAuth first-run setup and credential/privacy rules; API-key compatibility caveats.
- Multi-avatar photo reference packs and AI Director floating chat specification.
- Scene timeline, clip ordering, preview and MP4 export plan.
- Phased PR roadmap, release gates, QA scenarios and vector brand concept.
### Not shipped
- No Gradle source, genuine provider credentials, generated video, APK, or production deployment yet.
