# MVG Google Login and $300 Google Cloud Welcome Credit

Updated 2026-11-05. For MVG **v0.4.0** Android debug builds.

## What the $300 credit actually means
Google Cloud's standard Free Trial generally supplies $300 USD of Welcome Credit for 90 days, attached to a **Cloud Billing Account** and eligible services/projects. It is not attached to a phone login or to an Android API key. An eligible Vertex AI request on a project linked to that billing account can consume credit, subject to specific trial limits, project API and quota availability.

**Do not assume Veo is enabled solely because a $300 banner appears**. Free Trial accounts have service restrictions and Veo 3.1 can require quota, entitlement or specific commercial access. Google documentation for the Veo model calls out fixed quota (not regular pay-as-you-go). Review Google's console before requesting a paid output. Do not manually upgrade to a paid account simply for this app without reviewing charges beyond credits.

- Cloud Free Trial terms: https://docs.cloud.google.com/free/docs/free-cloud-features
- Credits & balance: https://console.cloud.google.com/billing
- Veo 3.1 model and quotas: https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/veo/3-1-generate

## Setup on phone and Google Cloud (one-time per signing identity)

1. Open [Cloud Billing](https://console.cloud.google.com/billing) using the Google account that received $300. Check the eligible promotion, expiry, remaining credit and billing account ID yourself; **do not share your account ID, billing details or API credentials in chat**.
2. Select or create the Google Cloud **project** linked to that billing account. Copy its **Project ID** (not numeric Project Number); no need to share it publicly. Verify [Vertex AI API](https://console.cloud.google.com/apis/library/aiplatform.googleapis.com) is enabled in this project. Veo 3.1's documented deployment region is **us-central1**; requests process data in that region, not necessarily in Australia.
3. In [Google Auth Platform](https://console.cloud.google.com/auth/overview), configure the consent screen for **MVG**, and if still in Testing add your own Google account as a **Test user**. Cloud access scopes can be sensitive; testing with an owner-approved account does not guarantee public OAuth verification.
4. Install the exact MVG v0.4 APK on your Android phone. In **Settings → GOOGLE** copy:
   - Package name: `com.parmtech.mvg`
   - **Signing certificate SHA-1** using the new **Copy SHA-1** button.
5. Go to [Auth Platform → Clients](https://console.cloud.google.com/auth/clients) → **Create client** → **Android**, enter the package and SHA-1 from the app. Avoid using an SHA-1 from a different APK. If your device signs updates differently, Google OAuth sign-in will reject the request.
6. Return to MVG, select **GOOGLE**, enter **your Google Cloud Project ID** and video region `us-central1`, tap **Save Cloud Settings**, and tap **Continue with Google**. Choose the same Google account with Cloud IAM permissions and approve consent. The app will keep only a short-lived access token in Android Keystore encryption; when expired, tap Continue with Google to renew.
7. Open **Check Setup**. This verifies configuration format, *not* billing/Veо entitlement or actual balance. The first Veo submit should be a small, owner-approved request; verify Google Cloud quota and spending alerts **before** pressing paid Generate. MVG displays a cost estimate and asks for separate confirmation per video.

### Important distinction: Account / project / billing / OAuth

- The *Google account* identifies who is authorized to call Cloud APIs.
- The *Cloud project* identifies which resources and quotas are used.
- The linked *billing account* determines whether eligible $300 credit applies.
- The Android *OAuth client* identifies and authorizes **this signed app**. It does not itself enable Veo quota or attach promotional credit.

## Stable signing for future debug APKs (optional but strongly recommended)

A GitHub-hosted runner normally generates an ephemeral debug signing keystore, potentially different for each build. MVG's workflow now supports a **user-owned signing keystore via GitHub Actions Secrets**. This prevents the SHA-1 changing between new builds, preserves ability to upgrade in place, and keeps signing secrets outside the public repository.

Create a dedicated MVG test signing key locally and keep an offline backup in a secure place. **Never commit or send the private keystore or passwords in chat.** In your repository Settings → Secrets and variables → Actions, add:

- `MVG_KEYSTORE_BASE64`: base64 of the binary JKS/PKCS12 keystore (single line).
- `MVG_KEYSTORE_PASSWORD`: keystore password.
- `MVG_KEY_ALIAS`: key alias.
- `MVG_KEY_PASSWORD`: key password.

On the next workflow run, GitHub temporarily decodes the keystore on its runner (file permission 600), uses the stable signing config for the **debug** APK, and deletes it when the runner is discarded. If secrets are absent, the workflow falls back to Android's usual debug key. No stable signing secrets have been configured by this code change.

Each APK artifact also includes `MVG_SIGNING_FINGERPRINTS.txt` produced by `apksigner verify --print-certs`. You can use its SHA-1 even before installing. **Only one stable certificate should be registered for regular updates.** Do not uninstall old MVG with valuable saved projects merely because an APK signing mismatch occurs.

## Safety / limits

- $300 credit is a Google Cloud billing promotion, **not a promise that every Veo model or 4K mode is enabled**.
- MVG doesn't read balance, expiry or billing account details automatically; it does not spend credits without an explicit video generation confirmation.
- Native Google AuthorizationClient receives a **short-lived user OAuth access token**; MVG conservatively treats it as expired after 45 minutes and asks the user to authorize again. A production-ready refresh-token backend is not implemented.
- Local Google Disconnect removes the locally stored token; it does not necessarily revoke your grant on Google's server.
- Android OAuth testing on a real configured device is still outstanding after compilation.
- Google doesn't require you to manually paste API keys or service-account JSON for this Google Login mode.
