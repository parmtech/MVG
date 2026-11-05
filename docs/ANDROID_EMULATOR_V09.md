# MVG v0.9 — Android emulator integration verification

Verified: 2026-11-05 (Android emulator API 35).

## ✅ Actual Android runtime tests
- **Workflow:** https://github.com/parmtech/MVG/actions/runs/42011138522
- **Status:** SUCCESS
- **Device:** Android emulator, API 35, Google APIs x86_64, GitHub Actions Ubuntu runner.
- **Test suite:** com.parmtech.mvg.PortableBackupDeviceTest
- **JUnit XML results:** 3 tests, 0 failures, 0 errors, 0 skipped.
- **Test report artifact:** MVG-Android-Emulator-Test-Results, attached to the above run.

### Passing tests
1. encryptedBackupThroughFileProviderVerifiesAndRestoresAsNewProject — creates a complete project with app-private MP4 bytes and a draft; writes a password-encrypted archive through Android FileProvider/ContentResolver; independently verifies the archive; restores media as a new project with new IDs; checks original and restored bytes/drafts, and verifies persisted metadata after reloading StudioStore.
2. wrongPasswordCannotImportOrModifyOriginalProject — refuses the wrong password and confirms no existing projects changed.
3. legacyZipIsReadableWithoutPasswordAndDoesNotAutoGenerateVideos — accepts prior v0.6 plain ZIP for metadata restoration without initiating any cloud call.

## ✅ v0.9 debug APK verification
- **GitHub Actions build:** https://github.com/parmtech/MVG/actions/runs/42011197591
- **Built commit:** 00dffe9957de47c443cfb68586dd76ba252ab5ac
- **Build checks:** Gradle unit tests, debug APK build, apksigner verify PASS.
- **APK size:** 23,052,915 bytes
- **APK SHA-256:** fd03382e0590213d40427c2642e7f4939eae82aef83dbb406b85bcb2205d02fb (extracted APK matches GitHub build log).
- **Package:** com.parmtech.mvg
- **Signer SHA-1 for EXACT APK:** B8:8D:65:03:83:E6:BF:D9:53:E5:E1:BA:10:95:B8:E6:57:53:CA:DB
- **Signing:** ephemeral Android debug key. Owner's durable signing secrets have NOT been configured.

## Video operation recovery
- After successful native Google authorization, MVG re-enqueues only previously known nonterminal provider operation IDs. It does **not** issue any new paid Veo generation request during recovery.
- WorkManager refuses polling when Google OAuth token is expired/missing or connection was switched to API-key mode. User can renew via Continue with Google.
- MP4 retrieval validation has been hardened to reject malformed/HTML downloads, credential-bearing redirects and partial video data before promoting to Complete. Network retrieval still needs real provider testing.

## Important limitations
- Emulator tests are NOT tests on the owner's physical Android phone.
- The tests do not interact with the Google account, Google Cloud console, owner $300 credit, Vertex AI quota, Veo models, actual generated 4K outputs, UI touch interactions, or the Android system document picker UI. They exercise Android ContentResolver/FileProvider programmatically.
- The emulator suite exercises archives with small synthetic MP4 bytes, not complex multi-gigabyte real video files. Large archives, interrupted I/O, low disk space, cloud-backed document providers, and Android OEM battery limits still need testing.
- Google Cloud OAuth requires registering package and THIS APK signing SHA-1. Signing will change between GitHub-hosted debug builds until owner-managed stable signing secrets are configured.
- If earlier MVG APK is signed with a different key, Android may reject in-place installation. DO NOT uninstall an old app containing important projects/media until an existing backup has been created AND successfully recovered. MVG v0.5 has no portable backup feature.
- Running emulator tests does not spend the owner's Cloud credit or enable paid Veo access.

## Follow-up
- Real phone owner acceptance test: Google authorization, test restoration using Android system file picker, in-place upgrade with a stable signing identity and a first explicit owner-approved Veo generation within verified available quota.
- Configuration: docs/STABLE_SIGNING_SETUP.md and docs/GOOGLE_LOGIN_AND_300_CREDIT_SETUP.md
