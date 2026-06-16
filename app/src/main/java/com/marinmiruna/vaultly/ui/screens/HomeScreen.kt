package com.marinmiruna.vaultly.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.marinmiruna.vaultly.R
import com.marinmiruna.vaultly.ui.components.HeaderButton

@Composable
fun HomeScreen(
    onOpenNotes: () -> Unit,
    onOpenPasswords: () -> Unit,
    onOpenPhotos: () -> Unit,
    onOpenFiles: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFaceRecognitionDemo: () -> Unit
) {
    val modules = listOf(
        VaultModule(
            title = stringResource(R.string.home_module_notes_title),
            description = stringResource(R.string.home_module_notes_description),
            type = VaultModuleType.Notes,
            onClick = onOpenNotes
        ),
        VaultModule(
            title = stringResource(R.string.home_module_passwords_title),
            description = stringResource(R.string.home_module_passwords_description),
            type = VaultModuleType.Passwords,
            onClick = onOpenPasswords
        ),
        VaultModule(
            title = stringResource(R.string.home_module_photos_title),
            description = stringResource(R.string.home_module_photos_description),
            type = VaultModuleType.Photos,
            onClick = onOpenPhotos
        ),
        VaultModule(
            title = stringResource(R.string.home_module_files_title),
            description = stringResource(R.string.home_module_files_description),
            type = VaultModuleType.Files,
            onClick = onOpenFiles
        ),
        VaultModule(
            title = stringResource(R.string.face_demo_module_title),
            description = stringResource(R.string.face_demo_module_description),
            type = VaultModuleType.FaceDemo,
            onClick = onOpenFaceRecognitionDemo
        )
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = stringResource(R.string.home_subtitle),
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HeaderButton(
                    text = stringResource(R.string.home_settings_button),
                    onClick = onOpenSettings
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.home_modules_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(14.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(490.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(modules) { module ->
                    VaultModuleCard(module = module)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_protection_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = stringResource(R.string.home_protection_body),
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun VaultModuleCard(
    module: VaultModule
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(154.dp)
            .clickable(onClick = module.onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                ModuleIcon(
                    type = module.type,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = module.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = module.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ModuleIcon(
    type: VaultModuleType,
    modifier: Modifier = Modifier
) {
    val iconColor = MaterialTheme.colorScheme.onPrimary

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.09f

        when (type) {
            VaultModuleType.Notes -> {
                drawRoundRect(
                    color = iconColor,
                    size = Size(w * 0.72f, h * 0.82f),
                    topLeft = Offset(w * 0.14f, h * 0.08f),
                    cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
                    style = Stroke(width = strokeWidth)
                )
                drawLine(
                    color = iconColor,
                    start = Offset(w * 0.28f, h * 0.34f),
                    end = Offset(w * 0.72f, h * 0.34f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = iconColor,
                    start = Offset(w * 0.28f, h * 0.52f),
                    end = Offset(w * 0.64f, h * 0.52f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            VaultModuleType.Passwords -> {
                drawCircle(
                    color = iconColor,
                    radius = w * 0.2f,
                    center = Offset(w * 0.34f, h * 0.42f),
                    style = Stroke(width = strokeWidth)
                )
                drawLine(
                    color = iconColor,
                    start = Offset(w * 0.48f, h * 0.52f),
                    end = Offset(w * 0.82f, h * 0.76f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = iconColor,
                    start = Offset(w * 0.67f, h * 0.66f),
                    end = Offset(w * 0.67f, h * 0.82f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = iconColor,
                    start = Offset(w * 0.78f, h * 0.74f),
                    end = Offset(w * 0.78f, h * 0.88f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            VaultModuleType.Photos -> {
                drawRoundRect(
                    color = iconColor,
                    size = Size(w * 0.82f, h * 0.68f),
                    topLeft = Offset(w * 0.09f, h * 0.16f),
                    cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
                    style = Stroke(width = strokeWidth)
                )
                drawCircle(
                    color = iconColor,
                    radius = w * 0.09f,
                    center = Offset(w * 0.68f, h * 0.34f),
                    style = Stroke(width = strokeWidth)
                )
                drawLine(
                    color = iconColor,
                    start = Offset(w * 0.18f, h * 0.74f),
                    end = Offset(w * 0.42f, h * 0.54f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = iconColor,
                    start = Offset(w * 0.42f, h * 0.54f),
                    end = Offset(w * 0.62f, h * 0.72f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = iconColor,
                    start = Offset(w * 0.58f, h * 0.72f),
                    end = Offset(w * 0.82f, h * 0.5f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            VaultModuleType.Files -> {
                val folderPath = Path().apply {
                    moveTo(w * 0.08f, h * 0.28f)
                    lineTo(w * 0.36f, h * 0.28f)
                    lineTo(w * 0.46f, h * 0.4f)
                    lineTo(w * 0.9f, h * 0.4f)
                    lineTo(w * 0.9f, h * 0.82f)
                    lineTo(w * 0.08f, h * 0.82f)
                    close()
                }

                drawPath(
                    path = folderPath,
                    color = iconColor,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            VaultModuleType.FaceDemo -> {
                drawCircle(
                    color = iconColor,
                    radius = w * 0.28f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = Stroke(width = strokeWidth)
                )
                drawCircle(
                    color = iconColor,
                    radius = w * 0.05f,
                    center = Offset(w * 0.4f, h * 0.44f)
                )
                drawCircle(
                    color = iconColor,
                    radius = w * 0.05f,
                    center = Offset(w * 0.6f, h * 0.44f)
                )
                drawLine(
                    color = iconColor,
                    start = Offset(w * 0.4f, h * 0.66f),
                    end = Offset(w * 0.6f, h * 0.66f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = iconColor,
                    start = Offset(w * 0.18f, h * 0.24f),
                    end = Offset(w * 0.3f, h * 0.24f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = iconColor,
                    start = Offset(w * 0.7f, h * 0.24f),
                    end = Offset(w * 0.82f, h * 0.24f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = iconColor,
                    start = Offset(w * 0.18f, h * 0.76f),
                    end = Offset(w * 0.3f, h * 0.76f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = iconColor,
                    start = Offset(w * 0.7f, h * 0.76f),
                    end = Offset(w * 0.82f, h * 0.76f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Immutable
private data class VaultModule(
    val title: String,
    val description: String,
    val type: VaultModuleType,
    val onClick: () -> Unit
)

private enum class VaultModuleType {
    Notes,
    Passwords,
    Photos,
    Files,
    FaceDemo
}