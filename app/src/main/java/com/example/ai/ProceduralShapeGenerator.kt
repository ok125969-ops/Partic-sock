package com.example.ai

import kotlin.math.*
import kotlin.random.Random

object ProceduralShapeGenerator {

    fun generateShape(
        shape: ShapePrimitive,
        count: Int,
        outX: FloatArray,
        outY: FloatArray,
        outZ: FloatArray,
        textString: String? = null,
        parameters: Map<String, VisualParameter> = emptyMap()
    ) {
        val random = Random(1337)

        when (shape) {
            ShapePrimitive.TEXT -> {
                val str = if (textString.isNullOrBlank()) "ULTRON" else textString
                TextToParticleConverter.sampleTextPoints(str, count, outX, outY, outZ)
            }

            ShapePrimitive.SPHERE -> {
                val radius = (parameters["radius"]?.value ?: 1f) * 220f
                val phi = (1f + sqrt(5f)) / 2f
                for (i in 0 until count) {
                    val theta = 2f * PI.toFloat() * (i / phi)
                    val z = 1f - (2f * i + 1f) / count
                    val r = sqrt(max(0f, 1f - z * z))
                    outX[i] = r * cos(theta) * radius
                    outY[i] = r * sin(theta) * radius
                    outZ[i] = z * radius
                }
            }

            ShapePrimitive.EARTH -> {
                // Spherical globe with continental clusters and atmospheric halo
                val radius = (parameters["radius"]?.value ?: 1f) * 210f
                val phi = (1f + sqrt(5f)) / 2f
                for (i in 0 until count) {
                    val theta = 2f * PI.toFloat() * (i / phi)
                    val z = 1f - (2f * i + 1f) / count
                    val r = sqrt(max(0f, 1f - z * z))
                    // Harmonic continents
                    val lat = asin(z)
                    val lon = theta
                    val landNoise = sin(3f * lon) * cos(2f * lat) + sin(5f * lon + 2f) * 0.4f
                    val elevation = if (landNoise > 0.1f) 1.05f else 1.0f
                    val currentR = radius * elevation + (random.nextFloat() - 0.5f) * 4f
                    outX[i] = r * cos(theta) * currentR
                    outY[i] = r * sin(theta) * currentR
                    outZ[i] = z * currentR
                }
            }

            ShapePrimitive.CUBE -> {
                val s = (parameters["scale"]?.value ?: 1f) * 150f
                for (i in 0 until count) {
                    val face = i % 6
                    val u = (random.nextFloat() - 0.5f) * 2f * s
                    val v = (random.nextFloat() - 0.5f) * 2f * s
                    when (face) {
                        0 -> { outX[i] = s; outY[i] = u; outZ[i] = v }
                        1 -> { outX[i] = -s; outY[i] = u; outZ[i] = v }
                        2 -> { outX[i] = u; outY[i] = s; outZ[i] = v }
                        3 -> { outX[i] = u; outY[i] = -s; outZ[i] = v }
                        4 -> { outX[i] = u; outY[i] = v; outZ[i] = s }
                        else -> { outX[i] = u; outY[i] = v; outZ[i] = -s }
                    }
                }
            }

            ShapePrimitive.CYLINDER -> {
                val r = 140f
                val h = 320f
                for (i in 0 until count) {
                    val angle = (i.toFloat() / count) * 20f * PI.toFloat()
                    val y = (random.nextFloat() - 0.5f) * h
                    val jitter = (random.nextFloat() - 0.5f) * 8f
                    outX[i] = cos(angle) * r + jitter
                    outY[i] = y
                    outZ[i] = sin(angle) * r + jitter
                }
            }

            ShapePrimitive.CONE -> {
                val h = 340f
                for (i in 0 until count) {
                    val t = random.nextFloat()
                    val r = t * 180f
                    val angle = random.nextFloat() * 2f * PI.toFloat()
                    outX[i] = cos(angle) * r
                    outY[i] = (1f - t) * h - (h / 2f)
                    outZ[i] = sin(angle) * r
                }
            }

            ShapePrimitive.TORUS -> {
                val majorR = 190f
                val minorR = 65f
                for (i in 0 until count) {
                    val u = (i.toFloat() / count) * 32f * PI.toFloat()
                    val v = (i.toFloat() / count) * 2f * PI.toFloat()
                    outX[i] = (majorR + minorR * cos(u)) * cos(v)
                    outY[i] = (majorR + minorR * cos(u)) * sin(v)
                    outZ[i] = minorR * sin(u)
                }
            }

            ShapePrimitive.RINGS -> {
                val centerR = 180f
                for (i in 0 until count) {
                    val angle = (i.toFloat() / count) * 24f * PI.toFloat()
                    val dist = centerR + (random.nextFloat() - 0.5f) * 110f
                    outX[i] = cos(angle) * dist
                    outY[i] = (random.nextFloat() - 0.5f) * 12f
                    outZ[i] = sin(angle) * dist
                }
            }

            ShapePrimitive.SPIRAL, ShapePrimitive.GALAXY -> {
                val arms = 3
                for (i in 0 until count) {
                    val arm = i % arms
                    val dist = (i.toFloat() / count).pow(0.55f) * 360f
                    val angle = dist * 0.035f + arm * (2f * PI.toFloat() / arms)
                    val jitterR = (random.nextFloat() - 0.5f) * (dist * 0.22f + 14f)
                    val jitterY = (random.nextFloat() - 0.5f) * (35f - dist * 0.07f).coerceAtLeast(6f)
                    outX[i] = cos(angle) * (dist + jitterR)
                    outY[i] = jitterY
                    outZ[i] = sin(angle) * (dist + jitterR)
                }
            }

            ShapePrimitive.HELIX, ShapePrimitive.DNA -> {
                for (i in 0 until count) {
                    val t = (i.toFloat() / count) * 9f * PI.toFloat()
                    val y = (i.toFloat() / count - 0.5f) * 520f
                    val strand = i % 2
                    val angle = if (strand == 0) t else t + PI.toFloat()
                    val r = 115f
                    val jitter = (random.nextFloat() - 0.5f) * 10f

                    if (i % 7 == 0) {
                        val fraction = random.nextFloat() * 2f - 1f
                        outX[i] = cos(t) * r * fraction
                        outY[i] = y
                        outZ[i] = sin(t) * r * fraction
                    } else {
                        outX[i] = cos(angle) * r + jitter
                        outY[i] = y + jitter
                        outZ[i] = sin(angle) * r + jitter
                    }
                }
            }

            ShapePrimitive.WAVE -> {
                val freq = parameters["frequency"]?.value ?: 2f
                val amp = (parameters["amplitude"]?.value ?: 1f) * 80f
                val cols = 110
                val rows = count / cols
                for (i in 0 until count) {
                    val c = i % cols
                    val r = i / cols
                    val x = (c.toFloat() / cols - 0.5f) * 480f
                    val z = (r.toFloat() / rows - 0.5f) * 320f
                    val dist = sqrt(x * x + z * z) * 0.02f
                    val y = sin(dist * freq * 2.5f) * amp
                    outX[i] = x
                    outY[i] = y
                    outZ[i] = z
                }
            }

            ShapePrimitive.GRID, ShapePrimitive.PLANE -> {
                val side = sqrt(count.toFloat()).toInt().coerceAtLeast(1)
                val spacing = 420f / side
                for (i in 0 until count) {
                    val r = i / side
                    val c = i % side
                    outX[i] = (c - side / 2f) * spacing
                    outY[i] = (r - side / 2f) * spacing
                    outZ[i] = (random.nextFloat() - 0.5f) * 10f
                }
            }

            ShapePrimitive.ATOM -> {
                for (i in 0 until count) {
                    val ring = i % 4
                    val t = (i.toFloat() / count) * 12f * PI.toFloat()
                    val r = 240f
                    val jitter = (random.nextFloat() - 0.5f) * 10f
                    when (ring) {
                        0 -> {
                            val phi = random.nextFloat() * 2f * PI.toFloat()
                            val theta = random.nextFloat() * PI.toFloat()
                            val nr = random.nextFloat() * 45f
                            outX[i] = nr * sin(theta) * cos(phi)
                            outY[i] = nr * sin(theta) * sin(phi)
                            outZ[i] = nr * cos(theta)
                        }
                        1 -> {
                            outX[i] = cos(t) * r + jitter
                            outY[i] = sin(t) * r * 0.4f + jitter
                            outZ[i] = sin(t) * r * 0.9f + jitter
                        }
                        2 -> {
                            outX[i] = cos(t) * r * 0.4f + jitter
                            outY[i] = sin(t) * r + jitter
                            outZ[i] = cos(t) * r * 0.9f + jitter
                        }
                        else -> {
                            outX[i] = cos(t) * r * 0.8f + jitter
                            outY[i] = sin(t) * r * 0.8f + jitter
                            outZ[i] = -sin(t) * r * 0.4f + jitter
                        }
                    }
                }
            }

            ShapePrimitive.MOLECULE_H2O -> {
                // Water molecule: 1 Oxygen in center, 2 Hydrogen atoms at 104.5 degrees
                val bondLength = 160f
                val hAngle = 104.5f * (PI.toFloat() / 180f) / 2f

                for (i in 0 until count) {
                    when {
                        i < count / 2 -> {
                            // Central Oxygen atom (large sphere)
                            val phi = random.nextFloat() * 2f * PI.toFloat()
                            val theta = random.nextFloat() * PI.toFloat()
                            val r = random.nextFloat() * 65f
                            outX[i] = r * sin(theta) * cos(phi)
                            outY[i] = r * sin(theta) * sin(phi) + 40f
                            outZ[i] = r * cos(theta)
                        }
                        i < 3 * count / 4 -> {
                            // Hydrogen 1 (left at 52.25 deg)
                            val phi = random.nextFloat() * 2f * PI.toFloat()
                            val theta = random.nextFloat() * PI.toFloat()
                            val r = random.nextFloat() * 38f
                            val hx = -bondLength * sin(hAngle)
                            val hy = -bondLength * cos(hAngle) + 40f
                            outX[i] = hx + r * sin(theta) * cos(phi)
                            outY[i] = hy + r * sin(theta) * sin(phi)
                            outZ[i] = r * cos(theta)
                        }
                        else -> {
                            // Hydrogen 2 (right at 52.25 deg)
                            val phi = random.nextFloat() * 2f * PI.toFloat()
                            val theta = random.nextFloat() * PI.toFloat()
                            val r = random.nextFloat() * 38f
                            val hx = bondLength * sin(hAngle)
                            val hy = -bondLength * cos(hAngle) + 40f
                            outX[i] = hx + r * sin(theta) * cos(phi)
                            outY[i] = hy + r * sin(theta) * sin(phi)
                            outZ[i] = r * cos(theta)
                        }
                    }
                }
            }

            ShapePrimitive.PROJECTILE_MOTION -> {
                // Parabolic trajectory with projectile mass and velocity vectors
                for (i in 0 until count) {
                    val t = (i.toFloat() / count) * 2f - 1f // -1 to +1
                    val x = t * 240f
                    val y = -(1f - t * t) * 180f + 70f
                    val jitter = (random.nextFloat() - 0.5f) * 12f
                    outX[i] = x + jitter
                    outY[i] = y + jitter
                    outZ[i] = (random.nextFloat() - 0.5f) * 30f
                }
            }

            ShapePrimitive.PHOTOSYNTHESIS -> {
                // Sun (top left) -> Light photon stream -> Leaf (bottom right)
                for (i in 0 until count) {
                    if (i < count / 3) {
                        // Sun orb
                        val phi = random.nextFloat() * 2f * PI.toFloat()
                        val theta = random.nextFloat() * PI.toFloat()
                        val r = random.nextFloat() * 55f
                        outX[i] = -180f + r * sin(theta) * cos(phi)
                        outY[i] = -140f + r * sin(theta) * sin(phi)
                        outZ[i] = r * cos(theta)
                    } else if (i < 2 * count / 3) {
                        // Photon ray stream
                        val progress = random.nextFloat()
                        val sx = -180f + progress * 320f
                        val sy = -140f + progress * 240f
                        outX[i] = sx + (random.nextFloat() - 0.5f) * 25f
                        outY[i] = sy + (random.nextFloat() - 0.5f) * 25f
                        outZ[i] = (random.nextFloat() - 0.5f) * 35f
                    } else {
                        // Leaf silhouette
                        val u = (random.nextFloat() - 0.5f) * 2f
                        val lx = 140f + u * 120f
                        val ly = 100f + sin(u * PI.toFloat()) * 80f
                        outX[i] = lx + (random.nextFloat() - 0.5f) * 15f
                        outY[i] = ly + (random.nextFloat() - 0.5f) * 15f
                        outZ[i] = (random.nextFloat() - 0.5f) * 40f
                    }
                }
            }

            ShapePrimitive.HUMAN_SILHOUETTE, ShapePrimitive.ROBOT -> {
                for (i in 0 until count) {
                    val part = i % 5
                    val jitter = (random.nextFloat() - 0.5f) * 10f
                    when (part) {
                        0 -> { // Head
                            val angle = random.nextFloat() * 2f * PI.toFloat()
                            val r = random.nextFloat() * 32f
                            outX[i] = cos(angle) * r
                            outY[i] = -170f + sin(angle) * r
                            outZ[i] = (random.nextFloat() - 0.5f) * 30f
                        }
                        1 -> { // Torso
                            outX[i] = (random.nextFloat() - 0.5f) * 90f
                            outY[i] = -70f + (random.nextFloat() - 0.5f) * 120f
                            outZ[i] = (random.nextFloat() - 0.5f) * 40f
                        }
                        2, 3 -> { // Arms
                            val side = if (part == 2) -1f else 1f
                            outX[i] = side * (65f + random.nextFloat() * 45f)
                            outY[i] = -90f + random.nextFloat() * 140f
                            outZ[i] = jitter
                        }
                        else -> { // Legs
                            val side = if (i % 2 == 0) -1f else 1f
                            outX[i] = side * (25f + random.nextFloat() * 25f)
                            outY[i] = 40f + random.nextFloat() * 160f
                            outZ[i] = jitter
                        }
                    }
                }
            }

            ShapePrimitive.BUTTERFLY -> {
                for (i in 0 until count) {
                    val t = (i.toFloat() / count) * 24f * PI.toFloat()
                    val u = t % (2f * PI.toFloat())
                    val sinU = sin(u)
                    val cosU = cos(u)
                    val r = (exp(cosU) - 2f * cos(4f * u) - sin(u / 12f).pow(5)) * 65f
                    val side = if (i % 2 == 0) 1f else -1f
                    val wingDepth = sin(u * 2f) * 35f
                    val spread = (random.nextFloat() - 0.5f) * 16f
                    if (i < 800) {
                        val bodyY = (i.toFloat() / 800f - 0.5f) * 160f
                        outX[i] = (random.nextFloat() - 0.5f) * 8f
                        outY[i] = bodyY
                        outZ[i] = (random.nextFloat() - 0.5f) * 8f
                    } else {
                        outX[i] = (r * sinU * side) + spread
                        outY[i] = (-r * cosU) + spread
                        outZ[i] = wingDepth + spread
                    }
                }
            }

            ShapePrimitive.HEART -> {
                for (i in 0 until count) {
                    val t = (i.toFloat() / count) * 2f * PI.toFloat()
                    val sinT = sin(t)
                    val cosT = cos(t)
                    val hx = 16f * sinT.pow(3) * 14f
                    val hy = -(13f * cosT - 5f * cos(2f * t) - 2f * cos(3f * t) - cos(4f * t)) * 14f
                    val depth = (random.nextFloat() - 0.5f) * 70f
                    val jitter = (random.nextFloat() - 0.5f) * 20f
                    outX[i] = hx + jitter
                    outY[i] = hy + jitter
                    outZ[i] = depth
                }
            }

            ShapePrimitive.CAR -> {
                for (i in 0 until count) {
                    val f = i.toFloat() / count
                    when {
                        f < 0.35f -> {
                            outX[i] = (random.nextFloat() - 0.5f) * 360f
                            outY[i] = 40f + (random.nextFloat() - 0.5f) * 20f
                            outZ[i] = (random.nextFloat() - 0.5f) * 160f
                        }
                        f < 0.65f -> {
                            val rx = (random.nextFloat() - 0.5f) * 180f
                            outX[i] = rx - 20f
                            outY[i] = -20f + (random.nextFloat() - 0.5f) * 30f
                            outZ[i] = (random.nextFloat() - 0.5f) * 120f
                        }
                        else -> {
                            val wheel = i % 4
                            val wx = if (wheel < 2) -110f else 110f
                            val wz = if (wheel % 2 == 0) -85f else 85f
                            val angle = random.nextFloat() * 2f * PI.toFloat()
                            val wr = random.nextFloat() * 38f
                            outX[i] = wx + cos(angle) * wr
                            outY[i] = 60f + sin(angle) * wr
                            outZ[i] = wz + (random.nextFloat() - 0.5f) * 8f
                        }
                    }
                }
            }

            ShapePrimitive.DRAGON -> {
                for (i in 0 until count) {
                    val f = i.toFloat() / count
                    if (f < 0.3f) {
                        val spineT = f / 0.3f * 4f * PI.toFloat()
                        outX[i] = (f / 0.3f - 0.5f) * 350f
                        outY[i] = sin(spineT) * 40f
                        outZ[i] = cos(spineT) * 30f
                    } else {
                        val side = if (i % 2 == 0) 1f else -1f
                        val u = random.nextFloat() * PI.toFloat()
                        val v = random.nextFloat()
                        val wingSpan = 280f * v
                        outX[i] = cos(u) * 60f - 40f
                        outY[i] = -sin(u) * wingSpan * 0.5f - 20f
                        outZ[i] = side * (wingSpan + 30f)
                    }
                }
            }

            ShapePrimitive.EXPLOSION_FIELD -> {
                for (i in 0 until count) {
                    val dist = random.nextFloat() * 320f + 60f
                    val phi = random.nextFloat() * 2f * PI.toFloat()
                    val theta = random.nextFloat() * PI.toFloat()
                    outX[i] = dist * sin(theta) * cos(phi)
                    outY[i] = dist * sin(theta) * sin(phi)
                    outZ[i] = dist * cos(theta)
                }
            }

            ShapePrimitive.CUSTOM_CLOUD, ShapePrimitive.CUSTOM -> {
                for (i in 0 until count) {
                    val u = (i.toFloat() / count) * 4f * PI.toFloat()
                    val r = 160f + 40f * sin(u * 3f)
                    outX[i] = r * cos(u)
                    outY[i] = (i.toFloat() / count - 0.5f) * 280f
                    outZ[i] = r * sin(u)
                }
            }

            ShapePrimitive.SOLAR_SYSTEM -> {
                for (i in 0 until count) {
                    if (i < 1200) {
                        val phi = random.nextFloat() * 2f * PI.toFloat()
                        val theta = random.nextFloat() * PI.toFloat()
                        val r = random.nextFloat() * 45f
                        outX[i] = r * sin(theta) * cos(phi)
                        outY[i] = r * sin(theta) * sin(phi)
                        outZ[i] = r * cos(theta)
                    } else {
                        val orbitIdx = (i % 6) + 1
                        val orbitR = orbitIdx * 52f + 30f
                        val angle = (i.toFloat() / count) * 16f * PI.toFloat()
                        val jitter = (random.nextFloat() - 0.5f) * 6f
                        outX[i] = cos(angle) * orbitR + jitter
                        outY[i] = (random.nextFloat() - 0.5f) * 8f
                        outZ[i] = sin(angle) * orbitR + jitter
                    }
                }
            }

            ShapePrimitive.FLOWER -> {
                for (i in 0 until count) {
                    val theta = (i.toFloat() / count) * 12f * PI.toFloat()
                    val r = (120f + 60f * cos(5f * theta)) * (0.3f + 0.7f * random.nextFloat())
                    outX[i] = r * cos(theta)
                    outY[i] = r * sin(theta)
                    outZ[i] = (sin(r * 0.05f) * 45f) + (random.nextFloat() - 0.5f) * 10f
                }
            }

            ShapePrimitive.JET -> {
                for (i in 0 until count) {
                    val t = (i.toFloat() / count)
                    if (t < 0.4f) {
                        outX[i] = (random.nextFloat() - 0.5f) * 22f
                        outY[i] = (t / 0.4f - 0.5f) * 380f
                        outZ[i] = (random.nextFloat() - 0.5f) * 22f
                    } else {
                        val wingY = (random.nextFloat() - 0.5f) * 140f
                        val span = (1f - abs(wingY) / 140f) * 260f
                        val side = if (i % 2 == 0) 1f else -1f
                        outX[i] = side * span * random.nextFloat()
                        outY[i] = wingY
                        outZ[i] = (random.nextFloat() - 0.5f) * 10f
                    }
                }
            }

            ShapePrimitive.VOICE_LISTENING_WAVEFORM -> {
                // Triple concentric pulsating acoustic waveform rings with ripple waves
                for (i in 0 until count) {
                    val ringIdx = i % 3
                    val baseR = 120f + ringIdx * 50f
                    val angle = (i.toFloat() / count) * 8f * PI.toFloat()
                    val wave = sin(angle * (6f + ringIdx * 2f)) * (18f + ringIdx * 8f)
                    val r = baseR + wave + (random.nextFloat() - 0.5f) * 6f
                    outX[i] = r * cos(angle)
                    outY[i] = (sin(angle * 4f) * 20f) + (random.nextFloat() - 0.5f) * 8f
                    outZ[i] = r * sin(angle)
                }
            }

            ShapePrimitive.VOICE_THINKING_VORTEX -> {
                // Dual gyroscopic precession rings + glowing quantum thinking core
                for (i in 0 until count) {
                    if (i < 2500) {
                        // Dense quantum core
                        val phi = random.nextFloat() * 2f * PI.toFloat()
                        val theta = random.nextFloat() * PI.toFloat()
                        val r = random.nextFloat() * 45f
                        outX[i] = r * sin(theta) * cos(phi)
                        outY[i] = r * sin(theta) * sin(phi)
                        outZ[i] = r * cos(theta)
                    } else {
                        // Double helical orbital vortex
                        val t = (i.toFloat() / count) * 12f * PI.toFloat()
                        val r = 70f + (i.toFloat() / count) * 140f
                        val isRingA = (i % 2 == 0)
                        if (isRingA) {
                            outX[i] = r * cos(t)
                            outY[i] = sin(t * 2f) * 45f
                            outZ[i] = r * sin(t)
                        } else {
                            outX[i] = sin(t * 2f) * 45f
                            outY[i] = r * cos(t)
                            outZ[i] = r * sin(t)
                        }
                    }
                }
            }

            ShapePrimitive.VOICE_SPEAKING_PULSE -> {
                // Expanding sonic sphere with concentric acoustic latitude rings
                for (i in 0 until count) {
                    val u = random.nextFloat()
                    val theta = acos(2f * u - 1f)
                    val phi = random.nextFloat() * 2f * PI.toFloat()
                    val harmonic = 1f + 0.15f * sin(theta * 8f) * cos(phi * 6f)
                    val r = 160f * harmonic + (random.nextFloat() - 0.5f) * 8f
                    outX[i] = r * sin(theta) * cos(phi)
                    outY[i] = r * sin(theta) * sin(phi)
                    outZ[i] = r * cos(theta)
                }
            }

            ShapePrimitive.NONE -> {
                for (i in 0 until count) {
                    outX[i] = 0f
                    outY[i] = 0f
                    outZ[i] = 0f
                }
            }
        }
    }
}
