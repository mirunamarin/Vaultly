package com.marinmiruna.vaultly.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.marinmiruna.vaultly.data.db.entity.PasswordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {

    @Query(
        """
        SELECT * FROM passwords
        WHERE service LIKE '%' || :query || '%'
           OR username LIKE '%' || :query || '%'
           OR url LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
        """
    )
    fun observePasswords(query: String): Flow<List<PasswordEntity>>

    @Query("SELECT * FROM passwords ORDER BY updatedAt DESC")
    fun observeAllPasswords(): Flow<List<PasswordEntity>>

    @Query("SELECT * FROM passwords ORDER BY updatedAt DESC")
    suspend fun getAllPasswords(): List<PasswordEntity>

    @Query("SELECT * FROM passwords WHERE id = :id LIMIT 1")
    fun observePassword(id: Long): Flow<PasswordEntity?>

    @Query("SELECT * FROM passwords WHERE id = :id LIMIT 1")
    suspend fun getPasswordById(id: Long): PasswordEntity?

    @Upsert
    suspend fun upsert(password: PasswordEntity): Long

    @Delete
    suspend fun delete(password: PasswordEntity)
}
