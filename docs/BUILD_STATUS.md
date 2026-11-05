# MVG v0.3.0 — Android APK Build Status

Updated: 2026-11-05

## ✅ APK AND UNIT TEST BUILD SUCCESSFUL
- **Build:** v0.3.0, Android debug APK, applicationId com.parmtech.mvg
- **GitHub Actions successful run:** https://github.com/parmtech/MVG/actions/runs/41990960893
- **Built source commit:** 0679a12516cf0e7635dd1d94449786e60c8e72af
- **Tasks passing:** app:testDebugUnitTest and app:assembleDebug
- **Artifact:** MVG-v0.3.0-debug-apk (contains app-debug.apk)
- **APK bytes:** 19,663,268
- **APK SHA-256:** 0a1e0597d391146723dba1fc5b4585d4420ea0d102f718eda2c4871b1173b644
- **Integrity:** extracted and digest compared to GitHub CI job log; MATCH.
- **Not tested:** installing/opening this APK on a physical Android phone, live Google OAuth authorization, Gemini chat via key, live Veo charge, 4K output, long-term worker behavior and device codec playback.

## Connection UI: two authentication choices only (new)
- **GOOGLE:** Continue with Google. Uses Google Identity Services AuthorizationClient with cloud-platform scope, consent/account selection and a Keystore-encrypted short-lived access token. Configured Google Android OAuth client ID must match com.parmtech.mvg and APK's signing certificate SHA-1; signing stability is NOT solved.
- **API_KEY:** Paste API Key -> Save API Key. API key is stored separately from Google OAuth token in Android Keystore-encrypted preferences, not in source code. It is sent only on compatible Gemini AI Director generateContent requests as x-goog-api-key.
- **No manual OAuth token paste field or service-account JSON field.**
- Google Cloud Project ID, location/region, optional GCS destination are routing configuration, not a third authentication method.
- Veo video generation requires GOOGLE mode, because current predictLongRunning endpoint isn't in the documented Agent Platform standard-key method support list. API_KEY mode does NOT silently pretend to support Veo.
- A stored key or OAuth token doesn't by itself verify model permissions, billing or successful generation.

See docs/CONNECTION_OPTIONS.md for complete setup and security information.

## Existing source features
- Project storage, native Compose dark UX, generator with model-aware supported duration/resolution and public estimated cost.
- Veo Standard (4K option), Fast and Lite adapter with long-running operation submit/poll, local download and preview.
- WorkManager best-effort background checks for already-submitted operations, safe retry of status/download only, project drafts, scene ordering.
- Gemini AI Director prompt chat and Apply to Generator.
- Multiple private character photo references in a simple library; one may act as first frame. Native persistent face avatar training NOT supported.

## Pending major functionality
- Native Google account authorization tested on device, correct registered Android OAuth client and stable signing.
- Automated OAuth token refresh or approved backend account flow; current Google Login needs reauthorization when token expires.
- Dynamic model listing, measured user-account billing and strict provider capability/region validation.
- Multi-reference avatar pipeline, guided photo capture, scene trimming and MP4 sequence composition/export.
- Real unit/integration tests for provider HTTP, privacy, and Android WorkManager device behavior.
- Production release signing, on-device smoke and update data-preservation tests.

## Android install / signing warning
1. Open https://github.com/parmtech/MVG/actions/runs/41990960893
2. Under Artifacts download MVG-v0.3.0-debug-apk, unzip and install app-debug.apk.
3. **Debug-signing certificate may change** across ephemeral CI machines. If the phone reports APK cannot update because signatures don't match, do NOT uninstall an older app with important projects/media before exporting or backing up data. Stable release signing must be implemented before regular updates.
4. Never paste any API key, token or service-account JSON into GitHub issues, source, action logs or public PRs.

## Model/4K
ModelGuard currently offers Veo 3.1 Standard 4K with 8s, and 720p/1080p for Fast/Lite. Actual 4K availability is account, region and endpoint dependent; successful build doesn't verify Cloud model entitlement. Location us-central1 means generated material may be processed outside Australia.

## Security
The repo is now public. No owner credentials should be committed. Personal on-device API key entry and encryption do not make untrusted or rooted devices safe; use key restrictions and Cloud billing budgets. Cloud Google Login is preferable for Veo media calls.

## CI-to-live status honesty
Passing app:assembleDebug means the APK is compilable, not a guarantee of working Google OAuth account configuration or paid video endpoint. Live tests must be done with the owner's authorized Google Cloud project and cost approval.
