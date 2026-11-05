package com.thomas.qwirkle.ui.screens.scoring

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import com.thomas.qwirkle.ui.theme.GamesChipContainerDark
import com.thomas.qwirkle.ui.theme.GamesChipContainerLight
import com.thomas.qwirkle.ui.theme.GamesChipContentDark
import com.thomas.qwirkle.ui.theme.GamesChipContentLight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.thomas.qwirkle.R
import com.thomas.qwirkle.data.entity.Player
import com.thomas.qwirkle.ui.components.BackButton
import com.thomas.qwirkle.ui.components.PlayerColorDot
import com.thomas.qwirkle.ui.components.rememberRepository
import com.thomas.qwirkle.ui.components.screenPadding
import com.thomas.qwirkle.util.GameRules
import com.thomas.qwirkle.viewmodel.ScoreEntryViewModel
import com.thomas.qwirkle.viewmodel.ScoreInput

@Composable
fun ScoreEntryScreen(
    gameId: Long,
    onRoundSaved: () -> Unit,
    onFinish: () -> Unit,
    onShowHistory: () -> Unit = {},
    onBack: () -> Unit,
    roundId: Long? = null, // gesetzt = vorhandene Runde bearbeiten
    viewModel: ScoreEntryViewModel = viewModel(
        factory = ScoreEntryViewModel.factory(gameId, rememberRepository(), roundId)
    )
) {
    val players by viewModel.players.collectAsState()
    val inputs by viewModel.inputs.collectAsState()
    val roundCount by viewModel.roundCount.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val editedRoundNumber by viewModel.editedRoundNumber.collectAsState()
    val orderLocked by viewModel.orderLocked.collectAsState()
    val activeIndex by viewModel.activeIndex.collectAsState()
    val confirmed by viewModel.confirmed.collectAsState()
    val savedQwirkles by viewModel.savedQwirkles.collectAsState()
    val isEditing = roundId != null
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showUnconfirmed by rememberSaveable { mutableStateOf(false) }

    val canSave = players.isNotEmpty() && !isSaving && !isLoading

    // Während der Rundenerfassung bleibt der Bildschirm an
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // Die Liste folgt dem Cursor
    LaunchedEffect(activeIndex, players.size) {
        val visible = listState.layoutInfo.visibleItemsInfo.any { it.index == activeIndex }
        if (!visible && players.isNotEmpty()) listState.animateScrollToItem(activeIndex)
    }

    val doSave: () -> Unit = {
        viewModel.save {
            // Neue Runde beginnt oben in der Liste
            scope.launch { listState.scrollToItem(0) }
            onRoundSaved()
        }
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
                    if (isEditing) {
                        editedRoundNumber?.let { stringResource(R.string.scoring_edit_round, it) } ?: ""
                    } else {
                        stringResource(R.string.scoring_round, roundCount + 1)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
            }

            ScoreColumnHeader()

            // Karten und Knöpfe scrollen; das Nummernpad bleibt unten fest
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(players, key = { _, p -> p.id }) { index, player ->
                    PlayerScoreCard(
                        player = player,
                        isActive = index == activeIndex,
                        isConfirmed = player.id in confirmed,
                        showOrderArrows = !orderLocked,
                        canMoveUp = index > 0,
                        canMoveDown = index < players.lastIndex,
                        onMove = { viewModel.movePlayer(player.id, it) },
                        input = inputs[player.id] ?: ScoreInput(),
                        onSelect = { viewModel.setCursor(index) }
                    )
                }
                item {
                    Column(
                        modifier = Modifier.padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            stringResource(R.string.scoring_note, GameRules.QWIRKLE_BONUS),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = {
                                if (viewModel.unconfirmedCount() > 0) showUnconfirmed = true else doSave()
                            },
                            enabled = canSave,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                stringResource(
                                    if (isEditing) R.string.scoring_save_changes else R.string.scoring_save
                                )
                            )
                        }
                        if (!isEditing) {
                            FilledTonalButton(
                                onClick = onShowHistory,
                                enabled = roundCount >= 1,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.scoring_history))
                            }
                            // Beenden erst, wenn mindestens eine Runde gespeichert ist
                            OutlinedButton(
                                onClick = onFinish,
                                enabled = roundCount >= 1,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.common_finish))
                            }
                            if (roundCount < 1) {
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

            // Alle Qwirkles des Spiels bisher, einschließlich der laufenden Runde
            Text(
                stringResource(
                    R.string.scoring_game_qwirkles,
                    savedQwirkles + inputs.values.sumOf { it.qwirkles }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )

            NumberPad(
                onDigit = viewModel::pressDigit,
                onDelete = viewModel::pressDelete,
                onQwirkleLess = viewModel::pressQwirkleLess,
                onQwirkle = viewModel::pressQwirkle,
                onConfirm = viewModel::pressConfirm,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
        }
    }

    if (showUnconfirmed) {
        AlertDialog(
            onDismissRequest = { showUnconfirmed = false },
            title = { Text(stringResource(R.string.scoring_unconfirmed_title)) },
            text = {
                Text(stringResource(R.string.scoring_unconfirmed_text, viewModel.unconfirmedCount()))
            },
            confirmButton = {
                TextButton(onClick = {
                    showUnconfirmed = false
                    doSave()
                }) { Text(stringResource(R.string.scoring_save_anyway)) }
            },
            dismissButton = {
                TextButton(onClick = { showUnconfirmed = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

private val PointsFieldWidth = 72.dp
private val QwirkleControlWidth = 56.dp
private val KeyHeight = 48.dp

/** Spaltenüberschriften, passend zu den Breiten in PlayerScoreCard. */
@Composable
private fun ScoreColumnHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            stringResource(R.string.scoring_points_label),
            modifier = Modifier.width(PointsFieldWidth),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            stringResource(R.string.scoring_qwirkles),
            modifier = Modifier.width(QwirkleControlWidth),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

/** Ein Spieler: Name mit Zwischensumme, Punktefeld (nur Anzeige), Qwirkle-Zähler mit Bonus. */
@Composable
private fun PlayerScoreCard(
    player: Player,
    isActive: Boolean,
    isConfirmed: Boolean,
    showOrderArrows: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (delta: Int) -> Unit,
    input: ScoreInput,
    onSelect: () -> Unit
) {
    val pointsDescription = stringResource(R.string.scoring_points_for, player.name)
    val darkSurface = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val activeColor = if (darkSurface) GamesChipContainerDark else GamesChipContainerLight
    val activeContent = if (darkSurface) GamesChipContentDark else GamesChipContentLight
    val total = (input.points.toIntOrNull() ?: 0) + GameRules.qwirkleBonus(input.qwirkles)

    // Dezente Standardkarte; der aktive Spieler wird nur am Punktefeld markiert
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerColorDot(colorIndex = player.colorIndex, size = 16)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        player.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        (if (isConfirmed) "✓ " else "") + stringResource(R.string.scoring_running_total, total),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isConfirmed) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isConfirmed) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            if (showOrderArrows) {
                OrderArrows(
                    playerName = player.name,
                    canMoveUp = canMoveUp,
                    canMoveDown = canMoveDown,
                    onMove = onMove
                )
            }

            // Punktefeld: nur Anzeige, eingegeben wird über das Nummernpad
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isActive) activeColor else MaterialTheme.colorScheme.surface,
                contentColor = if (isActive) activeContent else MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(
                    if (isActive) 2.dp else 1.dp,
                    if (isActive) activeContent else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .width(PointsFieldWidth)
                    .height(48.dp)
                    .semantics { contentDescription = "$pointsDescription: ${input.points}" }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        input.points.ifEmpty { "0" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Qwirkle-Zähler (nur Anzeige): mehr/weniger über Q+ und Q− am Nummernpad
            Column(
                modifier = Modifier.width(QwirkleControlWidth),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("${input.qwirkles}", style = MaterialTheme.typography.titleLarge)
                if (input.qwirkles > 0) {
                    Text(
                        stringResource(R.string.scoring_bonus, GameRules.qwirkleBonus(input.qwirkles)),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** Pfeile links neben dem Punktefeld: Spieler nach oben/unten schieben (nur vor der ersten Eingabe). */
@Composable
private fun OrderArrows(
    playerName: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (delta: Int) -> Unit
) {
    val upDescription = stringResource(R.string.scoring_move_up, playerName)
    val downDescription = stringResource(R.string.scoring_move_down, playerName)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(
            onClick = { onMove(-1) },
            enabled = canMoveUp,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .size(width = 36.dp, height = 28.dp)
                .semantics { contentDescription = upDescription }
        ) { Text("▲") }
        TextButton(
            onClick = { onMove(+1) },
            enabled = canMoveDown,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .size(width = 36.dp, height = 28.dp)
                .semantics { contentDescription = downDescription }
        ) { Text("▼") }
    }
}

/**
 * Eigenes Nummernpad (ohne Systemtastatur) in den Farben der Spielerkarten:
 * Ziffern, Q− / Q+ (Qwirkle weniger/mehr), rechts ⌫ (letzte Stelle löschen)
 * und ✓ (Spieler bestätigen und zum nächsten).
 */
@Composable
private fun NumberPad(
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit,
    onQwirkleLess: () -> Unit,
    onQwirkle: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val deleteDescription = stringResource(R.string.pad_delete)
    val lessDescription = stringResource(R.string.scoring_qwirkle_less)
    val moreDescription = stringResource(R.string.pad_qwirkle)
    val confirmDescription = stringResource(R.string.pad_confirm)
    val gap = 6.dp
    val captionText = stringResource(R.string.pad_qwirkle_caption)

    // Tasten sind Standardkarten: dieselben Farben wie die Spielerkarten
    @Composable
    fun Key(
        label: String,
        keyModifier: Modifier,
        description: String? = null,
        textColor: Color = Color.Unspecified,
        caption: String? = null,
        onClick: () -> Unit
    ) {
        val sem = if (description != null) Modifier.semantics { contentDescription = description } else Modifier
        Card(
            onClick = onClick,
            shape = RoundedCornerShape(10.dp),
            modifier = keyModifier.then(sem)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                if (caption != null) {
                    Text(caption, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
        Column(modifier = Modifier.weight(3f), verticalArrangement = Arrangement.spacedBy(gap)) {
            for (rowStart in listOf(7, 4, 1)) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (digit in rowStart..rowStart + 2) {
                        Key(digit.toString(), Modifier.weight(1f).height(KeyHeight)) { onDigit(digit) }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                Key("−", Modifier.weight(1f).height(KeyHeight), lessDescription, caption = captionText, onClick = onQwirkleLess)
                Key("0", Modifier.weight(1f).height(KeyHeight)) { onDigit(0) }
                Key("+", Modifier.weight(1f).height(KeyHeight), moreDescription, caption = captionText, onClick = onQwirkle)
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(gap)) {
            val tall = Modifier.fillMaxWidth().height(KeyHeight * 2 + gap)
            Key("⌫", tall, deleteDescription, onClick = onDelete)
            Key("✓", tall, confirmDescription, MaterialTheme.colorScheme.primary, onClick = onConfirm)
        }
    }
}
