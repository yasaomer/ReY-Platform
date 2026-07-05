package com.example.privateoml.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object KeyStoreHelper {
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "OmlSecKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    init {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            android.util.Log.e("KeyStoreHelper", "Failed to initialize KeyStore: ${e.message}")
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    fun encrypt(data: String): String {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            // Combine IV (12 bytes for GCM) and encrypted bytes
            val combined = ByteArray(12 + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, 12)
            System.arraycopy(encryptedBytes, 0, combined, 12, encryptedBytes.size)
            return Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            android.util.Log.e("KeyStoreHelper", "Encryption failed: ${e.message}")
            return ""
        }
    }

    fun decrypt(encryptedBase64: String): String {
        try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.size <= 12) return ""
            val iv = ByteArray(12)
            val encryptedBytes = ByteArray(combined.size - 12)
            System.arraycopy(combined, 0, iv, 0, 12)
            System.arraycopy(combined, 12, encryptedBytes, 0, encryptedBytes.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            android.util.Log.e("KeyStoreHelper", "Decryption failed: ${e.message}")
            return ""
        }
    }
}
