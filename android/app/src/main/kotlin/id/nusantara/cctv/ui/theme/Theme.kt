package id.nusantara.cctv.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import id.nusantara.cctv.data.prefs.PresetPalettes
import id.nusantara.cctv.data.prefs.ThemeMode

// Palet dasar (fallback pre-Android 12)
private val Teal = Color(0xFF2BD9C8)
private val TealDim = Color(0xFF1A8F84)
private val DeepSea = Color(0xFF0F2A33)
private val DeepSeaElevated = Color(0xFF153944)

private val DarkScheme = darkColorScheme(
    primary = Teal,
    onPrimary = Color(0xFF00332E),
    primaryContainer = TealDim,
    onPrimaryContainer = Color(0xFFB8FFF5),
    secondary = Color(0xFFFFB74D),
    background = DeepSea,
    onBackground = Color(0xFFE4F2F1),
    surface = DeepSea,
    onSurface = Color(0xFFE4F2F1),
    surfaceVariant = DeepSeaElevated,
    onSurfaceVariant = Color(0xFFA7C4C1),
    error = Color(0xFFEF5350),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF00796B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB),
    secondary = Color(0xFFB26A00),
    background = Color(0xFFF4FAF9),
    surface = Color.White,
    surfaceVariant = Color(0xFFE0EEEC),
    onSurfaceVariant = Color(0xFF3F5A57),
)

private fun paletteScheme(p: id.nusantara.cctv.data.prefs.ThemePalette): ColorScheme {
    fun c(v: Long) = Color(v.toInt())
    return ColorScheme(
        primary = c(p.primary),
        onPrimary = c(p.onPrimary),
        primaryContainer = c(p.primaryContainer),
        onPrimaryContainer = c(p.onPrimaryContainer),
        inversePrimary = c(p.primary).copy(alpha = 0.85f),
        secondary = c(p.secondary),
        onSecondary = c(p.onSecondary),
        secondaryContainer = c(p.secondaryContainer),
        onSecondaryContainer = c(p.onSecondaryContainer),
        tertiary = c(p.tertiary),
        onTertiary = c(p.onTertiary),
        tertiaryContainer = c(p.secondaryContainer),
        onTertiaryContainer = c(p.onSecondaryContainer),
        background = c(p.background),
        onBackground = c(p.onBackground),
        surface = c(p.surface),
        onSurface = c(p.onSurface),
        surfaceVariant = c(p.surfaceVariant),
        onSurfaceVariant = c(p.onSurfaceVariant),
        surfaceTint = c(p.primary),
        inverseSurface = c(p.onSurface),
        inverseOnSurface = c(p.surface),
        error = c(p.error),
        onError = c(p.onPrimary),
        errorContainer = c(p.errorContainer),
        onErrorContainer = c(p.onBackground),
        outline = c(p.outline),
        outlineVariant = c(p.outline).copy(alpha = 0.5f),
        scrim = Color.Black,
        surfaceBright = c(p.surface),
        surfaceDim = c(p.surfaceVariant),
        surfaceContainer = c(p.surfaceVariant),
        surfaceContainerHigh = c(p.surfaceVariant),
        surfaceContainerHighest = c(p.surfaceVariant),
        surfaceContainerLow = c(p.surface),
        surfaceContainerLowest = c(p.background),
    )
}

/**
 * Tema aplikasi. SYSTEM memakai Material You dinamis (Android 12+) — warna
 * mengikuti wallpaper pengguna; fallback palet teal pada versi lebih lama.
 * CYBER/MONOCHROME memakai palet tetap.
 */
@Composable
fun NusantaraTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val scheme: ColorScheme = when (themeMode) {
        ThemeMode.SYSTEM ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (systemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else if (systemDark) DarkScheme else LightScheme
        ThemeMode.LIGHT -> LightScheme
        ThemeMode.DARK -> DarkScheme
        ThemeMode.CYBER -> paletteScheme(PresetPalettes.CYBER)
        ThemeMode.MONOCHROME ->
            paletteScheme(if (systemDark) PresetPalettes.MONOCHROME_DARK else PresetPalettes.MONOCHROME)
    }
    MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
}
