package dev.flondy.mynotes.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.flondy.mynotes.ui.theme.LocalAppExtras
import kotlin.math.sin
import kotlin.random.Random

/**
 * Konfetti (2.6): kurzer Regen bunter Papierschnipsel, wenn eine Checkliste komplett erledigt ist.
 * Läuft einmal (ca. 1,8 s) pro [trigger]-Wert > 0; mit „Animationen aus“ erscheint nur kurz der Text.
 */
@Composable
fun ConfettiBurst(trigger: Int, message: String, modifier: Modifier = Modifier) {
    if (trigger <= 0) return
    val extras = LocalAppExtras.current
    val t = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) { t.snapTo(0f); t.animateTo(1f, tween(if (extras.animationsEnabled) 1800 else 1200, easing = LinearEasing)) }
    if (t.value >= 1f) return
    val colors = listOf(extras.accent, extras.leaf, Color(0xFFE85D75), Color(0xFF5B8DEF), Color(0xFFF2A541), extras.leather)
    val pieces = remember(trigger) {
        val r = Random(trigger)
        List(70) { Triple(r.nextFloat(), r.nextFloat() * 0.35f, r.nextFloat()) }
    }
    Box(modifier.fillMaxSize().testTag("confetti"), contentAlignment = Alignment.Center) {
        if (extras.animationsEnabled) {
            Canvas(Modifier.fillMaxSize()) {
                val p = t.value
                pieces.forEachIndexed { i, (x0, delay, spin) ->
                    val local = ((p - delay) / (1f - delay)).coerceIn(0f, 1f)
                    if (local <= 0f) return@forEachIndexed
                    val x = size.width * (x0 + 0.05f * sin((local * 6f + i) * 1.3f))
                    val y = -20f + (size.height + 40f) * local * local
                    rotate(degrees = 720f * spin * local, pivot = Offset(x, y)) {
                        drawRect(colors[i % colors.size].copy(alpha = 1f - local * 0.6f), Offset(x - 5f, y - 8f), Size(10f, 16f))
                    }
                }
            }
        }
        val a = when { t.value < 0.15f -> t.value / 0.15f; t.value > 0.8f -> (1f - t.value) / 0.2f; else -> 1f }
        Surface(shape = RoundedCornerShape(20.dp), color = extras.accent, shadowElevation = 8.dp,
            modifier = Modifier.graphicsLayer { alpha = a; scaleX = 0.9f + 0.1f * a; scaleY = scaleX }) {
            Text(message, color = extras.onAccent, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
        }
    }
}
