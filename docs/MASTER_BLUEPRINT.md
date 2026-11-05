# MVG — MASTER PRODUCT BLUEPRINT v1.0
**Date:** 2026-11-05 | **Status:** implementation specification (not working APK) | **Owner:** single private Android user  
**Goal:** A Flow-inspired, premium AI filmmaking workspace backed by real Vertex AI media APIs, with independent AI prompt/chat/image workflows, reusable characters and an editable project scene board.

> This document is prescriptive. If the implementation diverges, edit the document and record the decision before shipping. Do not confuse conceptual product features with verified provider APIs.

## 1. End-user promise and boundaries
A user opens MVG and sees a library of Projects. Opening a project reveals the film dashboard: **Generate**, **AI Director**, **Scenes**, **Assets**, and **Project settings**. The user chooses a supported video model, sees dynamic settings and an estimated charge, attaches images/avatars, generates a clip, and keeps working with AI Director. Each result is attached to the project and can be put into a custom ordered sequence. MVG can export one playable MP4 from the selected timeline.

MVG is:
- **Android-only at launch**, private/personal-use, APK installable outside Play Store, with potential future Play Store support.
- **Local-first**: project records, chats, drafts, previews and timeline are recoverable offline; calling remote models still requires data connection.
- **Provider-driven**: only actually supported model configurations are selectable, never fabricated.
- **Pay-as-you-go aware**: previews of cost/limits before charge; Google Cloud billing is external to MVG.
- **Character-aware**: can organize high-quality references for recurring subjects, but exact likeness depends on selected model and is not guaranteed.
- **Independent of Google Flow**: creative workflow inspiration, no imitation, no use of undocumented Flow-only APIs.

Not included in the initial verified scope: true provider-side persistent 3D avatar model creation, voice cloning, infinite generations without quota, permanent cloud sync for free, iOS, real-time collaboration, guaranteed video results, or universal access to consumer ChatGPT model labels.

## 2. Information architecture
**Global root**
- First-run connection / authorization wizard.
- Projects home (search, sort, create, rename, archive, duplicate, delete with confirmations).
- Shared Library (optional global stills/avatars/prompt presets and cached exports).
- Settings (Accounts/API, Avatar Studio, Default models, Costs & limits, Downloads, Security, Appearance, About, Diagnostics).

**Project workspace**
- Header: back, project title, status indicator, overflow project menu.
- **Generate** tab: composition and job progress + generated outputs.
- **AI Director** tab: assistant conversation, structured prompt drafts, reference image generation, direct Apply to Generator.
- **Scenes** tab: shot cards, timeline, order/trim/play/export.
- **Assets** tab: uploaded photos, avatars used, clips, reference frames, generated stills and exports.
- Persistent compact job status in tab header, accessible while in chat/scenes.

**Chat overlay**
- Within project, AI Director may be collapsed to a floating pill/bubble anchored safely inside MVG activity, not over other apps.
- Bubble shows unread/working indicator; tap opens bottom sheet with conversation retained, expand to full chat page.
- Closing/minimizing chat never cancels server generation, queued prompt, or asset load.

## 3. Main user journeys with explicit states

### J1 — First install/connect
1. Launch -> three-page feature tour (skip) -> connection wizard.
2. Choose Google Cloud Vertex AI primary provider.
3. Ask for Cloud Project ID, region (valid list, recommended only if supported), auth method, optional output bucket and cost currency (default AUD display with USD base rate).
4. Preferred: **Sign in with Google / OAuth 2.0 user credentials** with the correct scopes, Android OAuth client and consent (no embedded long-lived service account JSON).
5. Alternative: manually entered API key only when selected endpoint explicitly supports key-based auth and appropriate restrictions. Display limitations, test through real API, not a dummy "connected" green tick.
6. Test: authenticated identity, API enablement, publisher/model permissions, quota, billing signal when determinable, Cloud Storage access if needed. If validation cannot prove billing is enabled, mark **Not verified**.
7. Save non-secret configuration locally and protect tokens with Android Keystore-backed encryption. Never store credentials in source.
8. Discover capabilities; a **Ready** badge only after a real successful capability/auth test. Allow offline use of local project features without connection.

