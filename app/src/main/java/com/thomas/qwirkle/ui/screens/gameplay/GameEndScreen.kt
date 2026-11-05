package com.thomas.qwirkle.ui.screens.gameplay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
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

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(players, key = { it.id }) { player ->
                    SelectableTile(
                        text = player.name,
                        selected = choice == player.id,
                        selectedColor = playerColor(player.colorIndex),
                        onClick = { viewModel.select(player.id) }
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SelectableTile(
                        text = stringResource(R.string.gameend_nobody),
                        selected = choice == GameEndViewModel.NO_BONUS,
                        selectedColor = MaterialTheme.colorScheme.primary,
                        onClick = { viewModel.select(GameEndViewModel.NO_BONUS) }
                    )
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
