package com.marinmiruna.vaultly.ui.screens.facerecognition

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@Composable
internal fun CameraPreview(
    onFaceAnalysis: (FaceAnalysisResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraExecutor = Executors.newSingleThreadExecutor()
        val faceEmbeddingModel = runCatching { FaceEmbeddingModel(context) }.getOrNull()

        val faceDetector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .enableTracking()
                .build()
        )

        val mainExecutor = ContextCompat.getMainExecutor(context)

        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also { cameraPreview ->
                        cameraPreview.setSurfaceProvider(previewView.surfaceProvider)
                    }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            processFaceAnalysisFrame(
                                imageProxy = imageProxy,
                                faceDetector = faceDetector,
                                faceEmbeddingModel = faceEmbeddingModel,
                                callbackExecutor = cameraExecutor,
                                onFaceAnalysis = onFaceAnalysis
                            )
                        }
                    }

                runCatching {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        imageAnalysis
                    )
                }.onFailure {
                    onFaceAnalysis(FaceAnalysisResult())
                }
            },
            mainExecutor
        )

        onDispose {
            runCatching {
                cameraProviderFuture.get().unbindAll()
            }

            faceDetector.close()
            faceEmbeddingModel?.close()
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { previewView }
    )
}

@OptIn(ExperimentalGetImage::class)
@SuppressLint("UnsafeOptInUsageError")
private fun processFaceAnalysisFrame(
    imageProxy: ImageProxy,
    faceDetector: FaceDetector,
    faceEmbeddingModel: FaceEmbeddingModel?,
    callbackExecutor: Executor,
    onFaceAnalysis: (FaceAnalysisResult) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }

    val inputImage = InputImage.fromMediaImage(
        mediaImage,
        imageProxy.imageInfo.rotationDegrees
    )

    faceDetector
        .process(inputImage)
        .addOnSuccessListener(callbackExecutor) { faces ->
            val primaryFace = faces.firstOrNull()

            val landmarks = primaryFace?.allLandmarks
                ?.map { landmark -> landmark.position.toFacePoint() }
                .orEmpty()

            val embedding = if (primaryFace != null && faceEmbeddingModel != null) {
                val rotatedBitmap = imageProxy.toBitmap()
                    ?.rotate(imageProxy.imageInfo.rotationDegrees)

                val faceBitmap = rotatedBitmap?.cropFace(primaryFace.boundingBox)

                faceBitmap?.let { bitmap ->
                    runCatching {
                        faceEmbeddingModel.getEmbedding(bitmap)
                    }.getOrNull()
                }
            } else {
                null
            }

            val result = FaceAnalysisResult(
                faceCount = faces.size,
                smileProbability = primaryFace?.smilingProbability,
                leftEyeOpenProbability = primaryFace?.leftEyeOpenProbability,
                rightEyeOpenProbability = primaryFace?.rightEyeOpenProbability,
                headEulerAngleX = primaryFace?.headEulerAngleX,
                headEulerAngleY = primaryFace?.headEulerAngleY,
                headEulerAngleZ = primaryFace?.headEulerAngleZ,
                imageWidth = inputImage.width,
                imageHeight = inputImage.height,
                faceBounds = primaryFace?.let { face ->
                    listOf(
                        FaceBounds(
                            left = face.boundingBox.left.toFloat(),
                            top = face.boundingBox.top.toFloat(),
                            right = face.boundingBox.right.toFloat(),
                            bottom = face.boundingBox.bottom.toFloat()
                        )
                    )
                }.orEmpty(),
                contours = primaryFace?.extractFaceContours().orEmpty(),
                landmarks = landmarks,
                descriptor = landmarks.toFaceDescriptor(),
                embedding = embedding
            )

            onFaceAnalysis(result)
        }
        .addOnFailureListener(callbackExecutor) {
            onFaceAnalysis(FaceAnalysisResult())
        }
        .addOnCompleteListener(callbackExecutor) {
            imageProxy.close()
        }
}

private fun ImageProxy.toBitmap(): Bitmap? {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val outputStream = ByteArrayOutputStream()

    return if (yuvImage.compressToJpeg(Rect(0, 0, width, height), 90, outputStream)) {
        val imageBytes = outputStream.toByteArray()
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    } else {
        null
    }
}

private fun Bitmap.rotate(rotationDegrees: Int): Bitmap {
    if (rotationDegrees == 0) return this

    val matrix = Matrix().apply {
        postRotate(rotationDegrees.toFloat())
    }

    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

private fun Bitmap.cropFace(bounds: Rect): Bitmap? {
    val paddingX = (bounds.width() * 0.18f).toInt()
    val paddingY = (bounds.height() * 0.18f).toInt()

    val left = (bounds.left - paddingX).coerceIn(0, width - 1)
    val top = (bounds.top - paddingY).coerceIn(0, height - 1)
    val right = (bounds.right + paddingX).coerceIn(left + 1, width)
    val bottom = (bounds.bottom + paddingY).coerceIn(top + 1, height)

    val cropWidth = right - left
    val cropHeight = bottom - top

    if (cropWidth <= 0 || cropHeight <= 0) return null

    return Bitmap.createBitmap(this, left, top, cropWidth, cropHeight)
}