### J2 — Create project
1. Tap New project.
2. Enter name (required 1–80 Unicode characters), optional description, default ratio (16:9 or 9:16), project frame rate/export preset, optional thumbnail.
3. Save immediately in Room. Generate UUID, createdAt/updatedAt; create project-private storage directory.
4. Opening goes to Generate with helpful empty state. Project remains after force close or reboot.
5. Archive preserves data; deleting uses confirmation and shows size and delete/keep exported files choices.

### J3 — Compose and generate
1. Choose Provider then **Video Model**. Show known IDs and readiness, speed/quality tier, approximate rate and capabilities; hide retired IDs from active picker (keep historical jobs readable).
2. Select task: Text-to-video, Image-to-video, First & Last frames, Subject references, Extend clip — **only if supported by model**.
3. Composer: required textual prompt; optional negative prompt if supported; scene title; optional prompt preset/version.
4. Media tray: upload image from Android Photo Picker/SAF, attach existing asset/approved avatar pack, reorder references, crop to correct ratio without mutating original.
5. Options rendered from capability schema, not hardcoded: duration, aspect ratio, resolution, audio, count, seed, region, safety/person policy.
6. On model switch, preserve still-valid settings, reset only invalid ones with a readable message and full before/after cost update.
7. Under Generate: cost summary, resolution/duration/count/audio, source currency, rate timestamp and confidence. If an exact rate is unknown, write **Estimate unavailable — confirm billing independently** and require explicit override, never display "free".
8. Tap Generate -> validate assets, consent, connected account, budget, quota; present **Submit billable generation?** confirmation.
9. Create draft Job row with local requestId (UUID) and immutable request snapshot, then submit at most once; update server operation ID once obtained.
10. Running operation is asynchronous; show: queued -> submitting -> running -> processing -> downloading -> completed, or error/policy/cancel status. Persist every transition.
11. On completion, fetch results safely, verify playable file, generate thumbnail, attach to Project as immutable ClipAsset, add new Scene suggestion (not forced into final sequence).
12. The job view contains retry/reconcile, source prompt, exact model and settings, billed amount if retrievable (otherwise estimate), output IDs, download/share options.
13. Do not silently repeat requests on timeout; check the operation first to avoid duplicate charges.

### J4 — AI Director alongside generation
1. While generation runs, tap AI Director or floating chat.
2. Choose assistant provider and available chat model independently from video model.
3. Describe an idea: AI Director returns **short synopsis + enhanced cinematic prompt + shot timing + camera movement + lighting + art direction + character IDs + audio/speech notes + negative prompt + target model considerations**.
4. Assistant supports continuing discussion, manually editing suggestions, and one-tap **Use in Generator** without auto-charging.
5. When user requests an image, select a genuinely connected image-capable model; create still, save to Assets, optionally use as reference.
6. Each AI response may link back to project assets and shots, but AI has no authority to generate/spend/delete/publish unless user explicitly confirms a separate action.
7. Work offline: transcript remains readable; send area displays waiting-for-network, not fake responses.

### J5 — Reusable avatars
1. Settings > Avatar Studio > Create avatar.
2. Owner enters nickname and optionally role/look/persona notes, sets an explicit consent acknowledgement, chooses gallery photos or guided CameraX capture.
3. Guided capture: front, left 45°, right 45°, profile, upward/downward angle, optional neutral and expressive frames; instruct proper lighting and no occlusions. Skip any angle.
4. App makes a private **Reference Pack**: original stills, crops, quality checks, optional color/style guide. Does **not** claim to learn a permanent face embedding or 3D model unless true training exists.
5. At generation, select one/multiple avatar packs; model compatibility adapter caps references (e.g., subject image maximum), preflight flags extra images as unused or offers selected best frames.
6. Prompt references avatar nicknames as a human-readable aid; actual likeness depends on image reference provider support.
7. Show accurate **Identity consistency: best effort** note. Track reference IDs and generation settings so later scenes can reuse images/last-frame options.
8. Avatar deletion: remove media and metadata, clear project references or show broken-source marker while keeping already generated video, revoke share/export references and stop any new uses.
9. Do not allow unconsented third-party face uploads or deepfake/impersonation use violating policies.

