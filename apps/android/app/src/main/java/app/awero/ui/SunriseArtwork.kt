package app.awero.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp

@Composable
fun ApprovedHomeSunriseArtwork(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 160.dp,
    scene: AweroScene = AweroScene.current()
) {
    Box(modifier.fillMaxWidth().height(height)) {
        Image(
            painter = painterResource(scene.band),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(height)
        )
        Box(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().height(height * 0.18f)
                .background(Brush.verticalGradient(listOf(AweroDesign.ivory, AweroDesign.ivory.copy(alpha = 0f))))
        )
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(height * 0.30f)
                .background(Brush.verticalGradient(listOf(AweroDesign.ivory.copy(alpha = 0f), AweroDesign.ivory)))
        )
    }
}

/**
 * Dawn photo header for the in-app wake screen: lightens the top so the navy title stays readable
 * over the sky; the caller fades the bottom into the ivory mission surface.
 */
@Composable
fun WakeSceneArtwork(modifier: Modifier = Modifier, height: androidx.compose.ui.unit.Dp = 390.dp) {
    Box(modifier.fillMaxWidth().height(height)) {
        Image(
            painter = painterResource(AweroScene.wakePortrait),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(height)
        )
        Box(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().height(height * 0.55f)
                .background(Brush.verticalGradient(listOf(AweroDesign.ivory.copy(alpha = .70f), AweroDesign.ivory.copy(alpha = 0f))))
        )
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(height * 0.25f)
                .background(Brush.verticalGradient(listOf(AweroDesign.ivory.copy(alpha = 0f), AweroDesign.ivory)))
        )
    }
}

@Composable
fun SunriseArtwork(modifier: Modifier = Modifier, height: androidx.compose.ui.unit.Dp = 148.dp, rounded: Boolean = false, showsForest: Boolean = false, showsLake: Boolean = false) {
    val shape = if (rounded) RoundedCornerShape(22.dp) else RoundedCornerShape(0.dp)
    Canvas(modifier.fillMaxWidth().height(height).background(Color(0xFFFFE9B8), shape)) {
        val clip = Path().apply {
            if (rounded) addRoundRect(androidx.compose.ui.geometry.RoundRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height), CornerRadius(22.dp.toPx())))
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
                    for (index in 1 until points.lastIndex) {
                        val current = points[index]
                        val next = points[index + 1]
                        quadraticBezierTo(
                            size.width * current.first,
                            size.height * current.second,
                            size.width * (current.first + next.first) / 2f,
                            size.height * (current.second + next.second) / 2f
                        )
                    }
                    lineTo(size.width * points.last().first, size.height * points.last().second)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(path, color)
            }

            drawOval(Color.White.copy(alpha = .20f), Offset(size.width * .12f, size.height * .31f), androidx.compose.ui.geometry.Size(size.width * .25f, size.height * .055f))
            drawOval(Color.White.copy(alpha = .16f), Offset(size.width * .72f, size.height * .27f), androidx.compose.ui.geometry.Size(size.width * .30f, size.height * .055f))
            ridge(listOf(0f to .74f, .13f to .61f, .24f to .68f, .40f to .53f, .53f to .69f, .70f to .57f, .86f to .69f, 1f to .56f), Color(0xFFE8A89A))
            ridge(listOf(0f to .82f, .17f to .68f, .32f to .82f, .48f to .60f, .66f to .80f, .83f to .65f, 1f to .80f), Color(0xFF899BAB))

            if (showsLake) {
                val lake = Path().apply {
                    moveTo(0f, size.height * .82f)
                    cubicTo(size.width * .30f, size.height * .75f, size.width * .67f, size.height * .90f, size.width, size.height * .82f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(lake, Brush.verticalGradient(listOf(Color(0xFF577A76), Color(0xFF9BA79A)), size.height * .80f, size.height))
            }
            if (showsForest && showsLake) {
                val treeColor = Color(0xFF1F454A)
                listOf(Triple(.02f, .78f, .18f), Triple(.08f, .80f, .13f), Triple(.14f, .79f, .10f), Triple(.84f, .80f, .11f), Triple(.91f, .78f, .16f), Triple(.98f, .79f, .19f)).forEach { (x, base, treeHeight) ->
                    val centerX = size.width * x
                    val baseY = size.height * base
                    val topY = baseY - size.height * treeHeight
                    val pine = Path().apply {
                        moveTo(centerX, topY)
                        lineTo(centerX - size.width * treeHeight * .20f, baseY - size.height * treeHeight * .25f)
                        lineTo(centerX - size.width * treeHeight * .09f, baseY - size.height * treeHeight * .25f)
                        lineTo(centerX - size.width * treeHeight * .27f, baseY - size.height * treeHeight * .03f)
                        lineTo(centerX + size.width * treeHeight * .27f, baseY - size.height * treeHeight * .03f)
                        lineTo(centerX + size.width * treeHeight * .09f, baseY - size.height * treeHeight * .25f)
                        lineTo(centerX + size.width * treeHeight * .20f, baseY - size.height * treeHeight * .25f)
                        close()
                    }
                    drawPath(pine, treeColor)
                }
            }
            if (showsLake) repeat(5) { index ->
                val y = size.height * (.86f + index * .025f)
                drawLine(Color.White.copy(alpha = .22f - index * .025f), Offset(size.width * .43f, y), Offset(size.width * .63f, y), 1.dp.toPx())
            }
        }
    }
}
