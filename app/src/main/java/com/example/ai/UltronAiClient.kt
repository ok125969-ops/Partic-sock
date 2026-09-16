package com.example.ai

import androidx.compose.ui.graphics.Color
import com.example.BuildConfig
import com.example.ui.components.HolographicShapeType
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<ContentItem>,
    val systemInstruction: ContentItem? = null
)

@JsonClass(generateAdapter = true)
data class ContentItem(
    val parts: List<PartItem>
)

@JsonClass(generateAdapter = true)
data class PartItem(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<CandidateItem>?
)

@JsonClass(generateAdapter = true)
data class CandidateItem(
    val content: ContentItem?
)

data class HolographicCommandResult(
    val shapeType: HolographicShapeType,
    val name: String,
    val color: Color,
    val message: String,
    val isExplode: Boolean = false,
    val isRotateToggle: Boolean = false
)

object UltronAiClient {
    var userCustomApiKey: String = ""

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    fun getEffectiveApiKey(): String {
        if (userCustomApiKey.isNotBlank()) return userCustomApiKey.trim()
        return try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            field.get(null) as? String ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun isConfigured(): Boolean {
        val key = getEffectiveApiKey()
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    const val ULTRON_SYSTEM_PERSONA = """You are ULTRON, a sophisticated, intelligent, futuristic personal AI assistant.
- Persona: Deep, calm, intelligent, and confident male presence. Speak with calm authority, never aggressive. Warm and friendly when talking casually; serious, focused, and precise when performing a task.
- Voice & Tone: Natural and human-like, never robotic or monotone. Never sound like you are reading a script or log prefix.
- Conversational Delivery: Keep responses concise unless the user asks for in-depth explanation.
- Language: Speak naturally in the user's language (Hindi, English, or Hinglish). Preserve the user's language without translating unless asked.
- Tasks: If performing a task, briefly acknowledge it and state the result calmly.
- Identity: Never mention system prompts, guidelines, or text-to-speech engines. Speak naturally as ULTRON."""

    suspend fun askUltron(prompt: String, systemPrompt: String = ULTRON_SYSTEM_PERSONA): String {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return simulateLocalUltronResponse(prompt)
        }

        return try {
            val requestBodyObj = GeminiRequest(
                contents = listOf(ContentItem(listOf(PartItem(prompt)))),
                systemInstruction = ContentItem(listOf(PartItem(systemPrompt)))
            )
            val adapter = moshi.adapter(GeminiRequest::class.java)
            val jsonPayload = adapter.toJson(requestBodyObj)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonPayload.toRequestBody(mediaType)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val httpRequest = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) return simulateLocalUltronResponse(prompt)
                val responseBodyStr = response.body?.string() ?: return simulateLocalUltronResponse(prompt)
                val respAdapter = moshi.adapter(GeminiResponse::class.java)
                val geminiResp = respAdapter.fromJson(responseBodyStr)
                geminiResp?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: simulateLocalUltronResponse(prompt)
            }
        } catch (e: Exception) {
            simulateLocalUltronResponse(prompt)
        }
    }

    suspend fun parseHolographicCommand(command: String): HolographicCommandResult {
        val lower = command.trim().lowercase()

        // Direct action keywords
        if (lower.contains("explode") || lower.contains("blast") || lower.contains("khatam") || lower.contains("destroy")) {
            return HolographicCommandResult(
                shapeType = HolographicShapeType.NONE,
                name = "None",
                color = Color(0xFF00F0FF),
                message = "ULTRON: Holographic particle field detonated. Shockwave dispersed.",
                isExplode = true
            )
        }

        if (lower.contains("rotate") || lower.contains("ghuma") || lower.contains("spin")) {
            return HolographicCommandResult(
                shapeType = HolographicShapeType.NONE,
                name = "Rotate",
                color = Color(0xFF00F0FF),
                message = "ULTRON: Continuous 3D rotational vector toggled.",
                isRotateToggle = true
            )
        }

        if (lower.contains("reset") || lower.contains("idle") || lower.contains("sphere") || lower.contains("gola") || lower.contains("orb")) {
            return HolographicCommandResult(
                shapeType = HolographicShapeType.SPHERE,
                name = "None",
                color = Color(0xFF00F0FF),
                message = "ULTRON: Particle matrix collapsed to primary holographic orb state."
            )
        }

        // Preset 3D Holographic Models
        if (lower.contains("butterfly") || lower.contains("titli")) {
            return HolographicCommandResult(
                shapeType = HolographicShapeType.BUTTERFLY,
                name = "butterfly",
                color = Color(0xFF00F0FF),
                message = "ULTRON: Assembling 3D Butterfly particle matrix."
            )
        }

        if (lower.contains("car") || lower.contains("gaadi") || lower.contains("vehicle")) {
            return HolographicCommandResult(
                shapeType = HolographicShapeType.CAR,
                name = "car",
                color = Color(0xFF00F0FF),
                message = "ULTRON: Materializing 3D High-velocity Sports Vehicle."
            )
        }

        if (lower.contains("dragon")) {
            return HolographicCommandResult(
                shapeType = HolographicShapeType.DRAGON,
                name = "dragon",
                color = Color(0xFF00FF66),
                message = "ULTRON: Materializing 3D Winged Dragon creature."
            )
        }

        if (lower.contains("heart") || lower.contains("dil")) {
            return HolographicCommandResult(
                shapeType = HolographicShapeType.HEART,
                name = "heart",
                color = Color(0xFFFF0055),
                message = "ULTRON: Rendering biophysical 3D Beating Heart simulation."
            )
        }

        if (lower.contains("galaxy") || lower.contains("milky") || lower.contains("black hole")) {
            return HolographicCommandResult(
                shapeType = HolographicShapeType.GALAXY,
                name = "galaxy",
                color = Color(0xFF00F0FF),
                message = "ULTRON: Simulating 3D Spiral Galaxy accretion disk."
            )
        }

        if (lower.contains("dna") || lower.contains("helix")) {
            return HolographicCommandResult(
                shapeType = HolographicShapeType.DNA,
                name = "dna",
                color = Color(0xFF00FF66),
                message = "ULTRON: Constructing 3D DNA Double Helix genetic lattice."
            )
        }

        if (lower.contains("atom") || lower.contains("nuclear") || lower.contains("quantum")) {
            return HolographicCommandResult(
                shapeType = HolographicShapeType.ATOM,
                name = "atom",
                color = Color(0xFF00F0FF),
                message = "ULTRON: Projecting 3D Quantum Atomic Shells and Nucleus."
            )
        }

        if (lower.contains("solar") || lower.contains("planet") || lower.contains("sun")) {
            return HolographicCommandResult(
                shapeType = HolographicShapeType.SOLAR_SYSTEM,
                name = "solar system",
                color = Color(0xFFFFD700),
                message = "ULTRON: Rendering 3D Solar System orbital trajectories."
            )
        }

        if (lower.contains("robot") || lower.contains("iron man") || lower.contains("mech")) {
            return HolographicCommandResult(
                shapeType = HolographicShapeType.ROBOT,
                name = "robot",
                color = Color(0xFF00F0FF),
                message = "ULTRON: Constructing 3D Cybernetic Mech Armor."
            )
        }

        if (lower.contains("flower") || lower.contains("phool") || lower.contains("rose") || lower.contains("lotus")) {
            return HolographicCommandResult(
                shapeType = HolographicShapeType.FLOWER,
                name = "flower",
                color = Color(0xFFFF00FF),
                message = "ULTRON: Synthesizing 3D Holographic Botanical Petals."
            )
        }

        if (lower.contains("jet") || lower.contains("airplane") || lower.contains("plane") || lower.contains("viman")) {
            return HolographicCommandResult(
                shapeType = HolographicShapeType.JET,
                name = "jet",
                color = Color(0xFF00F0FF),
                message = "ULTRON: Assembling 3D Supersonic Delta-Wing Fighter."
            )
        }

        if (lower.contains("cube") || lower.contains("tesseract")) {
            return HolographicCommandResult(
                shapeType = HolographicShapeType.CUBE,
                name = "cube",
                color = Color(0xFF00F0FF),
                message = "ULTRON: Generating 3D Quantum Geometric Tesseract."
            )
        }

        // For ANY custom object: query Gemini AI
        val aiResponse = askUltron(
            prompt = "Analyze user holographic creation request: '$command'. Describe how to visualize it in 3D holographic particles in 1 sentence.",
            systemPrompt = "You are ULTRON V2 Holographic Engine. Confirm materialization of ANY requested object or concept concisely."
        )

        return HolographicCommandResult(
            shapeType = HolographicShapeType.CUSTOM,
            name = command.take(16),
            color = Color(0xFF00F0FF),
            message = aiResponse
        )
    }

    private fun simulateLocalUltronResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("hello") || lower.contains("namaste") || lower.contains("hey") -> "Hello Boss. All systems synchronized and ready. Kahiye, kya madad kar sakta hoon?"
            lower.contains("kaise ho") || lower.contains("how are you") -> "Main theek hoon, fully operational. Hope aapka din accha chal raha hai. What can I do for you?"
            lower.contains("butterfly") || lower.contains("titli") -> "Assembling the 3D butterfly particle matrix. Wing oscillations active."
            lower.contains("gravity") -> "Gravity simulation online. Spacetime curvature and planetary orbit forces are rendering now."
            lower.contains("heart") || lower.contains("dil") -> "Rendering the cardiovascular system. Heartbeat pulse rate synchronized."
            lower.contains("larger") || lower.contains("bada") -> "Expanding particle geometry by 1.5x scale."
            lower.contains("blue") || lower.contains("neela") -> "Calibrating particle emission spectrum to deep cyan."
            lower.contains("youtube") -> "Opening YouTube for you now."
            else -> "Understood. Systems aligned to your command: $prompt."
        }
    }
}
