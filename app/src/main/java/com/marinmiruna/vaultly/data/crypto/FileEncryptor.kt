package com.marinmiruna.vaultly.data.crypto

import com.marinmiruna.vaultly.R
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class FileEncryptor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keystoreHelper: KeystoreHelper
) {

    suspend fun encryptBytesToInternalFile(
        plainBytes: ByteArray,
        directoryName: String,
        fileName: String
    ): EncryptedFileMetadata = withContext(Dispatchers.IO) {
        val directory = getOrCreateDirectory(directoryName)
        val safeFileName = generateSecureStorageFileName(fileName)
        val targetFile = File(directory, safeFileName)
        val temporaryFile = File(directory, "$safeFileName$TEMPORARY_FILE_EXTENSION")

        runCatching {
            if (temporaryFile.exists()) {
                temporaryFile.delete()
            }

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, keystoreHelper.getOrCreateVaultKey())

            FileOutputStream(temporaryFile).use { fileOutputStream ->
                writeIv(fileOutputStream, cipher.iv)

                CipherOutputStream(fileOutputStream, cipher).use { cipherOutputStream ->
                    cipherOutputStream.write(plainBytes)
                }
            }

            replaceWithEncryptedFile(
                temporaryFile = temporaryFile,
                targetFile = targetFile
            )

            EncryptedFileMetadata(
                absolutePath = targetFile.absolutePath,
                fileName = safeFileName,
                sizeBytes = targetFile.length()
            )
        }.getOrElse { exception ->
            temporaryFile.delete()
            throw exception
        }
    }

    suspend fun encryptUriToInternalFile(
        sourceUri: Uri,
        directoryName: String,
        fileName: String
    ): EncryptedFileMetadata = withContext(Dispatchers.IO) {
        val directory = getOrCreateDirectory(directoryName)
        val safeFileName = generateSecureStorageFileName(fileName)
        val targetFile = File(directory, safeFileName)
        val temporaryFile = File(directory, "$safeFileName$TEMPORARY_FILE_EXTENSION")

        runCatching {
            if (temporaryFile.exists()) {
                temporaryFile.delete()
            }

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, keystoreHelper.getOrCreateVaultKey())

            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: throw IllegalArgumentException(
                    context.getString(R.string.encrypted_file_error_cannot_read_selected)
                )

            inputStream.use { plainInputStream ->
                FileOutputStream(temporaryFile).use { fileOutputStream ->
                    writeIv(fileOutputStream, cipher.iv)

                    CipherOutputStream(fileOutputStream, cipher).use { cipherOutputStream ->
                        plainInputStream.copyTo(cipherOutputStream, DEFAULT_BUFFER_SIZE)
                    }
                }
            }

            replaceWithEncryptedFile(
                temporaryFile = temporaryFile,
                targetFile = targetFile
            )

            EncryptedFileMetadata(
                absolutePath = targetFile.absolutePath,
                fileName = safeFileName,
                sizeBytes = targetFile.length()
            )
        }.getOrElse { exception ->
            temporaryFile.delete()
            throw exception
        }
    }

    suspend fun decryptInternalFile(filePath: String): ByteArray = withContext(Dispatchers.IO) {
        val encryptedFile = File(filePath)
        require(encryptedFile.exists()) {
            context.getString(R.string.encrypted_file_error_missing)
        }

        FileInputStream(encryptedFile).use { fileInputStream ->
            val iv = readIv(fileInputStream)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                keystoreHelper.getOrCreateVaultKey(),
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            )

            CipherInputStream(fileInputStream, cipher).use { cipherInputStream ->
                ByteArrayOutputStream().use { outputStream ->
                    cipherInputStream.copyTo(outputStream, DEFAULT_BUFFER_SIZE)
                    outputStream.toByteArray()
                }
            }
        }
    }

    suspend fun decryptInternalFileToFile(
        filePath: String,
        outputFile: File
    ): Long = withContext(Dispatchers.IO) {
        runCatching {
            val encryptedFile = File(filePath)
            require(encryptedFile.exists()) {
                context.getString(R.string.encrypted_file_error_missing)
            }

            FileInputStream(encryptedFile).use { fileInputStream ->
                val iv = readIv(fileInputStream)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    keystoreHelper.getOrCreateVaultKey(),
                    GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
                )

                CipherInputStream(fileInputStream, cipher).use { cipherInputStream ->
                    FileOutputStream(outputFile, false).use { outputStream ->
                        cipherInputStream.copyTo(outputStream, DEFAULT_BUFFER_SIZE)
                        outputStream.flush()
                    }
                }
            }

            outputFile.length()
        }.getOrElse { exception ->
            outputFile.delete()
            throw exception
        }
    }

    suspend fun securelyDelete(filePath: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) {
            return@withContext false
        }

        val length = file.length()
        FileOutputStream(file, false).use { outputStream ->
            val buffer = ByteArray(SECURE_DELETE_BUFFER_SIZE)
            var remaining = length

            while (remaining > 0) {
                val bytesToWrite = minOf(buffer.size.toLong(), remaining).toInt()
                outputStream.write(buffer, 0, bytesToWrite)
                remaining -= bytesToWrite
            }

            outputStream.flush()
            outputStream.fd.sync()
        }

        file.delete()
    }

    private fun getOrCreateDirectory(directoryName: String): File {
        return File(context.filesDir, directoryName).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    private fun replaceWithEncryptedFile(
        temporaryFile: File,
        targetFile: File
    ) {
        if (targetFile.exists() && !targetFile.delete()) {
            throw IOException(
                context.getString(R.string.encrypted_file_error_replace_failed)
            )
        }

        if (!temporaryFile.renameTo(targetFile)) {
            throw IOException(
                context.getString(R.string.encrypted_file_error_temp_finalize_failed)
            )
        }
    }

    private fun writeIv(outputStream: FileOutputStream, iv: ByteArray) {
        outputStream.write(iv.size)
        outputStream.write(iv)
    }

    private fun readIv(inputStream: FileInputStream): ByteArray {
        val ivSize = inputStream.read()
        require(ivSize > 0) {
            context.getString(R.string.encrypted_file_error_invalid_structure)
        }

        return ByteArray(ivSize).also { iv ->
            val bytesRead = inputStream.read(iv)
            require(bytesRead == ivSize) {
                context.getString(R.string.encrypted_file_error_invalid_structure)
            }
        }
    }

    private fun generateSecureStorageFileName(originalFileName: String): String {
        val randomName = generateSecureFileName()
        val extension = originalFileName
            .substringAfterLast('.', missingDelimiterValue = "")
            .trim()
            .replace(Regex("""[\\/:*?"<>|\s.]"""), "")
            .take(MAX_FILE_EXTENSION_LENGTH)

        return if (extension.isBlank()) {
            randomName
        } else {
            "$randomName.$extension"
        }
    }


    private fun generateSecureFileName(): String {
        val randomBytes = ByteArray(16)
        secureRandom.nextBytes(randomBytes)
        return randomBytes.joinToString(separator = "") { byte ->
            "%02x".format(byte)
        }
    }

    companion object {
        const val TEMPORARY_FILE_EXTENSION = ".tmp"

        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val DEFAULT_BUFFER_SIZE = 64 * 1024
        private const val SECURE_DELETE_BUFFER_SIZE = 8 * 1024
        private const val MAX_FILE_EXTENSION_LENGTH = 12
        private val secureRandom = SecureRandom()
    }
}

data class EncryptedFileMetadata(
    val absolutePath: String,
    val fileName: String,
    val sizeBytes: Long
)