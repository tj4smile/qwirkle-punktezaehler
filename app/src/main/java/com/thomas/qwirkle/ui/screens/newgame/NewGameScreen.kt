package com.thomas.qwirkle.ui.screens.newgame

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thomas.qwirkle.R
import com.thomas.qwirkle.ui.components.BackButton
import com.thomas.qwirkle.ui.components.SelectableTile
import com.thomas.qwirkle.ui.theme.playerColor
import com.thomas.qwirkle.ui.components.rememberRepository
import com.thomas.qwirkle.ui.components.screenPadding
import com.thomas.qwirkle.util.GameRules
import com.thomas.qwirkle.viewmodel.NewGameViewModel

@Composable
fun NewGameScreen(
    onBack: () -> Unit,
    onManagePlayers: () -> Unit,
    onGameStarted: (gameId: Long) -> Unit,
    viewModel: NewGameViewModel = viewModel(factory = NewGameViewModel.factory(rememberRepository()))
) {
    val players by viewModel.players.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val isTestGame by viewModel.isTestGame.collectAsState()
    val colors by viewModel.colors.collectAsState()

    val selectedCount = selectedIds.size
    val isFull = selectedCount >= GameRules.MAX_PLAYERS
    val canStart = selectedCount >= GameRules.MIN_PLAYERS

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
                    stringResource(R.string.newgame_title),
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            if (players.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        stringResource(R.string.newgame_no_players, GameRules.MIN_PLAYERS),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(onClick = onManagePlayers) {
                        Text(stringResource(R.string.players_title))
                    }
                }
            } else {
                // Kacheln (zwei pro Zeile) und Optionen scrollen zusammen, der Start-Knopf bleibt unten
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f).padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(players, key = { it.id }) { player ->
                        val checked = player.id in selectedIds
                        SelectableTile(
                            text = player.name,
                            selected = checked,
                            selectedColor = playerColor(colors[player.id] ?: player.colorIndex),
                            enabled = checked || !isFull,
                            badge = if (checked) "${selectedIds.indexOf(player.id) + 1}" else null,
                            onClick = { viewModel.togglePlayer(player.id) }
                        )
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                stringResource(
                                    R.string.newgame_selected,
                                    selectedCount,
                                    GameRules.MAX_PLAYERS
                                ),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                if (selectedCount < GameRules.MIN_PLAYERS) {
                                    stringResource(R.string.newgame_min_hint, GameRules.MIN_PLAYERS)
                                } else {
                                    stringResource(R.string.newgame_order_hint)
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setTestGame(!isTestGame) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = isTestGame, onCheckedChange = null)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        stringResource(R.string.newgame_test_game),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        stringResource(R.string.newgame_test_game_hint),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { viewModel.start(onGameStarted) },
                enabled = canStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(stringResource(R.string.newgame_start))
            }
        }
    }
}
