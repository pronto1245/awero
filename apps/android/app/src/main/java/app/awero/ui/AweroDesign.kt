package app.awero.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.lightColorScheme

object AweroDesign {
    val ivoryArgb: Int = 0xFFFFF8EF.toInt()
    val navyArgb: Int = 0xFF14294B.toInt()
    val coralArgb: Int = 0xFFFF684B.toInt()
    val ivory = Color(0xFFFFF8EF)
    val navy = Color(0xFF14294B)
    val coral = Color(0xFFFF684B)
    val sage = Color(0xFF4F8B66)
    val colors = lightColorScheme(
        primary = coral, onPrimary = navy,
        primaryContainer = coral.copy(alpha = .18f), onPrimaryContainer = navy,
        secondary = sage, onSecondary = Color.White,
        secondaryContainer = coral.copy(alpha = .15f), onSecondaryContainer = navy,
        background = ivory, onBackground = navy,
        surface = ivory, onSurface = navy,
        surfaceVariant = Color(0xFFFFEFDB), onSurfaceVariant = navy,
        error = Color(0xFFB3261E)
    )
}
