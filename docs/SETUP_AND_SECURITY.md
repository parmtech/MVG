# MVG — CONNECTION WIZARD, VERTEX AUTH AND SECURITY CHECKLIST

## 1. Reality of the requested "paste Vertex API key and everything works"
Google Cloud Vertex AI does not guarantee a single unrestricted API key works for every media model and API method. Some endpoints require OAuth bearer tokens, IAM permissions, enabled API, eligible location, quota, billing and Cloud Storage output. Google API keys may work only on particular API modes; authorization keys associated with service accounts carry extra security risk. MVG must implement a **guided connection**, not promise universal key-only setup.

Confirmed sources:
- https://docs.cloud.google.com/docs/authentication/api-keys
- https://developers.google.com/identity/protocols/oauth2
- https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/video/generate-videos-from-text
- https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/video/generate-videos-from-references

## 2. Recommended Settings > Google Cloud connection flow
A. Provider choice: "Google Vertex AI" (primary).  
B. Cloud project: Project ID (not numeric project number unless API specifically asks), permitted region selector (e.g. us-central1 only if the target model supports).  
C. Authentication: "Sign in with Google" / Google OAuth (recommended for personal device), or a provider-documented API-key flow when truly supported. Do not auto-offer account-owner service-account JSON upload as default.  
D. Permissions: inform user to enable Vertex AI API, authorize IAM roles needed for prediction and optional GCS output; never request Owner/Editor broadly as a shortcut.  
E. Billing: show instructions to enable billing and set a separate Cloud Budget alert; account billing may not be programmatically readable with chosen scopes.  
F. Optional GCS bucket: use for model output only when required/selected; validate region/location, write/read access and lifecycle/deletion.  
G. Test Connection (real request that avoids billable generation when possible): user identity/token -> API reachability -> model access metadata -> Cloud Storage check if needed.  
H. Result: Verified / Partially verified / Needs billing verification / Missing permission / Invalid auth / Unsupported region.  
I. Optional: "Make a small billable test generation" with explicit cost review and user confirmation only after setup.

## 3. Android OAuth constraints
- Register proper Android OAuth client with package name and signing certificate SHA-1/SHA-256 as applicable, configure consent and test users.
- Use current Google-supported authorization code + PKCE / appropriate official Android identity library. Follow official mobile-specific flow; do not use deprecated loopback on Android or unsupported custom URI redirect.
- Don't hardcode refresh tokens, credentials, bearer access tokens or service-account private keys in repo/APK.
- Keep tokens encrypted behind Keystore-backed secret storage. Handle access expiry and refresh gracefully; disconnect revokes/clears local credentials when supported.
- Official client ID is not secret. Sensitive project data and filenames should not go in unencrypted logs.

## 4. API-key alternative — only when verified
When a specific Google endpoint supports key auth:
- Accept API key via masked sensitive field.
- Store in Keystore-protected encrypted prefs, never DataStore plaintext.
- Document project/Android app/API restrictions; verify method works.
- Require explicit scope/disclaimer: key-based auth cannot inherit IAM authorization if not linked to a principal in a permitted manner.
- For endpoints demanding OAuth/IAM, tell owner "Google sign-in required for this model" rather than trying insecure hacks.
- **Never** write a server's service-account JSON to Android assets or commits.
- If a secure server-side proxy is ever introduced, protect with auth, minimum permissions, server-side secrets, audit and rate limits; require owner's explicit opt-in.

## 5. Permissions and device privacy
- Android INTERNET; ACCESS_NETWORK_STATE.
- Camera only upon guided avatar capture.
- Photo Picker for uploads, SAF for file import/export; no blanket READ_MEDIA_IMAGES/VIDEO permission unless a proven feature requires it.
- POST_NOTIFICATIONS runtime permission when appropriate.
- USE_BIOMETRIC where supported.
- No accessibility service, contacts, SMS, broad storage, or SYSTEM_ALERT_WINDOW.
- Material safety: block screenshots specifically in credential and private avatar capture screens when requested, optional user override for normal app screenshots.
- No tracking/analytics by default. Keep exported files under user-owned MediaStore, and source photos app-private.

## 6. Cloud usage / identity management
- IAM should authorize only model inference, required output object storage and minimal model metadata. Don't use broad project Owner role for app.
- Avoid public GCS buckets and indefinitely readable object URLs. Prefer authorized downloads, short-lived signed links if appropriate, bucket cleanup policies and per-job prefixes.
- Verify allowed content, likeness consent and media retention policies. Cloud provider may retain processing data according to service terms; show user disclaimer.
- Display actual Cloud project and region in settings; show remote data destination.
- Allow Delete local data and remove cached cloud object links; remote object deletion is separate and requires explicit confirmation/permission.
- Cloud API rate limits/quotas and project billing enforced by cloud; app's own budgets are advisory enforcement on app requests, not account-global hard caps.

## 7. Security acceptance checklist
[ ] No real API key/token/private key in any tracked file.  
[ ] No secret in API errors, URLs, analytics, logs, crash reports, WorkManager inputs or backups.  
[ ] Encrypted token store and connection reset tested after reboot.  
[ ] HTTPS strict; no trust-all certificates.  
[ ] Paid model requests require deliberate cost confirmation.  
[ ] Repeated Generate taps cannot duplicate paid jobs.  
[ ] Retry after ambiguous timeout **reconciles**, not blindly resubmits.  
[ ] Avatar capture has consent, storage notice and deletion.  
[ ] Project backup export strips credentials and private settings.  
[ ] Offline mode still shows local projects and drafts.  
[ ] Google consent verified on release signing key and debug signing key separately.  
[ ] App can recover token expiry, 401/403 and missing billing without erasing work.  
[ ] Sensitive files not auto-backed-up to public destinations.  
[ ] Production release is signed using keystore kept outside repo.
