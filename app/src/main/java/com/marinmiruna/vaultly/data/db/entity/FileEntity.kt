package com.marinmiruna.vaultly.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val displayName: String,
    val encryptedFilePath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val importedAt: Long
)
