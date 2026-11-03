package com.thomas.qwirkle.ui.screens.players

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thomas.qwirkle.R
import com.thomas.qwirkle.data.entity.Player
import com.thomas.qwirkle.ui.components.BackButton
import com.thomas.qwirkle.ui.components.PlayerColorDot
import com.thomas.qwirkle.ui.components.rememberRepository
import com.thomas.qwirkle.ui.components.screenPadding
import com.thomas.qwirkle.ui.theme.playerColor
import com.thomas.qwirkle.util.NameValidation
import com.thomas.qwirkle.util.PlayerColors
import com.thomas.qwirkle.util.Validation
import com.thomas.qwirkle.viewmodel.PlayerViewModel

@Composable
fun PlayerManagementScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.factory(rememberRepository()))
) {
    val players by viewModel.players.collectAsState()

    // Nur IDs und Flags werden gemerkt, damit Dialoge eine Bildschirmdrehung überstehen
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val editingPlayer = players.firstOrNull { it.id == editingId }
    val deletingPlayer = players.firstOrNull { it.id == deletingId }

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
                    stringResource(R.string.players_title),
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(stringResource(R.string.players_add))
            }

            if (players.isEmpty()) {
                Text(
                    stringResource(R.string.players_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(players, key = { it.id }) { player ->
                    PlayerRow(
                        player = player,
                        onEdit = { editingId = player.id },
                        onDelete = { deletingId = player.id }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        PlayerDialog(
            title = stringResource(R.string.players_dialog_new_title),
            confirmLabel = stringResource(R.string.players_dialog_add),
            initialName = "",
            initialColorIndex = null, // Farbe wird automatisch vergeben
            onDismiss = { showAddDialog = false },
            onConfirm = { name, _, onError ->
                viewModel.addPlayer(name) { result ->
                    if (result == NameValidation.VALID) showAddDialog = false else onError(result)
                }
            }
        )
    }

    editingPlayer?.let { player ->
        PlayerDialog(
            title = stringResource(R.string.players_dialog_edit_title),
            confirmLabel = stringResource(R.string.common_save),
            initialName = player.name,
            initialColorIndex = player.colorIndex,
            onDismiss = { editingId = null },
            onConfirm = { name, color, onError ->
                viewModel.editPlayer(player, name, color) { result ->
                    if (result == NameValidation.VALID) editingId = null else onError(result)
                }
            }
        )
    }

    deletingPlayer?.let { player ->
        AlertDialog(
            onDismissRequest = { deletingId = null },
            title = { Text(stringResource(R.string.players_delete_title)) },
            text = { Text(stringResource(R.string.players_delete_text, player.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlayer(player)
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
private fun PlayerRow(player: Player, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerColorDot(colorIndex = player.colorIndex, size = 20)
            Spacer(Modifier.width(12.dp))
            Text(
                text = player.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onEdit) { Text(stringResource(R.string.players_edit)) }
            TextButton(onClick = onDelete) { Text(stringResource(R.string.common_delete)) }
        }
    }
}

@Composable
private fun SelectableColorDot(colorIndex: Int, selected: Boolean, onClick: () -> Unit) {
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

/**
 * Dialog für Name (und optional Farbe).
 * initialColorIndex == null blendet die Farbauswahl aus.
 */
@Composable
private fun PlayerDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    initialColorIndex: Int?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorIndex: Int, onError: (NameValidation) -> Unit) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var color by rememberSaveable { mutableStateOf(initialColorIndex ?: 0) }
    var error by remember { mutableStateOf<NameValidation?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    label = { Text(stringResource(R.string.players_field_name)) },
                    singleLine = true,
                    isError = error != null,
                    supportingText = { error?.let { Text(it.message()) } }
                )
                if (initialColorIndex != null) {
                    Text(
                        stringResource(R.string.players_field_color),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(PlayerColors.COUNT) { index ->
                            SelectableColorDot(
                                colorIndex = index,
                                selected = index == color,
                                onClick = { color = index }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, color) { error = it } }) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
private fun NameValidation.message(): String = when (this) {
    NameValidation.EMPTY -> stringResource(R.string.error_name_empty)
    NameValidation.TOO_LONG ->
        stringResource(R.string.error_name_too_long, Validation.MAX_NAME_LENGTH)
    NameValidation.DUPLICATE -> stringResource(R.string.error_name_duplicate)
    NameValidation.VALID -> ""
}
