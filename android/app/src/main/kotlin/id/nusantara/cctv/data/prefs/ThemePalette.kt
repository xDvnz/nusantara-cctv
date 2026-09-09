package id.nusantara.cctv.data.prefs

// ThemeMode didefinisikan di AppPreferences.kt pada package yang sama.

/** Palet tema kustom untuk aplikasi — semua slot warna wajib diisi. */
data class ThemePalette(
    val nameResId: Int,
    val primary: Long,
    val onPrimary: Long,
    val primaryContainer: Long,
    val onPrimaryContainer: Long,
    val secondary: Long,
    val onSecondary: Long,
    val secondaryContainer: Long,
    val onSecondaryContainer: Long,
    val tertiary: Long,
    val onTertiary: Long,
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

/**
 * Palet tetap ala monitoring center. Kontras teks dijaga minimal WCAG AA (4.5:1)
 * untuk onSurface / onBackground / onSurfaceVariant — tema Monokrom lama gagal
 * karena onSurfaceVariant (0x6E) menyatu dengan permukaan abu.
 */
object PresetPalettes {

    private fun rgb(r: Int, g: Int, b: Int): Long =
        0xFF000000L or ((r and 0xFF).toLong() shl 16) or ((g and 0xFF).toLong() shl 8) or (b and 0xFF).toLong()

    /** CYBER: neon monitoring di atas navy pekat (selalu gelap). */
    val CYBER = ThemePalette(
        nameResId = id.nusantara.cctv.R.string.theme_cyber,
        primary = rgb(0x00, 0xE5, 0xFF),
        onPrimary = rgb(0x00, 0x1A, 0x24),
        primaryContainer = rgb(0x00, 0x3A, 0x4C),
        onPrimaryContainer = rgb(0x83, 0xF3, 0xFF),
        secondary = rgb(0x6D, 0xCC, 0xB8),
        onSecondary = rgb(0x00, 0x20, 0x1C),
        secondaryContainer = rgb(0x00, 0x38, 0x31),
        onSecondaryContainer = rgb(0x8A, 0xF5, 0xE4),
        tertiary = rgb(0xBF, 0x40, 0xD9),
        onTertiary = rgb(0x26, 0x00, 0x2E),
        background = rgb(0x04, 0x0A, 0x10),
        onBackground = rgb(0xE0, 0xF7, 0xFA),
        surface = rgb(0x08, 0x12, 0x1C),
        onSurface = rgb(0xD6, 0xF2, 0xF8),
        surfaceVariant = rgb(0x0C, 0x1C, 0x2A),
        onSurfaceVariant = rgb(0x9D, 0xD5, 0xDE),
        error = rgb(0xFF, 0x54, 0x5A),
        errorContainer = rgb(0x5C, 0x00, 0x0C),
        outline = rgb(0x3E, 0x8A, 0x96),
    )

    /** MONOCHROME terang: putih di atas abu muda, teks hitam pekat. */
    val MONOCHROME = ThemePalette(
        nameResId = id.nusantara.cctv.R.string.theme_monochrome,
        primary = rgb(0x11, 0x11, 0x11),
        onPrimary = rgb(0xFF, 0xFF, 0xFF),
        primaryContainer = rgb(0x2E, 0x2E, 0x2E),
        onPrimaryContainer = rgb(0xF5, 0xF5, 0xF5),
        secondary = rgb(0x44, 0x44, 0x44),
        onSecondary = rgb(0xFF, 0xFF, 0xFF),
        secondaryContainer = rgb(0xE4, 0xE4, 0xE4),
        onSecondaryContainer = rgb(0x1A, 0x1A, 0x1A),
        tertiary = rgb(0x55, 0x55, 0x55),
        onTertiary = rgb(0xFF, 0xFF, 0xFF),
        background = rgb(0xFF, 0xFF, 0xFF),
        onBackground = rgb(0x11, 0x11, 0x11),
        surface = rgb(0xFF, 0xFF, 0xFF),
        onSurface = rgb(0x11, 0x11, 0x11),
        surfaceVariant = rgb(0xEE, 0xEE, 0xEE),
        onSurfaceVariant = rgb(0x3A, 0x3A, 0x3A),
        error = rgb(0xB3, 0x26, 0x1E),
        errorContainer = rgb(0xF9, 0xDE, 0xDC),
        outline = rgb(0x8A, 0x8A, 0x8A),
    )

    /** MONOCHROME gelap: teks putih di atas abu gelap (dipakai saat sistem dark). */
    val MONOCHROME_DARK = ThemePalette(
        nameResId = id.nusantara.cctv.R.string.theme_monochrome,
        primary = rgb(0xEE, 0xEE, 0xEE),
        onPrimary = rgb(0x11, 0x11, 0x11),
        primaryContainer = rgb(0xD4, 0xD4, 0xD4),
        onPrimaryContainer = rgb(0x11, 0x11, 0x11),
        secondary = rgb(0xBB, 0xBB, 0xBB),
        onSecondary = rgb(0x11, 0x11, 0x11),
        secondaryContainer = rgb(0x33, 0x33, 0x33),
        onSecondaryContainer = rgb(0xE6, 0xE6, 0xE6),
        tertiary = rgb(0x99, 0x99, 0x99),
        onTertiary = rgb(0x11, 0x11, 0x11),
        background = rgb(0x0D, 0x0D, 0x0D),
        onBackground = rgb(0xF2, 0xF2, 0xF2),
        surface = rgb(0x14, 0x14, 0x14),
        onSurface = rgb(0xF2, 0xF2, 0xF2),
        surfaceVariant = rgb(0x22, 0x22, 0x22),
        onSurfaceVariant = rgb(0xC9, 0xC9, 0xC9),
        error = rgb(0xFF, 0xB4, 0xAB),
        errorContainer = rgb(0x93, 0x00, 0x0A),
        outline = rgb(0x6E, 0x6E, 0x6E),
    )
}
