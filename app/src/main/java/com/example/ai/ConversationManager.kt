package com.example.ai

import android.content.Context
import com.example.tools.*
import com.example.voice.TeacherController
import com.example.voice.TimelineEvent
import com.example.voice.VisualTimelineEventType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ConversationResponse(
    val spokenText: String,
    val displayText: String = spokenText,
    val toolUsed: UltronCapability? = null,
    val toolResultSummary: String? = null,
    val newScene: VisualScene? = null,
    val parameterUpdated: String? = null,
    val isInterruptionHandled: Boolean = false,
    val isPauseRequested: Boolean = false,
    val isResumeRequested: Boolean = false,
    val isNextStepRequested: Boolean = false,
    val isPrevStepRequested: Boolean = false,
    val isRepeatStepRequested: Boolean = false
)

data class ConversationTurn(
    val role: String, // "user" or "ultron"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

class ConversationManager(
    private val context: Context,
    private val toolContext: ToolContext
) {
    private val history = mutableListOf<ConversationTurn>()
    var activeScene: VisualScene?
        get() = toolContext.activeScene
        set(value) {
            toolContext.activeScene = value
        }

    suspend fun processInput(
        userInput: String,
        teacherController: TeacherController? = null
    ): ConversationResponse = withContext(Dispatchers.Default) {
        val trimmed = userInput.trim()
        history.add(ConversationTurn("user", trimmed))
        val lower = trimmed.lowercase()

        // 1. Check for immediate flow control / playback commands
        if (isPauseCommand(lower)) {
            teacherController?.pause()
            val resp = "Lesson paused. Jab aap ready hon, 'Continue' ya 'Resume' bolein."
            history.add(ConversationTurn("ultron", resp))
            return@withContext ConversationResponse(
                spokenText = resp,
                isPauseRequested = true
            )
        }

        if (isResumeCommand(lower)) {
            teacherController?.resume()
            val resp = "Continuing lesson."
            history.add(ConversationTurn("ultron", resp))
            return@withContext ConversationResponse(
                spokenText = resp,
                isResumeRequested = true
            )
        }

        if (isNextCommand(lower)) {
            val moved = teacherController?.nextStep() ?: false
            val resp = if (moved) "Next step par chalte hain." else "Aap lesson ke aakhiri step par hain."
            history.add(ConversationTurn("ultron", resp))
            return@withContext ConversationResponse(
                spokenText = resp,
                isNextStepRequested = true
            )
        }

        if (isPrevCommand(lower)) {
            val moved = teacherController?.prevStep() ?: false
            val resp = if (moved) "Previous step par laut rahe hain." else "Aap pehle step par hain."
            history.add(ConversationTurn("ultron", resp))
            return@withContext ConversationResponse(
                spokenText = resp,
                isPrevStepRequested = true
            )
        }

        if (isRepeatCommand(lower)) {
            teacherController?.repeatStep()
            val resp = "Step dobara repeat kar raha hoon."
            history.add(ConversationTurn("ultron", resp))
            return@withContext ConversationResponse(
                spokenText = resp,
                isRepeatStepRequested = true
            )
        }

        // 2. Explode / Reform / Rotate / Zoom commands
        if (lower == "explode" || lower.contains("blast") || lower.contains("is object ko hatao") || lower.contains("object hatao")) {
            teacherController?.executeEvent(TimelineEvent(VisualTimelineEventType.START_PARTICLE_EFFECT, target = "EXPLODE"))
            val resp = "Radial particle explosion effect initiated on active hologram."
            history.add(ConversationTurn("ultron", resp))
            return@withContext ConversationResponse(spokenText = resp)
        }

        if (lower == "reform" || lower.contains("wapas bana") || lower.contains("wapas jodo") || lower.contains("re-form")) {
            teacherController?.executeEvent(TimelineEvent(VisualTimelineEventType.START_PARTICLE_EFFECT, target = "REFORM"))
            val resp = "Particles reforming into original geometry."
            history.add(ConversationTurn("ultron", resp))
            return@withContext ConversationResponse(spokenText = resp)
        }

        if (lower.contains("rotate karo") || lower.contains("ghuma") || lower == "rotate") {
            teacherController?.executeEvent(TimelineEvent(VisualTimelineEventType.ROTATE_OBJECT))
            val resp = "3D Continuous rotation mode toggled."
            history.add(ConversationTurn("ultron", resp))
            return@withContext ConversationResponse(spokenText = resp)
        }

        // 3. Contextual parameter modification on active scene (NO lesson restart!)
        val scene = activeScene
        if (scene != null) {
            // Check for contextual questions regarding the current scene
            if (lower.contains("ab kya hua") || lower.contains("kya effect hua") || lower.contains("result kya hai") || lower.contains("explain karo")) {
                val explanation = explainCurrentSceneState(scene)
                history.add(ConversationTurn("ultron", explanation))
                return@withContext ConversationResponse(spokenText = explanation)
            }

            if (lower.contains("ye kya ho raha hai") || lower.contains("ye kya hai")) {
                val explanation = "Current 3D model is ${scene.title}. ${scene.explanation}"
                history.add(ConversationTurn("ultron", explanation))
                return@withContext ConversationResponse(spokenText = explanation)
            }

            if (lower.contains("simple language") || lower.contains("aasan bhasha") || lower.contains("saral bhasha")) {
                val simplified = simplifyConcept(scene)
                history.add(ConversationTurn("ultron", simplified))
                return@withContext ConversationResponse(spokenText = simplified)
            }

            if (lower.contains("example do") || lower.contains("udaharan do")) {
                val example = giveRealWorldExample(scene)
                history.add(ConversationTurn("ultron", example))
                return@withContext ConversationResponse(spokenText = example)
            }

            if (lower.contains("quiz") || lower.contains("question")) {
                val currentStep = scene.steps.getOrNull(teacherController?.currentStepIndex ?: 0)
                val qText = currentStep?.question ?: "Quiz: ${scene.title} me primary principle kya apply hota hai?"
                history.add(ConversationTurn("ultron", qText))
                return@withContext ConversationResponse(spokenText = qText)
            }

            // Check if user is asking to modify parameters:
            // "Object ko double mass ka karo", "Mass double karo", "Mass half karo", "Force double karo", "Distance badhao"
            val paramMod = checkParameterModification(lower, scene)
            if (paramMod != null) {
                val tool = ToolRegistry.get(UltronCapability.MODIFY_PARTICLE_SCENE)
                val result = tool?.execute(
                    toolContext,
                    mapOf("param" to paramMod.first, "multiplier" to paramMod.second)
                )
                val reply = result?.resultSummary ?: "Parameter updated."
                history.add(ConversationTurn("ultron", reply))
                return@withContext ConversationResponse(
                    spokenText = reply,
                    toolUsed = UltronCapability.MODIFY_PARTICLE_SCENE,
                    toolResultSummary = reply,
                    parameterUpdated = paramMod.first
                )
            }
        }

        // 4. Intent: Math Calculation tool (e.g. "Calculator kholo aur 25 × 48 calculate karo", "15 * 8 kitna hoga")
        val mathMatch = extractMathCalculation(lower)
        if (mathMatch != null) {
            val calcTool = ToolRegistry.get(UltronCapability.CALCULATE)
            val result = calcTool?.execute(toolContext, mapOf("expression" to mathMatch))
            val reply = if (result?.success == true) {
                "Calculation result: $mathMatch = ${result.data}."
            } else {
                "Calculation error: ${result?.resultSummary}"
            }
            history.add(ConversationTurn("ultron", reply))
            return@withContext ConversationResponse(
                spokenText = reply,
                toolUsed = UltronCapability.CALCULATE,
                toolResultSummary = result?.resultSummary
            )
        }

        // 5. Intent: Note / Reminder creation
        if (lower.contains("note") || lower.contains("save karo") || lower.contains("likho") || lower.contains("yaad rakhna")) {
            val noteContent = trimmed.replace(Regex("(?i)^(note|save karo|notes me save karo|likho|yaad rakhna)[: ]*"), "")
            val noteTool = ToolRegistry.get(UltronCapability.CREATE_NOTE)
            val result = noteTool?.execute(toolContext, mapOf("content" to noteContent, "title" to "Voice Note"))
            val reply = result?.resultSummary ?: "Note saved."
            history.add(ConversationTurn("ultron", reply))
            return@withContext ConversationResponse(
                spokenText = reply,
                toolUsed = UltronCapability.CREATE_NOTE,
                toolResultSummary = reply
            )
        }

        // 6. Intent: Real Time / Date
        if (lower.contains("time") || lower.contains("samay") || lower.contains("kitne baje") || lower.contains("date") || lower.contains("tareekh")) {
            val timeTool = ToolRegistry.get(UltronCapability.GET_TIME)
            val result = timeTool?.execute(toolContext, emptyMap())
            val reply = result?.resultSummary ?: "Current time retrieved."
            history.add(ConversationTurn("ultron", reply))
            return@withContext ConversationResponse(
                spokenText = reply,
                toolUsed = UltronCapability.GET_TIME,
                toolResultSummary = reply
            )
        }

        // 7. Intent: Device state / Battery
        if (lower.contains("battery") || lower.contains("charging") || lower.contains("phone status")) {
            val deviceTool = ToolRegistry.get(UltronCapability.GET_DEVICE_STATE)
            val result = deviceTool?.execute(toolContext, emptyMap())
            val reply = result?.resultSummary ?: "Device status retrieved."
            history.add(ConversationTurn("ultron", reply))
            return@withContext ConversationResponse(
                spokenText = reply,
                toolUsed = UltronCapability.GET_DEVICE_STATE,
                toolResultSummary = reply
            )
        }

        // 8. Intent: Visual Learning / Concept Visualization
        // e.g. "Ultron, gravity samjhao.", "Particles se gravity visualize karo.", "Newton's second law samjhao", "Earth banao"
        val conceptTool = ToolRegistry.get(UltronCapability.VISUALIZE_CONCEPT)
        val result = conceptTool?.execute(toolContext, mapOf("topic" to trimmed))

        if (result?.success == true && result.generatedScene != null) {
            val generated = result.generatedScene
            activeScene = generated

            val firstStep = generated.steps.firstOrNull()
            val spoken = firstStep?.narration ?: generated.explanation.ifBlank { "Observing ${generated.title}." }

            history.add(ConversationTurn("ultron", spoken))
            return@withContext ConversationResponse(
                spokenText = spoken,
                displayText = "ULTRON: ${generated.title} - $spoken",
                toolUsed = UltronCapability.VISUALIZE_CONCEPT,
                toolResultSummary = result.resultSummary,
                newScene = generated
            )
        }

        // 9. Conversational Fallback (Online via Gemini API if key configured, otherwise local offline brain)
        val aiReply = UltronAiClient.askUltron(trimmed)
        history.add(ConversationTurn("ultron", aiReply))
        ConversationResponse(spokenText = aiReply)
    }

    private fun isPauseCommand(lower: String) =
        lower == "pause" || lower == "ruko" || lower == "ruk jao" || lower == "stop" || lower.contains("pause karo")

    private fun isResumeCommand(lower: String) =
        lower == "resume" || lower == "continue" || lower == "chalu karo" || lower.contains("aage badho") || lower == "play"

    private fun isNextCommand(lower: String) =
        lower == "next" || lower == "next step" || lower == "agla step" || lower.contains("aage chalo")

    private fun isPrevCommand(lower: String) =
        lower == "prev" || lower == "previous" || lower == "previous step" || lower == "pichhla step"

    private fun isRepeatCommand(lower: String) =
        lower == "repeat" || lower == "repeat step" || lower == "dobara bolo" || lower.contains("phir se bolo")

    private fun checkParameterModification(lower: String, scene: VisualScene): Pair<String, Float>? {
        when {
            lower.contains("mass double") || lower.contains("double mass") -> {
                val paramKey = if (scene.parameters.containsKey("massM")) "massM" else "mass"
                return paramKey to 2.0f
            }
            lower.contains("mass half") || lower.contains("mass aadha") -> {
                val paramKey = if (scene.parameters.containsKey("massM")) "massM" else "mass"
                return paramKey to 0.5f
            }
            lower.contains("force double") || lower.contains("double force") -> {
                return "force" to 2.0f
            }
            lower.contains("force half") -> {
                return "force" to 0.5f
            }
            lower.contains("distance double") || lower.contains("distance badhao") -> {
                return "distance" to 2.0f
            }
            lower.contains("distance half") || lower.contains("distance kam") -> {
                return "distance" to 0.5f
            }
            lower.contains("frequency double") -> {
                return "frequency" to 2.0f
            }
        }
        return null
    }

    private fun explainCurrentSceneState(scene: VisualScene): String {
        return when (scene.sceneId) {
            "gravity_spacetime" -> {
                val dist = scene.parameters["distance"]?.value ?: 1.0f
                val mass = scene.parameters["massM"]?.value ?: 5.97f
                val forceFactor = mass / (dist * dist)
                "Current state: Central mass is $mass, orbital distance is $dist r. Inverse square law ke anusar, gravitational pull proportional hai ${String.format("%.2f", forceFactor)}."
            }
            "newton_second_law" -> {
                val f = scene.parameters["force"]?.value ?: 10f
                val m = scene.parameters["mass"]?.value ?: 2f
                val a = scene.parameters["acceleration"]?.value ?: (f / m)
                "Current state: Force = $f N, Mass = $m kg. Newton ke niyam se Acceleration = F/m = ${String.format("%.2f", a)} m/s²."
            }
            "math_sine_wave" -> {
                val freq = scene.parameters["frequency"]?.value ?: 2.0f
                "Current state: Wave frequency is $freq Hz. Oscillation rate dynamic particles par update ho raha hai."
            }
            else -> {
                scene.explanation.ifBlank { "Scene '${scene.title}' is actively running." }
            }
        }
    }

    private fun simplifyConcept(scene: VisualScene): String {
        return when (scene.sceneId) {
            "gravity_spacetime" -> "Simple bhasha me: Badi cheezein jaise Earth space ko jhuka deti hain, isliye paas ki cheezein uski taraf khinchti hain. Door jane par khichav bohot tezi se kam hota hai."
            "newton_second_law" -> "Simple bhasha me: Jitna zyada dhakka (force) lagaoge, cheez utni tezi se bhagegi (acceleration). Lekin agar cheez bhari (mass) hai, toh speed badhana mushkil hoga."
            "water_molecule" -> "Simple bhasha me: Pani ka har molecule ek Oxygen aur do Hydrogen se banta hai, jaise ek V-shape. Is bent angle ki wajah se pani doosre molecules ko chipkata hai."
            else -> "Concept ${scene.title}: Fundamental physics particles ki geometry aur dynamic force vectors par aadharit hai."
        }
    }

    private fun giveRealWorldExample(scene: VisualScene): String {
        return when (scene.sceneId) {
            "gravity_spacetime" -> "Real world example: Chandrama (Moon) Earth ke charon taraf ghoomta hai kyunki Earth ki gravity use bandh kar rakhti hai, bilkul jaise haath me rassi se bandha pathar ghumaate hain."
            "newton_second_law" -> "Real world example: Ek khali shopping cart ko dhakelna aasan hai (chhota mass), par bhari cart ko accelerate karne ke liye bohot zyada force lagana padta hai."
            "biology_photosynthesis" -> "Real world example: Poudhon ki pattiyan natural solar panels ki tarah kaam karti hain jo sooraj ki dhoop se khana aur oxygen banati hain."
            else -> "Real world example: Everyday mechanics aur physical observations is mathematical principle ko follow karte hain."
        }
    }

    private fun extractMathCalculation(input: String): String? {
        val calcRegex = Regex("""(\d+(?:\.\d+)?)\s*([\+\-\*\/×x÷\^%])\s*(\d+(?:\.\d+)?)""")
        val match = calcRegex.find(input)
        return match?.value
    }
}
