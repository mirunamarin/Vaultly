package com.marinmiruna.vaultly.data.repository

import com.marinmiruna.vaultly.R
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.marinmiruna.vaultly.data.crypto.FileEncryptor
import com.marinmiruna.vaultly.data.db.dao.PhotoDao
import com.marinmiruna.vaultly.data.db.entity.PhotoEntity
import com.marinmiruna.vaultly.domain.model.PhotoItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.security.MessageDigest

@Singleton
class PhotoRepository @Inject constructor(
    private val photoDao: PhotoDao,
    private val fileEncryptor: FileEncryptor,
    @ApplicationContext private val context: Context
) {

    fun observePhotos(): Flow<List<PhotoItem>> {
        return photoDao.observePhotos().map { photos ->
            photos.map { entity -> entity.toDomain() }
        }
    }

    fun observePhoto(id: Long): Flow<PhotoItem?> {
        return photoDao.observePhoto(id).map { entity ->
            entity?.toDomain()
        }
    }

    suspend fun importPhoto(
        sourceUri: Uri,
        displayName: String,
        mimeType: String
    ) {
        validatePhotoForImport(
            sourceUri = sourceUri,
            displayName = displayName,
            mimeType = mimeType
        )

        val sourceBytes = readSourceBytes(sourceUri)

        val contentHash = calculateContentHash(sourceBytes)

        require(!photoDao.existsByContentHash(contentHash)) {
            context.getString(R.string.photo_error_duplicate)
        }

        val encryptedFile = fileEncryptor.encryptBytesToInternalFile(
            plainBytes = sourceBytes,
            directoryName = PHOTO_DIRECTORY,
            fileName = displayName
        )

        val thumbnailBytes = createResizedImageBytes(
            sourceBytes = sourceBytes,
            targetSizePx = THUMBNAIL_TARGET_SIZE_PX,
            quality = THUMBNAIL_QUALITY
        )

        val encryptedThumbnail = fileEncryptor.encryptBytesToInternalFile(
            plainBytes = thumbnailBytes,
            directoryName = PHOTO_THUMBNAIL_DIRECTORY,
            fileName = displayName
        )

        val previewBytes = createResizedImageBytes(
            sourceBytes = sourceBytes,
            targetSizePx = PREVIEW_TARGET_SIZE_PX,
            quality = PREVIEW_QUALITY
        )

        val encryptedPreview = fileEncryptor.encryptBytesToInternalFile(
            plainBytes = previewBytes,
            directoryName = PHOTO_PREVIEW_DIRECTORY,
            fileName = displayName
        )

        photoDao.upsert(
            PhotoEntity(
                displayName = displayName,
                encryptedFilePath = encryptedFile.absolutePath,
                thumbnailEncryptedFilePath = encryptedThumbnail.absolutePath,
                previewEncryptedFilePath = encryptedPreview.absolutePath,
                mimeType = mimeType,
                sizeBytes = encryptedFile.sizeBytes,
                importedAt = System.currentTimeMillis(),
                contentHash = contentHash
            )
        )
    }

    suspend fun decryptPhoto(photoItem: PhotoItem): ByteArray {
        return fileEncryptor.decryptInternalFile(photoItem.encryptedFilePath)
    }

    suspend fun decryptPhotoThumbnail(photoItem: PhotoItem): ByteArray {
        return fileEncryptor.decryptInternalFile(photoItem.thumbnailEncryptedFilePath)
    }

    suspend fun decryptPhotoPreview(photoItem: PhotoItem): ByteArray {
        return fileEncryptor.decryptInternalFile(photoItem.previewEncryptedFilePath)
    }

    suspend fun deletePhoto(photoItem: PhotoItem) {
        photoDao.delete(photoItem.toEntity())

        runCatching {
            fileEncryptor.securelyDelete(photoItem.encryptedFilePath)
        }

        runCatching {
            fileEncryptor.securelyDelete(photoItem.thumbnailEncryptedFilePath)
        }

        runCatching {
            fileEncryptor.securelyDelete(photoItem.previewEncryptedFilePath)
        }
    }

    private suspend fun readSourceBytes(sourceUri: Uri): ByteArray = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            inputStream.readBytes()
        } ?: throw IllegalArgumentException(
            context.getString(R.string.photo_error_cannot_read)
        )
    }

    private suspend fun createResizedImageBytes(
        sourceBytes: ByteArray,
        targetSizePx: Int,
        quality: Int
    ): ByteArray = withContext(Dispatchers.Default) {
        val bitmap = decodeSampledBitmap(
            bytes = sourceBytes,
            targetSizePx = targetSizePx
        )

        ByteArrayOutputStream().use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.toByteArray()
        }
    }

    private fun decodeSampledBitmap(
        bytes: ByteArray,
        targetSizePx: Int
    ): Bitmap {
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(
                width = boundsOptions.outWidth,
                height = boundsOptions.outHeight,
                targetSizePx = targetSizePx
            )
        }

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
            ?: throw IllegalArgumentException(
                context.getString(R.string.photo_error_cannot_process)
            )
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        targetSizePx: Int
    ): Int {
        if (width <= 0 || height <= 0 || targetSizePx <= 0) {
            return 1
        }

        var inSampleSize = 1
        var halfWidth = width / 2
        var halfHeight = height / 2

        while ((halfWidth / inSampleSize) >= targetSizePx &&
            (halfHeight / inSampleSize) >= targetSizePx
        ) {
            inSampleSize *= 2
        }

        return inSampleSize.coerceAtLeast(1)
    }

    private fun PhotoEntity.toDomain(): PhotoItem {
        return PhotoItem(
            id = id,
            displayName = displayName,
            encryptedFilePath = encryptedFilePath,
            thumbnailEncryptedFilePath = thumbnailEncryptedFilePath,
            previewEncryptedFilePath = previewEncryptedFilePath,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            importedAt = importedAt,
            contentHash = contentHash
        )
    }

    private fun PhotoItem.toEntity(): PhotoEntity {
        return PhotoEntity(
            id = id,
            displayName = displayName,
            encryptedFilePath = encryptedFilePath,
            thumbnailEncryptedFilePath = thumbnailEncryptedFilePath,
            previewEncryptedFilePath = previewEncryptedFilePath,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            importedAt = importedAt,
            contentHash = contentHash
        )
    }

    private fun validatePhotoForImport(
        sourceUri: Uri,
        displayName: String,
        mimeType: String
    ) {
        require(displayName.isNotBlank()) {
            context.getString(R.string.photo_error_invalid_name)
        }

        require(mimeType.startsWith("image/")) {
            context.getString(R.string.photo_error_invalid_type)
        }

        val sourceSizeBytes = resolveSourceSize(sourceUri)

        require(sourceSizeBytes != 0L) {
            context.getString(R.string.photo_error_empty)
        }

        if (sourceSizeBytes > 0L) {
            require(sourceSizeBytes <= MAX_PHOTO_SIZE_BYTES) {
                context.getString(R.string.photo_error_too_large)
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

    private fun calculateContentHash(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)

        return digest.joinToString(separator = "") { byte ->
            "%02x".format(byte)
        }
    }

    companion object {
        const val PHOTO_DIRECTORY = "encrypted_photos"
        private const val PHOTO_THUMBNAIL_DIRECTORY = "encrypted_photo_thumbnails"
        private const val PHOTO_PREVIEW_DIRECTORY = "encrypted_photo_previews"

        private const val MAX_PHOTO_SIZE_BYTES = 25L * 1024L * 1024L

        private const val THUMBNAIL_TARGET_SIZE_PX = 320
        private const val THUMBNAIL_QUALITY = 80

        private const val PREVIEW_TARGET_SIZE_PX = 1600
        private const val PREVIEW_QUALITY = 85
    }
}