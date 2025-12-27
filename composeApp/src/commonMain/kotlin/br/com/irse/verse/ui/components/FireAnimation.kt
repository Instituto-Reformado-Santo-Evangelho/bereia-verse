package br.com.irse.verse.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import br.com.irse.verse.PrimaryAmber
import kotlinx.coroutines.isActive
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun FireAnimation(modifier: Modifier = Modifier) {
    // Estado das partículas
    val particles = remember { mutableStateListOf<FireParticle>() }
    val maxParticles = 50
    
    // Loop de animação real e infinito
    LaunchedEffect(Unit) {
        val startTime = withFrameMillis { it }
        while (isActive) {
            withFrameMillis { frameTime ->
                val elapsed = (frameTime - startTime) / 1000f
                
                // 1. Adicionar novas chamas
                if (particles.size < maxParticles) {
                    repeat(2) {
                        particles.add(FireParticle().apply { reset(elapsed) })
                    }
                }

                // 2. Atualizar chamas existentes
                val iterator = particles.iterator()
                while (iterator.hasNext()) {
                    val p = iterator.next()
                    p.update(elapsed)
                    if (p.life <= 0f) {
                        p.reset(elapsed) // Reutiliza a partícula para ser infinito
                    }
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val baseY = size.height * 0.8f

        particles.forEach { p ->
            val scale = p.size * p.life
            if (scale > 0) {
                val drawX = centerX + p.currentX
                val drawY = baseY + p.currentY

                val color = when {
                    p.life > 0.8f -> Color.White
                    p.life > 0.5f -> PrimaryAmber
                    p.life > 0.3f -> Color(0xFFFF9800)
                    else -> Color(0xFFFF5722).copy(alpha = p.life)
                }

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = p.life * 0.8f), Color.Transparent),
                        center = Offset(drawX, drawY),
                        radius = scale
                    ),
                    radius = scale,
                    center = Offset(drawX, drawY)
                )
            }
        }
    }
}

private class FireParticle {
    var baseX: Float = 0f
    var currentX: Float = 0f
    var currentY: Float = 0f
    var size: Float = 0f
    var speed: Float = 0f
    var life: Float = 0f
    var decay: Float = 0f
    var freq: Float = 0f
    var amplitude: Float = 0f

    fun reset(elapsed: Float) {
        baseX = Random.nextFloat() * 100f - 50f 
        currentY = 0f 
        size = Random.nextFloat() * 40f + 30f
        speed = Random.nextFloat() * 3.0f + 2.0f
        life = 1.0f
        decay = Random.nextFloat() * 0.015f + 0.01f
        freq = Random.nextFloat() * 4f + 1f
        amplitude = Random.nextFloat() * 20f + 10f
    }

    fun update(elapsed: Float) {
        currentY -= speed
        // Movimento lateral orgânico (senoidal)
        currentX = baseX + (sin(currentY * 0.05f + elapsed * freq) * amplitude)
        life -= decay
    }
}