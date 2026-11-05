# MVG v0.12 — Easy Google Login on Android

## What the owner sees

The MVG home screen now offers **Continue with Google** directly above the project list, so users don't have to hunt through Settings. Settings presents a simple ordered sequence:

1. **Continue with Google** — Tap once; Android Google Play services shows any required account chooser and consent. Loading state prevents repeated taps.
2. **Find my Cloud projects** — After authorized login, MVG makes read-only Cloud Resource Manager projects.list requests and shows accessible ACTIVE projects by name and ID. If exactly one project is visible and no project was already selected, MVG chooses it automatically. If multiple are listed, pick the correct project; a previous owner selection is never silently changed.
3. **Check Cloud Project & Vertex API (read-only)** — Confirms project metadata and Vertex AI service enablement. Does not generate Veo video, use credits, or change Cloud settings.

**Other connection methods**, manually entering the Google Cloud Project ID, and Google OAuth SHA-1 / console setup are now available in optional Advanced sections. Missing project permissions or network problems offer a manual Project ID fallback.

## Less repetitive authorization

Google AuthorizationClient.authorize can reuse an existing, previously granted Google OAuth scope. On later app starts, MVG attempts a **silent refresh only if the owner has already consented** and the local token is no longer fresh. If Google requires another account selection or consent, the silent attempt never opens an unexpected dialog; the owner can press Continue with Google. No refresh token or client secret is embedded in MVG.

Only short-lived Google cloud-platform access tokens are retained in Android local encrypted token storage. Removing Google connection clears MVG's prior-consent flag and local bearer token; project list is ephemeral (in memory) and is cleared on disconnect.

**Important:** This is not bypassing Google's mandatory Android OAuth application setup. The OAuth Android client must still be configured in the owner's Google Cloud project for package `com.parmtech.mvg` and the actual **installed APK's public signing SHA-1**. OAuth consent/testing user settings may also be required. An APK built with a different temporary debug signing key has a different SHA-1. Stable user-owned keystore signing is still a separate setup step.

## API request and security details

- The project picker only performs `GET https://cloudresourcemanager.googleapis.com/v1/projects?pageSize=100` with optional URL-encoded pagination token, using the consented Google OAuth access token. It never POSTs, creates projects, enables APIs, edits billing, grants IAM or generates videos.
- It ignores inactive projects and invalid IDs, limits results to 200 projects / 4 pages, restricts each response to 128 KiB, refuses cross-host redirects and imposes timeouts. It does not display raw Google error bodies or credentials.
- 401 prompts authorization renewal. 403 means this account may not have access to list its projects; manual entry remains available. A project that is not listed can still be valid if the user knows its Project ID.
- Project discovery cannot verify billing credits, Veo access, quotas or predicted pricing. A specific video generation is charged separately, after its own explicit confirmation screen.
- Newly discovered project names/IDs are only stored as ephemeral UI state. Only a single unambiguous, explicitly chosen or auto-selected project ID is persisted as normal app configuration, and it is not a secret.

## How to set up the Google Cloud OAuth client

1. On an owner-controlled browser, go to https://console.cloud.google.com/auth/clients (Google Auth Platform → Clients).
2. Select or create a Cloud project you control. Register an **Android OAuth client** with package `com.parmtech.mvg` and the SHA-1 from this exact installed MVG APK, available in Settings → Advanced → Google OAuth Setup → Copy SHA-1.
3. Configure the OAuth consent screen and add your own account to the test-user list if the OAuth application is in Testing mode.
4. Return to the phone, tap Continue with Google and choose the permitted Google account.
5. If the app shows no projects, select another authorized account or open Advanced Cloud Settings and enter the Project ID you own.
6. For future APK upgrades without changing SHA-1 each build, follow docs/STABLE_SIGNING_SETUP.md to configure the **owner-held private signing keystore** in GitHub encrypted Secrets. Do not paste any password or private key into ChatGPT or GitHub code.

Google developer references:
- https://developer.android.com/identity/authorization
- https://docs.cloud.google.com/resource-manager/reference/rest/v1/projects/list
- https://support.google.com/cloud/answer/15549257

## Testing boundaries

Automated JVM tests cover project listing, active projects, pagination, invalid and repeated tokens, ambiguous selection, permission failure and no-network fallback without contacting any private Google account. Existing Android emulator tests cover local backups, MediaStore and FileProvider. **Real Google account selection/OAuth registration, billing credit eligibility, and Veo model access must be validated on the owner's Android phone and Cloud Console**.
