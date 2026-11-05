package com.thomas.qwirkle.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Neutrales Grau für nicht gewählte Kacheln: hebt sich vom hellen und vom dunklen Design ab. */
val TileGray = Color(0xFF757575)

/** Schwarz oder Weiß, je nachdem, was auf dem Hintergrund besser lesbar ist. */
fun contrastColor(background: Color): Color =
    if (background.luminance() > 0.4f) Color.Black else Color.White

/**
 * Kachel zum Auswählen. Nicht gewählt: grau. Gewählt: in [selectedColor]
 * mit kontrastreicher Schrift. [badge] zeigt z. B. die Spielreihenfolge.
 */
@Composable
fun SelectableTile(
    text: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    badge: String? = null
) {
    val background = if (selected) selectedColor else TileGray
    val content = if (selected) contrastColor(selectedColor) else Color.White
    Surface(
        color = background,
        contentColor = content,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .semantics { this.selected = selected }
            .clickable(enabled = enabled, role = Role.Checkbox, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (badge != null) {
                Text(
                    badge,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }
        }
    }
}
