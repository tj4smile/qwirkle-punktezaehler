package com.thomas.qwirkle.ui.screens.data

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thomas.qwirkle.R
import com.thomas.qwirkle.data.backup.CsvExporter
import com.thomas.qwirkle.data.backup.CsvLabels
import com.thomas.qwirkle.ui.components.BackButton
import com.thomas.qwirkle.ui.components.rememberBackupRepository
import com.thomas.qwirkle.ui.components.rememberSettings
import com.thomas.qwirkle.ui.components.screenPadding
import com.thomas.qwirkle.viewmodel.DataError
import com.thomas.qwirkle.viewmodel.DataStatus
import com.thomas.qwirkle.viewmodel.DataViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DataScreen(
    onBack: () -> Unit,
    viewModel: DataViewModel = viewModel(factory = DataViewModel.factory(rememberBackupRepository(), rememberSettings()))
) {
    val context = LocalContext.current
    val status by viewModel.status.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()
    var confirmImport by rememberSaveable { mutableStateOf(false) }

    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date()) }

    val jsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportJson { context.contentResolver.openOutputStream(uri, "wt") }
        }
    }

    val csvLabels = CsvLabels(
        date = stringResource(R.string.csv_date),
        game = stringResource(R.string.csv_game),
        testGame = stringResource(R.string.csv_test_game),
        finished = stringResource(R.string.csv_finished),
        round = stringResource(R.string.csv_round),
        player = stringResource(R.string.csv_player),
        points = stringResource(R.string.csv_points),
        qwirkles = stringResource(R.string.csv_qwirkles),
        qwirkleBonus = stringResource(R.string.csv_qwirkle_bonus),
        roundTotal = stringResource(R.string.csv_round_total),
        endBonus = stringResource(R.string.csv_end_bonus),
        yes = stringResource(R.string.csv_yes),
        no = stringResource(R.string.csv_no)
    )

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            viewModel.exportCsv(csvLabels, CsvExporter.delimiterFor(Locale.getDefault())) {
                context.contentResolver.openOutputStream(uri, "wt")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackup { context.contentResolver.openInputStream(uri) }
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
                    stringResource(R.string.data_title),
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isBusy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                status?.let { StatusCard(it) }

                ActionCard(
                    title = stringResource(R.string.data_backup_title),
                    text = stringResource(R.string.data_backup_text)
                ) {
                    Button(
                        onClick = {
                            viewModel.clearStatus()
                            jsonLauncher.launch("qwirkle-backup-$today.json")
                        },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.data_backup_button)) }
                }

                ActionCard(
                    title = stringResource(R.string.data_csv_title),
                    text = stringResource(R.string.data_csv_text)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.clearStatus()
                            csvLauncher.launch("qwirkle-games-$today.csv")
                        },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.data_csv_button)) }
                }

                ActionCard(
                    title = stringResource(R.string.data_import_title),
                    text = stringResource(R.string.data_import_text)
                ) {
                    OutlinedButton(
                        onClick = { confirmImport = true },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.data_import_button)) }
                }
            }
        }
    }

    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text(stringResource(R.string.data_import_confirm_title)) },
            text = { Text(stringResource(R.string.data_import_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmImport = false
                    viewModel.clearStatus()
                    importLauncher.launch(arrayOf("*/*"))
                }) { Text(stringResource(R.string.data_import_confirm_action)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmImport = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun ActionCard(title: String, text: String, button: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(text, style = MaterialTheme.typography.bodyMedium)
            button()
        }
    }
}

@Composable
private fun StatusCard(status: DataStatus) {
    val isError = status is DataStatus.Failed
    val message = when (status) {
        DataStatus.ExportDone -> stringResource(R.string.data_status_export_ok)
        is DataStatus.ImportDone ->
            stringResource(R.string.data_status_import_ok, status.players, status.games)
        is DataStatus.Failed -> stringResource(
            when (status.error) {
                DataError.WRITE -> R.string.data_error_write
                DataError.READ -> R.string.data_error_read
                DataError.NOT_A_BACKUP -> R.string.data_error_not_backup
                DataError.NEWER_VERSION -> R.string.data_error_newer
                DataError.INCONSISTENT -> R.string.data_error_inconsistent
                DataError.TOO_LARGE -> R.string.data_error_too_large
                DataError.UNKNOWN -> R.string.data_error_unknown
            }
        )
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp)
        )
    }
}
