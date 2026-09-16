package com.example.ui.components

import android.graphics.Paint
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ai.ParticleBehaviorType
import com.example.ai.ProceduralShapeGenerator
import com.example.ai.ShapePrimitive
import com.example.ai.VisualScene
import kotlin.math.*
import kotlin.random.Random

enum class HolographicState {
    IDLE, FORMING, ACTIVE, EXPLODING
}

enum class VoiceVisualFeedbackMode {
    NORMAL,
    LISTENING,
    THINKING,
    SPEAKING
}

typealias HolographicShapeType = ShapePrimitive

class ProgrammableParticleEngine(val count: Int = 13107) {
    val curX = FloatArray(count)
    val curY = FloatArray(count)
    val curZ = FloatArray(count)

    val targetX = FloatArray(count)
    val targetY = FloatArray(count)
    val targetZ = FloatArray(count)

    val velX = FloatArray(count)
    val velY = FloatArray(count)
    val velZ = FloatArray(count)

    // Flat drawing buffer [x, y, x, y, ...]
    val drawPts = FloatArray(count * 2)
    val corePts = FloatArray(min(count, 3200) * 2)

    var state = HolographicState.IDLE
    var activeScene: VisualScene? = null
    var activeShape = ShapePrimitive.SPHERE
    var activeName = "None"
    var baseColor = Color(0xFF00F0FF)

    var rotX = 0f
    var rotY = 0f
    var autoRotate = true
    var zoom = 1.0f

    // Touch interaction coordinates
    var touchActive = false
    var touchX = 0f
    var touchY = 0f
    var touchAttract = true

    // Voice Visual Feedback state
    var currentVoiceMode = VoiceVisualFeedbackMode.NORMAL
    val voiceFeedbackMode: VoiceVisualFeedbackMode get() = currentVoiceMode
    var voiceAudioLevel: Float = 0f // Normalized audio RMS or speaking amplitude 0..1

    private val random = Random(42)

    init {
        // Initialize as glowing plasma orb
        ProceduralShapeGenerator.generateShape(ShapePrimitive.SPHERE, count, targetX, targetY, targetZ)
        for (i in 0 until count) {
            curX[i] = targetX[i]
            curY[i] = targetY[i]
            curZ[i] = targetZ[i]
        }
    }

    fun applyScene(scene: VisualScene) {
        activeScene = scene
        activeShape = scene.shape
        activeName = scene.title
        baseColor = scene.baseColor
        state = HolographicState.FORMING

        ProceduralShapeGenerator.generateShape(
            shape = scene.shape,
            count = count,
            outX = targetX,
            outY = targetY,
            outZ = targetZ,
            textString = scene.textToRender,
            parameters = scene.parameters
        )

        // Give initial fluid burst velocity for smooth morphing
        for (i in 0 until count) {
            velX[i] = (random.nextFloat() - 0.5f) * 8f
            velY[i] = -random.nextFloat() * 10f - 3f
            velZ[i] = (random.nextFloat() - 0.5f) * 8f
        }
    }

    fun setShape(shape: ShapePrimitive, name: String, color: Color) {
        activeShape = shape
        activeName = name
        baseColor = color
        state = HolographicState.FORMING

        ProceduralShapeGenerator.generateShape(
            shape = shape,
            count = count,
            outX = targetX,
            outY = targetY,
            outZ = targetZ,
            textString = name
        )

        for (i in 0 until count) {
            velX[i] = (random.nextFloat() - 0.5f) * 8f
            velY[i] = -random.nextFloat() * 10f - 3f
            velZ[i] = (random.nextFloat() - 0.5f) * 8f
        }
    }

    fun updateParameters() {
        val scene = activeScene ?: return
        ProceduralShapeGenerator.generateShape(
            shape = scene.shape,
            count = count,
            outX = targetX,
            outY = targetY,
            outZ = targetZ,
            textString = scene.textToRender,
            parameters = scene.parameters
        )
    }

