package com.marinmiruna.vaultly.data.repository

import com.marinmiruna.vaultly.R
import android.net.Uri
import com.marinmiruna.vaultly.data.crypto.FileEncryptor
import com.marinmiruna.vaultly.data.db.dao.FileDao
import com.marinmiruna.vaultly.data.db.entity.FileEntity
import com.marinmiruna.vaultly.domain.model.FileItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File

@Singleton
class FileRepository @Inject constructor(
    private val fileDao: FileDao,
    private val fileEncryptor: FileEncryptor,
    @ApplicationContext private val context: Context
) {

    fun observeFiles(query: String): Flow<List<FileItem>> {
        return fileDao.observeFiles(query).map { files ->
            files.map { entity -> entity.toDomain() }
        }
    }

    fun observeFile(id: Long): Flow<FileItem?> {
        return fileDao.observeFile(id).map { entity ->
            entity?.toDomain()
        }
    }

    suspend fun importFile(
        sourceUri: Uri,
        displayName: String,
        mimeType: String
    ) {
        validateFileForImport(
            sourceUri = sourceUri,
            displayName = displayName
        )
        val encryptedFile = fileEncryptor.encryptUriToInternalFile(
            sourceUri = sourceUri,
            directoryName = FILE_DIRECTORY,
            fileName = displayName
        )

        fileDao.upsert(
            FileEntity(
                displayName = displayName,
                encryptedFilePath = encryptedFile.absolutePath,
                mimeType = mimeType,
                sizeBytes = encryptedFile.sizeBytes,
                importedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun decryptFileTo(
        fileItem: FileItem,
        outputFile: File
    ): Long {
        return fileEncryptor.decryptInternalFileToFile(
            filePath = fileItem.encryptedFilePath,
            outputFile = outputFile
        )
    }

    suspend fun deleteFile(fileItem: FileItem) {
        fileDao.delete(fileItem.toEntity())

        runCatching {
            fileEncryptor.securelyDelete(fileItem.encryptedFilePath)
        }
    }

    private fun FileEntity.toDomain(): FileItem {
        return FileItem(
            id = id,
            displayName = displayName,
            encryptedFilePath = encryptedFilePath,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            importedAt = importedAt
        )
    }

    private fun FileItem.toEntity(): FileEntity {
        return FileEntity(
            id = id,
            displayName = displayName,
            encryptedFilePath = encryptedFilePath,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            importedAt = importedAt
        )
    }

    private fun validateFileForImport(
        sourceUri: Uri,
        displayName: String
    ) {
        require(displayName.isNotBlank()) {
            context.getString(R.string.file_error_invalid_name)
        }

        val sourceSizeBytes = resolveSourceSize(sourceUri)

        require(sourceSizeBytes != 0L) {
            context.getString(R.string.file_error_empty)
        }

        if (sourceSizeBytes > 0L) {
            require(sourceSizeBytes <= MAX_FILE_SIZE_BYTES) {
                context.getString(R.string.file_error_too_large)
            }
        }
    }

    private fun resolveSourceSize(sourceUri: Uri): Long {
        context.contentResolver.query(
            sourceUri,
            arrayOf(OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex >= 0 && cursor.moveToFirst() && !cursor.isNull(sizeIndex)) {
                return cursor.getLong(sizeIndex)
            }
        }

        return context.contentResolver.openAssetFileDescriptor(sourceUri, "r")?.use { descriptor ->
            descriptor.length
        } ?: -1L
    }

    companion object {
        const val FILE_DIRECTORY = "encrypted_files"
        private const val MAX_FILE_SIZE_BYTES = 100L * 1024L * 1024L
    }
}