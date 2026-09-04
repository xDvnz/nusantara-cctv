package id.nusantara.cctv.ui.about

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

@Composable
fun AboutScreen() {
    val vm: AboutViewModel = viewModel(factory = factoryOf { extras ->
        val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
            as android.app.Application
        val container = extras.appContainer
        AboutViewModel(app, container.preferencesRepository, container.catalogRepository)
    })
    val state by vm.state.collectAsState()
    val version by vm.version.collectAsState()
    val sources by vm.sources.collectAsState()
    val themeMode by vm.themeMode.collectAsState()
    val locale by vm.locale.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text(stringResource(R.string.about_title), style = MaterialTheme.typography.headlineSmall) }

        // Aplikasi
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.about_app_section), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.about_version) + ": " + BuildConfig.VERSION_NAME,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        stringResource(R.string.catalog_version_label,
                            version?.version?.toString() ?: "-",
                            version?.generatedAt?.ifEmpty { "-" } ?: "-"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Pengembang
        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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

        // Tampilan: tema + bahasa
        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.about_appearance_section), style = MaterialTheme.typography.titleMedium)

                    Text(
                        stringResource(R.string.theme_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = themeMode == ThemeMode.SYSTEM,
                            onClick = { vm.setThemeMode(ThemeMode.SYSTEM) },
                            label = { Text(stringResource(R.string.theme_system)) },
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
                    }

                    Text(
                        stringResource(R.string.language_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        }

        // Katalog (pindahan dari Pengaturan)
        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        }

        // Sumber & atribusi
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.about_sources_section), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.about_sources_note),
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
