package com.marinmiruna.vaultly.ui.screens.facerecognition

import kotlin.math.abs
import kotlin.math.sqrt


internal data class FaceDescriptor(
    val features: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FaceDescriptor) return false
        return features.contentEquals(other.features)
    }

    override fun hashCode(): Int = features.contentHashCode()
}


private fun FacePoint.distanceTo(other: FacePoint): Float {
    val dx = x - other.x
    val dy = y - other.y
    return sqrt(dx * dx + dy * dy)
}

internal fun List<FacePoint>.toFaceDescriptor(): FaceDescriptor? {
    if (size < 6) return null


    val rightEye = getOrNull(3) ?: return null
    val leftEye = getOrNull(4) ?: return null
    val noseBase = getOrNull(9) ?: return null
    val rightMouth = getOrNull(1) ?: return null
    val leftMouth = getOrNull(2) ?: return null
    val bottomMouth = getOrNull(0) ?: return null


    val eyeDistance = rightEye.distanceTo(leftEye)
    if (eyeDistance < 1f) return null


    val features = floatArrayOf(

        rightMouth.distanceTo(leftMouth) / eyeDistance,

        rightMouth.distanceTo(bottomMouth) / eyeDistance,
        leftMouth.distanceTo(bottomMouth) / eyeDistance,

        noseBase.distanceTo(midpoint(rightEye, leftEye)) / eyeDistance,

        noseBase.distanceTo(midpoint(rightMouth, leftMouth)) / eyeDistance,

        rightEye.distanceTo(noseBase) / eyeDistance,
        leftEye.distanceTo(noseBase) / eyeDistance,

        midpoint(rightEye, leftEye).distanceTo(bottomMouth) / eyeDistance
    )

    return FaceDescriptor(features)
}

private fun midpoint(a: FacePoint, b: FacePoint): FacePoint {
    return FacePoint(x = (a.x + b.x) / 2f, y = (a.y + b.y) / 2f)
}

internal fun FaceDescriptor.similarityScore(other: FaceDescriptor): Float {
    if (features.size != other.features.size) return 0f

    var sumSquaredDiff = 0f
    for (i in features.indices) {
        val diff = features[i] - other.features[i]
        sumSquaredDiff += diff * diff
    }

    val distance = sqrt(sumSquaredDiff)

    return (1f / (1f + distance * 3f))
}


internal const val FACE_MATCH_THRESHOLD = 0.72f