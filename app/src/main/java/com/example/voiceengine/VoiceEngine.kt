package com.example.voiceengine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class VoiceEngine(
    private val context: Context,
    private var config: VoiceConfig = VoiceConfig(),
    private val listener: VoiceEngineListener? = null
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val neuralProvider = NeuralTTSProvider(context)
    private val fallbackProvider = AndroidTTSFallback(context)
    
    private var currentActiveProvider: TTSProvider = neuralProvider
    private val speechQueue = VoiceSpeechQueue(scope) { text ->
        executeSpeakInternal(text)
    }

    private suspend fun executeSpeakInternal(text: String) {
        // Try Neural TTS first
        currentActiveProvider = neuralProvider
        val success = neuralProvider.synthesizeAndPlay(text, config, listener)

        if (!success) {
            Log.w("VoiceEngine", "Neural TTS failed or unavailable. Falling back to Android TTS.")
            currentActiveProvider = fallbackProvider
            fallbackProvider.synthesizeAndPlay(text, config, listener)
        }
    }

    fun speak(text: String, emotion: VoiceEmotion? = null) {
        if (text.isBlank()) return
        if (emotion != null) {
            config = config.copyWithEmotion(emotion)
        }
        speechQueue.enqueue(text)
    }

    fun setEmotion(emotion: VoiceEmotion) {
        config = config.copyWithEmotion(emotion)
    }

    fun stop() {
        speechQueue.clearQueue()
        neuralProvider.stop()
        fallbackProvider.stop()
    }

    fun pause() {
        currentActiveProvider.pause()
    }

    fun resume() {
        currentActiveProvider.resume()
    }

    fun isSpeaking(): Boolean {
        return neuralProvider.isSpeaking() || fallbackProvider.isSpeaking()
    }

    fun clearQueue() {
        speechQueue.clearQueue()
    }

    fun setVoice(voiceId: String) {
        config = config.copy(voiceId = voiceId)
    }

    fun setRate(rate: Float) {
        config = config.copy(speakingRate = rate.coerceIn(0.5f, 2.0f))
    }

    fun setPitch(pitch: Float) {
        config = config.copy(pitch = pitch.coerceIn(-6.0f, 6.0f))
    }

    fun setVolume(volume: Float) {
        config = config.copy(volume = volume.coerceIn(0.0f, 1.0f))
    }

    fun setLanguage(language: String) {
        config = config.copy(language = language)
    }

    fun release() {
        stop()
        neuralProvider.release()
        fallbackProvider.release()
    }
}
