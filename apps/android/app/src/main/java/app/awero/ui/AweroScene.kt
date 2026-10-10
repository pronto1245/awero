package app.awero.ui

import androidx.annotation.DrawableRes
import app.awero.R
import java.util.Calendar

/**
 * Illustrated mountain-lake scenes used as AWERO page backgrounds. In the light theme Home
 * follows the time of day and the wake screen uses dawn; the dark theme always uses night.
 */
enum class AweroScene(@DrawableRes val portrait: Int) {
    DAWN(R.drawable.scene_dawn_portrait),
    DAY(R.drawable.scene_day_portrait),
    SUNSET(R.drawable.scene_sunset_portrait),
    NIGHT(R.drawable.scene_night_portrait);

    companion object {
        /** The scene for a page: [light] in the light theme, night in the dark theme. */
        fun page(light: AweroScene): AweroScene = if (AweroDesign.isDark) NIGHT else light

        @get:DrawableRes
        val wakePortrait: Int
            get() = page(DAWN).portrait

        /** Dawn 05–11, day 11–18, sunset 18–05 (device local time). */
        fun current(hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): AweroScene = when (hour) {
            in 5..10 -> DAWN
            in 11..17 -> DAY
            else -> SUNSET
        }
    }
}