    fun explode() {
        state = HolographicState.EXPLODING
        for (i in 0 until count) {
            val dist = sqrt(curX[i] * curX[i] + curY[i] * curY[i] + curZ[i] * curZ[i]).coerceAtLeast(1f)
            val speed = random.nextFloat() * 24f + 12f
            velX[i] = (curX[i] / dist) * speed + (random.nextFloat() - 0.5f) * 6f
            velY[i] = (curY[i] / dist) * speed + (random.nextFloat() - 0.5f) * 6f
            velZ[i] = (curZ[i] / dist) * speed + (random.nextFloat() - 0.5f) * 6f
        }
    }

    fun reform() {
        state = HolographicState.FORMING
        for (i in 0 until count) {
            val dx = targetX[i] - curX[i]
            val dy = targetY[i] - curY[i]
            val dz = targetZ[i] - curZ[i]
            velX[i] = dx * 0.15f
            velY[i] = dy * 0.15f
            velZ[i] = dz * 0.15f
        }
    }

    fun resetToIdle() {
        activeScene = null
        activeShape = ShapePrimitive.SPHERE
        activeName = "None"
        baseColor = Color(0xFF00F0FF)
        ProceduralShapeGenerator.generateShape(ShapePrimitive.SPHERE, count, targetX, targetY, targetZ)
        state = HolographicState.IDLE
    }

    fun setVoiceFeedbackMode(mode: VoiceVisualFeedbackMode) {
        if (currentVoiceMode == mode) return
        currentVoiceMode = mode

        when (mode) {
            VoiceVisualFeedbackMode.LISTENING -> {
                ProceduralShapeGenerator.generateShape(
                    shape = ShapePrimitive.VOICE_LISTENING_WAVEFORM,
                    count = count,
                    outX = targetX,
                    outY = targetY,
                    outZ = targetZ
                )
                state = HolographicState.FORMING
                for (i in 0 until count) {
                    velX[i] = (random.nextFloat() - 0.5f) * 6f
                    velY[i] = (random.nextFloat() - 0.5f) * 6f
                    velZ[i] = (random.nextFloat() - 0.5f) * 6f
                }
            }
            VoiceVisualFeedbackMode.THINKING -> {
                ProceduralShapeGenerator.generateShape(
                    shape = ShapePrimitive.VOICE_THINKING_VORTEX,
                    count = count,
                    outX = targetX,
                    outY = targetY,
                    outZ = targetZ
                )
                state = HolographicState.FORMING
                for (i in 0 until count) {
                    velX[i] = (random.nextFloat() - 0.5f) * 8f
                    velY[i] = (random.nextFloat() - 0.5f) * 8f
                    velZ[i] = (random.nextFloat() - 0.5f) * 8f
                }
            }
            VoiceVisualFeedbackMode.SPEAKING -> {
                val targetShape = activeScene?.shape ?: ShapePrimitive.VOICE_SPEAKING_PULSE
                ProceduralShapeGenerator.generateShape(
                    shape = targetShape,
                    count = count,
                    outX = targetX,
                    outY = targetY,
                    outZ = targetZ,
                    textString = activeScene?.textToRender,
                    parameters = activeScene?.parameters ?: emptyMap()
                )
                state = HolographicState.ACTIVE
            }
            VoiceVisualFeedbackMode.NORMAL -> {
                if (activeScene != null) {
                    ProceduralShapeGenerator.generateShape(
                        shape = activeScene!!.shape,
                        count = count,
                        outX = targetX,
                        outY = targetY,
                        outZ = targetZ,
                        textString = activeScene!!.textToRender,
                        parameters = activeScene!!.parameters
                    )
                    state = HolographicState.FORMING
                } else {
                    ProceduralShapeGenerator.generateShape(
                        shape = ShapePrimitive.SPHERE,
                        count = count,
                        outX = targetX,
                        outY = targetY,
                        outZ = targetZ
                    )
                    state = HolographicState.IDLE
                }
            }
        }
    }

