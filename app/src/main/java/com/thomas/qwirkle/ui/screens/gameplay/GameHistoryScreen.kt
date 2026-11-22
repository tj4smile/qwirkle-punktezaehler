package com.thomas.qwirkle.ui.screens.gameplay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thomas.qwirkle.R
import com.thomas.qwirkle.data.entity.Player
import com.thomas.qwirkle.ui.components.BackButton
import com.thomas.qwirkle.ui.components.PlayerColorDot
import com.thomas.qwirkle.ui.components.rememberRepository
import com.thomas.qwirkle.ui.components.screenPadding
import com.thomas.qwirkle.util.GameCalculations
import com.thomas.qwirkle.util.Formatting
import com.thomas.qwirkle.util.GameRules
import com.thomas.qwirkle.viewmodel.GameHistoryViewModel

private val LabelWidth = 64.dp

@Composable
fun GameHistoryScreen(
    gameId: Long,
    onAddRound: () -> Unit,
    onEditRound: (roundId: Long) -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit,
    readOnly: Boolean = false, // true = gespeichertes Spiel nur ansehen
    viewModel: GameHistoryViewModel = viewModel(
        factory = GameHistoryViewModel.factory(gameId, rememberRepository())
    )
) {
    val players by viewModel.players.collectAsState()
    val rounds by viewModel.rounds.collectAsState()
    val qwirkleBonus by viewModel.qwirkleBonus.collectAsState()
    val game by viewModel.game.collectAsState()
    var confirmDeleteLast by rememberSaveable { mutableStateOf(false) }

    // Gespeicherte Spiele: Endspielbonus gehört zur Summe
    val endBonusPlayerId = if (readOnly) game?.endBonusPlayerId else null
    val totals = remember(players, rounds, qwirkleBonus, endBonusPlayerId) {
        GameCalculations.playerTotals(
            players, rounds, endBonusPlayerId, qwirkleBonus
        )
    }
    val listState = rememberLazyListState()

    // Neueste Runde sichtbar halten
    LaunchedEffect(rounds.size) {
        if (!readOnly && rounds.isNotEmpty()) listState.animateScrollToItem(rounds.size - 1)
    }

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
                    game?.takeIf { readOnly }?.let {
                        stringResource(R.string.history_title_saved, Formatting.date(it.date))
                    } ?: stringResource(R.string.history_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
            }

            if (qwirkleBonus != GameRules.QWIRKLE_BONUS) {
                Text(
                    stringResource(R.string.variant_alternative_info, qwirkleBonus),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }

            // Kopfzeile und Summenzeile bleiben immer sichtbar
            HeaderRow(players)
            ValueRow(
                label = stringResource(R.string.history_sum),
                values = totals.map { it.points.toString() },
                bold = true
            )
            HorizontalDivider()

            if (rounds.isEmpty()) {
                Text(
                    stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 16.dp)
                )
            } else {
                LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                    items(rounds, key = { it.round.id }) { roundWithScores ->
                        ValueRow(
                            label = stringResource(
                                R.string.history_round,
                                roundWithScores.round.roundNumber
                            ),
                            values = players.map { player ->
                                val score = roundWithScores.scores.find { it.playerId == player.id }
                                if (score == null) {
                                    "–"
                                } else {
                                    GameRules.roundTotal(score.points, score.qwirkles, qwirkleBonus).toString() +
                                        if (score.qwirkles > 0) "★" else ""
                                }
                            },
                            bold = false,
                            onClick = if (readOnly) null else {
                                { onEditRound(roundWithScores.round.id) }
                            }
                        )
                    }
                    item {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                if (qwirkleBonus == GameRules.QWIRKLE_BONUS) {
                                    stringResource(R.string.history_legend)
                                } else {
                                    stringResource(R.string.history_legend_alternative, qwirkleBonus)
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (readOnly) {
                                val bonusPlayer = players.firstOrNull { it.id == endBonusPlayerId }
                                if (bonusPlayer != null) {
                                    Text(
                                        stringResource(
                                            R.string.history_end_bonus,
                                            bonusPlayer.name,
                                            GameRules.END_BONUS
                                        ),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            } else {
                                Text(
                                    stringResource(R.string.history_edit_hint),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            if (!readOnly) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(onClick = onAddRound, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.history_add_round))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { confirmDeleteLast = true },
                            enabled = rounds.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.history_delete_last))
                        }
                        OutlinedButton(
                            onClick = onFinish,
                            enabled = rounds.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.common_finish))
                        }
                    }
                    if (rounds.isEmpty()) {
                        Text(
                            stringResource(R.string.finish_needs_round),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (confirmDeleteLast) {
        AlertDialog(
            onDismissRequest = { confirmDeleteLast = false },
            title = { Text(stringResource(R.string.history_delete_last_title)) },
            text = { Text(stringResource(R.string.history_delete_last_text)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteLastRound()
                    confirmDeleteLast = false
                }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteLast = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

/** Ab dieser Spieleranzahl zeigt die Kopfzeile nur Kürzel (Farbpunkt + zwei Buchstaben). */
private const val ABBREVIATE_FROM_PLAYERS = 5
private const val ABBREVIATION_LENGTH = 2

@Composable
private fun HeaderRow(players: List<Player>) {
    val abbreviate = players.size >= ABBREVIATE_FROM_PLAYERS
    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Spacer(Modifier.width(LabelWidth))
        players.forEach { player ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PlayerColorDot(colorIndex = player.colorIndex, size = 12)
                Text(
                    // Ab 5 Spielern wird es eng: dann nur die ersten zwei Buchstaben
                    if (abbreviate) player.name.take(ABBREVIATION_LENGTH) else player.name,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { contentDescription = player.name }
                )
            }
        }
    }
}

@Composable
private fun ValueRow(
    label: String,
    values: List<String>,
    bold: Boolean,
    onClick: (() -> Unit)? = null
) {
    val weight = if (bold) FontWeight.Bold else FontWeight.Normal
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            label,
            modifier = Modifier.width(LabelWidth),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = weight
        )
        values.forEach { value ->
            Text(
                value,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = weight
            )
        }
    }
}
