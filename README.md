# MVG — Private AI Video Studio

**Status:** v0.12.0 Android APK and unit tests verified; Google one-tap sign-in on Home/Settings, silent reauthorization after prior consent, read-only Cloud Project picker, encrypted backup and local Gallery export; user Google Cloud OAuth setup and real Veo access still require owner verification.
**Owner:** Private personal-use Android application  
**Working app name:** MVG | **Tagline:** Your AI Film Studio  
**Primary platform:** Android APK, phone-first with tablet layout  
**Primary model platform:** Google Cloud Vertex AI / Gemini Enterprise Agent Platform (product names can change)

MVG is a premium, project-based cinematic video creation application. Create a project, write or refine a prompt, choose a provider/model, configure **only supported** settings, attach subject/avatar imagery, generate clips, and organize scenes in a reorderable timeline. An always-available AI Director chat helps create prompts and images while generation continues.

## Read this first
1. [MASTER BLUEPRINT](docs/MASTER_BLUEPRINT.md) — product requirements, flows, rules, database entities, acceptance criteria.
2. [ARCHITECTURE](docs/ARCHITECTURE.md) — Android modules, offline projects, provider adapters, operations and security.
3. [MODEL CATALOG + PRICING](docs/MODEL_CATALOG_AND_PRICING.md) — verified model IDs, capabilities and cost-estimate strategy.
4. [UX DESIGN SYSTEM](docs/UX_DESIGN_SYSTEM.md) — design tokens, screen hierarchy, interactions, accessibility.
5. [CONNECTION OPTIONS](docs/CONNECTION_OPTIONS.md) — Google Login OR API Key; exact capability and security limits.
6. [AVATAR + AI DIRECTOR](docs/AVATAR_AND_AI_DIRECTOR.md) — character library, consent, chat, structured prompts.
6. [SETUP / CREDENTIALS](docs/SETUP_AND_SECURITY.md) — Android Google Cloud authorization and first-run setup.
7. [IMPLEMENTATION / QA](docs/IMPLEMENTATION_ROADMAP_QA.md) — phased implementation, test matrix and release gates.
8. [BUILD STATUS](docs/BUILD_STATUS.md) — committed Android source, build blocker, next steps.
9. [AGENTS.md](AGENTS.md) — permanent machine-readable developer contract.
10. [VERTEX REST CONTRACT](docs/API_TRANSPORT_SPEC.md) — video generation, polling and safe recovery.
11. [SCREEN WIREFRAMES](docs/SCREEN_WIREFRAMES.md) — precise screen layout and interaction behavior.
12. [Brand mark](assets/branding/mvg-logo.svg) — editable vector icon concept.

## Non-negotiable behavior
- Create **Project** before making clips; clips, prompts, image references, chat, assets, avatar bindings, jobs, estimates, and timeline order belong to one project.
- **Generator ↔ AI Director** tabs remain accessible. Chat can also become a draggable minimized floating bubble/sheet within the app, without stopping a running server operation.
- Select provider/model first. Dynamically show **only supported** video features: duration, ratio, resolution, audio, number of outputs, image/reference/start/end frames, extension, region, quotas.
- Present a **clearly labeled estimated** cost in currency before any billable request, plus hard per-request / daily / monthly limits and explicit confirmation.
- Project Scene Board: add/duplicate/reorder/trim/preview, versioned prompts, export locally as MP4, never destructively overwrite a source clip.
- Settings > Avatar Library: create multiple private character packs through guided face angles or photo imports; select avatar(s) in the generator. *Reference photos do not guarantee perfect likeness or constitute a provider-native persistent avatar API.*
- Local-first project metadata and assets; remote Google operations may take time, require network, credentials, billing, quota and output storage.
- No API key, OAuth token or service account JSON ever in GitHub, crash logs, analytics or project exports.
- If a provider/model cannot actually perform a feature, **disable and explain it**; do not silently fake the feature or label app-side compositing as native model generation.
- Respect consent and applicable platform/provider rules for people and likeness.

