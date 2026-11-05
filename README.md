# MVG — Private AI Video Studio

**Status:** Architecture and design blueprint (no APK or production integration yet)  
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
5. [AVATAR + AI DIRECTOR](docs/AVATAR_AND_AI_DIRECTOR.md) — character library, consent, chat, structured prompts.
6. [SETUP / CREDENTIALS](docs/SETUP_AND_SECURITY.md) — Android Google Cloud authorization and first-run setup.
7. [IMPLEMENTATION / QA](docs/IMPLEMENTATION_ROADMAP_QA.md) — phased implementation, test matrix and release gates.
8. [AGENTS.md](AGENTS.md) — permanent machine-readable developer contract.
9. [Brand mark](assets/branding/mvg-logo.svg) — editable vector icon concept.

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
- **Stage 1:** Compileable Android skeleton, design components and offline project management.
- **Stage 2:** Real Google auth + connection test + capability discovery + costs.
- **Stage 3:** Real video generation with async operations, downloads and project persistence.
- **Stage 4:** Avatar references + AI Director chat/image + timeline export.
- **Stage 5:** Harden, test, optimize, sign and produce installable APK.

No feature should be represented as complete until a physical-device test validates it. There are no working builds in this repo at blueprint stage.

## Official source links
- https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/veo/3-1-generate
- https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/video/generate-videos-from-text
- https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/video/generate-videos-from-references
- https://cloud.google.com/gemini-enterprise-agent-platform/generative-ai/pricing
- https://docs.cloud.google.com/docs/authentication/api-keys
- https://developers.google.com/identity/protocols/oauth2

**Documentation snapshot:** 2026-11-05. Provider pricing/capabilities/lifecycle change; validate before implementing or billing.
