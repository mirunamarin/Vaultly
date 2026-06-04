package com.marinmiruna.vaultly.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "passwords")
data class PasswordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val service: String,
    val username: String,
    val encryptedPasswordIv: ByteArray,
    val encryptedPasswordCipherText: ByteArray,
    val url: String,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long
)
