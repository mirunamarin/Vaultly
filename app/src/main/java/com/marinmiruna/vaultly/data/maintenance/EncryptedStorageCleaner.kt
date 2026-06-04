package com.marinmiruna.vaultly.data.maintenance

import android.content.Context
import com.marinmiruna.vaultly.data.crypto.FileEncryptor
import com.marinmiruna.vaultly.data.db.dao.FileDao
import com.marinmiruna.vaultly.data.db.dao.PhotoDao
import com.marinmiruna.vaultly.data.repository.FileRepository
import com.marinmiruna.vaultly.data.repository.PhotoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class EncryptedStorageCleaner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileDao: FileDao,
    private val photoDao: PhotoDao,
    private val fileEncryptor: FileEncryptor
) {

    suspend fun cleanOrphanedEncryptedFiles() = withContext(Dispatchers.IO) {
        cleanDirectory(
            directoryName = FileRepository.FILE_DIRECTORY,
            referencedPaths = fileDao.getAllEncryptedFilePaths().toSet()
        )

        cleanDirectory(
            directoryName = PhotoRepository.PHOTO_DIRECTORY,
            referencedPaths = photoDao.getAllEncryptedFilePaths().toSet()
        )
    }

    private suspend fun cleanDirectory(
        directoryName: String,
        referencedPaths: Set<String>
    ) {
        val directory = File(context.filesDir, directoryName)
        if (!directory.exists() || !directory.isDirectory) {
            return
        }

        directory.listFiles()
            ?.filter { file ->
                EncryptedStorageCleanerPolicy.shouldDelete(
                    file = file,
                    referencedPaths = referencedPaths,
                    temporaryFileExtension = FileEncryptor.TEMPORARY_FILE_EXTENSION
                )
            }
            ?.forEach { orphanedFile ->
                runCatching {
                    fileEncryptor.securelyDelete(orphanedFile.absolutePath)
                }.onFailure {
                    orphanedFile.delete()
                }
            }
    }
}