    fun update(dt: Float, timeSec: Float) {
        if (autoRotate || voiceFeedbackMode == VoiceVisualFeedbackMode.THINKING) {
            rotY += if (voiceFeedbackMode == VoiceVisualFeedbackMode.THINKING) 0.035f else 0.012f
        }

        val wingFlap = sin(timeSec * 7f) * 0.45f
        val pulse = 1f + 0.06f * sin(timeSec * 4f)
        val waveOsc = sin(timeSec * 5f) * 20f

        val hasVortex = activeScene?.behaviors?.contains(ParticleBehaviorType.VORTEX) == true || voiceFeedbackMode == VoiceVisualFeedbackMode.THINKING
        val hasWave = activeScene?.behaviors?.contains(ParticleBehaviorType.WAVE_OSCILLATE) == true || voiceFeedbackMode == VoiceVisualFeedbackMode.LISTENING
        val hasTurbulence = activeScene?.behaviors?.contains(ParticleBehaviorType.SURFACE_TURBULENCE) == true

        var settledCount = 0

        for (i in 0 until count) {
            when (state) {
                HolographicState.IDLE -> {
                    val tx = targetX[i] * pulse
                    val ty = targetY[i] * pulse
                    val tz = targetZ[i] * pulse
                    val dx = tx - curX[i]
                    val dy = ty - curY[i]
                    val dz = tz - curZ[i]
                    velX[i] = velX[i] * 0.88f + dx * 0.12f
                    velY[i] = velY[i] * 0.88f + dy * 0.12f
                    velZ[i] = velZ[i] * 0.88f + dz * 0.12f
                    curX[i] += velX[i]
                    curY[i] += velY[i]
                    curZ[i] += velZ[i]
                }

                HolographicState.FORMING -> {
                    val dx = targetX[i] - curX[i]
                    val dy = targetY[i] - curY[i]
                    val dz = targetZ[i] - curZ[i]
                    velX[i] = velX[i] * 0.90f + dx * 0.09f
                    velY[i] = velY[i] * 0.90f + dy * 0.09f
                    velZ[i] = velZ[i] * 0.90f + dz * 0.09f
                    curX[i] += velX[i]
                    curY[i] += velY[i]
                    curZ[i] += velZ[i]

                    if (abs(dx) < 16f && abs(dy) < 16f && abs(dz) < 16f) {
                        settledCount++
                    }
                }

                HolographicState.ACTIVE -> {
                    var tx = targetX[i]
                    var ty = targetY[i]
                    var tz = targetZ[i]

                    // Apply dynamic behaviors
                    if (activeShape == ShapePrimitive.BUTTERFLY) {
                        val wingFactor = abs(tx) / 250f
                        tz += sin(wingFlap * 2f) * wingFactor * 80f
                    } else if (activeShape == ShapePrimitive.HEART) {
                        tx *= pulse
                        ty *= pulse
                        tz *= pulse
                    } else if (hasVortex) {
                        val angle = 0.02f
                        val nx = tx * cos(angle) - tz * sin(angle)
                        val nz = tx * sin(angle) + tz * cos(angle)
                        tx = nx
                        tz = nz
                        targetX[i] = tx
                        targetZ[i] = tz
                    } else if (hasWave) {
                        val audioBoost = if (voiceFeedbackMode == VoiceVisualFeedbackMode.LISTENING) (voiceAudioLevel * 45f + 12f) else waveOsc
                        ty += audioBoost * sin(i * 0.05f + timeSec * 6f)
                    } else if (hasTurbulence) {
                        tx += sin(timeSec * 3f + i) * 3f
                        ty += cos(timeSec * 3f + i) * 3f
                    }

                    if (voiceFeedbackMode == VoiceVisualFeedbackMode.SPEAKING) {
                        val speakPulse = 1f + 0.10f * sin(timeSec * 14f) * (voiceAudioLevel.coerceAtLeast(0.4f))
                        tx *= speakPulse
                        ty *= speakPulse
                        tz *= speakPulse
                    }

                    val dx = tx - curX[i]
                    val dy = ty - curY[i]
                    val dz = tz - curZ[i]
                    velX[i] = velX[i] * 0.85f + dx * 0.15f
                    velY[i] = velY[i] * 0.85f + dy * 0.15f
                    velZ[i] = velZ[i] * 0.85f + dz * 0.15f
                    curX[i] += velX[i]
                    curY[i] += velY[i]
                    curZ[i] += velZ[i]
                }

                HolographicState.EXPLODING -> {
                    curX[i] += velX[i]
                    curY[i] += velY[i]
                    curZ[i] += velZ[i]
                    velX[i] *= 0.96f
                    velY[i] *= 0.96f
                    velZ[i] *= 0.96f
                }
            }
        }

        if (state == HolographicState.FORMING && settledCount > count * 0.72f) {
            state = HolographicState.ACTIVE
        }
    }

