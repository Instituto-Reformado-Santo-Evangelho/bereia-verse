package br.com.irse.verse.ui.components

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
import androidx.compose.ui.platform.LocalDensity
import br.com.irse.verse.PrimaryAmber
import kotlinx.coroutines.isActive
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun FireAnimation(modifier: Modifier = Modifier) {
    val particles = remember { mutableStateListOf<FireParticle>() }
    // Estado para forçar recomposição a cada frame
    var frameTrigger by remember { mutableStateOf(0L) }
    
    // Aumentamos a contagem para uma densidade muito maior
    val maxParticles = 250 

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

                particles.forEach { p -> p.update(dt) }

                val spawnRate = 10 // Fluxo constante de partículas
                repeat(spawnRate) {
                    val deadParticle = particles.firstOrNull { it.life <= 0f }
                    if (deadParticle != null) {
                        deadParticle.reset()
                    } else if (particles.size < maxParticles) {
                        particles.add(FireParticle().apply { reset() })
                    }
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val currentFrame = frameTrigger 
        val centerX = size.width / 2
        // Base deslocada para baixo (fora da tela) para esconder o nascimento das partículas
        val baseY = size.height * 1.02f 
        val scaleRef = size.width.coerceAtMost(size.height)
        
        // Efeito de pulsação da luz (Glow dinâmico)
        // Usa o tempo para oscilar a intensidade e o tamanho levemente
        val timeSecs = currentFrame / 1000f
        val pulse = (sin(timeSecs * 2f) + 1f) / 2f // 0.0 a 1.0
        val glowAlpha = 0.1f + (pulse * 0.08f) // Oscila entre 0.10 e 0.18
        val glowRadiusMult = 1.0f + (pulse * 0.05f) // Oscila tamanho levemente

        // Glow de fundo (Luz ambiente)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    PrimaryAmber.copy(alpha = glowAlpha),
                    PrimaryAmber.copy(alpha = glowAlpha * 0.5f),
                    Color.Transparent
                ),
                center = Offset(centerX, baseY - (scaleRef * 0.1f)),
                radius = scaleRef * 0.7f * glowRadiusMult
            ),
            radius = scaleRef * 0.7f * glowRadiusMult,
            center = Offset(centerX, baseY - (scaleRef * 0.1f))
        )

        particles.forEach { p ->
            if (p.life > 0) {
                val drawX = centerX + (p.x * scaleRef)
                val drawY = baseY - (p.y * size.height)

                val color = getFireColor(p.life, p.isEmber)
                
                val sizeFactor = if (p.isEmber) p.life else (p.life * p.life * p.life)
                val baseSize = p.size * scaleRef * sizeFactor

                if (p.isEmber) {
                    drawCircle(
                        color = color.copy(alpha = p.life),
                        radius = baseSize * 0.8f, // Fagulhas um pouco mais visíveis
                        center = Offset(drawX, drawY)
                    )
                } else {
                    val width = baseSize * 1.6f
                    val height = baseSize * 4.0f 

                    val brush = Brush.radialGradient(
                        0.0f to color,
                        0.7f to color.copy(alpha = 0.5f), // Núcleo mais sólido
                        1.0f to Color.Transparent,
                        center = Offset(drawX, drawY),
                        radius = height / 2
                    )

                    drawOval(
                        brush = brush,
                        topLeft = Offset(drawX - width / 2, drawY - height / 2),
                        size = Size(width, height)
                    )
                }
            }
        }
    }
}
// ... (Particle class remains mostly the same, ensuring imports are kept)

private class FireParticle {
    var x: Float = 0f
    var y: Float = 0f
    var vx: Float = 0f
    var vy: Float = 0f
    var life: Float = 0f
    var decay: Float = 0f
    var size: Float = 0f
    var turbulenceOffset: Float = 0f
    var isEmber: Boolean = false

    fun reset() {
        isEmber = Random.nextFloat() < 0.25f 

        if (isEmber) {
            val spread = 0.18f
            x = (Random.nextFloat() - 0.5f) * spread * 2f
            y = Random.nextFloat() * 0.15f
            vy = Random.nextFloat() * 0.7f + 0.5f 
            vx = (Random.nextFloat() - 0.5f) * 0.25f 
            size = Random.nextFloat() * 0.012f + 0.008f 
            decay = Random.nextFloat() * 0.7f + 0.4f
        } else {
            // Base mais larga
            val spread = 0.14f 
            x = (Random.nextFloat() - 0.5f) * spread * 2.0f
            y = 0f
            vy = Random.nextFloat() * 0.3f + 0.3f
            // Velocidade lateral inicial reduzida para estabilidade na base
            vx = (Random.nextFloat() - 0.5f) * 0.02f 
            size = Random.nextFloat() * 0.04f + 0.05f 
            decay = Random.nextFloat() * 0.4f + 0.3f
        }
        
        life = 1.0f
        turbulenceOffset = Random.nextFloat() * 100f
    }

    fun update(dt: Float) {
        val turbulenceScale = if (isEmber) 12f else 4f
        val noise = sin(y * turbulenceScale + turbulenceOffset) 
        
        // A turbulência aumenta com a altura (y). Base estável, pontas agitadas.
        val instability = if (isEmber) 1f else (y * 5f).coerceAtMost(1f)

        x += (vx + noise * 0.06f * instability) * dt
        y += vy * dt
        life -= decay * dt
    }
}

private fun getFireColor(life: Float, isEmber: Boolean): Color {
    if (isEmber) {
        return when {
            life > 0.4f -> Color(0xFFFFEB3B) 
            life > 0.15f -> Color(0xFFFF5722) 
            else -> Color(0xFF424242).copy(alpha = life)
        }
    }

    return when {
        // Branco apenas no pico extremo de calor (spawn inicial)
        life > 0.98f -> Color(0xFFFFFDE7) 
        life > 0.82f -> PrimaryAmber 
        life > 0.50f -> Color(0xFFFF9800) 
        life > 0.20f -> Color(0xFFFF5722) 
        else -> Color(0xFF5D4037).copy(alpha = life * 0.6f) 
    }
}