### J6 — Sequence and export
1. Scenes tab: unsequenced clips at top, timeline lanes below.
2. Add clip to sequence, change display name, reorder by drag handle or Move up/down accessible menu, duplicate as reference (no media copy), remove from sequence without deleting original.
3. Each scene stores source clip ID, in/out trim, optional audio mute/volume, transition, title and updated revision.
4. Local preview plays clips in ordered sequence; if not downloaded show clear unavailable/download status; never silently omit.
5. Export screen: title, requested output ratio/resolution/fps/bitrate (show realistic options based on sources/device), total duration, estimated disk space; warn upscaling does not add true captured detail.
6. Use a media editing pipeline with device compatibility checks, progress, cancellation, crash-safe temp file; publish final MP4 via MediaStore/SAF.
7. If any clip lacks file, surface a repair/download action, not a corrupted exported film.
8. Maintain export history and keep source clips unchanged.

## 4. Detailed UI & state rules
All screens must define **loading, empty, success, error, disabled, offline** states. No white page flashes. Never show "no clips" while clips are still loading. Video jobs display independent status so users can switch tabs.
- All taps have feedback and at least 48dp target on Android.
- Back gesture from overlay closes overlay, then returns to project; unsaved prompt draft is retained.
- Large photos/videos decode to sample sizes; thumbnail list never decodes full 4K.
- Support portrait phones, Android tablets, screen rotations and split-screen; no horizontal overflow, clipped buttons, overlap with keyboard/notches/gesture bar.
- Preserve chosen model and project draft across process death; re-run capability check when stale.

## 5. Functional requirements (MUST = release blocking)
| ID | Requirement | Required evidence |
|---|---|---|
| FR-01 | Project CRUD + offline persistence | Instrumented test + process restart |
| FR-02 | Per-project draft, history and assets | DB migration + screenshot test |
| FR-03 | Auth validation, private credential storage | Integration test, no secret in logs |
| FR-04 | Live/curated provider capability adapter | Contract tests, unsupported option blocked |
| FR-05 | Honest price estimate with version timestamp | Unit tests for cost multiplication, stale/unknown behavior |
| FR-06 | Explicit per-request spend confirmation | UI and negative-path test |
| FR-07 | Submit video and persist provider operation ID | Real integration test with authorized test account |
| FR-08 | Resume/reconcile unfinished jobs safely | Kill/reboot/timeout tests |
| FR-09 | Download, integrity check, thumbnail & preview | Real MP4 playback tests |
| FR-10 | AI Director independent chat/model | Mock provider tests; optional real provider smoke |
| FR-11 | Image generation -> project assets | Real provider test (if enabled) |
| FR-12 | Multiple avatar reference packs | Camera/gallery test; consent + deletion |
| FR-13 | Model-specific image reference constraints | Compatibility matrix tests |
| FR-14 | Scene reorder, trim, export MP4 | 3-clip deterministic export test |
| FR-15 | Offline operation and graceful errors | Flight mode, network loss, retries |
| FR-16 | Secure secrets, optional app lock | Security checklist and restore test |
| FR-17 | UX polish on low-end Android | Frame/drop and memory profiling |
| FR-18 | Installable APK from reproducible CI build | GitHub Actions artifact + device smoke |

