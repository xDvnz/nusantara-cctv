package id.nusantara.cctv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import id.nusantara.cctv.ui.AppRoot
import id.nusantara.cctv.ui.theme.NusantaraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NusantaraTheme {
                AppRoot()
            }
        }
    }
}
