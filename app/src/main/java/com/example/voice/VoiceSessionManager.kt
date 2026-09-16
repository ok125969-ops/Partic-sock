package com.example.voice

import android.content.Context
import com.example.ai.ConversationManager
import com.example.ai.UltronAiClient
import com.example.ai.VisualScene
import com.example.ui.components.HolographicState
import com.example.ui.components.ProgrammableParticleEngine
import com.example.ui.components.VoiceVisualFeedbackMode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VoiceSessionManager(
    private val context: Context,
    private val particleEngine: ProgrammableParticleEngine,
    private val conversationManager: ConversationManager,
    private val speechRecognizer: SpeechInputController = UltronSpeechRecognizer(context),
    private val ttsController: SpeechOutput? = null
) {
    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _sessionState = MutableStateFlow(VoiceSessionState())
    val sessionState: StateFlow<VoiceSessionState> = _sessionState.asStateFlow()

    private val actualTtsController: SpeechOutput = ttsController ?: UltronTTSController(
        context = context,
        onStateChanged = { outputState ->
            _sessionState.value = _sessionState.value.copy(outputState = outputState)
            if (outputState == VoiceOutputState.SPEAKING) {
                _sessionState.value = _sessionState.value.copy(inputState = VoiceInputState.SPEAKING)
                particleEngine.setVoiceFeedbackMode(VoiceVisualFeedbackMode.SPEAKING)
            } else if (outputState == VoiceOutputState.IDLE && _sessionState.value.inputState == VoiceInputState.SPEAKING) {
                _sessionState.value = _sessionState.value.copy(inputState = VoiceInputState.IDLE)
                particleEngine.setVoiceFeedbackMode(VoiceVisualFeedbackMode.NORMAL)
            }
        },
        onAudioPulse = { pulse ->
            particleEngine.voiceAudioLevel = pulse
            _sessionState.value = _sessionState.value.copy(audioRmsLevel = pulse)
        }
    )

    val teacherController = TeacherController(
        particleEngine = particleEngine,
        ttsController = actualTtsController,
        onStepUpdated = { stepNum, total, step, formula ->
            _sessionState.value = _sessionState.value.copy(
                currentStepNumber = stepNum,
                totalSteps = total,
                activeNarrationText = step.narration,
                currentFormula = formula
            )
        },
        onLessonComplete = {
            _sessionState.value = _sessionState.value.copy(
                inputState = VoiceInputState.IDLE,
                activeNarrationText = "Lesson completed."
            )
            particleEngine.setVoiceFeedbackMode(VoiceVisualFeedbackMode.NORMAL)
        }
    )

    private val wakeWordDetector: WakeWordDetector = DefaultWakeWordDetector()

    init {
        _sessionState.value = _sessionState.value.copy(
            isApiConfigured = UltronAiClient.isConfigured()
        )
    }

    fun startListening() {
        // If teacher is currently speaking, user initiates interruption!
        if (_sessionState.value.inputState == VoiceInputState.SPEAKING || actualTtsController.isSpeaking()) {
            handleTeacherInterruption()
        }

        _sessionState.value = _sessionState.value.copy(
            inputState = VoiceInputState.LISTENING,
            errorMessage = null,
            recognizedPartialText = ""
        )

        // Morph particle engine to responsive circular waveform
        particleEngine.setVoiceFeedbackMode(VoiceVisualFeedbackMode.LISTENING)

        speechRecognizer.startListening(
            onPartial = { partial ->
                _sessionState.value = _sessionState.value.copy(
                    recognizedPartialText = partial,
                    inputState = VoiceInputState.LISTENING
                )
            },
            onFinal = { result ->
                _sessionState.value = _sessionState.value.copy(
                    recognizedFinalText = result.text,
                    recognizedPartialText = "",
                    inputState = VoiceInputState.PROCESSING
                )
                processSpeechInput(result.text)
            },
            onError = { error ->
                _sessionState.value = _sessionState.value.copy(
                    inputState = VoiceInputState.ERROR,
                    errorMessage = error
                )
                particleEngine.setVoiceFeedbackMode(VoiceVisualFeedbackMode.NORMAL)
            },
            onRmsChanged = { rms ->
                particleEngine.voiceAudioLevel = rms
                _sessionState.value = _sessionState.value.copy(audioRmsLevel = rms)
            },
            onStateChanged = { state ->
                _sessionState.value = _sessionState.value.copy(inputState = state)
            }
        )
    }

    fun stopListening() {
        speechRecognizer.stopListening()
        if (_sessionState.value.inputState == VoiceInputState.LISTENING) {
            _sessionState.value = _sessionState.value.copy(inputState = VoiceInputState.PROCESSING)
        }
    }

    fun cancelListening() {
        speechRecognizer.cancel()
        _sessionState.value = _sessionState.value.copy(inputState = VoiceInputState.IDLE)
        particleEngine.setVoiceFeedbackMode(VoiceVisualFeedbackMode.NORMAL)
    }

    /**
     * Interruption Support:
     * When user speaks or taps mic during teacher narration:
     * SPEAKING -> INTERRUPTED -> LISTENING
     */
    fun handleTeacherInterruption() {
        teacherController.interrupt()
        _sessionState.value = _sessionState.value.copy(
            inputState = VoiceInputState.INTERRUPTED,
            isPaused = true
        )
    }

    fun togglePauseResume() {
        if (teacherController.isPaused) {
            teacherController.resume()
            _sessionState.value = _sessionState.value.copy(isPaused = false)
        } else {
            teacherController.pause()
            _sessionState.value = _sessionState.value.copy(isPaused = true)
        }
    }

    private fun processSpeechInput(userVoiceText: String) {
        managerScope.launch {
            _sessionState.value = _sessionState.value.copy(
                inputState = VoiceInputState.THINKING
            )
            // Particle engine shows high-speed quantum thinking vortex
            particleEngine.setVoiceFeedbackMode(VoiceVisualFeedbackMode.THINKING)

            try {
                // Route input through ConversationManager
                _sessionState.value = _sessionState.value.copy(
                    inputState = VoiceInputState.EXECUTING
                )

                val response = conversationManager.processInput(
                    userInput = userVoiceText,
                    teacherController = teacherController
                )

                // If a new 3D particle scene was synthesized, load into teacher controller
                if (response.newScene != null) {
                    teacherController.loadScene(response.newScene)
                    _sessionState.value = _sessionState.value.copy(
                        activeLessonTitle = response.newScene.title,
                        totalSteps = response.newScene.steps.size.coerceAtLeast(1),
                        currentStepNumber = 1
                    )
                    teacherController.startTeaching()
                } else if (response.parameterUpdated != null) {
                    // Particle engine updates active scene parameters
                    particleEngine.updateParameters()
                    speakTeacherReply(response.spokenText)
                } else {
                    speakTeacherReply(response.spokenText)
                }
            } catch (e: Exception) {
                _sessionState.value = _sessionState.value.copy(
                    inputState = VoiceInputState.ERROR,
                    errorMessage = "Error processing voice command: ${e.localizedMessage}"
                )
                particleEngine.setVoiceFeedbackMode(VoiceVisualFeedbackMode.NORMAL)
            }
        }
    }

    fun sendTextCommand(text: String) {
        _sessionState.value = _sessionState.value.copy(
            recognizedFinalText = text
        )
        processSpeechInput(text)
    }

    private fun speakTeacherReply(text: String) {
        _sessionState.value = _sessionState.value.copy(
            inputState = VoiceInputState.SPEAKING,
            activeNarrationText = text
        )
        particleEngine.setVoiceFeedbackMode(VoiceVisualFeedbackMode.SPEAKING)

        actualTtsController.speak(
            text = text,
            utteranceId = "reply_${System.currentTimeMillis()}",
            onStart = {
                _sessionState.value = _sessionState.value.copy(
                    inputState = VoiceInputState.SPEAKING
                )
                particleEngine.setVoiceFeedbackMode(VoiceVisualFeedbackMode.SPEAKING)
            },
            onDone = {
                _sessionState.value = _sessionState.value.copy(
                    inputState = VoiceInputState.IDLE
                )
                particleEngine.setVoiceFeedbackMode(VoiceVisualFeedbackMode.NORMAL)
            },
            onError = { _ ->
                _sessionState.value = _sessionState.value.copy(
                    inputState = VoiceInputState.IDLE
                )
                particleEngine.setVoiceFeedbackMode(VoiceVisualFeedbackMode.NORMAL)
            }
        )
    }

    fun shutdown() {
        managerScope.cancel()
        speechRecognizer.destroy()
        teacherController.destroy()
        actualTtsController.shutdown()
        wakeWordDetector.stopMonitoring()
    }
}
