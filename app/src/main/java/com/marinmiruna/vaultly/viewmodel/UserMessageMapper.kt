package com.marinmiruna.vaultly.viewmodel

import android.content.Context
import com.marinmiruna.vaultly.R

object UserMessageMapper {

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
}