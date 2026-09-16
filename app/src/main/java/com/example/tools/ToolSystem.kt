package com.example.tools

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.provider.MediaStore
import android.provider.Settings
import com.example.ai.VisualLearningEngine
import com.example.ai.VisualScene
import com.example.ai.YouTubeTeacherEngine
import com.example.data.UltronMemoryEntity
import com.example.data.UltronRepository
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

enum class UltronCapability {
    CALCULATE,
    GET_TIME,
    CREATE_NOTE,
    CREATE_REMINDER,
    OPEN_APP,
    SEARCH_WEB,
    READ_SUPPORTED_CONTENT,
    CREATE_PARTICLE_SCENE,
    MODIFY_PARTICLE_SCENE,
    VISUALIZE_CONCEPT,
    GET_DEVICE_STATE
}

data class ToolExecutionResult(
    val capability: UltronCapability,
    val success: Boolean,
    val resultSummary: String,
    val data: Any? = null,
    val generatedScene: VisualScene? = null
)

data class ToolContext(
    val context: Context,
    val repository: UltronRepository,
    var activeScene: VisualScene? = null
)

interface UltronTool {
    val capability: UltronCapability
    val name: String
    val description: String
    suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolExecutionResult
}

// 1. CALCULATE TOOL
class CalculateTool : UltronTool {
    override val capability = UltronCapability.CALCULATE
    override val name = "CALCULATE"
    override val description = "Accurately evaluates mathematical expressions and physics calculations."

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolExecutionResult {
        val expression = args["expression"]?.toString() ?: ""
        if (expression.isBlank()) {
            return ToolExecutionResult(capability, false, "Empty calculation expression.")
        }

        return try {
            val evaluated = evaluateMathExpression(expression)
            ToolExecutionResult(
                capability = capability,
                success = true,
                resultSummary = "$expression = $evaluated",
                data = evaluated
            )
        } catch (e: Exception) {
            ToolExecutionResult(capability, false, "Calculation error: ${e.message}")
        }
    }

    private fun evaluateMathExpression(expr: String): Double {
        // Sanitize and replace math symbols
        val cleaned = expr.lowercase()
            .replace("×", "*")
            .replace("x", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace(" ", "")

        return SimpleMathParser(cleaned).parse()
    }
}

// Simple recursive descent parser for basic math
private class SimpleMathParser(val str: String) {
    private var pos = -1
    private var ch = 0

    private fun nextChar() {
        ch = if (++pos < str.length) str[pos].code else -1
    }

    private fun eat(charToEat: Int): Boolean {
        while (ch == ' '.code) nextChar()
        if (ch == charToEat) {
            nextChar()
            return true
        }
        return false
    }

    fun parse(): Double {
        nextChar()
        val x = parseExpression()
        if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
        return x
    }

    private fun parseExpression(): Double {
        var x = parseTerm()
        while (true) {
            when {
                eat('+'.code) -> x += parseTerm()
                eat('-'.code) -> x -= parseTerm()
                else -> return x
            }
        }
    }

    private fun parseTerm(): Double {
        var x = parseFactor()
        while (true) {
            when {
                eat('*'.code) -> x *= parseFactor()
                eat('/'.code) -> x /= parseFactor()
                eat('%'.code) -> x %= parseFactor()
                else -> return x
            }
        }
    }

    private fun parseFactor(): Double {
        if (eat('+'.code)) return +parseFactor()
        if (eat('-'.code)) return -parseFactor()

        var x: Double
        val startPos = pos
        if (eat('('.code)) {
            x = parseExpression()
            eat(')'.code)
        } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
            while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
            x = str.substring(startPos, pos).toDouble()
        } else if (ch in 'a'.code..'z'.code) {
            while (ch in 'a'.code..'z'.code) nextChar()
            val func = str.substring(startPos, pos)
            x = if (eat('('.code)) {
                val arg = parseExpression()
                eat(')'.code)
                when (func) {
                    "sqrt" -> sqrt(arg)
                    "sin" -> sin(Math.toRadians(arg))
                    "cos" -> cos(Math.toRadians(arg))
                    "tan" -> tan(Math.toRadians(arg))
                    else -> throw RuntimeException("Unknown function: $func")
                }
            } else {
                when (func) {
                    "pi" -> Math.PI
                    "e" -> Math.E
                    else -> throw RuntimeException("Unknown constant: $func")
                }
            }
        } else {
            throw RuntimeException("Unexpected: " + ch.toChar())
        }

