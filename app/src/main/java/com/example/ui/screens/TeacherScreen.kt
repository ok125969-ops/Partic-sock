package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ai.UltronAiClient
import com.example.ai.YouTubeTeacherEngine
import com.example.ai.LessonSegment
import com.example.ai.LessonTimelineNode
import com.example.data.UltronMemoryEntity
import com.example.data.UltronRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherScreen(repository: UltronRepository) {
    var youtubeUrl by remember { mutableStateOf("") }
    var userInstruction by remember { mutableStateOf("") }
    var currentSegment by remember { mutableStateOf<LessonSegment?>(null) }
    var activeNodeIndex by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var quizSelectedOption by remember { mutableStateOf<Int?>(null) }
    var quizResultFeedback by remember { mutableStateOf<String?>(null) }
    var voiceCompanionText by remember { mutableStateOf("ULTRON Teacher AI online. Paste a YouTube lecture link or select a preset module to begin interactive 3D particle teaching.") }

    val coroutineScope = rememberCoroutineScope()

    val presetModules = listOf(
        "https://youtube.com/watch?v=physics_gravity_jee" to "Physics: Spacetime & Gravity (JEE Advanced)",
        "https://youtube.com/watch?v=math_circle_area" to "Mathematics: Circle Area & Pi Derivation",
        "https://youtube.com/watch?v=dna_biology_helix" to "Biology: DNA Double Helix & Replication"
    )

    fun startYouTubeLesson(url: String, instruction: String) {
        coroutineScope.launch {
            isLoading = true
            isPlaying = true
            voiceCompanionText = "ULTRON CORE: Analyzing YouTube content and establishing lesson timeline..."
            
            val segment = YouTubeTeacherEngine.analyzeYouTubeUrl(url, instruction)
            currentSegment = segment
            activeNodeIndex = 0
            quizSelectedOption = null
            quizResultFeedback = null

            val firstNode = segment.timeline.firstOrNull()
            voiceCompanionText = firstNode?.voicePrompt ?: "Lesson initiated. Observe the 3D particle visualization."
            isLoading = false

            repository.insertMemory(
                UltronMemoryEntity(
                    category = "LEARNING",
                    title = segment.videoTitle,
                    content = "YouTube Teacher Session initialized with ${segment.timeline.size} timeline concepts."
                )
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.SmartDisplay, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("YOUTUBE 3D PARTICLE TEACHER", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Text("Video → Understanding → 3D Simulation → Voice Tutor", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // YouTube Input Bar
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = youtubeUrl,
                        onValueChange = { youtubeUrl = it },
                        placeholder = { Text("Paste YouTube study URL (e.g. youtube.com/watch?v=...)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = userInstruction,
                            onValueChange = { userInstruction = it },
                            placeholder = { Text("Instruction (e.g., 'JEE level pe samjhao')") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (youtubeUrl.isBlank()) {
                                    youtubeUrl = "https://youtube.com/watch?v=physics_gravity_jee"
                                }
                                startYouTubeLesson(youtubeUrl, userInstruction)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start Lesson")
                        }
                    }
                }
            }

            // Preset Quick Links
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.height(110.dp)
            ) {
                item {
                    Text("Preset Study Lectures", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                }
                items(presetModules) { (url, title) ->
                    Card(
                        onClick = {
                            youtubeUrl = url
                            userInstruction = "Explain for JEE / 3D particle visualization"
                            startYouTubeLesson(url, userInstruction)
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                Text("Ready")
                            }
                        }
                    }
                }
            }

            // Voice Companion & Timeline Controller
            currentSegment?.let { segment ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(segment.videoTitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                                Text(segment.category)
                            }
                        }

                        // Voice Companion Bubble
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(voiceCompanionText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        // Timeline Node stepper
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            segment.timeline.forEachIndexed { index, node ->
                                FilterChip(
                                    selected = activeNodeIndex == index,
                                    onClick = {
                                        activeNodeIndex = index
                                        voiceCompanionText = node.voicePrompt
                                    },
                                    label = { Text("${node.timestamp} - ${node.conceptTitle}") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }

                        // Playback & Interruption Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                isPlaying = !isPlaying
                                voiceCompanionText = if (isPlaying) "Lesson resumed. Observing particle simulation." else "Lesson paused by Boss."
                            }) {
                                Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = "Play/Pause", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = {
                                if (activeNodeIndex > 0) activeNodeIndex--
                                voiceCompanionText = segment.timeline[activeNodeIndex].voicePrompt
                            }) {
                                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous Concept", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = {
                                if (activeNodeIndex < segment.timeline.size - 1) activeNodeIndex++
                                voiceCompanionText = segment.timeline[activeNodeIndex].voicePrompt
                            }) {
                                Icon(Icons.Filled.SkipNext, contentDescription = "Next Concept", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = {
                                voiceCompanionText = "ULTRON EXPLANATION: " + segment.timeline[activeNodeIndex].description + " (Simplified for Boss)"
                            }) {
                                Icon(Icons.Filled.Psychology, contentDescription = "Explain Simply", tint = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }

                // Interactive Quiz Section
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Quiz, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Interactive JEE Concept Check", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
                        }

                        Text(segment.quizQuestion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)

                        segment.quizOptions.forEachIndexed { optIdx, option ->
                            OutlinedButton(
                                onClick = {
                                    quizSelectedOption = optIdx
                                    if (optIdx == segment.correctAnswerIndex) {
                                        quizResultFeedback = "CORRECT! Brilliant reasoning, Boss. The particle simulation confirms this."
                                    } else {
                                        quizResultFeedback = "HINT: Review the particle forces at timestamp ${segment.timeline[activeNodeIndex].timestamp}. Try again!"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (quizSelectedOption == optIdx) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Text(option, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        quizResultFeedback?.let { feedback ->
                            Text(feedback, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
