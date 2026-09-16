package com.example.ai

data class LessonTimelineNode(
    val timestamp: String,
    val conceptTitle: String,
    val description: String,
    val particleShape: String,
    val voicePrompt: String
)

data class LessonSegment(
    val videoTitle: String,
    val category: String,
    val timeline: List<LessonTimelineNode>,
    val quizQuestion: String,
    val quizOptions: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String
)

object YouTubeTeacherEngine {
    suspend fun analyzeYouTubeUrl(url: String, userInstruction: String): LessonSegment {
        val lowerUrl = url.lowercase()
        val basePrompt = "Analyze YouTube lecture: $url with instruction: '$userInstruction'. Extract key physics/math/science concepts, timestamp timeline, and generate a JEE-style concept breakdown."
        val aiResponse = UltronAiClient.askUltron(basePrompt, systemPrompt = "You are ULTRON V2 YouTube AI Teacher. Provide a structured lesson breakdown with topic segmentation and particle visualization instructions.")

        val isPhysics = lowerUrl.contains("physics") || lowerUrl.contains("gravity") || lowerUrl.contains("motion") || lowerUrl.contains("wave") || userInstruction.lowercase().contains("physics") || userInstruction.lowercase().contains("gravity")
        val isMath = lowerUrl.contains("math") || lowerUrl.contains("circle") || lowerUrl.contains("area") || lowerUrl.contains("calculus") || userInstruction.lowercase().contains("math")

        return when {
            isPhysics -> LessonSegment(
                videoTitle = if (url.isBlank()) "Advanced Physics Lecture: Spacetime & Gravity" else "YouTube Study: $url",
                category = "PHYSICS",
                timeline = listOf(
                    LessonTimelineNode("00:00", "Introduction to Mass & Curvature", "Understanding how mass bends spacetime fabric.", "GRAVITY", "Boss, let's observe how massive bodies curve space."),
                    LessonTimelineNode("02:15", "Orbital Mechanics & Force", "Analyzing velocity, acceleration, and gravitational pull.", "SOLAR_SYSTEM", "Notice the orbital balance maintained by gravitational force."),
                    LessonTimelineNode("05:30", "JEE Problem Solving (F = ma)", "Applying Newton's laws to dynamic particle motion.", "ATOM", "Now let's test our understanding with a JEE mechanics equation.")
                ),
                quizQuestion = "What happens to the orbital velocity of a satellite if the central mass increases by 2x?",
                quizOptions = listOf("Velocity increases by sqrt(2)", "Velocity remains unchanged", "Velocity decreases by half", "Satellite escapes orbit immediately"),
                correctAnswerIndex = 0,
                explanation = aiResponse
            )
            isMath -> LessonSegment(
                videoTitle = if (url.isBlank()) "Advanced Mathematics: Circle Area & Pi Calculus" else "YouTube Study: $url",
                category = "MATHEMATICS",
                timeline = listOf(
                    LessonTimelineNode("00:00", "Circle Geometry & Radius", "Defining radius r=4 and circumference.", "CUBE", "Boss, examining circle dimensions and radius vector."),
                    LessonTimelineNode("03:10", "Deriving Area Formula (pi * r^2)", "Visualizing radial sectors expanding into rectangular grid.", "BUTTERFLY", "Watch how radial expansion forms pi r squared."),
                    LessonTimelineNode("06:45", "Numerical Problem & JEE Integration", "Solving definite integrals for complex geometric areas.", "ATOM", "Applying calculus to calculate exact boundary areas.")
                ),
                quizQuestion = "What is the area of a circle with radius r = 4 units?",
                quizOptions = listOf("16π", "8π", "32π", "4π"),
                correctAnswerIndex = 0,
                explanation = aiResponse
            )
            else -> LessonSegment(
                videoTitle = if (url.isBlank()) "General Science & Molecular Structures" else "YouTube Study: $url",
                category = "SCIENCE",
                timeline = listOf(
                    LessonTimelineNode("00:00", "Core Concept Overview", "Introduction to atomic and molecular arrangements.", "DNA", "Boss, analyzing molecular code and nucleotide bonding."),
                    LessonTimelineNode("03:00", "Structural Morphing", "Transitioning from molecular chains to dynamic particle interaction.", "HEART", "Observing biophysical heartbeat synchronization."),
                    LessonTimelineNode("06:00", "Synthesis & Review", "Comprehensive visual recap of the lecture modules.", "ATOM", "Reviewing key insights from the study session.")
                ),
                quizQuestion = "Which molecular bond pairs Adenine with Thymine in DNA?",
                quizOptions = listOf("Hydrogen bonds", "Covalent bonds", "Ionic bonds", "Metallic bonds"),
                correctAnswerIndex = 0,
                explanation = aiResponse
            )
        }
    }
}
