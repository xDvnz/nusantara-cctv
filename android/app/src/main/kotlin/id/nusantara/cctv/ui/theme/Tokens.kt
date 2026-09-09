package id.nusantara.cctv.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Design tokens — spacing, shapes, semantic colors.
 * Centralized constants untuk consistency + maintainability.
 */

object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

object Shapes {
    val small = RoundedCornerShape(8.dp)
    val medium = RoundedCornerShape(12.dp)
    val large = RoundedCornerShape(16.dp)
    val pill = RoundedCornerShape(999.dp)
}

/**
 * Status colors — semantic naming untuk camera states.
 * Hardcoded fallback (Material 3 theme tidak define status colors).
 * Bisa override dengan theme-aware logic kalau perlu.
 */
object StatusColors {
    val live = Color(0xFF4CAF50)       // green
    val offline = Color(0xFFEF5350)    // red
    val degraded = Color(0xFFFFB74D)   // amber/orange
    val unknown = Color(0xFF9E9E9E)    // gray
}
