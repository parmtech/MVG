# MVG v0.3 — API Key OR Google Login

**Status:** two-method authentication UI committed to app source; real account/device verification is still pending.

## Exactly two choices

1. **Continue with Google** — requests the Google Cloud cloud-platform OAuth scope using Google Identity Services on Android. After the native Google consent/account flow, the short-lived access token is kept on the phone, encrypted with Android Keystore. Use this mode for **Veo video generation** and Gemini AI Director. When token expires, tap Continue with Google again. There is no hardcoded secret, private key or manual OAuth token paste.
2. **API Key** — paste a Google Cloud API key in a masked field. It is stored only locally with Android Keystore AES/GCM and sent as the x-goog-api-key HTTP header for compatible Gemini generateContent requests in the AI Director. The current Veo video implementation uses predictLongRunning/fetchPredictOperation; Cloud's regular API key documentation does not list these as API-key-supported methods. MVG deliberately blocks paid video submission in API Key mode.

Cloud Project ID, region and optional GCS output path are **configuration**, not additional sign-in methods.

## Screens
Settings > Connect to Google AI:
- Connection method: GOOGLE / API_KEY
- GOOGLE: Continue with Google, connected status, Disconnect
- API_KEY: Paste API Key, Save API Key, encrypted-key status, Disconnect
- Cloud Project ID, video region (Veo typically us-central1), optional gs:// output folder, Save Cloud Settings
- Check Setup: validates format, not billing, IAM or quotas
- Video Generator: Google Login is required; AI Director uses either method where supported

## Google Login prerequisites
1. In the owner's Google Cloud project, enable the relevant Vertex/Agent Platform API, billing and video-model access.
2. Configure OAuth consent and test users where required.
3. Register an **Android OAuth client** with package **com.parmtech.mvg** and the APK's actual **signing certificate SHA-1**.
4. On phone, select GOOGLE > Continue with Google, approve authorization and choose the configured Cloud project.
5. The token expires. The app can request authorization again, but no persistent backend refresh-token flow has been implemented.
6. Debug APK signing keys on ephemeral CI machines can change across builds. Stable release signing is needed to make a repeatable OAuth configuration and upgrades.

## API Key prerequisites and limitations
1. Create a restricted key in Google Cloud for supported API methods, account and model availability.
2. Select API_KEY, paste it and tap Save API Key. Do not share it in ChatGPT or GitHub.
3. Configure Cloud Project ID and region, open AI Director and test.
4. Saving a key is not a verified API connection. The first generateContent call may still be denied based on key policy or quota.
5. Video generation continues to require Google Login. Gemini API Veo 3.1 Preview endpoints were shut down October 22, 2026; do not attempt an expired preview endpoint to bypass the limitation.

## Security
- Avoid hardcoding keys in APK source or committing them to a **public** GitHub repository.
- A key entered on the device is still extractable from a compromised device or instrumented app; restrict key access, set budgets and avoid giving broad service-account permissions.
- Google advises against using service-account-bound authorization keys in production client applications.
- Neither sign-in method silently submits a paid video. The Generate button still requires cost consent.
- If a video was submitted with Google Login and the mode changes to API Key, you may need to reconnect Google to poll/download the original job.

## Official references
- https://docs.cloud.google.com/gemini-enterprise-agent-platform/machine-learning/authentication
- https://docs.cloud.google.com/docs/authentication/api-keys-use
- https://docs.cloud.google.com/docs/authentication/api-keys-best-practices
- https://developers.google.com/android/reference/com/google/android/gms/auth/api/identity/AuthorizationClient
- https://developers.google.com/android/reference/com/google/android/gms/auth/api/identity/AuthorizationResult
- https://ai.google.dev/gemini-api/docs/video

## Release gate
- Unit tests and debug assemble must pass.
- Only two auth methods visible, no manual OAuth paste field.
- Separate, encrypted credential slots for Google and API Key.
- Proper Authorization Bearer vs x-goog-api-key header for each method.
- API Key cannot call paid Veo predictLongRunning.
- Google sign-in works on a real registered Android device with actual signing certificate (requires testing; not proven by compilation).
