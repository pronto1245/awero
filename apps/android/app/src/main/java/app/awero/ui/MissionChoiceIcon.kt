package app.awero.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
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
fun MissionChoiceIcon(kind: String, modifier: Modifier = Modifier, tint: Color = AweroDesign.navy) {
    Canvas(modifier.size(34.dp)) {
        val stroke = 2.dp.toPx()
        val navy = tint
        when (kind) {
            "math" -> {
                drawRoundRect(navy, Offset(size.width * .19f, size.height * .08f), Size(size.width * .62f, size.height * .84f), CornerRadius(3.dp.toPx()), style = Stroke(stroke))
                drawRoundRect(navy, Offset(size.width * .30f, size.height * .20f), Size(size.width * .40f, size.height * .16f), CornerRadius(1.dp.toPx()))
                for (row in 0..2) for (column in 0..2) {
                    drawCircle(navy, size.width * .045f, Offset(size.width * (.34f + column * .16f), size.height * (.52f + row * .13f)))
                }
            }
            "steps" -> {
                val shoe = Path().apply {
                    moveTo(size.width * .16f, size.height * .68f)
                    cubicTo(size.width * .34f, size.height * .66f, size.width * .38f, size.height * .48f, size.width * .50f, size.height * .45f)
                    lineTo(size.width * .62f, size.height * .60f)
                    cubicTo(size.width * .72f, size.height * .62f, size.width * .84f, size.height * .62f, size.width * .88f, size.height * .72f)
                    lineTo(size.width * .86f, size.height * .82f)
                    lineTo(size.width * .18f, size.height * .82f)
                    close()
                }
                drawPath(shoe, navy)
                drawLine(androidx.compose.ui.graphics.Color.White, Offset(size.width * .53f, size.height * .59f), Offset(size.width * .67f, size.height * .66f), stroke * .7f)
                drawLine(androidx.compose.ui.graphics.Color.White, Offset(size.width * .47f, size.height * .64f), Offset(size.width * .62f, size.height * .71f), stroke * .7f)
            }
            else -> {
                val cell = size.width * .22f
                fun finder(x: Float, y: Float) {
                    drawRect(navy, Offset(x, y), Size(cell, cell), style = Stroke(stroke))
                    drawRect(navy, Offset(x + cell * .28f, y + cell * .28f), Size(cell * .44f, cell * .44f))
                }
                finder(size.width * .08f, size.height * .08f)
                finder(size.width * .70f, size.height * .08f)
                finder(size.width * .08f, size.height * .70f)
                listOf(.42f to .18f, .48f to .18f, .42f to .42f, .58f to .42f, .42f to .58f, .62f to .62f, .48f to .72f, .73f to .80f)
                    .forEach { (x, y) -> drawRect(navy, Offset(size.width * x, size.height * y), Size(size.width * .08f, size.height * .08f)) }
            }
        }
    }
}
