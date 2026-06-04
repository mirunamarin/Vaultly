package com.marinmiruna.vaultly.data.maintenance

import java.io.File

object EncryptedStorageCleanerPolicy {

    fun shouldDelete(
        file: File,
        referencedPaths: Set<String>,
        temporaryFileExtension: String
    ): Boolean {
        return file.isFile &&
                !file.name.endsWith(temporaryFileExtension) &&
                file.absolutePath !in referencedPaths
    }
}