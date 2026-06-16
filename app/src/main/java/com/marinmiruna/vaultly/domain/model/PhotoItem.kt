package com.marinmiruna.vaultly.domain.model

data class PhotoItem(
    val id: Long = 0,
    val displayName: String,
    val encryptedFilePath: String,
    val thumbnailEncryptedFilePath: String,
    val previewEncryptedFilePath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val importedAt: Long,
    val contentHash: String
)