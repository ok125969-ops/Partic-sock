package com.example.voice

interface WakeWordDetector {
    val isAvailable: Boolean
    val keyword: String
    fun startMonitoring(onWakeWordDetected: () -> Unit)
    fun stopMonitoring()
    fun isMonitoring(): Boolean
}

class DefaultWakeWordDetector(
    override val keyword: String = "Ultron"
) : WakeWordDetector {
    override val isAvailable: Boolean = false // Reliable on-device hotword models require offline DSP engines
    private var active = false

    override fun startMonitoring(onWakeWordDetected: () -> Unit) {
        // Keeps architecture ready for offline DSP model integration (e.g. Porcupine / Snowboy)
        active = true
    }

    override fun stopMonitoring() {
        active = false
    }

    override fun isMonitoring(): Boolean = active
}
