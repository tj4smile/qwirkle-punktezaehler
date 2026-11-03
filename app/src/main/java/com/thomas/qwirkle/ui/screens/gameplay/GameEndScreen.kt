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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import com.thomas.qwirkle.ui.components.PlayerColorDot
import com.thomas.qwirkle.ui.components.rememberRepository
import com.thomas.qwirkle.ui.components.screenPadding
import com.thomas.qwirkle.util.GameRules
import com.thomas.qwirkle.viewmodel.GameEndViewModel

@Composable
fun GameEndScreen(
    gameId: Long,
    onBack: () -> Unit,
    onShowResults: () -> Unit,
    viewModel: GameEndViewModel = viewModel(
        factory = GameEndViewModel.factory(gameId, rememberRepository())
    )
) {
    val players by viewModel.players.collectAsState()
    val choice by viewModel.choice.collectAsState()

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
                    stringResource(R.string.gameend_title),
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            Text(
                stringResource(R.string.gameend_question),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(players, key = { it.id }) { player ->
                    ChoiceRow(
                        selected = choice == player.id,
                        onSelect = { viewModel.select(player.id) }
                    ) {
                        PlayerColorDot(colorIndex = player.colorIndex, size = 20)
                        Spacer(Modifier.width(12.dp))
                        Text(player.name, style = MaterialTheme.typography.titleMedium)
                    }
                }
                item {
                    ChoiceRow(
                        selected = choice == GameEndViewModel.NO_BONUS,
                        onSelect = { viewModel.select(GameEndViewModel.NO_BONUS) }
                    ) {
                        Text(
                            stringResource(R.string.gameend_nobody),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            val hasPlayerChoice = choice != null && choice != GameEndViewModel.NO_BONUS
            if (hasPlayerChoice) {
                Text(
                    stringResource(R.string.gameend_bonus, GameRules.END_BONUS),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Button(
                onClick = { viewModel.confirm(onShowResults) },
                enabled = choice != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp)
            ) {
                Text(stringResource(R.string.gameend_show_results))
            }
        }
    }
}

@Composable
private fun ChoiceRow(
    selected: Boolean,
    onSelect: () -> Unit,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        content()
    }
}
