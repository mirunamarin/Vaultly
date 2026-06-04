package com.marinmiruna.vaultly.data.export

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object VaultExportCrypto {

    fun encrypt(
        plainBytes: ByteArray,
        password: CharArray,
        createdAt: Long = System.currentTimeMillis(),
        emptyPasswordMessage: String = "The export password cannot be empty."
    ): VaultExportEnvelope {
        require(password.isNotEmpty()) {
            emptyPasswordMessage
        }

        val salt = ByteArray(SALT_SIZE_BYTES)
        secureRandom.nextBytes(salt)

        val iv = ByteArray(IV_SIZE_BYTES)
        secureRandom.nextBytes(iv)

        val secretKey = deriveKey(
            password = password,
            salt = salt
        )

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            secretKey,
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        )

        val cipherText = cipher.doFinal(plainBytes)

        return VaultExportEnvelope(
            version = EXPORT_VERSION,
            createdAt = createdAt,
            salt = salt,
            iv = iv,
            cipherText = cipherText
        )
    }

    fun decrypt(
        envelope: VaultExportEnvelope,
        password: CharArray,
        emptyPasswordMessage: String = "The export password cannot be empty.",
        unsupportedVersionMessage: String = "The export version is not supported."
    ): ByteArray {
        require(password.isNotEmpty()) {
            emptyPasswordMessage
        }

        require(envelope.version == EXPORT_VERSION) {
            unsupportedVersionMessage
        }

        val secretKey = deriveKey(
            password = password,
            salt = envelope.salt
        )

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey,
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, envelope.iv)
        )

        return cipher.doFinal(envelope.cipherText)
    }

    private fun deriveKey(
        password: CharArray,
        salt: ByteArray
    ): SecretKeySpec {
        val keySpec = PBEKeySpec(
            password,
            salt,
            PBKDF2_ITERATIONS,
            KEY_SIZE_BITS
        )

        val keyBytes = SecretKeyFactory
            .getInstance(KEY_DERIVATION_ALGORITHM)
            .generateSecret(keySpec)
            .encoded

        return SecretKeySpec(keyBytes, KEY_ALGORITHM)
    }

    const val EXPORT_VERSION = 1

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY_ALGORITHM = "AES"
    private const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val KEY_SIZE_BITS = 256
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val SALT_SIZE_BYTES = 16
    private const val IV_SIZE_BYTES = 12
    private const val PBKDF2_ITERATIONS = 120_000

    private val secureRandom = SecureRandom()
}