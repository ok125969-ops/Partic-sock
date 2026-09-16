package com.example.ai

import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import kotlin.random.Random

object TextToParticleConverter {
    private val paint = Paint().apply {
        textSize = 90f
        isAntiAlias = true
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    fun sampleTextPoints(
        text: String,
        targetCount: Int,
        outX: FloatArray,
        outY: FloatArray,
        outZ: FloatArray
    ) {
        val path = Path()
        val bounds = RectF()
        paint.getTextPath(text, 0, text.length, 0f, 0f, path)
        path.computeBounds(bounds, true)

        val textWidth = bounds.width().coerceAtLeast(10f)
        val textHeight = bounds.height().coerceAtLeast(10f)
        val centerX = bounds.centerX()
        val centerY = bounds.centerY()

        val scale = 360f / textWidth.coerceAtLeast(textHeight)

        val pathMeasure = PathMeasure(path, false)
        val contourLengths = mutableListOf<Float>()
        var totalLength = 0f

        do {
            val length = pathMeasure.length
            if (length > 0f) {
                contourLengths.add(length)
                totalLength += length
            }
        } while (pathMeasure.nextContour())

        if (totalLength <= 0f) {
            // Fallback grid
            for (i in 0 until targetCount) {
                outX[i] = (Random.nextFloat() - 0.5f) * 200f
                outY[i] = (Random.nextFloat() - 0.5f) * 100f
                outZ[i] = (Random.nextFloat() - 0.5f) * 30f
            }
            return
        }

        val pos = FloatArray(2)
        val tan = FloatArray(2)
        var particleIdx = 0

        // Reset path measure
        pathMeasure.setPath(path, false)

        var contourIdx = 0
        do {
            val cLength = contourLengths.getOrNull(contourIdx) ?: 0f
            if (cLength > 0f) {
                val particlesForContour = ((cLength / totalLength) * targetCount).toInt().coerceAtLeast(20)
                val step = cLength / particlesForContour

                for (s in 0 until particlesForContour) {
                    if (particleIdx >= targetCount) break
                    val distance = s * step
                    pathMeasure.getPosTan(distance, pos, tan)

                    val jitterX = (Random.nextFloat() - 0.5f) * 6f
                    val jitterY = (Random.nextFloat() - 0.5f) * 6f
                    val depthZ = (Random.nextFloat() - 0.5f) * 45f

                    outX[particleIdx] = (pos[0] - centerX) * scale + jitterX
                    outY[particleIdx] = (pos[1] - centerY) * scale + jitterY
                    outZ[particleIdx] = depthZ
                    particleIdx++
                }
            }
            contourIdx++
        } while (pathMeasure.nextContour() && particleIdx < targetCount)

        // Fill remaining particles along existing points with small volumetric dispersion
        val existingCount = particleIdx.coerceAtLeast(1)
        while (particleIdx < targetCount) {
            val srcIdx = particleIdx % existingCount
            outX[particleIdx] = outX[srcIdx] + (Random.nextFloat() - 0.5f) * 12f
            outY[particleIdx] = outY[srcIdx] + (Random.nextFloat() - 0.5f) * 12f
            outZ[particleIdx] = outZ[srcIdx] + (Random.nextFloat() - 0.5f) * 35f
            particleIdx++
        }
    }
}
