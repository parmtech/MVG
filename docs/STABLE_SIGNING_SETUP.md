# MVG — Stable Android APK signing (owner setup)

This setup requires owner action because the signing **private key must never appear in a public GitHub commit or chat**.

## Why
GitHub-hosted debug APK builds currently use a different ephemeral debug signing certificate on separate runners. Android may refuse an in-place update, and Google Cloud Android OAuth clients require a registered package name and **actual signer SHA-1**.

Stable signing is already supported in app/build.gradle.kts and .github/workflows/android-apk.yml. **It is not active until all four GitHub secrets are configured.**

## Create a permanent key on a trusted computer
With JDK keytool installed, open a terminal **on a device you control**:

~~~sh
keytool -genkeypair -v -keystore mvg-stable.jks -storetype PKCS12 \
  -alias mvg -keyalg RSA -keysize 3072 -validity 10000
~~~

Choose a strong secret store password and keep the file and password offline in your password manager/encrypted storage. Do not upload this file to a public repo or share it with assistants.

## Add secrets to your GitHub repository
Open https://github.com/parmtech/MVG/settings/secrets/actions → New repository secret.

| Secret name | Value |
|---|---|
| MVG_KEYSTORE_BASE64 | Single-line Base64 representation of the binary mvg-stable.jks file |
| MVG_KEYSTORE_PASSWORD | The keystore password |
| MVG_KEY_ALIAS | mvg |
| MVG_KEY_PASSWORD | The key password (often the same as the store password for PKCS12) |

Create a one-line Base64 keystore representation locally:

Linux: 
~~~sh
base64 -w0 mvg-stable.jks
~~~

macOS:
~~~sh
base64 < mvg-stable.jks | tr -d '\n'
~~~

Windows PowerShell:
~~~powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path ".\mvg-stable.jks")))
~~~

**Do not paste that Base64 data into a chat**. Paste it only into the GitHub repository secret editor on the official GitHub website while signed in to your own account.

## Build, verify and enroll OAuth
1. Run the MVG Android APK workflow via GitHub Actions → MVG Android APK → Run workflow, or push a source code change.
2. The "Prepare optional stable signing keystore" step should state "Stable signing configured from GitHub Secrets" (no secret values shown).
3. Under Artifacts, download the APK and MVG_SIGNING_FINGERPRINTS.txt.
4. Verify APK SHA-256 against the workflow's printed digest. The public signer SHA-1 should be identical for every new build with the same keystore.
5. Google Cloud → Auth Platform → Clients → Android OAuth client, register package **com.parmtech.mvg** and the correct public SHA-1. This is separate from Cloud project billing.
6. Install a build signed with this keystore. All subsequent APK updates should use **the same key**. A v0.6/v0.7 APK signed with an earlier ephemeral debug key cannot be upgraded in place to the stable signed APK. Preserve your data first.

## Legacy device safety
- If your current MVG installation already contains important project media, don't uninstall until you've made and verified a portable backup with the installed version that can export one.
- v0.6 supports legacy plaintext ZIP backups; v0.7 supports encrypted .mvgbackup export as well as legacy ZIP import.
- v0.5 has **no** portable project backup. Never claim its data is safe to uninstall.
- Even with stable signing, keep backups before substantial updates.
- GitHub Actions secrets are not present in pull requests from untrusted forks; do not expose signing to untrusted workflows.
- Treat the keystore as a long-lived identity credential: losing it can permanently break upgrades and requires re-registering an Android OAuth client with a new SHA-1.

## Future production release
A debug APK is a private testing artifact, not a Google Play release. When ready for public distribution, create a dedicated *release* signing and Play App Signing process. Do not reuse debug signing as a default production distribution key.