## Stack proposal
- Kotlin, Jetpack Compose + Material 3 customized design system.
- Room / SQLite for projects, timeline, prompt history, chat and job ledger.
- Coroutines / Flow, ViewModel, Hilt, Retrofit/OkHttp or official SDK where suitable.
- Media3 ExoPlayer, Media3 Transformer (verify current APIs), Android CameraX, Android Keystore.
- WorkManager for durable periodic job reconciliation and Android foreground notification for user-initiated long operations as permitted by OS policy.
- Typed provider adapter interfaces. Google Vertex for video first; other video providers only by explicit integration. Optional chat/image provider plugins.
- No mandatory proprietary app backend for single-user mode; Google Cloud services remain external dependencies.

## Execution sequence
- **Stage 0 complete:** Product blueprint, brand direction, security boundaries.
- **Stage 1 in source (not compiled):** Jetpack Compose Android skeleton, project CRUD, video composer, AI Director, avatar photo reference and clip records.
- **Stage 2 partial in source:** Manual short-lived encrypted OAuth bearer credential and Vertex REST transport; Google Sign-in, dynamic model discovery and accurate costs still pending.
- **Stage 3 partial in source:** Veo async submit/poll/download and project persistence; cannot claim real generation without credentials and a passing device test.
- **Stage 4 partial in source:** Avatar photo-first-frame option, Gemini AI Director, simple scene reordering; image generation and MP4 timeline export pending.
- **Stage 5:** v0.4.0 unit tests and APK build passed in [GitHub Actions run](https://github.com/parmtech/MVG/actions/runs/41990960893); Google OAuth account registration, device smoke, durable signing and production hardening remain.

No feature should be represented as complete until a physical-device test validates it. v0.3.0 debug APK is downloadable from [successful Actions run](https://github.com/parmtech/MVG/actions/runs/41990960893). See [BUILD STATUS](docs/BUILD_STATUS.md) and [GitHub Actions](https://github.com/parmtech/MVG/actions).

## Official source links
- https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/veo/3-1-generate
- https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/video/generate-videos-from-text
- https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/video/generate-videos-from-references
- https://cloud.google.com/gemini-enterprise-agent-platform/generative-ai/pricing
- https://docs.cloud.google.com/docs/authentication/api-keys
- https://developers.google.com/identity/protocols/oauth2

**Documentation snapshot:** 2026-11-05. Provider pricing/capabilities/lifecycle change; validate before implementing or billing.

## Authentication choices (v0.4)
- **Continue with Google:** uses native Android Google consent and encrypted short-lived Google Cloud OAuth token. In Settings copy the **installed APK's SHA-1** and register it as Android OAuth client. Required for Veo long-running video calls. Requires Android OAuth client package+SHA-1 registration in Cloud Console.
- **API Key:** paste a Cloud API key into masked on-device field for supported Gemini AI Director generateContent calls. The current Veo predictLongRunning API is **not** enabled in API Key mode. See [Connection Options](docs/CONNECTION_OPTIONS.md).
- Manual OAuth token entry has been **removed**. No service-account JSON or private key belongs in source code.

## $300 Google Cloud Welcome Credit setup
The credit belongs to the **linked Google Cloud Billing Account**, not the Google Login button. Validate credit remaining/expiry, billing-project linkage, eligible Vertex AI service and Veo model quota before generating. Free trial restrictions may prevent access even while credit remains. See [Google Login + $300 Credit Setup](docs/GOOGLE_LOGIN_AND_300_CREDIT_SETUP.md). Existing APKS are debug-signed; for repeatable OAuth SHA-1 and in-place updates, configure the documented optional GitHub Secrets signing keystore (no secret stored in this public repo).

### v0.4 APK OAuth fingerprint
For the [verified v0.4 build](https://github.com/parmtech/MVG/actions/runs/41995652362), Android package `com.parmtech.mvg`, signing SHA-1 `A7:19:BE:35:58:DD:94:8C:43:B1:BF:38:2B:EF:1D:17:E4:1C:0C:41`. These are public identification values, **not credentials**. Future ephemeral debug builds may change SHA-1 until user-managed stable signing secrets are set. See [v0.4 build status](docs/BUILD_STATUS.md).

## Local movie export (v0.5)
The Scenes tab now permits joining **two or more complete, locally downloaded** shots into a single MP4, in storyboard order, with progress/cancel and Preview/Save to Movies/Share/Delete export actions. Local Media3 export does not request new Veo generation or spend Google Cloud credit. Device codec limits apply, especially with 4K. Full details: [v0.5 MP4 Export](docs/VIDEO_EXPORT_V05.md).

**Google OAuth registration for v0.5 APK:** Package `com.parmtech.mvg`, debug signer SHA-1 `6A:EB:D8:F0:A4:B0:77:C5:D4:92:F7:97:EB:BD:5C:97:ED:B6:38:CB`. It differs from previous debug builds. Register the exact installed app's SHA-1 or configure stable signing before using repeated versions. Avoid uninstalling MVG that contains media/projects without a proper backup.

## Portable project backup (v0.6)
Settings includes **Create complete ZIP backup** and **Restore ZIP (add as new projects)**. The Android system file picker saves a portable ZIP containing project metadata, storyboard, prompt drafts, AI Director history and available private media. Imported content is integrity-checked, restored as new project copies, and never replaces existing project IDs. **The backup is unencrypted** and may contain sensitive photos/prompts/videos; keep it private. Tokens, API keys and Android signing secrets are excluded. Read [v0.6 Backup & Restore](docs/PROJECT_BACKUP_V06.md).

**Critical install warning:** v0.6 still uses an ephemeral CI debug signing certificate because stable user-owned secrets have NOT been configured. SHA-1 for this exact v0.6 APK: `F9:ED:62:D0:03:1D:E3:3D:64:AA:05:4D:A8:9F:40:67:0E:75:97:B5`. Earlier v0.5 APKs may have different signing certificates; if the update is rejected, **do not uninstall an app that contains important projects/media unless they have been preserved**. v0.5 does not contain this ZIP backup feature. Use the project's [stable signing instructions](docs/GOOGLE_LOGIN_AND_300_CREDIT_SETUP.md) for future builds.

## Password-encrypted backups (v0.7)
New backups are password-protected `.mvgbackup` files, encrypted with **AES-256-GCM**, a unique random salt, 250,000 PBKDF2-HMAC-SHA256 key-derivation iterations, authenticated streaming blocks and a mandatory authenticated end marker. Backups include available private media and project data, **never** Google OAuth tokens, API keys or signing secrets. Passwords are not saved, and a forgotten password cannot be recovered. v0.7 imports older **unencrypted v0.6 ZIP** archives, but new exports are encrypted by default. [Security/restore details](docs/ENCRYPTED_BACKUP_V07.md).

### APK verification and Android OAuth signing (v0.7)
[Latest verified build](https://github.com/parmtech/MVG/actions/runs/42004882683) • APK SHA-256 `4bdf1eff8643bd7c914bb15d1d1cc72c8728c9c7746e3c46c03258620ecbe8c6` • Package `com.parmtech.mvg` • **this build's debug signing SHA-1** `B6:C9:C8:8A:EA:2F:04:69:58:BF:19:10:7E:8F:DF:F4:7B:65:1D:67`.

**Stable signing NOT configured:** the CI log confirms an ephemeral Android debug certificate. APK upgrades may fail across signing keys. Configure the user-controlled secrets in [Stable Signing Setup](docs/STABLE_SIGNING_SETUP.md). Do not uninstall an older app with important projects/media unless a backup has been **successfully verified for restoration**. Physical Android restore and Google OAuth login remain untested by CI.

## v0.8 — Verify Before Uninstall
Use **Settings → Project Backup & Restore → Verify backup before uninstall** to read every encrypted block and media checksum without importing any data. The verifier checks archive metadata and attachment consistency and reports any original local media omitted from the backup. Verify your backup before changing a differently signed APK. A successful integrity check is **not proof that the backup includes every original file or that a full restore works on your device**; read and follow [v0.8 Backup Verification and Android Upgrade](docs/BACKUP_VERIFY_V08.md).

Background Veo job polling no longer uses Google credentials that are expired or from API-key mode; existing jobs move to **Auth required** until Google consent is renewed. This never re-submits paid video generation.

**Verified v0.8 build:** [GitHub Actions](https://github.com/parmtech/MVG/actions/runs/42008578198) • APK SHA-256 `dde2b99202e5f767608b85009cdeab339bc3965589a3e731aa2ee1652fc67879` • Package `com.parmtech.mvg` • Signing SHA-1 for THIS debug APK: `7A:15:26:87:28:3E:11:0F:94:9B:0D:FB:AB:A1:FC:A5:9D:A4:DC:EB`.

**The signing certificate is still temporary.** Android may reject installing v0.8 over v0.7 because the cert differs. Do **not** uninstall an app containing irreplaceable project data without a backup and tested recovery. Stable signing still requires user-owned keystore GitHub Secrets; [setup](docs/STABLE_SIGNING_SETUP.md).

## v0.9 Android emulator integration verification
The [Android emulator workflow](https://github.com/parmtech/MVG/actions/runs/42011138522) passed **3 instrumented tests, zero failures** on API 35: real Android FileProvider/ContentResolver encrypted backup→read-only verify→add-only restore; rejection of wrong backup passwords without changing existing projects; and legacy v0.6 ZIP compatibility. It uses synthetic local MP4 media and does **not** exercise a paid Google Cloud endpoint. [Full device-test details](docs/ANDROID_EMULATOR_V09.md).

[Successful v0.9 Android APK](https://github.com/parmtech/MVG/actions/runs/42011197591): SHA-256 `fd03382e0590213d40427c2642e7f4939eae82aef83dbb406b85bcb2205d02fb`. Google Android OAuth signer SHA-1 of this exact APK is `B8:8D:65:03:83:E6:BF:D9:53:E5:E1:BA:10:95:B8:E6:57:53:CA:DB`. Stable signing is **NOT configured**; an Android OEM device, actual OAuth consent, the $300 Cloud credit and real Veo output still require separate owner tests. Preserve local projects before uninstalling older APKs with different signers.

## v0.9 — Google reauthorization & emulator-tested backup
When Google authorization is renewed, MVG safely resumes checks of **existing provider operation IDs**, without generating a second paid video. Expired tokens and API-key mode cannot run Google Cloud video polling. Completed MP4 downloads now reject non-MP4 responses, disable bearer-bearing redirects and atomically promote validated temp output. [Full v0.9 guide](docs/GOOGLE_RECONNECT_AND_MEDIA_SAFETY_V09.md).

**Android emulator validation:** Three Android API 35 integration tests passed for encrypted FileProvider backup→verify→add-only restore, bad-password rejection and legacy ZIP compatibility. See [emulator workflow](https://github.com/parmtech/MVG/actions/runs/42011138522). This does **not** test live Google OAuth, Veo, personal Cloud credits or the owner's physical phone.

**Latest v0.9 APK:** [Successful CI build](https://github.com/parmtech/MVG/actions/runs/42011919796). APK SHA-256: `025115f0f40116d57101dee31e966a770c3f8057a8f78392df01a38dbc71bd22`. Public Android signer SHA-1 **for this exact APK only**: `87:36:FD:A0:A5:25:F3:61:A7:DC:74:DC:7D:EA:18:F3:80:8D:36:5B`. The artifact contains MVG_SIGNING_FINGERPRINTS.txt and MVG_INSTALL_NOTES.txt. **Signing is still ephemeral**; a later APK may have a new SHA-1. Do not uninstall older MVG with valuable local projects without a verified backup and restore plan.

## v0.10 — Save every generated scene to Android Gallery
Completed and locally downloaded MP4 clips now offer **Play**, **Share** and **Save scene to Movies** across Generator, Scenes, Assets and Library. On Android 10+, Save puts a copy in the **Movies/MVG** Gallery album via MediaStore. On Android 8/9, use Android's Share chooser. No additional Veo generation API call is made by local save or share. Before saving/sharing, the app checks for a structurally plausible MP4 header. See [Individual Scene Export Guide](docs/SINGLE_CLIP_EXPORT_V010.md).

**Verified v0.10 debug APK:** [GitHub build](https://github.com/parmtech/MVG/actions/runs/42032608687), SHA-256 `2d74ce79670ea340d034a81d230876a2376863ddb564285c282adb509b7d4886`, public signing SHA-1 `6F:4F:1F:DF:DA:67:8A:AA:BD:C0:90:11:DD:AA:F0:01:5B:96:5B:0E`. [Android emulator integration run](https://github.com/parmtech/MVG/actions/runs/42032581802) — **5/5 tests passed**, 0 failures/errors. Source MP4 remains unchanged after Gallery save. Emulator tests do not prove real Veo decoding or Google OAuth on a physical phone. **Stable signing is still unconfigured**: do not uninstall a prior differently signed MVG APK with valuable data without preserving a verified backup first.

## v0.11 — Read-only Cloud readiness
Settings → Google Login → Continue with Google → enter the Cloud Project ID and choose us-central1 → **Check Cloud Project & Vertex API (read-only)**. The diagnostic calls only Cloud Resource Manager projects.get and Service Usage services.get with the short-lived Google OAuth access token. It labels each check PASS, BLOCKED or UNKNOWN and provides troubleshooting. No Veo generation, billing update, API enable/disable or project IAM change occurs. **PASS/PASS does not confirm the $300 promotional credit, model entitlement, 4K permission, quota or a successful Veo output.** [Details](docs/CLOUD_READINESS_V011.md).

**Latest v0.11 debug APK:** [Successful CI build](https://github.com/parmtech/MVG/actions/runs/42039405898) • SHA-256 `90c4d7ca248bcf86a5729ead0eac6acf481d8f6fcb832b88b8fbb2410bd53ba8` • APK signer SHA-1 `F5:C7:A2:1B:97:01:EA:90:13:5D:6D:4E:DE:77:0D:B4:FD:7D:3D:66` for **this exact build only**. Signing still uses a temporary debug key; a differently signed old install cannot necessarily be upgraded without preserving data. Register the exact signing SHA-1 with Google Android OAuth. Physical device and live Google Login/Veo tests remain pending.

**v0.11 emulator regression:** [GitHub Actions successful run](https://github.com/parmtech/MVG/actions/runs/42039406133). Downloaded JUnit report confirms **5/5 Android instrumentation tests passed, 0 failures/errors/skipped**. These verify local Gallery and encrypted backup/restore operations; they do not access your Google account or perform Veo generation.

## v0.12 — Easier mobile sign-in
- **Continue with Google** is now visible on the project home screen and at the top of Settings, with progress/loading feedback and protection against duplicate taps.
- After successful consent, MVG **finds the active Cloud projects you can access** through read-only Google Cloud Resource Manager. A single unambiguous project is selected automatically; multiple projects appear in a picker. Manual Project ID entry remains available under Advanced settings.
- The app can **reuse previously granted Google authorization** without opening a new consent prompt if Google Play services returns a token silently. If consent is needed, you approve it on the device.
- OAuth SHA-1, API Key mode and manually editing advanced Cloud region/bucket are available in expandable settings rather than blocking first-time sign-in. Google Cloud OAuth clients still require registration for the exact APK signer.
- No project creation, billing change or paid Veo generation is performed automatically. [Easy Google Login setup](docs/EASY_GOOGLE_LOGIN_V012.md).

**Verified v0.12:** [successful APK CI](https://github.com/parmtech/MVG/actions/runs/42117626505) and [Android emulator 5/5 tests](https://github.com/parmtech/MVG/actions/runs/42117626478) (zero failures/errors/skips). APK SHA-256: `ea26e48994945da14b637df35b90cdf1bd9f88f5bb27af47127c176a41a0ff90`. Installed APK signing SHA-1: `15:39:41:7E:DB:18:19:AF:E2:13:96:A4:FB:63:B4:9E:C3:9F:5F:43`. **Signer remains an ephemeral debug key**, so Android in-place upgrades over differently signed versions are not guaranteed. Preserve verified backups before uninstall.
