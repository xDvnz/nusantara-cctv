package id.nusantara.cctv.ui.about

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import id.nusantara.cctv.BuildConfig
import id.nusantara.cctv.R
import id.nusantara.cctv.data.prefs.AppLocale
import id.nusantara.cctv.data.prefs.ThemeMode
import id.nusantara.cctv.ui.appContainer
import id.nusantara.cctv.ui.factoryOf

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutScreen() {
    val vm: AboutViewModel = viewModel(factory = factoryOf { extras ->
        val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
            as android.app.Application
        val container = extras.appContainer
        AboutViewModel(app, container.preferencesRepository, container.catalogRepository, container.updateChecker)
    })
    val state by vm.state.collectAsState()
    val version by vm.version.collectAsState()
    val sources by vm.sources.collectAsState()
    val themeMode by vm.themeMode.collectAsState()
    val locale by vm.locale.collectAsState()
    val context = LocalContext.current
    var sourcesExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text(stringResource(R.string.about_title), style = MaterialTheme.typography.headlineSmall) }

        // Aplikasi + versi
        item {
            AboutCard {
                Text(stringResource(R.string.about_app_section), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.about_version) + ": " + BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(
                        R.string.catalog_version_label,
                        version?.version?.toString() ?: "-",
                        version?.generatedAt?.ifEmpty { "-" } ?: "-",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Tampilan: tema + bahasa
        item {
            AboutCard {
                Text(stringResource(R.string.about_appearance_section), style = MaterialTheme.typography.titleMedium)

                Text(
                    stringResource(R.string.theme_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = themeMode == ThemeMode.SYSTEM,
                        onClick = { vm.setThemeMode(ThemeMode.SYSTEM) },
                        label = { Text(stringResource(R.string.theme_material_you)) },
                    )
                    FilterChip(
                        selected = themeMode == ThemeMode.LIGHT,
                        onClick = { vm.setThemeMode(ThemeMode.LIGHT) },
                        label = { Text(stringResource(R.string.theme_light)) },
                    )
                    FilterChip(
                        selected = themeMode == ThemeMode.DARK,
                        onClick = { vm.setThemeMode(ThemeMode.DARK) },
                        label = { Text(stringResource(R.string.theme_dark)) },
                    )
                    FilterChip(
                        selected = themeMode == ThemeMode.CYBER,
                        onClick = { vm.setThemeMode(ThemeMode.CYBER) },
                        label = { Text(stringResource(R.string.theme_cyber)) },
                    )
                    FilterChip(
                        selected = themeMode == ThemeMode.MONOCHROME,
                        onClick = { vm.setThemeMode(ThemeMode.MONOCHROME) },
                        label = { Text(stringResource(R.string.theme_monochrome)) },
                    )
                }

                Text(
                    stringResource(R.string.language_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = locale == AppLocale.ID,
                        onClick = {
                            vm.setLocale(AppLocale.ID)
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("id"))
                        },
                        label = { Text(stringResource(R.string.language_id)) },
                    )
                    FilterChip(
                        selected = locale == AppLocale.EN,
                        onClick = {
                            vm.setLocale(AppLocale.EN)
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                        },
                        label = { Text(stringResource(R.string.language_en)) },
                    )
                }
            }
        }

        // Pembaruan
        item {
            AboutCard {
                Text(stringResource(R.string.update_section), style = MaterialTheme.typography.titleMedium)
                Button(onClick = vm::checkForUpdate, enabled = !state.updateChecking) {
                    Text(
                        if (state.updateChecking) stringResource(R.string.update_checking)
                        else stringResource(R.string.check_update_button),
                    )
                }
                if (state.updateChecked) {
                    val info = state.updateAvailable
                    if (info == null) {
                        Text(
                            stringResource(R.string.update_latest_ok, BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            stringResource(R.string.update_dialog_message, info.version, BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        TextButton(onClick = {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(info.url),
                            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            runCatching { context.startActivity(intent) }
                        }) { Text(stringResource(R.string.download_button)) }
                    }
                }
            }
        }

        // Katalog remote (opsional, penjelasan + contoh host)
        item {
            AboutCard {
                Text(stringResource(R.string.about_catalog_section), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.catalog_remote_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = state.catalogUrl,
                    onValueChange = vm::onCatalogUrlChange,
                    label = { Text(stringResource(R.string.catalog_url_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Button(onClick = vm::saveAndSync, enabled = !state.syncing) {
                    Text(
                        if (state.syncing) stringResource(R.string.syncing)
                        else stringResource(R.string.save_sync_button),
                    )
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

        // Sumber data & atribusi — SATU kartu ringkas, detail bisa dibuka
        item {
            AboutCard {
                Text(stringResource(R.string.about_sources_section), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(
                        R.string.about_sources_count,
                        sources.size,
                        vm.cameraCount.collectAsState(initial = 0).value,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.about_sources_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { sourcesExpanded = !sourcesExpanded }) {
                    Text(
                        if (sourcesExpanded) stringResource(R.string.sources_collapse)
                        else stringResource(R.string.sources_expand),
                    )
                }
                AnimatedVisibility(visible = sourcesExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        sources.forEach { source ->
                            Column {
                                Text(source.sourceName, style = MaterialTheme.typography.titleMedium)
                                Text(source.operator, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    source.sourceUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Pengembang
        item {
            AboutCard {
                Text(stringResource(R.string.about_developer_section), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.about_developer_name), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.about_developer_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.about_repo) + ": github.com/xDvnz/nusantara-cctv",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** Kartu standar — semua kartu About memakai ini agar lebar/elevasi konsisten. */
@Composable
private fun AboutCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}
