package app.spotlightguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.spotlightguard.ui.MainScreen
import app.spotlightguard.ui.SpotlightGuardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SpotlightGuardTheme {
                MainScreen()
            }
        }
    }
}
