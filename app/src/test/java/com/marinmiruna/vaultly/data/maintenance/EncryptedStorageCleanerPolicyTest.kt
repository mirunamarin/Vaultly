package com.marinmiruna.vaultly.data.maintenance

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptedStorageCleanerPolicyTest {

    @Test
    fun shouldDelete_returnsFalseForReferencedFile() {
        val directory = Files.createTempDirectory("vaultly-cleaner-test").toFile()
        val file = File(directory, "encrypted-file").apply {
            writeText("content")
        }

        val shouldDelete = EncryptedStorageCleanerPolicy.shouldDelete(
            file = file,
            referencedPaths = setOf(file.absolutePath),
            temporaryFileExtension = ".tmp"
        )

        assertFalse(shouldDelete)
        directory.deleteRecursively()
    }

    @Test
    fun shouldDelete_returnsFalseForTemporaryFile() {
        val directory = Files.createTempDirectory("vaultly-cleaner-test").toFile()
        val file = File(directory, "encrypted-file.tmp").apply {
            writeText("content")
        }

        val shouldDelete = EncryptedStorageCleanerPolicy.shouldDelete(
            file = file,
            referencedPaths = emptySet(),
            temporaryFileExtension = ".tmp"
        )

        assertFalse(shouldDelete)
        directory.deleteRecursively()
    }

    @Test
    fun shouldDelete_returnsTrueForUnreferencedFinalFile() {
        val directory = Files.createTempDirectory("vaultly-cleaner-test").toFile()
        val file = File(directory, "encrypted-file").apply {
            writeText("content")
        }

        val shouldDelete = EncryptedStorageCleanerPolicy.shouldDelete(
            file = file,
            referencedPaths = emptySet(),
            temporaryFileExtension = ".tmp"
        )

        assertTrue(shouldDelete)
        directory.deleteRecursively()
    }
}