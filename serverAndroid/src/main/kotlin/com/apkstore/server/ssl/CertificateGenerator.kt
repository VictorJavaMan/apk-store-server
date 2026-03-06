package com.apkstore.server.ssl

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore

/**
 * Manages SSL certificate for the HTTPS server.
 * Uses a pre-generated certificate embedded in assets.
 */
object CertificateManager {
    private const val TAG = "CertificateManager"
    private const val ASSET_NAME = "server.p12"

    /**
     * Loads the PKCS12 keystore from assets.
     */
    fun getKeyStore(context: Context, password: String): KeyStore? {
        return try {
            val keyStore = KeyStore.getInstance("PKCS12")
            context.assets.open(ASSET_NAME).use { inputStream ->
                keyStore.load(inputStream, password.toCharArray())
            }
            Log.d(TAG, "Keystore loaded from assets")
            keyStore
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load keystore from assets", e)
            null
        }
    }
}
