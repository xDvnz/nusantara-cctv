package id.nusantara.cctv.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import id.nusantara.cctv.ui.theme.Spacing

/**
 * D2: Shimmer modifier — gradient sweep kiri ke kanan untuk skeleton placeholder.
 * Hanya aktif saat dipasang di composable yang sedang tampil.
 */
fun Modifier.shimmer(): Modifier = composed {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer",
    )
    val base = MaterialTheme.colorScheme.onSurfaceVariant
    background(
        brush = Brush.linearGradient(
            colors = listOf(
                base.copy(alpha = 0.12f),
                base.copy(alpha = 0.28f),
                base.copy(alpha = 0.12f),
            ),
            start = Offset(translateAnim - 300f, 0f),
            end = Offset(translateAnim, 0f),
        ),
    )
}

/** Placeholder bentuk garis untuk skeleton. */
@Composable
private fun SkeletonBar(widthFraction: Float, height: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth(widthFraction)
            .height(height.dp)
            .clip(CircleShape)
            .shimmer(),
    )
}

/** D2: Skeleton meniru bentuk CameraCard (dot + nama + lokasi + metadata). */
@Composable
fun CameraCardSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .shimmer(),
                )
                Spacer(Modifier.width(Spacing.sm))
                SkeletonBar(0.7f, 18)
            }
            Spacer(Modifier.height(Spacing.sm))
            SkeletonBar(0.5f, 14)
            Spacer(Modifier.height(Spacing.xs))
            SkeletonBar(0.4f, 12)
        }
    }
}
