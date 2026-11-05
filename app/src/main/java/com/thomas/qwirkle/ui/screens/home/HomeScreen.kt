package com.thomas.qwirkle.ui.screens.home

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thomas.qwirkle.R
import com.thomas.qwirkle.data.settings.AppSettings
import com.thomas.qwirkle.ui.components.QwirkleStones
import com.thomas.qwirkle.ui.components.rememberRepository
import com.thomas.qwirkle.ui.components.rememberSettings
import com.thomas.qwirkle.ui.theme.GamesChipContainerDark
import com.thomas.qwirkle.ui.theme.GamesChipContainerLight
import com.thomas.qwirkle.ui.theme.GamesChipContentDark
import com.thomas.qwirkle.ui.theme.GamesChipContentLight
import com.thomas.qwirkle.ui.components.screenPadding
import com.thomas.qwirkle.util.BackupReminder
import com.thomas.qwirkle.util.Formatting
import com.thomas.qwirkle.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onNewGame: () -> Unit,
    onStatistics: () -> Unit,
    onPlayers: () -> Unit,
    onData: () -> Unit,
    onInfo: () -> Unit,
    onResumeGame: (gameId: Long) -> Unit,
    settings: AppSettings = rememberSettings(),
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(rememberRepository()))
) {
    val lastGame by viewModel.lastGame.collectAsState()
    val gameCount by viewModel.gameCount.collectAsState()
    val unfinishedGame by viewModel.unfinishedGame.collectAsState()
    val runningInfo by viewModel.runningInfo.collectAsState()
    val preferDark by settings.darkModePreferred.collectAsState()
    val lastBackupAt by settings.lastBackupAt.collectAsState()
    val gamesAtBackup by settings.gamesAtBackup.collectAsState()
    val snoozedUntil by settings.backupSnoozedUntil.collectAsState()
    val systemDark = isSystemInDarkTheme()

    var confirmNewGame by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    // Zeitpunkt beim Öffnen der Startseite genügt für die Erinnerung
    val showBackupReminder = remember(lastBackupAt, gamesAtBackup, snoozedUntil, gameCount) {
        BackupReminder.shouldRemind(
            now = System.currentTimeMillis(),
            lastBackupAt = lastBackupAt,
            gamesAtBackup = gamesAtBackup,
            gameCount = gameCount,
            snoozedUntil = snoozedUntil
        )
    }
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .screenPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo: sechs Spielsteine mit Symbolen
            QwirkleStones()
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )

            unfinishedGame?.let { game ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            stringResource(
                                if (game.isTestGame) R.string.home_running_title_test
                                else R.string.home_running_title
                            ),
                            style = MaterialTheme.typography.titleLarge
                        )
                        // Höchstens zwei Infozeilen
                        val info = runningInfo
                        val players = stringResource(R.string.home_running_players, game.playerCount)
                        val line1 = if (info == null || info.rounds == 0) {
                            players
                        } else {
                            players + " · " + pluralStringResource(
                                R.plurals.home_running_rounds, info.rounds, info.rounds
                            )
                        }
                        val line2 = when {
                            info == null -> null
                            info.rounds == 0 -> stringResource(R.string.home_running_no_round)
                            else -> {
                                val lead = if (info.isTie) {
                                    stringResource(R.string.home_running_tie, info.leaderPoints)
                                } else {
                                    stringResource(
                                        R.string.home_running_leader,
                                        info.leaderName.orEmpty(),
                                        info.leaderPoints
                                    )
                                }
                                lead + " · " + stringResource(R.string.home_running_total, info.totalPoints)
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(line1, style = MaterialTheme.typography.bodyLarge)
                            if (line2 != null) {
                                Text(line2, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onResumeGame(game.id) }) {
                                Text(stringResource(R.string.home_resume))
                            }
                            TextButton(onClick = { confirmDiscard = true }) {
                                Text(stringResource(R.string.common_discard))
                            }
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(R.string.home_last_game),
                        style = MaterialTheme.typography.titleLarge
                    )
                    val last = lastGame
                    val body = MaterialTheme.typography.bodyLarge
                    if (last == null) {
                        Text(stringResource(R.string.home_no_game), style = body)
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                stringResource(R.string.home_last_date, Formatting.date(last.date)),
                                style = body
                            )
                            Text(
                                stringResource(R.string.home_running_players, last.playerCount),
                                style = body
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                stringResource(R.string.home_last_points, last.totalPoints),
                                style = body
                            )
                            Text(
                                stringResource(R.string.home_last_qwirkles, last.totalQwirkles),
                                style = body
                            )
                        }
                    }
                    // Anzahl gespielter Partien, hervorgehoben
                    val darkSurface = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    Surface(
                        color = if (darkSurface) GamesChipContainerDark else GamesChipContainerLight,
                        contentColor = if (darkSurface) GamesChipContentDark else GamesChipContentLight,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            stringResource(R.string.home_games_played, gameCount),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            if (showBackupReminder) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            stringResource(R.string.home_backup_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            stringResource(
                                if (lastBackupAt <= 0L) R.string.home_backup_never
                                else R.string.home_backup_old
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onData) {
                                Text(stringResource(R.string.home_backup_now))
                            }
                            TextButton(onClick = {
                                settings.snoozeBackupReminder(
                                    BackupReminder.snoozeUntil(System.currentTimeMillis())
                                )
                            }) { Text(stringResource(R.string.home_backup_later)) }
                        }
                    }
                }
            }

            Button(
                onClick = { if (unfinishedGame != null) confirmNewGame = true else onNewGame() },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text(stringResource(R.string.home_new_game), style = MaterialTheme.typography.titleMedium) }

            OutlinedButton(onClick = onStatistics, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(stringResource(R.string.home_statistics), style = MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(onClick = onPlayers, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(stringResource(R.string.home_players), style = MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(onClick = onData, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(stringResource(R.string.home_data), style = MaterialTheme.typography.titleMedium)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.settings_dark_theme),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (systemDark) {
                        Text(
                            stringResource(R.string.settings_dark_theme_system),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                // Bei dunklem System ist der Schalter fest an und gesperrt
                Switch(
                    checked = systemDark || preferDark,
                    onCheckedChange = { settings.setDarkModePreferred(it) },
                    enabled = !systemDark
                )
            }

            // Fußzeile: Info, Version und Autor dicht beieinander
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                TextButton(onClick = onInfo) {
                    Text(stringResource(R.string.home_info))
                }
                Text(
                    stringResource(R.string.home_version, version),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    stringResource(R.string.home_author, stringResource(R.string.author_name)),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    if (confirmNewGame) {
        AlertDialog(
            onDismissRequest = { confirmNewGame = false },
            title = { Text(stringResource(R.string.home_confirm_new_title)) },
            text = { Text(stringResource(R.string.home_confirm_new_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmNewGame = false
                    onNewGame()
                }) { Text(stringResource(R.string.home_new_game)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmNewGame = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.home_confirm_discard_title)) },
            text = { Text(stringResource(R.string.home_confirm_discard_text)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.discardUnfinishedGame()
                    confirmDiscard = false
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
