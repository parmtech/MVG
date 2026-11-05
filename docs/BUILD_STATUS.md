# MVG v0.11.0 — Verified Android APK

Updated 2026-11-06.

## ✅ Android APK compiler and unit tests
- [GitHub Actions success](https://github.com/parmtech/MVG/actions/runs/42039405898)
- Compiled commit: 2ae08ed1d8775737497da316775a08477c308618
- Tasks: app:testDebugUnitTest, app:assembleDebug, apksigner verify, SHA-256 and artifact upload all PASS.
- APK artifact: MVG-v0.11.0-debug-apk
- Package: com.parmtech.mvg
- APK size: 23,069,299 bytes.
- APK SHA-256: `90c4d7ca248bcf86a5729ead0eac6acf481d8f6fcb832b88b8fbb2410bd53ba8` (verified actual extracted APK against GitHub Actions log).
- Public signer SHA-1: `F5:C7:A2:1B:97:01:EA:90:13:5D:6D:4E:DE:77:0D:B4:FD:7D:3D:66`
- Signing mode: **ephemeral debug**. User-owned stable keystore secrets are NOT configured.

## New in v0.11
- Settings → Check Cloud Project & Vertex API (read-only), after Google Login and entering Project ID.
- Network requests: Cloud Resource Manager projects.get, Service Usage services.get; only authorized HTTPS GET metadata calls; no paid video generation or Cloud configuration change.
- PASS / BLOCKED / UNKNOWN classifications for accessible ACTIVE Cloud Project and Vertex AI API ENABLED.
- 403 IAM/permission errors reported UNKNOWN, not falsely diagnosed as API disabled.
- Client rejects invalid project IDs and expired/non-Google OAuth without attempting Cloud network calls.
- No redirects; bounded responses and request timeouts; raw HTTP error bodies and access tokens never displayed.
- Diagnostic state invalidated when authentication or Cloud Project changes; stale asynchronous results are discarded.
- Unit tests use an injected fake HTTP transport to validate request order, disabled Vertex, IAM denials, malformed Project ID, credentials, source validation and rate/network failures.

## Separate Android emulator test
- Existing 5/5 local backup and Gallery tests last verified in [v0.10 Android emulator run](https://github.com/parmtech/MVG/actions/runs/42032581802).
- **v0.11 emulator run SUCCESS:** https://github.com/parmtech/MVG/actions/runs/42039406133 — Android API 35 instrumentation JUnit report: **5 tests, 0 failures, 0 errors, 0 skipped**.
- Tests: encrypted backup + FileProvider add-only restore, wrong-password rejection, legacy ZIP, MediaStore save/read-back without changing source, invalid-MP4 rejection. This tests local Android media/archive operations, not live Google Cloud authorization or Veo.

## Still unverified
- Android device Google Login and OAuth client identity setup, real Cloud Resource Manager + Service Usage permissions on owner's account, Veo entitlement, free-trial $300 credit applicability, model quotas, actual video generation and 4K playback.
- Read-only check does not prove paid prediction permission or video model access.
- A debug APK may not upgrade over an older APK signed with a different certificate. Never uninstall a prior install containing important media without a verified backup/restore plan.
- See [v0.11 Cloud Readiness](CLOUD_READINESS_V011.md) and [Stable Signing Setup](STABLE_SIGNING_SETUP.md).
