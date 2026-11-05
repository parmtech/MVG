# MVG v0.5 — Local Scene-to-Movie MP4 Export

**Build status:** GitHub CI compiled the source and unit tests on 2026-11-05. Android physical-device testing remains pending.

## User workflow
1. Generate two or more videos using authorized Google Login and Vertex AI, then finish/download those videos on the phone.
2. Open the project → Scenes tab. Use up/down controls to arrange storyboard order.
3. Tap Export N scenes as MP4. Only locally downloaded, Complete scenes are included; pending and failed scenes are excluded.
4. Watch device-side export progress. Cancel stops the active Media3 Transformer job and discards partial output.
5. After completion: Preview, Save to Movies, Share, or Delete export. A preview/export copy remains in private app files until deleted. Save to Movies writes an additional copy to the system Movies/MVG collection.

## Technical scope
- AndroidX Media3 stable 1.11.1 Transformer Composition / EditedMediaItemSequence APIs.
- The operation runs ON THE DEVICE; it does not submit a new paid Veo generation request.
- Project records store exported movie path, scene count and date.
- Export files live in app-private media/exports storage, shared via the existing FileProvider.
- Save to Movies uses MediaStore on Android 10+; on Android 8-9 use Share movie to save the file.

## Explicit limitations
- Export remains Activity-scoped: rotating the device, navigating away through Android process shutdown, or force-closing may interrupt an in-progress encode.
- No transitions, crossfades, trimming, subtitles, soundtrack, audio mix, color-grading or 4K upscaling.
- 4K/HDR and mixed source codecs require hardware codec support and free storage. Actual output dimensions, bitrate and quality can differ from originals. Not all devices can export 4K.
- Finished clips are included only when a local MP4 exists; incomplete clips are intentionally skipped.
- The app does not yet provide complete backup/export of all project media. A debug APK signed with a different certificate from the previous version might require uninstall/reinstall; DO NOT uninstall a version containing important data without separately preserving it.
- Media3 output and real Google account generation have NOT been verified on a physical phone in this session.

## Verified build
- Success: https://github.com/parmtech/MVG/actions/runs/41998640289
- Built commit: 0d71d0dbd2f8f629cba8f661922f078196c28027
- Artifact: MVG-v0.5.0-debug-apk, contains app-debug.apk and MVG_SIGNING_FINGERPRINTS.txt.
- APK size: 23,003,763 bytes
- APK SHA-256: e43b2c1d9d87184741b8f414d3f05854d97819a6fd19ef9542fc40f106605020 (verified against GitHub build log)
- This specific APK's signing SHA-1: 6A:EB:D8:F0:A4:B0:77:C5:D4:92:F7:97:EB:BD:5C:97:ED:B6:38:CB. The debug certificate is ephemeral. Register this SHA-1 as an Android OAuth client certificate only if you are installing this exact APK, and prefer stable signing for ongoing versions.

## Official docs
- https://developer.android.com/media/media3/transformer/composition
- https://developer.android.com/media/media3/transformer/getting-started
- https://developer.android.com/jetpack/androidx/releases/media3
