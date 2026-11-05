# MVG — VERIFIED MODEL CATALOG, COSTS & PROVIDER COMPATIBILITY
**Research snapshot:** 2026-11-05. Providers change features, pricing, quota, regions and lifecycle: every generation must use a verified, stored capability and rate profile.

## 1. Priority models (not automatic account entitlements)
Primary Vertex video candidates documented by Google Cloud:
- **Veo 3.1** — veo-3.1-generate-001; high-quality video, reference & first/last frame workflows where supported, native audio capability.
- **Veo 3.1 Fast** — veo-3.1-fast-generate-001; faster tier, reference capabilities per official API.
- **Veo 3.1 Lite** — veo-3.1-lite-generate-001; cost-focused preview, **no subject reference images** according to its model spec, even though text/image-to-video may be supported.
- Older Veo 2 / Veo 3 endpoints have deprecations, regional availability limits and retirement risk; don't expose by default without active confirmation.
- Google may provide newer image/video endpoints, including Gemini Omni preview: these require **separate** explicit adapter verification, not automatic inclusion from a list.

Useful Google docs:
- https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/veo/3-1-generate
- https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/video/generate-videos-from-text
- https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/video/generate-videos-from-an-image
- https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/video/generate-videos-from-references
- https://docs.cloud.google.com/vertex-ai/generative-ai/docs/release-notes

## 2. Verified general Veo constraints — not universal
Official Veo 3.1 docs describe clip lengths **4, 6 or 8 seconds** and aspect ratios **16:9, 9:16**, commonly 720p or 1080p; 4K has **model/mode/endpoint-specific** support, not a universal toggle. Max outputs per prompt vary (some docs show 1–4). Subject-reference workflows commonly accept up to **3 images** and can require exactly 8s for a model/mode. Check per-model technical spec, region and task restrictions before UI.
- Even when generator can choose a duration, **final film** duration can be many minutes because Scene Board concatenates multiple clips. Do not show a fake single 60s model option.
- Native audio availability/price are conditional by model and mode; show supported audio toggle only if endpoint allows it.
- "Avatar" means our reusable image reference pack, not an undocumented face-training endpoint.
- The Google Flow consumer UI may offer Flow-only features; a feature is enabled in MVG only after public endpoint documentation and account test.

## 3. Capability JSON schema (conceptual source-of-truth)
Each ModelProfile must include:
  providerKey; modelId; displayName; lifecycle; regions[];
  tasks[] (TEXT_VIDEO, IMAGE_VIDEO, START_END, SUBJECT_REFERENCES, EXTEND, UPSCALE);
  supportedDurationsByTask{task:[4,6,8]}; supportedRatiosByTask{task:["16:9","9:16"]};
  resolutionsByTask{task:["720p","1080p","4k"]};
  audioModes[]; maxSampleCount; maxReferenceImagesByTask; maxInputBytesByTask;
  inputMimeTypes[]; supportsNegativePrompt; supportsSeed; maxPromptChars?;
  requiresOutputBucketByTask; authModes[]; operationPollingMode; rateCardId?;
  sourceUrl; verifiedAt; expiresAt; accountEligibility (VERIFIED / UNKNOWN / UNAVAILABLE).

Defaults should be conservative. Example: Veo 3.1 Lite has SUBJECT_REFERENCES disabled until official documentation says otherwise. A future new model can be configured without rebuilding the entire UI if the adapter understands its request types; otherwise show "Integration needed."

## 4. Field dependency validation (before quote/submission)
- Choose model -> choose task -> available duration & resolution/ration & audio & reference counts.
- If task changes, remove unsupported old fields; show change summary.
- When attaching reference: type must match task (first-frame vs subject), count <= max, MIME valid and input size within max, dimensions and ratio checked.
- When choosing 4K: if only some generation modes support it, only offer in those modes; require current model/region confirmation.
- When setting first/last frames: must provide both when required, chronological shape and compatible aspect.
- When requesting extension: source must be a supported generated clip, with documented duration and max cumulative extension.
- Error response from provider suggesting unsupport -> invalidate capability cache and surface "Settings changed; review before resubmitting".

## 5. Pricing policy (never invent tokens/credits)
The pricing information from Google Cloud typically shows model tier, feature, resolution and a **count-based** unit in the table. The app must inspect the **current full billing-unit definition** before using it as "per second" or any equivalent. Do not assume one count equals one video, one request or one second if it is not explicitly confirmed.
Pricing source:
https://cloud.google.com/gemini-enterprise-agent-platform/generative-ai/pricing

