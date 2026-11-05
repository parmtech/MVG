# MVG v0.2.0 — Android APK Build Status

Updated: 2026-11-05

## ✅ GitHub CI BUILD SUCCESS
- **Source commit:** 01ec804bb2a08a3557d3911bf2c9c13972a9f0d3
- **Successful Actions run:** https://github.com/parmtech/MVG/actions/runs/41988115822
- **CI tasks:** :app:testDebugUnitTest and :app:assembleDebug — both passed.
- **Build artifact:** MVG-v0.2.0-debug-apk (GitHub Actions artifact), contains app-debug.apk.
- **APK size:** 18,843,309 bytes.
- **APK SHA-256:** 4245f8f51cea75e253f9e853fcc3bfb05c7c01358e47b7c7c3fbbd5274a6c752.
- **APK integrity:** extracted and SHA-256 matched GitHub Actions log.
- **Not tested:** install/launch on user's Android device, Google Cloud authentication, paid Veo generation, 4K output, media playback and real WorkManager background scheduling.

## What's new in v0.2.0
1. Integrated WorkManager 2.12.0 for best-effort durable status checks of **already submitted** Veo operations.
2. Known operation IDs are never resubmitted as paid generation requests by the background worker. Worker may retry querying/downloading; charges can still arise from Google Cloud policies.
3. Network constraints and exponential backoff; at most 12 retry attempts before prompting user to check again.
4. UI refreshes job status written by WorkManager about every 2.5 seconds when foreground. Android OS may defer WorkManager execution on restricted/battery-saver devices; this is not real-time background streaming.
5. Missing/expired OAuth token prompts reconnect; the v0.1 manual short-lived token design still needs production OAuth sign-in/refresh.
6. Per-record SharedPreferences writes avoid most lost updates between Activity and Worker; Room migration and device concurrency tests remain future tasks.
7. Kotlin JVM toolchain and integration code compile with unit tests for 4K/1080p constraints, pricing and unsupported models.
8. v0.2 versionCode 2 / versionName 0.2.0.

## Main existing functionality from v0.1
- Jetpack Compose project workspace, generator, clip list, Gemini AI Director, Avatar photo library, scene up/down ordering.
- Veo Standard, Fast and Lite model picker, limited resolution/duration/aspect controls, public-rate USD estimates and explicit paid submit confirmation.
- Local clip download and opening with device's media player; GCS output URI supported if account authorizes it.
- Owner-controlled Google Cloud Project ID, region and manually supplied OAuth token encrypted with Android Keystore.

## Not completed (important)
- Native Android Google Sign-In and token refresh, automatic model discovery and account entitlement checks.
- Accurate actual billing, daily cap enforcement, billing alert integration.
- Multi-frame/face-scan avatar capture, multi-subject photos in one request, video edit/export timeline.
- Chat multi-provider model selection, image generation, real-time streaming assistant.
- Android physical device test, live account API test and production release signature.

## 4K nuance and validation
Google's dedicated Veo 3.1 technical specification lists **4K output only for Veo 3.1 Standard** among the three model variants here, with Veo 3.1 Fast/Lite limited to 1080p. Another generic video API guide still mentions "4k" only for Preview; Google documentation is not fully consistent. MVG enables 4k for Standard only, at 8 seconds, subject to account eligibility, quotas, region and the exact REST method. This is a requested option, not proof a user account can actually generate 4K.
Sources:
- https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/veo/3-1-generate
- https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/video/generate-videos-from-text

## Install / debug signing warning
1. Go to https://github.com/parmtech/MVG/actions/runs/41988115822
2. Artifacts -> MVG-v0.2.0-debug-apk -> unzip -> install app-debug.apk.
3. Debug signing certificates on ephemeral GitHub-hosted runners can differ. **Do not uninstall an older MVG app if it contains important private projects/media without first exporting or preserving them**. A mismatched signature can prevent upgrade and deleting/reinstalling can erase all app-private project records, media and tokens. Stable release signing with a secret-backed keystore is a future priority.
4. Never paste credentials into the PUBLIC GitHub repo or issues. Use Settings in the app.

## Next implementation priorities
- Create stable signing configuration backed by secrets (user-managed), validate upgrades while retaining projects.
- Finish native Google OAuth login and refresh.
- Harden price & entitlement checks and provider error handling.
- Add multi-reference avatar support and actual project timeline MP4 export.
- Run an owner-approved paid 4K sample with a small budget only after successful cheaper test.
