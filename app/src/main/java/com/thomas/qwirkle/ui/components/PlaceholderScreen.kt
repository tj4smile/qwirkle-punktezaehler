package com.thomas.qwirkle.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Vorläufiger Bildschirm zum Testen der Navigation.
 * Wird in Phase 5 Schritt für Schritt durch echte Bildschirme ersetzt.
 */
@Composable
fun PlaceholderScreen(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: List<Pair<String, () -> Unit>> = emptyList()
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (onBack != null) {
                TextButton(onClick = onBack) { Text("← Zurück") }
            }
            Text(text = title, style = MaterialTheme.typography.headlineMedium)
            actions.forEach { (label, action) ->
                Button(onClick = action, modifier = Modifier.fillMaxWidth()) {
                    Text(label)
                }
            }
        }
    }
}
