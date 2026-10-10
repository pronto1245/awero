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
fun AweroNavigationIcon(kind: String) {
    val color = LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        fun point(x: Float, y: Float) = Offset(size.width * x / 24, size.height * y / 24)
        when (kind) {
            "home" -> {
                val house = Path().apply {
                    moveTo(size.width * 2 / 24, size.height * 11 / 24)
                    lineTo(size.width * 12 / 24, size.height * 2 / 24)
                    lineTo(size.width * 22 / 24, size.height * 11 / 24)
                    moveTo(size.width * 5 / 24, size.height * 9 / 24)
                    lineTo(size.width * 5 / 24, size.height * 22 / 24)
                    lineTo(size.width * 10 / 24, size.height * 22 / 24)
                    lineTo(size.width * 10 / 24, size.height * 15 / 24)
                    lineTo(size.width * 14 / 24, size.height * 15 / 24)
                    lineTo(size.width * 14 / 24, size.height * 22 / 24)
                    lineTo(size.width * 19 / 24, size.height * 22 / 24)
                    lineTo(size.width * 19 / 24, size.height * 9 / 24)
                }
                drawPath(house, color, style = Stroke(width = 1.8.dp.toPx()))
            }
            "progress" -> {
                listOf(10f, 16f, 21f).forEachIndexed { index, height ->
                    drawRoundRect(
                        color, point(3f + index * 7, 23f - height),
                        Size(size.width * 4 / 24, size.height * height / 24),
                        CornerRadius(1.dp.toPx())
                    )
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
