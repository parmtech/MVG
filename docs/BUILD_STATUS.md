# MVG v0.5.0 — Build Status

Updated: 2026-11-05

## ✅ Android Debug Build Verified
- **GitHub Actions run:** https://github.com/parmtech/MVG/actions/runs/41998640289
- **Source SHA:** 0d71d0dbd2f8f629cba8f661922f078196c28027
- **Workflow:** SUCCESS, unit tests, Gradle assembleDebug, apksigner verify and artifact upload all passed.
- **Artifact:** MVG-v0.5.0-debug-apk (contains app-debug.apk, MVG_SIGNING_FINGERPRINTS.txt)
- **APK size:** 23,003,763 bytes.
- **APK SHA-256:** e43b2c1d9d87184741b8f414d3f05854d97819a6fd19ef9542fc40f106605020. Verified against the GitHub build log.
- **Signing SHA-1 for this APK:** 6A:EB:D8:F0:A4:B0:77:C5:D4:92:F7:97:EB:BD:5C:97:ED:B6:38:CB
- **Package:** com.parmtech.mvg
- **Do not confuse build passing with real-device or cloud integration tests.** Physical install, Google OAuth, live Veo generation and local Media3 export are not yet device tested.

## v0.5 functionality
- Scenes tab: complete downloaded clips in current storyboard order can be joined into a single local MP4 with AndroidX Media3 Transformer 1.11.1.
- Export controls: progress, cancel, preview, share, gallery Movies/MVG save (Android 10+) and private-export delete confirmation.
- Persist exported movies per project. Original clips are not changed.
- Does not submit any new Veo video generation call for local export.
- See [v0.5 export guide](VIDEO_EXPORT_V05.md) for restrictions and media support limits.

## Continued v0.4 Google Cloud setup
- Native Google Login via Google Identity Services Android authorization (cloud-platform scope).
- API Key mode remains limited to documented compatible Gemini AI Director calls; Veo requires Google Login in this app.
- $300 Cloud Welcome Credit is a project/Billing Account promotion; Free Trial/model quota/entitlement rules apply. Neither compiling the APK nor signing in proves 4K model access.
- Android OAuth registration for this exact **v0.5** debug APK needs package com.parmtech.mvg and SHA-1 above. Earlier v0.4 build used a different debug signer.
- Optional user-managed stable keystore via GitHub Secrets supported, but **not configured** here. Repeated debug builds may use different certs. See [Google Login & Credit](GOOGLE_LOGIN_AND_300_CREDIT_SETUP.md).

## Known limitations
- No Android device test, provider billing test, model entitlement test, input video codec test or export screenshot validation.
- v0.5 export is foreground Activity-scoped, can stop if app/Activity is destroyed.
- Large 4K videos may fail or be converted in device-specific resolution/codec. No promise that output stays 4K.
- No transitions/crossfades, trimming, professional multi-track editing, auto soundtrack or subtitles.
- No complete migration/backup of app-private projects and movie source files. **Do not uninstall an older MVG app containing valuable data solely because a differently signed debug APK cannot upgrade it.**
- Real Google Login can require owner to register the signing SHA-1 and configure consent screen/Cloud IAM and billing.

## Install
Open GitHub run above, download MVG-v0.5.0-debug-apk under Artifacts, extract app-debug.apk and install on Android. The fingerprint TXT is included for registering the Android OAuth client. This debug APK is not suitable for general release.

## References
- https://developer.android.com/media/media3/transformer/composition
- https://developer.android.com/jetpack/androidx/releases/media3
