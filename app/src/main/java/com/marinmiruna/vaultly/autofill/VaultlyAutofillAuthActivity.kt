package com.marinmiruna.vaultly.autofill

import androidx.fragment.app.FragmentActivity
import android.os.Bundle
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.marinmiruna.vaultly.R
import com.marinmiruna.vaultly.data.repository.PasswordRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.content.Intent
import android.service.autofill.Dataset
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import kotlinx.coroutines.runBlocking

@Suppress("DEPRECATION")
@AndroidEntryPoint
class VaultlyAutofillAuthActivity : FragmentActivity() {

    @Inject
    lateinit var passwordRepository: PasswordRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authenticate()
    }

    private fun authenticate() {
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(
                this,
                getString(R.string.biometric_unavailable),
                Toast.LENGTH_SHORT
            ).show()
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val executor = ContextCompat.getMainExecutor(this)

        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    returnAutofillDataset()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    setResult(RESULT_CANCELED)
                    finish()
                }

            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.app_name))
            .setSubtitle(getString(R.string.autofill_auth_subtitle))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(promptInfo)
    }

    private fun returnAutofillDataset() {
        val passwordId = intent.getLongExtra(
            VaultlyAutofillService.EXTRA_PASSWORD_ID,
            0L
        )

        val requestId = intent.getLongExtra(
            VaultlyAutofillService.EXTRA_AUTOFILL_REQUEST_ID,
            0L
        )

        val autofillIds = VaultlyAutofillService.autofillIdStore[requestId]

        if (passwordId == 0L || autofillIds.isNullOrEmpty()) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val passwordEntry = runBlocking {
            passwordRepository.getPasswordEntryById(passwordId)
        }

        if (passwordEntry == null) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
            setTextViewText(
                android.R.id.text1,
                "${passwordEntry.service} - ${passwordEntry.username}"
            )
        }

        val datasetBuilder = Dataset.Builder(presentation)

        autofillIds.forEachIndexed { index, autofillId ->
            val value = if (index == 0) {
                passwordEntry.username
            } else {
                passwordEntry.password
            }

            datasetBuilder.setValue(
                autofillId,
                AutofillValue.forText(value),
                presentation
            )
        }

        val replyIntent = Intent().apply {
            putExtra(
                android.view.autofill.AutofillManager.EXTRA_AUTHENTICATION_RESULT,
                datasetBuilder.build()
            )
        }

        setResult(RESULT_OK, replyIntent)
        finish()
    }
}