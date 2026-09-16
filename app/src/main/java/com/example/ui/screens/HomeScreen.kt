package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.ConversationManager
import com.example.ai.UltronAiClient
import com.example.ai.VisualLearningEngine
import com.example.ai.VisualScene
import com.example.data.UltronMemoryEntity
import com.example.data.UltronRepository
import com.example.tools.ToolContext
import com.example.ui.components.HolographicParticleSystem
import com.example.ui.components.HolographicState
import com.example.ui.components.HolographicWorldEngineView
import com.example.voice.VoiceInputState
import com.example.voice.VoiceOutputState
import com.example.voice.VoiceSessionManager
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    repository: UltronRepository,
    onNavigateToTeacher: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val particleSystem = remember { HolographicParticleSystem(count = 13107) }

    val toolContext = remember { ToolContext(context = context, repository = repository, activeScene = null) }
    val conversationManager = remember { ConversationManager(context = context, toolContext = toolContext) }
    val voiceSessionManager = remember {
        VoiceSessionManager(
            context = context,
            particleEngine = particleSystem,
            conversationManager = conversationManager
        )
    }

    val voiceState by voiceSessionManager.sessionState.collectAsStateWithLifecycle()

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            voiceSessionManager.startListening()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceSessionManager.shutdown()
        }
    }

    var commandInput by remember { mutableStateOf("") }
    var currentFps by remember { mutableStateOf(60) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var tempApiKey by remember { mutableStateOf(UltronAiClient.getEffectiveApiKey()) }

    val currentScene = voiceSessionManager.teacherController.activeScene
    val currentStepIndex = voiceSessionManager.teacherController.currentStepIndex
    val teacherNarration = voiceState.activeNarrationText.ifBlank {
        "ULTRON VISUAL ENGINE: Ready. Voice or type commands to create 3D holograms or teach concepts."
    }
    var quizSelectedOption by remember { mutableStateOf<Int?>(null) }
    var quizFeedback by remember { mutableStateOf<String?>(null) }

    fun execute(cmd: String) {
        val trimmed = cmd.trim()
        if (trimmed.isBlank()) return
        commandInput = ""
        coroutineScope.launch {
            voiceSessionManager.sendTextCommand(trimmed)
        }
    }

    fun handleMicClick() {
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        when (voiceState.inputState) {
            VoiceInputState.LISTENING -> {
                voiceSessionManager.stopListening()
            }
            VoiceInputState.SPEAKING -> {
                voiceSessionManager.handleTeacherInterruption()
                voiceSessionManager.startListening()
            }
            else -> {
                voiceSessionManager.startListening()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000306))
    ) {
        // Holographic 3D Particle Canvas
        HolographicWorldEngineView(
            system = particleSystem,
            onFpsCalculated = { fps ->
                currentFps = fps
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top HUD Status Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ULTRON V2 // VOICE + PARTICLE OS",
                    color = Color(0xFF00F0FF),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                // Sleek HUD icon for API / Capabilities & Navigation
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { showApiKeyDialog = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VpnKey,
                            contentDescription = "API Key",
                            tint = Color(0xFF00F0FF).copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (onNavigateToTeacher != null) {
                        IconButton(
                            onClick = onNavigateToTeacher,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.School,
                                contentDescription = "Teacher Mode",
                                tint = Color(0xFF00F0FF).copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            val stateString = when (particleSystem.state) {
                HolographicState.IDLE -> "IDLE"
                HolographicState.FORMING -> "FORMING"
                HolographicState.ACTIVE -> "ACTIVE"
                HolographicState.EXPLODING -> "EXPLODING"
            }

            Text(
                text = "FPS: $currentFps | PARTICLES: ${particleSystem.count} | STATE: $stateString | ACTIVE: ${particleSystem.activeName}",
                color = Color(0xFF00F0FF).copy(alpha = 0.85f),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 0.4.sp
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Voice Engine Status & Brain Badges Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Voice State Badge
                val (voiceBadgeColor, voiceBadgeText) = when (voiceState.inputState) {
                    VoiceInputState.LISTENING -> Color(0xFF00F0FF) to "● LISTENING (WAVEFORM ACTIVE)"
                    VoiceInputState.PROCESSING -> Color(0xFF00B0FF) to "◌ PROCESSING SPEECH..."
                    VoiceInputState.THINKING -> Color(0xFFFFD700) to "✦ THINKING (QUANTUM VORTEX)"
                    VoiceInputState.EXECUTING -> Color(0xFFFFAA00) to "⚙ EXECUTING TOOL..."
                    VoiceInputState.SPEAKING -> Color(0xFF00FF66) to "🔊 TEACHER SPEAKING (AUDIO PULSE)"
                    VoiceInputState.INTERRUPTED -> Color(0xFFFF0055) to "⏸ INTERRUPTED (ASK QUESTION)"
                    VoiceInputState.ERROR -> Color(0xFFFF4444) to "! VOICE ISSUE"
                    VoiceInputState.IDLE -> Color(0xFF00F0FF).copy(alpha = 0.5f) to "VOICE: IDLE"
                }

                Box(
                    modifier = Modifier
                        .background(Color(0xFF021017), RoundedCornerShape(3.dp))
                        .border(1.dp, voiceBadgeColor.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = voiceBadgeText,
                        color = voiceBadgeColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Brain Mode Badge (Online vs Offline Fallback)
                val isOnline = UltronAiClient.isConfigured()
                Box(
                    modifier = Modifier
                        .background(Color(0xFF021017), RoundedCornerShape(3.dp))
                        .border(1.dp, if (isOnline) Color(0xFF00F0FF).copy(alpha = 0.5f) else Color(0xFF88AA88).copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isOnline) "BRAIN: ONLINE GEMINI" else "BRAIN: OFFLINE LOCAL",
                        color = if (isOnline) Color(0xFF00F0FF) else Color(0xFF88AA88),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }

                // Audio Permission Status
                Box(
                    modifier = Modifier
                        .background(Color(0xFF021017), RoundedCornerShape(3.dp))
                        .border(1.dp, Color(0xFF00F0FF).copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                        .clickable {
                            if (!hasAudioPermission) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (hasAudioPermission) "MIC: READY" else "MIC: TAP TO PERMIT",
                        color = if (hasAudioPermission) Color(0xFF00FF66) else Color(0xFFFF9900),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
            }

            // Current Active Formula / Law Badge
            currentScene?.currentFormula?.let { formula ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF002233), RoundedCornerShape(3.dp))
                            .border(1.dp, Color(0xFF00F0FF).copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "FORMULA: $formula",
                            color = Color(0xFF00F0FF),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Bottom Controls, Narration, Quick Chips and Command Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Live Voice Feedback Banner (Acoustic audio wave, recognized words, or interruption alert)
            if (voiceState.inputState != VoiceInputState.IDLE || voiceState.errorMessage != null || voiceState.recognizedPartialText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF02111D).copy(alpha = 0.95f), RoundedCornerShape(4.dp))
                        .border(
                            1.dp,
                            when (voiceState.inputState) {
                                VoiceInputState.LISTENING -> Color(0xFF00F0FF)
                                VoiceInputState.THINKING -> Color(0xFFFFD700)
                                VoiceInputState.SPEAKING -> Color(0xFF00FF66)
                                VoiceInputState.INTERRUPTED -> Color(0xFFFF0055)
                                VoiceInputState.ERROR -> Color(0xFFFF4444)
                                else -> Color(0xFF00F0FF).copy(alpha = 0.5f)
                            },
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            when (voiceState.inputState) {
                                VoiceInputState.LISTENING -> {
                                    Icon(
                                        imageVector = Icons.Filled.GraphicEq,
                                        contentDescription = null,
                                        tint = Color(0xFF00F0FF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (voiceState.recognizedPartialText.isNotBlank())
                                            "Listening: \"${voiceState.recognizedPartialText}\""
                                        else
                                            "Listening for voice command... (Particles forming waveform)",
                                        color = Color(0xFF00F0FF),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                                VoiceInputState.THINKING -> {
                                    Icon(
                                        imageVector = Icons.Filled.Autorenew,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "AI Thinking & Synthesizing Tool Execution...",
                                        color = Color(0xFFFFD700),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                                VoiceInputState.SPEAKING -> {
                                    Icon(
                                        imageVector = Icons.Filled.VolumeUp,
                                        contentDescription = null,
                                        tint = Color(0xFF00FF66),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Teacher speaking... Tap 'RUKO' or mic to interrupt",
                                        color = Color(0xFF00FF66),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                                VoiceInputState.INTERRUPTED -> {
                                    Icon(
                                        imageVector = Icons.Filled.PauseCircle,
                                        contentDescription = null,
                                        tint = Color(0xFFFF0055),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Narration paused. Ask a question or say 'Continue'.",
                                        color = Color(0xFFFF0055),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                                VoiceInputState.ERROR -> {
                                    Icon(
                                        imageVector = Icons.Filled.ErrorOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFFF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = voiceState.errorMessage ?: "Voice input issue.",
                                        color = Color(0xFFFF4444),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                                else -> {
                                    if (voiceState.recognizedFinalText.isNotBlank()) {
                                        Text(
                                            text = "Understood: \"${voiceState.recognizedFinalText}\"",
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        // Interruption / Stop affordance
                        if (voiceState.inputState == VoiceInputState.SPEAKING) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFF0055), RoundedCornerShape(2.dp))
                                    .clickable {
                                        voiceSessionManager.handleTeacherInterruption()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "RUKO",
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (voiceState.inputState == VoiceInputState.LISTENING) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF003344), RoundedCornerShape(2.dp))
                                    .clickable {
                                        voiceSessionManager.stopListening()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "DONE",
                                    color = Color(0xFF00F0FF),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Educational Step Card (if active scene has multi-step lesson)
            currentScene?.let { scene ->
                if (scene.steps.isNotEmpty()) {
                    val activeStep = scene.steps.getOrNull(currentStepIndex)
                    if (activeStep != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF05131C).copy(alpha = 0.94f)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF00F0FF).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "LESSON STEP ${activeStep.stepId}/${scene.steps.size}: ${activeStep.title}",
                                        color = Color(0xFF00F0FF),
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        // Pause / Resume Teacher Narration
                                        Text(
                                            text = if (voiceState.isPaused) "▶ RESUME" else "❚❚ PAUSE",
                                            color = if (voiceState.isPaused) Color(0xFF00FF66) else Color(0xFFFFCC00),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clickable { voiceSessionManager.togglePauseResume() }
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )

                                        if (currentStepIndex > 0) {
                                            Text(
                                                text = "< PREV",
                                                color = Color(0xFF00F0FF),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .clickable {
                                                        voiceSessionManager.teacherController.prevStep()
                                                        quizSelectedOption = null
                                                        quizFeedback = null
                                                    }
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }

                                        if (currentStepIndex < scene.steps.size - 1) {
                                            Text(
                                                text = "NEXT >",
                                                color = Color(0xFF00FF66),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .clickable {
                                                        voiceSessionManager.teacherController.nextStep()
                                                        quizSelectedOption = null
                                                        quizFeedback = null
                                                    }
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                // Interactive Question if step has concept check
                                activeStep.question?.let { qText ->
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "QUIZ: $qText",
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        activeStep.options.forEachIndexed { optIndex, optText ->
                                            val isSelected = quizSelectedOption == optIndex
                                            val isCorrect = optIndex == activeStep.correctOptionIndex
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (isSelected) (if (isCorrect) Color(0xFF005522) else Color(0xFF550011)) else Color(0xFF08202C),
                                                        RoundedCornerShape(3.dp)
                                                    )
                                                    .border(1.dp, if (isSelected) (if (isCorrect) Color(0xFF00FF66) else Color(0xFFFF0055)) else Color(0xFF00F0FF).copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                                                    .clickable {
                                                        quizSelectedOption = optIndex
                                                        quizFeedback = if (isCorrect) "✓ Correct! Concept verified." else "✗ Re-checking physics principle."
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = optText,
                                                    color = Color.White,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.5.sp
                                                )
                                            }
                                        }
                                    }
                                    quizFeedback?.let { fb ->
                                        Text(
                                            text = fb,
                                            color = if (fb.startsWith("✓")) Color(0xFF00FF66) else Color(0xFFFF4466),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Interactive Parameter Chips (e.g. Force: 10N [+], Mass: 2kg [-])
                if (scene.parameters.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        scene.parameters.values.forEach { param ->
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF061A24).copy(alpha = 0.9f), RoundedCornerShape(3.dp))
                                    .border(1.dp, Color(0xFF00F0FF).copy(alpha = 0.45f), RoundedCornerShape(3.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${param.label}: ${String.format("%.1f", param.value)} ${param.unit}",
                                        color = Color(0xFF00F0FF),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "[+]",
                                        color = Color(0xFF00FF66),
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.clickable {
                                            VisualLearningEngine.modifySceneParameter(scene, param.name, 1.5f)
                                            particleSystem.updateParameters()
                                        }
                                    )
                                    Text(
                                        text = "[-]",
                                        color = Color(0xFFFF5555),
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.clickable {
                                            VisualLearningEngine.modifySceneParameter(scene, param.name, 0.67f)
                                            particleSystem.updateParameters()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Teacher Narration Bubble
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF030D14).copy(alpha = 0.88f), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFF00F0FF).copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.RecordVoiceOver,
                    contentDescription = null,
                    tint = if (voiceState.outputState == VoiceOutputState.SPEAKING) Color(0xFF00FF66) else Color(0xFF00F0FF),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = teacherNarration,
                    color = Color.White.copy(alpha = 0.95f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    maxLines = 2,
                    modifier = Modifier.weight(1f)
                )
            }

            // Quick Interactive Scene & Learning Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val presets = listOf(
                    "🎙 Gravity Samjhao" to "Gravity samjhao",
                    "🎙 Double Mass" to "Object ko double mass ka karo",
                    "🎙 Ab Kya Hua?" to "Ab kya hua?",
                    "🎙 Calc: 25 × 48" to "Calculator kholo aur 25 × 48 calculate karo",
                    "🎙 Time Kya Hua?" to "Abhi kya time hua hai",
                    "Earth" to "Make Earth using particles",
                    "Write OM" to "Write OM using particles",
                    "Newton's 2nd Law" to "Newton's second law samjhao",
                    "Water (H₂O)" to "Water molecule samjhao",
                    "Sine Wave" to "sin wave samjhao",
                    "Photosynthesis" to "Photosynthesis visualize karo",
                    "Galaxy" to "Create a galaxy",
                    "Explode" to "Explode",
                    "Reform" to "Reform",
                    "Rotate" to "Rotate"
                )

                presets.forEach { (label, command) ->
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF041520), RoundedCornerShape(3.dp))
                            .border(1.dp, Color(0xFF00F0FF).copy(alpha = 0.35f), RoundedCornerShape(3.dp))
                            .clickable { execute(command) }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = label,
                            color = Color(0xFF00F0FF),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Voice-Aware Command Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color(0xFF051017).copy(alpha = 0.92f), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFF00F0FF).copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Interactive Microphone Button with Glowing Visual Pulse
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            when (voiceState.inputState) {
                                VoiceInputState.LISTENING -> Color(0xFF00F0FF)
                                VoiceInputState.SPEAKING -> Color(0xFFFF0055)
                                VoiceInputState.THINKING -> Color(0xFFFFD700)
                                else -> Color(0xFF05202C)
                            }
                        )
                        .border(
                            1.5.dp,
                            when (voiceState.inputState) {
                                VoiceInputState.LISTENING -> Color.White
                                VoiceInputState.SPEAKING -> Color(0xFFFF6699)
                                VoiceInputState.THINKING -> Color(0xFFFFEE55)
                                else -> Color(0xFF00F0FF).copy(alpha = 0.7f)
                            },
                            CircleShape
                        )
                        .clickable { handleMicClick() },
                    contentAlignment = Alignment.Center
                ) {
                    when (voiceState.inputState) {
                        VoiceInputState.LISTENING -> {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = "Listening",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        VoiceInputState.SPEAKING -> {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = "Interrupt",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        VoiceInputState.THINKING -> {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = "Voice Input",
                                tint = Color(0xFF00F0FF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (commandInput.isEmpty()) {
                        Text(
                            text = if (voiceState.inputState == VoiceInputState.LISTENING)
                                "Boliye (Listening)..."
                            else
                                "Voice mic tap karein ya likhein...",
                            color = Color(0xFF00F0FF).copy(alpha = 0.45f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.5.sp,
                            maxLines = 1
                        )
                    }
                    BasicTextField(
                        value = commandInput,
                        onValueChange = { commandInput = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        ),
                        cursorBrush = SolidColor(Color(0xFF00F0FF)),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Cyan Solid EXECUTE Button
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .background(Color(0xFF00F0FF), RoundedCornerShape(2.dp))
                        .clickable {
                            execute(commandInput)
                        }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "EXECUTE",
                        color = Color.Black,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }
    }

    // Gemini API Key Dialog
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Text("INTEGRATED GEMINI API KEY", color = Color(0xFF00F0FF), fontFamily = FontFamily.Monospace)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "ULTRON integrates the Gemini API so you can create ANY 3D particle entity in existence (e.g. 'dragon', 'taj mahal', 'galaxy', 'quantum atom').",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = tempApiKey,
                        onValueChange = { tempApiKey = it },
                        placeholder = { Text("Enter Gemini API Key or leave default") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    )
                    Text(
                        if (UltronAiClient.getEffectiveApiKey().isNotBlank()) "Status: API Key Active & Integrated" else "Status: Running in Autonomous Local Simulation Mode",
                        color = Color(0xFF00FF66),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        UltronAiClient.userCustomApiKey = tempApiKey.trim()
                        showApiKeyDialog = false
                    }
                ) {
                    Text("SAVE & INTEGRATE", color = Color(0xFF00F0FF), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("CLOSE", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = Color(0xFF07121A)
        )
    }
}
