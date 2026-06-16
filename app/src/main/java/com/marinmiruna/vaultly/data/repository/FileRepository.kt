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
import java.security.MessageDigest
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import java.io.ByteArrayOutputStream

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
            displayName = displayName,
            mimeType = mimeType
        )

        val contentHash = calculateContentHash(sourceUri)

        require(!fileDao.existsByContentHash(contentHash)) {
            context.getString(R.string.file_error_duplicate)
        }

        val encryptedFile = fileEncryptor.encryptUriToInternalFile(
            sourceUri = sourceUri,
            directoryName = FILE_DIRECTORY,
            fileName = displayName
        )

        val previewEncryptedFilePath = if (mimeType == "application/pdf") {
            createPdfPreview(sourceUri, displayName)
        } else {
            null
        }

        fileDao.upsert(
            FileEntity(
                displayName = displayName,
                encryptedFilePath = encryptedFile.absolutePath,
                previewEncryptedFilePath = previewEncryptedFilePath,
                mimeType = mimeType,
                sizeBytes = encryptedFile.sizeBytes,
                importedAt = System.currentTimeMillis(),
                contentHash = contentHash
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

    suspend fun decryptPreview(fileItem: FileItem): ByteArray? {
        val previewPath = fileItem.previewEncryptedFilePath ?: return null

        return fileEncryptor.decryptInternalFile(previewPath)
    }

    suspend fun deleteFile(fileItem: FileItem) {
        fileDao.delete(fileItem.toEntity())

        runCatching {
            fileEncryptor.securelyDelete(fileItem.encryptedFilePath)
            fileItem.previewEncryptedFilePath?.let { previewPath ->
                runCatching {
                    fileEncryptor.securelyDelete(previewPath)
                }
            }
        }
    }

    private fun FileEntity.toDomain(): FileItem {
        return FileItem(
            id = id,
            displayName = displayName,
            encryptedFilePath = encryptedFilePath,
            previewEncryptedFilePath = previewEncryptedFilePath,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            importedAt = importedAt,
            contentHash = contentHash
        )
    }

    private fun FileItem.toEntity(): FileEntity {
        return FileEntity(
            id = id,
            displayName = displayName,
            encryptedFilePath = encryptedFilePath,
            previewEncryptedFilePath = previewEncryptedFilePath,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            importedAt = importedAt,
            contentHash = contentHash
        )
    }

    private fun validateFileForImport(
        sourceUri: Uri,
        displayName: String,
        mimeType: String
    ) {
        require(displayName.isNotBlank()) {
            context.getString(R.string.file_error_invalid_name)
        }

        require(isSupportedDocumentMimeType(mimeType)) {
            context.getString(R.string.file_error_invalid_type)
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

    private fun isSupportedDocumentMimeType(mimeType: String): Boolean {
        return mimeType in setOf(
            "application/pdf",
            "text/plain",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        )
    }

    private fun calculateContentHash(sourceUri: Uri): String {
        val digest = MessageDigest.getInstance("SHA-256")

        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) break
                digest.update(buffer, 0, bytesRead)
            }
        } ?: throw IllegalArgumentException(
            context.getString(R.string.encrypted_file_error_cannot_read_selected)
        )

        return digest.digest().joinToString(separator = "") { byte ->
            "%02x".format(byte)
        }
    }

    private suspend fun createPdfPreview(
        sourceUri: Uri,
        displayName: String
    ): String? {
        val previewBytes = renderFirstPdfPage(sourceUri) ?: return null

        return fileEncryptor.encryptBytesToInternalFile(
            plainBytes = previewBytes,
            directoryName = FILE_PREVIEW_DIRECTORY,
            fileName = "${displayName}_preview.jpg"
        ).absolutePath
    }

    private fun renderFirstPdfPage(sourceUri: Uri): ByteArray? {
        val descriptor = context.contentResolver.openFileDescriptor(sourceUri, "r") ?: return null

        descriptor.use { parcelFileDescriptor ->
            PdfRenderer(parcelFileDescriptor).use { renderer ->
                if (renderer.pageCount <= 0) {
                    return null
                }

                renderer.openPage(0).use { page ->
                    val scale = PDF_PREVIEW_WIDTH.toFloat() / page.width.toFloat()
                    val targetHeight = (page.height * scale).toInt().coerceAtLeast(1)

                    val bitmap = Bitmap.createBitmap(
                        PDF_PREVIEW_WIDTH,
                        targetHeight,
                        Bitmap.Config.ARGB_8888
                    )

                    bitmap.eraseColor(Color.WHITE)

                    page.render(
                        bitmap,
                        null,
                        null,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                    )

                    return ByteArrayOutputStream().use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, PDF_PREVIEW_QUALITY, outputStream)
                        outputStream.toByteArray()
                    }
                }
            }
        }
    }

    companion object {
        const val FILE_DIRECTORY = "encrypted_files"
        private const val FILE_PREVIEW_DIRECTORY = "encrypted_file_previews"
        private const val PDF_PREVIEW_WIDTH = 360
        private const val PDF_PREVIEW_QUALITY = 82
        private const val MAX_FILE_SIZE_BYTES = 100L * 1024L * 1024L
    }
}