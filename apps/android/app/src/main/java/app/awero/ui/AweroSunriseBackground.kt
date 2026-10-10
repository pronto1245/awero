package app.awero.ui

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable

/**
 * Background of the OS-facing wake activity: the approved dawn photo across the top of the
 * screen, fading into the ivory mission surface. Without [resources] (or if the photo cannot be
 * decoded) it draws the offline layered sunrise instead.
 */
class AweroSunriseBackground(resources: Resources? = null) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private var opacity = 255
    private val photo: Bitmap? = resources?.let {
        runCatching { BitmapFactory.decodeResource(it, AweroScene.wakePortrait) }.getOrNull()
    }

    override fun draw(canvas: Canvas) {
        val width = bounds.width().toFloat()
        val screenHeight = bounds.height().toFloat()
        val height = screenHeight * .52f
        if (width <= 0f || height <= 0f) return
        photo?.let { image ->
            drawPhoto(canvas, image, width, screenHeight, height)
            return
        }
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
                for (index in 1 until points.lastIndex) {
                    val current = points[index]
                    val next = points[index + 1]
                    quadTo(
                        width * current.first,
                        height * current.second,
                        width * (current.first + next.first) / 2f,
                        height * (current.second + next.second) / 2f
                    )
                }
                lineTo(width * points.last().first, height * points.last().second)
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
        ridge(listOf(0f to .78f, .12f to .65f, .22f to .69f, .37f to .55f, .50f to .71f,
            .68f to .58f, .82f to .70f, 1f to .54f), 0xFFE8AA9A.toInt())
        ridge(listOf(0f to .86f, .16f to .70f, .31f to .83f, .48f to .62f, .66f to .82f,
            .84f to .66f, 1f to .79f), 0xFF8799A8.toInt())

        val lakeTop = height * .80f
        paint.shader = LinearGradient(0f, lakeTop, 0f, height,
            intArrayOf(0xFF597B78.toInt(), 0xFF9BA796.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, lakeTop, width, height, paint)
        paint.shader = null
        val treeColor = 0xFF1F454A.toInt()
        listOf(Triple(.02f, .81f, .18f), Triple(.08f, .82f, .13f), Triple(.14f, .82f, .10f), Triple(.84f, .82f, .11f), Triple(.91f, .81f, .16f), Triple(.98f, .81f, .19f)).forEach { (x, base, treeHeight) ->
            val centerX = width * x
            val baseY = height * base
            val topY = baseY - height * treeHeight
            val pine = Path().apply {
                moveTo(centerX, topY)
                lineTo(centerX - width * treeHeight * .20f, baseY - height * treeHeight * .25f)
                lineTo(centerX - width * treeHeight * .09f, baseY - height * treeHeight * .25f)
                lineTo(centerX - width * treeHeight * .27f, baseY - height * treeHeight * .03f)
                lineTo(centerX + width * treeHeight * .27f, baseY - height * treeHeight * .03f)
                lineTo(centerX + width * treeHeight * .09f, baseY - height * treeHeight * .25f)
                lineTo(centerX + width * treeHeight * .20f, baseY - height * treeHeight * .25f)
                close()
            }
            paint.color = treeColor
            canvas.drawPath(pine, paint)
        }
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

    private fun drawPhoto(canvas: Canvas, image: Bitmap, width: Float, screenHeight: Float, height: Float) {
        paint.shader = null
        paint.color = AweroDesign.ivoryArgb
        paint.alpha = opacity
        canvas.drawRect(bounds, paint)

        // Centre-crop the portrait photo into the top band of the screen.
        val scale = maxOf(width / image.width, height / image.height)
        val sourceWidth = width / scale
        val sourceHeight = height / scale
        val left = (image.width - sourceWidth) / 2f
        val top = (image.height - sourceHeight) / 2f
        val source = Rect(left.toInt(), top.toInt(), (left + sourceWidth).toInt(), (top + sourceHeight).toInt())
        paint.alpha = opacity
        canvas.drawBitmap(image, source, RectF(0f, 0f, width, height), paint)

        // Lighten the sky behind the navy title, then fade into the ivory mission surface.
        paint.shader = LinearGradient(0f, 0f, 0f, height * .55f,
            intArrayOf(0xB3FFF8EF.toInt(), 0x00FFF8EF), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width, height * .55f, paint)
        paint.shader = LinearGradient(0f, height * .75f, 0f, height + screenHeight * .03f,
            intArrayOf(0x00FFF8EF, AweroDesign.ivoryArgb), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, height * .75f, width, height + screenHeight * .03f, paint)
        paint.shader = null
    }

    override fun setAlpha(alpha: Int) { opacity = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter; invalidateSelf() }
    @Deprecated("Android drawable opacity API")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
