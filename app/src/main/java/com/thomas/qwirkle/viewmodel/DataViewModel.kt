package com.thomas.qwirkle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.thomas.qwirkle.data.backup.BackupFormatException
import com.thomas.qwirkle.data.backup.BackupJson
import com.thomas.qwirkle.data.backup.CsvExporter
import com.thomas.qwirkle.data.backup.CsvLabels
import com.thomas.qwirkle.data.repository.BackupRepository
import com.thomas.qwirkle.data.settings.AppSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

enum class DataError { WRITE, READ, NOT_A_BACKUP, NEWER_VERSION, INCONSISTENT, TOO_LARGE, UNKNOWN }

sealed interface DataStatus {
    data object ExportDone : DataStatus
    data class ImportDone(val players: Int, val games: Int) : DataStatus
    data class Failed(val error: DataError) : DataStatus
}

class DataViewModel(
    private val repository: BackupRepository,
    private val settings: AppSettings
) : ViewModel() {

    private val _status = MutableStateFlow<DataStatus?>(null)
    val status: StateFlow<DataStatus?> = _status

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy

    /** Schreibt die komplette Sicherung als JSON in den Stream, den open() liefert. */
    fun exportJson(open: () -> OutputStream?) {
        var savedGames = 0
        exportText(
            open = open,
            onSuccess = { settings.recordBackup(System.currentTimeMillis(), savedGames) }
        ) {
            val snapshot = repository.createSnapshot()
            savedGames = countSavedGames(snapshot)
            BackupJson.encode(snapshot)
        }
    }

    /** Schreibt die Spieltabelle als CSV (mit BOM für Excel). */
    fun exportCsv(labels: CsvLabels, delimiter: Char, open: () -> OutputStream?) =
        exportText(open) {
            CsvExporter.BOM + CsvExporter.build(repository.createSnapshot(), labels, delimiter)
        }

    private fun exportText(
        open: () -> OutputStream?,
        onSuccess: () -> Unit = {},
        createText: suspend () -> String
    ) {
        if (_isBusy.value) return
        _isBusy.value = true
        _status.value = null
        viewModelScope.launch {
            val result: DataStatus = withContext(Dispatchers.IO) {
                try {
                    val text = createText()
                    val stream = open() ?: throw IOException("Ziel nicht verfügbar")
                    stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                    DataStatus.ExportDone
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    DataStatus.Failed(DataError.WRITE)
                }
            }
            if (result is DataStatus.ExportDone) onSuccess()
            _status.value = result
            _isBusy.value = false
        }
    }

    /** Liest eine JSON-Sicherung, prüft sie und ersetzt dann alle Daten. */
    fun importBackup(open: () -> InputStream?) {
        if (_isBusy.value) return
        _isBusy.value = true
        _status.value = null
        viewModelScope.launch {
            val result: DataStatus = withContext(Dispatchers.IO) {
                try {
                    val stream = open() ?: return@withContext DataStatus.Failed(DataError.READ)
                    val text = stream.use { readLimited(it) }
                        ?: return@withContext DataStatus.Failed(DataError.TOO_LARGE)
                    val data = BackupJson.decode(text)
                    repository.restore(data)
                    settings.recordBackup(System.currentTimeMillis(), countSavedGames(data))
                    DataStatus.ImportDone(data.players.size, data.games.size)
                } catch (e: BackupFormatException) {
                    DataStatus.Failed(
                        when (e.reason) {
                            BackupFormatException.Reason.NOT_A_BACKUP -> DataError.NOT_A_BACKUP
                            BackupFormatException.Reason.NEWER_VERSION -> DataError.NEWER_VERSION
                            BackupFormatException.Reason.INCONSISTENT -> DataError.INCONSISTENT
                        }
                    )
                } catch (e: IOException) {
                    DataStatus.Failed(DataError.READ)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    DataStatus.Failed(DataError.UNKNOWN)
                }
            }
            _status.value = result
            _isBusy.value = false
        }
    }

    /** Gespeicherte Nicht-Testspiele, so wie auch die Startseite sie zählt. */
    private fun countSavedGames(data: com.thomas.qwirkle.data.backup.BackupData): Int =
        data.games.count { it.isFinished && !it.isTestGame }

    fun clearStatus() {
        _status.value = null
    }

    /** Liest höchstens MAX_BYTES; bei größeren Dateien null (keine Sicherung dieser App). */
    private fun readLimited(input: InputStream): String? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > MAX_BYTES) return null
            out.write(buffer, 0, read)
        }
        return out.toString(Charsets.UTF_8.name())
    }

    companion object {
        private const val MAX_BYTES = 20 * 1024 * 1024

        fun factory(repository: BackupRepository, settings: AppSettings): ViewModelProvider.Factory =
            viewModelFactory { initializer { DataViewModel(repository, settings) } }
    }
}