## 6. Domain entities (minimum)
All entities include stable UUID primary key and timestamp fields; local migrations are **additive** with tests.
- **Project**: id, title, description, defaultRatio, exportPreset, thumbnailAssetId, archivedAt?, createdAt, updatedAt.
- **ProviderAccount**: id, providerType, label, projectId?, location?, authType, secureCredentialAlias, state, lastValidatedAt. No raw secret in Room.
- **ModelProfile**: provider, modelId, displayName, tasks[], durations[], ratios[], resolutions[], supportsAudio, supportsNegativePrompt, supportsSeed, sampleRange, referenceLimits, regionAvailability[], rateCardRef, lifecycle, verifiedAt.
- **PromptRevision**: id, projectId, source(ai/manual), userText, enhancedText, negativeText, modelTarget?, parentRevisionId?, createdAt.
- **Asset**: id, projectId?, type(image/video/avatar_ref/export/audio), origin(import/generated), localUri?, remoteUri?, mimeType, bytes, checksum, width, height, durationMs?, permissionState, thumbnailUri?, createdAt.
- **AvatarPack**: id, nickname, consentFlag, consentAt, description, coverAssetId?, qualityState, deletedAt?.
- **AvatarReference**: avatarPackId, assetId, angle(front/left/right/other), cropRect?, qualityFlags, selectedRank.
- **ProjectAvatarLink**: projectId, avatarPackId, selected, shotNotes.
- **GenerationJob**: id, projectId, providerAccountId, modelId, mode, requestSnapshotJson, idempotencyKey, operationName?, attemptCount, state, providerErrorCode?, initiatedAt, completedAt?, estimatedUsd?, billedUsd?, outputAssetIds[], lastPolledAt?.
- **Clip**: id, projectId, assetId, promptRevisionId?, generationJobId?, originalDurationMs, status.
- **Timeline**: id, projectId, title, revision, targetRatio, updatedAt.
- **Scene**: id, timelineId, clipId, fractionalOrderIndex, trimInMs, trimOutMs, mute, gain, transitionType, transitionMs, label.
- **ChatThread**: id, projectId, provider, modelId, systemPromptVersion, createdAt.
- **ChatMessage**: id, threadId, role, text, linkedPromptRevisionId?, linkedAssetId?, requestId?, state, createdAt.
- **RateCard**: id, provider, modelId, unit, usdPerUnit, resolutionTier?, audio?, effectiveAt, sourceUrl, checkedAt, verifiedStatus.
- **BudgetPolicy**: localCurrency, showCurrency, exchangeSource?, maxPerRequestUsd, dailyUsd, monthlyUsd, warningThresholdPct, blockedAtLimit.
- **ExportJob**: id, projectId, timelineRevision, targetUri?, parametersJson, state, progressPct, error?, startedAt, finishedAt?.

Relational rules: Project has many Jobs, Assets, PromptRevisions, Timelines, Threads. Timeline has ordered Scenes referencing Clips. A Clip references one immutable Asset; deleting from timeline does not delete Clip. Asset original is immutable. Every generated output links to its exact request snapshot.

## 7. State machines
**GenerationJob:** Draft -> Preflighting -> AwaitingConfirmation -> Submitting -> Submitted -> Running -> OutputReady -> Downloading -> Complete. Terminal alternatives: Rejected, Failed, PolicyBlocked, CancelRequested, Cancelled, UnknownNeedsReconcile. Retry is an explicit **new request** from a terminal failure unless provider can safely resume the same operation. Operation unknown after network failure = reconcile first.
**Network**: Disconnected / Connecting / Verified / Expired / PermissionDenied / BillingUnknown / Limited.
**Asset**: ExternalUri -> Copying -> LocalReady -> RemoteUploading -> Uploaded -> Verified; or MissingPermission / Failed.
**Export**: Ready -> Preparing -> Rendering -> Writing -> Done; or Paused/Failed/Cancelled.
Never conflate the server's operation completion with local successful download.

