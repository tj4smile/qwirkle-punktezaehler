package com.thomas.qwirkle.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Die sechs Symbole der Spielsteine. */
private enum class StoneShape { CIRCLE, STAR4, DIAMOND, SQUARE, CLOVER, STAR8 }

private val stoneColors = listOf(
    Color(0xFFE53935), // Rot
    Color(0xFFFB8C00), // Orange
    Color(0xFFFDD835), // Gelb
    Color(0xFF43A047), // Grün
    Color(0xFF1E88E5), // Blau
    Color(0xFF8E24AA)  // Lila
)

private val stoneShapes = StoneShape.entries

private val stoneBackground = Color(0xFF14171F)
private val stoneBorder = Color(0xFF3A4152)

/**
 * Eine Reihe von sechs Spielsteinen als Logo: dunkle, abgerundete Steine mit je
 * einem farbigen Symbol (Kreis, Stern, Raute, Quadrat, Kleeblatt, Strahlenstern).
 * Alles wird mit Canvas gezeichnet, es werden keine Bilder oder Icon-Pakete benötigt.
 */
@Composable
fun QwirkleStones(
    modifier: Modifier = Modifier,
    stoneSize: Dp = 40.dp,
    spacing: Dp = 6.dp
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(spacing)) {
        stoneShapes.forEachIndexed { index, shape ->
            QwirkleStone(shape, stoneColors[index], stoneSize)
        }
    }
}

@Composable
private fun QwirkleStone(shape: StoneShape, color: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size.minDimension
        val corner = CornerRadius(s * 0.18f)

        drawRoundRect(color = stoneBackground, cornerRadius = corner)
        drawRoundRect(
            color = stoneBorder,
            cornerRadius = corner,
            style = Stroke(width = s * 0.035f)
        )
        drawSymbol(shape, color, center = Offset(s / 2f, s / 2f), radius = s * 0.33f)
    }
}

private fun DrawScope.drawSymbol(shape: StoneShape, color: Color, center: Offset, radius: Float) {
    when (shape) {
        StoneShape.CIRCLE -> drawCircle(color, radius * 0.92f, center)

        StoneShape.SQUARE -> drawRoundRect(
            color = color,
            topLeft = Offset(center.x - radius * 0.85f, center.y - radius * 0.85f),
            size = Size(radius * 1.7f, radius * 1.7f),
            cornerRadius = CornerRadius(radius * 0.12f)
        )

        StoneShape.DIAMOND -> drawPath(
            Path().apply {
                moveTo(center.x, center.y - radius * 1.1f)
                lineTo(center.x + radius * 0.85f, center.y)
                lineTo(center.x, center.y + radius * 1.1f)
                lineTo(center.x - radius * 0.85f, center.y)
                close()
            },
            color
        )

        StoneShape.STAR4 -> drawPath(starPath(center, points = 4, outer = radius * 1.15f, inner = radius * 0.38f), color)

        StoneShape.STAR8 -> drawPath(starPath(center, points = 8, outer = radius * 1.1f, inner = radius * 0.62f), color)

        StoneShape.CLOVER -> {
            val leaf = radius * 0.5f
            val offset = radius * 0.5f
            drawCircle(color, leaf, Offset(center.x, center.y - offset))
            drawCircle(color, leaf, Offset(center.x + offset, center.y))
            drawCircle(color, leaf, Offset(center.x, center.y + offset))
            drawCircle(color, leaf, Offset(center.x - offset, center.y))
            drawCircle(color, leaf * 0.9f, center)
        }
    }
}

/** Stern mit [points] Spitzen; die erste Spitze zeigt nach oben. */
private fun starPath(center: Offset, points: Int, outer: Float, inner: Float): Path {
    val path = Path()
    val steps = points * 2
    for (i in 0 until steps) {
        val radius = if (i % 2 == 0) outer else inner
        val angle = -PI / 2 + i * PI / points
        val x = center.x + (radius * cos(angle)).toFloat()
        val y = center.y + (radius * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
