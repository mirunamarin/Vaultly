package com.marinmiruna.vaultly.data.crypto

import com.marinmiruna.vaultly.R
import android.content.Context
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.UserNotAuthenticatedException
import java.security.GeneralSecurityException

@Singleton
class CryptoManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keystoreHelper: KeystoreHelper
) {

    fun encryptBytes(plainBytes: ByteArray): EncryptedPayload {
        return runCryptoOperation {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, keystoreHelper.getOrCreateVaultKey())

            val cipherBytes = cipher.doFinal(plainBytes)

            EncryptedPayload(
                iv = cipher.iv,
                cipherText = cipherBytes
            )
        }
    }

    fun decryptBytes(payload: EncryptedPayload): ByteArray {
        return runCryptoOperation {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, payload.iv)

            cipher.init(
                Cipher.DECRYPT_MODE,
                keystoreHelper.getOrCreateVaultKey(),
                spec
            )

            cipher.doFinal(payload.cipherText)
        }
    }

    fun encryptText(plainText: String): EncryptedPayload {
        return encryptBytes(plainText.toByteArray(Charsets.UTF_8))
    }

    fun decryptText(payload: EncryptedPayload): String {
        return decryptBytes(payload).toString(Charsets.UTF_8)
    }

    private fun <T> runCryptoOperation(operation: () -> T): T {
        return try {
            operation()
        } catch (exception: UserNotAuthenticatedException) {
            throw SecurityException(
                context.getString(R.string.crypto_error_auth_expired),
                exception
            )
        } catch (exception: KeyPermanentlyInvalidatedException) {
            throw SecurityException(
                context.getString(R.string.crypto_error_key_invalidated),
                exception
            )
        } catch (exception: AEADBadTagException) {
            throw SecurityException(
                context.getString(R.string.crypto_error_data_validation_failed),
                exception
            )
        } catch (exception: GeneralSecurityException) {
            throw SecurityException(
                context.getString(R.string.crypto_error_operation_failed),
                exception
            )
        } catch (exception: IllegalArgumentException) {
            throw SecurityException(
                context.getString(R.string.crypto_error_invalid_structure),
                exception
            )
        }
    }

    fun getOrCreateDatabasePassphrase(): ByteArray {
        val preferences = context.getSharedPreferences(DB_KEY_PREFS, Context.MODE_PRIVATE)
        val savedIv = preferences.getString(DB_KEY_IV, null)
        val savedCipherText = preferences.getString(DB_KEY_CIPHER_TEXT, null)

        if (savedIv != null && savedCipherText != null) {
            return runCatching {
                val payload = EncryptedPayload(
                    iv = Base64.decode(savedIv, Base64.NO_WRAP),
                    cipherText = Base64.decode(savedCipherText, Base64.NO_WRAP)
                )
                decryptBytes(payload)
            }.getOrElse { exception ->
                throw VaultDatabaseKeyException(
                    context.getString(R.string.database_key_decrypt_failed),
                    exception
                )
            }
        }

        val passphrase = ByteArray(DB_PASSPHRASE_LENGTH_BYTES)
        secureRandom.nextBytes(passphrase)

        val encryptedPassphrase = runCatching {
            encryptBytes(passphrase)
        }.getOrElse { exception ->
            throw VaultDatabaseKeyException(
                context.getString(R.string.database_key_create_failed),
                exception
            )
        }

        val saved = preferences.edit()
            .putString(DB_KEY_IV, Base64.encodeToString(encryptedPassphrase.iv, Base64.NO_WRAP))
            .putString(
                DB_KEY_CIPHER_TEXT,
                Base64.encodeToString(encryptedPassphrase.cipherText, Base64.NO_WRAP)
            )
            .commit()

        if (!saved) {
            throw VaultDatabaseKeyException(
                context.getString(R.string.database_key_save_failed)
            )
        }

        return passphrase
    }

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val DB_PASSPHRASE_LENGTH_BYTES = 32

        private const val DB_KEY_PREFS = "vaultly_db_key_preferences"
        private const val DB_KEY_IV = "encrypted_database_key_iv"
        private const val DB_KEY_CIPHER_TEXT = "encrypted_database_key_cipher_text"

        private val secureRandom = SecureRandom()
    }
}

data class EncryptedPayload(
    val iv: ByteArray,
    val cipherText: ByteArray
)

class VaultDatabaseKeyException(
    message: String,
    cause: Throwable? = null
) : SecurityException(message, cause)