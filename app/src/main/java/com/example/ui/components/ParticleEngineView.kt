package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.*

enum class ParticleObjectShape {
    BUTTERFLY, DNA, HEART, SOLAR_SYSTEM, GRAVITY, ATOM, VEHICLE, CUBE
}

enum class UltronParticleState {
    IDLE, LISTENING, THINKING, CREATING, SUCCESS
}

data class Particle(
    var x: Float,
    var y: Float,
    var targetX: Float,
    var targetY: Float,
    val color: Color,
    val size: Float,
    val alpha: Float
)

@Composable
fun ParticleEngineView(
    currentShape: ParticleObjectShape,
    particleState: UltronParticleState,
    scaleFactor: Float,
    rotationAngle: Float,
    selectedColor: Color,
    modifier: Modifier = Modifier
) {
    var userScale by remember { mutableStateOf(1f) }
    var userRotation by remember { mutableStateOf(0f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "particle_anim")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progress"
    )

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    userScale = (userScale * zoom).coerceIn(0.5f, 3f)
                    userRotation += rotation
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f + offsetX
            val cy = size.height / 2f + offsetY
            val baseScale = min(size.width, size.height) * 0.35f * scaleFactor * userScale

            // Draw HUD grid background
            drawHudGrid(cx, cy)

            val particleCount = when (particleState) {
                UltronParticleState.THINKING -> 300
                UltronParticleState.CREATING -> 400
                else -> 250
            }

            for (i in 0 until particleCount) {
                val t = (i.toFloat() / particleCount) * 2f * PI.toFloat()
                val angle = t + (animProgress * 2f * PI.toFloat()) + userRotation

                val (nx, ny) = getShapeCoordinates(currentshape = currentShape, t = t, angle = angle, progress = animProgress)

                val finalX = cx + nx * baseScale
                val finalY = cy + ny * baseScale

                val particleColor = when {
                    particleState == UltronParticleState.THINKING -> Color(0xFF00F0FF)
                    particleState == UltronParticleState.LISTENING -> Color(0xFF00FF66)
                    i % 3 == 0 -> selectedColor
                    i % 2 == 0 -> Color(0xFF00F0FF)
                    else -> Color(0xFFFFD700)
                }

                val pSize = (2f + (i % 4).toFloat()) * (if (particleState == UltronParticleState.THINKING) 1.5f else 1f)

                drawCircle(
                    color = particleColor.copy(alpha = 0.75f),
                    radius = pSize,
                    center = Offset(finalX, finalY)
                )
            }
        }
    }
}

private fun getShapeCoordinates(currentshape: ParticleObjectShape, t: Float, angle: Float, progress: Float): Pair<Float, Float> {
    return when (currentshape) {
        ParticleObjectShape.BUTTERFLY -> {
            val sinT = sin(t)
            val cosT = cos(t)
            val r = exp(cos(t)) - 2f * cos(4f * t) - sin(t / 12f).pow(5)
            Pair(r * sinT * 0.4f, r * cosT * -0.4f)
        }
        ParticleObjectShape.DNA -> {
            val x = sin(t * 3f) * 0.8f
            val y = (t / (PI.toFloat())) - 0.5f
            Pair(x, y * 1.5f)
        }
        ParticleObjectShape.HEART -> {
            val x = 16f * sin(t).pow(3) / 16f
            val y = -(13f * cos(t) - 5f * cos(2f * t) - 2f * cos(3f * t) - cos(4f * t)) / 16f
            Pair(x * 0.8f, y * 0.8f)
        }
        ParticleObjectShape.SOLAR_SYSTEM -> {
            val orbit = (t % 3f) * 0.4f + 0.3f
            Pair(cos(angle) * orbit, sin(angle) * orbit)
        }
        ParticleObjectShape.GRAVITY -> {
            val dist = abs(sin(t)) * (1f - progress * 0.3f)
            Pair(cos(angle) * dist, sin(angle) * dist)
        }
        ParticleObjectShape.ATOM -> {
            val r = 0.7f
            Pair(cos(angle + t) * r, sin(angle) * r * 0.4f)
        }
        ParticleObjectShape.VEHICLE -> {
            val x = cos(t) * 0.9f
            val y = sin(t * 2f) * 0.3f
            Pair(x, y)
        }
        ParticleObjectShape.CUBE -> {
            val scale = 0.6f
            val px = cos(angle) * scale
            val py = sin(angle) * scale
            Pair(px, py)
        }
    }
}

private fun DrawScope.drawHudGrid(cx: Float, cy: Float) {
    val gridColor = Color(0xFF00F0FF).copy(alpha = 0.12f)
    val stroke = 1f
    // Concentric rings
    drawCircle(color = gridColor, radius = 120f, center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
    drawCircle(color = gridColor, radius = 240f, center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
    drawCircle(color = gridColor, radius = 360f, center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
    
    // Crosshairs
    drawLine(color = gridColor, start = Offset(cx - 400f, cy), end = Offset(cx + 400f, cy), strokeWidth = stroke)
    drawLine(color = gridColor, start = Offset(cx, cy - 400f), end = Offset(cx, cy + 400f), strokeWidth = stroke)
}
