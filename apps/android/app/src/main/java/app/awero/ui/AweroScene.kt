package app.awero.ui

import androidx.annotation.DrawableRes
import app.awero.R
import java.util.Calendar

/**
 * Mountain-lake photo scenes used as AWERO backgrounds. The Home band follows the time of day;
 * the wake screen always uses the dawn scene from the approved design.
 */
enum class AweroScene(@DrawableRes val band: Int) {
    DAWN(R.drawable.scene_dawn_band),
    DAY(R.drawable.scene_day_band),
    SUNSET(R.drawable.scene_sunset_band),
    NIGHT(R.drawable.scene_night_band);

    companion object {
        @DrawableRes
        val wakePortrait: Int = R.drawable.scene_dawn_portrait

        /** Dawn 05–11, day 11–18, sunset 18–23, night 23–05 (device local time). */
        fun current(hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): AweroScene = when (hour) {
            in 5..10 -> DAWN
            in 11..17 -> DAY
            in 18..22 -> SUNSET
            else -> NIGHT
        }
    }
}
