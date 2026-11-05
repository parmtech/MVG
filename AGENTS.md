# AGENTS.md — Permanent MVG Development Contract (read before every change)

You are an Android product team working on the **private MVG AI Video Studio**. This repository is source of truth. The goal is a polished, functioning, maintainable personal Android APK — never a simulated video generator.

## 0. Authority order
1. User's latest explicit requirement.
2. This AGENTS.md.
3. docs/MASTER_BLUEPRINT.md and linked architecture/safety docs.
4. Confirmed live provider documentation and Android platform behavior.
5. Existing verified implementation.

When a source is stale, flag the discrepancy and propose an update. Never claim a feature supported because it appears in a consumer product UI. Google Flow web app features are not automatically public Vertex AI API features.

## 1. Scope and product principles
- A real end-to-end app: project creation -> composition -> cost estimate -> confirmation -> API generation -> job tracking -> retrieval -> scene ordering -> MP4 export.
- Beautiful, discoverable, one-hand-friendly and fast even on mid/low-end Android phones.
- UI inspired by the simplicity of Google Flow, **not** a pixel-perfect clone, copied trade dress, or impersonation.
- Google Vertex is the video baseline. Chat provider choice is independent from video choice.
- ChatGPT app/model labels such as "GPT-6 Luna" are **not assumed to be public API IDs**; list only models verified for a connected account and supported API.
- Show error states (unauthorized, unsupported, rate-limited, quota, safety, billing, timeout, offline); no fake successes or empty placeholders.
- Never delete user projects on failed API calls or app updates.
- Run background operations per OS limitations; no guarantee of always-online floating overlay or indefinite service.
- Provider compatibility is checked at runtime and versions are pinned/audited.

## 2. Engineering discipline
For each unit of work:
1. Read the relevant docs and inspect current code.
2. State scope, affected files, risks and measurable acceptance criteria in PR.
3. Implement the smallest complete vertical slice, not placeholder "coming soon" claims.
4. Tests: unit, instrumented, offline/error paths, integration using test account when authorized.
5. Run Android lint/format, gradle test and assembleDebug, report commands + outcomes. Never invent results.
6. Update docs/CHANGELOG.md, tests and any affected API capability matrix.
7. Protect signed artifacts, Android signing keys, OAuth credentials, API tokens, billing values.

## 2A. Authentication invariant (effective v0.3)
User requested **exactly two connection methods** in app UI:
1. Continue with Google: native Google Identity Services authorization; encrypted short-lived cloud-platform OAuth access token. Veo operations use Google account authorization. No manual token paste or service-account JSON.
2. API Key: a masked field, Keystore-encrypted device-only entry, used only with API methods officially supporting API keys (currently Gemini AI Director generateContent). Do NOT use a regular API key for Veo predictLongRunning or claim working video generation through a nonexistent key endpoint.
Cloud Project ID/region/bucket are configuration, not extra auth methods. Ensure Google Cloud OAuth Android package and actual signing SHA-1 are configured. Maintain unit tests for key-vs-login enforcement and prevent token/key leakage in public GitHub. Read docs/CONNECTION_OPTIONS.md.

## 3. Model/provider adapter contract
All providers expose typed capability profiles (not hardcoded UI drop-down assumptions), validate request client-side, and produce normalized asynchronous Job state transitions. API transport and authentication remain outside Composables.

No provider calls before user explicitly taps Generate and accepts displayed estimate. On timeout, reconcile operation by idempotency token / provider operation ID **before retry**; avoid duplicate charged jobs.

## 4. Privacy and identity
- Never commit/store raw face scans unencrypted in broadly accessible folders.
- Avatar packs require informed consent from the person depicted; support deletion of source media, derivatives and references.
- No auto face recognition, identity inference, biometrics enrollment, or custom training claims without a real supported API and consent design.
- Use Android Keystore-backed credential protection; prevent secrets in logcat, backups, cloud sync and share intents.
- OAuth user authorization is preferred. Never ship a long-lived service-account private key inside an APK.
- A single pasted API key may be insufficient for Vertex media endpoints. Explain authentication and billing truthfully.

## 5. UI/UX invariants
- Main: Projects, project workspace, Generator, AI Director, Scene Board, Avatar Library (Settings), Library, Settings.
- Generator and chat run independently; overlay minimization never cancels a generation.
- Every generator control displays support state and why disabled; accessible labels; clear currency/units.
- Save drafts and attachment metadata on each significant edit; preserve undo/redo; process death recovery.
- Media preview never blocks scrolling; use thumbnails, caching, low-memory modes.

## 6. Documentation & delivery
Use English in repo technical docs for broad developer compatibility, with Punjabi/English app localization support. No feature may be marked complete without code plus reproducible verification. Provide versioned APK only after real signed/unsigned build and smoke test; do not fabricate artifact links.

## 7. Initial implementation order
P0: scaffold + Room CRUD + navigation + design system -> P1: Google OAuth / capabilities -> P2: Veo generation -> P3: scene board and export -> P4: avatars and AI Director -> P5: release hardening.

**Stop and document** any conflict with auth, quotas, model support, likeness permissions, local privacy, or billing.

## Project backups and signing (effective v0.6)
- The app must allow owner-triggered Android SAF ZIP backup/restore of existing projects, drafts, Director chats and available media, **excluding OAuth access tokens, API keys, Keystore content, private signing files and passwords**.
- The portable v1 ZIP is **not encrypted**. Display clear privacy warning before exporting and avoid automatically uploading it anywhere.
- Restore is add-only: assign fresh project/clip/movie/avatar IDs, validate ZIP paths/checksums/size limits and do not overwrite existing project data or media. Never execute archive contents.
- Do not claim v0.6 can retroactively save a v0.5 installation with a different APK signature. Never recommend uninstalling an app containing valuable data until verified preservation exists.
- Stable CI signing requires ALL four owner-managed GitHub Secrets; if absent, honestly label debug APK signing as ephemeral. Never commit a private keystore or generate a public signing key.
- CI/JVM tests do not substitute for physical Android SAF backup/restore and Google OAuth testing. Reference docs/PROJECT_BACKUP_V06.md and docs/BUILD_STATUS.md before a release.
