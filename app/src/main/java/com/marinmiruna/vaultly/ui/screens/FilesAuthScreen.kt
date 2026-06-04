package com.marinmiruna.vaultly.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marinmiruna.vaultly.ui.components.AuthGate
import com.marinmiruna.vaultly.viewmodel.FilesViewModel
import androidx.compose.ui.res.stringResource
import com.marinmiruna.vaultly.R

@Composable
fun FilesAuthScreen(
    onAuthenticateClick: () -> Unit,
    onAuthenticated: () -> Unit,
    viewModel: FilesViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshAuthState()
    }

    LaunchedEffect(authState.isAuthenticated) {
        if (authState.isAuthenticated) {
            onAuthenticated()
        }
    }

    AuthGate(
        isAuthenticated = authState.isAuthenticated,
        title = stringResource(R.string.files_auth_title),
        message = stringResource(R.string.files_auth_message),
        buttonText = stringResource(R.string.files_auth_button),
        onAuthenticateClick = onAuthenticateClick
    ) {
        Unit
    }
}
