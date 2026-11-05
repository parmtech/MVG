# MVG v0.1.0 — Android Build Status

Updated: 2026-11-05

## APK BUILD SUCCESSFUL
- **GitHub Actions run:** https://github.com/parmtech/MVG/actions/runs/41986788218
- **Source commit:** 331d036d3954663a53179f10e21e2e997d8e0a17
- **Workflow result:** SUCCESS (all build steps passed)
- **Artifact:** MVG-v0.1.0-debug-apk (download from run page; available for 30 days as configured)
- **Embedded APK name:** app-debug.apk
- **APK size:** 18,279,955 bytes
- **APK SHA-256:** 5c7b9dab1b5e84bc402083fff4c173cbddfaec1e254f80e58c6bb6dcd58985e0
- **Type:** Debug APK, NOT production-signed.
- **Verified:** assembled by Gradle, artifact uploaded, extracted and APK ZIP verified; SHA matches GitHub build log.
- **NOT verified:** Physical Android install, launch/smoke, live Vertex account video generation, 4K output, model billing.

## Main working source features
- Kotlin/Jetpack Compose dark project and studio UI; create/delete projects.
- Video composer: Veo 3.1 / Fast / Lite model choice, 4/6/8 seconds, portrait/landscape, 720p/1080p, **4K on 3.1 Standard** with 8 seconds.
- Snapshot public USD rate estimate per generated second, explicit user confirmation before one billable submit.
- Local-first project prompts and scene/clip records; simple up/down scene reordering.
- Gemini AI Director chat and prompt application.
- Optional selected avatar photo as first frame, app-private photo storage; multiple stored named images.
- REST long-running Vertex submission, operation ID persistence, manual poll & GCS/inline video download.
- Short-lived owner-provided OAuth bearer token encrypted with Android Keystore.
- Public GitHub source has **no embedded owner API key**.

## Not yet complete / not tested
- Google OAuth user sign-in and automatic token refresh (v0.1 uses manually supplied short-lived token).
- Full account-aware model discovery, per-feature capability probing, authoritative dynamic pricing and spend caps.
- Reliable WorkManager background operation polling after app termination.
- Guided multi-angle avatar face capture and multi-photo subject reference generation.
- AI image generation, OpenAI and Claude chat providers.
- Trim, transitions and final multi-clip MP4 export.
- Physical device smoke and approved paid live provider test. Unsupported or unverified UI controls may require correction after real API test.

## Build fixes in this iteration
- Public repo allowed GitHub-hosted runner startup.
- Removed legacy android-actions/setup-android@v3 package invocation because sdkmanager "tools" no longer exists.
- Resolved SDK command path explicitly from ANDROID_HOME/cmdline-tools; API 36/Build Tools 36 installed.
- Migrated Kotlin 2.3 jvmTarget to compilerOptions DSL.
- Fixed scene reorder handler and saved per-project prompt drafts.
- Added model-dependent duration/resolution validation and cost quote.

## Download/install
Go to https://github.com/parmtech/MVG/actions/runs/41986788218
Select Artifacts > MVG-v0.1.0-debug-apk. Extract ZIP and install app-debug.apk on Android; install unknown apps only for trusted APK you downloaded yourself. This is a personal debug build; APK signing key is auto-generated for CI and future GitHub runs might use a different debug key, requiring uninstall rather than seamless upgrade. Preserve project data before replacing builds.

## 4K details
Official model spec says veo-3.1-generate-001 supports 720p, 1080p, 4K output; Fast and Lite currently documented up to 1080p. The current MVG picker exposes 4k only for the Standard model, with 8-second clips. Account entitlement/region and successful live generation remain prerequisites, so a checked 4K UI setting is not proof of successful 4K output. Model location for Veo 3.1 is us-central1; using from Australia may process media in US.
Sources:
https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/veo/3-1-generate
https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/video/generate-videos-from-references
https://cloud.google.com/model-garden/pricing

## Next development gates
1. Install and smoke test app on a real Android phone.
2. Repair any runtime crashes.
3. Complete first-run real Google OAuth and provider credential validation.
4. With owner-approved spend, run a small test generation, verify output playback, then 4K/8s if account permits.
5. Add background job reconciliation, multi-photo avatar references and local MP4 sequence export.
