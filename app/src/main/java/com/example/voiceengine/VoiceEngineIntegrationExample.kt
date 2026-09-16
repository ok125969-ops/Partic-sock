package com.example.voiceengine

import android.content.Context
import android.util.Log

/**
 * Example integration class showing how ULTRON initializes and calls the VoiceEngine.
 * Per requirements, this demonstrates ULTRON calling:
 * voiceEngine.speak("Hello Boss, I am ready.")
 */
class VoiceEngineIntegrationExample(private val context: Context) {

    private val voiceConfig = VoiceConfig(
        voiceId = "hi-IN-Neural2-A",
        language = "hi-IN",
        speakingRate = 1.05f,
        pitch = 1.0f,
        volume = 1.0f,
        style = "authoritative_assistant"
    )

    private val voiceListener = object : VoiceEngineListener {
        override fun onSpeechStart(text: String) {
            Log.d("UltronVoice", "Speech started: $text")
        }

        override fun onSpeechProgress(text: String, charIndex: Int, progressPercent: Float, rmsAmplitude: Float) {
            // Can be used by Particle Engine to pulse animations in sync with speech
            Log.d("UltronVoice", "Progress: ${(progressPercent * 100).toInt()}% | RMS: $rmsAmplitude")
        }

        override fun onSpeechComplete(text: String) {
            Log.d("UltronVoice", "Speech completed: $text")
        }

        override fun onSpeechError(text: String, errorCode: Int, errorMessage: String) {
            Log.e("UltronVoice", "Speech error [$errorCode]: $errorMessage for text: $text")
        }
    }

    private val voiceEngine = VoiceEngine(context, voiceConfig, voiceListener)

    fun initializeAndGreet() {
        // ULTRON greeting call as requested
        voiceEngine.speak("Hello Boss, I am ready.")
        voiceEngine.speak("Namaste Boss, Ultron voice engine online.")
    }

    fun shutdown() {
        voiceEngine.release()
    }
}