Official visible table snapshot (USD **per published count unit**, not an asserted per-video price):
| Model | 720/1080 Video+audio | 720/1080 Video only | 4K Video+audio | 4K Video only |
|---|---:|---:|---:|---:|
| Veo 3.1 | $0.40 | $0.20 | $0.60 | $0.40 |
| Veo 3.1 Fast | $0.10 (720p), $0.12 (1080p) | $0.08 (720p), $0.10 (1080p) | $0.30 | $0.25 |
| Veo 3.1 Lite | $0.05 (720p), $0.08 (1080p) | $0.03 (720p), $0.05 (1080p) | Not listed | Not listed |

**Warning:** Rates are a **research snapshot** only and may not apply to every provider endpoint, account/contract, region, discount, taxes, storage, output count, or private preview. These numbers are **not credits** and must not be shown as a committed charge.

### Quote engine requirements
- RateCard has explicit unit: PER_GENERATED_SECOND, PER_OPERATION, PER_OUTPUT, or PROVIDER_DEFINED_COUNT with verified conversion rule. Do not quote without it.
- Match rate to exact provider + model ID + task + output resolution + audio + region.
- Quote details: user controls (duration, sample count), unit, applied multipliers, base USD, optional currency conversion dated source, storage/egress may add cost, timestamp.
- Calculate money as integer micro-USD/BigDecimal; never use floating-point money math.
- Submit blocks beyond user-configured local caps; a preflight cap is not a Cloud Billing hard stop.
- If rate card stale/missing, show "Estimate unavailable — review Google Cloud billing" and require explicit informed override; never claim $0.
- Preserve estimate snapshot with GenerationJob; if actual bill not queryable, label actual spend **Unknown**, not equal to estimate.
- Keep local spend history in USD with clear time zone and estimated vs charged distinction.
- Support per-request, daily and monthly limits; default nonzero conservative cap with owner confirmation before initial paid call. Google Cloud budgets should be enabled externally.

### Example quote decision
User selects Fast + 8s + 1080p + audio on + 2 outputs. Look up the selected model's verified current price unit and documented quantity calculation, then compute estimate; show cost breakdown and source. If billing unit definition cannot be verified, no number is calculated; show source link and "I understand estimate unavailable" confirmation. DO NOT multiply arbitrary table number by 8 just because the clip length is 8 seconds unless the billing unit is confirmed to be per generated second.

## 6. Model picker display priority
- **Verified in this account** first (Quality / Balanced / Economy tags only if grounded).
- Eligible but not recently tested: "Verify"; unsupported in this region: disabled with reason.
- Unsupported by app adapter: "Integration not yet implemented", never submit unsupported raw call.
- Retired models: only visible as historical job metadata.
- Live request failures override stale capability cache.

## 7. Chat and image model integrations
Chat provider/model dropdown independent from video provider. Vertex Gemini chat and Vertex-enabled partner Claude models are possibilities if enabled. Separate OpenAI API models require their own OpenAI API credential/account and accessible API IDs. The ChatGPT product's "GPT-6 Luna" name should NOT be offered as an API model just because the user sees it in ChatGPT; list only available and verified API-accessible models.
Image generation likewise needs a real compatible Vertex/Gemini image endpoint, not assumption that every chat model can generate images.
Official partner models info:
https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/partner-models/use-partner-models
Google auth info:
https://docs.cloud.google.com/docs/authentication/api-keys

## 8. Suggested model integration test plan
For each candidate model perform account-specific smoke tests only after authorizing cost:
1. Entitlement + region + IAM + billing check.
2. Text video at minimum supported duration, 720p landscape.
3. Portrait and 1080p if documented.
4. 4K if documented and owner approved cost.
5. Image reference, subject reference, start/end, extend only when documented.
6. Audio on/off if supported.
7. Invalid ratio/duration sends **client-side block**, zero billable submission.
8. Permission denied/quota/safety response expected normalized error.
9. Recovery from interrupted long operation + retry download without duplicate generation.

## 9. Unknowns that require live validation before implementation
- Which auth types the owner's Cloud project can use for exact Vertex endpoints.
- Whether a Cloud Storage output bucket is mandatory for chosen task.
- Specific account's Veo quotas, regions, generation count and paid entitlements.
- The current SKU unit definition and any discounted actual bill.
- Any future avatar-native endpoint or model lifecycle changes.
Do not guess unknowns; present clear setup/test states.
