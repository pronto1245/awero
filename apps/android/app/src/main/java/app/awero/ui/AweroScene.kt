package app.awero.ui

import androidx.annotation.DrawableRes
import app.awero.R
import java.util.Calendar

/**
 * Illustrated mountain-lake scenes used as AWERO page backgrounds. Home follows the time of day;
 * the wake screen always uses dawn. The night scene arrives with the dark theme.
 */
enum class AweroScene(@DrawableRes val portrait: Int) {
    DAWN(R.drawable.scene_dawn_portrait),
    DAY(R.drawable.scene_day_portrait),
    SUNSET(R.drawable.scene_sunset_portrait);

    companion object {
        @DrawableRes
        val wakePortrait: Int = DAWN.portrait

        /** Dawn 05–11, day 11–18, sunset 18–05 (device local time). */
        fun current(hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): AweroScene = when (hour) {
            in 5..10 -> DAWN
            in 11..17 -> DAY
            else -> SUNSET
        }
    }
}
