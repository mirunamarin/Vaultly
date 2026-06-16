package com.marinmiruna.vaultly.ui.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.widthIn

@Composable
fun BiometricButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onAuthenticateClick: () -> Unit
) {
    Button(
        onClick = onAuthenticateClick,
        enabled = enabled,
        modifier = modifier
            .widthIn(min = 220.dp, max = 300.dp)
            .defaultMinSize(minHeight = 52.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 2.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}