    fun projectToScreen(centerX: Float, centerY: Float) {
        val cosY = cos(rotY)
        val sinY = sin(rotY)
        val cosX = cos(rotX)
        val sinX = sin(rotX)

        val fov = 750f
        val coreLimit = min(count, 3200)

        for (i in 0 until count) {
            val x = curX[i] * zoom
            val y = curY[i] * zoom
            val z = curZ[i] * zoom

            // 3D rotation Y
            val x1 = x * cosY - z * sinY
            val z1 = x * sinY + z * cosY

            // 3D rotation X
            val y1 = y * cosX - z1 * sinX
            val z2 = y * sinX + z1 * cosX

            val depth = fov / (fov + z2).coerceAtLeast(100f)
            val px = centerX + x1 * depth
            val py = centerY + y1 * depth

            drawPts[i * 2] = px
            drawPts[i * 2 + 1] = py

            if (i < coreLimit) {
                corePts[i * 2] = px
                corePts[i * 2 + 1] = py
            }
        }
    }
}

// Backward compatibility alias
typealias HolographicParticleSystem = ProgrammableParticleEngine

@Composable
fun HolographicWorldEngineView(
    system: ProgrammableParticleEngine,
    onFpsCalculated: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var frameCount by remember { mutableStateOf(0) }
    var lastFpsTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }

    val primaryPaint = remember {
        Paint().apply {
            color = android.graphics.Color.argb(210, 0, 240, 255)
            strokeWidth = 2.4f
            isAntiAlias = false
            strokeCap = Paint.Cap.ROUND
        }
    }

    val whiteCorePaint = remember {
        Paint().apply {
            color = android.graphics.Color.argb(250, 255, 255, 255)
            strokeWidth = 3.6f
            isAntiAlias = false
            strokeCap = Paint.Cap.ROUND
        }
    }

    // Animation frame ticker loop
    LaunchedEffect(Unit) {
        var lastTime = System.nanoTime()
        while (true) {
            withInfiniteAnimationFrameMillis { nowMs ->
                val nowNano = System.nanoTime()
                val dt = ((nowNano - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastTime = nowNano

                system.update(dt, nowMs / 1000f)

                frameCount++
                val curTime = System.currentTimeMillis()
                if (curTime - lastFpsTimestamp >= 500) {
                    val currentFps = ((frameCount * 1000f) / (curTime - lastFpsTimestamp)).roundToInt()
                    onFpsCalculated(currentFps.coerceIn(12, 60))
                    frameCount = 0
                    lastFpsTimestamp = curTime
                }
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        // Double tap triggers interactive particle pulse / reform
                        system.reform()
                    },
                    onTap = { offset ->
                        // Single tap toggles continuous rotation
                        system.autoRotate = !system.autoRotate
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    system.rotY += dragAmount.x * 0.008f
                    system.rotX -= dragAmount.y * 0.008f
                    system.autoRotate = false
                }
            }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        val argb = system.baseColor.toArgb()
        val r = android.graphics.Color.red(argb)
        val g = android.graphics.Color.green(argb)
        val b = android.graphics.Color.blue(argb)
        primaryPaint.color = android.graphics.Color.argb(210, r, g, b)

        system.projectToScreen(cx, cy)

        // Draw primary 3D point cloud
        drawContext.canvas.nativeCanvas.drawPoints(system.drawPts, primaryPaint)

        // Draw radiant glowing inner core
        drawContext.canvas.nativeCanvas.drawPoints(system.corePts, whiteCorePaint)
    }
}
