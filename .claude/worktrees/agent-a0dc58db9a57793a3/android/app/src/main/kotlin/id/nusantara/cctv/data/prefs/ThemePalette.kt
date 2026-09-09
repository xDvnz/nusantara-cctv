package id.nusantara.cctv.data.prefs

// ThemeMode didefinisikan di AppPreferences.kt pada package yang sama.

/** Palet tema kustom untuk aplikasi. */
data class ThemePalette(
    val nameResId: Int,
    val primary: Long,       // 0xFFRRGGBB
    val onPrimary: Long,
    val primaryContainer: Long,
    val onPrimaryContainer: Long,
    val secondary: Long,
    val tertiary: Long,
    val background: Long,
    val onBackground: Long,
    val surface: Long,
    val onSurface: Long,
    val surfaceVariant: Long,
    val onSurfaceVariant: Long,
    val error: Long,
    val errorContainer: Long,
    val outline: Long,
)

/** Palet siap pakai — warna dipilih khusus untuk aplikasi CCTV (tampilan monitoring). */
object PresetPalettes {

    private fun rgb(r: Int, g: Int, b: Int): Long =
        0xFF000000L or ((r and 0xFF).toLong() shl 16) or ((g and 0xFF).toLong() shl 8) or (b and 0xFF).toLong()

    val CYBER = ThemePalette(
        nameResId = id.nusantara.cctv.R.string.theme_cyber,
        primary = rgb(0x00, 0xE5, 0xFF),           // cyan neon
        onPrimary = rgb(0x00, 0x1A, 0x24),
        primaryContainer = rgb(0x00, 0x3A, 0x4C),
        onPrimaryContainer = rgb(0x83, 0xF3, 0xFF),
        secondary = rgb(0x6D, 0xCC, 0xB8),
        tertiary = rgb(0xBF, 0x40, 0xD9),          // ungu neon aksen
        background = rgb(0x04, 0x0A, 0x10),         // deep navy-black
        onBackground = rgb(0xE0, 0xF7, 0xFA),
        surface = rgb(0x08, 0x12, 0x1C),
        onSurface = rgb(0xC2, 0xEC, 0xF5),
        surfaceVariant = rgb(0x0C, 0x1C, 0x2A),
        onSurfaceVariant = rgb(0x7F, 0xC9, 0xD4),
        error = rgb(0xFF, 0x45, 0x50),
        errorContainer = rgb(0x5C, 0x00, 0x0C),
        outline = rgb(0x2B, 0x6E, 0x7A),
    )

    val MONOCHROME = ThemePalette(
        nameResId = id.nusantara.cctv.R.string.theme_monochrome,
        primary = rgb(0x1A, 0x1A, 0x1A),
        onPrimary = rgb(0xFF, 0xFF, 0xFF),
        primaryContainer = rgb(0x3A, 0x3A, 0x3A),
        onPrimaryContainer = rgb(0xE8, 0xE8, 0xE8),
        secondary = rgb(0x6D, 0x6D, 0x6D),
        tertiary = rgb(0x8A, 0x8A, 0x8A),
        background = rgb(0xFA, 0xFA, 0xFA),
        onBackground = rgb(0x14, 0x14, 0x14),
        surface = rgb(0xEE, 0xEE, 0xEE),
        onSurface = rgb(0x18, 0x18, 0x18),
        surfaceVariant = rgb(0xE2, 0xE2, 0xE2),
        onSurfaceVariant = rgb(0x6E, 0x6E, 0x6E),
        error = rgb(0xC6, 0x28, 0x28),
        errorContainer = rgb(0xFF, 0xEB, 0xEE),
        outline = rgb(0xA1, 0xA1, 0xA1),
    )
}
