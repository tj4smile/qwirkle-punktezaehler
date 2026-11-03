package com.thomas.qwirkle.ui.screens.newgame

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thomas.qwirkle.R
import com.thomas.qwirkle.ui.components.BackButton
import com.thomas.qwirkle.ui.components.rememberRepository
import com.thomas.qwirkle.ui.components.screenPadding
import com.thomas.qwirkle.ui.theme.playerColor
import com.thomas.qwirkle.util.PlayerColors
import com.thomas.qwirkle.viewmodel.GameSetupViewModel
import com.thomas.qwirkle.viewmodel.SetupEntry

/** Reihenfolge und Farben vor der ersten Runde; legt danach das Spiel an. */
@Composable
fun GameSetupScreen(
    playerIds: List<Long>,
    isTestGame: Boolean,
    onBack: () -> Unit,
    onGameStarted: (gameId: Long) -> Unit,
    viewModel: GameSetupViewModel = viewModel(
        factory = GameSetupViewModel.factory(rememberRepository(), playerIds, isTestGame)
    )
) {
    val entries by viewModel.entries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val adjusted by viewModel.colorsAdjusted.collectAsState()
    val isStarting by viewModel.isStarting.collectAsState()
    var colorDialogFor by rememberSaveable { mutableStateOf<Long?>(null) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .screenPadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BackButton(onClick = onBack)
                Text(
                    stringResource(R.string.setup_title),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            Text(
                stringResource(R.string.setup_hint),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            if (adjusted) {
                Text(
                    stringResource(R.string.setup_colors_adjusted),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(entries, key = { _, e -> e.player.id }) { index, entry ->
                    SetupRow(
                        position = index + 1,
                        entry = entry,
                        canMoveUp = index > 0,
                        canMoveDown = index < entries.lastIndex,
                        onUp = { viewModel.moveUp(index) },
                        onDown = { viewModel.moveDown(index) },
                        onColor = { colorDialogFor = entry.player.id }
                    )
                }
            }

            Button(
                onClick = { viewModel.start(onGameStarted) },
                enabled = !isLoading && !isStarting && entries.size >= 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) { Text(stringResource(R.string.setup_start)) }
        }
    }

    val dialogEntry = entries.firstOrNull { it.player.id == colorDialogFor }
    if (dialogEntry != null) {
        AlertDialog(
            onDismissRequest = { colorDialogFor = null },
            title = { Text(stringResource(R.string.setup_color_title, dialogEntry.player.name)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(PlayerColors.COUNT) { index ->
                            ColorChoice(
                                colorIndex = index,
                                selected = index == dialogEntry.colorIndex,
                                onClick = {
                                    viewModel.setColor(dialogEntry.player.id, index)
                                    colorDialogFor = null
                                }
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.setup_color_swap_hint),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { colorDialogFor = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun SetupRow(
    position: Int,
    entry: SetupEntry,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onColor: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$position.",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(end = 12.dp)
            )
            // Farbpunkt antippen = Farbe ändern
            val colorDescription = stringResource(R.string.setup_change_color, entry.player.name)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onColor)
                    .semantics { contentDescription = colorDescription },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(playerColor(entry.colorIndex))
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
            }
            Text(
                entry.player.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
            ArrowButton(
                up = true,
                enabled = canMoveUp,
                description = stringResource(R.string.setup_move_up, entry.player.name),
                onClick = onUp
            )
            ArrowButton(
                up = false,
                enabled = canMoveDown,
                description = stringResource(R.string.setup_move_down, entry.player.name),
                onClick = onDown
            )
        }
    }
}

/** Gezeichneter Pfeil nach oben/unten (kein Textzeichen, siehe BackButton). */
@Composable
private fun ArrowButton(up: Boolean, enabled: Boolean, description: String, onClick: () -> Unit) {
    val color = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .alpha(if (enabled) 1f else 0.3f)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val stroke = 2.5.dp.toPx()
            val w = size.width
            val h = size.height
            val tipY = if (up) h * 0.2f else h * 0.8f
            val baseY = if (up) h * 0.8f else h * 0.2f
            val tip = Offset(w * 0.5f, tipY)
            drawLine(color, Offset(w * 0.5f, baseY), tip, stroke, StrokeCap.Round)
            drawLine(color, tip, Offset(w * 0.15f, h * 0.5f), stroke, StrokeCap.Round)
            drawLine(color, tip, Offset(w * 0.85f, h * 0.5f), stroke, StrokeCap.Round)
        }
    }
}

@Composable
private fun ColorChoice(colorIndex: Int, selected: Boolean, onClick: () -> Unit) {
    val description = stringResource(R.string.players_color_description, colorIndex + 1)
    var modifier = Modifier
        .size(28.dp)
        .clip(CircleShape)
        .background(playerColor(colorIndex))
    if (selected) {
        modifier = modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
    }
    Box(
        modifier
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
    )
}
