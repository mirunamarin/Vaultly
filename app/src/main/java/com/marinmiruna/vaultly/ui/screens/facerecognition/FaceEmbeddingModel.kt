package com.marinmiruna.vaultly.ui.screens.facerecognition

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

internal class FaceEmbeddingModel(
    context: Context
) : AutoCloseable {

    private val interpreter = Interpreter(
        loadModelFile(context),
        Interpreter.Options().apply {
            setNumThreads(4)
        }
    )

    fun getEmbedding(faceBitmap: Bitmap): FaceEmbedding {
        val input = bitmapToInputBuffer(faceBitmap)
        val output = Array(1) { FloatArray(EMBEDDING_SIZE) }

        interpreter.run(input, output)

        return FaceEmbedding(output[0].l2Normalize())
    }

    override fun close() {
        interpreter.close()
    }

    private fun bitmapToInputBuffer(bitmap: Bitmap): ByteBuffer {
        val resizedBitmap = Bitmap.createScaledBitmap(
            bitmap,
            INPUT_IMAGE_SIZE,
            INPUT_IMAGE_SIZE,
            true
        )

        val inputBuffer = ByteBuffer.allocateDirect(
            1 * INPUT_IMAGE_SIZE * INPUT_IMAGE_SIZE * CHANNEL_COUNT * FLOAT_SIZE
        ).apply {
            order(ByteOrder.nativeOrder())
        }

        val pixels = IntArray(INPUT_IMAGE_SIZE * INPUT_IMAGE_SIZE)
        resizedBitmap.getPixels(
            pixels,
            0,
            INPUT_IMAGE_SIZE,
            0,
            0,
            INPUT_IMAGE_SIZE,
            INPUT_IMAGE_SIZE
        )

        pixels.forEach { pixel ->
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            inputBuffer.putFloat((r - IMAGE_MEAN) / IMAGE_STD)
            inputBuffer.putFloat((g - IMAGE_MEAN) / IMAGE_STD)
            inputBuffer.putFloat((b - IMAGE_MEAN) / IMAGE_STD)
        }

        inputBuffer.rewind()
        return inputBuffer
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(MODEL_FILE_NAME)

        FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
            return inputStream.channel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength
            )
        }
    }

    private fun FloatArray.l2Normalize(): FloatArray {
        var sum = 0f

        forEach { value ->
            sum += value * value
        }

        val norm = sqrt(sum)
        if (norm == 0f) return this

        return FloatArray(size) { index ->
            this[index] / norm
        }
    }

    private companion object {
        const val MODEL_FILE_NAME = "facenet.tflite"
        const val INPUT_IMAGE_SIZE = 160
        const val CHANNEL_COUNT = 3
        const val EMBEDDING_SIZE = 128
        const val FLOAT_SIZE = 4
        const val IMAGE_MEAN = 127.5f
        const val IMAGE_STD = 128f
    }
}

internal data class FaceEmbedding(
    val values: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FaceEmbedding) return false
        return values.contentEquals(other.values)
    }

    override fun hashCode(): Int {
        return values.contentHashCode()
    }
}

internal fun FaceEmbedding.cosineSimilarity(other: FaceEmbedding): Float {
    if (values.size != other.values.size) return 0f

    var dotProduct = 0f

    for (index in values.indices) {
        dotProduct += values[index] * other.values[index]
    }

    return ((dotProduct + 1f) / 2f).coerceIn(0f, 1f)
}