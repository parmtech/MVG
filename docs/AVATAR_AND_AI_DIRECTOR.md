# MVG — AVATAR STUDIO + AI DIRECTOR PROMPT SYSTEM v1.0

## 1. Avatar Studio goals
The user may create and keep **multiple named character reference packs** in Settings. Each pack contains user-chosen photos and pose labels; a generator can attach one or more compatible packs to a model request. This is a non-destructive asset management capability, not proof of a model-native face scanner, trained identity, or guaranteed perfect character consistency.

### Avatar create wizard — exact steps
1. Settings > Avatar Studio > New Avatar -> show privacy/consent notice.
2. Nickname (required) + optional description/look: hair, clothes, age range if volunteered, costume, continuity rules.
3. Capture mode: Guided Camera / Import Photos / Combine.
4. Guided camera quality: neutral front angle, 45° left, 45° right, profile left, profile right, face slightly up, down, optional full-body/costume. Each view optional. Request camera permission only on capture action.
5. On-device validation: media type, image size, blur/lighting guidance, cropped preview and face-framing hints if implementing local landmarks. No identification/matching claims.
6. Review: select cover, choose best photos, remove unwanted, add outfit variations, label individual shots.
7. Save with consent timestamp to encrypted or access-controlled app-private storage, plus media metadata in Room.
8. Avatar details: edit, recapture, import, reorder/relabel refs, preview reference set, projects using it, delete.
9. When choosing an avatar in Generator, present actual target model's max reference count / types. The user selects which refs to submit. Never silently send an unseen subset or all photos.
10. Allow multiple avatars in project, but if the model accepts only one subject reference group or fewer refs, show a blocker explaining compatibility. If no reference support, user can only include avatar's visual description in prompt **without implying actual identity control**.

### Privacy/consent policy
- Consent from each real depicted person must be documented before personal image generation.
- Minors or sensitive likeness usage require restrictions per provider policies; no nudity, deceptive impersonation or harassment.
- Raw face photos are sensitive: no analytics, no cloud upload beyond user's explicit generation request, no global gallery exposure.
- Delete local originals and derivatives on user request subject to surviving video outputs; explain that already generated video cannot retroactively remove likeness.
- Store reference pack provenance (source/import date/owner note, consent timestamp) and avoid face recognition identity databases.

## 2. Cinematic AI Director: goal and roles
AI Director acts as a **creative assistant** while the Video Generator remains independently usable. It asks minimal clarifying questions when truly essential, produces film-ready prompt drafts and optional reference stills, preserves each scene's story continuity, and **does not submit paid video generations**.

Inputs available by explicit project sharing:
- Project title, description, genre, target ratio, timeline ordering and previous generated clip descriptions.
- Selected character pack nicknames and *text descriptions* (raw face photos only if user opts to attach them to chat).
- Selected provider/model with capability profile and clip duration.
- User brief, genre, mood, lighting, camera style, dialogue, environment, continuity constraints, cultural/language preferences.
- Cost and provider limitations: do not promise unverified capabilities.

Recommended context structure:
PROJECT_BIBLE { projectId, genre, storySynopsis, tone, palette, era, timeline, aspectRatio, recurringLocations[], characterCards[], styleGuide[], prohibitedDetails[], lastRevision }.
SHOT_CONTEXT { shotId?, precedingShotSummary?, nextShotSummary?, targetedDurationSeconds, modelId, capabilityProfile, referenceAssetIds[] }.
USER_REQUEST { freeform text, optional attachments, targetLanguage }.

## 3. System instructions — canonical initial template
**Persona:** "You are MVG AI Director, a seasoned cinematographer, script supervisor, prompt engineer and film editor. Your job is to help the owner create coherent, cinematic video scenes using real AI-video model limitations. Be concise and practical. Never fabricate provider APIs, supported resolutions, credit costs, actor identity guarantees, or successful generations. Keep characters, wardrobe, geography, time of day and shot direction consistent across a sequence. If the owner requests a prompt, output an editable **StructuredShotPrompt** as well as a natural-language cinematic prompt. If an instruction needs a provider-only feature not supported by the target model, mark the field unsupported and give a feasible alternative. Never call a paid generation endpoint or delete assets without explicit owner confirmation in the generator." 

Preferred prompt assembly order:
1. Subject/characters and action.
2. Location/set and exact time of day.
3. Emotion and narrative purpose.
4. Camera shot size (wide/medium/close), lens/focal aesthetic, POV.
5. Camera angle, motion and trajectory.
6. Blocking, movement tempo and continuity.
7. Lighting, palette, weather, grade and material realism.
8. Voice/dialogue/music/sound when native audio supported.
9. Duration/ratio/resolution if provider permits.
10. Negative constraints only if provider supports them; otherwise translate to positive direction.
11. Identity continuity hints + linked image reference IDs that the owner actually selected.

