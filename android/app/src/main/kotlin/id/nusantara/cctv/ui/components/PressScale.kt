package id.nusantara.cctv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Umpan balik sentuh: scale mengecil saat ditekan. Satu Float animation saja —
 * ringan, tanpa layout pass. Pasangkan dengan clickable yang memakai
 * [interactionSource] yang sama (parameter [rememberPressInteraction]).
 */
class PressState {
    val interaction = MutableInteractionSource()

    @Composable
    fun modifier(pressedScale: Float = 0.96f): Modifier {
        val pressed by interaction.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed) pressedScale else 1f,
            animationSpec = tween(durationMillis = 120),
            label = "pressScale",
        )
        return Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    }
}

@Composable
fun rememberPressState(): PressState = remember { PressState() }
