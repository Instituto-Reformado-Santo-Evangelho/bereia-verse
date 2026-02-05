package br.com.irse.verse.ui.components
import br.com.irse.verse.ui.theme.VerseColors

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun FireAnimation(modifier: Modifier = Modifier) {
    val maxParticles = 400
    val maxHeatWaves = 10
    
    // Using plain arrays avoids StateObject overhead per frame. 
    // We trigger recomposition manually with frameTrigger.
    val particles = remember { Array(maxParticles) { FireParticle() } }
    val heatWaves = remember { Array(maxHeatWaves) { HeatWave() } }
    
    var frameTrigger by remember { mutableStateOf(0L) }
    
    LaunchedEffect(Unit) {
        var lastTime = 0L
        while (true) {
            withFrameMillis { frameTimeMillis ->
                if (lastTime == 0L) {
                    lastTime = frameTimeMillis
                    return@withFrameMillis
                }
                val dt = (frameTimeMillis - lastTime) / 1000f
                lastTime = frameTimeMillis
                
                // Force redraw
                frameTrigger = frameTimeMillis

                // Update particles
                for (i in particles.indices) {
                    val p = particles[i]
                    if (p.active) {
                        p.update(dt)
                    }
                }

                // Update heat waves
                var activeWaves = 0
                for (i in heatWaves.indices) {
                    val w = heatWaves[i]
                    if (w.active) {
                        w.update(dt)
                        activeWaves++
                    }
                }

                // Spawn particles
                val spawnRate = 25
                var spawned = 0
                for (i in particles.indices) {
                    if (spawned >= spawnRate) break
                    val p = particles[i]
                    if (!p.active) {
                        p.reset()
                        spawned++
                    }
                }
                
                // Spawn heat waves
                if (activeWaves < maxHeatWaves && Random.nextFloat() < 0.15f) {
                    for (i in heatWaves.indices) {
                        if (!heatWaves[i].active) {
                            heatWaves[i].reset()
                            break
                        }
                    }
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        // Read frameTrigger to subscribe to updates
        val currentFrame = frameTrigger
        
        val centerX = size.width / 2
        val baseY = size.height * 0.85f
        
        // Cache geometry
        val scaleRef = size.width.coerceAtMost(size.height)
        val fireScale = scaleRef * 0.35f
        val timeSecs = currentFrame / 1000f
        
        // --- DRAWING ---
        
        // Layer 1: Deep Base Glow
        val basePulse = (sin(timeSecs * 1.5f) + 1f) / 2f
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to Color(0xFFFF6F00).copy(alpha = 0.15f + basePulse * 0.08f),
                0.3f to Color(0xFFFF8F00).copy(alpha = 0.12f + basePulse * 0.05f),
                0.6f to Color(0xFFFFB74D).copy(alpha = 0.08f),
                1.0f to Color.Transparent
            ),
            radius = fireScale * 1.8f,
            center = Offset(centerX, baseY)
        )
        
        // Layer 2: Intense Warm Light
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to Color(0xFFFFD54F).copy(alpha = 0.25f),
                0.4f to Color(0xFFFFB74D).copy(alpha = 0.15f),
                0.7f to Color(0xFFFF8A65).copy(alpha = 0.08f),
                1.0f to Color.Transparent
            ),
            radius = fireScale * 1.2f,
            center = Offset(centerX, baseY)
        )
        
        // Layer 3: Bright Core
        val corePulse = (sin(timeSecs * 3f) + 1f) / 2f
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to Color(0xFFFFF59D).copy(alpha = 0.35f + corePulse * 0.15f),
                0.5f to Color(0xFFFFD54F).copy(alpha = 0.2f),
                1.0f to Color.Transparent
            ),
            radius = fireScale * 0.5f,
            center = Offset(centerX, baseY - fireScale * 0.1f)
        )

        // Heat Waves
        for (i in heatWaves.indices) {
            val wave = heatWaves[i]
            if (wave.active) {
                val waveY = baseY - wave.y * fireScale * 1.8f
                val waveAlpha = wave.life * 0.15f
                
                drawOval(
                    brush = Brush.radialGradient(
                        0.0f to Color(0xFFFFCC80).copy(alpha = waveAlpha),
                        0.6f to Color(0xFFFFB74D).copy(alpha = waveAlpha * 0.5f),
                        1.0f to Color.Transparent
                    ),
                    topLeft = Offset(centerX - wave.width * fireScale, waveY - fireScale * 0.08f),
                    size = Size(wave.width * fireScale * 2f, fireScale * 0.16f)
                )
            }
        }

        // Particles
        
        // 1. Core Flames
        for (i in particles.indices) {
            val p = particles[i]
            if (p.active && p.isCore) {
                drawFlameParticle(p, centerX, baseY, fireScale, true)
            }
        }
        
        // 2. Main Flames
        for (i in particles.indices) {
            val p = particles[i]
            if (p.active && !p.isEmber && !p.isCore) {
                drawFlameParticle(p, centerX, baseY, fireScale, false)
            }
        }
        
        // 3. Embers
        for (i in particles.indices) {
            val p = particles[i]
            if (p.active && p.isEmber) {
                drawEmberParticle(p, centerX, baseY, fireScale)
            }
        }
    }
}

