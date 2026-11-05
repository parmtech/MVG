# MVG v0.4.0 — Android Google Login APK status

Updated: 2026-11-05

## ✅ Verified CI build and APK
- **Workflow run:** https://github.com/parmtech/MVG/actions/runs/41995652362
- **Built source commit:** 7fb6d7f0fdad2afa726fa5d79a3bac9d50251d3e
- **GitHub Actions result:** SUCCESS. Tests `app:testDebugUnitTest` passed; `app:assembleDebug` passed; `apksigner verify --print-certs` passed; artifact uploaded.
- **Artifact:** MVG-v0.4.0-debug-apk. Files: `app-debug.apk` and `MVG_SIGNING_FINGERPRINTS.txt`.
- **APK size:** 19,663,268 bytes.
- **APK SHA-256:** `3dcf75a608036e9bfa5f5a2b3eb819d052526b2cebb90ddb0582d7edd7e5f29c`, extracted and matched to CI log.
- **THIS BUILD'S SHA-1 (for Google Android OAuth client):** `A7:19:BE:35:58:DD:94:8C:43:B1:BF:38:2B:EF:1D:17:E4:1C:0C:41`.
- **Package:** `com.parmtech.mvg`.
- **Signing type:** ephemeral Android DEBUG certificate, **not** a durable release signature; next build may have another fingerprint unless signing secrets are configured.

## Changes since v0.3
- Added **Signing SHA-1** shown directly in app Settings with Copy SHA-1 and a link to Google Cloud Auth Platform OAuth clients.
- Added **Check $300 Credit in Cloud Billing** link and visible warning that credits are tied to billing account/project, not the login button.
- Added conservative 45-minute Google token freshness guard, and reauthorization hint on expiry instead of indefinitely claiming Connected.
- Added optional CI signing configuration using **four user-managed GitHub Actions secrets** (see docs/GOOGLE_LOGIN_AND_300_CREDIT_SETUP.md). None of the private signing values are committed to GitHub; absent secrets -> ephemeral debug signing remains.
- CI artifact includes public certificate fingerprints verified from APK by `apksigner`.
- Moved version to 0.4.0.

## What the app can do at source level
- Android Compose Projects/Generate/AI Director/Scenes/Assets/Settings.
- **GOOGLE** — native Google Identity Services AuthorizationClient with cloud-platform scope; token encrypted at rest. Manual OAuth token paste was removed in v0.3.
- **API_KEY** — separate masked encrypted key entry limited to compatible Gemini AI Director calls; **not** a Veo video credential.
- Cloud project ID and region configuration; Veo 3.1 Standard/Fast/Lite generator with quote confirmation, 4K option on Standard (8-second) subject to account/model eligibility; safe long-running operation tracking and MP4 download.
- Local drafts and scene order.

## Not validated / not done
- **Real device** login, OAuth consent screen, model permission, paid Veo generation, 4K output, playback.
- Eligibility of the **owner's** $300 trial credit for Veo; verify remaining credit, billing linkage, model quota/entitlement in Google Cloud Console.
- Permanent Google OAuth token refresh backend or full on-device Google account/profile display. After local 45-minute expiry user must authorize again. Google grant may remain server-side after local Disconnect.
- Stable release signing and Play Store production artifact; only optional CI debug signing support is wired.
- Multi-scene MP4 export, multi-reference avatars, images/other future functionality.

## First-time Google Login setup
1. Install the **exact** v0.4 APK from the workflow artifact.
2. In Settings → GOOGLE, copy the SHA-1 shown (or from this build's fingerprint file). 
3. In Google Cloud Auth Platform → OAuth Clients, create **Android** client package `com.parmtech.mvg` and **matching SHA-1**. Configure consent screen and add your Google account as test user when needed.
4. Verify your $300 credit and selected project's linked billing account; enable Vertex AI API and confirm supported model/quota. `us-central1` is the Veo 3.1 model location, not an Australia-local processing guarantee.
5. Enter Cloud Project ID, save; tap **Continue with Google**; approve access. Actual IAM and billing eligibility can only be confirmed by live account/provider checks.
6. Test without automatic spend: **Check Setup** only validates configuration, not backend entitlement. Confirm rates before any paid Generate.

**Never paste tokens, cloud service-account JSON, keystores, passwords, or API keys into this public GitHub repo or a chat.** Full instructions: [Google Login + $300 Credit Setup](GOOGLE_LOGIN_AND_300_CREDIT_SETUP.md).

## Signing/update warning
The current v0.4 debug APK is signed with a debug certificate from its runner; it **may not update in place** an earlier differently signed v0.3 build. Do not delete an older app with important locally stored project data before backing it up. Stable signing needs user-owned keystore GitHub Secrets; no secret is bundled with APK or source.
