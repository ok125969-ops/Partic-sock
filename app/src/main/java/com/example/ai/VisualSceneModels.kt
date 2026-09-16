package com.example.ai

import androidx.compose.ui.graphics.Color

enum class ShapePrimitive {
    NONE,
    SPHERE,
    EARTH,
    CUBE,
    CYLINDER,
    CONE,
    TORUS,
    RINGS,
    SPIRAL,
    GALAXY,
    HELIX,
    DNA,
    WAVE,
    GRID,
    PLANE,
    TEXT,
    ATOM,
    MOLECULE_H2O,
    PROJECTILE_MOTION,
    PHOTOSYNTHESIS,
    HUMAN_SILHOUETTE,
    BUTTERFLY,
    CAR,
    DRAGON,
    HEART,
    ROBOT,
    SOLAR_SYSTEM,
    FLOWER,
    JET,
    CUSTOM,
    EXPLOSION_FIELD,
    CUSTOM_CLOUD,
    VOICE_LISTENING_WAVEFORM,
    VOICE_THINKING_VORTEX,
    VOICE_SPEAKING_PULSE
}

enum class ParticleBehaviorType {
    IDLE_PULSE,
    ROTATE,
    ORBIT,
    WAVE_OSCILLATE,
    SURFACE_TURBULENCE,
    VORTEX,
    ATTRACT,
    REPEL,
    EXPLODE,
    IMPLODE,
    DISSOLVE,
    REFORM,
    MAGNETIC,
    SPRING
}

data class VisualParameter(
    val name: String,
    var value: Float,
    val unit: String = "",
    val min: Float = 0.1f,
    val max: Float = 10f,
    val label: String
)

data class TeachingStep(
    val stepId: Int,
    val title: String,
    val narration: String,
    val formula: String? = null,
    val shape: ShapePrimitive,
    val displayText: String? = null,
    val primaryColor: Color = Color(0xFF00F0FF),
    val secondaryColor: Color = Color(0xFFFFFFFF),
    val behaviors: List<ParticleBehaviorType> = listOf(ParticleBehaviorType.ROTATE),
    val parameterUpdates: Map<String, Float> = emptyMap(),
    val question: String? = null,
    val options: List<String> = emptyList(),
    val correctOptionIndex: Int = 0
)

data class VisualScene(
    val sceneId: String,
    val title: String,
    val topic: String,
    val shape: ShapePrimitive,
    val particleCount: Int = 13107,
    val textToRender: String? = null,
    val baseColor: Color = Color(0xFF00F0FF),
    val behaviors: MutableList<ParticleBehaviorType> = mutableListOf(ParticleBehaviorType.ROTATE),
    val parameters: MutableMap<String, VisualParameter> = mutableMapOf(),
    val currentFormula: String? = null,
    val steps: List<TeachingStep> = emptyList(),
    var currentStepIndex: Int = 0,
    val explanation: String = ""
)
