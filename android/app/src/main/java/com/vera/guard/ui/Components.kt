package com.vera.guard.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vera.guard.service.SensorStatus
import kotlinx.coroutines.launch

/** Material "navigation" arrow (not in the core icon set). */
val NavigationIcon: ImageVector by lazy {
    ImageVector.Builder("navigation", 24.dp, 24.dp, 24f, 24f).path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 2f); lineTo(4.5f, 20.29f); lineToRelative(0.71f, 0.71f); lineTo(12f, 18f); lineToRelative(6.79f, 3f); lineToRelative(0.71f, -0.71f); close()
    }.build()
}

@Composable
fun StatusPill(text: String, color: Color, pulsing: Boolean = false) {
    val t = rememberInfiniteTransition(label = "pill")
    val a by t.animateFloat(1f, if (pulsing) 0.25f else 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "dot")
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.16f)).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color.copy(alpha = a)))
        Text(text, Modifier.padding(start = 8.dp), color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
    }
}

/** Big SOS button: pulsing rings, and it only fires after being held for [holdMs] (progress ring) so it cannot be triggered by accident. */
@Composable
fun SosButton(label: String, holdLabel: String, onActivated: () -> Unit, holdMs: Int = 2000) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var progress by remember { mutableFloatStateOf(0f) }
    val fire = rememberUpdatedState(onActivated)
    val t = rememberInfiniteTransition(label = "sos")
    val ring by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "ring")
    val ring2 by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, delayMillis = 1100, easing = LinearEasing)), label = "ring2")
    Box(
        Modifier.fillMaxWidth().height(250.dp)
            .semantics { contentDescription = label; onLongClick(label = holdLabel) { fire.value(); true } },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(250.dp)) {
            val c = center; val base = size.minDimension * 0.34f
            for (p in listOf(ring, ring2)) drawCircle(VeraRed.copy(alpha = (1f - p) * 0.35f), radius = base + p * (size.minDimension * 0.16f), center = c, style = Stroke(width = 3f))
        }
        Box(
            Modifier.size(172.dp).clip(CircleShape).background(SosBrush)
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        val job = scope.launch {
                            animate(0f, 1f, animationSpec = tween(holdMs, easing = LinearEasing)) { v, _ -> progress = v }
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            fire.value()
                            progress = 0f
                        }
                        tryAwaitRelease()
                        job.cancel(); progress = 0f
                    })
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(172.dp)) {
                if (progress > 0f) drawArc(Color.White, -90f, 360f * progress, false, topLeft = Offset(8f, 8f), size = Size(size.width - 16f, size.height - 16f), style = Stroke(width = 10f, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, color = Color.White, fontSize = 46.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Text(if (progress > 0f) "…" else holdLabel, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 20.dp))
            }
        }
    }
}

@Composable
fun ActionTile(icon: ImageVector, label: String, tint: Color, modifier: Modifier = Modifier, active: Boolean = false, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp))
            .background(if (active) tint.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick).padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(tint.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        }
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 15.sp)
    }
}

/** Live motion-sensor monitor: g-force sparkline, level bubble, rotation, and the detector state. */
@Composable
fun SensorMonitor(
    st: SensorStatus, live: Boolean, history: List<Float>,
    forceLabel: String, rotationLabel: String, stateLabel: String, stateName: String,
) {
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outline
    val bubbleBg = MaterialTheme.colorScheme.surfaceVariant
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.weight(1f).height(84.dp).clip(RoundedCornerShape(14.dp)).background(bubbleBg)) {
            val w = size.width; val h = size.height; val maxG = 4f
            fun y(g: Float) = h - (g.coerceIn(0f, maxG) / maxG) * h
            drawLine(grid, Offset(0f, y(1f)), Offset(w, y(1f)), strokeWidth = 2f)
            if (history.size > 1) {
                val step = w / 79f
                val path = Path()
                history.forEachIndexed { i, g -> val x = w - (history.size - 1 - i) * step; if (i == 0) path.moveTo(x, y(g)) else path.lineTo(x, y(g)) }
                drawPath(path, if (live) line else grid, style = Stroke(width = 4f, cap = StrokeCap.Round))
            }
        }
        Canvas(Modifier.size(84.dp)) {
            val r = size.minDimension / 2f
            drawCircle(bubbleBg, r)
            drawCircle(grid, r, style = Stroke(width = 3f))
            drawLine(grid, Offset(r, 6f), Offset(r, size.height - 6f), 2f); drawLine(grid, Offset(6f, r), Offset(size.width - 6f, r), 2f)
            val bx = r + (st.ax / 1f).coerceIn(-1f, 1f) * (r - 14f)
            val by = r - (st.ay / 1f).coerceIn(-1f, 1f) * (r - 14f)
            drawCircle(if (live) VeraGreen else grid, 11f, Offset(bx, by))
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Stat(forceLabel, "%.2f g".format(st.g))
        Stat(rotationLabel, "%.0f °/s".format(st.gyroDps))
        Stat(stateLabel, stateName)
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
