package com.example.voiceengine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.LinkedList
import java.util.Queue

class VoiceSpeechQueue(
    private val scope: CoroutineScope,
    private val speakAction: suspend (String) -> Unit
) {
    private val queue: Queue<String> = LinkedList()
    private val _queueSize = MutableStateFlow(0)
    val queueSize: StateFlow<Int> = _queueSize

    private var processingJob: Job? = null
    private var isProcessing = false

    fun enqueue(text: String) {
        synchronized(queue) {
            queue.add(text)
            _queueSize.value = queue.size
        }
        processNext()
    }

    fun clearQueue() {
        synchronized(queue) {
            queue.clear()
            _queueSize.value = 0
        }
        processingJob?.cancel()
        isProcessing = false
    }

    private fun processNext() {
        if (isProcessing) return

        synchronized(queue) {
            if (queue.isEmpty()) {
                isProcessing = false
                _queueSize.value = 0
                return
            }
            isProcessing = true
        }

        processingJob = scope.launch(Dispatchers.Main) {
            val textToSpeak: String
            synchronized(queue) {
                textToSpeak = queue.poll() ?: ""
                _queueSize.value = queue.size
            }

            if (textToSpeak.isNotBlank()) {
                speakAction(textToSpeak)
            }

            isProcessing = false
            processNext()
        }
    }
}
