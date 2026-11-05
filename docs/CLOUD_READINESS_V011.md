# MVG v0.11 — Cloud Readiness (Read-only)

Updated: 2026-11-06.

## Goal
Help the owner diagnose common Google OAuth and Cloud setup issues BEFORE explicitly approving paid Veo generation.

**This check only uses metadata GET requests.** It does not submit a video generation request, retrieve video output, enable any API, change IAM permissions or modify Cloud Billing. Google Cloud may separately apply quotas or policies to metadata requests.

## In-app steps
1. Settings -> Google Login -> Continue with Google. Register the installed APK SHA-1 for Android package com.parmtech.mvg in Cloud OAuth clients.
2. Set the correct Google Cloud Project ID and currently supported video region us-central1.
3. Choose Check Cloud Project & Vertex API (read-only).
4. Inspect two independently classified results:
   - Cloud Project: ACTIVE and accessible using resourcemanager.projects.get.
   - Vertex AI API: aiplatform.googleapis.com reports ENABLED under Service Usage, using serviceusage.services.get.
5. Fix BLOCKED issues in Cloud Console. UNKNOWN means the app cannot determine service state (for example 403, network failure or inaccessible metadata). UNKNOWN never means definitely disabled.

## Exactly which requests occur
- GET https://cloudresourcemanager.googleapis.com/v1/projects/{projectId}
- GET https://serviceusage.googleapis.com/v1/projects/{projectNumber}/services/aiplatform.googleapis.com

The projectNumber comes from the first successful response. Both are authorized with the user's short-lived Google Cloud OAuth access token, not an API key. Hosts are hardcoded HTTPS, redirects disabled, 12-second timeouts, response body limited to 8 KiB and raw error responses are never displayed.

Google official documentation:
- https://docs.cloud.google.com/resource-manager/reference/rest/v1/projects/get
- https://docs.cloud.google.com/service-usage/docs/reference/rest/v1/services/get

## What PASS/PASS does not prove
PASS/PASS indicates only project visibility and API enablement. It does NOT prove eligibility for the Google Cloud $300 Welcome Credit, actual Veo model permission or entitlement, regional quota, billing attachment, a successful long-running prediction, phone codec support, or Google OAuth restrictions on this exact APK.

The check never sends a POST to predictLongRunning, never resubmits a paid Veo job and never reads a billing balance. Google OAuth expires; when needed, reauthorize with Continue with Google. An in-flight check result is discarded if the owner changes the Cloud project or authorization.

## Tests and limitations
Mocked JVM tests verify exact GET endpoints, success, disabled API, 403 -> UNKNOWN, expired Google authorization -> no call, API Key mode -> no call, invalid project IDs and malformed identity responses. Android emulator regression tests remain responsible for backup/restore and local media save operations.

CI and emulator testing cannot test the owner's Google account, Cloud trial credits, real Veo generation, 4K decoding or a physical phone. Stable signing still requires owner-generated private keystore secrets in GitHub; never commit or paste these in public source/chat.
