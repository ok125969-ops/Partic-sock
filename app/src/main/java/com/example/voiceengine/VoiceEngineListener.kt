package com.example.voiceengine

interface VoiceEngineListener {
    fun onSpeechStart(text: String)
    fun onSpeechProgress(text: String, charIndex: Int, progressPercent: Float, rmsAmplitude: Float)
    fun onSpeechComplete(text: String)
    fun onSpeechError(text: String, errorCode: Int, errorMessage: String)
}
