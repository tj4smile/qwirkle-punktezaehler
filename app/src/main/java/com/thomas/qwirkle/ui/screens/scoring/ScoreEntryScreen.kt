package com.thomas.qwirkle.ui.screens.scoring

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.abs
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
    val isEditing = roundId != null

    val canSave = players.isNotEmpty() && !isSaving && !isLoading

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

            // Karten und Knöpfe scrollen zusammen, so bleibt auch bei 7 Spielern und
            // eingeblendeter Tastatur genug Platz zum Eintragen
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(players, key = { it.id }) { player ->
                    PlayerScoreCard(
                        player = player,
                        input = inputs[player.id] ?: ScoreInput(),
                        onPointsChange = { viewModel.setPoints(player.id, it) },
                        onQwirkleChange = { viewModel.changeQwirkles(player.id, it) }
                    )
                }
                item {
                    Column(
                        modifier = Modifier.padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            stringResource(R.string.scoring_note, GameRules.QWIRKLE_BONUS),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = { viewModel.save(onRoundSaved) },
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
        }
    }
}

private val PointsFieldWidth = 72.dp
private val QwirkleControlWidth = 116.dp // Minus (40) + Zahl (36) + Plus (40)

/** Spaltenüberschriften, passend zu den Breiten in PlayerScoreRow. */
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

/** Ein Spieler in einer Zeile: Farbpunkt + Name, Punktefeld, Qwirkle-Zähler mit Bonus. */
@Composable
private fun PlayerScoreCard(
    player: Player,
    input: ScoreInput,
    onPointsChange: (String) -> Unit,
    onQwirkleChange: (delta: Int) -> Unit
) {
    val lessDescription = stringResource(R.string.scoring_qwirkle_less)
    val moreDescription = stringResource(R.string.scoring_qwirkle_more)
    val pointsDescription = stringResource(R.string.scoring_points_for, player.name)
    var showPicker by rememberSaveable { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
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
                Text(
                    player.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Tippen öffnet das Zahlenrad (mit Umschalter auf die Tastatur)
            OutlinedButton(
                onClick = { showPicker = true },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .width(PointsFieldWidth)
                    .height(48.dp)
                    .semantics { contentDescription = "$pointsDescription: ${input.points}" }
            ) {
                Text(input.points.ifEmpty { "0" }, style = MaterialTheme.typography.titleMedium)
            }

            Row(
                modifier = Modifier.width(QwirkleControlWidth),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onQwirkleChange(-1) },
                    enabled = input.qwirkles > 0,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .size(40.dp)
                        .semantics { contentDescription = lessDescription }
                ) { Text("−") }
                Column(
                    modifier = Modifier.width(36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("${input.qwirkles}", style = MaterialTheme.typography.titleMedium)
                    if (input.qwirkles > 0) {
                        Text(
                            stringResource(
                                R.string.scoring_bonus,
                                GameRules.qwirkleBonus(input.qwirkles)
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }
                OutlinedButton(
                    onClick = { onQwirkleChange(+1) },
                    enabled = input.qwirkles < ScoreEntryViewModel.MAX_QWIRKLES,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .size(40.dp)
                        .semantics { contentDescription = moreDescription }
                ) { Text("+") }
            }
        }
    }

    if (showPicker) {
        PointsPickerDialog(
            playerName = player.name,
            current = input.points.toIntOrNull() ?: 0,
            onDismiss = { showPicker = false },
            onConfirm = {
                onPointsChange(it.toString())
                showPicker = false
            }
        )
    }
}

private const val WHEEL_MAX = 20
private const val WHEEL_VISIBLE_ROWS = 5
private val WheelRowHeight = 44.dp

/**
 * Dialog zur Punkteingabe: ein Zahlenrad von 0 bis 20 zum Drehen und Antippen.
 * Über "Tastatur" lassen sich auch größere Werte (bis 2 Stellen) eintippen.
 * Ist der aktuelle Wert größer als das Rad, öffnet der Dialog gleich mit der Tastatur.
 */
@Composable
private fun PointsPickerDialog(
    playerName: String,
    current: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var keyboardMode by rememberSaveable { mutableStateOf(current > WHEEL_MAX) }
    var typed by rememberSaveable { mutableStateOf(current.toString()) }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = current.coerceIn(0, WHEEL_MAX)
    )
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    // Das Element, das der Mitte des Rads am nächsten liegt, ist die Auswahl
    val selected by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo
                .minByOrNull { abs(it.offset + it.size / 2 - center) }
                ?.index ?: current.coerceIn(0, WHEEL_MAX)
        }
    }

    // Zurück zum Rad: auf den getippten Wert springen
    LaunchedEffect(keyboardMode) {
        if (keyboardMode) {
            focusRequester.requestFocus()
        } else {
            listState.scrollToItem((typed.toIntOrNull() ?: 0).coerceIn(0, WHEEL_MAX))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(playerName) },
        text = {
            if (keyboardMode) {
                OutlinedTextField(
                    value = typed,
                    onValueChange = {
                        typed = it.filter(Char::isDigit)
                            .take(ScoreEntryViewModel.MAX_POINT_DIGITS)
                    },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            } else {
              Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WheelRowHeight * WHEEL_VISIBLE_ROWS),
                    contentAlignment = Alignment.Center
                ) {
                    // Markierung der mittleren Zeile
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(WheelRowHeight)
                    ) {}
                    LazyColumn(
                        state = listState,
                        flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Center),
                        contentPadding = PaddingValues(
                            vertical = WheelRowHeight * (WHEEL_VISIBLE_ROWS / 2)
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(WHEEL_MAX + 1) { value ->
                            val isSelected = value == selected
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(WheelRowHeight)
                                    .clickable {
                                        scope.launch { listState.animateScrollToItem(value) }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    value.toString(),
                                    style = if (isSelected) {
                                        MaterialTheme.typography.titleLarge
                                    } else {
                                        MaterialTheme.typography.bodyLarge
                                    },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
                Text(
                    stringResource(R.string.scoring_picker_more_hint, WHEEL_MAX),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
              }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(if (keyboardMode) typed.toIntOrNull() ?: 0 else selected)
            }) { Text(stringResource(R.string.common_ok)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    if (!keyboardMode) typed = selected.toString()
                    keyboardMode = !keyboardMode
                }) {
                    Text(
                        stringResource(
                            if (keyboardMode) R.string.scoring_picker_wheel
                            else R.string.scoring_picker_keyboard
                        )
                    )
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
            }
        }
    )
}
