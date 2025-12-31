package br.com.irse.verse.ui.components
import br.com.irse.verse.ui.theme.VerseColors

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity

import kotlinx.coroutines.isActive
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.pow
import kotlin.math.PI
import kotlin.random.Random

@Composable
fun FireAnimation(modifier: Modifier = Modifier) {
    val particles = remember { mutableStateListOf<FireParticle>() }
    val heatWaves = remember { mutableStateListOf<HeatWave>() }
    var frameTrigger by remember { mutableStateOf(0L) }
    
    val maxParticles = 400
    val maxHeatWaves = 6

    LaunchedEffect(Unit) {
        var lastTime = 0L
        while (isActive) {
            withFrameMillis { frameTimeMillis ->
                if (lastTime == 0L) {
                    lastTime = frameTimeMillis
                    return@withFrameMillis
                }
                val dt = (frameTimeMillis - lastTime) / 1000f
                lastTime = frameTimeMillis
                
                frameTrigger = frameTimeMillis

                // Atualizar partículas
                particles.forEach { p -> p.update(dt) }

                // Atualizar ondas de calor
                heatWaves.forEach { w -> w.update(dt) }
                heatWaves.removeAll { it.life <= 0f }

                // Spawn partículas (taxa alta para densidade)
                val spawnRate = 25
                repeat(spawnRate) {
                    val deadParticle = particles.firstOrNull { it.life <= 0f }
                    if (deadParticle != null) {
                        deadParticle.reset()
                    } else if (particles.size < maxParticles) {
                        particles.add(FireParticle().apply { reset() })
                    }
                }
                
                // Spawn ondas de calor ocasionalmente
                if (Random.nextFloat() < 0.15f && heatWaves.size < maxHeatWaves) {
                    heatWaves.add(HeatWave())
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val currentFrame = frameTrigger
        val centerX = size.width / 2
        val baseY = size.height * 0.85f
        val scaleRef = size.width.coerceAtMost(size.height)
        val fireScale = scaleRef * 0.35f
        
        val timeSecs = currentFrame / 1000f
        
        // Camada 1: Brilho base profundo
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
        
        // Camada 2: Luz quente intensa
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
        
        // Camada 3: Núcleo brilhante
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

        // Desenhar ondas de calor (efeito de distorção)
        heatWaves.forEach { wave ->
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

        // Separar partículas por tipo para ordem de desenho
        val embers = particles.filter { it.isEmber && it.life > 0 }
        val mainFlames = particles.filter { !it.isEmber && !it.isCore && it.life > 0 }
        val coreFlames = particles.filter { it.isCore && it.life > 0 }

        // Desenhar partículas do núcleo primeiro (mais brilhantes)
        coreFlames.forEach { p ->
            drawFlameParticle(p, centerX, baseY, fireScale, true)
        }
        
        // Desenhar chamas principais
        mainFlames.forEach { p ->
            drawFlameParticle(p, centerX, baseY, fireScale, false)
        }
        
        // Desenhar fagulhas por último (sobreposição)
        embers.forEach { p ->
            drawEmber(p, centerX, baseY, fireScale)
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
    val color = getFireColor(p.temperature, p.life)
    
    val baseSizeFactor = if (isCore) 1.3f else 1.0f
    val sizeFactor = lifeCurve * p.size * baseSizeFactor
    val baseSize = fireScale * sizeFactor
    
    // Partículas do núcleo são mais redondas, externas mais alongadas
    val aspectRatio = if (isCore) 2.5f else 3.8f
    val width = baseSize * (1.2f + p.flicker * 0.3f)
    val height = baseSize * aspectRatio * (1.0f - p.flicker * 0.2f)
    
    // Gradiente multi-camadas para realismo
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

private fun DrawScope.drawEmber(
    p: FireParticle,
    centerX: Float,
    baseY: Float,
    fireScale: Float
) {
    val drawX = centerX + (p.x * fireScale)
    val drawY = baseY - (p.y * fireScale)
    
    val color = getEmberColor(p.life, p.temperature)
    val baseSize = fireScale * p.size * p.life
    
    // Brilho interno
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
    
    // Núcleo sólido
    drawCircle(
        color = color.copy(alpha = p.life),
        radius = baseSize * 0.5f,
        center = Offset(drawX, drawY)
    )
}

private class HeatWave {
    var y: Float = 0f
    var width: Float = Random.nextFloat() * 0.3f + 0.4f
    var speed: Float = Random.nextFloat() * 0.4f + 0.3f
    var life: Float = 1.0f
    var decay: Float = Random.nextFloat() * 0.3f + 0.3f
    
    fun update(dt: Float) {
        y += speed * dt
        life -= decay * dt
        width += dt * 0.1f
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

    fun reset() {
        val rand = Random.nextFloat()
        
        when {
            rand < 0.15f -> {
                // Fagulhas (15%)
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
                // Núcleo quente (20%)
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
                // Chamas principais (65%)
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
    }

    fun update(dt: Float) {
        if (isEmber) {
            // Fagulhas: movimento mais errático
            val turbulence = sin(y * 20f + turbulenceOffset) * 0.12f
            x += (vx + turbulence) * dt
            y += vy * dt
            
            // Desaceleração por resistência do ar
            vx *= (1f - dt * 0.4f)
            vy *= (1f - dt * 0.3f)
        } else {
            // Chamas: turbulência baseada em altura
            val heightFactor = (y * 2.5f).coerceAtMost(1f)
            val turbulenceStrength = if (isCore) 0.08f else 0.15f
            
            // Múltiplas frequências de turbulência
            val turb1 = sin(y * 8f + turbulenceOffset) * turbulenceStrength
            val turb2 = sin(y * 15f + turbulenceOffset * 0.7f) * turbulenceStrength * 0.5f
            val turbulence = (turb1 + turb2) * heightFactor
            
            x += (vx + turbulence) * dt
            y += vy * dt
            
            // Aceleração sutil para cima (efeito de convecção)
            vy += dt * 0.15f
            
            // Flicker (tremulação)
            flicker = (sin(life * flickerSpeed) + 1f) / 2f
        }
        
        life -= decay * dt
        
        // Temperatura diminui com o tempo
        temperature *= (1f - dt * 0.2f)
    }
}

private fun getFireColor(temperature: Float, life: Float): Color {
    val temp = temperature * life
    return when {
        temp > 0.9f -> Color(0xFFFFFDE7)      // Branco quente
        temp > 0.8f -> Color(0xFFFFF9C4)      // Amarelo muito claro
        temp > 0.7f -> Color(0xFFFFEB3B)      // Amarelo brilhante
        temp > 0.6f -> Color(0xFFFFC107)      // Amarelo âmbar
        temp > 0.5f -> Color(0xFFFFB300)      // Laranja claro
        temp > 0.4f -> Color(0xFFFF8F00)      // Laranja
        temp > 0.3f -> Color(0xFFFF6F00)      // Laranja escuro
        temp > 0.2f -> Color(0xFFE64A19)      // Vermelho laranja
        temp > 0.1f -> Color(0xFFD84315)      // Vermelho
        else -> Color(0xFF5D4037).copy(alpha = life * 0.6f)  // Marrom escuro (fumaça)
    }
}

private fun getEmberColor(life: Float, temperature: Float): Color {
    val temp = temperature * life
    return when {
        temp > 0.6f -> Color(0xFFFFEB3B)      // Amarelo brilhante
        temp > 0.4f -> Color(0xFFFF9800)      // Laranja
        temp > 0.2f -> Color(0xFFFF5722)      // Vermelho laranja
        else -> Color(0xFF424242)              // Cinza escuro
    }
}