package app.awero.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
                val gear = Path()
                for (index in 0 until 32) {
                    val tooth = index % 4
                    val radius = when (tooth) {
                        1, 2 -> .45f
                        else -> .35f
                    }
                    val angle = Math.toRadians(index * 11.25 - 90.0)
                    val p = Offset(
                        center.x + kotlin.math.cos(angle).toFloat() * size.width * radius,
                        center.y + kotlin.math.sin(angle).toFloat() * size.height * radius
                    )
                    if (index == 0) gear.moveTo(p.x, p.y) else gear.lineTo(p.x, p.y)
                }
                gear.close()
                drawPath(gear, color, style = Stroke(width = 2.dp.toPx()))
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
            "warning" -> {
                val warning = Path().apply {
                    moveTo(point(12f, 2f).x, point(12f, 2f).y)
                    lineTo(point(23f, 21f).x, point(23f, 21f).y)
                    lineTo(point(1f, 21f).x, point(1f, 21f).y)
                    close()
                }
                drawPath(warning, color)
                drawLine(Color.White, point(12f, 8f), point(12f, 14f), strokeWidth = 2.2.dp.toPx())
                drawCircle(Color.White, radius = 1.2.dp.toPx(), center = point(12f, 17f))
            }
            "plant" -> {
                val leaf = Path().apply {
                    moveTo(point(12f, 20f).x, point(12f, 20f).y)
                    cubicTo(point(4f, 18f).x, point(4f, 18f).y, point(4f, 9f).x, point(4f, 9f).y, point(7f, 6f).x, point(7f, 6f).y)
                    cubicTo(point(12f, 3f).x, point(12f, 3f).y, point(20f, 4f).x, point(20f, 4f).y, point(20f, 8f).x, point(20f, 8f).y)
                    cubicTo(point(20f, 13f).x, point(20f, 13f).y, point(15f, 19f).x, point(15f, 19f).y, point(12f, 20f).x, point(12f, 20f).y)
                    close()
                }
                drawPath(leaf, color)
                drawLine(Color.White.copy(alpha = .88f), point(7f, 16f), point(16f, 8f), strokeWidth = 1.2.dp.toPx())
                drawLine(Color.White.copy(alpha = .88f), point(11f, 13f), point(10f, 8f), strokeWidth = 1.dp.toPx())
                drawLine(Color.White.copy(alpha = .88f), point(12f, 12f), point(17f, 13f), strokeWidth = 1.dp.toPx())
            }
            "profile" -> {
                drawCircle(color, radius = size.width * .18f, center = point(12f, 7f))
                val shoulders = Path().apply {
                    moveTo(point(3f, 21f).x, point(3f, 21f).y)
                    cubicTo(point(3f, 16f).x, point(3f, 16f).y, point(7f, 13f).x, point(7f, 13f).y, point(12f, 13f).x, point(12f, 13f).y)
                    cubicTo(point(17f, 13f).x, point(17f, 13f).y, point(21f, 16f).x, point(21f, 16f).y, point(21f, 21f).x, point(21f, 21f).y)
                    close()
                }
                drawPath(shoulders, color)
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
