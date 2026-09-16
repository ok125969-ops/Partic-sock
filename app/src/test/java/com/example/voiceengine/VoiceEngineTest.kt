package com.example.voiceengine

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VoiceEngineTest {

    private lateinit var context: Context
    private lateinit var voiceEngine: VoiceEngine

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        voiceEngine = VoiceEngine(context)
    }

    @After
    fun tearDown() {
        voiceEngine.release()
    }

    @Test
    fun `voice config defaults are valid`() {
        val config = VoiceConfig()
        assertTrue(config.speakingRate in 0.9f..1.1f)
        assertTrue(config.pitch < 0.0f) // Deep baritone pitch
        assertTrue(config.volume == 1.0f)
        assertNotNull(config.voiceId)
        assertTrue(config.emotion == VoiceEmotion.CASUAL)

        val greetingConfig = config.copyWithEmotion(VoiceEmotion.GREETING)
        assertTrue(greetingConfig.emotion == VoiceEmotion.GREETING)
    }

    @Test
    fun `voice engine initializes and queues speech safely`() {
        voiceEngine.speak("Hello Boss, I am ready.")
        voiceEngine.clearQueue()
        assertTrue(!voiceEngine.isSpeaking())
    }
}
