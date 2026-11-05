# MVG v0.12.0 — Verified Easy Google Login Android APK

Date: 2026-11-06

## Build and emulator evidence
- GitHub APK workflow: **SUCCESS** https://github.com/parmtech/MVG/actions/runs/42117626505
- Compiled source commit: e8c1fc12435193e79a11da8e685bcc623833d4b8
- Verified app:testDebugUnitTest, app:assembleDebug, apksigner, artifact upload.
- Android API 35 emulator: **SUCCESS** https://github.com/parmtech/MVG/actions/runs/42117626478
- Downloaded JUnit report: **5 tests, 0 failures, 0 errors, 0 skipped**, covering encrypted backups, wrong-password/legacy ZIP, Android MediaStore and invalid-MP4 rejection.
- APK artifact: MVG-v0.12.0-debug-apk; file size **23,085,683 bytes**.
- APK SHA-256: `ea26e48994945da14b637df35b90cdf1bd9f88f5bb27af47127c176a41a0ff90` (downloaded and matched to GitHub CI).
- Android signer SHA-1: `15:39:41:7E:DB:18:19:AF:E2:13:96:A4:FB:63:B4:9E:C3:9F:5F:43`.
- Signing mode: **ephemeral Android debug**; stable private keystore secrets are not configured.

## New mobile Google connection flow
- Large **Continue with Google** action on project Home and Settings, disabling duplicate sign-in taps and showing progress.
- Google AuthorizationClient silent token reacquisition only after prior explicit owner consent, and only when a no-UI token is available. Does not store refresh tokens.
- After successful interactive login, Cloud Resource Manager read-only projects.list discovers accessible ACTIVE projects.
- Auto-select exactly one project only if no project previously chosen and list is complete. Multiple projects displayed via picker, existing selections never overwritten automatically.
- Advanced API-key method, manual Project ID and OAuth Android client SHA-1 setup collapsed in optional settings.
- Safe, bounded read-only project listing (100 per page, max 4 pages / 200 projects, HTTPS host-validated, no redirect, 128-KiB response bound, OAuth token excluded from UI/errors).
- Mocked JVM tests cover pagination, active filtering, missing permission, no Google authorization, network failures and no unsafe auto-selection.
- **No paid Veo generation, billing mutation, account creation or Google Cloud project mutation** as part of onboarding.

## Requirements not verified
- Google OAuth client must still be registered to this exact Android app package **com.parmtech.mvg** and signer SHA-1, including user OAuth consent/test access as appropriate.
- The owner must validate actual login on their Android phone, their Cloud account permissions and selected project, $300 trial-credit applicability, Veo quotas and actual video generation.
- Debug signing certificate may change per build. **Never uninstall an older differently signed MVG install with valuable local media before verifying a portable backup and a workable restore plan.**
- Browser Cloud Console registration was not performed in Chat; connection from a user's browser was unavailable in this session.

See [Easy Google Login](EASY_GOOGLE_LOGIN_V012.md) and [Stable Signing](STABLE_SIGNING_SETUP.md).
