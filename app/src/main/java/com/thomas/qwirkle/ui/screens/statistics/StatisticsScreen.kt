package com.thomas.qwirkle.ui.screens.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thomas.qwirkle.R
import com.thomas.qwirkle.ui.components.BackButton
import com.thomas.qwirkle.ui.components.PlayerColorDot
import com.thomas.qwirkle.ui.components.rememberRepository
import com.thomas.qwirkle.ui.components.screenPadding
import com.thomas.qwirkle.util.Formatting
import com.thomas.qwirkle.util.PlayerStats
import com.thomas.qwirkle.viewmodel.StatisticsViewModel

@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    viewModel: StatisticsViewModel = viewModel(
        factory = StatisticsViewModel.factory(rememberRepository())
    )
) {
    val stats by viewModel.stats.collectAsState()
    val games by viewModel.games.collectAsState()
    val playerStats by viewModel.playerStats.collectAsState()
    var deletingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val deletingGame = games.firstOrNull { it.id == deletingId }

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
                    stringResource(R.string.stats_title),
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                stats?.let { s ->
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                StatLine(stringResource(R.string.stats_games), s.totalGames.toString())
                                StatLine(stringResource(R.string.stats_points), s.totalPoints.toString())
                                StatLine(stringResource(R.string.stats_qwirkles), s.totalQwirkles.toString())
                                StatLine(
                                    stringResource(R.string.stats_avg_points),
                                    Formatting.decimal(s.averagePoints)
                                )
                                StatLine(
                                    stringResource(R.string.stats_avg_qwirkles),
                                    Formatting.decimal(s.averageQwirkles)
                                )
                            }
                        }
                    }
                }

                stats?.let { s ->
                    if (s.totalGames > 0) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        stringResource(R.string.stats_records),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    StatLine(
                                        stringResource(R.string.stats_max_points),
                                        s.maxPoints.toString()
                                    )
                                    StatLine(
                                        stringResource(R.string.stats_max_qwirkles),
                                        s.maxQwirkles.toString()
                                    )
                                }
                            }
                        }
                    }
                }

                if (playerStats.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                stringResource(R.string.pstats_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                stringResource(R.string.pstats_hint),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    items(playerStats, key = { "player-" + it.player.id }) { PlayerStatsCard(it) }
                }

                item {
                    Text(
                        stringResource(R.string.stats_list_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                if (games.isEmpty()) {
                    item { Text(stringResource(R.string.stats_list_empty)) }
                } else {
                    items(games, key = { it.id }) { game ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(
                                        R.string.stats_game_line,
                                        Formatting.date(game.date),
                                        game.totalPoints
                                    ),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    stringResource(
                                        R.string.stats_game_detail,
                                        game.playerCount,
                                        game.totalQwirkles
                                    ),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            TextButton(onClick = { deletingId = game.id }) {
                                Text(stringResource(R.string.common_delete))
                            }
                        }
                    }
                }
            }
        }
    }

    deletingGame?.let { game ->
        AlertDialog(
            onDismissRequest = { deletingId = null },
            title = { Text(stringResource(R.string.stats_delete_title)) },
            text = { Text(stringResource(R.string.stats_delete_text, Formatting.date(game.date))) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGame(game.id)
                    deletingId = null
                }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deletingId = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PlayerStatsCard(stats: PlayerStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlayerColorDot(colorIndex = stats.player.colorIndex)
                Text(stats.player.name, style = MaterialTheme.typography.titleMedium)
            }
            StatLine(stringResource(R.string.pstats_games), stats.games.toString())
            StatLine(
                stringResource(R.string.pstats_wins),
                stringResource(
                    R.string.pstats_wins_value,
                    stats.wins,
                    Formatting.percent(stats.winRate)
                )
            )
            StatLine(
                stringResource(R.string.pstats_avg_points),
                Formatting.decimal(stats.averagePoints)
            )
            StatLine(stringResource(R.string.pstats_best_points), stats.bestPoints.toString())
            StatLine(
                stringResource(R.string.pstats_qwirkles),
                stringResource(
                    R.string.pstats_qwirkles_value,
                    stats.totalQwirkles,
                    Formatting.decimal(stats.averageQwirkles)
                )
            )
        }
    }
}
