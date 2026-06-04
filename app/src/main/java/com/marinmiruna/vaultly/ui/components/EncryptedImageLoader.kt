package com.marinmiruna.vaultly.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.marinmiruna.vaultly.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun EncryptedImageLoader(
    encryptedFilePath: String,
    contentDescription: String,
    decryptImage: suspend (String) -> ByteArray,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    targetSizePx: Int = DEFAULT_TARGET_IMAGE_SIZE_PX,
    loadingText: String? = null
) {
    var imageBitmap by remember(encryptedFilePath, targetSizePx) {
        mutableStateOf<ImageBitmap?>(null)
    }
    var hasError by remember(encryptedFilePath, targetSizePx) {
        mutableStateOf(false)
    }

    val invalidImageMessage = stringResource(R.string.encrypted_image_invalid)

    LaunchedEffect(encryptedFilePath, targetSizePx) {
        imageBitmap = null
        hasError = false

        runCatching {
            val bytes = decryptImage(encryptedFilePath)
            withContext(Dispatchers.Default) {
                decodeSampledBitmap(
                    bytes = bytes,
                    targetSizePx = targetSizePx,
                    invalidImageMessage = invalidImageMessage
                ).asImageBitmap()
            }
        }.onSuccess { bitmap ->
            imageBitmap = bitmap
        }.onFailure {
            imageBitmap = null
            hasError = true
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when {
            imageBitmap != null -> {
                Image(
                    bitmap = imageBitmap!!,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }

            hasError -> {
                Text(
                    text = stringResource(R.string.encrypted_image_load_error),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            else -> {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )

                    if (loadingText != null) {
                        Text(
                            text = loadingText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private fun decodeSampledBitmap(
    bytes: ByteArray,
    targetSizePx: Int,
    invalidImageMessage: String
): android.graphics.Bitmap {
    val boundsOptions = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }

    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(
            width = boundsOptions.outWidth,
            height = boundsOptions.outHeight,
            targetSizePx = targetSizePx
        )
    }

    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
        ?: throw IllegalArgumentException(invalidImageMessage)
}

private fun calculateInSampleSize(
    width: Int,
    height: Int,
    targetSizePx: Int
): Int {
    if (width <= 0 || height <= 0 || targetSizePx <= 0) {
        return 1
    }

    var inSampleSize = 1
    var halfWidth = width / 2
    var halfHeight = height / 2

    while ((halfWidth / inSampleSize) >= targetSizePx &&
        (halfHeight / inSampleSize) >= targetSizePx
    ) {
        inSampleSize *= 2
    }

    return inSampleSize.coerceAtLeast(1)
}

private const val DEFAULT_TARGET_IMAGE_SIZE_PX = 1080