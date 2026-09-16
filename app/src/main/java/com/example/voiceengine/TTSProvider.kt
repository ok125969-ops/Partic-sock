package com.example.voiceengine

import android.content.Context

interface TTSProvider {
    suspend fun synthesizeAndPlay(
        text: String,
        config: VoiceConfig,
        listener: VoiceEngineListener?,
        onAudioStreamReady: (suspend (ByteArray) -> Unit)? = null
    ): Boolean

    fun stop()
    fun pause()
    fun resume()
    fun isSpeaking(): Boolean
    fun release()
}