### StructuredShotPrompt (conceptual JSON fields)
- title: string
- goal: string
- subjectAndAction: string
- setting: string
- characterRefs: [avatarPackId]
- wardrobeAndContinuity: string
- cinematography: { shotSize, lensStyle, cameraAngle, cameraMotion, focusBehavior }
- visualStyle: { lighting, timeOfDay, colorGrade, atmosphere, realismNotes }
- audio: { ambience, music, dialogue: [{speaker,language,text}] } or unsupported
- timing: { totalSeconds, beats: [{startSecond,endSecond,action}] }
- aspectRatio: string
- resolution: string
- negativePrompt: string or null
- nativeVideoPrompt: string
- sourceCapabilitiesCheckedAt: datetime
- warnings: string[]
- suggestedNextShot: string

The output is a **draft**. Parse carefully using validated schema, never execute AI-supplied code or arbitrary URLs, and show a manual review/Edit screen.

## 4. Prompt examples
**Action/cinematic:** "A lone traveler stands under a rain-soaked Sydney train platform canopy at dusk. Slow push-in from waist-up to close-up as a train's headlight passes behind, turquoise reflections streak across the wet tiles. Natural physical movement, restrained emotion. 35mm lens feel, shallow depth of field, soft volumetric haze, 16:9, 8-second shot. No on-screen text." (Only apply 8s/16:9 after model validation.)
**Character continuity:** "Use the selected Character Ref A image(s) as visual reference where the model supports subject reference. Same hairstyle, face features and jacket across shots; do not change outfit unless this scene says so. Camera slowly arcs 15 degrees clockwise while subject turns toward frame left."
**Interview/education:** "Medium two-shot, warm studio key light, soft negative fill, slow lateral dolly, microphone on desk, presenter speaks a single short line in Punjabi; natural voice timing and accurate lip movement are model-dependent, so keep dialogue short."

## 5. Director action buttons (exact outcomes)
- **Refine:** text -> StructuredShotPrompt and edited natural prompt, no paid media call.
- **Shot List:** a sequence of 4/6/8-second shots according to model, suggested camera angles.
- **Character Match:** optional guidance + recommended reference attachments, cannot guarantee facial match.
- **Make Reference Image:** separate explicit image model selector, show image estimate/confirmation and save image to Assets.
- **Use in Generator:** writes prompt revision to project Draft, navigates to Generate, sets only compatible options, **does not** submit.
- **Make Next Shot:** uses current project bible and previous scene; produces a new draft.
- **Copy/Save:** copies textual prompt or stores it as saved revision.
- **Explain Cost:** responds from RateCard snapshot, clarifies actual charges may differ.

## 6. Chat provider adapters and fallback
- **Default** if available: connected Vertex Gemini chat model.
- **Optional:** Anthropic Claude on Vertex partner models when account and region support it, or direct provider credentials.
- **Optional:** OpenAI API chat/image models with independent API key or supported authorization. ChatGPT consumer-only labels (including Luna) are not equivalent to externally callable API model IDs.
- Model dropdown only lists verified compatible API model IDs; unavailable ones disabled with reason.
- Image requests are not silently routed through text chat; require a distinct image model adapter.

## 7. Floating chat concurrency and state
Chat UI may be full tab, bottom sheet, or minimized in-app bubble:
- Separate Director ViewModel / message DB from Video Generation Worker and state.
- Switching the chat tab preserves video prompt, scroll position, pending generation and selected images.
- Multiple user messages queue sequentially per thread by default. Streaming answers cancel gracefully; do not duplicate on reconnection.
- Pending media job and chat request each have independent notification/status.
- Bubble only floats in app; Android overlay outside app is not necessary and not requested.
- Support keyboard/IME resizing, screen reader, tablet split layout.

## 8. Prompt quality evaluation
Every enhanced prompt should be checked automatically (deterministic validations first) for:
- The requested shot fits selected duration and aspect.
- Character identity and outfit are consistent with prior scene notes.
- Prompt does not refer to unavailable reference APIs.
- No contradictory camera motions or multiple simultaneous narrative beats in 4 seconds.
- Spoken dialogue realistically fits clip duration; no guarantee of perfect lip sync.
- Safety constraints, consent and provider policy.
- Explicit final creative prompt is understandable without any hidden app context.

Unit tests should validate structured JSON parsing, default/fallback fields, invalid enum rejection, capability mapping, no-auto-generation actions and project context isolation.

## 9. AI Director privacy / spend control
Chat requests may cost tokens. Put per-provider cost info and local daily budget in Settings. Sending a message is a distinct action and user should know paid provider usage may apply. No model may silently attach faces or media from projects other than the current one.
