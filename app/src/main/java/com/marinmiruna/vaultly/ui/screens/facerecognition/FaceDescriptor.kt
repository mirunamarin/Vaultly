package com.marinmiruna.vaultly.ui.screens.facerecognition

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Descriptor facial geometric simplificat.
 * Bazat exclusiv pe distanțe relative între landmark-uri ML Kit.
 * NU este un sistem biometric real — doar demo academic.
 */
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

/**
 * Calculează distanța euclidiană între două puncte.
 */
private fun FacePoint.distanceTo(other: FacePoint): Float {
    val dx = x - other.x
    val dy = y - other.y
    return sqrt(dx * dx + dy * dy)
}

/**
 * Construiește un FaceDescriptor din lista de landmark-uri ML Kit.
 * Returnează null dacă nu sunt suficiente landmark-uri disponibile.
 *
 * Landmark indices ML Kit (LANDMARK_MODE_ALL):
 * 0 = BOTTOM_MOUTH, 1 = RIGHT_MOUTH, 2 = LEFT_MOUTH,
 * 3 = RIGHT_EYE, 4 = LEFT_EYE, 5 = RIGHT_EAR,
 * 6 = LEFT_EAR, 7 = RIGHT_CHEEK, 8 = LEFT_CHEEK,
 * 9 = NOSE_BASE
 */
internal fun List<FacePoint>.toFaceDescriptor(): FaceDescriptor? {
    if (size < 6) return null

    // Găsim landmark-urile cheie după poziție relativă
    // ML Kit returnează landmark-urile într-o ordine fixă
    val rightEye = getOrNull(3) ?: return null
    val leftEye = getOrNull(4) ?: return null
    val noseBase = getOrNull(9) ?: return null
    val rightMouth = getOrNull(1) ?: return null
    val leftMouth = getOrNull(2) ?: return null
    val bottomMouth = getOrNull(0) ?: return null

    // Baza de normalizare: distanța inter-oculară
    val eyeDistance = rightEye.distanceTo(leftEye)
    if (eyeDistance < 1f) return null

    // Vector de trăsături normalizate
    val features = floatArrayOf(
        // Lățimea gurii relativă
        rightMouth.distanceTo(leftMouth) / eyeDistance,
        // Înălțimea gurii (comisuri → buza de jos)
        rightMouth.distanceTo(bottomMouth) / eyeDistance,
        leftMouth.distanceTo(bottomMouth) / eyeDistance,
        // Distanța nas → centrul ochilor
        noseBase.distanceTo(midpoint(rightEye, leftEye)) / eyeDistance,
        // Distanța nas → gură
        noseBase.distanceTo(midpoint(rightMouth, leftMouth)) / eyeDistance,
        // Asimetrie ochi-nas (stânga vs dreapta)
        rightEye.distanceTo(noseBase) / eyeDistance,
        leftEye.distanceTo(noseBase) / eyeDistance,
        // Înălțimea feței (ochi → gură)
        midpoint(rightEye, leftEye).distanceTo(bottomMouth) / eyeDistance
    )

    return FaceDescriptor(features)
}

private fun midpoint(a: FacePoint, b: FacePoint): FacePoint {
    return FacePoint(x = (a.x + b.x) / 2f, y = (a.y + b.y) / 2f)
}

/**
 * Calculează scorul de similaritate între două descriptori.
 * Returnează o valoare între 0f (complet diferit) și 1f (identic).
 */
internal fun FaceDescriptor.similarityScore(other: FaceDescriptor): Float {
    if (features.size != other.features.size) return 0f

    var sumSquaredDiff = 0f
    for (i in features.indices) {
        val diff = features[i] - other.features[i]
        sumSquaredDiff += diff * diff
    }

    val distance = sqrt(sumSquaredDiff)
    // Convertim distanța în scor 0-1 (distanța 0 = scor 1, distanța ≥ 1 = scor ~0)
    return (1f / (1f + distance * 3f))
}

/**
 * Prag recomandat pentru „potrivire".
 * Valoare empirică pentru demo — nu pentru autentificare reală.
 */
internal const val FACE_MATCH_THRESHOLD = 0.72f