private fun DrawScope.drawFlameParticle(
    p: FireParticle,
    centerX: Float,
    baseY: Float,
    fireScale: Float,
    isCore: Boolean
) {
    val drawX = centerX + (p.x * fireScale)
    val drawY = baseY - (p.y * fireScale)
    
    val lifeCurve = p.life * p.life
    val color = p.getColor()
    
    val baseSizeFactor = if (isCore) 1.3f else 1.0f
    val sizeFactor = lifeCurve * p.size * baseSizeFactor
    val baseSize = fireScale * sizeFactor
    
    val aspectRatio = if (isCore) 2.5f else 3.8f
    val width = baseSize * (1.2f + p.flicker * 0.3f)
    val height = baseSize * aspectRatio * (1.0f - p.flicker * 0.2f)
    
    val alpha = (lifeCurve * (0.7f + p.intensity * 0.3f)).coerceIn(0f, 1f)
    
    val gradient = if (isCore) {
        Brush.radialGradient(
            0.0f to Color.White.copy(alpha = alpha * 0.9f),
            0.2f to color.copy(alpha = alpha),
            0.5f to color.copy(alpha = alpha * 0.8f),
            0.75f to color.copy(alpha = alpha * 0.4f),
            1.0f to Color.Transparent,
            center = Offset(drawX, drawY),
            radius = height / 2
        )
    } else {
        Brush.radialGradient(
            0.0f to color.copy(alpha = alpha),
            0.4f to color.copy(alpha = alpha * 0.75f),
            0.65f to color.copy(alpha = alpha * 0.5f),
            0.85f to color.copy(alpha = alpha * 0.25f),
            1.0f to Color.Transparent,
            center = Offset(drawX, drawY),
            radius = height / 2
        )
    }
    
    drawOval(
        brush = gradient,
        topLeft = Offset(drawX - width / 2, drawY - height / 2),
        size = Size(width, height)
    )
}

private fun DrawScope.drawEmberParticle(
    p: FireParticle,
    centerX: Float,
    baseY: Float,
    fireScale: Float
) {
    val drawX = centerX + (p.x * fireScale)
    val drawY = baseY - (p.y * fireScale)
    
    val color = p.getEmberColor()
    val baseSize = fireScale * p.size * p.life
    
    drawCircle(
        brush = Brush.radialGradient(
            0.0f to Color.White.copy(alpha = p.life * 0.6f),
            0.3f to color.copy(alpha = p.life * 0.8f),
            0.7f to color.copy(alpha = p.life * 0.4f),
            1.0f to Color.Transparent
        ),
        radius = baseSize * 1.2f,
        center = Offset(drawX, drawY)
    )
    
    drawCircle(
        color = color.copy(alpha = p.life),
        radius = baseSize * 0.5f,
        center = Offset(drawX, drawY)
    )
}

private class HeatWave {
    var y: Float = 0f
    var width: Float = 0f
    var speed: Float = 0f
    var life: Float = 0f
    var decay: Float = 0f
    var active: Boolean = false
    
    fun reset() {
        y = 0f
        width = Random.nextFloat() * 0.3f + 0.4f
        speed = Random.nextFloat() * 0.4f + 0.3f
        life = 1.0f
        decay = Random.nextFloat() * 0.3f + 0.3f
        active = true
    }
    
    fun update(dt: Float) {
        y += speed * dt
        life -= decay * dt
        width += dt * 0.1f
        if (life <= 0) active = false
    }
}

private class FireParticle {
    var x: Float = 0f
    var y: Float = 0f
    var vx: Float = 0f
    var vy: Float = 0f
    var life: Float = 0f
    var decay: Float = 0f
    var size: Float = 0f
    var temperature: Float = 1.0f
    var turbulenceOffset: Float = 0f
    var isEmber: Boolean = false
    var isCore: Boolean = false
    var intensity: Float = 1.0f
    var flicker: Float = 0f
    var flickerSpeed: Float = 0f
    var active: Boolean = false