## 8. Cost, privacy and authorization rules
- Model cost is **USD estimate** with optional approximate AUD conversion. Display user-visible unit (e.g., per generated second) only when verified against current vendor billing specification.
- Input formula is versioned in RateCard; examples: (durationSeconds × outputCount × applicable rate), plus any documented resolution/audio premiums, tax and storage/network caveats. No missing pricing values replaced with zero.
- At preset submit, show a modal confirmation with the full estimated maximum; in-flight partial output/cancellation might still incur vendor charge.
- Local cap gates all submission; local cap cannot enforce upstream cost outside this app. Point user to actual Google Cloud budget alerts.
- App lock with biometric or device credential. Allow clearing data, exporting projects **without credentials**, deleting avatar captures, revoking sign-in.
- No cloud sync or telemetry by default; optional diagnostics scrub secrets, prompts, faces and media URIs.

## 9. Non-functional acceptance thresholds (targets, not promises)
- Home cold launch to usable local Projects UI: target <= 2.5 seconds on representative mid-range phone with 100 projects.
- Touch/scroll: target 60fps on midrange, adapt under load; no UI-blocking decode, disk or API.
- Timeline of 200 clips renders with virtualization; local search debounced, stable keyboard input.
- Crash-free usage target >= 99.5% in internal tests; no OOM from original 4K images.
- All project records accessible without network; remote generation clearly disabled offline.
- Battery: use bounded polling/backoff, WorkManager and OS-compliant notifications; never keep network wakelock all night.
- Storage: warn when below 1 GiB available or estimated export + safety overhead.
- Accessibility: contrast >= WCAG AA where practicable, screen reader, 200% font scaling, reduced motion toggle, non-color status labels.

## 10. Failure & recovery matrix
- 401/403: stop, suggest reconnect and account permission; preserve job state.
- API not enabled/billing unavailable: surface setup instructions, no phantom completions.
- 400 unsupported setting: explain exact conflicting option, revert only invalid setting.
- 429/quota: use Retry-After and exponential backoff with jitter; do not duplicate charges.
- Network lost after submit: mark UnknownNeedsReconcile, look up server operation, never blindly resubmit.
- Content/safety rejection: show human-readable reason when provider supplies it, keep original prompt.
- Operation successful but download failed: hold OutputReady and allow download retry.
- Android kills process: restore Room state and WorkManager job reconciliation.
- User revokes gallery URI: mark broken asset and request regrant, not auto-delete clip.
- Stale pricing: show unable to verify and require explicit confirmation; never "0 credits".
- Storage full: pause output copy/export, retain remote operation and temp cleanup.
- Lost auth while chat active: chat transcript retains locally and send is clearly blocked.

## 11. "Definition of Done" for a whole-app release
The release APK can: create offline project; connect via real Google account or supported auth; verify at least one real Vertex video model; submit approved paid video generation; recover result after kill/restart; download/play; chat with connected provider; create/delete multiple reference packs; reorder at least three scenes; export playable combined MP4; lock app; report estimated costs without false promises. No placeholder "Connect" or "Generate" action may pass release QA.

## 12. Explicit implementation exclusions / decisions
- Do not integrate billing with GitHub. GitHub stores code only; cloud account charges are entirely external.
- No user backend required for v1; remote GCS may be required for videos. If backend becomes necessary, create a separately reviewed architecture and threat model.
- Prefer OAuth authorization by the human owner; do not assume a simple Vertex API key can call every endpoint.
- No "guaranteed 100% identical face" copy; identity consistency is controllable only within provider capability.
- No scraped/undocumented Google Flow API usage.
- Models not in official documentation must enter through provider adapter verification, not a fake model dropdown.

## 13. Dependency map
Read [ARCHITECTURE](ARCHITECTURE.md), [MODEL CATALOG](MODEL_CATALOG_AND_PRICING.md), [UX](UX_DESIGN_SYSTEM.md), [AVATAR/AI DIRECTOR](AVATAR_AND_AI_DIRECTOR.md), [SECURITY](SETUP_AND_SECURITY.md) and [QA](IMPLEMENTATION_ROADMAP_QA.md). The acceptance matrix above is canonical for delivery.

**This document is a blueprint, not proof of implemented source code or an APK.**
