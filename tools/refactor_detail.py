"""B3+D4: restructure CameraDetailScreen (progressive disclosure + quick actions)."""
import re

P = 'app/src/main/kotlin/id/nusantara/cctv/ui/detail/CameraDetailScreen.kt'
s = open(P, encoding='utf-8').read()

start_marker = '        Column(\n            modifier = Modifier\n                .fillMaxSize()\n                .verticalScroll(rememberScrollState())'
end_marker = '@Composable\nfun PlayerSurface'

i = s.index(start_marker)
j = s.index(end_marker)

new_block = '''        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // ===== Quick actions row (D4): Save | Share | Fullscreen | Refresh =====
            val context = androidx.compose.ui.platform.LocalContext.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                OutlinedButton(
                    onClick = { if (cam != null) vm.toggleFavorite() },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = Spacing.xs),
                ) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(stringResource(if (isFavorite) R.string.action_saved else R.string.action_save), maxLines = 1)
                }
                OutlinedButton(
                    onClick = { cam?.let { shareCamera(it, context) } },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = Spacing.xs),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.action_share), maxLines = 1)
                }
                OutlinedButton(
                    onClick = onFullscreen,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = Spacing.xs),
                ) {
                    Icon(Icons.Filled.Fullscreen, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.fullscreen_button), maxLines = 1)
                }
                OutlinedButton(
                    onClick = { cam?.let(vm::refreshStatus) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = Spacing.xs),
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.action_refresh), maxLines = 1)
                }
            }

            // ===== Essential info (selalu tampil) =====
            Column(
                modifier = Modifier.padding(horizontal = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(cam.status)
                    Text(
                        "  ${cam.status}" + (cam.lastChecked?.let {
                            " \\u2022 " + stringResource(R.string.checked_at, it)
                        } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(cam.cameraName, style = MaterialTheme.typography.titleLarge)
                if (!cam.district.isNullOrBlank()) {
                    Text(
                        cam.district,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text("${cam.cityRegency} \\u2022 ${cam.province}", style = MaterialTheme.typography.bodyMedium)
                Surface(
                    shape = Shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        cam.operator,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                    )
                }
            }

            // ===== Technical details (collapsible, B3) =====
            var showTechnical by remember { mutableStateOf(false) }
            Column(Modifier.padding(horizontal = Spacing.lg)) {
                TextButton(
                    onClick = { showTechnical = !showTechnical },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            if (showTechnical) R.string.hide_technical_details
                            else R.string.show_technical_details,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                AnimatedVisibility(visible = showTechnical) {
                    Column(
                        modifier = Modifier.padding(top = Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        MetadataRow(stringResource(R.string.metadata_stream_type), cam.streamType)
                        MetadataRow(
                            stringResource(R.string.metadata_stream_url),
                            cam.streamUrl.take(50) + if (cam.streamUrl.length > 50) "\\u2026" else "",
                        )
                        MetadataRow(stringResource(R.string.metadata_source), cam.sourceName)
                        MetadataRow(stringResource(R.string.metadata_portal_url), cam.sourceUrl)
                        if (cam.latitude != null && cam.longitude != null) {
                            MetadataRow(
                                stringResource(R.string.metadata_coordinates),
                                "%.6f, %.6f".format(cam.latitude, cam.longitude),
                            )
                        }
                        if (cam.confidenceScore > 0) {
                            MetadataRow(
                                stringResource(R.string.metadata_confidence),
                                "%.0f%%".format(cam.confidenceScore * 100),
                            )
                        }
                        if (cam.publicIdentifier.isNotBlank()) {
                            MetadataRow(stringResource(R.string.metadata_camera_id), cam.publicIdentifier)
                        }
                        Text(
                            cam.termsOfUse,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // ===== Buka di peta =====
            if (cam.latitude != null && cam.longitude != null) {
                OutlinedButton(
                    onClick = onOpenMap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                ) {
                    Icon(Icons.Filled.Map, contentDescription = null)
                    Spacer(Modifier.width(Spacing.sm))
                    Text(stringResource(R.string.open_in_map))
                }
            }
            Spacer(Modifier.height(Spacing.xl))
        }
    }
}

private fun shareCamera(camera: Camera, context: android.content.Context) {
    val shareText = buildString {
        appendLine("CCTV: ${camera.cameraName}")
        appendLine("${camera.cityRegency}, ${camera.province}")
        appendLine(camera.streamUrl)
    }
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_SUBJECT, "CCTV: ${camera.cameraName}")
        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(android.content.Intent.createChooser(intent, null))
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

'''

new_block = new_block.replace('\\u2022', '\u2022').replace('\\u2026', '\u2026')
s = s[:i] + new_block + s[j:]

# imports tambahan
extra_imports = '''import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Surface
import id.nusantara.cctv.ui.theme.Shapes
'''
for line in extra_imports.strip().split('\n'):
    if line not in s:
        m = re.search(r'^import id\.nusantara', s, re.M)
        s = s[:m.start()] + line + '\n' + s[m.start():]

open(P, 'w', encoding='utf-8').write(s)
print('detail restructured')
