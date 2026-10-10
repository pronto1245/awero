package app.awero.ui

import android.content.Context
import android.content.res.Configuration
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

/**
 * AWERO design tokens, measured from the owner's light and dark reference screens
 * (`docs/design/screens`, `docs/design/screens/dark`).
 *
 * The theme follows the system appearance. Each activity calls [applySystemTheme] in `onCreate`;
 * Android recreates activities when the appearance changes, so plain getters stay correct for
 * both Compose and View code. Names keep their historical roles: [ivory] is the page background
 * and [navy] the primary text color in both themes.
 */
object AweroDesign {
    @Volatile
    var isDark: Boolean = false
        private set

    fun applySystemTheme(context: Context) {
        isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }

    private fun pick(light: Long, dark: Long) = Color(if (isDark) dark else light)

    /** Page background. */
    val ivory: Color get() = pick(0xFFFBF6EF, 0xFF121624)
    /** Primary text and icons. */
    val navy: Color get() = pick(0xFF1D2433, 0xFFF2EDE6)
    val textSecondary: Color get() = pick(0xFF6E6B6B, 0xFFA39D97)
    /** Accent for icons, selection outlines, the selected tab and progress rings. */
    val coral: Color get() = Color(0xFFF26A3D)
    /** Filled controls with white text; darker than [coral] so labels meet contrast. */
    val coralStrong: Color get() = Color(0xFFE0502F)
    val coralSoft: Color get() = pick(0xFFFDEEE6, 0xFF2E2326)
    val sage: Color get() = Color(0xFF6AA84F)
    val successSoft: Color get() = pick(0xFFE9F0DF, 0xFF1F2E24)
    val successText: Color get() = pick(0xFF3F8A3A, 0xFF7FC36A)
    /** Cards, list groups and keypad keys. */
    val surface: Color get() = pick(0xFFFFFFFF, 0xFF1E2335)
    val surfaceMuted: Color get() = pick(0xFFF4ECE2, 0xFF1E2335)
    val surfaceStrong: Color get() = pick(0xFFECE2D6, 0xFF2B3247)
    val chip: Color get() = pick(0xFFEFE8DF, 0xFF262C40)
    /** Tip and notice cards. */
    val surfaceWarm: Color get() = pick(0xFFFDF0DC, 0xFF2E2A1F)
    /** Destructive and emergency text. */
    val warning: Color get() = pick(0xFFB5241D, 0xFFF0645A)
    val warningSoft: Color get() = pick(0xFFFBE9E5, 0xFF2E1F22)
    val warningLine: Color get() = pick(0xFFF2C9C2, 0xFF5A2C2C)
    val sun: Color get() = Color(0xFFF5A623)
    val border: Color get() = navy.copy(alpha = .10f)

    val ivoryArgb: Int get() = ivory.toArgb()
    val navyArgb: Int get() = navy.toArgb()
    val coralArgb: Int get() = coralStrong.toArgb()
    val surfaceArgb: Int get() = surface.toArgb()
    val warningArgb: Int get() = warning.toArgb()
    val warningSoftArgb: Int get() = warningSoft.toArgb()
    val warningLineArgb: Int get() = warningLine.toArgb()

    val pagePadding = 20.dp
    val sectionSpacing = 16.dp
    val compactSpacing = 8.dp
    val controlCorner = 14.dp
    val cardCorner = 22.dp
    val minimumTouchTarget = 44.dp

    val colors: ColorScheme
        get() = if (isDark) {
            darkColorScheme(
                primary = coralStrong, onPrimary = Color.White,
                primaryContainer = coralSoft, onPrimaryContainer = navy,
                secondary = sage, onSecondary = Color.White,
                secondaryContainer = coralSoft, onSecondaryContainer = navy,
                background = ivory, onBackground = navy,
                surface = ivory, onSurface = navy,
                surfaceVariant = chip, onSurfaceVariant = navy,
                error = warning
            )
        } else {
            lightColorScheme(
                primary = coralStrong, onPrimary = Color.White,
                primaryContainer = coralSoft, onPrimaryContainer = navy,
                secondary = sage, onSecondary = Color.White,
                secondaryContainer = coralSoft, onSecondaryContainer = navy,
                background = ivory, onBackground = navy,
                surface = ivory, onSurface = navy,
                surfaceVariant = chip, onSurfaceVariant = navy,
                error = warning
            )
        }
}
