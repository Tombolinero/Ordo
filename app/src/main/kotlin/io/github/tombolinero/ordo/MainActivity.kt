package io.github.tombolinero.ordo

import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.mutableStateOf
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.navigation.OrdoNavHost

class MainActivity : ComponentActivity() {
    /** A list to open, e.g. after tapping the widget. Consumed by the navigation host. */
    private val openList = mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openList.value = intent.getStringExtra(EXTRA_LIST_ID)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) openList.value = intent?.getStringExtra(EXTRA_LIST_ID)
        installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            OrdoTheme { OrdoNavHost(openList = openList.value, onListOpened = { openList.value = null }) }
        }
    }

    companion object {
        const val EXTRA_LIST_ID = "io.github.tombolinero.ordo.LIST_ID"
    }
}
