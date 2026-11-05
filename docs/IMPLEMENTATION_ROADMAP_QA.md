# MVG — IMPLEMENTATION ROADMAP, HANDOFF AND QUALITY GATES v1.0
**Purpose:** A literal step-by-step build order that a junior developer or smaller AI model can execute without inventing crucial architecture. This is not permission to report unbuilt features as complete.

## Principles for every PR
- Each PR has single scope, visible functionality, tests, before/after screenshots when UI changes, and migration plan where needed.
- Avoid PRs with only UI mock buttons pretending to invoke a provider.
- Keep main compileable and all docs synchronized; source code becomes mandatory when Stage 1 begins.
- No secret in source, issues or action logs.
- Verify Android / Google API signatures against pinned official docs before writing integration code.
- No live paid API tests without owner approval; mock integration in CI.

## P0 — Blueprint + brand (current stage)
**Deliver:** README, AGENTS, MASTER_BLUEPRINT, design system, provider specs, privacy/auth, avatar/director, roadmap, icon SVG and changelog.  
**Acceptance:** all documents internally consistent; each major feature mapped to a build phase; exact architecture constraints stated; no claims of an APK.  
**Gate:** repo browse/read check for every linked doc.

## P1 — Compilable Android foundation (independent PRs)
**P1.1 Bootstrap**
1. Create Gradle Kotlin DSL, version catalog, Android applicationId (proposal: com.parmtech.mvg; confirm signing identity), Compose Material3, Hilt, Room, WorkManager, Media3, Navigation, Google auth prerequisites.
2. Add GitHub Actions gradle wrapper validation, unit tests, lint, assembleDebug with no paid API traffic.
3. Add debug-only sample data behind a clearly labeled preview/debug flag; strip from release.
4. Smoke install on physical Android and emulator.
Acceptance: clean clone -> Gradle sync -> tests -> APK; launch without crash and no network.

**P1.2 Design system**
1. Implement specified theme tokens, dark/light, typography, buttons, segmented tabs, inputs, bottom sheets, cards, loaders, error views.
2. Compose previews for small/large phone/tablet, IME and accessibility.
3. Instrument screenshot tests and responsive navigation.
Acceptance: every screen has loading/error/empty visual primitives and no clipping at font scale 2.0.

**P1.3 Room schema & projects**
1. Add entities/DAOs/migrations for Project, Asset, PromptRevision, Chat, AvatarPack, Clip, Scene, Job, RateCard, Budget.
2. Add fake memory provider tests and sample data migrations.
3. Projects CRUD, archive/duplicate and recover after force close.
Acceptance: offline use creates/reopens 100 projects with media import and app upgrade preserved.

**P1.4 Studio navigation**
1. Project editor with Generator, Director, Scenes, Assets tabs; model not connected empty state.
2. Prompt autosave and restoration on tab switch/process death; job indicator.
3. In-app floating chat sheet UI with safe window insets, keyboard behavior.
Acceptance: 500 tab switches do not lose prompt or project context.

## P2 — Real Google account & model capability verification
**P2.1 OAuth + connection wizard**
1. Android OAuth sign-in using verified current official library, correct Android client and PKCE where appropriate.
2. Settings for Project ID, region, optional output bucket; secure token store.
3. Test real API auth with nonbillable method when feasible. Distinguish API access from billing confirmation.
4. Sign-out/disconnect and expired credential recovery.
Acceptance: real login on owner debug device; no secrets in logcat, screenshots, Git history.

**P2.2 Vertex provider adapter**
1. Typed VideoProvider + models profile; verified IDs and capability matrix.
2. Adapter for target video models: 3.1, Fast, Lite where account allows.
3. Model picker with exact options per task; reject invalid sample/resolution/ratio/duration/images.
4. Tests: unsupported reference on Lite disabled; 4K not shown where unsupported; 4/6/8 model options only when currently valid.
Acceptance: account-specific capability status verifiably correct; no fake models.

**P2.3 Cost and budget engine**
1. Configurable versioned RateCards with source URL, checkedAt, exact billing unit.
2. BigDecimal unit/quantity math, local estimated daily/monthly caps and per-request confirmation.
3. Stale/unknown rate fallback to N/A plus consent.
Acceptance: deterministic quote tests with sample rates and multiple output counts; double-tap cannot submit twice.

## P3 — Real video workflow
**P3.1 Generation submit**
1. Composer, media attachments, preflight and review dialog.
2. Submit genuine provider request, store operation name.
3. Explicit state machine for queued/submitted/running/failure.
Acceptance: with authorized billed test account, one generated clip appears in provider operation and in app DB.

**P3.2 Durable operation reconciliation**
1. WorkManager poll job with constraints and backoff.
2. Resolve server state on app restart, background, phone reboot and account refresh.
3. Network loss after submission does NOT create second job; operation lookup first.
Acceptance: kill app mid-job, restart, and retrieve exactly one paid result.

**P3.3 Result delivery**
1. Resolve GCS/provider output, download via private temp, verify media metadata and stream/play in Media3.
2. Thumbnail creation, library row, download/share via MediaStore.
3. Download retry separated from generation retry.
Acceptance: 720p and 1080p playable results (4K if legitimately supported) preserved across app restart.

