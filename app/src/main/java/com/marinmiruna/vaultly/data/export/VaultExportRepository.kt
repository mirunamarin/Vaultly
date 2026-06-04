package com.marinmiruna.vaultly.data.export

import android.content.Context
import com.marinmiruna.vaultly.R
import dagger.hilt.android.qualifiers.ApplicationContext
import android.util.Base64
import com.marinmiruna.vaultly.data.crypto.CryptoManager
import com.marinmiruna.vaultly.data.crypto.EncryptedPayload
import com.marinmiruna.vaultly.data.db.dao.NoteDao
import com.marinmiruna.vaultly.data.db.dao.PasswordDao
import com.marinmiruna.vaultly.data.db.entity.NoteEntity
import com.marinmiruna.vaultly.data.db.entity.PasswordEntity
import com.marinmiruna.vaultly.data.crypto.FileEncryptor
import com.marinmiruna.vaultly.data.db.dao.FileDao
import com.marinmiruna.vaultly.data.db.dao.PhotoDao
import com.marinmiruna.vaultly.data.db.entity.FileEntity
import com.marinmiruna.vaultly.data.db.entity.PhotoEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class VaultExportRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val passwordDao: PasswordDao,
    private val fileDao: FileDao,
    private val photoDao: PhotoDao,
    private val cryptoManager: CryptoManager,
    private val fileEncryptor: FileEncryptor,
    @ApplicationContext private val context: Context
) {

    suspend fun exportVault(
        exportPassword: CharArray
    ): ByteArray = withContext(Dispatchers.IO) {
        require(exportPassword.isNotEmpty()) {
            context.getString(R.string.export_error_password_empty)
        }

        val createdAt = System.currentTimeMillis()

        val payload = VaultExportPayload(
            version = VaultExportCrypto.EXPORT_VERSION,
            createdAt = createdAt,
            notes = noteDao.getAllNotes().map { note ->
                note.toExportNote()
            },
            passwords = passwordDao.getAllPasswords().map { password ->
                password.toExportPassword()
            },
            files = fileDao.getAllFiles().map { file ->
                file.toExportFile()
            },
            photos = photoDao.getAllPhotos().map { photo ->
                photo.toExportPhoto()
            }
        )

        val payloadBytes = payload.toJsonObject()
            .toString()
            .toByteArray(Charsets.UTF_8)

        val envelope = VaultExportCrypto.encrypt(
            plainBytes = payloadBytes,
            password = exportPassword,
            createdAt = createdAt,
            emptyPasswordMessage = context.getString(R.string.export_error_password_empty)
        )

        envelope.toJsonObject()
            .toString()
            .toByteArray(Charsets.UTF_8)
    }

    private fun NoteEntity.toExportNote(): VaultExportNote {
        return VaultExportNote(
            title = title,
            content = content,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun PasswordEntity.toExportPassword(): VaultExportPassword {
        return VaultExportPassword(
            service = service,
            username = username,
            password = cryptoManager.decryptText(
                EncryptedPayload(
                    iv = encryptedPasswordIv,
                    cipherText = encryptedPasswordCipherText
                )
            ),
            url = url,
            note = note,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private suspend fun FileEntity.toExportFile(): VaultExportFile {
        return VaultExportFile(
            displayName = displayName,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            importedAt = importedAt,
            contentBase64 = fileEncryptor
                .decryptInternalFile(encryptedFilePath)
                .toBase64()
        )
    }

    private suspend fun PhotoEntity.toExportPhoto(): VaultExportPhoto {
        return VaultExportPhoto(
            displayName = displayName,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            importedAt = importedAt,
            contentBase64 = fileEncryptor
                .decryptInternalFile(encryptedFilePath)
                .toBase64()
        )
    }

    private fun VaultExportPayload.toJsonObject(): JSONObject {
        return JSONObject()
            .put("version", version)
            .put("createdAt", createdAt)
            .put("notes", notes.toJsonArray { note ->
                JSONObject()
                    .put("title", note.title)
                    .put("content", note.content)
                    .put("createdAt", note.createdAt)
                    .put("updatedAt", note.updatedAt)
            })
            .put("passwords", passwords.toJsonArray { password ->
                JSONObject()
                    .put("service", password.service)
                    .put("username", password.username)
                    .put("password", password.password)
                    .put("url", password.url)
                    .put("note", password.note)
                    .put("createdAt", password.createdAt)
                    .put("updatedAt", password.updatedAt)
            })
            .put("files", files.toJsonArray { file ->
                JSONObject()
                    .put("displayName", file.displayName)
                    .put("mimeType", file.mimeType)
                    .put("sizeBytes", file.sizeBytes)
                    .put("importedAt", file.importedAt)
                    .put("contentBase64", file.contentBase64)
            })
            .put("photos", photos.toJsonArray { photo ->
                JSONObject()
                    .put("displayName", photo.displayName)
                    .put("mimeType", photo.mimeType)
                    .put("sizeBytes", photo.sizeBytes)
                    .put("importedAt", photo.importedAt)
                    .put("contentBase64", photo.contentBase64)
            })
    }

    private fun VaultExportEnvelope.toJsonObject(): JSONObject {
        return JSONObject()
            .put("format", EXPORT_FORMAT)
            .put("version", version)
            .put("createdAt", createdAt)
            .put("salt", salt.toBase64())
            .put("iv", iv.toBase64())
            .put("cipherText", cipherText.toBase64())
    }

    private fun <T> List<T>.toJsonArray(
        transform: (T) -> JSONObject
    ): JSONArray {
        val array = JSONArray()
        forEach { item ->
            array.put(transform(item))
        }
        return array
    }

    private fun ByteArray.toBase64(): String {
        return Base64.encodeToString(this, Base64.NO_WRAP)
    }

    companion object {
        private const val EXPORT_FORMAT = "vaultly.encrypted.export"
    }
}