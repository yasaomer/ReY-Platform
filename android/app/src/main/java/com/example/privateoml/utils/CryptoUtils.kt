package com.example.privateoml.utils

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {

    // Simple SHA-256 Hashing for quickly comparing hashes
    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // PBKDF2 Password Hashing
    fun hashPassword(password: CharArray, salt: ByteArray, iterations: Int = 100000, keyLength: Int = 256): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, keyLength)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return skf.generateSecret(spec).encoded
    }

    fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }

    // Basic AES Encryption
    fun encrypt(data: String, secretKey: String): String {
        try {
            val keyBytes = sha256(secretKey).substring(0, 32).toByteArray(Charsets.UTF_8)
            val secretKeySpec = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val iv = ByteArray(16)
            SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec)
            val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            val combined = iv + encryptedBytes
            return Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    // Basic AES Decryption
    fun decrypt(encryptedData: String, secretKey: String): String {
        try {
            val combined = Base64.decode(encryptedData, Base64.DEFAULT)
            val iv = combined.sliceArray(0..15)
            val encryptedBytes = combined.sliceArray(16 until combined.size)
            val keyBytes = sha256(secretKey).substring(0, 32).toByteArray(Charsets.UTF_8)
            val secretKeySpec = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }
}
