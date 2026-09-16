package com.example.voice

import com.example.ai.TeachingStep
import com.example.ai.VisualScene
import com.example.ui.components.HolographicState
import com.example.ui.components.ProgrammableParticleEngine
import kotlinx.coroutines.*

class TeacherController(
    private val particleEngine: ProgrammableParticleEngine,
    private val ttsController: SpeechOutput,
    private val onStepUpdated: (Int, Int, TeachingStep, String?) -> Unit,
    private val onLessonComplete: () -> Unit
) {
    var activeScene: VisualScene? = null
        private set

    var currentStepIndex: Int = 0
        private set

    var isPaused: Boolean = false
        private set

    private var lessonScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timelineJob: Job? = null

    fun loadScene(scene: VisualScene) {
        stop()
        activeScene = scene
        currentStepIndex = 0
        isPaused = false
        particleEngine.applyScene(scene)
    }

    fun startTeaching() {
        val scene = activeScene ?: return
        if (scene.steps.isEmpty()) {
            // Single step scene
            ttsController.speak(
                text = scene.explanation.ifBlank { "Observing 3D particle visualization for ${scene.title}." }
            )
            return
        }

        playCurrentStep()
    }

    private fun playCurrentStep() {
        val scene = activeScene ?: return
        val step = scene.steps.getOrNull(currentStepIndex) ?: run {
            onLessonComplete()
            return
        }

        isPaused = false
        timelineJob?.cancel()

        // 1. Dispatch Visual Timeline Events for this Step
        executeVisualStepEvents(step)

        // 2. Notify UI
        onStepUpdated(currentStepIndex + 1, scene.steps.size, step, step.formula)

        // 3. Coordinate Teacher Narration via TTS with completion callback
        ttsController.speak(
            text = step.narration,
            utteranceId = "step_${step.stepId}_${System.currentTimeMillis()}",
            onStart = {
                // Synchronize particle behavior on start of speech
            },
            onDone = {
                // When teacher finishes narration, if not paused and has question, wait for user voice answer;
                // otherwise auto-advance after small pause if multi-step demo
                if (!isPaused && step.question == null && currentStepIndex < scene.steps.size - 1) {
                    timelineJob = lessonScope.launch {
                        delay(1200)
                        if (!isPaused && isActive) {
                            nextStep()
                        }
                    }
                }
            },
            onError = { _ ->
                // TTS error fallback
            }
        )
    }

    private fun executeVisualStepEvents(step: TeachingStep) {
        // Change shape or parameters if specified
        if (step.shape != particleEngine.activeShape) {
            particleEngine.setShape(step.shape, step.title, step.primaryColor)
        }

        // Apply parameter updates
        val scene = activeScene
        if (scene != null && step.parameterUpdates.isNotEmpty()) {
            step.parameterUpdates.forEach { (name, value) ->
                scene.parameters[name]?.value = value
            }
            particleEngine.updateParameters()
        }
    }

    fun pause() {
        isPaused = true
        timelineJob?.cancel()
        ttsController.pause()
    }

    fun resume() {
        if (!isPaused) return
        isPaused = false
        playCurrentStep()
    }

    fun interrupt() {
        isPaused = true
        timelineJob?.cancel()
        ttsController.stop()
    }

    fun nextStep(): Boolean {
        val scene = activeScene ?: return false
        if (currentStepIndex < scene.steps.size - 1) {
            currentStepIndex++
            playCurrentStep()
            return true
        }
        return false
    }

    fun prevStep(): Boolean {
        val scene = activeScene ?: return false
        if (currentStepIndex > 0) {
            currentStepIndex--
            playCurrentStep()
            return true
        }
        return false
    }

    fun repeatStep() {
        playCurrentStep()
    }

    fun stop() {
        isPaused = false
        timelineJob?.cancel()
        ttsController.stop()
    }

    fun executeEvent(event: TimelineEvent) {
        when (event.type) {
            VisualTimelineEventType.SHOW_OBJECT -> {
                particleEngine.state = HolographicState.ACTIVE
            }
            VisualTimelineEventType.HIDE_OBJECT -> {
                particleEngine.resetToIdle()
            }
            VisualTimelineEventType.ROTATE_OBJECT -> {
                particleEngine.autoRotate = true
            }
            VisualTimelineEventType.CHANGE_PARAMETER -> {
                val paramName = event.target ?: return
                val mult = (event.value as? Number)?.toFloat() ?: 1.0f
                activeScene?.let { scene ->
                    com.example.ai.VisualLearningEngine.modifySceneParameter(scene, paramName, mult)
                    particleEngine.updateParameters()
                }
            }
            VisualTimelineEventType.START_PARTICLE_EFFECT -> {
                if (event.target == "EXPLODE") particleEngine.explode()
                if (event.target == "REFORM") particleEngine.reform()
            }
            VisualTimelineEventType.STOP_PARTICLE_EFFECT -> {
                particleEngine.reform()
            }
            VisualTimelineEventType.PAUSE -> pause()
            else -> {}
        }
    }

    fun destroy() {
        stop()
        lessonScope.cancel()
    }
}
