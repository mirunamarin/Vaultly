package com.marinmiruna.vaultly.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.marinmiruna.vaultly.data.db.entity.PhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {

    @Query("SELECT * FROM photos ORDER BY importedAt DESC")
    fun observePhotos(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE id = :id LIMIT 1")
    fun observePhoto(id: Long): Flow<PhotoEntity?>

    @Query("SELECT * FROM photos ORDER BY importedAt DESC")
    suspend fun getAllPhotos(): List<PhotoEntity>

    @Query("SELECT encryptedFilePath FROM photos")
    suspend fun getAllEncryptedFilePaths(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM photos WHERE contentHash = :contentHash)")
    suspend fun existsByContentHash(contentHash: String): Boolean

    @Upsert
    suspend fun upsert(photo: PhotoEntity): Long

    @Delete
    suspend fun delete(photo: PhotoEntity)
}
