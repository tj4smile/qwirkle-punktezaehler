package com.thomas.qwirkle.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.thomas.qwirkle.QwirkleApp
import com.thomas.qwirkle.data.settings.AppSettings

/** Holt die App-Einstellungen aus der Application-Klasse. */
@Composable
fun rememberSettings(): AppSettings {
    val app = LocalContext.current.applicationContext as QwirkleApp
    return app.settings
}
