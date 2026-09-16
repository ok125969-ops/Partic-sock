package com.example.ai

import androidx.compose.ui.graphics.Color

object VisualLearningEngine {

    suspend fun planVisualScene(prompt: String): VisualScene {
        val lower = prompt.trim().lowercase()

        // 1. Text commands: e.g. "Write OM", "Write ULTRON in particles", "particles se OM likho"
        if (lower.contains("write") || lower.contains("likho") || lower.contains("text")) {
            val textToExtract = when {
                lower.contains("om") || lower.contains("ॐ") -> "OM"
                lower.contains("ultron") -> "ULTRON"
                lower.contains("f = ma") || lower.contains("f=ma") -> "F = ma"
                lower.contains("e = mc") || lower.contains("e=mc") -> "E = mc²"
                else -> {
                    val words = prompt.split(" ")
                    val quoteMatch = Regex("\"([^\"]*)\"").find(prompt)?.groupValues?.get(1)
                    quoteMatch ?: words.lastOrNull { it.length in 2..8 }?.uppercase() ?: "ULTRON"
                }
            }

            return VisualScene(
                sceneId = "text_${textToExtract.lowercase()}",
                title = "Particle Text: $textToExtract",
                topic = "Typography & Geometry",
                shape = ShapePrimitive.TEXT,
                textToRender = textToExtract,
                baseColor = Color(0xFF00F0FF),
                behaviors = mutableListOf(ParticleBehaviorType.ROTATE, ParticleBehaviorType.SURFACE_TURBULENCE),
                explanation = "ULTRON: Sampled volumetric particle contour points for '$textToExtract'. 3D rotational vector initialized."
            )
        }

        // 2. Physics: Newton's Second Law (F = ma)
        if (lower.contains("newton") || lower.contains("second law") || lower.contains("f = ma") || lower.contains("f=ma")) {
            return VisualScene(
                sceneId = "newton_second_law",
                title = "Newton's Second Law (F = ma)",
                topic = "Physics Mechanics",
                shape = ShapePrimitive.CUBE,
                currentFormula = "F = m · a",
                baseColor = Color(0xFF00F0FF),
                parameters = mutableMapOf(
                    "force" to VisualParameter("force", 10f, "N", 1f, 50f, "Force (F)"),
                    "mass" to VisualParameter("mass", 2f, "kg", 0.5f, 10f, "Mass (m)"),
                    "acceleration" to VisualParameter("acceleration", 5f, "m/s²", 0.1f, 25f, "Acceleration (a)")
                ),
                behaviors = mutableListOf(ParticleBehaviorType.ROTATE),
                steps = listOf(
                    TeachingStep(
                        stepId = 1,
                        title = "Object of Mass (m)",
                        narration = "Boss, consider an object with mass m = 2 kg represented in 3D particle space.",
                        formula = "m = 2 kg",
                        shape = ShapePrimitive.CUBE,
                        primaryColor = Color(0xFF00F0FF)
                    ),
                    TeachingStep(
                        stepId = 2,
                        title = "Applying Force (F)",
                        narration = "When a net external force F = 10 N is exerted, particles accelerate in the force direction.",
                        formula = "F = 10 N",
                        shape = ShapePrimitive.CUBE,
                        primaryColor = Color(0xFF00FF66),
                        behaviors = listOf(ParticleBehaviorType.WAVE_OSCILLATE)
                    ),
                    TeachingStep(
                        stepId = 3,
                        title = "Acceleration Relation (a = F/m)",
                        narration = "Acceleration is directly proportional to net force and inversely proportional to mass: a = 10/2 = 5 m/s².",
                        formula = "a = F / m = 5 m/s²",
                        shape = ShapePrimitive.CUBE,
                        primaryColor = Color(0xFFFFD700)
                    ),
                    TeachingStep(
                        stepId = 4,
                        title = "Interactive Concept Check",
                        narration = "Let's verify: If we double the Force to 20 N keeping mass at 2 kg, what is the new acceleration?",
                        formula = "F = 20 N, m = 2 kg → a = ?",
                        shape = ShapePrimitive.CUBE,
                        question = "If Force is doubled from 10 N to 20 N with constant mass (2 kg), acceleration becomes:",
                        options = listOf("10 m/s² (Doubles)", "5 m/s² (Unchanged)", "2.5 m/s² (Halves)", "20 m/s² (Quadruples)"),
                        correctOptionIndex = 0
                    )
                ),
                explanation = "ULTRON TEACHER: Initialized step-by-step 3D particle demonstration for Newton's Second Law."
            )
        }

        // 3. Physics: Gravity & Spacetime
        if (lower.contains("gravity") || lower.contains("gurutvakarshan")) {
            return VisualScene(
                sceneId = "gravity_spacetime",
                title = "Gravitational Force & Spacetime",
                topic = "Astrophysics",
                shape = ShapePrimitive.EARTH,
                currentFormula = "F = G · (M · m) / r²",
                baseColor = Color(0xFF00F0FF),
                parameters = mutableMapOf(
                    "massM" to VisualParameter("massM", 5.97f, "x10²⁴ kg", 1f, 20f, "Central Mass (M)"),
                    "distance" to VisualParameter("distance", 1f, "r", 0.5f, 5f, "Orbital Radius (r)"),
                    "gravityForce" to VisualParameter("gravityForce", 9.8f, "m/s²", 1f, 30f, "Force (g)")
                ),
                behaviors = mutableListOf(ParticleBehaviorType.ROTATE, ParticleBehaviorType.ORBIT),
                steps = listOf(
                    TeachingStep(
                        stepId = 1,
                        title = "Central Massive Body (Earth)",
                        narration = "Boss, observe Earth rendered in 13,000 holographic particles. Its immense mass curves the surrounding spacetime matrix.",
                        formula = "M = 5.97 × 10²⁴ kg",
                        shape = ShapePrimitive.EARTH,
                        primaryColor = Color(0xFF00F0FF)
                    ),
                    TeachingStep(
                        stepId = 2,
                        title = "Gravitational Attraction Field",
                        narration = "Every secondary mass feels an inward radial pull toward the center of mass.",
                        formula = "F_g = G · (M · m) / r²",
                        shape = ShapePrimitive.EARTH,
                        primaryColor = Color(0xFF00FF66),
                        behaviors = listOf(ParticleBehaviorType.ROTATE, ParticleBehaviorType.ATTRACT)
                    ),
                    TeachingStep(
                        stepId = 3,
                        title = "Inverse Square Law",
                        narration = "Notice that doubling the distance cuts the gravitational attraction to one-fourth (1/4th) of its value.",
                        formula = "F ∝ 1 / r²",
                        shape = ShapePrimitive.RINGS,
                        primaryColor = Color(0xFFFF9900)
                    )
                ),
                explanation = "ULTRON: Visualizing planetary gravitational field and spacetime curvature."
            )
        }

        // 4. Mathematics: Sine Wave
        if (lower.contains("sine") || lower.contains("sin wave") || lower.contains("wave") || lower.contains("tarang")) {
            return VisualScene(
                sceneId = "math_sine_wave",
                title = "Trigonometric Sine Wave (y = A·sin(ωt))",
                topic = "Mathematics & Harmonic Motion",
                shape = ShapePrimitive.WAVE,
                currentFormula = "y(x, t) = A · sin(2πft + φ)",
                baseColor = Color(0xFF00F0FF),
                parameters = mutableMapOf(
                    "frequency" to VisualParameter("frequency", 2.0f, "Hz", 0.5f, 6.0f, "Frequency (f)"),
                    "amplitude" to VisualParameter("amplitude", 1.0f, "A", 0.2f, 3.0f, "Amplitude (A)")
                ),
                behaviors = mutableListOf(ParticleBehaviorType.WAVE_OSCILLATE),
                steps = listOf(
                    TeachingStep(
                        stepId = 1,
                        title = "Fundamental Wave Anatomy",
                        narration = "Boss, observing a continuous 3D particle wave. The vertical displacement oscillates between +A and -A.",
                        formula = "y = A · sin(ωt)",
                        shape = ShapePrimitive.WAVE,
                        primaryColor = Color(0xFF00F0FF)
                    ),
                    TeachingStep(
                        stepId = 2,
                        title = "Frequency Modulation",
                        narration = "Increasing frequency packs more wave cycles per unit distance, representing higher quantum energy.",
                        formula = "f = 4.0 Hz, λ = v / f",
                        shape = ShapePrimitive.WAVE,
                        primaryColor = Color(0xFFFF0055),
                        parameterUpdates = mapOf("frequency" to 4.0f)
                    )
                ),
                explanation = "ULTRON: Synthesized 3D trigonometric sine wave simulation with dynamic harmonic oscillation."
            )
        }

        // 5. Chemistry: Water Molecule (H2O)
        if (lower.contains("water") || lower.contains("h2o") || lower.contains("pani") || lower.contains("molecule")) {
            return VisualScene(
                sceneId = "water_molecule",
                title = "Water Molecule (H₂O) Covalent Geometry",
                topic = "Chemistry & Molecular Orbitals",
                shape = ShapePrimitive.MOLECULE_H2O,
                currentFormula = "H - O - H (Bond Angle: 104.5°)",
                baseColor = Color(0xFF00F0FF),
                behaviors = mutableListOf(ParticleBehaviorType.ROTATE, ParticleBehaviorType.SURFACE_TURBULENCE),
                steps = listOf(
                    TeachingStep(
                        stepId = 1,
                        title = "Oxygen & Hydrogen Nuclei",
                        narration = "Boss, central red/cyan core is Oxygen, bonded to two Hydrogen nuclei at exactly 104.5 degrees.",
                        formula = "H₂O Molecular Dipole",
                        shape = ShapePrimitive.MOLECULE_H2O,
                        primaryColor = Color(0xFF00F0FF)
                    ),
                    TeachingStep(
                        stepId = 2,
                        title = "Polarity & Hydrogen Bonding",
                        narration = "Electronegative Oxygen pulls shared electrons, creating a dipole moment responsible for surface tension.",
                        formula = "δ⁻ (O) ···· δ⁺ (H)",
                        shape = ShapePrimitive.MOLECULE_H2O,
                        primaryColor = Color(0xFFFF00FF)
                    )
                ),
                explanation = "ULTRON: Constructing 3D H₂O molecular polar lattice."
            )
        }

        // 6. Biology: Photosynthesis
        if (lower.contains("photosynthesis") || lower.contains("prakash sanshleshan") || lower.contains("chloroplast")) {
            return VisualScene(
                sceneId = "biology_photosynthesis",
                title = "Photosynthesis Energy Conversion",
                topic = "Biology & Biochemistry",
                shape = ShapePrimitive.PHOTOSYNTHESIS,
                currentFormula = "6CO₂ + 6H₂O + photons → C₆H₁₂O₆ + 6O₂",
                baseColor = Color(0xFF00FF66),
                behaviors = mutableListOf(ParticleBehaviorType.ROTATE),
                steps = listOf(
                    TeachingStep(
                        stepId = 1,
                        title = "Photon Absorption by Chlorophyll",
                        narration = "Solar photons strike the leaf's thylakoid membranes, exciting electrons into high-energy transport chains.",
                        formula = "hν (Photons) → Chlorophyll",
                        shape = ShapePrimitive.PHOTOSYNTHESIS,
                        primaryColor = Color(0xFFFFD700)
                    ),
                    TeachingStep(
                        stepId = 2,
                        title = "Synthesis of Glucose (C6H12O6)",
                        narration = "Calvin cycle assimilates CO₂ and water to produce vital glucose energy stores and Oxygen gas.",
                        formula = "C₆H₁₂O₆ + 6O₂ Generated",
                        shape = ShapePrimitive.PHOTOSYNTHESIS,
                        primaryColor = Color(0xFF00FF66)
                    )
                ),
                explanation = "ULTRON: Visualizing biochemical solar energy transduction in plants."
            )
        }

        // 7. Physics: Projectile Motion
        if (lower.contains("projectile") || lower.contains("trajectory") || lower.contains("parabola")) {
            return VisualScene(
                sceneId = "projectile_motion",
                title = "2D/3D Parabolic Projectile Trajectory",
                topic = "Classical Kinematics",
                shape = ShapePrimitive.PROJECTILE_MOTION,
                currentFormula = "y = x·tan(θ) - (g·x²) / (2·u²·cos²(θ))",
                baseColor = Color(0xFF00F0FF),
                behaviors = mutableListOf(ParticleBehaviorType.ROTATE),
                explanation = "ULTRON: Simulating projectile arc with velocity and gravitational vector decomposition."
            )
        }

        // 8. Galaxy
        if (lower.contains("galaxy") || lower.contains("milky") || lower.contains("nebula")) {
            return VisualScene(
                sceneId = "astronomy_galaxy",
                title = "3D Spiral Galaxy Accretion Disk",
                topic = "Cosmology",
                shape = ShapePrimitive.GALAXY,
                baseColor = Color(0xFF00F0FF),
                behaviors = mutableListOf(ParticleBehaviorType.ROTATE, ParticleBehaviorType.VORTEX),
                explanation = "ULTRON: Assembling rotating logarithmic spiral galaxy accretion arms."
            )
        }

        // 9. DNA Double Helix
        if (lower.contains("dna") || lower.contains("helix") || lower.contains("genetic")) {
            return VisualScene(
                sceneId = "biology_dna",
                title = "DNA Double Helix & Nucleotide Pairing",
                topic = "Molecular Genetics",
                shape = ShapePrimitive.DNA,
                currentFormula = "Adenine=Thymine, Guanine≡Cytosine",
                baseColor = Color(0xFF00FF66),
                behaviors = mutableListOf(ParticleBehaviorType.ROTATE),
                explanation = "ULTRON: Synthesizing antiparallel DNA double helix lattice."
            )
        }

        // 10. Human silhouette / Robot mech
        if (lower.contains("human") || lower.contains("silhouette") || lower.contains("manav") || lower.contains("robot") || lower.contains("mech") || lower.contains("iron man")) {
            return VisualScene(
                sceneId = "human_silhouette",
                title = "3D Cybernetic Humanoid Silhouette",
                topic = "Biometrics & Robotics",
                shape = ShapePrimitive.HUMAN_SILHOUETTE,
                baseColor = Color(0xFF00F0FF),
                behaviors = mutableListOf(ParticleBehaviorType.ROTATE),
                explanation = "ULTRON: Rendering full 3D cybernetic anatomical silhouette."
            )
        }

        // 11. Holographic primitives: Cube, Cylinder, Cone, Torus
        if (lower.contains("torus") || lower.contains("donut")) {
            return VisualScene(
                sceneId = "primitive_torus",
                title = "3D Geometric Torus",
                topic = "Topology",
                shape = ShapePrimitive.TORUS,
                baseColor = Color(0xFF00F0FF),
                behaviors = mutableListOf(ParticleBehaviorType.ROTATE),
                explanation = "ULTRON: Projecting 3D holographic topological Torus ring."
            )
        }
        if (lower.contains("cube") || lower.contains("tesseract")) {
            return VisualScene(
                sceneId = "primitive_cube",
                title = "3D Holographic Cube",
                topic = "Solid Geometry",
                shape = ShapePrimitive.CUBE,
                baseColor = Color(0xFF00F0FF),
                behaviors = mutableListOf(ParticleBehaviorType.ROTATE),
                explanation = "ULTRON: Generating 3D orthogonal coordinate cube."
            )
        }

        // Default: If Gemini API is active, get conversational explanation or use custom cloud
        val aiResponse = UltronAiClient.askUltron("Provide visual 3D particle breakdown for concept: '$prompt'")
        return VisualScene(
            sceneId = "custom_${prompt.take(8).trim().replace(" ", "_")}",
            title = "Custom Holographic Field: $prompt",
            topic = "General Hologram",
            shape = ShapePrimitive.CUSTOM_CLOUD,
            baseColor = Color(0xFF00F0FF),
            behaviors = mutableListOf(ParticleBehaviorType.ROTATE, ParticleBehaviorType.SURFACE_TURBULENCE),
            explanation = aiResponse
        )
    }

    fun modifySceneParameter(scene: VisualScene, paramName: String, multiplier: Float): String {
        val param = scene.parameters[paramName] ?: return "Parameter '$paramName' not found in current scene."
        val oldVal = param.value
        param.value = (param.value * multiplier).coerceIn(param.min, param.max)

        // Update connected physics parameters if Newton's second law
        if (scene.sceneId == "newton_second_law") {
            val f = scene.parameters["force"]?.value ?: 10f
            val m = scene.parameters["mass"]?.value ?: 2f
            val newA = f / m
            scene.parameters["acceleration"]?.value = newA
            return "ULTRON: Updated $paramName from $oldVal to ${param.value} ${param.unit}. New Acceleration = ${String.format("%.2f", newA)} m/s²."
        }

        return "ULTRON: Adjusted $paramName from $oldVal to ${param.value} ${param.unit}."
    }
}
