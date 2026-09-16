package com.example.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.*
import java.util.Locale

interface SpeechOutput {
    fun speak(
        text: String,
        utteranceId: String = System.currentTimeMillis().toString(),
        onStart: (() -> Unit)? = null,
        onDone: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    )
    fun pause()
    fun stop()
    fun queue(text: String)
    fun flush()
    fun isSpeaking(): Boolean
    fun shutdown()
}

class UltronTTSController(
    private val context: Context,
    private val onStateChanged: ((VoiceOutputState) -> Unit)? = null,
    private val onAudioPulse: ((Float) -> Unit)? = null
) : SpeechOutput, TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val pendingQueue = mutableListOf<String>()

    private val callbackMap = mutableMapOf<String, Triple<(() -> Unit)?, (() -> Unit)?, ((String) -> Unit)?>>()
    private var pulseJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    var currentState: VoiceOutputState = VoiceOutputState.IDLE
        private set(value) {
            field = value
            onStateChanged?.invoke(value)
        }

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            currentState = VoiceOutputState.ERROR
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            val hindi = Locale("hi", "IN")
            val hindiResult = tts?.isLanguageAvailable(hindi)
            val preferredLocale = if (hindiResult == TextToSpeech.LANG_AVAILABLE || hindiResult == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                hindi
            } else {
                Locale.US
            }
            tts?.language = preferredLocale

            try {
                val voices = tts?.voices
                val maleVoice = voices?.firstOrNull { v ->
                    val name = v.name.lowercase()
                    (name.contains("male") || name.contains("-b-") || name.contains("-d-")) &&
                    (v.locale.language == preferredLocale.language)
                } ?: voices?.firstOrNull { v ->
                    val name = v.name.lowercase()
                    name.contains("male") || name.contains("-b-") || name.contains("-d-")
                }
                if (maleVoice != null) {
                    tts?.voice = maleVoice
                }
            } catch (e: Exception) {
                // Ignore voice selection fallback
            }

            tts?.setSpeechRate(0.98f)
            tts?.setPitch(0.92f) // Deep baritone tone for ULTRON persona

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    coroutineScope.launch {
                        currentState = VoiceOutputState.SPEAKING
                        startAudioPulseSimulation()
                        utteranceId?.let { callbackMap[it]?.first?.invoke() }
                    }
                }

                override fun onDone(utteranceId: String?) {
                    coroutineScope.launch {
                        stopAudioPulseSimulation()
                        currentState = VoiceOutputState.IDLE
                        utteranceId?.let {
                            callbackMap[it]?.second?.invoke()
                            callbackMap.remove(it)
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    coroutineScope.launch {
                        stopAudioPulseSimulation()
                        currentState = VoiceOutputState.ERROR
                        utteranceId?.let {
                            callbackMap[it]?.third?.invoke("TTS playback error on utterance $utteranceId")
                            callbackMap.remove(it)
                        }
                    }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    coroutineScope.launch {
                        stopAudioPulseSimulation()
                        currentState = VoiceOutputState.ERROR
                        utteranceId?.let {
                            callbackMap[it]?.third?.invoke("TTS error code: $errorCode")
                            callbackMap.remove(it)
                        }
                    }
                }
            })

            // Process any pending queued items
            if (pendingQueue.isNotEmpty()) {
                val combined = pendingQueue.joinToString(" ")
                pendingQueue.clear()
                speak(combined)
            }
        } else {
            currentState = VoiceOutputState.ERROR
        }
    }

    override fun speak(
        text: String,
        utteranceId: String,
        onStart: (() -> Unit)?,
        onDone: (() -> Unit)?,
        onError: ((String) -> Unit)?
    ) {
        if (text.isBlank()) {
            onDone?.invoke()
            return
        }

        callbackMap[utteranceId] = Triple(onStart, onDone, onError)

        if (!isInitialized || tts == null) {
            pendingQueue.add(text)
            return
        }

        // Dynamically select language based on script or keywords
        val containsDevanagariOrHindi = text.any { it in '\u0900'..'\u097F' } ||
                text.contains("hai", ignoreCase = true) ||
                text.contains("karo", ignoreCase = true) ||
                text.contains("samjhao", ignoreCase = true) ||
                text.contains("ruko", ignoreCase = true)

        if (containsDevanagariOrHindi) {
            val hindi = Locale("hi", "IN")
            if (tts?.isLanguageAvailable(hindi) == TextToSpeech.LANG_AVAILABLE) {
                tts?.language = hindi
            }
        } else {
            tts?.language = Locale.US
        }

        val cleanedText = text
            .replace(Regex("""(?i)\bULTRON\b"""), "Ultron")
            .replace(Regex("""(?i)\bAI\b"""), "A.I.")
            .replace(Regex("""(?i)\bOS\b"""), "O.S.")
            .replace(Regex("""(?i)\bUI\b"""), "U.I.")
            .replace(Regex("""(?i)\bAPI\b"""), "A.P.I.")
            .replace(Regex("""(?i)\bTTS\b"""), "T.T.S.")
            .replace(Regex("""(?i)\b3D\b"""), "Three D")
            .replace(Regex("""(?i)\b(\d+)\s*m/s²"""), "$1 meters per second squared")
            .replace(Regex("""(?i)\b(\d+)\s*km/h"""), "$1 kilometers per hour")
            .replace(Regex("""(?i)\b(\d+)\s*kg\b"""), "$1 kilograms")
            .replace(Regex("""(?i)\b(\d+)\s*hz\b"""), "$1 Hertz")
            .replace(Regex("""(?i)\b(\d+)\s*n\b"""), "$1 Newtons")
            .replace(Regex("""(?i)\b(\d+)\s*%"""), "$1 percent")
            .replace(";", ", ")
            .replace("—", ", ")

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        val result = tts?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            currentState = VoiceOutputState.ERROR
            onError?.invoke("Failed to dispatch TTS speak request.")
        }
    }

    override fun pause() {
        stop()
        currentState = VoiceOutputState.PAUSED
    }

    override fun stop() {
        stopAudioPulseSimulation()
        try {
            tts?.stop()
        } catch (_: Exception) {}
        currentState = VoiceOutputState.STOPPED
        callbackMap.clear()
    }

    override fun queue(text: String) {
        if (!isInitialized || tts == null) {
            pendingQueue.add(text)
            return
        }
        val utteranceId = System.currentTimeMillis().toString()
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        tts?.speak(text, TextToSpeech.QUEUE_ADD, params, utteranceId)
    }

    override fun flush() {
        stop()
    }

    override fun isSpeaking(): Boolean {
        return tts?.isSpeaking == true || currentState == VoiceOutputState.SPEAKING
    }

    private fun startAudioPulseSimulation() {
        pulseJob?.cancel()
        pulseJob = coroutineScope.launch {
            var phase = 0f
            while (isActive) {
                phase += 0.25f
                val amplitude = 0.4f + 0.5f * kotlin.math.abs(kotlin.math.sin(phase))
                onAudioPulse?.invoke(amplitude)
                delay(40)
            }
        }
    }

    private fun stopAudioPulseSimulation() {
        pulseJob?.cancel()
        pulseJob = null
        onAudioPulse?.invoke(0f)
    }

    override fun shutdown() {
        stopAudioPulseSimulation()
        coroutineScope.cancel()
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        currentState = VoiceOutputState.IDLE
    }
}
