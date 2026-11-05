# MVG — ANDROID ARCHITECTURE, DATA & OPERATIONS v1.0
Companion to MASTER_BLUEPRINT.md. Design target: Kotlin-native, offline-first, phone/tablet-friendly, process-death-safe and vendor-adapter-based.

## 1. Build and module layout (prescriptive)
Suggested repo tree:
- settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml, gradle/wrapper/*
- app/ — Android Application, manifest, DI bootstrap, root activity, navigation, notification channels, app icon
- core/designsystem/ — Compose theme/tokens/components, accessibility, screenshot previews
- core/model/ — pure Kotlin entities, units, typed result/errors and capability profiles
- core/database/ — Room schema, DAOs, migrations, transaction repositories
- core/datastore/ — Preferences DataStore, secure credentials interface
- core/network/ — normalized HTTP client, refresh/interceptors, retry and redaction
- core/media/ — URI import/export, thumbnails, metadata probing, safe media copies, Player abstraction
- core/security/ — Android Keystore, biometric gate, provider credential store
- core/providers/ — adapter API, capability cache, pricing and request normalization
- provider/vertex/ — Google's REST or supported official Android transport implementation, operation polling, models, GCS IO
- provider/openai/ — OPTIONAL chat/image provider, enabled only when verified
- provider/anthropic/ — OPTIONAL direct or Vertex partner, model-specific adapter
- feature/projects/, feature/studio/, feature/generate/, feature/director/, feature/scenes/, feature/assets/, feature/avatars/, feature/settings/, feature/exports/
- worker/ — WorkManager job reconcilers, download workers, export cleanup
- docs/, assets/branding/, .github/workflows/

Use modularization progressively: prioritize a compileable minimal vertical slice over creating empty modules. Dependency direction: app -> feature -> domain interfaces <- data/provider implementations. Compose never directly touches API keys, raw Retrofit clients or file operations.

## 2. API contracts (Kotlin-like, conceptual)
**ProviderRegistry**
- listAccounts(): Flow<List<ProviderAccount>>
- listModels(providerAccount, task): Flow<List<ModelProfile>>
- refreshCapabilities(accountId): Result<CapabilityRefresh>
- validateAccount(accountId): ConnectionCheck
- compareMode(request): EligibilityReport

**VideoProvider**
- capabilities(modelId, location): ModelCapability
- validate(request: VideoRequest): List<ValidationIssue>
- estimate(request: VideoRequest, rateCard): Estimate? // null if unknown
- submit(request, accessToken): SubmitResult(operationName, providerRequestId?)
- query(operationName): JobSnapshot
- cancel(operationName): CancelResult // only when API supports
- fetchOutputs(operationName): List<RemoteOutput>

**ChatProvider**
- listChatModels(accountId): List<ChatModelCapability>
- complete(conversation, model, attachments?, responseSchema): Flow<ChatEvent>
- cancel(requestId) where supported

**ImageProvider**
- listImageModels(accountId): List<ImageModelCapability>
- generate(imageRequest): ImageJobResult

Every provider adapter must specify auth requirements, endpoint base URL, geographic region, request schema, max image bytes/count, provider policy constraints, response transform and retryable errors. Keep exact model IDs in a tested data source; do not infer features from the model-name substring.

**VideoRequest immutable snapshot:**
requestUuid, accountId, projectId, taskType, modelId, region, promptRevisionId, promptText, negativePrompt?, sourceAssetIds[], referenceKinds[], durationSeconds, aspectRatio, resolution, audioRequested?, sampleCount, seed?, storageDestination?, consentAcknowledgements[], quotedPriceId?, createdAt. Use model-specific request codecs and validation; never submit unsupported null/default fields with incorrect values.

## 3. Video generation job orchestration
**Local transaction BEFORE network:** save Job + request snapshot + user spend-confirmation receipt + an immutable client request UUID. Then worker reads record.
**Submit:** authenticated POST to documented provider endpoint. Save operationName from response in same DB transaction as submitted state. Network failure during submit is ambiguous: preserve local attempt and use provider-reconciliation/operation discovery if supported; never automatically resubmit without user understanding charge risk.
**Poll:** after operationName saved, bounded exponential backoff with jitter; initial e.g. 5s -> 10s -> 20s -> 30s capped; respect API Retry-After and provider constraints. WorkManager minimum intervals/OS scheduling rules apply; no hard promise of exact 5s in background.
**Response:** map to OutputReady on successful provider completion. Cache remote URI and expiry; download with streaming to app-private temp, verify checksum if exposed else MP4 MIME/size/playable, atomically rename, add Asset.
**Background:** WorkManager reconciles surviving operations under network constraint; use user-visible ongoing notification when appropriate and Android OS policy permits. Process restart always rebuilds pending list from DB.
**Cancellation:** cancellation not guaranteed to prevent provider charges; reflect cancellation status truthfully.
**Budget:** preflight local cap check, perform atomic local spend-reservation to prevent accidental parallel overspend estimates; reservations reconciled to actual billing when known, else shown as estimated. Prompt confirm each call.

## 4. Video project state persistence
Local Room database with foreign keys and WAL, migrations on every schema change. Metadata includes Clip immutable reference, Scene mutable timeline segment. Store large binary media on app-private disk, never in SQLite BLOB.
For SAF media references: persist read permission where possible, else copy into app-owned storage; verify on start. Asset DB maps contentUri and checksum. Export via MediaStore Downloads/Movies with pending state completed atomically; recovery cleans orphan temp files.
Projects export/import: ZIP manifest + files optional; no keys/tokens, include schemaVersion; validate CRC/path traversal/size before import; duplicate IDs remapped; incremental import not destructive.

## 5. Multi-provider model configuration
Provider can be GoogleVertex, OpenAI, Anthropic or Additional. **VideoProvider type distinct from ChatProvider type.** ChatGPT app product labels are not model IDs; only use models returned by provider APIs/curated verified lists and verified account eligibility. Claude via Vertex may require publisher model enabling and region/contract; direct Anthropic separate secret/account.
Model capabilities form a conservative intersection:
(official-model-spec ∩ current-region-availability ∩ live-account-test ∩ selected-task).
If an account cannot be probed due to permissions, show Pending verification, not "All models available". Cache capabilities with verification timestamp and expiry, recheck on launch/model selection and after provider 400/404. Keep historical profile snapshot attached to each Job.

## 6. Authentication architecture
Default: Google OAuth native Android account sign-in with PKCE where applicable, scopes for Vertex API. Avoid placing service-account key file inside the APK. Android is a public OAuth client; treat tokens as sensitive even for private single-user app.
- Cloud Project ID is not a password. Region and output bucket are configuration.
- API keys may work for specific Google endpoints and auth modes, but cannot be assumed for every Vertex media endpoint. Feature-gate per actual API method.
- If a proxy becomes needed for secure service-account access, deploy separately with narrow IAM, authenticated client, secret manager, rate limit and user-specific authorization; never silently route through ParmTech shared hosting.
- Credential Store interface: save / read / refresh / revoke / clear; no raw secret through navigation arguments, saved state or Crashlytics.
- Public OAuth client ID in code is acceptable; long-lived private keys are not.
- Cloud Storage optional only when endpoint supports inline result; otherwise show real bucket setup and necessary permissions; expire signed access URLs responsibly.

## 7. Chat & context isolation
Each project has one or more ChatThreads, each with independent model/provider. Context budget and provider token usage are tracked. System instructions ask for cinematic structured JSON drafts, never direct API calls. Keep assistant's suggestions as *drafts* until user applies.
When images sent to chat, require explicit asset selection; shrink/compress to API size before upload; don't upload all personal media by default.
No chat request may call Generate or consume video credits without user confirmation from Generator screen.

## 8. Image / avatar pipeline
CameraX/photo picker -> image decoder downsample -> EXIF orientation fix -> privacy consent -> quality check -> app-private encrypted-at-rest strategy as feasible -> reference pack. Face-guided pose hints based on local visual position detection if implemented, but never assert identity recognition or immutable unique biometric model.
Upload only selected 1–3 or model-defined count references; return clear unsupported error when selected model lacks subject reference. Reuse same selected reference set and matching prompt description between scenes. Independent seeds/last frame can improve continuity where the API supports them; no guaranteed identity lock.
Avatar file deletion must clean all private source assets not otherwise referenced. Maintain provenance and consent date.

## 9. Timeline/render engine
Separate persistent project scene ordering from provider clip creation. Use local media composition/export pipeline compatible with Android Media3 Transformer or equivalent verified version. Build deterministic composition with:
- clips ordered by Scene.orderIndex
- in/out trim validation
- audio track selection, volume/mute
- transitions limited initially to hard cut/crossfade (only if engine supports)
- final output ratio and resolution, frame rate, bitrate & rotation
- quality/resolution warnings for mixed inputs
- test with 3 sources and varied trims.
Avoid introducing an FFmpeg dependency without licensing, binary size, maintenance and performance review. Media3 API signatures must be verified against the pinned version.

## 10. Networking, observability, error types
- HTTPS exclusively, TLS platform trust, no custom trust-all SSL.
- Standard request timeouts, size caps, backoff, network-aware offline queue.
- Structured domain errors: Unauthorized, Forbidden, Billing, UnsupportedFeature, InvalidParameters, SafetyBlocked, QuotaExceeded, Timeout, OperationUnknown, DownloadFailed, StorageFull, MissingMedia, Unknown.
- Redact Authorization, API key, prompts, photo references, Cloud Storage signed URLs and user identity from logs. Diagnostics are opt-in and local.
- Add performance markers for cold-start, preview decode, API latency, download bytes, export render time (anonymized if logging).

## 11. Android security and app hardening
- minimum SDK recommendation 26+ (validate current support); target latest required SDK at release; support 64-bit devices.
- App-local data encrypted credential protection; configurable biometric lock; FLAG_SECURE on credential entry and sensitive media where desired.
- Keep allowBackup=false by default until a carefully designed encrypted backup exists; exports exclude keys.
- Validate imported filenames, MIME and file signatures; avoid path traversal, decompression bombs and oversized bitmap memory usage.
- Use least-privilege Camera permission only when capture active; Photo Picker avoids broad gallery permission; use SAF for downloads.
- Rate-limit rapid double-taps for paid operations, atomic spend reservation per job.
- CI and PR checks scan for secrets, lint, unit tests, build, dependency vulnerabilities. Signed release keystore remains outside repo.
- No third-party analytics, ads or trackers in private personal MVP by default.

## 12. Testing contract
Unit tests for validation matrix, capability intersection, price quote units, budget reservations, Room migrations, job reducer and state machine; deterministic fake providers; instrumentation tests for Android Photo Picker/camera permissions, UI process restart, account disconnect and recovery, scene export.
Use authorized test account for real paid integration smoke; require explicit budget approval to incur costs. No paid API call inside GitHub Actions by default.

## 13. Architecture decision records
When deviating from initial spec, create docs/adr/NNNN-title.md recording context, considered alternatives, decision, consequences, migration and verification. Keep docs/CHANGELOG.md.
