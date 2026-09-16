package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

interface SpeechInputController {
    fun startListening(
        onPartial: (String) -> Unit,
        onFinal: (SpeechResult) -> Unit,
        onError: (String) -> Unit,
        onRmsChanged: (Float) -> Unit,
        onStateChanged: ((VoiceInputState) -> Unit)? = null
    )
    fun stopListening()
    fun cancel()
    fun isListening(): Boolean
    fun destroy()
}

class UltronSpeechRecognizer(
    private val context: Context
) : SpeechInputController {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isCurrentlyListening = false
    private val mainScope = CoroutineScope(Dispatchers.Main)

    private var partialCallback: ((String) -> Unit)? = null
    private var finalCallback: ((SpeechResult) -> Unit)? = null
    private var errorCallback: ((String) -> Unit)? = null
    private var rmsCallback: ((Float) -> Unit)? = null
    private var stateCallback: ((VoiceInputState) -> Unit)? = null

    init {
        ensureRecognizer()
    }

    private fun ensureRecognizer() {
        if (speechRecognizer == null && SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                }
            } catch (_: Exception) {
                speechRecognizer = null
            }
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isCurrentlyListening = true
                mainScope.launch {
                    stateCallback?.invoke(VoiceInputState.LISTENING)
                }
            }

            override fun onBeginningOfSpeech() {
                mainScope.launch {
                    stateCallback?.invoke(VoiceInputState.LISTENING)
                }
            }

            override fun onRmsChanged(rmsdB: Float) {
                // rmsdB ranges typically from -2 to 10+
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                mainScope.launch {
                    rmsCallback?.invoke(normalized)
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isCurrentlyListening = false
                mainScope.launch {
                    stateCallback?.invoke(VoiceInputState.PROCESSING)
                    rmsCallback?.invoke(0f)
                }
            }

            override fun onError(error: Int) {
                isCurrentlyListening = false
                val message = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "Sorry, mujhe clearly sunai nahi diya. Please repeat karein."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Voice timeout: Kuch bola nahi gaya. Mic tap karke dobara bole."
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording me error aaya."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required for voice interaction."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network issue detected in speech recognition."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy. Resetting session..."
                    else -> "Voice recognition issue (Code: $error)."
                }
                mainScope.launch {
                    stateCallback?.invoke(VoiceInputState.ERROR)
                    errorCallback?.invoke(message)
                    rmsCallback?.invoke(0f)
                }
            }

            override fun onResults(results: Bundle?) {
                isCurrentlyListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val scores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                val recognizedText = matches?.firstOrNull()?.trim() ?: ""
                val confidence = scores?.firstOrNull() ?: 1.0f

                mainScope.launch {
                    if (recognizedText.isNotBlank()) {
                        stateCallback?.invoke(VoiceInputState.PROCESSING)
                        finalCallback?.invoke(SpeechResult(text = recognizedText, isFinal = true, confidence = confidence))
                    } else {
                        stateCallback?.invoke(VoiceInputState.ERROR)
                        errorCallback?.invoke("Sorry, mujhe clearly sunai nahi diya.")
                    }
                    rmsCallback?.invoke(0f)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull() ?: ""
                if (partial.isNotBlank()) {
                    mainScope.launch {
                        partialCallback?.invoke(partial)
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    override fun startListening(
        onPartial: (String) -> Unit,
        onFinal: (SpeechResult) -> Unit,
        onError: (String) -> Unit,
        onRmsChanged: (Float) -> Unit,
        onStateChanged: ((VoiceInputState) -> Unit)?
    ) {
        partialCallback = onPartial
        finalCallback = onFinal
        errorCallback = onError
        rmsCallback = onRmsChanged
        stateCallback = onStateChanged

        ensureRecognizer()

        if (speechRecognizer == null) {
            onError("Android Speech Recognition is not available on this device/emulator.")
            stateCallback?.invoke(VoiceInputState.ERROR)
            return
        }

        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
            isCurrentlyListening = true
            stateCallback?.invoke(VoiceInputState.LISTENING)
        } catch (e: Exception) {
            isCurrentlyListening = false
            onError("Unable to initialize speech recognizer: ${e.localizedMessage}")
            stateCallback?.invoke(VoiceInputState.ERROR)
        }
    }

    override fun stopListening() {
        if (isCurrentlyListening) {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {}
            isCurrentlyListening = false
        }
    }

    override fun cancel() {
        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) {}
        isCurrentlyListening = false
        rmsCallback?.invoke(0f)
    }

    override fun isListening(): Boolean = isCurrentlyListening

    override fun destroy() {
        cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
