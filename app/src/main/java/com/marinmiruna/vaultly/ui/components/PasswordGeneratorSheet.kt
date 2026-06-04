package com.marinmiruna.vaultly.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.compose.ui.res.stringResource
import com.marinmiruna.vaultly.R
import com.marinmiruna.vaultly.ui.theme.JetBrainsMono

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordGeneratorSheet(
    generatedPassword: String,
    length: Int,
    includeUppercase: Boolean,
    includeDigits: Boolean,
    includeSymbols: Boolean,
    onLengthChange: (Int) -> Unit,
    onIncludeUppercaseChange: (Boolean) -> Unit,
    onIncludeDigitsChange: (Boolean) -> Unit,
    onIncludeSymbolsChange: (Boolean) -> Unit,
    onGenerate: () -> Unit,
    onUsePassword: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.password_generator_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = stringResource(R.string.password_generator_length_format, length),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Slider(
                value = length.toFloat(),
                onValueChange = { value ->
                    onLengthChange(value.roundToInt().coerceIn(8, 32))
                },
                valueRange = 8f..32f,
                steps = 23
            )

            GeneratorOptionRow(
                text = stringResource(R.string.password_generator_uppercase),
                checked = includeUppercase,
                onCheckedChange = onIncludeUppercaseChange
            )

            GeneratorOptionRow(
                text = stringResource(R.string.password_generator_digits),
                checked = includeDigits,
                onCheckedChange = onIncludeDigitsChange
            )

            GeneratorOptionRow(
                text = stringResource(R.string.password_generator_symbols),
                checked = includeSymbols,
                onCheckedChange = onIncludeSymbolsChange
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline
                )
            ) {
                SelectionContainer {
                    Text(
                        text = generatedPassword.ifBlank {
                            stringResource(R.string.password_generator_placeholder)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = JetBrainsMono
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Button(
                onClick = onGenerate,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.password_generator_generate_button))
            }

            Button(
                onClick = onUsePassword,
                modifier = Modifier.fillMaxWidth(),
                enabled = generatedPassword.isNotBlank()
            ) {
                Text(text = stringResource(R.string.password_generator_use_button))
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.password_generator_close_button))
            }
        }
    }
}

@Composable
private fun GeneratorOptionRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}