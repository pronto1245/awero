package app.awero.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

@Composable
fun SunriseArtwork(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(148.dp)) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFFFFCAA1), Color(0xFFFFE9B8), Color(0xFFE9D9C8))
            )
        )
        drawCircle(
            color = Color(0xFFFFC550),
            radius = 34.dp.toPx(),
            center = Offset(size.width * .5f, size.height * .66f)
        )
        val distant = Path().apply {
            moveTo(0f, size.height * .72f)
            lineTo(size.width * .20f, size.height * .48f)
            lineTo(size.width * .39f, size.height * .72f)
            lineTo(size.width * .62f, size.height * .42f)
            lineTo(size.width * .84f, size.height * .73f)
            lineTo(size.width, size.height * .57f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(distant, Color(0xFF899BAB))
        val foreground = Path().apply {
            moveTo(0f, size.height * .83f)
            cubicTo(
                size.width * .28f, size.height * .68f,
                size.width * .67f, size.height * .96f,
                size.width, size.height * .82f
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(foreground, Color(0xFF577A76))
    }
}
