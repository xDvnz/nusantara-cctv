package id.nusantara.cctv.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.appPrefs by preferencesDataStore("settings")

/** Mode tampilan aplikasi. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Bahasa antarmuka. */
enum class AppLocale(val tag: String) {
    ID("id"),
    EN("en");

    companion object {
        fun fromTag(tag: String): AppLocale = entries.firstOrNull { it.tag == tag } ?: ID
    }
}

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val locale: AppLocale = AppLocale.ID,
    val remoteCatalogUrl: String = "",
)

/**
 * Preferensi aplikasi (tema, bahasa, URL katalog remote).
 * Satu DataStore dibagi semua konsumen — hindari duplikasi instance.
 */
class AppPreferencesRepository(private val context: Context) {

    private val KEY_THEME = stringPreferencesKey("theme_mode")
    private val KEY_LOCALE = stringPreferencesKey("locale")
    private val KEY_CATALOG_URL = stringPreferencesKey("remote_catalog_url")

    val preferences: Flow<AppPreferences> = context.appPrefs.data.map { p ->
        AppPreferences(
            themeMode = p[KEY_THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            locale = p[KEY_LOCALE]?.let { AppLocale.fromTag(it) } ?: AppLocale.ID,
            remoteCatalogUrl = p[KEY_CATALOG_URL].orEmpty(),
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.appPrefs.edit { it[KEY_THEME] = mode.name }
    }

    suspend fun setLocale(locale: AppLocale) {
        context.appPrefs.edit { it[KEY_LOCALE] = locale.tag }
    }

    suspend fun setRemoteCatalogUrl(url: String) {
        context.appPrefs.edit { it[KEY_CATALOG_URL] = url }
    }

    suspend fun snapshot(): AppPreferences = preferences.first()
}
