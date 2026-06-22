package com.vahak.mehrban.core.util

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.util.Base64
import android.util.Log
import timber.log.Timber
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Arrays

class AppSignatureHelper(context: Context) : ContextWrapper(context) {
    companion object {
        private const val TAG = "AppSignatureHelper"
        private const val HASH_TYPE = "SHA-256"
        private const val NUM_HASHED_BYTES = 9
        private const val NUM_BASE64_CHAR = 11
    }

    fun getAppSignatures(): ArrayList<String> {
        val appSignatures = ArrayList<String>()
        try {
            val packageName = packageName
            val packageManager = packageManager

            // 🚀 THE FIX: Use modern GET_SIGNING_CERTIFICATES for Android 9+
            val signatures: Array<Signature>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signingInfo = packageInfo.signingInfo
                if (signingInfo != null) {
                    if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners
                    } else {
                        signingInfo.signingCertificateHistory
                    }
                } else null
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
            }

            if (signatures != null) {
                for (signature in signatures) {
                    val hash = hash(packageName, signature.toCharsString())
                    if (hash != null) {
                        appSignatures.add(String.format("%s", hash))
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Unable to find package to obtain hash.")
        }
        // 🚀 Look in Logcat for this log! This is your 11-character hash.
        Timber.tag(TAG).d("HASHES: $appSignatures")
        return appSignatures
    }

    private fun hash(packageName: String, signature: String): String? {
        val appInfo = "$packageName $signature"
        try {
            val messageDigest = MessageDigest.getInstance(HASH_TYPE)
            messageDigest.update(appInfo.toByteArray(StandardCharsets.UTF_8))
            var hashSignature = messageDigest.digest()
            hashSignature = hashSignature.copyOfRange(0, NUM_HASHED_BYTES)
            var base64Hash = Base64.encodeToString(hashSignature, Base64.NO_PADDING or Base64.NO_WRAP)
            base64Hash = base64Hash.substring(0, NUM_BASE64_CHAR)
            return base64Hash
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "hash:Unable to obtain MessageDigest")
        }
        return null
    }
}