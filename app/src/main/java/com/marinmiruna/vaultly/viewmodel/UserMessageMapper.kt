package com.marinmiruna.vaultly.viewmodel

import android.content.Context
import com.marinmiruna.vaultly.R

object UserMessageMapper {

    fun fileOperationMessage(exception: Throwable): String {
        return when (exception) {
            is SecurityException -> exception.message ?: DEFAULT_SECURE_ACCESS_EXPIRED_MESSAGE
            is IllegalArgumentException -> exception.message ?: DEFAULT_INVALID_SELECTED_FILE_MESSAGE
            else -> DEFAULT_FILE_OPERATION_FAILED_MESSAGE
        }
    }

    fun photoOperationMessage(exception: Throwable): String {
        return when (exception) {
            is SecurityException -> exception.message ?: DEFAULT_SECURE_ACCESS_EXPIRED_MESSAGE
            is IllegalArgumentException -> exception.message ?: DEFAULT_INVALID_SELECTED_PHOTO_MESSAGE
            else -> DEFAULT_PHOTO_OPERATION_FAILED_MESSAGE
        }
    }

    fun noteOperationMessage(exception: Throwable): String {
        return when (exception) {
            is SecurityException -> exception.message ?: DEFAULT_SECURE_ACCESS_EXPIRED_MESSAGE
            else -> DEFAULT_NOTE_OPERATION_FAILED_MESSAGE
        }
    }

    fun passwordOperationMessage(exception: Throwable): String {
        return when (exception) {
            is SecurityException -> exception.message ?: DEFAULT_SECURE_ACCESS_EXPIRED_MESSAGE
            else -> DEFAULT_PASSWORD_OPERATION_FAILED_MESSAGE
        }
    }

    fun fileOperationMessage(
        context: Context,
        exception: Throwable
    ): String {
        return when (exception) {
            is SecurityException -> {
                exception.message ?: context.getString(R.string.error_secure_access_expired)
            }
            is IllegalArgumentException -> {
                exception.message ?: context.getString(R.string.error_invalid_selected_file)
            }
            else -> {
                context.getString(R.string.error_file_operation_failed)
            }
        }
    }

    fun photoOperationMessage(
        context: Context,
        exception: Throwable
    ): String {
        return when (exception) {
            is SecurityException -> {
                exception.message ?: context.getString(R.string.error_secure_access_expired)
            }
            is IllegalArgumentException -> {
                exception.message ?: context.getString(R.string.error_invalid_selected_photo)
            }
            else -> {
                context.getString(R.string.error_photo_operation_failed)
            }
        }
    }

    fun noteOperationMessage(
        context: Context,
        exception: Throwable
    ): String {
        return when (exception) {
            is SecurityException -> {
                exception.message ?: context.getString(R.string.error_secure_access_expired)
            }
            else -> {
                context.getString(R.string.error_note_operation_failed)
            }
        }
    }

    fun passwordOperationMessage(
        context: Context,
        exception: Throwable
    ): String {
        return when (exception) {
            is SecurityException -> {
                exception.message ?: context.getString(R.string.error_secure_access_expired)
            }
            else -> {
                context.getString(R.string.error_password_operation_failed)
            }
        }
    }

    private const val DEFAULT_SECURE_ACCESS_EXPIRED_MESSAGE =
        "Accesul securizat a expirat. Autentifică-te din nou."
    private const val DEFAULT_INVALID_SELECTED_FILE_MESSAGE =
        "Fișierul selectat nu este valid."
    private const val DEFAULT_FILE_OPERATION_FAILED_MESSAGE =
        "Operația cu fișierul nu a putut fi finalizată."
    private const val DEFAULT_INVALID_SELECTED_PHOTO_MESSAGE =
        "Fotografia selectată nu este validă."
    private const val DEFAULT_PHOTO_OPERATION_FAILED_MESSAGE =
        "Operația cu fotografia nu a putut fi finalizată."
    private const val DEFAULT_NOTE_OPERATION_FAILED_MESSAGE =
        "Operația cu notița nu a putut fi finalizată."
    private const val DEFAULT_PASSWORD_OPERATION_FAILED_MESSAGE =
        "Operația nu a putut fi finalizată."
}