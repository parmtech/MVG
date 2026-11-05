# MVG — PREMIUM ANDROID DESIGN / INTERACTION SYSTEM v1.0

## 1. Design strategy
Personality: cinematic, confident, restrained, modern, fast. Reference the **clarity** of Google Flow and modern film-editing tools; build a distinct MVG identity with no copying of product branding. UI should be understandable to someone using their first generative video tool.

Brand identity: **MVG**, "Your AI Film Studio". App icon: stylized iridescent film-strip M and a triangular Play glyph, azure/violet gradation on a dark cinematic surface. See assets/branding/mvg-logo.svg.
- Dark first; opt-in light theme. Avoid a black void: use layered deep-navy neutrals and subtle surface gradients.
- Palette tokens: background #080B14; surface #121827; elevated #1B2335; stroke #2C3549; primary #63DDF4; accent #A67BFF; text #F4F7FC; text-muted #A8B3C7; success #47CFAB; caution #FFCF70; destructive #FF6D83.
- Typeface: platform Inter-compatible/system sans; numbers tabular; typography scale 30/24/20/16/14/12sp; minimum body 14sp. Sizes respond to fontScale.
- Radius: 24dp project cards, 20dp sheets, 16dp settings, 12dp controls; 8dp chips. 8dp spacing rhythm: 4/8/12/16/24/32.
- Animations: 120–220ms for controls, 250–320ms navigation, reduced-motion fully respected; no huge blur/shader on low-end devices.
- Video thumbnails 16:9 landscape or 9:16 portrait consistent with source; artwork placeholders are geometric, not random stock photos.

## 2. Navigation architecture
App-level bottom bar (when outside project):
**Projects** | **Library** | **Settings**.
Inside project: top project name & exit, horizontally scrollable **Generate** | **AI Director** | **Scenes** | **Assets**, compact operation-status icon. On phone use a segmented tabs strip or adaptive bottom workspace rail; on tablet two-pane workspace. Avoid more than one giant fixed bottom navigation.

## 3. Screen specifications
### Splash / first-run
- 1–2s maximum if cold initialization is necessary, then Projects quickly with connection state.
- Page 1 "Direct your imagination"; page 2 "Your models, your projects"; page 3 "Your private studio". Skip always visible.
- Connection flow = Google provider card -> sign-in -> Cloud Project ID and region -> verify -> pricing safety -> Done. No secret ever printed in confirmation.

### Projects home
- Hero header "Your Studio" and last project shortcut.
- Search/filter bar, grid/list toggle, Create button as main visual CTA.
- Cards: cover image, project title, number of clips, latest edit date, aspect badge, active job spinner if any.
- Empty state shows Create your first project, not a wall of technical text.
- Press-and-hold / menu: Rename, Duplicate, Archive, Export project metadata, Delete.
- Grid adaptive 1 column compact, 2 columns medium, 3+ tablet; no fixed-width clipping.

### Project Generator
Vertical layout:
1. Heading "Create scene", project badge and selected video model card.
2. Provider/model picker modal with clearly separated Quality / Fast / Economy and *Verified* availability, last-checked time; not fake "Pro" branding.
3. Task type chips ONLY supported by selected model. Disabled task explains why.
4. Large multiline prompt composer with character counter, magic **Refine with AI**, undo, Prompt History.
5. Reference strip: + Add photo, + Character, + Start/End frame, asset picker; model max note; mini preview and remove.
6. Model-driven controls in collapsible "Scene settings": video length (discrete supported values), aspect (16:9/9:16), resolution (720p/1080p/4K as allowed), audio, number of videos, seed and advanced settings. No unsupported sliders.
7. Pinned bottom generate dock above system navigation: label "Estimated [currency]..." with info popover and primary Generate. On incomplete form, disabled with specific instruction.
8. Upon submit: operation card with live status, percent only when provider reports meaningful numeric progress, otherwise indeterminate; show thumbnail placeholder, elapsed clock, cancel/reconcile actions.
9. Result card: autoplay muted preview only when visible and user permits; Use as Scene, Variations, Download, Details, Share.

