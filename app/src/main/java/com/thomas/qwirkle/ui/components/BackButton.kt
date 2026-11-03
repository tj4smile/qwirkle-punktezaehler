package com.thomas.qwirkle.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.thomas.qwirkle.R

/**
 * Zurück-Pfeil. Der Pfeil wird selbst gezeichnet, damit er auf jedem Gerät
 * gleich gut sichtbar ist und keine Icon-Bibliothek nötig ist.
 * Die Klickfläche ist 48 dp groß (empfohlene Mindestgröße).
 */
@Composable
fun BackButton(onClick: () -> Unit) {
    val description = stringResource(R.string.common_back)
    val color = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val stroke = 2.5.dp.toPx()
            val w = size.width
            val h = size.height
            val tip = Offset(w * 0.1f, h * 0.5f)
            drawLine(color, Offset(w * 0.9f, h * 0.5f), tip, stroke, StrokeCap.Round)
            drawLine(color, tip, Offset(w * 0.5f, h * 0.1f), stroke, StrokeCap.Round)
            drawLine(color, tip, Offset(w * 0.5f, h * 0.9f), stroke, StrokeCap.Round)
        }
    }
}
