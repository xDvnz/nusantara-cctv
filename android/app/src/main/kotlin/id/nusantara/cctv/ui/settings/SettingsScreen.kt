package id.nusantara.cctv.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import id.nusantara.cctv.ui.appContainer
import id.nusantara.cctv.ui.factoryOf

@Composable
fun SettingsScreen() {
    val vm: SettingsViewModel = viewModel(factory = factoryOf { extras ->
        val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
            as android.app.Application
        SettingsViewModel(app, extras.appContainer.catalogRepository)
    })
    val state by vm.state.collectAsState()
    val version by vm.version.collectAsState()
    val sources by vm.sources.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Pengaturan", style = MaterialTheme.typography.headlineSmall) }

        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Katalog", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Versi: ${version?.version ?: "-"} • Dibuat: ${version?.generatedAt?.ifEmpty { "-" } ?: "-"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Katalog dibundel bersama aplikasi. Isi URL katalog remote (JSON skema sama) " +
                            "untuk memperbarui daftar kamera tanpa update APK. Kosong = hanya seed bundel.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = state.catalogUrl,
                        onValueChange = vm::onCatalogUrlChange,
                        label = { Text("URL katalog remote (opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Button(onClick = vm::saveAndSync, enabled = !state.syncing) {
                        Text(if (state.syncing) "Menyinkronkan..." else "Simpan & sinkron")
                    }
                    state.message?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.isError) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Sumber data & atribusi", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Seluruh stream berasal dari portal publik resmi pemerintah daerah. " +
                            "Aplikasi hanya menampilkan tautan publik; tidak merekam dan tidak " +
                            "mendistribusikan ulang. Hormati ketentuan tiap operator.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        items(sources, key = { it.sourceId }) { source ->
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text(source.sourceName, style = MaterialTheme.typography.titleMedium)
                    Text(source.operator, style = MaterialTheme.typography.bodySmall)
                    Text(
                        source.sourceUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        source.licenseNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
