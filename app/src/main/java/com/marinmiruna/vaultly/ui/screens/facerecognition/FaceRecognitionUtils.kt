package com.marinmiruna.vaultly.ui.screens.facerecognition

import android.graphics.PointF
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import kotlin.math.roundToInt

internal fun PointF.toFacePoint(): FacePoint {
    return FacePoint(
        x = x,
        y = y
    )
}

internal fun Face.extractFaceContours(): List<FaceContourPath> {
    return listOf(
        FaceContour.FACE to true,
        FaceContour.LEFT_EYEBROW_TOP to false,
        FaceContour.LEFT_EYEBROW_BOTTOM to false,
        FaceContour.RIGHT_EYEBROW_TOP to false,
        FaceContour.RIGHT_EYEBROW_BOTTOM to false,
        FaceContour.LEFT_EYE to true,
        FaceContour.RIGHT_EYE to true,
        FaceContour.UPPER_LIP_TOP to false,
        FaceContour.UPPER_LIP_BOTTOM to false,
        FaceContour.LOWER_LIP_TOP to false,
        FaceContour.LOWER_LIP_BOTTOM to false,
        FaceContour.NOSE_BRIDGE to false,
        FaceContour.NOSE_BOTTOM to false
    ).mapNotNull { (type, closed) ->
        val points = getContour(type)?.points.orEmpty()
        if (points.isEmpty()) {
            null
        } else {
            FaceContourPath(
                points = points.map { it.toFacePoint() },
                closed = closed
            )
        }
    }
}

internal fun FaceAnalysisResult.smoothWith(
    previous: FaceAnalysisResult?,
    alpha: Float = 0.28f
): FaceAnalysisResult {
    if (previous == null || faceCount == 0 || previous.faceCount == 0) {
        return this
    }

    return copy(
        smileProbability = smileProbability.smoothFloat(previous.smileProbability, alpha),
        leftEyeOpenProbability = leftEyeOpenProbability.smoothFloat(previous.leftEyeOpenProbability, alpha),
        rightEyeOpenProbability = rightEyeOpenProbability.smoothFloat(previous.rightEyeOpenProbability, alpha),
        headEulerAngleX = headEulerAngleX.smoothFloat(previous.headEulerAngleX, alpha),
        headEulerAngleY = headEulerAngleY.smoothFloat(previous.headEulerAngleY, alpha),
        headEulerAngleZ = headEulerAngleZ.smoothFloat(previous.headEulerAngleZ, alpha)
    )
}

private fun Float?.smoothFloat(
    previous: Float?,
    alpha: Float
): Float? {
    if (this == null || previous == null) {
        return this
    }

    return previous + (this - previous) * alpha
}

internal fun Float?.formatPercentOrUnavailable(unavailable: String): String {
    return this?.let { "${(it * 100).roundToInt()}%" } ?: unavailable
}

internal fun Float?.formatAngleOrUnavailable(unavailable: String): String {
    return this?.let { "${"%.1f".format(it)} deg" } ?: unavailable
}