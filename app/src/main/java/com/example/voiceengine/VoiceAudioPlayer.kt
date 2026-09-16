package com.example.voiceengine

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class VoiceAudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var isPlayingState = false
    private var isPausedState = false
    private var currentVolume = 1.0f

    fun playAudioBytes(audioBytes: ByteArray, volume: Float, onComplete: () -> Unit, onError: (String) -> Unit) {
        try {
            stop()
            currentVolume = volume

            val tempFile = File.createTempFile("ultron_voice_", ".mp3", context.cacheDir)
            FileOutputStream(tempFile).use { fos ->
                fos.write(audioBytes)
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                setVolume(currentVolume, currentVolume)
                setOnPreparedListener { mp ->
                    isPlayingState = true
                    mp.start()
                }
                setOnCompletionListener {
                    isPlayingState = false
                    isPausedState = false
                    tempFile.delete()
                    onComplete()
                }
                setOnErrorListener { _, what, extra ->
                    isPlayingState = false
                    isPausedState = false
                    tempFile.delete()
                    onError("MediaPlayer error: what=$what, extra=$extra")
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("VoiceAudioPlayer", "Error playing audio bytes", e)
            isPlayingState = false
            onError(e.message ?: "Audio playback exception")
        }
    }

    fun stop() {
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.release()
            }
            mediaPlayer = null
            isPlayingState = false
            isPausedState = false
        } catch (e: Exception) {
            Log.e("VoiceAudioPlayer", "Error stopping audio player", e)
        }
    }

    fun pause() {
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.pause()
                    isPlayingState = false
                    isPausedState = true
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceAudioPlayer", "Error pausing audio player", e)
        }
    }

    fun resume() {
        try {
            mediaPlayer?.let { mp ->
                if (isPausedState) {
                    mp.start()
                    isPlayingState = true
                    isPausedState = false
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceAudioPlayer", "Error resuming audio player", e)
        }
    }

    fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1f)
        try {
            mediaPlayer?.setVolume(currentVolume, currentVolume)
        } catch (e: Exception) {
            Log.e("VoiceAudioPlayer", "Error setting volume", e)
        }
    }

    fun isPlaying(): Boolean = isPlayingState
}
