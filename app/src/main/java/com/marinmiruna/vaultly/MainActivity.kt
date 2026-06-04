package com.marinmiruna.vaultly

import java.io.File
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.marinmiruna.vaultly.session.SensitiveSessionManager
import com.marinmiruna.vaultly.ui.screens.LockScreen
import com.marinmiruna.vaultly.ui.screens.UnlockedScreen
import com.marinmiruna.vaultly.ui.theme.VaultlyTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import com.marinmiruna.vaultly.ui.theme.VaultlyBackground
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.marinmiruna.vaultly.data.db.VaultlyDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Provider
import com.marinmiruna.vaultly.data.crypto.VaultDatabaseKeyException
import com.marinmiruna.vaultly.data.sharing.DecryptedFileSharer
import com.marinmiruna.vaultly.data.maintenance.EncryptedStorageCleaner
import com.marinmiruna.vaultly.data.security.SecureClipboardManager

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var sensitiveSessionManager: SensitiveSessionManager
    @Inject
    lateinit var vaultlyDatabaseProvider: Provider<VaultlyDatabase>
    @Inject
    lateinit var encryptedStorageCleanerProvider: Provider<EncryptedStorageCleaner>
    @Inject
    lateinit var secureClipboardManager: SecureClipboardManager

    private var isUnlocked by mutableStateOf(false)
    private var statusMessage by mutableStateOf("")

    private var unlockedAt: Long = 0L
    private var isStartingTrustedSystemActivity = false

    private val relockHandler = Handler(Looper.getMainLooper())
    private val relockRunnable = Runnable {
        lockVault(getString(R.string.vault_session_expired))
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        statusMessage = getString(R.string.vault_initial_status)
        clearTemporaryDecryptedFiles()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(VaultlyBackground.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(VaultlyBackground.toArgb())
        )

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContent {
            VaultlyTheme {
                if (isUnlocked) {
                    UnlockedScreen(
                        onTrustedSystemActivityStarted = {
                            markTrustedSystemActivityStarted()
                        },
                        isPasswordsSessionValid = {
                            sensitiveSessionManager.isPasswordsSessionValid()
                        },
                        onPasswordsAuthRequested = { onSuccess ->
                            authenticateForSensitiveSection(
                                subtitle = getString(R.string.biometric_subtitle_passwords),
                                onSuccess = {
                                    sensitiveSessionManager.markPasswordsAuthenticated()
                                    onSuccess()
                                }
                            )
                        },
                        onFilesAuthRequested = { onSuccess ->
                            authenticateForSensitiveSection(
                                subtitle = getString(R.string.biometric_subtitle_files),
                                onSuccess = {
                                    sensitiveSessionManager.markFilesAuthenticated()
                                    onSuccess()
                                }
                            )
                        },
                        onPhotosAuthRequested = { onSuccess ->
                            authenticateForSensitiveSection(
                                subtitle = getString(R.string.biometric_subtitle_photos),
                                onSuccess = {
                                    sensitiveSessionManager.markPhotosAuthenticated()
                                    onSuccess()
                                }
                            )
                        },
                        onExportAuthRequested = { onSuccess ->
                            authenticateForSensitiveSection(
                                subtitle = getString(R.string.biometric_subtitle_export),
                                onSuccess = onSuccess
                            )
                        }
                    )
                } else {
                    LockScreen(
                        statusMessage = statusMessage,
                        onUnlockClick = {
                            authenticateForInitialUnlock()
                        }
                    )
                }
            }
        }
    }


    override fun onStart() {
        super.onStart()

        if (isUnlocked && unlockedAt > 0L) {
            val elapsedMillis = SystemClock.elapsedRealtime() - unlockedAt
            if (elapsedMillis >= APP_RELOCK_TIMEOUT_MILLIS) {
                lockVault(getString(R.string.vault_session_expired))
            } else {
                scheduleRelock(APP_RELOCK_TIMEOUT_MILLIS - elapsedMillis)
            }
        }
    }

    override fun onStop() {
        super.onStop()

        if (!isUnlocked) {
            return
        }

        if (isStartingTrustedSystemActivity) {
            isStartingTrustedSystemActivity = false
            return
        }

        lockVault(getString(R.string.vault_background_locked))
    }

    override fun onUserInteraction() {
        super.onUserInteraction()

        if (isUnlocked) {
            resetVaultInactivityTimer()
        }
    }

    private fun authenticateForInitialUnlock() {
        authenticateWithBiometrics(
            subtitle = getString(R.string.biometric_subtitle_initial_unlock),
            negativeButtonText = getString(R.string.biometric_negative_exit),
            closeAppOnNegativeButton = true,
            onSuccess = {
                unlockVault()
            }
        )
    }

    private fun authenticateForSensitiveSection(
        subtitle: String,
        onSuccess: () -> Unit
    ) {
        authenticateWithBiometrics(
            subtitle = subtitle,
            negativeButtonText = getString(R.string.biometric_negative_cancel),
            closeAppOnNegativeButton = false,
            onSuccess = onSuccess
        )
    }

    private fun authenticateWithBiometrics(
        subtitle: String,
        negativeButtonText: String,
        closeAppOnNegativeButton: Boolean,
        onSuccess: () -> Unit
    ) {
        val biometricManager = BiometricManager.from(this)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG

        when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                showBiometricPrompt(
                    subtitle = subtitle,
                    negativeButtonText = negativeButtonText,
                    closeAppOnNegativeButton = closeAppOnNegativeButton,
                    onSuccess = onSuccess
                )
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                statusMessage = getString(R.string.biometric_no_hardware)
            }

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                statusMessage = getString(R.string.biometric_hw_unavailable)
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                statusMessage = getString(R.string.biometric_none_enrolled)
            }

            else -> {
                statusMessage = getString(R.string.biometric_unavailable)
            }
        }
    }

    private fun showBiometricPrompt(
        subtitle: String,
        negativeButtonText: String,
        closeAppOnNegativeButton: Boolean,
        onSuccess: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.biometric_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)

                    if (
                        closeAppOnNegativeButton &&
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        finish()
                        return
                    }

                    statusMessage = errString.toString()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.app_name))
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
    private fun unlockVault() {
        isUnlocked = true
        statusMessage = getString(R.string.auth_success)
        resetVaultInactivityTimer()
        prewarmDatabase()
    }

    private fun resetVaultInactivityTimer() {
        unlockedAt = SystemClock.elapsedRealtime()
        scheduleRelock(APP_RELOCK_TIMEOUT_MILLIS)
    }

    private fun lockVault(message: String) {
        isUnlocked = false
        unlockedAt = 0L
        relockHandler.removeCallbacks(relockRunnable)
        sensitiveSessionManager.clearSensitiveSessions()
        secureClipboardManager.clear()
        statusMessage = message
    }

    private fun scheduleRelock(delayMillis: Long) {
        relockHandler.removeCallbacks(relockRunnable)
        relockHandler.postDelayed(relockRunnable, delayMillis.coerceAtLeast(0L))
    }

    private fun markTrustedSystemActivityStarted() {
        isStartingTrustedSystemActivity = true
    }

    private fun prewarmDatabase() {
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                vaultlyDatabaseProvider.get().openHelper.writableDatabase
                encryptedStorageCleanerProvider.get().cleanOrphanedEncryptedFiles()
            }.onFailure { exception ->
                Log.w(TAG, "Database prewarm failed.", exception)

                val userMessage = when (exception) {
                    is VaultDatabaseKeyException -> {
                        exception.message ?: getString(R.string.database_key_unavailable)
                    }
                    else -> {
                        getString(R.string.database_open_failed)
                    }
                }

                launch(Dispatchers.Main) {
                    lockVault(userMessage)
                }
            }
        }
    }

    override fun onDestroy() {
        relockHandler.removeCallbacks(relockRunnable)
        super.onDestroy()
    }

    private fun clearTemporaryDecryptedFiles() {
        val cacheDirectory = File(cacheDir, DecryptedFileSharer.DECRYPTED_CACHE_DIRECTORY)
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
        private const val TAG = "VaultlyMainActivity"
        private const val APP_RELOCK_TIMEOUT_MILLIS = 30_000L
    }
}
