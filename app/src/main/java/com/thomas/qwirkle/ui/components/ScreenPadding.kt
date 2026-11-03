package com.thomas.qwirkle.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Zentrale Abstände aller Bildschirme. Hier ändern, wirkt überall. */
object ScreenLayout {
    /** Zusätzlicher Abstand unter der Statusleiste (zum oberen Bildschirmrand). */
    val TopExtra: Dp = 12.dp
}

/**
 * Standard-Rand für jeden Bildschirm: Systemleisten und Tastatur plus
 * [ScreenLayout.TopExtra] oben. Ersetzt safeDrawingPadding().
 */
fun Modifier.screenPadding(): Modifier =
    this.safeDrawingPadding().padding(top = ScreenLayout.TopExtra)
