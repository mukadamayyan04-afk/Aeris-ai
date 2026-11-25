package com.example.ai

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed Android KeyStore AES-256-GCM vault for encrypting user-supplied
 * external provider bearer tokens before storing them in DataStore.
 * Gracefully falls back when running in non-TEE JVM unit tests (Robolectric).
 */
object CredentialVault {

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "aeris_provider_credential_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val ENC_PREFIX = "ENCv1:"
    private const val RAW_PREFIX = "RAW:"

    fun encrypt(plainToken: String): String {
        val trimmed = plainToken.trim()
        if (trimmed.isEmpty()) return ""
        return try {
            val secretKey = getOrCreateSecretKey() ?: return RAW_PREFIX + trimmed
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(trimmed.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(1 + iv.size + encryptedBytes.size)
            combined[0] = iv.size.toByte()
            System.arraycopy(iv, 0, combined, 1, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, 1 + iv.size, encryptedBytes.size)
            ENC_PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (_: Exception) {
            RAW_PREFIX + trimmed
        }
    }

    fun decrypt(storedValue: String): String {
        if (storedValue.isBlank()) return ""
        if (storedValue.startsWith(RAW_PREFIX)) {
            return storedValue.removePrefix(RAW_PREFIX)
        }
        if (!storedValue.startsWith(ENC_PREFIX)) {
            return storedValue
        }
        return try {
            val payload = Base64.decode(storedValue.removePrefix(ENC_PREFIX), Base64.NO_WRAP)
            if (payload.isEmpty()) return ""
            val ivLen = payload[0].toInt() and 0xFF
            if (payload.size <= 1 + ivLen) return ""
            val iv = payload.copyOfRange(1, 1 + ivLen)
            val cipherText = payload.copyOfRange(1 + ivLen, payload.size)
            val secretKey = getOrCreateSecretKey() ?: return ""
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Produces a safe non-reversible masked preview (e.g., "••••••abcd") so the UI never displays
     * a saved secret key back in plaintext.
     */
    fun maskKey(plainToken: String): String {
        val trimmed = plainToken.trim()
        if (trimmed.isEmpty()) return "Not saved"
        return if (trimmed.length <= 6) {
            "••••••••"
        } else {
            "••••••••${trimmed.takeLast(4)}"
        }
    }

    private fun getOrCreateSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)
            val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            if (existing != null) return existing

            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGen.init(spec)
            keyGen.generateKey()
        } catch (_: Exception) {
            null
        }
    }
}
