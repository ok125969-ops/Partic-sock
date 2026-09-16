package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ai.ConversationManager
import com.example.ai.VisualLearningEngine
import com.example.data.UltronDatabase
import com.example.data.UltronRepository
import com.example.tools.CalculateTool
import com.example.tools.GetTimeTool
import com.example.tools.ToolContext
import com.example.tools.ToolRegistry
import com.example.tools.UltronCapability
import com.example.ui.components.HolographicParticleSystem
import com.example.voice.TeacherController
import com.example.voice.UltronTTSController
import com.example.voice.VoiceInputState
import com.example.voice.VoiceSessionManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VoiceAndToolSystemTest {

    private lateinit var database: UltronDatabase
    private lateinit var repository: UltronRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, UltronDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = UltronRepository(database.ultronDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `calculate tool evaluates arithmetic correctly`() = runBlocking {
        val toolContext = ToolContext(context = context, repository = repository, activeScene = null)
        val calcTool = CalculateTool()

        val result = calcTool.execute(toolContext, mapOf("expression" to "25 * 4"))
        assertTrue(result.success)
        assertTrue(result.resultSummary.contains("100"))
    }

    @Test
    fun `get time tool returns current system time`() = runBlocking {
        val toolContext = ToolContext(context = context, repository = repository, activeScene = null)
        val timeTool = GetTimeTool()

        val result = timeTool.execute(toolContext, emptyMap())
        assertTrue(result.success)
        assertTrue(result.resultSummary.isNotBlank())
    }

    @Test
    fun `tool registry registers all core capabilities`() {
        val tools = ToolRegistry.getAllTools()
        assertTrue(tools.isNotEmpty())
        assertNotNull(ToolRegistry.get(UltronCapability.CALCULATE))
        assertNotNull(ToolRegistry.get(UltronCapability.GET_TIME))
        assertNotNull(ToolRegistry.get(UltronCapability.VISUALIZE_CONCEPT))
        assertNotNull(ToolRegistry.get(UltronCapability.MODIFY_PARTICLE_SCENE))
    }

    @Test
    fun `conversation manager processes calculation command`() = runBlocking {
        val toolContext = ToolContext(context = context, repository = repository, activeScene = null)
        val convManager = ConversationManager(context = context, toolContext = toolContext)
        val tts = UltronTTSController(context)
        val particles = HolographicParticleSystem(count = 100)
        val teacherController = TeacherController(
            particleEngine = particles,
            ttsController = tts,
            onStepUpdated = { _, _, _, _ -> },
            onLessonComplete = {}
        )

        val response = convManager.processInput("calculate 12 + 18", teacherController)
        assertTrue(response.spokenText.isNotBlank())
        assertTrue(response.spokenText.contains("30"))
    }

    @Test
    fun `teacher controller loads scene and manages step transitions`() = runBlocking {
        val tts = UltronTTSController(context)
        val particles = HolographicParticleSystem(count = 100)
        val teacherController = TeacherController(
            particleEngine = particles,
            ttsController = tts,
            onStepUpdated = { _, _, _, _ -> },
            onLessonComplete = {}
        )

        val scene = VisualLearningEngine.planVisualScene("Newton's second law samjhao")
        teacherController.loadScene(scene)

        assertNotNull(teacherController.activeScene)
        assertEquals(0, teacherController.currentStepIndex)

        if (scene.steps.size > 1) {
            teacherController.nextStep()
            assertEquals(1, teacherController.currentStepIndex)
            teacherController.prevStep()
            assertEquals(0, teacherController.currentStepIndex)
        }
    }

    @Test
    fun `voice session manager initializes in idle state`() {
        val toolContext = ToolContext(context = context, repository = repository, activeScene = null)
        val convManager = ConversationManager(context = context, toolContext = toolContext)
        val particles = HolographicParticleSystem(count = 100)
        val sessionManager = VoiceSessionManager(context, particles, convManager)

        assertEquals(VoiceInputState.IDLE, sessionManager.sessionState.value.inputState)
        sessionManager.shutdown()
    }
}