    fun reset() {
        val rand = Random.nextFloat()
        
        when {
            rand < 0.15f -> {
                // Embers (15%)
                isEmber = true
                isCore = false
                
                val spread = 0.25f
                x = (Random.nextFloat() - 0.5f) * spread
                y = Random.nextFloat() * 0.1f
                
                vy = Random.nextFloat() * 1.2f + 0.6f
                vx = (Random.nextFloat() - 0.5f) * 0.4f
                
                size = Random.nextFloat() * 0.018f + 0.01f
                decay = Random.nextFloat() * 0.9f + 0.5f
                temperature = Random.nextFloat() * 0.3f + 0.7f
            }
            rand < 0.35f -> {
                // Core (20%)
                isEmber = false
                isCore = true
                
                val spread = 0.18f
                x = (Random.nextFloat() - 0.5f) * spread
                y = 0f
                
                vy = Random.nextFloat() * 0.5f + 0.4f
                vx = (Random.nextFloat() - 0.5f) * 0.03f
                
                size = Random.nextFloat() * 0.07f + 0.06f
                decay = Random.nextFloat() * 0.35f + 0.25f
                temperature = Random.nextFloat() * 0.15f + 0.85f
                intensity = Random.nextFloat() * 0.3f + 0.7f
            }
            else -> {
                // Main Flames (65%)
                isEmber = false
                isCore = false
                
                val spread = 0.3f
                x = (Random.nextFloat() - 0.5f) * spread
                y = Random.nextFloat() * 0.05f
                
                vy = Random.nextFloat() * 0.7f + 0.35f
                vx = (Random.nextFloat() - 0.5f) * 0.08f
                
                size = Random.nextFloat() * 0.055f + 0.04f
                decay = Random.nextFloat() * 0.45f + 0.3f
                temperature = Random.nextFloat() * 0.5f + 0.5f
                intensity = Random.nextFloat() * 0.4f + 0.6f
            }
        }
        
        life = 1.0f
        turbulenceOffset = Random.nextFloat() * 100f
        flickerSpeed = Random.nextFloat() * 15f + 10f
        flicker = 0f
        active = true
    }

    fun update(dt: Float) {
        if (isEmber) {
            val turbulence = sin(y * 20f + turbulenceOffset) * 0.12f
            x += (vx + turbulence) * dt
            y += vy * dt
            
            vx *= (1f - dt * 0.4f)
            vy *= (1f - dt * 0.3f)
        } else {
            val heightFactor = (y * 2.5f).coerceAtMost(1f)
            val turbulenceStrength = if (isCore) 0.08f else 0.15f
            
            val turb1 = sin(y * 8f + turbulenceOffset) * turbulenceStrength
            val turb2 = sin(y * 15f + turbulenceOffset * 0.7f) * turbulenceStrength * 0.5f
            val turbulence = (turb1 + turb2) * heightFactor
            
            x += (vx + turbulence) * dt
            y += vy * dt
            
            vy += dt * 0.15f
            
            flicker = (sin(life * flickerSpeed) + 1f) / 2f
        }
        
        life -= decay * dt
        temperature *= (1f - dt * 0.2f)
        if (life <= 0) active = false
    }

    fun getColor(): Color {
        val temp = temperature * life
        return when {
            temp > 0.9f -> Color(0xFFFFFDE7)
            temp > 0.8f -> Color(0xFFFFF9C4)
            temp > 0.7f -> Color(0xFFFFEB3B)
            temp > 0.6f -> Color(0xFFFFC107)
            temp > 0.5f -> Color(0xFFFFB300)
            temp > 0.4f -> Color(0xFFFF8F00)
            temp > 0.3f -> Color(0xFFFF6F00)
            temp > 0.2f -> Color(0xFFE64A19)
            temp > 0.1f -> Color(0xFFD84315)
            else -> Color(0xFF5D4037).copy(alpha = life * 0.6f)
        }
    }

    fun getEmberColor(): Color {
        val temp = temperature * life
        return when {
            temp > 0.6f -> Color(0xFFFFEB3B)
            temp > 0.4f -> Color(0xFFFF9800)
            temp > 0.2f -> Color(0xFFFF5722)
            else -> Color(0xFF424242)
        }
    }
}
