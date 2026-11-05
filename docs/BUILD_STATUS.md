# MVG v0.1.0 — Android Build Status

Updated: 2026-11-05

## Actual state
Android project source is committed to the private repository. **No APK has successfully built. No device test has passed.** Never claim otherwise.

## Implemented in source (not yet compiled or smoke-tested)
- Android Gradle / Compose shell, cinematic project navigation, local projects/clips/chat/avatar records.
- Veo 3.1 / Fast / Lite text-to-video composer (4/6/8 sec, portrait/landscape, 720/1080p, audio) with single photo as first-frame option.
- Real Vertex long-running predict REST request, operation polling, cloud-output download, per-project results and playback.
- Gemini 2.5 Flash AI Director prompt assistance and Apply to Generator.
- Project scene ordering, private avatar photo import, encrypted manual short-lived OAuth bearer token Settings.
- Separate charge acknowledgment + confirm for billable video calls; **exact price estimate unavailable** in v0.1.

## Not shipped
Full Google Sign-in token auto-renewal; automatic WorkManager operations; dynamic live model discovery/pricing; multiple model-native avatar references; guided camera scan; image generation; OpenAI/Claude provider support; multi-clip timeline trim/MP4 export; final signed release; end-to-end or physical Android test.

## GitHub Actions blocker (evidence)
Workflow: https://github.com/parmtech/MVG/actions/runs/41984948386
Run #4 attempt 1 failed immediately. A second attempt was requested and also failed immediately. Both executions show no build steps or downloadable logs (BlobNotFound). **No compilation task began**. Similar behavior can happen with private repo GitHub Actions billing/budgets, runner permissions or provisioning problems; precise cause is not confirmed.
- Check repo Settings > Actions > General to confirm GitHub-hosted runners and workflows are allowed.
- Check account Settings > Billing / Budgets and alerts / Actions for an account payment issue, blocked spending cap or exhausted minutes (menu labels may vary).
- Read any run-page annotations for the exact reason.
- Do not switch this private repo to public.

## Download after successful build
1. Open https://github.com/parmtech/MVG/actions/workflows/android-apk.yml
2. Select a run showing success.
3. Under Artifacts, download MVG-v0.1.0-debug-apk.
4. Unzip and install app-debug.apk. This is debug-signed for testing, not production.
5. Keep Cloud credentials out of GitHub; token supplied in Settings is encrypted locally.

## Alternative local build
In Android Studio with JDK 17, Android SDK 36 and Gradle 8.13: clone private repo, run Gradle task :app:assembleDebug. Look for app/build/outputs/apk/debug/app-debug.apk only after confirmed build success.

## Australia-specific Vertex
Account location in Australia does not imply the Veo 3.1 model is served from Sydney. The documented Veo 3.1 region is us-central1. MVG v0.1 prompts the user for Cloud Project ID, region, optional GCS URI and a short-lived OAuth token. An API key alone may not authorize video endpoints. Video output processed in the US can have data residency implications.

References:
https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/veo/3-1-generate
https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/video/generate-videos-from-text
https://cloud.google.com/gemini-enterprise-agent-platform/generative-ai/pricing
https://docs.github.com/en/actions/how-tos/troubleshoot-workflows
