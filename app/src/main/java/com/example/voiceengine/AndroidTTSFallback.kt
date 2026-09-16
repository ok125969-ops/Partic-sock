package com.example.voiceengine

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale
import java.util.UUID

class AndroidTTSFallback(private val context: Context) : TTSProvider {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var isCurrentlySpeaking = false
    private var isPausedState = false
    private var lastUtteranceId: String? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                setupPreferredVoice()
            } else {
                Log.e("AndroidTTSFallback", "Initialization failed with status: $status")
            }
        }
    }

    private fun setupPreferredVoice() {
        try {
            val hindi = Locale("hi", "IN")
            val langResult = tts?.isLanguageAvailable(hindi)
            val preferredLocale = if (langResult == TextToSpeech.LANG_AVAILABLE || langResult == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                hindi
            } else {
                Locale.US
            }
            tts?.language = preferredLocale

            // Search for available male voice
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
            Log.w("AndroidTTSFallback", "Voice preference setup notice: ${e.message}")
        }
    }

    private fun detectEmotion(text: String, baseEmotion: VoiceEmotion): VoiceEmotion {
        if (baseEmotion != VoiceEmotion.CASUAL) return baseEmotion
        val lower = text.lowercase()
        return when {
            lower.contains("hello") || lower.contains("namaste") || lower.contains("hey") ||
            lower.contains("welcome") || lower.contains("ready") || lower.contains("good morning") -> VoiceEmotion.GREETING

            lower.contains("done") || lower.contains("complete") || lower.contains("ho gaya") ||
            lower.contains("saved") || lower.contains("safal") -> VoiceEmotion.TASK_COMPLETED

            lower.contains("error") || lower.contains("failed") || lower.contains("dikkat") ||
            lower.contains("galti") || lower.contains("problem") -> VoiceEmotion.ERROR

            lower.contains("warning") || lower.contains("savdhan") || lower.contains("khatra") ||
            lower.contains("careful") -> VoiceEmotion.WARNING

            lower.contains("urgent") || lower.contains("jaldi") || lower.contains("turant") -> VoiceEmotion.URGENT

            else -> VoiceEmotion.CASUAL
        }
    }

    private fun preprocessText(text: String): String {
        var s = text
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

        // Smooth conversational pauses
        s = s.replace(";", ", ")
            .replace("—", ", ")
            .replace(Regex("""\s*,\s*"""), ", ")
            .replace(Regex("""\s*\.\s*"""), ". ")
        return s.trim()
    }

    override suspend fun synthesizeAndPlay(
        text: String,
        config: VoiceConfig,
        listener: VoiceEngineListener?,
        onAudioStreamReady: (suspend (ByteArray) -> Unit)?
    ): Boolean {
        if (!isInitialized) {
            var waitCount = 0
            while (!isInitialized && waitCount < 20) {
                kotlinx.coroutines.delay(100)
                waitCount++
            }
            if (!isInitialized) {
                listener?.onSpeechError(text, -1, "Android TTS not initialized")
                return false
            }
        }

        try {
            val detectedEmotion = detectEmotion(text, config.emotion)
            val effectiveConfig = config.copyWithEmotion(detectedEmotion)

            val locale = when {
                config.language.startsWith("hi", ignoreCase = true) -> Locale("hi", "IN")
                config.language.startsWith("en", ignoreCase = true) -> Locale.US
                else -> Locale("hi", "IN")
            }
            tts?.language = locale

            // Convert semitone pitch to Android TTS float multiplier (deep male baritone)
            val pitchMultiplier = Math.pow(2.0, (effectiveConfig.pitch / 12.0).toDouble()).toFloat().coerceIn(0.70f, 1.25f)
            tts?.setPitch(pitchMultiplier)
            tts?.setSpeechRate(effectiveConfig.speakingRate.coerceIn(0.7f, 1.4f))

            val processedText = preprocessText(text)
            val utteranceId = UUID.randomUUID().toString()
            lastUtteranceId = utteranceId

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    if (utteranceId == lastUtteranceId) {
                        isCurrentlySpeaking = true
                        listener?.onSpeechStart(text)
                    }
                }

                override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                    if (utteranceId == lastUtteranceId && text.isNotEmpty()) {
                        val progress = (end.toFloat() / text.length.toFloat()).coerceIn(0f, 1f)
                        val rms = 0.4f + (Math.random() * 0.5f).toFloat() // Acoustic pulse for visualizer
                        listener?.onSpeechProgress(text, end, progress, rms)
                    }
                }

                override fun onDone(utteranceId: String?) {
                    if (utteranceId == lastUtteranceId) {
                        isCurrentlySpeaking = false
                        listener?.onSpeechComplete(text)
                    }
                }

                override fun onError(utteranceId: String?) {
                    if (utteranceId == lastUtteranceId) {
                        isCurrentlySpeaking = false
                        listener?.onSpeechError(text, -2, "Android TTS playback error")
                    }
                }
            })

            val queueMode = TextToSpeech.QUEUE_FLUSH
            val params = android.os.Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, effectiveConfig.volume.coerceIn(0f, 1f))
            }

            val result = tts?.speak(processedText, queueMode, params, utteranceId)
            return result == TextToSpeech.SUCCESS
        } catch (e: Exception) {
            Log.e("AndroidTTSFallback", "Error speaking text", e)
            listener?.onSpeechError(text, -3, e.message ?: "Unknown exception")
            return false
        }
    }

    override fun stop() {
        try {
            tts?.stop()
            isCurrentlySpeaking = false
            isPausedState = false
        } catch (e: Exception) {
            Log.e("AndroidTTSFallback", "Error stopping TTS", e)
        }
    }

    override fun pause() {
        stop()
        isPausedState = true
    }

    override fun resume() {
        isPausedState = false
    }

    override fun isSpeaking(): Boolean {
        return tts?.isSpeaking() == true || isCurrentlySpeaking
    }

    override fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (e: Exception) {
            Log.e("AndroidTTSFallback", "Error releasing TTS", e)
        }
    }
}

