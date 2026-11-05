# CHANGELOG

## [0.2.0] — 2026-11-05
### Added
- Android WorkManager 2.12.0 job reconciliation for already-submitted Veo operations with network constraints, retry/backoff, and output download recovery.
- Foreground UI sync of worker-updated clip states without reissuing paid video requests.
- Unit tests for 4K duration constraints, Lite/Fast resolution gating, and public-list-price calculations.
### Fixed
- Lost updates between background job and UI through per-record storage writes and clip refresh.
- Gradle and CI build with tests; GitHub Actions artifact MVG-v0.2.0-debug-apk verified by SHA-256.
### Known limitations
- Android device install, live Vertex generation and 4K output untested.
- Owner must still supply short-lived OAuth bearer token manually; background retry stops for invalid/expired credentials.
- No timeline MP4 render, image generation or multi-reference native avatar support yet.
- CI debug signing is not stable for guaranteed in-place upgrades; do not uninstall an existing app that contains valuable projects.

## [0.0.0-blueprint] — 2026-11-05
### Added
- Private MVG product specification and permanent AI engineering contract.
- Cinematic Android UX design tokens, screen and interaction spec.
- Kotlin/Jetpack Compose offline-first architecture with real provider adapters and durable jobs.
- Google Vertex AI Veo 3.1 / Fast / Lite model compatibility and estimated-cost design.
- OAuth first-run setup and credential/privacy rules; API-key compatibility caveats.
- Multi-avatar photo reference packs and AI Director floating chat specification.
- Scene timeline, clip ordering, preview and MP4 export plan.
- Phased PR roadmap, release gates, QA scenarios and vector brand concept.
### Not shipped
- No Gradle source, genuine provider credentials, generated video, APK, or production deployment yet.
