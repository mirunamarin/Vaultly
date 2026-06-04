package com.marinmiruna.vaultly.data.export

data class VaultExportEnvelope(
    val version: Int,
    val createdAt: Long,
    val salt: ByteArray,
    val iv: ByteArray,
    val cipherText: ByteArray
)

data class VaultExportPayload(
    val version: Int,
    val createdAt: Long,
    val notes: List<VaultExportNote>,
    val passwords: List<VaultExportPassword>,
    val files: List<VaultExportFile>,
    val photos: List<VaultExportPhoto>
)

data class VaultExportNote(
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
)

data class VaultExportPassword(
    val service: String,
    val username: String,
    val password: String,
    val url: String,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long
)

data class VaultExportFile(
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val importedAt: Long,
    val contentBase64: String
)

data class VaultExportPhoto(
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val importedAt: Long,
    val contentBase64: String
)