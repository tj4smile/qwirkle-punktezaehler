package com.thomas.qwirkle.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.thomas.qwirkle.ui.theme.playerColor

/** Farbiger Punkt in der Spielerfarbe, für Listen und Tabellen. */
@Composable
fun PlayerColorDot(colorIndex: Int, size: Int = 20) {
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(playerColor(colorIndex))
    )
}
