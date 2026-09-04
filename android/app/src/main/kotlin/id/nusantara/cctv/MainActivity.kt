package id.nusantara.cctv

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import id.nusantara.cctv.data.prefs.AppPreferences
import id.nusantara.cctv.ui.AppRoot
import id.nusantara.cctv.ui.theme.NusantaraTheme
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = (application as CctvApp).container.preferencesRepository

        // Terapkan bahasa tersimpan (persist via autoStoreLocales < API 33).
        lifecycleScope.launch {
            prefs.preferences.collect { p ->
                val wanted = LocaleListCompat.forLanguageTags(p.locale.tag)
                if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != wanted.toLanguageTags()) {
                    AppCompatDelegate.setApplicationLocales(wanted)
                }
            }
        }

        setContent {
            val prefsState by prefs.preferences.collectAsState(initial = AppPreferences())
            NusantaraTheme(themeMode = prefsState.themeMode) {
                AppRoot()
            }
        }
    }
}
