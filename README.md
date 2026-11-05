# MVG — Private AI Video Studio

**Status:** v0.9.0 Android debug APK compiled and verified; **Android emulator backup/restore integration tests 3/3 PASS**, Google reauthorization resumes existing Veo jobs without paid re-generation, safer MP4 download validation, encrypted ZIP backup and local multi-clip export; physical phone/Cloud provider tests pending  
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
