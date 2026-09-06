package id.nusantara.cctv.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.nusantara.cctv.data.model.Camera

@Composable
fun CameraCard(camera: Camera, onClick: (Camera) -> Unit, modifier: Modifier = Modifier) {
    val press = rememberPressState()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(press.modifier())
            .clickable(
                interactionSource = press.interaction,
                indication = androidx.compose.material3.ripple(),
            ) { onClick(camera) },
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusDot(camera.status)
                Text(
                    camera.cameraName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            val place = buildList {
                if (!camera.district.isNullOrBlank()) add(camera.district)
                add(camera.cityRegency)
            }.joinToString(" • ")
            Text(
                place,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (camera.locationName.isNotBlank() && camera.locationName != camera.cameraName) {
                Text(
                    camera.locationName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            Text(
                "${camera.streamType} • ${camera.operator}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
