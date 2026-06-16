package com.marinmiruna.vaultly.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.marinmiruna.vaultly.data.db.entity.FileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {

    @Query(
        """
        SELECT * FROM files
        WHERE displayName LIKE '%' || :query || '%'
           OR mimeType LIKE '%' || :query || '%'
        ORDER BY importedAt DESC
        """
    )
    fun observeFiles(query: String): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE id = :id LIMIT 1")
    fun observeFile(id: Long): Flow<FileEntity?>

    @Query("SELECT * FROM files ORDER BY importedAt DESC")
    suspend fun getAllFiles(): List<FileEntity>

    @Query("SELECT encryptedFilePath FROM files")
    suspend fun getAllEncryptedFilePaths(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM files WHERE contentHash = :contentHash)")
    suspend fun existsByContentHash(contentHash: String): Boolean

    @Upsert
    suspend fun upsert(file: FileEntity): Long

    @Delete
    suspend fun delete(file: FileEntity)
}
