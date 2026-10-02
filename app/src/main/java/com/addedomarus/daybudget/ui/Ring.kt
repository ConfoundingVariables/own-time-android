package com.addedomarus.daybudget.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.addedomarus.daybudget.core.PriorityId
import com.addedomarus.daybudget.core.Row
import com.addedomarus.daybudget.core.ring
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private const val GAP_DEGREES = 1.2f

/**
 * The day's total budget as a circle: remaining parts bright, spent parts sunk into [surface]. Tapping or
 * long-pressing a part reports its priority; [centre] draws inside the ring.
 */
@Composable
fun Ring(
    rows: List<Row>,
    surface: Color,
    track: Color,
    indicator: Color,
    onTap: (PriorityId) -> Unit,
    onLongPress: (PriorityId) -> Unit,
    modifier: Modifier = Modifier,
    centre: @Composable BoxScope.() -> Unit,
) {
    val arcs = ring(rows)
    val colours = rows.associate { it.priority.id to (it.priority.color.color() to it.priority.color.spent(surface)) }
    val running = rows.firstOrNull { it.running }?.priority?.id
    val runningArc = arcs.firstOrNull { it.id == running && !it.spent }
    val indicatorEnd by animateFloatAsState((runningArc?.let { it.start + it.sweep } ?: 0f), label = "indicator")

    fun hit(offset: Offset, size: IntSize): PriorityId? {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = minOf(size.width, size.height) / 2f
        if (hypot(offset.x - c.x, offset.y - c.y) < r * 0.55f) return null
        val turn = ((atan2(offset.y - c.y, offset.x - c.x) * 180 / PI + 90 + 360) % 360 / 360).toFloat()
        return arcs.firstOrNull { turn >= it.start && turn < it.start + it.sweep }?.id
    }

    Box(modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier.fillMaxSize().pointerInput(arcs) {
                detectTapGestures(
                    onTap = { o -> hit(o, size)?.let(onTap) },
                    onLongPress = { o -> hit(o, size)?.let(onLongPress) },
                )
            },
        ) {
            val stroke = size.minDimension * 0.11f
            val radius = size.minDimension / 2 - stroke / 2
            val topLeft = Offset(center.x - radius, center.y - radius)
            val box = Size(radius * 2, radius * 2)
            drawCircle(track, radius, style = Stroke(stroke))
            for (arc in arcs) {
                val (bright, sunk) = colours.getValue(arc.id)
                val gap = if (arcs.size > 1) GAP_DEGREES else 0f
                drawArc(
                    color = if (arc.spent) sunk else bright,
                    startAngle = -90f + arc.start * 360f + gap / 2,
                    sweepAngle = (arc.sweep * 360f - gap).coerceAtLeast(0.5f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = box,
                    style = Stroke(stroke),
                )
            }
            if (runningArc != null) {
                val inner = radius - stroke / 2 - 8.dp.toPx()
                val start = runningArc.start
                drawArc(
                    color = indicator,
                    startAngle = -90f + start * 360f,
                    sweepAngle = (indicatorEnd - start).coerceAtLeast(0f) * 360f,
                    useCenter = false,
                    topLeft = Offset(center.x - inner, center.y - inner),
                    size = Size(inner * 2, inner * 2),
                    style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
                )
                val angle = (-90f + indicatorEnd * 360f) * PI.toFloat() / 180f
                val from = inner - 6.dp.toPx()
                val to = radius + stroke / 2
                drawLine(
                    indicator,
                    Offset(center.x + from * cos(angle), center.y + from * sin(angle)),
                    Offset(center.x + to * cos(angle), center.y + to * sin(angle)),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
        centre()
    }
}
