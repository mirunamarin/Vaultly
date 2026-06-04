package com.marinmiruna.vaultly.di

import android.content.Context
import androidx.room.Room
import com.marinmiruna.vaultly.data.crypto.CryptoManager
import com.marinmiruna.vaultly.data.db.VaultlyDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideVaultlyDatabase(
        @ApplicationContext context: Context,
        cryptoManager: CryptoManager
    ): VaultlyDatabase {
        System.loadLibrary("sqlcipher")

        val passphrase = cryptoManager.getOrCreateDatabasePassphrase()

        return Room.databaseBuilder(
            context,
            VaultlyDatabase::class.java,
            DATABASE_NAME
        )
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideNoteDao(database: VaultlyDatabase) = database.noteDao()

    @Provides
    fun providePasswordDao(database: VaultlyDatabase) = database.passwordDao()

    @Provides
    fun providePhotoDao(database: VaultlyDatabase) = database.photoDao()

    @Provides
    fun provideFileDao(database: VaultlyDatabase) = database.fileDao()

    private const val DATABASE_NAME = "vaultly_secure.db"
}
