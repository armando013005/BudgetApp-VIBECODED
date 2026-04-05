package com.budgetapp.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoManager @Inject constructor() {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun getOrCreateKey(alias: String = KEY_ALIAS): SecretKey {
        val existingKey = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
        if (existingKey != null) return existingKey.secretKey

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return keyGenerator.generateKey()
    }

    fun encrypt(plaintext: String): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        // Prepend IV length (1 byte) + IV + ciphertext
        return byteArrayOf(iv.size.toByte()) + iv + encrypted
    }

    fun decrypt(ciphertext: ByteArray): String {
        val ivLength = ciphertext[0].toInt()
        val iv = ciphertext.sliceArray(1..ivLength)
        val encrypted = ciphertext.sliceArray((ivLength + 1) until ciphertext.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    fun encryptToBase64(plaintext: String): String {
        return android.util.Base64.encodeToString(encrypt(plaintext), android.util.Base64.NO_WRAP)
    }

    fun decryptFromBase64(base64Ciphertext: String): String {
        val ciphertext = android.util.Base64.decode(base64Ciphertext, android.util.Base64.NO_WRAP)
        return decrypt(ciphertext)
    }

    companion object {
        private const val KEY_ALIAS = "budget_app_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
