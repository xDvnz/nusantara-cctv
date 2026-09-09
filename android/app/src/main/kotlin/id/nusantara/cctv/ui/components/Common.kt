package id.nusantara.cctv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.nusantara.cctv.R
import id.nusantara.cctv.ui.theme.Shapes
import id.nusantara.cctv.ui.theme.Spacing
import id.nusantara.cctv.ui.theme.StatusColors

@Composable
fun StatusDot(status: String, modifier: Modifier = Modifier, enablePulse: Boolean = true) {
    val color = when (status) {
        "ONLINE" -> StatusColors.live
        "OFFLINE", "TIMEOUT", "INVALID_STREAM", "MOVED" -> StatusColors.offline
        "AUTH_REQUIRED" -> StatusColors.degraded
        else -> StatusColors.unknown
    }
    // D1: pulse halus hanya ONLINE (alpha 1<->0.35, 1s) — aman, bukan flicker
    val alpha = if (status == "ONLINE" && enablePulse) {
        val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "livePulse")
        val a by t.animateFloat(
            initialValue = 1f,
            targetValue = 0.35f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(
                    1000, easing = androidx.compose.animation.core.FastOutSlowInEasing,
                ),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
            ),
            label = "livePulseAlpha",
        )
        a
    } else {
        1f
    }
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(10.dp)
            .graphicsLayer { this.alpha = alpha }
            .background(color, Shapes.small),
    )
}

@Composable
fun EmptyState(icon: ImageVector, title: String, description: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(Spacing.xxxl), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun OfflineBanner(visible: Boolean) {
    if (!visible) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(Icons.Filled.CloudOff, contentDescription = null, modifier = Modifier.size(Spacing.lg))
        Text(stringResource(R.string.offline_banner), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ErrorRetry(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
fun EmptyListState() {
    EmptyState(Icons.Filled.Inbox, "Belum ada data", "Katalog kamera kosong atau belum dimuat.")
}
