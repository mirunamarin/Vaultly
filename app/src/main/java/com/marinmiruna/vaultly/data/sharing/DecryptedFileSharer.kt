package com.marinmiruna.vaultly.data.sharing

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.marinmiruna.vaultly.domain.model.FileItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class DecryptedFileSharer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun createShareUri(
        fileItem: FileItem,
        writeDecryptedFile: suspend (File) -> Unit
    ): Uri = withContext(Dispatchers.IO) {
        clearExpiredTemporaryDecryptedFiles()

        val cacheDirectory = File(context.cacheDir, DECRYPTED_CACHE_DIRECTORY).apply {
            if (!exists()) {
                mkdirs()
            }
        }

        val safeExtension = fileItem.displayName.substringAfterLast('.', missingDelimiterValue = "")
            .takeIf { it.isNotBlank() && it.length <= 12 }
            ?.let { ".$it" }
            ?: ""

        val outputFile = File(cacheDirectory, "${UUID.randomUUID()}$safeExtension")

        runCatching {
            writeDecryptedFile(outputFile)

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )
        }.getOrElse { exception ->
            outputFile.delete()
            throw exception
        }
    }

    private fun clearExpiredTemporaryDecryptedFiles() {
        val cacheDirectory = File(context.cacheDir, DECRYPTED_CACHE_DIRECTORY)
        if (!cacheDirectory.exists() || !cacheDirectory.isDirectory) {
            return
        }

        val expirationThreshold = System.currentTimeMillis() - TEMPORARY_FILE_MAX_AGE_MILLIS

        cacheDirectory.listFiles()?.forEach { file ->
            runCatching {
                if (file.isFile && file.lastModified() < expirationThreshold) {
                    file.delete()
                }
            }
        }
    }

    fun clearTemporaryDecryptedFiles() {
        val cacheDirectory = File(context.cacheDir, DECRYPTED_CACHE_DIRECTORY)
        if (!cacheDirectory.exists() || !cacheDirectory.isDirectory) {
            return
        }

        cacheDirectory.listFiles()?.forEach { file ->
            runCatching {
                if (file.isFile) {
                    file.delete()
                }
            }
        }
    }

    companion object {
        const val DECRYPTED_CACHE_DIRECTORY = "decrypted_share_cache"
        private const val TEMPORARY_FILE_MAX_AGE_MILLIS = 10 * 60 * 1000L
    }
}