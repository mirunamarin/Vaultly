package com.marinmiruna.vaultly.ui.screens.facerecognition

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.min

@Composable
internal fun FaceDetectionOverlay(
    result: FaceAnalysisResult,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (result.imageWidth == 0 || result.imageHeight == 0) {
            return@Canvas
        }

        val imageWidth = result.imageHeight.toFloat()
        val imageHeight = result.imageWidth.toFloat()

        val scale = min(
            size.width / imageWidth,
            size.height / imageHeight
        )

        val displayedWidth = imageWidth * scale
        val displayedHeight = imageHeight * scale

        val offsetX = (size.width - displayedWidth) / 2f
        val offsetY = (size.height - displayedHeight) / 2f

        fun mapX(x: Float): Float {
            return offsetX + (imageWidth - x) * scale
        }

        fun mapY(y: Float): Float {
            return offsetY + y * scale
        }

        fun mapPoint(point: FacePoint): Offset {
            return Offset(x = mapX(point.x), y = mapY(point.y))
        }

        result.contours.forEach { contour ->
            if (contour.points.size >= 2) {
                val path = Path()
                val firstPoint = mapPoint(contour.points.first())
                path.moveTo(firstPoint.x, firstPoint.y)
                contour.points.drop(1).forEach { point ->
                    val mappedPoint = mapPoint(point)
                    path.lineTo(mappedPoint.x, mappedPoint.y)
                }
                if (contour.closed) path.close()

                drawPath(
                    path = path,
                    color = Color(0xFF9B5CFF),
                    style = Stroke(width = 1.8f)
                )
            }
        }

        result.landmarks.forEach { point ->
            val mappedPoint = mapPoint(point)
            drawCircle(color = Color(0xFFE9D5FF), radius = 4.2f, center = mappedPoint)
            drawCircle(color = Color(0xFF7C3AED), radius = 2.2f, center = mappedPoint)
        }
    }
}