## P4 — Assistant, avatars and film timeline
**P4.1 AI Director chat**
1. Implement independent ChatProvider and ChatThread storage.
2. Default eligible Vertex Gemini model; optional OpenAI/Claude adapters behind actual auth and model verification.
3. Streaming chat, prompt draft schema parsing, Apply to Generator; in-app floating bubble.
Acceptance: chat works while paid operation runs, switching tabs does not reset either.

**P4.2 Image generation**
1. Separate ImageProvider, supported image model selection, quote/confirm, generated image asset.
2. Offer attach-to-composer and image preview/confirmation.
Acceptance: real (approved) output or clear unsupported/permission error; no fake images.

**P4.3 Avatar Studio**
1. Consent and nickname CRUD, CameraX guided angles, Photo Picker, quality hints, app-private storage.
2. Model-aware avatar reference selection, multiple packs per project and reference-count limits.
3. Avatar delete/cleanup and account privacy checks.
Acceptance: 3 avatars with 5 photos each can coexist; model requiring <=3 picks only selected compatible images; deletion verified.

**P4.4 Scenes and timeline**
1. Add/clone/remove/reorder source clips with clear timeline semantics, trim, mute and transitions.
2. Use tested media composition pipeline (Media3 Transformer or legally reviewed alternative).
3. Export sequence into one MP4 and save in Movies/Downloads.
Acceptance: combine 3 real clips in chosen order, exported MP4 correct duration/audio and survives app restart.

## P5 — Release quality / privacy hardening
**P5.1 Robustness**
- Handle expired tokens, 401/403, missing IAM, invalid cloud project, API disabled, billing, quota 429, provider safety, malformed response, network handoffs, remote output expiration.
- Disk full, process death, out-of-memory, invalid file MIME, corrupt video, unsupported video codec, revoked URI.
- Security scan, dependency/SBOM review, lint, accessibility, biometric lock, encrypted storage and safe project export.

**P5.2 Device matrix**
- Android 8–current supported OS version (choose target SDK consistent with release requirements).
- Low-end 4GB RAM Android, mid-range 8GB, high-end, one tablet; both orientations.
- Android 16 + gesture navigation, light/dark, fontScale 2.0, talkback, reduced motion, battery saver, roaming/offline.
- Test spotty 3G/4G, toggled airplane mode, background restrictions, Doze, process kill.
- Camera/photo picker from at least two vendors, 4K 60fps media imported and downsampled, Unicode/Punjabi filenames.
- 200 clips, 20 projects, 1+GB total private media, insufficient storage and export progress.
- Phone thermal throttling while preview/export. Limit simultaneous media decodes and offer reduced performance profile.

**P5.3 APK release**
1. Choose versionCode/versionName; update changelog.
2. Build assembleRelease with signing key stored outside repo or protected GitHub secrets.
3. Generate signed APK; verify SHA-256; sign/verify Android certificate.
4. Fresh install and upgrade install (without data loss) on physical device.
5. Complete end-to-end *one approved paid generation* smoke on release configuration; ensure no debug credentials shipped.
6. Attach release note with build commit SHA, tests actually run, known limitations and download artifact.
7. Never claim "fully working" until entire Definition of Done passes.

## Test cases table (minimum)
| Case | Steps | Pass condition |
|---|---|---|
| T001 | New Project, kill, reopen | Title/draft/assets persisted |
| T002 | Model A -> Model B | Invalid settings removed with notice, cost refreshed |
| T003 | Veo Lite + subject refs | Disabled with accurate reason |
| T004 | Model supports only 8s reference | 4s/6s unavailable |
| T005 | Estimate missing | N/A, no fake zero, explicit override |
| T006 | Rapid tap Generate 10× | At most 1 paid request/operation |
| T007 | App killed after POST before response | UnknownNeedsReconcile; no auto-resubmit |
| T008 | Network off while job running | Local job present, resumes reconciliation |
| T009 | Remote complete, storage full | OutputReady retained, downloadable later |
| T010 | AI Director during generation | Chat and generator states independent |
| T011 | Import 3 avatars and delete 1 | No missing unrelated photos, references updated |
| T012 | Attach 5 photos to max-3 model | User selects <=3; clear validation |
| T013 | Apply assistant prompt | Text inserted; no paid video triggered |
| T014 | Reorder 3 clips | Export plays new order and correct trim |
| T015 | Disconnect and reconnect account | No secrets exposed, project data preserved |
| T016 | Unsupported ratio/resolution | Before-submit validation blocks billable call |
| T017 | 2.0× fonts / talkback | Actions discoverable and visible |
| T018 | Import malformed project ZIP | Reject safely; no path traversal |
| T019 | Reverse orientation mid-generation | State and UI preserved |
| T020 | Signed release fresh/upgrade | Installs, opens, preserves older projects |

## Status board (update with each completed PR)
| Phase | Status | Evidence |
|---|---|---|
| P0 Blueprint | Complete: files committed and read back | GitHub docs + editable SVG brand mark |
| P1 Android foundation | Not started | No Gradle/app source yet |
| P2 Google integration | Not started | No validated Cloud credentials |
| P3 Video generation | Not started | No API test or video output |
| P4 Avatar/chat/editor | Not started | Specifications only |
| P5 APK release | Not started | No APK artifact |

## Every AI developer's final handoff format
1. What was changed (paths and commit SHA).
2. Tested commands and exact pass/fail output.
3. Physical device/API test performed or explicitly NOT performed.
4. Existing risks and blockers, especially provider costs and auth.
5. Next smallest safe PR.
No empty "finished" claims, no fabricated APK links, no background promises.
