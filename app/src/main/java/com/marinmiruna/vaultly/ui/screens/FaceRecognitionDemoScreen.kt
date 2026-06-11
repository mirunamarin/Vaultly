package com.marinmiruna.vaultly.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.marinmiruna.vaultly.R
import com.marinmiruna.vaultly.ui.screens.facerecognition.CameraPreview
import com.marinmiruna.vaultly.ui.screens.facerecognition.FaceAnalysisResult
import com.marinmiruna.vaultly.ui.screens.facerecognition.FaceDetectionOverlay
import com.marinmiruna.vaultly.ui.screens.facerecognition.FaceEmbedding
import com.marinmiruna.vaultly.ui.screens.facerecognition.cosineSimilarity
import com.marinmiruna.vaultly.ui.screens.facerecognition.formatAngleOrUnavailable
import com.marinmiruna.vaultly.ui.screens.facerecognition.formatPercentOrUnavailable
import com.marinmiruna.vaultly.ui.screens.facerecognition.smoothWith
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun FaceRecognitionDemoScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var permissionDenied by remember {
        mutableStateOf(false)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        permissionDenied = !isGranted
    }

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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.face_demo_title),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )

                TextButton(onClick = onBack) {
                    Text(text = stringResource(R.string.common_back))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.face_demo_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (hasCameraPermission) {
                CameraPreviewCard()
            } else {
                CameraPermissionCard(
                    permissionDenied = permissionDenied,
                    onRequestPermission = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            }
        }
    }
}

@Composable
private fun CameraPreviewCard() {
    var faceAnalysis by remember { mutableStateOf(FaceAnalysisResult()) }
    var previousFaceAnalysis by remember { mutableStateOf<FaceAnalysisResult?>(null) }
    var referenceEmbedding by remember { mutableStateOf<FaceEmbedding?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                .padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
            ) {
                CameraPreview(
                    onFaceAnalysis = { result ->
                        val smoothedResult = result.smoothWith(
                            previous = previousFaceAnalysis,
                            alpha = FACE_ANALYSIS_SMOOTHING_FACTOR
                        )
                        previousFaceAnalysis = smoothedResult
                        faceAnalysis = smoothedResult
                    },
                    modifier = Modifier.fillMaxSize()
                )

                FaceDetectionOverlay(
                    result = faceAnalysis,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            SaveReferenceButton(
                currentEmbedding = faceAnalysis.embedding,
                onSave = { referenceEmbedding = it }
            )

            Spacer(modifier = Modifier.height(14.dp))

            FaceAnalysisPanel(
                result = faceAnalysis,
                referenceEmbedding = referenceEmbedding
            )
        }
    }
}

@Composable
private fun SaveReferenceButton(
    currentEmbedding: FaceEmbedding?,
    onSave: (FaceEmbedding) -> Unit
) {
    val canSave = currentEmbedding != null

    Button(
        onClick = { currentEmbedding?.let(onSave) },
        enabled = canSave,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = if (canSave) "Salvează fața de referință" else "Nicio față detectată")
    }
}

@Composable
private fun FaceAnalysisPanel(
    result: FaceAnalysisResult,
    referenceEmbedding: FaceEmbedding? = null
) {
    val unavailable = stringResource(R.string.face_demo_value_unavailable)
    val facePoseValid = result.isFacePoseValidForRecognition()

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.face_demo_analysis_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.face_demo_faces_detected, result.faceCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = stringResource(
                R.string.face_demo_smile_probability,
                result.smileProbability.formatPercentOrUnavailable(unavailable)
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = stringResource(
                R.string.face_demo_left_eye_probability,
                result.leftEyeOpenProbability.formatPercentOrUnavailable(unavailable)
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = stringResource(
                R.string.face_demo_right_eye_probability,
                result.rightEyeOpenProbability.formatPercentOrUnavailable(unavailable)
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = stringResource(
                R.string.face_demo_head_angle,
                result.headEulerAngleX.formatAngleOrUnavailable(unavailable),
                result.headEulerAngleY.formatAngleOrUnavailable(unavailable),
                result.headEulerAngleZ.formatAngleOrUnavailable(unavailable)
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        val matchText = when {
            referenceEmbedding == null -> "Referință FaceNet lipsă"
            result.embedding == null -> "Embedding facial indisponibil"
            !facePoseValid -> "Poziționează fața frontal pentru recunoaștere"
            else -> {
                val score = result.embedding.cosineSimilarity(referenceEmbedding)
                val match = score >= FACE_NET_MATCH_THRESHOLD
                "Similaritate FaceNet: ${(score * 100).roundToInt()}% — ${if (match) "✓ Potrivire" else "✗ Nepotrivire"}"
            }
        }

        Text(
            text = matchText,
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                referenceEmbedding == null -> MaterialTheme.colorScheme.onSurfaceVariant
                result.embedding == null -> MaterialTheme.colorScheme.error
                !facePoseValid -> MaterialTheme.colorScheme.tertiary
                result.embedding.cosineSimilarity(referenceEmbedding) >= FACE_NET_MATCH_THRESHOLD ->
                    MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.error
            }
        )
    }
}

private fun FaceAnalysisResult.isFacePoseValidForRecognition(): Boolean {
    if (faceCount != 1) return false

    val xAngle = headEulerAngleX ?: return false
    val yAngle = headEulerAngleY ?: return false
    val zAngle = headEulerAngleZ ?: return false

    return abs(xAngle) <= MAX_RECOGNITION_PITCH_DEGREES &&
            abs(yAngle) <= MAX_RECOGNITION_YAW_DEGREES &&
            abs(zAngle) <= MAX_RECOGNITION_ROLL_DEGREES
}

@Composable
private fun CameraPermissionCard(
    permissionDenied: Boolean,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                text = stringResource(R.string.face_demo_camera_permission_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.face_demo_camera_permission_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (permissionDenied) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.face_demo_camera_permission_denied),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.face_demo_camera_permission_button))
            }
        }
    }
}

private const val FACE_ANALYSIS_SMOOTHING_FACTOR = 0.25f
private const val FACE_NET_MATCH_THRESHOLD = 0.80f
private const val MAX_RECOGNITION_YAW_DEGREES = 25f
private const val MAX_RECOGNITION_ROLL_DEGREES = 20f
private const val MAX_RECOGNITION_PITCH_DEGREES = 20f