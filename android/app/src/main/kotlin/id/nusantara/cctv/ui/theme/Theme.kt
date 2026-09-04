package id.nusantara.cctv.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import id.nusantara.cctv.data.prefs.ThemeMode

// Palet: teal monitoring di atas latar biru-gelap kehutanan (identitas CCTV malam hari)
private val Teal = Color(0xFF2BD9C8)
private val TealDim = Color(0xFF1A8F84)
private val DeepSea = Color(0xFF0F2A33)
private val DeepSeaElevated = Color(0xFF153944)
private val AmberStatus = Color(0xFFFFB74D)
private val RedStatus = Color(0xFFEF5350)

private val DarkScheme = darkColorScheme(
    primary = Teal,
    onPrimary = Color(0xFF00332E),
    primaryContainer = TealDim,
    onPrimaryContainer = Color(0xFFB8FFF5),
    secondary = AmberStatus,
    background = DeepSea,
    onBackground = Color(0xFFE4F2F1),
    surface = DeepSea,
    onSurface = Color(0xFFE4F2F1),
    surfaceVariant = DeepSeaElevated,
    onSurfaceVariant = Color(0xFFA7C4C1),
    error = RedStatus,
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

@Composable
fun NusantaraTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
}
