package com.example.voice

import androidx.compose.ui.graphics.Color
import com.example.ai.ParticleBehaviorType
import com.example.ai.ShapePrimitive

enum class VoiceInputState {
    IDLE,
    LISTENING,
    PROCESSING,
    THINKING,
    EXECUTING,
    SPEAKING,
    INTERRUPTED,
    ERROR
}

enum class VoiceOutputState {
    IDLE,
    SPEAKING,
    PAUSED,
    STOPPED,
    ERROR
}

data class SpeechResult(
    val text: String,
    val isFinal: Boolean,
    val confidence: Float = 1.0f
)

enum class VisualTimelineEventType {
    SHOW_OBJECT,
    HIDE_OBJECT,
    MOVE_OBJECT,
    ROTATE_OBJECT,
    CHANGE_PARAMETER,
    START_PARTICLE_EFFECT,
    STOP_PARTICLE_EFFECT,
    SHOW_FORMULA,
    SHOW_LABEL,
    CAMERA_FOCUS,
    PAUSE,
    WAIT
}

data class TimelineEvent(
    val type: VisualTimelineEventType,
    val target: String? = null,
    val value: Any? = null,
    val delayMs: Long = 0L,
    val description: String = ""
)

data class VoiceSessionState(
    val inputState: VoiceInputState = VoiceInputState.IDLE,
    val outputState: VoiceOutputState = VoiceOutputState.IDLE,
    val recognizedPartialText: String = "",
    val recognizedFinalText: String = "",
    val activeNarrationText: String = "",
    val errorMessage: String? = null,
    val audioRmsLevel: Float = 0f,
    val isWakeWordArmed: Boolean = true,
    val activeLessonTitle: String = "",
    val currentStepNumber: Int = 1,
    val totalSteps: Int = 1,
    val currentFormula: String? = null,
    val isPaused: Boolean = false,
    val isApiConfigured: Boolean = false
)
