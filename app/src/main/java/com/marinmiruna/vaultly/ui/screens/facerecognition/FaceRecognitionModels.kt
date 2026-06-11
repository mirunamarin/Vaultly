package com.marinmiruna.vaultly.ui.screens.facerecognition

import androidx.compose.runtime.Immutable

@Immutable
internal data class FaceAnalysisResult(
    val faceCount: Int = 0,
    val smileProbability: Float? = null,
    val leftEyeOpenProbability: Float? = null,
    val rightEyeOpenProbability: Float? = null,
    val headEulerAngleX: Float? = null,
    val headEulerAngleY: Float? = null,
    val headEulerAngleZ: Float? = null,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val faceBounds: List<FaceBounds> = emptyList(),
    val contours: List<FaceContourPath> = emptyList(),
    val landmarks: List<FacePoint> = emptyList(),
    val descriptor: FaceDescriptor? = null,
    val embedding: FaceEmbedding? = null
)

@Immutable
internal data class FaceBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

@Immutable
internal data class FaceContourPath(
    val points: List<FacePoint>,
    val closed: Boolean = false
)

@Immutable
internal data class FacePoint(
    val x: Float,
    val y: Float
)