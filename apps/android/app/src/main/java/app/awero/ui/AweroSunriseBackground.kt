package app.awero.ui

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Shader
import android.graphics.drawable.Drawable

/** Offline sunrise behind the OS-facing wake activity, without occupying control space. */
class AweroSunriseBackground : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var opacity = 255

    override fun draw(canvas: Canvas) {
        val width = bounds.width().toFloat()
        val height = bounds.height() * .43f
        if (width <= 0f || height <= 0f) return
        paint.alpha = opacity
        paint.shader = null
        paint.color = AweroDesign.ivoryArgb
        paint.alpha = opacity
        canvas.drawRect(bounds, paint)
        paint.shader = LinearGradient(
            0f, 0f, 0f, height,
            intArrayOf(0xFFFFCAA1.toInt(), 0xFFFFE9B8.toInt(), 0xFFE9D9C8.toInt()),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width, height, paint)
        paint.shader = null
        paint.color = 0xFFFFC550.toInt()
        paint.alpha = opacity
        canvas.drawCircle(width * .52f, height * .66f, width * .10f, paint)
        val hills = Path().apply {
            moveTo(0f, height * .72f)
            lineTo(width * .20f, height * .48f)
            lineTo(width * .39f, height * .72f)
            lineTo(width * .62f, height * .42f)
            lineTo(width * .84f, height * .73f)
            lineTo(width, height * .57f)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        paint.color = 0xFF899BAB.toInt()
        paint.alpha = opacity
        canvas.drawPath(hills, paint)
        paint.shader = LinearGradient(0f, height * .74f, 0f, height, 0x00FFF8EF, AweroDesign.ivoryArgb, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, height * .74f, width, height, paint)
        paint.shader = null
    }

    override fun setAlpha(alpha: Int) { opacity = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter; invalidateSelf() }
    @Deprecated("Android drawable opacity API")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
