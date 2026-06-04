package com.marinmiruna.vaultly.data.export

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultExportCryptoTest {

    @Test
    fun encrypt_thenDecryptWithCorrectPassword_returnsOriginalBytes() {
        val plainText = "date sensibile Vaultly"
        val password = "parola-export-puternica".toCharArray()

        val envelope = VaultExportCrypto.encrypt(
            plainBytes = plainText.toByteArray(Charsets.UTF_8),
            password = password,
            createdAt = 123L
        )

        val decryptedBytes = VaultExportCrypto.decrypt(
            envelope = envelope,
            password = "parola-export-puternica".toCharArray()
        )

        assertArrayEquals(
            plainText.toByteArray(Charsets.UTF_8),
            decryptedBytes
        )
    }

    @Test
    fun encrypt_setsExpectedEnvelopeMetadata() {
        val envelope = VaultExportCrypto.encrypt(
            plainBytes = "payload".toByteArray(Charsets.UTF_8),
            password = "parola-export".toCharArray(),
            createdAt = 456L
        )

        assertEquals(VaultExportCrypto.EXPORT_VERSION, envelope.version)
        assertEquals(456L, envelope.createdAt)
        assertTrue(envelope.salt.isNotEmpty())
        assertTrue(envelope.iv.isNotEmpty())
        assertTrue(envelope.cipherText.isNotEmpty())
    }

    @Test
    fun encrypt_cipherTextDoesNotContainPlainText() {
        val plainText = "parola foarte secreta"
        val envelope = VaultExportCrypto.encrypt(
            plainBytes = plainText.toByteArray(Charsets.UTF_8),
            password = "parola-export".toCharArray()
        )

        val cipherTextAsString = envelope.cipherText.toString(Charsets.UTF_8)

        assertFalse(cipherTextAsString.contains(plainText))
    }

    @Test(expected = Exception::class)
    fun decrypt_withWrongPassword_fails() {
        val envelope = VaultExportCrypto.encrypt(
            plainBytes = "payload secret".toByteArray(Charsets.UTF_8),
            password = "parola-corecta".toCharArray()
        )

        VaultExportCrypto.decrypt(
            envelope = envelope,
            password = "parola-gresita".toCharArray()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun encrypt_withEmptyPassword_fails() {
        VaultExportCrypto.encrypt(
            plainBytes = "payload".toByteArray(Charsets.UTF_8),
            password = charArrayOf()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun decrypt_withUnsupportedVersion_fails() {
        val envelope = VaultExportCrypto.encrypt(
            plainBytes = "payload".toByteArray(Charsets.UTF_8),
            password = "parola-export".toCharArray()
        )

        val unsupportedEnvelope = envelope.copy(
            version = VaultExportCrypto.EXPORT_VERSION + 1
        )

        VaultExportCrypto.decrypt(
            envelope = unsupportedEnvelope,
            password = "parola-export".toCharArray()
        )
    }
}