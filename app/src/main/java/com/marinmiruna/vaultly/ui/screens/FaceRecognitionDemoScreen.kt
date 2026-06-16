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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.marinmiruna.vaultly.R
import com.marinmiruna.vaultly.ui.components.ScreenHeader
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
import androidx.compose.material3.ButtonDefaults

private enum class LivenessState {
    NotStarted,
    WaitingForOpenEyes,
    WaitingForBlink,
    WaitingForEyesOpenAgain,
    Verified
}

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
            ScreenHeader(
                title = stringResource(R.string.face_demo_title),
                onBack = onBack,
                titleStyle = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

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
    var livenessState by remember { mutableStateOf(LivenessState.NotStarted) }
    var referenceSavedMessageVisible by remember { mutableStateOf(false) }

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
                        livenessState = livenessState.next(smoothedResult)
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
                onSave = {
                    referenceEmbedding = it
                    livenessState = LivenessState.NotStarted
                    referenceSavedMessageVisible = true
                }
            )
            if (referenceSavedMessageVisible) {
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Text(
                        text = stringResource(R.string.face_demo_reference_saved),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            LivenessButton(
                livenessState = livenessState,
                onStart = {
                    livenessState = LivenessState.WaitingForOpenEyes
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            FaceAnalysisPanel(
                result = faceAnalysis,
                referenceEmbedding = referenceEmbedding,
                livenessState = livenessState
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = if (canSave) {
                stringResource(R.string.face_demo_save_reference_button)
            } else {
                stringResource(R.string.face_demo_no_face_detected)
            }
        )
    }
}

@Composable
private fun LivenessButton(
    livenessState: LivenessState,
    onStart: () -> Unit
) {
    Button(
        onClick = onStart,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(
            text = when (livenessState) {
                LivenessState.NotStarted -> stringResource(R.string.face_demo_liveness_start)
                LivenessState.Verified -> stringResource(R.string.face_demo_liveness_retry)
                else -> stringResource(R.string.face_demo_liveness_restart)
            }
        )
    }
}

@Composable
private fun FaceAnalysisPanel(
    result: FaceAnalysisResult,
    referenceEmbedding: FaceEmbedding? = null,
    livenessState: LivenessState
) {
    val unavailable = stringResource(R.string.face_demo_value_unavailable)
    val facePoseValid = result.isFacePoseValidForRecognition()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = stringResource(R.string.face_demo_analysis_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(2.dp))

        AnalysisRow(
            label = stringResource(R.string.face_demo_faces_detected_label),
            value = result.faceCount.toString()
        )

        AnalysisRow(
            label = stringResource(R.string.face_demo_smile_probability_label),
            value = result.smileProbability.formatPercentOrUnavailable(unavailable)
        )

        AnalysisRow(
            label = stringResource(R.string.face_demo_left_eye_probability_label),
            value = result.leftEyeOpenProbability.formatPercentOrUnavailable(unavailable)
        )

        AnalysisRow(
            label = stringResource(R.string.face_demo_right_eye_probability_label),
            value = result.rightEyeOpenProbability.formatPercentOrUnavailable(unavailable)
        )

        AnalysisRow(
            label = stringResource(R.string.face_demo_head_angle_label),
            value = "X ${result.headEulerAngleX.formatAngleOrUnavailable(unavailable)} / " +
                    "Y ${result.headEulerAngleY.formatAngleOrUnavailable(unavailable)} / " +
                    "Z ${result.headEulerAngleZ.formatAngleOrUnavailable(unavailable)}"
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = livenessState.statusText(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = when (livenessState) {
                LivenessState.Verified -> MaterialTheme.colorScheme.primary
                LivenessState.NotStarted -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.tertiary
            }
        )

        val matchText = when {
            referenceEmbedding == null -> stringResource(R.string.face_demo_reference_missing)
            result.embedding == null -> stringResource(R.string.face_demo_embedding_unavailable)
            !facePoseValid -> stringResource(R.string.face_demo_position_face_front)
            livenessState != LivenessState.Verified -> {
                stringResource(R.string.face_demo_complete_liveness_first)
            }
            else -> {
                val score = result.embedding.cosineSimilarity(referenceEmbedding)
                val scorePercent = (score * 100).roundToInt()
                val match = score >= FACE_NET_MATCH_THRESHOLD

                if (match) {
                    stringResource(R.string.face_demo_similarity_match, scorePercent)
                } else {
                    stringResource(R.string.face_demo_similarity_no_match, scorePercent)
                }
            }
        }

        Text(
            text = matchText,
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                referenceEmbedding == null -> MaterialTheme.colorScheme.onSurfaceVariant
                result.embedding == null -> MaterialTheme.colorScheme.error
                !facePoseValid -> MaterialTheme.colorScheme.tertiary
                livenessState != LivenessState.Verified -> MaterialTheme.colorScheme.tertiary
                result.embedding.cosineSimilarity(referenceEmbedding) >= FACE_NET_MATCH_THRESHOLD -> {
                    MaterialTheme.colorScheme.primary
                }
                else -> MaterialTheme.colorScheme.error
            }
        )
    }
}

@Composable
private fun AnalysisRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.9f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            modifier = Modifier.weight(1.35f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

private fun LivenessState.next(result: FaceAnalysisResult): LivenessState {
    if (this == LivenessState.NotStarted || this == LivenessState.Verified) {
        return this
    }

    if (!result.isFacePoseValidForRecognition()) {
        return this
    }

    return when (this) {
        LivenessState.WaitingForOpenEyes -> {
            if (result.areEyesOpenForLiveness()) {
                LivenessState.WaitingForBlink
            } else {
                this
            }
        }

        LivenessState.WaitingForBlink -> {
            if (result.areEyesClosedForLiveness()) {
                LivenessState.WaitingForEyesOpenAgain
            } else {
                this
            }
        }

        LivenessState.WaitingForEyesOpenAgain -> {
            if (result.areEyesOpenForLiveness()) {
                LivenessState.Verified
            } else {
                this
            }
        }

        LivenessState.NotStarted,
        LivenessState.Verified -> this
    }
}

@Composable
private fun LivenessState.statusText(): String {
    return when (this) {
        LivenessState.NotStarted -> stringResource(R.string.face_demo_liveness_not_started)
        LivenessState.WaitingForOpenEyes -> stringResource(R.string.face_demo_liveness_open_eyes)
        LivenessState.WaitingForBlink -> stringResource(R.string.face_demo_liveness_blink)
        LivenessState.WaitingForEyesOpenAgain -> stringResource(R.string.face_demo_liveness_open_again)
        LivenessState.Verified -> stringResource(R.string.face_demo_liveness_verified)
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

private fun FaceAnalysisResult.areEyesOpenForLiveness(): Boolean {
    val leftEye = leftEyeOpenProbability ?: return false
    val rightEye = rightEyeOpenProbability ?: return false

    return leftEye >= LIVENESS_EYE_OPEN_THRESHOLD &&
            rightEye >= LIVENESS_EYE_OPEN_THRESHOLD
}

private fun FaceAnalysisResult.areEyesClosedForLiveness(): Boolean {
    val leftEye = leftEyeOpenProbability ?: return false
    val rightEye = rightEyeOpenProbability ?: return false

    return leftEye <= LIVENESS_EYE_CLOSED_THRESHOLD &&
            rightEye <= LIVENESS_EYE_CLOSED_THRESHOLD
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

            Spacer(modifier = Modifier.height(10.dp))

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
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
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
private const val LIVENESS_EYE_OPEN_THRESHOLD = 0.60f
private const val LIVENESS_EYE_CLOSED_THRESHOLD = 0.35f