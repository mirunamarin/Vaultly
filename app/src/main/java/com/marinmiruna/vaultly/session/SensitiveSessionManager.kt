package com.marinmiruna.vaultly.session

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensitiveSessionManager @Inject constructor() {

    private var passwordsAuthenticatedAt: Long = 0L
    private var filesAuthenticatedAt: Long = 0L
    private var photosAuthenticatedAt: Long = 0L

    fun markPasswordsAuthenticated() {
        passwordsAuthenticatedAt = SystemClock.elapsedRealtime()
    }

    fun markFilesAuthenticated() {
        filesAuthenticatedAt = SystemClock.elapsedRealtime()
    }

    fun markPhotosAuthenticated() {
        photosAuthenticatedAt = SystemClock.elapsedRealtime()
    }

    fun isPasswordsSessionValid(): Boolean {
        return isStillValid(passwordsAuthenticatedAt)
    }

    fun isFilesSessionValid(): Boolean {
        return isStillValid(filesAuthenticatedAt)
    }

    fun isPhotosSessionValid(): Boolean {
        return isStillValid(photosAuthenticatedAt)
    }

    fun clearSensitiveSessions() {
        passwordsAuthenticatedAt = 0L
        filesAuthenticatedAt = 0L
        photosAuthenticatedAt = 0L
    }

    private fun isStillValid(authenticatedAt: Long): Boolean {
        if (authenticatedAt == 0L) {
            return false
        }

        val elapsedMillis = SystemClock.elapsedRealtime() - authenticatedAt
        return elapsedMillis <= SESSION_TIMEOUT_MILLIS
    }

    companion object {
        private const val SESSION_TIMEOUT_MILLIS = 5 * 60 * 1000L
    }
}
