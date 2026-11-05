# MVG — VERTEX VIDEO REST TRANSPORT CONTRACT v1.0
Research snapshot 2026-11-05. This is an implementation guide, NOT proof a paid model has been tested. Always compare against current official API reference before coding.

## 1. Base request — one text-to-video job
The documented Veo long-running inference pattern (example endpoint for us-central1, project ID and model must be replaced, auth must be valid):

**HTTP POST**  
https://us-central1-aiplatform.googleapis.com/v1/projects/PROJECT_ID/locations/us-central1/publishers/google/models/veo-3.1-generate-001:predictLongRunning

**Headers:** Authorization: Bearer ACCESS_TOKEN; Content-Type: application/json; charset=utf-8.

Illustrative request JSON:
{
  "instances": [{
    "prompt": "A traveler crosses a quiet misty bridge at dawn. Wide cinematic tracking shot; natural motion."
  }],
  "parameters": {
    "aspectRatio": "16:9",
    "durationSeconds": 8,
    "sampleCount": 1,
    "storageUri": "gs://YOUR_PRIVATE_BUCKET/mvg/YOUR_JOB_ID/"
  }
}

Only pass optional parameters which the chosen model/task explicitly accepts. A missing storageUri can allow inline output on some documented methods, but do not assume that is universal, or that a long video will be comfortable to handle inline on mobile. If Cloud Storage is chosen, validate location/bucket policy/credentials first.

Initial response commonly returns an operation resource name:
{
  "name": "projects/PROJECT_ID/locations/us-central1/publishers/google/models/veo-3.1-generate-001/operations/OPERATION_ID"
}
Save **full name** immediately and atomically to GenerationJob, never only the suffix.

## 2. Poll a known long-running operation
**HTTP POST**  
https://us-central1-aiplatform.googleapis.com/v1/projects/PROJECT_ID/locations/us-central1/publishers/google/models/veo-3.1-generate-001:fetchPredictOperation

Request JSON:
{ "operationName": "projects/PROJECT_ID/locations/us-central1/publishers/google/models/veo-3.1-generate-001/operations/OPERATION_ID" }

When response says done=false, keep polling with bounded backoff. When done=true, inspect provider error / safety metadata / response:
{
  "name": "projects/.../operations/...",
  "done": true,
  "response": {
    "videos": [{
      "gcsUri": "gs://YOUR_PRIVATE_BUCKET/mvg/YOUR_JOB_ID/sample_0.mp4",
      "mimeType": "video/mp4"
    }]
  }
}
An operation can be done with no accepted output or a safety filter; do not equate done=true with success. Provider API response schemas vary by version. Parse defensively.

Documentation:
- https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/video/generate-videos-from-text
- https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/video/generate-videos-from-references
- https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/video/extend-videos
- https://docs.cloud.google.com/gemini-enterprise-agent-platform/reference/rest

## 3. Task-specific payload builders
Do not use one giant nullable JSON object for all tasks. Use sealed request types with separate, tested builders:
- TextVideoRequest: prompt, supported settings.
- ImageVideoRequest: prompt, image with documented bytesBase64Encoded/mimeType or supported GCS URI.
- SubjectReferenceRequest: referenceImages[{image,referenceType:"asset"}], model-specific count and optional fixed 8s restriction.
- FirstLastFrameRequest: model-specific image entry/last frame fields as official docs require.
- ExtendVideoRequest: prompt, video.gcsUri/mimeType when supported.
- UnsupportedTask: blocked locally with reason, not transmitted.
Attach model/test/version-specific schema fixture files to unit tests once implementation begins.

## 4. Android network boundary
- Retrofit/OkHttp, typed serializers, HTTPS and timeouts.
- OAuth token fetched only just before request; no token persisted in HTTP request journal.
- Payload input photos compressed/downsampled and validated; original files remain local.
- API transport returns sealed ResultSuccess/ResultFailure. Never throw raw provider HTML/error bodies to UI.
- Operation polling updates Room, observable through Flow to all tabs.
- GCS output download uses permitted authenticated method or user-scoped signed URL; validate media type/size, hash, safe path and atomic temp completion.
- Cloud Storage 'gs://' is not an ordinary public HTTPS download URL.
- **No universal idempotency promise:** a local UUID prevents duplicate UI submits, but if provider submit response is lost, provider may lack a lookup by client UUID. In that case state must be NeedsManualResolution and user warned before resubmit. Do not claim a guaranteed provider idempotency key exists.
- Never send generation body to a random public proxy or paste arbitrary service-account JSON into a mobile app.

## 5. Clock, lifecycle and errors
- Operation might be queued for minutes; no fixed progress percentage when API returns none.
- Record startedAt, lastPollAt, serverName, provider error category, output list and downloaded media URIs.
- If the authenticated account changes, pending operation remains associated with the old account; prompt to reconnect to that account.
- Retry only known-safe GET/poll/download operations automatically. Paid submit retry is a separate user-controlled request after reconciliation.

## 6. Integration test fixtures
Build test payload/response JSON for:
- 200 accepted submission and known operation.
- 400 unsupported resolution/reference.
- 401 expired access token; 403 model IAM.
- 429 quota + Retry-After.
- done=false after poll.
- done=true with valid GCS URI.
- done=true with safety-blocked/no video.
- transient 5xx.
- response lost after submit and no operation ID (manual resolution).
- output exists remotely but local download fails.
- wrong project/bucket permissions.
CI mocks all cases; production paid smoke only on user's approved test credentials and cost cap.
