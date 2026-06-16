package com.marinmiruna.vaultly.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marinmiruna.vaultly.R
import com.marinmiruna.vaultly.domain.model.PasswordListItem
import com.marinmiruna.vaultly.domain.security.PasswordSecurityReport
import com.marinmiruna.vaultly.ui.components.ScreenHeader
import com.marinmiruna.vaultly.viewmodel.PasswordsViewModel
import com.marinmiruna.vaultly.ui.components.VaultlyTextField

@Composable
fun PasswordsListScreen(
    onOpenPassword: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: PasswordsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (!viewModel.isPasswordSessionValid()) {
            onBack()
        }
    }

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onOpenPassword(0L) },
                shape = RoundedCornerShape(8.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                ScreenHeader(
                    title = stringResource(R.string.passwords_title),
                    onBack = onBack
                )
                VaultlyTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    label = stringResource(R.string.passwords_search_label),
                    modifier = Modifier.padding(top = 20.dp)
                )
                if (uiState.passwords.isEmpty()) {
                    EmptyPasswordsState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 20.dp),
                        contentPadding = PaddingValues(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.passwords,
                            key = { password -> password.id }
                        ) { password ->
                            PasswordCard(
                                password = password,
                                securityReport = uiState.securityReports[password.id],
                                onClick = { onOpenPassword(password.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordCard(
    password: PasswordListItem,
    securityReport: PasswordSecurityReport?,
    onClick: () -> Unit
) {
    val serviceIdentity = rememberServiceIdentity(
        service = password.service,
        url = password.url
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PasswordServiceBadge(
                identity = serviceIdentity,
                modifier = Modifier.size(52.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = password.service,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = password.username,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (securityReport?.isWeak == true || securityReport?.isReused == true) {
                SecurityIssueBadge()
                Spacer(modifier = Modifier.width(2.dp))
            }

            Text(
                text = ">",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            thickness = 1.dp
        )
    }
}

@Composable
private fun SecurityIssueBadge() {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "!",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PasswordServiceBadge(
    identity: ServiceIdentity,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = identity.fallbackLetter,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
            textAlign = TextAlign.Center
        )
    }
}

private fun rememberServiceIdentity(
    service: String,
    url: String
): ServiceIdentity {
    val fallbackLetter = service
        .trim()
        .firstOrNull()
        ?.uppercaseChar()
        ?.toString()
        ?: "#"

    return ServiceIdentity(
        iconType = ServiceIconType.Generic,
        fallbackLetter = fallbackLetter
    )
}

@Composable
private fun passwordSecurityStatusText(
    securityReport: PasswordSecurityReport?
): String {
    return when {
        securityReport == null -> stringResource(R.string.password_security_ok)
        securityReport.isWeak && securityReport.isReused -> {
            stringResource(R.string.password_security_weak_reused)
        }
        securityReport.isWeak -> stringResource(R.string.password_security_weak)
        securityReport.isReused -> stringResource(R.string.password_security_reused)
        else -> stringResource(R.string.password_security_ok)
    }
}

@Composable
private fun passwordSecurityStatusColor(
    securityReport: PasswordSecurityReport?
) = when {
    securityReport == null -> MaterialTheme.colorScheme.primary
    securityReport.isWeak || securityReport.isReused -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.primary
}

@Composable
private fun EmptyPasswordsState(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.passwords_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.passwords_empty_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Immutable
private data class ServiceIdentity(
    val iconType: ServiceIconType,
    val fallbackLetter: String
)

private enum class ServiceIconType {
    Generic
}