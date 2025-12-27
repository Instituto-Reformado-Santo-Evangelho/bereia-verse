package br.com.irse.verse.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
    // Aumentamos a contagem para preencher melhor áreas grandes
    val maxParticles = 150 

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

                val particlesToSpawn = if (particles.size < maxParticles) 4 else 2
                repeat(particlesToSpawn) {
                    if (particles.size < maxParticles) {
                        particles.add(FireParticle().apply { reset() })
                    } else {
                        val deadParticle = particles.firstOrNull { it.life <= 0f }
                        deadParticle?.reset()
                    }
                }

                particles.forEach { p ->
                    p.update(dt)
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val baseY = size.height * 0.9f 
        // Usamos a largura como referência de escala principal para manter proporção
        val scaleRef = size.width.coerceAtMost(size.height)

        // 3. Desenhar Glow de fundo (mais sutil)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    PrimaryAmber.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = Offset(centerX, baseY - (scaleRef * 0.1f)),
                radius = scaleRef * 0.5f
            ),
            radius = scaleRef * 0.5f,
            center = Offset(centerX, baseY - (scaleRef * 0.1f))
        )

        // 4. Desenhar partículas
        particles.forEach { p ->
            if (p.life > 0) {
                // Mapeia coordenadas normalizadas para pixels
                val drawX = centerX + (p.x * scaleRef)
                val drawY = baseY - (p.y * size.height) // Y escala com altura para subir na tela toda

                val color = getFireColor(p.life)
                // Tamanho base visual mais nítido
                val currentRadius = (p.size * scaleRef) * (0.6f + 0.4f * p.life)

                // Gradiente mais "duro" para definir melhor a forma
                val brush = Brush.radialGradient(
                    0.0f to color,
                    0.6f to color.copy(alpha = 0.6f),
                    1.0f to Color.Transparent,
                    center = Offset(drawX, drawY),
                    radius = currentRadius
                )

                drawCircle(
                    brush = brush,
                    radius = currentRadius,
                    center = Offset(drawX, drawY)
                )
            }
        }
    }
}

private class FireParticle {
    // Coordenadas Normalizadas (-0.5 a 0.5 para X, 0.0 a 1.0 para Y)
    var x: Float = 0f
    var y: Float = 0f
    var vx: Float = 0f
    var vy: Float = 0f
    var life: Float = 0f
    var decay: Float = 0f
    var size: Float = 0f
    var turbulenceOffset: Float = 0f

    fun reset() {
        // Spread horizontal reduzido para criar uma "coluna" de fogo mais definida na base
        val spread = 0.12f 
        x = (Random.nextFloat() - 0.5f) * spread * 1.5f + (Random.nextFloat() - 0.5f) * spread 
        
        y = Random.nextFloat() * 0.05f // Começa bem na base
        
        // Sobe ~40% a 70% da altura da tela por segundo
        vy = Random.nextFloat() * 0.3f + 0.4f 
        
        vx = (Random.nextFloat() - 0.5f) * 0.1f
        
        life = 1.0f
        decay = Random.nextFloat() * 0.5f + 0.4f // Vida mais curta para dinamismo
        
        // Tamanho relativo à largura (0.05 = 5% da largura)
        size = Random.nextFloat() * 0.06f + 0.04f 
        
        turbulenceOffset = Random.nextFloat() * 100f
    }

    fun update(dt: Float) {
        val noise = sin(y * 5f + turbulenceOffset) 
        // Turbulência aumenta com a altura
        x += (vx + noise * 0.1f * y) * dt
        y += vy * dt
        life -= decay * dt
    }
}

private fun getFireColor(life: Float): Color {
    return when {
        // Reduzimos drasticamente a área branca
        life > 0.92f -> Color(0xFFFFF8E1).copy(alpha = 0.9f) 
        life > 0.7f -> PrimaryAmber // Amarelo
        life > 0.5f -> Color(0xFFFF9800) // Laranja vibrante
        life > 0.3f -> Color(0xFFFF5722) // Laranja avermelhado
        else -> Color(0xFF3E2723).copy(alpha = life * 0.6f) // Fumaça escura
    }
}