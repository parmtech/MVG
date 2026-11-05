# CHANGELOG

## [0.4.0] — 2026-11-05
### Added
- In-app readout and copy action for the **installed** APK's public SHA-1, plus a link to Google OAuth client setup.
- Direct link to Cloud Billing for $300 Welcome Credit remaining/expiry; no claim that app sees billing balance.
- Expiration/freshness handling for short-lived Google OAuth token, reauthorization prompt when stale.
- Optional stable CI debug signing with user-controlled GitHub Secrets and published APK signing certificate report.
- [Detailed Cloud Google Login/$300 billing setup](GOOGLE_LOGIN_AND_300_CREDIT_SETUP.md).
### Build
- Successful unit tests, debug APK and apksigner verification: https://github.com/parmtech/MVG/actions/runs/41995652362
- v0.4 APK SHA-256 verified; Android OAuth SHA-1 for **this build only**: A7:19:BE:35:58:DD:94:8C:43:B1:BF:38:2B:EF:1D:17:E4:1C:0C:41.
### Limits
- Real device login and user's Google Cloud project/Veo $300 eligibility not yet tested.
- Debug sign may vary across runner builds until user configures stable signing.

## [0.3.0] — 2026-11-05
### Added
- Two authentication options in Android Settings: native **Continue with Google** and encrypted **API Key**.
- Google Identity Services cloud-platform permission flow; account returns on-device encrypted short-lived token.
- API key stored in a separate Android Keystore-backed vault and routed via x-goog-api-key for compatible Gemini AI Director calls.
- Model endpoint guard prevents using API Key for Veo predictLongRunning requests.
- AuthModeTest unit tests for both paths, credentials, and video restriction.
- Android APK and unit tests built successfully: https://github.com/parmtech/MVG/actions/runs/41990960893
### Removed
- Manual OAuth token paste field in Settings.
### Limitations
- Google Android OAuth client (package + SHA-1) and actual live device consent still need setup and testing.
- API Key mode is NOT equivalent to Veo video generation.
- Existing timeline MP4 export and other advanced features remain unimplemented.

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
