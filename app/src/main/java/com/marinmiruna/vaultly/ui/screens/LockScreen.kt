package com.marinmiruna.vaultly.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.marinmiruna.vaultly.R
import com.marinmiruna.vaultly.ui.components.BiometricButton

@Composable
fun LockScreen(
    statusMessage: String,
    onUnlockClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 28.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1.1f))

            SecurityMark(
                modifier = Modifier.size(88.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.lock_subtitle),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.titleLarge.lineHeight
            )

            Spacer(modifier = Modifier.height(34.dp))

            BiometricButton(
                text = stringResource(R.string.lock_unlock_button),
                modifier = Modifier.fillMaxWidth(),
                onAuthenticateClick = onUnlockClick
            )

            if (statusMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            LockFooter(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SecurityMark(
    modifier: Modifier = Modifier
) {
    val backgroundBrush = Brush.radialGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
            MaterialTheme.colorScheme.background
        )
    )

    val shieldColor = MaterialTheme.colorScheme.onSurface
    val biometricColor = MaterialTheme.colorScheme.primary
    val centerColor = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(brush = backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(54.dp)) {
            val w = size.width
            val h = size.height
            val strokeWidth = w * 0.06f

            val shieldPath = Path().apply {
                moveTo(w * 0.5f, h * 0.08f)
                lineTo(w * 0.78f, h * 0.18f)
                lineTo(w * 0.78f, h * 0.46f)
                quadraticTo(
                    w * 0.78f,
                    h * 0.70f,
                    w * 0.5f,
                    h * 0.88f
                )
                quadraticTo(
                    w * 0.22f,
                    h * 0.70f,
                    w * 0.22f,
                    h * 0.46f
                )
                lineTo(w * 0.22f, h * 0.18f)
                close()
            }

            drawPath(
                path = shieldPath,
                color = shieldColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            drawCircle(
                color = biometricColor,
                radius = w * 0.13f,
                center = Offset(w * 0.5f, h * 0.52f),
                style = Stroke(width = strokeWidth)
            )

            drawCircle(
                color = centerColor,
                radius = w * 0.035f,
                center = Offset(w * 0.5f, h * 0.52f)
            )
        }
    }
}

@Composable
private fun LockFooter(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MiniVaultIcon(
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.size(10.dp))

        Text(
            text = stringResource(R.string.lock_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MiniVaultIcon(
    modifier: Modifier = Modifier
) {
    val borderColor = MaterialTheme.colorScheme.onSurfaceVariant
    val biometricColor = MaterialTheme.colorScheme.primary
    val centerColor = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.08f

        val shieldPath = Path().apply {
            moveTo(w * 0.5f, h * 0.08f)
            lineTo(w * 0.82f, h * 0.2f)
            lineTo(w * 0.82f, h * 0.48f)
            quadraticTo(
                w * 0.82f,
                h * 0.72f,
                w * 0.5f,
                h * 0.9f
            )
            quadraticTo(
                w * 0.18f,
                h * 0.72f,
                w * 0.18f,
                h * 0.48f
            )
            lineTo(w * 0.18f, h * 0.2f)
            close()
        }

        drawPath(
            path = shieldPath,
            color = borderColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        drawCircle(
            color = biometricColor,
            radius = w * 0.14f,
            center = Offset(w * 0.5f, h * 0.52f),
            style = Stroke(width = strokeWidth)
        )

        drawCircle(
            color = centerColor,
            radius = w * 0.04f,
            center = Offset(w * 0.5f, h * 0.52f)
        )
    }
}