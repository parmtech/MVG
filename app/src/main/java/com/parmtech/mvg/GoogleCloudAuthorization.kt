package com.parmtech.mvg

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope

/**
 * User-consented Google Identity Services authorization for a native Android public client.
 *
 * This requests only a short-lived cloud-platform access token; there is no private key,
 * no web OAuth client secret and no refresh-token exchange in the APK.
 *
 * To use on a device, register an Android OAuth client for com.parmtech.mvg and the
 * **actual APK signing certificate SHA-1**, and configure OAuth consent/test users.
 * A GitHub-hosted debug signing certificate can change between builds.
 */
class GoogleCloudAuthorization(
    private val activity: ComponentActivity,
    private val onResult: (token: String?, message: String) -> Unit
) {
    private val client = Identity.getAuthorizationClient(activity)
    private val scope = "https://www.googleapis.com/auth/cloud-platform"

    // Must register before the hosting Activity reaches STARTED.
    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
            onResult(null, "Google authorization cancelled or denied.")
            return@registerForActivityResult
        }
        try {
            accept(client.getAuthorizationResultFromIntent(result.data!!))
        } catch (e: Exception) {
            onResult(null, "Google authorization failed: " + (e.localizedMessage ?: "unknown error"))
        }
    }

    fun request() {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(scope)))
            .build()

        try {
            client.authorize(request)
                .addOnSuccessListener { result ->
                    if (result.hasResolution()) {
                        val pending = result.pendingIntent
                        if (pending == null) {
                            onResult(null, "Google returned no authorization screen.")
                        } else {
                            try {
                                launcher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                            } catch (e: Exception) {
                                onResult(null, "Could not open Google authorization: " +
                                    (e.localizedMessage ?: "device error"))
                            }
                        }
                    } else {
                        accept(result)
                    }
                }
                .addOnFailureListener { e ->
                    onResult(null, "Google authorization not available: " +
                        (e.localizedMessage ?: "check Google Play services and OAuth setup"))
                }
        } catch (e: Exception) {
            onResult(null, "Google authorization unavailable: " +
                (e.localizedMessage ?: "check device Google services"))
        }
    }

    private fun accept(result: AuthorizationResult) {
        val token = result.accessToken?.takeIf { it.isNotBlank() }
        if (token.isNullOrBlank()) {
            onResult(null, "No OAuth access token returned. Check OAuth consent configuration.")
            return
        }
        if (result.grantedScopes?.contains(scope) != true) {
            onResult(null, "Google cloud-platform permission was not granted.")
            return
        }
        onResult(token, "Google cloud-platform authorization granted. Token expires; reconnect when needed.")
    }
}
