# MVG v0.10 — Individual scene MP4 export to Android Movies

## User workflow
1. Generate a scene with the normal confirmation screen. Confirmed Veo generation may be billed separately by Google Cloud.
2. Wait until that scene is Complete with a locally downloaded MP4 (if necessary, select Check / Download on the existing provider operation).
3. In **Generate**, **Scenes**, **Assets**, or **Library**, tap **Play** for preview, **Share** for an Android share chooser, or **Save scene to Movies** for a persistent Gallery copy.
4. The Save action is offered on Android 10+ (API 29+) through MediaStore and writes into **Movies/MVG**. Android 8–9 owners can use the Share action instead.
5. The saved copy remains available outside the app even if MVG's private project record is later removed. Save/share actions never call the Veo generation endpoint and do not consume the user's $300 Google Cloud promotional credit.
6. The merged scene sequence export from v0.5 continues to work independently.

## Design and security
- Only clips with status Complete and an existing local MP4 can be saved or shared.
- Before copying to Gallery or sharing, check the media file has a plausible MP4 file signature, not just a .mp4 filename or text/HTML error response.
- Use Android MediaStore on API 29+, including IS_PENDING for atomic visibility. On error, delete the incomplete MediaStore entry. Never request broad device-storage access.
- Use the app FileProvider to grant read-only, short-lived access for user-selected share recipients, not public file paths.
- The clip remains app-private until the user taps Save or Share. Play uses an Android media-view intent.
- Saving an existing complete video makes no additional Google Cloud generation request, hence no extra Veo generation charge. Copying uses local device storage and phone battery.

## Explicit limits
- A successfully downloaded file is only structurally checked at its MP4 header; on-device playback depends on compatible codecs, available space and player. Full 4K compatibility has not been verified on the user's physical phone.
- A 4K Veo generation output is not upscaled when copied; the source bytes are preserved exactly.
- Android emulator tests verify MediaStore save/read-back and rejection of invalid MP4 files; the test media is a synthetic structural MP4 and does not verify decoding of a real Veo output.
- Installing an APK with a different signing certificate than your existing one may fail without uninstall/reinstall. Make and verify a backup of important local data BEFORE uninstalling.
- Stable private signing is not configured in this public repository. See [Stable Signing Setup](STABLE_SIGNING_SETUP.md).
- Google OAuth test and actual Veo generation are still owner-account tests, not implied by app compilation or media-copy tests.
