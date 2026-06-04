package com.marinmiruna.vaultly.data.repository

import com.marinmiruna.vaultly.data.crypto.CryptoManager
import com.marinmiruna.vaultly.data.crypto.EncryptedPayload
import com.marinmiruna.vaultly.data.db.dao.PasswordDao
import com.marinmiruna.vaultly.data.db.entity.PasswordEntity
import com.marinmiruna.vaultly.domain.model.PasswordEntry
import com.marinmiruna.vaultly.domain.model.PasswordListItem
import com.marinmiruna.vaultly.domain.security.PasswordGenerator
import com.marinmiruna.vaultly.domain.security.PasswordSecurityAnalyzer
import com.marinmiruna.vaultly.domain.security.PasswordSecurityInput
import com.marinmiruna.vaultly.domain.security.PasswordSecurityReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class PasswordRepository @Inject constructor(
    private val passwordDao: PasswordDao,
    private val cryptoManager: CryptoManager
) {

    fun observePasswords(query: String): Flow<List<PasswordListItem>> {
        return passwordDao.observePasswords(query).map { passwords ->
            passwords.map { entity -> entity.toListItem() }
        }
    }

    fun observePassword(id: Long): Flow<PasswordEntry?> {
        return passwordDao.observePassword(id).map { entity ->
            entity?.toDomain()
        }
    }

    fun observePasswordSecurityInputs(): Flow<List<PasswordSecurityInput>> {
        return passwordDao.observeAllPasswords().map { passwordEntities ->
            passwordEntities.map { entity ->
                PasswordSecurityInput(
                    id = entity.id,
                    password = decryptPassword(entity)
                )
            }
        }
    }

    fun observePasswordSecurityReports(): Flow<Map<Long, PasswordSecurityReport>> {
        return observePasswordSecurityInputs().map { securityInputs ->
            PasswordSecurityAnalyzer.analyzePasswords(securityInputs)
        }
    }

    suspend fun savePassword(
        id: Long,
        service: String,
        username: String,
        password: String,
        url: String,
        note: String
    ) {
        val now = System.currentTimeMillis()
        val existingPassword = if (id == 0L) null else passwordDao.getPasswordById(id)
        val encryptedPassword = cryptoManager.encryptText(password)

        passwordDao.upsert(
            PasswordEntity(
                id = id,
                service = service.trim(),
                username = username.trim(),
                encryptedPasswordIv = encryptedPassword.iv,
                encryptedPasswordCipherText = encryptedPassword.cipherText,
                url = url.trim(),
                note = note.trim(),
                createdAt = existingPassword?.createdAt ?: now,
                updatedAt = now
            )
        )
    }

    suspend fun deletePassword(passwordEntry: PasswordEntry) {
        val existingPassword = passwordDao.getPasswordById(passwordEntry.id) ?: return
        passwordDao.delete(existingPassword)
    }

    fun generatePassword(
        length: Int,
        includeUppercase: Boolean,
        includeDigits: Boolean,
        includeSymbols: Boolean
    ): String {
        return PasswordGenerator.generate(
            length = length,
            includeUppercase = includeUppercase,
            includeDigits = includeDigits,
            includeSymbols = includeSymbols
        )
    }

    private fun decryptPassword(entity: PasswordEntity): String {
        return cryptoManager.decryptText(
            EncryptedPayload(
                iv = entity.encryptedPasswordIv,
                cipherText = entity.encryptedPasswordCipherText
            )
        )
    }

    private fun PasswordEntity.toListItem(): PasswordListItem {
        return PasswordListItem(
            id = id,
            service = service,
            username = username,
            url = url,
            updatedAt = updatedAt
        )
    }

    private fun PasswordEntity.toDomain(): PasswordEntry {
        return PasswordEntry(
            id = id,
            service = service,
            username = username,
            password = decryptPassword(this),
            url = url,
            note = note,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        const val MIN_PASSWORD_LENGTH = PasswordGenerator.MIN_PASSWORD_LENGTH
        const val MAX_PASSWORD_LENGTH = PasswordGenerator.MAX_PASSWORD_LENGTH
    }
}