package app.awero.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp

@Composable
fun SunriseArtwork(modifier: Modifier = Modifier, height: androidx.compose.ui.unit.Dp = 148.dp, rounded: Boolean = false) {
    val shape = if (rounded) RoundedCornerShape(22.dp) else RoundedCornerShape(0.dp)
    Canvas(modifier.fillMaxWidth().height(height).background(Color(0xFFFFE9B8), shape)) {
        val clip = Path().apply {
            if (rounded) addRoundRect(androidx.compose.ui.geometry.RoundRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height), 22.dp.toPx()))
            else addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
        }
        clipPath(clip) {
            drawRect(brush = Brush.verticalGradient(listOf(Color(0xFFFFB98D), Color(0xFFFFD8A0), Color(0xFFFFF0CD))))

            val sun = Offset(size.width * .53f, size.height * .64f)
            drawCircle(Color(0x30FFC75C), size.height * .37f, sun)
            drawCircle(Color(0xFFFFC550), size.height * .29f, sun)

            fun ridge(points: List<Pair<Float, Float>>, color: Color) {
                val path = Path().apply {
                    moveTo(size.width * points.first().first, size.height * points.first().second)
                    points.drop(1).forEach { lineTo(size.width * it.first, size.height * it.second) }
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(path, color)
            }

            ridge(listOf(0f to .74f, .13f to .56f, .24f to .67f, .40f to .47f, .53f to .68f, .70f to .51f, .86f to .69f, 1f to .53f), Color(0xFFE8A89A))
            ridge(listOf(0f to .82f, .17f to .63f, .32f to .82f, .48f to .55f, .66f to .79f, .83f to .61f, 1f to .78f), Color(0xFF899BAB))

            val lake = Path().apply {
                moveTo(0f, size.height * .82f)
                cubicTo(size.width * .30f, size.height * .75f, size.width * .67f, size.height * .90f, size.width, size.height * .82f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(lake, Brush.verticalGradient(listOf(Color(0xFF577A76), Color(0xFF9BA79A)), size.height * .80f, size.height))
            repeat(5) { index ->
                val y = size.height * (.86f + index * .025f)
                drawLine(Color.White.copy(alpha = .22f - index * .025f), Offset(size.width * .43f, y), Offset(size.width * .63f, y), 1.dp.toPx())
            }
        }
    }
}