        if (eat('^'.code)) x = x.pow(parseFactor())
        return x
    }
}

// 2. GET_TIME TOOL
class GetTimeTool : UltronTool {
    override val capability = UltronCapability.GET_TIME
    override val name = "GET_TIME"
    override val description = "Retrieves real current system time, date, and day."

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolExecutionResult {
        val now = Date()
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val formattedTime = timeFormat.format(now)
        val formattedDate = dateFormat.format(now)
        val summary = "Abhi samay hai $formattedTime, $formattedDate."

        return ToolExecutionResult(
            capability = capability,
            success = true,
            resultSummary = summary,
            data = mapOf("time" to formattedTime, "date" to formattedDate)
        )
    }
}

// 3. CREATE_NOTE TOOL
class CreateNoteTool : UltronTool {
    override val capability = UltronCapability.CREATE_NOTE
    override val name = "CREATE_NOTE"
    override val description = "Persists a user note or research point into local Room database."

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolExecutionResult {
        val content = args["content"]?.toString() ?: ""
        val title = args["title"]?.toString() ?: "Note ${SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())}"

        if (content.isBlank()) {
            return ToolExecutionResult(capability, false, "Note content cannot be empty.")
        }

        context.repository.insertMemory(
            UltronMemoryEntity(
                category = "NOTE",
                title = title,
                content = content
            )
        )

        return ToolExecutionResult(
            capability = capability,
            success = true,
            resultSummary = "Note '$title' successfully saved in ULTRON Memory.",
            data = title
        )
    }
}

// 4. CREATE_REMINDER TOOL
class CreateReminderTool : UltronTool {
    override val capability = UltronCapability.CREATE_REMINDER
    override val name = "CREATE_REMINDER"
    override val description = "Stores a reminder in ULTRON local memory database."

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolExecutionResult {
        val text = args["text"]?.toString() ?: ""
        if (text.isBlank()) return ToolExecutionResult(capability, false, "Reminder text cannot be empty.")

        context.repository.insertMemory(
            UltronMemoryEntity(
                category = "REMINDER",
                title = "Reminder",
                content = text
            )
        )

        return ToolExecutionResult(
            capability = capability,
            success = true,
            resultSummary = "Reminder set: '$text'",
            data = text
        )
    }
}

// 5. OPEN_APP TOOL
class OpenAppTool : UltronTool {
    override val capability = UltronCapability.OPEN_APP
    override val name = "OPEN_APP"
    override val description = "Resolves Android Intents to open installed applications or system settings."

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolExecutionResult {
        val targetApp = args["app"]?.toString()?.lowercase() ?: ""
        val appContext = context.context

        return try {
            val intent = when {
                targetApp.contains("calc") -> {
                    val calcIntent = appContext.packageManager.getLaunchIntentForPackage("com.google.android.calculator")
                        ?: appContext.packageManager.getLaunchIntentForPackage("com.android.calculator2")
                    calcIntent ?: Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR)
                }
                targetApp.contains("cam") -> {
                    Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                }
                targetApp.contains("setting") -> {
                    Intent(Settings.ACTION_SETTINGS)
                }
                else -> {
                    null
                }
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                appContext.startActivity(intent)
                ToolExecutionResult(capability, true, "Opened $targetApp successfully.")
            } else {
                ToolExecutionResult(capability, false, "Application '$targetApp' could not be resolved directly.")
            }
        } catch (e: Exception) {
            ToolExecutionResult(capability, false, "Failed to launch $targetApp: ${e.message}")
        }
    }
}

// 6. SEARCH_WEB TOOL
class SearchWebTool : UltronTool {
    override val capability = UltronCapability.SEARCH_WEB
    override val name = "SEARCH_WEB"
    override val description = "Dispatches a web search query via Android Intent."

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolExecutionResult {
        val query = args["query"]?.toString() ?: ""
        if (query.isBlank()) return ToolExecutionResult(capability, false, "Query cannot be blank.")

        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra("query", query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.context.startActivity(intent)
            ToolExecutionResult(capability, true, "Searching web for '$query'.")
        } catch (e: Exception) {
            ToolExecutionResult(capability, false, "Web search failed: ${e.message}")
        }
    }
}

