package com.example.voiceengine

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class NeuralTTSProvider(private val context: Context) : TTSProvider {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val audioPlayer = VoiceAudioPlayer(context)
    private var isCurrentlySpeaking = false
    private var lastSpokenText = ""

    private fun getApiKey(): String {
        return try {
            val field = BuildConfig::class.java.getField("ULTRON_TTS_API_KEY")
            field.get(null) as? String ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun detectEmotion(text: String, baseEmotion: VoiceEmotion): VoiceEmotion {
        if (baseEmotion != VoiceEmotion.CASUAL) return baseEmotion
        val lower = text.lowercase()
        return when {
            lower.contains("hello") || lower.contains("namaste") || lower.contains("hey") ||
            lower.contains("welcome") || lower.contains("ready") || lower.contains("good morning") ||
            lower.contains("good evening") -> VoiceEmotion.GREETING

            lower.contains("done") || lower.contains("complete") || lower.contains("ho gaya") ||
            lower.contains("saved") || lower.contains("safal") || lower.contains("successful") -> VoiceEmotion.TASK_COMPLETED

            lower.contains("error") || lower.contains("failed") || lower.contains("dikkat") ||
            lower.contains("galti") || lower.contains("problem") || lower.contains("unable") -> VoiceEmotion.ERROR

            lower.contains("warning") || lower.contains("savdhan") || lower.contains("khatra") ||
            lower.contains("careful") || lower.contains("alert") || lower.contains("caution") -> VoiceEmotion.WARNING

            lower.contains("urgent") || lower.contains("emergency") || lower.contains("jaldi") ||
            lower.contains("turant") || lower.contains("critical") -> VoiceEmotion.URGENT

            else -> VoiceEmotion.CASUAL
        }
    }

    private fun buildSsml(text: String, emotion: VoiceEmotion, config: VoiceConfig): String {
        // Sanitize for XML compliance
        var s = text
            .replace("&", " and ")
            .replace("<", "")
            .replace(">", "")
            .replace("\"", "")

        // Expand common units and technical abbreviations for natural cadence
        s = s
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

        // Insert natural conversational pauses around punctuation
        s = s.replace(Regex("""\s*,\s*"""), ", <break time=\"220ms\"/> ")
            .replace(Regex("""\s*;\s*"""), "; <break time=\"280ms\"/> ")
            .replace(Regex("""\s*:\s*"""), ": <break time=\"250ms\"/> ")
            .replace(Regex("""\s*—\s*"""), ", <break time=\"240ms\"/> ")
            .replace(Regex("""\s*\.\s+"""), ". <break time=\"420ms\"/> ")
            .replace(Regex("""\s*\?\s+"""), "? <break time=\"400ms\"/> ")
            .replace(Regex("""\s*!\s+"""), "! <break time=\"360ms\"/> ")

        val effectiveConfig = config.copyWithEmotion(emotion)
        val pitchStr = "${effectiveConfig.pitch}st"
        val ratePct = "${(effectiveConfig.speakingRate * 100).toInt()}%"

        return "<speak><prosody pitch=\"$pitchStr\" rate=\"$ratePct\">$s</prosody></speak>"
    }

    override suspend fun synthesizeAndPlay(
        text: String,
        config: VoiceConfig,
        listener: VoiceEngineListener?,
        onAudioStreamReady: (suspend (ByteArray) -> Unit)?
    ): Boolean = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_API_KEY" || apiKey == "YOUR_API_KEY") {
            Log.w("NeuralTTSProvider", "Neural TTS API key not configured. Triggering Android TTS fallback.")
            return@withContext false
        }

        val detectedEmotion = detectEmotion(text, config.emotion)
        val effectiveConfig = config.copyWithEmotion(detectedEmotion)
        val ssmlPayload = buildSsml(text, detectedEmotion, effectiveConfig)
        lastSpokenText = text

        try {
            // Select deep, confident male neural voice
            val voiceName = if (effectiveConfig.voiceId.isNotBlank()) effectiveConfig.voiceId else "hi-IN-Neural2-B"
            val langCode = if (voiceName.startsWith("en", ignoreCase = true)) "en-IN" else "hi-IN"

            val jsonBody = JSONObject().apply {
                put("input", JSONObject().put("ssml", ssmlPayload))
                put("voice", JSONObject()
                    .put("languageCode", langCode)
                    .put("name", voiceName)
                    .put("ssmlGender", "MALE"))
                put("audioConfig", JSONObject()
                    .put("audioEncoding", "MP3")
                    .put("speakingRate", 1.0)
                    .put("pitch", 0.0) // Pitch is controlled inside SSML prosody
                    .put("volumeGainDb", (Math.log10(effectiveConfig.volume.toDouble().coerceAtLeast(0.01)) * 20.0)))
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("https://texttospeech.googleapis.com/v1/text:synthesize?key=$apiKey")
                .post(requestBody)
                .build()

            listener?.onSpeechStart(text)
            isCurrentlySpeaking = true

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: "HTTP error ${response.code}"
                    Log.e("NeuralTTSProvider", "TTS API failed: $err")
                    isCurrentlySpeaking = false
                    listener?.onSpeechError(text, response.code, err)
                    return@withContext false
                }

                val responseJsonStr = response.body?.string() ?: ""
                val responseJson = JSONObject(responseJsonStr)
                val base64Audio = responseJson.optString("audioContent")

                if (base64Audio.isNullOrEmpty()) {
                    isCurrentlySpeaking = false
                    listener?.onSpeechError(text, -4, "Empty audio stream received")
                    return@withContext false
                }

                val audioBytes = Base64.decode(base64Audio, Base64.DEFAULT)
                if (audioBytes.isEmpty()) {
                    isCurrentlySpeaking = false
                    listener?.onSpeechError(text, -4, "Zero audio byte buffer")
                    return@withContext false
                }

                if (onAudioStreamReady != null) {
                    onAudioStreamReady(audioBytes)
                }

                // Play audio via audio player
                kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                    audioPlayer.playAudioBytes(
                        audioBytes = audioBytes,
                        volume = effectiveConfig.volume,
                        onComplete = {
                            isCurrentlySpeaking = false
                            listener?.onSpeechComplete(text)
                            if (continuation.isActive) continuation.resume(Unit) {}
                        },
                        onError = { errStr ->
                            isCurrentlySpeaking = false
                            listener?.onSpeechError(text, -5, errStr)
                            if (continuation.isActive) continuation.resume(Unit) {}
                        }
                    )
                }

                return@withContext true
            }
        } catch (e: Exception) {
            Log.e("NeuralTTSProvider", "Exception during neural TTS synthesis", e)
            isCurrentlySpeaking = false
            listener?.onSpeechError(text, -6, e.message ?: "Network or parsing exception")
            return@withContext false
        }
    }

    override fun stop() {
        audioPlayer.stop()
        isCurrentlySpeaking = false
    }

    override fun pause() {
        audioPlayer.pause()
        isCurrentlySpeaking = false
    }

    override fun resume() {
        audioPlayer.resume()
        isCurrentlySpeaking = true
    }

    override fun isSpeaking(): Boolean {
        return isCurrentlySpeaking || audioPlayer.isPlaying()
    }

    override fun release() {
        stop()
    }
}

