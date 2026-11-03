package com.thomas.qwirkle.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.thomas.qwirkle.QwirkleApp
import com.thomas.qwirkle.data.repository.GameRepository

/** Holt das zentrale Repository aus der Application-Klasse (manuelle DI). */
@Composable
fun rememberRepository(): GameRepository {
    val app = LocalContext.current.applicationContext as QwirkleApp
    return app.repository
}
