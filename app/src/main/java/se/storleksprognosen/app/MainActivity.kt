package se.storleksprognosen.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import se.storleksprognosen.app.data.prefs.ThemeMode
import se.storleksprognosen.app.ui.navigation.StorleksprognosenNavHost
import se.storleksprognosen.app.ui.theme.StorleksprognosenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = (application as StorleksprognosenApp).container.settings
        setContent {
            val themeMode by settings.themeMode.collectAsStateWithLifecycle()
            val dynamicColor by settings.dynamicColor.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            StorleksprognosenTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
                StorleksprognosenNavHost()
            }
        }
    }
}
