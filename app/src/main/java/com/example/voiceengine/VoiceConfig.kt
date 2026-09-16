package com.example.voiceengine

enum class VoiceEmotion {
    GREETING,
    TASK_COMPLETED,
    ERROR,
    WARNING,
    CASUAL,
    URGENT
}

data class VoiceConfig(
    val voiceId: String = "hi-IN-Neural2-B", // Deep, calm, confident neural male voice
    val language: String = "hi-IN", // Supports hi-IN, en-US, hinglish
    val speakingRate: Float = 0.98f, // Calm, measured pacing
    val pitch: Float = -1.5f, // Deep, resonant male pitch (semitones in SSML)
    val volume: Float = 1.0f, // 0.0f to 1.0f
    val style: String = "calm_authoritative",
    val emotion: VoiceEmotion = VoiceEmotion.CASUAL
) {
    fun copyWithEmotion(newEmotion: VoiceEmotion): VoiceConfig {
        return when (newEmotion) {
            VoiceEmotion.GREETING -> copy(
                emotion = newEmotion,
                pitch = -1.2f,
                speakingRate = 0.99f
            )
            VoiceEmotion.TASK_COMPLETED -> copy(
                emotion = newEmotion,
                pitch = -1.5f,
                speakingRate = 1.00f
            )
            VoiceEmotion.ERROR -> copy(
                emotion = newEmotion,
                pitch = -1.0f,
                speakingRate = 0.95f
            )
            VoiceEmotion.WARNING -> copy(
                emotion = newEmotion,
                pitch = -2.0f,
                speakingRate = 0.96f
            )
            VoiceEmotion.CASUAL -> copy(
                emotion = newEmotion,
                pitch = -1.5f,
                speakingRate = 1.00f
            )
            VoiceEmotion.URGENT -> copy(
                emotion = newEmotion,
                pitch = -0.8f,
                speakingRate = 1.08f
            )
        }
    }
}
