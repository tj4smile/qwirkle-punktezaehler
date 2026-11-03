package com.thomas.qwirkle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.thomas.qwirkle.navigation.AppNavigation
import com.thomas.qwirkle.ui.theme.QwirkleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = (application as QwirkleApp).settings
        setContent {
            val preferDark by settings.darkModePreferred.collectAsState()
            // System dunkel -> immer dunkel. System hell -> der Nutzer entscheidet.
            val darkTheme = isSystemInDarkTheme() || preferDark
            QwirkleTheme(darkTheme = darkTheme) {
                AppNavigation()
            }
        }
    }
}
