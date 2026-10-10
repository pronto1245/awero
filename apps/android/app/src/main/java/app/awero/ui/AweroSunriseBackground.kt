package app.awero.ui

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Shader
import android.graphics.drawable.Drawable

/** Offline layered sunrise shared by the OS-facing wake activity. */
class AweroSunriseBackground : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var opacity = 255

    override fun draw(canvas: Canvas) {
        val width = bounds.width().toFloat()
        val screenHeight = bounds.height().toFloat()
        val height = screenHeight * .52f
        if (width <= 0f || height <= 0f) return
        paint.shader = null
        paint.color = AweroDesign.ivoryArgb
        paint.alpha = opacity
        canvas.drawRect(bounds, paint)
        paint.shader = LinearGradient(0f, 0f, 0f, height,
            intArrayOf(0xFFFFB88A.toInt(), 0xFFFFD39B.toInt(), 0xFFFFE9C5.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width, height, paint)
        paint.shader = null

        val sunX = width * .60f
        val sunY = height * .75f
        paint.color = 0x30FFC45A
        paint.alpha = opacity
        canvas.drawCircle(sunX, sunY, width * .16f, paint)
        paint.color = 0xFFFFC64D.toInt()
        paint.alpha = opacity
        canvas.drawCircle(sunX, sunY, width * .105f, paint)

        fun ridge(points: List<Pair<Float, Float>>, color: Int) {
            val path = Path().apply {
                moveTo(width * points.first().first, height * points.first().second)
                points.drop(1).forEach { lineTo(width * it.first, height * it.second) }
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
            paint.shader = null
            paint.color = color
            paint.alpha = opacity
            canvas.drawPath(path, paint)
        }

        // Soft cloud banks sit behind the mountain silhouettes, as in the approved dawn scene.
        paint.color = 0x42F59C7D
        paint.alpha = opacity
        canvas.drawOval(width * .04f, height * .57f, width * .34f, height * .67f, paint)
        canvas.drawOval(width * .63f, height * .55f, width * .96f, height * .66f, paint)
        ridge(listOf(0f to .78f, .12f to .62f, .22f to .69f, .37f to .53f, .50f to .71f,
            .68f to .56f, .82f to .70f, 1f to .52f), 0xFFE8AA9A.toInt())
        ridge(listOf(0f to .86f, .16f to .68f, .31f to .83f, .48f to .60f, .66f to .82f,
            .84f to .64f, 1f to .79f), 0xFF8799A8.toInt())

        val lakeTop = height * .80f
        paint.shader = LinearGradient(0f, lakeTop, 0f, height,
            intArrayOf(0xFF597B78.toInt(), 0xFF9BA796.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, lakeTop, width, height, paint)
        paint.shader = null
        for (index in 0..5) {
            val y = lakeTop + height * (.035f + index * .023f)
            paint.color = 0x40FFFFFF
            paint.alpha = (opacity * (0.9f - index * .1f)).toInt()
            paint.strokeWidth = (1f + (2 - index).coerceAtLeast(0))
            canvas.drawLine(width * .43f, y, width * (.60f - index * .015f), y, paint)
        }

        // Fade artwork into the ivory mission surface, keeping the keypad legible.
        paint.shader = LinearGradient(0f, height * .90f, 0f, height + screenHeight * .03f,
            intArrayOf(0x00FFF8EF, AweroDesign.ivoryArgb), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, height * .90f, width, height + screenHeight * .03f, paint)
        paint.shader = null
    }

    override fun setAlpha(alpha: Int) { opacity = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter; invalidateSelf() }
    @Deprecated("Android drawable opacity API")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
