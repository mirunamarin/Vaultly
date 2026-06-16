package com.marinmiruna.vaultly.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.marinmiruna.vaultly.data.db.dao.FileDao
import com.marinmiruna.vaultly.data.db.dao.NoteDao
import com.marinmiruna.vaultly.data.db.dao.PasswordDao
import com.marinmiruna.vaultly.data.db.dao.PhotoDao
import com.marinmiruna.vaultly.data.db.entity.FileEntity
import com.marinmiruna.vaultly.data.db.entity.NoteEntity
import com.marinmiruna.vaultly.data.db.entity.PasswordEntity
import com.marinmiruna.vaultly.data.db.entity.PhotoEntity

@Database(
    entities = [
        NoteEntity::class,
        PasswordEntity::class,
        PhotoEntity::class,
        FileEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class VaultlyDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun passwordDao(): PasswordDao
    abstract fun photoDao(): PhotoDao
    abstract fun fileDao(): FileDao
}
