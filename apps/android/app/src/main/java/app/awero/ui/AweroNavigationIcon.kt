package app.awero.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun AweroNavigationIcon(kind: String, tint: androidx.compose.ui.graphics.Color = LocalContentColor.current) {
    val color = tint
    Canvas(Modifier.size(24.dp)) {
        fun point(x: Float, y: Float) = Offset(size.width * x / 24, size.height * y / 24)
        when (kind) {
            "home" -> {
                val house = Path().apply {
                    moveTo(size.width * 2 / 24, size.height * 11 / 24)
                    lineTo(size.width * 12 / 24, size.height * 2 / 24)
                    lineTo(size.width * 22 / 24, size.height * 11 / 24)
                    lineTo(size.width * 19 / 24, size.height * 11 / 24)
                    lineTo(size.width * 19 / 24, size.height * 22 / 24)
                    lineTo(size.width * 5 / 24, size.height * 22 / 24)
                    close()
                }
                drawPath(house, color)
                drawRoundRect(AweroDesign.ivory, point(10f, 15f), Size(size.width * 4 / 24, size.height * 7 / 24), CornerRadius(1.dp.toPx()))
            }
            "progress" -> {
                listOf(10f, 16f, 21f).forEachIndexed { index, height ->
                    drawRoundRect(
                        color, point(3f + index * 7, 23f - height),
                        Size(size.width * 4 / 24, size.height * height / 24),
                        CornerRadius(1.dp.toPx())
                    )
                }
                drawRoundRect(color, point(2f, 22f), Size(size.width * 21 / 24, size.height / 24), CornerRadius(1.dp.toPx()))
            }
            "settings" -> {
                val center = point(12f, 12f)
                for (index in 0 until 8) {
                    val angle = Math.toRadians(index * 45.0)
                    val inner = Offset(center.x + kotlin.math.cos(angle).toFloat() * size.width * .29f, center.y + kotlin.math.sin(angle).toFloat() * size.height * .29f)
                    val outer = Offset(center.x + kotlin.math.cos(angle).toFloat() * size.width * .43f, center.y + kotlin.math.sin(angle).toFloat() * size.height * .43f)
                    drawLine(color, inner, outer, strokeWidth = 2.dp.toPx())
                }
                drawCircle(color, radius = size.width * .30f, center = center, style = Stroke(width = 2.dp.toPx()))
                drawCircle(color, radius = size.width * .10f, center = center, style = Stroke(width = 2.dp.toPx()))
            }
            "sun" -> {
                val center = point(12f, 12f)
                drawCircle(color, radius = size.width * .22f, center = center)
                for (index in 0 until 8) {
                    val angle = Math.toRadians(index * 45.0)
                    val inner = Offset(center.x + kotlin.math.cos(angle).toFloat() * size.width * .34f, center.y + kotlin.math.sin(angle).toFloat() * size.height * .34f)
                    val outer = Offset(center.x + kotlin.math.cos(angle).toFloat() * size.width * .45f, center.y + kotlin.math.sin(angle).toFloat() * size.height * .45f)
                    drawLine(color, inner, outer, strokeWidth = 1.8.dp.toPx())
                }
            }
            else -> {
                drawCircle(color, radius = size.width * 4 / 24, center = point(12f, 6f))
                drawRoundRect(
                    color, point(4f, 13f), Size(size.width * 16 / 24, size.height * 9 / 24),
                    CornerRadius(size.width * 5 / 24)
                )
            }
        }
    }
}
