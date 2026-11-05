package com.parmtech.mvg

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest
import java.util.Locale

/**
 * Public SHA-1 fingerprint of the CURRENT installed APK signing certificate.
 * This is not a secret and is safe to copy into Google Cloud Android OAuth client settings.
 * Never compute this from a debug keystore guessed to be the same as the installed APK.
 */
object SigningIdentity {
    @Suppress("DEPRECATION")
    fun certificateSha1(context: Context): String {
        return try {
            val manager = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                manager.getPackageInfo(
                    context.packageName, PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                manager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }
            val certificate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signing = info.signingInfo ?: return "Unavailable"
                // OAuth requires the signer on this installed APK, not an old certificate
                // from signing rotation history.
                signing.apkContentsSigners?.firstOrNull()
            } else {
                info.signatures?.firstOrNull()
            } ?: return "Unavailable"
            MessageDigest.getInstance("SHA-1")
                .digest(certificate.toByteArray())
                .joinToString(":") { String.format(Locale.US, "%02X", it.toInt() and 0xff) }
        } catch (_: Exception) {
            "Unavailable"
        }
    }
}
