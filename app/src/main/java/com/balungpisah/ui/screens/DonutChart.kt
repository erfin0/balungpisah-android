package com.balungpisah.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun DonutChart(
    segments: List<DonutSegment>,
    total: Double,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = size
            val radius = min(canvasSize.width, canvasSize.height) / 2
            val innerRadius = radius * 0.618f
            val center = Offset(canvasSize.width / 2, canvasSize.height / 2)

            var startAngle = -90f

            segments.forEach { segment ->
                val sweepAngle = ((segment.value / total) * 360).toFloat()

                drawDonutSlice(
                    center = center,
                    radius = radius,
                    innerRadius = innerRadius,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    color = segment.color
                )

                startAngle += sweepAngle
            }
        }
    }
}

private fun DrawScope.drawDonutSlice(
    center: Offset,
    radius: Float,
    innerRadius: Float,
    startAngle: Float,
    sweepAngle: Float,
    color: Color
) {
    val path = Path().apply {
        // Calculate start point on outer arc
        val startRad = Math.toRadians(startAngle.toDouble())
        val outerStartX = center.x + radius * cos(startRad).toFloat()
        val outerStartY = center.y + radius * sin(startRad).toFloat()

        moveTo(outerStartX, outerStartY)

        // Draw outer arc
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                left = center.x - radius,
                top = center.y - radius,
                right = center.x + radius,
                bottom = center.y + radius
            ),
            startAngleDegrees = startAngle,
            sweepAngleDegrees = sweepAngle,
            forceMoveTo = false
        )

        // Calculate end point and draw line to inner arc
        val endRad = Math.toRadians((startAngle + sweepAngle).toDouble())
        val innerEndX = center.x + innerRadius * cos(endRad).toFloat()
        val innerEndY = center.y + innerRadius * sin(endRad).toFloat()

        lineTo(innerEndX, innerEndY)

        // Draw inner arc (backwards)
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                left = center.x - innerRadius,
                top = center.y - innerRadius,
                right = center.x + innerRadius,
                bottom = center.y + innerRadius
            ),
            startAngleDegrees = startAngle + sweepAngle,
            sweepAngleDegrees = -sweepAngle,
            forceMoveTo = false
        )

        close()
    }

    drawPath(
        path = path,
        color = color
    )
}