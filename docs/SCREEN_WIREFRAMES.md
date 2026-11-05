# MVG — SCREEN WIREFRAMES AND COMPONENT BEHAVIOR v1.0
These low-fidelity layout specs complement the UX Design System. Adapt to Compose responsive layouts; do not build literal ASCII art.

## A. Projects home (compact phone)
[Top safe area]
MVG logo        "Your Studio"               [Settings]
Search projects [________________________________]
[ + NEW PROJECT ]     [Sort: Recent v]   [Grid/List]
[Project thumbnail 16:9]        [Project thumbnail 16:9]
Title • 8 scenes • Edited today Title • 2 scenes
[Active generating pill if needed]
[Bottom bar] Projects | Library | Settings

**Interaction contract:** + opens sheet with name and ratio; project tap opens workspace; card menu contains Rename/Archive/Duplicate/Delete. Empty state button remains prominent.

## B. Workspace — Generate
[Back] Project title       [Queued 1]       [more]
[Generate] [AI Director] [Scenes] [Assets]
[ Model: Veo 3.1 Fast        v ]
[ Task: Text-to-video       v ]
[ Prompt editor multiline                       ]
[ Refine with AI ] [History]
[ References: + Photo | + Avatar | + Frames   ]
[ Duration chips: 4s   6s   (8s) ]
[ Ratio: (16:9) 9:16  ]  [Resolution: 720p v]
[ Audio: On/Off if supported ] [Outputs: 1 v]
[ Estimated cost: USD X.XX  • rate checked at time ]
[ GENERATE VIDEO ]   [Why disabled? when invalid]
[ Scrollable active jobs / generated results below ]

**Behavior:** selecting a different model recomputes capability controls and resets invalid choices with explanation. Generate dock moves up with IME, not over editor. No response until confirm cost. Server progress independent from UI tab.

## C. Workspace — AI Director
[Back] Project title       [Job: Running]   [more]
[Generate] (AI Director) [Scenes] [Assets]
[AI model: Gemini ... v] [Thread v]
[Assistant] "What's your next scene?"
[User message bubble]
[Structured prompt card]
   [Preview] [Edit] [Use in Generator] [Generate still]
[Suggested actions: Shot list | Cinematic | Consistency]
[Attachment+] [Enter your idea...] [Send]
[Collapse to in-app bubble]

**Behavior:** when collapsed, a 56dp safe-area bubble remains inside app. It can be docked, hidden from workspace header, reopened without losing text or generation. The chat does not auto-generate media.

## D. Workspace — Scenes
[Project] [Generate] [Director] (Scenes) [Assets]
[Preview canvas (follows source ratio)]
[00:00 / total duration] [Play full sequence]
[Unsequenced clips strip — thumbnail + plus]
Timeline:
 [drag] 01 [thumb] Intro Bridge      00:08 [Edit]
 [drag] 02 [thumb] Close-up           00:06 [Edit]
 [drag] 03 [thumb] Walk away          00:08 [Edit]
[+ Add a clip]  [Export movie]
[Export sheet: ratio, resolution, frames, expected size, destination]

**Behavior:** reordering persists immediately to Room; deleting a Scene does not delete original video. If local download missing, show Download instead of black preview; explain export block.

## E. Settings — Avatar Studio
[Avatar Studio] [ + CREATE AVATAR ]
[Avatar A card] [Avatar B card] [Avatar C card]
Wizard:
 Nickname -> I have consent -> Gallery/Camera ->
 capture angle guidance -> Review photo grid ->
 Select cover + description -> Save.
Details:
 [Cover]
 [front] [left45] [right45] [profile] [up] [down]
 [Preferred references] [Projects using] [Delete Avatar]

**Behavior:** multiple packs; no global biometric identity inference. A model can accept fewer photos than pack stores; selection in Generator limited by capability.

## F. Settings — Connection
[Google Vertex AI  ● Connected / ● Needs setup]
Project ID [________________________]
Region [Supported locations v]
[ Sign in securely with Google ]
[ Check model access ]
[ Output Cloud Storage: optional / required per model ]
[ Cost limits ] [Cloud Billing Link]
[ Connect OpenAI chat (optional) ] [Anthropic/Claude (optional)]

**Behavior:** no universal "Paste any key, all models enabled" message. Mask sensitive fields; show exact missing permissions and connectivity results.

## G. Critical overlays
1. Billable request confirmation: selected provider/model, task, 8s, 1080p, audio, N clips, estimated USD with source/time, local budget remaining, explicit button Confirm & Generate.
2. Generation state sheet: model, operation state, last update, Retry Status Check, Dismiss (job continues), Cancel only if available.
3. Error panel: readable cause, what happened to project/job, what action is safe, Help/Details. Always show no-charge assumption only if proven.
4. Delete project/Avatar: confirm irreversible local assets cleanup; separately discuss remote objects; never silently delete cloud files.
5. Offline: small banner and disable only network actions; local project and timeline usable.

## H. Accessibility / responsiveness sign-off
Screen 360dp, 393dp, 600dp, 840dp, 1200dp; fontScale 1, 1.3, 2; rotated; IME; TalkBack. Every Generate and Confirm tap target >=48dp, proper semantics, stable draft state, no inaccessible bubble placement.
