package com.thomas.qwirkle.ui.screens.results

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thomas.qwirkle.R
import com.thomas.qwirkle.ui.components.BackButton
import com.thomas.qwirkle.ui.components.PlayerColorDot
import com.thomas.qwirkle.ui.components.rememberRepository
import com.thomas.qwirkle.ui.components.screenPadding
import com.thomas.qwirkle.ui.theme.playerColor
import com.thomas.qwirkle.util.PlayerTotal
import com.thomas.qwirkle.viewmodel.ResultViewModel

@Composable
fun ResultScreen(
    gameId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onDiscarded: () -> Unit,
    viewModel: ResultViewModel = viewModel(
        factory = ResultViewModel.factory(gameId, rememberRepository())
    )
) {
    val state by viewModel.state.collectAsState()
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }

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
                    stringResource(R.string.results_title),
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            val current = state
            if (current == null) {
                Text(stringResource(R.string.common_loading), modifier = Modifier.padding(16.dp))
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (current.game.isTestGame) {
                        Text(
                            stringResource(R.string.results_test_game),
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    current.ranking.forEach { (rank, total) ->
                        RankingRow(rank, total)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        stringResource(R.string.results_qwirkles_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    QwirkleBars(current.ranking.map { it.second })
                }

                Button(
                    onClick = { viewModel.save(onSaved) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) { Text(stringResource(R.string.results_save)) }
                TextButton(
                    onClick = { confirmDiscard = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) { Text(stringResource(R.string.results_discard)) }
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.results_discard_title)) },
            text = { Text(stringResource(R.string.results_discard_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    viewModel.discard(onDiscarded)
                }) { Text(stringResource(R.string.common_discard)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun RankingRow(rank: Int, total: PlayerTotal) {
    val weight = if (rank == 1) FontWeight.Bold else FontWeight.Normal
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.results_rank, rank),
            modifier = Modifier.width(32.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = weight
        )
        PlayerColorDot(colorIndex = total.player.colorIndex, size = 20)
        Spacer(Modifier.width(12.dp))
        Text(
            total.player.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = weight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            stringResource(R.string.results_points, total.points),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = weight
        )
    }
}

@Composable
private fun QwirkleBars(totals: List<PlayerTotal>) {
    val max = totals.maxOfOrNull { it.qwirkles } ?: 0
    if (max == 0) {
        Text(
            stringResource(R.string.results_no_qwirkles),
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        totals.forEach { total ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    total.player.name,
                    modifier = Modifier.width(80.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
                Box(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(total.qwirkles.toFloat() / max)
                            .height(18.dp)
                            .background(playerColor(total.player.colorIndex))
                    )
                }
                Text(
                    "${total.qwirkles}",
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