### AI Director
- Conversation header: "AI Director", provider/model switch, optional persona + prompt style.
- Chat supports user text, attached stills, named assets (explicit access), assistant answer components with full text and **Prompt Draft** action card.
- Commands: "Write cinematic prompt", "Shot list", "Character consistency", "Image idea", "Improve realism", "Fix continuity".
- Output prompt cards: Copy, Edit, Apply to Generator, Generate reference image (requires supported image provider).
- Floating chat: collapsible bubble 56dp, dock to nearest safe inside-app edge, swipe hide/restore from project header; not Android SYSTEM_ALERT_WINDOW. Bottom sheet expanded 60–92% screen.
- System remains online only as network/provider permits. No claim of permanent active socket.
- During video generation, chat updates do not remount the generator or clear drafts.

### Scenes / Timeline
- Unsequenced clip library as horizontal strip with duration and model badges.
- Timeline stacked reorderable shot cards; numbered 01,02,03 with drag handle, trim bar, thumbnail, transition selector.
- Tapping shot enters detail modal with chosen source asset, prompt info and trim markers.
- Inline preview header can play the entire sequence without changing original clips.
- Primary CTA: Export movie.
- Empty state: "Your first scene will appear here" + Add from generated clips.

### Assets
- Filter segmented: All / Generated clips / Photos / Avatars / Exports.
- Local/remote icon, download retry, file size/duration; deduplicate by content checksum while retaining logical associations.
- Never expose raw GCS URL to unrelated apps by default.

### Settings > Avatar Studio
- Avatar list grid showing cover/nickname/photo count and last use; multiple unlimited locally (storage warnings).
- Create flow: Give nickname -> Consent -> Guided captures or Import -> Review angles -> Save reference pack -> Test in supported model.
- Capture on a stable camera screen with face outline and "turn slightly left / right / up / down", optional and skippable.
- Never claim biometric scan or trained personal AI model without implemented training support.
- Avatar details: Edit appearance notes, pick best 1–3 refs, set hero photo, view linked projects, export/delete.
- Separate privacy warnings when photograph includes someone other than user.

### Settings > Accounts & API
- Cards: Google Vertex (primary), OpenAI chat (optional), Anthropic/Claude chat (optional, direct or via eligible Vertex partner availability), and future providers.
- Each card: Connection state, endpoint/project, available model count, region, last test, Connect, Re-test, Disconnect.
- Sensitive values masked with reveal-on-long-press; paste into protected field only; not displayed in screenshots by default.
- Explain a single API key does NOT universally authorize Google Cloud Vertex video generation.
- Separate Cloud Billing link and local budget cap management.

### Settings > Costs
- Estimated spend today/month, local caps, caution thresholds, preferred display currency, model rate validity.
- "Actual provider bill may differ" warning and external billing link.
- Price list includes unit and timestamp for each model; show N/A instead of wrong rate.

## 4. Responsive/adaptive layout rules
- Width <600dp: 1-column composer, sticky button dock, vertical bottom sheets and horizontally scrollable model controls.
- 600–840dp: optional two-column project view, image tray beside prompt.
- >840dp: Generator left 60%, right preview/job history 40%; AI Director as right resizable pane optional.
- Portrait and landscape supported. Edge-to-edge with safe insets, IME avoiding controls; back/gesture standard.
- Display cutouts, font scale 1.0–2.0, keyboard, RTL text in attached prompts must not break layout.

## 5. Accessibility and microcopy
- Every icon has content description and visible tooltip/info on long press.
- Model-capability disabled labels say e.g. "Reference images unavailable on Veo Lite".
- Estimated cost: "[USD] estimate based on published unit rate; actual billed cost may differ". Do not mislabel as account "credits" unless actual credit balance API exists.
- Generation: "Submitted — preparing video"; on waiting "Still generating — you can use AI Director"; on network lost "Trying to reconnect — your request is saved".
- No spinners shown indefinitely without last update, cancel/retry/diagnostic link.
- Text length 2–3 words for primary buttons; limit animated gradients during active editing.

## 6. Motion and feedback
- Scene cards scale 0.98 on press with subtle haptic, no page jumps.
- Tab transitions crossfade/small slide, preserve ViewModel state.
- Prompt-to-video completion uses one subtle ring animation; respect reduced-motion.
- Lazy image loading, cached thumbnails, gracefully downgrade motion on low power.

## 7. Design testing sign-off
Screenshots required: compact Android 5.8" device, large Android 6.8", 11" tablet; dark/light; keyboard open; no connection; no projects; 200 scenes; active job + floating chat; 200% font. Compare to design tokens and user journeys, fix clipping before release.