// 7. READ_SUPPORTED_CONTENT TOOL
class ReadSupportedContentTool : UltronTool {
    override val capability = UltronCapability.READ_SUPPORTED_CONTENT
    override val name = "READ_SUPPORTED_CONTENT"
    override val description = "Retrieves and parses educational content and transcripts from supported sources."

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolExecutionResult {
        val url = args["url"]?.toString() ?: ""
        val instruction = args["instruction"]?.toString() ?: ""

        if (url.isBlank()) return ToolExecutionResult(capability, false, "URL must be provided.")

        return try {
            val segment = YouTubeTeacherEngine.analyzeYouTubeUrl(url, instruction)
            ToolExecutionResult(
                capability = capability,
                success = true,
                resultSummary = "Analyzed '${segment.videoTitle}' with ${segment.timeline.size} concept nodes.",
                data = segment
            )
        } catch (e: Exception) {
            ToolExecutionResult(capability, false, "Failed to analyze content: ${e.message}")
        }
    }
}

// 8. VISUALIZE_CONCEPT TOOL
class VisualizeConceptTool : UltronTool {
    override val capability = UltronCapability.VISUALIZE_CONCEPT
    override val name = "VISUALIZE_CONCEPT"
    override val description = "Synthesizes structured 3D particle scene and multi-step interactive lesson."

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolExecutionResult {
        val topic = args["topic"]?.toString() ?: ""
        if (topic.isBlank()) return ToolExecutionResult(capability, false, "Topic cannot be empty.")

        val scene = VisualLearningEngine.planVisualScene(topic)
        context.activeScene = scene

        // Save scene memory
        context.repository.insertMemory(
            UltronMemoryEntity(
                category = "VISUAL_SCENE",
                title = scene.title,
                content = scene.explanation
            )
        )

        return ToolExecutionResult(
            capability = capability,
            success = true,
            resultSummary = "Synthesized 3D Particle Scene for '${scene.title}'.",
            data = scene.explanation,
            generatedScene = scene
        )
    }
}

// 9. MODIFY_PARTICLE_SCENE TOOL
class ModifyParticleSceneTool : UltronTool {
    override val capability = UltronCapability.MODIFY_PARTICLE_SCENE
    override val name = "MODIFY_PARTICLE_SCENE"
    override val description = "Adjusts physical parameters on the active particle scene (force, mass, distance, etc.)."

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolExecutionResult {
        val scene = context.activeScene
            ?: return ToolExecutionResult(capability, false, "Koi active particle scene nahi hai jise modify kiya ja sake.")

        val paramName = args["param"]?.toString()?.lowercase() ?: ""
        val multiplier = (args["multiplier"] as? Number)?.toFloat() ?: 1.0f

        val resultMsg = VisualLearningEngine.modifySceneParameter(scene, paramName, multiplier)
        return ToolExecutionResult(
            capability = capability,
            success = true,
            resultSummary = resultMsg,
            data = scene.parameters[paramName]?.value
        )
    }
}

// 10. GET_DEVICE_STATE TOOL
class GetDeviceStateTool : UltronTool {
    override val capability = UltronCapability.GET_DEVICE_STATE
    override val name = "GET_DEVICE_STATE"
    override val description = "Reads device telemetry (battery level, charging status)."

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolExecutionResult {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.context.registerReceiver(null, filter)
        }
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 100

        val isCharging = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING
        val stateSummary = "Device Battery: $batteryPct% ${if (isCharging) "(Charging)" else "(Discharging)"}."

        return ToolExecutionResult(
            capability = capability,
            success = true,
            resultSummary = stateSummary,
            data = mapOf("battery" to batteryPct, "charging" to isCharging)
        )
    }
}

// Tool Registry Singleton
object ToolRegistry {
    private val tools = mutableMapOf<UltronCapability, UltronTool>()

    init {
        register(CalculateTool())
        register(GetTimeTool())
        register(CreateNoteTool())
        register(CreateReminderTool())
        register(OpenAppTool())
        register(SearchWebTool())
        register(ReadSupportedContentTool())
        register(VisualizeConceptTool())
        register(ModifyParticleSceneTool())
        register(GetDeviceStateTool())
    }

    fun register(tool: UltronTool) {
        tools[tool.capability] = tool
    }

    fun get(capability: UltronCapability): UltronTool? = tools[capability]

    fun getAllTools(): List<UltronTool> = tools.values.toList()
}
